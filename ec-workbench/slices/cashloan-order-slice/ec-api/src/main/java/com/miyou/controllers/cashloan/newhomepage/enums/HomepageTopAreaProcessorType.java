package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HomepageTopAreaProcessorType implements HomepageBaseInfoProcessorType{
  // 下单页头图
  CREATE_ORDER_PAGE_TOP_AREA("下单页头图"),
  ;
  private String desc;
  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.TOP_AREA;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends HomepageTopAreaProcessorType> getTypeClazz() {
    return HomepageTopAreaProcessorType.class;
  }
}
