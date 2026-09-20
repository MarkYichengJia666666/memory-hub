package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;

import java.util.List;

public interface IPointProcessorSelector extends IStatusProcessorSelector{
  List<HomepagePointProcessorType> getPointProcessorList();
}
