package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.loanmarket.LoanMarketCardElementProvider;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.LoanMarketForOldHomePageResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.homepage.display.IncreaseReviewRejectSupplier;
import com.yqg.core.service.homepage.display.dto.HomePageInfo;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class IncreaseReviewRejectUserInfoProcessor extends AbstractUserInfoProcessor {
  @Autowired
  private IncreaseReviewRejectSupplier increaseReviewRejectSupplier;
  @Autowired
  private LoanMarketCardElementProvider loanMarketCardElementProvider;
  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;

  @Override
  public void process(UserResponse userInfo, HomePageContext homePageContext) {
    IDNHomepageLoanStatusV5 status = homePageContext.getStatus();
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    HomePageInfo homeInfo = increaseReviewRejectSupplier.getHomePageInfoForExtraInfoReapply(status, creditsInfoVO);

    LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext, true);
    UserResponse.LoanMarketInfoResponse loanMarketInfoResponse = loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk ?
        new UserResponse.LoanMarketInfoResponse(TT.gen("查看其他贷款平台"), homepageV5Config.getRejectLoanMarketInfoUrl(), loanMarketCheckResult.riskFlowCheckType) : null;

    LoanMarketForOldHomePageResponse loanMarketForOldHomePageResponse = homepageContentTool.buildLoanMarketForOldHomePageResp(homePageContext, loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk);
    Boolean showLoanMarketCardForOldHomePage = Objects.nonNull(loanMarketForOldHomePageResponse);
    userInfo
        .setTitle(homeInfo.title)
        .setContent(homeInfo.subTitle)
        .setRejectedUserDisplayStrategy(homeInfo.rejectedUserDisplayStrategy)
        .setCanButtonDisplay(homeInfo.buttonInfo.show)
        .setButtonName(homeInfo.buttonInfo.title)
        .setButtonJumpUrl(homeInfo.buttonInfo.url)
        .setLoanMarketInfoResponse(showLoanMarketCardForOldHomePage ? null : loanMarketInfoResponse)
        .setShowLoanMarketInfo(loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk)
        .setHomePageMainCardInfo(HomePageMainCardInfo.builder()
            .title(homeInfo.homePageNewInfo.title)
            .content(homeInfo.homePageNewInfo.subTitle)
            .buttonContent(homeInfo.homePageNewInfo.buttonInfo.title)
            .buttonUrl(homeInfo.homePageNewInfo.buttonInfo.url)
            .loanMarketInfoResponse(showLoanMarketCardForOldHomePage ? null : loanMarketInfoResponse)
            .build())
        .setLoanMarketCardForV2Response(loanMarketForOldHomePageResponse);
    if (HomepageVersion.V2 == homePageContext.getHomepageV3ExperimentContext().getHomepageDisplayVersion() && !showLoanMarketCardForOldHomePage
        && homePageContext.loanMarketGotoLinkReported.compareAndSet(false, true)) {
      sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.gotoLinkParam(homePageContext));
      loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
          homePageContext, LoanMarketReportType.GOTO_LINK, getClass());
    }
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.INCREASE_REVIEW_REJECT_USER_INFO;
  }
}
