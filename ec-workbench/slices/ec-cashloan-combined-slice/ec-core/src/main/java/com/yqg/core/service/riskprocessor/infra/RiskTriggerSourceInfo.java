package com.yqg.core.service.riskprocessor.infra;

import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;

/**
 * 触发来源 + 触发来源外部 id 的组合载体：把「事件来源」与「引发该事件的业务对象主键」成对传递，
 * 避免在各风控入口方法上分别追加两个参数导致参数膨胀（外部 id 指向的对象类型由 source 决定，可空）。
 */
public class RiskTriggerSourceInfo {

  public final LoanUserRiskTriggerSource source;
  public final String externalId;

  private RiskTriggerSourceInfo(LoanUserRiskTriggerSource source, String externalId) {
    this.source = source;
    this.externalId = externalId;
  }

  /**
   * 统一入口：调用方必须显式传入外部 id（无外部对象时传 null），
   * 以便在编码时明确「该来源是否携带外部对象主键」。
   */
  public static RiskTriggerSourceInfo of(LoanUserRiskTriggerSource source, Long externalId) {
    return new RiskTriggerSourceInfo(source, externalId == null ? null : String.valueOf(externalId));
  }
}
