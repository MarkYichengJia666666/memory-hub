package com.yqg.core.service.risk.facade;

import lombok.Data;

@Data
public class TriggerRiskAfterCurrentRiskVO {
  private boolean submitDegradeRisk;
  private boolean submitRevolvingRisk;

  public static TriggerRiskAfterCurrentRiskVO from(boolean submitDegradeRisk, boolean submitRevolvingRisk) {
    TriggerRiskAfterCurrentRiskVO vo = new TriggerRiskAfterCurrentRiskVO();
    vo.submitDegradeRisk = submitDegradeRisk;
    vo.submitRevolvingRisk = submitRevolvingRisk;
    return vo;
  }
}
