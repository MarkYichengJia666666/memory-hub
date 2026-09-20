package com.miyou.controllers.cashloan.newhomepage.processormap.repayment;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.*;
import com.miyou.controllers.cashloan.newhomepage.processormap.IAlertProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.ICommonProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IMiddleProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.INoticeProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IUserInfoProcessorSelector;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReloanOverdueSelector implements IUserInfoProcessorSelector, IMiddleProcessorSelector,
    INoticeProcessorSelector, ICommonProcessorSelector, IAlertProcessorSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.RELOAN_OVERDUE;
  }

  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR,
        HomepageUserInfoProcessorType.REPAYMENT_USER_INFO, HomepageUserInfoProcessorType.RELOAN_REJECT_USER_INFO, HomepageUserInfoProcessorType.REPAYMENT_QUOTA_CARD_INFO);
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.LOAN_MARKET_WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    return Lists.newArrayList(HomepageNoticeProcessorType.RISK_EXTRA_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.OVERDUE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.APPROACH_OVERDUE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.DEFAULT_READY_NOTICE_INFO_PROCESSOR);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(
        PageCardV3ProcessorType.OVERDUE_REPAYMENT_CARD,
        PageCardV3ProcessorType.MAIN_CARD_REPAYMENT_INFO,
        PageCardV3ProcessorType.LOAN_MARKET,
        PageCardV3ProcessorType.LAST_COMMON_CARD
    );
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(
        HomepageUserInfoV3ProcessorType.HOME_PAGE_GUIDE_BUBBLE_V3,
        HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3,
        HomepageUserInfoV3ProcessorType.AUTO_JUMP_BILL_PAGE_FOR_OVERDUE,
        HomepageUserInfoV3ProcessorType.LOAN_MARKET_INFO_V3
    );
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
    return Lists.newArrayList(AppResourceProcessorType.BANNER_HOME_POP_UP_FOR_OVERDUE);
  }
}
