package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.review;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class CalcCreditsInReviewUserInfoProcessor extends AbstractReviewUserInfoProcessor {

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.CALC_CREDITS_IN_REVIEW_USER_INFO;
  }

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext) {
    HomepageUserParamsVO paramsVO = homePageContext.getHomepageUserParamsVO();
    HomeDisplayStrategy revolvingReviewDisplayStrategy = homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy;
    return homePageMainCardInfoTool.getReCalcCreditsReviewMainCardInfo(revolvingReviewDisplayStrategy, paramsVO);
  }

  @Override
  public TT getTitle(HomePageContext homePageContext) {
    return TT.gen("额度测算中");
  }

  @Override
  public void updateUserResponseForRevolvingUserReview(UserResponse userResponse, HomePageContext homePageContext) {
    String loanTipsPopupStr = homepageV5Config.getRevolvingLoanCalcReviewLoanTipsPopUp();
    LoanTipsPopUp loanTipsPopUp = LoanTipsPopUp.from(JsonUtils.fromOrNull(loanTipsPopupStr, LoanTipsPopUpVO.class));
    if (Objects.nonNull(loanTipsPopUp)) {
      userResponse.setLoanTipsPopUp(loanTipsPopUp);
    }
    log.info("loanTipsPopUp is {}, userId is {}", JsonUtils.toString(loanTipsPopUp), homePageContext.getUserId());
  }
}
