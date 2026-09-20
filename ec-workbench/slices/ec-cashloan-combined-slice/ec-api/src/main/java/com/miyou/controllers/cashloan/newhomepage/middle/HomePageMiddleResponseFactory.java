package com.miyou.controllers.cashloan.newhomepage.middle;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.newhomepage.middle.processor.AbstractMiddleInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HomePageMiddleResponseFactory extends AbstractHomePageResponseFactory<MiddleListResponse, HomepageMiddleProcessorType, AbstractMiddleInfoProcessor> {

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.MIDDLE_INFO;
  }


  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, MiddleListResponse value) {
    homepageResponseV5.setMiddle(value);
  }
}
