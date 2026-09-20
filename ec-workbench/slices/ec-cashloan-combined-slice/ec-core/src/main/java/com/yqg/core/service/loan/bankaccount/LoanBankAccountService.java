package com.yqg.core.service.loan.bankaccount;

import static com.yqg.core.model.sql.bankaccount.enums.BankAccountAvailableStatus.CHECK_STATUS_LIST;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yqg.apichannel.dto.response.commonInternal.ApiChannelUserResponse;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.LoanBankAccountRecord;
import com.yqg.core.model.loader.AddBankAccountNotMatchTimesLoader;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.apichannel.enums.ApiChannel;
import com.yqg.core.model.sql.bankaccount.LoanBankAccountModel;
import com.yqg.core.model.sql.bankaccount.LoanBankAccountSearchCondition;
import com.yqg.core.model.sql.bankaccount.enums.BankAccountAvailableStatus;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.loan.account.UserCheckHistoryModel;
import com.yqg.core.model.sql.loan.account.enums.BankAccountType;
import com.yqg.core.model.sql.loan.account.enums.CheckType;
import com.yqg.core.model.sql.loan.additionalinfo.LoanUserAdditionalInfoModel;
import com.yqg.core.model.sql.loan.additionalinfo.enums.LoanUserAdditionalInfoType;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.AbstractExpClient;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.abtest.vo.ABTestUserIdRequestVO;
import com.yqg.core.service.apichannel.ApiChannelUserService;
import com.yqg.core.service.apichannel.config.ApiChannelConfig;
import com.yqg.core.service.bankconfig.BankConfigService;
import com.yqg.core.service.bankconfig.vo.BankAttributeVO;
import com.yqg.core.service.bankconfig.vo.BankConfigVO;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.auth.AuthService;
import com.yqg.core.service.cashloan.auth.BindCardDelayService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.vo.enums.AddBankAccountNotMatchStrategy;
import com.yqg.core.service.cashloan.vo.enums.FinalValidationBankAccountStatus;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.cashloan.vo.enums.ValidationCardStatus;
import com.yqg.core.service.cashloan.vo.enums.ValidationVersion;
import com.yqg.core.service.cashloan.vo.loanbankaccount.ValidationResult;
import com.yqg.core.service.cashloan.vo.loanbankaccount.ValidationResultV3;
import com.yqg.core.service.loan.account.AuthCheckTimeCostMonitorService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.vo.CheckAuthConfigVO;
import com.yqg.core.service.loan.bankaccount.enums.VerifyEWalletAccountMethod;
import com.yqg.core.service.loan.bankaccount.monitor.BankCardUniqueKeyCheckMonitorService;
import com.yqg.core.service.loan.vo.SimpleLoanAccountVO;
import com.yqg.core.service.loan.vo.UserInfoVO;
import com.yqg.core.service.loan.vo.bankaccount.BindCardResultVO;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.core.service.payment.ICredential;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.PaymentBusinessNameMapper;
import com.yqg.core.service.payment.PaymentCredential;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.payment.UserPaymentCredential;
import com.yqg.core.service.payment.pm.BankAccountPaymentMethod;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.utils.BankAccountValidationUtil;
import com.yqg.core.service.secure.UserSecureConfig;
import com.yqg.core.service.tool.IdGeneratorService;
import com.yqg.core.service.user.UserEventService;
import com.yqg.core.service.user.UserService;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.core.util.thirdparty.loan.LoanCreditService;
import com.yqg.core.util.thirdparty.loan.vo.TongDunNodeTriggerParam;
import com.yqg.core.util.validator.GlobalBankAccountNumValidator;
import com.yqg.ec.common.configuration.ISiteVars;
import com.yqg.ec.common.constant.ExperimentKeyConstants;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.ec.common.utils.SysEnvironment;
import com.yqg.overseas.ads.client.api.IAdsAppsflyerService;
import com.yqg.overseas.ads.client.common.response.AdsResponse;
import com.yqg.overseas.ads.client.common.vo.appsflyer.AppsflyerResultVO;
import com.yqg.translation.client.utils.TT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by xiahonggao on 2/16/16.
 */
@Service
@Slf4j
public class LoanBankAccountService {
  @Autowired
  private LoanCreditService loanCreditService;
  @Autowired
  private LoanBankAccountModel loanBankAccountModel;
  @Autowired
  private BindCardLocker bindCardLocker;
  @Autowired
  private BankConfigService bankConfigService;
  @Autowired
  private BankAccountPaymentMethod bankAccountPaymentMethod;
  @Autowired
  private ThreadTransactionalModel transactionalModel;
  @Autowired
  private IdGeneratorService idGeneratorService;
  @Autowired
  private ISiteVars ecSiteVars;
  @Autowired
  private BindCardRequestService bindCardRequestService;
  @Autowired
  private LoanBankAccountMonitorService bankAccountMonitorService;
  @Autowired
  private LoanBankConfig bankConfig;
  @Autowired
  private UserService userService;
  @Autowired
  private ApiChannelUserService apiChannelUserService;
  @Autowired
  private ApiChannelConfig apiChannelConfig;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private AddBankAccountNotMatchTimesLoader addBankAccountNotMatchTimesLoader;
  @Autowired
  private GlobalBankAccountNumValidator globalBankAccountNumValidator;
  @Autowired
  private ExpFacade expFacade;
  @Autowired
  private LoanBankConfig loanBankConfig;
  @Autowired
  private BankAccountValidationUtil bankAccountValidationUtil;
  @Autowired
  private UserSecureConfig userSecureConfig;
  @Autowired
  private LoanUserAdditionalInfoModel loanUserAdditionalInfoModel;
  @Autowired
  private AuthCheckTimeCostMonitorService authCheckTimeCostMonitorService;
  @Autowired
  private UserCheckHistoryModel userCheckHistoryModel;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private UserEventService userEventService;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private IAdsAppsflyerService adsAppsflyerService;
  @Autowired
  private BindCardDelayService bindCardDelayService;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private AuthService authService;
  @Autowired
  private BankCardUniqueKeyCheckMonitorService bankCardUniqueKeyCheckMonitorService;
  @Autowired
  private BindCardNameSimilarityCheckService bindCardNameSimilarityCheckService;

  public List<LoanBankAccountVO> getAvailableBankAccounts(Long userId, SDKType sdkType) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    List<LoanBankAccountRecord> bankAccountRecords = loanBankAccountModel.findAvailableByUserIdAndBusiness(userId, businessName);
    List<LoanBankAccountVO> vos = new LinkedList<>();
    bankAccountRecords.forEach(record -> vos.add(loanBankAccountModel.findVOById(record.getId())));
    return vos;
  }

  public List<LoanBankAccountVO> getBankAccounts(Long userId, SDKType sdkType) {
    return getBankAccounts(userId, sdkType, null, null);
  }

  public boolean hasAvailableBankAccount(Long userId, SDKType sdkType) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    return loanBankAccountModel.countAvailableByUserIdAndBusiness(userId, businessName) > 0;
  }

  public List<LoanBankAccountVO> getBankAccounts(Long userId, SDKType sdkType, Long timeStarted, Long timeEnded) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    List<LoanBankAccountRecord> bankAccountRecords = loanBankAccountModel.findByUserIdAndBusiness(userId, businessName, timeStarted, timeEnded);
    List<LoanBankAccountVO> bankAccountVOs = new ArrayList<>();
    for (LoanBankAccountRecord bankAccountRecord : bankAccountRecords) {
      LoanBankAccountVO bankAccountVO = loanBankAccountModel.findVOById(bankAccountRecord.getId());
      bankAccountVOs.add(bankAccountVO);
    }
    return bankAccountVOs;
  }

  public LoanBankAccountVO findVOById(Long id) {
    return loanBankAccountModel.findVOById(id);
  }

  public LoanBankAccountVO addBankAccountOrWarning(Long userId, SDKType sdkType, BankType bankType, String accountNumber, String name, Long build) {
    return addBankAccountOrWarningV2(userId, sdkType, bankType, accountNumber, name, null, build);
  }

  public LoanBankAccountVO addBankAccountOrWarningV2(Long userId, SourceType sourceType, SDKType sdkType, BankType bankType,
      String accountNumber, String name, BankAccountType bankAccountType, Long build) {
    BindCardResultVO bindCardResultVO = addBankAccount(userId, sourceType, sdkType, bankType, accountNumber, name, bankAccountType, build, null);
    BankAccountAvailableStatus status = bindCardResultVO.loanBankAccountVO.bankAccountAvailableStatus;
    //只有UNAVAILABLE状态需要抛错
    if (BankAccountAvailableStatus.INACTIVE_STATUS_LIST.contains(status) && bindCardResultVO.errRemind != null) {
      throw EcException.warn(EcExceptionType.LOAN_BANK_ACCOUNT_UNAVAILABLE, bindCardResultVO.errRemind, "bind card failed, userId is {}, accountNumber is {}", userId, accountNumber);
    }
    return bindCardResultVO.loanBankAccountVO;
  }

  public LoanBankAccountVO addBankAccountOrWarningV2(Long userId, SDKType sdkType, BankType bankType, String accountNumber, String name,
      BankAccountType bankAccountType, Long build) {
    return addBankAccountOrWarningV2(userId, null, sdkType, bankType, accountNumber, name, bankAccountType, build);
  }

  public BindCardResultVO queryBankValidationResult(LoanBankAccountVO bankAccountVO, SDKType sdkType) {
    if (bankAccountVO.isCredentialAvailable()) {
      return BindCardResultVO.from(bankAccountVO);
    }

    ValidationResultV3 validationResultV3 = bankAccountPaymentMethod.queryValidationResultV3(
        bankAccountVO.userId,
        bankAccountVO.bankType,
        bankAccountVO.accountNumber,
        bankAccountVO.validationId,
        bankAccountVO.name,
        bankAccountVO.sdk,
        bankAccountVO.bankAccountId,
        true
    );
    return updateIdnLoanBankStatusV3(
        bankAccountVO.userId,
        bankAccountVO.bankAccountId,
        validationResultV3,
        bankAccountVO.name,
        sdkType,
        bankAccountVO.bankType,
        null,
        ValidationVersion.V1);
  }

  public BindCardResultVO queryIdnValidationResultV2(LoanBankAccountVO bankAccountVO, SDKType sdkType) {
    if (bankAccountVO.isCredentialAvailable()) {
      return BindCardResultVO.from(bankAccountVO);
    }
    String nationalMobileNumber = getNationalMobileNumberForBindCard(bankAccountVO.userId, sdkType);

    ValidationResultV3 validationResultV3 = bankAccountPaymentMethod.queryValidationResultV3(
        bankAccountVO.userId,
        bankAccountVO.bankType,
        bankAccountVO.accountNumber,
        bankAccountVO.validationId,
        bankAccountVO.name,
        bankAccountVO.sdk,
        bankAccountVO.bankAccountId,
        true
    );
    return updateIdnLoanBankStatusV3(
        bankAccountVO.userId,
        bankAccountVO.bankAccountId,
        validationResultV3,
        bankAccountVO.name,
        sdkType,
        bankAccountVO.bankType,
        nationalMobileNumber,
        ValidationVersion.V2);
  }

  public BindCardResultVO addBankAccount(Long userId,
      SourceType sourceType,
      SDKType sdkType,
      BankType bankType,
      String accountNumber,
      String name,
      BankAccountType bankAccountType,
      Long build,
      TongDunNodeTriggerParam tongDunParam) {
    checkParam(sdkType, name);
    bindCardNameSimilarityCheckService.check(userId, sdkType, accountNumber, name, build,
        BindCardNameSimilarityCheckService.BindCardEntry.NATIVE_REBIND);
    bindCardRequestService.storeRequestInfo(userId, accountNumber, bankType, sdkType);
    // 绑卡请求落库后触发同盾（BIND_CARD_TG，请求即触发、不等验卡/返回）；内部异步、失败兜底在 LoanCreditService，不阻断绑卡主流程
    loanCreditService.triggerBindCard(tongDunParam);
    String nationalMobileNumber = getNationalMobileNumberForBindCard(userId, sourceType, sdkType);
    log.info("addBankAccount, userId: {}, sourceType: {}, sdkType: {}, nationalMobileNumber: {}", userId, sourceType, sdkType,
        nationalMobileNumber);
    BindCardResultVO resultVO = preCheckOrGetErrResultVO(userId, sdkType, bankType, accountNumber, name, bankAccountType, nationalMobileNumber);
    if (resultVO != null) {
      return resultVO;
    }
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);

    if (sdkType.isIdnLoanSDKType()) {
      checkBankAccountNumberBindUserCount(userId, sdkType, bankType, accountNumber, build);
    }
    return bindCardLocker.lockAndRunResult(accountNumber, () -> {
      LoanBankAccountRecord record = getExistLoanBankAccountRecord(userId, sdkType, bankType, accountNumber, businessName);
      if (record != null) {
        BankAccountAvailableStatus status = BankAccountAvailableStatus.fromCodeOrThrow(record.getAvailableStatus());
        if (!BankAccountAvailableStatus.INACTIVE_STATUS_LIST.contains(status)) {
          return BindCardResultVO.from(LoanBankAccountVO.from(record));
        } else {
          record = loanBankAccountModel.updateName(record, name);
        }
      }

      //不需要验卡
      if (bankConfig.noNeedValidation(sdkType) || bankConfig.noNeedValidationByUserId(userId)) {
        record = dealLoanBankAccountWithNoValidate(
            userId,
            sdkType,
            bankType,
            accountNumber,
            name,
            bankAccountType,
            record,
            build
        );
        return BindCardResultVO.from(LoanBankAccountVO.from(record));
      }

      // 进行验卡
      String validationId = idGeneratorService.genId(IdGeneratorService.Type.BANK_CARD_NV);

      if (record == null) {
        record = initLoanBankAccountNewLogic(
            userId,
            sdkType,
            bankType,
            bankAccountType,
            accountNumber,
            name,
            BankAccountAvailableStatus.INIT,
            validationId
        );
      } else {
        loanBankAccountModel.updateValidationId(record, bankType, validationId);
      }

      return getIdnLoanValidResultAndUpdateStatusV3(userId, sdkType, bankType, accountNumber, name, record, validationId, null, ValidationVersion.V1);
    });
  }

  public BindCardResultVO addBankAccount(Long userId,
      SDKType sdkType,
      BankType bankType,
      String accountNumber,
      String name,
      BankAccountType bankAccountType,
      Long build,
      TongDunNodeTriggerParam tongDunParam) {
    return addBankAccount(userId, null, sdkType, bankType, accountNumber, name, bankAccountType, build, tongDunParam);
  }

  public BindCardResultVO addBankAccount(Long userId,
      SDKType sdkType,
      BankType bankType,
      String accountNumber,
      String name,
      BankAccountType bankAccountType,
      Long build) {
    return addBankAccount(userId, sdkType, bankType, accountNumber, name, bankAccountType, build, null);
  }

  public BindCardResultVO addIdnBankAccountV2(Long userId,
                                              SDKType sdkType,
                                              BankType bankType,
                                              String accountNumber,
                                              String name,
                                              Long build) {
    return addIdnBankAccountV2(userId, sdkType, bankType, accountNumber, name, build, null);
  }

  public BindCardResultVO addIdnBankAccountV2(Long userId,
                                              SDKType sdkType,
                                              BankType bankType,
                                              String accountNumber,
                                              String name,
                                              Long build,
                                              TongDunNodeTriggerParam tongDunParam) {
    checkParam(sdkType, name);
    bindCardNameSimilarityCheckService.check(userId, sdkType, accountNumber, name, build,
        BindCardNameSimilarityCheckService.BindCardEntry.H5_AUTH);
    bindCardRequestService.storeRequestInfo(userId, accountNumber, bankType, sdkType);
    // 绑卡请求落库后触发同盾（BIND_CARD_TG，请求即触发、不等验卡/返回）；内部异步、失败兜底在 LoanCreditService，不阻断绑卡主流程
    loanCreditService.triggerBindCard(tongDunParam);
    String nationalMobileNumber = getNationalMobileNumberForBindCard(userId, sdkType);
    BindCardResultVO resultVO = preCheckOrGetErrResultVO(userId, sdkType, bankType, accountNumber, name, null, nationalMobileNumber);
    if (resultVO != null) {
      return resultVO;
    }
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);

    if (sdkType.isIdnLoanSDKType()) {
      checkBankAccountNumberBindUserCount(userId, sdkType, bankType, accountNumber, build);
    }

    return bindCardLocker.lockAndRunResult(accountNumber, () -> {
      LoanBankAccountRecord record = getExistLoanBankAccountRecord(userId, sdkType, bankType, accountNumber, businessName);
      if (record != null) {
        BankAccountAvailableStatus status = BankAccountAvailableStatus.fromCodeOrThrow(record.getAvailableStatus());
        if (!BankAccountAvailableStatus.INACTIVE_STATUS_LIST.contains(status)) {
          return BindCardResultVO.from(LoanBankAccountVO.from(record));
        } else {
          record = loanBankAccountModel.updateName(record, name);
        }
      }

      //不需要验卡
      if (bankConfig.noNeedValidation(sdkType) || bankConfig.noNeedValidationByUserId(userId)) {
        record = dealLoanBankAccountWithNoValidate(
            userId,
            sdkType,
            bankType,
            accountNumber,
            name,
            null,
            record,
            build
        );
        return BindCardResultVO.from(LoanBankAccountVO.from(record));
      }

      // 进行验卡
      String validationId = idGeneratorService.genId(IdGeneratorService.Type.BANK_CARD_NV);
      if (record == null) {
        record = initLoanBankAccountNewLogic(
            userId,
            sdkType,
            bankType,
            null,
            accountNumber,
            name,
            BankAccountAvailableStatus.INIT,
            validationId
        );
      } else {
        loanBankAccountModel.updateValidationId(record, bankType, validationId);
      }
      return getIdnLoanValidResultAndUpdateStatusV3(userId, sdkType, bankType, accountNumber, name, record, validationId, nationalMobileNumber, ValidationVersion.V2);
    });
  }

  @Nullable
  private BindCardResultVO preCheckOrGetErrResultVO(Long userId, SDKType sdkType, BankType bankType, String accountNumber, String name, BankAccountType bankAccountType, String nationalMobileNumber) {
    precheckBankCard(bankType, accountNumber, name);
    YqgLocale locale = sdkType.getLocale();
    boolean expression = globalBankAccountNumValidator.isValidBankAccountNumber(locale, bankType, bankAccountType, accountNumber);
    if (!expression) {
      log.warn("银行卡号格式不正确, userId:{}, accountNumber: {}, ", userId, accountNumber);
      String errMsg = bankType.isEWallet ? "账号格式不正确" : "银行账户格式不正确";
      return BindCardResultVO.from(LoanBankAccountVO.from(bankType, accountNumber, name, BankAccountAvailableStatus.UNAVAILABLE), TT.gen(errMsg));
    }

    if (!bankType.isEWallet) {
      return null;
    }

    if (sdkType.isIdnSDKType() && (bankConfig.getVerifyGoPayAccountMethod() == VerifyEWalletAccountMethod.MOBILE_NUMBER || bankConfig.getVerifyGoPayAccountMethod() == VerifyEWalletAccountMethod.NAME_AND_MOBILE_NUMBER)) {
      if (!accountNumber.equals(nationalMobileNumber)) {
        return BindCardResultVO.fromIdnLoanEWalletV2(
            LoanBankAccountVO.from(bankType, accountNumber, name, BankAccountAvailableStatus.UNAVAILABLE),
            FinalValidationBankAccountStatus.E_WALLET_MOBILE_NUMBER_NOT_MATCH,
            false);
      }
    }
    return null;
  }

  public void precheckBankCard(BankType bankType, String accountNumber, String name) {
    Map<String, String> bankCardValidationCheckVO = loanBankConfig.getBankCardVallidationCheck(bankType);
    if (Objects.isNull(bankCardValidationCheckVO)) {
      return;
    }

    //todo shubo/andrey later will ask pm provider content how to descibe this
    if (!StringUtils.isBlank(bankCardValidationCheckVO.get("length")) && accountNumber.length() > Integer.parseInt(bankCardValidationCheckVO.get("length"))) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("Add Bank Card Failed. Account Number Length={0}, Max Length={1}, BankType={2}", accountNumber.length(), bankCardValidationCheckVO.get("length"), bankType.name()));
    }
    if (!StringUtils.isBlank(bankCardValidationCheckVO.get("prefix")) && !accountNumber.startsWith(bankCardValidationCheckVO.get("prefix"))) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("Add Bank Card Failed. Account Number={0}, Prefix Required={1}, BankType={2}", accountNumber, bankCardValidationCheckVO.get("prefix"), bankType.name()));
    }
  }

  public LoanBankAccountRecord getExistLoanBankAccountRecord(
      Long userId,
      SDKType sdkType,
      BankType bankType,
      String accountNumber,
      PaymentBusinessName businessName) {
    LoanBankAccountRecord record;
    boolean isBankCardUniqueKeyWithBankCode = bankType.isEWallet || isBankCardUniqueKeyWithBankCode(userId);
    if (isBankCardUniqueKeyWithBankCode) {
      record = loanBankAccountModel.findByUserIdAndAccountNumberAndBankCode(userId, businessName, accountNumber, bankType.name());
    } else {
      record = loanBankAccountModel.findByUserIdAndAccountNumber(userId, businessName, accountNumber);
    }
    boolean recordIsNull = Objects.isNull(record);
    log.info(
        "getExistLoanBankAccountRecord, userId: {}, sdkType: {}, bankType: {}, accountNumber: {}, businessName: {}, isEWallet: {}, isBankCardUniqueKeyWithBankCode: {}, record is null: {}",
        userId, sdkType, bankType, accountNumber, businessName, bankType.isEWallet, isBankCardUniqueKeyWithBankCode, recordIsNull);
    bankCardUniqueKeyCheckMonitorService.log(userId, sdkType, bankType, accountNumber, businessName, isBankCardUniqueKeyWithBankCode,
        recordIsNull);
    return record;
  }

  public LoanBankAccountVO getExistLoanBankAccountVO(Long userId,
                                                     SDKType sdkType,
                                                     BankType bankType,
                                                     String accountNumber,
                                                     PaymentBusinessName businessName) {
    return LoanBankAccountVO.from(getExistLoanBankAccountRecord(userId, sdkType, bankType, accountNumber, businessName));
  }

  /**
   * 3.25版本下掉此分流，都使用c分流
   */
  public AddBankAccountNotMatchStrategy getAddBankAccountNotMatchStrategy() {
    return AddBankAccountNotMatchStrategy.C;
  }

  private BindCardResultVO getIdnLoanValidResultAndUpdateStatusV3(
      Long userId,
      SDKType sdkType,
      BankType bankType,
      String accountNumber,
      String name,
      LoanBankAccountRecord record,
      String validationId,
      String nationalMobileNumber,
      ValidationVersion validationVersionFrom) {
    PaymentAccount paymentAccount = PaymentAccount.getDefaultAccount(sdkType);
    ValidationResultV3 validationResultV3 = bankAccountPaymentMethod.validateBankAccountV3(
        userId,
        paymentAccount,
        bankType,
        accountNumber,
        name,
        validationId,
        record.getId(),
        sdkType
    );
    return updateIdnLoanBankStatusV3(userId, record.getId(), validationResultV3, name, sdkType, bankType, nationalMobileNumber, validationVersionFrom);
  }

  private String getNationalMobileNumberForBindCard(Long userId, SDKType sdkType) {
    return getNationalMobileNumberForBindCard(userId, null, sdkType);
  }


  private String getNationalMobileNumberForBindCard(Long userId, SourceType sourceType, SDKType sdkType) {
    if (!Boolean.TRUE.equals(apiChannelConfig.getChangeMobileSource())) {
      return userService.getNationalMobileNumberOrThrow(userId, sdkType);
    }
    if (sourceType != null) {
      return getNationalMobileNumberWithSourceType(userId, sourceType, sdkType);
    }
    SourceType requestSourceType = ImpliedContextUtils.sourceType();
    if (requestSourceType != null && requestSourceType.isApiChannelSourceType()) {
      return getNationalMobileNumberForApi(userId, requestSourceType, sdkType);
    }
    return userService.getNationalMobileNumberOrThrow(userId, sdkType);
  }

  private String getNationalMobileNumberWithSourceType(Long userId, SourceType sourceType, SDKType sdkType) {
    if (sourceType.isApiChannelSourceType()) {
      return getNationalMobileNumberForApi(userId, sourceType, sdkType);
    }
    return userService.getNationalMobileNumberOrThrow(userId, sdkType);
  }

  private String getNationalMobileNumberForApi(Long userId, SourceType sourceType, SDKType sdkType) {
    ApiChannel apiChannel = ApiChannel.fromSourceType(sourceType);
    ApiChannelUserResponse apiChannelUserResponse = apiChannelUserService.getApiChannelUser(userId, apiChannel);
    if (apiChannelUserResponse == null || StringUtils.isBlank(apiChannelUserResponse.getNormalizedMobileNumber())) {
      throw EcException.error("failed to get api channel user mobile number, userId: {}, apiChannel: {}", userId, apiChannel);
    }
    return MobileConverter.getNationalOrThrow(sdkType.getLocale(), apiChannelUserResponse.getNormalizedMobileNumber());
  }

  private BindCardResultVO updateIdnLoanBankStatusV3(
      Long userId,
      Long bankAccountId,
      ValidationResultV3 validationResultV3,
      String name,
      SDKType sdkType,
      BankType bankType,
      String nationalMobileNumber,
      ValidationVersion validationVersionFrom) {
    boolean canModifyName = false;
    BankAccountAvailableStatus curStatus;
    LoanBankAccountVO bankAccountVO;
    switch (validationVersionFrom) {
      case V1:
        ValidationCardStatus validationCardStatus;
        if (sdkType == SDKType.IDN_FIN) {
          validationCardStatus = bankAccountValidationUtil.getFinValidationStatusFromResultV3(validationResultV3, name, bankType, sdkType);
        } else {
          Long loanAccountId = loanAccountService.getAccountIdByUserIdOrThrow(userId, sdkType);
          if (StringUtils.isBlank(nationalMobileNumber)) {
            nationalMobileNumber = getNationalMobileNumberForBindCard(userId, sdkType);
          }
          validationCardStatus = bankAccountValidationUtil.getLoanValidationStatusFromResultV3(name, nationalMobileNumber, validationResultV3, bankType, sdkType);
          if (ValidationCardStatus.NOT_MATCH_LIST.contains(validationCardStatus)) {
            long notMatchTimes = increaseAndGetNotMatchRemindTimes(loanAccountId);
            canModifyName = notMatchTimes > bankConfig.getAddBankAccountMaxNotMatchTimes();
          }
        }

        curStatus = getBindAccountStatus(validationCardStatus);
        bankAccountVO = updateBankStatus(bankAccountId, name, curStatus);
        if (curStatus == BankAccountAvailableStatus.UNAVAILABLE) {
          log.info("validation v3 convert to v1 result status: {}, user name: {}, userId: {}", validationCardStatus.name(), name, userId);
        }
        return BindCardResultVO.from(bankAccountVO, validationCardStatus, sdkType, canModifyName);
      case V2:
        Long loanAccountId = loanAccountService.getAccountIdByUserIdOrThrow(userId, sdkType);
        if (StringUtils.isBlank(nationalMobileNumber)) {
          nationalMobileNumber = getNationalMobileNumberForBindCard(userId, sdkType);
        }
        LoanBankAccountVO loanBankAccountVO = getBankAccountVO(bankAccountId);
        FinalValidationBankAccountStatus finalValidationStatus = bankAccountValidationUtil.getFinalValidationStatusFromResultV3(validationResultV3, name, loanBankAccountVO.accountNumber, nationalMobileNumber, bankType);
        if (FinalValidationBankAccountStatus.E_WALLET_MOBILE_NUMBER_AND_NAME_NOT_MATCH == finalValidationStatus ||
            FinalValidationBankAccountStatus.NAME_NOT_MATCH == finalValidationStatus) {
          long notMatchTimes = increaseAndGetNotMatchRemindTimes(loanAccountId);
          canModifyName = notMatchTimes > bankConfig.getAddBankAccountMaxNotMatchTimes();
        }

        curStatus = getBindAccountStatusV2(finalValidationStatus);
        bankAccountVO = updateBankStatus(bankAccountId, name, curStatus);
        if (curStatus == BankAccountAvailableStatus.UNAVAILABLE) {
          log.info("validation v3 convert to v2 result status: {}, user name: {}, userId: {}", finalValidationStatus.name(), name, userId);
        }
        return bankAccountVO.bankType.isEWallet ?
            BindCardResultVO.fromIdnLoanEWalletV2(bankAccountVO, finalValidationStatus, canModifyName) :
            BindCardResultVO.fromIdnLoanBankCardV2(bankAccountVO, finalValidationStatus, canModifyName);
      default:
        throw EcException.error("unsupported Bank Card Validation Version Switch");
    }
  }

  private void checkParam(SDKType sdkType, String name) {
    if (sdkType == null) {
      throw EcException.error("sdk is null");
    }
    if (StringUtils.isBlank(name)) {
      throw EcException.error("持卡人姓名不能为空");
    }
    if (name.length() > 50) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("姓名不能超过50个字符"));
    }
  }

  private LoanBankAccountRecord initLoanBankAccountNewLogic(Long userId,
                                                            SDKType sdkType,
                                                            BankType bankType,
                                                            BankAccountType bankAccountType,
                                                            String accountNumber,
                                                            String name,
                                                            BankAccountAvailableStatus status,
                                                            String validationId) {
    checkBankAccountNumberMultiple(sdkType, bankType, accountNumber, userId);
    //更新绑卡后置log的绑卡时间
    bindCardDelayService.updateTimeBind(userId);
    return loanBankAccountModel.initAllowMultipleBindCard(
        userId,
        sdkType,
        bankType,
        bankAccountType,
        accountNumber,
        name,
        status,
        validationId
    );
  }

  public LoanBankAccountRecord dealLoanBankAccountWithNoValidate(Long userId,
                                                                 SDKType sdkType,
                                                                 BankType bankType,
                                                                 String accountNumber,
                                                                 String name,
                                                                 BankAccountType bankAccountType,
                                                                 LoanBankAccountRecord existRecord,
                                                                 Long build) {
    if (sdkType.isIdnLoanSDKType()) {
      checkBankAccountNumberBindUserCount(userId, sdkType, bankType, accountNumber, build);
    }
    if (existRecord == null) {
      //先初始化一条PENDING，调用payment成功后，再置为AVAILABLE
      existRecord = initLoanBankAccountNewLogic(
          userId,
          sdkType,
          bankType,
          bankAccountType,
          accountNumber,
          name,
          BankAccountAvailableStatus.INIT,
          null
      );
    }
    BankAccountAvailableStatus status = BankAccountAvailableStatus.fromCodeOrThrow(existRecord.getAvailableStatus());
    if (status == BankAccountAvailableStatus.AVAILABLE) {
      return existRecord;
    }
    return registerAndUpdateToAvailable(sdkType, bankType, accountNumber, name, existRecord);
  }

  public LoanBankAccountRecord registerAndUpdateToAvailable(
      SDKType sdkType,
      BankType bankType,
      String accountNumber,
      String name,
      LoanBankAccountRecord existRecord
  ) {
    bankAccountPaymentMethod.registerOverseasPayment(sdkType, PaymentAccount.getDefaultAccount(sdkType), bankType, accountNumber, name, existRecord.getId());
    return updateAvailableStatus(existRecord.getId(), BankAccountAvailableStatus.AVAILABLE);
  }

  private void checkBankAccountNumberMultiple(SDKType sdkType, BankType bankType, String accountNumber, Long userId) {
    if (!curEnvSupportMultipleCheck()) {
      return;
    }

    //印尼现在只有IDN_YQD和IDN_FIN，这里弃用之前bySdkType取配置的方式
    switch (sdkType) {
      //理财同一个SDK不允许重复，跨SDK不允许重复 ---> 有任何一条都不行
      case IDN_FIN:
        Integer countByAccountNumber;
        if (isBankCardUniqueKeyWithBankCode(userId)) {
          countByAccountNumber = loanBankAccountModel.countByAccountNumberAndBankCode(bankType, accountNumber);
        } else {
          countByAccountNumber = loanBankAccountModel.countByAccountNumber(bankType, accountNumber);
        }
        assertTrueOrThrowBankAccountDuplicate(countByAccountNumber <= 0, accountNumber);
        return;
      case IDN_YQD:
        //借贷同一个SDK允许重复，跨SDK允许重复 ---> 百无禁忌
        return;
      default:
        throw EcException.error("Unsupported sdk type: {}!", sdkType);
    }
  }

  public void checkBankAccountNumberBindUserCount(Long userId, SDKType sdkType, BankType bankType, String accountNumber, Long build) {
    if (build < userSecureConfig.getAuthCheckMinBuild()) {
      return;
    }

    if (loanBankConfig.getAuthCheckBankAccountNumberWhitelist().contains(accountNumber)) {
      return;
    }

    UserInfoVO userInfoVO = userService.fetchById(userId, sdkType);
    String channel = userInfoVO.channel;
    SourceType sourceType = userInfoVO.sourceType;

    if (sourceType.isApiChannelSourceType()) {
      return;
    }

    Map<String, CheckAuthConfigVO> channelAndLimitCountMap = userSecureConfig.getCheckNeedMergeAccountChannelMap();
    if (!channelAndLimitCountMap.containsKey(channel)) {
      return;
    }
    //配置里面没有添加银行卡的数量限制
    if (Objects.isNull(channelAndLimitCountMap.get(channel).bankCardBindUserLimit)) {
      return;
    }

    long startTime = Clock.now();

    long limitTime = startTime - userSecureConfig.getAuthCheckStartTime() * Clock.MILLS_PER_DAY;
    Set<String> nikSet = getNikSet(sdkType, bankType, accountNumber, limitTime);

    authCheckTimeCostMonitorService.log(userId, sourceType, sdkType, CheckType.BANK_ACCOUNT, channel, Clock.now() - startTime);
    if (nikSet.size() >= channelAndLimitCountMap.get(channel).bankCardBindUserLimit) {
      userCheckHistoryModel.insert(userId, userInfoVO.loanAccountId, sourceType, CheckType.BANK_ACCOUNT, String.valueOf(nikSet.size()));
      assertTrueOrThrowBankAccountDuplicate(false, accountNumber);
    }

  }

  public Set<String> getNikSet(SDKType sdkType, BankType bankType, String accountNumber, long limitTime) {
    List<LoanBankAccountRecord> loanBankAccountRecordList = loanBankAccountModel.findListByAccountNumberAndBankCodeAndAvailableStatusAndTimeCreate(sdkType,
        accountNumber,
        bankType,
        CHECK_STATUS_LIST,
        limitTime);
    List<Long> userIdSet = loanBankAccountRecordList
        .stream()
        .map(LoanBankAccountRecord::getUserId)
        .collect(Collectors.toList());

    List<Long> loanAccountIds = loanAccountService.fetchLoanAccountIdsByUserIdsAndSDK(userIdSet, sdkType);

    return loanUserAdditionalInfoModel.getInfosByAccountsAndTypes(loanAccountIds, ImmutableList.of(LoanUserAdditionalInfoType.IDN_ID_NO))
        .stream()
        .map(vo -> vo.value)
        .collect(Collectors.toSet());
  }

  private boolean curEnvSupportMultipleCheck() {
    if (SysEnvironment.isProd() || SysEnvironment.isFeat()) {
      return true;
    }
    return bankConfig.supportMultipleCheckForNonProd();
  }

  private static void assertTrueOrThrowBankAccountDuplicate(boolean expression, String accountNumber) {
    if (!expression) {
      log.warn("bank account Multiple check failed！accountNumber is {}", accountNumber);
      throw EcException.warn(EcExceptionType.LOAN_BANK_ACCOUNT_DUPLICATE, TT.gen("该银行卡已被其他账号绑定"));
    }
  }

  private long increaseAndGetNotMatchRemindTimes(Long loanAccountId) {
    Long expireSeconds = bankConfig.getAddBankAccountPeriodSeconds();
    return addBankAccountNotMatchTimesLoader.incrNotMatchTimes(loanAccountId, expireSeconds);
  }

  @NotNull
  private BankAccountAvailableStatus getBindAccountStatus(ValidationCardStatus validationCardStatus) {
    switch (validationCardStatus) {
      case MATCH:
        return BankAccountAvailableStatus.AVAILABLE;
      case IN_REVIEW:
        return BankAccountAvailableStatus.IN_REVIEW;
      case PENDING:
        return BankAccountAvailableStatus.PENDING;
      default:
        return BankAccountAvailableStatus.UNAVAILABLE;
    }
  }

  @NotNull
  private BankAccountAvailableStatus getBindAccountStatusV2(FinalValidationBankAccountStatus finalValidationStatus) {
    switch (finalValidationStatus) {
      case MATCH:
        return BankAccountAvailableStatus.AVAILABLE;
      case PENDING:
        return BankAccountAvailableStatus.PENDING;
      default:
        return BankAccountAvailableStatus.UNAVAILABLE;
    }
  }

  private LoanBankAccountVO updateBankStatus(Long bankAccountId, String name, BankAccountAvailableStatus curStatus) {
    return transactionalModel.transactionResult(configuration -> {
      LoanBankAccountRecord record = loanBankAccountModel.findByIdForUpdateOrThrow(bankAccountId);
      BankAccountAvailableStatus lastStatus = BankAccountAvailableStatus.fromCodeOrThrow(record.getAvailableStatus());
      if (lastStatus == curStatus) {
        return LoanBankAccountVO.from(record);
      }
      switch (curStatus) {
        case UNAVAILABLE:
          updateAvailableStatus(bankAccountId, curStatus);
          break;
        case PENDING:
        case IN_REVIEW:
        case AVAILABLE:
          updateAvailableStatusAndName(bankAccountId, name, curStatus);
          break;
        default:
          throw EcException.error("invalid loan_bank_account_status. id: {}, status: {}", record.getId(), curStatus);
      }
      record = loanBankAccountModel.findById(bankAccountId);
      return LoanBankAccountVO.from(record);
    });
  }

  private LoanBankAccountRecord updateAvailableStatusAndName(Long bankAccountId, String name, BankAccountAvailableStatus curStatus) {
    LoanBankAccountRecord record = loanBankAccountModel.findByIdOrThrow(bankAccountId);
    record = loanBankAccountModel.updateByValidationResult(record, curStatus, name);
    userEventService.publishBindBankAccountAvailableEvent(record.getUserId(), SDKType.fromCode(record.getSdkType()), record.getBankCode(), record.getAccountNumber(), curStatus);
    return record;
  }

  public List<BankType> getSupportedChannelList(SDKType sdkType, boolean isEWallet) {
    PaymentAccount paymentAccount = PaymentAccount.getDefaultAccount(sdkType);
    return bankAccountPaymentMethod.getAvailableBankType(paymentAccount).stream()
        .filter(bankType -> Objects.nonNull(bankType) && isEWallet == bankType.isEWallet).collect(Collectors.toList());
  }

  public List<BankType> getSupportedChannelListALL(SDKType sdkType) {
    PaymentAccount paymentAccount = PaymentAccount.getDefaultAccount(sdkType);
    return bankAccountPaymentMethod.getAvailableBankType(paymentAccount).stream()
        .filter(bankType -> Objects.nonNull(bankType)).collect(Collectors.toList());
  }

  /**
   * 获取银行卡 不包含电子钱包
   *
   * @param sdkType
   * @return
   */
  public List<BankConfigVO> getSupportedBankVOList(SDKType sdkType) {
    List<BankType> bankTypeList = getSupportedChannelList(sdkType, false);
    return this.sortBankAndConvers(bankTypeList, sdkType);
  }

  /**
   * 获取银行卡（所有的类型）
   *
   * @param sdkType
   * @return
   */
  public List<BankConfigVO> getSupportedBankVOListALL(SDKType sdkType) {
    List<BankType> bankTypeList = getSupportedChannelListALL(sdkType);
    return this.sortBankAndConvers(bankTypeList, sdkType);
  }

  /**
   * 只获取电子钱包
   *
   * @param sdkType
   * @return
   */
  public List<BankConfigVO> getSupportedBankVOListEWallet(SDKType sdkType) {
    List<BankType> bankTypeList = getSupportedChannelList(sdkType, true);
    return this.sortBankAndConvers(bankTypeList, sdkType);
  }

  public List<BankConfigVO> sortBankAndConvers(List<BankType> bankTypeList, SDKType sdkType) {
    bankTypeList = sortBank(bankTypeList, bankConfig.getDisplayRule(sdkType));
    if (CollectionUtils.isEmpty(bankTypeList)) {
      throw EcException.error("can't find bind_card_config for sdk: " + sdkType);
    }
    Map<String, BankConfigVO> bankConfigVOMap = bankConfigService.getBankVoMap(Sets.newHashSet(bankTypeList), sdkType);

    List<BankConfigVO> voList = Lists.newArrayList();
    for (BankType type : bankTypeList) {
      BankConfigVO vo = bankConfigVOMap.get(type.name());
      if (vo == null) {
        log.error("can't find bank config. bank_type:{}", type);
        continue;
      }
      voList.add(vo);
    }
    return voList;
  }

  public List<BankConfigVO> filterBankTypeByABTest(List<BankConfigVO> bankList, Long userId) {
    if (CollectionUtils.isEmpty(bankList)) {
      return bankList;
    }
    //如果在白名单内，直接返回
    if (loanBankConfig.getFilterBankTypeWhiteUserId().contains(userId)) {
      return bankList;
    }
    Boolean needFilter = Boolean.valueOf(
        expFacade.fetchResult(ABTestSceneType.USER_BIND_BANK_CARD_FILTER, ABTestUtil.genDiversionKeyMapByUserId(userId), "TRUE"));
    if (needFilter) {
      List<BankType> filterBankType = loanBankConfig.getFilterBankType();
      bankList = bankList.stream().filter(e -> !filterBankType.contains(e.getBankType())).collect(Collectors.toList());
    }
    return bankList;
  }

  /**
   * 命中"SuperBank渠道绑卡"实验组（B组）时，仅保留 SuperBank。
   */
  public List<BankConfigVO> keepOnlySuperBankIfInExperiment(List<BankConfigVO> banks, Long userId, Long build, String deviceId) {
    // 基础参数校验
    if (CollectionUtils.isEmpty(banks) || Objects.isNull(userId) || Objects.isNull(build) || Objects.isNull(deviceId)) {
      return banks;
    }

    // 获取广告来源并校验
    String mediaSource = getMediaSourceByDeviceId(deviceId);
    if (Objects.isNull(mediaSource) || !isValidMediaSource(mediaSource)) {
      return banks;
    }

    if (!isInSuperBankBindCardExperiment(userId, build)) {
      return banks;
    }

    List<BankConfigVO> list = banks.stream()
        .filter(Objects::nonNull)
        .filter(b -> b.getBankType() == BankType.FAMA)
        .collect(Collectors.toList());

    if (CollectionUtils.isEmpty(list)) {
      return banks;
    }

    return list;
  }

  private String getMediaSourceByDeviceId(String deviceId) {
    try {
      AdsResponse<AppsflyerResultVO> adsResponse = adsAppsflyerService.getAppsflyerMediaSourceByDeviceId(deviceId);
      if (adsResponse == null || adsResponse.body == null) {
        log.warn("Failed to get media source from Appsflyer, deviceId: {}", deviceId);
        return null;
      }
      return adsResponse.body.getMediaSource();
    } catch (Exception e) {
      log.error("Error getting media source from Appsflyer", e);
      return null;
    }
  }

  private boolean isValidMediaSource(String mediaSource) {
    List<String> filterMediaSourceList = loanBankConfig.getFilterBankTypeMediaSourceAllowList();
    return !CollectionUtils.isEmpty(filterMediaSourceList) && filterMediaSourceList.contains(mediaSource);
  }

  private boolean isInSuperBankBindCardExperiment(Long userId, Long build) {
    ABTestUserIdRequestVO request = ABTestUserIdRequestVO.from(
        ExperimentNameSpace.SUPERBANK_CHANNEL_USER_BINDING_CARD, userId);
    String abTestResult = expFacade.fetchResultFallBackWithDefaultScene("technology-auth-abroad-loan-superbank_rename_0908", ExpFacade.ClientType.DIVERSION, request, build);
    if (StringUtils.isEmpty(abTestResult)) {
      return false;
    }
    return Boolean.parseBoolean(abTestResult);
  }

  public List<BankConfigVO> filterEWalletTypeByABTest(List<BankConfigVO> bankList, Long userId) {
    Boolean needFilter = Boolean.valueOf(
        expFacade.fetchResult(ABTestSceneType.IDN_EWALLET_BIND_DISPLAY_FILTER, ABTestUtil.genDiversionKeyMapByUserId(userId), "TRUE"));
    if (needFilter) {
      List<BankType> filterEWalletType = loanBankConfig.getFilterEWalletType();
      bankList = bankList.stream().filter(e -> !filterEWalletType.contains(e.getBankType())).collect(Collectors.toList());
    }
    return bankList;
  }

  /**
   * 命中"IDN银行弹窗文案实验"实验组时，返回一个新的 Map，其中所有 value 的 popWindowText 设置为空字符串
   * 避免直接修改传入的共享配置对象，防止跨请求数据污染
   *
   * @param bankTypeToBankAttributeVOMap 原始配置 map
   * @param userId 用户ID
   * @param build 构建版本
   * @param sourceType 来源类型
   * @return 如果命中实验，返回新的 Map（popWindowText 为空字符串）；否则返回原始 map（不创建新对象）
   */
  public Map<BankType, BankAttributeVO> clearPopWindowTextIfInExperiment(Map<BankType, BankAttributeVO> bankTypeToBankAttributeVOMap, Long userId, Long build, SourceType sourceType, Long loanAccountId) {
    if (bankTypeToBankAttributeVOMap == null || userId == null || build == null) {
      return bankTypeToBankAttributeVOMap;
    }
    
    boolean shouldClearText = isInPopWindowTextExperiment(userId, build, sourceType, loanAccountId);
    
    if (!shouldClearText) {
      return bankTypeToBankAttributeVOMap;
    }
    
    // 命中实验时，创建新的 Map 和新的 BankAttributeVO 对象
    return bankTypeToBankAttributeVOMap.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
              BankAttributeVO originalVO = entry.getValue();
              if (originalVO == null) {
                return null;
              }
              // 使用 copy 方法，代码更简洁清晰
              return originalVO.copy("");
            }
        ));
  }

  private boolean isInPopWindowTextExperiment(Long userId, Long build, SourceType sourceType, Long loanAccountId) {
    boolean authFinished = authService.isAuthFinished(loanAccountId);
    if (authFinished) {
      //  若已完件，直接返回
      return false;
    }
    String tempKey = "technology-auth-abroad-loan-card_page_notification_remove";
    ExpUser expUser = ExpUser.builder()
        .userId(userId)
        .versionBuild(build)
        .sourceType(sourceType)
        .build();
    String result = expDiversionClient.getResult(tempKey, expUser);
    if (StringUtils.isEmpty(result) || AbstractExpClient.BLANK_GROUP.equals(result)) {
      return false;
    }
    return "B".equals(result);
  }

  private List<BankType> sortBank(List<BankType> bankTypeList, Map<BankType, Integer> displayRule) {
    int defaultIndex = displayRule.size();
    return bankTypeList
        .stream()
        .sorted((o1, o2) -> ComparisonChain.start()
            .compare(displayRule.getOrDefault(o1, defaultIndex), displayRule.getOrDefault(o2, defaultIndex))
            .compare(o1.name(), o2.name())
            .result())
        .collect(Collectors.toList());
  }

  public void updateStatusBy3rdPartyResult(Long startTime, Long endTime) {
    List<LoanBankAccountRecord> accountRecords = loanBankAccountModel.findPendingAccountListInUpdatedTimeWindow(startTime, endTime);

    accountRecords.forEach(record -> {
      try {
        ValidationResultV3 validationResultV3 = bankAccountPaymentMethod.queryValidationResultV3(
            record.getUserId(),
            BankType.valueOf(record.getBankCode()),
            record.getAccountNumber(),
            record.getValidationId(),
            record.getName(),
            SDKType.fromCode(record.getSdkType()),
            record.getId(),
            true);
        updateIdnLoanBankStatusV3(
            record.getUserId(),
            record.getId(),
            validationResultV3,
            record.getName(),
            SDKType.fromCode(record.getSdkType()),
            BankType.valueOf(record.getBankCode()),
            null,
            ValidationVersion.V1);
      } catch (Exception e) {
        log.error("validation bank card failed, bankAccountId is {}", record.getId(), e);
      }
    });
  }

  public LoanBankAccountVO verifyBankAccount(Long bankAccountId) {
    LoanBankAccountRecord accountRecord = loanBankAccountModel.findByIdOrThrow(bankAccountId);
    if (accountRecord.getAvailableStatus().equals(BankAccountAvailableStatus.IN_REVIEW.charCode)) {
      accountRecord = updateAvailableStatus(bankAccountId, BankAccountAvailableStatus.AVAILABLE);
    }
    return LoanBankAccountVO.from(accountRecord);
  }

  public LoanBankAccountVO vetoedBankAccount(Long bankAccountId) {
    LoanBankAccountRecord accountRecord = loanBankAccountModel.findByIdOrThrow(bankAccountId);
    if (accountRecord.getAvailableStatus().equals(BankAccountAvailableStatus.IN_REVIEW.charCode)) {
      accountRecord = updateAvailableStatus(bankAccountId, BankAccountAvailableStatus.UNAVAILABLE);
    }
    return LoanBankAccountVO.from(accountRecord);
  }

  public LoanBankAccountVO vetoedBankAccountForForeignerAuth(Long bankAccountId) {
    LoanBankAccountRecord accountRecord = updateAvailableStatus(bankAccountId, BankAccountAvailableStatus.UNAVAILABLE);
    return LoanBankAccountVO.from(accountRecord);
  }

  private ValidationCardStatus getFinValidationStatusFromResult(ValidationResult validationResult, String name) {
    if (validationResult.validationStatus == ValidationResult.ValidationStatus.PENDING) {
      return ValidationCardStatus.PENDING;
    }
    if (validationResult.validationStatus == ValidationResult.ValidationStatus.FAILURE) {
      return ValidationCardStatus.CARD_UNAVAILABLE;
    }
    //直接根据result获取
    if (validationResult.result != null) {
      switch (validationResult.result) {
        case MATCH:
          return ValidationCardStatus.MATCH;
        case UNCLEAR:
          return ValidationCardStatus.IN_REVIEW;
        case NOT_MATCH:
          return ValidationCardStatus.NAME_NOT_MATCH;
        default:
          throw EcException.error("unsupported result : {}", validationResult.result);
      }
    }
    //不存在result的情况
    // 检查 用户提供的姓名与第三方返回的姓名 是否一致 忽略大小写
    if ((validationResult.name).equalsIgnoreCase(name)) {
      return ValidationCardStatus.MATCH;
    } else if (isNameSimilarForFinancing(validationResult.name.toLowerCase(), name.toLowerCase())) {
      // 检查 用户提供的姓名与第三方返回的姓名 是否相似 忽略大小写
      return ValidationCardStatus.IN_REVIEW;
    } else {
      // 如果用户提供的姓名与第三方返回的姓名相似度低于配置 直接拒绝
      log.warn("The provided account name is not similar the actual account name; actual name from 3rdParty: {}, provided name: {}",
          validationResult.name, name);
      return StringUtils.isNotBlank(validationResult.account) ? ValidationCardStatus.EWALLET_NOT_MATCH : ValidationCardStatus.NAME_NOT_MATCH;
    }
  }

  private boolean isNameSimilarForFinancing(String validationResultName, String name) {
    int result = FuzzySearch.tokenSetRatio(validationResultName, name);
    return result >= getFinancingBankAccountNameSimilarityLimit();
  }

  public LoanBankAccountVO getAvailableBankAccountVO(long paymentCredentialId) {
    LoanBankAccountRecord accountRecord = loanBankAccountModel.findByIdOrThrow(paymentCredentialId);
    if (!BankAccountAvailableStatus.AVAILABLE.charCode.equals(accountRecord.getAvailableStatus())) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("The loan bank account is no longer available. credential_id:{0}", paymentCredentialId));
    }
    return LoanBankAccountVO.from(accountRecord);
  }

  public LoanBankAccountVO getBankAccountVO(long paymentCredentialId) {
    LoanBankAccountRecord accountRecord = loanBankAccountModel.findByIdOrThrow(paymentCredentialId);
    return LoanBankAccountVO.from(accountRecord);
  }

  public Map<Long, LoanBankAccountVO> getBankAccountVOMap(Set<Long> paymentCredentialIds) {
    List<LoanBankAccountRecord> accountRecordList = loanBankAccountModel.fetchByIds(paymentCredentialIds);
    return accountRecordList
        .stream()
        .map(LoanBankAccountVO::from)
        .collect(Collectors.toMap(LoanBankAccountVO::getId, bankAccountVO -> bankAccountVO));
  }

  public LoanBankAccountVO getFinancingBankAccountVO(Long userId, SDKType sdkType) {
    List<LoanBankAccountVO> loanBankAccountVOS = getAvailableBankAccounts(userId, sdkType);
    if (CollectionUtils.isEmpty(loanBankAccountVOS)) {
      throw EcException.error(EcExceptionType.LOAN_BANK_ACCOUNT_NOT_FOUND, "用户未绑卡! userId = " + userId);
    }
    if (loanBankAccountVOS.size() > 1) {
      throw EcException.error("用户绑定了超过一张卡! userId = " + userId);
    }
    return loanBankAccountVOS.get(0);
  }

  public LoanBankAccountVO getFinancingBankAccountVOOrNull(Long userId, SDKType sdkType) {
    List<LoanBankAccountVO> loanBankAccountVOS = getAvailableBankAccounts(userId, sdkType);
    if (CollectionUtils.isEmpty(loanBankAccountVOS)) {
      return null;
    }
    if (loanBankAccountVOS.size() > 1) {
      throw EcException.error("用户绑定了超过一张卡! userId = " + userId);
    }
    return loanBankAccountVOS.get(0);
  }

  private int getFinancingBankAccountNameSimilarityLimit() {
    return ecSiteVars.getInt("financing.bank_account_name_similarity_limit", 0);
  }

  public void checkLoanBankCardMatch(LoanBankAccountVO bankAccountVO) {
    //TODO(chaoye,T00000) 下单前校验暂不使用V2的验卡结果，等未来验卡方法全量替换成V2了再修改
    ValidationCardStatus validationCardStatus;
    String validationResultAsString;
    ValidationResultV3 validationResultV3 = bankAccountPaymentMethod.queryValidationResultV3(
        bankAccountVO.userId,
        bankAccountVO.bankType,
        bankAccountVO.accountNumber,
        bankAccountVO.validationId,
        bankAccountVO.name,
        bankAccountVO.sdk,
        bankAccountVO.bankAccountId,
        false);
    String nationalMobileNumber = userService.getNationalMobileNumberOrThrow(bankAccountVO.userId, bankAccountVO.sdk);
    validationCardStatus = bankAccountValidationUtil.getLoanValidationStatusFromResultV3(bankAccountVO.name, nationalMobileNumber, validationResultV3, bankAccountVO.bankType, bankAccountVO.sdk);
    validationResultAsString = JsonUtils.toString(validationResultV3);
    boolean isMatch = validationCardStatus == ValidationCardStatus.MATCH;
    bankAccountMonitorService.logCreateOrderBankCardMatch(validationCardStatus, bankAccountVO.userId, isMatch);
    if (ValidationCardStatus.NOT_MATCH_LIST.contains(validationCardStatus)) {
      log.info("validation result: {}, create order user name: {}, userId: {}", validationResultAsString, bankAccountVO.name, bankAccountVO.userId);
      if (bankAccountVO.bankType.isEWallet) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE_DIALOG, TT.gen("不能用非本人账号提现，请点击收款账户绑定本人的收款账号"));
      }
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE_DIALOG, TT.gen("不能用非本人银行卡提现，请点击收款账户重新绑卡"));
    }
    if (validationCardStatus != ValidationCardStatus.MATCH) {
      throw EcException.warn(EcExceptionType.LOAN_BANK_ACCOUNT_FAIL, TT.gen(validationCardStatus.getLoanRemind()));
    }
  }

  public ValidationCardStatus getLoanValidationStatusFromResult(Long userId, String createOrderUserName, ValidationResult validationResult,
                                                                SDKType sdkType, BankType bankType) {
    if (validationResult.validationStatus == ValidationResult.ValidationStatus.PENDING) {
      return ValidationCardStatus.PENDING;
    }
    if (validationResult.validationStatus == ValidationResult.ValidationStatus.FAILURE) {
      return ValidationCardStatus.CARD_UNAVAILABLE;
    }
    if (StringUtils.isNotBlank(validationResult.account)) {
      String nationalMobileNumber = userService.getNationalMobileNumberOrThrow(userId, sdkType);
      return bankAccountValidationUtil.isUserEWalletAccountValid(bankType, validationResult.account, validationResult.name, createOrderUserName, nationalMobileNumber) ? ValidationCardStatus.MATCH : ValidationCardStatus.EWALLET_NOT_MATCH;
    }
    if (StringUtils.isBlank(validationResult.name)) {
      return validationResult.result == ValidationResult.Result.MATCH ? ValidationCardStatus.MATCH : ValidationCardStatus.NAME_NOT_MATCH;
    }
    return bankAccountValidationUtil.isNameMatch(bankType, validationResult.name, createOrderUserName) ? ValidationCardStatus.MATCH : ValidationCardStatus.NAME_NOT_MATCH;
  }

  public LoanBankAccountRecord updateAvailableStatus(Long bankAccountId, BankAccountAvailableStatus status) {
    LoanBankAccountRecord accountRecord = loanBankAccountModel.findByIdOrThrow(bankAccountId);
    accountRecord = loanBankAccountModel.updateAvailableStatus(accountRecord, status);
    userEventService.publishBindBankAccountAvailableEvent(accountRecord.getUserId(), SDKType.fromCode(accountRecord.getSdkType()), accountRecord.getBankCode(), accountRecord.getAccountNumber(), BankAccountAvailableStatus.AVAILABLE);
    return accountRecord;
  }

  public LoanBankAccountVO updateAvailableStatusAndReturnBankAccountVO(Long bankAccountId, BankAccountAvailableStatus status) {
    updateAvailableStatus(bankAccountId, status);
    return findVOById(bankAccountId);
  }

  public void updateLoanBankLastTimeUsed(PaymentCredential paymentCredential, Long timePayout) {
    if (paymentCredential.getMethod() == PaymentMethod.BANKCARD) {
      LoanBankAccountRecord record = loanBankAccountModel.findByIdOrThrow(paymentCredential.getId());
      loanBankAccountModel.updateLastTimeUsed(record, timePayout);
    }
  }

  public LoanBankAccountVO getByUserIdAndBankNumberAndType(Long userId, SDKType sdkType, String accountNumber, BankType bankType) {
    LoanBankAccountRecord bankAccountRecord = loanBankAccountModel.getByUserIdAndBankNumberAndType(userId, sdkType, accountNumber, bankType);
    if (bankAccountRecord == null) {
      return null;
    }
    return LoanBankAccountVO.from(bankAccountRecord);
  }

  public void checkBankNameMatch(ICredential credentialInfo, SDKType sdkType, Long loanAccountId) {
    //只有印尼需要校验银行卡姓名
    if (!sdkType.isIdnLoanSDKType()) {
      return;
    }
    if (!bankConfig.getNeedCheckNameMatch()) {
      return;
    }

    SimpleLoanAccountVO simpleLoanAccountVo = loanAccountService.getSimpleLoanAccountVo(loanAccountId);
    if (loanBankConfig.noNeedValidationByUserId(simpleLoanAccountVo.userId)) {
      return;
    }
    if (!(credentialInfo instanceof LoanBankAccountVO)) {
      throw EcException.error("credentialInfo is not a instance of LoanBankAccountVO, sdkType: {}, credentialId: {}, loanAccountId: {}",
          sdkType, credentialInfo.getId(), loanAccountId);
    }
    LoanBankAccountVO loanBankAccountVO = (LoanBankAccountVO) credentialInfo;
    if (canSkipBankCardCheck(loanAccountId, UserPaymentCredential.from(loanBankAccountVO))) {
      return;
    }
    checkLoanBankCardMatch(loanBankAccountVO);
  }

  //存在结清订单，且申请银行卡打款成功过
  private boolean canSkipBankCardCheck(Long loanAccountId, UserPaymentCredential paymentCredential) {
    return ecOrderService.existOrder(loanAccountId, CashLoanOrderStatus.COMPLETE) && ecOrderService.existOrder(paymentCredential, Arrays.asList(CashLoanOrderStatus.PAYOUT_STATUSES));
  }

  public void checkBankTypeSupported(ICredential credentialInfo, SDKType sdkType, Long loanAccountId, SourceType sourceType) {
    if (!(credentialInfo instanceof LoanBankAccountVO)) {
      throw EcException.error("credentialInfo is not a instance of LoanBankAccountVO, sdkType: {}, credentialId: {}, loanAccountId: {}",
          sdkType, credentialInfo.getId(), loanAccountId);
    }
    LoanBankAccountVO loanBankAccountVO = (LoanBankAccountVO) credentialInfo;
    checkEWallet(loanBankAccountVO, sourceType);
    if (bankConfig.getUnsupportedBankType().contains(loanBankAccountVO.bankType)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE_DIALOG, TT.gen("该银行卡不可用, 请点击收款账户重新绑卡"));
    }
  }

  //电子钱包类型检查
  private void checkEWallet(LoanBankAccountVO loanBankAccountVO, SourceType sourceType) {
    if (!loanBankAccountVO.bankType.isEWallet) {
      return;
    }

    List<BankType> illegalEWalletBankTypeList = bankConfig.getIllegalEWalletBankType();
    //是可用电子钱包
    if (!illegalEWalletBankTypeList.contains(loanBankAccountVO.bankType)) {
      return;
    }

    //电子钱包不可用时，根据分流决定是否检查并提示用户
    Boolean needCheck = Boolean.valueOf(expFacade.fetchResult(ABTestSceneType.CHECK_EWALLET_WHEN_CREATE_ORDER,
        ABTestUtil.genDiversionKeyMapByUserId(loanBankAccountVO.userId), "TRUE"));
    if (!needCheck) {
      return;
    }

    boolean h5Redirect = false;
    if (sourceType == SourceType.WEB) {
      h5Redirect = Boolean.valueOf(expFacade.fetchExistOrDefaultResult(ABTestSceneType.H5_LOAN_BIND_CARD, ABTestUtil.genDiversionKeyMapByUserId(loanBankAccountVO.userId), "TRUE"));
    }
    try {
      List<BankType> legalEWalletBankTypeList = bankConfig.getLegalEWalletBankType();
      //所有电子钱包都不可用
      if (legalEWalletBankTypeList.isEmpty()) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE_DIALOG, TT.gen("抱歉，{0}电子钱包不能提供本次借款的放款服务，请您使用本人银行账号进行借款", loanBankAccountVO.bankType.description));
      }

      String legalEWalletStr = legalEWalletBankTypeList
          .stream()
          .map(o -> o.description)
          .collect(Collectors.joining("/"));
      //用户所选电子钱包不可用，部分电子钱包可用
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE_DIALOG, TT.gen("抱歉，{0}不能提供本次借款的放款服务，请您使用本人银行账号或者绑定其他电子钱包({1})进行借款。", loanBankAccountVO.bankType.description, legalEWalletStr));
    } catch (EcException e) {
      if (e.exceptionType == EcExceptionType.COMMON_CUSTOM_MESSAGE_DIALOG && h5Redirect) {
        throw EcException.warn(EcExceptionType.CREATE_ORDER_CHECK_EWALLET_ERROR, e.detailTT);
      }
      throw e;
    }

  }

  public void checkPaymentCredential(PaymentMethod paymentMethod, String hashPaymentCredentialId, SDKType sdkType, Long loanAccountId, SourceType sourceType) {
    // 判断是否删卡
    ICredential credentialInfo = checkAndGetAvailableCredential(paymentMethod, hashPaymentCredentialId);

    //下单仅支持本人银行卡
    checkBankNameMatch(credentialInfo, sdkType, loanAccountId);

    //检查银行是否支持打款
    checkBankTypeSupported(credentialInfo, sdkType, loanAccountId, sourceType);
  }

  private ICredential checkAndGetAvailableCredential(PaymentMethod paymentMethod, String hashPaymentCredentialId) {
    Long paymentCredentialId = YqgHashids.decode(hashPaymentCredentialId);
    PaymentCredential paymentCredential = new PaymentCredential(paymentMethod, paymentCredentialId);
    ICredential credentialInfo = paymentService.getCredentialInfo(paymentCredential);
    Boolean isCredentialAvailable = credentialInfo.isCredentialAvailable();
    EcAsserts.assertNotNull(isCredentialAvailable, "isCredentialAvailable can not be null");
    if (!isCredentialAvailable) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE_DIALOG, TT.gen("该银行卡不可用, 请点击收款账户重新绑卡"));
    }
    return credentialInfo;
  }

  public List<LoanBankAccountVO> getByCondition(LoanBankAccountSearchCondition condition) {
    List<LoanBankAccountRecord> loanBankAccountRecords = loanBankAccountModel.fetchByCondition(condition);

    return loanBankAccountRecords.stream()
        .map(LoanBankAccountVO::from)
        .collect(Collectors.toList());
  }

  // 获取用户可用的打款账号
  public List<LoanBankAccountVO> getAvailableLoanBankAccount(Long userId, SDKType sdkType) {
    Map<PaymentMethod, List<ICredential>> paymentMethodAndCredentialsMap = paymentService.getPayoutCredentialInfo(userId, sdkType);
    if (null == paymentMethodAndCredentialsMap) {
      return Lists.newArrayList();
    }
    List<ICredential> credentialList = paymentMethodAndCredentialsMap.get(PaymentMethod.BANKCARD);
    if (CollectionUtils.isEmpty(credentialList)) {
      return Lists.newArrayList();
    }
    return credentialList.stream()
        .map(v -> (LoanBankAccountVO) v)
        .collect(Collectors.toList());
  }

  public List<LoanBankAccountVO> getAvailableBankAccountForIllegalEWallet(Long userId, SDKType sdkType) {
    List<LoanBankAccountVO> availableLoanBankAccount = getAvailableLoanBankAccount(userId, sdkType);
    if (CollectionUtils.isEmpty(availableLoanBankAccount)) {
      return Lists.newArrayList();
    }
    List<BankType> illegalEWalletBankTypeList = bankConfig.getIllegalEWalletBankType();
    return availableLoanBankAccount.stream()
        .filter(v -> !illegalEWalletBankTypeList.contains(v.bankType))
        .collect(Collectors.toList());
  }

  // 获取用户默认的打款账号
  public LoanBankAccountVO getDefaultLoanBankAccount(Long userId, SDKType sdkType) {
    List<LoanBankAccountVO> availableLoanBankAccounts = getAvailableLoanBankAccount(userId, sdkType);
    if (CollectionUtils.isEmpty(availableLoanBankAccounts)) {
      return null;
    }

    return availableLoanBankAccounts.get(0);
  }

  public HomeDisplayStrategy routePaymentCredentialStrategy(Long userId, SDKType sdkType, Long build, SourceType sourceType) {
    LoanBankAccountVO loanBankAccountVO = getDefaultLoanBankAccount(userId, sdkType);
    List<BankType> illegalEWalletBankTypeList = bankConfig.getIllegalEWalletBankType();

    if (homepageV5Config.getUnavailableLoanAccountVersion() > build
        || Objects.isNull(loanBankAccountVO)
        || SourceType.WEB == sourceType) {
      return HomeDisplayStrategy.A;
    }

    String result = expFacade.fetchResultOrDefaultOrNullByUserIdAndABTestSceneType(ABTestSceneType.UNSUPPORTED_BANK, userId, "B");
    if (StringUtils.isNotEmpty(result)) {
      return HomeDisplayStrategy.valueOf(result);
    }

    if (!illegalEWalletBankTypeList.contains(loanBankAccountVO.bankType)) {
      return HomeDisplayStrategy.A;
    }

    return HomeDisplayStrategy.valueOf(expFacade.fetchResult(ABTestSceneType.UNSUPPORTED_BANK, ABTestUtil.genDiversionKeyMapByUserId(userId), "B"));
  }

  public LoanBankAccountVO getLatestUsedBankAccountVO(Long userId, SDKType sdkType) {
    List<LoanBankAccountVO> accountVOS = getAvailableBankAccounts(userId, sdkType);
    if (CollectionUtils.isEmpty(accountVOS)) {
      return null;
    }
    return accountVOS.stream()
        .max(Comparator.comparing(LoanBankAccountVO::getLastTimeUsed))
        .orElse(null);
  }

  private boolean isBankCardUniqueKeyWithBankCode(Long userId) {
    if (!bankConfig.isBankCardUniqueKeyWithBankCode()) {
      return false;
    }

    ExpUser expUser = ExpUser
        .builder()
        .userId(userId)
        .sourceType(ImpliedContextUtils.sourceType())
        .versionBuild(Optional.ofNullable(ImpliedContextUtils.build()).orElse(1L))
        .build();
    String res = expDiversionClient.getResult(ExperimentKeyConstants.BANK_CARD_UNIQUE_KEY_WITH_BANK_CODE, expUser);
    return UserFlowConstants.isExperimentGroup(res);
  }
}
