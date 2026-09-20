package com.yqg.core.model.sql.bankaccount.enums;

import com.yqg.ec.common.exception.EcException;

public enum BankDataQueryType {
  BANK_NAME("bankName"),
  STATE("state"),
  DISTRICT("district"),
  BRANCH("branch"),
  ;

  public String code;

  BankDataQueryType(String charCode) {
    this.code = charCode;
  }

  public static BankDataQueryType fromCodeOrThrow(String code) {
    for (BankDataQueryType bankAccountAvailableStatus : BankDataQueryType.values()) {
      if (bankAccountAvailableStatus.code.equals(code)) {
        return bankAccountAvailableStatus;
      }
    }

    throw EcException.error("BankDataQueryType code is unknown for charcode = " + code);
  }
}
