package com.yqg.core.service.cashloan.ordercenter.ordersubstep;

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
import java.util.Objects;

@Service
public class BeginRiskControlSubStepProcessor extends BaseOrderSubStepProcessor {
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;

  @Override
  public boolean checkCondition(CashLoanOrderVO orderVO, List<OrderSubStepVO> orderSubStepVOs) {
    return true;
  }

  @Override
  public OrderSubStep getOrderSubStep() {
    return OrderSubStep.SECOND_RISK_CONTROL_BEGIN;
  }

  @Override
  public Map<String, Object> getStepInfo(CashLoanOrderVO orderVO, List<OrderSubStepVO> orderSubStepVOs) {
    Map<String, Object> extraInfo = new HashMap<>();
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrNull(orderVO.traceId);
    Long timeStamp = Objects.nonNull(loanUserRiskTraceVO) ? loanUserRiskTraceVO.timeCreated : null;
    extraInfo.put(TIME_STAMP_KEY, timeStamp);
    extraInfo.put(EXTRA_INFO_DESCRIPTION_KEY, null);
    return extraInfo;
  }
}
