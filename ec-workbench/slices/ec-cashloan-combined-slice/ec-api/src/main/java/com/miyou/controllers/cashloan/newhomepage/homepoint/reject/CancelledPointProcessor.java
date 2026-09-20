package com.miyou.controllers.cashloan.newhomepage.homepoint.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.newhomepage.homepoint.HomePagePointProcessor;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomePointHolder;
import org.springframework.stereotype.Service;

@Service
public class CancelledPointProcessor extends HomePagePointProcessor {
  @Override
  public void process(HomePagePointResponse fieldsInfo, HomePageContext homePageContext) {
    fieldsInfo.getHomePointHolder().addCreditsStatusPoint(HomePointHolder.PointCreditsStatus.PERMANENTLY_REJECTED);
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.CANCELLED_POINT;
  }
}
