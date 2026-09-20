package com.yqg.ec.common.spring.response.basedata;

import lombok.Data;

/**
 * @Author: horn lee
 * @Created: 2024-03-26 15:30
 */
@Data
public class LoanBankAccountEntity {
  public Long bankAccountId;

  public Long userId;

  public String bankCode;
  public String bankType;
  public String bankAccountType;
  public String bankName;
  public String bankLogoUrl;
  public String maskedAccountNumber;
  public String accountNumber;
  public String name;
  public Long timeFirstAdded;
  public String validationId;
}
