package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.LoanMarketForOldHomePageResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ReapplyUserInfoProcessor extends AbstractCreditStatusUserInfoProcessor {

  @Override
  public HomePageMainCardInfo getHomePageMainCardInfo(HomePageContext homePageContext, BigDecimal maxCreditsBySDK) {
    return homePageMainCardInfoTool.getReCalcCreditsMainCardInfo(maxCreditsBySDK);
  }

  @Override
  public UserResponse.LoanMarketInfoResponse getLoanMarketInfoResponse(HomePageContext homePageContext) {
    LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext, true);
    return homepageContentTool.showLoanMarketEntranceInMainCard(homePageContext, loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk) ?
        new UserResponse.LoanMarketInfoResponse(TT.gen("查看其他贷款平台"), homepageV5Config.getRejectLoanMarketInfoUrl(), loanMarketCheckResult.riskFlowCheckType) : null;
  }

  @Override
  public LoanMarketForOldHomePageResponse buildLoanMarketForOldHomePageResp(HomePageContext homePageContext) {
    LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext, true);
    return homepageContentTool.buildLoanMarketForOldHomePageResp(homePageContext, loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk);
  }

  @Override
  public BigDecimal getDisplayCredit(HomePageContext homePageContext, BigDecimal maxCreditsBySDK) {
    return maxCreditsBySDK;
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.REAPPLY_USER_INFO;
  }
}
