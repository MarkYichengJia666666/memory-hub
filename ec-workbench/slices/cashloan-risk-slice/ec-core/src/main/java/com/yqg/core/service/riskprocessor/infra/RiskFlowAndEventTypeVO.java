package com.yqg.core.service.riskprocessor.infra;

import com.yqg.core.service.cashloan.risk.vo.EventTypeVO;
import lombok.Data;

@Data
public class RiskFlowAndEventTypeVO {
  public EventTypeVO eventTypeVO;
  public Long riskFlowId;

  public static RiskFlowAndEventTypeVO from(EventTypeVO eventTypeVO, Long riskFlowId) {
    RiskFlowAndEventTypeVO vo = new RiskFlowAndEventTypeVO();
    vo.eventTypeVO = eventTypeVO;
    vo.riskFlowId = riskFlowId;
    return vo;
  }
}
