package com.miyou.controllers.cashloan.newhomepage.processormap.reapply;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanCreditsExpiredSelector extends AbstractReapplySelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.LOAN_CREDITS_EXPIRED;
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    return Lists.newArrayList(HomepageNoticeProcessorType.LOAN_CREDITS_DECREASE_NOTICE_INFO_PROCESSOR);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.COMMON_TYPE, PageCardV3ProcessorType.CREDITS_EXPIRED, PageCardV3ProcessorType.LOAN_MARKET, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }
}
