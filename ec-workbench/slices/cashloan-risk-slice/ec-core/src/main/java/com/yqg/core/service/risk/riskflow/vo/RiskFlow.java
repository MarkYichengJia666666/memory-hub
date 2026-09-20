package com.yqg.core.service.risk.riskflow.vo;


import com.yqg.risk.riskflow.IEventType;

import java.math.BigDecimal;

/**
 * @author shubo
 * @date 05/11/24 17.31
 */
public class RiskFlow {
  public Long id;
  public String name;
  public BigDecimal experimentPercentage;
  public Boolean isDefault = false;
  public IEventType eventType;
  public Integer priority;

  public static RiskFlow from(RiskFlowVO vo) {
    RiskFlow riskFlow = new RiskFlow();
    riskFlow.experimentPercentage = vo.percentage.divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP);
    riskFlow.id = vo.id;
    riskFlow.name = vo.name;
    riskFlow.isDefault = vo.isDefault.bool;
    riskFlow.eventType = vo.eventType;
    riskFlow.priority = vo.priority;
    return riskFlow;
  }
}
