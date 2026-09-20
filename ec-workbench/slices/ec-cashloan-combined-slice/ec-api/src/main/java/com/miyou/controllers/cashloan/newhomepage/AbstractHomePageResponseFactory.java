package com.miyou.controllers.cashloan.newhomepage;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageBaseInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.StatusProcessorSelectMapFactory;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class AbstractHomePageResponseFactory<V extends HomePageResponseFields, T extends HomepageBaseInfoProcessorType,
    P extends IHomePageFieldProcessor<V, T>> implements IHomePageResponseFactory<V> {


  @Autowired
  private StatusProcessorSelectMapFactory<V, P, T> statusProcessorSelectMapFactory;

  private List<P> getProcessorTypes(IDNHomepageLoanStatusV5 homepageLoanStatusV5) {
    return statusProcessorSelectMapFactory.getProcessorTypesByStatus(homepageLoanStatusV5, getFactoryType());
  }

  @Override
  public V getResult(HomePageContext homePageContext) {
    List<P> processors = getProcessorTypes(homePageContext.getStatus());
    V result = getFactoryType().newResultInstance();
    if (CollectionUtils.isEmpty(processors)) {
      return result;
    }
    for (P processor : processors) {
      processor.process(result, homePageContext);
    }
    return result;
  }


}
