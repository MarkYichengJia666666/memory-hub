package com.miyou.controllers.cashloan.newhomepage.middle.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayDecision;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyService;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyType;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleListResponse;
import com.yqg.core.service.cashloan.homepage.middle.MiddleConfigVO;
import com.yqg.core.service.loanmarket.LoanMarketOverdueExperimentDecisionService;
import com.yqg.ec.common.enums.loan.SourceType;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoanMarketSmeEntranceMiddleInfoProcessor extends AbstractMiddleInfoProcessor {

  @Autowired
  private LoanMarketEntranceDisplayStrategyService loanMarketEntranceDisplayStrategyService;
  @Autowired
  private LoanMarketOverdueExperimentDecisionService loanMarketOverdueExperimentDecisionService;

  @Override
  public void process(MiddleListResponse middleListResponse, HomePageContext homePageContext) {
    LoanMarketEntranceDisplayDecision loanMarketDecision = loanMarketEntranceDisplayStrategyService.evaluate(homePageContext,
        LoanMarketEntranceDisplayStrategyType.LOAN_MARKET_V3, false);
    if (!loanMarketDecision.isShowLoanMarketEntrance()) {
      return;
    }
    // 逾期用户场景，注意和非逾期用户走的是不同实验
    Long userId = homePageContext.getUserId();
    if (loanMarketDecision.isOverdue()) {
      // 贷超入口拓展逾期实验分流
      Long build = homePageContext.getUserDeviceContextVO().getBuild();
      SourceType sourceType = homePageContext.getUserDeviceContextVO().getSourceType();
      boolean shownLoanMarket = loanMarketOverdueExperimentDecisionService.decide(homePageContext.getUserId(),
          homePageContext.getUserDeviceContextVO().getBuild(),
          homePageContext.getUserDeviceContextVO().getSourceType());
      if (shownLoanMarket) {
        // 展示贷超卡片，不展示中通位
        return;
      }
    }
    // 无在贷场景+永拒
    else if (!homepageContentTool.checkUserNotInLoanMarketCardExpr(homePageContext)) {
      return;
    }

    String middleMessage = super.getMiddleMessage(homePageContext, homepageV5Config.getMiddleMessageWithoutSmeEntrance(), homepageV5Config.getMiddleMessageWithoutSmeEntranceForNewHomepage());

    List<MiddleConfigVO> middleList = homepageMiddleTool.handleInviteUrlToMiddleVo(super.convertJsonToClass(middleMessage), userId);

    middleListResponse
        .setData(super.convertMiddleConfigVOsToRespList(middleList));
  }


  @Override
  public HomepageMiddleProcessorType getProcessorType() {
    return HomepageMiddleProcessorType.LOAN_MARKET_WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR;
  }
}
