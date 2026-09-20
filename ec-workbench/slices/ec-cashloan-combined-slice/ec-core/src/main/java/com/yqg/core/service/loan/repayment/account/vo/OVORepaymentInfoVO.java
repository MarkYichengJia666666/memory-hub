package com.yqg.core.service.loan.repayment.account.vo;

public class OVORepaymentInfoVO {
  public String mobileNumber;
  public boolean hasAvailableRepayment;

  public static OVORepaymentInfoVO from(String mobileNumber, boolean hasAvailableRepayment) {
    OVORepaymentInfoVO ovoRepaymentInfoVO = new OVORepaymentInfoVO();
    ovoRepaymentInfoVO.mobileNumber = mobileNumber;
    ovoRepaymentInfoVO.hasAvailableRepayment = hasAvailableRepayment;
    return ovoRepaymentInfoVO;
  }
}
