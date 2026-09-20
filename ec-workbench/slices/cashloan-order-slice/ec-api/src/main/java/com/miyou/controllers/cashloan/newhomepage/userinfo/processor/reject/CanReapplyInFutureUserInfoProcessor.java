package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.loanmarket.LoanMarketCardElementProvider;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.miyou.controllers.cashloan.response.v5.user.*;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.homepage.display.CanReapplyInFutureSupplier;
import com.yqg.core.service.homepage.display.dto.HomePageInfo;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CanReapplyInFutureUserInfoProcessor extends AbstractUserInfoProcessor {
  @Autowired
  private CanReapplyInFutureSupplier canReapplyInFutureSupplier;
  @Autowired
  private LoanMarketCardElementProvider loanMarketCardElementProvider;
  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;

  @Override
  public void process(UserResponse fieldsInfo, HomePageContext homePageContext) {
    boolean hasReadyOrder = CollectionUtils.isNotEmpty(homePageContext.getHomepageUserParamsVO().readyOrderList);
    HomePageInfo homeInfo = canReapplyInFutureSupplier.getHomeInfo(homePageContext.getStatus(),
        homePageContext.getUserCreditsContext().getCreditsInfoVO(),
        homePageContext.getUserDeviceContextVO().getBuild(),
        hasReadyOrder);

        LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext, BooleanUtils.toBoolean(homeInfo.canReapply));
        boolean marketResult = Objects.nonNull(homeInfo.canReapply) && loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk;
        UserResponse.LoanMarketInfoResponse loanMarketInfoResponse = marketResult ?
                new UserResponse.LoanMarketInfoResponse(TT.gen("查看其他贷款平台"), homepageV5Config.getRejectLoanMarketInfoUrl(), loanMarketCheckResult.riskFlowCheckType) : null;
        boolean showLoanMarketButtonForOldPage = marketResult && Objects.isNull(homeInfo.buttonInfo.title);
        boolean showLoanMarketButtonForNewPage = marketResult && Objects.isNull(homeInfo.homePageNewInfo.buttonInfo.title);

    LoanMarketForOldHomePageResponse loanMarketForOldHomePageResponse = homepageContentTool.buildLoanMarketForOldHomePageResp(homePageContext, loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk);
    boolean showLoanMarketCardForOldHomePage = Objects.nonNull(loanMarketForOldHomePageResponse);

    fieldsInfo.setTitle(homeInfo.title)
        .setContent(homeInfo.subTitle)
        .setRejectedUserDisplayStrategy(homeInfo.rejectedUserDisplayStrategy)
        .setCanButtonDisplay(homeInfo.buttonInfo.show)
        .setButtonName(showLoanMarketButtonForOldPage ? TT.gen("查看其他贷款平台") : homeInfo.buttonInfo.title)
        .setButtonJumpUrl(showLoanMarketButtonForOldPage ? homepageV5Config.getRejectLoanMarketInfoUrl() : homeInfo.buttonInfo.url)
        .setLoanMarketRule(loanMarketCheckResult.riskFlowCheckType)
        .setRejectHomeDisplayStrategy(homeInfo.rejectHomeDisplayStrategy)
        .setTextAboveButton(homeInfo.textAboveButton)
        .setAboveButtonIconUrl(homeInfo.aboveButtonIconUrl)
        .setLoanTipsPopUp(LoanTipsPopUp.from(homeInfo.loanTipsPopUpVO))
        .setReserveLoanCardInfo(Objects.isNull(homeInfo.reserveLoanCardInfo) ? null : ReserveLoanCardInfo.builder()
            .title(homeInfo.reserveLoanCardInfo.title)
            .reserveLoanButtonJumpUrl(homeInfo.reserveLoanCardInfo.url)
            .build())
        .setLoanMarketInfoResponse(showLoanMarketButtonForOldPage || showLoanMarketCardForOldHomePage ? null : loanMarketInfoResponse)
        .setShowLoanMarketInfo(marketResult)
        .setHomePageMainCardInfo(HomePageMainCardInfo.builder()
            .title(homeInfo.homePageNewInfo.title)
            .content(homeInfo.homePageNewInfo.subTitle)
            .buttonContent(showLoanMarketButtonForNewPage ? TT.gen("查看其他贷款平台") : homeInfo.homePageNewInfo.buttonInfo.title)
            .buttonUrl(showLoanMarketButtonForNewPage ? homepageV5Config.getRejectLoanMarketInfoUrl() : homeInfo.homePageNewInfo.buttonInfo.url)
            .loanMarketRule(loanMarketCheckResult.riskFlowCheckType)
            .tips(homeInfo.homePageNewInfo.tips)
            .tipsIcon(homeInfo.homePageNewInfo.tipsIcon)
            .amountStr(homeInfo.homePageNewInfo.amountStr)
            .loanMarketInfoResponse(showLoanMarketButtonForNewPage || showLoanMarketCardForOldHomePage ?
                null : loanMarketInfoResponse)
            .build())
        .setLoanMarketCardForV2Response(loanMarketForOldHomePageResponse);
    eventTrackForV2(homePageContext, showLoanMarketCardForOldHomePage, showLoanMarketButtonForOldPage, loanMarketInfoResponse);
  }

  private void eventTrackForV2(
      HomePageContext homePageContext,
      boolean showLoanMarketCardForOldHomePage,
      boolean showLoanMarketButtonForOldPage,
      UserResponse.LoanMarketInfoResponse loanMarketInfoResponse
  ) {
    if (HomepageVersion.V2 == homePageContext.getHomepageV3ExperimentContext().getHomepageDisplayVersion() && !showLoanMarketCardForOldHomePage) {
      if (showLoanMarketButtonForOldPage && homePageContext.loanMarketMainButtonReported.compareAndSet(false, true)) {
        sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.mainButtonParam(homePageContext));
        loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
            homePageContext, LoanMarketReportType.MAIN_BUTTION, getClass());
      }
      if (!showLoanMarketButtonForOldPage
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
    return HomepageUserInfoProcessorType.CAN_REAPPLY_IN_FUTURE;
  }
}
