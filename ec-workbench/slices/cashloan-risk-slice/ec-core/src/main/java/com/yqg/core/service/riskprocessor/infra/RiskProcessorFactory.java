package com.yqg.core.service.riskprocessor.infra;

import com.google.common.collect.Maps;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/21 5:57 下午
 */
@Component
public class RiskProcessorFactory {
  @Autowired
  private List<BaseRiskProcessor> riskProcessors;

  private final Map<LoanUserRiskType, BaseRiskProcessor> processorMap = Maps.newHashMap();

  @PostConstruct
  private void init() {
    for (BaseRiskProcessor processor : riskProcessors) {
      processorMap.put(processor.getLoanUserRiskType(), processor);
    }
  }

  public BaseRiskProcessor getInstance(LoanUserRiskType riskType) {
    BaseRiskProcessor clazz = processorMap.get(riskType);
    if (clazz == null) {
      throw EcException.error("No instance found for class {}", clazz);
    }
    return clazz;
  }
}
