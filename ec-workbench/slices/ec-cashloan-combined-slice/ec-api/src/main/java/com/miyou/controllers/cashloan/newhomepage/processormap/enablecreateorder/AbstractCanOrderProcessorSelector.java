package com.miyou.controllers.cashloan.newhomepage.processormap.enablecreateorder;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.*;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor.HomePageContextProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.ICommonProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IProductProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.ITopAreaProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.IUserInfoProcessorSelector;
import com.miyou.controllers.cashloan.newhomepage.processormap.*;


import java.util.List;

/**
 * 可下单的statuses中剔除降额快速下单的statuses
 * 如果要在全部可下单的状态下都生效，不要遗漏降额快速下单AbstractDecreaseQuickOrderSelector
 */
public abstract class AbstractCanOrderProcessorSelector implements IProductProcessorSelector, IUserInfoProcessorSelector,
    ITopAreaProcessorSelector, ICommonProcessorSelector, INoticeProcessorSelector, IMiddleProcessorSelector, IAlertProcessorSelector {

  @Override
  public List<HomepageProductProcessorType> getProductProcessorList() {
    return Lists.newArrayList(HomepageProductProcessorType.CAN_CREATE_ORDER_PRODUCT_INFO_PROCESSOR);
  }

  @Override
  public List<HomepageUserInfoProcessorType> getUserInfoProcessorList() {
    return Lists.newArrayList(HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR,
        HomepageUserInfoProcessorType.CAN_CREATE_ORDER_USER_INFO);
  }

  @Override
  public List<HomepageTopAreaProcessorType> getTopAreaProcessorList() {
    return Lists.newArrayList(HomepageTopAreaProcessorType.CREATE_ORDER_PAGE_TOP_AREA);
  }

  @Override
  public List<HomepageNoticeProcessorType> getNoticeProcessorList() {
    return Lists.newArrayList(HomepageNoticeProcessorType.CAN_CREATE_ORDER_NOTICE_INFO_PROCESSOR);
  }

  @Override
  public List<AppResourceProcessorType> getAppResourceProcessorList() {
    return Lists.newArrayList(AppResourceProcessorType.RESOURCE_CAN_CREATE_ORDER_PRE_ACTION, AppResourceProcessorType.BANNER_HOME_POP_UP_FOR_CAN_CREATE_ORDER);
  }

  @Override
  public List<HomepageMiddleProcessorType> getMiddleProcessorList() {
    return Lists.newArrayList(HomepageMiddleProcessorType.WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR);
  }

  @Override
  public List<PageCardV3ProcessorType> getPageCardSelector() {
    return Lists.newArrayList(PageCardV3ProcessorType.COMMON_TYPE, PageCardV3ProcessorType.LOAN_PRODUCT_INFO, PageCardV3ProcessorType.REPAYMENT_CARD, PageCardV3ProcessorType.CREDIT_GAIN_UPGRADE_POPUP, PageCardV3ProcessorType.LAST_COMMON_CARD);
  }

  @Override
  public List<HomepageUserInfoV3ProcessorType> getPageUserInfoV3Type() {
    return Lists.newArrayList(HomepageUserInfoV3ProcessorType.REPAYMENT_INFO_V3);
  }

  @Override
  public List<HomePageConfigProcessorType> getPageConfigProcessorTypes() {
    return Lists.newArrayList(HomePageConfigProcessorType.COMMON_CONFIG, 
        HomePageConfigProcessorType.ORDER_PAGE_CONFIG,
        HomePageConfigProcessorType.H5_WHOLE_PROCESS_ORDER_UI_CONFIG
    );
  }

  @Override
  public List<AlertProcessorType> getAlertProcessorList() {
    return Lists.newArrayList(AlertProcessorType.LENDING_GUIDANCE_ALERT_PROCESSOR, AlertProcessorType.REPAYMENT_GUIDANCE_ALERT_PROCESSOR);
  }

  @Override
  public List<HomePageContextProcessorType> getContextProcessors() {
    return Lists.newArrayList(HomePageContextProcessorType.CHANNEL_MERGE_DETECTION, HomePageContextProcessorType.ORDER_PAGE_AMOUNT_INPUT_SIMPLIFY);
  }

}
