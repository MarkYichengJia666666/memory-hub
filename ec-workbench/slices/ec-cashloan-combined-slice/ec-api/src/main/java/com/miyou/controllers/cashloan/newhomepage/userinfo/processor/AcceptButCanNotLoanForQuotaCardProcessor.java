package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType.ACCEPT_BUT_CAN_NOT_LOAN_FOR_QUOTA_CARD;

@Service
public class AcceptButCanNotLoanForQuotaCardProcessor extends QuotaCardProcessor{
  @Override
  protected BigDecimal getCredits(HomePageContext homePageContext) {
    UserCreditsContext userCreditsContext = homePageContext.getUserCreditsContext();
    return userCreditsContext.calcCreditsForCannotLoanStatus(homePageContext.getHomepageUserParamsVO().homeConfigVO);
  }

  @Override
  public HomepageUserInfoProcessorType getProcessorType() {
    return ACCEPT_BUT_CAN_NOT_LOAN_FOR_QUOTA_CARD;
  }
}