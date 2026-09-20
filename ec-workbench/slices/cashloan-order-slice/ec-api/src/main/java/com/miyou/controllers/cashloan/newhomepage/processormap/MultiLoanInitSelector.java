package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.*;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MultiLoanInitSelector implements IUserInfoProcessorSelector,
    IMiddleProcessorSelector, ICommonProcessorSelector, INoticeProcessorSelector, IAlertProcessorSelector {

  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.MULTI_LOAN_INIT;
  }

  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR, HomepageUserInfoProcessorType.MULTI_LOAN_INIT_USER_INFO);
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.LOAN_MARKET_WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    return Lists.newArrayList(HomepageNoticeProcessorType.OVERDUE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.APPROACH_OVERDUE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.DEFAULT_READY_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.MULTI_LOAN_CREDITS_ACCEPTED_NOTICE_INFO_PROCESSOR);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.COMMON_TYPE, PageCardV3ProcessorType.CREDITS_EXPIRED, PageCardV3ProcessorType.REPAYMENT_CARD, PageCardV3ProcessorType.LOAN_MARKET, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3,
        HomepageUserInfoV3ProcessorType.LOAN_MARKET_INFO_V3,
        HomepageUserInfoV3ProcessorType.AUTO_JUMP_BILL_PAGE_FOR_NOT_OVERDUE);
  }

  @Override
  public List<AlertProcessorType> getAlertProcessorList() {
    return Lists.newArrayList(
        AlertProcessorType.LENDING_GUIDANCE_ALERT_PROCESSOR,
        AlertProcessorType.REPAYMENT_GUIDANCE_ALERT_PROCESSOR
    );
  }

  @Override
  public List<AppResourceProcessorType> getAppResourceProcessorList() {
    return Lists.newArrayList(AppResourceProcessorType.BANNER_HOME_POP_UP_FOR_HAS_ORDER);
  }
}
