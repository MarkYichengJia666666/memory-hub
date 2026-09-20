package com.miyou.controllers.cashloan.newhomepage;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageBaseInfoProcessorType;

public interface IHomePageFieldProcessor<V extends HomePageResponseFields, T extends HomepageBaseInfoProcessorType> {

  default void process(V fieldsInfo, HomePageContext homePageContext) {

  }

  T getProcessorType();
}
