package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.loanmarket.LoanMarketCardElementProvider;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.miyou.controllers.cashloan.response.v5.user.LoanMarketForOldHomePageResponse;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.response.v5.user.ReserveLoanCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.homepage.display.dto.HomePageInfo;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RejectUserinfoProcessor extends AbstractUserInfoProcessor {
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private LoanMarketCardElementProvider loanMarketCardElementProvider;
  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;

  @Override
  public void process(UserResponse userInfo, HomePageContext homePageContext) {
    boolean hasReadyOrder = CollectionUtils.isNotEmpty(homePageContext.getHomepageUserParamsVO().readyOrderList);
    HomePageInfo homeInfo = rejectedSupplier.getHomeInfo(homePageContext.getStatus(),
        homePageContext.getUserCreditsContext().getCreditsInfoVO(),
        homePageContext.getUserDeviceContextVO().getBuild(),
        hasReadyOrder);
    boolean cannotReapply = homepageContentTool.isNotQualifiedToReapply(homePageContext);
    LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext, !cannotReapply);
    boolean showLoanMarketEntrance = loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk;

    UserResponse.LoanMarketInfoResponse loanMarketInfoResponse = showLoanMarketEntrance ?
        new UserResponse.LoanMarketInfoResponse(TT.gen("查看其他贷款平台"), homepageV5Config.getRejectLoanMarketInfoUrl(), loanMarketCheckResult.riskFlowCheckType) : null;
    boolean showLoanMarketInfoButton = showLoanMarketEntrance && Objects.isNull(homeInfo.buttonInfo.title);
    boolean showLoanMarketInfoButtonForNewHomePage = showLoanMarketEntrance && Objects.isNull(homeInfo.homePageNewInfo.buttonInfo.title);
    HomeDisplayStrategy revolvingReviewDisplayStrategy = homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy;
    LoanTipsPopUp loanTipsPopUp = LoanTipsPopUp.from(homeInfo.loanTipsPopUpVO);
    if (homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy.isStrategyB()) {
      loanTipsPopUp = LoanTipsPopUp.from(
          JsonUtils.fromOrNull(homePageMainCardInfoTool.getRepaymentLoanTipsPopup(HomeDisplayStrategy.B, homePageContext),
              LoanTipsPopUpVO.class));
    }
    LoanMarketForOldHomePageResponse loanMarketForOldHomePageResponse = homepageContentTool.buildLoanMarketForOldHomePageResp(homePageContext, showLoanMarketEntrance);
    boolean showLoanMarketForOldHomePage = Objects.nonNull(loanMarketForOldHomePageResponse);

    userInfo.setTitle(homeInfo.title)
        .setContent(homeInfo.subTitle)
        .setRejectedUserDisplayStrategy(homeInfo.rejectedUserDisplayStrategy)
        .setCanButtonDisplay(homeInfo.buttonInfo.show)
        .setButtonName(showLoanMarketInfoButton ? TT.gen("查看其他贷款平台") : homeInfo.buttonInfo.title)
        .setButtonJumpUrl(showLoanMarketInfoButton ? homepageV5Config.getRejectLoanMarketInfoUrl() : homeInfo.buttonInfo.url)
        .setLoanMarketRule(loanMarketCheckResult.riskFlowCheckType)
        .setTextAboveButton(homeInfo.textAboveButton)
        .setAboveButtonIconUrl(homeInfo.aboveButtonIconUrl)
        .setReserveLoanCardInfo(Objects.isNull(homeInfo.reserveLoanCardInfo) ? null : ReserveLoanCardInfo.builder()
            .title(homeInfo.reserveLoanCardInfo.title)
            .reserveLoanButtonJumpUrl(homeInfo.reserveLoanCardInfo.url)
            .build())
        .setLoanTipsPopUp(loanTipsPopUp)
        .setRejectHomeDisplayStrategy(homeInfo.rejectHomeDisplayStrategy)
        .setShowLoanMarketInfo(showLoanMarketEntrance)
        .setLoanMarketInfoResponse(showLoanMarketInfoButton || showLoanMarketForOldHomePage ? null : loanMarketInfoResponse)
        .setRevolvingReviewDisplayStrategy(revolvingReviewDisplayStrategy)
        .setHomePageMainCardInfo(homePageMainCardInfoTool.getMainCardForRejectUser(homeInfo, showLoanMarketInfoButtonForNewHomePage, loanMarketInfoResponse, userInfo, homePageContext, showLoanMarketEntrance))
        .setLoanMarketCardForV2Response(loanMarketForOldHomePageResponse);
    eventTrackForV2(homePageContext, showLoanMarketInfoButton, showLoanMarketForOldHomePage, loanMarketInfoResponse);
  }

  public void eventTrackForV2(HomePageContext homePageContext, boolean showLoanMarketInfoButton, boolean showLoanMarketForOldHomePage, UserResponse.LoanMarketInfoResponse loanMarketInfoResponse){
    if (HomepageVersion.V2 == homePageContext.getHomepageV3ExperimentContext().getHomepageDisplayVersion() && !showLoanMarketForOldHomePage) {
      if (showLoanMarketInfoButton && homePageContext.loanMarketMainButtonReported.compareAndSet(false, true)) {
        sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.mainButtonParam(homePageContext));
        loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
            homePageContext, LoanMarketReportType.MAIN_BUTTION, getClass());
      }
      if (!showLoanMarketInfoButton
          && Objects.nonNull(loanMarketInfoResponse)
          && homePageContext.loanMarketGotoLinkReported.compareAndSet(false, true)) {
        sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.gotoLinkParam(homePageContext));
        loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
            homePageContext, LoanMarketReportType.GOTO_LINK, getClass());
      }
    }
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.REJECT;
  }
}
