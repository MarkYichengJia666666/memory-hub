package com.yqg.core.service.loan.bankaccount;

import java.math.BigDecimal;

/**
 * 放款渠道限额配置（TAPD-364191）。
 *
 * <p>按 {@link com.yqg.core.model.sql.bankaccount.enums.BankType} 维度配置单笔放款上限与两处提示文案，
 * 命中实验的用户在查询接口拿到超限标识后由前端置灰不可选，主链路不做硬拦截。文案配置中文模板，
 * 由 {@link com.yqg.translation.client.utils.TT} 翻译下发，{@code {0}} 占位符会被替换为格式化后的限额金额。
 */
public class PayoutLimitConfigVO {

  /**
   * 单笔放款上限；{@code null} 表示该渠道不限额。
   */
  public BigDecimal maxPayoutAmount;

  /**
   * 放款账户列表（listPaymentCredentials）超限时的长文案，支持 {@code {0}} 金额占位。
   */
  public String listHint;

  /**
   * 下单页（productDetail）账户模块超限时的短标签文案，支持 {@code {0}} 金额占位。
   */
  public String badgeHint;
}
