package com.yqg.core.service.risk.riskflow.vo;

import com.yqg.common.util.type.BooleanType;
import com.yqg.risk.orm.sql.tables.records.RiskFlowRecord;
import com.yqg.risk.riskflow.IEventType;
import com.yqg.risk.util.RiskModule;
import com.yqg.risk.util.SingletonContainer;

import java.math.BigDecimal;

/**
 * @author shubo
 * @date 31/10/24 11.20
 */
public class RiskFlowVO {
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

  public static RiskFlowVO from(RiskFlowRecord record) {
    RiskFlowVO vo = new RiskFlowVO();
    vo.id = record.getId();
    vo.percentage = record.getPercentage().multiply(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_UP);
    vo.states = record.getStates();
    vo.enabled = BooleanType.fromCharCode(record.getEnabled());
    vo.name = record.getName();
    vo.timeCreated = record.getTimeCreated();
    vo.isDefault = BooleanType.fromCharCode(record.getDefault());
    vo.eventType = SingletonContainer.getInstance(RiskModule.class).getEventTypeFactory().getById(record.getEventId());
    vo.comment = record.getComment();
    vo.priority = record.getPriority();
    vo.eventId = record.getEventId();
    return vo;
  }

  public static RiskFlowVO from(com.yqg.risk.riskflow.RiskFlowVO riskFlowVO) {
    RiskFlowVO vo = new RiskFlowVO();
    vo.id = riskFlowVO.id;
    vo.percentage = riskFlowVO.percentage;
    vo.states = riskFlowVO.states;
    vo.enabled = riskFlowVO.enabled;
    vo.name = riskFlowVO.name;
    vo.timeCreated = riskFlowVO.timeCreated;
    vo.isDefault = riskFlowVO.isDefault;
    vo.eventType = riskFlowVO.eventType;
    vo.comment = riskFlowVO.comment;
    vo.priority = riskFlowVO.priority;
    vo.eventId = riskFlowVO.eventId;
    return vo;
  }
}