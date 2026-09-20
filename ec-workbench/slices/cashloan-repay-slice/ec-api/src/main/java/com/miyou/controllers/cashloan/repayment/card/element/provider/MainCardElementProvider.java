package com.miyou.controllers.cashloan.repayment.card.element.provider;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.util.HomePageV3CardTool;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.PopUpAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.BaseElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.ButtonElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.commonelement.CommonElement;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.utilities.HomePageMainCardInfoTool;
import com.yqg.core.service.cashloan.RepaymentConfig;
import com.yqg.core.service.cashloan.homepage.config.HomepageV3ElementConfig;
import com.yqg.core.service.cashloan.homepage.config.NativePathConstant;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.yqg.core.service.loan.repayment.status.RepaymentDisplayStatus.OVERDUE_WITHIN_X_DAYS;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.ButtonElement.ButtonColor.RED;

@Component
public class MainCardElementProvider {

  @Autowired
  private RepaymentConfig repaymentConfig;
  @Autowired
  private HomepageV3ElementConfig elementConfig;
  @Autowired
  private HomePageMainCardInfoTool homePageMainCardInfoTool;
  @Autowired
  private RepaymentElementSupport repaymentElementSupport;
  @Autowired
  private HomePageV3CardTool homePageV3CardTool;

  public List<IElement> buildMainCard(RepaymentContext ctx) {
    if (ctx.repaymentReminderStrategy.isC()) {
      return buildMainCardV2(ctx);
    }
    return buildMainCardV1(ctx);
  }

  private List<IElement> buildMainCardV1(RepaymentContext ctx) {
    List<IElement> mainCardElementList;
    if (ctx.repaymentDisplayStatus.isOverdueMoreThanOneDay()) {
      mainCardElementList = buildMainCardForOverdueMoreThanOneDay(ctx);
    } else {
      mainCardElementList = buildMainCardForNotOverdueOrLessThanOneDay(ctx);
    }
    return mainCardElementList;
  }

  private List<IElement> buildMainCardV2(RepaymentContext ctx) {
    List<IElement> mainCardElementList;
    if (ctx.repaymentDisplayStatus.isOverdue()) {
      mainCardElementList = buildMainCardForOverdueMoreThanOneDay(ctx);
    } else {
      mainCardElementList = buildMainCardForNotOverdueOrLessThanOneDay(ctx);
    }
    return mainCardElementList;
  }

  private List<IElement> buildMainCardForOverdueMoreThanOneDay(RepaymentContext ctx) {
    IElement loanOverdueIcon = ElementBuilder.image()
        .id(MainCardElementId.LOAN_OVERDUE_ICON)
        .imageUrl(repaymentConfig.getRepaymentCardReminderIconUrl().get(RepaymentConfig.RepaymentCardReminderIconType.WARNING_MAIN))
        .build();

    IElement loanOverdueTitle = ElementBuilder.text()
        .id(MainCardElementId.LOAN_OVERDUE_TITLE)
        .text(TT.gen("还款提醒"))
        .build();

    TT loanOverdueSubTitleText = ctx.repaymentDisplayStatus == OVERDUE_WITHIN_X_DAYS
        ? TT.gen("请重视您的信用，逾期将不利于您的下一次借款申请")
        : TT.gen("您已严重逾期，违反了借款协议约定，请尽快还款");
    IElement loanOverdueSubTitle = ElementBuilder.text()
        .id(MainCardElementId.LOAN_OVERDUE_SUBTITLE)
        .text(loanOverdueSubTitleText)
        .build();

    IElement title = ElementBuilder.text()
        .id(MainCardElementId.TITLE)
        .text(TT.gen("总待还金额"))
        .build();

    IElement amountText = ElementBuilder.text()
        .id(MainCardElementId.AMOUNT_TEXT)
        .text(TT.gen("{0}", AmountFormatter.formatForIDNNoRP(ctx.recentUnpaidAmount)))
        .build();

    IElement subTitle = ElementBuilder.text()
        .id(MainCardElementId.SUB_TITLE)
        .text(TT.gen("你已经逾期 {0} 天", Clock.getAbsCalenderDaysBetween(ctx.recentlyBillingDate, Clock.now(), ctx.homepageUserParam.getSDKType().getTimeZone())))
        .build();

    IElement mainButton = ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("立即还款"))
        .action(new RedirectAction(new RedirectAction.LinkActionParam(elementConfig.getRedirectPage(NativePathConstant.REPAYMENT_PAGE))))
        .elementParam(ButtonElement.ButtonParam.builder().colorType(RED).build())
        .build();

    List<IElement> mainCardElementList = Lists.newArrayList(title, amountText, subTitle, mainButton, loanOverdueIcon, loanOverdueTitle, loanOverdueSubTitle);
    repaymentElementSupport.tryBuildCollectionReductionGuide(ctx).ifPresent(mainCardElementList::add);
    return mainCardElementList;
  }


  private List<IElement> buildMainCardForNotOverdueOrLessThanOneDay(RepaymentContext ctx) {
    IElement topTip = CommonElement.TOP_TIP;

    IElement title = ElementBuilder.text()
        .id(MainCardElementId.TITLE)
        .text(TT.gen("剩余可借金额"))
        .build();

    LoanUserCreditsInfoVO creditsInfoVO = ctx.homepageUserParam.creditsInfoVO;

    IElement amountText = ElementBuilder.text()
        .id(MainCardElementId.AMOUNT_TEXT)
        .text(TT.gen("{0}", AmountFormatter.formatForIDNNoRP(ctx.remainCredits)))
        .build();

    IElement subTitle = ElementBuilder.text()
        .id(MainCardElementId.SUB_TITLE)
        .text(ctx.secondRiskRejectedDisplay ?
            TT.gen("额度正在评估中。良好的还款记录将加快评估进程。")
            : TT.gen("总额度 {0}", AmountFormatter.format(EcCurrency.IDR, creditsInfoVO.getTotalFixedCreditsForVirtual())))
        .build();

    IElement mainButton = buildButtonElementForMainCard(ctx);
    // 额度进度条
    Optional<IElement> amountProgressBar = homePageV3CardTool.buildAmountProgressBarElement(ctx.homePageContext);

    List<IElement> iElements;
    // 额度进度条和TOP_TIP不能同时存在，优先展示额度进度条
    // 展示进度条时，不展示包含总金额的SUB_TITLE（"总额度 {0}"）；不含金额的文案（如"额度正在评估中"）保留
    if (amountProgressBar.isPresent()) {
      iElements = ctx.secondRiskRejectedDisplay
          ? Lists.newArrayList(amountProgressBar.get(), title, amountText, subTitle, mainButton)
          : Lists.newArrayList(amountProgressBar.get(), title, amountText, mainButton);
    } else {
      iElements = Lists.newArrayList(topTip, title, amountText, subTitle, mainButton);
    }
    buildSubTitleTipElementForMainCard(ctx).ifPresent(iElements::add);
    return iElements;
  }

  private IElement buildButtonElementForMainCard(RepaymentContext ctx) {
    HomeDisplayStrategy revolvingReviewDisplayStrategy = ctx.homepageUserParam.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy;
    String loanTipsPopupStr = homePageMainCardInfoTool.getRepaymentLoanTipsPopup(revolvingReviewDisplayStrategy, ctx.homePageContext);
    LoanTipsPopUp loanTipsPopup = LoanTipsPopUp.from(JsonUtils.fromOrNull(loanTipsPopupStr, LoanTipsPopUpVO.class));

    ElementBuilder<BaseElement> mainButtonBuilder = ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(ctx.secondRiskRejectedDisplay ? TT.gen("开启额度通知") : TT.gen("立即申请"))
        .elementParam(ButtonElement.ButtonParam.builder().colorType(ButtonElement.ButtonColor.GREEN).build());
    if (!ctx.secondRiskRejectedDisplay && Objects.nonNull(loanTipsPopup)) {
      mainButtonBuilder.action(new PopUpAction(PopUpAction.PopUpParam.builder()
          .title(TT.gen("温馨提示"))
          .content(loanTipsPopup.content)
          .buttonContent(loanTipsPopup.buttonContent)
          .buttonUrl(loanTipsPopup.buttonUrl)
          .build()));
    }
    return mainButtonBuilder.build();
  }

  private Optional<IElement> buildSubTitleTipElementForMainCard(RepaymentContext ctx) {
    if (!ctx.secondRiskRejectedDisplay) {
      return Optional.empty();
    }

    String recentlyBillingDateStr = Clock.dateTimeStringFromTimestampWithLocale(ctx.recentlyBillingDate, DateFormatter.dd___MMM___yyyy, ctx.homePageContext.getSdkType().getTimeZone(), ctx.homePageContext.getSdkType().getLocale());
    BaseElement subTitleTip = ElementBuilder.button()
        .id(MainCardElementId.SUB_TITLE_TIP)
        .action(new PopUpAction(PopUpAction.PopUpParam.builder()
            .title(TT.gen("温馨提示"))
            .content(TT.gen("贷款资格因风险评估因素暂时尚未通过。请保持良好还款记录，您可在{0}再次借款", recentlyBillingDateStr))
            .boldContent(TT.gen("{0}", recentlyBillingDateStr))
            .buttonContent(TT.gen("开启额度通知"))
            .build()))
        .build();

    return Optional.of(subTitleTip);
  }
}
