package com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.IHomepageHomeContextProcessor;
import com.yqg.core.service.loan.credits.LoanUserVirtualCreditsService;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AcceptButCanNotLoanContextProcessor implements IHomepageHomeContextProcessor {

  @Autowired
  private LoanUserVirtualCreditsService loanUserVirtualCreditsService;
  @Autowired
  private LoanCreditsQuotaService loanCreditsQuotaService;
  @Override
  public void processHomeContext(HomePageContext homePageContext) {
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    Boolean disableVirtualCreditsSuccess = loanUserVirtualCreditsService.disableVirtualCredits(homePageContext.getLoanAccountId(), creditsInfoVO.creditsQuota);
    if (!disableVirtualCreditsSuccess) {
      return;
    }
    RemainCreditsVO remainCreditsVO = loanCreditsQuotaService.getHomepageRemainCredits(homePageContext.getLoanAccountVO());
    // flush context
    homePageContext.flushContext(remainCreditsVO);

  }

  @Override
  public HomePageContextProcessorType getType() {
    return HomePageContextProcessorType.ACCEPT_BUT_CAN_NOT_LOAN;
  }
}
