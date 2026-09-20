package com.yqg.core.service.risk.usergroup;

import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.LoanRiskUserGroupLogRecord;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.LoanRiskUserGroupLogModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.model.generated.tables.records.RiskOutputResultRecord;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.model.core.OperationLogRecordVo;
import com.yqg.core.model.sql.loan.account.enums.LogEventType;
import com.yqg.core.model.sql.loan.account.enums.ObjType;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.operationlog.OperationLogService;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.core.service.loan.vo.LoanUserSimpleCreditsInfoVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.risk.facade.RiskFacadeTool;
import com.yqg.core.service.risk.facade.RiskTypeTool;
import com.yqg.core.service.risk.feature.BusinessRiskConfig;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.riskoutput.RiskOutputService;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.service.risk.usergroup.vo.UserGroupLogVO;
import com.yqg.core.service.risk.usergroup.vo.UserGroupTriggerResult;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 风控人群升降级相关
 *
 * @author chaoye
 * @date 2025/6/13
 */
@Service
@Slf4j
public class RiskUserGroupEntranceService {

  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private BusinessRiskConfig businessRiskConfig;
  @Autowired
  private RiskFacadeTool riskFacadeTool;
  @Autowired
  private LoanRiskUserGroupMonitorService loanRiskUserGroupMonitorService;
  @Autowired
  private RiskUserGroupTool riskUserGroupTool;
  @Autowired
  private RiskUserGroupProcessorFactory riskUserGroupProcessorFactory;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;
  @Autowired
  private RiskTypeTool riskTypeTool;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  @Autowired
  private LoanRiskUserGroupLogModel loanRiskUserGroupLogModel;
  @Autowired
  private RiskUserGroupCheckDecisionService riskUserGroupCheckDecisionService;
  @Autowired
  private RiskOutputService riskOutputService;
  @Autowired
  private OperationLogService operationLogService;

  public LoanRiskUserGroupVO initRiskUserGroup(Long accountId) {
    return threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord loanAccountRecord = loanAccountModel.findByIdForUpdateOrThrow(accountId);
      LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
      if (loanRiskUserGroupVO != null) {
        return loanRiskUserGroupVO;
      }

      return loanRiskUserGroupService.insertWithNoExpire(loanAccountRecord.getUserId(), accountId, LoanRiskUserGroupEnum.NORMAL, null, LoanRiskUserGroupChangeReason.INIT);
    });
  }

  //仅允许新facade代码调用！！
  // 功能：查询用户当前的等级，如果之前未记录等级，说明是以下两种情况
  // 1 提交完件风控用户，初始化等级
  // 2 历史用户首次进入facade代码，手动回刷用户等级
  public LoanRiskUserGroupVO backfillOrInitLoanRiskUserGroupVOByAccountIdForNewCode(Long accountId, LoanUserRiskSubmitScene submitScene) {

    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
    LoanAccountRecord loanAccountRecord = loanAccountModel.findByIdOrThrow(accountId);

    //历史不存在
    if (loanRiskUserGroupVO == null) {
      //用户 完件前风控/完件风控时 初始化用户等级
      if (submitScene == LoanUserRiskSubmitScene.BEFORE_AUTH_FINISH_CREDITS || submitScene == LoanUserRiskSubmitScene.AUTH_CALC_CREDITS) {
        return initRiskUserGroup(accountId);
      }
      LoanRiskUserGroupEnum loanRiskUserGroupEnum = getLoanRiskUserGroupEnumForBackfill(accountId);
      loanRiskUserGroupVO = loanRiskUserGroupService.insertWithNoExpire(loanAccountRecord.getUserId(), accountId, loanRiskUserGroupEnum, null, LoanRiskUserGroupChangeReason.BACKFILL);
      return loanRiskUserGroupVO;
    }
    //历史存在
    if (needRefreshBackfill(loanRiskUserGroupVO)) {
      //需要重新回刷
      LoanRiskUserGroupEnum loanRiskUserGroupEnum = getLoanRiskUserGroupEnumForBackfill(accountId);
      return loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(accountId, loanRiskUserGroupEnum, null, LoanRiskUserGroupChangeReason.BACKFILL);
    } else {
      //不需要重新回刷
      return loanRiskUserGroupVO;
    }

  }

  private boolean needRefreshBackfill(LoanRiskUserGroupVO loanRiskUserGroupVO) {
    //是否重新回刷已存在的等级
    boolean refreshUserGroup = riskConfig.getRiskFacadeRefreshUserGroupSwitch();
    if (!refreshUserGroup) {
      return false;
    }
    //旧等级在什么时间节点之前，需要重新回刷
    long refreshUserGroupStartTime = riskConfig.getRiskFacadeRefreshUserGroupStartTime();
    return loanRiskUserGroupVO.timeUpdated < refreshUserGroupStartTime;
  }

  public LoanRiskUserGroupEnum getLoanRiskUserGroupEnumForBackfill(Long accountId) {
    //最新的trace，包括测额和订单风控
    LoanUserRiskTraceVO latestTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(accountId);
    //最新的测额trace
    LoanUserRiskTraceVO latestCalcCreditsTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountIdAndRiskTypes(accountId, LoanUserRiskType.getAllCalcCreditsType());

    //首先判断重审
    if (isReapplyForBackfill(latestTraceVO)) {
      return LoanRiskUserGroupEnum.REAPPLY;
    }
    //判断回捞
    if (isRetrievalForBackfill(latestTraceVO, latestCalcCreditsTraceVO)) {
      return LoanRiskUserGroupEnum.RETRIEVAL;
    }
    //其余的是主营
    return LoanRiskUserGroupEnum.NORMAL;
  }

  public boolean isReapplyForBackfill(LoanUserRiskTraceVO latestTraceVO) {
    //先判断风控输出是否为拒绝
    if (latestTraceVO.creditsStatus == LoanCreditsStatus.REJECTED) {
      return true;
    }
    //判断授信状态是否为拒绝（状态可能会通过admin或者用户为管制状态下进行变更）
    LoanUserSimpleCreditsInfoVO userSimpleCreditsInfoVO = loanUserCreditsService.getLoanUserSimpleCreditsInfoVOByUserId(latestTraceVO.userId);
    if (Objects.nonNull(userSimpleCreditsInfoVO) && userSimpleCreditsInfoVO.isCreditsReject()) {
      return true;
    }
    //最近一次非续借测额是重审
    if (!businessRiskConfig.getRiskUserGroupBackfillNewLogicSwitch()) {
      LoanUserRiskTraceVO latestCalcCreditsWithoutMultiTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountIdAndRiskTypes(latestTraceVO.accountId, LoanUserRiskType.getAllCalcCreditsTypeWithoutMulti());
      if (LoanUserRiskType.getAllReapplyRiskTypes().contains(latestCalcCreditsWithoutMultiTraceVO.riskType)) {
        return true;
      }
    }
    //判断 当前userType是否为归属重审的特定值
    LoanUserTypeVO loanUserTypeVO = loanAccountBasicInfoService.getUserTypeByAccountId(latestTraceVO.accountId);
    return riskConfig.getUserGroupBackfillReapplyUserTypeList().contains(loanUserTypeVO.code);
  }

  public boolean isRetrievalForBackfill(LoanUserRiskTraceVO latestTraceVO, LoanUserRiskTraceVO latestCalcCreditsTraceVO) {
    //回捞riskType通过
    if (LoanUserRiskType.getRetrievalRiskTypes().contains(latestCalcCreditsTraceVO.riskType) && latestTraceVO.creditsStatus == LoanCreditsStatus.ACCEPTED) {
      return true;
    }
    //最新trace是特定riskType风控通过，并且输出47,直接降级为回捞
    if (riskUserGroupTool.needDegradeAfterRiskAcceptWithRetrievalUserType(latestTraceVO)) {
      return true;
    }
    //最新的风控是以userType为47的身份跑的，并且风控结果通过
    if (latestTraceVO.creditsStatus == LoanCreditsStatus.ACCEPTED && loanAccountBasicInfoService.isRetrievalUserTypeByTime(latestTraceVO.accountId, latestTraceVO.timeCreated)) {
      return true;
    }
    //当前userType为I14、47
    if (businessRiskConfig.getRiskUserGroupBackfillNewLogicSwitch()) {
      LoanUserTypeVO loanUserTypeVO = loanAccountBasicInfoService.getUserTypeByAccountId(latestTraceVO.accountId);
      if (LoanUserTypeVO.FIRST_RETRIEVAL_INIT.code.equals(loanUserTypeVO.code) || LoanUserTypeVO.RETRIEVAL_USER.code.equals(loanUserTypeVO.code)) {
        return true;
      }
    }
    return false;
  }

  public UserGroupTriggerResult triggerBeforeSubmitRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, LoanUserRiskType riskType) {
    return triggerBeforeSubmitRisk(riskProcessParam, submitScene, riskType, false);
  }

  public UserGroupTriggerResult triggerBeforeSubmitRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, LoanUserRiskType riskType, boolean readOnly) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = backfillOrInitLoanRiskUserGroupVOByAccountIdForNewCode(riskProcessParam.accountId, submitScene);
    loanRiskUserGroupVO = riskUserGroupTool.handleUserFromHistoryApiChannel(riskProcessParam, loanRiskUserGroupVO);

    String oldUserType = getUserTypeCode(riskProcessParam.accountId);

    LoanRiskUserGroupEnum currentUserGroup = loanRiskUserGroupVO.userGroup;
    IRiskUserGroupProcessor processor = riskUserGroupProcessorFactory.getInstance(currentUserGroup);
    UserGroupTriggerResult userGroupTriggerResult = processor.triggerBeforeSubmitRisk(riskProcessParam, submitScene, readOnly);
    if (readOnly) {
      userGroupTriggerResult.setOldUserType(oldUserType);
      return userGroupTriggerResult;
    }
    if (Objects.isNull(riskType)) {
      riskType = riskTypeTool.getRiskType(riskProcessParam.accountId, submitScene, riskProcessParam);
    }

    processor.triggerBeforeSubmitRiskForChangeSubNewUserType(riskProcessParam, riskType);

    fillUserType(userGroupTriggerResult, oldUserType, getUserTypeCode(riskProcessParam.accountId));
    return userGroupTriggerResult;
  }

  public UserGroupTriggerResult triggerAfterRisk(LoanUserRiskTraceVO previousRiskTraceVO, LoanUserTagData data) {
    // 1 对于没有等级的用户，backfill回刷等级
    LoanRiskUserGroupVO loanRiskUserGroupVO = backfillOrInitLoanRiskUserGroupVOByAccountIdForNewCode(previousRiskTraceVO.accountId, null);

    String oldUserType = getUserTypeCode(previousRiskTraceVO.accountId);

    // 2 修复数据
    loanRiskUserGroupVO = repairUserGroupDataForTriggerRetrieval(previousRiskTraceVO.accountId, previousRiskTraceVO.traceId, loanRiskUserGroupVO, previousRiskTraceVO.riskType);

    // 3 执行风控后的升降级
    UserGroupTriggerResult userGroupTriggerResult = executeUserGroupChangeAfterRisk(previousRiskTraceVO, data, loanRiskUserGroupVO);

    // 4 升降级结束后校验
    riskUserGroupCheckDecisionService.checkUserGroup(previousRiskTraceVO, previousRiskTraceVO.accountId);

    fillUserType(userGroupTriggerResult, oldUserType, getUserTypeCode(previousRiskTraceVO.accountId));
    return userGroupTriggerResult;
  }

  private UserGroupTriggerResult executeUserGroupChangeAfterRisk(LoanUserRiskTraceVO previousRiskTraceVO, LoanUserTagData data, LoanRiskUserGroupVO loanRiskUserGroupVO) {
    //非主流程风控，不进行升降级
    if (!LoanUserRiskType.getAllUserCreditsRiskTypes().contains(previousRiskTraceVO.riskType)) {
      return UserGroupTriggerResult.fromNoUpdate(loanRiskUserGroupVO.userGroup);
    }
    LoanRiskUserGroupEnum currentUserGroup = loanRiskUserGroupVO.userGroup;
    IRiskUserGroupProcessor processor = riskUserGroupProcessorFactory.getInstance(currentUserGroup);

    return processor.triggerAfterRisk(previousRiskTraceVO, data);
  }

  //TODO 等全量修复后，删除此修复逻辑
  // 修复逻辑：
  // 如果
  // 1 当前风控结果是不可借
  // 2 风控前后userType不变，并且userType值不为47、i14、59、63、65
  // 3 当前用户等级不为主营
  // 执行：将【用户等级】字段刷成【主营】，刷数变更原因记录【触发回捞场景自动修复】
  private LoanRiskUserGroupVO repairUserGroupDataForTriggerRetrieval(Long accountId, Long traceId, LoanRiskUserGroupVO loanRiskUserGroupVO, LoanUserRiskType riskType) {
    List<String> allowRiskTypes = riskConfig.getTriggerRetrievalRepairUserGroupRiskTypeList();
    if (!allowRiskTypes.contains(riskType.code)) {
      return loanRiskUserGroupVO;
    }

    if (riskFacadeTool.canGetLoanByTraceOutput(traceId)) {
      return loanRiskUserGroupVO;
    }

    RiskOutputResultRecord userTypeOutputRes = riskOutputService.findByTraceIdAndType(traceId, RiskOutputType.USER_TYPE);
    if (Objects.isNull(userTypeOutputRes)) {
      return loanRiskUserGroupVO;
    }
    String oldValue = userTypeOutputRes.getOldValue();
    String newValue = userTypeOutputRes.getNewValue();
    List<String> excludeValues = riskConfig.getTriggerRetrievalRepairUserGroupExcludeUserTypeList();
    if (Objects.equals(oldValue, newValue) && !excludeValues.contains(newValue) && loanRiskUserGroupVO.userGroup != LoanRiskUserGroupEnum.NORMAL) {
      log.info("triggerAfterRisk repairUserGroupDataForTriggerRetrieval accountId:{}, traceId:{}, oldValue:{}, newValue:{}, currentUserGroup:{}", accountId, traceId, oldValue, newValue, loanRiskUserGroupVO.userGroup);
      loanRiskUserGroupMonitorService.logRepairUserGroupDataForTriggerRetrieval(loanRiskUserGroupVO.userGroup,
          oldValue,
          loanRiskUserGroupVO.accountId,
          riskType,
          traceId);

      if (!riskConfig.getTriggerRetrievalRepairUserGroupDataSwitch()) {
        return loanRiskUserGroupVO;
      }

      return loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(accountId, LoanRiskUserGroupEnum.NORMAL, traceId, LoanRiskUserGroupChangeReason.REPAIR_FOR_TRIGGER_RETRIEVAL_RISK, String.valueOf(traceId));
    }

    return loanRiskUserGroupVO;
  }


  public UserGroupTriggerResult onOrderPayoutSuccess(CashLoanOrderVO cashLoanOrderVO) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = backfillOrInitLoanRiskUserGroupVOByAccountIdForNewCode(cashLoanOrderVO.accountId, null);

    String oldUserType = getUserTypeCode(cashLoanOrderVO.accountId);

    LoanRiskUserGroupEnum currentUserGroup = loanRiskUserGroupVO.userGroup;
    IRiskUserGroupProcessor processor = riskUserGroupProcessorFactory.getInstance(currentUserGroup);
    UserGroupTriggerResult userGroupTriggerResult = processor.onOrderPayoutSuccess(cashLoanOrderVO);

    fillUserType(userGroupTriggerResult, oldUserType, getUserTypeCode(cashLoanOrderVO.accountId));
    return userGroupTriggerResult;
  }

  private String getUserTypeCode(Long accountId) {
    return loanAccountBasicInfoService.getUserTypeByAccountId(accountId).code;
  }

  private void fillUserType(UserGroupTriggerResult result, String oldUserType, String currentUserType) {
    result.setOldUserType(oldUserType);
    result.setUpdatedUserType(Objects.equals(oldUserType, currentUserType) ? null : currentUserType);
  }

  public LoanRiskUserGroupVO getLoanRiskUserGroupVOByAccountIdOrNull(Long accountId) {
    return loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
  }

  public Map<Long, LoanRiskUserGroupVO> getLoanRiskUserGroupVOsByAccountIds(List<Long> accountIds) {
    return loanRiskUserGroupService.getLoanRiskUserGroupVOsByAccountIds(accountIds);
  }

  public LoanRiskUserGroupVO getLoanRiskUserGroupVOByAccountIdOrThrow(Long accountId) {
    return loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrThrow(accountId);
  }

  //判断当前用户等级是否有过期时间
  public boolean isUserGroupHavingExpireTimeByAccountId(Long accountId) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
    if (loanRiskUserGroupVO == null) {
      return false;
    }

    return loanRiskUserGroupService.isUserGroupHavingExpireTimeByAccountId(loanRiskUserGroupVO);
  }

  /**
   * 检查用户是否是回捞/重审用户，且额度已经过期
   */
  public boolean checkUserGroupExpireByAccountId(Long accountId) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
    if (Objects.isNull(loanRiskUserGroupVO)) {
      return false;
    }
    return checkUserGroupCreditsExpireByGroupVO(loanRiskUserGroupVO);
  }

  public boolean checkUserGroupCreditsExpireByGroupVO(LoanRiskUserGroupVO loanRiskUserGroupVO) {
    if (Objects.isNull(loanRiskUserGroupVO)) {
      return false;
    }

    return loanRiskUserGroupService.isUserGroupExpire(loanRiskUserGroupVO);
  }

  public LoanRiskUserGroupVO insertOrUpdateLoanRiskUserGroupWithNoExpire(Long userId,
                                                                         Long accountId,
                                                                         LoanRiskUserGroupEnum userGroup,
                                                                         Long traceId,
                                                                         LoanRiskUserGroupChangeReason reason) {
    return loanRiskUserGroupService.insertOrUpdateLoanRiskUserGroupWithNoExpire(userId, accountId, userGroup, traceId, reason);
  }

  /**
   * Admin 批量修改用户等级专用：与 {@link #insertOrUpdateLoanRiskUserGroupWithNoExpire} 业务语义相同，
   * 但返回新写入的 loan_risk_user_group_log 记录 ID，供调用方写 operation_log 时用作 objId。
   *
   * @return 新写入的 loan_risk_user_group_log 记录 ID
   */
  public Long insertOrUpdateLoanRiskUserGroupWithNoExpireAndGetLogId(Long userId,
                                                                     Long accountId,
                                                                     LoanRiskUserGroupEnum userGroup,
                                                                     Long traceId,
                                                                     LoanRiskUserGroupChangeReason reason) {
    return loanRiskUserGroupService.insertOrUpdateLoanRiskUserGroupWithNoExpireAndGetLogId(userId, accountId, userGroup, traceId, reason);
  }

  /**
   * 分页查询用户组变更日志，并从 operation_log 补充 admin 批量操作的备注和操作人邮箱。
   * <p>
   * 匹配策略：以 {@code ObjType.LOAN_RISK_USER_GROUP_LOG} + log 记录 ID 精确关联 operation_log。
   */
  @SuppressWarnings("unchecked")
  public List<UserGroupLogVO> getUserGroupLogPage(Integer pageNo, Integer pageSize, Long accountId) {
    if (pageNo <= 0) {
      pageNo = 1;
    }
    if (pageSize <= 0) {
      pageSize = 10;
    }

    int offset = (pageNo - 1) * pageSize;
    List<LoanRiskUserGroupLogRecord> records = loanRiskUserGroupLogModel.findAllByAccountIdOrderByTimeCreatedDescWithPagination(accountId, offset, pageSize);
    List<UserGroupLogVO> vos = records.stream()
        .map(UserGroupLogVO::from)
        .collect(Collectors.toList());

    // 收集本页所有 logId，一次性批量查询 operation_log，再内存回填，避免 N+1
    List<Long> logIds = vos.stream().map(UserGroupLogVO::getLogId).collect(Collectors.toList());
    Map<Long, OperationLogRecordVo> opLogMap = operationLogService.findByObjTypeAndLogEventTypeIn(
        ObjType.LOAN_RISK_USER_GROUP_LOG, LogEventType.BATCH_UPDATE_RISK_USER, logIds);
    for (UserGroupLogVO vo : vos) {
      OperationLogRecordVo opLog = opLogMap.get(vo.getLogId());
      if (opLog != null) {
        enrichVoFromOpLog(vo, opLog);
      }
    }

    return vos;
  }

  /**
   * 将 operation_log 中的备注和操作人邮箱填充到 VO；operation_log 的 contentAfter 为
   * {@code {"action":"…","operatorEmail":"…"}} 格式的 JSON。
   */
  private void enrichVoFromOpLog(UserGroupLogVO vo, OperationLogRecordVo opLog) {
    vo.setChangeReasonRemark(opLog.getReason());
    try {
      Map<String, String> contentAfter = JsonUtils.from(opLog.getContentAfter(), Map.class);
      if (contentAfter != null && contentAfter.containsKey("operatorEmail")) {
        vo.setOperatorEmail(contentAfter.get("operatorEmail"));
      }
    } catch (Exception e) {
      log.warn("Failed to parse contentAfter for operatorEmail, logId={}", vo.getLogId(), e);
    }
  }

  /**
   * 获取用户组变更日志总数
   *
   * @param accountId 贷款账户ID
   * @return 总记录数
   */
  public Integer getUserGroupLogCount(Long accountId) {
    return loanRiskUserGroupLogModel.countByAccountId(accountId);
  }

  /**
   * 判断用户最新一笔非续借测额是不是回捞，是的话，用户当前处于回捞流程
   */
  public boolean isCurrentRetrievalUser(Long userId) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findLastedCreditsRiskTraceByUserId(userId, LoanUserRiskType.getAllCalcCreditsTypeWithoutMulti());

    return loanUserRiskTraceService.isRetrievalTraceByEcRiskId(loanUserRiskTraceVO.id);
  }

  // 提供一个带默认值的方法，当数据库中没有记录时返回backfill逻辑的默认值
  public LoanRiskUserGroupEnum getLoanRiskUserGroupVOByAccountIdWithFallback(Long accountId) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = getLoanRiskUserGroupVOByAccountIdOrNull(accountId);
    if (Objects.isNull(loanRiskUserGroupVO)) {
      return getLoanRiskUserGroupEnumForBackfill(accountId);
    }
    return loanRiskUserGroupVO.userGroup;
  }
}
