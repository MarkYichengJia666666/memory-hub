package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.cancreateorder;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.homepage.display.MinimalistInReviewSupplier;
import com.yqg.core.service.homepage.display.dto.HomePageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MinimalistInReviewUserInfoProcessor extends AbstractUserInfoProcessor {
  @Autowired
  private MinimalistInReviewSupplier minimalistInReviewSupplier;

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    HomePageInfo homePageInfo = minimalistInReviewSupplier.getHomePageInfo(homePageContext.getStatus(), homePageContext.getHomepageUserParamsVO());
    userResponse
        .setTitle(homePageInfo.title)
        .setContent(homePageInfo.subTitle)
        .setRemainTimeEndRisk(homePageInfo.countdownSeconds);
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.MINIMALIST_IN_REVIEW_USER_INFO;
  }
}
