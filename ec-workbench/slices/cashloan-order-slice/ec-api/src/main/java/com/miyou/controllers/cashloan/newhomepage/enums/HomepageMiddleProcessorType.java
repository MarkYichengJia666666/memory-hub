package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

/**
 * @author chenxianrui
 * @date 2024/12/13
 */
@AllArgsConstructor
public enum HomepageMiddleProcessorType implements HomepageBaseInfoProcessorType{
  EMPTY_MIDDLE_INFO_PROCESSOR("空信息"),
  SME_ENTRANCE_MIDDLE_INFO_PROCESSOR("sme entrance middle info"),
  WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR("with sme entrance middle info"),

  LOAN_MARKET_WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR("贷超中通位适配 -- with sme entrance middle info")
  ;

  public String desc;

  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.MIDDLE_INFO;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends HomepageMiddleProcessorType> getTypeClazz() {
    return HomepageMiddleProcessorType.class;
  }
}
