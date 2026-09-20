package com.yqg.core.service.general.pageconfig.filterstrategy.processor;

import com.google.common.collect.ImmutableMap;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.GeneralPageConfigFilterRuleType;
import com.yqg.ec.common.exception.EcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeneralPageConfigFilterRuleProcessorFactory {
  @Autowired
  private List<GeneralPageConfigBaseFilterRuleProcessor> processorList;

  private static Map<GeneralPageConfigFilterRuleType, GeneralPageConfigBaseFilterRuleProcessor> typeToProcessorMap = new HashMap<>();

  @PostConstruct
  private void init() {
    ImmutableMap.Builder<GeneralPageConfigFilterRuleType, GeneralPageConfigBaseFilterRuleProcessor> typeToProcessorMapBuilder = new ImmutableMap.Builder<>();
    for (GeneralPageConfigBaseFilterRuleProcessor processor : processorList) {
      typeToProcessorMapBuilder.put(processor.getRuleType(), processor);
    }
    typeToProcessorMap = typeToProcessorMapBuilder.build();
  }

  public static GeneralPageConfigBaseFilterRuleProcessor getProcessor(GeneralPageConfigFilterRuleType type) {
    GeneralPageConfigBaseFilterRuleProcessor processor = typeToProcessorMap.get(type);
    if (processor == null) {
      throw EcException.error("no processor for GeneralPageConfigFilterRuleType: {}", type);
    }
    return processor;
  }

}