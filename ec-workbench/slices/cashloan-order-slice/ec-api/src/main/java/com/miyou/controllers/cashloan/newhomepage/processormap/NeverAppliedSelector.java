package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.*;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NeverAppliedSelector implements IUserInfoProcessorSelector,
    IMiddleProcessorSelector , ICommonProcessorSelector, INoticeProcessorSelector{

  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.NEVER_APPLIED;
  }

  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR, HomepageUserInfoProcessorType.NEVER_APPLIED_USER_INFO);
  }

  @Override
  public List<HomepagePointProcessorType> getPointProcessorList() {
    List<HomepagePointProcessorType> pointProcessorList = IUserInfoProcessorSelector.super.getPointProcessorList();
    pointProcessorList.add(HomepagePointProcessorType.NEVER_APPLIED_POINT);
    return pointProcessorList;
  }

  @Override
  public List<AppResourceProcessorType> getAppResourceProcessorList() {
    return Lists.newArrayList(AppResourceProcessorType.BANNER_HOME_POP_UP_FOR_NEVER_APPLIED);
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    return Lists.newArrayList(HomepageNoticeProcessorType.RECENTLY_ORDERS_NOTICE_INFO_PROCESSOR);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.COMMON_TYPE, PageCardV3ProcessorType.NEVER_APPLIED, PageCardV3ProcessorType.REPAYMENT_CARD, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3, HomepageUserInfoV3ProcessorType.PAGE_JUMP_V3);
  }
}
