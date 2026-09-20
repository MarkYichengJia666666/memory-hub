package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

@Service
public class NeedSupplementUserInfoProcessor extends AbstractUserInfoProcessor {

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    userResponse
        .setTitle(TT.gen("继续借款申请"))
        .setContent(TT.gen("请您进行身份核验，完成后将会立即完成审批。"))
        .setButtonName(TT.gen("去申请"))
        .setHomePageMainCardInfo(homePageMainCardInfoTool.getNeedSupplementMainCardInfo());
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.NEED_SUPPLEMENT_USER_INFO;
  }
}
