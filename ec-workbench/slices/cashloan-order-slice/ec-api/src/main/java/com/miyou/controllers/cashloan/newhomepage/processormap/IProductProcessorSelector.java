package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProductProcessorType;

import java.util.List;

public interface IProductProcessorSelector extends IStatusProcessorSelector {

  List<HomepageProductProcessorType> getProductProcessorList();
}
