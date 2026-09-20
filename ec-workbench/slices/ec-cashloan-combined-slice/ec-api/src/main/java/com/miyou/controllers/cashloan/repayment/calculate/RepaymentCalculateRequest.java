package com.miyou.controllers.cashloan.repayment.calculate;

import com.miyou.controllers.cashloan.repayment.calculate.infra.RepaymentCalculateScene;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.repay.enums.RepaymentChannelPageDisplayStrategy;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

public class RepaymentCalculateRequest {

  /**
   * 还款试算场景
   */
  @NotNull(message = "还款试算场景 scene 不能为空")
  public RepaymentCalculateScene scene;

  /**
   * 用户实还金额
   */
  @NotNull(message = "用户实还金额 amount 不能为空")
  public BigDecimal amount;

  /**
   * 是否支持部分还款（从 precheck 接口获取）
   */
  @NotNull(message = "是否支持部分还款 canPartialRepayment 不能为空")
  public Boolean canPartialRepayment;

  /**
   * 部分还款实验结果（从 precheck 接口获取，如策略A/B）
   */
  @NotNull(message = "部分还款实验结果 repaymentChannelPageDisplayStrategy 不能为空")
  public RepaymentChannelPageDisplayStrategy repaymentChannelPageDisplayStrategy;

}
