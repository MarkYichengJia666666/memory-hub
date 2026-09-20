package com.miyou.controllers.cashloan.request;

import com.yqg.core.model.sql.mobile.enums.VerificationPurposeType;
import org.hibernate.validator.constraints.NotBlank;

public class CashLoanGetOVORepaymentAccountRequest extends CashLoanGetRepaymentAccountRequest {
  @NotBlank(message = "mobileNumber不能为空")
  public String mobileNumber;
  public String verificationCode;
  public VerificationPurposeType verificationPurpose;
}
