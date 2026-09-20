package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.cancreateorder;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.credits.vo.HomePageCreditsChangeVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.longshortuser.MinimalistUserConfig;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.yqg.core.service.risk.longshortuser.MinimalistUserDisplayStatus.FIRST_LOAN_RISK_ACCEPTED;

@Service
public class MinimalistAcceptUserInfoProcessor extends AbstractUserInfoProcessor {
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private MinimalistUserConfig minimalistUserConfig;
  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.MINIMALIST_ACCEPT_USER_INFO;
  }

  @Override
  public void process(UserResponse userInfo, HomePageContext homePageContext) {
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    HomePageCreditsChangeVO creditsChangeInfo = loanUserCreditsService.getCreditsChangeInfo(creditsInfoVO);
    MinimalistUserConfig.HomepageDisplayInfo homepageDisplayInfo = minimalistUserConfig.getHomepageDisplayInfo(FIRST_LOAN_RISK_ACCEPTED);
    Long build = homePageContext.getUserDeviceContextVO().getBuild();
    HomepageV5Config homepageV5Config = homePageContext.getHomePageServiceManager().getHomepageV5Config();
    userInfo
        .setUiV2(homePageContext.newHomePageUI())
        .setTempCredit(creditsInfoVO.tempCredits)
        .setHasCornerMark(loanUserCouponService.havingAvailableCoupon(homePageContext.getUserId()))
        .setTitle(TT.gen(homepageDisplayInfo.title))
        .setContent(TT.gen(homepageDisplayInfo.subTitle))
        .setRemainingCredit(homePageContext.getUserProductVO().getEnableVirtualCredits())
        .setTotalCredit(creditsInfoVO.totalCredits)
        .setButtonName(TT.gen(homepageDisplayInfo.buttonName))
        .setMinimalistProcessIncreaseCreditsResponse(homepageContentTool.getMinimalistProcessIncreaseCreditsResponse(homePageContext.getStatus(), homePageContext.getHomepageUserParamsVO()))
        .setTempCreditsContent(creditsChangeInfo == null ? null : creditsChangeInfo.increaseCreditContext)
        .setBackgroundImageUrl(homepageContentTool.getMinimalistProcessUserAcceptBackgroundImageUrl(build))
        .setToolBarColor(homepageV5Config.getMinimalistProcessUserAcceptToolBarColor())
        .setTopInfoContent(homepageV5Config.getMinimalistProcessUserTopContentPrefixInfo() + homePageContext.getLoanAccountVO().name)
        .setTopInfoSubContent(TT.gen(homepageV5Config.getMinimalistProcessUserSubContentInfo()));
  }
}
