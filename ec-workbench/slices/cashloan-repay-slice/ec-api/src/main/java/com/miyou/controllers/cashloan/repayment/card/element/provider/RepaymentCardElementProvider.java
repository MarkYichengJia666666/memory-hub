package com.miyou.controllers.cashloan.repayment.card.element.provider;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContext;
import com.yqg.core.service.cashloan.homepage.config.HomepageV3ElementConfig;
import com.yqg.core.service.cashloan.homepage.config.NativePathConstant;
import com.miyou.controllers.cashloan.repayment.card.element.param.RepaymentRemainingTimeElementParam;
import com.miyou.controllers.cashloan.repayment.card.element.param.RepaymentTextElementParam;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.ButtonElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.CompositeElement;
import com.yqg.core.service.cashloan.RepaymentConfig;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.RepaymentCardElementId.*;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.ButtonElement.ButtonColor.RED;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.*;


@Component
public class RepaymentCardElementProvider {

    public static final String HIGHLIGHT_KEY_BILLING_DATE = "BILLING_DATE";
    public static final String HIGHLIGHT_KEY_DAY_INTEREST_RATE = "DAY_INTEREST_RATE";

    @Autowired
    private RepaymentConfig repaymentConfig;
    @Autowired
    private HomepageV3ElementConfig elementConfig;
    @Autowired
    private RepaymentElementSupport repaymentElementSupport;


    public List<IElement> buildRepaymentCard(RepaymentContext ctx) {
      if (ctx.repaymentReminderStrategy.isC()) {
        return buildRepaymentCardV2(ctx);
      }
      return buildRepaymentCardV1(ctx);
    }

    private List<IElement> buildRepaymentCardV1(RepaymentContext ctx) {
        List<IElement> repaymentElementList;
        switch (ctx.repaymentDisplayStatus) {
            case NO_UNPAID_INSTALMENT_IN_X_DAYS:
                repaymentElementList = buildForNoUnpaidInstalmentInXDays(ctx);
                repaymentElementSupport.tryBuildCollectionReductionGuide(ctx).ifPresent(repaymentElementList::add);
                break;
            case HAS_UNPAID_INSTALMENT_IN_X_DAYS:
                repaymentElementList = buildForHasUnpaidInstalmentInXDays(ctx);
                repaymentElementSupport.tryBuildCollectionReductionGuide(ctx).ifPresent(repaymentElementList::add);
                break;
            case HAS_UNPAID_INSTALMENT_WITHIN_24H:
                repaymentElementList = buildForHasUnpaidInstalmentWith24Hours(ctx);
                repaymentElementSupport.tryBuildCollectionReductionGuide(ctx).ifPresent(repaymentElementList::add);
                break;
            case OVERDUE_ONE_DAY:
                repaymentElementList = buildForOverdueOneDay(ctx);
                repaymentElementSupport.tryBuildCollectionReductionGuide(ctx).ifPresent(repaymentElementList::add);
                break;
            case OVERDUE_WITHIN_X_DAYS:
            case OVERDUE_BEYOND_X_DAYS:
                repaymentElementList = buildForOverdueMoreThanOneDay(ctx);
                break;
            default:
                repaymentElementList = Lists.newArrayList();
                break;
        }
        return repaymentElementList;
    }

    private List<IElement> buildRepaymentCardV2(RepaymentContext ctx) {
        List<IElement> repaymentElementList;
        switch (ctx.repaymentDisplayStatus) {
            case NO_UNPAID_INSTALMENT_IN_X_DAYS:
                repaymentElementList = buildForNoUnpaidInstalmentInXDays(ctx);
                repaymentElementSupport.tryBuildCollectionReductionGuide(ctx).ifPresent(repaymentElementList::add);
                break;
            case HAS_UNPAID_INSTALMENT_IN_X_DAYS:
                repaymentElementList = buildForHasUnpaidInstalmentInXDaysV2(ctx);
                repaymentElementSupport.tryBuildCollectionReductionGuide(ctx).ifPresent(repaymentElementList::add);
                break;
            case HAS_UNPAID_INSTALMENT_WITHIN_24H:
                repaymentElementList = buildForHasUnpaidInstalmentWith24HoursV2(ctx);
                repaymentElementSupport.tryBuildCollectionReductionGuide(ctx).ifPresent(repaymentElementList::add);
                break;
            case OVERDUE_ONE_DAY:
            case OVERDUE_WITHIN_X_DAYS:
            case OVERDUE_BEYOND_X_DAYS:
                repaymentElementList = buildForOverdueMoreThanOneDay(ctx);
                break;
            default:
                repaymentElementList = Lists.newArrayList();
                break;
        }
        return repaymentElementList;
    }


  private List<IElement> buildForNoUnpaidInstalmentInXDays(RepaymentContext ctx) {

        IElement billingDate = ElementBuilder.text()
                .id(BILL_DUE_DATE)
                .text(TT.gen(String.format("到期日: %s", HIGHLIGHT_KEY_BILLING_DATE)))
                .elementParam(RepaymentTextElementParam.builder()
                        .highlightMap(buildBillingDateHighlightMap(ctx))
                        .highlightColor(BLACK_000)
                        .textColor(BLACK_000)
                        .fontSize(14)
                        .build())
                .build();

        IElement billDueAmount = ElementBuilder.text()
                .id(BILL_DUE_AMOUNT)
                .text(TT.gen("{0}", AmountFormatter.format(EcCurrency.IDR, ctx.recentUnpaidAmount)))
                .build();

        IElement repaymentButton = buildRepaymentButton(ButtonElement.ButtonColor.GREEN);

        CompositeElement repaymentTitle = new CompositeElement(REPAYMENT_TITLE)
                .addChild(billingDate)
                .addChild(billDueAmount)
                .addChild(repaymentButton);
        return Lists.newArrayList(repaymentTitle);
    }

    private List<IElement> buildForHasUnpaidInstalmentInXDays(RepaymentContext ctx) {
        // 第一部分 还款卡片标题
        IElement billingDate = ElementBuilder.text()
                .id(BILL_DUE_DATE)
                .text(TT.gen(String.format("到期日: %s", HIGHLIGHT_KEY_BILLING_DATE)))
                .elementParam(RepaymentTextElementParam.builder()
                        .iconUrl(repaymentConfig.getRepaymentCardReminderIconUrl().get(RepaymentConfig.RepaymentCardReminderIconType.PROMPT))
                        .highlightMap(buildBillingDateHighlightMap(ctx))
                        .highlightColor(BLACK_000)
                        .textColor(BLACK_000)
                        .fontSize(14)
                        .build())
                .build();
        IElement repaymentButton = buildRepaymentButton(ButtonElement.ButtonColor.GREEN);
        CompositeElement repaymentTitle = new CompositeElement(REPAYMENT_TITLE).addChild(billingDate).addChild(repaymentButton);

        // 第二部分 还款卡片详情 账单金额
        IElement billAmountLabel = buildBillAmountLabel();
        IElement billAmountValue = buildBillAmountValue(ctx.recentUnpaidAmount, BLACK_000);
        CompositeElement repaymentDetailsAmount = new CompositeElement(BILL_AMOUNT)
                .setElementParam(RepaymentTextElementParam.builder().itemBackgroundColor(PALE_GREEN).build())
                .addChild(billAmountLabel)
                .addChild(billAmountValue);

        // 第二部分 还款卡片详情 剩余还款时间
        IElement remainingRepaymentTimeLabel = buildRemainingRepaymentTimeLabel();
        IElement remainingRepaymentTimeValue = ElementBuilder.text()
                .id(REMAINING_REPAYMENT_TIME_VALUE)
                .text(TT.gen("{0} 天", Clock.getAbsCalenderDaysBetween(ctx.recentlyBillingDate, Clock.now(), ctx.homepageUserParam.getSDKType().getTimeZone())))
                .elementParam(RepaymentTextElementParam.builder().textColor(BLACK_333).build())
                .build();
        CompositeElement repaymentDetailsRemainingRepaymentTime = new CompositeElement(REMAINING_REPAYMENT_TIME)
                .setElementParam(RepaymentTextElementParam.builder().itemBackgroundColor(PALE_GREEN).build())
                .addChild(remainingRepaymentTimeLabel)
                .addChild(remainingRepaymentTimeValue);

        CompositeElement repaymentDetails = new CompositeElement(REPAYMENT_DETAILS)
                .addChild(repaymentDetailsAmount)
                .addChild(repaymentDetailsRemainingRepaymentTime);

        return Lists.newArrayList(repaymentTitle, repaymentDetails);
    }

  private List<IElement> buildForHasUnpaidInstalmentInXDaysV2(RepaymentContext ctx) {

    // 第一部分 还款卡片顶部区域
    IElement repaymentTopArea = buildRepaymentTopAreaForHasUnpaidInstalmentInXDays(ctx);

    // 第二部分 还款卡片标题
    IElement repaymentTitle = buildRepaymentTitleV2(ctx);

    return Lists.newArrayList(repaymentTopArea, repaymentTitle);
  }

  /**
   * 构造与 REPAYMENT_TOP_AREA 配合使用的 REPAYMENT_TITLE 元素
   */
  private IElement buildRepaymentTitleV2(RepaymentContext ctx) {
    IElement billDueDate = ElementBuilder.text().id(BILL_DUE_DATE).text(TT.gen("待还金额")).build();
    IElement billDueAmount = ElementBuilder.text().id(BILL_DUE_AMOUNT)
        .text(TT.gen("{0}", AmountFormatter.formatForIDNNoRP(ctx.recentUnpaidAmount)))
        .elementParam(RepaymentTextElementParam.builder().fontSize(20).build())
        .build();
    IElement billDueAmountUnit = ElementBuilder.text().id(BILL_DUE_AMOUNT_UNIT).text(TT.gen("Rp")).build();
    IElement repaymentButton = buildRepaymentButton(ButtonElement.ButtonColor.GREEN);
    return new CompositeElement(REPAYMENT_TITLE)
        .addChild(billDueDate)
        .addChild(billDueAmount)
        .addChild(billDueAmountUnit)
        .addChild(repaymentButton);
  }

  private IElement buildRepaymentTopAreaForHasUnpaidInstalmentInXDays(RepaymentContext ctx) {
      // 左侧 账单到期日提醒文案
      IElement billingDate = ElementBuilder.text()
          .id(BILL_DUE_DATE)
          .text(TT.gen(String.format("到期日: %s", HIGHLIGHT_KEY_BILLING_DATE)))
          .elementParam(RepaymentTextElementParam.builder()
              .iconUrl(null)
              .highlightMap(buildBillingDateHighlightMap(ctx))
              .highlightColor(BLACK_000)
              .textColor(BLACK_000)
              .fontSize(14)
              .build())
          .build();

      // 右侧 剩余天数提醒文案
      int daysLeft = Clock.getAbsCalenderDaysBetween(ctx.recentlyBillingDate, Clock.now(), ctx.homepageUserParam.getSDKType().getTimeZone());
      IElement remainingRepaymentTimeValue = ElementBuilder.text()
              .id(REMAINING_REPAYMENT_TIME_VALUE)
              .text(TT.gen("剩余 {0} 天", daysLeft))
              .elementParam(RepaymentTextElementParam.builder().textColor(BLACK_333).blockText(String.valueOf(daysLeft)).build())
              .build();

      CompositeElement remainingRepaymentTime = new CompositeElement(REMAINING_REPAYMENT_TIME)
          .setElementParam(RepaymentTextElementParam.builder().itemBackgroundColor(PALE_GREEN).build())
          .addChild(remainingRepaymentTimeValue);

      return new CompositeElement(REPAYMENT_TOP_AREA).addChild(billingDate).addChild(remainingRepaymentTime);
  }

  private List<IElement> buildForHasUnpaidInstalmentWith24Hours(RepaymentContext ctx) {
        // 第一部分 还款卡片标题
        TT title = TT.gen("今日待还");
        String translatedTitle = title.toString(ctx.homePageContext.getSdkType().getLocale().locale);
        IElement billingDate = ElementBuilder.text()
                .id(BILL_DUE_DATE)
                .text(title)
                .elementParam(RepaymentTextElementParam.builder()
                        .iconUrl(repaymentConfig.getRepaymentCardReminderIconUrl().get(RepaymentConfig.RepaymentCardReminderIconType.PROMPT))
                        .highlightMap(ImmutableMap.of(translatedTitle, translatedTitle))
                        .highlightColor(BLACK_333)
                        .textColor(BLACK_333)
                        .fontSize(14)
                        .build())
                .build();
        IElement repaymentButton = buildRepaymentButton(ButtonElement.ButtonColor.GREEN);
        CompositeElement repaymentTitle = new CompositeElement(REPAYMENT_TITLE).addChild(billingDate).addChild(repaymentButton);

        // 第二部分 还款卡片详情 账单金额
        IElement billAmountLabel = buildBillAmountLabel();
        IElement billAmountValue = buildBillAmountValue(ctx.recentUnpaidAmount, BLACK_000);
        CompositeElement repaymentDetailsAmount = new CompositeElement(BILL_AMOUNT)
                .setElementParam(RepaymentTextElementParam.builder().itemBackgroundColor(PALE_GREEN).build())
                .addChild(billAmountLabel).addChild(billAmountValue);

        // 第二部分 还款卡片详情 剩余还款时间
        IElement remainingRepaymentTimeLabel = buildRemainingRepaymentTimeLabel();
        IElement remainingRepaymentTimeValue = ElementBuilder.text()
                .id(REMAINING_REPAYMENT_TIME_VALUE)
                .text(null)
                .elementParam(RepaymentRemainingTimeElementParam.builder()
                        .countDown(true)
                        .remainingMillis(Clock.getMilliSecondsBetween(Clock.now(), Clock.getMaxMillisOfDay(ctx.recentlyBillingDate, ctx.homepageUserParam.getSDKType().getTimeZone())))
                        .textColor(BLACK_333)
                        .build())
                .build();
        CompositeElement repaymentDetailsRemainingRepaymentTime = new CompositeElement(REMAINING_REPAYMENT_TIME)
                .setElementParam(RepaymentTextElementParam.builder().itemBackgroundColor(PALE_GREEN).build())
                .addChild(remainingRepaymentTimeLabel)
                .addChild(remainingRepaymentTimeValue);

        CompositeElement repaymentDetails = new CompositeElement(REPAYMENT_DETAILS)
                .addChild(repaymentDetailsAmount)
                .addChild(repaymentDetailsRemainingRepaymentTime);

        return Lists.newArrayList(repaymentTitle, repaymentDetails);
    }

  private List<IElement> buildForHasUnpaidInstalmentWith24HoursV2(RepaymentContext ctx) {

    // 第一部分 还款卡片顶部区域
    IElement repaymentTopArea = buildRepaymentTopAreaForHasUnpaidInstalmentWithin24Hours(ctx);

    // 第二部分 还款卡片标题
    IElement repaymentTitle = buildRepaymentTitleV2(ctx);

    return Lists.newArrayList(repaymentTopArea, repaymentTitle);
  }

    private IElement buildRepaymentTopAreaForHasUnpaidInstalmentWithin24Hours(RepaymentContext ctx) {
        // 左侧 今日待还提醒
        TT title = TT.gen("今日待还");
        String translatedTitle = title.toString(ctx.homePageContext.getSdkType().getLocale().locale);
        IElement billingDate = ElementBuilder.text()
            .id(BILL_DUE_DATE)
            .text(title)
            .elementParam(RepaymentTextElementParam.builder()
                .iconUrl(null)
                .highlightMap(ImmutableMap.of(translatedTitle, translatedTitle))
                .highlightColor(BLACK_333)
                .textColor(BLACK_333)
                .fontSize(14)
                .build())
            .build();

        // 右侧 还款倒计时
        long millisLeft = Clock.getMilliSecondsBetween(Clock.now(), Clock.getMaxMillisOfDay(ctx.recentlyBillingDate, ctx.homepageUserParam.getSDKType().getTimeZone()));
        IElement remainingRepaymentTimeValue = ElementBuilder.text()
            .id(REMAINING_REPAYMENT_TIME_VALUE)
            .text(null)
            .elementParam(RepaymentRemainingTimeElementParam.builder()
                .countDown(true)
                .remainingMillis(millisLeft)
                .textColor(BLACK_333)
                .build())
            .build();

        CompositeElement remainingRepaymentTime = new CompositeElement(REMAINING_REPAYMENT_TIME)
            .setElementParam(RepaymentTextElementParam.builder().itemBackgroundColor(PALE_GREEN).build())
            .addChild(remainingRepaymentTimeValue);
        return new CompositeElement(REPAYMENT_TOP_AREA).addChild(billingDate).addChild(remainingRepaymentTime);
    }

    private List<IElement> buildForOverdueOneDay(RepaymentContext ctx) {
        // 第一部分 还款卡片标题
        TT title = TT.gen("您当前已逾期");
        String translatedTitle = title.toString(ctx.homePageContext.getSdkType().getLocale().locale);
        IElement billingDate = ElementBuilder.text()
                .id(BILL_DUE_DATE)
                .text(title)
                .elementParam(RepaymentTextElementParam.builder()
                        .iconUrl(repaymentConfig.getRepaymentCardReminderIconUrl().get(RepaymentConfig.RepaymentCardReminderIconType.WARNING))
                        .highlightMap(ImmutableMap.of(translatedTitle, translatedTitle))
                        .highlightColor(SCARLET)
                        .textColor(SCARLET)
                        .fontSize(14)
                        .build())
                .build();
        IElement repaymentButton = buildRepaymentButton(ButtonElement.ButtonColor.RED);
        CompositeElement repaymentTitle = new CompositeElement(REPAYMENT_TITLE).addChild(billingDate).addChild(repaymentButton);

        // 第二部分 还款卡片详情 账单金额
        IElement billAmountLabel = buildBillAmountLabel();
        IElement billAmountValue = buildBillAmountValue(ctx.recentUnpaidAmount, SCARLET);
        CompositeElement repaymentDetailsAmount = new CompositeElement(BILL_AMOUNT)
                .setElementParam(RepaymentTextElementParam.builder().itemBackgroundColor(PALE_RED).build())
                .addChild(billAmountLabel)
                .addChild(billAmountValue);

        // 第二部分 还款卡片详情 剩余还款时间
        IElement remainingRepaymentTimeLabel = ElementBuilder.text()
                .id(REMAINING_REPAYMENT_TIME_LABEL)
                .text(TT.gen("警告"))
                .build();
        IElement remainingRepaymentTimeValue = ElementBuilder.text()
                .id(REMAINING_REPAYMENT_TIME_VALUE)
                .text(TT.gen("你已经逾期 {0} 天", Clock.getAbsCalenderDaysBetween(ctx.recentlyBillingDate, Clock.now(), ctx.homepageUserParam.getSDKType().getTimeZone())))
                .elementParam(RepaymentTextElementParam.builder().textColor(SCARLET).build())
                .build();
        CompositeElement repaymentDetailsRemainingRepaymentTime = new CompositeElement(REMAINING_REPAYMENT_TIME)
                .setElementParam(RepaymentTextElementParam.builder().itemBackgroundColor(PALE_RED).build())
                .addChild(remainingRepaymentTimeLabel)
                .addChild(remainingRepaymentTimeValue);

        CompositeElement repaymentDetails = new CompositeElement(REPAYMENT_DETAILS)
                .addChild(repaymentDetailsAmount)
                .addChild(repaymentDetailsRemainingRepaymentTime);

        return Lists.newArrayList(repaymentTitle, repaymentDetails);
    }

    private List<IElement> buildForOverdueMoreThanOneDay(RepaymentContext ctx) {
        IElement overdueRepaymentContent = ElementBuilder.text()
                .id(OVERDUE_REPAYMENT_CONTENT)
                .text(TT.gen("预计还款后可借"))
                .elementParam(RepaymentTextElementParam.builder()
                        .textColor(BLACK_1A1E23)
                        .fontSize(12)
                        .build())
                .build();

        IElement billDueAmount = ElementBuilder.text()
                .id(BILL_DUE_AMOUNT)
                .text(TT.gen("{0}", "Rp20.000.000"))
                .build();

        IElement repaymentButton = buildRepaymentButton(RED);

        IElement overdueRepaymentTip = ElementBuilder.text()
                .id(OVERDUE_REPAYMENT_TIP)
                .text(TT.gen(String.format("利息低至 %s 每天", HIGHLIGHT_KEY_DAY_INTEREST_RATE)))
                .elementParam(RepaymentTextElementParam.builder()
                        .highlightMap(ImmutableMap.of(HIGHLIGHT_KEY_DAY_INTEREST_RATE, "0,2%"))
                        .highlightColor(FOREST_GREEN)
                        .build())
                .build();

        CompositeElement repaymentTitle = new CompositeElement(REPAYMENT_TITLE)
                .addChild(overdueRepaymentContent)
                .addChild(billDueAmount)
                .addChild(repaymentButton)
                .addChild(overdueRepaymentTip);
        return Lists.newArrayList(repaymentTitle);
    }

    private ImmutableMap<String, String> buildBillingDateHighlightMap(RepaymentContext ctx) {
        SDKType sdkType = ctx.homepageUserParam.getSDKType();
        TimeZone tz = sdkType.getTimeZone();
        boolean dueInCurrentYear = Objects.equals(Clock.getMinMillisOfYear(ctx.recentlyBillingDate, tz), Clock.getMinMillisOfYear(Clock.now(), tz));
        return ImmutableMap.of(
                HIGHLIGHT_KEY_BILLING_DATE,
                Clock.dateTimeStringFromTimestampWithLocale(ctx.recentlyBillingDate, dueInCurrentYear ? DateFormatter.dd___MMM : DateFormatter.dd___MMM___yyyy, tz, sdkType.getLocale())
        );
    }

    private IElement buildRepaymentButton(ButtonElement.ButtonColor buttonColor) {
        return ElementBuilder.button()
                .id(REPAYMENT_BUTTON)
                .text(TT.gen("还款"))
                .action(new RedirectAction(new RedirectAction.LinkActionParam(elementConfig.getRedirectPage(NativePathConstant.REPAYMENT_PAGE))))
                .elementParam(ButtonElement.ButtonParam.builder()
                        .colorType(buttonColor)
                        .build())
                .build();
    }

    private IElement buildBillAmountLabel() {
        return ElementBuilder.text()
                .id(BILL_AMOUNT_LABEL)
                .text(TT.gen("待还金额"))
                .build();
    }

    private IElement buildBillAmountValue(BigDecimal recentUnpaidAmount, String textColor) {
        return ElementBuilder.text()
                .id(BILL_AMOUNT_VALUE)
                .text(TT.gen("{0}", AmountFormatter.format(EcCurrency.IDR, recentUnpaidAmount)))
                .elementParam(RepaymentTextElementParam.builder().textColor(textColor).build())
                .build();
    }

    private IElement buildRemainingRepaymentTimeLabel() {
        return ElementBuilder.text()
                .id(REMAINING_REPAYMENT_TIME_LABEL)
                .text(TT.gen("剩余付款时间"))
                .build();
    }


}
