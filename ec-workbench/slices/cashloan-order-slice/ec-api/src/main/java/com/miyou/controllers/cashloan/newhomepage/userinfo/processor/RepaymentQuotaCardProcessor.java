package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.yqg.common.util.math.BigDecimalHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RepaymentQuotaCardProcessor extends QuotaCardProcessor {

  @Override
  protected BigDecimal getCredits(HomePageContext homePageContext) {
    if (!BigDecimalHelper.lessThan(homePageContext.getUserProductVO().getEnableVirtualCredits(), BigDecimal.ZERO)) {
      return homePageContext.getUserProductVO().getEnableVirtualCredits();
    }
    return super.getCredits(homePageContext);
  }
    @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.REPAYMENT_QUOTA_CARD_INFO;
  }
}
