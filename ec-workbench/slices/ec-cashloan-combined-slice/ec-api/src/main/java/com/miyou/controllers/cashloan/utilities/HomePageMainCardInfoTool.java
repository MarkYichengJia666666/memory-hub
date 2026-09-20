package com.miyou.controllers.cashloan.utilities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.RepaymentResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.config.HomePageMainCardInfoConfig;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.utilities.StandardInterestUtil;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserCashLoanOrderContext;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.UserProductDetailVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.homepage.display.dto.HomePageInfo;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.core.userflow.domain.loan.model.discounts.OrderDiscounts;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.core.service.sensors.SensorsService;
import com.yqg.core.util.common.NumberFormatter;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;

@Slf4j
@Service
public class HomePageMainCardInfoTool {

  @Autowired
  private HomePageMainCardInfoConfig homePageMainCardInfoConfig;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private StandardInterestUtil standardInterestUtil;
  @Autowired
  private SensorsService sensorsService;
  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;
  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private ExpFacade expFacade;


  /**
   * 未授信卡片
   *
   * @param paramsVO
   * @param maxAmount
   * @return
   */
  public HomePageMainCardInfo getNeverAppliedMainCardInfo(HomepageUserParamsVO paramsVO,
                                                          BigDecimal maxAmount, int unFinishedStep) {
    if (!paramsVO.newHomePageUI()) {
      return null;
    }

    return HomePageMainCardInfo.builder()
        .title(TT.gen("最高额度（Rp）"))
        .amountStr(AmountFormatter.formatForIDNNoRP(maxAmount))
        .buttonContent(TT.gen("测算额度"))
        .content(getDefaultContent())
        .tips(TT.gen("还差{0}步即可获得专属借款额度", Math.max(1, unFinishedStep - 1)))
        .tipsIcon(homePageMainCardInfoConfig.getNeverAppliedTipsIcon())
        .build();
  }

  @NotNull
  private String getStandardInterestRateStr() {
    return NumberFormatter.percentFormat(SDKType.IDN_YQD.getLocale(), standardInterestUtil.getStandardInterestRateForNotOrder());
  }

  private TT getDefaultContent() {
    return TT.gen("日利率<{0}，分期1-12月", getStandardInterestRateStr());
  }

  /**
   * 未登录卡片
   *
   * @param maxCreditsBySDKOrZero
   * @return
   */
  public HomePageMainCardInfo getNotLoginMainCardInfo(BigDecimal maxCreditsBySDKOrZero) {
    return HomePageMainCardInfo.builder()
        .title(TT.gen("最高额度（Rp）"))
        .amountStr(AmountFormatter.formatForIDNNoRP(maxCreditsBySDKOrZero))
        .buttonContent(TT.gen("测算额度"))
        .content(getDefaultContent())
        .build();
  }

  /**
   * 审核
   *
   * @param paramsVO
   * @param homePageContext
   * @return
   */
  public HomePageMainCardInfo getReviewMainCardInfo(HomepageUserParamsVO paramsVO, HomePageContext homePageContext) {
    if (homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()) {
      HomePageMainCardInfo.RevolvingLoanUserCardInfo revolvingLoanUserCardInfo = new HomePageMainCardInfo.RevolvingLoanUserCardInfo();
      revolvingLoanUserCardInfo.setTipsIconForRevolvingLoan(homePageMainCardInfoConfig.getRevolvingLoanCreditsCalcTipsIcon());
      return HomePageMainCardInfo.builder()
          .tips(TT.gen("借款额度刷新中"))
          .buttonContent(TT.gen("申请借款"))
          .revolvingLoanUserCardInfo(revolvingLoanUserCardInfo)
          .build();
    }
    return HomePageMainCardInfo.builder()
        .title(TT.gen("额度测算中"))
        .buttonContent(TT.gen("审批中"))
        .content(TT.gen(homepageV5Config.getRiskInReviewContent(paramsVO.latestUserRiskTraceVO.riskType)))
        .build();
  }

  /**
   * ktp充传
   *
   * @param content
   * @return
   */
  public HomePageMainCardInfo getKtpReuploadMainCardInfo(TT content) {
    return HomePageMainCardInfo.builder()
        .title(TT.gen("需重传KTP"))
        .buttonContent(TT.gen("重拍KTP"))
        .content(content)
        .build();
  }

  /**
   * 没有增信重审资格
   *
   * @param rejectHomeDisplayStrategy
   * @param maxCredits
   * @param newHomepage
   * @return
   */
  public HomePageMainCardInfo getNoIncreaseCreditsQualificationMainCardInfo(
      HomePageContext homePageContext,
      HomeDisplayStrategy rejectHomeDisplayStrategy,
      BigDecimal maxCredits,
      boolean newHomepage,
      LoanMarketUserQualifyCheckResult loanMarketCheckResult
  ) {
    Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
    if (!newHomepage || isWholeProcess) {
      return null;
    }

    HomePageMainCardInfo mainCardInfo;
    if (rejectHomeDisplayStrategy == HomeDisplayStrategy.B) {
      UserResponse.LoanMarketInfoResponse loanMarketInfoResponse =
          new UserResponse.LoanMarketInfoResponse(TT.gen("查看其他贷款平台"), homepageV5Config.getRejectLoanMarketInfoUrl(), loanMarketCheckResult.riskFlowCheckType);
      Boolean showLoanMarketEntranceInMainCard = homepageContentTool.showLoanMarketEntranceInMainCard(homePageContext, loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk);
      mainCardInfo = HomePageMainCardInfo.builder()
          .title(TT.gen("最高额度（Rp）"))
          .amountStr(AmountFormatter.formatForIDNNoRP(maxCredits))
          .buttonContent(TT.gen("申请借款"))
          .loanMarketInfoResponse(showLoanMarketEntranceInMainCard ? loanMarketInfoResponse : null)
          .content(getDefaultContent())
          .build();
      if (homePageContext.loanMarketGotoLinkReported.compareAndSet(false, true)
          && showLoanMarketEntranceInMainCard) {
        sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.gotoLinkParam(homePageContext));
        loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
            homePageContext, LoanMarketReportType.GOTO_LINK, getClass());
      }
    } else {
      mainCardInfo = HomePageMainCardInfo.builder()
          .title(TT.gen("暂无额度"))
          .content(TT.gen("请保持良好的信用，后续还有机会获得额度"))
          .buttonContent(TT.gen("查看其他贷款平台"))
          .buttonUrl(homepageV5Config.getRejectLoanMarketInfoUrl())
          .loanMarketRule(loanMarketCheckResult.riskFlowCheckType)
          .build();
      if (homePageContext.loanMarketMainButtonReported.compareAndSet(false, true)) {
        sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.mainButtonParam(homePageContext));
        loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
            homePageContext, LoanMarketReportType.MAIN_BUTTION, getClass());
      }
    }
    return mainCardInfo;
  }

  /**
   * 重新测额
   *
   * @param maxCredits
   * @return
   */
  public HomePageMainCardInfo getReCalcCreditsMainCardInfo(BigDecimal maxCredits) {
    return HomePageMainCardInfo.builder()
        .title(TT.gen("最高额度（Rp）"))
        .amountStr(AmountFormatter.formatForIDNNoRP(maxCredits))
        .buttonContent(TT.gen("测算额度"))
        .content(getDefaultContent())
        .build();
  }

  /**
   * 债匹中
   *
   * @return
   */
  public HomePageMainCardInfo getDebtCheckMainCardInfo(HomePageContext homePageContext) {
    if (homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()) {
      CashLoanOrderVO orderVO = homePageContext.getUserCashLoanOrderContext().getLatestOrderVO();
      return updateMainCardInfoForRevolvingLoan(homePageContext, null, orderVO);
    }
    return HomePageMainCardInfo.builder()
        .title(TT.gen("资金准备中"))
        .content(TT.gen("正在为您匹配合适的放款资金，请耐心等待"))
        .buttonContent(TT.gen("查看"))
        .build();
  }

  /**
   * 等待签名
   *
   * @return
   */
  public HomePageMainCardInfo getPreCheckMainCardInfo() {
    return HomePageMainCardInfo.builder()
        .title(TT.gen("等待签署合同"))
        .buttonContent(TT.gen("去签名"))
        .content(TT.gen("确认借款合同后方可收到资金"))
        .build();
  }

  /**
   * 打款中
   *
   * @return
   */
  public HomePageMainCardInfo getFundPayingMainCardInfo() {
    return HomePageMainCardInfo.builder()
        .title(TT.gen("正在打款中"))
        .buttonContent(TT.gen("查看"))
        .content(TT.gen("资金即将到账，请耐心等待。"))
        .build();
  }

  /**
   * 可下单状态
   * TODO(LTB) 这块复杂度越来越高，可以先定义HomePageMainCardInfo.builder()，再每个if中去set，最后调用.builder返回。
   *  这样的话应该可以抽多个方法，后续改动不需要看这块这么多if-else
   *
   * @return
   */
  public HomePageMainCardInfo getCanOrderHomePageMainCardInfo(boolean newHomePageUI,
                                                              Long userId,
                                                              BigDecimal remainingCredits,
                                                              SDKType sdkType,
                                                              BigDecimal minActualInterestRate,
                                                              BigDecimal discountAmount,
                                                              HomeDisplayStrategy home366DisplayStrategy,
                                                              UserProductDetailVO userProductVO,
                                                              boolean homePageLevel1,
                                                              LoanUserCreditsInfoVO creditsInfoVO,
                                                              Boolean revolvingLoanUser) {
    if (!newHomePageUI || !homePageLevel1) {
      return null;
    }
    boolean canCutInterest = discountAmount.compareTo(BigDecimal.ZERO) > 0;
    TT content = null;
    TT tips = null;
    TT buttonRightCornerMark = null;
    String tipsIcon = null;
    HomePageMainCardInfo.OrderPageInfo.OrderPageInfoBuilder orderPageInfoBuilder = HomePageMainCardInfo.OrderPageInfo.builder();
    if (home366DisplayStrategy == HomeDisplayStrategy.A) {
      content = Objects.isNull(minActualInterestRate) ? null :
          TT.gen("日利率{0}", NumberFormatter.percentFormat(sdkType.getLocale(), minActualInterestRate));
      tips = canCutInterest ? TT.gen("利息最多降低{0}",
          AmountFormatter.format(sdkType.getCurrency(), discountAmount)) : null;
      tipsIcon = canCutInterest ? homePageMainCardInfoConfig.getOrderHomePageMainCardInfoTipsIcon() : null;
    } else {
      if (!userProductVO.hasLowInterestProduct()) {
        orderPageInfoBuilder.discountTips(TT.gen("利率低至{0}/天 | {1}"));
        orderPageInfoBuilder.discountTipsHighLightMap(ImmutableMap.of("{0}", NumberFormatter.percentFormat(sdkType.getLocale(), userProductVO.getStandardInterestRate()),
            "{1}", TT.gen("无手续费").toString(sdkType.getLocale().locale)));
      } else {
        OrderDiscounts orderDiscounts = ecHomePageProductTool.getOrderDiscountsByCreditsForMaxDaysProduct(
            userProductVO.getEnableVirtualCredits(), userId, sdkType);
        if (Objects.isNull(orderDiscounts) || BigDecimalHelper.lessThanOrEqual(orderDiscounts.getTotalDiscountsAmount(), BigDecimal.ZERO)) {
          orderPageInfoBuilder.discountTips(TT.gen("利率低至{0}每天"));
          orderPageInfoBuilder.discountTipsHighLightMap(ImmutableMap.of("{0}", NumberFormatter.percentFormat(sdkType.getLocale(), userProductVO.getMinActualInterestRate())));
        } else {
          orderPageInfoBuilder.discountTips(TT.gen("利率低至{0}每天 | 最高优惠至{1}"));
          orderPageInfoBuilder.discountTipsHighLightMap(ImmutableMap.of("{0}", NumberFormatter.percentFormat(sdkType.getLocale(), userProductVO.getMinActualInterestRate()),
              "{1}", AmountFormatter.format(sdkType.getCurrency(), orderDiscounts.getTotalDiscountsAmount())));
          buttonRightCornerMark = TT.gen("折扣 {0}", NumberFormatter.percentFormat(sdkType.getLocale(), orderDiscounts.getTotalDiscountRatio()
              .setScale(2, RoundingMode.UP)));
        }

      }
    }
    String buttonTextResult = expFacade.fetchResult(ABTestSceneType.NEW_UI_CREATE_ORDER_PAGE_BUTTON_TEXT,
        ABTestUtil.genDiversionKeyMapByUserId(userId), "C");
    CommonABTestResultGroup buttonTextGroup = CommonABTestResultGroup.valueOf(buttonTextResult);
    TT title = revolvingLoanUser ? TT.gen("最高可借额度(RP)") : TT.gen("你的额度(Rp)");
    String amountStr = AmountFormatter.formatForIDNNoRP(remainingCredits);
    HomePageMainCardInfo.RevolvingLoanUserCardInfo revolvingLoanUserCardInfo = new HomePageMainCardInfo.RevolvingLoanUserCardInfo();
    if (revolvingLoanUser) {
      if (home366DisplayStrategy.isStrategyA()) {
        tips = null;
      }
      revolvingLoanUserCardInfo.setNormalTextAboveButtonOfMainCard(TT.gen("总额度{0}",
          AmountFormatter.format(sdkType.getCurrency(), creditsInfoVO.getTotalCreditsForVirtual())).toString(sdkType.getLocale().locale));
    }
    return HomePageMainCardInfo.builder()
        .title(title)
        .amountStr(amountStr)
        .buttonContent(TT.gen(homePageMainCardInfoConfig.getCanCreateOrderHomePageButton(buttonTextGroup)))
        .buttonRightCornerMark(buttonRightCornerMark)
        .content(content)
        .tips(tips)
        .tipsIcon(tipsIcon)
        .orderPageInfo(orderPageInfoBuilder.build())
        .tempCreditsQuotaExpirePrompt(getTempCreditsQuotaExpirePrompt(creditsInfoVO, userId, home366DisplayStrategy, sdkType))
        .revolvingLoanUserCardInfo(revolvingLoanUserCardInfo)
        .build();
  }

  /**
   * 重新测额审核中
   *
   * @param revolvingReviewDisplayStrategy
   * @param paramsVO
   * @return
   */
  public HomePageMainCardInfo getReCalcCreditsReviewMainCardInfo(HomeDisplayStrategy revolvingReviewDisplayStrategy, HomepageUserParamsVO paramsVO) {
    if (revolvingReviewDisplayStrategy.isStrategyB()) {
      HomePageMainCardInfo.RevolvingLoanUserCardInfo revolvingLoanUserCardInfo = new HomePageMainCardInfo.RevolvingLoanUserCardInfo();
      revolvingLoanUserCardInfo.setTipsIconForRevolvingLoan(homePageMainCardInfoConfig.getRevolvingLoanCreditsCalcTipsIcon());
      return HomePageMainCardInfo.builder()
          .tips(TT.gen("借款额度刷新中"))
          .buttonContent(TT.gen("申请借款"))
          .tipsIcon(homePageMainCardInfoConfig.getRevolvingLoanCreditsCalcTipsIcon())
          .revolvingLoanUserCardInfo(revolvingLoanUserCardInfo)
          .build();
    }
    return HomePageMainCardInfo.builder()
        .title(TT.gen("额度测算中"))
        .buttonContent(TT.gen("审批中"))
        .content(TT.gen(homepageV5Config.getRiskInReviewContent(paramsVO.latestUserRiskTraceVO.riskType)))
        .build();
  }

  /**
   * 待还款
   */
  public HomePageMainCardInfo getRepaidMainCardInfo(UserCreditsContext userCreditsContext,
                                                    UserCashLoanOrderContext userCashLoanOrderContext,
                                                    RepaymentResponse repaymentResponse,
                                                    HomeDisplayStrategy repaymentDisplayStrategy,
                                                    BigDecimal credits,
                                                    HomeDisplayStrategy revolvingReviewDisplayStrategy) {
    if (Objects.isNull(repaymentResponse)) {
      log.info("repaymentResponse is null");
      if (revolvingReviewDisplayStrategy.isStrategyB()) {
        return getRevolvingUserRepayMainCardInfo(userCreditsContext,
            userCashLoanOrderContext,
            null,
            repaymentDisplayStrategy,
            credits);
      }
      return getNormalRepayHomePageMainCardInfo(null, repaymentDisplayStrategy, credits);
    }
    //循环贷用户还款主卡
    if (revolvingReviewDisplayStrategy.isStrategyB()) {
      return getRevolvingUserRepayMainCardInfo(userCreditsContext,
          userCashLoanOrderContext,
          repaymentResponse,
          repaymentDisplayStrategy,
          credits);
    }
    return getNormalRepayHomePageMainCardInfo(repaymentResponse, repaymentDisplayStrategy, credits);
  }

  private HomePageMainCardInfo getNormalRepayHomePageMainCardInfo(RepaymentResponse repaymentResponse,
                                                                  HomeDisplayStrategy repaymentDisplayStrategy,
                                                                  BigDecimal credits) {
    if (repaymentDisplayStrategy.isStrategyB() || Objects.isNull(repaymentResponse)) {
      return HomePageMainCardInfo.builder()
          .title(TT.gen("最高额度（Rp）"))
          .amountStr(AmountFormatter.formatForIDNNoRP(credits))
          .buttonContent(TT.gen("申请借款"))
          .content(getDefaultContent())
          .build();
    }
    HomePageMainCardInfo res = new HomePageMainCardInfo();
    res.setTitle(TT.gen("总待还金额（Rp）"));
    res.setAmountStr(AmountFormatter.formatForIDNNoRP(Objects.nonNull(repaymentResponse) ? repaymentResponse.totalUnpaidAmount : BigDecimal.ZERO));
    res.setButtonContent(TT.gen("还款"));
    res.setRepaymentInfo(HomePageMainCardInfo.UserRepaymentInfo.builder()
        .overdue(repaymentResponse.overdue)
        .tip(repaymentResponse.tip)
        .recentlyBillingDateContent(repaymentResponse.recentlyBillingDateDesc)
        .build());
    return res;
  }

  /**
   * 逾期 ＞低额度 ＞ 管制
   */
  private HomePageMainCardInfo getRevolvingUserRepayMainCardInfo(UserCreditsContext userCreditsContext,
                                                                 UserCashLoanOrderContext userCashLoanOrderContext,
                                                                 RepaymentResponse repaymentResponse,
                                                                 HomeDisplayStrategy repaymentDisplayStrategy,
                                                                 BigDecimal credits) {
    log.info("getRevolvingUserRepayMainCardInfo start get HomePageMainCardInfo.");
    HomePageMainCardInfo mainCardInfo = new HomePageMainCardInfo();
    BigDecimal stepAmount = cashLoanConfig.getStepAmount(userCashLoanOrderContext.getLatestOrderVO().sdkType);
    BigDecimal remainingCredits = userCreditsContext.getCreditsInfoVO().getRemainingVirtualCreditsForDisplay(stepAmount);
    //逾期处理
    if (userCashLoanOrderContext.isOverdue()) {
      return getRevolvingLoanOverdueHomePageMainCardInfo(userCreditsContext, userCashLoanOrderContext, mainCardInfo, remainingCredits);
    }
    //循环贷用户余额不足
    BigDecimal revolvingLoanMinRemainCredits = homepageV5Config.getRevolvingLoanMinRemainCredits();
    log.info("getRevolvingUserRepayMainCardInfo remainingCredits is {},userId is {},accountId is {}",
        remainingCredits,
        userCashLoanOrderContext.getLatestOrderVO().userId,
        userCashLoanOrderContext.getLatestOrderVO().accountId);
    if (revolvingLoanMinRemainCredits.compareTo(remainingCredits) > 0) {
      return getRevolvingLoanLessRemainCreditsHomePageMainCardInfo(userCreditsContext, mainCardInfo, remainingCredits, credits, userCashLoanOrderContext);
    }
    //管制
    if (userCreditsContext.getLoanAccountRevolvingCreditVO().getUserControl()) {
      return getRevolvingLoanControlledHomePageMainCardInfo(userCreditsContext, userCashLoanOrderContext, mainCardInfo, credits);
    }

    return getNormalRepayHomePageMainCardInfo(repaymentResponse, repaymentDisplayStrategy, credits);
  }

  private HomePageMainCardInfo getRevolvingLoanControlledHomePageMainCardInfo(UserCreditsContext userCreditsContext,
                                                                              UserCashLoanOrderContext userCashLoanOrderContext,
                                                                              HomePageMainCardInfo res, BigDecimal remainCredits) {
    res.setTitle(TT.gen("可借额度"));
    res.setButtonContent(TT.gen("申请借款"));
    res.setAmountStr(AmountFormatter.format(userCreditsContext.getCreditsInfoVO().getCurrency(), remainCredits));
    HomePageMainCardInfo.RevolvingLoanUserCardInfo revolvingLoanUserCardInfo = new HomePageMainCardInfo.RevolvingLoanUserCardInfo();
    revolvingLoanUserCardInfo.setNormalTextAboveButtonOfMainCard(TT.gen("总额度{0}",
        AmountFormatter.format(userCashLoanOrderContext.getLatestOrderVO().sdkType.getCurrency(),
            userCreditsContext.getCreditsInfoVO().getTotalCreditsForVirtual())).toString(SDKType.IDN_YQD.getLocale().locale) + " "
    );
    //有在贷
    if (userCashLoanOrderContext.hasReadyOrders()) {
      revolvingLoanUserCardInfo.setHighLightTextAboveButtonOfMainCard(TT.gen(" | 结清账单后可发起提现"));
    }
    res.setRevolvingLoanUserCardInfo(revolvingLoanUserCardInfo);
    return res;
  }

  private HomePageMainCardInfo getRevolvingLoanLessRemainCreditsHomePageMainCardInfo(UserCreditsContext userCreditsContext,
                                                                                     HomePageMainCardInfo homePageMainCardInfo,
                                                                                     BigDecimal remainingCredits, BigDecimal credits,
                                                                                     UserCashLoanOrderContext userCashLoanOrderContext) {
    if (!userCashLoanOrderContext.hasReadyOrders()) {
      return getRevolvingLoanControlledHomePageMainCardInfo(userCreditsContext, userCashLoanOrderContext, homePageMainCardInfo, credits);
    }
    HomePageMainCardInfo.RevolvingLoanUserCardInfo revolvingLoanUserCardInfo = new HomePageMainCardInfo.RevolvingLoanUserCardInfo();
    homePageMainCardInfo.setButtonContent(TT.gen("申请借款"));
    revolvingLoanUserCardInfo.setNormalTextAboveButtonOfMainCard(TT.gen("总额度{0}",
        AmountFormatter.format(userCreditsContext.getCreditsInfoVO().currency,
            userCreditsContext.getCreditsInfoVO().getTotalCreditsForVirtual())).toString(SDKType.IDN_YQD.getLocale().locale) + " ");
    revolvingLoanUserCardInfo.setHighLightTextAboveButtonOfMainCard(TT.gen(homepageV5Config.getRevolvingLoanHighLightTestWhenRemainCreditsNotEnough()));
    //余额为 0
    if (remainingCredits.compareTo(BigDecimal.ZERO) == 0) {
      revolvingLoanUserCardInfo.setTipsIconForRevolvingLoan(homepageV5Config.getTipsIconForRevolvingLoan());
      homePageMainCardInfo.setTips(TT.gen("额度已用完"));
    } else {
      homePageMainCardInfo.setTitle(TT.gen("可借额度"));
      homePageMainCardInfo.setAmountStr(AmountFormatter.format(userCreditsContext.getCreditsInfoVO().currency, credits));
    }
    homePageMainCardInfo.setRevolvingLoanUserCardInfo(revolvingLoanUserCardInfo);
    return homePageMainCardInfo;
  }

  private HomePageMainCardInfo getRevolvingLoanOverdueHomePageMainCardInfo(UserCreditsContext userCreditsContext,
                                                                           UserCashLoanOrderContext userCashLoanOrderContext,
                                                                           HomePageMainCardInfo homePageMainCardInfo,
                                                                           BigDecimal remainingCredits) {
    homePageMainCardInfo.setButtonContent(TT.gen("申请借款"));
    homePageMainCardInfo.setAmountStr(AmountFormatter.format(userCreditsContext.getCreditsInfoVO().currency, remainingCredits));
    HomePageMainCardInfo.RevolvingLoanUserCardInfo revolvingLoanUserCardInfo = new HomePageMainCardInfo.RevolvingLoanUserCardInfo();
    revolvingLoanUserCardInfo.setNormalTextAboveButtonOfMainCard(TT.gen("总额度{0}",
        AmountFormatter.format(userCreditsContext.getCreditsInfoVO().currency,
            userCreditsContext.getCreditsInfoVO().getTotalCreditsForVirtual())).toString(SDKType.IDN_YQD.getLocale().locale) + " "
    );
    revolvingLoanUserCardInfo.setHighLightTextAboveButtonOfMainCard(TT.gen(" | 结清逾期账单后可发起提现"));
    //余额为 0
    if (remainingCredits.compareTo(BigDecimal.ZERO) == 0) {
      revolvingLoanUserCardInfo.setTipsIconForRevolvingLoan(homepageV5Config.getTipsIconForRevolvingLoan());
      homePageMainCardInfo.setTips(TT.gen("额度已用完"));
      homePageMainCardInfo.setAmountStr(null);
    } else {
      homePageMainCardInfo.setTitle(TT.gen("可借额度"));
      homePageMainCardInfo.setAmountStr(AmountFormatter.format(userCreditsContext.getCreditsInfoVO().currency, remainingCredits));
    }
    homePageMainCardInfo.setRevolvingLoanUserCardInfo(revolvingLoanUserCardInfo);
    return homePageMainCardInfo;
  }

  public HomePageMainCardInfo getFundCheckingMainCard(TT content, HomePageContext homePageContext) {
    if (homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()) {
      CashLoanOrderVO orderVO = homePageContext.getUserCashLoanOrderContext().getLatestOrderVO();
      return updateMainCardInfoForRevolvingLoan(homePageContext, null, orderVO);
    }
    return HomePageMainCardInfo.builder()
        .title(TT.gen("资金准备中"))
        .buttonContent(TT.gen("查看"))
        .content(content)
        .build();
  }

  public HomePageMainCardInfo getOrderReview(HomepageUserParamsVO paramsVO, HomePageContext homePageContext) {
    if (homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()) {
      CashLoanOrderVO orderVO = homePageContext.getUserCashLoanOrderContext().getLatestOrderVO();
      return updateMainCardInfoForRevolvingLoan(homePageContext, null, orderVO);
    }
    return HomePageMainCardInfo.builder()
        .title(TT.gen("订单审核中"))
        .content(TT.gen(homepageV5Config.getRiskInReviewContent(paramsVO.latestUserRiskTraceVO.riskType)))
        .buttonContent(TT.gen("审批中"))
        .build();
  }

  public HomePageMainCardInfo updateMainCardInfoForRevolvingLoan(HomePageContext homePageContext,
                                                                 HomePageMainCardInfo mainCardInfo,
                                                                 CashLoanOrderVO orderVO) {
    if (Objects.isNull(mainCardInfo)) {
      mainCardInfo = new HomePageMainCardInfo();
    }
    mainCardInfo.setTitle(TT.gen("可借额度"));
    mainCardInfo.setAmountStr(AmountFormatter.format(orderVO.currency, homePageContext.getUserProductVO().getEnableVirtualCredits()));
    HomePageMainCardInfo.RevolvingLoanUserCardInfo revolvingLoanUserCardInfo = new HomePageMainCardInfo.RevolvingLoanUserCardInfo();
    revolvingLoanUserCardInfo.setPayingNormalTextAboveButtonOfMainCard(TT.gen("借款金额{0} ({1}{2} × {3})",
        AmountFormatter.format(orderVO.currency, orderVO.principal),
        orderVO.termPeriod,
        TT.gen(orderVO.termsUnit.description).toString(homePageContext.getSdkType().getLocale().locale),
        orderVO.terms).toString(homePageContext.getSdkType().getLocale().locale));
    TT highLight = orderVO.status == CashLoanOrderStatus.RESERVE ? TT.gen("审核中 >") : TT.gen("放款中 >");
    revolvingLoanUserCardInfo.setPayingHighLightTextAboveButtonOfMainCard(highLight);
    mainCardInfo.setRevolvingLoanUserCardInfo(revolvingLoanUserCardInfo);
    mainCardInfo.setButtonContent(TT.gen("申请借款"));
    setRevolvingLoanOrderSteps(orderVO, mainCardInfo);
    return mainCardInfo;
  }

  private void setRevolvingLoanOrderSteps(CashLoanOrderVO orderVO, HomePageMainCardInfo mainCardInfo) {
//    getRevolvingLoanUserCardInfo 不可能是 null
    mainCardInfo.getRevolvingLoanUserCardInfo().setPayingOrderStepTitleOfMainCard(TT.gen(homepageV5Config.getOrderProcessTitleOfMainCard()));
    //订单流程分三步，这里对每一步的名称划分
    Map<String, String> orderStepMap = homepageV5Config.getOrderProcessStepsOfMainCard();
    List<HomePageMainCardInfo.OrderStepVO> stepList = new ArrayList<>();
    if (orderVO.status == CashLoanOrderStatus.RESERVE) {
      setOrderStep(stepList, orderStepMap, OrderProcessStatus.DOING, OrderProcessStatus.UNDONE, OrderProcessStatus.UNDONE);
    } else if (orderVO.status == CashLoanOrderStatus.CHECK) {
      setOrderStep(stepList, orderStepMap, OrderProcessStatus.DONE, OrderProcessStatus.DOING, OrderProcessStatus.UNDONE);
    } else {
      setOrderStep(stepList, orderStepMap, OrderProcessStatus.DONE, OrderProcessStatus.DONE, OrderProcessStatus.DOING);
    }
    mainCardInfo.getRevolvingLoanUserCardInfo().setPayingOrderStepListOfMainCard(stepList);
  }

  private static void setOrderStep(List<HomePageMainCardInfo.OrderStepVO> stepList, Map<String, String> orderStepMap, OrderProcessStatus doing, OrderProcessStatus undone, OrderProcessStatus undone1) {
    stepList.add(HomePageMainCardInfo.OrderStepVO.from(TT.gen(orderStepMap.getOrDefault("IN_REVIEW", "风控审核中")), doing));
    stepList.add(HomePageMainCardInfo.OrderStepVO.from(TT.gen(orderStepMap.getOrDefault("CHECKING", "资方审核中")), undone));
    stepList.add(HomePageMainCardInfo.OrderStepVO.from(TT.gen(orderStepMap.getOrDefault("PAYING", "放款中")), undone1));
  }

  public HomePageMainCardInfo getWaitingSupplementMainCardInfo() {
    return HomePageMainCardInfo.builder()
        .title(TT.gen("申请审批中"))
        .content(TT.gen("正在审核您的借款申请，预计1分钟内完成，请耐心等待。"))
        .build();
  }

  public HomePageMainCardInfo getFinishSupplementMainCardInfo() {
    return HomePageMainCardInfo.builder()
        .title(TT.gen("身份验证中"))
        .content(TT.gen("正在进行身份信息核验，预计30秒内完成，请稍等。"))
        .build();
  }

  public HomePageMainCardInfo getNeedSupplementMainCardInfo() {
    return HomePageMainCardInfo.builder()
        .title(TT.gen("继续借款申请"))
        .content(TT.gen("请您进行身份核验，完成后将会立即完成审批。"))
        .buttonContent(TT.gen("去申请"))
        .build();
  }

  public HomePageMainCardInfo getQuickOrderMainCardInfo(Long userId, BigDecimal remainingCredits, boolean newHomePageUi) {
    if (!newHomePageUi) {
      return null;
    }
    String buttonTextResult = expFacade.fetchResult(ABTestSceneType.NEW_UI_CREATE_ORDER_PAGE_BUTTON_TEXT,
        ABTestUtil.genDiversionKeyMapByUserId(userId), "C");
    CommonABTestResultGroup buttonTextGroup = CommonABTestResultGroup.valueOf(buttonTextResult);
    return HomePageMainCardInfo.builder()
        .title(TT.gen("你的额度(Rp)"))
        .amountStr(AmountFormatter.formatForIDNNoRP(remainingCredits))
        .buttonContent(TT.gen(homePageMainCardInfoConfig.getCanCreateOrderHomePageButton(buttonTextGroup)))
        .build();
  }

  private TT getTempCreditsQuotaExpirePrompt(LoanUserCreditsInfoVO creditsInfoVO, Long userId, HomeDisplayStrategy homePage366Strategy, SDKType sdkType) {
    if (!homePage366Strategy.isStrategyB()) {
      return null;
    }
    BigDecimal threshold = new BigDecimal("100000");
    if (creditsInfoVO.tempCreditsForCoupon.compareTo(threshold) < 0) {
      return null;
    }
    Long remainingDays = creditsInfoVO.getTempCreditsCouponRemainingDays();
    if (Objects.isNull(remainingDays)) {
      return null;
    }
    String amount = AmountFormatter.format(creditsInfoVO.currency, creditsInfoVO.tempCreditsForCoupon);
    log.info("[expirePrompt]userId = {}, credits = {}, remainingDays = {}", userId, amount, remainingDays);
    return TT.gen("有{0}的临时额度，将在{1}后失效！", amount, remainingDays + TT.gen("天").toString(sdkType.getLocale().locale));
  }

  public HomeDisplayStrategy getRepaymentDisplayStrategy(Long userId, Long build) {
    if (build < homepageV5Config.getRepaymentDisplayStrategyBuild()) {
      return HomeDisplayStrategy.A;
    }
    return HomeDisplayStrategy.valueOf(
        expFacade.fetchResult(ABTestSceneType.REPAYMENT_HOME_PAGE_STYLE, ABTestUtil.genDiversionKeyMapByUserId(userId), "B"));
  }

  public HomePageMainCardInfo getMainCardForRejectUser(HomePageInfo homeInfo,
                                                       boolean showLoanMarketInfoButtonForNewHomePage,
                                                       UserResponse.LoanMarketInfoResponse loanMarketInfoResponse,
                                                       UserResponse userInfo,
                                                       HomePageContext homePageContext,
                                                       boolean showLoanMarketEntrance) {
    if (homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()) {
      BigDecimal remainingCredits = BigDecimal.ZERO;
      if (Objects.nonNull(homePageContext.getUserProductVO())) {
        remainingCredits = homePageContext.getUserProductVO().getEnableVirtualCredits();
      }
      return getRevolvingUserRepayMainCardInfo(homePageContext.getUserCreditsContext(),
          homePageContext.getUserCashLoanOrderContext(),
          new RepaymentResponse(),
          getRepaymentDisplayStrategy(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild()),
          remainingCredits);
    }
    HomePageMainCardInfo mainCardInfo;
    boolean showLoanMarketCard = false;
    //如果用户展示贷超，且进了实验组，走新逻辑
    if (showLoanMarketEntrance && !homepageContentTool.checkUserNotInLoanMarketCardExpr(homePageContext)) {
      mainCardInfo = HomePageMainCardInfo.builder()
          .title(TT.gen("贷款暂不可用"))
          .content(TT.gen("拥有良好的信用记录，未来仍有机会。"))
          .build();
      showLoanMarketCard = true;
    } else {
      mainCardInfo = HomePageMainCardInfo.builder()
          .title(homeInfo.homePageNewInfo.title)
          .content(homeInfo.homePageNewInfo.subTitle)
          .buttonContent(showLoanMarketInfoButtonForNewHomePage ? TT.gen("查看其他贷款平台") : homeInfo.homePageNewInfo.buttonInfo.title)
          .buttonUrl(showLoanMarketInfoButtonForNewHomePage ? homepageV5Config.getRejectLoanMarketInfoUrl() : homeInfo.homePageNewInfo.buttonInfo.url)
          .amountStr(homeInfo.homePageNewInfo.amountStr)
          .tips(homeInfo.homePageNewInfo.tips)
          .loanMarketInfoResponse(showLoanMarketInfoButtonForNewHomePage ? null : loanMarketInfoResponse)
          .build();
    }
    eventTrackForV2(homePageContext, showLoanMarketInfoButtonForNewHomePage, showLoanMarketCard, loanMarketInfoResponse);
    return mainCardInfo;
  }

  private void eventTrackForV2(HomePageContext homePageContext, boolean showLoanMarketInfoButtonForNewHomePage, boolean showLoanMarketCard, UserResponse.LoanMarketInfoResponse loanMarketInfoResponse) {
    if (HomepageVersion.V2 == homePageContext.getHomepageV3ExperimentContext().getHomepageDisplayVersion() && !showLoanMarketCard) {
      if (showLoanMarketInfoButtonForNewHomePage && homePageContext.loanMarketMainButtonReported.compareAndSet(false, true)) {
        sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.mainButtonParam(homePageContext));
        loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
            homePageContext, LoanMarketReportType.MAIN_BUTTION, getClass());
      }
      if (!showLoanMarketInfoButtonForNewHomePage
          && Objects.nonNull(loanMarketInfoResponse)
          && homePageContext.loanMarketGotoLinkReported.compareAndSet(false, true)) {
        sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.gotoLinkParam(homePageContext));
        loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
            homePageContext, LoanMarketReportType.GOTO_LINK, getClass());
      }
    }
  }

  public String getRepaymentLoanTipsPopup(HomeDisplayStrategy revolvingReviewDisplayStrategy, HomePageContext homePageContext) {
    String loanTipsPopup;
    if (revolvingReviewDisplayStrategy.isStrategyB()) {
      UserCreditsContext userCreditsContext = homePageContext.getUserCreditsContext();
      UserCashLoanOrderContext userCashLoanOrderContext = homePageContext.getUserCashLoanOrderContext();
      // 循环贷适配 loanTipsPopup 提示按钮
      // 循环贷逾期
      log.info("getRevolvingUserRepayMainCardInfo start get HomePageMainCardInfo.");
      //逾期处理
      if (userCashLoanOrderContext.isOverdue()) {
        return homepageV5Config.getRevolvingLoanOverdue();
      }
      //循环贷用户余额不足
      BigDecimal stepAmount = cashLoanConfig.getStepAmount(userCashLoanOrderContext.getLatestOrderVO().sdkType);
      BigDecimal remainingCredits = userCreditsContext.getCreditsInfoVO().getRemainingVirtualCreditsForDisplay(stepAmount);
      BigDecimal revolvingLoanMinRemainCredits = homepageV5Config.getRevolvingLoanMinRemainCredits();
      log.info("getRevolvingUserRepayMainCardInfo remainingCredits is {},userId is {},accountId is {}",
          remainingCredits,
          userCashLoanOrderContext.getLatestOrderVO().userId,
          userCashLoanOrderContext.getLatestOrderVO().accountId);
      if (revolvingLoanMinRemainCredits.compareTo(remainingCredits) > 0) {
        if (!userCashLoanOrderContext.hasReadyOrders()) {
          return homepageV5Config.getRevolvingRepaymentLoanTipsPopUp();
        }
        return remainingCredits.compareTo(BigDecimal.ZERO) == 0
            //场景二：额度已借完： 剩余额度 ≤ Rp100.000，  温馨提示 - 您的借款额度已用完，还款后可恢复借款额度。
            ? homepageV5Config.getRevolvingLoanRemainCreditsNotEnoughAndNoOverdueWithZeroCredits()
            //场景一：Rp100.000 ≤ 剩余额度 ≤ 循环场景最小可借额度（风控配置值） 温馨提示 - 您的借款额度已用完，还款后可恢复借款额度。
            : getRevolvingLoanRemainCreditsNotEnoughAndNoOverdue(revolvingLoanMinRemainCredits);
      }
      //管制
      if (userCreditsContext.getLoanAccountRevolvingCreditVO().getUserControl()) {
        //有在贷则去还款，没有在贷提示信用不足
        return homePageContext.getUserCashLoanOrderContext().hasReadyOrders()
            ? homepageV5Config.getRevolvingLoanOverdueLoanTipsPopUp()
            : homepageV5Config.getRevolvingRepaymentLoanTipsPopUp();
      }
    }

    //非循环贷用户
    loanTipsPopup = homePageContext.getUserCashLoanOrderContext().isOverdue()
        ? homepageV5Config.getOverdueLoanTipsPopUp()
        : homepageV5Config.getRepaymentLoanTipsPopUp();
    return loanTipsPopup;
  }

  private String getRevolvingLoanRemainCreditsNotEnoughAndNoOverdue(BigDecimal revolvingLoanMinRemainCredits) {
    String loanTipsPopup = homepageV5Config.getRevolvingLoanRemainCreditsNotEnoughAndNoOverdue();
    LoanTipsPopUpVO loanTipsPopUpVO = JsonUtils.fromOrNull(loanTipsPopup, LoanTipsPopUpVO.class);
    if (Objects.nonNull(loanTipsPopUpVO) && Objects.nonNull(loanTipsPopUpVO.content)) {
      loanTipsPopUpVO.contentArgs = ImmutableList.of(revolvingLoanMinRemainCredits).toArray();
      return JsonUtils.toString(loanTipsPopUpVO);
    }
    return loanTipsPopup;
  }

  public enum OrderProcessStatus {
    DOING,
    DONE,
    UNDONE
  }
}
