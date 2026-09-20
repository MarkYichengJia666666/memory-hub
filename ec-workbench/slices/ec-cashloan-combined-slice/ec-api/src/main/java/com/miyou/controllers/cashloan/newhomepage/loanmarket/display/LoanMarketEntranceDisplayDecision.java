package com.miyou.controllers.cashloan.newhomepage.loanmarket.display;

import com.yqg.core.model.sql.risk.enums.RiskIncreaseCreditsReviewStatus;
import com.yqg.core.service.risk.riskflowcheck.enums.RiskFlowCheckType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 贷超入口展示决策结果，仅表示“是否展示贷超”及对应规则，不包含入口构建内容。
 */
@Data
@NoArgsConstructor
public class LoanMarketEntranceDisplayDecision {

  /**
   * 是否展示贷超入口
   */
  private boolean showLoanMarketEntrance = false;

  /**
   * 对应的 loanMarketRule（如 riskFlowCheckType）
   */
  private RiskFlowCheckType loanMarketRule;


  private boolean overdue = false;


  private boolean notQualifiedToReapply = false;


  private boolean rejectWaitingForFirstSubmission = false;


  private boolean rejectPermanently = false;

  private RiskIncreaseCreditsReviewStatus riskIncreaseCreditsReviewStatus;

  public static LoanMarketEntranceDisplayDecision notShow() {
    return new LoanMarketEntranceDisplayDecision();
  }

}
