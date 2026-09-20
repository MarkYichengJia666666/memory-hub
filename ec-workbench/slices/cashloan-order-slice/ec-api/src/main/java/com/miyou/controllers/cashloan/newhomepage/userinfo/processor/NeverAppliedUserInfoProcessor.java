package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.yqg.core.model.sql.loan.account.enums.AuthStep;
import com.yqg.core.service.cashloan.auth.step.AuthStepService;
import com.yqg.core.service.cashloan.auth.vo.AuthStepConditionVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NeverAppliedUserInfoProcessor extends AbstractCreditStatusUserInfoProcessor {

  @Autowired
  private AuthStepService authStepService;

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext, BigDecimal maxCreditsBySDK) {
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

    List<AuthStep> unFinishedStep = authStepService.getUnfinishedSteps(authStepConditionVO);
    return homePageMainCardInfoTool.getNeverAppliedMainCardInfo(homePageContext.getHomepageUserParamsVO(), maxCreditsBySDK, unFinishedStep.size());
  }

  @Override
  public BigDecimal getDisplayCredit(HomePageContext homePageContext, BigDecimal maxCreditsBySDK) {
    return maxCreditsBySDK;
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.NEVER_APPLIED_USER_INFO;
  }
}
