package com.miyou.controllers.cashloan.newhomepage.enums;

import com.google.common.collect.Lists;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigType;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资源位都是调用mc获取，为了减少mc的调用次数，将processor直接关联需要的资源位，一次性请求
 * 但这种组合方式当前是比较hack的，后续可以考虑优化
 */
@NoArgsConstructor
public enum AppResourceProcessorType implements HomepageBaseInfoProcessorType{
  @Deprecated
  RESOURCE_CAN_CREATE_ORDER_PRE_ACTION,
  BANNER_HOME_POP_UP(Lists.newArrayList(GeneralPageConfigType.BANNER, GeneralPageConfigType.HOME_MESSAGE)),
  BANNER_HOME_POP_UP_FOR_CAN_CREATE_ORDER(Lists.newArrayList(GeneralPageConfigType.BANNER, GeneralPageConfigType.HOME_MESSAGE)),
  BANNER_HOME_POP_UP_FOR_NEVER_APPLIED(Lists.newArrayList(GeneralPageConfigType.BANNER, GeneralPageConfigType.HOME_MESSAGE)),
  BANNER_HOME_POP_UP_FOR_HAS_ORDER(Lists.newArrayList(GeneralPageConfigType.BANNER, GeneralPageConfigType.HOME_MESSAGE)),
  BANNER_HOME_POP_UP_FOR_OVERDUE(Lists.newArrayList(GeneralPageConfigType.BANNER, GeneralPageConfigType.HOME_MESSAGE)),
  ;

  public List<GeneralPageConfigType> resourceTypeList;

  AppResourceProcessorType(List<GeneralPageConfigType> resourceTypeList) {
    this.resourceTypeList = resourceTypeList;
  }

  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.APP_RESOURCE;
  }

  @Override
  public String getName() {
    return name();
  }

  @Override
  public Class<? extends AppResourceProcessorType> getTypeClazz() {
    return AppResourceProcessorType.class;
  }
}
