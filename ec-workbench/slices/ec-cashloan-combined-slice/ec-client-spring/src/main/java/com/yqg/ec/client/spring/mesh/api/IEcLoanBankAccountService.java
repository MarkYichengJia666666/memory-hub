package com.yqg.ec.client.spring.mesh.api;


import com.yqg.ec.common.spring.request.EcQueryLoanBankAccountByConditionRequest;
import com.yqg.ec.common.spring.response.EcLoanBankAccountResponse;
import com.yqg.ec.common.spring.response.risk.EcLoanBankAccountListResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

/**
 * @author : haoranzhao
 * @date : 2023-03-28
 **/


@EcFeignServiceMeshClient
public interface IEcLoanBankAccountService {
  @GetMapping(path = "/ecInternalApi/loanBankAccount/queryById")
  EcLoanBankAccountResponse queryById(@RequestParam(value = "id") Long id);

  @GetMapping(path = "/ecInternalApi/loanBankAccount/queryByUserIdAndBusiness")
  List<EcLoanBankAccountResponse> queryByUserIdAndBusiness(@RequestParam(value = "userId") Long userId, @RequestParam(value = "businessName") String businessName);

  @GetMapping(path = "/ecInternalApi/loanBankAccount/queryByUserIdJoinLatestOrder")
  EcLoanBankAccountResponse queryByUserIdJoinLatestOrder(@RequestParam(value = "userId") Long userId);

  @GetMapping(path = "/ecInternalApi/loanBankAccount/queryByUserIds")
  List<EcLoanBankAccountResponse> queryByUserIds(@RequestParam(value = "userIds") Collection<Long> userIds);

  @GetMapping(path = "/ecInternalApi/loanBankAccount/queryByAccountNumbersAndBusiness")
  List<EcLoanBankAccountResponse> queryByAccountNumbersAndBusiness(@RequestParam(value = "accountNumbers") List<String> accountNumbers, @RequestParam(value = "businessName") String businessName);

  @GetMapping(path = "/ecInternalApi/loanBankAccount/queryByBankCodesAndAccountNumbers")
  List<EcLoanBankAccountResponse> queryByBankCodesAndAccountNumbers(@RequestParam(value = "bankCodes") List<String> bankCodes, @RequestParam(value = "accountNumbers") List<String> accountNumbers);

  //this interface will be deleted, please use queryLoanBankAccountByConditionV2
  @GetMapping(path = "/ecInternalApi/loanBankAccount/queryLoanBankAccountByCondition")
  EcLoanBankAccountListResponse queryLoanBankAccountByCondition(
      @RequestParam(value = "bankCodes", required = false) List<String> bankCodes,
      @RequestParam(value = "accountNumbers", required = false) List<String> accountNumber,
      @RequestParam(value = "availableStatusCodes", required = false) List<String> availableStatusCodes,
      @RequestParam(value = "userIds", required = false) List<Long> userIds,
      @RequestParam(value = "startTimeCreated", required = false) Long startTimeCreated,
      @RequestParam("endTimeCreated") Long endTimeCreated
  );

  @PostMapping(path = "/ecInternalApi/loanBankAccount/queryLoanBankAccountByConditionV2")
  EcLoanBankAccountListResponse queryLoanBankAccountByConditionV2(@RequestBody @Validated EcQueryLoanBankAccountByConditionRequest request);
}
