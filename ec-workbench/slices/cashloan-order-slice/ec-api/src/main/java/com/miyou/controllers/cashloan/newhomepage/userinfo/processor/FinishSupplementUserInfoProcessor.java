package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

@Service
public class FinishSupplementUserInfoProcessor extends AbstractUserInfoProcessor {

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    userResponse
        .setTitle(TT.gen("身份验证中"))
        .setContent(TT.gen("正在进行身份信息核验，预计30秒内完成，请稍等。"))
        .setHomePageMainCardInfo(homePageMainCardInfoTool.getFinishSupplementMainCardInfo())
    ;
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.FINISH_SUPPLEMENT_USER_INFO;
  }
}
