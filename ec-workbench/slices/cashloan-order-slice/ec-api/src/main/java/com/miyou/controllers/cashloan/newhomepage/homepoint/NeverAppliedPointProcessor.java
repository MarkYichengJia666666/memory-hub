package com.miyou.controllers.cashloan.newhomepage.homepoint;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.model.sql.loan.account.enums.AuthStep;
import com.yqg.core.service.cashloan.auth.step.AuthStepService;
import com.yqg.core.service.cashloan.auth.vo.AuthStepConditionVO;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NeverAppliedPointProcessor extends HomePagePointProcessor {
  @Autowired
  private AuthStepService authStepService;
  @Override
  public void process(HomePagePointResponse pagePointResponse, HomePageContext homePageContext) {
    HomepageUserParamsVO paramsVO = homePageContext.getHomepageUserParamsVO();
    UserDeviceContextVO viewerContext = homePageContext.getUserDeviceContextVO();
    AuthStepConditionVO authStepConditionVO = new AuthStepConditionVO(paramsVO.getLoanAccountId(),
        paramsVO.getSDKType(),
        paramsVO.build,
        viewerContext.getPlatformType(),
        viewerContext.getSourceType(),
        viewerContext.getChannel()
    );
    List<AuthStep> unFinishedStep = authStepService.getUnfinishedSteps(authStepConditionVO);
    pagePointResponse.getHomePointHolder().addUnCompleteAuthPoint(unFinishedStep);
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.NEVER_APPLIED_POINT;
  }
}
