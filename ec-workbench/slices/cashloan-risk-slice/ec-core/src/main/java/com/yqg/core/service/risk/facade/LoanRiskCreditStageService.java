package com.yqg.core.service.risk.facade;

import com.yqg.core.model.sql.loanusertrace.LoanRiskCreditStage;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 信贷阶段计算。
 *
 * <p>计算策略：
 * <ol>
 *   <li>测额风控 / 订单风控：信贷阶段由 riskType 本身确定（首贷/结清复贷/续借/循环贷家族），
 *       直接按 riskType 推导，避免提交风控前后用户状态发生变化导致阶段漂移；</li>
 *   <li>其余风控类型（与首复续借家族无关，如增信、活动订单、完件前等）：按用户当前状态实时计算。</li>
 * </ol>
 *
 * @author chaoye
 */
@Slf4j
@Service
public class LoanRiskCreditStageService {

  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;

  /**
   * 计算 trace 落库的信贷阶段，永不为空。
   *
   * @param accountId 借贷账户 id
   * @param riskType  本次风控类型
   * @return 信贷阶段
   */
  public LoanRiskCreditStage resolveCreditStage(Long accountId, LoanUserRiskType riskType) {
    LoanRiskCreditStage stageByRiskType = deriveCreditStageByRiskType(riskType);
    if (stageByRiskType != null) {
      return stageByRiskType;
    }
    return calcCreditStageByCurrentUserStatus(accountId);
  }

  /**
   * 测额风控 / 订单风控：信贷阶段由 riskType 家族确定；其余类型返回 null（交由实时计算）。
   */
  private LoanRiskCreditStage deriveCreditStageByRiskType(LoanUserRiskType riskType) {
    boolean calcOrOrderRisk = LoanUserRiskType.getAllCalcCreditsType().contains(riskType)
        || LoanUserRiskType.getAllOrderRiskTypes().contains(riskType);
    if (!calcOrOrderRisk) {
      return null;
    }
    if (LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(riskType)) {
      return LoanRiskCreditStage.REVOLVING_LOAN;
    }
    if (LoanUserRiskType.getAllMultiLoanRiskType().contains(riskType)) {
      return LoanRiskCreditStage.MULTI_LOAN;
    }
    if (riskType == LoanUserRiskType.RELOAN || LoanUserRiskType.getReloanCalcCreditWithoutRevolvingRiskType().contains(riskType)) {
      return LoanRiskCreditStage.RELOAN_WITHOUT_READY_ORDER;
    }
    return LoanRiskCreditStage.LOAN;
  }

  /**
   * 按用户当前状态实时计算信贷阶段。
   * 「在贷」口径取 {@link CashLoanOrderStatus#READY}（已打款未结清），与现有续借判定同口径，不含在途单；
   * 复贷判定以历史放款次数（{@code loanTimes > 0}）为准。
   */
  private LoanRiskCreditStage calcCreditStageByCurrentUserStatus(Long accountId) {
    if (loanAccountRevolvingService.checkUserInRevolvingLoanProcess(accountId)) {
      return LoanRiskCreditStage.REVOLVING_LOAN;
    }
    if (ecOrderService.existOrder(accountId, CashLoanOrderStatus.READY)) {
      return LoanRiskCreditStage.MULTI_LOAN;
    }
    if (loanAccountBasicInfoService.isReloan(accountId)) {
      return LoanRiskCreditStage.RELOAN_WITHOUT_READY_ORDER;
    }
    return LoanRiskCreditStage.LOAN;
  }
}
