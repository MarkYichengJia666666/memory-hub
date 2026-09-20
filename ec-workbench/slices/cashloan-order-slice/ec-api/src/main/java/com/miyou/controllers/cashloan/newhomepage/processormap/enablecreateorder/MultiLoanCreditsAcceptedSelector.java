package com.miyou.controllers.cashloan.newhomepage.processormap.enablecreateorder;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.AlertProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.IAlertProcessorSelector;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MultiLoanCreditsAcceptedSelector extends AbstractCanOrderProcessorSelector implements IAlertProcessorSelector{
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.MULTI_LOAN_CREDITS_ACCEPTED;
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    List<HomepageNoticeProcessorType> result = Lists.newArrayList(HomepageNoticeProcessorType.ORDER_REJECT_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.OVERDUE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.APPROACH_OVERDUE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.DEFAULT_READY_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.LOW_INTEREST_PRODUCT_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.MULTI_LOAN_CREDITS_ACCEPTED_NOTICE_INFO_PROCESSOR);
    result.addAll(super.getNoticeProcessorList());
    return result;
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    List<HomepageUserInfoV3ProcessorType> pageUserInfoV3Type = super.getPageUserInfoV3Type();
    pageUserInfoV3Type.add(HomepageUserInfoV3ProcessorType.AUTO_JUMP_BILL_PAGE_FOR_NOT_OVERDUE);
    return pageUserInfoV3Type;
  }

  @Override
  public List<AlertProcessorType> getAlertProcessorList() {
    return Lists.newArrayList(
        AlertProcessorType.LENDING_GUIDANCE_ALERT_PROCESSOR,
        AlertProcessorType.REPAYMENT_GUIDANCE_ALERT_PROCESSOR
    );
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.COMMON_TYPE, PageCardV3ProcessorType.MULTI_LOAN_PRODUCT_INFO, PageCardV3ProcessorType.REPAYMENT_CARD, PageCardV3ProcessorType.CREDIT_GAIN_UPGRADE_POPUP, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }
}
