package com.yqg.core.service.risk.usergroup.enums;

import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;

import lombok.Getter;

@Getter
public enum RiskUserGroupCheckCreditStatus {
  ACCEPTED("通过", LoanCreditsStatus.ACCEPTED),
  REJECTED("拒绝", LoanCreditsStatus.REJECTED),
  ;

  public final String desc;
  public final LoanCreditsStatus traceCreditsStatus;

  RiskUserGroupCheckCreditStatus(String desc, LoanCreditsStatus traceCreditsStatus) {
    this.desc = desc;
    this.traceCreditsStatus = traceCreditsStatus;
  }

}
