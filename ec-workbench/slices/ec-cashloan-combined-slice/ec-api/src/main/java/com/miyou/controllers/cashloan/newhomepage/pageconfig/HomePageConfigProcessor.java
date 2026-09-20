package com.miyou.controllers.cashloan.newhomepage.pageconfig;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomePageConfigProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageConfigResponse;
import com.yqg.core.service.cashloan.HomepageWebChannelConfig;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.loan.repayment.experiment.RepaymentExperimentSupport;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class HomePageConfigProcessor implements IHomePageFieldProcessor<HomepageConfigResponse, HomePageConfigProcessorType> {
  @Autowired
  private RepaymentExperimentSupport repaymentExperimentSupport;
  @Autowired
  private HomepageWebChannelConfig homepageWebChannelConfig;

  @Override
  public void process(HomepageConfigResponse fieldsInfo, HomePageContext homePageContext) {
    fieldsInfo.version = homePageContext.getHomepageV3ExperimentContext().getHomepageDisplayVersion();
    if (needExperimentForRepaymentDisplay(homePageContext)) {
      fieldsInfo.repaymentEnhancementStrategy = repaymentExperimentSupport.getRepaymentReminderStrategy(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());
    }
    if (IDNHomepageLoanStatusV5.SECOND_RISK_REJECT_DISPLAY.contains(homePageContext.getStatus())) {
      fieldsInfo.secondRiskRejectDisplayStrategy = homePageContext.getPrepareAbTestVO().secondRiskRejectDisplayStrategy;
    }
    fieldsInfo.channelMergeInfo = homePageContext.getHomePageContextHolder().getChannelMergeInfoVO();
    UserDeviceContextVO deviceCtx = homePageContext.getUserDeviceContextVO();
    if (deviceCtx != null && deviceCtx.getSourceType() != null
        && deviceCtx.getSourceType().isWebSourceType()) {
      fieldsInfo.webChannelRevolvingEnabled =
          homepageWebChannelConfig.isWebChannelRevolvingEnabled(deviceCtx.getChannel());
    }
  }

  private Boolean needExperimentForRepaymentDisplay(HomePageContext homePageContext) {
    if (Objects.isNull(homePageContext.getHomepageUserParamsVO())) {
      return false;
    }
    return CollectionUtils.isNotEmpty(homePageContext.getHomepageUserParamsVO().readyOrderList);
  }

  @Override
  public HomePageConfigProcessorType getProcessorType() {
    return HomePageConfigProcessorType.COMMON_CONFIG;
  }

}
