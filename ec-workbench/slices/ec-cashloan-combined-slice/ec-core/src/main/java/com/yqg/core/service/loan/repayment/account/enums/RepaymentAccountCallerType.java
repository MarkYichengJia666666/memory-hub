package com.yqg.core.service.loan.repayment.account.enums;

/**
 * 还款账号获取的调用来源类型
 * 用于控制 Provider 选择逻辑是否生效
 */
public enum RepaymentAccountCallerType {
  /**
   * 默认调用方，不应用 Provider 分流选择逻辑
   */
  DEFAULT,

  /**
   * UnionRepaymentAccountHandler 调用，应用 Provider 分流选择逻辑
   */
  UNION_REPAYMENT_HANDLER,
  ;

  /**
   * 是否需要应用 Provider 选择器的分流逻辑
   */
  public boolean shouldApplyProviderSelector() {
    return this == UNION_REPAYMENT_HANDLER;
  }
}

