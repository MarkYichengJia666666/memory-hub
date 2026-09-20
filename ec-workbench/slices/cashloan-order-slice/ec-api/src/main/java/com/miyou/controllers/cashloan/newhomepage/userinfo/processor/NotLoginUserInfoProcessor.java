package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.CreditsLabel;
import com.miyou.controllers.cashloan.response.v5.user.RateOrPeriodLabel;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.config.HomepageV3ElementConfig;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;

/**
 * 未登录
 */
@Service
public class NotLoginUserInfoProcessor extends AbstractUserInfoProcessor {

  @Autowired
  private HomepageV3ElementConfig homepageV3ElementConfig;

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
    BigDecimal maxCreditsBySDK = isWholeProcess ? homepageV3ElementConfig.getH5WholeProcessMaxCredit() : productConfigService.getMaxCreditsBySDKOrZero(homePageContext.getSdkType());
    IDNHomepageLoanStatusV5 status = homePageContext.getStatus();
    UserDeviceContextVO userDeviceContextVO = homePageContext.getUserDeviceContextVO();
    Long build = userDeviceContextVO.getBuild();
    PlatformType platformType = userDeviceContextVO.getPlatformType();
    String deviceToken = homePageContext.getUserDeviceContextVO().getDeviceToken();
    HomepageV5Config homepageV5Config = homePageContext.getHomePageServiceManager().getHomepageV5Config();
    CreditsLabel creditsLabel = new CreditsLabel()
        .setTitle(TT.gen(
            homepageV5Config.getCreditsTitle(status.name())))
        .setCredits(maxCreditsBySDK)
        .setTip(TT.gen(homepageV5Config.getCreditsTip(status.name())));
    RateOrPeriodLabel rateOrPeriodLabel = new RateOrPeriodLabel()
        .setTitle(homepageContentTool.generateDisplayRateTitle(status, build, platformType))
        .setContent(homepageContentTool.generateDisplayRate(status, build, platformType, deviceToken));
    RateOrPeriodLabel defaultDisplayPeriod = new RateOrPeriodLabel()
        .setTitle(TT.gen(homepageV5Config.getProductPeriodTitle(status.name())))
        .setContent(TT.gen(homepageContentTool.getDefaultProductPeriodContent(null, build, platformType)));
    // userInfo赋值
    userResponse
        .setExactStatus(IDNHomepageLoanStatusV5.NOT_LOGIN.name())
        .setDisplayStatus(IDNHomepageLoanStatusV5.NOT_LOGIN.displayStatusV5.name())
        .setDefaultDisplayCredits(creditsLabel)
        .setHasIrregularProduct(false)
        .setHasLowInterestProduct(false)
        .setHasCornerMark(false)
        .setDefaultDisplayRate(rateOrPeriodLabel)
        .setDefaultDisplayPeriod(defaultDisplayPeriod)
        .setButtonName(TT.gen(homepageV5Config.getButtonName(status.name())))
        .setUiV2(homepageV5Config.isNewHomePageUIForNotLogin())
        .setHomePageMainCardInfo(homePageMainCardInfoTool.getNotLoginMainCardInfo(maxCreditsBySDK))
    ;

  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.NOT_LOGIN_USER_INFO_PROCESSOR;
  }
}
