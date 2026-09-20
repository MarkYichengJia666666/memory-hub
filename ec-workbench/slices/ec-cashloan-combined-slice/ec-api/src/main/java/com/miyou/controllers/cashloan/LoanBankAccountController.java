package com.miyou.controllers.cashloan;

import com.miyou.configure.aop.AuthEcExceptionPointDW;
import com.miyou.controllers.cashloan.response.BindBankInitWritingResponse;
import com.miyou.controllers.cashloan.response.SupportedBankListResponse;
import com.miyou.controllers.cashloan.utilities.BankAccountApiMessageConvertUtil;
import com.miyou.controllers.loan.BaseLoanController;
import com.miyou.controllers.loan.account.request.UploadBankCardInfo;
import com.miyou.controllers.loan.account.request.UploadBankCardWithIdInfo;
import com.miyou.controllers.loan.account.request.VerifyBankAccountInfoRequest;
import com.miyou.controllers.loan.account.response.AddBankAccountNotMatchStrategyResponse;
import com.miyou.controllers.loan.account.response.BankCardCredentialResponse;
import com.yqg.core.service.cashloan.auth.BindCardDelayService;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.miyou.controllers.secure.SecureCheckUserContextFactory;
import com.miyou.controllers.user.BaseViewerDeviceContext;
import com.miyou.utilities.secureapi.ECSecuredApi;
import com.miyou.utilities.secureapi.RequestParser;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.model.sql.bankaccount.enums.BankAccountAvailableStatus;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.mobile.enums.VerificationPurposeType;
import com.yqg.core.service.bankconfig.BankConfigService;
import com.yqg.core.service.bankconfig.vo.BankAttributeVO;
import com.yqg.core.service.bankconfig.vo.BankConfigVO;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.vo.enums.AddBankAccountNotMatchStrategy;
import com.yqg.core.service.cashloan.vo.enums.FinalValidationBankAccountStatus;
import com.yqg.core.service.cashloan.vo.enums.ValidationCardStatus;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.bankaccount.LoanBankConfig;
import com.yqg.core.service.loan.infos.BindBankInitWritingInfo;
import com.yqg.core.service.loan.vo.bankaccount.BindBankAccountResultLogApi;
import com.yqg.core.service.loan.vo.bankaccount.BindBankAccountResultLogVO;
import com.yqg.core.service.loan.vo.bankaccount.BindCardResultVO;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.core.service.mobile.verification.VerificationService;
import com.yqg.core.service.monitor.AuthExceptionMonitor;
import com.yqg.core.service.secure.UserSecureService;
import com.yqg.core.service.secure.check.context.SecureCheckUserContext;
import com.yqg.core.service.secure.enums.SecureCheckAspect;
import com.yqg.core.service.user.UserService;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.core.util.thirdparty.loan.vo.TongDunNodeTriggerParam;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.yqg.core.service.secure.enums.SecureCheckAspect.LIVING_INFO;
import static com.yqg.core.service.secure.enums.SecureCheckAspect.SMS_CODE;
import static com.yqg.core.service.secure.enums.SecureCheckPurpose.MODIFY_BANK_CARD_INFO;

/**
 * Created by xiuqichenyang on 17/7/12.
 */
@RestController
@Slf4j
public class LoanBankAccountController extends BaseLoanController {
  @Autowired
  private LoanBankAccountService loanBankAccountService;
  @Autowired
  private UserService userService;
  @Autowired
  private VerificationService verificationService;
  @Autowired
  private AuthExceptionMonitor authExceptionMonitor;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private UserSecureService userSecureService;
  @Autowired
  private BankConfigService bankConfigService;
  @Autowired
  private LoanBankConfig bankConfig;
  @Autowired
  private SecureCheckUserContextFactory userContextFactory;
  @Autowired
  private BindCardDelayService bindCardDelayService;

  @PostMapping(path = "/api/cashloan/addBankAccount")
  @ResponseBody
  @ECSecuredApi
  public Result addBankAccount(@RequestBody @Valid UploadBankCardInfo request) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    //TODO(shihaoqu, T00000) 后续等前端上了根据pwd加卡的接口时，这块儿需要限制只能在未鉴权完成前才能调用
    try {
      LoanBankAccountVO accountVO = addBankCardOrWarning(viewerContext, request);
      BankConfigVO configVO = bankConfigService.getBankVo(accountVO.bankType);
      authExceptionMonitor.logAuthException(viewerContext.loanAccountId, viewerContext.sdkType, viewerContext.build, "REAL_BIND_BANK_CARD", null);
      return EcResponseUtil.generate(BankCardCredentialResponse.from(accountVO, configVO, true));
    } catch (EcException e) {
      authExceptionMonitor.logAuthException(viewerContext.loanAccountId, viewerContext.sdkType, viewerContext.build, "REAL_BIND_BANK_CARD", e);
      throw e;
    }
  }

  @PostMapping(path = "/api/v2/cashloan/addBankAccount")
  @ResponseBody
  @ECSecuredApi(invalidForFrozenUser = true)
  @AuthEcExceptionPointDW("binding")
  public Result addBankAccountV2(@RequestBody @Valid UploadBankCardInfo request) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    submissionPreCheck(viewerContext);
    BaseViewerDeviceContext viewerDeviceContext = getBaseViewerDeviceContextFromRequest();
    //TODO(shihaoqu, T00000) 后续等前端上了根据pwd加卡的接口时，这块儿需要限制只能在未鉴权完成前才能调用
    try {
      BindCardResultVO bindCardResultVO = loanBankAccountService.addBankAccount(viewerContext.userId,
          viewerContext.sdkType,
          BankType.valueOf(request.bankCode),
          request.cardNumber,
          request.name,
          request.bankAccountType,
          viewerContext.build,
          TongDunNodeTriggerParam.from(viewerContext)
      );

      addOldPageEventTrackLog(
          bindCardResultVO.loanBankAccountVO.bankAccountAvailableStatus,
          bindCardResultVO.validationCardStatus,
          bindCardResultVO.canModifyName,
          bindCardResultVO.loanBankAccountVO.bankAccountId,
          bindCardResultVO.loanBankAccountVO.userId,
          bindCardResultVO.loanBankAccountVO.bankType,
          BindBankAccountResultLogApi.OLD_PAGE_ADD_BANK_ACCOUNT,
          null,
          RequestParser.getRemoteIp(ecRequest()),
          RequestParser.getDeviceToken(ecRequest()),
          RequestParser.getPlatformType(ecRequest()),
          viewerContext.build,
          viewerContext.sdkType,
          viewerDeviceContext.userAgent,
          Objects.nonNull(viewerDeviceContext.environmentInfo) ? viewerDeviceContext.environmentInfo.deviceUniqueId : null,
          viewerContext.loanAccountId
      );

      EcException e = bindCardResultVO.errRemind == null ? null : EcException.warn(EcExceptionType.LOAN_BANK_ACCOUNT_UNAVAILABLE, bindCardResultVO.errRemind);
      authExceptionMonitor.logAuthException(viewerContext.loanAccountId, viewerContext.sdkType, viewerContext.build, "REAL_BIND_BANK_CARD", e);
      return EcResponseUtil.generate(BankCardCredentialResponse.from(bindCardResultVO, true, viewerContext.build));
    } catch (EcException e) {
      authExceptionMonitor.logAuthException(viewerContext.loanAccountId, viewerContext.sdkType, viewerContext.build, "REAL_BIND_BANK_CARD", e);
      addOldPageEventTrackLog(
          null,
          null,
          null,
          null,
          null,
          null,
          BindBankAccountResultLogApi.OLD_PAGE_ADD_BANK_ACCOUNT,
          e.detailTT.toString(),
          RequestParser.getRemoteIp(ecRequest()),
          RequestParser.getDeviceToken(ecRequest()),
          RequestParser.getPlatformType(ecRequest()),
          viewerContext.build,
          viewerContext.sdkType,
          viewerDeviceContext.userAgent,
          Objects.nonNull(viewerDeviceContext.environmentInfo) ? viewerDeviceContext.environmentInfo.deviceUniqueId : null,
          viewerContext.loanAccountId
      );
      throw e;
    }
  }

  @PostMapping(path = "/api/idn/cashloan/addBankAccount")
  @ResponseBody
  @ECSecuredApi(invalidForFrozenUser = true)
  @AuthEcExceptionPointDW("binding")
  public Result addIdnBankAccountV2(@RequestBody @Valid UploadBankCardInfo request) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    BaseViewerDeviceContext viewerDeviceContext = getBaseViewerDeviceContextFromRequest();
    //TODO(shihaoqu, T00000) 后续等前端上了根据pwd加卡的接口时，这块儿需要限制只能在未鉴权完成前才能调用
    try {
      BindCardResultVO bindCardResultVO = loanBankAccountService.addIdnBankAccountV2(viewerContext.userId,
          viewerContext.sdkType,
          BankType.valueOf(request.bankCode),
          request.cardNumber,
          request.name,
          viewerContext.build,
          TongDunNodeTriggerParam.from(viewerContext)
      );

      addNewPageEventTrackLog(
          bindCardResultVO.loanBankAccountVO.bankAccountAvailableStatus,
          bindCardResultVO.finalValidationBankAccountStatus,
          bindCardResultVO.canModifyName,
          bindCardResultVO.loanBankAccountVO.bankAccountId,
          bindCardResultVO.loanBankAccountVO.userId,
          bindCardResultVO.loanBankAccountVO.bankType,
          BindBankAccountResultLogApi.NEW_PAGE_ADD_BANK_ACCOUNT,
          null,
          RequestParser.getRemoteIp(ecRequest()),
          RequestParser.getDeviceToken(ecRequest()),
          RequestParser.getPlatformType(ecRequest()),
          viewerContext.build,
          viewerContext.sdkType,
          viewerDeviceContext.userAgent,
          Objects.nonNull(viewerDeviceContext.environmentInfo) ? viewerDeviceContext.environmentInfo.deviceUniqueId : null,
          viewerContext.loanAccountId
      );
      return EcResponseUtil.generate(BankCardCredentialResponse.fromIdnV2(bindCardResultVO));
    } catch (EcException e) {
      addNewPageEventTrackLog(
          null,
          null,
          null,
          null,
          null,
          null,
          BindBankAccountResultLogApi.NEW_PAGE_ADD_BANK_ACCOUNT,
          e.detailTT.toString(),
          RequestParser.getRemoteIp(ecRequest()),
          RequestParser.getDeviceToken(ecRequest()),
          RequestParser.getPlatformType(ecRequest()),
          viewerContext.build,
          viewerContext.sdkType,
          viewerDeviceContext.userAgent,
          Objects.nonNull(viewerDeviceContext.environmentInfo) ? viewerDeviceContext.environmentInfo.deviceUniqueId : null,
          viewerContext.loanAccountId
      );
      throw e;
    }

  }

  @GetMapping(path = "/api/idn/cashloan/queryBankAccount")
  @ResponseBody
  @ECSecuredApi(invalidForFrozenUser = true)
  @AuthEcExceptionPointDW("binding")
  public Result queryIdnBankAccountV2(@RequestParam(value = "loanBankAccountId", required = false) String accountId) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    BaseViewerDeviceContext viewerDeviceContext = getBaseViewerDeviceContextFromRequest();
    LoanBankAccountVO accountVO = StringUtils.isBlank(accountId) ?
        getLoanAccountVOByUser(viewerContext.userId, viewerContext.sdkType, viewerContext.build, false, viewerDeviceContext.sourceType) : loanBankAccountService.findVOById(YqgHashids.decode(accountId));
    if (accountVO == null) {
      return EcResponseUtil.generate(new BankCardCredentialResponse());
    }
    BindCardResultVO bindCardResultVO = loanBankAccountService.queryIdnValidationResultV2(accountVO, viewerContext.sdkType);

    addNewPageEventTrackLog(
        bindCardResultVO.loanBankAccountVO.bankAccountAvailableStatus,
        bindCardResultVO.finalValidationBankAccountStatus,
        bindCardResultVO.canModifyName,
        bindCardResultVO.loanBankAccountVO.bankAccountId,
        bindCardResultVO.loanBankAccountVO.userId,
        bindCardResultVO.loanBankAccountVO.bankType,
        BindBankAccountResultLogApi.NEW_PAGE_QUERY_BANK_ACCOUNT,
        null,
        RequestParser.getRemoteIp(ecRequest()),
        RequestParser.getDeviceToken(ecRequest()),
        RequestParser.getPlatformType(ecRequest()),
        viewerContext.build,
        viewerContext.sdkType,
        viewerDeviceContext.userAgent,
        Objects.nonNull(viewerDeviceContext.environmentInfo) ? viewerDeviceContext.environmentInfo.deviceUniqueId : null,
        viewerContext.loanAccountId
    );

    return EcResponseUtil.generate(BankCardCredentialResponse.fromIdnV2(bindCardResultVO));
  }

  private LoanBankAccountVO addBankCardOrWarning(LoanApiViewerContext viewerContext, UploadBankCardInfo request) {
    return loanBankAccountService.addBankAccountOrWarningV2(viewerContext.userId,
        viewerContext.sdkType,
        BankType.valueOf(request.bankCode),
        request.cardNumber,
        request.name,
        request.bankAccountType,
        viewerContext.build
    );
  }

  @PostMapping(path = "/api/v3/cashloan/addBankAccountWithIdNumber")
  @ResponseBody
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result addBankAccountWithIdNumberV3(@RequestBody @Valid UploadBankCardWithIdInfo request) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    checkForBindCard(request, viewerContext);

    BindCardResultVO bindCardResultVO = loanBankAccountService.addBankAccount(viewerContext.userId,
        viewerContext.sdkType,
        BankType.valueOf(request.bankCode),
        request.cardNumber,
        request.name,
        request.bankAccountType,
        viewerContext.build,
        TongDunNodeTriggerParam.from(viewerContext));

    return EcResponseUtil.generate(BankCardCredentialResponse.from(bindCardResultVO, true, viewerContext.build));
  }

  private void checkForBindCard(UploadBankCardWithIdInfo request, LoanApiViewerContext viewerContext) {
    userSecureService.skipFaceComparisonIfUnavailable(userContextFactory.createUserContextForLogin(ecRequest(), MODIFY_BANK_CARD_INFO, LIVING_INFO));
    verificationService.checkMobileVerificationCode(viewerContext.normalizedMobileNumber, request.verifyCode, VerificationPurposeType.BANK, viewerContext.sdkType);

    SecureCheckUserContext userContext = userContextFactory.createUserContextForLogin(ecRequest(), MODIFY_BANK_CARD_INFO, SMS_CODE);
    userSecureService.finishAspect(userContext);
    userSecureService.isSecureCheckFinishedForOldVersion(userContext, Collections.singletonList(SecureCheckAspect.SMS_CODE));

    userService.checkIdentityNumberByNormalizedMobileNumber(viewerContext.normalizedMobileNumber,
        request.identityNumber,
        request.documentType,
        viewerContext.sdkType);
  }

  @GetMapping(path = "/api/v2/cashloan/addBankAccountNotMatchStrategy")
  @ResponseBody
  @ECSecuredApi
  public Result getAddBankAccountNotMatchStrategy() {
    AddBankAccountNotMatchStrategy strategy = loanBankAccountService.getAddBankAccountNotMatchStrategy();
    return EcResponseUtil.generate(AddBankAccountNotMatchStrategyResponse.from(strategy));
  }

  @GetMapping(path = "/api/cashloan/getSupportedBankV2")
  @ResponseBody
  @ECSecuredApi
  public Result getSupportedBankListWithLogo() {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    if (viewerContext.sdkType == null) {
      throw EcException.error("sdk is null");
    }
    List<BankConfigVO> bankList = loanBankAccountService.getSupportedBankVOListALL(viewerContext.sdkType);
    bankList = loanBankAccountService.filterBankTypeByABTest(bankList, viewerContext.userId);

    Map<BankType, BankAttributeVO> bankTypeToBankAttributeVOMap = bankConfig.getBankAttributeVOMap();
    SupportedBankListResponse response = SupportedBankListResponse.from(
        viewerContext.sdkType,
        cashLoanConfig.getCommonBanksNumbers(viewerContext.sdkType),
        bankList,
        bankTypeToBankAttributeVOMap,
        true);
    return EcResponseUtil.generate(response);
  }

  @GetMapping(path = "/api/idn/cashloan/getSupportedBank")
  @ResponseBody
  @ECSecuredApi
  public Result getIdnSupportedBankListWithLogo() {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    if (viewerContext.sdkType == null) {
      throw EcException.error("sdk is null");
    }
    List<BankConfigVO> bankList = loanBankAccountService.getSupportedBankVOList(viewerContext.sdkType);
    Map<BankType, BankAttributeVO> bankTypeToBankAttributeVOMap = bankConfig.getBankAttributeVOMap();
    bankList = loanBankAccountService.keepOnlySuperBankIfInExperiment(bankList,viewerContext.userId,viewerContext.build,viewerContext.deviceToken);
    bankTypeToBankAttributeVOMap = loanBankAccountService.clearPopWindowTextIfInExperiment(bankTypeToBankAttributeVOMap, viewerContext.userId, viewerContext.build, viewerContext.sourceType, viewerContext.loanAccountId);
    SupportedBankListResponse response = SupportedBankListResponse.from(
        viewerContext.sdkType,
        cashLoanConfig.getCommonBanksNumbers(viewerContext.sdkType),
        bankList,
        bankTypeToBankAttributeVOMap,
        true);
    return EcResponseUtil.generate(response);
  }

  @GetMapping(path = "/api/idn/cashloan/getSupportedEwallet")
  @ResponseBody
  @ECSecuredApi
  public Result getIdnSupportedEwalletListWithLogo() {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    if (viewerContext.sdkType == null) {
      throw EcException.error("sdk is null");
    }
    List<BankConfigVO> bankList = loanBankAccountService.getSupportedBankVOListEWallet(viewerContext.sdkType);
    if (viewerContext.sdkType.isIdnSDKType()) {
      bankList = loanBankAccountService.filterEWalletTypeByABTest(bankList, viewerContext.userId);
    }
    Map<BankType, BankAttributeVO> bankTypeToBankAttributeVOMap = bankConfig.getBankAttributeVOMap();
    SupportedBankListResponse response = SupportedBankListResponse.from(
        viewerContext.sdkType,
        cashLoanConfig.getCommonBanksNumbers(viewerContext.sdkType),
        bankList,
        bankTypeToBankAttributeVOMap,
        false);
    return EcResponseUtil.generate(response);
  }

  @GetMapping(path = "/api/idn/cashloan/getInitCopyWriting")
  @ResponseBody
  public Result getIdnInitCopyWriting() {
    LoanApiViewerContext context = getViewerContextFromRequest();
    BindBankInitWritingInfo bindBankInitText = bankConfig.getBindBankInitText();
    boolean bankAccountDelay = false;
    BindBankInitWritingResponse result = BankAccountApiMessageConvertUtil.convertBindBankText(bindBankInitText, bankAccountDelay);
    return EcResponseUtil.generate(result);
  }

  @GetMapping(path = "/api/cashloan/queryBankAccount")
  @ResponseBody
  @ECSecuredApi
  public Result queryLoanBankAccountVO(@RequestParam("loanBankAccountId") String accountId) {
    LoanBankAccountVO accountVO = loanBankAccountService.findVOById(YqgHashids.decode(accountId));
    BankConfigVO configVO = bankConfigService.getBankVo(accountVO.bankType);
    return EcResponseUtil.generate(BankCardCredentialResponse.from(accountVO, configVO, true));
  }

  @ECSecuredApi
  @GetMapping("/api/cashloan/queryBankAccountByUser")
  @ResponseBody
  public Result queryBankAccountByUser() {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    LoanBankAccountVO accountVO = getLoanAccountVOByUser(viewerContext.userId, viewerContext.sdkType, viewerContext.build, false, viewerContext.sourceType);
    if (accountVO == null) {
      return EcResponseUtil.generate(new BankCardCredentialResponse());
    }
    return EcResponseUtil.generate(BankCardCredentialResponse.from(accountVO, true));
  }

  @GetMapping(path = "/api/v2/cashloan/queryBankAccount")
  @ResponseBody
  @ECSecuredApi(invalidForFrozenUser = true)
  @AuthEcExceptionPointDW("binding")
  public Result queryLoanBankAccountByIdOrUser(@RequestParam(value = "loanBankAccountId", required = false) String accountId) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    BaseViewerDeviceContext viewerDeviceContext = getBaseViewerDeviceContextFromRequest();
    LoanBankAccountVO accountVO = StringUtils.isBlank(accountId) ?
        getLoanAccountVOByUser(viewerContext.userId, viewerContext.sdkType, viewerContext.build, true, viewerContext.sourceType) : loanBankAccountService.findVOById(YqgHashids.decode(accountId));
    if (accountVO == null) {
      return EcResponseUtil.generate(new BankCardCredentialResponse());
    }
    BindCardResultVO bindCardResultVO = loanBankAccountService.queryBankValidationResult(accountVO, viewerContext.sdkType);
    addOldPageEventTrackLog(
        bindCardResultVO.loanBankAccountVO.bankAccountAvailableStatus,
        bindCardResultVO.validationCardStatus,
        bindCardResultVO.canModifyName,
        bindCardResultVO.loanBankAccountVO.bankAccountId,
        bindCardResultVO.loanBankAccountVO.userId,
        bindCardResultVO.loanBankAccountVO.bankType,
        BindBankAccountResultLogApi.OLD_PAGE_QUERY_BANK_ACCOUNT,
        null,
        RequestParser.getRemoteIp(ecRequest()),
        viewerDeviceContext.deviceToken,
        viewerDeviceContext.platform.name(),
        viewerContext.build,
        viewerContext.sdkType,
        viewerDeviceContext.userAgent,
        Objects.nonNull(viewerDeviceContext.environmentInfo) ? viewerDeviceContext.environmentInfo.deviceUniqueId : null,
        viewerContext.loanAccountId
    );
    return EcResponseUtil.generate(BankCardCredentialResponse.from(bindCardResultVO, true, viewerContext.build));
  }

  private void addOldPageEventTrackLog(
      BankAccountAvailableStatus bankAccountAvailableStatus,
      ValidationCardStatus oldPageStatus,
      Boolean canModifyName,
      Long bankAccountId,
      Long userId,
      BankType bankType,
      BindBankAccountResultLogApi api,
      String exceptionMessage,
      String ip,
      String deviceToken,
      String platformType,
      Long build,
      SDKType sdkType,
      String userAgent,
      String deviceId,
      Long loanAccountId) {
    if (!sdkType.isIdnSDKType()) {
      return;
    }
    DwLogUtil.log(LogBusinessType.BIND_BANK_ACCOUNT_RESULT,
        BindBankAccountResultLogVO.fromOldPage(
            bankAccountAvailableStatus,
            oldPageStatus,
            canModifyName,
            bankAccountId,
            userId,
            bankType,
            api,
            null,
            ip,
            deviceToken,
            platformType,
            build,
            sdkType,
            userAgent,
            deviceId,
            loanAccountId
        ));
  }

  private void addNewPageEventTrackLog(
      BankAccountAvailableStatus bankAccountAvailableStatus,
      FinalValidationBankAccountStatus finalValidationBankAccountStatus,
      Boolean canModifyName,
      Long bankAccountId,
      Long userId,
      BankType bankType,
      BindBankAccountResultLogApi api,
      String exceptionMessage,
      String ip,
      String deviceToken,
      String platformType,
      Long build,
      SDKType sdkType,
      String userAgent,
      String deviceId,
      Long loanAccountId) {
    if (!sdkType.isIdnSDKType()) {
      return;
    }

    DwLogUtil.log(LogBusinessType.BIND_BANK_ACCOUNT_RESULT,
        BindBankAccountResultLogVO.fromNewPage(
            bankAccountAvailableStatus,
            finalValidationBankAccountStatus,
            canModifyName,
            bankAccountId,
            userId,
            bankType,
            api,
            null,
            ip,
            deviceToken,
            platformType,
            build,
            sdkType,
            userAgent,
            deviceId,
            loanAccountId
        ));
  }

  private LoanBankAccountVO getLoanAccountVOByUser(Long userId, SDKType sdkType, Long build, Boolean availableBankAccountRoute, SourceType sourceType) {
    List<LoanBankAccountVO> loanBankAccountVOS = loanBankAccountService.getBankAccounts(userId, sdkType);
    List<BankType> illegalEWalletBankTypeList = bankConfig.getIllegalEWalletBankType();
    //忽略无效的卡
    loanBankAccountVOS = loanBankAccountVOS.stream()
        .filter(vo -> !BankAccountAvailableStatus.INACTIVE_STATUS_LIST.contains(vo.bankAccountAvailableStatus))
        .collect(Collectors.toList());
    if (loanBankAccountService.routePaymentCredentialStrategy(userId, sdkType, build, sourceType).isStrategyB() && availableBankAccountRoute) {
      loanBankAccountVOS = loanBankAccountVOS.stream()
          .filter(vo -> !illegalEWalletBankTypeList.contains(vo.bankType))
          .collect(Collectors.toList());
    }
    return CollectionUtils.isNotEmpty(loanBankAccountVOS) ? loanBankAccountVOS.get(0) : null;
  }

  @PutMapping(path = "/api/cashloan/verifyBankAccount")
  @ResponseBody
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result verifyBankAccount(@RequestParam(value = "bankAccountId", required = false) String bankAccountId) {
    if (StringUtils.isBlank(bankAccountId)) {
      throw EcException.error("bankAccountId不能为空");
    }
    LoanBankAccountVO accountVO = loanBankAccountService.verifyBankAccount(YqgHashids.decode(bankAccountId));
    return EcResponseUtil.generate(accountVO);
  }

  @PostMapping(path = "/api/cashloan/verifyBankAccountInfo")
  @ResponseBody
  @ECSecuredApi
  public Result verifyBankAccountInfo(@RequestBody @Valid VerifyBankAccountInfoRequest request) {
    LoanBankAccountVO accountVO = loanBankAccountService.verifyBankAccount(YqgHashids.decode(request.bankAccountId));
    return EcResponseUtil.generate(BankCardCredentialResponse.from(accountVO));
  }
}
