package com.miyou.controllers.cashloan.utilities;

import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.AMOUNT_SLIDE_BAR;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.COUPON_STYLE_AB;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.LOAN_ACTIVITY_ORDER;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.LOAN_TERM_PERIOD_DISPLAY_STRATEGY;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.ORDER_GUIDE_ANIMATION_368;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.REJECT_LOAN_MARKET_INFO_CAN_INCREASE;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.REJECT_LOAN_MARKET_INFO_NOT_INCREASE;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.RELOAN_REJECTED_BUBBLE_GUIDE;
import static com.yqg.core.service.abtest.AbstractExpClient.BLANK_GROUP;
import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.miyou.controllers.apichannel.common.utils.ReloanRejectedBubbleVOConverter;
import com.miyou.controllers.cashloan.enums.IncreaseCreditsEntranceDisplayLocation;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.loanmarket.LoanMarketCardElementProvider;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.HomePageResponseBuildTool;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContext;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContextFactory;
import com.miyou.controllers.cashloan.repayment.card.element.provider.MainCardElementProvider;
import com.miyou.controllers.cashloan.repayment.card.element.provider.RepaymentCardElementProvider;
import com.miyou.controllers.cashloan.response.ExtendedTT;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementColor;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.color.MainCardBackgroundColorType;
import com.miyou.controllers.cashloan.response.v5.user.ActivityOrderInfoResponse;
import com.miyou.controllers.cashloan.response.v5.user.AnimationEffectResponse;
import com.miyou.controllers.cashloan.response.v5.user.CanOrderPageResponse;
import com.miyou.controllers.cashloan.response.v5.user.CardLabelEnum;
import com.miyou.controllers.cashloan.response.v5.user.HomePageGuideBubbleInfo;
import com.miyou.controllers.cashloan.response.v5.user.LoanMarketForOldHomePageResponse;
import com.miyou.controllers.cashloan.response.v5.user.MinimalistProcessIncreaseCreditsResponse;
import com.miyou.controllers.cashloan.response.v5.user.ReduceCreditsResponse;
import com.miyou.controllers.cashloan.response.v5.user.RepaymentResponse;
import com.miyou.controllers.cashloan.response.v5.user.ReserveLoanCardInfo;
import com.miyou.controllers.directdebit.DirectDebitAccountApiService;
import com.miyou.controllers.directdebit.response.DirectDebitAccountListResponse;
import com.miyou.controllers.directdebit.response.DirectDebitBankListResponse;
import com.miyou.controllers.directdebit.response.DirectDebitBankResponse;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.model.sql.signature.enums.SignatureProvider;
import com.yqg.core.model.sql.signature.enums.VidaSignatureDivisionStrategy;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.ExpFacade.ClientType;
import com.yqg.core.service.abtest.ExpLastResultRunningClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.bizcheck.signature.HandWrittenSignatureService;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.alert.FraudAlertService;
import com.yqg.core.service.cashloan.risk.AppListDialogSuppressService;
import com.yqg.core.service.cashloan.fee.enums.CalcFeeScale;
import com.yqg.core.service.cashloan.fee.enums.CalcFeeScaleMapper;
import com.yqg.core.service.cashloan.homepage.AppRegisterSwitchService;
import com.yqg.core.service.cashloan.homepage.enums.HomePageScene;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageDisplayStatusV5;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageActivityTool;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageParamTool;
import com.yqg.core.service.cashloan.homepage.utilities.StandardInterestUtil;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserCashLoanOrderContext;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.ordercenter.EcActivityOrderService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.cashloan.util.rate.ProductRateUtil;
import com.yqg.core.service.cashloan.vo.CreateOrderSignFormatConfigVO;
import com.yqg.core.service.cashloan.vo.CreateOrderSignFormatVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.directdebit.DirectDebitExperimentService;
import com.yqg.core.service.directdebit.enums.DirectDebitProvider;
import com.yqg.core.userflow.infrastructure.adapter.compare.OpAuthorizationCompareService;
import com.yqg.core.userflow.infrastructure.adapter.vo.DirectDebitAuthListCompareVO;
import com.yqg.core.userflow.infrastructure.adapter.vo.DirectDebitHomeEntranceCompareVO;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.vo.T0OrderPageInfoVO;
import com.yqg.core.service.loan.account.vo.VirtualIncreaseCreditsVO;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.credits.vo.HomePageCreditsChangeVO;
import com.yqg.core.service.loan.extrainfo.IncreaseReapplyExtraInfoService;
import com.yqg.core.userflow.domain.loan.model.discounts.OrderDiscounts;
import com.yqg.core.service.loan.repayment.experiment.RepaymentExperimentSupport;
import com.yqg.core.service.loan.repayment.education.RepayEducationSupport;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.loanmarket.LoanMarketOverdueExperimentDecisionService;
import com.yqg.core.service.loanmarket.LoanMarketUserQualifyService;
import com.yqg.core.service.loanmarket.config.LoanMarketConfig;
import com.yqg.core.service.loanmarket.enums.LoanMarketDisplayStrategy;
import com.yqg.core.service.loanmarket.enums.LoanMarketH5Strategy;
import com.yqg.core.service.loanmarket.enums.LoanMarketRiskFlowRuleScene;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.core.service.ly.LyConfig;
import com.yqg.core.service.ly.LyDemoUserService;
import com.yqg.core.service.sensors.SensorsService;
import com.yqg.core.userflow.domain.increasecredit.service.IIncreaseCreditService;
import com.yqg.core.util.common.NumberFormatter;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * V5版首页内容获取工具
 */
@Slf4j
@Component
public class HomepageContentTool {

  @Autowired
  private HomepageNoticeTool noticeTool;
  @Autowired
  private HomepageMiddleTool middleTool;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private StandardInterestUtil standardInterestUtil;
  @Autowired
  private ProductConfigService productConfigService;
  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private LoanMarketUserQualifyService loanMarketUserQualifyService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private LyDemoUserService lyDemoUserService;
  @Autowired
  private DirectDebitExperimentService directDebitExperimentService;
  @Autowired
  private HomepageCommonTool homepageCommonTool;
  @Autowired
  private HomePageTopAreaTool homePageTopAreaTool;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private HandWrittenSignatureService handWrittenSignatureService;
  @Autowired
  private EcActivityOrderService ecActivityOrderService;
  @Autowired
  private AppRegisterSwitchService appRegisterSwitchService;
  @Autowired
  private HomepageParamTool homepageParamTool;
  @Autowired
  private HomepageActivityTool homepageActivityTool;
  @Autowired
  private HomePageResponseBuildTool homePageResponseBuildTool;
  @Autowired
  private RepaymentContextFactory repaymentContextFactory;
  @Autowired
  private RepaymentCardElementProvider repaymentCardElementProvider;
  @Autowired
  private MainCardElementProvider mainCardElementProvider;
  @Autowired
  private ExpLastResultRunningClient expLastResultRunningClient;
  @Autowired
  private RepaymentExperimentSupport repaymentExperimentSupport;
  /** 按时还款教育展示支持。 */
  @Autowired
  private RepayEducationSupport repayEducationSupport;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private FraudAlertService fraudAlertService;
  @Autowired
  private LyConfig lyConfig;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private LoanMarketConfig loanMarketConfig;
  @Autowired
  private IIncreaseCreditService increaseCreditService;
  @Autowired
  private LoanMarketCardElementProvider loanMarketCardElementProvider;
  @Autowired
  private SensorsService sensorsService;
  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;
  @Autowired
  private IncreaseReapplyExtraInfoService reapplyService;
  @Autowired
  private DirectDebitAccountApiService directDebitAccountApiService;

  @Autowired
  private OpAuthorizationCompareService opAuthorizationCompareService;
  @Autowired
  private LoanMarketOverdueExperimentDecisionService loanMarketOverdueExperimentDecisionService;
  @Autowired
  private ExpFacade expFacade;
  @Autowired
  private AppListDialogSuppressService appListDialogSuppressService;

  public HomepageResponseV5 getHomepage(IDNHomepageLoanStatusV5 status, LoanApiViewerContext viewerContext, HomepageUserParamsVO paramsVO,
      String triggerSource, HttpServletRequest request) {
    HomepageResponseV5 homepageResponseV5 = homePageResponseBuildTool.buildResponse(status, viewerContext.getUserDeviceContextVO(),
        paramsVO, HomePageType.HOME_PAGE_FOR_LEVEL_1, HomePageScene.DEFAULT, triggerSource);
    if (lyDemoUserService.isDemoLoanUser(viewerContext.userId)) {
      return HomepageResponseV5.fromLyDemo(homepageResponseV5.userInfo, homepageResponseV5.product, homepageResponseV5.banner,
          homepageResponseV5.popupWindow);
    }
    try {
      boolean isNotVersionV3 =
          Objects.isNull(homepageResponseV5.getPageConfig()) || !Objects.equals(homepageResponseV5.getPageConfig().version,
              HomepageVersion.V3);
      if (isNotVersionV3) {
        // 头图逻辑非常复杂，前端也有相关的hack逻辑，不建议动
        homepageResponseV5.topArea = homePageTopAreaTool.getTopArea(status, paramsVO, homepageResponseV5.userInfo,
            homepageResponseV5.product);
      }
      if (homepageV5Config.oldHomepageMiddleStructSwitch()) {
        homepageResponseV5.middle = middleTool.getMiddle(status, viewerContext, paramsVO);
      }
      if (isNotVersionV3) {
        homepageResponseV5.notice = noticeTool.getNotice(status, viewerContext, paramsVO,
            homepageResponseV5.product.hasLowInterestProduct(), homepageResponseV5.userInfo, homepageResponseV5.topArea);
      }
    } catch (Exception e) {
      log.warn("getHomepage error, userId:{}, status:{}", viewerContext.userId, status, e);
    }
    return homepageResponseV5;
  }

  public ActivityOrderInfoResponse getActivityOrderInfoResponse(SourceType sourceType, LoanAccountVO loanAccountVO, Long build) {
    if (sourceType.isNotAppSourceType()) {
      return ActivityOrderInfoResponse.NOT_SHOW_INFO;
    }
    if (build < homepageV5Config.getActivityOrderInfoStartVersion() || !homepageV5Config.openLoanActivityOrder()) {
      return ActivityOrderInfoResponse.NOT_SHOW_INFO;
    }
    String result = expFacade.fetchResult(LOAN_ACTIVITY_ORDER, ABTestUtil.genDiversionKeyMapByUserId(loanAccountVO.userId), "FALSE");
    if (!Boolean.parseBoolean(result)) {
      return ActivityOrderInfoResponse.NOT_SHOW_INFO;
    }
    // 是否存在未结清的占坑订单
    if (ecActivityOrderService.countOrder(loanAccountVO.id, CashLoanOrderStatus.UNDONE_STATUSES) > 0) {
      return ActivityOrderInfoResponse.NOT_SHOW_INFO;
    }

    String amount = AmountFormatter.format(loanAccountVO.sdkType.getCurrency(), homepageV5Config.getActivityAwardAmount());

    Map<String, String> activityContentHighLightMap = Maps.newHashMap();
    activityContentHighLightMap.put(ActivityOrderInfoResponse.AWARD_AMOUNT, amount);
    Map<String, TT> agreementHighLightMap = Maps.newHashMap();
    homepageV5Config.getActivityHighLightMap().forEach((key, value) -> {
      agreementHighLightMap.put(key, TT.gen(value));
    });
    return ActivityOrderInfoResponse.showActivityOrderInfo(homepageV5Config.getActivityOrderAgreementLink(),
        TT.gen(homepageV5Config.getActivityOrderAgreementContent()), homepageV5Config.getActivityContentLink(),
        TT.gen(homepageV5Config.getActivityContent(), homepageV5Config.getActivityOrderDays()), agreementHighLightMap,
        activityContentHighLightMap);
  }

  /**
   * 3.49.23版本之后新的优惠券展示分流
   *
   * @param userId
   * @param build
   * @return
   */
  public HomeDisplayStrategy getDisplayCouponStrategy(Long userId, Long build) {
    if (!cashLoanConfig.getCouponSwitch()) {
      return null;
    }
    if (Objects.isNull(userId) || Objects.isNull(build) || build < cashLoanConfig.getCouponVersion()) {
      return null;
    }
    if (homepageV5Config.getDisplayCouponTipsStrategyStartVersion() <= build) {
      return HomeDisplayStrategy.C;
    }

    return HomeDisplayStrategy.A;
  }

  public ReserveLoanCardInfo buildReserveLoanCardInfo(HomeDisplayStrategy homeDisplayStrategy) {
    if (homeDisplayStrategy.isStrategyA()) {
      return null;
    }

    return ReserveLoanCardInfo.builder().title(TT.gen(homepageV5Config.getReserveLoanTitle()))
        .reserveLoanButtonJumpUrl(homepageV5Config.getReserveLoanUrl()).build();
  }

  public MinimalistProcessIncreaseCreditsResponse getMinimalistProcessIncreaseCreditsResponse(IDNHomepageLoanStatusV5 status,
      HomepageUserParamsVO paramsVO) {
    return homepageCommonTool.shouldDisplayIncreaseCreditsEntrance(IncreaseCreditsEntranceDisplayLocation.MINIMALIST_PROCESS, status,
        paramsVO.accountVO.userId, paramsVO.accountVO.id, paramsVO.build) ? MinimalistProcessIncreaseCreditsResponse.from(
        homepageV5Config.getMinimalistProcessUserIncreaseCreditsFrontContent(),
        homepageV5Config.getMinimalistProcessUserIncreaseCreditsBackendContent(), homepageV5Config.getIncreaseCreditsEntranceUrl(),
        homepageV5Config.getMinimalistProcessUserIncreaseCreditsIconUrl()) : null;
  }

  public String getMinimalistProcessUserAcceptBackgroundImageUrl(long build) {
    //旧版本
    if (build < homepageV5Config.getMinimalistProcessUserContentInfoV2Version()) {
      return homepageV5Config.getMinimalistProcessUserAcceptBackgroundImageUrl();
    }
    return homepageV5Config.getMinimalistProcessUserAcceptBackgroundImageUrlV2();
  }

  /**
   * 增加新注册用户展示虚拟提额的情况
   */
  public TT getCreditsTips(HomepageUserParamsVO paramsVO, SourceType sourceType, IDNHomepageLoanStatusV5 status, Long build) {
    if (paramsVO == null || paramsVO.accountVO == null) {
      return TT.gen(homepageV5Config.getCreditsTip(status.name()));
    }
    VirtualIncreaseCreditsVO virtualIncreaseCreditsVO = loanAccountService.getVirtualIncreaseCreditsVO(paramsVO.accountVO.userId,
        paramsVO.accountVO.sdkType, paramsVO.accountVO.timeCreated, sourceType, build);
    if (!virtualIncreaseCreditsVO.canShow) {
      return TT.gen(homepageV5Config.getCreditsTip(status.name()));
    }
    return getVirtualIncreaseCreditsContent(virtualIncreaseCreditsVO.remainHours);
  }

  public TT getVirtualIncreaseCreditsContent(int remainHours) {
    if (remainHours <= 1) {
      String lastHourContent = homepageV5Config.getVirtualIncreaseCreditsContentForLastHour();
      return TT.gen(lastHourContent);
    }
    String virtualIncreaseCreditsContent = homepageV5Config.getVirtualIncreaseCreditsContent();
    return TT.gen(virtualIncreaseCreditsContent, remainHours);
  }

  public String getDefaultProductPeriodContent(Long userId, Long build, PlatformType platformType) {
    if (userId != null && lyDemoUserService.isDemoUser(userId)) {
      return homepageV5Config.getLyProductPeriodContent();
    }
    if (appRegisterSwitchService.isRegisterUserNotEnough(build, platformType)) {
      return homepageV5Config.getRegisterUserNotEnoughPeriodText(platformType);
    }
    return homepageV5Config.getProductPeriodContent();
  }

  public CreateOrderSignFormatVO getOrderPageSignFormatVO(HomeDisplayStrategy homeDisplayStrategy,
      VidaSignatureDivisionStrategy signatureStrategy, Map<SignatureProvider, CreateOrderSignFormatConfigVO> configVOMap, Long userId,
      Long build) {
    if (homeDisplayStrategy == HomeDisplayStrategy.A) {
      return new CreateOrderSignFormatVO();
    }
    return handWrittenSignatureService.getOrderPageSignContent(signatureStrategy, configVOMap, userId, build);
  }

  public CanOrderPageResponse.AmountSlideBar getAmountSlideBar(Long userId, Long build) {
    HomeDisplayStrategy homeDisplayStrategy = getAmountSlideBarAbResult(userId, build);
    return CanOrderPageResponse.AmountSlideBar.from(homeDisplayStrategy, homepageV5Config.getAmountSlideBarTimeInterval());
  }

  public HomeDisplayStrategy getAmountSlideBarAbResult(Long userId, Long build) {
    if (build < homepageV5Config.getOrderPageAmountSlideBarVersion()) {
      return getCreditsAreaDisplayStrategy(userId, build);
    }
    return HomeDisplayStrategy.valueOf(expFacade.fetchResult(AMOUNT_SLIDE_BAR, ABTestUtil.genDiversionKeyMapByUserId(userId), "C"));
  }

  public HomeDisplayStrategy getCouponStyleAb(boolean havingAvailableCoupon, Long userId, Long build) {
    if (havingAvailableCoupon && build >= homepageV5Config.getCouponStyleStartVersion()) {
      return HomeDisplayStrategy.valueOf(expFacade.fetchResult(COUPON_STYLE_AB, ABTestUtil.genDiversionKeyMapByUserId(userId), "A"));
    }
    return HomeDisplayStrategy.A;
  }

  public HomeDisplayStrategy getCreditsAreaDisplayStrategy(Long userId, Long build) {
    if (build < homepageV5Config.getCreateOrderPageStyleStartBuild()) {
      return HomeDisplayStrategy.A;
    }
    return HomeDisplayStrategy.valueOf(
        expFacade.fetchResult(ABTestSceneType.CREATE_PAGE_CREDITS_AREA, ABTestUtil.genDiversionKeyMapByUserId(userId), "B"));
  }

  public TT getDiscountAmountFormat(BigDecimal amount, SDKType sdkType) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return null;
    }
    CalcFeeScale feeScale = CalcFeeScaleMapper.getScaleBySdk(sdkType);
    return TT.gen("Rp{0}",
        NumberFormatter.format(sdkType.getLocale(), amount.setScale(feeScale.interestScale, feeScale.interestRoundType)));
  }

  @NotNull
  public TT getInputLoanAmountContent(HomepageUserParamsVO paramsVO, HomeDisplayStrategy creditsAreaDisplayStrategy) {
    if (creditsAreaDisplayStrategy == HomeDisplayStrategy.B) {
      return TT.gen("借款金额");
    }
    if (isReduceCreditsUser(paramsVO)) {
      return TT.gen(homepageV5Config.getReduceCreditsInputLoanAmount());
    }
    return TT.gen("借款金额(Rp)");
  }

  public HomeDisplayStrategy getFeeDetailDisplayComplianceOptimizationStrategy(Long userId, PlatformType platform,
      List<LoanProductConfigVO> productConfigVOList) {
    if (userId == null) {
      return null;
    }
    //如果是H5用户，不分流，直接返回对照组A
    if (!platform.appPlatform()) {
      return HomeDisplayStrategy.A;
    }

    if (CollectionUtils.isEmpty(productConfigVOList)) {
      return HomeDisplayStrategy.A;
    }

    boolean hasLowInterestProduct = productConfigVOList.stream()
        .anyMatch(item -> ProductRateUtil.isLowInterestProduct(item, standardInterestUtil.getStandardInterestRate(userId)));

    //非低息用户返回B
    if (!hasLowInterestProduct) {
      return HomeDisplayStrategy.B;
    }
    //低息用户进行分流
    return HomeDisplayStrategy.A;
  }

  public HomeDisplayStrategy getExistFeeDetailDisplayComplianceOptimizationStrategy(Long userId, Long loanAccountId, Long build,
      SDKType sdkType) {
    HomepageUserParamsVO paramsVO = homepageParamTool.getHomepageV5ParamsVO(loanAccountId, build, sdkType);
    List<LoanProductConfigVO> productConfigVOList = paramsVO.productConfigList;
    //如果用户没产品，返回默认分流A
    if (CollectionUtils.isEmpty(productConfigVOList)) {
      return HomeDisplayStrategy.A;
    }
    boolean hasLowInterestProduct = productConfigVOList.stream()
        .anyMatch(item -> ProductRateUtil.isLowInterestProduct(item, standardInterestUtil.getStandardInterestRate(userId)));
    //非低息用户返回B
    if (!hasLowInterestProduct) {
      return HomeDisplayStrategy.B;
    }
    //低息用户查询这两个场景 当前实验已存在的分流结果/实验结束的分流结果，若都无结果，则返回默认分流A
    return HomeDisplayStrategy.A;
  }


  public RepaymentResponse getRepaymentInfo(HomePageContext homePageContext) {
    HomepageUserParamsVO paramsVO = homePageContext.getHomepageUserParamsVO();
    RepaymentContext ctx = repaymentContextFactory.buildRepaymentContext(homePageContext);
    if (!ctx.hasOutstanding()) {
      return null;
    }
    List<OrderInstalment> orderList = Optional.ofNullable(paramsVO.readyOrderList).orElse(new ArrayList<>());
    Map<String /* elementId */, IElement> repaymentCard = repaymentCardElementProvider.buildRepaymentCard(ctx).stream()
        .collect(Collectors.toMap(e -> e.getId().getCode(), e -> e));
    Map<String /* elementId */, IElement> mainCard = mainCardElementProvider.buildMainCard(ctx).stream()
        .collect(Collectors.toMap(e -> e.getId().getCode(), e -> e));

    MainCardBackgroundColorType mainCardBackgroundColorType = calculateMainCardBackgroundColorType(ctx);
    RepaymentResponse repaymentResponse = new RepaymentResponse().setTotalUnpaidAmount(ctx.totalUnpaidAmount).setRecentUnpaidAmount(ctx.recentUnpaidAmount)
        .setRecentUnpaidAmountDesc(paramsVO.newHomePageUI() ? TT.gen("最近应还金额（Rp）") : TT.gen("最近应还金额"))
        .setRecentlyBillingDate(ctx.recentlyBillingDate)
        .setRecentlyBillingDateDesc(paramsVO.newHomePageUI() ? TT.gen("账单日:{0}", ctx.recentlyBillingDateStr) : TT.gen("账单日"))
        .setTip(ctx.repaymentTip).setOverdue(paramsVO.isOverdue())
        .setOrderId(orderList.size() == 1 ? YqgHashids.encode(orderList.get(0).orderVO.id) : null).setRepaymentButtonContent(TT.gen("还款"))
        .setRepaymentEnhancementStrategy(repaymentExperimentSupport.getRepaymentReminderStrategy(paramsVO.getUserId(), paramsVO.build))
        .setRepaymentDisplayStatus(ctx.repaymentDisplayStatus).setSeriouslyOverdue(mainCardBackgroundColorType.isSeriouslyOverdue())
        .setMainCardBackgroundColorType(mainCardBackgroundColorType).setRepaymentCard(repaymentCard).setMainCard(mainCard);

    RepayEducationSupport.RepayEducationContext repayEducationContext =
        repayEducationSupport.resolveRepayEducationContext(homePageContext.getUserId(), paramsVO.build);
    if (repayEducationContext.isShouldShowMainFlow()) {
      repaymentResponse.setRepayTaskInfo(
          new RepaymentResponse.RepayTaskInfo(
              buildRepayTaskTitle(),
              repayEducationContext.getCurrentCompletedTimes(),
              repayEducationContext.getTargetCompletedTimes()));
    }
    return repaymentResponse;
  }

  /**
   * 组装首页按时还款任务标题；当前首页仅需稳定返回任务主标题，不复用资源位权益文案。
   *
   * @return 首页任务标题
   */
  private String buildRepayTaskTitle() {
    return homepageV5Config.getRepayEducationHomepageTitle();
  }

  private MainCardBackgroundColorType calculateMainCardBackgroundColorType(RepaymentContext ctx) {
    if (IDNHomepageDisplayStatusV5.REPAYMENT != ctx.homePageContext.getStatus().displayStatusV5) {
      return MainCardBackgroundColorType.GREEN;
    }
    if (ctx.repaymentReminderStrategy.isC()) {
      return ctx.repaymentDisplayStatus.isOverdue() ? MainCardBackgroundColorType.RED : MainCardBackgroundColorType.GREEN;
    }
    return ctx.repaymentDisplayStatus.isOverdueMoreThanOneDay() ? MainCardBackgroundColorType.RED : MainCardBackgroundColorType.GREEN;
  }

  public TT getDeductContent(LoanAccountVO accountVO, BigDecimal remainingCredit) {
    OrderDiscounts orderDiscounts = ecHomePageProductTool.getOrderDiscountsForOptimalCouponConsiderRemainCreditLimit(
        accountVO, remainingCredit, true);
    BigDecimal maxDeductAmount = orderDiscounts != null ? orderDiscounts.getCouponDiscountAmount() : BigDecimal.ZERO;
    if (maxDeductAmount.compareTo(BigDecimal.ZERO) != 0) {
      return TT.gen("现在借款最多可少还{0}利息", AmountFormatter.format(accountVO.sdkType.getCurrency(), maxDeductAmount));
    }
    return null;
  }

  /**
   * 首页展示代扣渠道（仅展示代扣渠道支持的、未授权的银行列表）
   */
  public DirectDebitBankListResponse getDirectDebitBankListResponse(Long userId, Long build, SDKType sdkType) {
    DirectDebitAccountListResponse directDebitAccountListResponse = directDebitAccountApiService.getDirectDebitAccountListResponse(userId, build, sdkType, false);
    if (directDebitAccountListResponse == null) {
      opAuthorizationCompareService.compareHomepageIfEnabled(userId, build, sdkType,
          DirectDebitHomeEntranceCompareVO.absent());
      return null;
    }
    // 当前已经授权过，不展示授权入口
    if (CollectionUtils.isNotEmpty(directDebitAccountListResponse.linkedAccountList)) {
      opAuthorizationCompareService.compareHomepageIfEnabled(userId, build, sdkType,
          DirectDebitHomeEntranceCompareVO.absent());
      return null;
    }
    List<DirectDebitBankResponse> supportBankList = filterHomepageSupportBankList(directDebitAccountListResponse.supportBankList);
    // 未授权代扣渠道为空返回null
    if (CollectionUtils.isEmpty(supportBankList)) {
      opAuthorizationCompareService.compareHomepageIfEnabled(userId, build, sdkType,
          DirectDebitHomeEntranceCompareVO.absent());
      return null;
    }
    DirectDebitBankListResponse response = DirectDebitBankListResponse.builder().supportBankList(supportBankList)
        .guidePageStrategy(directDebitExperimentService.fetchGuidePageResult(userId, build)).provider(directDebitAccountListResponse.provider)
        .build();
    opAuthorizationCompareService.compareHomepageIfEnabled(userId, build, sdkType, toHomeEntranceCompareVo(response));
    return response;
  }

  private static DirectDebitHomeEntranceCompareVO toHomeEntranceCompareVo(DirectDebitBankListResponse response) {
    if (response == null || CollectionUtils.isEmpty(response.supportBankList)) {
      return DirectDebitHomeEntranceCompareVO.absent();
    }
    List<String> supportKeys = response.supportBankList.stream()
        .map(bank -> DirectDebitAuthListCompareVO.bankKey(
            bank.provider == null ? null : bank.provider.name(), bank.bankValue))
        .sorted()
        .collect(Collectors.toList());
    return DirectDebitHomeEntranceCompareVO.ofPresent(supportKeys,
        response.provider == null ? null : response.provider.name(),
        response.guidePageStrategy == null ? null : response.guidePageStrategy.name());
  }

  /** 首页可展示的代扣银行：排除 DANA 渠道。 */
  private List<DirectDebitBankResponse> filterHomepageSupportBankList(List<DirectDebitBankResponse> supportBankList) {
    if (CollectionUtils.isEmpty(supportBankList)) {
      return supportBankList;
    }
    return supportBankList.stream()
        .filter(bank -> bank.provider != DirectDebitProvider.DANA)
        .collect(Collectors.toList());
  }


  public TT generateDisplayRateTitle(IDNHomepageLoanStatusV5 status, Long build, PlatformType platformType) {
    if (appRegisterSwitchService.isRegisterUserNotEnough(build, platformType)) {
      return TT.gen(homepageV5Config.getRegisterUserNotEnoughRateTitleText(platformType));
    }
    return TT.gen(homepageV5Config.getProductRateTitle(status.name()));
  }

  @Deprecated
  public TT generateDisplayRate(IDNHomepageLoanStatusV5 status, Long build, PlatformType platformType, String deviceToken) {
    List<String> whiteDeviceTokens = cashLoanConfig.getWhiteDeviceTokensForRate();
    boolean isWhiteDevice = whiteDeviceTokens.contains(deviceToken);
    if (appRegisterSwitchService.isRegisterUserNotEnough(build, platformType)) {
      return TT.gen(homepageV5Config.getRegisterUserNotEnoughRateText(platformType));
    }
    // 首贷未完件的用户走分流 不考虑重新申请的 只考虑第一次鉴权未走完的
    if (status == IDNHomepageLoanStatusV5.NEVER_APPLIED) {
      // 这个灰度有两个因子 按钮名 产品利率，这里读取产品利率
      String rateText = isWhiteDevice ? homepageV5Config.getProductRateContentForWhiteDevice(CardLabelEnum.EXP_B.name())
          : homepageV5Config.getProductRateContent(CardLabelEnum.EXP_B.name());
      return TT.gen(rateText);
    }
    // 默认按状态区分
    String rateText = isWhiteDevice ? homepageV5Config.getProductRateContentForWhiteDevice(status.name())
        : homepageV5Config.getProductRateContent(status.name());
    return TT.gen(rateText);
  }

  public BigDecimal getUserDisplayCredits(HomepageUserParamsVO paramsVO, SDKType sdkType) {
    BigDecimal totalRemainCredits = Objects.nonNull(paramsVO.creditsInfoVO) ? paramsVO.creditsInfoVO.totalRemainCredits : BigDecimal.ZERO;
    int diff = BigDecimalHelper.compareTo(totalRemainCredits, BigDecimal.ZERO);
    if (diff <= 0) {
      return productConfigService.getMaxCreditsBySDKOrZero(sdkType);
    }
    return paramsVO.remainingCredits;
  }

  public AnimationEffectResponse getCreditsChange(LoanUserCreditsInfoVO creditsInfoVO,
      HomeDisplayStrategy orderPageTempAmountTipDisplayStrategy, SourceType sourceType, Long userId, Long accountId) {
    if (SourceType.isApiChannelFromH5(sourceType)) {
      // api渠道h5没有提额
      return null;
    }
    if (RequestClientType.isWholeProcess(requestClientType()) && isInH5WholeProcessOrderUIExp(creditsInfoVO.userId)) {
      // h5全流程命中ui对齐下单跳转UI实验不显示提额入口
      return null;
    }
    //如果提额券有n笔可用限制，不考虑orderPageTempAmountTipDisplayStrategy分流，优先展示临时额度相关逻辑
    if (creditsInfoVO.tempCreditsOrderTimesLimitForCoupon != null || HomeDisplayStrategy.A == orderPageTempAmountTipDisplayStrategy) {
      HomePageCreditsChangeVO homePageCreditsChangeVO = loanUserCreditsService.getCreditsChangeInfo(creditsInfoVO);
      if (homePageCreditsChangeVO == null) {
        return null;
      }
      AnimationEffectResponse animationEffectResponse = new AnimationEffectResponse();
      animationEffectResponse.setShowAnimationEffect(true);
      animationEffectResponse.setLastCredit(homePageCreditsChangeVO.lastCredit);
      animationEffectResponse.setCurrentCredit(homePageCreditsChangeVO.currentCredit);
      animationEffectResponse.setIncreaseCreditContent(homePageCreditsChangeVO.increaseCreditContext);
      if (creditsInfoVO.tempCreditsOrderTimesLimitForCoupon != null) {
        animationEffectResponse.setIncreaseCreditSubContent(
            creditsInfoVO.tempCreditsOrderTimesLimitForCoupon == 1 ? TT.gen("请在本次借款中使用")
                : TT.gen("请在之后的{0}笔借款中使用", creditsInfoVO.tempCreditsOrderTimesLimitForCoupon));
      }
      return animationEffectResponse;
    }

    // 首贷回捞用户不展示增信提额入口
    if (!increaseCreditService.hideIncreaseCreditEntrance(userId, accountId)) {
      AnimationEffectResponse animationEffectResponse = new AnimationEffectResponse();
      animationEffectResponse.setShowAnimationEffect(true);
      animationEffectResponse.setTempQuotaPageUrl(homepageV5Config.getIncreaseCreditsJumpUrl());
      animationEffectResponse.setIncreaseCreditContent(TT.gen(homepageV5Config.getIncreaseCreditsContent()));
      return animationEffectResponse;
    }
    return null;
  }

  public ReduceCreditsResponse getReduceCreditsResponse(HomepageUserParamsVO paramsVO) {
    if (!isReduceCreditsUser(paramsVO)) {
      return null;
    }
    return new ReduceCreditsResponse().setReduceCreditContent(TT.gen(homepageV5Config.getReduceCreditsContent()))
        .setMaxCredits(paramsVO.creditsInfoVO.maxCredits).setRedirectUrl(homepageV5Config.getReduceCreditsRedirectUrl())
        .setReduceCreditDetailTitle(TT.gen(homepageV5Config.getReduceCreditsDetailTitle()))
        .setReduceCreditDetailContent(TT.gen(homepageV5Config.getReduceCreditsDetailContent()));
  }

  public boolean isReduceCreditsUser(HomepageUserParamsVO paramsVO) {
    return loanUserCreditsService.isIndonesianReloanReduceCredits(paramsVO.accountVO)
        && paramsVO.creditsInfoVO.maxCredits.compareTo(paramsVO.creditsInfoVO.creditsQuota) > 0;
  }

  //看是否有命中任何活动，如果有，就展示气泡
  public boolean needShowIncreaseCreditTaskBubbleContent(Long userId) {
    if (userId == null) {
      return false;
    }
    List<Long> activityIdList = homepageV5Config.getCreditsCouponActivityIdList();
    if (CollectionUtils.isEmpty(activityIdList)) {
      return false;
    }
    return homepageActivityTool.userEligibleAndNotCompleteTaskForActivity(userId, activityIdList);
  }

  public HomeDisplayStrategy getLoanTermDisplayStrategy(HomepageUserParamsVO paramsVO) {
    if (Objects.isNull(paramsVO) || !paramsVO.isProductConfigPeriodTermsConsistent()) {
      return HomeDisplayStrategy.A;
    }

    Long userId = paramsVO.getUserId();
    if (Objects.nonNull(userId) && paramsVO.build > cashLoanConfig.getLoanTermPeriodDisplayBuild()) {
      return HomeDisplayStrategy.valueOf(
          expFacade.fetchResult(LOAN_TERM_PERIOD_DISPLAY_STRATEGY, ABTestUtil.genDiversionKeyMapByUserId(paramsVO.getUserId()), "B"));
    }

    return HomeDisplayStrategy.A;
  }

  public Long getAppRefreshPopupWindowIntervalMilisecond(Long build) {
    if (build >= homepageV5Config.getAppRefreshPopupWindowNewStartVersion()) {
      return homepageV5Config.getAppRefreshPopupWindowIntervalMilisecondNew();
    }
    return homepageV5Config.getAppRefreshPopupWindowIntervalMilisecondOld();
  }

  public HomeDisplayStrategy getOrderPlanStyleFor363(Long userId, Long build) {
    if (Objects.isNull(userId) || build < homepageV5Config.getOrderPlanStyleFor363StartVersion()) {
      return HomeDisplayStrategy.A;
    }
    return HomeDisplayStrategy.valueOf(expFacade.fetchResult(ABTestSceneType.ORDER_PLAN_STYLE_FOR_363, ABTestUtil.genDiversionKeyMapByUserId(userId), "B"));
  }

  public HomeDisplayStrategy getReloanRejectedBubbleStrategy(Long userId, Long build) {
    if (build < homepageV5Config.getReloanRejectedBubbleGuideVersion()) {
      return HomeDisplayStrategy.A;
    }
    return HomeDisplayStrategy.valueOf(expFacade.fetchResult(RELOAN_REJECTED_BUBBLE_GUIDE, ABTestUtil.genDiversionKeyMapByUserId(userId), "B"));
  }

  public HomePageGuideBubbleInfo getHomePageGuideBubbleInfo(Long userId, Long build) {
    HomeDisplayStrategy homeDisplayStrategy = getReloanRejectedBubbleStrategy(userId, build);
    if (homeDisplayStrategy.isStrategyA()) {
      return null;
    }
    return ReloanRejectedBubbleVOConverter.convertToHomePageGuideBubbleInfo(homepageV5Config.getReloanRejectedBubbleVO());
  }

  public ExtendedTT getTempCreditsQuotaPrompt(LoanUserCreditsInfoVO creditsInfoVO, Long userId, SDKType sdkType,
      HomeDisplayStrategy homePage366Strategy, HomeDisplayStrategy amountSlideBarStrategy, T0OrderPageInfoVO t0OrderPageInfoVO) {
    
    ExtendedTT t0Result = processT0OrderPrompt(t0OrderPageInfoVO, sdkType);
    if (t0Result != null) {
      return t0Result;
    }
    
    if (!isValidStrategyForTempCredits(homePage366Strategy, amountSlideBarStrategy)) {
      return null;
    }
    
    return buildTempCreditsQuotaPrompt(creditsInfoVO, userId, sdkType);
  }

  /**
   * Process T0 order prompt based on order info and style
   */
  private ExtendedTT processT0OrderPrompt(T0OrderPageInfoVO t0OrderPageInfoVO, SDKType sdkType) {
    if (!isValidT0OrderInfo(t0OrderPageInfoVO)) {
      return null;
    }
    
    BigDecimal t0CreditIncreaseAmount = t0OrderPageInfoVO.getT0CreditIncreaseAmount();
    String formattedAmount = AmountFormatter.format(sdkType.getCurrency(), t0CreditIncreaseAmount);
    String style = t0OrderPageInfoVO.getAfRankT0Style();
    
    if ("mid".equals(style)) {
      return ExtendedTT.builder()
          .content(TT.gen("限时提额{0}"))
          .contentHighLightMap(ImmutableMap.of("{0}", formattedAmount))
          .build();
    } else if ("rest".equals(style)) {
      return ExtendedTT.builder()
          .content(TT.gen("提额 {0} 只有一次 只限今天"))
          .contentHighLightMap(ImmutableMap.of("{0}", formattedAmount))
          .build();
    }else {
      return null;
    }
  }
  
  /**
   * Check if T0 order info is valid for processing
   */
  private boolean isValidT0OrderInfo(T0OrderPageInfoVO t0OrderPageInfoVO) {
    if (Objects.isNull(t0OrderPageInfoVO) || !t0OrderPageInfoVO.isShow()) {
      return false;
    }
    
    BigDecimal t0CreditIncreaseAmount = t0OrderPageInfoVO.getT0CreditIncreaseAmount();
    return t0CreditIncreaseAmount != null && t0CreditIncreaseAmount.compareTo(BigDecimal.ZERO) > 0;
  }
  
  /**
   * Validate if the strategies are correct for showing temp credits
   */
  private boolean isValidStrategyForTempCredits(HomeDisplayStrategy homePage366Strategy, HomeDisplayStrategy amountSlideBarStrategy) {
    return homePage366Strategy.isStrategyB() && HomeDisplayStrategy.C.equals(amountSlideBarStrategy);
  }
  
  /**
   * Build temp credits quota prompt with validation
   */
  private ExtendedTT buildTempCreditsQuotaPrompt(LoanUserCreditsInfoVO creditsInfoVO, Long userId, SDKType sdkType) {
    BigDecimal threshold = new BigDecimal("100000");
    if (creditsInfoVO.tempCreditsForCoupon.compareTo(threshold) < 0) {
      return null;
    }
    
    Long remainingDays = creditsInfoVO.getTempCreditsCouponRemainingDays();
    if (Objects.isNull(remainingDays)) {
      return null;
    }
    
    String amount = AmountFormatter.format(creditsInfoVO.currency, creditsInfoVO.tempCreditsForCoupon);
    log.info("[QuotaPrompt]userId = {}, amount = {}, remainingDays = {}", userId, amount, remainingDays);
    
    return ExtendedTT.builder()
        .content(TT.gen("有{0}的临时额度，将在{1}后失效！"))
        .contentHighLightMap(ImmutableMap.of("{0}", amount, "{1}", remainingDays + TT.gen("天").toString(sdkType.getLocale().locale)))
        .build();
  }

  /**
   * 贷超结果后置处理
   */
  public LoanMarketUserQualifyCheckResult postProcessLoanMarketCheckResult(HomePageContext homePageContext, boolean canReapply) {
    return postProcessLoanMarketCheckResult(homePageContext, canReapply, false);
  }

  /**
   * 贷超结果后置处理（支持跳过实验与版本判断，供策略等复用）
   *
   * @param skipExperimentAndVersion true 时仅用 context 中风控结果，跳过版本与 AB 实验
   */
  public LoanMarketUserQualifyCheckResult postProcessLoanMarketCheckResult(HomePageContext homePageContext, boolean canReapply,
      boolean skipExperimentAndVersion) {
    LoanMarketUserQualifyCheckResult checkResult = homePageContext.getUserCreditsContext().getLoanMarketUserQualifyCheckResult();
    Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
    if (isWholeProcess) {
      return checkResult.mutate().qualifiedLoanMarketEntranceByRisk(false).build();
    }
    // Live Demo 纯素合规：命中抑制开关时首页贷超入口/卡片一律不下发
    if (shouldSuppressLoanMarketForLiveDemo(homePageContext)) {
      return checkResult.mutate().qualifiedLoanMarketEntranceByRisk(false).build();
    }
    if (skipExperimentAndVersion) {
      return checkResult;
    }
    Long build = homePageContext.getUserDeviceContextVO().getBuild();
    if (build < homepageV5Config.getRejectLoanMarketInfoStartVersion()) {
      return checkResult.mutate().qualifiedLoanMarketEntranceByRisk(false).build();
    }
    String result = expFacade.fetchResult(
        canReapply ? "braavos-other-abroad-loan_all-loanmarketcanretry90" : "braavos-other-abroad-loan_all-loanmarketcannotretry90",
        ClientType.DIVERSION, canReapply ? REJECT_LOAN_MARKET_INFO_CAN_INCREASE : REJECT_LOAN_MARKET_INFO_NOT_INCREASE,
        ABTestUtil.genDiversionKeyMapByUserId(homePageContext.getUserId()));
    HomeDisplayStrategy homeDisplayStrategy = HomeDisplayStrategy.valueOf(result);
    if (homeDisplayStrategy.isStrategyA()) {
      return checkResult.mutate().qualifiedLoanMarketEntranceByRisk(false).build();
    }
    loanMarketUserQualifyService.doDwlogForLoanMarketResult(LoanMarketRiskFlowRuleScene.HOME_PAGE,
        checkResult.qualifiedLoanMarketEntranceByRisk, checkResult, homePageContext.getStatus(), build,
        homePageContext.getUserDeviceContextVO().getSourceType());
    log.info("LoanMarketUserQualifyCheckResult result:{}, userId:{}, loanAccountId:{}", JsonUtils.toString(checkResult),
        Objects.nonNull(homePageContext.getUserId()) ? homePageContext.getUserId() : null,
        Objects.nonNull(homePageContext.getLoanAccountId()) ? homePageContext.getLoanAccountId() : null);
    return checkResult;
  }

  /**
   * Live Demo 开关命中时抑制首页贷超（卡片「为您精选的贷款产品」、主卡入口、中通位等共用本后置处理）。
   */
  private boolean shouldSuppressLoanMarketForLiveDemo(HomePageContext homePageContext) {
    if (homePageContext == null || homePageContext.getUserDeviceContextVO() == null) {
      return false;
    }
    return appListDialogSuppressService.shouldSuppress(
        homePageContext.getUserId(),
        homePageContext.getUserDeviceContextVO().getDeviceToken(),
        homePageContext.getUserDeviceContextVO().getBuild());
  }

  public HomeDisplayStrategy getStyleFor367(Long userId, Long build, int productCount) {
    if (build < homepageV5Config.getHome367DisplayBuild()) {
      return HomeDisplayStrategy.A;
    }
    if (productCount < 2) {
      return HomeDisplayStrategy.B;
    }
    return HomeDisplayStrategy.valueOf(
        expFacade.fetchResult(ABTestSceneType.PRODUCT_STYLE_FOR_367, ABTestUtil.genDiversionKeyMapByUserId(userId), "B"));
  }

  public boolean canShowOrderGuideAnimation(boolean newHomeUI, Long userId, Long build) {
    if (!newHomeUI) {
      return false;
    }
    if (build < homepageV5Config.getShowOrderGuideAnimationVersion()) {
      return false;
    }
    return BooleanUtils.toBoolean(
        expFacade.fetchResult("test_run-not_withdraw-abroad-loan_all-ORDER_GUIDE_ANIMATION_368", ClientType.DIVERSION,
            ORDER_GUIDE_ANIMATION_368, ABTestUtil.genDiversionKeyMapByUserId(userId)));
  }

  public Integer showOrderGuideAnimationTimes() {
    return homepageV5Config.getShowOrderGuideAnimationTimes();
  }


  public boolean isShowReplyPopup(Long userId, Long build) {
    if (Objects.isNull(userId) || Objects.isNull(build)) {
      return false;
    }
    return lyConfig.getLyConfirmPiplaySet().contains(userId) && build >= lyConfig.getH5BuildVersion();
  }

  public Boolean isInH5WholeProcessOrderUIExp(Long userId) {
    String result = expLastResultRunningClient.getResult("technology-lending-abroad-loan_all-h5_ui_new_order_1215",
        ExpUser.builder().userId(userId).versionBuild(ImpliedContextUtils.build()).build());
    return !BLANK_GROUP.equals(result) && StringUtils.equals(CommonABTestResultGroup.B.name(), result);
  }

  public TT getInstalmentListPageFraudAlert(Long userId) {
    return fraudAlertService.processFraudAlertWith1To8DaysWindow(userId);
  }

  public boolean isNotQualifiedToReapply(HomePageContext homePageContext) {
    UserCreditsContext userCreditsContext = homePageContext.getUserCreditsContext();
    Long userId = homePageContext.getUserId();
    if (userCreditsContext.getCreditsInfoVO().creditsStatus == LoanCreditsStatus.CANCELLED) {
      return true;
    }
    return reapplyService.isNotQualifiedToReapply(userId, homePageContext.getLoanAccountId());
  }

  public LoanMarketDisplayStrategy getLoanMarketCardDisplayResult(Long userId, Long build, SourceType sourceType) {
    String result = expDiversionClient.getString(loanMarketConfig.getLoanMarketDisplayStretegyExprKey(),
        ExpUser.builder().sourceType(sourceType).userId(userId).versionBuild(build).build(), LoanMarketDisplayStrategy.NONE.name());
    return LoanMarketDisplayStrategy.valueOf(result);
  }

  public boolean getLoanMarketDisplayInH5Result(Long userId, Long build, SourceType sourceType) {
    String result = expDiversionClient.getString(loanMarketConfig.getLoanMarketDisplayInH5ExprKey(),
        ExpUser.builder().sourceType(sourceType).userId(userId).versionBuild(build).build(),
        LoanMarketH5Strategy.LOAN_MARKET_NATIVE.name());
    return LoanMarketH5Strategy.valueOf(result).displayInH5();
  }

  private LoanMarketForOldHomePageResponse buildOverdueLoanMarketResponse(HomePageContext homePageContext,
      boolean displayInH5, LoanMarketDisplayStrategy displayStrategy) {
    Map<String, IElement> elementMap = loanMarketCardElementProvider.buildLoanMarketCard(homePageContext, null).stream()
        .collect(Collectors.toMap(e -> e.getId().getCode(), e -> e));
    if (homePageContext.loanMarketCardReported.compareAndSet(false, true)) {
      sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.cardParam(homePageContext));
      loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
          homePageContext, LoanMarketReportType.CARD, getClass());
    }
    return LoanMarketForOldHomePageResponse.build(elementMap, displayInH5, displayStrategy,
        loanMarketConfig.getLoanMarketDisplayH5UrlPrefix(), loanMarketConfig.getLoanMarketH5CardHeight());
  }

  public boolean checkUserOverdueDaysWithHistory(HomePageContext homePageContext) {
    return checkUserOverdueDaysWithHistory(homePageContext, false);
  }

  public boolean checkUserOverdueDaysWithHistory(HomePageContext homePageContext, boolean skipExperimentAndVersion) {
    if (!skipExperimentAndVersion
        && homePageContext.getUserDeviceContextVO().getBuild() < loanMarketConfig.getLoanMarketOverdueDisplayExprStartBuild()) {
      return false;
    }
    int overdueDays = loanMarketConfig.getLoanMarketOverdueExpandDays();
    return ecOrderService.hasHistoricalOverdueInstalmentWithDays(homePageContext.getUserId(), overdueDays);
  }

  //V2Response组装方法
  public LoanMarketForOldHomePageResponse buildLoanMarketForOldHomePageResp(HomePageContext homePageContext,
      Boolean showLoanMarketEntrance) {
    //风控决定贷超入口是否展示
    if (!showLoanMarketEntrance) {
      return null;
    }
    // 标准在还（有在还订单且当前分期未逾期）：不组装旧版贷超卡片，避免走非逾期贷超实验与曝光打点；逾期在还仍走下方逾期分支
    if (suppressOldHomePageLoanMarketForNonOverdueRepayment(homePageContext)) {
      return null;
    }
    // 逾期用户场景，注意和非逾期用户走的是不同实验，需要兜底return，否则会影响后面非逾期用户的进组
    if (homePageContext.getHomepageUserParamsVO().isOverdue()) {
      // 在贷&有逾期,需要逾期天数(当前+历史)&版本满足限制
      if (!checkUserOverdueDaysWithHistory(homePageContext)) {
        return null;
      }
      Long userId = homePageContext.getUserId();
      Long build = homePageContext.getUserDeviceContextVO().getBuild();
      SourceType sourceType = homePageContext.getUserDeviceContextVO().getSourceType();
      // 贷超入口拓展逾期实验分流
      boolean shownLoanMarket = loanMarketOverdueExperimentDecisionService.decide(homePageContext.getUserId(),
          homePageContext.getUserDeviceContextVO().getBuild(),
          homePageContext.getUserDeviceContextVO().getSourceType());
      if (!shownLoanMarket) {
        return null;
      }
      boolean displayInH5 = build != null && build >= 38410L;
      return buildOverdueLoanMarketResponse(homePageContext, displayInH5, LoanMarketDisplayStrategy.LOAN_MARKET_LIST_BANNER_DOWN);
    }

    //无在贷场景+永拒
    //版本控制
    if (homePageContext.getUserDeviceContextVO().getBuild() < loanMarketConfig.getLoanMarketDisplayExprStartBuild()) {
      return null;
    }

    LoanMarketDisplayStrategy strategy = getLoanMarketCardDisplayResult(homePageContext.getUserId(),
        homePageContext.getUserDeviceContextVO().getBuild(), homePageContext.getUserDeviceContextVO().getSourceType());
    if (!strategy.strategyInExprGroup()) {
      return null;
    }
    boolean displayInH5 = getLoanMarketDisplayInH5Result(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild(),
        homePageContext.getUserDeviceContextVO().getSourceType());
    Map<String /* elementId */, IElement> elementMap = loanMarketCardElementProvider.buildLoanMarketCard(homePageContext,
        ElementColor.GREEN).stream().collect(Collectors.toMap(e -> e.getId().getCode(), e -> e));
    if (homePageContext.loanMarketCardReported.compareAndSet(false, true)) {
      sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.cardParam(homePageContext));
      loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
          homePageContext, LoanMarketReportType.CARD, getClass());
    }
    return LoanMarketForOldHomePageResponse.build(elementMap, displayInH5, strategy, loanMarketConfig.getLoanMarketDisplayH5UrlPrefix(),
        loanMarketConfig.getLoanMarketH5CardHeight());
  }

  public Boolean showLoanMarketEntranceInMainCard(HomePageContext homePageContext, Boolean showLoanMarketEntrance) {
    return showLoanMarketEntranceInMainCard(homePageContext, showLoanMarketEntrance, false);
  }

  /**
   * 主卡片是否展示贷超入口（支持跳过实验与版本判断）
   *
   * @param skipExperimentAndVersion true 时仅按 showLoanMarketEntrance 返回，不再做实验/版本校验
   */
  public Boolean showLoanMarketEntranceInMainCard(HomePageContext homePageContext, Boolean showLoanMarketEntrance,
      boolean skipExperimentAndVersion) {
    //如果本身就不该展示，返回false
    if (!showLoanMarketEntrance) {
      return false;
    }
    return checkUserNotInLoanMarketCardExpr(homePageContext, skipExperimentAndVersion);
  }

  public Boolean checkUserNotInLoanMarketCardExpr(HomePageContext homePageContext) {
    return checkUserNotInLoanMarketCardExpr(homePageContext, false);
  }

  /**
   * 是否不在贷超卡片实验组（即主卡片可展示贷超链接）。支持跳过实验与版本判断。
   *
   * @param skipExperimentAndVersion true 时视为“可展示主卡贷超”，直接返回 true
   */
  public Boolean checkUserNotInLoanMarketCardExpr(HomePageContext homePageContext, boolean skipExperimentAndVersion) {
    if (skipExperimentAndVersion) {
      return true;
    }
    //版本控制，低于版本直接返回结果
    if (homePageContext.getUserDeviceContextVO().getBuild() < loanMarketConfig.getLoanMarketDisplayExprStartBuild()) {
      return true;
    }
    //如果下方贷超卡片展示，则主卡片不展示
    LoanMarketDisplayStrategy strategy = getLoanMarketCardDisplayResult(homePageContext.getUserId(),
        homePageContext.getUserDeviceContextVO().getBuild(), homePageContext.getUserDeviceContextVO().getSourceType());
    return !strategy.strategyInExprGroup();
  }

  /**
   * 标准在还：存在在还订单，且按 {@link com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment#isOverdue()} 当前无逾期分期。
   */
  private boolean suppressOldHomePageLoanMarketForNonOverdueRepayment(HomePageContext homePageContext) {
    UserCashLoanOrderContext orderContext = homePageContext.getUserCashLoanOrderContext();
    if (orderContext == null) {
      return false;
    }
    return orderContext.hasReadyOrders() && !homePageContext.getHomepageUserParamsVO().isOverdue();
  }
}
