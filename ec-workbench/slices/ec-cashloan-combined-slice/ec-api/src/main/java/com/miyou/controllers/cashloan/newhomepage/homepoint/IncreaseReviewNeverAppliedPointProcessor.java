package com.miyou.controllers.cashloan.newhomepage.homepoint;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomePointHolder;
import org.springframework.stereotype.Service;

@Service
public class IncreaseReviewNeverAppliedPointProcessor extends HomePagePointProcessor {
  @Override
  public void process(HomePagePointResponse fieldsInfo, HomePageContext homePageContext) {
    fieldsInfo.getHomePointHolder().addCreditsStatusPoint(HomePointHolder.PointCreditsStatus.REJECT_CAN_INCREASE_CREDIT);
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.INCREASE_REVIEW_NEVER_APPLIED_POINT;
  }
}
