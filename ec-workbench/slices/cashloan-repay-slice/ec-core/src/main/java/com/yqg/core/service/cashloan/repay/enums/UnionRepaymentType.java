package com.yqg.core.service.cashloan.repay.enums;

public enum UnionRepaymentType {
  EC_REPAY("EC", "EasyCash"),
  JBP("JBP", "JBP"),
  ;

  public String code;
  public String desc;

  UnionRepaymentType(String code, String desc) {
    this.code = code;
    this.desc = desc;
  }
}
