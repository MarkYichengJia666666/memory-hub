package com.miyou.controllers.cashloan.newhomepage.loanmarket.display;

import static com.yqg.core.model.sql.risk.enums.RiskIncreaseCreditsReviewStatus.REVIEWING_STATUS_LIST;
import static com.yqg.core.model.sql.risk.enums.RiskIncreaseCreditsReviewStatus.RISK_COMPLETE;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.model.generated.tables.records.RiskIncreaseCreditsReviewLogRecord;
import com.yqg.core.model.sql.risk.RiskIncreaseCreditsReviewLogModel;
import com.yqg.core.model.sql.risk.enums.RiskIncreaseCreditsReviewStatus;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 被拒状态主卡片/按钮的贷超入口展示策略，与 RejectCardProcessor 中“影响贷超入口展示”的逻辑一一对应。 仅使用 context 及可通过 context 调用的服务，不依赖 processor 内独有变量。
 */
@Slf4j
@Component
public class RejectStatusLoanMarketEntranceDisplayStrategy implements LoanMarketEntranceDisplayStrategy {

  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private RiskIncreaseCreditsReviewLogModel riskIncreaseCreditsReviewLogModel;

  @Override
  public LoanMarketEntranceDisplayStrategyType getType() {
    return LoanMarketEntranceDisplayStrategyType.REJECT_STATUS;
  }

  @Override
  public LoanMarketEntranceDisplayDecision evaluate(HomePageContext context, boolean skipExperimentAndVersion) {
    LoanMarketEntranceDisplayDecision loanMarketDecision = new LoanMarketEntranceDisplayDecision();
    // 判断用户是否有资格重申
    boolean notQualifiedToReapply = homepageContentTool.isNotQualifiedToReapply(context);
    if (notQualifiedToReapply) {
      return evaluateNotQualified(loanMarketDecision, context, skipExperimentAndVersion);
    }
    // 用户有资格重申，获取最近一次的申请记录
    RiskIncreaseCreditsReviewLogRecord latest =
        riskIncreaseCreditsReviewLogModel.findLatestByLoanAccountId(context.getLoanAccountId());
    if (Objects.isNull(latest)) {
      // 用户有资格但尚未提交申请
      return evaluateWaitingFirstSubmission(loanMarketDecision, context, skipExperimentAndVersion);
    }
    // 检查申请状态
    RiskIncreaseCreditsReviewStatus status = RiskIncreaseCreditsReviewStatus.fromCode(latest.getStatus());
    loanMarketDecision.setRiskIncreaseCreditsReviewStatus(status);
    if (REVIEWING_STATUS_LIST.contains(status)) {
      // 申请正在审核中
      return loanMarketDecision;
    }
    if (RISK_COMPLETE == status) {
      // 审核完成但被永久拒绝
      return evaluatePermanentlyRejected(loanMarketDecision, context, skipExperimentAndVersion);
    }
    return loanMarketDecision;
  }

  private LoanMarketEntranceDisplayDecision evaluateNotQualified(LoanMarketEntranceDisplayDecision loanMarketDecision,
      HomePageContext context, boolean skipExperimentAndVersion) {
    loanMarketDecision.setNotQualifiedToReapply(true);
    UserCreditsContext userCreditsContext = context.getUserCreditsContext();
    if (userCreditsContext == null || userCreditsContext.getCreditsInfoVO() == null) {
      return loanMarketDecision;
    }
    return checkLoanMarketUserQualify(loanMarketDecision, context, false, skipExperimentAndVersion);
  }

  private LoanMarketEntranceDisplayDecision evaluateWaitingFirstSubmission(LoanMarketEntranceDisplayDecision loanMarketDecision,
      HomePageContext context, boolean skipExperimentAndVersion) {
    loanMarketDecision.setRejectWaitingForFirstSubmission(true);
    return checkLoanMarketUserQualify(loanMarketDecision, context, true, skipExperimentAndVersion);
  }

  private LoanMarketEntranceDisplayDecision evaluatePermanentlyRejected(LoanMarketEntranceDisplayDecision loanMarketDecision,
      HomePageContext context, boolean skipExperimentAndVersion) {
    loanMarketDecision.setRejectPermanently(true);
    return checkLoanMarketUserQualify(loanMarketDecision, context, true, skipExperimentAndVersion);
  }

  private LoanMarketEntranceDisplayDecision checkLoanMarketUserQualify(LoanMarketEntranceDisplayDecision loanMarketDecision,
      HomePageContext context, boolean canReapply,
      boolean skipExperimentAndVersion) {
    LoanMarketUserQualifyCheckResult checkResult =
        homepageContentTool.postProcessLoanMarketCheckResult(context, canReapply, skipExperimentAndVersion);
    return checkResult.qualifiedLoanMarketEntranceByRisk ? paddingLoanMarketEntranceDisplayDecision(loanMarketDecision, checkResult)
        : loanMarketDecision;
  }

  private LoanMarketEntranceDisplayDecision paddingLoanMarketEntranceDisplayDecision(LoanMarketEntranceDisplayDecision loanMarketDecision,
      LoanMarketUserQualifyCheckResult checkResult) {
    loanMarketDecision.setShowLoanMarketEntrance(checkResult.qualifiedLoanMarketEntranceByRisk);
    loanMarketDecision.setLoanMarketRule(checkResult.riskFlowCheckType);
    return loanMarketDecision;
  }
}
