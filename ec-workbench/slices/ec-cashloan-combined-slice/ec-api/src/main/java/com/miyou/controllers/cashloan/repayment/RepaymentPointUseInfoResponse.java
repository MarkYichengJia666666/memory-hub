package com.miyou.controllers.cashloan.repayment;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 还款 preCheck 积分使用出参，对齐技术方案 5.3.4。
 */
@Data
public class RepaymentPointUseInfoResponse {

  /** SKIPPED / RESERVED */
  private String pointStatus;
  private String reservationNo;
  private Long usePoint;
  private BigDecimal useAmount;
  private BigDecimal payableAmount;
  private String expiredTime;
}
