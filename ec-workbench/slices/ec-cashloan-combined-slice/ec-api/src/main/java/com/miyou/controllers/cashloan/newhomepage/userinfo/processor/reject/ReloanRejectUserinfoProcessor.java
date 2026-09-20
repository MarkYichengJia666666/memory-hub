package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import org.springframework.stereotype.Service;

@Service
public class ReloanRejectUserinfoProcessor extends AbstractUserInfoProcessor {

  @Override
  public void process(UserResponse userInfo, HomePageContext homePageContext) {
    userInfo
        .setHomePageGuideBubbleInfo(homepageContentTool.getHomePageGuideBubbleInfo(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild()));
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.RELOAN_REJECT_USER_INFO;
  }
}
