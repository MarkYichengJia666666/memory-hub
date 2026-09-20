package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.review;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public abstract class AbstractReviewUserInfoProcessor extends AbstractUserInfoProcessor {

  public abstract HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext);

  public abstract TT getTitle(HomePageContext homePageContext);

  public abstract void updateUserResponseForRevolvingUserReview(UserResponse userResponse, HomePageContext homePageContext);


  @Autowired
  private LoanBankAccountService loanBankAccountService;

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    boolean hasAvailableBankAccount =
        loanBankAccountService.hasAvailableBankAccount(homePageContext.getUserId(), homePageContext.getSdkType());
    //循环贷 二次风控审核中 / 放款中 loanTips信息
    if (homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()) {
      updateUserResponseForRevolvingUserReview(userResponse, homePageContext);
    }
    HomeDisplayStrategy revolvingReviewDisplayStrategy = homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy;
    userResponse
        .setTitle(getTitle(homePageContext))
        .setContent(getContent(homePageContext))
        .setHasAvailableBankAccount(hasAvailableBankAccount)
        .setHomePageMainCardInfo(getHomePageMainCardInfo(homePageContext))
        .setRevolvingReviewDisplayStrategy(revolvingReviewDisplayStrategy);
  }

  protected TT getContent(HomePageContext homePageContext) {
    if(homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()){
      HomepageUserParamsVO paramsVO = homePageContext.getHomepageUserParamsVO();
      if(paramsVO.latestOrderReserveStatus()){
        return TT.gen("预计60秒完成，请耐心等待");
      }
    }
    if (autoReview(homePageContext.getUserCreditsContext().getCreditsInfoVO())) {
      return TT.gen(homepageV5Config.getRiskInReviewContent(homePageContext.getUserCreditsContext().getLatestUserRiskTraceVO().riskType));
    }
    return TT.gen(homepageV5Config.getQuoteManualReviewContent());
  }

  private boolean autoReview(LoanUserCreditsInfoVO creditsInfoVO) {
    if (creditsInfoVO.isCreditsAccept()) {
      return true;
    }
    if (creditsInfoVO.isInManualReviewStatus()) {
      return false;
    }
    return Clock.now() - creditsInfoVO.getTimeUpdated() < homepageV5Config.getAutoReviewTextTime();
  }
}
