package com.miyou.controllers.cashloan.repayment.card;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.RepaymentConfig;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserCashLoanOrderContext;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.UserProductDetailVO;
import com.yqg.core.service.cashloan.repay.enums.RepaymentReminderStrategy;
import com.yqg.core.service.cashloan.util.rate.CalcFeeUtil;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.loan.manualreduction.ManualReductionTaskService;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionCheckResult;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionDetail;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import com.yqg.core.service.loan.repayment.billpage.BillPageInstalmentItem;
import com.yqg.core.service.loan.repayment.billpage.BillPageInstalmentItemCalculator;
import com.yqg.core.service.loan.repayment.experiment.RepaymentExperimentSupport;
import com.yqg.core.service.loan.repayment.status.RepaymentDisplayStatus;
import com.yqg.core.service.loan.repayment.status.RepaymentStatusSupport;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;


@Component
public class RepaymentContextFactory {

  @Autowired
  private RepaymentConfig repaymentConfig;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private ManualReductionTaskService manualReductionTaskService;
  @Autowired
  private RepaymentStatusSupport repaymentStatusSupport;
  @Autowired
  private RepaymentExperimentSupport repaymentExperimentSupport;


  public RepaymentContext buildRepaymentContext(HomePageContext ctx) {
    HomepageUserParamsVO params = ctx.getHomepageUserParamsVO();
    UserCashLoanOrderContext userCashLoanOrderContext = ctx.getUserCashLoanOrderContext();
    List<CashLoanInstalmentVO> instalmentList = userCashLoanOrderContext.getSortedInstalmentForViewVOs();
    if (CollectionUtils.isEmpty(instalmentList)) {
      return RepaymentContext.noOutstandingContext();
    }
    LoanAccountVO loanAccount = params.accountVO;
    TimeZone timeZone = loanAccount.sdkType.getTimeZone();
    long now = Clock.now();
    long recentlyBillingDate = instalmentList.get(0).billingDate;
    RepaymentDisplayStatus repaymentDisplayStatus = repaymentStatusSupport.calculateRepaymentStatus(ctx.getUserId());
    ManualReductionCheckResult manualReductionCheckResult = manualReductionTaskService.checkManualReduction(loanAccount.userId, loanAccount.id, ctx.getUserDeviceContextVO().getBuild());
    RepaymentReminderStrategy repaymentReminderStrategy = repaymentExperimentSupport.getRepaymentReminderStrategy(loanAccount.userId, ctx.getUserDeviceContextVO().getBuild());

    BigDecimal totalUnpaidAmount = calculateTotalUnpaidAmount(manualReductionCheckResult, instalmentList);
    BigDecimal recentUnpaidAmount = calculateRecentUnpaidAmount(manualReductionCheckResult, repaymentDisplayStatus, instalmentList, repaymentReminderStrategy, now, timeZone);
    TT repaymentTip = getRepaymentTip(params.isOverdue(), recentlyBillingDate, now, timeZone);
    String recentlyBillingDateStr = Clock.dateTimeStringFromTimestamp(recentlyBillingDate, DateFormatter.dd__MM__yyyy, timeZone);
    UserProductDetailVO userProductVO = ctx.getUserProductVO();
    Boolean secondRiskRejectedDisplay = ctx.getPrepareAbTestVO().secondRiskRejectDisplayStrategy.isStrategyB();

    return RepaymentContext.builder()
        .homepageUserParam(params)
        .homePageContext(ctx)
        .manualReductionCheckResult(manualReductionCheckResult)
        .repaymentDisplayStatus(repaymentDisplayStatus)
        .totalUnpaidAmount(totalUnpaidAmount)
        .recentlyBillingDate(recentlyBillingDate)
        .recentUnpaidAmount(recentUnpaidAmount)
        .recentlyBillingDateStr(recentlyBillingDateStr)
        .repaymentTip(repaymentTip)
        .remainCredits(Objects.nonNull(userProductVO) ? userProductVO.getEnableVirtualCredits() : BigDecimal.ZERO)
        .secondRiskRejectedDisplay(secondRiskRejectedDisplay)
        .repaymentReminderStrategy(repaymentReminderStrategy)
        .build();
  }

  /**
   * 计算待还账单总金额
   */
  private BigDecimal calculateTotalUnpaidAmount(ManualReductionCheckResult checkResult, List<CashLoanInstalmentVO> instalmentList) {
    return instalmentList.stream()
        .map(instalment -> getOwedAmountForOneInstalment(checkResult, instalment))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * 计算还款卡片（主卡片）最近待还金额
   */
  private BigDecimal calculateRecentUnpaidAmount(
      ManualReductionCheckResult checkResult,
      RepaymentDisplayStatus status,
      List<CashLoanInstalmentVO> instalmentList,
      RepaymentReminderStrategy repaymentReminderStrategy,
      long now,
      TimeZone timeZone
  ) {
    BigDecimal totalOverdueAmount = BigDecimal.ZERO;        // 已逾期账单总金额
    BigDecimal overdueAndUpcomingAmount = BigDecimal.ZERO;  // 已逾期账单金额 与 未来几天到期账单金额 之和
    BigDecimal todayDueAmount = BigDecimal.ZERO;            // 当日到期的待还账单金额之和
    int dayThreshold = homepageV5Config.getBillingOrderDayThreshold();
    for (CashLoanInstalmentVO instalment : instalmentList) {
      BigDecimal owedAmount = getOwedAmountForOneInstalment(checkResult, instalment);
      if (Clock.getDaysBetween(instalment.billingDate, now + dayThreshold * Clock.MILLS_PER_DAY, timeZone) >= 0) {
        overdueAndUpcomingAmount = overdueAndUpcomingAmount.add(owedAmount);
      }
      if (now > instalment.billingDate) {
        totalOverdueAmount = totalOverdueAmount.add(owedAmount);
      }
      if (Clock.isToday(instalment.billingDate, timeZone)) {
        todayDueAmount = todayDueAmount.add(owedAmount);
      }
    }
    BigDecimal recentUnpaidAmount;
    switch (status) {
      case NO_UNPAID_INSTALMENT_IN_X_DAYS:
        recentUnpaidAmount = repaymentReminderStrategy.isC()
            ? getOwedAmountForDueDate(checkResult, instalmentList, instalmentList.get(0).billingDate, timeZone)
            : getOwedAmountForOneInstalment(checkResult, instalmentList.get(0));
        break;
      case OVERDUE_ONE_DAY:
        recentUnpaidAmount = getOwedAmountForDueDate(checkResult, instalmentList, now, timeZone);
        break;
      case HAS_UNPAID_INSTALMENT_IN_X_DAYS:
        recentUnpaidAmount = overdueAndUpcomingAmount;
        break;
      case HAS_UNPAID_INSTALMENT_WITHIN_24H:
        recentUnpaidAmount = todayDueAmount;
        break;
      case OVERDUE_WITHIN_X_DAYS:
      case OVERDUE_BEYOND_X_DAYS:
        recentUnpaidAmount = totalOverdueAmount;
        break;
      default:
        recentUnpaidAmount = BigDecimal.ZERO;
    }
    return recentUnpaidAmount;
  }

  /**
   * 在给定的账单列表中，挑选出在 dueDate 到期的账单，累计这些账单的待还金额。
   * <p>
   * 催收减免账单使用减免后的金额进行计算。
   */
  private BigDecimal getOwedAmountForDueDate(ManualReductionCheckResult checkResult, List<CashLoanInstalmentVO> instalmentList, long dueDate, TimeZone timeZone) {
    return instalmentList.stream()
        .filter(instalment -> 0 == Clock.getDaysBetween(dueDate, instalment.billingDate, timeZone))
        .map(instalment -> getOwedAmountForOneInstalment(checkResult, instalment))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }


  /**
   * 计算给定账单的待还金额
   * <p>
   * 催收减免账单要展示减免后的金额
   */
  private BigDecimal getOwedAmountForOneInstalment(ManualReductionCheckResult checkResult, CashLoanInstalmentVO instalment) {
    if (checkResult.billPageDisplayStrategy == BillPageDisplayStrategyKey.COLLECTION_REDUCTION_STRATEGY) {
      ManualReductionDetail.InstalmentReductionDetail collectionReductionDetail = checkResult.validInstalmentManualReduction.get(instalment.id);
      BillPageInstalmentItem owedAmount = BillPageInstalmentItemCalculator.getOwedAmount(instalment, collectionReductionDetail);
      return owedAmount.currentAmount;
    }
    return CalcFeeUtil.getOwedAmount(instalment);
  }

  private TT getRepaymentTip(boolean isOverdue, Long recentlyBillingDate, Long now, TimeZone timeZone) {
    int diffDay = Clock.getAbsCalenderDaysBetween(recentlyBillingDate, now, timeZone);
    if (isOverdue) {
      return TT.gen("逾期{0}天", diffDay);
    } else if (diffDay == 0) {
      return TT.gen("不足{0}天", 1);
    } else {
      return TT.gen("剩余{0}天", diffDay);
    }
  }
}
