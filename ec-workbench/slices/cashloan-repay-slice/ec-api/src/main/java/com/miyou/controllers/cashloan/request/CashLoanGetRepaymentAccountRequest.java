package com.miyou.controllers.cashloan.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class CashLoanGetRepaymentAccountRequest {
  @NotBlank(message = "channel不能为空")
  public String channel;
  public Long couponId;
  @NotNull(message = "金额不能为空")
  public BigDecimal amount;
  //  @NotEmpty(message = "分期Id不能为空")
  @JsonProperty("instalmentIds")
  public List<String> encodeInstalmentIds;
  public String mobileNumber;
  //JBP订单id
  @JsonProperty("jbpOrderIds")
  public List<String> encodeJBPOrderIds;
  public RepayStyleVersion repayStyleVersion;

  @AssertTrue(message = "分期Id和jbp订单id必填其一")
  public boolean getValid() {
    //校验EC分期id列表encodeInstalmentIds和联合支付列表unionRepaymentRequests必填其一
    return CollectionUtils.isNotEmpty(this.encodeInstalmentIds) || CollectionUtils.isNotEmpty(this.encodeJBPOrderIds);
  }
}
