package com.miyou.controllers.cashloan.newhomepage.elementmodel.auth;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyService;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.ApiRequestAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.cashloan.homepage.config.ApiRequestPath;
import com.yqg.core.service.loanmarket.config.LoanMarketConfig;
import com.yqg.core.service.sensors.SensorsService;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 可重新提交授信processer，主button动作为调用重新上传applist API，对应以下状态
 * CAN_REAPPLY_NOW("当前可重新提交授信", IDNHomepageDisplayStatusV5.ENABLE_REAPPLIED),
 * RELOAN_CAN_REAPPLY_NOW("当前可重新提交授信", IDNHomepageDisplayStatusV5.ENABLE_REAPPLIED),
 */
@Component
@Slf4j
public class CanReapplyProcessor extends AbstractAuthProcessor {
  @Autowired
  protected HomepageContentTool homepageContentTool;
  @Autowired
  protected SensorsService sensorsService;
  @Autowired
  protected LoanMarketConfig loanMarketConfig;
  @Autowired
  private LoanMarketEntranceDisplayStrategyService loanMarketEntranceDisplayStrategyService;
  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;

  @Override
  protected void processCustomElement(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("查看额度"))
        .action(new ApiRequestAction(ApiRequestPath.REAPPLY))
        .build());

    boolean showLoanMarketEntrance = loanMarketEntranceDisplayStrategyService.evaluate(homePageContext,
        LoanMarketEntranceDisplayStrategyType.CAN_REAPPLY, false).isShowLoanMarketEntrance();
    if (homepageContentTool.showLoanMarketEntranceInMainCard(homePageContext, showLoanMarketEntrance)) {
      loanMarketGotoLink(pageCardV3VO, homePageContext);
    }
    //这种case会展示LoanMarket贷超卡片，而不展示此处的链接
  }

  private void loanMarketGotoLink(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    RedirectAction.LoanMarketLinkActionParam loanMarketParam = new RedirectAction.LoanMarketLinkActionParam();
    loanMarketParam.redirectUrl = homepageV5Config.getRejectLoanMarketInfoUrl();
    loanMarketParam.loanMarket = true;
    loanMarketParam.loanMarketRule = homePageContext.getUserCreditsContext().getLoanMarketUserQualifyCheckResult().riskFlowCheckType;

    pageCardV3VO.addElementForMainCard(ElementBuilder.link()
        .id(MainCardElementId.GOTO_LINK_INFO)
        .text(TT.gen("查看其他贷款平台"))
        .action(new RedirectAction(loanMarketParam))
        .build());
    if (homePageContext.loanMarketGotoLinkReported.compareAndSet(false, true)) {
      sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.gotoLinkParam(homePageContext));
      loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
          homePageContext, LoanMarketReportType.GOTO_LINK, getClass());
    }
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.CAN_REAPPLY;
  }
}
