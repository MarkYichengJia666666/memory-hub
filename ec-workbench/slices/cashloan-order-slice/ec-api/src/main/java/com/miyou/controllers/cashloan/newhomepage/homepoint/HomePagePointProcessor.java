package com.miyou.controllers.cashloan.newhomepage.homepoint;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;

public abstract class HomePagePointProcessor implements IHomePageFieldProcessor<HomePagePointResponse, HomepagePointProcessorType> {
  @Override
  public HomepagePointProcessorType getProcessorType() {
    return getPointProcessorType();
  }

  protected abstract HomepagePointProcessorType getPointProcessorType();
}
