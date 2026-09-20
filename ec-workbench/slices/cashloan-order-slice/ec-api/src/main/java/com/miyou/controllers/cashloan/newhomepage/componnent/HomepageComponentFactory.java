package com.miyou.controllers.cashloan.newhomepage.componnent;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 首页组件工厂
 */
@Component
@Slf4j
public class HomepageComponentFactory {
  @Autowired
  private List<IStatusComponentTypeService> statusComponentTypeServices;

  private Map<IDNHomepageLoanStatusV5, IStatusComponentTypeService> statusComponentTypeMap;

  @PostConstruct
  private void init() {
    statusComponentTypeMap = statusComponentTypeServices.stream()
        .collect(Collectors.toMap(IStatusComponentTypeService::getStatus, s -> s));
  }


  /**
   * todo 渲染成response返回
   * @param homePageContext
   * @return
   */
  public List<ComponentBaseParam> createComponentParam(HomePageContext homePageContext) {
    IStatusComponentTypeService typeService = statusComponentTypeMap.get(homePageContext.getStatus());
    if (typeService == null) {
      log.error("statusComponentTypeMap not found status:{}", homePageContext.getStatus());
      return Collections.emptyList();
    }
    List<ComponentType> componentTypes = statusComponentTypeMap.get(homePageContext.getStatus()).getComponentTypeList(homePageContext);
    return componentTypes.stream()
        .map(type -> {
          try {
            if (!type.getClazz().isInstance(typeService)) {
              log.error("componentParamCreateServiceTable not found status:{} type:{}", homePageContext.getStatus(), type);
              return null;
            }
            return type.createComponentParam((IComponentParamCreateService) typeService, homePageContext);
          } catch (Exception e) {
            log.error("createComponentParam error, status:{}, type:{}, status:{}, userId:{}", homePageContext.getStatus(), type, homePageContext.getUserId(), e);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .filter(ComponentBaseParam::canDisplay)
        .collect(Collectors.toList());
  }
}
