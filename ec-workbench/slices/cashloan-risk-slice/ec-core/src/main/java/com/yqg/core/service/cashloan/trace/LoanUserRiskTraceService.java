package com.yqg.core.service.cashloan.trace;

import com.google.common.collect.ImmutableList;
import com.yqg.core.model.generated.tables.records.BatchTriggerRiskLogRecord;
import com.yqg.core.model.generated.tables.records.LoanAccountAdditionalInfoRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.generated.tables.records.RiskOutputResultRecord;
import com.yqg.core.model.mongo.MongoSubmitRiskParamModel;
import com.yqg.core.model.sql.loan.account.LoanAccountAdditionalInfoModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loanusertrace.LoanRiskCreditStage;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.model.sql.risk.BatchTriggerRiskLogModel;
import com.yqg.core.model.sql.risk.RiskOutputResultModel;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.model.sql.risk.enums.SubmitCreditsAdditionalInfoType;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.risk.RiskAcceptAbTestConfig;
import com.yqg.core.service.cashloan.risk.monitor.RiskMonitor;
import com.yqg.core.service.cashloan.util.ReloanFeeWillUtil;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.risk.batchtrigger.enums.BatchTriggerLogStatus;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.risk.submitadditional.vo.SubmitCreditsAdditionalInfoVO;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.LoanAccountAdditionalTypeEnum;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskSource;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jiewu on 2021/3/19
 */
@Service
@Slf4j
public class LoanUserRiskTraceService {
  @Autowired
  private LoanUserRiskTraceModel riskTraceModel;
  @Autowired
  private MongoSubmitRiskParamModel mongoSubmitRiskParamModel;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private RiskMonitor riskMonitor;
  @Autowired
  private BatchTriggerRiskLogModel batchTriggerRiskLogModel;
  @Autowired
  private RiskOutputResultModel riskOutputResultModel;
  @Autowired
  private RiskAcceptAbTestConfig riskAcceptAbTestConfig;
  @Autowired
  private LoanAccountAdditionalInfoModel loanAccountAdditionalInfoModel;
  @Autowired
  private SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private LoanUserRiskTraceDisplayConfig loanUserRiskTraceDisplayConfig;

  public LoanUserRiskTraceVO findLastedRelatedCalcTraceVO(Long accountId, Long currentOrderCreatedTime, List<LoanUserRiskType> calcCreditRiskTypes) {
    //1、选取上一条的测算记录
    LoanUserRiskTraceRecord latestRiskTrace = riskTraceModel.findLatestByTimestampAndAccountId(accountId, calcCreditRiskTypes, currentOrderCreatedTime);
    if (latestRiskTrace == null) {
      return null;
    }

    // 循环额度用户可重复下单，除非月度测额，否则不需要再次测额，所以取上一次额度测算记录，多笔订单的traceId可能会出现重复
    if (loanAccountRevolvingService.checkUserInRevolvingLoanProcess(accountId)) {
      return LoanUserRiskTraceVO.fromOrNull(latestRiskTrace);
    }
    // 2、拿到上一条 complete or ready 订单，如果订单创建时间大于最新额度测算时间，则该订单没有对应的额度测算记录
    CashLoanOrderVO latestPayoutStatusOrder = ecOrderService.getLatestOrder(accountId, currentOrderCreatedTime - 1, CashLoanOrderStatus.COMPLETE, CashLoanOrderStatus.READY);
    if (latestPayoutStatusOrder != null && latestPayoutStatusOrder.timeCreated > latestRiskTrace.getTimeCreated()) {
      log.info("订单创建时间大于最新额度测算时间，orderId:{}, riskTraceId:{}", latestPayoutStatusOrder.id, latestRiskTrace.getId());
      return null;
    }
    return LoanUserRiskTraceVO.fromOrNull(latestRiskTrace);
  }

  /**
   * 按账户 + riskType + 时间上限取 trace 表中最新一条测算记录。
   * 仅按 {@code TIME_CREATED < endTime} 过滤，不判断循环额度、不比较订单时间。
   */
  public LoanUserRiskTraceVO findLatestCalcTraceVO(Long accountId, Long endTime, List<LoanUserRiskType> calcCreditRiskTypes) {
    LoanUserRiskTraceRecord latestRiskTrace =
        riskTraceModel.findLatestByTimestampAndAccountId(accountId, calcCreditRiskTypes, endTime);
    return LoanUserRiskTraceVO.fromOrNull(latestRiskTrace);
  }

  public Map<Long, LoanUserRiskType> fetchMapByTraceIds(List<Long> traceIds) {
    return riskTraceModel.fetchMapByTraceIds(traceIds)
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> LoanUserRiskType.fromCode(e.getValue().getRiskType())));
  }

  public LoanUserRiskTraceVO findByTraceIdOrThrow(Long traceId) {
    LoanUserRiskTraceRecord traceRecord = riskTraceModel.findByTraceId(traceId);
    if (traceRecord == null) {
      throw EcException.error("can't find by traceId : {}", traceId);
    }
    return LoanUserRiskTraceVO.fromOrNull(traceRecord);
  }

  public LoanUserRiskTraceVO findByTraceIdOrNull(Long traceId) {
    if (Objects.isNull(traceId)) {
      return null;
    }
    LoanUserRiskTraceRecord traceRecord = riskTraceModel.findByTraceId(traceId);
    return LoanUserRiskTraceVO.fromOrNull(traceRecord);
  }

  /**
   * 按 orderId 取关联 Trace（库侧 limit 1）；不存在时返回 null。
   */
  public LoanUserRiskTraceVO findByOrderIdOrNull(Long orderId) {
    if (Objects.isNull(orderId)) {
      return null;
    }
    return LoanUserRiskTraceVO.fromOrNull(riskTraceModel.findByOrderId(orderId));
  }

  public LoanUserRiskTraceVO findLatestCreditRiskByAccountId(Long accountId) {
    return findLatestCreditRiskByAccountIdAndRiskTypes(accountId, LoanUserRiskType.getAllUserCreditsRiskTypes());
  }

  public LoanUserRiskTraceVO findLatestCalcCreditRiskByAccountId(Long accountId) {
    return findLatestCreditRiskByAccountIdAndRiskTypes(accountId, LoanUserRiskType.getAllCalcCreditsType());
  }

  public LoanUserRiskTraceVO findLatestCreditRiskByAccountIdAndRiskTypes(Long accountId, List<LoanUserRiskType> riskTypes) {
    LoanUserRiskTraceRecord traceRecord = riskTraceModel.findLastedCreditsRiskTraceByAccountId(accountId, riskTypes);
    return LoanUserRiskTraceVO.fromOrNull(traceRecord);
  }

  public void updateAfterRiskFlowTraceFinished(RiskFlowTraceVOV2 traceVO) {
    LoanUserRiskTraceRecord record = riskTraceModel.findByTraceId(traceVO.getId());
    if (record == null) {
      log.error("cannot find LoanUserRiskTraceRecord when riskFlowTrace finished, riskFlowTraceId is :{}", traceVO.getId());
      return;
    }
    if (RiskFlowTraceStatusV2.fromCode(record.getStatus()) == RiskFlowTraceStatusV2.FINISH) {
      log.info("LoanUserRiskTraceRecord has finished when riskFlowTrace finished, riskFlowTraceId is :{}", traceVO.getId());
      return;
    }
    riskTraceModel.updateTrace(record, traceVO.eventId, traceVO.riskFlowId, traceVO.status);
    //Run batch trace status callbacks
    if (traceVO.status == RiskFlowTraceStatusV2.FINISH) {
      batchTriggerRiskLogModel.updateStatus(traceVO.getId(), BatchTriggerLogStatus.FINISH);
    }

    //移除掉跑批的trace
    LoanUserRiskSource riskSource = (batchTriggerRiskLogModel.isExistByTraceId(traceVO.id)) ? LoanUserRiskSource.BATCH : LoanUserRiskSource.NORMAL;
    SourceType sourceType = Objects.nonNull(record.getSourceType()) ? SourceType.valueOf(record.getSourceType()) : null;
    RiskFlowTraceStatusV2 status = RiskFlowTraceStatusV2.fromCode(traceVO.status.code);
    LoanUserRiskType riskType = LoanUserRiskType.fromCode(record.getRiskType());
    LoanUserRiskTriggerSource triggerSource = record.getTriggerSource() == null ? null : LoanUserRiskTriggerSource.valueOf(record.getTriggerSource());
    LoanRiskCreditStage creditStage = record.getCreditStage() == null ? null : LoanRiskCreditStage.valueOf(record.getCreditStage());
    riskMonitor.logSubmitRiskAndComplete(
        record.getLoanAccountId(),
        sourceType,
        status,
        riskType,
        riskSource,
        record.getTriggerType(),
        triggerSource,
        creditStage,
        traceVO.id,
        record.getTimeCreated());
  }

  public LoanUserRiskTraceVO findByIdOrThrow(Long ecRiskId) {
    LoanUserRiskTraceRecord riskTraceRecord = riskTraceModel.findById(ecRiskId);
    if (riskTraceRecord == null) {
      throw EcException.error("can not find trace record by id:{}", ecRiskId);
    }
    return LoanUserRiskTraceVO.fromOrNull(riskTraceRecord);
  }

  public List<LoanUserRiskTraceVO> findByUserIdAndRiskTypeAndTimeCreated(Long userId, List<LoanUserRiskType> riskTypeList, Long startTime, Long endTime) {
    List<LoanUserRiskTraceRecord> records = riskTraceModel.findByUserIdAndRiskTypeAndTimeCreated(userId, riskTypeList, startTime, endTime);
    return records.stream().map(LoanUserRiskTraceVO::fromOrNull).collect(Collectors.toList());
  }

  public List<LoanUserRiskTraceVO> findByUserIdAndRiskTypeAndTimeCreatedAllowTraceIdIsNull(Long userId, List<LoanUserRiskType> riskTypeList, Long startTime, Long endTime) {
    List<LoanUserRiskTraceRecord> records = riskTraceModel.findByUserIdAndRiskTypeAndTimeCreatedAllowedTraceIdIsNull(userId, riskTypeList, startTime, endTime);
    if (CollectionUtils.isEmpty(records)) {
      return new ArrayList<>();
    }
    return records
        .stream()
        .map(LoanUserRiskTraceVO::fromOrNull)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  //TODO （chaoye）目前这种写法不好，耦合太深，未来改为提供鉴权风控通过的trace和通过时间
  @Deprecated
  public List<LoanUserRiskTraceVO> findFirstLoanAcceptedByUserIdAndRiskTypeAndTimeUpdate(Long userId, Long startTime, Long endTime) {
    return riskTraceModel.findAcceptedByUserIdAndRiskTypeAndTimeUpdate(userId, LoanUserRiskType.getFirstLoanAcceptRiskTypes(), startTime, endTime)
        .stream()
        .map(LoanUserRiskTraceVO::fromOrNull)
        .filter(this::isFirstLoanAcceptedTrace)
        .collect(Collectors.toList());
  }

  @Deprecated
  public RiskProcessParam getRiskProcessParamByTraceIdOrThrow(Long traceId) {
    LoanUserRiskTraceRecord riskTraceRecord = riskTraceModel.findByTraceId(traceId);
    if (Objects.isNull(riskTraceRecord)) {
      throw EcException.error("can't get trace record, traceId:{}", traceId);
    }
    return getRiskProcessParam(riskTraceRecord);
  }

  @Deprecated
  public RiskProcessParam getRiskProcessParam(LoanUserRiskTraceRecord riskTraceRecord) {
    List<LoanAccountAdditionalInfoRecord> additionalInfoRecordList =
        loanAccountAdditionalInfoModel.findByAccountIdsAndTypeAndExternalIds(ImmutableList.of(riskTraceRecord.getLoanAccountId()),
            LoanAccountAdditionalTypeEnum.SUBMIT_RISK_PARAM,
            ImmutableList.of(riskTraceRecord.getId().toString()));

    if (CollectionUtils.isEmpty(additionalInfoRecordList) || additionalInfoRecordList.size() != 1) {
      throw EcException.error("can't get submitRisk for user, do not find addition info. loanAccountId is  {}", riskTraceRecord.getLoanAccountId());
    }
    RiskProcessParam riskProcessParam = mongoSubmitRiskParamModel.findByObjectIdOrNull(additionalInfoRecordList.get(0).getValue());
    if (Objects.isNull(riskProcessParam)) {
      throw EcException.error("can't get submitRisk for user, do not find param info. loanAccountId is  {}", riskTraceRecord.getLoanAccountId());
    }
    return riskProcessParam;
  }

  /**
   * 找到用户完件风控通过的trace，可能是以下两种情况
   * - LOAN通过
   * - LOAN拒绝，回捞通过
   */
  public LoanUserRiskTraceVO findAuthLoanAcceptedTraceVO(Long userId) {

    //先找LoanUserRiskType.LOAN类型的数据
    LoanUserRiskTraceRecord authRiskRecord = riskTraceModel.findLastedByUserIdAndRiskType(userId, LoanUserRiskType.LOAN);
    if (authRiskRecord == null || null == authRiskRecord.getCreditsStatus()) {
      return null;
    }
    LoanCreditsStatus authRiskStatus = LoanCreditsStatus.fromCode(authRiskRecord.getCreditsStatus());
    if (authRiskStatus != LoanCreditsStatus.ACCEPTED && authRiskStatus != LoanCreditsStatus.REJECTED) {
      return null;
    }
    //状态为通过
    if (authRiskStatus == LoanCreditsStatus.ACCEPTED) {
      return LoanUserRiskTraceVO.fromOrNull(authRiskRecord);
    }
    //拒绝状态，看下回捞
    SubmitCreditsAdditionalInfoVO submitCreditsAdditionalInfoVO = submitCreditsAdditionalInfoService.fetchByPreRiskIdAndType(authRiskRecord.getId(), SubmitCreditsAdditionalInfoType.PRE_TRIGGER_RETRIEVAL);
    if (submitCreditsAdditionalInfoVO == null) {
      return null;
    }
    Long ecRiskId = submitCreditsAdditionalInfoVO.getRecord().getLoanUserRiskId();
    LoanUserRiskTraceVO riskTraceVO = findByIdOrThrow(ecRiskId);
    return riskTraceVO.creditsStatus == LoanCreditsStatus.ACCEPTED ? riskTraceVO : null;
  }

  /**
   * 给外部的功能出口，别删除
   * 检查当前用户是否是首贷一次风控通过的用户
   * 包括 1. Loan 风控通过  Loan 风控拒绝，回捞通过
   * 不包括：
   * 首贷用户额度失效重新测额的情况
   */
  public Boolean isFirstLoanAcceptedByUserId(Long userId) {
    Long accountId = loanAccountService.getAccountByUserIdOrThrow(userId, SDKType.IDN_YQD).getId();
    LoanUserRiskTraceRecord lastFinishRiskTrace = riskTraceModel.getLastFinishRiskTrace(accountId, LoanUserRiskType.getAllUserCreditsRiskTypes());
    return lastFinishRiskTrace != null && isFirstLoanAcceptedTrace(lastFinishRiskTrace.getTraceId());
  }

  /**
   * 给外部的功能出口，别删除
   * 检查当前用户是否是首贷一次风控通过的用户
   * 包括 1. Loan 风控通过 2. Loan 风控拒绝，回捞通过
   * 不包括：
   * 首贷用户额度失效重新测额的情况
   */
  public Boolean isFirstLoanAcceptedByAccountId(Long accountId) {
    LoanUserRiskTraceRecord lastFinishRiskTrace = riskTraceModel.getLastFinishRiskTrace(accountId, LoanUserRiskType.getAllUserCreditsRiskTypes());
    return lastFinishRiskTrace != null && isFirstLoanAcceptedTrace(lastFinishRiskTrace.getTraceId());
  }

  /**
   * 判断此trace是否为以下两种情况之一
   * - LOAN通过
   * - LOAN拒绝，回捞通过
   */
  public Boolean isFirstLoanAcceptedTrace(Long traceId) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = findByTraceIdOrThrow(traceId);
    return isFirstLoanAcceptedTrace(loanUserRiskTraceVO);
  }

  /**
   * 判断此trace是否为以下两种情况之一
   * - LOAN通过
   * - LOAN拒绝，回捞通过
   *
   * @param loanUserRiskTraceVO
   * @return
   */
  private Boolean isFirstLoanAcceptedTrace(LoanUserRiskTraceVO loanUserRiskTraceVO) {
    if (loanUserRiskTraceVO == null) {
      return false;
    }
    if (loanUserRiskTraceVO.creditsStatus != LoanCreditsStatus.ACCEPTED) {
      return false;
    }
    if (loanUserRiskTraceVO.riskType == LoanUserRiskType.LOAN) {
      return true;
    }
    if (loanUserRiskTraceVO.riskType == LoanUserRiskType.LOAN_RETRIEVAL) {
      SubmitCreditsAdditionalInfoVO additionalInfoVO = submitCreditsAdditionalInfoService.fetchByTraceIdAndType(loanUserRiskTraceVO.traceId, SubmitCreditsAdditionalInfoType.PRE_TRIGGER_RETRIEVAL);
      return additionalInfoVO != null && additionalInfoVO.getInfoPojo() != null && additionalInfoVO.getInfoPojo().preTriggerRetrievalInfo != null &&
          additionalInfoVO.getInfoPojo().preTriggerRetrievalInfo.lastedRiskType == LoanUserRiskType.LOAN;
    }
    return false;
  }

  /**
   * 此条风控是否为回捞降级风控
   *
   * @param ecRiskId loan_user_risk_trace的主键id
   * @return
   */
  public Boolean isRetrievalTraceByEcRiskId(Long ecRiskId) {
    SubmitCreditsAdditionalInfoVO submitCreditsAdditionalInfoVO = submitCreditsAdditionalInfoService.fetchByLoanUserRiskIdAndType(ecRiskId, SubmitCreditsAdditionalInfoType.PRE_TRIGGER_RETRIEVAL);
    return submitCreditsAdditionalInfoVO != null;
  }

  // 如果当前trace为降级风控，返回前置风控的vo
  // 如果当前trace不为降级风控，返回当前trace的vo
  public LoanUserRiskTraceVO getOriginRiskTraceVOOrThrow(Long traceId) {
    LoanUserRiskTraceVO currentTraceVO = findByTraceIdOrThrow(traceId);
    SubmitCreditsAdditionalInfoVO submitCreditsAdditionalInfoVO = submitCreditsAdditionalInfoService.fetchByLoanUserRiskIdAndType(currentTraceVO.id, SubmitCreditsAdditionalInfoType.PRE_TRIGGER_RETRIEVAL);

    //非降级风控
    if (submitCreditsAdditionalInfoVO == null) {
      return currentTraceVO;
    }

    //降级风控
    return findByIdOrThrow(submitCreditsAdditionalInfoVO.getRecord().getPreLoanUserRiskId());

  }


  /**
   * 此条风控是否触发了后续回捞流程
   *
   * @param preRiskId loan_user_risk_trace的主键id
   * @return
   */
  public Boolean isTriggerRetrievalAfterPreTrace(Long preRiskId) {
    SubmitCreditsAdditionalInfoVO submitCreditsAdditionalInfoVO = submitCreditsAdditionalInfoService.fetchByPreRiskIdAndType(preRiskId, SubmitCreditsAdditionalInfoType.PRE_TRIGGER_RETRIEVAL);
    return submitCreditsAdditionalInfoVO != null;
  }

  public LoanUserRiskTraceVO getLastFirstLoanCreditsRiskTrace(Long loanAccountId) {
    LoanUserRiskTraceRecord loanUserRiskTraceRecord = riskTraceModel.findLastAcceptRiskTraceByAccountIdAndRiskTypes(loanAccountId, LoanUserRiskType.getFirstLoanRiskTypes());
    return LoanUserRiskTraceVO.fromOrNull(loanUserRiskTraceRecord);
  }

  public LoanUserRiskTraceVO getLastFinishCreditsRiskTrace(Long loanAccountId) {
    LoanUserRiskTraceRecord loanUserRiskTraceRecord = riskTraceModel.getLastFinishRiskTrace(loanAccountId, LoanUserRiskType.getAllUserCreditsRiskTypes());
    return LoanUserRiskTraceVO.fromOrNull(loanUserRiskTraceRecord);
  }

  public Set<SourceType> getSourceTypeSetByAccountId(Long accountId) {
    List<String> sourceTypeList = riskTraceModel.listSourceTypeByAccountId(accountId);
    if (CollectionUtils.isEmpty(sourceTypeList)) {
      return Collections.emptySet();
    }
    return sourceTypeList.stream()
        .map(SourceType::getSourceType)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  public List<LoanUserRiskTraceVO> getRiskTraceByLoanAccountId(Long loanAccountId, Integer limit, Integer offset) {
    List<LoanUserRiskTraceRecord> loanUserRiskTraceList = riskTraceModel.listTraceByAccountId(loanAccountId, limit, offset);
    return loanUserRiskTraceList.stream().map(LoanUserRiskTraceVO::fromOrNull).collect(Collectors.toList());
  }

  public Long countRiskTraceByLoanAccountId(Long loanAccountId) {
    return countRiskTraceByLoanAccountId(loanAccountId, false);
  }

  /**
   * @param isLy true 时按配置隐藏 riskType 同步过滤 count（与分页一致）
   */
  public Long countRiskTraceByLoanAccountId(Long loanAccountId, Boolean isLy) {
    return riskTraceModel.countTraceByAccountId(loanAccountId, resolveExcludedRiskTypesForOjk(isLy));
  }

  public List<Long> findLatestTraceIdsByAccountIdsAndType(List<Long> accountIds, LoanUserRiskType userRiskType) {
    return riskTraceModel.findLatestTraceIdsByAccountIdsAndType(accountIds, userRiskType);
  }

  /**
   * 首贷首次风控通过
   */
  public LoanUserRiskTraceVO getFirstLoanAcceptFirstCreditsRiskTraceByAccountId(Long loanAccountId) {
    LoanUserRiskTraceRecord loanUserRiskTraceRecord = riskTraceModel.findLastAcceptRiskTraceByAccountIdAndRiskTypes(loanAccountId, Collections.singletonList(LoanUserRiskType.LOAN));
    return LoanUserRiskTraceVO.fromOrNull(loanUserRiskTraceRecord);
  }

  public LoanUserRiskTraceVO findLastedCreditsRiskTraceByUserId(Long userId, List<LoanUserRiskType> riskTypes) {
    LoanUserRiskTraceRecord loanUserRiskTraceRecord = riskTraceModel.findLastedCreditsRiskTraceByUserId(userId, riskTypes);
    return LoanUserRiskTraceVO.fromOrNull(loanUserRiskTraceRecord);
  }

  public boolean needDoArrivalAmountAbTestAfterFirstRiskAccept(Long traceId) {
    Set<String> riskUserTypes = riskAcceptAbTestConfig.getRiskUserTypes();
    if (CollectionUtils.isEmpty(riskUserTypes)) {
      return false;
    }

    Long riskTraceCreatedTimeStart = riskAcceptAbTestConfig.getRiskTraceCreatedTimeStart();
    if (riskTraceCreatedTimeStart <= 0) {
      return false;
    }

    if (traceId == null) {
      return false;
    }
    LoanUserRiskTraceRecord loanUserRiskTraceRecord = riskTraceModel.findByTraceId(traceId);
    if (Objects.isNull(loanUserRiskTraceRecord)) {
      return false;
    }
    if (!Objects.equals(loanUserRiskTraceRecord.getRiskFlowId(), riskAcceptAbTestConfig.getRiskFlowId())) {
      return false;
    }
    if (!Objects.equals(loanUserRiskTraceRecord.getCreditsStatus(), LoanCreditsStatus.ACCEPTED.code) || loanUserRiskTraceRecord.getTimeCreated() < riskTraceCreatedTimeStart) {
      return false;
    }

    RiskOutputResultRecord userLevelOutputRes = riskOutputResultModel.findByTraceIdAndType(traceId, RiskOutputType.USER_LEVEL);
    if (Objects.nonNull(userLevelOutputRes) && Objects.equals(userLevelOutputRes.getNewValue(), RiskAcceptAbTestConfig.EXCLUDE_USER_LEVEL_TYPE)) {
      return false;
    }
    RiskOutputResultRecord userTypeOutputRes = riskOutputResultModel.findByTraceIdAndType(traceId, RiskOutputType.USER_TYPE);
    if (Objects.nonNull(userTypeOutputRes) && !riskUserTypes.contains(userTypeOutputRes.getNewValue())) {
      return false;
    }
    return true;
  }

  public LoanUserRiskTraceVO getLoanRiskTypeTrace(Long loanAccountId) {
    LoanUserRiskTraceRecord loanUserRiskTraceRecord = riskTraceModel.findFirstTraceByAccountIdAndRiskTypes(loanAccountId, Collections.singletonList(LoanUserRiskType.LOAN));
    return LoanUserRiskTraceVO.fromOrNull(loanUserRiskTraceRecord);
  }

  public List<Long> fetchByAccountIdAndEndTimeWithLimit(Long loanAccountId, Long endTime, Integer limit) {
    return riskTraceModel.fetchByAccountIdAndEndTimeWithLimit(loanAccountId, endTime, limit);
  }

  public LoanUserRiskTraceVO findRiskAcceptByAccountId(Long loanAccountId, LoanUserRiskType loanUserRiskType) {
    LoanUserRiskTraceRecord loanUserRiskTraceRecord = riskTraceModel.findLatestByTimestampAndAccountId(loanAccountId, Collections.singletonList(loanUserRiskType), Clock.now());
    return LoanUserRiskTraceVO.fromOrNull(loanUserRiskTraceRecord);
  }

  public boolean checkOrderCreatedWithNewRiskProcess(Long orderId) {
    LoanUserRiskTraceRecord loanUserRiskTraceRecord = riskTraceModel.findByOrderId(orderId);
    if (Objects.isNull(loanUserRiskTraceRecord)) {
      return false;
    }
    List<LoanAccountAdditionalInfoRecord> additionalInfoRecordList =
        loanAccountAdditionalInfoModel.findByAccountIdsAndTypeAndExternalIds(ImmutableList.of(loanUserRiskTraceRecord.getLoanAccountId()),
            LoanAccountAdditionalTypeEnum.SUBMIT_RISK_PARAM,
            ImmutableList.of(loanUserRiskTraceRecord.getId().toString()));
    return CollectionUtils.isNotEmpty(additionalInfoRecordList);
  }

  public List<Long> getFirstLoanAcceptedAccountIdList(List<Long> inviteeAccountIdList, Long startTime, Long endTime) {
    return riskTraceModel.findFinishedRiskByAccountIdsAndUpdatedTime(inviteeAccountIdList, LoanUserRiskType.getFirstLoanAcceptRiskTypes(), startTime, endTime)
        .stream()
        .map(LoanUserRiskTraceVO::fromOrNull)
        .filter(this::isFirstLoanAcceptedTrace)
        .map(riskTraceVO -> riskTraceVO.accountId)
        .collect(Collectors.toList());
  }

  public List<Long> findFirstLoanAcceptByAccountIds(List<Long> accountIdList) {
    return riskTraceModel.findFirstRiskAcceptByAccountIds(accountIdList, LoanUserRiskType.getFirstLoanAcceptRiskTypes())
        .stream()
        .map(LoanUserRiskTraceVO::fromOrNull)
        .filter(this::isFirstLoanAcceptedTrace)
        .map(riskTraceVO -> riskTraceVO.accountId)
        .collect(Collectors.toList());
  }

  public void updateStatusByTraceIdIfExists(Long traceId) {
    BatchTriggerRiskLogRecord batchTriggerRiskLogRecord = batchTriggerRiskLogModel.findByTraceId(traceId);
    if (Objects.isNull(batchTriggerRiskLogRecord)) {
      return;
    }
    batchTriggerRiskLogModel.updateStatus(batchTriggerRiskLogRecord, BatchTriggerLogStatus.CANCEL);
  }

  public boolean isLoanRiskReject(List<LoanUserRiskType> riskTypeList, LoanUserRiskTraceVO previousTraceVO, LoanCreditsStatus creditsStatus) {
    if (riskTypeList.contains(previousTraceVO.riskType) && previousTraceVO.creditsStatus == LoanCreditsStatus.REJECTED) {
      return true;
    }

    return false;
  }

  public boolean isMultiRiskReject(LoanUserRiskTraceVO previousTraceVO, Boolean rejectMultiApply, Boolean rejectMultiOrder) {
    if (LoanUserRiskType.getMultiLoanRiskType().contains(previousTraceVO.riskType) && Boolean.TRUE.equals(rejectMultiOrder)) {
      return true;
    }

    if (LoanUserRiskType.getMultiLoanCalcRiskType().contains(previousTraceVO.riskType) && Boolean.TRUE.equals(rejectMultiApply)) {
      return true;
    }

    return false;
  }

  @Nullable
  public String findLatestByLoanAccountIdAndTypeValue(Long loanAccountId, RiskOutputType type) {
    RiskOutputResultRecord defaultRecord = riskOutputResultModel.findLatestByLoanAccountIdAndType(loanAccountId, type);
    if (Objects.isNull(defaultRecord)) {
      return null;
    }
    return defaultRecord.getNewValue();
  }

  public String findLatestByTraceIdAndTypeValue(Long traceId, RiskOutputType type) {
    if (traceId == null) {
      return null;
    }
    RiskOutputResultRecord riskOutputResultRecord = riskOutputResultModel.findByTraceIdAndType(traceId, type);
    if (Objects.isNull(riskOutputResultRecord)) {
      return null;
    }
    return riskOutputResultRecord.getNewValue();
  }

  /**
   * 只用于符号为｜的类型
   *
   * @param traceId
   * @param type
   * @return
   */
  public List<String> getBlackRuleDetailValueListByTraceId(Long traceId, RiskOutputType type) {
    String value = findLatestByTraceIdAndTypeValue(traceId, type);
    if (StringUtils.isBlank(value)) {
      return Collections.emptyList();
    }
    return Arrays.asList(value.split("\\|"));
  }

  /**
   * 是否低复贷意愿
   */
  public Boolean isLowReloanFeeWill(Long loanAccountId) {
    String reloanFeeWillLevel = findLatestByLoanAccountIdAndTypeValue(loanAccountId, RiskOutputType.RELOAN_FEE_WILL_LEVEL);
    if (Objects.nonNull(reloanFeeWillLevel)) {
      return ReloanFeeWillUtil.isLowReloanFeeWillLevel(reloanFeeWillLevel);
    }
    String reloanFeeWillGroup = findLatestByLoanAccountIdAndTypeValue(loanAccountId, RiskOutputType.RELOAN_FEE_WILL_LEVEL_V4_GROUP);
    if (Objects.isNull(reloanFeeWillGroup)) {
      return false;
    }
    return ReloanFeeWillUtil.isLowReloanFeeWillGroup(reloanFeeWillGroup);
  }

  public String getFeeWillLevelV4(Long loanAccountId) {
      RiskOutputResultRecord result = riskOutputResultModel.findLatestByLoanAccountIdAndType(loanAccountId, RiskOutputType.RELOAN_FEE_WILL_LEVEL_V4);
      return result != null ? result.getNewValue() : null;
  }

  public List<LoanUserRiskTraceVO> getRiskTracePage(Long loanAccountId, Integer pageNo, Integer pageSize) {
    return getRiskTracePage(loanAccountId, pageNo, pageSize, false);
  }

  /**
   * @param isLy true 时按配置隐藏 riskType 过滤分页结果；配置空/缺失则不过滤（US3-6）
   */
  public List<LoanUserRiskTraceVO> getRiskTracePage(Long loanAccountId, Integer pageNo, Integer pageSize, Boolean isLy) {
    if (pageNo <= 0) {
      pageNo = 1;
    }
    if (pageSize <= 0) {
      pageSize = 10;
    }

    int offset = (pageNo - 1) * pageSize;
    Collection<String> excludedRiskTypes = resolveExcludedRiskTypesForOjk(isLy);
    List<LoanUserRiskTraceRecord> loanUserRiskTraceList =
        riskTraceModel.findByAccountIdWithPagination(loanAccountId, offset, pageSize, excludedRiskTypes);

    return loanUserRiskTraceList
        .stream()
        .map(LoanUserRiskTraceVO::fromOrNull)
        .collect(Collectors.toList());
  }

  /**
   * 仅当 isLy=true 且隐藏清单配置非空时返回需排除的 riskType code；否则空集合（不附加过滤）。
   */
  Collection<String> resolveExcludedRiskTypesForOjk(Boolean isLy) {
    if (!Boolean.TRUE.equals(isLy)) {
      return Collections.emptyList();
    }
    List<String> hidden = loanUserRiskTraceDisplayConfig.getOjkHiddenRiskTypes();
    if (CollectionUtils.isEmpty(hidden)) {
      return Collections.emptyList();
    }
    return hidden;
  }

}
