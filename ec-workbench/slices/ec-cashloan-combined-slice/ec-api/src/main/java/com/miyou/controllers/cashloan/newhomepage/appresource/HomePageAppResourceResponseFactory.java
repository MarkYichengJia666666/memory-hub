package com.miyou.controllers.cashloan.newhomepage.appresource;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.AppResourceProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomeAppResourceResponse;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HomePageAppResourceResponseFactory extends AbstractHomePageResponseFactory<HomeAppResourceResponse, AppResourceProcessorType, BannerHomePopupProcessor> {

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.APP_RESOURCE;
  }

  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, HomeAppResourceResponse value) {
    homepageResponseV5.setBanner(value.getBanner());
    homepageResponseV5.setExistBanner(value.isExistBanner());
    homepageResponseV5.setPopupWindow(value.getPopup());
    homepageResponseV5.setOrderPagePushPopup(value.getOrderPagePushPopup());
  }
}
