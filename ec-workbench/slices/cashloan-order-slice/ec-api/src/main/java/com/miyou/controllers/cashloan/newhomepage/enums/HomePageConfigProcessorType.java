package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HomePageConfigProcessorType implements HomepageBaseInfoProcessorType{
  COMMON_CONFIG("通用配置"),
  ORDER_PAGE_CONFIG("下单页配置"),
  H5_WHOLE_PROCESS_ORDER_UI_CONFIG("H5全流程下单跳转UI配置"),
  ;

  private String desc;
  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.PAGE_CONFIG;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends HomepageBaseInfoProcessorType> getTypeClazz() {
    return HomePageConfigProcessorType.class;
  }
}
