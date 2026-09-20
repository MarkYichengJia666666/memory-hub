package com.miyou.controllers.cashloan.newhomepage.processormap.rejectstatus;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.*;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor.HomePageContextProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.ICommonProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IMiddleProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IUserInfoProcessorSelector;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CanReapplyInFutureStatusSelector implements IUserInfoProcessorSelector, IMiddleProcessorSelector, ICommonProcessorSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.CAN_REAPPLY_IN_FUTURE;
  }

  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR, HomepageUserInfoProcessorType.CAN_REAPPLY_IN_FUTURE, HomepageUserInfoProcessorType.QUOTA_CARD_INFO);
  }

  @Override
  public List<HomepagePointProcessorType> getPointProcessorList() {
    List<HomepagePointProcessorType> pointProcessorList = IUserInfoProcessorSelector.super.getPointProcessorList();
    pointProcessorList.add(HomepagePointProcessorType.CAN_REAPPLY_IN_FUTURE_POINT);
    return pointProcessorList;
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.LOAN_MARKET_WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }


  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.COMMON_TYPE, PageCardV3ProcessorType.REJECT_STATUS, PageCardV3ProcessorType.REPAYMENT_CARD, PageCardV3ProcessorType.LOAN_MARKET, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3, HomepageUserInfoV3ProcessorType.LOAN_MARKET_INFO_V3);
  }

  @Override
  public List<HomePageContextProcessorType> getContextProcessors() {
    return Lists.newArrayList(HomePageContextProcessorType.CHANNEL_MERGE_DETECTION, HomePageContextProcessorType.SECOND_RISK_REJECT_DISPLAY_STRATEGY);
  }
}
