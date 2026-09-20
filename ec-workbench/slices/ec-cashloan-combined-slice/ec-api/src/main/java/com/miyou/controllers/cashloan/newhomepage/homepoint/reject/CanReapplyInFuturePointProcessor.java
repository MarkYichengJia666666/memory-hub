package com.miyou.controllers.cashloan.newhomepage.homepoint.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.newhomepage.homepoint.HomePagePointProcessor;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.homepage.display.CanReapplyInFutureSupplier;
import com.yqg.core.service.homepage.display.dto.HomePageInfo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CanReapplyInFuturePointProcessor extends HomePagePointProcessor {
  @Autowired
  private CanReapplyInFutureSupplier canReapplyInFutureSupplier;
  @Override
  public void process(HomePagePointResponse pagePointResponse, HomePageContext homePageContext) {
    boolean hasReadyOrder = CollectionUtils.isNotEmpty(homePageContext.getHomepageUserParamsVO().readyOrderList);
    HomePageInfo homeInfo = canReapplyInFutureSupplier.getHomeInfo(homePageContext.getStatus(), homePageContext.getUserCreditsContext().getCreditsInfoVO(), homePageContext.getUserDeviceContextVO().getBuild(), hasReadyOrder);
    pagePointResponse.getHomePointHolder()
        .addCreditsStatusPoint(homeInfo.pointCreditsStatus);
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.CAN_REAPPLY_IN_FUTURE_POINT;
  }
}
