package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.orderbizcheck;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.bizcheck.enums.BizCheckGroup;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

@Service
public class GrabBizCheckUserInfoProcessor extends AbstractBizCheckUserInfoProcessor {
  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.GRAB_ORDER_BIZ_CHECK;
  }

  @Override
  public TT getBizCheckTitle() {
    return TT.gen("正在准备您的资金");
  }

  @Override
  public BizCheckGroup getBizCheckGroup() {
    return BizCheckGroup.GRAB;
  }

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(UserResponse userResponse, HomePageContext homePageContext) {
    return homePageMainCardInfoTool.getFundCheckingMainCard(userResponse.content, homePageContext);
  }

  @Override
  public void updateUserResponseAndMainCardInfoForRevolvingLoan(UserResponse userResponse, HomePageContext homePageContext) {
    if (homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()) {
      String loanTipsPopup = homepageV5Config.getRevolvingLoanTipsPopUpWhenPaying();
      userResponse.setLoanTipsPopUp(LoanTipsPopUp.from(JsonUtils.fromOrNull(loanTipsPopup, LoanTipsPopUpVO.class)));
      userResponse.setTitle(TT.gen("打款中"));
      userResponse.setContent(TT.gen("预计3分钟完成，请耐心等待"));
    }
  }
}
