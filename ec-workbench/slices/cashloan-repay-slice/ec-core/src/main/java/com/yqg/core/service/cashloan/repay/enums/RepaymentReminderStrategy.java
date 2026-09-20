package com.yqg.core.service.cashloan.repay.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RepaymentReminderStrategy {
  A,
  B,
  C,
  ;

  public boolean isAorB() {
    return this == A || this == B;
  }

  public boolean isC() {
    return this == C;
  }

}
