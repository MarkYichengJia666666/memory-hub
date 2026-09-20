package com.miyou.controllers.cashloan.repayment.billpage.details;

import com.yqg.core.service.cashloan.RepaymentConfig;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionCheckResult;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionDetail;
import com.yqg.core.service.loan.repayment.billpage.BillPageInstalmentItem;
import com.yqg.core.service.loan.repayment.billpage.BillPageInstalmentItemCalculator;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.ec.common.i18n.AmountFormatter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

import static com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey.COLLECTION_REDUCTION_STRATEGY;

@Builder(buildMethodName = "create")
@AllArgsConstructor
public class BillPageDetailsContext {
  public final Long userId;
  public final Long loanAccountId;
  public final Long build;
  public final Long instalmentId;
  public final CashLoanInstalmentVO loanInstalment;
  public final CashLoanOrderVO loanOrder;
  public final LoanProductConfigVO loanProduct;
  public final Map<Long /* instalmentId */, BigDecimal /* cutInterestAmount */> cutInterestMap;
  public final Map<RepaymentConfig.RepaymentDetailsFeeItemType, String /* logoUrl */> feeItemLogo;
  public final ManualReductionCheckResult checkResult;
  public final HomeDisplayStrategy complianceStrategy;

  /**
   * 借款本金（订单）
   */
  public String getFormattedOrderPrincipal() {
    return AmountFormatter.format(loanOrder.currency, getOrderPrincipal());
  }

  /**
   * 借款本金（订单）
   */
  public BigDecimal getOrderPrincipal() {
    return loanOrder.principal;
  }

  /**
   * 借款本金
   */
  public String getFormattedPrincipal() {
    return AmountFormatter.format(loanOrder.currency, getPrincipal());
  }

  /**
   * 借款本金
   */
  public BigDecimal getPrincipal() {
    if (COLLECTION_REDUCTION_STRATEGY == checkResult.billPageDisplayStrategy) {
      BillPageInstalmentItem principalItem = BillPageInstalmentItemCalculator.getPrincipal(loanInstalment, complianceStrategy, checkResult.validInstalmentManualReduction.get(loanInstalment.id));
      return principalItem.currentAmount;
    }
    return loanInstalment.principal;
  }

  /**
   * 综合息费
   */
  public String getFormattedPostInterest() {
    return AmountFormatter.format(loanOrder.currency, getPostInterest());
  }

  /**
   * 综合息费
   */
  public BigDecimal getPostInterest() {
    ManualReductionDetail.InstalmentReductionDetail collectionReductionDetail = pickInstalmentCollectionReductionDetail();
    BigDecimal cutInterestAmount = cutInterestMap.getOrDefault(loanInstalment.id, BigDecimal.ZERO);
    BillPageInstalmentItem postInterestItem = BillPageInstalmentItemCalculator.getPostInterest(loanInstalment, complianceStrategy, cutInterestAmount, collectionReductionDetail);
    return postInterestItem.currentAmount;
  }

  /**
   * 逾期利息
   */
  public String getFormattedOverdueInterest() {
    return AmountFormatter.format(loanOrder.currency, getOverdueInterest());
  }


  /**
   * 逾期利息
   */
  public BigDecimal getOverdueInterest() {
    ManualReductionDetail.InstalmentReductionDetail collectionReductionDetail = pickInstalmentCollectionReductionDetail();
    BillPageInstalmentItem overdueInterestItem = BillPageInstalmentItemCalculator.getOverdueInterest(loanInstalment, collectionReductionDetail);
    return overdueInterestItem.currentAmount;
  }

  /**
   * 催收费
   */
  public String getFormattedPenaltyInterest() {
    return AmountFormatter.format(loanOrder.currency, getPenaltyInterest());
  }

  /**
   * 催收费
   */
  public BigDecimal getPenaltyInterest() {
    ManualReductionDetail.InstalmentReductionDetail collectionReductionDetail = pickInstalmentCollectionReductionDetail();
    BillPageInstalmentItem penaltyInterestItem = BillPageInstalmentItemCalculator.getPenalty(loanInstalment, collectionReductionDetail);
    return penaltyInterestItem.currentAmount;
  }

  /**
   * 总还款金额
   */
  public String getFormattedTotalRepayAmount() {
    return AmountFormatter.format(loanOrder.currency, getTotalRepayAmount());
  }

  /**
   * 总还款金额
   */
  public BigDecimal getTotalRepayAmount() {
    ManualReductionDetail.InstalmentReductionDetail collectionReductionDetail = pickInstalmentCollectionReductionDetail();
    BigDecimal cutInterestAmount = cutInterestMap.getOrDefault(loanInstalment.id, BigDecimal.ZERO);
    BillPageInstalmentItem penaltyInterestItem = BillPageInstalmentItemCalculator.getTotalAmount(loanInstalment, cutInterestAmount, collectionReductionDetail);
    return penaltyInterestItem.currentAmount;
  }

  public Integer getTerms() {
    return loanProduct.getTerms();
  }

  private ManualReductionDetail.InstalmentReductionDetail pickInstalmentCollectionReductionDetail() {
    return COLLECTION_REDUCTION_STRATEGY == checkResult.billPageDisplayStrategy
        ? checkResult.validInstalmentManualReduction.get(loanInstalment.id) : null;
  }

}
