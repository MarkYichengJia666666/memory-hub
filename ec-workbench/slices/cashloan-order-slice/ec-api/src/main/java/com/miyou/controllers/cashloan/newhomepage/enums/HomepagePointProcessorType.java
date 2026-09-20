package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HomepagePointProcessorType implements HomepageBaseInfoProcessorType{
  BASE_POINT("基础埋点"),
  REJECT("授信拒绝"),
  CAN_REAPPLY_IN_FUTURE_POINT("未来可重新申请"),
  IMAGE_REVIEW_REJECTED_POINT("图片审核拒绝"),

  CANCELLED_POINT("授信取消"),
  NEVER_APPLIED_POINT("Never Applied Point"),
  IN_REVIEW_POINT("In Review Point"),
  WAITING_FOR_SUPPLEMENT_POINT("Waiting For Supplement Point"),
  INCREASE_REVIEW_REJECT_POINT("Increase Review Reject Point"),
  REAPPLY_POINT("Reapply Point"),
  INCREASE_REVIEW_NEVER_APPLIED_POINT("Increase Review Never Applied Point"),
  ;

  public String desc;

  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.BURIED_PINT_INTO;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends HomepagePointProcessorType> getTypeClazz() {
    return HomepagePointProcessorType.class;
  }
}
