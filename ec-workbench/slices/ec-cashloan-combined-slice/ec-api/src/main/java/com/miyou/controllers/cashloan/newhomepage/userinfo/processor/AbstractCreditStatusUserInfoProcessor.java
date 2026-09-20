package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.loanmarket.LoanMarketCardElementProvider;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.miyou.controllers.cashloan.response.v5.user.*;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

@Service
public abstract class AbstractCreditStatusUserInfoProcessor extends AbstractUserInfoProcessor {

  @Autowired
  protected LoanMarketCardElementProvider loanMarketCardElementProvider;
  @Autowired
  protected LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;

  public abstract HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext, BigDecimal maxCreditsBySDK);

  public abstract BigDecimal getDisplayCredit(HomePageContext homePageContext, BigDecimal maxCreditsBySDK);

  public UserResponse.LoanMarketInfoResponse getLoanMarketInfoResponse(HomePageContext homePageContext) {
    return null;
  }

  public LoanMarketForOldHomePageResponse buildLoanMarketForOldHomePageResp(HomePageContext homePageContext) {
    return null;
  }

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    SDKType sdkType = homePageContext.getSdkType();
    Long build = homePageContext.getUserDeviceContextVO().getBuild();
    PlatformType platformType = homePageContext.getUserDeviceContextVO().getPlatformType();
    IDNHomepageLoanStatusV5 status = homePageContext.getStatus();
    HomepageV5Config homepageV5Config = homePageContext.getHomePageServiceManager().getHomepageV5Config();
    String deviceToken = homePageContext.getUserDeviceContextVO().getDeviceToken();
    BigDecimal maxCreditsBySDK = productConfigService.getMaxCreditsBySDKOrZero(sdkType);
    UserResponse.LoanMarketInfoResponse loanMarketInfoResponse = getLoanMarketInfoResponse(homePageContext);
    LoanMarketForOldHomePageResponse loanMarketForOldHomePageResponse = buildLoanMarketForOldHomePageResp(homePageContext);
    Boolean showLoanMarketCardForOldHomePage = Objects.nonNull(loanMarketForOldHomePageResponse);

    CreditsLabel creditsLabel = new CreditsLabel()
        .setTitle(TT.gen(
            homepageV5Config.getCreditsTitle(status.name())))
        .setCredits(getDisplayCredit(homePageContext, maxCreditsBySDK))
        .setTip(homepageContentTool.getCreditsTips(homePageContext.getHomepageUserParamsVO(), homePageContext.getUserDeviceContextVO().getSourceType(), status, build));

    RateOrPeriodLabel rateOrPeriodLabel = new RateOrPeriodLabel()
        .setTitle(homepageContentTool.generateDisplayRateTitle(status, build, platformType))
        .setContent(homepageContentTool.generateDisplayRate(status, build, platformType, deviceToken));

    RateOrPeriodLabel defaultDisplayPeriod = new RateOrPeriodLabel()
        .setTitle(TT.gen(homepageV5Config.getProductPeriodTitle(status.name())))
        .setContent(TT.gen(homepageContentTool.getDefaultProductPeriodContent(homePageContext.getUserId(), build, platformType)));
    HomePageMainCardInfo homePageMainCardInfo = getHomePageMainCardInfo(homePageContext, maxCreditsBySDK);
    if (Objects.nonNull(homePageMainCardInfo) && !showLoanMarketCardForOldHomePage) {
      homePageMainCardInfo.setLoanMarketInfoResponse(loanMarketInfoResponse);
    }
    userResponse
        .setDefaultDisplayCredits(creditsLabel)
        .setHasIrregularProduct(false)
        .setHasLowInterestProduct(false)
        .setHasCornerMark(loanUserCouponService.havingAvailableCoupon(homePageContext.getUserId()))
        .setDefaultDisplayRate(rateOrPeriodLabel)
        .setDefaultDisplayPeriod(defaultDisplayPeriod)
        .setLoanMarketInfoResponse(showLoanMarketCardForOldHomePage ? null : loanMarketInfoResponse)
        .setShowLoanMarketInfo(Objects.nonNull(loanMarketInfoResponse) && !showLoanMarketCardForOldHomePage)
        .setButtonName(TT.gen(homepageV5Config.getButtonName(status.name())))
        .setHomePageMainCardInfo(homePageMainCardInfo)
        .setLoanMarketCardForV2Response(loanMarketForOldHomePageResponse);

    if (HomepageVersion.V2 == homePageContext.getHomepageV3ExperimentContext().getHomepageDisplayVersion()
        && Objects.nonNull(loanMarketInfoResponse) && !showLoanMarketCardForOldHomePage
        && homePageContext.loanMarketGotoLinkReported.compareAndSet(false, true)) {
      sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.gotoLinkParam(homePageContext));
      loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
          homePageContext, LoanMarketReportType.GOTO_LINK, getClass());
    }
    if (status == IDNHomepageLoanStatusV5.INCREASE_REVIEW_NEVER_REAPPLIED) {
      userResponse.setButtonJumpUrl(homePageContext.getHomePageServiceManager().getHomepageV5Config().getIncreaseCreditsReviewHomeButtonSkippableUrl());
    }
  }
}
