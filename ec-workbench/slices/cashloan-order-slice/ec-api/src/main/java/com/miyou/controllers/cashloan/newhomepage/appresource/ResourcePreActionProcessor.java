package com.miyou.controllers.cashloan.newhomepage.appresource;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.AppResourceProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomeAppResourceResponse;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.overseas.activity.client.api.IActivityUserService;
import com.yqg.overseas.activity.clientcommon.request.CreateActivityUserByExperimentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;
import static com.yqg.core.util.scope.ImpliedContextUtils.sourceType;

@Slf4j
@Service
@Deprecated
public class ResourcePreActionProcessor implements IHomePageFieldProcessor<HomeAppResourceResponse, AppResourceProcessorType> {

  @Autowired
  private IActivityUserService activityUserService;

  @Autowired
  private HomepageV5Config homepageV5Config;

  @Autowired
  private LoanBankAccountService loanBankAccountService;

  @Override
  public AppResourceProcessorType getProcessorType() {
    return AppResourceProcessorType.RESOURCE_CAN_CREATE_ORDER_PRE_ACTION;
  }

  @Override
  public void process(HomeAppResourceResponse fieldsInfo, HomePageContext homePageContext) {
    if (Optional.ofNullable(sourceType()).map(SourceType::isBlockingMarketingResource).orElse(false)
        || Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false) ) {
      return;
    }
    if (homePageContext.getHomePageType() == HomePageType.HOME_PAGE_FOR_LEVEL_1 && homePageContext.hitJumpLevel2Strategy()) {
      log.info("jump to level2 order page cause not construct resource");
      return;
    }

    // 1.版本控制
    Long build = homePageContext.getUserDeviceContextVO().getBuild();
    if (build >= homepageV5Config.getHomePagePopUpStartBuild()) {
      Long userId = homePageContext.getUserId();
      ABTestSceneType abTestSceneType = ABTestSceneType.HOME_PAGE_POPUP_FOR_CASH;

      // 2.ab 分流
      String abResult = "A";

      // 3.推活动
      CreateActivityUserByExperimentRequest request = new CreateActivityUserByExperimentRequest();
      request.setUserId(userId);
      request.setAbTestSceneType(abTestSceneType.name());
      request.setAbResult(abResult);
      try {
        activityUserService.createActivityUserByExperiment(request);
      } catch (Exception e) {
        log.error("ResourcePreActionProcessor createActivityUserByExperiment error", e);
      }
    }
  }
}
