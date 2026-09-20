package com.yqg.core.service.risk.riskflow.vo;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.generated.tables.records.RiskFlowAudRecord;
import com.yqg.risk.riskflow.IEventType;
import com.yqg.risk.util.RiskModule;
import com.yqg.risk.util.SingletonContainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RiskFlowInfoVO {
  public Long id;
  public String states;
  public BooleanType enabled;
  /**
   * 实验比例， xx.xx% 保留2位小数
   */
  public BigDecimal percentage;
  public String name;
  public Long timeCreated;
  public BooleanType isDefault;
  public IEventType eventType;
  public String comment;
  public Integer priority;
  public Long eventId;
  public List<RiskFlowAudVO> audList;

  public static RiskFlowInfoVO from(RiskFlowVO riskFlowVO) {
    RiskFlowInfoVO vo = new RiskFlowInfoVO();
    if (riskFlowVO != null) {
      vo.id = riskFlowVO.id;
      vo.percentage = riskFlowVO.percentage.multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
      vo.states = riskFlowVO.states;
      vo.enabled = riskFlowVO.enabled;
      vo.name = riskFlowVO.name;
      vo.timeCreated = riskFlowVO.timeCreated;
      vo.isDefault = riskFlowVO.isDefault;
      vo.eventId = riskFlowVO.eventId;
      vo.eventType = SingletonContainer.getInstance(RiskModule.class).getEventTypeFactory().getById(riskFlowVO.eventId);
      vo.comment = riskFlowVO.comment;
      vo.priority = riskFlowVO.priority;
      vo.eventId = riskFlowVO.eventId;
    }
    return vo;
  }

  public static com.yqg.core.service.risk.riskflow.vo.RiskFlowInfoVO from(RiskFlowVO riskFlowVO, RiskFlowAudRecord audRecord) {
    com.yqg.core.service.risk.riskflow.vo.RiskFlowInfoVO vo = new com.yqg.core.service.risk.riskflow.vo.RiskFlowInfoVO();
    if (riskFlowVO != null) {
      vo.id = riskFlowVO.id;
      vo.percentage = riskFlowVO.percentage.multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
      vo.states = riskFlowVO.states;
      vo.enabled = riskFlowVO.enabled;
      vo.name = riskFlowVO.name;
      vo.timeCreated = riskFlowVO.timeCreated;
      vo.isDefault = riskFlowVO.isDefault;
      vo.eventId = riskFlowVO.eventId;
      vo.eventType = SingletonContainer.getInstance(RiskModule.class).getEventTypeFactory().getById(riskFlowVO.eventId);
      vo.comment = riskFlowVO.comment;
      vo.priority = riskFlowVO.priority;
      vo.eventId = riskFlowVO.eventId;
    }
    if (audRecord != null) {
      vo.audList = new ArrayList<>();
      vo.audList.add(RiskFlowAudVO.from(audRecord));
    }
    return vo;
  }
}
