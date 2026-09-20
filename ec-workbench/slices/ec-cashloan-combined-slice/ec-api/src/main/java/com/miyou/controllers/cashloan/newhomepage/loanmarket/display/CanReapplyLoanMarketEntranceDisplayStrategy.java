package com.miyou.controllers.cashloan.newhomepage.loanmarket.display;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 可重新提交授信主卡片链接的贷超入口展示策略，与 CanReapplyProcessor 中“影响贷超入口展示”的逻辑一一对应。
 */
@Component
public class CanReapplyLoanMarketEntranceDisplayStrategy implements LoanMarketEntranceDisplayStrategy {

  @Autowired
  private HomepageContentTool homepageContentTool;

  @Override
  public LoanMarketEntranceDisplayStrategyType getType() {
    return LoanMarketEntranceDisplayStrategyType.CAN_REAPPLY;
  }

  @Override
  public LoanMarketEntranceDisplayDecision evaluate(HomePageContext context, boolean skipExperimentAndVersion) {
    LoanMarketUserQualifyCheckResult checkResult =
        homepageContentTool.postProcessLoanMarketCheckResult(context, true, skipExperimentAndVersion);
    if (!checkResult.qualifiedLoanMarketEntranceByRisk) {
      return LoanMarketEntranceDisplayDecision.notShow();
    }
    return paddingDecision(checkResult);
  }

  private LoanMarketEntranceDisplayDecision paddingDecision(LoanMarketUserQualifyCheckResult checkResult) {
    LoanMarketEntranceDisplayDecision loanMarketEntranceDisplayDecision = new LoanMarketEntranceDisplayDecision();
    loanMarketEntranceDisplayDecision.setShowLoanMarketEntrance(checkResult.qualifiedLoanMarketEntranceByRisk);
    loanMarketEntranceDisplayDecision.setLoanMarketRule(checkResult.riskFlowCheckType);
    return loanMarketEntranceDisplayDecision;
  }
}
