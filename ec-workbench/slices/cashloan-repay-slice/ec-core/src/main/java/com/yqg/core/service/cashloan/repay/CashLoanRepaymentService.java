package com.yqg.core.service.cashloan.repay;

import com.yqg.core.aop.RunInTransaction;
import com.yqg.core.model.generated.tables.records.CashLoanRepaymentRecord;
import com.yqg.core.model.generated.tables.records.CashLoanRepaymentUnitRecord;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.loader.RepaymentAmountLoader;
import com.yqg.core.model.sql.cashloan.CashLoanRepaymentModel;
import com.yqg.core.model.sql.cashloan.CashLoanRepaymentUnitModel;
import com.yqg.core.model.sql.cashloan.condition.CashLoanRepaymentCondition;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentStatus;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentType;
import com.yqg.core.model.sql.enums.AvailabilityStatus;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.CashLoanRepayStrategyService;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repay.vo.CashLoanRepaymentVO;
import com.yqg.core.service.cashloan.repay.vo.DeductIntentionVO;
import com.yqg.core.service.cashloan.util.rate.CalcFeeUtil;
import com.yqg.core.service.cashloan.vo.*;
import com.yqg.core.service.cashloan.vo.repay.RepaymentParam;
import com.yqg.core.service.jbp.goldencard.JbpCardService;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vo.CouponCalcContext;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.PaymentBusinessNameMapper;
import com.yqg.ec.common.enums.LoanCouponUsageType;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.EcTimeZone;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: ListenYoung
 * @date: Created on 19:41 2019/8/8
 * @modified By:
 */
@Service
@Slf4j
public class CashLoanRepaymentService {
  @Autowired
  private LoanAccountModel accountModel;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private CashLoanRepaymentModel repaymentModel;
  @Autowired
  private CashLoanRepaymentUnitModel repaymentUnitModel;
  @Autowired
  private RepaymentAmountLoader amountLoader;
  @Autowired
  private LoanUserCouponService couponService;
  @Autowired
  private CashLoanRepayStrategyService cashLoanRepayStrategyService;
  @Autowired
  private DeductIntentionLogService deductIntentionLogService;
  @Autowired
  private JbpCardService jbpCardService;
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService instalmentCutInterestCouponDeductDetailService;

  public CashLoanRepaymentVO getRepaymentVOWithoutTransId(PaymentAccount paymentAccount, boolean deductCoupon, CashLoanOrderVO orderVO) {
    //获取欠款的订单和分期
    if (orderVO == null) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("网络异常，请稍后再试"));
    }
    CashLoanInstalmentVO instalmentVO = ecOrderService.getFirstOwedInstalmentVO(orderVO.id);
    if (instalmentVO == null) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("网络异常，请稍后再试"));
    }
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(orderVO.sdkType);
    CashLoanRepaymentVO repaymentVO = new CashLoanRepaymentVO();
    repaymentVO.userId = orderVO.userId;
    repaymentVO.amount = deductCoupon ? getCurrencyAmountAfterCouponDeduct(instalmentVO) : CalcFeeUtil.getOwedCurrencyAmount(instalmentVO);
    repaymentVO.orderId = orderVO.id;
    repaymentVO.businessName = businessName;
    repaymentVO.instalmentId = instalmentVO.id;
    repaymentVO.paymentAccount = paymentAccount;
    repaymentVO.expiredTime = Clock.getMaxMillisOfDay(Clock.now(), orderVO.sdkType.getTimeZone());
    return repaymentVO;
  }

  private CurrencyAmount getCurrencyAmountAfterCouponDeduct(CashLoanInstalmentVO instalmentVO) {
    BigDecimal repaymentAmount = CalcFeeUtil.getOwedAmount(instalmentVO);
    LoanUserCouponVO couponVO = couponService.getAtPresentPendingCoupon(instalmentVO.orderId, instalmentVO.loanAccountId, LoanCouponUsageType.MONEY_OFF);
    if (couponVO == null) {
      return CurrencyAmount.fromYuan(instalmentVO.currency, repaymentAmount);
    }

    EcAsserts.assertTrue(couponService.checkCouponAvailability(couponVO, repaymentAmount) == AvailabilityStatus.ENABLED,
        "Error while doing coupon deduct, illegal repayment amount. order id: {}, amount: {}", instalmentVO.orderId, repaymentAmount);

    BigDecimal deductAmount = couponService.getMoneyOffCouponExpectedDeductAmount(couponVO, repaymentAmount, CouponCalcContext.CalcType.DEDUCT);

    repaymentAmount = repaymentAmount.subtract(deductAmount);
    EcAsserts.assertTrue(repaymentAmount.compareTo(BigDecimal.ZERO) >= 0,
        "Error while doing coupon deduct, repayment amount after coupon <= 0. order id: {}, amount: {}", instalmentVO.orderId, repaymentAmount);

    return CurrencyAmount.fromYuan(instalmentVO.currency, repaymentAmount);
  }

  public Long getOwnedAmount(Long userId) {
    LoanAccountRecord account = accountModel.findByUserIdOrThrow(userId);

    // 优先查询还款策略表中的金额
    BigDecimal strategyAmount = cashLoanRepayStrategyService.calValidOwedAmountAmountByAccountId(account.getId());
    if (strategyAmount.compareTo(BigDecimal.ZERO) > 0) {
      CashLoanOrderVO orderVO = ecOrderService.getEarliestDueOrder(account.getId());
      if (orderVO == null) {
        log.warn("{} 策略金额>0但没有待还order", userId);
        return CurrencyAmount.fromYuan(EcCurrency.IDR, BigDecimal.ZERO).roundUpToYuan();
      }
      return CurrencyAmount.fromYuan(orderVO.currency, strategyAmount).roundUpToYuan();
    }

    //如果在缓存中有该用户的还款金额缓存，优先使用缓存中的金额进行还款
    Long amount = amountLoader.getOrDefault(account.getId().toString(), null);
    if (amount != null) {
      return new CurrencyAmount(EcCurrency.IDR, amount).roundUpToYuan();
    }

    DeductIntentionVO latestDeductIntention = deductIntentionLogService.findLatestDeductIntention(userId);
    if (DeductIntentionVO.existInitPlan(latestDeductIntention)) {
      return doCalcDeductIntentionAmount(latestDeductIntention);
    }

    CashLoanOrderVO orderVO = ecOrderService.getEarliestDueOrder(account.getId());

    // 没有还款意愿且没有待还订单或有正在处理的还款
    if (orderVO == null || hasProcessingRepayment(orderVO.id)) {
      log.warn("{} 没有待还order或有正在处理的还款", userId);
      return 0L;
    }
    CashLoanInstalmentVO instalmentVO = instalmentCutInterestCouponDeductDetailService.getViewInstalment(ecOrderService.getFirstOwedInstalmentVO(orderVO.id));
    return instalmentVO == null ? 0L : CalcFeeUtil.getOwedCurrencyAmount(instalmentVO).roundUpToYuan();
  }

  private Long doCalcDeductIntentionAmount(DeductIntentionVO latestDeductIntention) {
    Map<UnionRepaymentType, BigDecimal> unionRepaymentTypeBigDecimalMap = deductIntentionLogService.calculateOwedAmounts(
        latestDeductIntention.deductPlan);
    return CurrencyAmount.fromYuan(EcCurrency.IDR, unionRepaymentTypeBigDecimalMap.values()
            .stream().mapToLong(BigDecimal::longValue)
            .sum()).
        roundUpToYuan();
  }

  public Boolean hasProcessingRepayment(Long orderId) {
    return CollectionUtils.isNotEmpty(repaymentModel.findByOrderIdAndStatuses(orderId, CashLoanRepaymentStatus.INIT));
  }

  public Boolean hasProcessingInstalmentRepayment(Collection<Long> instalmentIds) {
    return CollectionUtils.isNotEmpty(repaymentModel.findByInstalmentIdsAndStatuses(instalmentIds, CashLoanRepaymentStatus.INIT));
  }

  public Map<String, RepaymentVO> findAllOverFlowRepaymentVOMap(Long accountId) {
    return repaymentModel.findAllOverFlowRepaymentRecord(accountId)
        .stream()
        .collect(Collectors.toMap(CashLoanRepaymentRecord::getPaymentTransId, RepaymentVO::from));
  }

  public Long findLastedRepayOrderIdWithRepaymentId(Long repaymentId) {
    return repaymentUnitModel.findLastedOrderIdOrThrow(repaymentId);
  }

  public RepaymentParam getRepaymentParam(Long accountId) {
    List<CashLoanInstalmentVO> waitRepaymentInstalmentVOS = ecOrderService.getAllSortedWaitRepaymentInstalmentVOSForLoanAccount(accountId);
    return RepaymentParam.fromSortedWaitRepaymentInstalmentVOS(waitRepaymentInstalmentVOS);
  }


  public RepaymentVO getRepaymentVOByIdOrThrow(Long id) {
    return RepaymentVO.from(repaymentModel.findByIdOrThrow(id));
  }

  public RepaymentVO getRepaymentVOByPaymentTransIdOrThrow(String paymentTransId) {
    return RepaymentVO.from(repaymentModel.findByPaymentTransIdOrThrow(paymentTransId));
  }

  public List<RepaymentUnitAmountVO> fetchByOrderIdAndLeTimeCreated(Long orderId, Long timeCreated) {
    List<CashLoanRepaymentUnitRecord> repaymentUnitRecords = repaymentUnitModel.findByOrderIdsAndLeTimeCreated(Collections.singletonList(orderId), timeCreated);
    return repaymentUnitRecords
        .stream()
        .map(RepaymentUnitAmountVO::from)
        .collect(Collectors.toList());
  }

  //获取订单某一时刻之前的RepaidPrincipal
  public BigDecimal findRepaidPrincipalByTimeCreated(Long orderId, Long endTime) {
    List<RepaymentUnitAmountVO> repaymentUnitAmountVOList = fetchByOrderIdAndLeTimeCreated(orderId, endTime);
    return repaymentUnitAmountVOList
        .stream()
        .map(o -> o.principal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public RepaymentVO getLatestRepaymentVOByOrderIdAndType(long orderId, CashLoanRepaymentType type) {
    List<CashLoanRepaymentRecord> repaymentRecords = repaymentModel.fetch(orderId, type);
    if (CollectionUtils.isEmpty(repaymentRecords)) {
      return null;
    }
    return repaymentRecords
        .stream()
        .map(RepaymentVO::from)
        .filter(o -> o.status == CashLoanRepaymentStatus.SUCCEED)
        .max(Comparator.comparing(RepaymentVO::getId))
        .orElse(null);
  }

  public Map<Long, List<RepaymentVO>> getOrderIdRepaymentMap(List<Long> orderIds, CashLoanRepaymentType type) {
    List<CashLoanRepaymentUnitRecord> cashLoanRepaymentUnitRecords = repaymentUnitModel.findByOrderIds(orderIds);
    List<Long> repaymentIds = cashLoanRepaymentUnitRecords.stream().map(CashLoanRepaymentUnitRecord::getRepaymentId).collect(Collectors.toList());
    Map<Long, Set<Long>> orderIdRepaymentIdMap = cashLoanRepaymentUnitRecords.stream()
        .collect(Collectors.groupingBy(CashLoanRepaymentUnitRecord::getOrderId,
            Collectors.mapping(CashLoanRepaymentUnitRecord::getRepaymentId, Collectors.toSet())));
    Map<Long, CashLoanRepaymentRecord> repaymentRecordMap = repaymentModel.fetchMapByIdsAndType(repaymentIds, type);
    Map<Long, List<RepaymentVO>> orderIdRepaymentMap = new HashMap<>();
    for (Map.Entry<Long, Set<Long>> entry : orderIdRepaymentIdMap.entrySet()) {
      Long orderId = entry.getKey();
      List<RepaymentVO> repaymentVOS = entry.getValue().stream().map(repaymentRecordMap::get).filter(Objects::nonNull).map(RepaymentVO::from).collect(Collectors.toList());
      orderIdRepaymentMap.put(orderId, repaymentVOS);
    }
    return orderIdRepaymentMap;
  }

  public List<RepaymentVO> fetchByIds(List<Long> ids) {
    return repaymentModel.fetchByIds(ids)
        .stream()
        .map(RepaymentVO::from)
        .collect(Collectors.toList());
  }

  public List<RepaymentUnitVO> getRepaymentUnitVOListByRepaymentId(Long repaymentId) {
    List<CashLoanRepaymentUnitRecord> cashLoanRepaymentUnitRecords = repaymentUnitModel.findByRepaymentId(repaymentId);
    return cashLoanRepaymentUnitRecords.stream().map(RepaymentUnitVO::from).collect(Collectors.toList());
  }

  public BigDecimal getRepayAmountByOrderIdAndGtTimeCreated(Long orderId, Long timeCreated) {
    List<CashLoanRepaymentUnitRecord> repaymentUnitRecords = repaymentUnitModel.findByOrderIdsAndGtTimeCreated(Collections.singletonList(orderId), timeCreated);
    return repaymentUnitRecords.stream().map(CashLoanRepaymentUnitRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public List<RepaymentUnitVO> getRepaymentUnitByOrderIds(List<Long> orderIds) {
    List<CashLoanRepaymentUnitRecord> repaymentUnitRecords = repaymentUnitModel.findByOrderIds(orderIds);
    return repaymentUnitRecords.stream().map(RepaymentUnitVO::from).collect(Collectors.toList());
  }

  public List<RepaymentVO> getByCondition(CashLoanRepaymentCondition condition) {
    List<CashLoanRepaymentRecord> repaymentRecords = repaymentModel.getByCondition(condition);
    return repaymentRecords.stream().map(RepaymentVO::fromInternal).collect(Collectors.toList());
  }

  public List<RepaymentUnitVO> getByOrderIdsAndStatuses(List<Long> orderIds, List<String> statuses) {
    List<CashLoanRepaymentStatus> statusList = statuses.stream().map(CashLoanRepaymentStatus::fromCode).collect(Collectors.toList());
    return repaymentUnitModel.getRepaymentUnitVOsByOrderIdsAndStatus(orderIds, statusList);
  }

  public List<RepaymentVO> getRepaymentVOByOrderIdsAndStatuses(List<Long> orderIds, List<String> statuses) {
    List<CashLoanRepaymentStatus> statusList = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(statuses)) {
      statusList = statuses.stream().map(CashLoanRepaymentStatus::fromCode).collect(Collectors.toList());
    }
    return repaymentModel.findByOrderIdsAndStatuses(orderIds, statusList).stream().map(RepaymentVO::from).collect(Collectors.toList());
  }

  public List<CashRepaymentOrderVO> getRepaymentUnitVOByTypes(List<Long> orderIds, List<CashLoanRepaymentType> types) {
    return repaymentModel.fetch(orderIds, types);
  }

  public boolean existRepaySuccessToday(Long userId) {
    return repaymentModel.existRepaySuccessByTime(userId, Clock.getMinMillisOfDay(Clock.now(), EcTimeZone.JAKARTA.tz)) > 0;
  }

  public RepaymentVO getLastSucceedRepaymentOrderAfterTime(Long userId, Long startTime) {
    return Optional.ofNullable(repaymentModel.getLastSucceedRepaymentOrderAfterTime(userId, startTime))
        .map(RepaymentVO::fromInternal)
        .orElse(null);
  }

  public int countSuccessRepaymentByUserId(Long userId) {
    return repaymentModel.countRepaySuccess(userId);
  }
}
