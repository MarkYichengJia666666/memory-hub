package com.yqg.scheduler.jobs.loan;

import com.yqg.core.model.sql.loan.manualreduction.enums.ManualReductionTaskStatus;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.CashLoanRepayStrategyService;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.enums.CashLoanRepayStrategyStatus;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.vo.CashLoanRepayStrategyVO;
import com.yqg.core.service.cashloan.enums.ReductionScene;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.loan.manualreduction.ManualReductionTaskService;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionTaskVO;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CashLoanRepayStrategyStatusJob extends YqgBaseJob {
  @Autowired
  private CashLoanRepayStrategyService cashLoanRepayStrategyService;
  @Autowired
  private ManualReductionTaskService manualReductionTaskService;
  @Autowired
  private EcOrderService ecOrderService;
  @Override
  public void exec(JobExecutionContext context) throws Exception {

    Param param = getParam(context, Param.class);
    Long maxId = 0L;
    while (true) {
      List<CashLoanRepayStrategyVO> validRecords = cashLoanRepayStrategyService.findValidRecords(param.limit, maxId);
      if (CollectionUtils.isEmpty(validRecords)) {
        break;
      }
      Map<Long, CashLoanInstalmentVO> instalmentVOMap =
          ecOrderService.getIdToInstalmentVOMap(validRecords.stream().map(CashLoanRepayStrategyVO::getInstalmentId).collect(Collectors.toList()));
      List<Long> expiredIds = validRecords.stream()
          .filter(s -> {
            if (s.getExpiredTime() < Clock.now()) {
              return true;
            }
            CashLoanInstalmentVO cashLoanInstalmentVO = instalmentVOMap.get(s.getInstalmentId());
            if (Objects.isNull(cashLoanInstalmentVO)) {
              return true;
            }
            return cashLoanInstalmentVO.getStatus() == CashLoanInstalmentStatus.COMPLETE;
          })
          .map(CashLoanRepayStrategyVO::getId)
          .collect(Collectors.toList());
      cashLoanRepayStrategyService.updateStrategyStatusByIds(expiredIds);
      maxId = validRecords.get(0).getId();
    }

    List<ManualReductionTaskVO> pendingTasks = manualReductionTaskService.findPendingTasks();
    if (pendingTasks.isEmpty()) {
      return;
    }
    pendingTasks.forEach(item -> {
      List<Long> allInstalmentIds = item.obtainAllInstalmentIds();
      Map<Long, CashLoanInstalmentVO> instalmentVOMap = ecOrderService.getIdToInstalmentVOMap(allInstalmentIds);
      // 判断task是已还清还是已失效
      boolean competed = allInstalmentIds.stream()
          .allMatch(i -> instalmentVOMap.get(i).getStatus() == CashLoanInstalmentStatus.COMPLETE);
      if (competed) {
        manualReductionTaskService.updateStatus(item.getId(), ManualReductionTaskStatus.SUCCESS);
      } else {
        List<CashLoanRepayStrategyVO> cashLoanRepayStrategyVOS = cashLoanRepayStrategyService.findByReductionTask(item);
        // 客服减免-如果工单中的策略有任何一个失效了，则工单失效;催收减免-如果工单中的策略没有任何一个有效了，则工单失效
        updateInvalid(item.getId(), item.getTaskType(), cashLoanRepayStrategyVOS);
      }
    });
  }


  // 客服减免-如果工单中的策略有任何一个失效了，则工单失效;催收减免-如果工单中的策略没有任何一个有效了，则工单失效
  private void updateInvalid(Long taskId, ReductionScene taskType, List<CashLoanRepayStrategyVO> cashLoanRepayStrategyVOS) {
    boolean isCsReduce = Objects.equals(ReductionScene.CUSTOMER_SERVICE_REDUCE, taskType);
    boolean isCollectionReduce = Objects.equals(ReductionScene.COLLECTION_REDUCE, taskType);
    boolean hasAnyInvalid = cashLoanRepayStrategyVOS.stream().anyMatch(s -> s.getStatus() == CashLoanRepayStrategyStatus.INVALID);
    boolean noneValid = cashLoanRepayStrategyVOS.stream().noneMatch(s -> s.getStatus() == CashLoanRepayStrategyStatus.VALID);
    if ((isCsReduce && hasAnyInvalid) || (isCollectionReduce && noneValid)) {
      manualReductionTaskService.updateStatus(taskId, ManualReductionTaskStatus.FAIL);
    }
  }

  public static class Param {
    public int limit = 100;
  }
}
