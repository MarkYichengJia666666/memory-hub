package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.review;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

@Service
public class MultiLoanInReviewUserInfoProcessor extends AbstractReviewUserInfoProcessor{
  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.MULTI_LOAN_IN_REVIEW_USER_INFO;
  }

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext) {
    HomepageUserParamsVO paramsVO = homePageContext.getHomepageUserParamsVO();
    return homePageMainCardInfoTool.getOrderReview(paramsVO, homePageContext);
  }

  @Override
  public TT getTitle(HomePageContext homePageContext) {
    return TT.gen("自动审核中");
  }

  @Override
  public void updateUserResponseForRevolvingUserReview(UserResponse userResponse, HomePageContext homePageContext) {

  }
}
