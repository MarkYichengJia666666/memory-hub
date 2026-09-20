package com.miyou.controllers.cashloan.newhomepage;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepageBaseInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.yqg.ec.common.exception.EcException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomepageProcessorFactory<V extends HomePageResponseFields, P extends IHomePageFieldProcessor<V, T>, T extends HomepageBaseInfoProcessorType> {
  @Autowired
  private List<P> processors;
  private Map<T, P> processorMap;

  @PostConstruct
  private void init() {
    processorMap = new HashMap<>();
    for (P processor : processors) {
      T processorType = processor.getProcessorType();
      if (processorMap.containsKey(processorType)) {
        throw EcException.error("Duplicate processor type: " + processorType);
      }
      processorMap.put(processorType, processor);
    }

    checkTypeToProcessor();
  }

  /**
   * 检查每个声明的processorType都有绑定的processor
   */
  private void checkTypeToProcessor() {
    Arrays.stream(HomepageProcessorType.values())
        .flatMap(item -> item.getProcessorType().get().stream().distinct())
        .forEach(type -> {
          P processor = processorMap.get(type);
          if (Objects.nonNull(processor)) {
            return;
          }
          throw EcException.error("no processor class for type: " + type);
        });

  }

  public  List<P> getProcessors(List<T> type) {
    if (CollectionUtils.isEmpty(type)) {
      return Collections.EMPTY_LIST;
    }
    return type.stream()
        .map(item -> processorMap.get(item))
        .collect(Collectors.toList());
  }
}
