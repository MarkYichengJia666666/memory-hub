package com.yqg.core.service.cashloan.repay.enums;

public enum RepaymentHomeDisplayStrategy {
  A, //首先按照金额匹配缓存，查找指定还款订单，匹配不上则自动归集还款
  B, //自动归集还款
  ;
}
