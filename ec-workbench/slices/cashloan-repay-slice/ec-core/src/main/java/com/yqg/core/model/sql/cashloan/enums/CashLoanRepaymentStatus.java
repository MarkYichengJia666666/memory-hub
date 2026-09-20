package com.yqg.core.model.sql.cashloan.enums;

import com.yqg.ec.common.exception.EcException;

/**
 * Created by ember on 16/4/5.
 */
public enum CashLoanRepaymentStatus {
  INIT("I", "还款初始化"),
  SUCCEED("S", "还款成功"),
  FAIL("F", "还款失败"),
  HANGING("H", "挂起"),
  REFUNDED("R", "已退款"),
  ;

  public String code;
  public String desc;

  CashLoanRepaymentStatus(String code, String desc) {
    this.code = code;
    this.desc = desc;
  }

  public static CashLoanRepaymentStatus fromCode(String code) {
    for (CashLoanRepaymentStatus status : CashLoanRepaymentStatus.values()) {
      if (status.code.equals(code)) {
        return status;
      }
    }
    throw EcException.error("unknown CashLoanOrderRepaymentStatus for code = " + code);
  }
}
