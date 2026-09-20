package com.miyou.controllers.cashloan.newhomepage.homepagecontext;

import com.google.common.collect.ArrayListMultimap;
import com.miyou.controllers.cashloan.newhomepage.HomePageResponseFields;
import com.miyou.controllers.cashloan.newhomepage.IHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.content.vo.FactoryFilterVO;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.FactoryThreadGroup;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HomepageFactoryFilter<V extends HomePageResponseFields> {
  @Autowired
  private List<IHomePageResponseFactory<V>> factoryList;

  private Map<HomepageProcessorType, IHomePageResponseFactory<V>> pageResponseFactoryMap = new HashMap<>();
  private ArrayListMultimap<FactoryThreadGroup, HomepageProcessorType> homepageProcessorTypeGroup = ArrayListMultimap.create();

  @PostConstruct
  private void init() {
    pageResponseFactoryMap = factoryList.stream()
        .collect(Collectors.toMap(IHomePageResponseFactory::getFactoryType, factory -> factory));
    Arrays.stream(HomepageProcessorType.values()).forEach(item -> {
      homepageProcessorTypeGroup.put(item.getFactoryThreadGroup(), item);
    });
    log.info("homepageProcessorTypeGroup:{}", homepageProcessorTypeGroup);
  }


  public IHomePageResponseFactory getFactoryByType(HomepageProcessorType type) {
    return pageResponseFactoryMap.get(type);
  }

  public ArrayListMultimap<FactoryThreadGroup, HomepageProcessorType> getHomepageProcessorTypeGroup(HomePageContext homePageContext) {
    ArrayListMultimap<FactoryThreadGroup, HomepageProcessorType> res = ArrayListMultimap.create();
    homepageProcessorTypeGroup.keySet().forEach(type -> {
      homepageProcessorTypeGroup.get(type).forEach(item -> {
        if (!item.getPredicate().test(new FactoryFilterVO(homePageContext))) {
          return;
        }
        res.put(type, item);
      });
    });
    return res;
  }


  public List<HomepageProcessorType> filter(HomePageContext context) {
    List<HomepageProcessorType> res = new ArrayList<>();



    return res;
  }
}
