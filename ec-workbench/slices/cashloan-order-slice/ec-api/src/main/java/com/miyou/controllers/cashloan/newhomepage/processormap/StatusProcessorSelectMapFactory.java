package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.HomePageResponseFields;
import com.miyou.controllers.cashloan.newhomepage.HomepageProcessorFactory;
import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageBaseInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.utils.EcAsserts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 所有的IDNHomepageLoanStatusV5需要绑定processor
 */
@Slf4j
@Service
public class StatusProcessorSelectMapFactory<V extends HomePageResponseFields, P extends IHomePageFieldProcessor<V, T>, T extends HomepageBaseInfoProcessorType> {
  @Autowired
  private List<IStatusProcessorSelector> statusProcessorList;
  @Autowired
  private HomepageProcessorFactory<V, P, T> homepageProcessorFactory;
  private Map<IDNHomepageLoanStatusV5, IStatusProcessorSelector> statusProcessorMap;

  @PostConstruct
  public void init() {
    statusProcessorMap = statusProcessorList.stream()
        .collect(Collectors.toMap(IStatusProcessorSelector::getStatus, Function.identity()));
  }

  public IStatusProcessorSelector getProcessorSelectorByStatusOrThrow(IDNHomepageLoanStatusV5 statusV5) {
    IStatusProcessorSelector processorSelector = statusProcessorMap.get(statusV5);
    EcAsserts.assertNotNull(processorSelector, "not find processorSelector, status is {}", statusV5);
    return processorSelector;
  }


  public List<P> getProcessorTypesByStatus(IDNHomepageLoanStatusV5 statusV5, HomepageProcessorType processorType) {
    IStatusProcessorSelector selector = statusProcessorMap.get(statusV5);
    if (Objects.isNull(selector)) {
      throw EcException.error("status not found selector", statusV5.name());
    }

    return homepageProcessorFactory.getProcessors(processorType.getProcessorList(selector));
  }
}
