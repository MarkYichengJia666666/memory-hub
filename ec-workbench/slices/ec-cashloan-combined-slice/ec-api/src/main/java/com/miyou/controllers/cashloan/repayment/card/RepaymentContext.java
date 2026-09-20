package com.miyou.controllers.cashloan.repayment.card;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.repay.enums.RepaymentReminderStrategy;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionCheckResult;
import com.yqg.core.service.loan.repayment.status.RepaymentDisplayStatus;
import com.yqg.translation.client.utils.TT;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
public class RepaymentContext {
  public final HomepageUserParamsVO homepageUserParam;
  public final HomePageContext homePageContext;
  public final Long recentlyBillingDate;
  public final BigDecimal totalUnpaidAmount;
  public final BigDecimal recentUnpaidAmount;
  public final String recentlyBillingDateStr;
  public final TT repaymentTip;
  public final RepaymentDisplayStatus repaymentDisplayStatus;
  public final BigDecimal remainCredits;
  public final Boolean secondRiskRejectedDisplay;
  public final RepaymentReminderStrategy repaymentReminderStrategy;
  public final ManualReductionCheckResult manualReductionCheckResult;

  public static RepaymentContext noOutstandingContext() {
    return RepaymentContext.builder()
        .repaymentDisplayStatus(RepaymentDisplayStatus.NO_OUTSTANDING)
        .build();
  }

  public boolean hasOutstanding() {
    return this.repaymentDisplayStatus != RepaymentDisplayStatus.NO_OUTSTANDING;
  }
}
