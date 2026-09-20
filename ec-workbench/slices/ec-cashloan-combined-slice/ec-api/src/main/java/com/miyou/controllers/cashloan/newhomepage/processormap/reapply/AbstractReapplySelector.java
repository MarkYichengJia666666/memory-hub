package com.miyou.controllers.cashloan.newhomepage.processormap.reapply;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.*;
import com.miyou.controllers.cashloan.newhomepage.processormap.ICommonProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IMiddleProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.INoticeProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IUserInfoProcessorSelector;

import java.util.List;

import static com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR;
import static com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType.REAPPLY_USER_INFO;

public abstract class AbstractReapplySelector implements IUserInfoProcessorSelector,
    IMiddleProcessorSelector, ICommonProcessorSelector, INoticeProcessorSelector {
  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(LOGIN_USER_INFO_PROCESSOR, REAPPLY_USER_INFO);
  }

  @Override
  public List<HomepagePointProcessorType> getPointProcessorList() {
    List<HomepagePointProcessorType> res = IUserInfoProcessorSelector.super.getPointProcessorList();
    res.add(HomepagePointProcessorType.REAPPLY_POINT);
    return res;
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.LOAN_MARKET_WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.REPAYMENT_CARD, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3, HomepageUserInfoV3ProcessorType.LOAN_MARKET_INFO_V3);
  }
}
