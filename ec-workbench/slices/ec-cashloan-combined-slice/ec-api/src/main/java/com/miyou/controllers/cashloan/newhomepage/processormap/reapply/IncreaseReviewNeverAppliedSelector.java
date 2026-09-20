package com.miyou.controllers.cashloan.newhomepage.processormap.reapply;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.*;
import com.miyou.controllers.cashloan.newhomepage.processormap.ICommonProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IMiddleProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.INoticeProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IUserInfoProcessorSelector;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR;
import static com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType.REAPPLY_USER_INFO;
import static com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType.COMMON_TYPE;

@Service
public class IncreaseReviewNeverAppliedSelector implements IUserInfoProcessorSelector,
    ICommonProcessorSelector, IMiddleProcessorSelector, INoticeProcessorSelector {

  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.INCREASE_REVIEW_NEVER_REAPPLIED;
  }

  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(LOGIN_USER_INFO_PROCESSOR, REAPPLY_USER_INFO);
  }

  @Override
  public List<HomepagePointProcessorType> getPointProcessorList() {
    List<HomepagePointProcessorType> res = IUserInfoProcessorSelector.super.getPointProcessorList();
    res.add(HomepagePointProcessorType.INCREASE_REVIEW_NEVER_APPLIED_POINT);
    return res;
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.LOAN_MARKET_WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    return Lists.newArrayList(HomepageNoticeProcessorType.CAN_APPLY_NOW_NOTICE_INFO_PROCESSOR);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(COMMON_TYPE, PageCardV3ProcessorType.REJECT_STATUS, PageCardV3ProcessorType.REPAYMENT_CARD, PageCardV3ProcessorType.LOAN_MARKET, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3, HomepageUserInfoV3ProcessorType.LOAN_MARKET_INFO_V3);
  }
}
