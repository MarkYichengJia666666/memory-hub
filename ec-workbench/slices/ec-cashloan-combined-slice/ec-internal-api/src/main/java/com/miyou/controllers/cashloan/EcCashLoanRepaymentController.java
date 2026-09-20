package com.miyou.controllers.cashloan;

import com.yqg.core.service.cashloan.repay.CashLoanRepaymentService;
import com.yqg.core.service.cashloan.vo.RepaymentVO;
import com.yqg.ec.common.spring.response.EcCashLoanRepaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Validated
@RequestMapping("/ecInternalApi/repayment")
public class EcCashLoanRepaymentController {

  @Autowired
  private CashLoanRepaymentService repaymentService;

  @GetMapping(path = "/getLastSucceedRepaymentOrderAfterTime")
  public EcCashLoanRepaymentResponse getLastSucceedRepaymentOrderAfterTime(@RequestParam("userId") Long userId,
      @RequestParam("startTime") Long startTime) {
    EcCashLoanRepaymentResponse response = new EcCashLoanRepaymentResponse();
    RepaymentVO repayment = repaymentService.getLastSucceedRepaymentOrderAfterTime(userId, startTime);
    if (repayment == null) {
      return response;
    }
    response.setTimeRepaid(repayment.getTimeRepaid());
    return response;
  }
}
