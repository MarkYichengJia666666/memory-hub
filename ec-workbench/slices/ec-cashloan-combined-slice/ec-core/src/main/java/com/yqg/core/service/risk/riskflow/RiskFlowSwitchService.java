package com.yqg.core.service.risk.riskflow;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.service.risk.feature.CallRiskApiPosition;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowVO;
import com.yqg.risk.riskflow.IEventType;
import com.yqg.risk.riskflow.RiskFlowDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shubo
 * @date 31/10/24 12.56
 */
@Service
public class RiskFlowSwitchService {
  @Autowired
  private RiskFlowV2Service riskFlowV2Service;
  @Autowired
  private RiskFlowDataService riskFlowDataService;
  @Autowired
  private RiskConfig riskConfig;

  public void updateRiskFlow(Long id, String name, BigDecimal percentage, String states, BooleanType enabled, BooleanType isDefault, IEventType eventType, String comment) {
    if (riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)) {
      riskFlowV2Service.updateRiskFlow(id, name, percentage, states, enabled, isDefault, eventType, comment, 0);
    }
    else {
      riskFlowDataService.updateRiskFlow(id, name, percentage, states, enabled, isDefault, eventType, comment, 0);
    }
  }

  public Long setRiskFlow(BigDecimal percentage, String states, BooleanType enabled, String name, BooleanType isDefault, IEventType eventType, String comment, Integer priority) {
    return riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)
        ? riskFlowV2Service.setRiskFlow(percentage, states, enabled, name, isDefault, eventType, comment, priority)
        : riskFlowDataService.setRiskFlow(percentage, states, enabled, name, isDefault, eventType, comment, priority);
  }

  public void updateRiskFlow(Long id, String name, BigDecimal percentage, String states, BooleanType enabled, BooleanType isDefault, IEventType eventType, String comment, Integer priority) {
    if (riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)) {
      riskFlowV2Service.updateRiskFlow(id, name, percentage, states, enabled, isDefault, eventType, comment, priority);
    } else {
      riskFlowDataService.updateRiskFlow(id, name, percentage, states, enabled, isDefault, eventType, comment, priority);
    }
  }

  public RiskFlowVO getRiskFlow(Long id) {
    if (riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)) {
      return riskFlowV2Service.getRiskFlow(id);
    }
    return RiskFlowVO.from(riskFlowDataService.getRiskFlow(id));
  }

  public Map<Long, RiskFlowVO> findMapByIds(Collection<Long> ids) {
    if (riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)) {
      return riskFlowV2Service.findMapByIds(ids);
    }
    Map<Long, com.yqg.risk.riskflow.RiskFlowVO> riskFlowDataServiceMapByIds = riskFlowDataService.findMapByIds(ids);
    return riskFlowDataServiceMapByIds.entrySet().stream().collect(Collectors.toMap(a -> a.getKey(), b -> RiskFlowVO.from(b.getValue())));
  }

  public List<RiskFlowVO> listRiskFlow() {
    if (riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)) {
      return riskFlowV2Service.listRiskFlow();
    }
    return riskFlowDataService.listRiskFlow().stream().map(RiskFlowVO::from).collect(Collectors.toList());
  }


  public List<RiskFlowVO> findEnabledWithEventType(IEventType eventType) {
    if (riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)) {
      return riskFlowV2Service.findEnabledWithEventType(eventType);
    }
    return riskFlowDataService.findEnabledWithEventType(eventType).stream().map(RiskFlowVO::from).collect(Collectors.toList());
  }

  public List<RiskFlowVO> findByEventTypes(Collection<? extends IEventType> eventTypes) {
    return riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)
        ? riskFlowV2Service.findByEventTypes(eventTypes)
        : riskFlowDataService.findByEventTypes(eventTypes).stream().map(RiskFlowVO::from).collect(Collectors.toList());
  }

  public List<RiskFlowVO> findEnabledByEventTypes(Collection<? extends IEventType> eventTypes) {
    return riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)
        ? riskFlowV2Service.findEnabledByEventTypes(eventTypes)
        : riskFlowDataService.findEnabledByEventTypes(eventTypes).stream().map(RiskFlowVO::from).collect(Collectors.toList());
  }
}
