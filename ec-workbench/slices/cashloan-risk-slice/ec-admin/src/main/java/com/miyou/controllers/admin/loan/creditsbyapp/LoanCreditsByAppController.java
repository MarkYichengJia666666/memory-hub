package com.miyou.controllers.admin.loan.creditsbyapp;

import com.google.common.collect.Lists;
import com.miyou.controllers.admin.loan.creditsbyapp.request.*;
import com.miyou.controllers.admin.loan.creditsbyapp.response.*;
import com.miyou.controllers.admin.loan.loanaccount.response.BankCardCredentialResponse;
import com.miyou.controllers.core.YqgBaseController;
import com.miyou.utilities.Utilities;
import com.miyou.utilities.datasecurity.DataSecurityUtils;
import com.miyou.utilities.datasecurity.enums.SceneTag;
import com.yqg.core.model.core.OperationLogRecordVo;
import com.yqg.core.model.mongo.VO.MongoLoanAccountExtraInfoPojo;
import com.yqg.core.model.sql.adminuser.AdminUserModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.enums.AuthStep;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsRejectedReason;
import com.yqg.core.service.cashloan.CashLoanAuthConfig;
import com.yqg.core.service.cashloan.CashLoanCreditsService;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.cashloan.RiskFeatureDataService;
import com.yqg.core.service.cashloan.auth.AuthService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.upload.UploadInformationService;
import com.yqg.core.service.cashloan.vo.LoanUserCreditsLogVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.LoanUserTypeService;
import com.yqg.core.service.loan.additioanalinfo.LoanUserAdditionalInfoService;
import com.yqg.core.service.loan.credits.CreditsStatusChangeSource;
import com.yqg.core.service.loan.extrainfo.LoanAccountExtraInfoService;
import com.yqg.core.service.loan.vo.*;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsVO;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.core.service.ly.AuditTrailActivity;
import com.yqg.core.service.ly.AuditTrailCopy;
import com.yqg.core.service.ly.LyConfig;
import com.yqg.core.service.ly.SystemOperationAuditTrailSupport;
import com.yqg.core.util.ioc.SpringUtils;
import com.yqg.core.service.notifblacklist.BlacklistConfig;
import com.yqg.core.service.notifblacklist.NotifBlacklistService;
import com.yqg.core.service.payment.ICredential;
import com.yqg.core.service.payment.PaymentCredential;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.reupload.RiskReuploadService;
import com.yqg.core.service.reupload.vo.RiskReuploadInfoPojo;
import com.yqg.core.service.risk.RiskEngineService;
import com.yqg.core.service.risk.RiskFlowConfig;
import com.yqg.core.service.risk.RiskFlowTraceRunType;
import com.yqg.core.service.risk.RiskKey;
import com.yqg.core.service.risk.facade.RiskFacadeService;
import com.yqg.core.service.risk.facade.RiskFacadeTool;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.provider.manualauth.ManualAuthService;
import com.yqg.core.service.risk.provider.manualauth.vo.AuthInfoVO;
import com.yqg.core.service.risk.riskcase.RiskCaseService;
import com.yqg.core.service.risk.riskcase.vo.UserRiskCaseOperationLogVO;
import com.yqg.core.service.risk.risklevel.RiskUserLevelMappingService;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.user.UserMobileChangeService;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.AccountChangeHistoryVO;
import com.yqg.core.service.user.vo.UsedAccountListResponse;
import com.yqg.core.service.user.vo.UsedAccountVO;
import com.yqg.data_security.mask.constant.MaskType;
import com.yqg.ec.common.enums.Label;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.TagOperatorTypeEnum;
import com.yqg.ec.common.enums.risk.CreditsInfoDisplayContext;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.overseasrisk.client.mesh.api.IRiskFlowExtraService;
import com.yqg.overseasrisk.client.mesh.api.ruleenginereborn.IEngineRiskFlowService;
import com.yqg.overseasrisk.common.mvc.riskflow.DryRunRiskFlowLoanAccountVO;
import com.yqg.overseasrisk.common.mvc.riskflow.DryRunRiskFlowUserInfoVO;
import com.yqg.overseasrisk.common.mvc.ruleenginereborn.trace.RiskRuleSetResultRequest;
import com.yqg.translation.client.utils.TT;
import com.yqg.translation.common.enums.TranslationLocale;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

import static com.yqg.core.service.risk.risklevel.RiskUserLevelMappingService.LOAN_MAPPING_USER_LEVEL_TYPES;
import static com.yqg.core.service.risk.risklevel.RiskUserLevelMappingService.REJECT_BEFORE_GRADING;

/**
 * Created by yanke on 4/21/16.
 *
 * @title 用户授信信息查询
 */

@Slf4j
@RestController
public class LoanCreditsByAppController extends YqgBaseController {
  @Autowired
  private AuthService authService;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private RiskFacadeTool riskFacadeTool;
  @Autowired
  private RiskFacadeService riskFacadeService;
  @Autowired
  private AdminUserModel adminUserModel;
  @Autowired
  private CashLoanCreditsService cashLoanCreditsService;
  @Autowired
  private RiskEngineService riskEngineService;
  @Autowired
  private RiskReuploadService riskReuploadService;
  @Autowired
  private LoanUserAdditionalInfoService userAdditionalInfoService;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private LoanUserTypeService loanUserTypeService;
  @Autowired
  private LyConfig lyConfig;
  @Autowired
  private UploadInformationService uploadInformationService;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;
  @Autowired
  private LoanAccountExtraInfoService loanAccountExtraInfoService;
  @Autowired
  private BlacklistConfig blacklistConfig;
  @Autowired
  private NotifBlacklistService notifBlacklistService;
  @Autowired
  private UserService userService;
  @Autowired
  private IRiskFlowExtraService iRiskFlowExtraService;
  @Autowired
  private UserMobileChangeService userMobileChangeService;
  @Autowired
  private RiskUserLevelMappingService riskUserLevelMappingService;
  @Autowired
  private RiskCaseService riskCaseService;
  @Autowired
  private RiskFeatureDataService riskFeatureDataService;
  @Autowired
  private IEngineRiskFlowService engineRiskFlowService;
  @Autowired
  private RiskFlowConfig riskFlowConfig;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private ManualAuthService manualAuthService;
  @Autowired
  private CashLoanAuthConfig cashLoanAuthConfig;

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.QUERY')")
  @PostMapping(path = "/admin/operation/loan/listCreditsInfoByAppV2")
  public Result listCreditsInfoByAppV2(@RequestBody @Valid ListCreditsInfoByAppRequestV2 request) {
    return getNewCreditsInfoResult(request);
  }

  private Result getNewCreditsInfoResult(ListCreditsInfoByAppRequestV2 request) {
    List<Long> accountIds = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(request.mobileNumbers)) {
      List<Long> userIds = userService.fetchIdByNormalizedMobiles(request.mobileNumbers);
      List<Long> loanAccountIds = loanAccountService.fetchLoanAccountIdsByUserIdsAndSDK(userIds, request.sdkType);
      accountIds.addAll(loanAccountIds);
    }
    if (CollectionUtils.isNotEmpty(request.loanAccountId)) {
      accountIds.addAll(request.loanAccountId);
    }

    //对于mobileNumbers或loanAccountId的查询，不受时间约束
    if (CollectionUtils.isNotEmpty(request.mobileNumbers) || CollectionUtils.isNotEmpty(request.loanAccountId)) {
      //当mobileNumbers、loanAccountId有值，但是未成功筛选到用户，则直接返回空
      if (CollectionUtils.isEmpty(accountIds)) {
        ListCreditsInfoByAppResponse response = new ListCreditsInfoByAppResponse();
        response.totalCount = 0;
        return EcResponseUtil.generate(response);
      }
      request.startTime = null;
      request.endTime = null;
    }
    boolean isLyAdminUser = lyConfig.isLyAdminUser(getAdminUserEmail());
    Set<Long> ruleSetIdWhiteSet = new HashSet<>(lyConfig.getLyRuleSetIdWhiteList());
    List<LoanCreditsInfoByAppSearchResult> loanCreditsInfoByAppSearchResults = loanAccountService.searchUserCreditsInfoByConditions(
        request.context,
        accountIds,
        request.startTime,
        request.endTime,
        request.creditsStatus,
        request.pageSize * (request.pageNo - 1),
        request.pageSize,
        request.sdkType,
        isLyAdminUser,
        ruleSetIdWhiteSet);
    ListCreditsInfoByAppResponse response = genListCreditsInfoResponse(loanCreditsInfoByAppSearchResults);
    response.totalCount = loanAccountService.searchUserCreditsInfoCountByConditions(request.context,
        accountIds,
        request.startTime,
        request.endTime,
        request.creditsStatus
    );
    return EcResponseUtil.generate(response);
  }

  @NotNull
  private ListCreditsInfoByAppResponse genListCreditsInfoResponse(List<LoanCreditsInfoByAppSearchResult> loanCreditsInfoByAppSearchResults) {
    loanCreditsInfoByAppSearchResults = filterDeletedUser(loanCreditsInfoByAppSearchResults);

    ListCreditsInfoByAppResponse response = new ListCreditsInfoByAppResponse();
    response.addCreditsInfo(loanCreditsInfoByAppSearchResults);
    return response;
  }

  private List<LoanCreditsInfoByAppSearchResult> filterDeletedUser(List<LoanCreditsInfoByAppSearchResult> results) {
    Set<Long> uidSet = results.stream().map(r -> r.userId).collect(Collectors.toSet());
    Set<Long> deletedUserIds = loanAccountModel.findDeletedUserIds(uidSet);
    return results.stream()
        .filter(r -> !deletedUserIds.contains(r.userId))
        .collect(Collectors.toList());
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.QUERY')")
  @GetMapping(path = "/admin/operation/loan/listReloanCreditsInfo")
  public Result listReloanCreditsInfo(
      @RequestParam("pageNo") Integer pageNo,
      @RequestParam("pageSize") Integer pageSize,
      @RequestParam(value = "creditsStatus", required = false) String creditsStatus
  ) {
    throw EcException.error("current api do not use,if have some problem, inform boshu");
  }

  @PreAuthorize("hasAnyAuthority('LOAN.USER.QUERY.CREDITS_INFO')")
  @GetMapping(path = "/admin/operation/loan/creditsInfoByApp")
  public Result getCreditsInfoByApp(
      @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
      @RequestParam(value = "loanAccountId", required = false) Long loanAccountId,
      @RequestParam(value = "email", required = false) String emailAddress
  ) {
    try {
      LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(mobileNumber, loanAccountId, emailAddress, getSDKType());
      LoanUserCreditsInfoVO loanUserCreditsInfoVO = loanAccountService.getCreditsInfo(loanAccountVO.id);
      GetCreditsInfoByAppResponse response = GetCreditsInfoByAppResponse.from(loanUserCreditsInfoVO);
      return EcResponseUtil.generate(response);
    } catch (EcException exception) {
      throw generateWarningExceptionIfUserNotExist(exception);
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.PROCESS')")
  @PutMapping(path = "/admin/operation/loan/forceReject")
  public Result forceRejectCreditsApplication(@RequestBody @Valid ForceRejectCreditsApplicationRequest request) {
    ForceRejectCreditsApplicationResponse response = new ForceRejectCreditsApplicationResponse();
    for (Long loanAccountId : request.loanAccountIds) {
      try {
        loanAccountService.forceRejectCreditsApplication(loanAccountId, LoanCreditsRejectedReason.UNKNOWN, request.changeReason);
        response.success.add(loanAccountId);
      } catch (Exception e) {
        log.warn("强行拒绝授信失败:" + loanAccountId, e);
        response.failure.add(loanAccountId);
      }
    }
    return EcResponseUtil.generate(response);
  }

  //TODO 不再使用，待删除
  @Deprecated
  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.PROCESS')")
  @PostMapping(path = "/admin/operation/loan/updateReloanCreditsInfo")
  public Result updateReloanCreditsInfo(@RequestBody @Valid UpdateReloanCreditsInfoRequest request) {
    LoanAccountVO loanAccountVO = loanAccountService.getAccountByNormalizedMobileNumberOrThrow(request.mobileNumber, getSDKType());
    switch (request.creditsStatus) {
      //通过授信
      case ACCEPTED:
        try {
          cashLoanCreditsService.acceptReloanCreditsApplicationFromAdmin(loanAccountVO.id, request.changeReason);
        } catch (Exception e) {
          log.warn("hit error when accepting credits by admin source loanAccountId = {}", loanAccountVO.id);
          throw EcException.wrap(e);
        }
        break;
      //拒绝授信
      case REJECTED:
        try {
          cashLoanCreditsService.rejectReloanCreditsApplication(loanAccountVO.id, LoanCreditsRejectedReason.UNKNOWN, CreditsStatusChangeSource.ADMIN, request.changeReason, null);
        } catch (Exception e) {
          log.warn("hit error when rejecting credits by admin source loanAccountId = {}", loanAccountVO.id);
          throw EcException.wrap(e);
        }
        break;
      default:
        throw EcException.error(EcExceptionType.COMMON_ILLEGAL_PARAM, "updateReloanCreditsInfo received an unexpected creditsStatus.");
    }
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.PROCESS')")
  @PostMapping(path = "/admin/operation/loan/batchCancelCreditsByApp")
  public Result batchCancelCreditsByApp(@RequestBody @Valid BatchCancelCreditsRequest request) {
    List<String> loanAccountIdStrings = new LinkedList<>(Arrays.asList(request.loanAccountIds.split(",")));
    List<Long> loanAccountIdsOriginal = new LinkedList<>();

    for (String loanAccountIdString : loanAccountIdStrings) {
      try {
        loanAccountIdsOriginal.add(Long.valueOf(loanAccountIdString));
      } catch (Exception e) {
        log.warn("{} cannot be parsed into a valid loanAccount Id", loanAccountIdString);
      }
    }
    List<Long> failedIds = new LinkedList<>();
    for (Long loanAccountId : loanAccountIdsOriginal) {
      try {
        LoanUserRiskTraceVO traceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(loanAccountId);
        Boolean sendNotification = !LoanUserRiskType.getBatchTriggerCreditsRiskTypes().contains(traceVO.riskType);
        cashLoanCreditsService.cancelCreditsApplication(loanAccountId, request.reason, CreditsStatusChangeSource.BATCH, sendNotification);
      } catch (Exception e) {
        log.warn("取消授信失败, accountId: {}", loanAccountId, e);
        failedIds.add(loanAccountId);
        continue;
      }
      publishAuditTrail(AuditTrailActivity.CREDIT_REJECTION, AuditTrailCopy.CREDIT_REJECTION_DESC,
          AuditTrailCopy.creditRejectionContent(loanAccountId));
    }
    List<Long> successIds = loanAccountIdsOriginal.stream()
        .filter(id -> !failedIds.contains(id))
        .collect(Collectors.toList());
    SpringUtils.getInstance(SystemOperationAuditTrailSupport.class)
        .recordCancelCredits(getAdminUserEmail(), successIds);
    return EcResponseUtil.generate(failedIds);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.QUERY')")
  @GetMapping(path = "/admin/operation/loan/countCreditsInfoByApp")
  public Result countCreditsInfoByApp() {
    throw EcException.error("current api do not use,if have some problem, inform boshu");
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.QUERY')")
  @GetMapping(path = "/admin/operation/loan/countCreditsInfoByUserType")
  public Result countCreditsInfoByUserType(
      @RequestParam(value = "context", defaultValue = "LOAN") CreditsInfoDisplayContext context
  ) {
    throw EcException.error("current api do not use,if have some problem, inform boshu");
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.CREATE')")
  @PostMapping(path = "/admin/operation/loan/dryRunRiskFlow")
  public Result dryRun(@RequestBody @Valid RiskFlowDryRunRequest request) {
    return EcResponseUtil.generate(iRiskFlowExtraService.dryRun(getRiskFlowDryRunRequest(request)));
  }

  private com.yqg.overseasrisk.common.mvc.riskflow.RiskFlowDryRunRequest getRiskFlowDryRunRequest(RiskFlowDryRunRequest request) {
    com.yqg.overseasrisk.common.mvc.riskflow.RiskFlowDryRunRequest riskFlowDryRunRequest = new com.yqg.overseasrisk.common.mvc.riskflow.RiskFlowDryRunRequest();
    String[] loanAccountIdStrs = request.loanAccountIds.split(",");
    Set<Long> loanAccountIds = new HashSet<>();
    for (String loanAccountIdStr : loanAccountIdStrs) {
      loanAccountIds.add(Long.valueOf(loanAccountIdStr));
    }

    Map<Long, UserInfoVO> userInfoVOMap = userAdditionalInfoService.genLoanUserInfoVOMap(loanAccountIds);
    Map<Long, DryRunRiskFlowUserInfoVO> dryRunUserInfoVOMap = new HashMap<>();
    for (Long userId : userInfoVOMap.keySet()) {
      dryRunUserInfoVOMap.put(userId, from(userInfoVOMap.get(userId)));
    }
    Map<Long, LoanAccountVO> loanAccountVOMap = loanAccountService.getAccounts(loanAccountIds);
    Map<Long, DryRunRiskFlowLoanAccountVO> dryRunLoanAccountVOMap = new HashMap<>();
    for (Long loanAccountId : loanAccountVOMap.keySet()) {
      dryRunLoanAccountVOMap.put(loanAccountId, from(loanAccountVOMap.get(loanAccountId)));
    }
    riskFlowDryRunRequest.loanAccountIds = loanAccountIds;
    riskFlowDryRunRequest.riskFlowId = request.riskFlowId;
    riskFlowDryRunRequest.userInfoVOMap = dryRunUserInfoVOMap;
    riskFlowDryRunRequest.loanAccountVOMap = dryRunLoanAccountVOMap;

    return riskFlowDryRunRequest;
  }

  private DryRunRiskFlowLoanAccountVO from(LoanAccountVO vo) {
    DryRunRiskFlowLoanAccountVO dryRunRiskFlowLoanAccountVO = new DryRunRiskFlowLoanAccountVO();
    dryRunRiskFlowLoanAccountVO.id = vo.id;
    dryRunRiskFlowLoanAccountVO.name = vo.name;
    dryRunRiskFlowLoanAccountVO.identityNumber = vo.identityNumber;
    dryRunRiskFlowLoanAccountVO.mobileNumber = vo.mobileNumber;
    return dryRunRiskFlowLoanAccountVO;
  }

  private DryRunRiskFlowUserInfoVO from(UserInfoVO vo) {
    DryRunRiskFlowUserInfoVO runRiskFlowUserInfoVO = new DryRunRiskFlowUserInfoVO();
    runRiskFlowUserInfoVO.name = vo.name;
    runRiskFlowUserInfoVO.normalizedMobileNumber = vo.normalizedMobileNumber;
    return runRiskFlowUserInfoVO;
  }

  /**
   * @param encryptedMobileNumber
   * @param mobileNumber
   * @param loanAccountId
   * @param emailAddress
   * @desc 用户详情-完件信息查询
   * @responseClass com.yqg.ec.common.spring.response.EcResponse<com.miyou.controllers.admin.loan.creditsbyapp.response.GetAuthInfoResponse>
   */
  @PreAuthorize("hasAnyAuthority('LOAN.USER.QUERY.AUTH_INFO')")
  @GetMapping(path = "/admin/operation/loan/authInfo")
  public Result getAuthInfo(
      @RequestParam(value = "encryptedMobileNumber", required = false) String encryptedMobileNumber,
      @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
      @RequestParam(value = "loanAccountId", required = false) Long loanAccountId,
      @RequestParam(value = "email", required = false) String emailAddress
  ) {
    try {
      GetAuthInfoResponse response = new GetAuthInfoResponse();
      mobileNumber = Objects.nonNull(mobileNumber) ? mobileNumber : (Objects.nonNull(encryptedMobileNumber) ? Utilities.decryptMobileNumber(encryptedMobileNumber) : null);
      LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(mobileNumber, loanAccountId, emailAddress, getSDKType());
      assetLoanAccountValid(loanAccountVO, getAdminUserEmail());
      LoanAccountDetailsVO loanAccountDetailsVO = loanAccountDetailsService.getExpandedLoanAccountDetailsVOByLoanAccountId(loanAccountVO.id);

      boolean isLyDemoAdminUser = lyConfig.isLyDemoAdminUser(getAdminUserEmail());
      if (isLyDemoAdminUser) {
        loanAccountDetailsVO = loanAccountDetailsService.adjustLoanAccountDetailsVOForLy(loanAccountDetailsVO);
      }
      RiskReuploadInfoPojo reUploadInfoPojo = riskReuploadService.getLatestProcessedPojoBeforeTime(loanAccountVO.id, Clock.now());
      AuthInfoVO authInfoVO = manualAuthService.getAuthInfoByUserId(loanAccountVO.userId, loanAccountVO.sdkType);
      response.sdkType = loanAccountVO.sdkType;
      response.authInfo = AuthInfoResponse.from(loanAccountDetailsVO, reUploadInfoPojo, loanAccountVO.sdkType, authInfoVO);
      response.accountStatus = loanAccountVO.loanAccountStatus;
      Long blackListSourceId = blacklistConfig.getAppUserRefuseNotifSource();
      response.acceptPromotion = !notifBlacklistService.existBlacklistNormalizedMobileNumber(loanAccountVO.normalizedMobileNumber, loanAccountVO.sdkType.sdkPackage, Collections.singletonList(blackListSourceId));

      //TODO(yuchenghuang, T36000) admin忽略，后面需要sourceType再加
      PaymentCredential paymentCredential = authService.getAuthCredential(loanAccountVO.userId, loanAccountVO.sdkType);
      if (paymentCredential != null) {
        ICredential credentialVO = PaymentMethod.getMethodInstance(paymentCredential.getMethod()).getCredential(paymentCredential.getId());
        BankCardCredentialResponse bankCardCredentialResponse = BankCardCredentialResponse.from((LoanBankAccountVO) credentialVO, true);
        bankCardCredentialResponse.accountNumber = DataSecurityUtils.encryptAndMask(bankCardCredentialResponse.accountNumber, SceneTag.AUTH_INFO_SAVING_CARD_NUMBER, MaskType.BANK_CARD);
        response.authInfo.credential = bankCardCredentialResponse;
      }

      overrideLoanUseForWhitelistAccount(loanAccountVO.id, response.authInfo);

      return EcResponseUtil.generate(response);
    } catch (EcException exception) {
      throw generateWarningExceptionIfUserNotExist(exception);
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.USER.QUERY.EXTRA_INFO')")
  @GetMapping(path = "/admin/operation/loan/extraInfo")
  public Result getExtraInfo(
      @RequestParam(value = "loanAccountId") Long loanAccountId
  ) {
    try {
      LoanAccountVO loanAccountVO = loanAccountService.checkAndGetLoanAccountVO(loanAccountId);
      MongoLoanAccountExtraInfoPojo extraInfoPojo = loanAccountExtraInfoService.getAllExtraInfo(loanAccountVO.id);
      extraInfoPojo.whatsAppNumber = DataSecurityUtils.encryptAndMask(extraInfoPojo.whatsAppNumber, SceneTag.SUPPLEMENT_INFO_WHATSAPP_NUMBER, MaskType.MOBILE_NUMBER);
      extraInfoPojo.companyName = DataSecurityUtils.encryptAndMask(extraInfoPojo.companyName, SceneTag.SUPPLEMENT_INFO_COMPANY_NAME, MaskType.COMPANY_NAME);
      ExtraInfoDisplayResponse response = ExtraInfoDisplayResponse.from(
          extraInfoPojo, loanAccountVO.sdkType, getLanguageLocaleFromHeader().locale);
      return EcResponseUtil.generate(response);
    } catch (EcException exception) {
      throw generateWarningExceptionIfUserNotExist(exception);
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.USER.QUERY.ACCOUNT_CHANGE_HISTORY')")
  @GetMapping(path = "/admin/operation/loan/accountChangeHistory")
  public Result accountChangeHistory(
      @RequestParam(value = "loanAccountId") Long loanAccountId
  ) {
    try {
      LoanAccountVO loanAccountVO = loanAccountService.checkAndGetLoanAccountVO(loanAccountId);
      List<AccountChangeHistoryVO> accountChangeHistoryList = userMobileChangeService.getAccountChangeHistoryList(loanAccountVO.userId, loanAccountVO.sdkType);
      List<UserRiskCaseOperationLogVO> riskCaseOperationLogs = riskCaseService.listOperationLog(loanAccountVO.id);
      return EcResponseUtil.generate(AccountChangeHistoryResponse.from(accountChangeHistoryList, riskCaseOperationLogs));
    } catch (EcException exception) {
      throw generateWarningExceptionIfUserNotExist(exception);
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.USER.QUERY.USERD_LOAN_ACCOUNT_LIST')")
  @GetMapping(path = "/admin/operation/loan/usedLoanAccountList")
  public Result usedLoanAccountList(
      @RequestParam(value = "loanAccountId") Long loanAccountId
  ) {
    try {
      LoanAccountVO loanAccountVO = loanAccountService.checkAndGetLoanAccountVO(loanAccountId);
      List<UsedAccountVO> usedAccountList = userMobileChangeService.getUsedAccountList(loanAccountVO.userId);
      return EcResponseUtil.generate(UsedAccountListResponse.from(usedAccountList));
    } catch (EcException exception) {
      throw generateWarningExceptionIfUserNotExist(exception);
    }
  }

  private void assetLoanAccountValid(LoanAccountVO loanAccountVO, String adminUserEmail) {
    if (riskConfig.getCanFetchDeleteUserInfoAdminEmailList().contains(adminUserEmail)) {
      return;
    }
    if (loanAccountVO.deleted) {
      throw EcException.error(EcExceptionType.USER_DOES_NOT_EXIST, "user has been deleted.");
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.USER.QUERY.OPERATION_INFO')")
  @GetMapping(path = "/admin/operation/loan/operationLog")
  public Result getOperationLog(
      @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
      @RequestParam(value = "loanAccountId", required = false) Long loanAccountId,
      @RequestParam(value = "email", required = false) String emailAddress
  ) {
    try {
      ListOperationLogResponse listOperationLogResponse = new ListOperationLogResponse();
      LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(mobileNumber, loanAccountId, emailAddress, getSDKType());
      List<OperationLogRecordVo> operationLogRecords = loanAccountService.getOperationLog(loanAccountVO.id);
      for (OperationLogRecordVo operationLogRecord : operationLogRecords) {
        OperationLogUnitResponse operationLogUnitResponse = OperationLogUnitResponse.from(operationLogRecord);
        if (operationLogRecord.getUserOpt() == 0L) {
          operationLogUnitResponse.operator = "System";
        } else {
          operationLogUnitResponse.operator = adminUserModel.findByIdOrThrow(operationLogRecord.getUserOpt()).getEmail();
        }
        listOperationLogResponse.logs.add(operationLogUnitResponse);
      }
      return EcResponseUtil.generate(listOperationLogResponse);
    } catch (EcException exception) {
      throw generateWarningExceptionIfUserNotExist(exception);
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.PROCESS')")
  @PostMapping(path = "/admin/operation/loan/batchRejectCreditsByApp")
  public Result batchRejectCreditsByApp(@RequestBody @Valid BatchRejectRequest request) {
    List<String> loanAccountIdStrings = new LinkedList<>(Arrays.asList(request.loanAccountIds.split(",")));
    List<Long> loanAccountIds = new LinkedList<>();
    List<Long> failedIds = new LinkedList<>();

    for (String loanAccountIdString : loanAccountIdStrings) {
      try {
        loanAccountIds.add(Long.valueOf(loanAccountIdString));
      } catch (Exception e) {
        log.warn("{} cannot be parsed into a valid loanAccount Id", loanAccountIdString);
      }
    }
    for (Long loanAccountId : loanAccountIds) {
      try {
        LoanUserRiskTraceVO riskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(loanAccountId);
        riskFacadeService.batchRejectLoanCreditsByAdmin(riskTraceVO, request.rejectReason);

      } catch (Exception e) {
        log.error("批量拒绝时有拒绝失败 loanAccountId " + loanAccountId, e);
        failedIds.add(loanAccountId);
        continue;
      }
      publishAuditTrail(AuditTrailActivity.CREDIT_REJECTION, AuditTrailCopy.CREDIT_REJECTION_DESC,
          AuditTrailCopy.creditRejectionContent(loanAccountId));
    }
    return EcResponseUtil.generate(failedIds);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.PROCESS')")
  @PostMapping(path = "/admin/operation/loan/acceptCreditsByApp")
  public Result acceptCreditsByApp(@RequestBody @Valid AcceptRequest request) {
    try {
      //todo(tianbao) 待修复不改riskTrace的问题
      LoanUserTypeVO loanUserTypeVO = loanUserTypeService.fromUserTypeName(request.userType);
      ProductTypeCodeVO productTypeCodeVO = ProductTypeCodeVO.from(request.productTagCode, request.preRateTagCode, request.rateTagCode);
      String adminUserName = getAdminUserName();
      riskFacadeService.acceptCreditsByAdmin(
          request.loanAccountId,
          loanUserTypeVO,
          request.credits,
          request.operationLevel,
          request.acceptReason,
          request.rejectMultiApply,
          request.rejectMultiOrder,
          CreditsStatusChangeSource.ADMIN,
          productTypeCodeVO,
          adminUserName,
          TagOperatorTypeEnum.MANUAL_OPERATE);
    } catch (Exception e) {
      if (EcException.wrap(e).exceptionType == EcExceptionType.LOAN_TICKET_RESTRICTED) {
        log.warn("通过时有未知的拒绝原因（ticket拒绝）loanAccountId " + request.loanAccountId, e);
      } else if (EcException.wrap(e).exceptionType == EcExceptionType.LOAN_ACCOUNT_UNEXPECTED_CREDITS_STATUS) {
        log.warn("通过时有未知的拒绝原因（非ticket拒绝）loanAccountId " + request.loanAccountId, e);
      } else {
        log.error("通过时有未知的拒绝原因（非ticket拒绝）loanAccountId " + request.loanAccountId, e);
      }
    }
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.PROCESS')")
  @PostMapping(path = "/admin/operation/loan/batchRejectReloanCreditsByApp")
  public Result batchRejectReloanCreditsByApp(@RequestBody @Valid BatchRejectRequest request) {
    List<String> loanAccountIdStrings = new LinkedList<>(Arrays.asList(request.loanAccountIds.split(",")));
    List<Long> loanAccountIds = new LinkedList<>();
    List<Long> failedIds = new LinkedList<>();

    for (String loanAccountIdString : loanAccountIdStrings) {
      try {
        loanAccountIds.add(Long.valueOf(loanAccountIdString));
      } catch (Exception e) {
        log.warn("{} cannot be parsed into a valid loanAccount Id", loanAccountIdString);
      }
    }
    for (Long loanAccountId : loanAccountIds) {
      try {
        LoanUserRiskTraceVO traceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(loanAccountId);
        Boolean sendNotification = !LoanUserRiskType.getBatchTriggerCreditsRiskTypes().contains(traceVO.riskType);
        riskFacadeService.batchRejectReloanCreditsByAdmin(loanAccountId, LoanCreditsRejectedReason.UNKNOWN, CreditsStatusChangeSource.BATCH, request.rejectReason, traceVO.traceId);
      } catch (Exception e) {
        log.error("复贷批量拒绝出现异常 loanAccountId = {}", loanAccountId, e);
        failedIds.add(loanAccountId);
      }
    }
    return EcResponseUtil.generate(failedIds);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.PROCESS')")
  @GetMapping(path = "/admin/operation/loan/getCanChangeUserTypeList")
  public Result getCanChangeUserTypeList(
      @RequestParam(value = "loanAccountId") Long loanAccountId
  ) {
    SDKType sdkType = loanAccountService.getLoanAccountVO(loanAccountId).sdkType;
    List<LoanUserTypeVO> loanUserTypeVOList = loanUserTypeService.getReloanUserTypeBySDKType(sdkType);
    List<Label<TT>> labelList = loanUserTypeVOList
        .stream()
        .map(type -> Label.gen(TT.gen(type.desc), type.name))
        .collect(Collectors.toList());
    return EcResponseUtil.generate(labelList);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.USER.MANAGE', 'LOAN.RISK.RESULT.PROCESS')")
  @PostMapping(path = "/admin/operation/loan/resetSteps")
  public Result resetSteps(@RequestBody @Valid ResetStepsRequest request) {
    //该接口先直接抛错，等产品确认逻辑后再适配
    if (CollectionUtils.isNotEmpty(request.steps)) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM, TT.gen("暂时只支持重置全部鉴权步骤"));
    }
    List<String> loanAccountIds = new LinkedList<>(Arrays.asList(request.loanAccountIds.split("\\n|\\r\\n|\\r")));
    List<Integer> steps = request.steps;
    if (steps.contains(AuthStep.DOCUMENT_PHOTO.hex)) {
      throw EcException.error("resetSteps: can't reset step DOCUMENT_PHOTO");
    } else if (steps.contains(AuthStep.IDENTITY_OR_DRIVER_LICENSE.hex)) {
      steps.add(AuthStep.DOCUMENT_PHOTO.hex);
    }
    for (String loanAccountId : loanAccountIds) {
      authService.resetSteps(Long.valueOf(loanAccountId), steps);
    }
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.USER.MANAGE', 'LOAN.RISK.RESULT.PROCESS')")
  @PostMapping(path = "/admin/operation/loan/resetCreditsInfo")
  public Result resetCreditsInfo(@RequestBody @Valid ResetCreditsInfoRequest request) {
    //暂时改成只支持重置全部鉴权步骤，等后续产品确认逻辑后再优化
    if (CollectionUtils.isNotEmpty(request.steps)) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM, TT.gen("暂时只支持重置全部鉴权步骤"));
    }
    List<String> loanAccountIds = new LinkedList<>(Arrays.asList(request.loanAccountIds.split("\\n|\\r\\n|\\r")));
    for (String loanAccountId : loanAccountIds) {
      LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(Long.valueOf(loanAccountId));
      List<Integer> steps = adaptSteps(accountVO.userId, request.steps);
      authService.resetCreditsInfo(accountVO.id, steps);
    }
    return EcResponseUtil.generateSuccess();
  }

  private List<Integer> adaptSteps(Long userId, List<Integer> steps) {
    if (CollectionUtils.isNotEmpty(steps)) {
      return steps;
    }
    return authService.getFinishedSteps(userId);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.QUERY', 'LOAN.RISK.RESULT.SCORE_QUERY')")
  @GetMapping(path = "/admin/operation/loan/ruleSetResult")
  public Result getRuleSetResult(
      @RequestParam("traceId") Long traceId
  ) {
    throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("This feature is under maintenance"));
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.SCORE_QUERY')")
  @GetMapping(path = "/admin/v2/operation/loan/ruleSetResult")
  public Result getRuleSetResultByAccountId(
      @RequestParam("loanAccountId") Long accountId
  ) {
    LoanUserRiskTraceVO loanUserCreditsInfoVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(accountId);
    if (loanUserCreditsInfoVO == null) {
      return EcResponseUtil.generate(new GetRuleSetResultResponse());
    }
    GetRuleSetResultV2Response v2Response = queryRuleSetV2ForNewEngineTrace(
        loanUserCreditsInfoVO.traceId,
        getAdminUserEmail()
    );
    if (v2Response == null) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("This feature is under maintenance"));
    }
    return EcResponseUtil.generate(v2Response);
  }

  /**
   * 新引擎决策链下按 traceId 查询规则集 V2；非新引擎返回 null。
   *
   * @param traceId 风控 traceId
   * @param operatorEmail 当前管理员邮箱
   * @return 规则集响应；无法按新引擎展示时返回 null
   */
  private GetRuleSetResultV2Response queryRuleSetV2ForNewEngineTrace(long traceId, String operatorEmail) {
    RiskFlowTraceVOV2 riskFlowTraceVOV2 = riskEngineService.getRiskFlowTraceVOV2(traceId);
    if (riskFlowTraceVOV2 == null || riskFlowTraceVOV2.getProps() == null) {
      return null;
    }
    Map<String, Object> propsMap = riskFlowTraceVOV2.getProps();
    if (!RiskFlowTraceRunType.NEW_ENGINE_DECISION.name().equals(propsMap.get(RiskKey.TRACE_RUN_TYPE))) {
      return null;
    }
    RiskRuleSetResultRequest riskRuleSetResultRequest = new RiskRuleSetResultRequest();
    riskRuleSetResultRequest.setTraceId(traceId);
    riskRuleSetResultRequest.setOjkAdminUser(lyConfig.isLyAdminUser(operatorEmail));
    com.yqg.overseasrisk.common.mvc.ruleenginereborn.trace.GetRuleSetResultResponse result =
        engineRiskFlowService.queryRuleSetResultResponse(riskRuleSetResultRequest);
    Long translationMaxRuleId = riskConfig.getUserRiskTranslationMaxRuleId();
    GetRuleSetResultV2Response response = GetRuleSetResultV2Response.from(result, YqgLocale.US, translationMaxRuleId);
    if (response != null && !CollectionUtils.isEmpty(response.results)) {
      postProcessRuleSetV2ResponseResults(response);
    }
    return response;
  }

  /**
   * 与既有 ruleSetResult 接口一致：按配置剔除规则 id、替换模型/特征展示名。
   *
   * @param response 新引擎规则集响应（就地修改）
   */
  private void postProcessRuleSetV2ResponseResults(GetRuleSetResultV2Response response) {
    List<String> excludeRuleId = riskFlowConfig.getExcludeNewRuleIdConfig();
    if (CollectionUtils.isEmpty(excludeRuleId)) {
      log.info("queryRuleSetResultResponse new excludeRuleId is empty");
      return;
    }
    Iterator<RuleSetRunResultV2Response> list = response.results.iterator();
    while (list.hasNext()) {
      RuleSetRunResultV2Response ruleSetRunResultResponse = list.next();
      if (ruleSetRunResultResponse == null || StringUtils.isBlank(ruleSetRunResultResponse.id)
          || !ruleSetRunResultResponse.id.contains("_")) {
        continue;
      }
      if (excludeRuleId.contains(ruleSetRunResultResponse.id.split("_")[0])) {
        list.remove();
      }
      Map<String, String> modelAndFeatureReplaceConfig = riskFlowConfig.getModelAndFeatureReplaceConfig();
      if (MapUtils.isNotEmpty(modelAndFeatureReplaceConfig)) {
        for (Map.Entry<String, String> entry : modelAndFeatureReplaceConfig.entrySet()) {
          if (ruleSetRunResultResponse.name.contains(entry.getKey())) {
            ruleSetRunResultResponse.name = ruleSetRunResultResponse.name.replaceAll(entry.getKey(), entry.getValue());
          }
        }
      }
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.SCORE_QUERY')")
  @GetMapping(path = "/admin/v2/operation/loan/creditsLog")
  public Result getCreditsLogByAccountId(
      @RequestParam("loanAccountId") Long accountId,
      @RequestParam("pageSize") Integer pageSize,
      @RequestParam("pageNo") Integer pageNo
  ) {
    int limit = pageSize;
    int offset = (pageNo - 1) * pageSize;
    Integer count = cashLoanCreditsService.countUserCreditsLogByAccountId(accountId);
    List<LoanUserCreditsLogVO> loanUserCreditsLogVOList = cashLoanCreditsService.getLoanUserCreditsLogVOByPage(accountId, limit, offset);
    List<Long> traceIds = loanUserCreditsLogVOList.stream()
        .filter(loanUserCreditsLogVO -> Objects.nonNull(loanUserCreditsLogVO.traceId))
        .map(LoanUserCreditsLogVO::getTraceId)
        .collect(Collectors.toList());
    Map<Long, LoanUserRiskType> riskTraceRecordMap = loanUserRiskTraceService.fetchMapByTraceIds(traceIds);
    return EcResponseUtil.generate(LoanCreditsLogResponse.from(loanUserCreditsLogVOList, riskTraceRecordMap, count));
  }


  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.SCORE_QUERY')")
  @GetMapping(path = "/admin/v2/operation/loan/modelFeatureInfo")
  public Result getModelFeatureInfoByAccountId(
      @RequestParam("loanAccountId") Long accountId
  ) {
    ModelFeatureInfoResult modelFeatureInfoResult = riskFeatureDataService.getModelFeatureInfoResult(accountId);
    ListModelFeatureInfoResponse listModelFeatureInfoResponse = new ListModelFeatureInfoResponse();
    listModelFeatureInfoResponse.addModelFeatureInfo(Lists.newArrayList(modelFeatureInfoResult));
    listModelFeatureInfoResponse.modelFeatureInfoList = modelFeatureInfoResult.getModelFeatureInfoList();
    return EcResponseUtil.generate(listModelFeatureInfoResponse);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.SCORE_QUERY')")
  @GetMapping(path = "/admin/v2/operation/loan/loanMappingUserLevel")
  public Result getLoanUserLevelByAccountId(
      @RequestParam("loanAccountId") Long loanAccountId
  ) {
    String userLevel = riskUserLevelMappingService.findRealUserLevel(loanAccountId, LOAN_MAPPING_USER_LEVEL_TYPES);
    if (Objects.isNull(userLevel)) {
      userLevel = riskUserLevelMappingService.findRealUserLevel(loanAccountId, Collections.singletonList(LoanUserRiskType.LOAN));
    }
    if (Objects.isNull(userLevel)) {
      userLevel = REJECT_BEFORE_GRADING;
    }
    return EcResponseUtil.generate(LoanMappingUserLevelResponse.from(userLevel));
  }


  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.PROCESS')")
  @PostMapping(path = "/admin/operation/loan/retryRiskFlowTrace")
  public Result retryRiskFlowTrace(@RequestBody @Valid RetryRiskFlowTraceRequest request) {
    int totalNum = request.loanAccountIds.size();
    int failedNum = 0;
    for (Long accountId : request.loanAccountIds) {
      try {
        cashLoanCreditsService.retryRiskFlowTrace(accountId);
      } catch (EcException e) {
        failedNum++;
        log.warn("error in retry risk flow trace, accountId : {}", accountId, e);
      }
    }
    if (failedNum != 0) {
      return EcResponseUtil.generate(RetryRiskFlowTraceResponse.from(totalNum, failedNum));
    }
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.RESULT.FORCE_MANUAL')")
  @PostMapping(path = "/admin/operation/loan/batchForceManual")
  public Result batchForceManual(@RequestBody @Valid BatchForceManualRequest request) {
    int totalNum = request.loanAccountIds.size();
    int failedNum = 0;
    for (Long accountId : request.loanAccountIds) {
      try {
        cashLoanCreditsService.batchForceManual(accountId);
      } catch (EcException e) {
        failedNum++;
        log.warn("error in batch force manual trace, accountId : {}", accountId, e);
      }
    }
    if (failedNum != 0) {
      return EcResponseUtil.generate(BatchForceManualResponse.from(totalNum, failedNum));
    }
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.USER.QUERY.AUTH_INFO')")
  @PostMapping(path = "/admin/operation/loan/uploadFieldSurveyData")
  public Result uploadFieldSurveyData(@RequestBody @Valid FieldSurveyDataRequest request) {
    LoanAccountVO loanAccountVO = loanAccountService.getAccountByNormalizedMobileNumberOrThrow(request.mobileNumber, getSDKType());
    uploadInformationService.updateFieldSurveyData(
        loanAccountVO.id,
        request.cooperationTimeWithDistributor,
        request.profitPerMonth,
        request.homeStatus,
        request.dataCompleteness,
        request.purchaseFrequency,
        request.businessPeriod,
        request.frontLookWIthSellerImageUrl,
        request.inventoryPhotoImageUrl,
        request.transactionReceiptImageUrl,
        request.warungAreaUrl
    );
    return EcResponseUtil.generateSuccess();
  }


  private EcException generateWarningExceptionIfUserNotExist(EcException exception) {
    if (exception.exceptionType == EcExceptionType.USER_DOES_NOT_EXIST || exception.exceptionType == EcExceptionType.LOAN_ACCOUNT_NOT_FOUND) {
      return EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("user does not exist"));
    } else {
      return exception;
    }
  }

  private void overrideLoanUseForWhitelistAccount(Long accountId, AuthInfoResponse authInfo) {
    Locale locale = getLanguageLocaleFromHeader().locale;
    // 仅印尼语生效
    if (cashLoanAuthConfig.getAuthInfoLoanUseWhitelistSwitch() && !TranslationLocale.INDONESIAN.locale.equals(locale)) {
      return;
    }

    if (authInfo == null || !cashLoanAuthConfig.isAuthInfoLoanUseWhitelistAccount(accountId)) {
      return;
    }
    String displayText = cashLoanAuthConfig.getAuthInfoLoanUseWhitelistDisplayText();
    if (StringUtils.isBlank(displayText)) {
      return;
    }
    Map<String, Object> employmentInfo = authInfo.groupData.get("cashLoanEmploymentInfo");
    if (MapUtils.isEmpty(employmentInfo)) {
      return;
    }
    employmentInfo.put("loanUse", displayText);
  }
}
