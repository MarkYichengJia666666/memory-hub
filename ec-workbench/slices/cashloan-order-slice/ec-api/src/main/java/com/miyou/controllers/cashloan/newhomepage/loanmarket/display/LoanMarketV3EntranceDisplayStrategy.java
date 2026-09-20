package com.miyou.controllers.cashloan.newhomepage.loanmarket.display;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.service.loanmarket.config.LoanMarketConfig;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 贷超卡片&贷超用户信息 V3的贷超入口展示策略&贷超中通位入口，与 LoanMarketProcessor、LoanMarketInfoV3Processor、LoanMarketSmeEntranceMiddleInfoProcessor中“影响贷超入口展示”的逻辑一一对应。
 */
@Component
public class LoanMarketV3EntranceDisplayStrategy implements LoanMarketEntranceDisplayStrategy {

  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private LoanMarketConfig loanMarketConfig;

  @Override
  public LoanMarketEntranceDisplayStrategyType getType() {
    return LoanMarketEntranceDisplayStrategyType.LOAN_MARKET_V3;
  }

  @Override
  public LoanMarketEntranceDisplayDecision evaluate(HomePageContext context, boolean skipExperimentAndVersion) {
    boolean cannotReapply = homepageContentTool.isNotQualifiedToReapply(context);
    LoanMarketUserQualifyCheckResult checkResult =
        homepageContentTool.postProcessLoanMarketCheckResult(context, !cannotReapply, skipExperimentAndVersion);
    if (!checkResult.qualifiedLoanMarketEntranceByRisk) {
      return LoanMarketEntranceDisplayDecision.notShow();
    }
    // 在贷&有逾期,需要逾期天数(当前+历史)满足限制
    boolean overdue = context.getHomepageUserParamsVO().isOverdue();
    if (overdue) {
      if (!homepageContentTool.checkUserOverdueDaysWithHistory(context,
          skipExperimentAndVersion)) {
        return LoanMarketEntranceDisplayDecision.notShow();
      }
    } else {
      //版本控制
      if (!skipExperimentAndVersion
          && context.getUserDeviceContextVO().getBuild() < loanMarketConfig.getLoanMarketDisplayExprStartBuild()) {
        return LoanMarketEntranceDisplayDecision.notShow();
      }
    }
    return paddingDecision(checkResult, overdue);
  }

  private LoanMarketEntranceDisplayDecision paddingDecision(LoanMarketUserQualifyCheckResult checkResult, boolean overdue) {
    LoanMarketEntranceDisplayDecision loanMarketEntranceDisplayDecision = new LoanMarketEntranceDisplayDecision();
    loanMarketEntranceDisplayDecision.setShowLoanMarketEntrance(checkResult.qualifiedLoanMarketEntranceByRisk);
    loanMarketEntranceDisplayDecision.setLoanMarketRule(checkResult.riskFlowCheckType);
    loanMarketEntranceDisplayDecision.setOverdue(overdue);
    return loanMarketEntranceDisplayDecision;
  }
}
