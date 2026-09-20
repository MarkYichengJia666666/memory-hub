package com.miyou.controllers.cashloan.response;

import com.yqg.core.service.loan.repayment.account.vo.OVORepaymentInfoVO;

public class OVORepaymentInfoResponse {
  public String mobileNumber;
  public boolean hasAvailableRepayment;

  public static OVORepaymentInfoResponse from(OVORepaymentInfoVO ovoRepaymentInfoVO) {
    OVORepaymentInfoResponse response = new OVORepaymentInfoResponse();
    response.mobileNumber = ovoRepaymentInfoVO.mobileNumber;
    response.hasAvailableRepayment = ovoRepaymentInfoVO.hasAvailableRepayment;
    return response;
  }
}
