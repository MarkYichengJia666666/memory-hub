  package com.miyou.controllers.cashloan.newhomepage.processormap.review;

  import com.google.common.collect.Lists;
  import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
  import com.miyou.controllers.cashloan.newhomepage.enums.HomepagePointProcessorType;
  import com.miyou.controllers.cashloan.newhomepage.processormap.ICommonProcessorSelector;
  import com.miyou.controllers.cashloan.newhomepage.processormap.IMiddleProcessorSelector;
  import com.miyou.controllers.cashloan.newhomepage.processormap.IUserInfoProcessorSelector;

  import java.util.List;

public abstract class AbstractInReviewStatusSelector implements IUserInfoProcessorSelector, IMiddleProcessorSelector , ICommonProcessorSelector {
  @Override
  public List<HomepagePointProcessorType> getPointProcessorList() {
    List<HomepagePointProcessorType> res = IUserInfoProcessorSelector.super.getPointProcessorList();
    res.add(HomepagePointProcessorType.IN_REVIEW_POINT);
    return res;
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }
}
