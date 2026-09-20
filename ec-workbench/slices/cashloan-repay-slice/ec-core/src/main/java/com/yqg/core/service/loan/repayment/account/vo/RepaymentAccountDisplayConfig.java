package com.yqg.core.service.loan.repayment.account.vo;

import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountType;
import com.yqg.core.service.payment.pm.pmenum.ReceiptPaymentChannel;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.overseas.spring.response.receipt.RepaymentChannelRouteResponse;

public class RepaymentAccountDisplayConfig {
  public PaymentProvider provider;
  public RepaymentAccountType type;
  public Boolean canBeUsed = true;
  public Long minAmount;
  public Long maxAmount;
  public String bankName;
  public String repayNeedTimeDesc;

  public static RepaymentAccountDisplayConfig from(RepaymentChannelRouteResponse response) {
    RepaymentAccountDisplayConfig config = new RepaymentAccountDisplayConfig();
    ReceiptPaymentChannel receiptPaymentChannel = ReceiptPaymentChannel.valueOf(response.getChannel());
    config.provider = PaymentProvider.valueOf(response.getProvider());
    config.type = ReceiptPaymentChannel.from(receiptPaymentChannel);
    config.canBeUsed = response.getCanBeUsed();
    config.minAmount = response.getMinAmount() != null
        ? response.getMinAmount().longValue() : null;
    config.maxAmount = response.getMaxAmount() != null
        ? response.getMaxAmount().longValue() : null;
    return config;
  }

  public static RepaymentAccountDisplayConfig copy(RepaymentAccountDisplayConfig config, PaymentProvider provider) {
    RepaymentAccountDisplayConfig newConfig = new RepaymentAccountDisplayConfig();
    newConfig.provider = provider;
    newConfig.type = config.type;
    newConfig.canBeUsed = config.canBeUsed;
    newConfig.minAmount = config.minAmount;
    newConfig.maxAmount = config.maxAmount;
    newConfig.bankName = config.bankName;
    newConfig.repayNeedTimeDesc = config.repayNeedTimeDesc;
    return newConfig;
  }
}
