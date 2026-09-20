package com.miyou.controllers.cashloan.repayment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 还款 preCheck 积分使用入参，对齐技术方案 5.3.4。
 */
@Data
public class RepaymentPointUseInfoRequest {

  @JsonProperty("selected")
  private Boolean selected;

  @JsonProperty("expectedUsePoint")
  private Long expectedUsePoint;

  /** 预算接口返回的券折扣额，服务端用于校正 bizAmount。来源：/api/loan/v2/user/coupon 的 deductAmount */
  @JsonProperty("couponDeductAmount")
  private BigDecimal couponDeductAmount;
}
