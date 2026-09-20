package com.miyou.controllers.cashloan.newhomepage.homepoint;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomePointHolder;
import org.springframework.stereotype.Service;

@Service
public class InReviewPointProcessor extends HomePagePointProcessor {
  @Override
  public void process(HomePagePointResponse fieldsInfo, HomePageContext homePageContext) {
    fieldsInfo.getHomePointHolder().addCreditsStatusPoint(HomePointHolder.PointCreditsStatus.IN_REVIEW);
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.IN_REVIEW_POINT;
  }
}
