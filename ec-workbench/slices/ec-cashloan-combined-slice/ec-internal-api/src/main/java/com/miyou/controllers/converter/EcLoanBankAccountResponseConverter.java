package com.miyou.controllers.converter;

import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.ec.common.spring.response.risk.EcLoanBankAccountListResponse;
import com.yqg.ec.common.spring.response.risk.EcLoanBankAccountResponse;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EcLoanBankAccountResponseConverter {
  public static List<EcLoanBankAccountListResponse.EcLoanBankAccount> convert(List<LoanBankAccountVO> loanBankAccountVOs) {
    return loanBankAccountVOs
        .stream()
        .map(EcLoanBankAccountResponseConverter::convertLoanBankAccount)
        .collect(Collectors.toList());
  }

  private static EcLoanBankAccountListResponse.EcLoanBankAccount convertLoanBankAccount(LoanBankAccountVO loanBankAccountVO) {
    EcLoanBankAccountListResponse.EcLoanBankAccount loanBankAccount = new EcLoanBankAccountListResponse.EcLoanBankAccount();
    loanBankAccount.id = loanBankAccountVO.getId();
    loanBankAccount.userId = loanBankAccountVO.getUserId();
    loanBankAccount.businessName = loanBankAccountVO.businessName;
    loanBankAccount.bankCode = loanBankAccountVO.bankCode;
    loanBankAccount.bankAccountType = Objects.nonNull(loanBankAccountVO.bankAccountType) ? loanBankAccountVO.bankAccountType.name() : null;
    loanBankAccount.accountNumber = loanBankAccountVO.accountNumber;
    loanBankAccount.name = loanBankAccountVO.name;
    loanBankAccount.availableStatus = loanBankAccountVO.bankAccountAvailableStatus.charCode;
    loanBankAccount.lastTimeUsed = loanBankAccountVO.lastTimeUsed;
    loanBankAccount.timeCreated = loanBankAccountVO.timeCreated;
    loanBankAccount.timeUpdated = loanBankAccountVO.timeUpdated;
    loanBankAccount.sdkType = loanBankAccountVO.sdk;
    loanBankAccount.validationId = loanBankAccountVO.validationId;
    return loanBankAccount;
  }

  public static EcLoanBankAccountResponse convertEcLoanBankAccountResponse(LoanBankAccountVO loanBankAccountVO) {
    EcLoanBankAccountResponse loanBankAccount = new EcLoanBankAccountResponse();
    loanBankAccount.id = loanBankAccountVO.getId();
    loanBankAccount.userId = loanBankAccountVO.getUserId();
    loanBankAccount.businessName = loanBankAccountVO.businessName;
    loanBankAccount.bankCode = loanBankAccountVO.bankCode;
    loanBankAccount.bankAccountType = Objects.nonNull(loanBankAccountVO.bankAccountType) ? loanBankAccountVO.bankAccountType.name() : null;
    loanBankAccount.accountNumber = loanBankAccountVO.accountNumber;
    loanBankAccount.name = loanBankAccountVO.name;
    loanBankAccount.availableStatus = loanBankAccountVO.bankAccountAvailableStatus.charCode;
    loanBankAccount.lastTimeUsed = loanBankAccountVO.lastTimeUsed;
    loanBankAccount.timeCreated = loanBankAccountVO.timeCreated;
    loanBankAccount.timeUpdated = loanBankAccountVO.timeUpdated;
    loanBankAccount.sdkType = loanBankAccountVO.sdk;
    loanBankAccount.validationId = loanBankAccountVO.validationId;
    return loanBankAccount;
  }
}
