package com.yqg.core.service.cashloan.repayment.vo;

import com.yqg.translation.client.utils.TT;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by xiuqichenyang on 17/7/26.
 */
@Data
public class VirtualAccountResVO {
  // 兼容channel
  private String bankType;
  private String virtualAccountNumber;
  // 兼容channel
  private TT bankName;
  private String logoUrl;
  private BigDecimal channelLimitAmount;

  public static VirtualAccountResVO from(String accountNumber, String credentialType, String desc, String logoUrl) {
    VirtualAccountResVO response = new VirtualAccountResVO();
    response.virtualAccountNumber = accountNumber;
    response.bankType = credentialType;
    response.bankName = TT.gen(desc);
    response.logoUrl = logoUrl;
    return response;
  }
}
