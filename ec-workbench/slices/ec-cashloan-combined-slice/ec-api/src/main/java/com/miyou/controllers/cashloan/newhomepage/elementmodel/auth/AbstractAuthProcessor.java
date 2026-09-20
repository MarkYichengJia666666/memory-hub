package com.miyou.controllers.cashloan.newhomepage.elementmodel.auth;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.yqg.core.model.sql.loan.account.enums.AuthStep;
import com.yqg.core.service.cashloan.auth.step.AuthStepService;
import com.yqg.core.service.cashloan.auth.vo.AuthStepConditionVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class AbstractAuthProcessor extends AbstractPageCardV3Processor {
  @Autowired
  private AuthStepService authStepService;

  @Override
  public final void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    processCommonElement(pageCardV3VO, homePageContext);
    processCustomElement(pageCardV3VO, homePageContext);
  }

  private void processCommonElement(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    homepageV3CardTool.buildCommonLoanInfoElementForMainCard(pageCardV3VO, homePageContext);
  }

  protected int getUnfinishedStep(HomePageContext homePageContext) {
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
    return Math.max(1, unFinishedStep.size() - 1);
  }

  protected abstract void processCustomElement(PageCardV3VO pageCardV3VO, HomePageContext homePageContext);
}