package com.yqg.ec.common.spring.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : haoranzhao
 * @date : 2023-03-29
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EcLoanBankAccountResponse {
  public Long id;
  public Long userId;
  public String businessName;
  public String bankCode;
  public String accountNumber;
  public String name;    //银行名称
  public String availableStatus;    //银行卡状态，见BankAccountAvailableStatus
  public String sdkTypeCode;   //sdkTypeCode
  public String bankAccountType;
  public Long lastTimeUsed;
  public Long timeCreated;
  public String validationId;
  public String matchResult;
  public MatchResultType matchResultType;
}
