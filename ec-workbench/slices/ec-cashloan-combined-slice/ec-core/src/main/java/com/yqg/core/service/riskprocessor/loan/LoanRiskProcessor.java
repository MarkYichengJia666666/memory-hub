package com.yqg.core.service.riskprocessor.loan;

import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.advertisement.AdAppsflyerModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.LoanUserCreditsChangeReason;
import com.yqg.core.model.sql.loan.account.enums.LoanUserTypeChangeReason;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.abtest.vo.ABTestUserIdRequestVO;
import com.yqg.core.service.advertisement.vo.appsflyer.AdAppsflyerRecordVO;
import com.yqg.core.service.loan.LoanAssertion;
import com.yqg.core.service.loan.account.CreditsQuota;
import com.yqg.core.service.loan.account.LoanUserTypeConfig;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.creditsdetails.CreditChangedEventType;
import com.yqg.core.service.loan.creditsdetails.CreditLoanStatus;
import com.yqg.core.service.loan.creditsdetails.LoanUserCreditsDetailsService;
import com.yqg.core.service.loan.creditsdetails.LoanUserCreditsDetailsVO;
import com.yqg.core.service.loan.infos.SubmitCreditsInfo;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.ly.LyConfig;
import com.yqg.core.service.ly.LyDemoUserService;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.longshortuser.UserMinimalistJudgeService;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.core.service.user.UserMigrateService;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.AppsFlyerInfo;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Objects;

import static com.yqg.core.service.loan.activity.LoanAuthCompleteAwardService.XIAO_MI_GLOBAL_INT;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/21 5:57 下午
 */
@Service
@Slf4j
public class LoanRiskProcessor extends BaseRiskProcessor {
  @Autowired
  private SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private LyDemoUserService lyDemoUserService;
  @Autowired
  private UserService userService;
  @Autowired
  private AdAppsflyerModel adAppsflyerModel;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private UserMigrateService userMigrateService;
  @Autowired
  private UserMinimalistJudgeService userMinimalistJudgeService;
  @Autowired
  private LyConfig lyConfig;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private LoanUserCreditsDetailsService loanUserCreditsDetailsService;
  @Autowired
  private LoanUserTypeConfig loanUserTypeConfig;
  @Autowired
  private ExpFacade expFacade;


  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    //校验用户状态为未授信
  }

  @Override
  public LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.LOAN;
  }

  //TODO 没太懂这里为什么更新了LOAN_USER_CREDITS_INFO表的timeCreated，后面看看能不能改为不更新
  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitCreditsApplication(creditsInfoRecord, traceVO.id);
  }

  @Override
  public void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord record = userCreditsInfoModel.findByAccountId(param.accountId);
    if (record == null) {
      LoanAccountRecord accountRecord = accountModel.findById(param.accountId);
      SDKType sdk = SDKType.fromCode(accountRecord.getSdkType());
      CurrencyAmount creditsQuota = CreditsQuota.getDefaultCreditsQuota(sdk, param.extraInfo.sourceType);
      record = userCreditsInfoModel.init(param.accountId, accountRecord.getUserId(), new CurrencyAmount(sdk.getCurrency(), 0L));
      // 这里hack下，给默认额度的时候，在日志表中插入一条从0 -> 默认额度的日志
      accountService.updateUserCredits(record, creditsQuota.getAmountInYuan(), LoanUserCreditsChangeReason.DEFAULT);
    }
    if (canUseNewUserTypeProcess(param.userId)) {
      updateUserTypeWhenSubmitCreditsV2(param.extraInfo.sourceType, param.accountId, param.userId);
    } else {
      updateUserTypeWhenSubmitCredits(param.extraInfo.platformType, param.extraInfo.sourceType, param.accountId, param.userId, param.channel);
    }
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    LoanAssertion.assertAccountAndAppInCreditsStatus(creditsInfoRecord, LoanCreditsStatus.NOT_APPLIED);
    userCreditsInfoModel.submitCreditsApplicationForCreditsStatus(creditsInfoRecord);

    LoanUserCreditsDetailsVO creditsDetails = initCreditsDetails(creditsInfoRecord);
    loanUserCreditsDetailsService.insertCreditsDetails(creditsDetails);

  }

  private boolean canUseNewUserTypeProcess(Long userId) {
    boolean processSwitch = loanUserTypeConfig.getNewUserTypeProcessSwitch();
    if (!processSwitch) {
      log.info("canUseNewUserTypeProcess switch is false, userId:{}", userId);
      return false;
    }
    return fetchExperimentRes(userId);
  }

  public Boolean fetchExperimentRes(Long userId) {
    ABTestUserIdRequestVO requestVO = ABTestUserIdRequestVO.from(ExperimentNameSpace.INIT_USER_TYPE, userId);
    String expRes = expFacade.fetchResultFallBackWithDefaultScene("risk_decision_userid-auth-abroad-loan-init_usertype_source", ExpFacade.ClientType.DIVERSION, requestVO, 0L);
    log.info("INIT_USER_TYPE fetchExperimentRes userId:{} result:{}", userId, expRes);
    return CommonABTestResultGroup.valueOf(expRes) == CommonABTestResultGroup.B;
  }

  private void updateUserTypeWhenSubmitCreditsV2(SourceType sourceType, Long accountId, Long userId) {
    //ly用户，修改为ly专用的UserType
    if (lyDemoUserService.isDemoUser(userId)) {
      String lyUserInitUserTypeCode = lyConfig.getLyUserInitUserTypeCode();
      LoanUserTypeVO loanUserTypeVO = loanUserTypeService.fromUserTypeCode(lyUserInitUserTypeCode);
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, loanUserTypeVO, LoanUserTypeChangeReason.OJK_DEMO_USER_INIT_CHANGE);
      return;
    }

    if (sourceType == SourceType.AKULAKU) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.AKULAKU_USER, LoanUserTypeChangeReason.AKULAKU_DEFAULT_CHANGE);
      return;
    }
    if (sourceType == SourceType.GOPAY) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.GOPAY_USER, LoanUserTypeChangeReason.CHANNEL_DEFAULT_USER_TYPE);
      return;
    }
    if (sourceType == SourceType.LAZADA_BUYER) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.LAZADA_BUYER_USER, LoanUserTypeChangeReason.CHANNEL_DEFAULT_USER_TYPE);
      return;
    }
    if (sourceType == SourceType.INDOSAT_CL2) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.INDOSAT_USER, LoanUserTypeChangeReason.CHANNEL_DEFAULT_USER_TYPE);
      return;
    }

    if (sourceType.isWebSourceType()) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.OVO_USER, LoanUserTypeChangeReason.CHANNEL_DEFAULT_USER_TYPE);
      return;
    }

    //内部员工测试使用的userType
    if (userService.getTestDemoLoanUserIdSet().contains(userId)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.TEST_INIT, LoanUserTypeChangeReason.TEST_DEMO_USER_INIT_CHANGE);
      return;
    }
  }


  private void updateUserTypeWhenSubmitCredits(PlatformType platformType, SourceType sourceType, Long accountId, Long userId, String channel) {
    LoanAccountVO loanAccountVO = accountService.getLoanAccountVO(accountId);
    boolean minimaListProcessByLoanAccountId = userMinimalistJudgeService.isMinimalistProcessUser(accountId);
    //ly用户，修改为ly专用的UserType
    if (lyDemoUserService.isDemoUser(userId)) {
      String lyUserInitUserTypeCode = lyConfig.getLyUserInitUserTypeCode();
      LoanUserTypeVO loanUserTypeVO = loanUserTypeService.fromUserTypeCode(lyUserInitUserTypeCode);
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, loanUserTypeVO, LoanUserTypeChangeReason.OJK_DEMO_USER_INIT_CHANGE);
      return;
    }
    if (minimaListProcessByLoanAccountId) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.MINIMALIST_USER, LoanUserTypeChangeReason.MINIMALIST_USER);
      return;
    }

    //因为谷歌需要用户选择是否上传applist，新建一个新的userType，针对用户没有上传applist的情况
    if (loanAccountVO.sdkType.isIdnLoanSDKType() && sourceType == SourceType.ANDROID && accountService.checkHasAppListInfo(accountId)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.NO_APP_LIST, LoanUserTypeChangeReason.NO_APP_LIST);
      return;
    }
    if (sourceType == SourceType.OPPO) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.OPPO_CUSTOM_MADE, LoanUserTypeChangeReason.OPPO_CUSTOME_CHANGE);
      return;
    }
    if (sourceType == SourceType.RONG_360) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.RONG_360_USER, LoanUserTypeChangeReason.RONG_360_DEFAULT_CHANGE);
      return;
    }
    if (sourceType == SourceType.AKULAKU) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.AKULAKU_USER, LoanUserTypeChangeReason.AKULAKU_DEFAULT_CHANGE);
      return;
    }
    if (sourceType == SourceType.GOPAY) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.GOPAY_USER, LoanUserTypeChangeReason.CHANNEL_DEFAULT_USER_TYPE);
      return;
    }
    if (sourceType == SourceType.LAZADA_BUYER) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.LAZADA_BUYER_USER, LoanUserTypeChangeReason.CHANNEL_DEFAULT_USER_TYPE);
      return;
    }
    if (sourceType == SourceType.INDOSAT_CL2) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.INDOSAT_USER, LoanUserTypeChangeReason.CHANNEL_DEFAULT_USER_TYPE);
      return;
    }
    if (checkOVOChannel(channel)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.OVO_USER, LoanUserTypeChangeReason.CHANNEL_DEFAULT_USER_TYPE);
      return;
    }

    if (userMigrateService.needSetMigrateUserType(userId)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.MIGRATE, LoanUserTypeChangeReason.MIGRATE_USER_ID);
      return;
    }
    //完件时如果是IOS设备，修改用户初始化类型
    if (PlatformType.IOS == platformType) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.IOS_INIT, LoanUserTypeChangeReason.IOS_DEFAULT_CHANGE);
      return;
    }
    //OPPO_H5单独适配
    if (riskConfig.getOppoH5ChannelList().contains(channel)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.OPPO_H5_INIT, LoanUserTypeChangeReason.OPPO_H5_CHANGE);
      return;
    }
    if (PlatformType.WEB == platformType && !checkXiaomiPaiYunduan(userId)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.H5_INIT, LoanUserTypeChangeReason.H5_DEFAULT_CHANGE);
      return;
    }
    if (checkOppoInit(userId)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.OPPO_INIT, LoanUserTypeChangeReason.OPPO_DEFAULT_CHANGE);
      return;
    }
    //当media_source=xiaomiglobal_int and campaign = 'yunduan'，也并入到I5
    if (checkXiaomiPaiYunduan(userId)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.OPPO_INIT, LoanUserTypeChangeReason.XIAOMI_PAI_YUNDUAN);
      return;
    }
    //内部员工测试使用的userType
    if (userService.getTestDemoLoanUserIdSet().contains(userId)) {
      accountService.updateUserTypeWithoutCheckForInitUserType(accountId, LoanUserTypeVO.TEST_INIT, LoanUserTypeChangeReason.TEST_DEMO_USER_INIT_CHANGE);
      return;
    }
  }

  private boolean checkOVOChannel(String channel) {
    try {
      log.info("channel info is {}:", channel);
      return riskConfig.getOVOChannelNameConfigList().contains(channel);
    } catch (Exception e) {
      log.warn("checkOVOChannel channel is {} error:", channel, e);
      return false;
    }
  }

  private LoanUserCreditsDetailsVO initCreditsDetails(LoanUserCreditsInfoRecord creditsInfoRecord) {
    Long userId = creditsInfoRecord.getUserId();
    Long loanAccountId = creditsInfoRecord.getLoanAccountId();
    BigDecimal riskFixedCredits = creditsInfoRecord.getCredits();
    BigDecimal marketingTotalCredit = loanUserCreditsService.evaluateMarketingTotalCreditByLoanAccountId(loanAccountId);
    BigDecimal marketingUpperBound = loanUserCreditsService.evaluateMarketingUpperBoundByLoanAccountId(loanAccountId);
    BigDecimal extraCredit = loanUserCreditsService.evaluateExtraTotalCreditByLoanAccountId(loanAccountId);

    LoanUserCreditsDetailsVO creditsDetails = new LoanUserCreditsDetailsVO();
    creditsDetails.userId = userId;
    creditsDetails.loanAccountId = loanAccountId;
    creditsDetails.loanStatus = CreditLoanStatus.UNAVAILABLE;

    creditsDetails.riskFixedTotalCredit = riskFixedCredits;
    creditsDetails.riskFixedActivatedCredit = riskFixedCredits;
    creditsDetails.riskFixedOutstandingCredit = BigDecimal.ZERO;
    creditsDetails.riskFixedRemainingCredit = riskFixedCredits;

    creditsDetails.riskExperimentalTotalCredit = BigDecimal.ZERO;
    creditsDetails.riskExperimentalActivatedCredit = BigDecimal.ZERO;
    creditsDetails.riskExperimentalOutstandingCredit = BigDecimal.ZERO;
    creditsDetails.riskExperimentalRemainingCredit = BigDecimal.ZERO;

    creditsDetails.marketingTotalCredit = marketingTotalCredit;
    creditsDetails.marketingActivatedCredit = marketingUpperBound;
    creditsDetails.marketingOutstandingCredit = BigDecimal.ZERO;
    creditsDetails.marketingRemainingCredit = marketingUpperBound;

    creditsDetails.extraTotalCredit = extraCredit;
    creditsDetails.extraActivatedCredit = extraCredit;
    creditsDetails.extraOutstandingCredit = BigDecimal.ZERO;
    creditsDetails.extraRemainingCredit = extraCredit;

    long now = Clock.now();
    creditsDetails.timeCreated = now;
    creditsDetails.timeUpdated = now;
    creditsDetails.timeChanged = now;
    creditsDetails.eventType = CreditChangedEventType.INIT;
    creditsDetails.relatedId = -1L;
    creditsDetails.creditDelta = BigDecimal.ZERO;
    creditsDetails.totalOutstandingCredit = BigDecimal.ZERO;
    return creditsDetails;
  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    // 发布用户提交风控事件
    if (riskConfig.getMinimalistProcessSubmitAllInformationMessageSwitch()) {
      LoanAccountVO accountVO = accountService.getLoanAccountVO(param.accountId);
      userEventService.publishLoanInformationSubmittedEvent(accountVO, "", null, null, null, null);
    }

    if (param.needSaveContextInfo && Objects.nonNull(param.terminalInfo) && Objects.nonNull(param.environmentInfo)) {
      SubmitCreditsInfo submitCreditsInfo = new SubmitCreditsInfo();
      submitCreditsInfo.terminalInfo = param.terminalInfo;
      submitCreditsInfo.environmentInfo = param.environmentInfo;
      submitCreditsAdditionalInfoService.saveContextInfoIgnoreException(param.accountId, traceVO.getId(), submitCreditsInfo, LoanUserRiskType.LOAN);
    }
  }


  private Boolean checkOppoInit(Long userId) {
    try {
      AdAppsflyerRecordVO adAppsflyerRecord = adAppsflyerModel.findByUserId(userId);
      return adAppsflyerRecord != null && StringUtils.isNotBlank(adAppsflyerRecord.getMediaSource())
          && riskConfig.getOppoInitSource().contains(adAppsflyerRecord.getMediaSource());
    } catch (Exception e) {
      log.error("checkOppoInit error", e);
      return false;
    }

  }

  /**
   * 小米预安装渠道并入I5
   * 当media_source=xiaomiglobal_int and campaign = 'yunduan'，也并入到预安装的组中。
   *
   * @param userId
   * @return
   */
  private Boolean checkXiaomiPaiYunduan(Long userId) {
    try {
      AdAppsflyerRecordVO adAppsflyerRecord = adAppsflyerModel.findByUserId(userId);
      AppsFlyerInfo info = Objects.nonNull(adAppsflyerRecord) ? JsonUtils.from(adAppsflyerRecord.getBody(), AppsFlyerInfo.class) : null;
      if (Objects.isNull(adAppsflyerRecord)
          || Objects.isNull(info)
          || Objects.isNull(adAppsflyerRecord.getMediaSource())
          || Objects.isNull(info.campaign)) {
        return false;
      }
      return XIAO_MI_GLOBAL_INT.equals(adAppsflyerRecord.getMediaSource()) && riskConfig.getXiaomiPreInstallCampaignList().contains(info.campaign);
    } catch (Exception e) {
      log.error("checkXiaomiPaiYunduan error", e);
      return false;
    }
  }
}
