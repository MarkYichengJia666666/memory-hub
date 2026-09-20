package com.miyou.controllers.cashloan.newhomepage.homepoint;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.homepage.display.IncreaseReviewRejectSupplier;
import com.yqg.core.service.homepage.display.dto.HomePageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IncreaseReviewRejectProcessor extends HomePagePointProcessor {
  @Autowired
  private IncreaseReviewRejectSupplier increaseReviewRejectSupplier;
  @Override
  public void process(HomePagePointResponse fieldsInfo, HomePageContext homePageContext) {
    HomePageInfo homeInfo = increaseReviewRejectSupplier.getHomePageInfoForExtraInfoReapply(homePageContext.getStatus(), homePageContext.getUserCreditsContext().getCreditsInfoVO());
    fieldsInfo.getHomePointHolder().addCreditsStatusPoint(homeInfo.pointCreditsStatus);
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.INCREASE_REVIEW_REJECT_POINT;
  }
}
