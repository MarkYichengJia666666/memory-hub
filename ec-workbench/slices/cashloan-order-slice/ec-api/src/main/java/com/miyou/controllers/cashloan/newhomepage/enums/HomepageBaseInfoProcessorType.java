package com.miyou.controllers.cashloan.newhomepage.enums;

public interface HomepageBaseInfoProcessorType {
  HomepageProcessorType getProcessorType();

  String getName();

  Class<? extends HomepageBaseInfoProcessorType> getTypeClazz();
}
