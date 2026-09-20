package com.yqg.core.service.risk.facade;

import com.yqg.core.model.generated.tables.records.*;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.LoanRiskUserGroupLogModel;
import com.yqg.core.model.sql.loan.account.LoanUserTypeLogModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loanusertrace.LoanRiskCreditStage;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.model.sql.loanusertrace.TriggerSubType;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.service.bizcheck.BizCheckMonitorService;
import com.yqg.core.service.cashloan.event.CashLoanEventType;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.AutoSubmitCalcCreditsMonitorService;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserSimpleCreditsInfoVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.riskflowcheck.RiskFlowCheckService;
import com.yqg.core.service.risk.riskflowcheck.vo.RiskFlowQualifyCheckResult;
import com.yqg.core.service.risk.riskoutput.RiskOutputService;
import com.yqg.core.service.risk.subnew.vo.SubNewAccountRequestVO;
import com.yqg.core.service.risk.usergroup.RiskUserGroupCheckDecisionService;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckAvailableStatus;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckOperationRelation;
import com.yqg.core.service.risk.usergroup.vo.RiskUserGroupCheckConfigVO;
import com.yqg.core.service.risk.usergroup.vo.UserGroupHistoryTraceData;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.RiskCrowdCategory;
import com.yqg.ec.common.i18n.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.yqg.core.service.risk.riskflowcheck.enums.RiskFlowCheckType.COMPLETED_AUTO_CREDITS;

/**
 * @author chaoye
 * @date 2025/7/28
 */
@Slf4j
@Service
public class RiskFacadeTool {

  // 续借测额 origin 触发来源，旧反查对外仅会产出这三类；陪跑对比只在旧值命中这三类时进行，其余类型无对照意义
  private static final Set<LoanUserRiskTriggerSource> MULTI_CALC_ORIGIN_TRIGGER_SOURCE_SHADOW_SCOPE = EnumSet.of(
      LoanUserRiskTriggerSource.PAYOUT_SUCCESS,
      LoanUserRiskTriggerSource.BATCH_RISK,
      LoanUserRiskTriggerSource.REPAY_PARTIAL_INSTALMENTS);

  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private RiskOutputService riskOutputService;
  @Autowired
  private LoanRiskUserGroupLogModel loanRiskUserGroupLogModel;
  @Autowired
  private LoanUserTypeLogModel loanUserTypeLogModel;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  @Lazy
  private RiskUserGroupCheckDecisionService riskUserGroupCheckDecisionService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private RiskFlowCheckService riskFlowCheckService;

  public LoanUserRiskTriggerSource getOriginTriggerSourceForMultiCalc(Long traceId) {
    //取原始风控trace
    LoanUserRiskTraceVO originRiskTraceVO = loanUserRiskTraceService.getOriginRiskTraceVOOrThrow(traceId);

    LoanUserRiskTriggerSource oldVal = resolveOriginTriggerSourceByRiskType(originRiskTraceVO);
    LoanUserRiskTriggerSource newVal = originRiskTraceVO.triggerSource;

    //陪跑对比：无论开关是否开启都执行，供开关开启前确认新旧一致；旧值仅命中对外使用的三类时才有对照意义
    if (MULTI_CALC_ORIGIN_TRIGGER_SOURCE_SHADOW_SCOPE.contains(oldVal) && !Objects.equals(newVal, oldVal)) {
      log.info("getOriginTriggerSourceForMultiCalc shadow diff, traceId:{}, originTraceId:{}, readTable:{}, byRiskType:{}",
          traceId, originRiskTraceVO.id, newVal, oldVal);
    }

    //开关决定权威返回值：关闭走旧反查，开启以落库触发来源为准
    return riskConfig.getMultiCalcOriginReadTableShadowSwitch() ? newVal : oldVal;
  }

  private LoanUserRiskTriggerSource resolveOriginTriggerSourceByRiskType(LoanUserRiskTraceVO originRiskTraceVO) {
    //如果原始风控trace是循环，走单独判断逻辑
    if (LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(originRiskTraceVO.riskType)) {
      return getOriginTriggerSourceForRevolvingLoan(originRiskTraceVO);
    }

    //检查是否手动触发
    if (originRiskTraceVO.triggerSubType == TriggerSubType.MANUAL) {
      return LoanUserRiskTriggerSource.MANUAL;
    }

    //检查是否为跑批风控
    if (TriggerSubType.BATCH_RISK_LIST.contains(originRiskTraceVO.triggerSubType)) {
      return LoanUserRiskTriggerSource.BATCH_RISK;
    }

    // 自动触发的场景，理论上只有续借打款测额和续借还款测额，根据risktype区分
    if (!LoanUserRiskType.getMultiLoanCalcRiskType().contains(originRiskTraceVO.riskType)) {
      log.error("getOriginTriggerSourceForMultiCalc, riskType not in multiLoanCalcRiskType, traceId:{}, riskType: {}", originRiskTraceVO.traceId, originRiskTraceVO.riskType);
      return null;
    }

    return (originRiskTraceVO.riskType == LoanUserRiskType.FIRST_MULTI_LOAN_PAYOUT_CALC_CREDITS || originRiskTraceVO.riskType == LoanUserRiskType.MULTI_LOAN_PAYOUT_CALC_CREDITS) ?
        LoanUserRiskTriggerSource.PAYOUT_SUCCESS :
        LoanUserRiskTriggerSource.REPAY_PARTIAL_INSTALMENTS;
  }

  private LoanUserRiskTriggerSource getOriginTriggerSourceForRevolvingLoan(LoanUserRiskTraceVO originRiskTraceVO) {
    //如果是循环二次，必定为手动触发
    if (originRiskTraceVO.riskType == LoanUserRiskType.REVOLVING_LOAN_SECOND) {
      return LoanUserRiskTriggerSource.MANUAL;
    }
    //TODO 后续优化这里
    //如果是循环一次，暂时先用一个通用的自动触发分类，后续细化
    return LoanUserRiskTriggerSource.OTHER_SYSTEM_AUTO;

  }

  public LoanRiskCreditStage getOriginLoanUserRiskCategoryForMultiCalc(Long traceId) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceId);
    if (!LoanUserRiskType.getMultiLoanCalcRiskType().contains(loanUserRiskTraceVO.riskType)) {
      return null;
    }

    //取原始风控trace
    //TODO 未来流程变多之后，这里取原始trace可能需要改成递归的写法
    LoanUserRiskTraceVO originRiskTraceVO = loanUserRiskTraceService.getOriginRiskTraceVOOrThrow(traceId);

    LoanRiskCreditStage oldVal = resolveOriginCreditStageByRiskType(originRiskTraceVO);
    LoanRiskCreditStage newVal = originRiskTraceVO.creditStage;

    //陪跑对比：无论开关是否开启都执行，供开关开启前确认新旧一致
    if (!Objects.equals(newVal, oldVal)) {
      log.info("getOriginLoanUserRiskCategoryForMultiCalc shadow diff, traceId:{}, originTraceId:{}, readTable:{}, byRiskType:{}",
          traceId, originRiskTraceVO.id, newVal, oldVal);
    }

    //开关决定权威返回值：关闭走旧反查，开启以落库信贷阶段为准
    return riskConfig.getMultiCalcOriginReadTableShadowSwitch() ? newVal : oldVal;
  }

  private LoanRiskCreditStage resolveOriginCreditStageByRiskType(LoanUserRiskTraceVO originRiskTraceVO) {
    //理论上只有循环和续借两种
    if (LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(originRiskTraceVO.riskType)) {
      return LoanRiskCreditStage.REVOLVING_LOAN;
    }

    return LoanRiskCreditStage.MULTI_LOAN;
  }


  public String getTriggerSubType(Long traceId) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrNull(traceId);
    if (Objects.isNull(loanUserRiskTraceVO)) {
      return null;
    }
    if (loanUserRiskTraceVO.triggerSubType == null) {
      return null;
    }
    return loanUserRiskTraceVO.triggerSubType.name();
  }

  public boolean isCalcRiskReject(LoanUserRiskTraceVO traceVO, LoanUserTagData data) {
    if (LoanUserRiskType.getMultiLoanCalcRiskType().contains(traceVO.riskType)) {
      return data != null && data.rejectMultiApply;
    }
    return traceVO.creditsStatus == LoanCreditsStatus.REJECTED;
  }

  public boolean isOrderRiskReject(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data) {
    if (LoanUserRiskType.getMultiLoanRiskType().contains(userTraceVO.riskType)) {
      return data != null && data.rejectMultiOrder;
    }
    return userTraceVO.creditsStatus == LoanCreditsStatus.REJECTED;
  }

  public Boolean canReapplyBasedOnRiskPeriod(LoanAccountRecord loanAccountRecord) {
    LoanUserSimpleCreditsInfoVO creditsInfo = loanUserCreditsService.getLoanUserSimpleCreditsInfoVOByUserId(loanAccountRecord.getUserId());
    if (Objects.isNull(creditsInfo) || !creditsInfo.hitRejectedReApply()) {
      return false;
    }
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(loanAccountRecord.getId());
    int diffDay = Clock.getDaysBetweenPlusOne(loanUserRiskTraceVO.timeUpdated, Clock.now(), SDKType.IDN_YQD.getTimeZone());
    return diffDay >= riskConfig.getLoanReapplyRiskDays();
  }

  /**
   * API 渠道（Gopay/Lazada）首贷、重审测额重申（REAPPLY_CALC_CREDITS）前置：是否与 {@link #canReapplyBasedOnRiskPeriod}
   * 同口径满足「可重申 + 已满 N 天」，命中则后续可升级主营并按渠道重置 userType。
   */
  public boolean shouldUpgradeToNormalBeforeApiChannelReapplyCalcCredits(RiskProcessParam riskProcessParam) {
    if (riskProcessParam == null || riskProcessParam.extraInfo == null) {
      return false;
    }
    return canReapplyBasedOnRiskPeriodForApiChannel(riskProcessParam.accountId, riskProcessParam.extraInfo.sourceType);
  }

  public boolean canReapplyBasedOnRiskPeriodForApiChannel(Long accountId, SourceType sourceType) {
    if (!SourceType.isReapplyIntervalSupportedApiChannel(sourceType)) {
      log.info("canReapplyBasedOnRiskPeriodForApiChannel, unsupported sourceType: {}", sourceType);
      return false;
    }
    LoanAccountRecord accountRecord = loanAccountModel.findById(accountId);
    if (accountRecord == null) {
      log.info("canReapplyBasedOnRiskPeriodForApiChannel, accountRecord is null, accountId: {}", accountId);
      return false;
    }
    if (loanAccountBasicInfoService.isReloan(accountId)) {
      log.info("canReapplyBasedOnRiskPeriodForApiChannel, isReloan, accountId: {}", accountId);
      return false;
    }
    return canReapplyBasedOnRiskPeriod(accountRecord);
  }


  /**
   * 判断用户当前是否满足 被拒180天后可重新申请的条件
   *
   * @param userId
   * @param sourceType 如果是api渠道，直接返回false，如果不需要判断渠道，可以传null
   * @return
   */
  public boolean hitCanReapplyAfterInterval(Long userId, @Nullable SourceType sourceType) {
    if (Objects.nonNull(sourceType) && sourceType.isApiChannelSourceType()) {
      return false;
    }

    LoanAccountRecord accountRecord = loanAccountModel.findByUserIdOrThrow(userId);
    boolean isReloan = loanAccountBasicInfoService.isReloan(accountRecord.getId());
    if (isReloan) {
      return false;
    }

    return canReapplyBasedOnRiskPeriod(accountRecord);
  }

  /**
   * ============================ 注意 ============================
   * 【仅限风控提交场景使用】，其余场景严禁调用此方法！！！
   * 误用会导致业务侧数据统计结果失真，引发统计口径错误！
   * =============================================================
   * <p>
   * 在标准规则基础上，为了迎合风控业务侧数据统计，获取的用户当前的人群定义
   *
   * @param loanUserRiskType 贷款用户风险类型
   * @param accountVO        账户VO对象
   * @return 用户当前的人群定义
   */
  public RiskCrowdCategory getRiskCrowdCategoryForSubmitRisk(LoanUserRiskType loanUserRiskType, LoanAccountVO accountVO) {
    switch (loanUserRiskType) {
      case SUPPLEMENT_INFO_BEFORE_CREATE_ORDER:
        //补件风控全部归到新客首贷
        return RiskCrowdCategory.LOAN;
      case ACTIVITY_ORDER_CREDITS:
        //活动订单归到老客复贷
        return RiskCrowdCategory.RELOAN;
      default:
        //其余情况，使用标准的分类
        return getStandardRiskCrowdCategory(accountVO);
    }
  }

  /**
   * 根据标准规则，获取用户当前的人群定义
   *
   * @param accountVO
   * @return
   */
  private RiskCrowdCategory getStandardRiskCrowdCategory(LoanAccountVO accountVO) {
    //是否回捞/重审等级
    boolean isRetrievalOrReapplyUser = loanAccountBasicInfoService.isRetrievalOrReapplyUserByAccountId(accountVO.id);

    if (isRetrievalOrReapplyUser) {
      return RiskCrowdCategory.RETRIEVAL;
    }
    if (LoanUserTypeVO.SUB_NEW_USER.code.equals(accountVO.getLoanUserTypeVO().code)) {
      return RiskCrowdCategory.NEW_CUSTOMER_RELOAN;
    }

    //打款成功次数
    Long loanTimes = accountVO.loanTimes;
    return loanTimes > 0 ? RiskCrowdCategory.RELOAN : RiskCrowdCategory.LOAN;
  }

  public boolean canGetLoanByTraceOutput(Long traceId) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceId);
    if (loanUserRiskTraceVO.creditsStatus != LoanCreditsStatus.ACCEPTED) {
      return false;
    }

    //续借测额风控
    if (LoanUserRiskType.getMultiLoanCalcRiskType().contains(loanUserRiskTraceVO.riskType)) {
      RiskOutputResultRecord riskOutputResultRecord = riskOutputService.findByTraceIdAndType(traceId, RiskOutputType.MULTI_LOAN_CALC_CREDITS_RESULT);
      return Objects.nonNull(riskOutputResultRecord) && StringUtils.equalsIgnoreCase(Boolean.FALSE.toString(), riskOutputResultRecord.getNewValue());
    }

    //续借二次风控
    if (LoanUserRiskType.getMultiLoanRiskType().contains(loanUserRiskTraceVO.riskType)) {
      RiskOutputResultRecord riskOutputResultRecord = riskOutputService.findByTraceIdAndType(traceId, RiskOutputType.MULTI_LOAN_RESULT);
      return Objects.nonNull(riskOutputResultRecord) && StringUtils.equalsIgnoreCase(Boolean.FALSE.toString(), riskOutputResultRecord.getNewValue());
    }

    //循环风控，包括一次和二次
    if (LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(loanUserRiskTraceVO.riskType)) {
      return loanAccountRevolvingService.checkCanLoanByRevolvingLoan(traceId);
    }

    return true;
  }

  //TODO 仅回刷历史数据使用，等待回刷后删除这些代码
  public UserGroupHistoryTraceData findFirstNotMatchData(Long accountId) {
    // 1、拉出创建时间在LoanRiskUserGroupLogModel的第一条创建时间之后的所有LoanUserRiskTraceModel的数据
    List<LoanRiskUserGroupLogRecord> userGroupLogs = loanRiskUserGroupLogModel.findAllByAccountIdOrderByTimeCreatedDesc(accountId);
    if (CollectionUtils.isEmpty(userGroupLogs)) {
      return null;
    }
    // 第一条创建时间 (正序的第一条即最老的一条)
    Long firstLogTime = userGroupLogs.get(userGroupLogs.size() - 1).getTimeCreated();

    List<LoanUserRiskTraceRecord> traceRecords = loanUserRiskTraceModel.findByAccountIdAndStartTimeAndTypesOrderByTimeAsc(accountId, firstLogTime, Arrays.stream(LoanUserRiskType.values()).collect(Collectors.toList()));
    if (CollectionUtils.isEmpty(traceRecords)) {
      return null;
    }

    // 2、正序遍历
    for (LoanUserRiskTraceRecord traceRecord : traceRecords) {
      Long snapshotTime = traceRecord.getTimeUpdated() + 2000L;
      // 获取快照：userGroup 和 userType
      LoanRiskUserGroupLogRecord groupSnapshot = loanRiskUserGroupLogModel.getLastByTimeCreated(accountId, snapshotTime);

      LoanUserTypeLogRecord typeSnapshot = loanUserTypeLogModel.getLastUserTypeByTimeCreated(accountId, snapshotTime);

      if (groupSnapshot == null || typeSnapshot == null) {
        continue;
      }

      LoanRiskUserGroupEnum userGroup = LoanRiskUserGroupEnum.valueOf(groupSnapshot.getUserGroup());
      String userType = typeSnapshot.getCurrentUserType();

      // 校验 userGroup 和 userType 是否匹配
      boolean isMatch = checkUserGroupMatch(userGroup, userType, traceRecord);
      if (!isMatch) {
        // 3、组装 UserGroupHistoryTraceData
        UserGroupHistoryTraceData data = new UserGroupHistoryTraceData();
        data.setUserGroup(userGroup);
        data.setUserType(userType);
        data.setTraceId(traceRecord.getTraceId());
        data.setRiskType(LoanUserRiskType.fromCode(traceRecord.getRiskType()));
        data.setCanGetLoanByTraceOutput(canGetLoanByTraceOutput(traceRecord.getTraceId()));
        data.setSourceType(traceRecord.getSourceType() == null ? null : SourceType.getSourceType(traceRecord.getSourceType()));
        return data;
      }
    }

    return null;
  }

  private boolean checkUserGroupMatch(LoanRiskUserGroupEnum userGroup, String userType, LoanUserRiskTraceRecord traceRecord) {
    List<RiskUserGroupCheckConfigVO> configVOs = riskUserGroupCheckDecisionService.getByUserGroup(userGroup);
    if (CollectionUtils.isEmpty(configVOs)) {
      return true;
    }

    LoanUserRiskTraceVO traceVO = loanUserRiskTraceService.findByTraceIdOrNull(traceRecord.getTraceId());
    if (traceVO == null) {
      return true;
    }

    for (RiskUserGroupCheckConfigVO configVO : configVOs) {
      // 检查是否需要校验规则
      if (!needCheckUserGroupRule(configVO, traceVO)) {
        continue;
      }

      // 判断规则是否命中 (isRuleHit)
      boolean isRuleHit = isRuleHit(configVO, userType);
      if (isRuleHit) {
        return false;
      }

    }
    return true;
  }

  private boolean needCheckUserGroupRule(RiskUserGroupCheckConfigVO configVO, LoanUserRiskTraceVO riskTraceVO) {
    if (configVO.availableStatus != RiskUserGroupCheckAvailableStatus.AVAILABLE) {
      return false;
    }
    List<String> riskTypes = configVO.riskTypeList;
    if (CollectionUtils.isNotEmpty(riskTypes) && !riskTypes.contains(riskTraceVO.riskType.code)) {
      return false;
    }
    if (configVO.creditStatus != null && riskTraceVO.creditsStatus != configVO.creditStatus.traceCreditsStatus) {
      return false;
    }
    return true;
  }

  private boolean isRuleHit(RiskUserGroupCheckConfigVO configVO, String userType) {
    RiskUserGroupCheckOperationRelation operationRelation = configVO.operationRelation;
    List<String> targetUserTypes = configVO.userTypeList;
    if (CollectionUtils.isEmpty(targetUserTypes)) {
      return false;
    }

    if (operationRelation == RiskUserGroupCheckOperationRelation.CONTAINS) {
      return targetUserTypes.contains(userType);
    } else if (operationRelation == RiskUserGroupCheckOperationRelation.NOT_CONTAINS) {
      return !targetUserTypes.contains(userType);
    }
    return false;
  }

  public boolean isNormalUserWhenFirstOrderPayout(SubNewAccountRequestVO subNewAccountRequestVO) {
    //找到第一笔订单
    CashLoanOrderVO cashLoanOrderVO = ecOrderService.getFirstOrder(subNewAccountRequestVO.loanAccountId, CashLoanOrderStatus.PAYOUT_STATUSES);
    if (Objects.isNull(cashLoanOrderVO)) {
      log.info("SubNew getBillingDateDays cashLoanOrderVO is null, accountId:{}", subNewAccountRequestVO.loanAccountId);
      return false;
    }

    LoanUserTypeLogRecord userTypeLogRecord = loanUserTypeLogModel.getLastUserTypeByTimeCreated(subNewAccountRequestVO.loanAccountId, cashLoanOrderVO.timePayout);
    if (Objects.isNull(userTypeLogRecord)) {
      log.info("SubNew getBillingDateDays userTypeLogRecord is null, accountId:{}", subNewAccountRequestVO.loanAccountId);
      return false;
    }

    return LoanUserTypeVO.RETRIEVAL_AND_REAPPLY_USER_TYPE_LIST.stream().map(vo -> vo.code).noneMatch(userTypeLogRecord.getCurrentUserType()::equals);
  }

  //结清之后用户也可能触发结清的前筛
  public boolean checkAndGetResultForAfterRepayallBeforeCalcRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, LoanUserRiskType riskType) {
    if(riskProcessParam == null) {
      return false;
    }
    // 再次校验submitScene 和 riskType
    if (submitScene != LoanUserRiskSubmitScene.AUTO_CALC_CREDITS_AFTER_REPAY_ALL
        || riskType != LoanUserRiskType.CALC_CREDITS) {
      log.info("checkAndGetResultForAfterRepayallBeforeCalcRisk submitScene is null or not AUTO_CALC_CREDITS_AFTER_REPAY_ALL, accountId is {}, return false", riskProcessParam.accountId);
      return false;
    }

    long accountId = riskProcessParam.accountId;
    CashLoanEventType eventType = CashLoanEventType.ORDER_COMPLETED;
    RiskFlowQualifyCheckResult result = null;
    try {
      result = riskFlowCheckService.getAndForceInsertQualifyResult(accountId, COMPLETED_AUTO_CREDITS).result;
    } catch (Exception e) {
      result = new RiskFlowQualifyCheckResult();
      result.hasQualify = false;
      log.error("get risk check result error! loanAccountId:{},eventType:{}", accountId, eventType.name(), e);
    }
    log.info("checkAndGetResultForAfterRepayallBeforeCalcRisk accountId is {} result:{}", accountId, result);
    if(result == null) {
      return false;
    }
    return result.hasQualify;
  }
}
