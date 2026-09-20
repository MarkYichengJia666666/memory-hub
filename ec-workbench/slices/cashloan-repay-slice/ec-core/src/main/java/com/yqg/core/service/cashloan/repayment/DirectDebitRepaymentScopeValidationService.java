package com.yqg.core.service.cashloan.repayment;

import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.model.sql.loan.coupon.enums.LoanCouponStatus;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.secure.check.context.SecureCheckUserContext;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 与 {@code /api/v3/cashloan/getRepaymentAccountByChannel} 中 EC/JBP 分支等价的前置校验（不生成 VA），供直连代扣编排复用。
 */
@Service
public class DirectDebitRepaymentScopeValidationService {

  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService instalmentCutInterestCouponDeductDetailService;

  public void validateEcInstalmentsAndCoupon(SecureCheckUserContext userContext, Long loanAccountId, List<String> encodeInstalmentIds,
      Long couponId) {
    List<Long> instalmentIds = encodeInstalmentIds.stream().map(YqgHashids::decode).collect(Collectors.toList());
    List<CashLoanInstalmentVO> instalmentVOs = ecOrderService.getInstalmentVOs(instalmentIds);

    instalmentVOs.forEach(instalmentVO -> EcAsserts.assertTrue(instalmentVO.userId.equals(userContext.userId),
        "instalmentId: {},instalment userId : {}, apply userId : {}", instalmentVO.id, instalmentVO.userId, userContext.userId));

    if (instalmentVOs.stream().anyMatch(instalmentVO -> instalmentVO.status != CashLoanInstalmentStatus.INIT)) {
      throw EcException.warn(EcExceptionType.CASH_LOAN_INSTALMENT_STATUS_CHANGED, TT.gen("您的未还账单已变更，请重新选择账单进行还款。"));
    }

    if (couponId != null) {
      LoanUserCouponVO loanUserCouponVO = loanUserCouponService.findByIdWithConfig(couponId);
      loanUserCouponService.checkSelfCoupon(loanUserCouponVO, loanAccountId);
      if (loanUserCouponVO.status != LoanCouponStatus.PENDING) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("优惠券不满足使用条件，请重新选择"),
            "coupon status is not PENDING for direct debit repayment scope,couponId :{}, coupon status:{}", couponId,
            loanUserCouponVO.status);
      }
    } else {
      loanUserCouponService.unbindAllMoneyOffCoupon(loanAccountId);
    }

    CashLoanInstalmentVO earliest = instalmentVOs.stream()
        .min(Comparator.comparing(CashLoanInstalmentVO::getBillingDate).thenComparing(CashLoanInstalmentVO::getTimeCreated)).orElse(null);
    Objects.requireNonNull(earliest);
    instalmentCutInterestCouponDeductDetailService.getViewInstalment(earliest);
  }
}
