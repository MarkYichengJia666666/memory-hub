package com.yqg.core.service.cashloan.ordercenter.ordersubstep;

import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.cashloan.ordercenter.ordersubstep.enums.OrderSubStep;
import com.yqg.core.service.cashloan.ordercenter.ordersubstep.vo.OrderSubStepVO;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RiskControlRejectedSubStepProcessor extends BaseOrderSubStepProcessor {
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;

  @Override
  public boolean checkCondition(CashLoanOrderVO orderVO, List<OrderSubStepVO> orderSubStepVOs) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrNull(orderVO.traceId);
    return loanUserRiskTraceVO != null && loanUserRiskTraceVO.creditsStatus == LoanCreditsStatus.REJECTED;
  }

  @Override
  public OrderSubStep getOrderSubStep() {
    return OrderSubStep.RISK_CONTROL_REJECTED;
  }

  @Override
  public Map<String, Object> getStepInfo(CashLoanOrderVO orderVO, List<OrderSubStepVO> orderSubStepVOs) {
    Map<String, Object> extraInfo = new HashMap<>();
    getExtraInfoFromRiskTrace(orderVO, extraInfo);

    return extraInfo;
  }
}
