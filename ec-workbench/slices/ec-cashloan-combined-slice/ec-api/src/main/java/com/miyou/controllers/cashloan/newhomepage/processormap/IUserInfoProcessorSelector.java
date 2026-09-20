package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;

import java.util.List;

public interface IUserInfoProcessorSelector extends IStatusProcessorSelector, IPointProcessorSelector {

  List<HomepageUserInfoProcessorType> getUserInfoProcessorList();

  default List<HomepagePointProcessorType> getPointProcessorList() {
    return Lists.newArrayList(HomepagePointProcessorType.BASE_POINT);
  }
}
