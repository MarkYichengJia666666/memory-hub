package com.yqg.core.service.cashloan.cashloanrepaystrategy.enums;

import com.google.common.collect.ImmutableList;
import com.yqg.ec.common.exception.EcException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public enum CashLoanRepayStrategyStatus {
  VALID("V", "有效"),
  INVALID("I","失效"),
  WAIT_NOTIFY_INVALID("WI", "失效待通知"),
  EXPIRED("E","过期"),
  WAIT_NOTIFY_EXPIRED("WE", "过期待通知"),
  SUCCESS("S","成功"),
  WAIT_NOTIFY_SUCCESS("WS", "成功待通知"),
  BACK("B","撤销"),
  WAIT_NOTIFY_BACK("WB", "撤销待通知"),
  ;

  /**
   * 有效：初始化任务
   * 成功：任务在有效期内足额还款，状态从有效变成功
   * 过期：任务在有效期内未还款，状态从有效变过期
   * 撤销：未到减免有效期，由于特殊情况需要撤回减免，状态从有效变撤销
   * 失效：任务在有效期内未足额还款，状态从有效变成功
   */
  private String code;
  private String desc;

  public final static List<CashLoanRepayStrategyStatus> WAIT_STATUS_LIST = ImmutableList.of(WAIT_NOTIFY_INVALID, WAIT_NOTIFY_EXPIRED, WAIT_NOTIFY_SUCCESS, WAIT_NOTIFY_BACK);
  public final static List<CashLoanRepayStrategyStatus> FINAL_STATUS_LIST = ImmutableList.of(INVALID, EXPIRED, SUCCESS, BACK);

  public static CashLoanRepayStrategyStatus fromCode(String code) {
    for (CashLoanRepayStrategyStatus status : CashLoanRepayStrategyStatus.values()) {
      if (status.getCode().equals(code)) {
        return status;
      }
    }
    throw EcException.error("CashLoanRepayStrategyStatus fromCode error, code: " + code);
  }

  public boolean isValid() {
    return this == CashLoanRepayStrategyStatus.VALID;
  }

  public Boolean isSuccess() {
    return this == CashLoanRepayStrategyStatus.SUCCESS;
  }

  public Boolean isFinalStatus() {
    return FINAL_STATUS_LIST.contains(this);
  }
}
