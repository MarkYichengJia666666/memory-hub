package com.yqg.ec.common.spring.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : haoranzhao
 * @date : 2023-03-28
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EcBindCardRequestInfo {
  public Long id;
  public Long userId;
  public String accountNumber;    //银行卡号
  public String bankCode;    //银行编号
  public String sdkTypeCode;    //sdkTypeCode
}
