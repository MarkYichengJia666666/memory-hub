package com.yqg.core.service.riskprocessor.infra;

import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * @ClassName: TraceRiskFlowMappingService
 * @Description: Maps the main credit risk flow by user status (first loan / settled reloan / multi-loan)
 * and risk stage (calc credits / order risk)
 */
@Slf4j
@Service
public class TraceRiskFlowMappingService {

  /**
   * First-loan calc-credits flow: newloan_v14_limit
   */
  private static final long NEW_LOAN_V14_LIMIT_FLOW_ID = 8804L;
  /**
   * First-loan calc-credits flow: newloan_v13_limit
   */
  private static final long NEW_LOAN_V13_LIMIT_FLOW_ID = 7953L;
  /**
   * First-loan order-risk flow: newloan_payout_withdraw_risk_flow
   */
  private static final long NEW_LOAN_PAYOUT_WITHDRAW_FLOW_ID = 686L;
  /**
   * Settled-reloan calc-credits flow: reloan_limit_risk_flow
   */
  private static final long RELOAN_LIMIT_FLOW_ID = 8113L;
  /**
   * Settled-reloan order-risk flow: reloan_payout_withdraw_risk_flow
   */
  private static final long RELOAN_PAYOUT_WITHDRAW_FLOW_ID = 7246L;
  /**
   * Multi-loan calc-credits flow: multiloan_limit_risk_flow
   */
  private static final long MULTI_LOAN_LIMIT_FLOW_ID = 5693L;
  /**
   * Multi-loan order-risk flow: multiloan_payout_withdraw_risk_flow
   */
  private static final long MULTI_LOAN_PAYOUT_WITHDRAW_FLOW_ID = 6941L;

  /**
   * Fixed cells of the status x stage matrix. First-loan calc-credits is omitted here because it
   * is split 50/50 by accountId rather than a single constant.
   */
  private static final Map<UserLoanStatus, Map<RiskStage, Long>> FIXED_FLOW_MATRIX = buildFixedFlowMatrix();

  @Value("${risk_flow.user_status_mapping_enabled:false}")
  private boolean userStatusMappingEnabled;

  /**
   * Resolves whether the account has completed at least one loan
   */
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  /**
   * Resolves whether the account currently has a READY order (in-repay multi-loan)
   */
  @Autowired
  private EcOrderService ecOrderService;

  public boolean isUserStatusMappingEnabled() {
    return userStatusMappingEnabled;
  }

  /**
   * Maps a risk flow id from user status and risk stage.
   * First-loan calc credits is split 50/50 between 8804 and 7953 by accountId.
   *
   * @param accountId loan account id
   * @param riskType  risk type of this submission
   * @return mapped flow id, or null when this risk type is outside the main-credit matrix
   */
  public Long mapRiskFlowId(Long accountId, LoanUserRiskType riskType) {
    if (accountId == null || riskType == null) {
      return null;
    }
    RiskStage riskStage = resolveRiskStage(riskType);
    if (riskStage == null) {
      return null;
    }
    UserLoanStatus userLoanStatus = resolveUserLoanStatus(accountId);
    Long riskFlowId = pickRiskFlowId(accountId, userLoanStatus, riskStage);
    log.info("map risk flow, accountId={}, riskType={}, userLoanStatus={}, riskStage={}, riskFlowId={}",
        accountId, riskType, userLoanStatus, riskStage, riskFlowId);
    return riskFlowId;
  }

  /**
   * First loan if never borrowed; multi-loan if a READY order exists; otherwise settled reloan.
   *
   * @param accountId loan account id
   * @return user loan status used for flow mapping
   */
  private UserLoanStatus resolveUserLoanStatus(Long accountId) {
    if (!loanAccountBasicInfoService.isReloan(accountId)) {
      return UserLoanStatus.FIRST_LOAN;
    }
    if (ecOrderService.existOrder(accountId, CashLoanOrderStatus.READY)) {
      return UserLoanStatus.MULTI_LOAN;
    }
    return UserLoanStatus.SETTLED_RELOAN;
  }

  /**
   * Calc-credits vs order-risk. Other families (increase credits, activity, ETL, etc.) are skipped.
   *
   * @param riskType risk type of this submission
   * @return risk stage, or null when this type is not in the main-credit matrix
   */
  private RiskStage resolveRiskStage(LoanUserRiskType riskType) {
    if (LoanUserRiskType.getAllCalcCreditsType().contains(riskType)) {
      return RiskStage.CALC_CREDITS;
    }
    if (LoanUserRiskType.getAllOrderRiskTypes().contains(riskType)) {
      return RiskStage.ORDER_RISK;
    }
    return null;
  }

  /**
   * Resolves the matrix cell; first-loan calc-credits uses accountId hash instead of a fixed id.
   *
   * @param accountId      loan account id, used by the first-loan calc-credits split
   * @param userLoanStatus first loan / settled reloan / multi-loan
   * @param riskStage      calc credits / order risk
   * @return risk flow id for that cell
   */
  private Long pickRiskFlowId(Long accountId, UserLoanStatus userLoanStatus, RiskStage riskStage) {
    if (userLoanStatus == UserLoanStatus.FIRST_LOAN && riskStage == RiskStage.CALC_CREDITS) {
      return pickFirstLoanLimitFlowId(accountId);
    }
    Map<RiskStage, Long> stageFlowMap = FIXED_FLOW_MATRIX.getOrDefault(userLoanStatus, Collections.emptyMap());
    return stageFlowMap.get(riskStage);
  }

  /**
   * Stable 50/50 split: even accountId -> 8804 (v14), odd -> 7953 (v13).
   *
   * @param accountId loan account id
   * @return 8804 or 7953
   */
  private Long pickFirstLoanLimitFlowId(Long accountId) {
    return (accountId & 1L) == 0L ? NEW_LOAN_V14_LIMIT_FLOW_ID : NEW_LOAN_V13_LIMIT_FLOW_ID;
  }

  /**
   * Builds the immutable status x stage matrix for the five fixed cells.
   *
   * @return enum matrix, never mutated after class load
   */
  private static Map<UserLoanStatus, Map<RiskStage, Long>> buildFixedFlowMatrix() {
    Map<UserLoanStatus, Map<RiskStage, Long>> matrix = new EnumMap<>(UserLoanStatus.class);

    Map<RiskStage, Long> firstLoan = new EnumMap<>(RiskStage.class);
    firstLoan.put(RiskStage.ORDER_RISK, NEW_LOAN_PAYOUT_WITHDRAW_FLOW_ID);
    matrix.put(UserLoanStatus.FIRST_LOAN, firstLoan);

    Map<RiskStage, Long> settledReloan = new EnumMap<>(RiskStage.class);
    settledReloan.put(RiskStage.CALC_CREDITS, RELOAN_LIMIT_FLOW_ID);
    settledReloan.put(RiskStage.ORDER_RISK, RELOAN_PAYOUT_WITHDRAW_FLOW_ID);
    matrix.put(UserLoanStatus.SETTLED_RELOAN, settledReloan);

    Map<RiskStage, Long> multiLoan = new EnumMap<>(RiskStage.class);
    multiLoan.put(RiskStage.CALC_CREDITS, MULTI_LOAN_LIMIT_FLOW_ID);
    multiLoan.put(RiskStage.ORDER_RISK, MULTI_LOAN_PAYOUT_WITHDRAW_FLOW_ID);
    matrix.put(UserLoanStatus.MULTI_LOAN, multiLoan);

    return matrix;
  }

  /**
   * User lending status that drives the flow matrix
   */
  private enum UserLoanStatus {
    FIRST_LOAN,
    SETTLED_RELOAN,
    MULTI_LOAN
  }

  /**
   * Risk stage that drives the flow matrix
   */
  private enum RiskStage {
    CALC_CREDITS,
    ORDER_RISK
  }
}
