package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepageTopAreaProcessorType;

import java.util.List;

public interface ITopAreaProcessorSelector extends IStatusProcessorSelector{
  List<HomepageTopAreaProcessorType> getTopAreaProcessorList();
}
