package com.miyou.controllers.cashloan.newhomepage.processormap.enablecreateorder;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanCreditsDecreaseSelector extends AbstractCanOrderProcessorSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.LOAN_CREDITS_DECREASE;
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    List<HomepageNoticeProcessorType> result = Lists.newArrayList(HomepageNoticeProcessorType.CREDITS_CHANGE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.LOW_INTEREST_PRODUCT_NOTICE_INFO_PROCESSOR);
    result.addAll(super.getNoticeProcessorList());
    return result;
  }
}