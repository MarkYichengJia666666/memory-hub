package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;

import java.util.ArrayList;
import java.util.List;

public interface IPageCardV3ProcessorSelector extends IStatusProcessorSelector{
  default List<PageCardV3ProcessorType> getPageCardSelector() {
    // 等之后实现后干掉
    return new ArrayList<>();
  }

}