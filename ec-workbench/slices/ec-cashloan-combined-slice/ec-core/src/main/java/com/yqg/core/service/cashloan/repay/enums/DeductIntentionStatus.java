package com.yqg.core.service.cashloan.repay.enums;

public enum DeductIntentionStatus {
  INIT("已创建"),
  EXPIRED("失效"),
  DEDUCTED("已抵扣"),
  ;

  public String desc;

  DeductIntentionStatus(String desc) {
    this.desc = desc;
  }
}
