package com.yqg.core.service.risk.usergroup;

import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.LoanUserTypeLogRecord;
import com.yqg.core.model.generated.tables.records.RiskOutputResultRecord;
import com.yqg.core.model.sql.cashloan.enums.OrderAdditionalInfoType;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.LoanUserTypeLogModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.model.sql.loan.account.enums.LoanUserTypeChangeReason;
import com.yqg.core.model.sql.risk.RiskOutputResultModel;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.service.abtest.ABTestVersionConfigService;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.ordercenter.CashLoanOrderAdditionalInfoService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.CashLoanOrderAdditionalInfoVO;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskRetrievalValidTimeWindow;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.LoanUserService;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.LoanUserTypeService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.loan.vo.LoanUserTypeLogVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.risk.batchtrigger.RiskBatchTriggerService;
import com.yqg.core.service.risk.batchtrigger.extrafunction.BatchRiskExtraFunctionFactory;
import com.yqg.core.service.risk.batchtrigger.extrafunction.enums.ExtraFunctionType;
import com.yqg.core.service.risk.batchtrigger.extrafunction.utils.BatchRiskExtraFunctionUserGroupChangeUtil;
import com.yqg.core.service.risk.batchtrigger.vo.ExtraFunctionsVO;
import com.yqg.core.service.risk.facade.RiskFacadeService;
import com.yqg.core.service.risk.facade.RiskFacadeTool;
import com.yqg.core.service.risk.feature.BusinessRiskConfig;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskoutput.RiskOutputService;
import com.yqg.core.service.risk.subnew.SubNewAccountService;
import com.yqg.core.service.risk.subnew.SubNewConfig;
import com.yqg.core.service.risk.subnew.vo.SubNewAccountRequestVO;
import com.yqg.core.service.risk.subnew.vo.SubNewAccountResultVO;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupLogVO;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.service.risk.usergroup.vo.UserGroupTriggerResult;
import com.yqg.ec.common.constant.ExperimentKeyConstants;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.core.service.riskprocessor.infra.SubmitExtraInfo;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.experiment.common.enums.ResultGetType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author chaoye
 * @date 2025/7/30
 */
@Service
@Slf4j
public class RiskUserGroupTool {
  @Autowired
  private CashLoanOrderAdditionalInfoService cashLoanOrderAdditionalInfoService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private LoanUserTypeLogModel loanUserTypeLogModel;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  @Autowired
  private SubNewConfig subNewConfig;
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;
  @Autowired
  private LoanUserService loanUserService;
  @Autowired
  private RiskOutputResultModel riskOutputResultModel;
  @Autowired
  private ABTestVersionConfigService abTestVersionConfigService;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private SubNewAccountService subNewAccountService;
  @Autowired
  private LoanUserTypeService loanUserTypeService;
  @Autowired
  private LoanAccountModel accountModel;
  @Autowired
  private RiskOutputService riskOutputService;
  @Autowired
  private RiskBatchTriggerService riskBatchTriggerService;
  @Autowired
  private BatchRiskExtraFunctionFactory batchRiskExtraFunctionFactory;
  @Autowired
  private RiskFacadeService riskFacadeService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private BusinessRiskConfig businessRiskConfig;
  @Autowired
  private RiskFacadeTool riskFacadeTool;
  @Autowired
  private ExpDiversionClient expDiversionClient;

  /**
   * 命中实验组的分组结果：仅实验组(B) 赋 90 天管制期，对照组/空白组/兜底一律不锁。
   */
  private static final String NON_FIRST_EXPERIMENT_GROUP_RESULT = CommonABTestResultGroup.B.name();

  public LoanRiskUserGroupVO handleUserFromHistoryApiChannel(RiskProcessParam riskProcessParam, LoanRiskUserGroupVO currentGroupVO) {
    if (!businessRiskConfig.getHistoryApiChannelAutoFixSwitch()) {
      return currentGroupVO;
    }

    if (currentGroupVO == null) {
      return currentGroupVO;
    }

    if (riskProcessParam == null || riskProcessParam.accountId == null || riskProcessParam.extraInfo == null || riskProcessParam.extraInfo.sourceType == null) {
      return currentGroupVO;
    }

    //1 是否历史trace全部来自api渠道
    if (!riskFacadeService.isUserHistoryTraceAllFromApiChannel(riskProcessParam.accountId)) {
      return currentGroupVO;
    }

    //2 当前提交风控前是否非api渠道
    if (riskProcessParam.extraInfo.sourceType.isApiChannelSourceType()) {
      return currentGroupVO;
    }

    //满足条件，执行重置等级
    LoanRiskUserGroupEnum targetGroup;
    LoanUserCreditsInfoVO loanUserCreditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(riskProcessParam.accountId);
    if (Objects.nonNull(loanUserCreditsInfoVO) && loanUserCreditsInfoVO.isCreditsReject()) {
      targetGroup = LoanRiskUserGroupEnum.REAPPLY;
    } else {
      targetGroup = LoanRiskUserGroupEnum.NORMAL;
    }

    if (targetGroup == currentGroupVO.userGroup) {
      return currentGroupVO;
    }

    log.info("HISTORY_API_CHANNEL_AUTO_FIX success,loanAccountId is {}, current group is {}, target group is {}", riskProcessParam.accountId, currentGroupVO.userGroup, targetGroup);

    return loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(riskProcessParam.accountId,
        targetGroup,
        null,
        LoanRiskUserGroupChangeReason.HISTORY_API_CHANNEL_AUTO_FIX);
  }

  public UserGroupTriggerResult handleBatchRiskExtraFunctionUserGroupChange(RiskProcessParam riskProcessParam, LoanRiskUserGroupEnum currentGroup, boolean readOnly) {
    if (riskProcessParam.logId == null) {
      return null;
    }

    Long taskId = riskBatchTriggerService.getTaskIdByLogId(riskProcessParam.logId);
    if (taskId == null) {
      return null;
    }

    ExtraFunctionsVO extraFunctionsVO = riskBatchTriggerService.getExtraFunctionsVOByTaskId(taskId);

    if (extraFunctionsVO == null) {
      return null;
    }

    BatchRiskExtraFunctionUserGroupChangeUtil util = (BatchRiskExtraFunctionUserGroupChangeUtil) batchRiskExtraFunctionFactory.getUtil(ExtraFunctionType.USER_GROUP_CHANGE);
    if (util == null) {
      return null;
    }
    BatchRiskExtraFunctionUserGroupChangeUtil.ExtraFunctionUserGroupChangeParam userGroupChangeParam = util.getParam(extraFunctionsVO);

    if (userGroupChangeParam == null) {
      return null;
    }

    for (BatchRiskExtraFunctionUserGroupChangeUtil.ExtraFunctionUserGroupChangeParam.ItemParam changeItem : userGroupChangeParam.userGroupChangeList) {
      UserGroupTriggerResult result = doExecuteExtraFunctionUserGroupChange(riskProcessParam, changeItem, currentGroup, readOnly);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

  private UserGroupTriggerResult doExecuteExtraFunctionUserGroupChange(RiskProcessParam riskProcessParam,
                                                                       BatchRiskExtraFunctionUserGroupChangeUtil.ExtraFunctionUserGroupChangeParam.ItemParam changeItem,
                                                                       LoanRiskUserGroupEnum currentGroup,
                                                                       boolean readOnly) {
    if (changeItem.beforeUserGroup != currentGroup) {
      return null;
    }

    if (changeItem.afterUserGroup != LoanRiskUserGroupEnum.NORMAL) {
      //跑批暂时只支持升级到主营
      return null;
    }

    if (readOnly) {
      return UserGroupTriggerResult.fromOnlyUpdated(changeItem.afterUserGroup, currentGroup);
    }

    handleUserGroupUpgradeToNormal(riskProcessParam,
        changeItem.afterUserGroup,
        LoanRiskUserGroupChangeReason.BATCH_RISK_FORCE_UPGRADE,
        LoanUserTypeVO.RETRIEVAL_CALC_CREDITS_EXPIRE_USER,
        LoanUserTypeChangeReason.RETRIEVAL_CALC_CREDITS_EXPIRE_USER_CHANGE);

    return UserGroupTriggerResult.fromOnlyUpdated(changeItem.afterUserGroup, currentGroup);

  }

  public boolean isReloanRetrievalAccessForNormalDegrade(LoanUserRiskTraceVO previousTraceVO) {
    long accountId = previousTraceVO.accountId;
    //如果当前是重审结清升级的自动测额被拒，视为已经走过回捞准入流程
    if (upgradeToNormalAfterReapplyCompletedOrderSwitch(accountId) && isRepayAllReapplyOrderAutoCalcReject(previousTraceVO)) {
      return true;
    }
    int retrievalPayoutTimes = getRetrievalPayoutTimes(accountId);
    return retrievalPayoutTimes > 0;
  }

  //判断用户
  // 1 当前被拒风控为结清后自动测额
  // 2 当前主营等级来源为重审结清升级
  private boolean isRepayAllReapplyOrderAutoCalcReject(LoanUserRiskTraceVO previousTraceVO) {
    if (previousTraceVO.creditsStatus != LoanCreditsStatus.REJECTED) {
      return false;
    }
    RiskProcessParam riskProcessParam = loanUserRiskTraceService.getRiskProcessParamByTraceIdOrThrow(previousTraceVO.traceId);
    if (riskProcessParam.submitScene == null) {
      log.error("riskProcessParam submitScene is null, traceId: {}", previousTraceVO.traceId);
      return false;
    }
    if (riskProcessParam.submitScene != LoanUserRiskSubmitScene.AUTO_CALC_CREDITS_AFTER_REPAY_ALL) {
      return false;
    }

    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrThrow(previousTraceVO.accountId);
    if (loanRiskUserGroupVO.userGroup != LoanRiskUserGroupEnum.NORMAL) {
      return false;
    }
    if (loanRiskUserGroupVO.reason != LoanRiskUserGroupChangeReason.REPAY_ALL_INSTALMENT) {
      return false;
    }
    //找到前一条记录
    LoanRiskUserGroupLogVO preLoanRiskUserGroupLogVO = loanRiskUserGroupService.getPreLoanRiskUserGroupLogVOByAccountIdOrNull(previousTraceVO.accountId);

    return preLoanRiskUserGroupLogVO != null
        && LoanRiskUserGroupEnum.REAPPLY.equals(preLoanRiskUserGroupLogVO.userGroup);
  }

  /**
   * user group 是否有过期时间，且未过期
   *
   * @param accountId
   * @return
   */
  public boolean isUserGroupHasExpireTimeAndNotExpire(Long accountId) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrThrow(accountId);
    return loanRiskUserGroupVO.expireTime != -1 && loanRiskUserGroupVO.expireTime > Clock.now();
  }

  /**
   * 只读判定：给定跑批任务是否配置了「等级变更」额外功能（USER_GROUP_CHANGE）。
   * <p>只判断任务是否配置了 USER_GROUP_CHANGE 额外功能且变更列表非空，<b>不校验具体的 before/after 等级</b>：
   * 只要跑批配置了等级变更能力，管制期内即应拦截。该判定只读、不产生任何升降级副作用。
   * <p>供跑批 90 天锁拦截（US4-1）判定使用。
   *
   * @param taskId 跑批任务 id
   */
  public boolean isBatchTaskConfiguredUserGroupChange(Long taskId) {
    if (taskId == null) {
      return false;
    }
    ExtraFunctionsVO extraFunctionsVO = riskBatchTriggerService.getExtraFunctionsVOByTaskId(taskId);
    if (extraFunctionsVO == null) {
      return false;
    }
    BatchRiskExtraFunctionUserGroupChangeUtil util =
        (BatchRiskExtraFunctionUserGroupChangeUtil) batchRiskExtraFunctionFactory.getUtil(ExtraFunctionType.USER_GROUP_CHANGE);
    if (util == null) {
      return false;
    }
    BatchRiskExtraFunctionUserGroupChangeUtil.ExtraFunctionUserGroupChangeParam param = util.getParam(extraFunctionsVO);
    return param != null && CollectionUtils.isNotEmpty(param.userGroupChangeList);
  }

  /**
   * 以47的身份放款过的次数
   *
   * @return
   */
  public int getRetrievalPayoutTimes(Long accountId) {
    //1 找到历史上47生效的时间窗口
    List<LoanUserRiskRetrievalValidTimeWindow> retrievalUserTypeValidTimeWindows = getRetrievalUserTypeValidTimeWindows(accountId);

    //2 找到用户打款订单的打款时间
    List<CashLoanOrderVO> orderVOList = ecOrderService.findByAccountIdAndStatuses(accountId, CashLoanOrderStatus.PAYOUT_STATUSES);
    List<Long> timePayoutList = orderVOList
        .stream()
        .map(o -> o.timePayout)
        .collect(Collectors.toList());

    //3 计算打款时间命中userType 47生效的时间窗口次数
    int cnt = 0;
    for (Long timePayout : timePayoutList) {
      for (LoanUserRiskRetrievalValidTimeWindow window : retrievalUserTypeValidTimeWindows) {
        if (timePayout >= window.startTime && timePayout <= window.endTime) {
          cnt++;
        }
      }
    }
    return cnt;
  }

  /**
   * 返回给定 userTypeList 中任意一个 userType 生效的时间窗口列表（不区分具体是哪一个）。
   *
   * @param accountId    账户 ID
   * @param userTypeList 关注的 userType 集合
   */
  private List<LoanUserRiskRetrievalValidTimeWindow> getUserTypeValidTimeWindows(long accountId,
                                                                                 List<String> userTypeList) {

    List<LoanUserRiskRetrievalValidTimeWindow> validTimeWindows = new ArrayList<>();

    List<LoanUserTypeLogRecord> useTypeLogRecordList = loanUserTypeLogModel.findByLoanAccountIdOrderByIdAsc(accountId);
    if (useTypeLogRecordList.isEmpty()) {
      return validTimeWindows;
    }

    Long startTime = null;

    for (LoanUserTypeLogRecord record : useTypeLogRecordList) {
      boolean currentIn = userTypeList.contains(record.getCurrentUserType());
      boolean lastIn = userTypeList.contains(record.getLastUserType());

      if (currentIn && !lastIn) {
        // 进入列表：开始一个新的窗口
        startTime = record.getTimeCreated();
      } else if (!currentIn && lastIn) {
        // 离开列表：结束当前窗口
        if (startTime != null) {
          validTimeWindows.add(LoanUserRiskRetrievalValidTimeWindow.from(startTime, record.getTimeCreated()));
          startTime = null;
        }
      }
    }

    // 如果最后一条记录仍在列表内，则窗口持续到“现在”
    if (startTime != null) {
      validTimeWindows.add(LoanUserRiskRetrievalValidTimeWindow.from(startTime, Long.MAX_VALUE));
    }

    return validTimeWindows;
  }

  /**
   * 返回用户userType为47的时间窗口列表
   *
   * @param accountId
   */
  public List<LoanUserRiskRetrievalValidTimeWindow> getRetrievalUserTypeValidTimeWindows(long accountId) {
    String retrievalUserType = LoanUserTypeVO.RETRIEVAL_USER.code;

    List<LoanUserRiskRetrievalValidTimeWindow> retrievalUserTypeValidTimeWindows = new ArrayList<>();
    List<LoanUserTypeLogRecord> loanUserTypeLogRecordList = loanUserTypeLogModel.findByLoanAccountIdOrderByIdAsc(accountId);
    Long startTime = null;
    Long endTime = null;
    for (LoanUserTypeLogRecord record : loanUserTypeLogRecordList) {

      if (record.getCurrentUserType().equals(retrievalUserType) && !record.getLastUserType().equals(retrievalUserType)) {
        // userType从非47变为47，记录开始时间
        startTime = record.getTimeCreated();
      } else if (!record.getCurrentUserType().equals(retrievalUserType) && record.getLastUserType().equals(retrievalUserType)) {
        // userType从47变为非47，记录结束时间
        endTime = record.getTimeCreated();
        EcAsserts.assertNotNull(startTime, "userType can not be 47 at first, accountId:{}", accountId);
        retrievalUserTypeValidTimeWindows.add(LoanUserRiskRetrievalValidTimeWindow.from(startTime, endTime));
        //重置时间
        startTime = null;
      }
    }

    // 如果最后一个记录的userType为47，且没有结束时间，表示userType为47一直生效到此刻
    if (startTime != null) {
      retrievalUserTypeValidTimeWindows.add(LoanUserRiskRetrievalValidTimeWindow.from(startTime, Long.MAX_VALUE));
    }
    return retrievalUserTypeValidTimeWindows;
  }

  /**
   * --- 续借实验使用 ----
   * 判断用户最新一笔打款订单是否归属回捞
   */
  public boolean isUserLatestOrderBelongRetrieval(Long accountId) {
    CashLoanOrderVO cashLoanOrderVO = ecOrderService.getLatestPayoutOrderVOByPayoutTime(accountId);
    if (cashLoanOrderVO == null) {
      return false;
    }
    RiskOutputResultRecord riskOutputResultRecord = riskOutputResultModel.findByTraceIdAndType(cashLoanOrderVO.traceId, RiskOutputType.USER_TYPE);
    if (riskOutputResultRecord == null) {
      log.error("USER_TYPE in riskOutputResultRecord is null, traceId is {}", cashLoanOrderVO.traceId);
      return false;
    }
    return LoanUserTypeVO.RETRIEVAL_USER.code.equals(riskOutputResultRecord.getNewValue());
  }

  /**
   * --- 续借实验使用 ----
   * 判断用户最新一笔打款订单是否归属主营
   */
  public boolean isUserLatestOrderBelongNormal(Long accountId) {
    CashLoanOrderVO cashLoanOrderVO = ecOrderService.getLatestPayoutOrderVOByPayoutTime(accountId);
    if (cashLoanOrderVO == null) {
      return false;
    }
    RiskOutputResultRecord riskOutputResultRecord = riskOutputResultModel.findByTraceIdAndType(cashLoanOrderVO.traceId, RiskOutputType.USER_TYPE);
    if (riskOutputResultRecord == null) {
      log.error("USER_TYPE in riskOutputResultRecord is null, traceId is {}", cashLoanOrderVO.traceId);
      return false;
    }
    List<String> normalUserGroupExcludeUserType = riskConfig.getNormalUserGroupExcludeUserTypeList();
    //不属于这些userType的，归属于主营
    return !normalUserGroupExcludeUserType.contains(riskOutputResultRecord.getNewValue());
  }


  /**
   * 暂时仅续借使用
   * 新逻辑：看用户等级 && 是否过期
   * 旧逻辑：判断用户最新一笔非续借测额是不是回捞，是的话，用户当前处于回捞流程
   */
  public boolean isCurrentRetrievalUserAndNotExpire(Long userId, Long accountId) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
    if (loanRiskUserGroupVO == null) {
      //如果loanRiskUserGroupVO为null，说明新的升降级代码可能有问题，打个error
      log.info("loanRiskUserGroupVO is null when usingNewFacadeProgress , accountId:{}", accountId);
      LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findLastedCreditsRiskTraceByUserId(userId, LoanUserRiskType.getAllCalcCreditsTypeWithoutMulti());
      return loanUserRiskTraceService.isRetrievalTraceByEcRiskId(loanUserRiskTraceVO.id);
    }

    return loanRiskUserGroupVO.userGroup == LoanRiskUserGroupEnum.RETRIEVAL && !loanRiskUserGroupService.isUserGroupExpire(loanRiskUserGroupVO);

  }

  /**
   * 用户未结清
   * 用户等级是否来源于续借或者循环管制风控trace
   */
  public boolean isUserGroupFromMultiOrRevolvingTraceId(Long accountId) {

    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
    if (loanRiskUserGroupVO == null) {
      //如果loanRiskUserGroupVO为null，说明新的升降级代码可能有问题，打个error
      log.info("loanRiskUserGroupVO is null in method isUserGroupFromMultiOrRevolvingTraceId , accountId:{}", accountId);
      return false;
    }

    if (loanRiskUserGroupVO.traceId == null) {
      return false;
    }

    List<CashLoanOrderVO> undoneOrderVOS = ecOrderService.findByUserIdAndStatuses(loanRiskUserGroupVO.userId, CashLoanOrderStatus.UNDONE_STATUSES);
    if (CollectionUtils.isEmpty(undoneOrderVOS)) {
      return false;
    }
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(loanRiskUserGroupVO.traceId);

    return LoanUserRiskType.getAllMultiLoanRiskType().contains(loanUserRiskTraceVO.riskType)
        || isUserGroupFromRevolvingTraceId(loanUserRiskTraceVO);
  }

  private boolean isUserGroupFromRevolvingTraceId(LoanUserRiskTraceVO loanUserRiskTraceVO) {
    if (!LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(loanUserRiskTraceVO.riskType)) {
      return false;
    }
    RiskOutputResultRecord resultRecord = riskOutputService.findByTraceIdAndType(loanUserRiskTraceVO.traceId, RiskOutputType.REVOLVING_LOAN_CONTROL_DAYS);
    //因为循环进入回捞，但是输出结果为被拒的，保持在回捞
    //因为循环管制进入回捞的才可以升级
    return loanUserRiskTraceVO.creditsStatus == LoanCreditsStatus.ACCEPTED && Objects.nonNull(resultRecord);
  }

  /**
   * 用户等级是否来源于续借风控trace
   */
  public boolean isUserGroupFromMultiTraceId(Long accountId) {

    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
    if (loanRiskUserGroupVO == null) {
      //如果loanRiskUserGroupVO为null，说明新的升降级代码可能有问题，打个error
      log.info("loanRiskUserGroupVO is null in method isUserGroupFromMultiTraceId , accountId:{}", accountId);
      return false;
    }

    if (loanRiskUserGroupVO.traceId == null) {
      return false;
    }

    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(loanRiskUserGroupVO.traceId);

    return LoanUserRiskType.getAllMultiLoanRiskType().contains(loanUserRiskTraceVO.riskType);

  }

  /**
   * 用户等级是否来源于续借风控trace
   */
  public boolean isUserGroupFromRevolvingTraceId(Long accountId) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
    if (loanRiskUserGroupVO == null) {
      //如果loanRiskUserGroupVO为null，说明新的升降级代码可能有问题，打个error
      log.info("loanRiskUserGroupVO is null in method isUserGroupFromMultiTraceId , accountId:{}", accountId);
      return false;
    }

    if (loanRiskUserGroupVO.traceId == null) {
      return false;
    }

    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(loanRiskUserGroupVO.traceId);
    return LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(loanUserRiskTraceVO.riskType);
  }

  /**
   * 回捞/重审打款成功赋管制期入口：管制期仍生效时直接返回，否则按笔次路由首笔全量赋锁 / 非首笔分流赋锁。
   * <p>入口处的「未到期直接返回」与 {@code setUserGroupExpireTimeIfNoActiveControlPeriod} 的幂等边界口径一致
   * （US1-4 / US2-9），赋锁结果不变；提前拦截额外避免锁内用户走非首笔分支被无谓入组 AB 实验（污染实验样本），
   * 并省掉笔次识别所需的订单与风控轨迹批量查询。
   */
  public void handleOrderPayoutSuccessForRetrievalAndReapply(CashLoanOrderVO cashLoanOrderVO) {
    //本轮管制期未到期：周期内已闭环，不再识别笔次、不分流、不赋锁
    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(cashLoanOrderVO.accountId);
    if (loanRiskUserGroupService.isUserGroupInActiveControlPeriod(loanRiskUserGroupVO)) {
      log.info("用户当前管制期未到期，跳过回捞/重审打款赋锁 accountId:{} expireTime:{} currentOrderId:{}",
          cashLoanOrderVO.accountId, loanRiskUserGroupVO.expireTime, cashLoanOrderVO.id);
      return;
    }

    //按打款时间升序取所有落在回捞/重审 userType 窗口内的符合条件放款订单（首笔/非首笔共用同一口径）
    List<CashLoanOrderVO> eligibleOrdersInWindow = getRetrievalOrReapplyNotMultiOrdersInUserTypeWindow(cashLoanOrderVO.accountId);
    if (CollectionUtils.isEmpty(eligibleOrdersInWindow)) {
      log.info("eligibleOrdersInWindow is empty when try handleOrderPayoutSuccessForRetrievalAndReapply, currentOrderId is {}", cashLoanOrderVO.id);
      return;
    }

    //定位当前订单在有序列表中的笔次；不在列表内说明非符合条件放款，不处理
    int currentOrderIndex = indexOfOrderById(eligibleOrdersInWindow, cashLoanOrderVO.id);
    if (currentOrderIndex < 0) {
      log.info("current order is not eligible retrieval/reapply order, currentOrderId is {}", cashLoanOrderVO.id);
      return;
    }

    //第 1 笔为首笔：全量赋锁；第 2 笔及以后为非首笔：走老客复贷 gate + AB 分流
    if (currentOrderIndex == 0) {
      log.info("是首笔回捞/重审订单，赋值管制期 accountId:{} cashLoanOrder id:{}", cashLoanOrderVO.accountId, cashLoanOrderVO.id);
      setRetrievalAndReapplyControlPeriod(cashLoanOrderVO, LoanRiskUserGroupChangeReason.RETRIEVAL_AND_REAPPLY_FIRST_ORDER_PAYOUT_SUCCESS);
      return;
    }
    handleNonFirstOrderPayoutSuccessForRetrievalAndReapply(cashLoanOrderVO);
  }

  /**
   * 非首笔放款分流赋锁：先过「老客复贷 gate」，再用户级 AB 分流，命中实验组(B) 才赋 90 天管制期。
   * <p>对照组/空白组/兜底（{@code BLANK_GROUP}）一律不锁，保证分流异常时安全侧不误锁（US2-2/US2-5）；
   * 仅在放款成功后调用，天然满足「仅放款后分流、仅测额不分流」（US2-7）。
   */
  private void handleNonFirstOrderPayoutSuccessForRetrievalAndReapply(CashLoanOrderVO cashLoanOrderVO) {
    Long accountId = cashLoanOrderVO.accountId;
    //老客复贷 gate：主营新客复贷（账龄 < 阈值）不纳入非首笔实验（US2-4）
    if (!passOldReloanGate(accountId)) {
      log.info("非首笔回捞/重审订单未通过老客复贷 gate，不赋锁 accountId:{} cashLoanOrder id:{}", accountId, cashLoanOrderVO.id);
      return;
    }

    //用户级 AB 分流，平台对同一 userId + expKey 稳定返回，天然复用已入组结果（NFR-001/US2-6）
    String abTestGroup = expDiversionClient.getResult(ExperimentKeyConstants.RETRIEVAL_REAPPLY_NON_FIRST_90D_LOCK,
        ExpUser.builder().userId(cashLoanOrderVO.userId).versionBuild(0L).build());
    log.info("非首笔回捞/重审订单 AB 分流结果:{} accountId:{} cashLoanOrder id:{}", abTestGroup, accountId, cashLoanOrderVO.id);
    if (!NON_FIRST_EXPERIMENT_GROUP_RESULT.equals(abTestGroup)) {
      //对照组/空白组/兜底不赋锁
      return;
    }

    log.info("是非首笔回捞/重审订单且命中实验组，赋值管制期 accountId:{} cashLoanOrder id:{}", accountId, cashLoanOrderVO.id);
    setRetrievalAndReapplyControlPeriod(cashLoanOrderVO, LoanRiskUserGroupChangeReason.RETRIEVAL_AND_REAPPLY_NON_FIRST_ORDER_PAYOUT_SUCCESS);
  }

  /**
   * 老客复贷 gate：判定非首笔用户是否满足入组前置。
   * <p>首笔放款时非主营身份（首笔即在回捞/重审等非主营身份放款，或无打款订单兜底）→ 不受账龄限制，纳入（US2-3）；
   * 首笔放款时主营身份 → 仅当「全局首笔放款距今 ≥ 次新阈值天数（默认 90）」的老客复贷阶段才纳入，
   * 账龄 &lt; 阈值的主营新客复贷排除（US2-4）。口径与 {@code checkSubnewAccountByBillingDate}
   * 的次新判定（{@code days < 阈值} 为次新）对齐：非次新（{@code days >= 阈值}）即老客。
   */
  public boolean passOldReloanGate(Long accountId) {
    boolean normalWhenFirstPayout = subNewAccountService.isNormalUserWhenFirstOrderPayout(
        SubNewAccountRequestVO.from(RiskProcessParam.from(accountId, null, null, null)));
    if (!normalWhenFirstPayout) {
      return true;
    }
    return subNewAccountService.getFirstPayoutToNowDays(accountId) >= subNewConfig.getSubNewUserDays();
  }

  /**
   * 赋予回捞/重审 90 天管制期（失效时间 = 打款时间 + 可配置天数，默认 90），{@code external_id} 落当前订单 id。
   * <p>管制期仍生效（未到期）时不续期、不写等级变更 log——本轮 90 天周期内的重复放款已在周期内闭环；
   * 上一轮管制期已到期时按本笔打款续一轮新管制期并写 log（更新到期时间与 reason），故非首笔锁不止能赋一次。
   */
  private void setRetrievalAndReapplyControlPeriod(CashLoanOrderVO cashLoanOrderVO, LoanRiskUserGroupChangeReason reason) {
    long expireTime = cashLoanOrderVO.timePayout + riskConfig.getRetrievalAndReapplyProgressValidDay() * Clock.MILLS_PER_DAY;
    loanRiskUserGroupService.setUserGroupExpireTimeIfNoActiveControlPeriod(cashLoanOrderVO.accountId, reason, expireTime, String.valueOf(cashLoanOrderVO.id));
  }

  //在有序符合条件订单列表中按订单 id 定位笔次下标，未命中返回 -1
  private int indexOfOrderById(List<CashLoanOrderVO> orderVOList, Long orderId) {
    for (int i = 0; i < orderVOList.size(); i++) {
      if (Objects.equals(orderVOList.get(i).id, orderId)) {
        return i;
      }
    }
    return -1;
  }

  //按打款时间升序返回所有落在回捞/重审 userType 生效窗口内的符合条件放款订单（首笔=第 0 笔）
  private List<CashLoanOrderVO> getRetrievalOrReapplyNotMultiOrdersInUserTypeWindow(Long accountId) {
    List<String> reapplyAndRetrievalUserTypeList = LoanUserTypeVO.RETRIEVAL_AND_REAPPLY_USER_TYPE_LIST.stream()
        .map(vo -> vo.code)
        .collect(Collectors.toList());

    //1 找到用户回捞/重审 userType生效的时间窗口
    List<LoanUserRiskRetrievalValidTimeWindow> validTimeWindows = getUserTypeValidTimeWindows(accountId, reapplyAndRetrievalUserTypeList);
    log.info("getRetrievalOrReapplyNotMultiOrdersInUserTypeWindow validTimeWindows: {}", validTimeWindows);
    //2 找到用户结清场景，重审/回捞所有打款订单，按打款时间排序
    List<CashLoanOrderVO> orderVOList = getAllRetrievalOrReapplyNotMultiOrder(accountId);
    if (CollectionUtils.isEmpty(orderVOList)) {
      return Collections.emptyList();
    }

    // LinkedHashMap 保证按打款时间升序
    orderVOList.sort(Comparator.comparingLong(o -> o.timePayout));

    Map<Long, CashLoanOrderVO> payoutTimeOrderMap = orderVOList.stream()
        .collect(Collectors.toMap(
            o -> o.timePayout,
            Function.identity(),
            (o1, o2) -> o1,           // 相同打款时间订单,取第一个即可
            LinkedHashMap::new));

    //3 依打款时间升序，收集所有命中 userType 生效窗口的订单
    List<CashLoanOrderVO> ordersInWindow = new ArrayList<>();
    for (Map.Entry<Long, CashLoanOrderVO> entry : payoutTimeOrderMap.entrySet()) {
      long timePayout = entry.getKey();
      for (LoanUserRiskRetrievalValidTimeWindow window : validTimeWindows) {
        if (timePayout >= window.startTime && timePayout <= window.endTime) {
          ordersInWindow.add(entry.getValue());
          break;
        }
      }
    }

    return ordersInWindow;
  }

  //找到用户结清重审/回捞所有符合条件的打款订单（按订单风控 riskType ∈ {D,P,B} 过滤，涵盖结清测额 C 场景、天然排续借）
  private List<CashLoanOrderVO> getAllRetrievalOrReapplyNotMultiOrder(Long accountId) {
    List<CashLoanOrderVO> orderVOList = ecOrderService.findByAccountIdAndStatuses(accountId, CashLoanOrderStatus.PAYOUT_STATUSES);
    if (CollectionUtils.isEmpty(orderVOList)) {
      return orderVOList;
    }

    // 取订单自身的订单风控 traceId，批量拉取对应 riskType
    List<Long> orderTraceIds = orderVOList.stream()
        .map(o -> o.traceId)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
    // 全部订单历史无 trace，无需查询，直接不补锁
    if (CollectionUtils.isEmpty(orderTraceIds)) {
      return Collections.emptyList();
    }
    Map<Long, LoanUserRiskType> orderTraceIdToRiskTypeMap = loanUserRiskTraceService.fetchMapByTraceIds(orderTraceIds);

    // 过滤出订单风控 riskType ∈ {D,P,B} 的订单；历史无 trace 的订单不补锁
    return orderVOList.stream()
        .filter(o -> {
          if (Objects.isNull(o.traceId)) {
            return false;
          }
          return isValidOrderTraceForRetrievalAndReapplyNotMultiOrder(orderTraceIdToRiskTypeMap.get(o.traceId));
        })
        .collect(Collectors.toList());
  }

  //符合条件的结清重审/回捞放款订单，对应的订单风控 riskType（D/P/B）
  private Boolean isValidOrderTraceForRetrievalAndReapplyNotMultiOrder(LoanUserRiskType loanUserRiskType) {
    return LoanUserRiskType.RETRIEVAL_REAPPLY_PAYOUT_ORDER_RISK_TYPES.contains(loanUserRiskType);
  }

  /**
   * trace是否为首贷二次风控，并且输出了回捞的userType，即47
   */
  public boolean isLoanOrderTraceAndOutputRetrievalUserType(Long traceId) {

    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceId);
    LoanUserTypeLogVO loanUserTypeLogVO = loanUserService.getByTraceId(traceId);
    if (loanUserTypeLogVO == null) {
      return false;
    }

    return LoanUserRiskType.SECOND == loanUserRiskTraceVO.riskType && LoanUserTypeVO.RETRIEVAL_USER.code.equals(loanUserTypeLogVO.currentUserType);

  }

  //判断历史是否有回捞准入风控(riskType = 1 , 3)通过
  public boolean isUserRetrievalAccessAccept(Long accountId) {
    return loanUserRiskTraceService.findLatestCreditRiskByAccountIdAndRiskTypes(accountId, LoanUserRiskType.getRetrievalRiskTypes()) != null;
  }

  /**
   * 判断当前最新一笔订单是否非续借并且来源于回捞测额
   * riskType = 1
   * riskType = 3
   * riskType = C && userType = 47
   */
  public boolean isRetrievalNotMultiOrder(Long orderId) {

    CashLoanOrderAdditionalInfoVO cashLoanOrderAdditionalInfoVO = cashLoanOrderAdditionalInfoService.findByOrderIdAndType(orderId, OrderAdditionalInfoType.CALC_CREDITS_TRACE_ID);
    if (Objects.isNull(cashLoanOrderAdditionalInfoVO) || StringUtils.isBlank(cashLoanOrderAdditionalInfoVO.info)) {
      log.error("not find CALC_CREDITS_TRACE_ID info from CASH_LOAN_ORDER_ADDITIONAL_INFO by order id :{}", orderId);
      return false;
    }
    Long traceId = Long.parseLong(cashLoanOrderAdditionalInfoVO.info);
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceId);
    return loanUserRiskTraceService.isRetrievalTraceByEcRiskId(loanUserRiskTraceVO.id);
  }

  /**
   * 当用户当前处于常规人群，进行人群降级，将用户类型重置为回捞人群
   *
   * @param accountId 账户ID
   */
  public void degradeUserTypeFromNormalToRetrieval(Long accountId) {
    if (!loanAccountBasicInfoService.isRetrievalUserTypeByTime(accountId, Clock.now())) {
      loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(accountId, null, LoanUserTypeVO.RETRIEVAL_USER, LoanUserTypeChangeReason.RETRIEVAL_CALC_CREDITS_AFTER_ACCESS);
    }
  }

  public boolean needUpgradeWhenAutoCalcRepayAll(Long accountId) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrThrow(accountId);
    if (loanRiskUserGroupVO.userGroup == LoanRiskUserGroupEnum.NORMAL) {
      return false;
    }
    // 回溯管制期源头保级：被拒降级重审后当前 reason 被改写为 PRE_RISK_REJECT，但只要管制期未过期
    // 且源头为回捞/重审打款锁就保级不升级。读取口径由 Service 内部按新开关
    // retrievalReapplyControlPeriodReadBySourceSwitch 切换：开启（回刷完成后灰度）直读
    // expire_time_source_log_id 源头 log 并纳入非首笔源头（US3-1/US3-6）；关闭按 (account_id, expire_time, 首笔) 反查兜底。
    if (!loanRiskUserGroupService.isUserGroupExpire(loanRiskUserGroupVO)
        && loanRiskUserGroupService.isCurrentControlPeriodFromRetrievalAndReapplyFirstOrderPayout(loanRiskUserGroupVO)) {
      log.info("needUpgradeWhenAutoCalcRepayAll keep user group by control period source, accountId:{}, currentReason:{}, expireTime:{}",
          accountId, loanRiskUserGroupVO.reason, loanRiskUserGroupVO.expireTime);
      return false;
    }
    return true;
  }

  public UserGroupTriggerResult handleAutoCalcRepayAllForRetrieval(RiskProcessParam riskProcessParam, LoanRiskUserGroupEnum oldUserGroup, boolean readOnly) {
    if (!needUpgradeWhenAutoCalcRepayAll(riskProcessParam.accountId)) {
      return UserGroupTriggerResult.fromNoUpdate(oldUserGroup);
    }
    LoanRiskUserGroupEnum updatedUserGroup = LoanRiskUserGroupEnum.NORMAL;
    if (readOnly) {
      return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, oldUserGroup);
    }
    handleUserGroupUpgradeToNormal(riskProcessParam, updatedUserGroup,
        LoanRiskUserGroupChangeReason.REPAY_ALL_INSTALMENT,
        LoanUserTypeVO.RETRIEVAL_CALC_CREDITS_EXPIRE_USER,
        LoanUserTypeChangeReason.RETRIEVAL_CALC_CREDITS_EXPIRE_USER_CHANGE);

    return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, oldUserGroup);

  }

  public UserGroupTriggerResult handleAutoCalcRepayAllForReapply(RiskProcessParam riskProcessParam, LoanRiskUserGroupEnum oldUserGroup, boolean readOnly) {
    if (!needUpgradeWhenAutoCalcRepayAll(riskProcessParam.accountId)) {
      return UserGroupTriggerResult.fromNoUpdate(oldUserGroup);
    }

    if (upgradeToNormalAfterReapplyCompletedOrderSwitch(riskProcessParam.accountId)) {
      LoanRiskUserGroupEnum updatedUserGroup = LoanRiskUserGroupEnum.NORMAL;
      if (readOnly) {
        return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, oldUserGroup);
      }
      handleUserGroupUpgradeToNormal(riskProcessParam, updatedUserGroup,
          LoanRiskUserGroupChangeReason.REPAY_ALL_INSTALMENT,
          LoanUserTypeVO.RETRIEVAL_CALC_CREDITS_EXPIRE_USER,
          LoanUserTypeChangeReason.RETRIEVAL_CALC_CREDITS_EXPIRE_USER_CHANGE);

      return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, oldUserGroup);
    }
    //自动结清时，重审升级到回捞
    LoanRiskUserGroupEnum updatedUserGroup = LoanRiskUserGroupEnum.RETRIEVAL;
    if (readOnly) {
      return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, oldUserGroup);
    }
    handleUserGroupUpgradeToRetrieval(riskProcessParam.accountId, updatedUserGroup, LoanRiskUserGroupChangeReason.REPAY_ALL_INSTALMENT);
    return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, oldUserGroup);

  }

  /**
   * 端外被拒用户回端内：根据被拒天数重置 ut 和等级。
   * <p>
   * 调用方已确认：当前来源为端内（!isApiChannelSourceType），上笔 trace 来源为端外（isApiChannelSourceType）。
   * <ul>
   *   <li>复贷用户：不处理，返回 fromNoUpdate(NORMAL)</li>
   *   <li>被拒 ≥180 天：仅重置 ut=I17，等级保持主营不变</li>
   *   <li>被拒 <180 天：重置 ut=I22 + 等级降级为重审</li>
   * </ul>
   *
   * @param riskProcessParam 风控提交参数
   * @param latestTrace      最近一笔风控 trace（调用方已验证来源为端外）
   * @param readOnly         true 时仅计算结果，不写 DB
   * @return 处理结果，始终非 null
   */
  public UserGroupTriggerResult handleApiChannelRejectUserBackToAppForNormal(RiskProcessParam riskProcessParam, LoanUserRiskTraceVO latestTrace, boolean readOnly) {
    if (!businessRiskConfig.getApiChannelRejectBackToAppSwitch()) {
      return UserGroupTriggerResult.fromNoUpdate(LoanRiskUserGroupEnum.NORMAL);
    }
    if (loanAccountBasicInfoService.isReloan(riskProcessParam.accountId)) {
      log.info("handleApiChannelRejectUserBackToApp, isReloan, skip, accountId:{}", riskProcessParam.accountId);
      return UserGroupTriggerResult.fromNoUpdate(LoanRiskUserGroupEnum.NORMAL);
    }

    LoanAccountRecord accountRecord = loanAccountModel.findByIdOrThrow(riskProcessParam.accountId);
    boolean overRiskPeriod = riskFacadeTool.canReapplyBasedOnRiskPeriod(accountRecord);

    if (overRiskPeriod) {
      // 被拒 ≥180 天：重置 ut=I17，等级保持主营
      if (!readOnly) {
        loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(
            riskProcessParam.accountId, null,
            LoanUserTypeVO.REAPPLY_AFTER_INTERVAL_INIT,
            LoanUserTypeChangeReason.API_CHANNEL_REJECT_BACK_TO_APP);
      }
      log.info("handleApiChannelRejectUserBackToApp, overRiskPeriod, reset ut=I17, accountId:{}", riskProcessParam.accountId);
      return UserGroupTriggerResult.fromNoUpdate(LoanRiskUserGroupEnum.NORMAL);
    }

    // 被拒 <180 天：重置 ut=I22 + 降级重审
    if (!readOnly) {
      loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(
          riskProcessParam.accountId, null,
          LoanUserTypeVO.API_CHANNEL_REAPPLY_BACK_TO_APP,
          LoanUserTypeChangeReason.API_CHANNEL_REJECT_BACK_TO_APP);
      loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(
          riskProcessParam.accountId,
          LoanRiskUserGroupEnum.REAPPLY, null,
          LoanRiskUserGroupChangeReason.API_CHANNEL_REJECT_BACK_TO_APP);
    }
    log.info("handleApiChannelRejectUserBackToApp, underRiskPeriod, reset ut=I22 and group=REAPPLY, accountId:{}", riskProcessParam.accountId);
    return UserGroupTriggerResult.fromOnlyUpdated(LoanRiskUserGroupEnum.REAPPLY, LoanRiskUserGroupEnum.NORMAL);
  }

  /**
   * API 渠道首贷被拒满重审周期时按渠道重置 userType；readOnly、非支持渠道或复贷时不执行。
   * <p>渠道映射：Gopay → I18（{@link LoanUserTypeVO#GOPAY_USER}）、Lazada Buyer → I19（{@link LoanUserTypeVO#LAZADA_BUYER_USER}）。
   */
  public void tryUpdateApiChannelFirstLoanUserTypeToReapplyIntervalInit(@Nullable SubmitExtraInfo extraInfo, @Nullable Long accountId, boolean readOnly) {
    SourceType sourceType = extraInfo == null ? null : extraInfo.sourceType;
    boolean checkedResult = riskFacadeTool.canReapplyBasedOnRiskPeriodForApiChannel(accountId, sourceType);
    if (!checkedResult) {
      log.info("tryUpdateApiChannelFirstLoanUserTypeToReapplyIntervalInit checkedResult is false, accountId:{}", accountId);
      return;
    }
    if (readOnly) {
      return;
    }
    updateUserTypeForApiChannelReapplyIntervalInit(accountId, sourceType);
  }

  private void updateUserTypeForApiChannelReapplyIntervalInit(Long accountId, SourceType sourceType) {
    LoanUserTypeVO userType = getReapplyIntervalUserTypeForApiChannel(sourceType);

    loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(
        accountId,
        null,
        userType,
        LoanUserTypeChangeReason.LOAN_REAPPLY_AFTER_INTERVAL);
  }

  private LoanUserTypeVO getReapplyIntervalUserTypeForApiChannel(SourceType sourceType) {
    if (sourceType == SourceType.LAZADA_BUYER) {
      return LoanUserTypeVO.LAZADA_BUYER_USER;
    }
    if (sourceType == SourceType.GOPAY) {
      return LoanUserTypeVO.GOPAY_USER;
    }
    throw EcException.error("unsupported sourceType for getReapplyIntervalUserTypeForApiChannel:" + sourceType);
  }

  //升级到主营
  public void handleUserGroupUpgradeToNormal(RiskProcessParam riskProcessParam, LoanRiskUserGroupEnum updatedUserGroup, LoanRiskUserGroupChangeReason userGroupChangeReason, LoanUserTypeVO userType, LoanUserTypeChangeReason reason) {
    loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(riskProcessParam.accountId, null, userType, reason);
    loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(riskProcessParam.accountId, updatedUserGroup, null, userGroupChangeReason);
  }

  //升级到回捞
  public void handleUserGroupUpgradeToRetrieval(Long accountId, LoanRiskUserGroupEnum updatedUserGroup, LoanRiskUserGroupChangeReason userGroupChangeReason) {
    //改下userType
    loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(accountId, null, LoanUserTypeVO.RETRIEVAL_USER, LoanUserTypeChangeReason.REAPPLY_REPAY_ALL_AUTO_SUBMIT_CREDITS);

    loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(accountId, updatedUserGroup, null, userGroupChangeReason);

  }

  //部分场景，回捞拒绝后，强制升级到主营
//这里无需改userType，风控策略会改为正确的
  public void handleUserGroupRetrievalRejectAndUpgradeToNormal(Long accountId, Long traceId, LoanRiskUserGroupEnum updatedUserGroup, LoanRiskUserGroupChangeReason userGroupChangeReason) {
    loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(accountId, updatedUserGroup, traceId, userGroupChangeReason);

  }

  public boolean upgradeToNormalAfterReapplyCompletedOrderSwitch(Long loanAccountId) {
    if (riskConfig.upgradeToNormalAfterReapplyCompletedOrderWhiteList().contains(loanAccountId)) {
      return true;
    }
    return riskConfig.upgradeToNormalAfterReapplyCompletedOrderSwitch();
  }

  /**
   * API 渠道（Gopay/Lazada）首贷重审测额：满足间隔重申条件时升级主营并按渠道重置 userType；不满足则走常规重审升级逻辑。
   * <p>渠道映射：Gopay → I18、Lazada Buyer → I19。
   */
  @Nullable
  public UserGroupTriggerResult tryHandleApiChannelReapplyAfterIntervalUpgrade(RiskProcessParam riskProcessParam,
                                                                               LoanRiskUserGroupEnum currentGroup,
                                                                               boolean readOnly) {
    if (!riskFacadeTool.shouldUpgradeToNormalBeforeApiChannelReapplyCalcCredits(riskProcessParam)) {
      return handleReapplyUserGroupUpgrade(riskProcessParam, currentGroup, riskProcessParam.extraInfo, riskProcessParam.build, readOnly);
    }
    LoanRiskUserGroupEnum updatedUserGroup = LoanRiskUserGroupEnum.NORMAL;
    if (readOnly) {
      return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, currentGroup);
    }
    handleUserGroupUpgradeToNormal(
        riskProcessParam,
        updatedUserGroup,
        LoanRiskUserGroupChangeReason.LOAN_REAPPLY_AFTER_INTERVAL,
        getReapplyIntervalUserTypeForApiChannel(riskProcessParam.extraInfo.sourceType),
        LoanUserTypeChangeReason.LOAN_REAPPLY_AFTER_INTERVAL);
    return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, currentGroup);
  }

  public UserGroupTriggerResult handleReapplyUserGroupUpgrade(RiskProcessParam riskProcessParam, LoanRiskUserGroupEnum sourceGroup, SubmitExtraInfo extraInfo, Long build, boolean readOnly) {
    LoanAccountRecord accountRecord = loanAccountModel.findByIdOrThrow(riskProcessParam.accountId);
    Boolean result = abTestVersionConfigService.getLoanDaysReapply(accountRecord, ResultGetType.LAST_RESULT, extraInfo, build);

    if (Boolean.TRUE.equals(result)) {
      LoanRiskUserGroupEnum updatedUserGroup = LoanRiskUserGroupEnum.NORMAL;

      if (readOnly) {
        return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, sourceGroup);
      }

      handleUserGroupUpgradeToNormal(
          riskProcessParam,
          updatedUserGroup,
          LoanRiskUserGroupChangeReason.LOAN_REAPPLY_AFTER_INTERVAL,
          LoanUserTypeVO.REAPPLY_AFTER_INTERVAL_INIT,
          LoanUserTypeChangeReason.LOAN_REAPPLY_AFTER_INTERVAL
      );

      return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, sourceGroup);
    }

    // 小于180天 ，如果是端外被拒回端场景，不升级，仅重置 ut=I22
    tryResetUserTypeForApiChannelRejectBackToApp(riskProcessParam, extraInfo, readOnly);

    return UserGroupTriggerResult.fromNoUpdate(sourceGroup);
  }

  /**
   * 端外被拒用户回端但不满足升级条件（<180天）时，重置 ut=I22。
   * <p>
   * 仅当前来源为端内（!isApiChannelSourceType）且上笔 trace 来源为端外（isApiChannelSourceType）时才执行。
   */
  private void tryResetUserTypeForApiChannelRejectBackToApp(RiskProcessParam riskProcessParam, SubmitExtraInfo extraInfo, boolean readOnly) {
    if (!businessRiskConfig.getApiChannelRejectBackToAppSwitch()) {
      return;
    }
    if (extraInfo == null || extraInfo.sourceType == null || extraInfo.sourceType.isApiChannelSourceType()) {
      return;
    }
    if (loanAccountBasicInfoService.isReloan(riskProcessParam.accountId)) {
      return;
    }
    LoanUserRiskTraceVO latestTrace = loanUserRiskTraceService.findLatestCreditRiskByAccountId(riskProcessParam.accountId);
    if (latestTrace == null || latestTrace.sourceType == null || !latestTrace.sourceType.isApiChannelSourceType()) {
      return;
    }
    if (readOnly) {
      return;
    }
    log.info("tryResetUserTypeForApiChannelRejectBackToApp, reset ut=I22, accountId:{}", riskProcessParam.accountId);
    loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(
        riskProcessParam.accountId, null,
        LoanUserTypeVO.API_CHANNEL_REAPPLY_BACK_TO_APP,
        LoanUserTypeChangeReason.API_CHANNEL_REJECT_BACK_TO_APP);
  }

  public boolean needDegradeAfterRiskAcceptWithRetrievalUserType(LoanUserRiskTraceVO traceVO) {
    return isRiskTypeAcceptedWithRetrievalUserType(traceVO, LoanUserRiskType.SECOND)
        || isRiskTypeAcceptedWithRetrievalUserType(traceVO, LoanUserRiskType.LOAN_RE_ORDER_CREDITS);
  }

  public boolean isRiskTypeAcceptedWithRetrievalUserType(LoanUserRiskTraceVO traceVO, LoanUserRiskType riskType) {
    return riskType == traceVO.riskType
        && traceVO.creditsStatus == LoanCreditsStatus.ACCEPTED
        && loanAccountService.isRetrievalUserTypeByTime(traceVO.accountId, Clock.now());
  }

  /**
   * 处理常规用户-次新用户 userType 变更逻辑
   */
  public void handleCommonSubNewUserTyperForNormalUser(RiskProcessParam riskProcessParam) {
    //用户已经回到主营，开始次新逻辑校验
    LoanAccountRecord record = accountModel.findByIdOrThrow(riskProcessParam.accountId);
    LoanUserTypeVO lastLoanUserTypeVO = loanUserTypeService.fromUserTypeCode(record.getUserType());
    SubNewAccountResultVO subNewAccountBillingDateVO = subNewAccountService.checkSubnewAccountByIntervalDays(SubNewAccountRequestVO.from(riskProcessParam));
    if (subNewAccountBillingDateVO.isSubNewUser) {
      handelUserTypeAboutSubNew(riskProcessParam.accountId, LoanUserTypeVO.SUB_NEW_USER);
      return;
    }
    if (LoanUserTypeVO.SUB_NEW_USER.code.equals(lastLoanUserTypeVO.code)) {
      LoanUserTypeVO notSubNewUserType = loanUserTypeService.fromUserTypeCode(subNewAccountService.getNormalProcessExitSubNewUserType());
      if (Objects.isNull(notSubNewUserType)) {
        log.error("not found normal process not sub new user type and i15 :{},accountId is {}.", subNewAccountService.getNormalProcessExitSubNewUserType(), riskProcessParam.accountId);
        return;
      }
      handelUserTypeAboutSubNew(riskProcessParam.accountId, notSubNewUserType);
    }
  }

  public void handelUserTypeAboutSubNew(Long accountId, LoanUserTypeVO userType) {
    loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(accountId, null, userType, LoanUserTypeChangeReason.ENTER_OR_EXIT_SUB_NEW_PROCESS);
    log.info("loanAccountBasicInfoService.handelUserTypeToSubNew, accountId:{}", accountId);
  }

  public void handleSubNewUserTyperForNormalUser(RiskProcessParam riskProcessParam, LoanUserRiskType riskType) {
    if (LoanUserRiskType.getAllSubNewCalcRiskTypeList().contains(riskType)) {
      handleCommonSubNewUserTyperForNormalUser(riskProcessParam);
    }
  }
}
