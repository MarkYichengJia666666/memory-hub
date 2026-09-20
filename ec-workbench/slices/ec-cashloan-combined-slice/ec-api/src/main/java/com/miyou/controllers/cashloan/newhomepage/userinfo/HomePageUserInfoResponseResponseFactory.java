package com.miyou.controllers.cashloan.newhomepage.userinfo;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class HomePageUserInfoResponseResponseFactory extends AbstractHomePageResponseFactory<UserResponse, HomepageUserInfoProcessorType, AbstractUserInfoProcessor> {

  @Override
  public UserResponse getResult(HomePageContext homePageContext) {
    UserResponse result = super.getResult(homePageContext);
    // 将 HomePageContext.enableRefresh 透传到 userInfo
    result.disableRefresh = homePageContext.getDisableRefresh();
    return result;
  }

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.USER_INFO;
  }

  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, UserResponse value) {
    homepageResponseV5.setUserInfo(value);
  }
}
