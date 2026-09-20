package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;

import java.util.ArrayList;
import java.util.List;

public interface IPageUserInfoV3Selector extends IStatusProcessorSelector{
  default List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return new ArrayList<>();
  }

}
