package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MultiLoanInitUserInfoProcessor extends AbstractCreditStatusUserInfoProcessor {

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext, BigDecimal maxCreditsBySDK) {
    BigDecimal userDisplayCredits = homepageContentTool.getUserDisplayCredits(homePageContext.getHomepageUserParamsVO(), homePageContext.getSdkType());
    return homePageMainCardInfoTool.getReCalcCreditsMainCardInfo(userDisplayCredits);
  }

  @Override
  public BigDecimal getDisplayCredit(HomePageContext homePageContext, BigDecimal maxCreditsBySDK) {
    return homepageContentTool.getUserDisplayCredits(homePageContext.getHomepageUserParamsVO(), homePageContext.getSdkType());
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.MULTI_LOAN_INIT_USER_INFO;
  }
}
