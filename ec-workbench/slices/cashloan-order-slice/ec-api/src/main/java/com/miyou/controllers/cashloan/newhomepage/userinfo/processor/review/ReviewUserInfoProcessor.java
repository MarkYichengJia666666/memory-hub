package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.review;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class ReviewUserInfoProcessor extends AbstractReviewUserInfoProcessor {

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext) {
    HomepageUserParamsVO paramsVO = homePageContext.getHomepageUserParamsVO();
    //TODO(LTB) 循环贷 这写的令人困惑 已经有了测额 inReview 了，这里应该就是二次 inReview，打印日志确认下
    log.info("paramsVO.latestOrderReserveStatus() is {}", paramsVO.latestOrderReserveStatus());
    return paramsVO.latestOrderReserveStatus()
        ? homePageMainCardInfoTool.getOrderReview(paramsVO, homePageContext)
        : homePageMainCardInfoTool.getReviewMainCardInfo(paramsVO, homePageContext);
  }

  @Override
  public TT getTitle(HomePageContext homePageContext) {
    if(homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()){
      HomepageUserParamsVO paramsVO = homePageContext.getHomepageUserParamsVO();
      if(paramsVO.latestOrderReserveStatus()){
        return TT.gen("审核中");
      }
    }
    return TT.gen("自动审核中");
  }

  @Override
  public void updateUserResponseForRevolvingUserReview(UserResponse userResponse, HomePageContext homePageContext) {
    String loanTipsPopupStr = homepageV5Config.getRevolvingLoanTipsPopUpWhenPaying();
    LoanTipsPopUp loanTipsPopUp = LoanTipsPopUp.from(JsonUtils.fromOrNull(loanTipsPopupStr, LoanTipsPopUpVO.class));
    if (Objects.nonNull(loanTipsPopUp)) {
      userResponse.setLoanTipsPopUp(loanTipsPopUp);
    }
    log.info("loanTipsPopUp is {}, userId is {}", JsonUtils.toString(loanTipsPopUp), homePageContext.getUserId());
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.IN_REVIEW_USER_INFO;
  }
}
