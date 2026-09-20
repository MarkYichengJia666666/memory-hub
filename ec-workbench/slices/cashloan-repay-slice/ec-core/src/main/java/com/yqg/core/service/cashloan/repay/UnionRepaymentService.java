package com.yqg.core.service.cashloan.repay;

import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.DeductIntentionLogRecord;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.RepaymentAnalysisRecord;
import com.yqg.core.model.generated.tables.records.RepaymentPlanLogRecord;
import com.yqg.core.model.generated.tables.records.RepaymentSplitUnitRecord;
import com.yqg.core.model.loader.RepaymentAmountLoader;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.cashloan.DeductIntentionLogModel;
import com.yqg.core.model.sql.cashloan.RepaymentPlanLogModel;
import com.yqg.core.model.sql.cashloan.RepaymentSplitUnitModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.ExpFacade.ClientType;
import com.yqg.core.service.cashloan.RepaymentConfig;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.repay.enums.DeductIntentionStatus;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentDisplayStrategy;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repay.strategy.RepaymentValidationStrategy;
import com.yqg.core.service.cashloan.repay.vo.DeductIntentionVO;
import com.yqg.core.service.cashloan.repay.vo.RepaySplitUnitVO;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentPlanVO;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentVO;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vo.CouponCalcContext;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.loan.repayment.analysis.RepaymentAnalysisModel;
import com.yqg.core.service.loan.repayment.analysis.RepaymentAnalysisStatus;
import com.yqg.core.service.payment.vo.PaymentProcessResult;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.LoanCouponUsageType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.EcAsserts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UnionRepaymentService {
  @Autowired
  private ExpFacade expFacade;
  @Autowired
  private DeductIntentionLogModel deductIntentionLogModel;
  @Autowired
  private RepaymentPlanLogModel repaymentPlanLogModel;
  @Autowired
  private RepaymentSplitUnitModel repaymentSplitUnitModel;
  @Autowired
  private UnionRepaymentLocker unionRepaymentLocker;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private ThreadTransactionalModel transactionalModel;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private RepaymentAnalysisModel repaymentAnalysisModel;
  @Autowired
  private DeductIntentionLogService deductIntentionLogService;
  @Autowired
  private RepaymentAmountLoader amountLoader;
  @Autowired
  private RepaymentConfig repaymentConfig;

  /**
   * JBP支持验证策略 - 允许0或1个EC还款单元，但至少有一个单元
   */
  private final RepaymentValidationStrategy jbpSupportValidationStrategy = (ecUnits, otherUnits, transNo) -> {
    EcAsserts.assertTrue(
        ecUnits.size() <= 1,
        "ecRepaySplitUnitVOS size error! transNo:{}, size:{}",
        transNo,
        ecUnits.size()
    );
    EcAsserts.assertTrue(
        !(CollectionUtils.isEmpty(ecUnits) && CollectionUtils.isEmpty(otherUnits)),
        "SplitUnitVOS size error, ecRepaySplitUnitVOS or otherSplitUnitVOS must not empty! transNo:{}",
        transNo
    );
  };

  //需要先做版本控制
  public boolean fetchUnionRepaymentResult(Long userId) {
    UnionRepaymentDisplayStrategy result = UnionRepaymentDisplayStrategy.valueOf(
        expFacade.fetchResult("pretii-other-abroad-loan_all-EASYPLUS_PAY_V1_0710", ClientType.DIVERSION,
            ABTestSceneType.UNION_REPAYMENT_EXPR, ABTestUtil.genDiversionKeyMapByUserId(userId)));
    return result == UnionRepaymentDisplayStrategy.GOLD_CARD_BILLING_PAGE;
  }

  public DeductIntentionVO initiateUnionRepaymentPlan(Long userId, List<UnionRepaymentPlanVO> unionRepaymentPlanVOList) {
    DeductIntentionLogRecord r = deductIntentionLogModel.insert(userId, JsonUtils.toString(unionRepaymentPlanVOList));
    return DeductIntentionVO.from(r);
  }

  private List<RepaySplitUnitVO> buildRepaySplitUnitVOList(PaymentVO paymentVO, DeductIntentionVO deductIntentionVO) {
    //保证可重入
    List<RepaySplitUnitVO> repaySplitUnitVOS = queryByTransNo(paymentVO.thirdPartyPayOrderId);
    if (CollectionUtils.isNotEmpty(repaySplitUnitVOS)) {
      return repaySplitUnitVOS;
    }
    //根据计划拆帐，并入库
    repaySplitUnitVOS = splitPaymentByPlan(paymentVO.thirdPartyPayOrderId, paymentVO.userId, paymentVO.amount.getAmountInYuan(), deductIntentionVO);
    repaymentSplitUnitModel.batchInsert(repaySplitUnitVOS);
    return queryByTransNo(paymentVO.thirdPartyPayOrderId);
  }


  /**
   * 支持只还中收订单，无EC账单时的抵扣逻辑
   *
   * @param paymentVO
   * @return
   */
  public PaymentProcessResult repay(PaymentVO paymentVO) {
    return processRepayment(paymentVO, jbpSupportValidationStrategy);
  }


  /**
   * 通用还款处理方法，接受不同的验证策略
   *
   * @param paymentVO          支付信息
   * @param validationStrategy 验证策略
   * @return 处理结果
   */
  private PaymentProcessResult processRepayment(PaymentVO paymentVO, RepaymentValidationStrategy validationStrategy) {
    return unionRepaymentLocker.lockAndRunResult(paymentVO.userId, () -> {
      // 1. 准备阶段：获取还款计划和构建分割单元 deductIntentionVO有可能为空
      DeductIntentionVO deductIntentionVO = insertRealtimeRepaymentPlan(paymentVO.userId, paymentVO.thirdPartyPayOrderId);
      List<RepaySplitUnitVO> unitVOS = buildRepaySplitUnitVOList(paymentVO, deductIntentionVO);
      EcAsserts.assertTrue(CollectionUtils.isNotEmpty(unitVOS), "unitVOS must not empty!, transNo:{}", paymentVO.thirdPartyPayOrderId);
      if (unitVOS.stream().allMatch(r -> r.repayStatus == ProcessStatus.PROCESSED)) {
        return PaymentProcessResult.from(ProcessStatus.PROCESSED, null);
      }

      // 2. 分类阶段：将单元分为EC还款和其他还款
      Map<Boolean, List<RepaySplitUnitVO>> groupedUnits = unitVOS.stream()
          .collect(Collectors.partitioningBy(vo -> vo.deductType == UnionRepaymentType.EC_REPAY));
      List<RepaySplitUnitVO> ecRepaySplitUnitVOS = groupedUnits.get(true);
      List<RepaySplitUnitVO> otherSplitUnitVOS = groupedUnits.get(false);

      // 3. 验证阶段：使用提供的验证策略
      validationStrategy.validate(ecRepaySplitUnitVOS, otherSplitUnitVOS, paymentVO.thirdPartyPayOrderId);

      // 4. 处理阶段：在事务中处理还款
      PaymentProcessResult processResult = processRepaymentsInTransaction(paymentVO, deductIntentionVO, unitVOS, ecRepaySplitUnitVOS,
          otherSplitUnitVOS);

      // 5.后置处理：删除缓存
      clearRepaymentAmountCacheOnProcessed(processResult, paymentVO);
      return processResult;
    });
  }

  /**
   * 在事务中处理还款
   */
  private PaymentProcessResult processRepaymentsInTransaction(
      PaymentVO paymentVO,
      DeductIntentionVO deductIntentionVO,
      List<RepaySplitUnitVO> allUnits,
      List<RepaySplitUnitVO> ecUnits,
      List<RepaySplitUnitVO> otherUnits) {

    return transactionalModel.transactionResult(configuration -> {
      // 1. 先加把大锁
      LoanAccountRecord accountRecord = loanAccountModel.findByUserIdForUpdateOrThrow(paymentVO.userId);

      List<PaymentProcessResult> processResultList = new ArrayList<>();

      // 2. 处理EC还款（如果有）
      if (CollectionUtils.isNotEmpty(ecUnits)) {
        PaymentProcessResult ecResult = processEcRepayment(ecUnits.get(0), paymentVO, deductIntentionVO, accountRecord, allUnits);
        if (ecResult.processStatus == ProcessStatus.FAILED) {
          return ecResult;
        }
        processResultList.add(ecResult);
      }

      // 3. 处理其他还款
      for (RepaySplitUnitVO unit : otherUnits) {
        PaymentProcessResult result = handleRepayment(unit, paymentVO, deductIntentionVO, accountRecord);
        processResultList.add(result);
      }

      // 4. 处理结果
      return handleRepaymentResult(processResultList, deductIntentionVO);
    });
  }

  /**
   * 处理EC还款
   */
  private PaymentProcessResult processEcRepayment(
      RepaySplitUnitVO ecUnit,
      PaymentVO paymentVO,
      DeductIntentionVO deductIntentionVO,
      LoanAccountRecord accountRecord,
      List<RepaySplitUnitVO> allUnits) {

    PaymentProcessResult ecRepayResult = handleRepayment(ecUnit, paymentVO, deductIntentionVO, accountRecord);
    if (ecRepayResult.processStatus == ProcessStatus.FAILED) {
      batchUpdateFailed(allUnits);
      return ecRepayResult;
    }
    return ecRepayResult;
  }

  private PaymentProcessResult handleRepayment(RepaySplitUnitVO repaySplitUnitVO, PaymentVO paymentVO, DeductIntentionVO deductIntentionVO, LoanAccountRecord accountRecord) {
    if (repaySplitUnitVO.getRepayStatus() != ProcessStatus.UNPROCESSED) {
      throw EcException.error("No Unprocessed status from EC repay! TransNo:" + repaySplitUnitVO.transNo);
    }
    UnionRepaymentHandler handler = UnionRepaymentFactory.getMethod(repaySplitUnitVO.deductType);
    UnionRepaymentVO unionRepaymentVO = UnionRepaymentVO.from(repaySplitUnitVO, paymentVO, accountRecord);
    return handler.handleRepayment(unionRepaymentVO, repaySplitUnitVO.id, deductIntentionVO);
  }

  private List<RepaySplitUnitVO> queryByTransNo(String transNo) {
    return repaymentSplitUnitModel.fetchByTransNo(transNo)
        .stream()
        .map(RepaySplitUnitVO::from)
        .collect(Collectors.toList());
  }

  private void batchUpdateFailed(List<RepaySplitUnitVO> repaySplitUnitVOS) {
    List<RepaymentSplitUnitRecord> records = repaymentSplitUnitModel.fetchByIds(repaySplitUnitVOS.stream().map(RepaySplitUnitVO::getId).collect(Collectors.toList()));
    records.forEach(record -> {
      record.setRepayStatus(ProcessStatus.FAILED.code);
    });
    repaymentSplitUnitModel.batchUpdate(records);
  }

  private PaymentProcessResult handleRepaymentResult(List<PaymentProcessResult> processResultList, DeductIntentionVO deductIntentionVO) {
    if (processResultList.stream().map(o -> o.processStatus).allMatch(result -> result == ProcessStatus.PROCESSED)) {
      completeDeductIntentionIfExist(deductIntentionVO);
      return PaymentProcessResult.from(ProcessStatus.PROCESSED, null);
    }
    throw EcException.error("handle repayment failed! DeductIntentionId:{}", deductIntentionVO.id);
  }

  private void completeDeductIntentionIfExist(DeductIntentionVO deductIntentionVO) {
    if (Objects.isNull(deductIntentionVO)) {
      return;
    }
    DeductIntentionLogRecord deductIntentionLogRecord = deductIntentionLogModel.fetchById(deductIntentionVO.id);
    deductIntentionLogModel.completeIntention(deductIntentionLogRecord);
  }

  /**
   * 处理合并支付jbp的抵扣回调消息
   *
   * @param transNo
   * @param repayAmount
   * @param userId
   */
  public void dealCombinedRepayJbpCallBack(String transNo, BigDecimal repayAmount, Long userId) {
    try {
      transactionalModel.transaction(configuration -> {
        //1. 锁
        RepaymentSplitUnitRecord record = repaymentSplitUnitModel.selectForUpdateOrThrowByTransNoAndType(transNo, UnionRepaymentType.JBP);
        // 2.验
        checkRepaySplitUnitModel(record, userId, repayAmount);
        // 3.更新
        repaymentSplitUnitModel.updateStatusByRecord(record, ProcessStatus.PROCESSED);
      });
    } catch (Exception e) {
      log.error("dealCombinedRepayCallBack exception transNo:{}", transNo, e);
    }
  }

  private void checkRepaySplitUnitModel(RepaymentSplitUnitRecord record, Long userId, BigDecimal repayAmount) {
    if (!record.getRepayStatus().equals(ProcessStatus.UNPROCESSED.code)) {
      log.error("checkRepaySplitUnitModel transNo:{} not UNPROCESSED, status:{}", record.getTranNo(), record.getRepayStatus());
      throw new IllegalArgumentException("Repayment status does not match");
    }
    if (!record.getUserId().equals(userId)) {
      log.error("checkRepaySplitUnitModel transNo:{} userId not match, expected: {}, actual: {}", record.getTranNo(),
          record.getUserId(), userId);
      throw new IllegalArgumentException("Repayment userId does not match");
    }
    if (record.getAmount().compareTo(repayAmount) != 0) {
      log.error("checkRepaySplitUnitModel transNo:{} repayAmount not match, expected: {}, actual: {}",
          record.getTranNo(), record.getAmount(), repayAmount);
      throw new IllegalArgumentException("Repayment amount does not match");
    }
  }

  public DeductIntentionVO insertRealtimeRepaymentPlan(Long userId, String transNo) {
    DeductIntentionLogRecord deductIntentionLogRecord = deductIntentionLogModel.fetchLatestDeductIntention(userId);
    if (Objects.isNull(deductIntentionLogRecord)) {
      return null;
    }
    DeductIntentionVO deductIntentionVO = DeductIntentionVO.from(deductIntentionLogRecord);
    if (deductIntentionVO.deductIntentionStatus != DeductIntentionStatus.INIT) {
      return null;
    }
    RepaymentPlanLogRecord record = repaymentPlanLogModel.fetchByTransNo(transNo);
    if (Objects.isNull(record)) {
      repaymentPlanLogModel.insert(deductIntentionVO.id, transNo);
    }
    return deductIntentionVO;
  }


  /**
   * 最终版拆账规则：先判断非 EC 是否具备进场资格，再根据 EC 意愿和在贷状态决定金额最终落点。
   */
  public List<RepaySplitUnitVO> splitPaymentByPlan(
      String transId, Long userId, BigDecimal totalAmount, DeductIntentionVO deductIntentionVO) {
    if (Objects.isNull(totalAmount) || BigDecimalHelper.compareTo(totalAmount, BigDecimal.ZERO) <= 0) {
      return Collections.emptyList();
    }
    if (Objects.isNull(deductIntentionVO) || CollectionUtils.isEmpty(deductIntentionVO.deductPlan)) {
      return buildEcOnlyResult(transId, userId, totalAmount);
    }

    Map<UnionRepaymentType, BigDecimal> owedAmounts =
        deductIntentionLogService.calculateOwedAmounts(deductIntentionVO.deductPlan);
    List<NonEcAllocationItem> nonEcItems = buildNonEcItems(deductIntentionVO.deductPlan, owedAmounts);
    BigDecimal nonEcOwedAmount = sumRequestedAmounts(nonEcItems);

    // 只有在存在可分配的非 EC 输入项时，才允许非 EC 参与资金路由。
    if (!hasValidNonEcIntention(nonEcItems, nonEcOwedAmount)) {
      return buildEcOnlyResult(transId, userId, totalAmount);
    }

    boolean hasEcIntention = deductIntentionVO.deductPlan.stream()
        .anyMatch(plan -> plan.unionRepaymentType == UnionRepaymentType.EC_REPAY);
    if (hasEcIntention) {
      BigDecimal ecOwedAmount = safeAmount(owedAmounts.get(UnionRepaymentType.EC_REPAY));
      return splitWithEcIntention(
          transId, userId, totalAmount, nonEcItems, ecOwedAmount, nonEcOwedAmount);
    }
    return splitWithoutEcIntention(transId, userId, totalAmount, nonEcItems, nonEcOwedAmount);
  }

  /**
   * 最终规则只关心“可实际参与分账的非 EC 项”，因此这里先按业务类型聚合，再过滤无效输入。
   */
  private boolean hasValidNonEcIntention(List<NonEcAllocationItem> nonEcItems, BigDecimal nonEcOwedAmount) {
    return CollectionUtils.isNotEmpty(nonEcItems)
        && BigDecimalHelper.compareTo(nonEcOwedAmount, BigDecimal.ZERO) > 0;
  }

  /**
   * 把非 EC 计划按类型聚合成最小分账项，避免后续在 service 主流程里处理重复类型与脏数据。
   */
  private List<NonEcAllocationItem> buildNonEcItems(
      List<UnionRepaymentPlanVO> deductPlan, Map<UnionRepaymentType, BigDecimal> owedAmounts) {
    Map<UnionRepaymentType, List<Long>> businessIdsByType = new LinkedHashMap<>();
    for (UnionRepaymentPlanVO plan : deductPlan) {
      if (Objects.isNull(plan) || plan.unionRepaymentType == UnionRepaymentType.EC_REPAY) {
        continue;
      }
      businessIdsByType.computeIfAbsent(plan.unionRepaymentType, key -> new ArrayList<>())
          .addAll(Objects.isNull(plan.businessIds) ? Collections.emptyList() : plan.businessIds);
    }

    List<NonEcAllocationItem> items = new ArrayList<>();
    for (Map.Entry<UnionRepaymentType, List<Long>> entry : businessIdsByType.entrySet()) {
      BigDecimal owedAmount = safeAmount(owedAmounts.get(entry.getKey()));
      NonEcAllocationItem item = new NonEcAllocationItem()
          .setType(entry.getKey())
          .setBusinessIds(entry.getValue())
          .setOwedAmount(owedAmount)
          .setRequestedAmount(owedAmount);
      if (item.isValidForSplit()) {
        items.add(item);
      }
    }
    return items;
  }

  private BigDecimal sumRequestedAmounts(List<NonEcAllocationItem> nonEcItems) {
    return nonEcItems.stream()
        .map(NonEcAllocationItem::resolveRequestedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * 有 EC 意愿时，要先看本次支付里真正能留给 EC 的金额，叠加券+积分抵扣后是否足以覆盖 EC 应还；
   * 只有覆盖得住，才允许非 EC 进场。
   */
  private List<RepaySplitUnitVO> splitWithEcIntention(
      String transId,
      Long userId,
      BigDecimal totalAmount,
      List<NonEcAllocationItem> nonEcItems,
      BigDecimal ecOwedAmount,
      BigDecimal nonEcOwedAmount) {
    if (!canReleaseNonEcWithEcIntention(userId, totalAmount, ecOwedAmount, nonEcOwedAmount)) {
      return buildEcOnlyResult(transId, userId, totalAmount);
    }
    return allocateNonEcThenEc(nonEcItems, transId, userId, totalAmount, true);
  }

  /**
   * 无 EC 意愿但用户仍有 EC 在贷时，只允许“精确等额”进入非 EC；否则整笔资金都回落到 EC。
   */
  private List<RepaySplitUnitVO> splitWithoutEcIntention(
      String transId,
      Long userId,
      BigDecimal totalAmount,
      List<NonEcAllocationItem> nonEcItems,
      BigDecimal nonEcOwedAmount) {
    if (hasEcLoan(userId)) {
      if (BigDecimalHelper.compareTo(totalAmount, nonEcOwedAmount) == 0) {
        return allocateNonEcThenEc(nonEcItems, transId, userId, totalAmount, false);
      }
      return buildEcOnlyResult(transId, userId, totalAmount);
    }
    return allocateNonEcThenEc(nonEcItems, transId, userId, totalAmount, true);
  }

  /**
   * 最终规则不做比例分摊，只按非 EC 输入项顺序逐项扣减；需要兜底时再把剩余金额挂到 EC。
   */
  private List<RepaySplitUnitVO> allocateNonEcThenEc(
      List<NonEcAllocationItem> nonEcItems,
      String transId,
      Long userId,
      BigDecimal totalAmount,
      boolean appendEcRemainder) {
    List<RepaySplitUnitVO> units = new ArrayList<>();
    BigDecimal remainAmount = totalAmount;
    for (NonEcAllocationItem item : nonEcItems) {
      if (BigDecimalHelper.compareTo(remainAmount, BigDecimal.ZERO) <= 0) {
        break;
      }
      BigDecimal requestedAmount = item.resolveRequestedAmount();
      if (BigDecimalHelper.compareTo(requestedAmount, BigDecimal.ZERO) <= 0) {
        continue;
      }
      BigDecimal allocatedAmount = BigDecimalHelper.min(requestedAmount, remainAmount);
      if (BigDecimalHelper.compareTo(allocatedAmount, BigDecimal.ZERO) <= 0) {
        continue;
      }
      units.add(RepaySplitUnitVO.buildUnprocessedUnit(item.getType(), transId, allocatedAmount, userId));
      remainAmount = remainAmount.subtract(allocatedAmount);
    }
    if (appendEcRemainder && BigDecimalHelper.compareTo(remainAmount, BigDecimal.ZERO) > 0) {
      units.add(RepaySplitUnitVO.buildUnprocessedUnit(UnionRepaymentType.EC_REPAY, transId, remainAmount, userId));
    }
    return units;
  }

  /**
   * 券只影响 EC 的够扣阈值，不改变非 EC 的应还金额口径。
   */
  private BigDecimal calculateCouponDeductAmount(Long userId, BigDecimal ecOwedAmount) {
    if (BigDecimalHelper.compareTo(ecOwedAmount, BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    LoanAccountRecord loanAccountRecord = loanAccountModel.findByUserIdOrThrow(userId);
    LoanUserCouponVO couponVO = loanUserCouponService.getAtPresentPendingCoupon(
        null,
        loanAccountRecord.getId(),
        LoanCouponUsageType.MONEY_OFF);
    if (Objects.isNull(couponVO)) {
      return BigDecimal.ZERO;
    }
    return safeAmount(loanUserCouponService.getMoneyOffCouponExpectedDeductAmount(
        couponVO,
        ecOwedAmount,
        CouponCalcContext.CalcType.DEDUCT));
  }

  /**
   * 是否存在 EC 在贷只参与“无 EC 意愿”路径，用于决定非 EC 是否可以直接承接整笔金额。
   */
  private boolean hasEcLoan(Long userId) {
    LoanAccountRecord loanAccountRecord = loanAccountModel.findByUserIdOrThrow(userId);
    return ecOrderService.hasOrderByStatuses(loanAccountRecord.getId(), CashLoanOrderStatus.UNDONE_STATUSES);
  }

  /**
   * 够扣判断必须基于本次支付里“扣完非 EC 后还能分给 EC 的金额”；
   * 券+积分抖扣也只能按这部分实际可用于 EC 的金额计算，避免高估覆盖能力后提前放行非 EC。
   */
  private boolean canReleaseNonEcWithEcIntention(
      Long userId, BigDecimal totalAmount, BigDecimal ecOwedAmount, BigDecimal nonEcOwedAmount) {
    BigDecimal candidateEcRepayAmount = subtractFloorZero(totalAmount, nonEcOwedAmount);
    if (BigDecimalHelper.compareTo(candidateEcRepayAmount, BigDecimal.ZERO) <= 0) {
      return false;
    }
    BigDecimal couponCalcBase = BigDecimalHelper.min(candidateEcRepayAmount, ecOwedAmount);
    BigDecimal couponDeductAmount = calculateCouponDeductAmount(userId, couponCalcBase);
    return BigDecimalHelper.compareTo(
        candidateEcRepayAmount.add(couponDeductAmount),
        ecOwedAmount) >= 0;
  }

  /**
   * EC 是最终拆账规则中的统一兜底账户，不额外生成 0 金额单元。
   */
  private List<RepaySplitUnitVO> buildEcOnlyResult(String transId, Long userId, BigDecimal totalAmount) {
    return Collections.singletonList(
        RepaySplitUnitVO.buildUnprocessedUnit(UnionRepaymentType.EC_REPAY, transId, totalAmount, userId));
  }

  private BigDecimal safeAmount(BigDecimal amount) {
    return Objects.isNull(amount) ? BigDecimal.ZERO : amount;
  }

  private BigDecimal subtractFloorZero(BigDecimal amount, BigDecimal deductAmount) {
    BigDecimal result = safeAmount(amount).subtract(safeAmount(deductAmount));
    if (BigDecimalHelper.compareTo(result, BigDecimal.ZERO) < 0) {
      return BigDecimal.ZERO;
    }
    return result;
  }
  public void expireDeductIntentionForOverdueUser(Long userId) {
      if (!ecOrderService.isCurrentlyOverdueByUserId(userId)) {
        return;
      }
      transactionalModel.transaction(configuration -> {
        loanAccountModel.findByUserIdForUpdateOrThrow(userId);
        DeductIntentionLogRecord deductIntentionLogRecord = deductIntentionLogModel.fetchLatestDeductIntention(userId);
        if (Objects.isNull(deductIntentionLogRecord)) {
          return;
        }
        if (!Objects.equals(deductIntentionLogRecord.getDeductStatus(), DeductIntentionStatus.INIT.name())) {
          return;
        }
        if (!DeductIntentionVO.existJbpPlan(deductIntentionLogRecord.getDeductPlan())) {
          return;
        }
        log.info("expireDeductIntentionForOverdueUser userId:{}", userId);
        // 逾期用户的最新的还款意愿存在jbp还款计划
        deductIntentionLogModel.expireIntentionLog(deductIntentionLogRecord);
        List<RepaymentAnalysisRecord> records = repaymentAnalysisModel.getByDeductIntentionLogId(deductIntentionLogRecord.getId()).stream()
            .filter(item -> StringUtils.equals(item.getRepaymentAnalysisStatus(), RepaymentAnalysisStatus.INIT.code))
            .peek(item -> {
              item.setRepaymentAnalysisStatus(RepaymentAnalysisStatus.EXPIRED.code);
              item.setTimeUpdated(Clock.now());
            })
            .collect(Collectors.toList());
        repaymentAnalysisModel.batchUpdate(records);
      });
  }

  /**
   * 抵扣成功后，如果本次抵扣金额和缓存中金额相同，则删除缓存
   *
   * @param processResult
   * @param paymentVO
   */
  public void clearRepaymentAmountCacheOnProcessed(PaymentProcessResult processResult, PaymentVO paymentVO) {
    try {
      if (!repaymentConfig.getRepaymentDelAmountCacheSwitch()) {
        return;
      }
      if (processResult.processStatus != ProcessStatus.PROCESSED) {
        return;
      }
      LoanAccountRecord loanAccountRecord = loanAccountModel.findByUserId(paymentVO.userId);
      String key = loanAccountRecord.getId().toString();
      Long expected = paymentVO.amount.amount;
      // 缓存中的金额和抵扣的金额相同时才删除缓存
      boolean deleted = amountLoader.compareAndDel(key, expected);
      if (deleted) {
        log.info("clearRepaymentAmountCacheOnProcessed deleted cache for accountId:{}, amount:{}", key, expected);
      } else {
        log.info("clearRepaymentAmountCacheOnProcessed skip delete for accountId:{}, reason:value not match or key missing", key);
      }
    } catch (Exception e) {
      log.error("clearRepaymentAmountCacheOnProcessed exception", e);
    }
  }
}
