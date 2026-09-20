package com.yqg.core.service.cashloan.ordercenter.orderstep;

import com.yqg.core.service.cashloan.ordercenter.orderstep.enums.OrderStep;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class OrderRiskControlStepProcessor extends BaseOrderStepProcessor {
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;

  @Override
  public boolean checkCondition(CashLoanOrderVO orderVO) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrNull(orderVO.traceId);
    return Objects.nonNull(loanUserRiskTraceVO) && Objects.nonNull(loanUserRiskTraceVO.creditsStatus);
  }

  @Override
  public OrderStep getOrderStep() {
    return OrderStep.ORDER_RISK_CONTROL;
  }
}
