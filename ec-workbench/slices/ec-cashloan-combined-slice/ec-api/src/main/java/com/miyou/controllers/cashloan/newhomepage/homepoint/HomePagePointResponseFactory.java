package com.miyou.controllers.cashloan.newhomepage.homepoint;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomePointHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HomePagePointResponseFactory extends AbstractHomePageResponseFactory<HomePagePointResponse, HomepagePointProcessorType, HomePagePointProcessor> {
  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.BURIED_PINT_INTO;
  }

  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, HomePagePointResponse value) {
    homepageResponseV5.reportContentMap = value.getHomePointHolder().getPointDetail();
    homepageResponseV5.reportContent = Optional.ofNullable(value.getHomePointHolder()).map(HomePointHolder::getPointDetailStr).orElse(null);
  }
}
