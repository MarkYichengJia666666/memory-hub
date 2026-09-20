package com.miyou.controllers.cashloan.newhomepage.processormap.enablecreateorder;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PayoutFailedSelector extends AbstractCanOrderProcessorSelector{
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.PAYOUT_FAILED;
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    List<HomepageNoticeProcessorType> result = Lists.newArrayList(HomepageNoticeProcessorType.NEW_PAYOUT_NOTICE_INFO_PROCESSOR, HomepageNoticeProcessorType.PAYOUT_NOTICE_INFO_PROCESSOR);
    result.addAll(super.getNoticeProcessorList());
    return result;
  }
}