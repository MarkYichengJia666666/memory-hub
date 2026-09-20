package com.miyou.controllers.cashloan.newhomepage.homepagecontext;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor.HomePageContextProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.IStatusProcessorSelector;
import java.util.List;

public interface IHomepageContextProcessorSelector extends IStatusProcessorSelector {

  default List<HomePageContextProcessorType> getContextProcessors() {
    return Lists.newArrayList(HomePageContextProcessorType.CHANNEL_MERGE_DETECTION);
  }
}
