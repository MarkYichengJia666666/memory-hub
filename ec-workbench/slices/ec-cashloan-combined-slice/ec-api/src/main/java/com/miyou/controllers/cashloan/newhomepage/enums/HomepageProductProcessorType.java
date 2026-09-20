package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HomepageProductProcessorType implements HomepageBaseInfoProcessorType{
  DEFAULT_PRODUCT_INFO_PROCESSOR("默认产品信息处理器"),
  CAN_CREATE_ORDER_PRODUCT_INFO_PROCESSOR("可下单状态产品信息处理器"),
  ;
  private String desc;
  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.PRODUCT_INFO;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends HomepageProductProcessorType> getTypeClazz() {
    return HomepageProductProcessorType.class;
  }
}
