package com.miyou.controllers.cashloan.newhomepage.homepoint;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.HomePointHolder;
import org.springframework.stereotype.Service;

@Service
public class ReapplyPointProcessor extends HomePagePointProcessor {
  @Override
  public void process(HomePagePointResponse fieldsInfo, HomePageContext homePageContext) {
    fieldsInfo.getHomePointHolder().addCreditsStatusPoint(homePageContext.getStatus() == IDNHomepageLoanStatusV5.CAN_REAPPLY_NOW ?
        HomePointHolder.PointCreditsStatus.REJECT_CAN_REAPPLY : HomePointHolder.PointCreditsStatus.NOT_SUBMIT);
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.REAPPLY_POINT;
  }
}
