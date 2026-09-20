package com.miyou.controllers.cashloan.newhomepage.userinfov3;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageUserInfoV3Response;
import org.springframework.stereotype.Component;

@Component
public class HomepageUserInfoV3ResponseFactory extends AbstractHomePageResponseFactory<PageUserInfoV3Response, HomepageUserInfoV3ProcessorType, AbstractHomepageUserInfoV3Processor> {
  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, PageUserInfoV3Response value) {
    homepageResponseV5.setPageUserInfoV3(value);
  }

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.PAGE_USER_INFO_V3;
  }
}