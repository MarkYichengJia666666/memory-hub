package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HomepageNoticeProcessorType implements HomepageBaseInfoProcessorType{
  CAN_CREATE_ORDER_NOTICE_INFO_PROCESSOR("create order notice info"),
  RECENTLY_ORDERS_NOTICE_INFO_PROCESSOR("recently orders notice"),
  NEW_PAYOUT_NOTICE_INFO_PROCESSOR("new payout notice"),
  PAYOUT_NOTICE_INFO_PROCESSOR("payout notice"),
  LOAN_CREDITS_DECREASE_NOTICE_INFO_PROCESSOR("loan credits decrease"),
  CAN_APPLY_NOW_NOTICE_INFO_PROCESSOR("can apply"),
  DEFAULT_READY_NOTICE_INFO_PROCESSOR("default ready"),
  OVERDUE_NOTICE_INFO_PROCESSOR("overdue notice"),
  APPROACH_OVERDUE_NOTICE_INFO_PROCESSOR("approach overdue notice"),
  MULTI_LOAN_CREDITS_ACCEPTED_NOTICE_INFO_PROCESSOR("multi loan credits accepted"),
  ORDER_REJECT_NOTICE_INFO_PROCESSOR("订单被拒"),
  LOW_INTEREST_PRODUCT_NOTICE_INFO_PROCESSOR("low interest product notice"),
  RISK_EXTRA_NOTICE_INFO_PROCESSOR("risk extra notice"),
  CREDITS_CHANGE_NOTICE_INFO_PROCESSOR("credits change notice"),
  ;

  public String desc;

  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.NOTICE_INFO;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends HomepageNoticeProcessorType> getTypeClazz() {
    return HomepageNoticeProcessorType.class;
  }
}