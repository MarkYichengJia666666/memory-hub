package com.miyou.controllers.cashloan.repayment.billpage.strategy.impl;

import com.miyou.controllers.cashloan.repayment.billpage.strategy.BillPageContext;
import com.yqg.core.service.loan.repayment.billpage.BillPageInstalmentItemCalculator;
import com.miyou.controllers.cashloan.repayment.billpage.strategy.infra.AbstractBillPageDisplayStrategy;
import com.miyou.controllers.cashloan.response.instalment.InstalmentResponse;
import com.miyou.controllers.cashloan.response.instalment.details.CollectionReductionGuideResponse;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.service.cashloan.RepaymentConfig;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionTaskVO;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.miyou.controllers.cashloan.enums.InstalmentDisplayStatus.PAID;

@Slf4j
@Component
public class CollectionReductionBillPageDisplayStrategy extends AbstractBillPageDisplayStrategy {
  @Override
  public BillPageDisplayStrategyKey getKey() {
    return BillPageDisplayStrategyKey.COLLECTION_REDUCTION_STRATEGY;
  }

  @Override
  protected void postBuildInstalmentResponse(InstalmentResponse instalmentResponse, BillPageContext billPageContext, CashLoanInstalmentVO instalment) {
    // 1. 催收减免策略下，都不展示【还款日】文案，只保留 dd/MM/yyyy 还款日期
    instalmentResponse.billingDateDesc = TT.gen("{0}", BillPageInstalmentItemCalculator.getFormattedRepaymentDueDate(instalment, billPageContext.billPageParam.sdkType));

    // 2. 催收减免策略下，对于有催收减免计划的账单，展示【已享减免】标签。
    billPageContext.getCollectionReductionDetailByInstalmentId(instalment.id)
        .filter(rd -> BigDecimalHelper.compareTo(rd.getReductionAmount(), BigDecimal.ZERO) > 0)
        .ifPresent(rd -> instalmentResponse.collectionReductionTip = TT.gen("已享减免"));
  }

  @Override
  protected CollectionReductionGuideResponse buildCollectionReductionGuide(BillPageContext ctx) {
    if (ctx.billPageParam.instalmentDisplayStatus == PAID) {
      return null;
    }
    ManualReductionTaskVO reductionTask = ctx.billPageParam.manualReductionCheckResult.validManualReductionTask;
    BigDecimal validReductionAmount = ctx.billPageParam.manualReductionCheckResult.getValidCollectionReductionAmount();
    long expiredTime = reductionTask.getReductionDetail().getExpiredTime();
    long leftMillis = Clock.getMilliSecondsBetween(Clock.now(), expiredTime);
    long leftDays = leftMillis / Clock.MILLS_PER_DAY;
    return CollectionReductionGuideResponse.builder()
        .title(TT.gen("限时减免"))
        .subTitle(TT.gen("总还款金额立减 {0}", AmountFormatter.format(ctx.billPageParam.sdkType.getCurrency(), validReductionAmount)))
        .iconUrl(repaymentConfig.getRepaymentCardReminderIconUrl().get(RepaymentConfig.RepaymentCardReminderIconType.CLOCK))
        .countDown(leftDays == 0)
        .countDownMillis(leftDays == 0 ? leftMillis : null)
        .countDownTip(leftDays == 0 ? null : TT.gen("限免：{0}天 ", leftDays))
        .prompt(TT.gen("利息和逾期利息已享减免"))
        .build();
  }


}
