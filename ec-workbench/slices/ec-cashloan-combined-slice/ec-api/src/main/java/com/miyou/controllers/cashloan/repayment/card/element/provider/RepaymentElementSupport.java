package com.miyou.controllers.cashloan.repayment.card.element.provider;

import com.miyou.controllers.cashloan.repayment.card.RepaymentContext;
import com.miyou.controllers.cashloan.repayment.card.element.param.RepaymentTextElementParam;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.PopUpAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.CompositeElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.CountDownTimeElement;
import com.yqg.core.service.cashloan.RepaymentConfig;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionCheckResult;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionTaskVO;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.RepaymentCardElementId.*;
import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.RepaymentCardElementId.REPAYMENT_COLLECTION_REDUCTION_GUIDE;

@Component
public class RepaymentElementSupport {

  @Autowired
  private RepaymentConfig repaymentConfig;

  public Optional<IElement> tryBuildCollectionReductionGuide(RepaymentContext ctx) {
    LoanAccountVO loanAccount = ctx.homepageUserParam.accountVO;
    ManualReductionCheckResult manualReductionCheckResult = ctx.manualReductionCheckResult;
    if (BillPageDisplayStrategyKey.COLLECTION_REDUCTION_STRATEGY != manualReductionCheckResult.billPageDisplayStrategy) {
      return Optional.empty();
    }

    IElement title = ElementBuilder.text()
        .id(COLLECTION_REDUCTION_TITLE)
        .text(TT.gen("限时减免"))
        .elementParam(RepaymentTextElementParam.builder()
            .iconUrl(repaymentConfig.getRepaymentCardReminderIconUrl().get(RepaymentConfig.RepaymentCardReminderIconType.CLOCK))
            .build())
        .build();

    IElement subTitle = ElementBuilder.text()
        .id(COLLECTION_REDUCTION_SUBTITLE)
        .text(TT.gen("总还款金额立减 {0}", AmountFormatter.format(loanAccount.sdkType.getCurrency(), manualReductionCheckResult.getValidCollectionReductionAmount())))
        .build();

    IElement reductionIcon = ElementBuilder.button()
        .id(COLLECTION_REDUCTION_ICON)
        .action(new PopUpAction(PopUpAction.PopUpParam.builder()
            .title(TT.gen("温馨提示"))
            .content(TT.gen("具体减免的情况，以账单展示为准"))
            .buttonContent(TT.gen("我知道了"))
            .buttonUrl(null)
            .build()))
        .build();

    ManualReductionTaskVO reductionTask = manualReductionCheckResult.validManualReductionTask;
    long expiredTime = reductionTask.getReductionDetail().getExpiredTime();
    long leftMillis = Clock.getMilliSecondsBetween(Clock.now(), expiredTime);
    long leftDays = leftMillis / Clock.MILLS_PER_DAY;
    IElement countdown;
    if (leftDays == 0) {
      countdown = new CountDownTimeElement(COLLECTION_REDUCTION_COUNTDOWN, null, new CountDownTimeElement.TimeElementParam(leftMillis, TimeUnit.MILLISECONDS));
    } else {
      countdown = ElementBuilder.text()
          .id(COLLECTION_REDUCTION_COUNTDOWN_TIP)
          .text(TT.gen("限免：{0}天", leftDays))
          .build();
    }

    return Optional.of(new CompositeElement(REPAYMENT_COLLECTION_REDUCTION_GUIDE)
        .addChild(title)
        .addChild(subTitle)
        .addChild(reductionIcon)
        .addChild(countdown));
  }
}
