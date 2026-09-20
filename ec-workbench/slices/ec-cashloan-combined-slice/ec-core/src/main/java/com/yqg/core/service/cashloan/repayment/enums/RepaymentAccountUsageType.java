package com.yqg.core.service.cashloan.repayment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 还款VA的用途
 * 一个用途对应一个Handler实现类
 */
@Getter
@AllArgsConstructor
public enum RepaymentAccountUsageType {
  JBP_VA("仅支付中收订单的JBP_VA"),
  UNION_REPAY_EC_VA("合并支付的EC_VA，适用于只还EC账单和同时还EC和中收的情况"),
  JBP_EC_VA("仅支付中收订单的EC_VA");

  private final String desc;
}
