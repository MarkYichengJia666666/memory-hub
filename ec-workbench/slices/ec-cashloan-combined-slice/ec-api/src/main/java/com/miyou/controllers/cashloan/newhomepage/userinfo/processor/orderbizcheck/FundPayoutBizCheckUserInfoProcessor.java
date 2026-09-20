package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.orderbizcheck;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.bizcheck.enums.BizCheckGroup;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

@Service
public class FundPayoutBizCheckUserInfoProcessor extends AbstractBizCheckUserInfoProcessor {
  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.FUND_PAYOUT_BIZ_CHECK;
  }

  @Override
  public TT getBizCheckTitle() {
    return TT.gen("已申请成功，正在打款中");
  }

  @Override
  public BizCheckGroup getBizCheckGroup() {
    return BizCheckGroup.FUND_PAYOUT;
  }

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(UserResponse userResponse, HomePageContext homePageContext) {
    return homePageMainCardInfoTool.getFundPayingMainCardInfo();
  }

  @Override
  public void updateUserResponseAndMainCardInfoForRevolvingLoan(UserResponse userResponse, HomePageContext homePageContext) {
  }
}
