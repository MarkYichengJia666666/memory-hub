package com.miyou.controllers.cashloan.newhomepage.pageconfig;

import com.miyou.controllers.cashloan.newhomepage.H5WholeProcessOrderUIAbTestService;
import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomePageConfigProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageConfigResponse;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.service.cashloan.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * H5全流程下单跳转UI配置处理器
 * 负责处理H5全流程下单跳转UI的实验配置，包括跳转动画效果等
 */
@Service
public class H5WholeProcessOrderUIConfigProcessor implements IHomePageFieldProcessor<HomepageConfigResponse, HomePageConfigProcessorType> {
  @Autowired
  private H5WholeProcessOrderUIAbTestService H5WholeProcessOrderUIAbTestService;

  @Autowired
  private AuthService authService;

  @Override
  public void process(HomepageConfigResponse fieldsInfo, HomePageContext homePageContext) {

    if (Objects.isNull(homePageContext.getUserId())
        || !authService.isAuthFinishedByUserId(homePageContext.getUserId())
        || Objects.isNull(homePageContext.getUserDeviceContextVO().getClientType())
        || !RequestClientType.isWholeProcess(homePageContext.getUserDeviceContextVO().getClientType())) {
      return;
    }
    
    fieldsInfo.H5WholeProcessOrderUIConfig = H5WholeProcessOrderUIAbTestService.fetchH5WholeProcessOrderUIConfigResponse(
        homePageContext.getUserId(),
        homePageContext.getUserDeviceContextVO().getBuild(),
        homePageContext.getSdkType());
  }
  


  @Override
  public HomePageConfigProcessorType getProcessorType() {
    return HomePageConfigProcessorType.H5_WHOLE_PROCESS_ORDER_UI_CONFIG;
  }
}