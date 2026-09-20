package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.CreditsLabel;
import com.miyou.controllers.cashloan.response.v5.user.RateOrPeriodLabel;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class QuotaCardProcessor extends AbstractUserInfoProcessor {
  @Override
  public void process(UserResponse fieldsInfo, HomePageContext homePageContext) {
    Long build = homePageContext.getUserDeviceContextVO().getBuild();
    String deviceToken = homePageContext.getUserDeviceContextVO().getDeviceToken();
    PlatformType platformType = homePageContext.getUserDeviceContextVO().getPlatformType();
    IDNHomepageLoanStatusV5 status = homePageContext.getStatus();

    CreditsLabel creditsLabel = new CreditsLabel()
        .setTitle(TT.gen(
            homepageV5Config.getCreditsTitle(status.name())))
        .setCredits(getCredits(homePageContext))
        .setTip(homepageContentTool.getCreditsTips(homePageContext.getHomepageUserParamsVO(), homePageContext.getUserDeviceContextVO().getSourceType(), status, build));

    RateOrPeriodLabel rateOrPeriodLabel = new RateOrPeriodLabel()
        .setTitle(homepageContentTool.generateDisplayRateTitle(status, build, platformType))
        .setContent(homepageContentTool.generateDisplayRate(status, build, platformType, deviceToken));

    RateOrPeriodLabel defaultDisplayPeriod = new RateOrPeriodLabel()
        .setTitle(TT.gen(homepageV5Config.getProductPeriodTitle(status.name())))
        .setContent(TT.gen(homepageContentTool.getDefaultProductPeriodContent(homePageContext.getUserId(), build, platformType)));

    fieldsInfo
        .setDefaultDisplayCredits(creditsLabel)
        .setDefaultDisplayRate(rateOrPeriodLabel)
        .setDefaultDisplayPeriod(defaultDisplayPeriod)
    ;

  }

  protected BigDecimal getCredits(HomePageContext homePageContext) {
    return ecHomePageProductTool.getDefaultOrHistoryLoanAmount(homePageContext.getLoanAccountId(), homePageContext.getSdkType());
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.QUOTA_CARD_INFO;
  }
}
