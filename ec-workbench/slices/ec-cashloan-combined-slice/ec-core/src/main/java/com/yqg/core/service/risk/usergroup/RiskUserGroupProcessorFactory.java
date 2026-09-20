package com.yqg.core.service.risk.usergroup;

import com.google.common.collect.Maps;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @author chaoye
 * @date 2025/6/25
 */
@Component
public class RiskUserGroupProcessorFactory {
  @Autowired
  private List<IRiskUserGroupProcessor> riskUserGroupProcessorList;

  private Map<LoanRiskUserGroupEnum, IRiskUserGroupProcessor> processorMap = Maps.newHashMap();

  @PostConstruct
  private void init() {
    for (IRiskUserGroupProcessor processor : riskUserGroupProcessorList) {
      processorMap.put(processor.getGroup(), processor);
    }
  }

  public IRiskUserGroupProcessor getInstance(LoanRiskUserGroupEnum userGroup) {
    return processorMap.get(userGroup);
  }
}
