package com.yqg.core.service.cashloan.repay.repaystrategy;

import com.yqg.core.service.cashloan.instalmentcutcoupon.vo.InstalmentCutInterestCouponDeductDetailVO;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentVO;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.repay.RepaymentParam;
import com.yqg.core.service.loan.coupon.vos.LoanMoneyOffCouponVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.i18n.CurrencyAmount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CashLoanRepayContext {

  private String transNo;
  private UnionRepaymentVO unionRepaymentVO;
  private CurrencyAmount currencyAmount;
  private SDKType sdkType;
  private Long loanAccountId;
  private BigDecimal userRepayAmount;
  private Long transactionTime;
  private RepaymentParam repaymentParam;
  private Boolean canAutoDeduct;
  private List<CashLoanInstalmentVO> sortedInstalmentVOList = Collections.emptyList();
  private List<Long> sortedInstalmentIds = Collections.emptyList();
  private Map<Long, Integer> actualOverdueDaysMap = Collections.emptyMap();
  private Map<Long, InstalmentCutInterestCouponDeductDetailVO> cutInterestCouponDeductMap = Collections.emptyMap();
  private Map<Long, BigDecimal> delayFeeMap = Collections.emptyMap();
  private LoanMoneyOffCouponVO couponVO;

}
