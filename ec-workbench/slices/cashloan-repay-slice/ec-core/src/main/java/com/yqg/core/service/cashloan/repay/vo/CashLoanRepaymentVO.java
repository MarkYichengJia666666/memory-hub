package com.yqg.core.service.cashloan.repay.vo;

import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
/**
 * @author: ListenYoung
 * @date: Created on 20:14 2019/8/8
 * @modified By:
 */
@Data
@Accessors(fluent = true, chain = true)
public class CashLoanRepaymentVO {
  public Long userId;
  public Long orderId;
  public PaymentBusinessName businessName;
  public Long instalmentId;
  public PaymentAccount paymentAccount;
  public String transId;
  public CurrencyAmount amount;
  public Map<String, String> extraData;
  public PaymentProvider provider;
  public DynamicAccountChannel channel;
  public Long expiredTime;
  public CashLoanRepaymentVO() {
  }

  public CashLoanRepaymentVO(
      Long userId,
      Long orderId,
      PaymentBusinessName businessName,
      Long instalmentId,
      PaymentAccount paymentAccount,
      String transId,
      CurrencyAmount amount,
      Map<String, String> extraData,
      PaymentProvider provider,
      DynamicAccountChannel channel,
      Long expiredTime) {
    this.userId = userId;
    this.orderId = orderId;
    this.businessName = businessName;
    this.instalmentId = instalmentId;
    this.paymentAccount = paymentAccount;
    this.transId = transId;
    this.amount = amount;
    this.extraData = extraData;
    this.provider = provider;
    this.channel = channel;
    this.expiredTime = expiredTime;
  }

  public static CashLoanRepaymentVO deepClone(
      CashLoanRepaymentVO repaymentVO,
      String transId,
      DynamicAccountChannel channel,
      PaymentProvider provider,
      Long expiryTime
      ) {
    String json = JsonUtils.toString(repaymentVO);
    return JsonUtils.fromOrException(json, CashLoanRepaymentVO.class)
        .transId(transId)
        .channel(channel)
        .provider(provider)
        .expiredTime(expiryTime);
  }

  //用于OVO第三方，获取用户还款账号时用
  public String fetchOVOAccount() {
    return extraData.get("account");
  }
}