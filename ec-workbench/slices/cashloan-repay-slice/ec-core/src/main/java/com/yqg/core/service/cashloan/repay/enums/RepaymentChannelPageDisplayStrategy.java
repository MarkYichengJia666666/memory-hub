package com.yqg.core.service.cashloan.repay.enums;

import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;

/**
 * 部分还款实验 - 还款渠道列表页 (APP|H5) 展示策略
 */
@AllArgsConstructor
public enum RepaymentChannelPageDisplayStrategy {
  /**
   * native + 老 UI + 不支持部分还款
   */
  A(false, false),
  /**
   * H5 + 新 UI + 不支持部分还款
   */
  B(false, true),
  /**
   * H5 + 新 UI + 支持部分还款
   */
  C(true, true),
  /**
   * H5 + 新 UI + 仅逾期后支持部分还款
   */
  D(true, true),
  ;

  private static final ImmutableList<RepaymentChannelPageDisplayStrategy> CONDITIONAL_SUPPORT_PARTIAL_REPAYMENT_STRATEGY_LIST = ImmutableList.of(D);

  public final boolean supportsPartialRepayment;
  public final boolean supportsWebPage;

  public boolean isConditionalSupportPartialRepaymentStrategy() {
    return CONDITIONAL_SUPPORT_PARTIAL_REPAYMENT_STRATEGY_LIST.contains(this);
  }
}
