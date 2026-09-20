package com.miyou.controllers.cashloan.newhomepage.elementmodel.util;

import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.FOREST_GREEN;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.GREEN;
import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;

import com.google.common.collect.ImmutableMap;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.constant.CustomerElementType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementModuleType;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MitraCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.RegulatorCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.TkbCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.CustomerElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement;
import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.auth.step.AuthStepService;
import com.yqg.core.service.cashloan.auth.vo.AuthStepConditionVO;
import com.yqg.core.service.cashloan.homepage.config.HomepageV3ElementConfig;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.general.appconfig.IdnTkbDisplayService;
import com.yqg.core.service.general.appconfig.provider.operationdata.IdnAppTkbDataProvider;
import com.yqg.core.service.general.appconfig.vo.IdnTKBVO;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.marketing.channel.HomeMainCardQian1Service;
import com.yqg.core.service.marketing.channel.wany.HomeMainCardWanyText;
import com.yqg.core.service.marketing.channel.wany.WanyAcquisitionService;
import com.yqg.core.service.marketing.channel.wany.vo.WanyAcquisitionParam;
import com.yqg.core.service.marketing.channel.zerointerest.HomeMainCardZeroInterestText;
import com.yqg.core.service.marketing.channel.zerointerest.ZeroInterestAcquisitionService;
import com.yqg.core.service.marketing.channel.zerointerest.vo.ZeroInterestAcquisitionParam;
import com.yqg.core.userflow.domain.common.Position;
import com.yqg.core.userflow.domain.increasecredit.service.IIncreaseCreditService;
import com.yqg.core.userflow.domain.loan.model.amount.AmountProgressBar;
import com.yqg.core.userflow.domain.loan.service.amount.IAmountDisplayService;
import com.yqg.core.util.common.NumberFormatter;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HomePageV3CardTool {
  @Autowired
  protected ProductConfigService productConfigService;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private LoanCreditsQuotaService loanCreditsQuotaService;
  @Autowired
  private IIncreaseCreditService increaseCreditService;
  @Autowired
  private HomepageV3ElementConfig homepageV3ElementConfig;
  @Autowired
  private IdnTkbDisplayService idnTkbDisplayService;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private HomeMainCardQian1Service homeMainCardQian1Service;
  @Autowired
  private AuthStepService authStepService;
  @Autowired
  private ZeroInterestAcquisitionService zeroInterestAcquisitionService;
  @Autowired
  private WanyAcquisitionService wanyAcquisitionService;
  @Autowired
  private IAmountDisplayService amountDisplayService;

  /**
   * 组装通用借款信息
   * 包括主标题、金额、副标题
   *
   * @param pageCardV3VO
   * @param homePageContext
   */
  public void buildCommonLoanInfoElementForMainCard(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    buildTitleElementForMainCard(pageCardV3VO, homePageContext);
    buildAmountTextForMainCard(pageCardV3VO, homePageContext);
    buildSubTitleForMainCard(pageCardV3VO, homePageContext);
  }

  public void buildTitleElementForMainCard(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.TITLE)
        .text(TT.gen("最大可借金额"))
        .build());
  }

  public void buildAmountTextForMainCard(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
    BigDecimal maxCreditsBySDK = isWholeProcess ? homepageV3ElementConfig.getH5WholeProcessMaxCredit() : productConfigService.getMaxCreditsBySDKOrZero(homePageContext.getSdkType());
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.AMOUNT_TEXT)
        .text(TT.gen("{0}", AmountFormatter.formatForIDNNoRP(maxCreditsBySDK)))
        .build());
  }

  public void buildSubTitleForMainCard(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    // 本方法承接的是 NeverApplied/CreditsExpired/CanReapply/NotLogin/Reject 等不可下单状态（spec US4-1），
    // 可下单状态由 LoanProductProcessor 单独承接千1，不走此处。未完件副标题优先级：千1 > 万一 > 0 息 > 默认；
    // 已完件（被拒/可重申/额度失效/未登录等）没有这三类代码文案，直接展示默认息费
    if (homePageContext.getLoanAccountVO() != null) {
      boolean applicationUnfinished = isApplicationUnfinished(homePageContext);
      if (applicationUnfinished && homeMainCardQian1Service.shouldShowMarketingBar(
          homePageContext.getUserId(),
          homePageContext.getUserDeviceContextVO().getDeviceToken(),
          homePageContext.getUserDeviceContextVO().getBuild(),
          homePageContext.getUserDeviceContextVO().getSourceType(),
          homePageContext.getSdkType(), false)) {
        pageCardV3VO.addElementForMainCard(ElementBuilder.text()
            .id(MainCardElementId.SUB_TITLE)
            .text(TT.gen(HomeMainCardQian1Service.SUB_TITLE_TEXT_NOT_AUTH))
            .elementParam(TextElement.TextParam.builder()
                .highlightMap(ImmutableMap.of(HomeMainCardQian1Service.SUB_TITLE_BOLD_PLACEHOLDER,
                    HomeMainCardQian1Service.SUB_TITLE_BOLD_DISPLAY))
                .highlightStyle(TextElement.TextStyle.BOLD)
                .highlightColor(FOREST_GREEN)
                .build())
            .build());
        return;
      }
      // 万一承接（TAPD-1369319 / PRD #13）：排在千1 之后、0 息之前；只覆盖未完件 SUB_TITLE
      if (applicationUnfinished && isWanyLandingUser(homePageContext)) {
        pageCardV3VO.addElementForMainCard(ElementBuilder.text()
            .id(MainCardElementId.SUB_TITLE)
            .text(TT.gen(HomeMainCardWanyText.SUB_TITLE_TEXT_NOT_AUTH))
            .elementParam(TextElement.TextParam.builder()
                .highlightMap(ImmutableMap.of(HomeMainCardWanyText.SUB_TITLE_BOLD_PLACEHOLDER,
                    HomeMainCardWanyText.SUB_TITLE_BOLD_DISPLAY))
                .highlightStyle(TextElement.TextStyle.BOLD)
                .highlightColor(GREEN)
                .build())
            .build());
        return;
      }
      // 0 息承接（spec US3-1/US3-3）：排在千1 / 万一之后，同时命中时不叠加文案
      if (applicationUnfinished && isZeroInterestAcquisition(homePageContext)) {
        pageCardV3VO.addElementForMainCard(ElementBuilder.text()
            .id(MainCardElementId.SUB_TITLE)
            .text(TT.gen(HomeMainCardZeroInterestText.SUB_TITLE_TEXT_NOT_AUTH))
            .elementParam(TextElement.TextParam.builder()
                .highlightMap(ImmutableMap.of(HomeMainCardZeroInterestText.SUB_TITLE_BOLD_PLACEHOLDER,
                    HomeMainCardZeroInterestText.SUB_TITLE_BOLD_DISPLAY))
                .highlightStyle(TextElement.TextStyle.BOLD)
                .highlightColor(TextElement.TextColor.GREEN)
                .build())
            .build());
        return;
      }
    }
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.SUB_TITLE)
        .text(TT.gen("利息低至INTEREST每天，借款周期TERMS个月"))
        .elementParam(TextElement.TextParam.builder()
            .highlightMap(ImmutableMap.of("INTEREST", NumberFormatter.percentFormat(YqgLocale.INDONESIAN, homepageV5Config.getHomepageDefaultInterestTip()),
                "TERMS", "1-12"))
            .build())
        .build());
  }

  private boolean isZeroInterestAcquisition(HomePageContext homePageContext) {
    ZeroInterestAcquisitionParam param = ZeroInterestAcquisitionParam.builder()
        .userId(homePageContext.getUserId())
        .deviceToken(homePageContext.getUserDeviceContextVO().getDeviceToken())
        .build(homePageContext.getUserDeviceContextVO().getBuild())
        .platformType(homePageContext.getUserDeviceContextVO().getPlatformType())
        .sourceType(homePageContext.getUserDeviceContextVO().getSourceType())
        .sdkType(homePageContext.getSdkType())
        .build();
    return zeroInterestAcquisitionService.isZeroInterestAcquisition(param);
  }

  private boolean isWanyLandingUser(HomePageContext homePageContext) {
    WanyAcquisitionParam param = WanyAcquisitionParam.builder()
        .userId(homePageContext.getUserId())
        .deviceToken(homePageContext.getUserDeviceContextVO().getDeviceToken())
        .build(homePageContext.getUserDeviceContextVO().getBuild())
        .platformType(homePageContext.getUserDeviceContextVO().getPlatformType())
        .sourceType(homePageContext.getUserDeviceContextVO().getSourceType())
        .sdkType(homePageContext.getSdkType())
        .build();
    return wanyAcquisitionService.isWanyLandingUser(param);
  }

  private boolean isApplicationUnfinished(HomePageContext homePageContext) {
    SDKType sdkType = homePageContext.getSdkType();
    Long build = homePageContext.getUserDeviceContextVO().getBuild();
    PlatformType platformType = homePageContext.getUserDeviceContextVO().getPlatformType();
    SourceType sourceType = homePageContext.getUserDeviceContextVO().getSourceType();
    String channel = homePageContext.getUserDeviceContextVO().getChannel();
    AuthStepConditionVO authStepConditionVO = new AuthStepConditionVO(
        homePageContext.getLoanAccountId(),
        sdkType,
        build,
        platformType,
        sourceType,
        channel
    );
    return !authStepService.isAuthStepsFinished(authStepConditionVO);
  }

  public void tryAddIncreaseCreditsLink(PageCardV3VO pageCardV3VO, HomePageContext ctx) {
    buildIncreaseCreditsLinkElement(ctx).ifPresent(pageCardV3VO::addElementForMainCard);
  }

  public Optional<IElement> buildIncreaseCreditsLinkElement(HomePageContext ctx) {
    HomeDisplayStrategy displayStrategy = loanCreditsQuotaService.getOrderPageTempAmountTipDisplayStrategy(
        ctx.getUserId(),
        homepageV5Config.getNewHomepageVersion(),
        ctx.getUserCreditsContext().getCreditsInfoVO().tempCreditsOrderTimesLimitForCoupon
    );
    Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
    if (displayStrategy == HomeDisplayStrategy.A || isWholeProcess) {
      return Optional.empty();
    }
    // 首贷回捞用户不展示增信提额入口
    if (increaseCreditService.hideIncreaseCreditEntrance(ctx.getUserId(), ctx.getLoanAccountId())) {
      return Optional.empty();
    }

    return Optional.of(ElementBuilder.link()
        .id(MainCardElementId.GOTO_LINK_INFO)
        .text(TT.gen("申请提额"))
        .action(new RedirectAction(RedirectAction.LinkActionParam.builder().redirectUrl(homepageV5Config.getIncreaseCreditsEntranceUrl()).build()))
        .build());
  }


  public Optional<IElement> buildAmountProgressBarElement(HomePageContext ctx) {
    return amountDisplayService.getHomeAmountProgressBar(ctx.getUserId(), ctx.getSdkType())
        .map(bar -> ElementBuilder.customerElement(CustomerElementType.AMOUNT_PROGRESS_BAR)
            .id(MainCardElementId.AMOUNT_PROGRESS_BAR)
            .action(new RedirectAction(RedirectAction.LinkActionParam.builder()
                .redirectUrl(amountDisplayService.getAmountCenterNewPageAddress(Position.MAIN_CARD_AMOUNT_PROGRESS_BAR))
                .build()))
            .elementParam(toAmountProgressBarParam(bar))
            .build());
  }

  private CustomerElement.AmountProgressBarParam toAmountProgressBarParam(AmountProgressBar bar) {
    return CustomerElement.AmountProgressBarParam.builder()
        .title(bar.getTitle())
        .totalAmount(bar.getTotalAmount())
        .totalAmountText(bar.getTotalAmountText())
        .maxTotalAmount(bar.getMaxTotalAmount())
        .maxTotalAmountText(bar.getMaxTotalAmountText())
        .lastTotalAmount(bar.getLastTotalAmount())
        .desc(bar.getDesc())
        .build();
  }

  public List<IElement> buildMainCardSafetyText() {
    return Collections.singletonList(ElementBuilder.text()
        .id(MainCardElementId.SAFETY_TEXT)
        .text(TT.gen("Easycash telah berizin dan diawasi oleh OJK & AFPI."))
        .build());
  }

  public List<IElement> buildTkbElements() {
    IdnTKBVO tkbVO = idnTkbDisplayService.getDisplayTkb();
    String tkb90 = IdnAppTkbDataProvider.getFormattedTKB(
        tkbVO != null && tkbVO.tkbNinety != null ? tkbVO.tkbNinety : "99", YqgLocale.INDONESIAN);
    CustomerElement.TkbCardParam param = CustomerElement.TkbCardParam.builder()
        .tkb90(tkb90)
        .build();
    IElement element = ElementBuilder.customerElement(CustomerElementType.TKB_CARD)
        .id(TkbCardElementId.TKB)
        .elementParam(param)
        .build();
    return Collections.singletonList(element);
  }

  public List<IElement> buildRegulatorElements() {
    IdnTKBVO tkbVO = idnTkbDisplayService.getDisplayTkb();
    String tkb90 = IdnAppTkbDataProvider.getFormattedTKB(tkbVO != null && tkbVO.tkbNinety != null ? tkbVO.tkbNinety : "99", YqgLocale.INDONESIAN);

    List<CustomerElement.RegulatorCardParam.RegulatorItem> items = homepageV5Config.getRegulatorItems()
        .stream()
        .map(m -> CustomerElement.RegulatorCardParam.RegulatorItem.builder()
            .name(m.get("name"))
            .subtitle(m.get("subtitle"))
            .logoUrl(m.get("logoUrl"))
            .build())
        .collect(Collectors.toList());

    String desc = IdnAppTkbDataProvider.buildTkb90DetailDesc(
        tkbVO, cashLoanConfig.getIdnLoanQualityDefault(), YqgLocale.INDONESIAN);

    CustomerElement.RegulatorCardParam param = CustomerElement.RegulatorCardParam.builder()
        .tkb90Text(tkb90)
        .desc(desc)
        .items(items)
        .build();

    IElement element = ElementBuilder.customerElement(CustomerElementType.REGULATOR_CARD)
        .id(RegulatorCardElementId.REGULATOR_LIST)
        .elementParam(param)
        .build();
    return Collections.singletonList(element);
  }

  public List<IElement> buildMitraElements() {
    List<CustomerElement.MitraCardParam.MitraItem> items = homepageV5Config.getMitraItems()
        .stream()
        .map(m -> CustomerElement.MitraCardParam.MitraItem.builder()
            .name(m.get("name"))
            .logoUrl(m.get("logoUrl"))
            .build())
        .collect(Collectors.toList());

    CustomerElement.MitraCardParam param = CustomerElement.MitraCardParam.builder()
        .title("Mitra")
        .subtitle("Kami menjalin kolaborasi dengan instansi atau perusahaan lain.")
        .items(items)
        .build();

    IElement element = ElementBuilder.customerElement(CustomerElementType.MITRA_CARD)
        .id(MitraCardElementId.MITRA_LIST)
        .elementParam(param)
        .build();
    return Collections.singletonList(element);
  }
}
