package com.miyou.controllers.cashloan.newhomepage.homepoint;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.cashloan.homepage.HomepagePointService;
import com.yqg.core.service.cashloan.homepage.vo.HomePointHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BaseHomepagePointDataProcessor extends HomePagePointProcessor {
  @Autowired
  private HomepagePointService homepagePointService;
  @Override
  public void process(HomePagePointResponse fieldsInfo, HomePageContext homePageContext) {
    fieldsInfo.setHomePointHolder(HomePointHolder.create(homePageContext.getStatus(), homePageContext.getHomepageUserParamsVO(), homepagePointService));
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.BASE_POINT;
  }
}
