package com.yqg.core.service.loan.repayment.account;

import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountType;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.pmenum.RepaymentChannelGroup;

/**
 * 还款渠道分组解析工具
 */
public final class RepaymentChannelGroupResolver {

  private RepaymentChannelGroupResolver() {
  }

  public static String resolveByChannelType(String channelType) {
    if (channelType == null) {
      return RepaymentChannelGroup.VIRTUAL_ACCOUNT.name();
    }
    try {
      DynamicAccountChannel channel = DynamicAccountChannel.valueOf(channelType);
      String opPaymentMethod = channel.opPaymentMethod;
      if (RepaymentChannelGroup.isValid(opPaymentMethod)) {
        return opPaymentMethod;
      }
      return RepaymentChannelGroup.VIRTUAL_ACCOUNT.name();
    } catch (IllegalArgumentException e) {
      return RepaymentChannelGroup.VIRTUAL_ACCOUNT.name();
    }
  }

  public static RepaymentChannelGroup resolveChannelGroup(String channelType) {
    return fromGroupName(resolveByChannelType(channelType));
  }

  public static RepaymentChannelGroup fromGroupName(String groupName) {
    if (RepaymentChannelGroup.isValid(groupName)) {
      return RepaymentChannelGroup.valueOf(groupName);
    }
    return RepaymentChannelGroup.VIRTUAL_ACCOUNT;
  }

  public static RepaymentChannelGroup resolveAccountGroup(String channel, RepaymentAccountType type) {
    if (DynamicAccountChannel.XENDIT_QRIS.name().equals(channel)) {
      return RepaymentChannelGroup.QR_CODE;
    }
    if (type == RepaymentAccountType.DYNAMIC) {
      return RepaymentChannelGroup.E_WALLETS;
    }
    return RepaymentChannelGroup.VIRTUAL_ACCOUNT;
  }
}
