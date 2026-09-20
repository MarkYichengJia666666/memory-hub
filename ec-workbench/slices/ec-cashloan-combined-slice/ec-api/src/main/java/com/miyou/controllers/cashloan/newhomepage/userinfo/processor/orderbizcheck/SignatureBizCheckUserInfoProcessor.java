package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.orderbizcheck;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.bizcheck.enums.BizCheckGroup;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

@Service
public class SignatureBizCheckUserInfoProcessor extends AbstractBizCheckUserInfoProcessor {
  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.SIGNATURE_BIZ_CHECK;
  }

  @Override
  public TT getBizCheckTitle() {
    return TT.gen("已通过，等待签署合同");
  }

  @Override
  public BizCheckGroup getBizCheckGroup() {
    return BizCheckGroup.SIGNATURE;
  }

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(UserResponse userResponse, HomePageContext homePageContext) {
    return homePageMainCardInfoTool.getPreCheckMainCardInfo();
  }

  @Override
  public void updateUserResponseAndMainCardInfoForRevolvingLoan(UserResponse userResponse, HomePageContext homePageContext) {
  }
}
