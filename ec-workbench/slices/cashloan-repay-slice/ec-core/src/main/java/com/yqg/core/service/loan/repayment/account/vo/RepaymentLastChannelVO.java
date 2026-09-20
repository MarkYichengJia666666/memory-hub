package com.yqg.core.service.loan.repayment.account.vo;

import com.yqg.core.service.payment.pm.PaymentMethod;
import lombok.Data;

@Data
public class RepaymentLastChannelVO {
  private String channelName;
  private PaymentMethod paymentMethod;
  private Integer userCount;

  public static RepaymentLastChannelVO from(String channelName, PaymentMethod paymentMethod, Integer userCount) {
    RepaymentLastChannelVO vo = new RepaymentLastChannelVO();
    vo.channelName = channelName;
    vo.paymentMethod = paymentMethod;
    vo.userCount = userCount;
    return vo;
  }
}
