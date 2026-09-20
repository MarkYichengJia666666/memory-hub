package com.yqg.core.model.sql.bankaccount.enums;

import com.google.common.collect.ImmutableList;
import com.yqg.ec.common.exception.EcException;

import java.util.List;

public enum BankAccountAvailableStatus {
  AVAILABLE("A", "可用"),
  UNAVAILABLE("U", "不可用"),
  PENDING("P", "验卡中"),
  IN_REVIEW("R", "待用户确认"),
  UNBIND("D", "previously available then unbind by the user"),
  INIT("N", "INIT")
  ;

  public String charCode;
  public String desc;

  public static final List<BankAccountAvailableStatus> NEED_CONFIRM_STATUS_LIST = ImmutableList.of(UNAVAILABLE, IN_REVIEW);

  public static final List<BankAccountAvailableStatus> CHECK_STATUS_LIST = ImmutableList.of(AVAILABLE, PENDING);

  public static final List<BankAccountAvailableStatus> INACTIVE_STATUS_LIST = ImmutableList.of(UNAVAILABLE, UNBIND, INIT);

  BankAccountAvailableStatus(String charCode, String desc) {
    this.charCode = charCode;
    this.desc = desc;
  }

  public static BankAccountAvailableStatus fromCodeOrThrow(String code) {
    for (BankAccountAvailableStatus bankAccountAvailableStatus : BankAccountAvailableStatus.values()) {
      if (bankAccountAvailableStatus.charCode.equals(code)) {
        return bankAccountAvailableStatus;
      }
    }

    throw EcException.error("bank account available status is unknown for charcode = " + code);
  }
}
