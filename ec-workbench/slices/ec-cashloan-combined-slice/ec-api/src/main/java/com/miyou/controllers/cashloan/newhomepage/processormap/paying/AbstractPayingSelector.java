package com.miyou.controllers.cashloan.newhomepage.processormap.paying;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.ICommonProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IMiddleProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IUserInfoProcessorSelector;

import java.util.List;

public abstract class AbstractPayingSelector implements IUserInfoProcessorSelector, IMiddleProcessorSelector, ICommonProcessorSelector {
  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR, HomepageUserInfoProcessorType.PAYING_USER_INFO);
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.COMMON_TYPE, PageCardV3ProcessorType.ORDER_PROGRESS_STEP, PageCardV3ProcessorType.REPAYMENT_CARD, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(HomepageUserInfoV3ProcessorType.DIRECT_DEBIT_BANK_V3, HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3);
  }
}
