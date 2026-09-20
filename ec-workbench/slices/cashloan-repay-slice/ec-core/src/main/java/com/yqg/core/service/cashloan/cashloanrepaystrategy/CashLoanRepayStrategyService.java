package com.yqg.core.service.cashloan.cashloanrepaystrategy;

import com.google.common.collect.Lists;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.aop.RunInTransaction;
import com.yqg.core.model.generated.tables.records.CashLoanRepaymentStrategyRecord;
import com.yqg.core.model.sql.loan.cashloanrepaystrategy.CashLoanRepayStrategyModel;
import com.yqg.core.model.sql.loan.manualreduction.ManualReductionTaskModel;
import com.yqg.core.model.generated.tables.records.ManualReductionTaskRecord;
import com.yqg.core.service.cashloan.enums.ReductionScene;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.enums.CashLoanRepayStrategyStatus;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.vo.CashLoanRepayStrategyVO;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.vo.RepaymentAccountInfoVO;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.util.rate.CalcFeeUtil;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionDetail;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionTaskVO;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.i18n.time.Clock;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CashLoanRepayStrategyService {

  @Autowired
  private CashLoanRepayStrategyModel cashLoanRepayStrategyModel;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private ManualReductionTaskModel manualReductionTaskModel;

  @RunInTransaction
  public void createCashLoanRepayStrategy(Long loanAccountId,
      ManualReductionDetail reductionDetail,
      RepaymentAccountInfoVO repaymentAccountInfoVO,
      Long reductionTaskId) {
    List<ManualReductionDetail.OrderReductionDetail> orderReductionDetails = reductionDetail.getOrderReductionDetails();
    orderReductionDetails.forEach(orderReductionDetail -> {
      List<ManualReductionDetail.InstalmentReductionDetail> installmentReductionDetails = orderReductionDetail.getInstalmentReductionDetails();
      installmentReductionDetails.forEach(installmentReductionDetail -> {
        CashLoanRepaymentStrategyRecord record = cashLoanRepayStrategyModel.insert(loanAccountId,
            orderReductionDetail.getOrderId(),
            installmentReductionDetail.getInstalmentId(),
            reductionDetail.getExpiredTime(),
            installmentReductionDetail.getReductionAmount(),
            repaymentAccountInfoVO,
            installmentReductionDetail.getDeductAmountDetail(),
            reductionTaskId);

      });
    });
  }


  public void updateStrategyStatusByIds(List<Long> expiredIds) {
    List<CashLoanRepaymentStrategyRecord> strategyRecords = cashLoanRepayStrategyModel.findByIds(expiredIds);
    if (CollectionUtils.isEmpty(strategyRecords)) {
      return;
    }
    // 根据 reductionTaskId 查询 manual_reduction_task.task_type
    List<Long> taskIds = strategyRecords.stream()
        .map(CashLoanRepaymentStrategyRecord::getReductionTaskId)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());

    Map<Long, ManualReductionTaskRecord> taskIdAndInfoMap = taskIds.isEmpty()
        ? Collections.emptyMap()
        : manualReductionTaskModel.findByIds(taskIds)
        .stream()
        .collect(Collectors.toMap(ManualReductionTaskRecord::getId, Function.identity()));

    long now = Clock.now();
    for (CashLoanRepaymentStrategyRecord r : strategyRecords) {

      Long taskId = r.getReductionTaskId();
      ManualReductionTaskRecord task = (taskId == null) ? null : taskIdAndInfoMap.get(taskId);
      String status = resolveStatusByTask(task);
      r.setStatus(status);
      r.setTimeUpdated(now);
    }
    cashLoanRepayStrategyModel.batchUpdateRecords(strategyRecords);
  }


  /**
   * 基于ID批量修改策略状态
   * 1. 开事务
   * 2. 数据库行级加锁
   * 3. 检查状态
   * 4. 修改状态（幂等：目标状态==当前状态则跳过）
   */
  @RunInTransaction
  public void updateStatusByIds(List<Long> strategyIds, CashLoanRepayStrategyStatus targetStatus) {
    if (CollectionUtils.isEmpty(strategyIds)) {
      return;
    }
    // 加锁读取
    List<CashLoanRepaymentStrategyRecord> records = cashLoanRepayStrategyModel.findByIdsForUpdate(strategyIds);
    if (records.size() != strategyIds.size()) {
      throw EcException.error("some strategy ids not found or duplicated, expect:{}, actual:{}", strategyIds.size(), records.size());
    }
    long now = Clock.now();
    List<CashLoanRepaymentStrategyRecord> toUpdate = new ArrayList<>(records.size());
    for (CashLoanRepaymentStrategyRecord r : records) {
      CashLoanRepayStrategyStatus current = CashLoanRepayStrategyStatus.fromCode(r.getStatus());
      // 只允许从 VALID 迁移到上述集合
      if (current != CashLoanRepayStrategyStatus.VALID) {
        throw EcException.error("strategy id:{} status is not valid", r.getId());
      }
      r.setTimeUpdated(now);
      r.setStatus(targetStatus.getCode());
      toUpdate.add(r);
    }
    cashLoanRepayStrategyModel.batchUpdateRecords(toUpdate);
  }

  public List<CashLoanRepayStrategyVO> findValidRecords(int limit, Long minId) {
    return cashLoanRepayStrategyModel.findValidRecords(limit, minId)
        .stream()
        .map(CashLoanRepayStrategyVO::from)
        .collect(Collectors.toList());
  }

  public List<CashLoanRepayStrategyVO> findValidStrategy(Long accountId) {
    return cashLoanRepayStrategyModel.findValidStrategyByLoanAccountId(accountId)
        .stream()
        .map(CashLoanRepayStrategyVO::from)
        .collect(Collectors.toList());
  }

  public BigDecimal calValidOwedAmountAmountByAccountId(Long loanAccountId) {
    Map<Long /* instalmentId */, CashLoanRepayStrategyVO> cashLoanRepayStrategyVOS = cashLoanRepayStrategyModel.findValidStrategyByLoanAccountId(loanAccountId)
        .stream()
        .map(CashLoanRepayStrategyVO::from)
        .collect(Collectors.toMap(CashLoanRepayStrategyVO::getInstalmentId, Function.identity(), (a, b) -> a));

    Map<Long /* instalmentId */, CashLoanInstalmentVO> instalmentVOMap = ecOrderService.getIdToInstalmentVOMap(Lists.newArrayList(cashLoanRepayStrategyVOS.keySet()));

    AtomicReference<BigDecimal> res = new AtomicReference<>(BigDecimal.ZERO);
    cashLoanRepayStrategyVOS.forEach((k, v) -> {
      CashLoanInstalmentVO cashLoanInstalmentVO = instalmentVOMap.get(k);
      if (Objects.isNull(cashLoanInstalmentVO)) {
        return;
      }
      if (cashLoanInstalmentVO.getStatus() != CashLoanInstalmentStatus.INIT) {
        throw EcException.error("instalment status is not init, loanAccountId:{}, instalmentId:{}", loanAccountId, k);
      }
      BigDecimal owedAmount = CalcFeeUtil.getOwedAmount(cashLoanInstalmentVO);
      res.set(BigDecimalHelper.addWithNullAsZeroAndScale(res.get(), BigDecimalHelper.substractNullAsZeroAndScale(owedAmount, v.getDeductAmount())));
    });
    return res.get();
  }

  public void updateInvalidByAccountId(Long accountId) {
    List<CashLoanRepaymentStrategyRecord> records = cashLoanRepayStrategyModel.findValidStrategyByLoanAccountId(accountId);
    long now = Clock.now();
    records.forEach(r -> {
      r.setTimeUpdated(now);
      r.setStatus(CashLoanRepayStrategyStatus.INVALID.getCode());
    });
    cashLoanRepayStrategyModel.batchUpdateRecords(records);
  }

  public List<CashLoanRepayStrategyVO> findByIds(List<Long> allStrategyIds) {
    return cashLoanRepayStrategyModel.findByIds(allStrategyIds).stream()
        .map(CashLoanRepayStrategyVO::from)
        .collect(Collectors.toList());
  }

  public List<CashLoanRepayStrategyVO> findByReductionTaskId(Long reductionTaskId) {
    return cashLoanRepayStrategyModel.findByReductionTaskId(reductionTaskId).stream()
        .map(CashLoanRepayStrategyVO::from)
        .collect(Collectors.toList());
  }

  /**
   * 获取手动减免任务下，关联的所有账单减免计划
   */
  public List<CashLoanRepayStrategyVO> findByReductionTask(ManualReductionTaskVO manualReductionTask) {
    // 在催收减免需求中，为 cash_loan_repayment_strategy 表中新增 reduction_task_id 列，并置空 manual_reduction_task 表中的 cashLoanRepayStrategyId 。
    // 如果能根据 taskId 查询到 strategy ，则说明是催收减免需求之后的数据；如果查不到，说明是催收减免需求之前的数据，按照 obtainAllStrategy 原逻辑取数。
    List<CashLoanRepayStrategyVO> cashLoanRepayStrategyVOList = findByReductionTaskId(manualReductionTask.getId());
    return CollectionUtils.isEmpty(cashLoanRepayStrategyVOList) ? findByIds(manualReductionTask.obtainAllStrategyIds()) : cashLoanRepayStrategyVOList;
  }

  /**
   * 批量修改减免任务中有效账单状态
   *
   * @param reductionTaskId
   * @param cashLoanRepayStrategyStatus
   */
  public List<CashLoanRepaymentStrategyRecord> updateValidStrategyByReductionTaskId(Long reductionTaskId, CashLoanRepayStrategyStatus cashLoanRepayStrategyStatus) {
    List<CashLoanRepaymentStrategyRecord> records = cashLoanRepayStrategyModel.findValidStrategyByReductionTaskId(reductionTaskId);
    long now = Clock.now();
    records.forEach(r -> {
      r.setTimeUpdated(now);
      r.setStatus(cashLoanRepayStrategyStatus.getCode());
    });
    cashLoanRepayStrategyModel.batchUpdateRecords(records);
    return records;
  }

  public boolean canDoCsDeduct(Long accountId, CurrencyAmount amount) {
    BigDecimal validOwedAmountAmount = calValidOwedAmountAmountByAccountId(accountId);
    return validOwedAmountAmount.compareTo(BigDecimal.ZERO) > 0
        && validOwedAmountAmount.compareTo(amount.getAmountInYuan()) <= 0;
  }

  private String resolveStatusByTask(ManualReductionTaskRecord task) {
    if (task == null) {
      return CashLoanRepayStrategyStatus.INVALID.getCode();
    }
    String taskType = task.getTaskType();
    if (Objects.equals(ReductionScene.CUSTOMER_SERVICE_REDUCE.getCode(), taskType)) {
      return CashLoanRepayStrategyStatus.INVALID.getCode();
    }
    return CashLoanRepayStrategyStatus.WAIT_NOTIFY_EXPIRED.getCode();
  }
}
