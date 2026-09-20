package com.yqg.core.service.risk.facade;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.model.sql.loanusertrace.TriggerType;
import com.yqg.core.service.abtest.ABTestVersionConfigService;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.multiloan.enums.MultiLoanTriggerType;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.risk.LoanRiskMetricService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.RevolvingUserGroupAbtestService;
import com.yqg.core.service.loan.account.RevolvingUserGroupService;
import com.yqg.core.service.risk.RiskApplicationSubmitService;
import com.yqg.core.service.risk.feature.BusinessRiskConfig;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.core.service.riskprocessor.infra.SubmitExtraInfo;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.PreTraceTriggerScene;
import com.yqg.ec.common.exception.EcException;
import com.yqg.experiment.common.enums.ResultGetType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author chaoye
 * @date 2025/7/4
 */
@Slf4j
@Service
public class RiskTypeTool {
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  private LoanAccountModel accountModel;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private RiskApplicationSubmitService riskApplicationSubmitService;
  @Autowired
  private ABTestVersionConfigService abTestVersionConfigService;
  @Autowired
  private BusinessRiskConfig businessRiskConfig;
  @Autowired
  private RevolvingUserGroupService revolvingUserGroupService;
  @Autowired
  @Lazy
  private RevolvingUserGroupAbtestService revolvingUserGroupAbtestService;
  @Autowired
  private LoanRiskMetricService loanRiskMetricService;
  @Autowired
  private RiskFacadeTool riskFacadeTool;

  /**
   * 创建提交风控的执行参数
   * 此方法用于前置trace输出结果后，需要立即触发下一条风控的场景
   *
   * @param accountId
   * @param lastTraceId 上一条trace的traceId
   * @return
   */
  public RiskProcessParam createRiskProcessParamByLastTrace(Long accountId, Long lastTraceId, PreTraceTriggerScene preTraceTriggerScene, LoanUserRiskSubmitScene submitScene, LoanUserRiskTriggerSource triggerSource) {
    LoanAccountRecord accountRecord = accountModel.findByIdOrThrow(accountId);
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(lastTraceId);

    SubmitExtraInfo submitExtraInfo = SubmitExtraInfo.fromTriggerSubTypeAndSubmitRisk(loanUserRiskTraceVO.triggerSubType, loanUserRiskTraceVO.sourceType, false, BooleanType.TRUE);
    RiskProcessParam param = RiskProcessParam.from(accountId, SDKType.fromCode(accountRecord.getSdkType()), TriggerType.AUTOMATIC, submitExtraInfo);
    //复用前置trace提交风控时的环境与终端信息
    RiskProcessParam lastTraceParam = riskApplicationSubmitService.getRiskProcessParamByTraceId(lastTraceId);
    if (lastTraceParam != null) {
      param.environmentInfo = lastTraceParam.environmentInfo;
      param.terminalInfo = lastTraceParam.terminalInfo;
      param.needSaveContextInfo = true;
    }
    //记录前置trace的相关信息
    param.lastTraceId = lastTraceId;
    param.lastTraceType = loanUserRiskTraceVO.riskType;
    param.preLastRiskId = loanUserRiskTraceVO.id;
    //触发来源与外部 id 一并在工厂内赋值：仅当来源非空时才同时写入外部 id，避免 source 为空却带 externalId 的语义不一致
    //外部 id 指向前置风控 trace 的 traceId（非表主键 id）
    if (triggerSource != null) {
      param.triggerSource = triggerSource;
      param.triggerSourceExternalId = loanUserRiskTraceVO.traceId == null ? null : String.valueOf(loanUserRiskTraceVO.traceId);
    }
    param.preTraceTriggerScene = preTraceTriggerScene;
    param.submitScene = submitScene;
    return param;
  }

  /**
   * 根据风控场景，获取riskType
   *
   * @param accountId
   * @param submitScene
   * @return
   */
  public LoanUserRiskType getRiskType(Long accountId, LoanUserRiskSubmitScene submitScene, RiskProcessParam riskProcessParam) {
    switch (submitScene) {
      case DEGRADE_TRY_ACCESS:
        return getForDegradeTryAccess(accountId);
      case DEGRADE_ALREADY_ACCESS:
        return getForDegradeAlreadyAccess(accountId, riskProcessParam);
      case NORMAL_CALC_CREDITS:
        return checkAndGetNormalCalcCreditsLoanUserRiskType(accountId, riskProcessParam.build, riskProcessParam.sdkType);
      case CREDITS_EXPIRE_AND_RE_CALC:
        return getLoanUserRiskTypeForCreditsExpireAndReCalc(accountId);
      case AUTH_CALC_CREDITS:
        return LoanUserRiskType.LOAN;
      case INCREASE_CREDITS:
        return LoanUserRiskType.INCREASE_CREDITS;
      case ACTIVITY_ORDER_RISK:
        return LoanUserRiskType.ACTIVITY_ORDER_CREDITS;
      case ORDER_RISK:
        return getOrderRiskType(accountId, riskProcessParam);
      case AUTO_MULTI_LOAN_CALC_CREDITS:
        return getMultiLoanCalcCreditsRiskTypeForPayoutAndRepay(riskProcessParam.triggerSource, accountId);
      case AUTO_CALC_CREDITS_AFTER_REPAY_ALL:
        return getLoanUserRiskTypeAfterRepayAll(accountId);
      case SUPPLEMENT_INFO_BEFORE_CREATE_ORDER:
        return LoanUserRiskType.SUPPLEMENT_INFO_BEFORE_CREATE_ORDER;
      case INCREASE_CREDITS_REAPPLY:
        return getRiskTypeForIncreaseCreditsReapply(accountId);
      case REVOLVING_CALC_CREDITS:
        return LoanUserRiskType.REVOLVING_LOAN_FIRST;
      case REAPPLY_CALC_CREDITS:
        return getRiskTypeForReapply(accountId, riskProcessParam.extraInfo, riskProcessParam.build);
      case BEFORE_AUTH_FINISH_CREDITS:
        return LoanUserRiskType.BEFORE_AUTH_FINISH_CREDITS;
      case API_RETURN_SUPPLEMENT_MULTI_LOAN_CALC_CREDITS:
        return getMultiLoanCalcCreditsRiskType(accountId);
      default:
        throw EcException.error("invalid submitScene:{}", submitScene);
    }
  }

  private LoanUserRiskType getLoanUserRiskTypeForCreditsExpireAndReCalc(Long accountId) {
    return getExpireReCalcCreditsLoanUserRiskType(accountId);
  }

  private LoanUserRiskType getLoanUserRiskTypeAfterRepayAll(Long accountId) {
    // 历史为循环用户，当前非循环，是常规，是老客，直接触发循环一次；
    if (checkReloanUserCanDirectEnterRevolvingProcess(accountId)) {
      log.info("AUTO_CALC_CREDITS_AFTER_REPAY_ALL -> REVOLVING_LOAN_FIRST, accountId={}", accountId);
      return LoanUserRiskType.REVOLVING_LOAN_FIRST;
    }
    return LoanUserRiskType.CALC_CREDITS;
  }

  /**
   * 获取额度失效后，重新测额触发的riskType
   *
   * @param accountId
   * @return
   */
  public LoanUserRiskType getExpireNextRiskTypeForAllVersion(Long accountId) {
    return getExpireReCalcCreditsLoanUserRiskType(accountId);
  }

  public LoanUserRiskType getRiskTypeForReapply(Long accountId, SubmitExtraInfo extraInfo, Long build) {
    boolean isReloan = loanAccountBasicInfoService.isReloan(accountId);
    if (isReloan) {
      return LoanUserRiskType.RELOAN_REAPPLY;
    }
    LoanAccountRecord loanAccountRecord = accountModel.findByIdOrThrow(accountId);
    if (isApiChannelCanReapplyAfterInterval(extraInfo)) {
      return riskFacadeTool.canReapplyBasedOnRiskPeriodForApiChannel(accountId, extraInfo.sourceType)
          ? LoanUserRiskType.REAPPLY_AFTER_INTERVAL
          : LoanUserRiskType.REAPPLY;
    }
    if (abTestVersionConfigService.getLoanDaysReapply(loanAccountRecord, ResultGetType.LAST_RESULT, extraInfo, build)) {
      return LoanUserRiskType.REAPPLY_AFTER_INTERVAL;
    }
    return LoanUserRiskType.REAPPLY;
  }

  private boolean isApiChannelCanReapplyAfterInterval(SubmitExtraInfo extraInfo) {
    return extraInfo != null
        && SourceType.isReapplyIntervalSupportedApiChannel(extraInfo.sourceType);
  }

  //TODO (chaoye)这里和loanAccountService循环依赖了，等后续把提交风控的方法挪到单独的service里面，保持loanAccountService仅用于借贷用户基本信息
  public LoanUserRiskType checkAndGetNormalCalcCreditsLoanUserRiskType(Long accountId, Long build, SDKType sdkType) {
    int readyOrderCount = ecOrderService.countOrder(accountId, CashLoanOrderStatus.READY);
    if (readyOrderCount == 0) {
      return getExpireReCalcRiskType(accountId);
    }
    if (!multiLoanStatusService.hasMultiCalcQualify(accountId)) {
      throw EcException.error("The user is unable to multi credits calc, accountId is {}", accountId);
    }
    return getMultiLoanCalcCreditsRiskType(accountId);
  }

  public LoanUserRiskType getExpireReCalcCreditsLoanUserRiskType(Long accountId) {
    //历史为循环用户，当前非循环，是常规，是老客
    if (checkReloanUserCanDirectEnterRevolvingProcess(accountId)) {
      log.info("getExpireReCalcCreditsLoanUserRiskType bypass to REVOLVING_LOAN_FIRST, accountId={}", accountId);
      return LoanUserRiskType.REVOLVING_LOAN_FIRST;
    }
    //非续借
    int readyOrderCount = ecOrderService.countOrder(accountId, CashLoanOrderStatus.READY);
    if (readyOrderCount == 0) {
      return getExpireReCalcRiskType(accountId);
    }
    //续借
    // 回捞额度失效，回主营一次风控
    return multiLoanStatusService.getTriggerType(accountId).isFirstLoan()
        ? LoanUserRiskType.FIRST_MULTI_LOAN_CALC_CREDITS
        : LoanUserRiskType.MULTI_LOAN_CALC_CREDITS;

  }

  private LoanUserRiskType getExpireReCalcRiskType(Long accountId) {
    return loanAccountBasicInfoService.isReloan(accountId) ? LoanUserRiskType.CALC_CREDITS : LoanUserRiskType.LOAN_RE_CREDITS;
  }

  public LoanUserRiskType getRiskTypeForIncreaseCreditsReapply(Long accountId) {
    LoanAccountRecord loanAccountRecord = accountModel.findById(accountId);

    return loanAccountRecord.getLoanTimes() > 0 ? LoanUserRiskType.RELOAN_INCREASE_CREDITS_REVIEW : LoanUserRiskType.LOAN_INCREASE_CREDITS_REVIEW;
  }

  public LoanUserRiskType getForDegradeTryAccess(Long accountId) {
    boolean isReloan = loanAccountBasicInfoService.isReloan(accountId);
    return isReloan ? LoanUserRiskType.RELOAN_RETRIEVAL : LoanUserRiskType.LOAN_RETRIEVAL;
  }

  //获取回捞承接风控的riskType
  public LoanUserRiskType getForDegradeAlreadyAccess(Long accountId, RiskProcessParam riskProcessParam) {
    if (loanAccountRevolvingService.checkNeedTriggerRetrieval(riskProcessParam.lastTraceId, accountId)) {
      return LoanUserRiskType.MULTI_LOAN_CALC_CREDITS;
    }
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(accountId);
    log.info("loanUserRiskTraceVO account is {}, traceId is {}, riskType is {}",
        loanUserRiskTraceVO.accountId, loanUserRiskTraceVO.traceId, loanUserRiskTraceVO.riskType.name());
    return LoanUserRiskType.getAllMultiLoanRiskType().contains(loanUserRiskTraceVO.riskType) ?
        LoanUserRiskType.MULTI_LOAN_CALC_CREDITS :
        LoanUserRiskType.CALC_CREDITS;
  }

  @NotNull
  public LoanUserRiskType getMultiLoanCalcCreditsRiskType(Long accountId) {
    MultiLoanTriggerType triggerType = multiLoanStatusService.getTriggerType(accountId);
    switch (triggerType) {
      case FIRST_LOAN_PAYOUT:
        return LoanUserRiskType.FIRST_MULTI_LOAN_PAYOUT_CALC_CREDITS;
      case FIRST_LOAN_REPAYMENT:
        return LoanUserRiskType.FIRST_MULTI_LOAN_CALC_CREDITS;
      case RELOAN_PAYOUT:
        return LoanUserRiskType.MULTI_LOAN_PAYOUT_CALC_CREDITS;
      case RELOAN_REPAYMENT:
        return LoanUserRiskType.MULTI_LOAN_CALC_CREDITS;
      default:
        throw EcException.error("unsupported trigger type, trigger type : {}, accountId : {}", triggerType, accountId);
    }
  }

  public LoanUserRiskType getMultiLoanCalcCreditsRiskTypeForPayoutAndRepay(
      LoanUserRiskTriggerSource triggerSource,
      Long accountId) {

    if (triggerSource != LoanUserRiskTriggerSource.PAYOUT_SUCCESS && triggerSource != LoanUserRiskTriggerSource.REPAY_PARTIAL_INSTALMENTS) {
      return getMultiLoanCalcCreditsRiskType(accountId);
    }
    //历史为循环用户，当前非循环，是常规，是老客
    if (checkReloanUserCanDirectEnterRevolvingProcess(accountId)) {
      log.info("getMultiLoanCalcCreditsRiskTypeForPayoutAndRepay bypass to REVOLVING_LOAN_FIRST, accountId={}", accountId);
      return LoanUserRiskType.REVOLVING_LOAN_FIRST;
    }

    MultiLoanTriggerType triggerType = multiLoanStatusService.getTriggerType(accountId);
    if (triggerType.isFirstLoan()) {
      return triggerSource == LoanUserRiskTriggerSource.PAYOUT_SUCCESS ?
          LoanUserRiskType.FIRST_MULTI_LOAN_PAYOUT_CALC_CREDITS :
          LoanUserRiskType.FIRST_MULTI_LOAN_CALC_CREDITS;
    } else if (triggerType.isReloan()) {
      return triggerSource == LoanUserRiskTriggerSource.PAYOUT_SUCCESS ?
          LoanUserRiskType.MULTI_LOAN_PAYOUT_CALC_CREDITS :
          LoanUserRiskType.MULTI_LOAN_CALC_CREDITS;
    }

    throw EcException.error("unsupported for getMultiLoanCalcCreditsRiskTypeForPayoutAndRepay, trigger type : {}, triggerSource : {}, accountId : {}", triggerType, triggerSource, accountId);
  }

  public boolean checkReloanUserCanDirectEnterRevolvingProcess(Long accountId) {
    return businessRiskConfig.isOldReloanDirectRevolvingRiskAfterRepayEnabledForAccount(accountId)
        && revolvingUserGroupAbtestService.isHistoricalRevolvingExperimentUser(accountId)
        && revolvingUserGroupService.isOldReloanUserByAccountAge(accountId)
        && loanRiskMetricService.isNormalUser(accountId)
        && !loanAccountRevolvingService.checkUserInRevolvingLoanProcess(accountId);
  }

  //获取二次风控的riskType
  private LoanUserRiskType getOrderRiskType(Long accountId, RiskProcessParam riskProcessParam) {
    boolean isMultiLoan = multiLoanStatusService.hasMultiLoanQualify(accountId);
    return loanAccountBasicInfoService.isReloan(accountId) ?
        getReloanRiskType(accountId, isMultiLoan) :
        getSecondRiskType(accountId);
  }

  public LoanUserRiskType getSecondRiskType(Long accountId) {
    LoanAccountRecord accountRecord = accountModel.findByIdOrThrow(accountId);
    //TODO (LTB,T000000) 首贷和循环额度没关系，这里只有兜底的作用
    LoanUserRiskTraceRecord userRiskTraceRecord = loanUserRiskTraceModel.findLastedByAccountIdAndRiskType(accountRecord.getId(), LoanUserRiskType.getLoanCalcCreditRiskTypesWithBatch());
    if (userRiskTraceRecord == null) {
      return null;
    }
    //循环贷用户提交二次风控，不依赖前置风控类型，直接使用循环贷二次
    boolean revolvingLoanUser = loanAccountRevolvingService.checkUserInRevolvingLoanProcessAndNoOverdueAndNoControl(accountId);
    if (revolvingLoanUser) {
      log.info("getSecondRiskType is REVOLVING_LOAN_SECOND loanAccountId is {}", accountId);
      return LoanUserRiskType.REVOLVING_LOAN_SECOND;
    }
    //如果riskType=M 首贷跑批暂时直接报错，后续再考虑
    LoanUserRiskType loanUserRiskType = LoanUserRiskType.getOrderRiskTriggerToMap().get(LoanUserRiskType.fromCode(userRiskTraceRecord.getRiskType()));
    if (Objects.isNull(loanUserRiskType)) {
      throw EcException.error("unsupported riskType, accountId is {}", accountId);
    }
    return loanUserRiskType;
  }

  @NotNull
  public LoanUserRiskType getReloanRiskType(Long accountId, boolean isMultiLoan) {
    //循环贷用户提交二次风控，不依赖前置风控类型，直接使用循环贷二次
    boolean revolvingLoanUser = loanAccountRevolvingService.checkUserInRevolvingLoanProcessAndNoOverdueAndNoControl(accountId);
    if (revolvingLoanUser) {
      log.info("getSecondRiskType is REVOLVING_LOAN_SECOND loanAccountId is {}", accountId);
      return LoanUserRiskType.REVOLVING_LOAN_SECOND;
    }
    if (!isMultiLoan) {
      return LoanUserRiskType.RELOAN;
    }
    MultiLoanTriggerType triggerType = multiLoanStatusService.getTriggerType(accountId);
    if (triggerType.isFirstLoan()) {
      return LoanUserRiskType.FIRST_MULTI_LOAN;
    } else if (triggerType.isReloan()) {
      return LoanUserRiskType.MULTI_LOAN;
    } else {
      throw EcException.error("unsupported trigger type, trigger type : {}, accountId : {}", triggerType, accountId);
    }
  }
}
