package com.miyou.controllers.cashloan;

import com.miyou.controllers.converter.EcLoanBankAccountResponseConverter;
import com.yqg.core.aop.StatQueryRequest;
import com.yqg.core.model.generated.tables.records.LoanBankAccountRecord;
import com.yqg.core.model.sql.bankaccount.LoanBankAccountModel;
import com.yqg.core.model.sql.bankaccount.LoanBankAccountSearchCondition;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.cashloan.vo.enums.ValidationAccountStatus;
import com.yqg.core.service.cashloan.vo.loanbankaccount.AccountInfoWithMatchResult;
import com.yqg.core.service.cashloan.vo.loanbankaccount.AccountInfoWithName;
import com.yqg.core.service.cashloan.vo.loanbankaccount.ValidationResultV3;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.core.service.payment.pm.BankAccountPaymentMethod;
import com.yqg.core.service.payment.utils.BankAccountValidationUtil;
import com.yqg.core.util.NameSimilarityUtil;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.spring.request.EcQueryLoanBankAccountByConditionRequest;
import com.yqg.ec.common.spring.response.EcLoanBankAccountResponse;
import com.yqg.ec.common.spring.response.MatchResultType;
import com.yqg.ec.common.spring.response.risk.EcLoanBankAccountListResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author : haoranzhao
 * @date : 2023-03-29
 **/

@Slf4j
@RestController
@RequestMapping("/ecInternalApi/loanBankAccount")
public class LoanBankAccountController {
  //TODO(LTB,T000000) 上下层隔离
  @Autowired
  private LoanBankAccountModel loanBankAccountModel;
  @Autowired
  private BankAccountPaymentMethod bankAccountPaymentMethod;
  @Autowired
  private BankAccountValidationUtil bankAccountValidationUtil;
  @Autowired
  private LoanBankAccountService loanBankAccountService;

  @StatQueryRequest
  @GetMapping(path = "/queryById")
  public EcLoanBankAccountResponse queryById(@RequestParam(value = "id") Long id) {
    LoanBankAccountRecord record = loanBankAccountModel.findById(id);
    EcLoanBankAccountResponse response = convertToResponseFromRecord(record);
    try {
      //历史的老卡可能没有验卡，直接返回null
      getMatchInfoV3(record, response);
      return response;
    } catch (Exception e) {
      log.warn("queryById exception.", e);
      return response;
    }

  }

  private void getMatchInfoV3(LoanBankAccountRecord record, EcLoanBankAccountResponse response) {
    ValidationResultV3 validationResultV3 = bankAccountPaymentMethod.queryValidationResultV3(
        record.getUserId(),
        BankType.valueOf(record.getBankCode()),
        record.getAccountNumber(),
        record.getValidationId(),
        record.getName(),
        SDKType.fromCode(record.getSdkType()),
        record.getId(),
        false);
    if (ValidationAccountStatus.ACTIVE.equals(validationResultV3.accountStatus)) {
      response.setMatchResultType(validationResultV3.type);
      String matchResult = getMatchResultByResultTypeAndValidResultV3(validationResultV3.type, record, validationResultV3);
      response.setMatchResult(matchResult);
    }
  }

  private String getMatchResultByResultTypeAndValidResultV3(MatchResultType resultType, LoanBankAccountRecord record, ValidationResultV3 validationResultV3) {
    switch (resultType) {
      case MATCH_RESULT:
        AccountInfoWithMatchResult accountInfoWithMatchResult = (AccountInfoWithMatchResult) validationResultV3.accountInfo;
        return accountInfoWithMatchResult.result.name();
      case FUZZY_NAME:
        boolean isNameMatch = bankAccountValidationUtil.isNameMatch(BankType.valueOf(record.getBankCode()), ((AccountInfoWithName) validationResultV3.accountInfo).name, record.getName());
        return isNameMatch ? AccountInfoWithMatchResult.Result.MATCH.name() : AccountInfoWithMatchResult.Result.NOT_MATCH.name();
      case FULL_NAME:
        return String.valueOf(NameSimilarityUtil.getSimilarScore(record.getName(), ((AccountInfoWithName) validationResultV3.accountInfo).name));
      default:
        return null;
    }
  }

  @StatQueryRequest
  @GetMapping(path = "/queryByUserIdAndBusiness")
  public List<EcLoanBankAccountResponse> queryByUserIdAndBusiness(@RequestParam(value = "userId") Long userId, @RequestParam(value = "businessName") String businessName) {
    List<LoanBankAccountRecord> records = loanBankAccountModel.fetchByUserIdAndBusinessName(userId, PaymentBusinessName.fromCodeOrThrow(businessName));
    return records.stream().map(this::convertToResponseFromRecord).collect(Collectors.toList());
  }

  @StatQueryRequest
  @GetMapping(path = "/queryByUserIdJoinLatestOrder")
  public EcLoanBankAccountResponse queryByUserIdJoinLatestOrder(@RequestParam(value = "userId") Long userId) {
    LoanBankAccountRecord record = loanBankAccountModel.findByUserIdJoinLatestOrder(userId);
    return convertToResponseFromRecord(record);
  }

  @StatQueryRequest
  @GetMapping(path = "/queryByUserIds")
  public List<EcLoanBankAccountResponse> queryByUserIds(@RequestParam(value = "userIds") Collection<Long> userIds) {
    List<LoanBankAccountRecord> records = loanBankAccountModel.findByUserIds(userIds);
    return records.stream().map(this::convertToResponseFromRecord).collect(Collectors.toList());
  }

  @StatQueryRequest
  @GetMapping(path = "/queryByAccountNumbersAndBusiness")
  public List<EcLoanBankAccountResponse> queryByAccountNumbersAndBusiness(@RequestParam(value = "accountNumbers") List<String> accountNumbers, @RequestParam(value = "businessName") String businessName) {
    List<LoanBankAccountRecord> records = loanBankAccountModel.fetchByAccountNumbersAndBusiness(accountNumbers, PaymentBusinessName.fromCodeOrThrow(businessName));
    return records.stream().map(this::convertToResponseFromRecord).collect(Collectors.toList());
  }

  @StatQueryRequest
  @GetMapping(path = "/queryByBankCodesAndAccountNumbers")
  public List<EcLoanBankAccountResponse> queryByBankCodesAndAccountNumbers(@RequestParam(value = "bankCodes") List<String> bankCodes, @RequestParam(value = "accountNumbers") List<String> accountNumbers) {
    List<LoanBankAccountRecord> records = loanBankAccountModel.queryByBankCodesAndAccountNumbers(bankCodes, accountNumbers);
    return records.stream().map(this::convertToResponseFromRecord).collect(Collectors.toList());
  }

  private EcLoanBankAccountResponse convertToResponseFromRecord(LoanBankAccountRecord record) {
    if (record == null) {
      return null;
    }
    EcLoanBankAccountResponse response = new EcLoanBankAccountResponse();
    response.id = record.getId();
    response.userId = record.getUserId();
    response.businessName = record.getBusinessName();
    response.bankCode = record.getBankCode();
    response.accountNumber = record.getAccountNumber();
    response.name = record.getName();
    response.availableStatus = record.getAvailableStatus();
    response.sdkTypeCode = record.getSdkType();
    response.lastTimeUsed = record.getLastTimeUsed();
    response.timeCreated = record.getTimeCreated();
    response.validationId = record.getValidationId();
    response.bankAccountType = record.getBankAccountType();
    return response;
  }

  /**
   * @param endTimeCreated if startTimeCreated is not null, TIME_CREATED between startTimeCreated and endTimeCreated
   * @return
   */
  @GetMapping(path = "/queryLoanBankAccountByCondition")
  public EcLoanBankAccountListResponse queryLoanBankAccountByCondition(
      @RequestParam(value = "bankCodes", required = false) List<String> bankCodes,
      @RequestParam(value = "accountNumbers", required = false) List<String> accountNumber,
      @RequestParam(value = "availableStatusCodes", required = false) List<String> availableStatusCodes,
      @RequestParam(value = "userIds", required = false) List<Long> userIds,
      @RequestParam(value = "startTimeCreated", required = false) Long startTimeCreated,
      @RequestParam("endTimeCreated") Long endTimeCreated
  ) {
    LoanBankAccountSearchCondition condition = new LoanBankAccountSearchCondition();
    condition.bankCodes = bankCodes;
    condition.accountNumbers = accountNumber;
    condition.availableStatusCodes = availableStatusCodes;
    condition.userIds = userIds;
    condition.startTimeCreated = startTimeCreated;
    condition.endTimeCreated = endTimeCreated;
    List<LoanBankAccountVO> loanBankAccountVOList = loanBankAccountService.getByCondition(condition);

    if (Objects.isNull(loanBankAccountVOList)) {
      return new EcLoanBankAccountListResponse();
    }

    EcLoanBankAccountListResponse response = new EcLoanBankAccountListResponse();
    response.loanBankAccountList = EcLoanBankAccountResponseConverter.convert(loanBankAccountVOList);
    return response;
  }

  @PostMapping(path = "/queryLoanBankAccountByConditionV2")
  public EcLoanBankAccountListResponse queryLoanBankAccountByConditionV2(@RequestBody @Validated EcQueryLoanBankAccountByConditionRequest request) {
    if (CollectionUtils.isEmpty(request.accountNumber) && CollectionUtils.isEmpty(request.userIds)) {
      throw EcException.error("accountNumber and userIds must not be null at the same time.");
    }

    LoanBankAccountSearchCondition condition = new LoanBankAccountSearchCondition();
    condition.bankCodes = request.bankCodes;
    condition.accountNumbers = request.accountNumber;
    condition.availableStatusCodes = request.availableStatusCodes;
    condition.userIds = request.userIds;
    condition.startTimeCreated = request.startTimeCreated;
    condition.endTimeCreated = request.endTimeCreated;
    List<LoanBankAccountVO> loanBankAccountVOList = loanBankAccountService.getByCondition(condition);

    if (Objects.isNull(loanBankAccountVOList)) {
      return new EcLoanBankAccountListResponse();
    }

    EcLoanBankAccountListResponse response = new EcLoanBankAccountListResponse();
    response.loanBankAccountList = EcLoanBankAccountResponseConverter.convert(loanBankAccountVOList);
    return response;
  }
}
