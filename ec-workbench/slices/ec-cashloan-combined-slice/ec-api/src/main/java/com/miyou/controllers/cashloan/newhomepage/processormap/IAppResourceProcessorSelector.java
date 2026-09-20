package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.AppResourceProcessorType;

import java.util.List;

public interface IAppResourceProcessorSelector extends IStatusProcessorSelector{
  default List<AppResourceProcessorType> getAppResourceProcessorList() {
    return Lists.newArrayList(AppResourceProcessorType.BANNER_HOME_POP_UP);
  }
}