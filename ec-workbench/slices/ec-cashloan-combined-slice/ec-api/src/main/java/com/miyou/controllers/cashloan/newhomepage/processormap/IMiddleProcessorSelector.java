package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;

import java.util.List;

public interface IMiddleProcessorSelector extends IStatusProcessorSelector{
  List<HomepageMiddleProcessorType> getMiddleProcessorList();
}