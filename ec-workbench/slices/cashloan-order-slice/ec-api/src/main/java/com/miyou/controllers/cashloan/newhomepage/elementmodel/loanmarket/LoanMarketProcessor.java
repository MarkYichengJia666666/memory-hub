package com.miyou.controllers.cashloan.newhomepage.elementmodel.loanmarket;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.LoanMarketEntranceMonitorService;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayDecision;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyService;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyType;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementColor;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementModuleType;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.controllers.loanmarket.LoanMarketEventTrackParamFactory;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.loanmarket.LoanMarketOverdueExperimentDecisionService;
import com.yqg.core.service.loanmarket.config.LoanMarketConfig;
import com.yqg.core.service.loanmarket.enums.LoanMarketDisplayStrategy;
import com.yqg.core.service.sensors.SensorsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoanMarketProcessor extends AbstractPageCardV3Processor {

  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private LoanMarketConfig loanMarketConfig;
  @Autowired
  private LoanMarketCardElementProvider loanMarketCardElementProvider;
  @Autowired
  private SensorsService sensorsService;
  @Autowired
  private LoanMarketEntranceDisplayStrategyService loanMarketEntranceDisplayStrategyService;
  @Autowired
  private LoanMarketEntranceMonitorService loanMarketEntranceMonitorService;
  @Autowired
  private LoanMarketOverdueExperimentDecisionService loanMarketOverdueExperimentDecisionService;

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.LOAN_MARKET;
  }

  @Override
  public void process(PageCardV3VO fieldsInfo, HomePageContext homePageContext) {
    //版本控制
    if (homePageContext.getUserDeviceContextVO().getBuild() < loanMarketConfig.getLoanMarketDisplayExprStartBuild()) {
      return;
    }
    LoanMarketEntranceDisplayDecision loanMarketDecision = loanMarketEntranceDisplayStrategyService.evaluate(homePageContext,
        LoanMarketEntranceDisplayStrategyType.LOAN_MARKET_V3, false);
    if (!loanMarketDecision.isShowLoanMarketEntrance()) {
      return;
    }
    // 逾期用户场景
    if (loanMarketDecision.isOverdue()) {
      // 贷超入口拓展逾期实验分流
      boolean shownLoanMarket = loanMarketOverdueExperimentDecisionService.decide(homePageContext.getUserId(),
          homePageContext.getUserDeviceContextVO().getBuild(),
          homePageContext.getUserDeviceContextVO().getSourceType());
      if (shownLoanMarket) {
        // 实验组：当前+历史逾期已通过入组检查，直接展示贷超卡片
        fieldsInfo.forceAddAllElements(ElementModuleType.LOAN_MARKET_CARD,
            loanMarketCardElementProvider.buildLoanMarketCard(homePageContext, null));
        if (homePageContext.loanMarketCardReported.compareAndSet(false, true)) {
          sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.cardParam(homePageContext));
          loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
              homePageContext, LoanMarketReportType.CARD, getClass());
        }
      }
      return;
    }
    //无在贷场景+永拒
    //版本控制（在策略已判断）
    LoanMarketDisplayStrategy displayStrategy =
        homepageContentTool.getLoanMarketCardDisplayResult(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild(), homePageContext.getUserDeviceContextVO().getSourceType());
    //如果在实验组且风控要求展示贷超模块，就要组装贷超卡片
    if (displayStrategy.strategyInExprGroup()) {
      fieldsInfo.forceAddAllElements(ElementModuleType.LOAN_MARKET_CARD, loanMarketCardElementProvider.buildLoanMarketCard(homePageContext, ElementColor.GREEN));
      if (homePageContext.loanMarketCardReported.compareAndSet(false, true)) {
        sensorsService.uploadLoanMarketEvent(LoanMarketEventTrackParamFactory.cardParam(homePageContext));
        loanMarketEntranceMonitorService.logLoanMarketEntranceExposure(
            homePageContext, LoanMarketReportType.CARD, getClass());
      }
    }
  }
}
