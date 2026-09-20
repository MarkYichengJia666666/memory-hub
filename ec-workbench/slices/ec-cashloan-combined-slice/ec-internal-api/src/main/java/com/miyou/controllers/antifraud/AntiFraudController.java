package com.miyou.controllers.antifraud;

import com.yqg.core.service.antifraud.AntiFraudOverdueService;
import com.yqg.core.service.antifraud.AntiFraudService;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.spring.request.antifraud.AntiFraudOverdueCaseCreateRequest;
import com.yqg.ec.common.spring.request.capital.CapitalLenderFeeRequest;
import com.yqg.ec.common.spring.response.capital.CapitalLenderFeeResponse;
import com.yqg.ec.common.spring.response.capital.enums.CapitalFeeSaveResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @author chenxianrui
 * @date 2025/6/13
 */
@Slf4j
@RestController
@RequestMapping("/ecInternalApi/antiFraud")
public class AntiFraudController {
  @Autowired
  private AntiFraudService antiFraudOverdueService;

  @PostMapping(path = "/overdue/caseCreate")
  public Boolean overdueCaseCreate(@RequestBody @Valid AntiFraudOverdueCaseCreateRequest request) {
    return antiFraudOverdueService.overdueCaseCreate(request.getUserId(), request.getCollectionCaseId(), request.getCollectionCaseTimeCreated());
  }
}
