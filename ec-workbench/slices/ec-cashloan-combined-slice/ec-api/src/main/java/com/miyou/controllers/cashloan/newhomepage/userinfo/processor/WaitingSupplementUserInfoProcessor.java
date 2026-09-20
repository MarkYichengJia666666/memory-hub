package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

@Service
public class WaitingSupplementUserInfoProcessor extends AbstractUserInfoProcessor {

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    userResponse
        .setTitle(TT.gen("申请审批中"))
        .setContent(TT.gen("正在审核您的借款申请，预计1分钟内完成，请耐心等待。"))
        .setHomePageMainCardInfo(homePageMainCardInfoTool.getWaitingSupplementMainCardInfo())
    ;
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.WAITING_SUPPLEMENT_USER_INFO;
  }
}
