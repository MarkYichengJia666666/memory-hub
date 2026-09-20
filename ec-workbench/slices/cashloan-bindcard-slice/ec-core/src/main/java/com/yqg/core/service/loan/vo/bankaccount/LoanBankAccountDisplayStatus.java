package com.yqg.core.service.loan.vo.bankaccount;

import com.yqg.core.model.sql.bankaccount.enums.BankAccountAvailableStatus;
import com.yqg.ec.common.exception.EcException;

/**
 * Created by StevenZhu on 16/5/3.
 */
public enum LoanBankAccountDisplayStatus {
  BINDED(1L, "已绑定"),
  AUTH_FAILED(2L, "鉴权失败"),
  UNBINDED(3L, "unbinded");

  public Long id;
  public String name;

  LoanBankAccountDisplayStatus(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public static LoanBankAccountDisplayStatus from(Long id) {
    for (LoanBankAccountDisplayStatus displayStatus : LoanBankAccountDisplayStatus.values()) {
      if (displayStatus.id.equals(id)) {
        return displayStatus;
      }
    }
    throw EcException.error("unknown LoanBankAccountDisplayStatus for id = " + id);
  }

  public static LoanBankAccountDisplayStatus from(BankAccountAvailableStatus status) {
    switch (status) {
      case AVAILABLE:
        return LoanBankAccountDisplayStatus.BINDED;
      case UNBIND:
        return LoanBankAccountDisplayStatus.UNBINDED;
      default:
        return LoanBankAccountDisplayStatus.AUTH_FAILED;
    }
  }

}
