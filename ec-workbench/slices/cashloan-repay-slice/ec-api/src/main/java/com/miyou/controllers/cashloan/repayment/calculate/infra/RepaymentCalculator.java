package com.miyou.controllers.cashloan.repayment.calculate.infra;


import com.miyou.controllers.cashloan.repayment.calculate.RepaymentCalculateRequest;
import com.miyou.controllers.cashloan.repayment.calculate.RepaymentCalculateResponse;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;

public interface RepaymentCalculator {

  RepaymentCalculateScene getScene();

  /**
   * 还款试算
   */
  void calculateRepaymentInfo(
      RepaymentCalculateRequest request,
      LoanApiViewerContext viewerContext,
      RepaymentCalculateResponse.RepaymentCalculateResponseBuilder builder
  );
}
