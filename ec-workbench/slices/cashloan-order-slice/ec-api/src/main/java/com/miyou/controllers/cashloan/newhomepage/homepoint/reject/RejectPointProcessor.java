package com.miyou.controllers.cashloan.newhomepage.homepoint.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.newhomepage.homepoint.HomePagePointProcessor;
import com.miyou.controllers.cashloan.response.v5.homepagepoint.HomePagePointResponse;
import com.yqg.core.service.homepage.display.RejectedSupplier;
import com.yqg.core.service.homepage.display.dto.HomePageInfo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RejectPointProcessor extends HomePagePointProcessor {
  @Autowired
  protected RejectedSupplier rejectedSupplier;
  @Override
  public void process(HomePagePointResponse fieldsInfo, HomePageContext homePageContext) {
    // 待后续迁移完成之后解这块埋点
    boolean hasReadyOrder = CollectionUtils.isNotEmpty(homePageContext.getHomepageUserParamsVO().readyOrderList);
    HomePageInfo homeInfo = rejectedSupplier.getHomeInfo(homePageContext.getStatus(), homePageContext.getUserCreditsContext().getCreditsInfoVO(), homePageContext.getUserDeviceContextVO().getBuild(), hasReadyOrder);
    fieldsInfo.getHomePointHolder()
        .addCreditsStatusPoint(homeInfo.pointCreditsStatus);
  }

  @Override
  protected HomepagePointProcessorType getPointProcessorType() {
    return HomepagePointProcessorType.REJECT;
  }
}
