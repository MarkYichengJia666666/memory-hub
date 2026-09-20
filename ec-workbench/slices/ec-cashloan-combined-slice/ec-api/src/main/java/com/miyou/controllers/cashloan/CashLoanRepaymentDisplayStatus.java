package com.miyou.controllers.cashloan;

import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentStatus;
import com.yqg.ec.common.exception.EcException;

/**
 * Created by ember on 16/4/13.
 */
public enum CashLoanRepaymentDisplayStatus {
  PROCESSING("I", "处理中"),
  SUCCEEDED("S", "还款成功"),
  FAILED("F", "还款失败"),
  HANGING("H", "挂起"),
  REFUNDED("R", "已退款"),
  ;
  public String code;
  public String desc;

  CashLoanRepaymentDisplayStatus(String code, String desc) {
    this.code = code;
    this.desc = desc;
  }

  public static CashLoanRepaymentDisplayStatus from(CashLoanRepaymentStatus status) {
    switch (status) {
      case INIT:
        return CashLoanRepaymentDisplayStatus.PROCESSING;
      case SUCCEED:
        return CashLoanRepaymentDisplayStatus.SUCCEEDED;
      case FAIL:
        return CashLoanRepaymentDisplayStatus.FAILED;
      case HANGING:
        return CashLoanRepaymentDisplayStatus.HANGING;
      case REFUNDED:
        return CashLoanRepaymentDisplayStatus.REFUNDED;
      default:
        throw EcException.error("Unexpected status = " + status.code);
    }
  }
}
