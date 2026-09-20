package com.yqg.ec.common.spring.request;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
public class EcAddLoanBankAccountRequest {
  @NotNull
  public Long userId;
  @NotNull
  public String bankName;
  @NotNull
  public String bankCode;
  @NotNull
  public String bankAccountNumber;
  @NotNull
  public String userName;
}
