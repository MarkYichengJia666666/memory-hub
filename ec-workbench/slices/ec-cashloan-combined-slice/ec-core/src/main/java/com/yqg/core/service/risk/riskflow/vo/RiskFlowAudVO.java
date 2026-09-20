package com.yqg.core.service.risk.riskflow.vo;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.core.RevType;
import com.yqg.core.model.generated.tables.records.RiskFlowAudRecord;

import java.math.BigDecimal;

public class RiskFlowAudVO {
  public Long id;
  public RevType revType;
  public Long userOpt;
  public Long timeOpt;
  public Long riskFlowId;
  public String states;
  public BooleanType enabled;
  public BigDecimal percentage;
  public String name;
  public Long timeCreated;
  public Long timeUpdated;
  public BooleanType isDefault;
  public Long eventId;
  public String comment;
  public Integer priority;

  public static RiskFlowAudVO from(RiskFlowAudRecord audRecord) {
    RiskFlowAudVO vo = new RiskFlowAudVO();
    if (audRecord != null) {
      vo.id = audRecord.getRev();
      vo.revType = RevType.fromCode(audRecord.getRevtype());
      vo.userOpt = audRecord.getUserOpt();
      vo.timeOpt  = audRecord.getTimeOpt();
      vo.riskFlowId = audRecord.getId();
      vo.states = audRecord.getStates();
      vo.enabled = BooleanType.fromCharCode(audRecord.getEnabled());
      vo.percentage = audRecord.getPercentage().multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
      vo.name = audRecord.getName();
      vo.timeCreated = audRecord.getTimeCreated();
      vo.timeUpdated = audRecord.getTimeUpdated();
      vo.eventId = audRecord.getEventId();
      vo.isDefault = BooleanType.fromCharCode(audRecord.getDefault());
      vo.comment = audRecord.getComment();
      vo.priority = audRecord.getPriority();
    }
    return vo;
  }
}
