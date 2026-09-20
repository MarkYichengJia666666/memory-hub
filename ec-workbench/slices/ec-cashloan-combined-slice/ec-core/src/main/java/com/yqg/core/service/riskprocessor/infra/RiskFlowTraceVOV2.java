package com.yqg.core.service.riskprocessor.infra;

import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.risk.riskflow.trace.RiskFlowTraceVO;
import lombok.Data;

import java.util.Map;

@Data
public class RiskFlowTraceVOV2 {
  public Long id;
  public Long eventId;
  public Long riskFlowId;
  public RiskFlowTraceStatusV2 status;
  public Long timeCreated;
  public Long timeUpdated;
  public Map<String, Object> props;

  public static RiskFlowTraceVOV2 fromInit(Long id, Long riskFlowId, Long eventId, Long timeCreated) {
    RiskFlowTraceVOV2 vo = new RiskFlowTraceVOV2();
    vo.id = id;
    vo.riskFlowId = riskFlowId;
    vo.eventId = eventId;
    vo.timeCreated = timeCreated;
    vo.status = RiskFlowTraceStatusV2.INIT;
    return vo;
  }

  public static RiskFlowTraceVOV2 fromRiskFlowTraceVO(RiskFlowTraceVO riskFlowTraceVO) {
    RiskFlowTraceVOV2 riskFlowTraceVOV2 = new RiskFlowTraceVOV2();
    riskFlowTraceVOV2.riskFlowId = riskFlowTraceVO.riskFlowId;
    riskFlowTraceVOV2.id = riskFlowTraceVO.id;
    riskFlowTraceVOV2.eventId = riskFlowTraceVO.eventId;
    riskFlowTraceVOV2.status = RiskFlowTraceStatusV2.fromCode(riskFlowTraceVO.status.code);
    riskFlowTraceVOV2.timeCreated = riskFlowTraceVO.timeCreated;
    riskFlowTraceVOV2.timeUpdated = riskFlowTraceVO.timeUpdated;
    riskFlowTraceVOV2.props = riskFlowTraceVO.props;
    return riskFlowTraceVOV2;
  }

  public static RiskFlowTraceVOV2 fromOverseasRiskFlowTraceVO(com.yqg.overseasrisk.common.lib.riskflow.trace.RiskFlowTraceVO resp) {
    RiskFlowTraceVOV2 riskFlowTraceVOV2 = new RiskFlowTraceVOV2();
    riskFlowTraceVOV2.riskFlowId = resp.riskFlowId;
    riskFlowTraceVOV2.id = resp.id;
    riskFlowTraceVOV2.eventId = resp.eventId;
    riskFlowTraceVOV2.status = RiskFlowTraceStatusV2.fromCode(resp.status.code);
    riskFlowTraceVOV2.timeCreated = resp.timeCreated;
    riskFlowTraceVOV2.timeUpdated = resp.timeUpdated;
    riskFlowTraceVOV2.props = resp.props;
    return riskFlowTraceVOV2;
  }
}
