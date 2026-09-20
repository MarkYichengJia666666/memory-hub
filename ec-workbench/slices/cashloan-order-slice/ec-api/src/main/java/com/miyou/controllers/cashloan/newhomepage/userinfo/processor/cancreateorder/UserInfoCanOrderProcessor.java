package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.cancreateorder;

import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.COUPON_STYLE_AB;
import static com.yqg.core.model.sql.abtest.enums.ABTestSceneType.CUT_INTEREST_POP_UP;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.CreateOrderSignFormatResponse;
import com.miyou.controllers.cashloan.response.v5.user.CanOrderPageResponse;
import com.miyou.controllers.cashloan.response.v5.user.DiscountDetailAnimationResponse;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.ReduceCreditsResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.signature.enums.SignatureProvider;
import com.yqg.core.model.sql.signature.enums.VidaSignatureDivisionStrategy;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.ABTestVersionConfigService;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.bizcheck.signature.HandWrittenSignatureService;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.JbpConfig;
import com.yqg.core.service.cashloan.homepage.abtest.OrderPageUserIdlePopupService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageContextHolderUtil;
import com.yqg.core.service.cashloan.homepage.utilities.StandardInterestUtil;
import com.yqg.core.service.cashloan.homepage.vo.DiscountDetailAnimationVO;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.UserProductDetailVO;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.CashLoanOrderLimitService;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.vo.UserOrderLimitVO;
import com.yqg.core.service.cashloan.vo.CreateOrderSignFormatConfigVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.jbp.blackcard.BlackCardDemoService;
import com.yqg.core.service.jbp.vo.BlackCardAbResultVO;
import com.yqg.core.service.loan.account.T0LoanAcceptService;
import com.yqg.core.service.loan.account.vo.T0OrderPageInfoVO;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.userflow.domain.loan.model.discounts.OrderDiscounts;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.util.common.NumberFormatter;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserInfoCanOrderProcessor extends AbstractUserInfoProcessor {
  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private OrderPageUserIdlePopupService orderPageUserIdlePopupService;
  @Autowired
  private CashLoanOrderLimitService cashLoanOrderLimitService;
  @Autowired
  private HandWrittenSignatureService handWrittenSignatureService;
  @Autowired
  private HomepageContextHolderUtil homepageContextHolderUtil;
  @Autowired
  private LoanBankAccountService loanBankAccountService;
  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private BlackCardDemoService blackCardDemoService;
  @Autowired
  private ABTestVersionConfigService abTestVersionConfigService;
  @Autowired
  private StandardInterestUtil standardInterestUtil;
  @Autowired
  private JbpConfig jbpConfig;
  @Autowired
  private T0LoanAcceptService t0LoanAcceptService;
  @Autowired
  private ExpFacade expFacade;
  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    BigDecimal standardInterestRateVal = standardInterestUtil.executeStandardInterestAbTest(homePageContext.getUserId());
    UserProductDetailVO userProductVO = homePageContext.getUserProductVO();
    HomepageV5Config homepageV5Config = homePageContext.getHomePageServiceManager().getHomepageV5Config();
    LoanAccountVO accountVO = homePageContext.getLoanAccountVO();
    UserDeviceContextVO userDeviceContextVO = homePageContext.getUserDeviceContextVO();
    LoanProductConfigVO loanProductConfigVO = ecHomePageProductTool.routeChooseDefaultOrMaxProduct(homePageContext.getUserId(),homePageContext.getSdkType(),homePageContext.getUserProductVO().getEnableVirtualCredits());
    T0OrderPageInfoVO t0OrderPageInfoVO = null;
    if (homePageContext.homePageLevel2()) {
      // ⚠️ 必须在 getT0OrderPageInfo 之前触发：compute() 会写入 popupSessionDecisionCache，
      // getT0OrderPageInfo 依赖该缓存判断下单页样式。因为 FirstLoanPopupDecisionSnapshot 是懒加载的，
      // 且由其他 processor 并行消费，此处显式调用以保证缓存已就绪。请勿删除。
      homePageContext.getFirstLoanPopupDecisionSnapshot();
      t0OrderPageInfoVO = t0LoanAcceptService.getT0OrderPageInfo(homePageContext.getUserId(),homePageContext.getSdkType(),loanProductConfigVO.getId(),userProductVO.getMaxAmountForVirtual(),userDeviceContextVO.getBuild(), null, true);
    }
    boolean hasLowInterestProduct = userProductVO.hasLowInterestProduct();
    boolean hasIrregularProduct = userProductVO.hasIrregularProduct();

    boolean havingAvailableCoupon = loanUserCouponService.havingAvailableCoupon(homePageContext.getUserId());
    BigDecimal lowInterestDiscountAmount = BigDecimal.ZERO;
    BigDecimal discountAmount = BigDecimal.ZERO;
    if (hasLowInterestProduct) {
      lowInterestDiscountAmount = Optional.ofNullable(ecHomePageProductTool.getLowInterestDiscountAmount(userProductVO.getEnableVirtualCredits(),
              userDeviceContextVO.getPlatformType(), userDeviceContextVO.getBuild(), homePageContext.getUserId(), homePageContext.getSdkType()))
          .orElse(BigDecimal.ZERO);
      discountAmount = lowInterestDiscountAmount;
    } else if (havingAvailableCoupon) {
      discountAmount = Optional.ofNullable(ecHomePageProductTool.getOrderDiscountsForOptimalByEnableCredits(
              userProductVO.getEnableVirtualCredits(), homePageContext.getUserId(), homePageContext.getSdkType()))
          .map(OrderDiscounts::getCouponDiscountAmount)
          .orElse(BigDecimal.ZERO);
    }

    UserOrderLimitVO userOrderLimitVO = cashLoanOrderLimitService.getOrderLimitVOBySourceTypeAndBuild(homePageContext.getLoanAccountId(),
        userDeviceContextVO.getBuild(), userDeviceContextVO.getSourceType(), homePageContext.getStatus());

    ReduceCreditsResponse reduceCreditsResponse = null;
    if (homePageContext.getStatus() != IDNHomepageLoanStatusV5.LOAN_CREDITS_DECREASE
        && homePageContext.getStatus() != IDNHomepageLoanStatusV5.RELOAN_CREDITS_DECREASE) {
      reduceCreditsResponse = homePageContext.getLoanAccountVO().firstLoan() ?
          null : homepageContentTool.getReduceCreditsResponse(homePageContext.getHomepageUserParamsVO());
    }

    HomeDisplayStrategy creditsAreaDisplayStrategy = homepageContentTool.getCreditsAreaDisplayStrategy(homePageContext.getUserId(),
        userDeviceContextVO.getBuild());
    HomeDisplayStrategy loanTermDisplayStrategy = homepageContentTool.getLoanTermDisplayStrategy(homePageContext.getHomepageUserParamsVO());
    if (userDeviceContextVO.getBuild() >= homepageV5Config.getCutInterestPopupStartVersion() && userProductVO.isAllLowInterestProduct()) {
      expFacade.fetchResult(CUT_INTEREST_POP_UP, ABTestUtil.genDiversionKeyMapByUserId(homePageContext.getUserId()), "A");
    }
    HomeDisplayStrategy couponStyleAb = HomeDisplayStrategy.A;
    if (havingAvailableCoupon && userDeviceContextVO.getBuild() >= homepageV5Config.getCouponStyleStartVersion()) {
      couponStyleAb = HomeDisplayStrategy.valueOf(
          expFacade.fetchResult(COUPON_STYLE_AB, ABTestUtil.genDiversionKeyMapByUserId(homePageContext.getUserId()), "A"));
    }

    DiscountDetailAnimationVO discountDetailAnimationVO =
        homepageContextHolderUtil.getAndSetDiscountDetailAnimationVO(homePageContext.getHomePageContextHolder(), homePageContext.getLoanAccountVO(), userDeviceContextVO.getBuild(), userProductVO, userDeviceContextVO.getPlatformType(), homePageContext.getUserCreditsContext().getCreditsInfoVO().tempCreditsOrderTimesLimitForCoupon)
            .getDiscountDetailAnimationVO();

    HomeDisplayStrategy home366DisplayStrategy = home366DisplayStrategy(homePageContext);
    LoanUserCreditsInfoVO creditsInfoVO = Optional.ofNullable(homePageContext.getUserCreditsContext()).map(UserCreditsContext::getCreditsInfoVO).orElse(new LoanUserCreditsInfoVO());
    HomeDisplayStrategy revolvingReviewDisplayStrategy = homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy;

    HomePageMainCardInfo canOrderHomePageMainCardInfo = homePageMainCardInfoTool.getCanOrderHomePageMainCardInfo(homePageContext.newHomePageUI(),
        homePageContext.getUserId(), userProductVO.getEnableVirtualCredits(),
        homePageContext.getSdkType(), userProductVO.getMinActualInterestRate(),
        discountAmount, home366DisplayStrategy, userProductVO, !homePageContext.homePageLevel2(), creditsInfoVO,
        revolvingReviewDisplayStrategy.isStrategyB());
    TT scrollLoanAmountContent = homepageContentTool.isReduceCreditsUser(homePageContext.getHomepageUserParamsVO()) ? TT.gen(homePageContext.getHomePageServiceManager().getHomepageV5Config().getReduceCreditsScrollLoanAmount()) : TT.gen("借款金额");
    TT standardInterestRate = TT.gen("{0}/day", NumberFormatter.percentFormat(homePageContext.getSdkType().getLocale(), standardInterestRateVal));
    TT standardInterestRateFormat = TT.gen("{0}", NumberFormatter.percentFormat(homePageContext.getSdkType().getLocale(), standardInterestRateVal));

    VidaSignatureDivisionStrategy signatureDivisionStrategy = handWrittenSignatureService.fetchVidaSignatureStrategyByDivision(homePageContext.getUserId());
    HomeDisplayStrategy orderPageSignStrategy = handWrittenSignatureService.getSignFormatStrategy(userDeviceContextVO.getSourceType(), homePageContext.getUserId(), userDeviceContextVO.getBuild(), signatureDivisionStrategy);

    HomeDisplayStrategy productStyleFor367 = homepageContentTool.getStyleFor367(homePageContext.getUserId(), userDeviceContextVO.getBuild(), userProductVO.getProductIds().size());
    CanOrderPageResponse.AmountSlideBar amountSlideBar = homepageContentTool.getAmountSlideBar(homePageContext.getUserId(), userDeviceContextVO.getBuild());
    BlackCardAbResultVO blackCardAbResultVO = blackCardDemoService.getBlackCardAbResultVO(homePageContext.getUserId(), userDeviceContextVO.getBuild());
    Map<SignatureProvider, CreateOrderSignFormatConfigVO> createOrderSignFormatConfigMap =
        SourceType.isApiChannelFromH5(userDeviceContextVO.getSourceType()) ?
            homepageV5Config.getCreateOrderSignFormatConfigMapForH5() :
            homepageV5Config.getCreateOrderSignFormatConfigMap();
    Map<SignatureProvider, CreateOrderSignFormatConfigVO> dialogSignFormatConfigMap =
        SourceType.isApiChannelFromH5(userDeviceContextVO.getSourceType()) ?
            homepageV5Config.getDialogSignFormatConfigMapForH5() :
            homepageV5Config.getDialogSignFormatConfigMap();
    if(RequestClientType.isWholeProcess(userDeviceContextVO.getClientType()) && homepageContentTool.isInH5WholeProcessOrderUIExp(homePageContext.getUserId())){
      //命中H5全流程下单对齐UI实验，跟api渠道保持一致，仅有文件域名不同。
      createOrderSignFormatConfigMap = homepageV5Config.getCreateOrderSignFormatConfigMapForH5();
      dialogSignFormatConfigMap = homepageV5Config.getDialogSignFormatConfigMapForH5();
    }
    userResponse
        .setTempCredit(creditsInfoVO.tempCredits)
        .setTempCreditFromCoupon(creditsInfoVO.tempCreditsForCoupon)
        .setDisplayButtonAnimation(false)
        .setRemainingCredit(userProductVO.getEnableVirtualCredits())
        .setReduceCreditsResponse(reduceCreditsResponse)
        .setNeedOrderPageUserIdlePopup(orderPageUserIdlePopupService.getNeedOrderPageUserIdlePopupV1(homePageContext.getUserId(), userDeviceContextVO.getBuild(), havingAvailableCoupon))
        .setActivityOrderInfoResponse(homepageContentTool.getActivityOrderInfoResponse(userDeviceContextVO.getSourceType(), homePageContext.getLoanAccountVO(), userDeviceContextVO.getBuild()))
        .setOrderLimitInfo(userOrderLimitVO)
        .setFirstLoan(Optional.ofNullable(homePageContext.getLoanAccountVO()).map(LoanAccountVO::firstLoan).orElse(true))
        .setTotalCredit(creditsInfoVO.totalCredits)
        .setHasCornerMark(false)
        .setTempRemainingTime(creditsInfoVO.tempCreditsExpiredTime == null ? null : creditsInfoVO.tempCreditsExpiredTime - Clock.now())
        .setStandardInterestRate(standardInterestRate)
        .setStandardInterestRateFormat(standardInterestRateFormat)
        .setDisplayCouponStrategy(homepageContentTool.getDisplayCouponStrategy(homePageContext.getUserId(), userDeviceContextVO.getBuild()))
        .setOrderPageTempAmountTipDisplayStrategy(discountDetailAnimationVO.orderPageTempAmountTipDisplayStrategy)
        .setAnimationEffectResponse(homepageContentTool.getCreditsChange(homePageContext.getUserCreditsContext().getCreditsInfoVO(),
            discountDetailAnimationVO.orderPageTempAmountTipDisplayStrategy, userDeviceContextVO.getSourceType(), homePageContext.getUserId(), accountVO.id))
        .setDiscountDetailAnimationResponse(DiscountDetailAnimationResponse.from(discountDetailAnimationVO))
        .setMinActualInterestRateFormat(userProductVO.getMinActualInterestRate() == null ? null : TT.gen("{0}", NumberFormatter.percentFormat(homePageContext.getSdkType().getLocale(), userProductVO.getMinActualInterestRate())))
        .setHasLowInterestProduct(hasLowInterestProduct)
        .setHasIrregularProduct(hasIrregularProduct)
        .setDeductContent(havingAvailableCoupon ? homepageContentTool.getDeductContent(accountVO, userProductVO.getEnableVirtualCredits()) : null)
        .setHavingAvailableCoupon(havingAvailableCoupon)
        .setDisplayStrategyV3(HomeDisplayStrategy.A2)
        .setButtonName(TT.gen(homepageV5Config.getButtonName(homePageContext.getStatus().name())))
        .setScrollLoanAmountContent(scrollLoanAmountContent)
        .setDiscountDetailTopAreaVipUIDisplayStrategy(homepageCommonTool.getDiscountDetailTopAreaVipUIDisplayStrategy(homePageContext.getUserId(), userDeviceContextVO.getBuild()))
        .setMaxDaysLowInterestProductDiscountAmountFormat(homepageContentTool.getDiscountAmountFormat(lowInterestDiscountAmount, homePageContext.getSdkType()))
        .setShowIncreaseCreditTaskBubble(homepageContentTool.needShowIncreaseCreditTaskBubbleContent(homePageContext.getUserId()))
        .setIncreaseCreditTaskBubbleContent(TT.gen("借款可提额，先到先得"))
        .setHomePageMainCardInfo(canOrderHomePageMainCardInfo)
        .setOrderFeeHideDecapitateDisplayStrategy(HomeDisplayStrategy.A)
        .setInputLoanAmountContent(homepageContentTool.getInputLoanAmountContent(homePageContext.getHomepageUserParamsVO(), creditsAreaDisplayStrategy))
        .setUnavailableLoanAccountDisplayStrategy(loanBankAccountService.routePaymentCredentialStrategy(homePageContext.getUserId(), homePageContext.getSdkType(), userDeviceContextVO.getBuild(), userDeviceContextVO.getSourceType()))
        .setRevolvingReviewDisplayStrategy(revolvingReviewDisplayStrategy)
        .setCanOrderPageResponse(new CanOrderPageResponse()
            .setCreditsAreaDisplayStrategy(creditsAreaDisplayStrategy)
            .setLoanTermPeriodDisplayStrategy(loanTermDisplayStrategy)
            .setCouponStyleAb(couponStyleAb)
            .setOrderPlanStyleFor363(homepageContentTool.getOrderPlanStyleFor363(homePageContext.getUserId(), userDeviceContextVO.getBuild()))
            .setAmountSlideBar(amountSlideBar)
            .setOrderPageSignStrategy(orderPageSignStrategy)
            .setOrderPageSignFormat(CreateOrderSignFormatResponse.from(
                homepageContentTool.getOrderPageSignFormatVO(orderPageSignStrategy, signatureDivisionStrategy,
                    createOrderSignFormatConfigMap, homePageContext.getUserId(), userDeviceContextVO.getBuild())))
            .setDialogPageSignFormat(CreateOrderSignFormatResponse.from(
                homepageContentTool.getOrderPageSignFormatVO(orderPageSignStrategy, signatureDivisionStrategy,
                    dialogSignFormatConfigMap, homePageContext.getUserId(), userDeviceContextVO.getBuild())))
            .setHome366DisplayStrategy(home366DisplayStrategy)
            .setTempCreditsQuotaPrompt(homepageContentTool.getTempCreditsQuotaPrompt(creditsInfoVO, homePageContext.getUserId(), homePageContext.getSdkType(), home366DisplayStrategy, amountSlideBar.slideBarDisplayStrategy, t0OrderPageInfoVO))
            .setProductStyleFor367(productStyleFor367)
            .setBlackCardDisplayStrategy(blackCardAbResultVO.blackCardAbResult)
            .setBlackCardProductId(blackCardAbResultVO.productId)
            .setJumpLevel2(homePageContext.hitJumpLevel2Strategy())
            .setWebCanCreateOrderPageUrl(getWebCanOrderPageUrl(homePageContext))
            .setProductPageStyleFor371(abTestVersionConfigService.getProductPageStyleFor371(homePageContext.getUserId(), userDeviceContextVO.getBuild()))
            .setShowRiplayPopup(homepageContentTool.isShowReplyPopup(homePageContext.getUserId(), userDeviceContextVO.getBuild()))
            .setT0InterestFreeStyle(Objects.isNull(t0OrderPageInfoVO)?null:t0OrderPageInfoVO.getAfRankT0Style())
        )
        .setCanShowOrderGuideAnimation(homepageContentTool.canShowOrderGuideAnimation(homePageContext.newHomePageUI(), homePageContext.getUserId(), userDeviceContextVO.getBuild()))
        .setShowOrderGuideAnimationTimes(homepageContentTool.showOrderGuideAnimationTimes())
    ;
  }


  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.CAN_CREATE_ORDER_USER_INFO;
  }

  private HomeDisplayStrategy home366DisplayStrategy(HomePageContext homePageContext) {
    if (!homePageContext.newHomePageUI()) {
      return HomeDisplayStrategy.A;
    }
    if (homePageContext.getUserDeviceContextVO().getBuild() < homepageV5Config.getHome366DisplayBuild()) {
      return HomeDisplayStrategy.A;
    }
    return HomeDisplayStrategy.valueOf(
        expFacade.fetchResult(ABTestSceneType.NEW_HOME_PAGE_366_STYLE, ABTestUtil.genDiversionKeyMapByUserId(homePageContext.getUserId()),
            "B"));
  }

  private String getWebCanOrderPageUrl(HomePageContext homePageContext) {
    if (!homePageContext.homePageLevel2()) {
      return null;
    }
    if (homePageContext.getUserDeviceContextVO().getBuild() < homepageV5Config.getWebCreateOrderBuild()) {
      return null;
    }
    return homepageV5Config.getWebCreateOrderUrl();
  }
}
