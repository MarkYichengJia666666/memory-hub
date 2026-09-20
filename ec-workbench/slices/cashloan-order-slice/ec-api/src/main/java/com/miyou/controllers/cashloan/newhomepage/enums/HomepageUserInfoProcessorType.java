package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HomepageUserInfoProcessorType implements HomepageBaseInfoProcessorType {
  NOT_LOGIN_USER_INFO_PROCESSOR("未登录用户信息"),
  LOGIN_USER_INFO_PROCESSOR("登录用户默认信息"),
  CAN_CREATE_ORDER_USER_INFO("可下单用户信息"),
  MINIMALIST_ACCEPT_USER_INFO("极简授信通过"),
  NEVER_APPLIED_USER_INFO("Never Applied User Info"),
  REAPPLY_USER_INFO("Reapply Credit User Info"),
  MULTI_LOAN_INIT_USER_INFO("Multi Loan Init User Info"),
  MINIMALIST_IN_REVIEW_USER_INFO("Minimalist In Review User Info"),
  FUND_ORDER_BIZ_CHECK("Fund Order Biz Check"),
  GRAB_ORDER_BIZ_CHECK("Grab Order Biz Check"),
  REJECT("授信拒绝"),
  CANCELLED("授信取消"),
  IMAGE_REVIEW_REJECTED("照片重传"),
  CAN_REAPPLY_IN_FUTURE("未来可重新申请"),
  NEED_SUPPLEMENT_USER_INFO("Need Supplement User Info"),
  SIGNATURE_BIZ_CHECK("Signature Biz Check"),
  DEBT_BIZ_CHECK("Debt Biz Check"),
  IN_REVIEW_USER_INFO("In Review User Info"),
  REUPLOAD_FINISHED_USER_INFO("Reupload Finished User Info"),
  FUND_PAYOUT_BIZ_CHECK("Fund Payout Biz Check"),
  CALC_CREDITS_IN_REVIEW_USER_INFO("Calc Credits In Review User Info"),
  MULTI_LOAN_IN_REVIEW_USER_INFO("Multi Loan In Review User Info"),
  INCREASE_REVIEW_REJECT_USER_INFO("Increase Review Reject User Info"),
  PAYING_USER_INFO("Paying User Info"),
  REPAYMENT_USER_INFO("Repayment User Info"),
  WAITING_SUPPLEMENT_USER_INFO("Waiting Supplement User Info"),
  FINISH_SUPPLEMENT_USER_INFO("Finish Supplement User Info"),
  RELOAN_REJECT_USER_INFO("Reloan Reject User Info"),
  READY_USER_INFO("Ready User Info"),
  QUOTA_CARD_INFO("额度卡片内容"),
  REPAYMENT_QUOTA_CARD_INFO("还款页额度卡片"),
  ACCEPT_BUT_CAN_NOT_LOAN("授信通过但不可借"),
  ACCEPT_BUT_CAN_NOT_LOAN_FOR_QUOTA_CARD("授信通过但不可借额度卡片")
  ;


  private String desc;
  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.USER_INFO;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends HomepageBaseInfoProcessorType> getTypeClazz() {
    return HomepageUserInfoProcessorType.class;
  }
}
