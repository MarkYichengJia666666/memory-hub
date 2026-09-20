package com.miyou.controllers.cashloan.newhomepage.pageconfig;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.HomePageConfigProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageConfigResponse;
import org.springframework.stereotype.Component;

@Component
public class HomePageConfigResponseFactory extends AbstractHomePageResponseFactory<HomepageConfigResponse, HomePageConfigProcessorType, HomePageConfigProcessor> {
  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, HomepageConfigResponse value) {
    homepageResponseV5.setPageConfig(value);
  }

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.PAGE_CONFIG;
  }
}
