package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.HomePageConfigProcessorType;

import com.miyou.controllers.cashloan.newhomepage.homepagecontext.IHomepageContextProcessorSelector;
import java.util.List;

/**
 * 作为标记接口，一些所有状态都需要的处理器选择器可以继承此接口
 */
public interface ICommonProcessorSelector extends IAppResourceProcessorSelector,
    IPageConfigSelector, IPageUserInfoV3Selector, IPageCardV3ProcessorSelector, IHomepageContextProcessorSelector {
  @Override
  default List<HomePageConfigProcessorType> getPageConfigProcessorTypes() {
    return Lists.newArrayList(HomePageConfigProcessorType.COMMON_CONFIG , HomePageConfigProcessorType.H5_WHOLE_PROCESS_ORDER_UI_CONFIG);
  }
}
