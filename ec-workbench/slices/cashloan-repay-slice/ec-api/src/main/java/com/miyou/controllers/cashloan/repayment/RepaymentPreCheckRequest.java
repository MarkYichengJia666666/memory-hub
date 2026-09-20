package com.miyou.controllers.cashloan.repayment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RepaymentPreCheckRequest {
  @JsonProperty("instalmentIds")
  public List<String> encodeInstalmentIds;

  @JsonProperty("unionRepaymentRequests")
  public List<UnionRepaymentRequest> unionRepaymentRequests;

  @JsonProperty("pointUseInfo")
  public RepaymentPointUseInfoRequest pointUseInfo;

  public static class UnionRepaymentRequest {
    /**
     * 业务ID
     */
    @JsonProperty("encodeBusinessIds")
    public List<String> encodeBusinessIds;
    /**
     * 联合支付类型
     */
    @JsonProperty("unionRepaymentType")
    public String unionRepaymentType;
  }
}
