package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AlertProcessorType implements HomepageBaseInfoProcessorType{
  LENDING_GUIDANCE_ALERT_PROCESSOR("lending guidance alert processor"),
  REPAYMENT_GUIDANCE_ALERT_PROCESSOR("repayment guidance alert processor"),
  ;

  public String desc;

  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.ALERT_INFO;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends AlertProcessorType> getTypeClazz() {
    return AlertProcessorType.class;
  }
}