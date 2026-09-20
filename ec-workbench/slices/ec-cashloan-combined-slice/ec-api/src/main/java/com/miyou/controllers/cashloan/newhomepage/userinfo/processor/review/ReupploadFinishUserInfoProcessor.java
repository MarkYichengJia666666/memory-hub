package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.review;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

@Service
public class ReupploadFinishUserInfoProcessor extends AbstractReviewUserInfoProcessor {

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext) {
    HomepageUserParamsVO paramsVO = homePageContext.getHomepageUserParamsVO();
    return homePageMainCardInfoTool.getReviewMainCardInfo(paramsVO, homePageContext);
  }

  @Override
  public TT getTitle(HomePageContext homePageContext) {
    return TT.gen("自动审核中");
  }

  @Override
  public void updateUserResponseForRevolvingUserReview(UserResponse userResponse, HomePageContext homePageContext) {

  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.REUPLOAD_FINISHED_USER_INFO;
  }
}
