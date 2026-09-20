package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.HomePageConfigProcessorType;

import java.util.List;

public interface IPageConfigSelector extends IStatusProcessorSelector{
  List<HomePageConfigProcessorType> getPageConfigProcessorTypes();
}
