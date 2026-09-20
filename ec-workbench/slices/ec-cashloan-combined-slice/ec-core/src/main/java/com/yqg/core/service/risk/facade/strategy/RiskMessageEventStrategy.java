package com.yqg.core.service.risk.facade.strategy;

import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.facade.TriggerRiskAfterCurrentRiskVO;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;

/**
 * @author chenxianrui
 * @date 2025/8/22
 */
public interface RiskMessageEventStrategy {
  void handlePostEvent(LoanUserRiskTraceVO traceVO, LoanUserTagData data, Boolean createOrderResult,
                       LoanUserCreditsInfoVO creditsInfoVO, TriggerRiskAfterCurrentRiskVO triggerRiskAfterCurrentRiskVO);
  boolean supports(LoanUserRiskType riskType);
}
