package com.miyou.controllers.cashloan.repayment.card.element.param;

import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElementParam;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class RepaymentRemainingTimeElementParam implements IElementParam {
  public final boolean countDown;
  public final long remainingMillis;
  public final String textColor;
}
