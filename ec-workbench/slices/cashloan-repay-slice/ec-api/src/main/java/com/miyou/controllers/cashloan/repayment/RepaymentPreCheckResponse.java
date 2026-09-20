package com.miyou.controllers.cashloan.repayment;

import com.yqg.core.service.cashloan.repay.enums.RepaymentChannelPageDisplayStrategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentPreCheckResponse {

  private String h5RepaymentUrl;
  private boolean canPartialRepayment;
  private RepaymentChannelPageDisplayStrategy repaymentChannelPageDisplayStrategy;
}
