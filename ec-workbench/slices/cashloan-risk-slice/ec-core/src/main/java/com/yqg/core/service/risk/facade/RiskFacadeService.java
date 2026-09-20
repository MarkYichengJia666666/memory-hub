package com.yqg.core.service.risk.facade;

import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.RevolvingChangeSource;
import com.yqg.core.model.sql.loan.account.RevolvingStatus;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsRejectedReason;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.LoanUserLevel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.service.cashloan.CashLoanCreditsService;
import com.yqg.core.service.cashloan.ExecuteAutoReviewResultLock;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.risk.monitor.RiskMonitor;
import com.yqg.core.service.cashloan.risk.vo.EventTypeVO;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.AutoReviewResult;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.LoanUserEventService;
import com.yqg.core.service.loan.account.RevolvingUserGroupAbtestService;
import com.yqg.core.service.loan.account.enums.RevolvingTriggerSource;
import com.yqg.core.service.loan.account.vo.RevolvingV2MonitorVO;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.loan.credits.CreditsStatusChangeSource;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.facade.dwlog.PanoramaRiskDwLogService;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.loan.vo.ProductTypeCodeVO;
import com.yqg.core.service.risk.RiskApplicationSubmitService;
import com.yqg.core.service.risk.batchtrigger.BatchRiskTriggerReasonTool;
import com.yqg.core.service.risk.batchtrigger.enums.BatchTriggerBlockReason;
import com.yqg.core.service.risk.batchtrigger.vo.BatchBlockReasonResult;
import com.yqg.core.service.risk.feature.CallRiskApiPosition;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.usergroup.RiskUserGroupEntranceService;
import com.yqg.core.service.risk.usergroup.vo.UserGroupTriggerResult;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.core.util.common.RenamedThreadFactory;
import com.yqg.ec.common.enums.TagOperatorTypeEnum;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.PreTraceTriggerScene;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.utils.SysEnvironment;
import com.yqg.overseasrisk.client.mesh.api.IRiskFlowTraceService;

import com.yqg.risk.riskflow.RiskFlowService;
import com.yqg.risk.riskflow.trace.RiskFlowTraceVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 风控管理模块，提供风控核心功能调用
 *
 * @author chaoye
 * @date 2025/6/3
 */
@Slf4j
@Service
public class RiskFacadeService {
  @Autowired
  private LoanAccountModel accountModel;
  @Autowired
  private RiskResultPostEventService riskResultPostEventService;
  @Autowired
  private RiskUserGroupEntranceService riskUserGroupEntranceService;
  @Autowired
  private BatchRiskTriggerReasonTool batchRiskTriggerReasonTool;
  @Autowired
  private RiskApplicationSubmitService riskApplicationSubmitService;
  @Autowired
  private CashLoanCreditsService cashLoanCreditsService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private ExecuteAutoReviewResultLock executeAutoReviewResultLock;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private IRiskFlowTraceService iRiskFlowTraceService;
  @Autowired
  private RiskFlowService riskFlowService;
  @Autowired
  private LoanUserEventService loanUserEventService;
  @Autowired
  private RiskMonitor riskMonitor;
  @Autowired
  private RiskTypeTool riskTypeTool;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private RevolvingUserGroupAbtestService revolvingUserGroupAbtestService;
  @Autowired
  private PanoramaRiskDwLogService panoramaRiskDwLogService;
  @Autowired
  private RiskFacadeTool riskFacadeTool;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;


  private static final ExecutorService handleRiskResultExecutor = new ThreadPoolExecutor(2,
      5,
      1L, TimeUnit.MINUTES,
      new LinkedBlockingQueue<>(200),
      new RenamedThreadFactory(RiskFacadeService.class.getSimpleName()),
      new ThreadPoolExecutor.CallerRunsPolicy());

  /**
   * 根据LoanUserRiskSubmitScene提交风控
   * TODO 这里先更新人群是考虑到历史用户可能没有等级，需要先回刷，后续可以优化为先获取riskType
   *
   * @param riskProcessParam
   * @param submitScene
   */
  public boolean submitRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene) {
    return threadTransactionalModel.transactionResult(configuration -> {
      log.info("submitRisk start, accountId is {},submitScene is {}", riskProcessParam.accountId, submitScene);

      //加锁
      accountModel.findByIdForUpdateOrThrow(riskProcessParam.accountId);

      //1 更新人群
      UserGroupTriggerResult userGroupTriggerResult = riskUserGroupEntranceService.triggerBeforeSubmitRisk(riskProcessParam, submitScene, null);
      if (Objects.nonNull(userGroupTriggerResult)) {
        log.info("submitRisk triggerBeforeSubmitRisk success, accountId is {},submitScene is {},nextRiskSubmitScene is {}", riskProcessParam.accountId,
            submitScene,
            userGroupTriggerResult.nextRiskSubmitScene);
      }

      //2 获取riskType  todo (ltb,T000000) 获取 riskType 正常来说应该可以在第一步就获取，现在打破了这个规则，后面看看怎么修
      LoanUserRiskType riskType = riskTypeTool.getRiskType(riskProcessParam.accountId, submitScene, riskProcessParam);
      log.info("submitRisk getRiskType success, accountId is {},submitScene is {},riskType is {}", riskProcessParam.accountId, submitScene, riskType);

      //3 提交风控
      return checkAndSubmitRiskIfNeed(riskProcessParam, submitScene, riskType, userGroupTriggerResult);
    });
  }

  /**
   * 根据riskType提交风控，仅RiskFacadeService内部以及跑批相关可以调用
   *
   * @param riskProcessParam
   * @param riskType
   */
  public BatchBlockReasonResult submitRiskForSpecificRiskType(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, LoanUserRiskType riskType) {
    return threadTransactionalModel.transactionResult(configuration -> {
      //加锁
      accountModel.findByIdForUpdateOrThrow(riskProcessParam.accountId);
      //对于跑批，这里需要先判断一下首复续和riskType是否对应
      UserGroupTriggerResult mockUserGroupTriggerResult = riskUserGroupEntranceService.triggerBeforeSubmitRisk(riskProcessParam, submitScene, riskType, true);
      BatchBlockReasonResult batchTriggerMatchResult = batchRiskTriggerReasonTool.prohibitedExecuteForBatchTrigger(riskProcessParam, submitScene, riskType, mockUserGroupTriggerResult);
      if (!batchTriggerMatchResult.isAllowSubmit()) {
        // 首续/复续与 riskType 不匹配：透出 FIRST_MULTI_USER / RELOAN_MULTI_USER
        return batchTriggerMatchResult;
      }

      //1 更新人群
      UserGroupTriggerResult realUserGroupTriggerResult = riskUserGroupEntranceService.triggerBeforeSubmitRisk(riskProcessParam, submitScene, riskType, false);
      //1.1 添加陪跑
      compareUserGroupTriggerResult(realUserGroupTriggerResult, mockUserGroupTriggerResult);
      //2 提交风控
      boolean submitted = checkAndSubmitRiskIfNeed(riskProcessParam, submitScene, riskType, realUserGroupTriggerResult);
      // 匹配校验通过后仍未提交（含前筛未过等）：本期统一归因为前筛拒绝（ext 为空），后续再细分
      return submitted ? BatchBlockReasonResult.allow() : BatchBlockReasonResult.block(BatchTriggerBlockReason.HIT_PRE_FILTER_RULE);
    });
  }

  private void compareUserGroupTriggerResult(UserGroupTriggerResult realUserGroupTriggerResult, UserGroupTriggerResult mockUserGroupTriggerResult) {
    boolean result = Objects.equals(getComparedUserGroup(realUserGroupTriggerResult), getComparedUserGroup(mockUserGroupTriggerResult));
    if (!result) {
      log.error("compareUserGroupTriggerResult result:{}, realUserGroupTriggerResult is {}, mockUserGroupTriggerResult is {}",
          result
          , realUserGroupTriggerResult,
          mockUserGroupTriggerResult);
    } else {
      log.info("compareUserGroupTriggerResult result:{}, realUserGroupTriggerResult is {}, mockUserGroupTriggerResult is {}",
          result
          , realUserGroupTriggerResult,
          mockUserGroupTriggerResult);
    }

  }

  private LoanRiskUserGroupEnum getComparedUserGroup(UserGroupTriggerResult userGroupTriggerResult) {
    return Objects.nonNull(userGroupTriggerResult.updatedUserGroup) ? userGroupTriggerResult.updatedUserGroup : userGroupTriggerResult.oldUserGroup;
  }

  /**
   * 处理风控结果
   */
  public void handleRiskResult(Long traceId, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO, Runnable runnable) {
    //加redis锁
    executeAutoReviewResultLock.lockAndRun(traceId, () -> {
      //开启事务
      threadTransactionalModel.transaction(configuration -> {
        //判断是否需要处理
        if (!needHandleTrace(traceId)) {
          return;
        }
        //1 处理风控结果
        runnable.run();
        LoanUserRiskTraceVO currentRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceId);
        //2 更新人群
        UserGroupTriggerResult userGroupTriggerResult = riskUserGroupEntranceService.triggerAfterRisk(currentRiskTraceVO, data);
        //3 全景视图打点（升降级后、提交降级/循环风控前）
        panoramaRiskDwLogService.logRiskCompleteEvent(currentRiskTraceVO, userGroupTriggerResult);
        //4 若有需要，提交降级风控
        TriggerRiskAfterCurrentRiskVO triggerRiskAfterCurrentRiskVO = triggerRiskAfterCurrentRisk(currentRiskTraceVO, data, userGroupTriggerResult);
        //5 创建订单
        CreateOrderAfterRiskResponseVO createOrderResult = riskResultPostEventService.createOrderAfterRisk(currentRiskTraceVO, data);
        //6 处理下需要降级风控结果才能决定的后置操作
        riskResultPostEventService.handlePostEventAfterDegradeJudge(currentRiskTraceVO, data, createOrderResult, oldCreditsInfoVO, triggerRiskAfterCurrentRiskVO);
      });
    });

  }

  private TriggerRiskAfterCurrentRiskVO triggerRiskAfterCurrentRisk(LoanUserRiskTraceVO currentRiskTraceVO, LoanUserTagData data, UserGroupTriggerResult userGroupTriggerResult) {
    boolean submitDegradeRisk = submitDegradeRiskIfNeed(currentRiskTraceVO, userGroupTriggerResult.nextRiskSubmitScene);
    if (submitDegradeRisk) {
      return TriggerRiskAfterCurrentRiskVO.from(true, false);
    }
    //没有升降级
    boolean canTriggerRevolvingFirst = revolvingUserGroupAbtestService.triggerRevolvingAbtestForNormalUser(currentRiskTraceVO.userId, currentRiskTraceVO.accountId, currentRiskTraceVO.traceId);
    if (canTriggerRevolvingFirst) {
      boolean submitSuccess = submitRevolvingFirstRiskIfNeed(currentRiskTraceVO, LoanUserRiskSubmitScene.REVOLVING_CALC_CREDITS);
      //打点：用户回到常规，且触发循环一次风控
      revolvingUserGroupAbtestService.monitorRevolvingV2(RevolvingV2MonitorVO.fromWithTriggerSource(
          currentRiskTraceVO.userId,
          currentRiskTraceVO.accountId,
          RevolvingTriggerSource.USER_RETURN_NORMAL,
          submitSuccess
      ));
      return TriggerRiskAfterCurrentRiskVO.from(false, true);
    }
    return null;
  }

  public boolean needHandleTrace(Long traceId) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceId);
    //如果已经是完成的状态了，说明接口已经同步过了，Job直接返回，避免job和接口同时执行产生的并发问题
    if (RiskFlowTraceStatusV2.FINISH == loanUserRiskTraceVO.status && loanUserRiskTraceVO.creditsStatus != LoanCreditsStatus.MANUAL_REVIEW) {
      log.info("Risk control callback result processing has been skipped because it has already been finished, with loanAccountId = {}", loanUserRiskTraceVO.accountId);
      return false;
    }

    boolean skipRiskCallback = riskConfig.fetchSkipRiskCallbackSwitch();
    if (skipRiskCallback
        && SysEnvironment.isTest()
        && riskConfig.fetchSkipRiskCallbackAccountIdList().contains(loanUserRiskTraceVO.accountId)) {
      log.info("Risk control callback result processing has been skipped because skipRiskCallback is true, with loanAccountId = {}", loanUserRiskTraceVO.accountId);
      return false;
    }

    return true;
  }

  /**
   * 进行检查，如果符合条件，提交风控
   */
  private boolean checkAndSubmitRiskIfNeed(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene,
                                           LoanUserRiskType riskType, UserGroupTriggerResult userGroupTriggerResult) {
    //风控前筛
    boolean checkResult = checkBeforeSubmitRisk(riskProcessParam, submitScene, riskType);
    log.info("checkAndSubmitRiskIfNeed success, accountId is {},riskType is {},checkResult is {}", riskProcessParam.accountId, riskType, checkResult);
    if (!checkResult) {
      return false;
    }
    if (riskType == LoanUserRiskType.REVOLVING_LOAN_FIRST
        && !riskTypeTool.checkReloanUserCanDirectEnterRevolvingProcess(riskProcessParam.accountId)
        && !riskProcessParam.isNotBatchRisk) {
      log.info("riskType is revolvingLoanFirst, accountId is {},riskType is {},can direct enter revolving process", riskProcessParam.accountId, riskType);
      return false;
    }
    if (riskType == LoanUserRiskType.REVOLVING_LOAN_FIRST
        && riskTypeTool.checkReloanUserCanDirectEnterRevolvingProcess(riskProcessParam.accountId)) {
      log.info("riskType is revolvingLoanFirst, accountId is {},riskType is {},can direct enter revolving process", riskProcessParam.accountId, riskType);
      loanAccountRevolvingService.updateRevolvingLoanStatus(riskProcessParam.accountId, null, RevolvingChangeSource.NORMAL_USER_DERECT_ENTER_REVOLVING, RevolvingStatus.VALID);
    }
    LoanUserCreditsInfoVO preSubmitCreditsInfo = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(riskProcessParam.accountId);
    //提交风控
    riskApplicationSubmitService.submitRiskApplication(riskProcessParam, riskType);
    //全景视图打点
    panoramaRiskDwLogService.logRiskSubmitEvent(riskProcessParam, riskType, userGroupTriggerResult, preSubmitCreditsInfo);
    return true;
  }


  /**
   * 如果需要降级风控，则完成前置处理并提交
   *
   * @param currentRiskTraceVO
   * @param nextScene
   * @return 是否成功提交降级风控
   */
  private boolean submitDegradeRiskIfNeed(LoanUserRiskTraceVO currentRiskTraceVO,
                                          LoanUserRiskSubmitScene nextScene) {
    log.info("submitDegradeRiskIfNeed start, accountId is {}, traceId is {},nextScene is {}", currentRiskTraceVO.accountId, currentRiskTraceVO.traceId, nextScene);
    if (nextScene == null) {
      return false;
    }

    //前置链式触发：因当前 trace 被管制进回捞的走 PRE_RISK_CONTROL，其余被拒降级走 PRE_RISK_REJECT
    LoanUserRiskTriggerSource triggerSource = loanAccountRevolvingService.checkNeedTriggerRetrieval(currentRiskTraceVO.traceId, currentRiskTraceVO.accountId)
        ? LoanUserRiskTriggerSource.PRE_RISK_CONTROL
        : LoanUserRiskTriggerSource.PRE_RISK_REJECT;
    RiskProcessParam param = riskTypeTool.createRiskProcessParamByLastTrace(
        currentRiskTraceVO.accountId,
        currentRiskTraceVO.traceId,
        PreTraceTriggerScene.RETRIEVAL,
        nextScene,
        triggerSource);

    submitRisk(param, nextScene);
    log.info("submitDegradeRiskIfNeed success, accountId is {},traceId is {},nextScene is {}", currentRiskTraceVO.accountId, currentRiskTraceVO.traceId, nextScene.name());
    return true;
  }

  /**
   * 如果需要循环一次风控，触发
   *
   */
  private boolean submitRevolvingFirstRiskIfNeed(LoanUserRiskTraceVO currentRiskTraceVO,
                                                 LoanUserRiskSubmitScene nextScene) {
    log.info("submitRevolvingFirstRiskIfNeed start, accountId is {}, traceId is {},nextScene is {}", currentRiskTraceVO.accountId, currentRiskTraceVO.traceId, nextScene);
    if (nextScene == null) {
      return false;
    }

    //TODO
    // 由于目前循环只开了新客复贷，这里生产环境不会使用到
    // 不接入触发来源标记，triggerSource 置空
    RiskProcessParam param = riskTypeTool.createRiskProcessParamByLastTrace(
        currentRiskTraceVO.accountId,
        currentRiskTraceVO.traceId,
        PreTraceTriggerScene.RETRIEVAL,
        nextScene,
        null);

    submitRisk(param, nextScene);
    log.info("submitRevolvingFirstRiskIfNeed success, accountId is {},traceId is {},nextScene is {}", currentRiskTraceVO.accountId, currentRiskTraceVO.traceId, nextScene.name());
    return true;
  }

  /**
   * 提交风控前筛
   * 续借、结清存在前筛流程，若前筛不过，不能提交风控
   *
   */
  public boolean checkBeforeSubmitRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, LoanUserRiskType riskType) {
    //续借测额类型需要进行前筛检查
    if (LoanUserRiskType.getMultiLoanCalcRiskType().contains(riskType)) {
      return multiLoanStatusService.checkAndGetResultBeforeSubmitMultiLoanRisk(riskProcessParam, submitScene, riskType);
    }
    //结清之后用户也可能触发结清的前筛,校验submitScene 和 riskType, 循环风控不走前筛
    if (submitScene == LoanUserRiskSubmitScene.AUTO_CALC_CREDITS_AFTER_REPAY_ALL && riskType == LoanUserRiskType.CALC_CREDITS) {
      return riskFacadeTool.checkAndGetResultForAfterRepayallBeforeCalcRisk(riskProcessParam, submitScene, riskType);
    }
    return true;
  }

  /**
   * admin拒绝复贷风控
   */
  public void batchRejectReloanCreditsByAdmin(Long loanAccountId, LoanCreditsRejectedReason rejectedReason, CreditsStatusChangeSource source, String reason, Long traceId) {
    //这里历史代码，traceId传的是null，不知道为什么，先保持，后面再优化
    Runnable runnable = () -> cashLoanCreditsService.rejectReloanCreditsApplication(loanAccountId, rejectedReason, source, reason, null);

    LoanUserCreditsInfoVO oldCreditsInfo = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(loanAccountId);
    handleRiskResult(traceId, LoanUserTagData.fromEmpty(), oldCreditsInfo, runnable);
  }

  /**
   * admin拒绝首贷风控
   */
  public void batchRejectLoanCreditsByAdmin(LoanUserRiskTraceVO riskTraceVO, String rejectReason) {
    Runnable runnable = () -> cashLoanCreditsService.rejectLoanCreditsByAdmin(riskTraceVO, rejectReason);
    LoanUserCreditsInfoVO oldCreditsInfo = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(riskTraceVO.accountId);
    handleRiskResult(riskTraceVO.traceId, LoanUserTagData.fromEmpty(), oldCreditsInfo, runnable);
  }

  /**
   * admin通过风控
   */
  public void acceptCreditsByAdmin(Long accountId,
                                   LoanUserTypeVO loanUserTypeVO,
                                   BigDecimal newCredits,
                                   LoanUserLevel operationLevel,
                                   String reason,
                                   Boolean rejectMultiApply,
                                   Boolean rejectMultiOrder,
                                   CreditsStatusChangeSource source,
                                   ProductTypeCodeVO productTypeCodeVO,
                                   String operator,
                                   TagOperatorTypeEnum operatorTypeEnum) {
    LoanUserTagData data = cashLoanCreditsService.checkAndGenerateLoanUserTagDataForAdminAccept(accountId,
        loanUserTypeVO,
        newCredits,
        operationLevel,
        rejectMultiApply,
        rejectMultiOrder,
        productTypeCodeVO);
    Runnable runnable = () -> cashLoanCreditsService.acceptCreditsApplicationWithUserType(
        accountId,
        reason,
        source,
        operator,
        operatorTypeEnum,
        data);
    LoanUserRiskTraceVO traceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(accountId);
    LoanUserCreditsInfoVO oldCreditsInfo = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(traceVO.accountId);
    handleRiskResult(traceVO.traceId, data, oldCreditsInfo, runnable);
  }

  private RiskFlowTraceVOV2 getRiskFlowTraceVOV2(Long traceId) {
    if (riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW_TRACE)) {
      List<com.yqg.overseasrisk.common.lib.riskflow.trace.RiskFlowTraceVO> riskFlowTraceResps = iRiskFlowTraceService.findByIds(Collections.singletonList(traceId));
      if (CollectionUtils.isEmpty(riskFlowTraceResps) || riskFlowTraceResps.size() > 1) {
        throw EcException.error("current trace have more than one record, need to check, traceid is {}", traceId);
      }
      return RiskFlowTraceVOV2.fromOverseasRiskFlowTraceVO(riskFlowTraceResps.get(0));
    } else {
      RiskFlowTraceVO riskFlowTraceVO = riskFlowService.queryTrace(traceId);
      return RiskFlowTraceVOV2.fromRiskFlowTraceVO(riskFlowTraceVO);
    }
  }


  public void executeAutoReviewResultForCallBack(Long traceId) {
    RiskFlowTraceVOV2 riskFlowTraceVOV2 = getRiskFlowTraceVOV2(traceId);
    try {
      EventTypeVO eventTypeVO = loanUserEventService.fromEventId(riskFlowTraceVOV2.eventId);
      executeAutoReviewResult(eventTypeVO, riskFlowTraceVOV2);
    } catch (Exception ex) {
      log.error("executeAutoReviewResultForCallBack error,traceId is {}", riskFlowTraceVOV2.id, ex);
    }
  }

  public void executeAutoReviewResult(EventTypeVO eventTypeVO, RiskFlowTraceVOV2 riskFlowTraceVOV2) {
    handleRiskResultExecutor.submit(() -> {
      try {
        handleReviewResultWithLock(eventTypeVO, riskFlowTraceVOV2);
      } catch (Exception ex) {
        log.error("executeAutoReviewResult error,traceId is {}", riskFlowTraceVOV2.id, ex);
        riskMonitor.logExecuteAutoReviewResultError(eventTypeVO, riskFlowTraceVOV2.id);
      }
    });
  }

  public void handleReviewResultWithLock(EventTypeVO eventTypeVO, RiskFlowTraceVOV2 riskFlowTraceVOV2) {
    AutoReviewResult autoReviewResult = cashLoanCreditsService.getResult(riskFlowTraceVOV2);
    handleRiskResult(riskFlowTraceVOV2.id, autoReviewResult.data, autoReviewResult.creditsInfoVO, () -> cashLoanCreditsService.checkAndDoHandleReviewResult(eventTypeVO, riskFlowTraceVOV2));
  }

  /**
   * 订单打款成功后，处理风控相关事件
   */
  public void onOrderPayoutSuccess(CashLoanOrderVO cashLoanOrderVO) {
    threadTransactionalModel.transaction(configuration -> {
      //加锁
      accountModel.findByIdForUpdateOrThrow(cashLoanOrderVO.accountId);
      //人群升降级
      riskUserGroupEntranceService.onOrderPayoutSuccess(cashLoanOrderVO);
    });
  }

  /**
   * 查询用户历史所有trace，判断是否全都是api渠道的
   */
  public boolean isUserHistoryTraceAllFromApiChannel(Long accountId) {
    Set<SourceType> sourceTypeSet = loanUserRiskTraceService.getSourceTypeSetByAccountId(accountId);
    if (CollectionUtils.isEmpty(sourceTypeSet)) {
      return false;
    }
    for (SourceType sourceType : sourceTypeSet) {
      if (!sourceType.isApiChannelSourceType()) {
        return false;
      }
    }
    return true;
  }
}
