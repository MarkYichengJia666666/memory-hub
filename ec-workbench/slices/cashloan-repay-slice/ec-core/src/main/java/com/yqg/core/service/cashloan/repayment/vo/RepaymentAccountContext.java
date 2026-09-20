package com.yqg.core.service.cashloan.repayment.vo;

import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.secure.check.context.SecureCheckUserContext;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;

/**
 * 获取VA的request上下文
 */
@Getter
public class RepaymentAccountContext {

  private String channel;

  private Long couponId;

  //金额
  private BigDecimal amount;

  //分期Id
  private List<String> encodeInstalmentIds;

  private String mobileNumber;

  //JBP订单id
  private List<String> encodeJBPOrderIds;

  private SecureCheckUserContext userContext;

  private RepayStyleVersion repayStyleVersion;

  private RepaymentAccountContext() {
  }

  public static RepaymentAccountContext from(String channel, Long couponId, BigDecimal amount, List<String> encodeInstalmentIds,
      String mobileNumber, List<String> encodeJBPOrderIds, RepayStyleVersion repayStyleVersion, SecureCheckUserContext userContext) {
    RepaymentAccountContext repaymentAccountContext = new RepaymentAccountContext();
    repaymentAccountContext.channel = channel;
    repaymentAccountContext.couponId = couponId;
    repaymentAccountContext.amount = amount;
    repaymentAccountContext.encodeInstalmentIds = encodeInstalmentIds;
    repaymentAccountContext.mobileNumber = mobileNumber;
    repaymentAccountContext.encodeJBPOrderIds = encodeJBPOrderIds;
    repaymentAccountContext.userContext = userContext;
    repaymentAccountContext.repayStyleVersion = repayStyleVersion == null ? RepayStyleVersion.V1 : repayStyleVersion;
    return repaymentAccountContext;
  }
}
