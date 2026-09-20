package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.*;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor.HomePageContextProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.IAlertProcessorSelector;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.loan.credits.LoanUserVirtualCreditsService;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AcceptButCanNotLoanSelector implements IUserInfoProcessorSelector, IMiddleProcessorSelector, INoticeProcessorSelector, ICommonProcessorSelector, IAlertProcessorSelector {

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    return Lists.newArrayList(HomepageNoticeProcessorType.RISK_EXTRA_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.OVERDUE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.APPROACH_OVERDUE_NOTICE_INFO_PROCESSOR,
        HomepageNoticeProcessorType.DEFAULT_READY_NOTICE_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR, HomepageUserInfoProcessorType.ACCEPT_BUT_CAN_NOT_LOAN_FOR_QUOTA_CARD,
        HomepageUserInfoProcessorType.ACCEPT_BUT_CAN_NOT_LOAN);
  }

  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.ACCEPTED_BUT_CAN_NOT_LOAN;
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(
        PageCardV3ProcessorType.COMMON_TYPE,
        PageCardV3ProcessorType.CAN_NOT_LOAN_CREDIT,
        PageCardV3ProcessorType.REPAYMENT_CARD,
        PageCardV3ProcessorType.MAIN_CARD_REPAYMENT_INFO,
        PageCardV3ProcessorType.LAST_COMMON_CARD
    );
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3, HomepageUserInfoV3ProcessorType.AUTO_JUMP_BILL_PAGE_FOR_NOT_OVERDUE);
  }

  @Override
  public List<HomePageContextProcessorType> getContextProcessors() {
    return Lists.newArrayList(HomePageContextProcessorType.CHANNEL_MERGE_DETECTION, HomePageContextProcessorType.ACCEPT_BUT_CAN_NOT_LOAN);
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
