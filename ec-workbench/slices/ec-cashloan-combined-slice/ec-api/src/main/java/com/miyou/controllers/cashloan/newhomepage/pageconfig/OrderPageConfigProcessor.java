package com.miyou.controllers.cashloan.newhomepage.pageconfig;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.OrderPageAbTestService;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomePageConfigProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageConfigResponse;
import com.yqg.core.service.cashloan.HomepageV5Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderPageConfigProcessor implements IHomePageFieldProcessor<HomepageConfigResponse, HomePageConfigProcessorType> {
  @Autowired
  private OrderPageAbTestService orderPageAbTestService;
  @Autowired
  private HomepageV5Config homepageV5Config;

  @Override
  public void process(HomepageConfigResponse fieldsInfo, HomePageContext homePageContext) {
    if (!homePageContext.homePageLevel2()) {
      return;
    }
    if (homePageContext.getUserDeviceContextVO().getBuild() < homepageV5Config.getOrderPageH5StartVersion()) {
      //低于该版本为native下单页，不进入分流
      return;
    }
    fieldsInfo.orderPageConfig = orderPageAbTestService.fetchOrderPageConfigResponse(
        homePageContext.getUserId(),
        homePageContext.getUserDeviceContextVO().getBuild(),
        homePageContext.getSdkType(),
        homePageContext.getUserDeviceContextVO().getClientType(),
        homePageContext
    );
  }


  @Override
  public HomePageConfigProcessorType getProcessorType() {
    return HomePageConfigProcessorType.ORDER_PAGE_CONFIG;
  }

}
