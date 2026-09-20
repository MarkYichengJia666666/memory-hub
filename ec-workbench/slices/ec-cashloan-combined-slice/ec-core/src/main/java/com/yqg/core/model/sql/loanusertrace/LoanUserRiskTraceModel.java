package com.yqg.core.model.sql.loanusertrace;

import com.google.common.collect.Lists;
import com.yqg.core.model.core.ModelInQueryBatchSize;
import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.LoanUserRiskTrace;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsRejectedReason;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.cashloan.risk.dashboard.RiskTraceProcessStats;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.risk.CreditsInfoDisplayContext;
import com.yqg.ec.common.enums.risk.LoanUserRiskSource;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.ec.common.i18n.time.Clock;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.max;

/**
 * 用户风控 Trace 的数据访问模型。
 *
 * <p>负责 Trace 创建、状态流转、业务查询及运行态处理/阻塞指标所需的窗口查询。</p>
 */
@Repository
public class LoanUserRiskTraceModel extends YqgBaseModel {
  /** 用户风控 Trace 主表引用。 */
  private static final LoanUserRiskTrace TABLE = Tables.LOAN_USER_RISK_TRACE;
  private static final int MAX_IN_BATCH_SIZE = 500;
  private static final long RISK_FLOW_TIME_CHUNK_MILLIS = 7L * Clock.MILLS_PER_DAY;

  public LoanUserRiskTraceRecord insert(Long userId,
                                        Long loanAccountId,
                                        Long traceId,
                                        Long orderId,
                                        LoanUserRiskType riskType,
                                        TriggerType triggerType,
                                        Long eventId,
                                        Long riskFlowId,
                                        RiskFlowTraceStatusV2 status,
                                        SourceType sourceType,
                                        TriggerSubType triggerSubType,
                                        LoanUserRiskTriggerSource triggerSource,
                                        String triggerSourceExternalId,
                                        LoanRiskCreditStage creditStage) {
    LoanUserRiskTraceRecord record = create().newRecord(TABLE);
    record.setUserId(userId);
    record.setLoanAccountId(loanAccountId);
    record.setTraceId(traceId);
    record.setOrderId(orderId);
    record.setRiskType(riskType.code);
    if (sourceType != null) {
      record.setSourceType(sourceType.name());
    }
    record.setTriggerType(triggerType.code);
    record.setEventId(eventId);
    record.setRiskFlowId(riskFlowId);
    record.setStatus(status.code);
    long now = Clock.now();
    record.setTimeCreated(now);
    record.setTimeUpdated(now);
    record.setTriggerSubType(triggerSubType.code);
    if (triggerSource != null) {
      record.setTriggerSource(triggerSource.name());
    }
    if (triggerSourceExternalId != null) {
      record.setTriggerSourceExternalId(triggerSourceExternalId);
    }
    if (creditStage != null) {
      record.setCreditStage(creditStage.name());
    }
    record.insert();
    return record;
  }

  public List<Long> findAccountIdsByTimeAndRiskType(Long startTime, Long endTime, LoanUserRiskType riskType) {
    return create()
        .select(TABLE.LOAN_ACCOUNT_ID)
        .from(TABLE)
        .where(TABLE.TIME_CREATED.ge(startTime))
        .and(TABLE.TIME_CREATED.le(endTime))
        .and(TABLE.RISK_TYPE.eq(riskType.code))
        .fetchInto(Long.class);
  }

  /**
   * 某个时间段中最新的一条riskType记录。
   * 取出该记录的AccountID,ID
   *
   * @param startTime
   * @param endTime
   * @param riskType
   * @return
   */
  public Map<Long, Long> findAccountIdAndTraceIdByTypeAndTimeWhichLatest(Long startTime, Long endTime, LoanUserRiskType riskType) {
    return create()
        .select(TABLE.LOAN_ACCOUNT_ID, DSL.max(TABLE.ID))
        .from(TABLE)
        .where(TABLE.TIME_CREATED.between(startTime, endTime))
        .and(TABLE.RISK_TYPE.eq(riskType.code))
        .and(TABLE.TRACE_ID.isNotNull())
        .groupBy(TABLE.LOAN_ACCOUNT_ID)
        .fetchMap(TABLE.LOAN_ACCOUNT_ID, DSL.max(TABLE.ID));
  }

  public List<LoanUserRiskTraceRecord> findByUserId(Long userId, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .fetch();
  }

  /**
   * 聚合创建时间窗口内的 Trace 提交、完成与通过数量。
   *
   * <p><b>核心逻辑</b>：同时按风险类型、普通/跑批来源和业务触发来源分组，
   * 确保 Grafana 中每个来源形成独立时序；跑批来源由 {@code trigger_source=BATCH_RISK}
   * 判定，历史空触发来源归入 UNKNOWN。</p>
   *
   * @param startTime Trace 创建时间下界，包含
   * @param endTime Trace 创建时间上界，不包含
   * @return 各业务维度组合的处理效率统计
   */
  public List<RiskTraceProcessStats> aggregateProcessStats(long startTime, long endTime) {
    // 1. 通过 trigger_source 区分普通触发与跑批触发，不关联跑批日志表。
    Field<String> riskSource = DSL.when(
            TABLE.TRIGGER_SOURCE.eq(LoanUserRiskTriggerSource.BATCH_RISK.name()),
            LoanUserRiskSource.BATCH.code)
        .otherwise(LoanUserRiskSource.NORMAL.code)
        .as("riskSource");
    Field<String> triggerSource =
        DSL.coalesce(TABLE.TRIGGER_SOURCE, "UNKNOWN").as("triggerSource");
    // 2. 在同一次扫描中完成提交、完成和通过数量的条件聚合。
    Field<BigDecimal> finish = DSL.sum(DSL.when(
        TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code), 1).otherwise(0)).as("finish");
    Field<BigDecimal> accepted = DSL.sum(DSL.when(
        TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code)
            .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code)),
        1).otherwise(0)).as("accepted");

    // 3. 核心：按三个低基数业务维度分组，返回 Collector 可直接写点的结果。
    return create()
        .select(
            TABLE.RISK_TYPE,
            riskSource,
            triggerSource,
            DSL.count().as("submit"),
            finish,
            accepted)
        .from(TABLE)
        .where(TABLE.TIME_CREATED.ge(startTime))
        .and(TABLE.TIME_CREATED.lt(endTime))
        .groupBy(TABLE.RISK_TYPE, riskSource, triggerSource)
        .fetch()
        .map(record -> new RiskTraceProcessStats(
            LoanUserRiskType.fromCode(record.get(TABLE.RISK_TYPE)).name(),
            record.get(riskSource),
            record.get(triggerSource),
            numberValue(record, "submit"),
            numberValue(record, "finish"),
            numberValue(record, "accepted")));
  }

  /**
   * 分页查询创建时间窗口内仍处于风控执行延迟阶段的 Trace。
   *
   * <p><b>核心逻辑</b>：仅扫描 PRE_INIT、INIT、EXECUTE_NEW_ENGINE 三个待推进状态，
   * 由 Collector 带着本页 {@code traceId} 去审核表做存在性判断后归因。</p>
   *
   * @param startTime Trace 创建时间下界，包含
   * @param endTime Trace 创建时间上界，不包含
   * @param lastId 上一页最后一条记录 ID
   * @param limit 单页最大记录数
   * @return 按 ID 升序排列的延迟 Trace
   */
  public List<LoanUserRiskTraceRecord> fetchDelayBlockByTimeCreatedAndIdAfter(
      long startTime,
      long endTime,
      long lastId,
      int limit) {
    // 核心：使用状态索引缩小候选集，并以创建窗口和 ID 游标控制扫描范围。
    return create()
        .selectFrom(TABLE.useIndex("index__status"))
        .where(TABLE.STATUS.in(
            RiskFlowTraceStatusV2.PRE_INIT.code,
            RiskFlowTraceStatusV2.INIT.code,
            RiskFlowTraceStatusV2.EXECUTE_NEW_ENGINE.code))
        .and(TABLE.TIME_CREATED.ge(startTime))
        .and(TABLE.TIME_CREATED.lt(endTime))
        .and(TABLE.ID.gt(lastId))
        .and(TABLE.TRACE_ID.isNotNull())
        .orderBy(TABLE.ID.asc())
        .limit(limit)
        .fetch();
  }

  private static long numberValue(Record record, String fieldName) {
    Number value = record.get(fieldName, Number.class);
    return value == null ? 0L : value.longValue();
  }

  public List<LoanUserRiskTraceRecord> findFinishedRiskByAccountIdsAndUpdatedTime(List<Long> loanAccountIds, List<LoanUserRiskType> types, Long startTime, Long endTime) {
    List<String> typeCodes = types.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(loanAccountIds))
        .and(TABLE.RISK_TYPE.in(typeCodes))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .and(TABLE.TIME_UPDATED.ge(startTime))
        .and(TABLE.TIME_UPDATED.le(endTime))
        .orderBy(TABLE.TIME_UPDATED.asc())
        .fetch();
  }

  public List<LoanUserRiskTraceRecord> findFirstRiskAcceptByAccountIds(List<Long> loanAccountIds, List<LoanUserRiskType> types) {
    List<String> typeCodes = types.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(loanAccountIds))
        .and(TABLE.RISK_TYPE.in(typeCodes))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .orderBy(TABLE.TIME_UPDATED.asc())
        .fetch();
  }

  /**
   * 找到一批用户这些type的风控记录中，最近一条通过的
   *
   * @param loanAccountIds
   * @param types
   * @return LoanUserRiskTraceRecord
   */
  public LoanUserRiskTraceRecord findLatestRiskAcceptByAccountIds(List<Long> loanAccountIds, List<LoanUserRiskType> types) {
    List<String> typeCodes = types.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(loanAccountIds))
        .and(TABLE.RISK_TYPE.in(typeCodes))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .orderBy(TABLE.TIME_UPDATED.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findLatestRiskAcceptByUserId(Long userId, List<LoanUserRiskType> types) {
    List<String> typeCodes = types.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.RISK_TYPE.in(typeCodes))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .orderBy(TABLE.TIME_UPDATED.desc())
        .limit(1)
        .fetchOne();
  }

  /**
   * 找到一批用户这些type的风控记录中，最近一条
   *
   * @param loanAccountIds
   * @param types
   * @return LoanUserRiskTraceRecord
   */
  public LoanUserRiskTraceRecord findLatestRiskByAccountIdsAndRiskTypes(List<Long> loanAccountIds, List<LoanUserRiskType> types) {
    List<String> typeCodes = types.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(loanAccountIds))
        .and(TABLE.RISK_TYPE.in(typeCodes))
        .orderBy(TABLE.TIME_UPDATED.desc())
        .limit(1)
        .fetchOne();
  }

  public List<Long> findLatestTraceIdsByAccountIdsAndType(List<Long> loanAccountIds, LoanUserRiskType type) {
    return create()
        .select(max(TABLE.TRACE_ID))
        .from(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(loanAccountIds))
        .and(TABLE.RISK_TYPE.eq(type.code))
        .and(TABLE.TRACE_ID.isNotNull())
        .groupBy(TABLE.LOAN_ACCOUNT_ID)
        .fetchInto(Long.class);
  }

  public LoanUserRiskTraceRecord findRiskAcceptByAccountId(Long loanAccountId, LoanUserRiskType type) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(loanAccountId))
        .and(TABLE.RISK_TYPE.eq(type.code))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .limit(1)
        .fetchOne();
  }

  public List<LoanUserRiskTraceRecord> listAcceptedRiskTracesByUserId(Long userId) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .fetch();
  }


  public List<LoanUserRiskTraceRecord> findByUserIdAndStatusAndType(Long userId, RiskFlowTraceStatusV2 traceStatus, LoanUserRiskType riskType) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.STATUS.eq(traceStatus.code))
        .and(TABLE.RISK_TYPE.eq(riskType.code))
        .orderBy(TABLE.TRACE_ID)
        .fetch();
  }


  public List<LoanUserRiskTraceRecord> findByAccountIdAndStatusAndTypes(Long accountId, LoanCreditsStatus status, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.CREDITS_STATUS.eq(status.code))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .orderBy(TABLE.TRACE_ID)
        .fetch();
  }

  public LoanUserRiskTraceRecord findEarliestByUserIdAndStatusesAndTypes(Long userId, Collection<LoanCreditsStatus> statuses, Collection<LoanUserRiskType> riskTypes) {
    List<String> statusesCodes = statuses.stream().map(o -> o.code).collect(Collectors.toList());
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.CREDITS_STATUS.in(statusesCodes))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .orderBy(TABLE.TRACE_ID.asc())
        .limit(1)
        .fetchOne();
  }

  public List<LoanUserRiskTraceRecord> findByAccountIdsAndTimeAndTypes(List<Long> accountIds, Long timeStart, LoanUserRiskType... riskTypes) {
    List<String> riskTypeCodes = Arrays.stream(riskTypes).map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(accountIds))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .and(TABLE.TIME_UPDATED.ge(timeStart))
        .and(TABLE.TRACE_ID.isNotNull())
        .fetch();
  }

  public List<LoanUserRiskTraceRecord> findByAccountIdAndStartTimeAndTypesOrderByTimeAsc(Long accountId, Long timeStart, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .and(TABLE.TIME_UPDATED.ge(timeStart))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .orderBy(TABLE.TIME_UPDATED.asc())
        .fetch();
  }

  public LoanUserRiskTraceRecord findLastedByUserIdAndRiskType(Long userId, LoanUserRiskType riskType) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.RISK_TYPE.eq(riskType.code))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findLastedByAccountIdAndRiskType(Long loanAccountId, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodeList = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(loanAccountId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findLastedByAccountIdAndRiskTypeForHaveTraceId(Long loanAccountId, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodeList = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(loanAccountId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .and(TABLE.TRACE_ID.isNotNull())
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findByTraceId(Long traceId) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.TRACE_ID.eq(traceId))
        .fetchOne();
  }

  public Map<Long, String> findRiskTypeNameByTraceIds(Collection<Long> traceIds) {
    if (CollectionUtils.isEmpty(traceIds)) {
      return java.util.Collections.emptyMap();
    }
    return create()
        .select(TABLE.TRACE_ID, TABLE.RISK_TYPE)
        .from(TABLE)
        .where(TABLE.TRACE_ID.in(traceIds))
        .fetchMap(
            TABLE.TRACE_ID,
            record -> LoanUserRiskType.fromCode(record.get(TABLE.RISK_TYPE)).name());
  }

  public LoanUserRiskTraceRecord findLastedCreditsRiskTraceByUserId(Long userId, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodeList = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }


  public LoanUserRiskTraceRecord findLastedCreditsRiskTraceByUserIdWithCreditsStatus(Long userId, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodeList = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.CREDITS_STATUS.isNotNull())
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findLastedCreditsRiskTraceByAccountId(Long accountId, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodeList = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());

    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findLatestByTimestampAndAccountId(Long accountId, List<LoanUserRiskType> riskTypes, Long endTime) {
    List<String> riskTypeCodeList = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .and(TABLE.TIME_CREATED.lt(endTime))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public List<LoanUserRiskTraceRecord> findByTimestampAndAccountId(Long accountId, List<LoanUserRiskType> riskTypes, Long endTime) {
    List<String> riskTypeCodeList = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .and(TABLE.TIME_CREATED.lt(endTime))
        .fetch();
  }

  public LoanUserRiskTraceRecord findLatestByAccountIdAndCreditsStatusWithEndTime(Long accountId, LoanCreditsStatus creditsStatus, Long endTime) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.CREDITS_STATUS.eq(creditsStatus.code))
        .and(TABLE.TIME_CREATED.lt(endTime))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findLatestByAccountIdAndTimestamp(Long accountId, Long endTime, List<LoanUserRiskType> loanUserRiskTypeList) {
    List<String> loanUserRiskTypeCodeList = loanUserRiskTypeList.stream().map(type -> type.code).collect(Collectors.toList());

    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.TIME_CREATED.lt(endTime))
        .and(TABLE.RISK_TYPE.in(loanUserRiskTypeCodeList))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public void updateTrace(LoanUserRiskTraceRecord record, Long eventId, Long riskFlowId, RiskFlowTraceStatusV2 status) {
    record.setEventId(eventId);
    record.setRiskFlowId(riskFlowId);
    record.setStatus(status.code);
    record.setTimeUpdated(Clock.now());
    record.store();
  }

  public void updateStatus(LoanUserRiskTraceRecord record, RiskFlowTraceStatusV2 status) {
    record.setStatus(status.code);
    record.setTimeUpdated(Clock.now());
    record.store();
  }

  public void acceptTrace(Long traceId) {
    LoanUserRiskTraceRecord record = findByTraceId(traceId);
    if (record == null) {
      return;
    }
    record.setCreditsStatus(LoanCreditsStatus.ACCEPTED.code);
    record.setTimeUpdated(Clock.now());
    record.store();
  }

  public void rejectTrace(Long traceId, LoanCreditsRejectedReason reason) {
    LoanUserRiskTraceRecord record = findByTraceId(traceId);
    if (record == null) {
      return;
    }
    record.setCreditsStatus(LoanCreditsStatus.REJECTED.code);
    record.setRejectReason(reason.code);
    record.setTimeUpdated(Clock.now());
    record.store();
  }

  public void manualReviewTrace(Long traceId) {
    LoanUserRiskTraceRecord record = findByTraceId(traceId);
    if (record == null) {
      return;
    }
    record.setCreditsStatus(LoanCreditsStatus.MANUAL_REVIEW.code);
    record.setTimeUpdated(Clock.now());
    record.store();
  }

  public List<Long> fetchAccountIdsByTraceId(Collection<Long> traceIds) {
    return create()
        .select(TABLE.LOAN_ACCOUNT_ID)
        .from(TABLE)
        .where(TABLE.TRACE_ID.in(traceIds))
        .fetchInto(Long.class);
  }

  public LoanUserRiskTraceRecord findById(Long id) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.eq(id))
        .fetchOne();
  }

  public List<LoanUserRiskTraceRecord> findByIds(Collection<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return Collections.emptyList();
    }
    List<Long> distinctIds = ModelInQueryBatchSize.dedupeIdsPreserveOrder(ids);
    if (distinctIds.isEmpty()) {
      return Collections.emptyList();
    }
    if (distinctIds.size() <= MAX_IN_BATCH_SIZE) {
      return findByIdsSingleBatch(distinctIds);
    }
    return Lists.partition(distinctIds, MAX_IN_BATCH_SIZE).stream()
        .flatMap(batch -> findByIdsSingleBatch(batch).stream())
        .collect(Collectors.toList());
  }

  private List<LoanUserRiskTraceRecord> findByIdsSingleBatch(Collection<Long> ids) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.in(ids))
        .fetch();
  }

  public List<LoanUserRiskTraceRecord> findByUserIdAndRiskTypeAndTimeCreated(Long userId, List<LoanUserRiskType> riskTypeList, Long startTime, Long endTime) {
    List<String> riskTypeCodeList = riskTypeList.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.equal(userId))
        .and(TABLE.TIME_CREATED.between(startTime, endTime))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .and(TABLE.TRACE_ID.isNotNull())
        .orderBy(TABLE.TIME_CREATED.desc())
        .fetch();
  }


  public List<LoanUserRiskTraceRecord> findByUserIdAndRiskTypeAndTimeCreatedAllowedTraceIdIsNull(Long userId, List<LoanUserRiskType> riskTypeList, Long startTime, Long endTime) {
    List<String> riskTypeCodeList = riskTypeList.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.equal(userId))
        .and(TABLE.TIME_CREATED.between(startTime, endTime))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .fetch();
  }

  public List<LoanUserRiskTraceRecord> findAcceptedByUserIdAndRiskTypeAndTimeUpdate(Long userId, List<LoanUserRiskType> riskTypes, Long startTime, Long endTime) {
    List<String> riskTypeCodeList = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.equal(userId))
        .and(TABLE.TIME_UPDATED.between(startTime, endTime))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .orderBy(TABLE.TIME_UPDATED.desc())
        .fetch();
  }

  /**
   * Admin 使用，无影响
   *
   * @param accountIds
   * @param context
   * @return
   */
  public Map<Long, Long> getMaxTraceId(List<Long> accountIds, CreditsInfoDisplayContext context) {
    List<LoanUserRiskType> riskTypes = LoanUserRiskType.getForCreditsInfoDisplayContext(context);
    List<String> riskTypeCodes = riskTypes.stream().map(t -> t.code).collect(Collectors.toList());
    return create()
        .select(
            TABLE.LOAN_ACCOUNT_ID,
            max(TABLE.TRACE_ID))
        .from(TABLE)
        .where(TABLE.RISK_TYPE.in(riskTypeCodes))
        .and(TABLE.LOAN_ACCOUNT_ID.in(accountIds))
        .and(TABLE.TRACE_ID.isNotNull())
        .groupBy(TABLE.LOAN_ACCOUNT_ID)
        .fetchMap(Record2::value1, Record2::value2);
  }

  public Map<Long, LoanUserRiskTraceRecord> fetchMapByTraceIds(Collection<Long> traceIds) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.TRACE_ID.in(traceIds))
        .fetchMap(LoanUserRiskTraceRecord::getTraceId, record -> record);
  }

  public Map<Long, LoanUserRiskTraceRecord> fetchMapByIds(Collection<Long> ids) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.in(ids))
        .and(TABLE.TRACE_ID.isNotNull())
        .fetchMap(LoanUserRiskTraceRecord::getTraceId, record -> record);
  }

  public Integer countForCanNotModifyMobileNumber(Long userId, RiskFlowTraceStatusV2 traceStatus, List<LoanUserRiskType> riskTypeList, LoanCreditsStatus creditsStatus, long startTime) {
    List<String> riskTypeCodeList = riskTypeList.stream().map(type -> type.code).collect(Collectors.toList());

    return create()
        .selectCount()
        .from(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.STATUS.eq(traceStatus.code))
        .and(TABLE.CREDITS_STATUS.eq(creditsStatus.code))
        .and(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .and(TABLE.TIME_UPDATED.greaterOrEqual(startTime))
        .fetchOneInto(Integer.class);
  }

  public LoanUserRiskTraceRecord findLastAcceptRiskTraceByAccountIdAndRiskTypes(Long accountId, List<LoanUserRiskType> firstLoanRiskTypes) {
    List<String> firstLoanRiskTypeCodes = firstLoanRiskTypes.stream().map(loanUserRiskType -> loanUserRiskType.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.equal(accountId))
        .and(TABLE.RISK_TYPE.in(firstLoanRiskTypeCodes))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord getLastFinishRiskTrace(Long loanAccountId, List<LoanUserRiskType> loanUserRiskTypeList) {
    List<String> loanUserRiskTypeCodes = loanUserRiskTypeList.stream().map(loanUserRiskType -> loanUserRiskType.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(loanAccountId))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .and(TABLE.RISK_TYPE.in(loanUserRiskTypeCodes))
        .orderBy(TABLE.ID.desc())
        .limit(1)
        .fetchOne();
  }

  public List<Long> listTraceIdByStatusAndRiskType(
      List<LoanUserRiskType> loanUserRiskTypeList, RiskFlowTraceStatusV2 status, Long minTimeUpdated) {
    List<String> riskTypeCodeList = loanUserRiskTypeList.stream().map(type -> type.code).collect(Collectors.toList());
    Condition condition = TABLE.RISK_TYPE.in(riskTypeCodeList)
        .and(TABLE.STATUS.eq(status.code));
    if (minTimeUpdated != null) {
      condition = condition.and(TABLE.TIME_UPDATED.ge(minTimeUpdated));
    }
    return create()
        .select(TABLE.TRACE_ID)
        .from(TABLE)
        .where(condition)
        .fetchInto(Long.class);
  }


  public List<String> listSourceTypeByAccountId(Long accountId) {
    return create()
        .selectDistinct(TABLE.SOURCE_TYPE)
        .from(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.TRACE_ID.isNotNull())
        .fetchInto(String.class);
  }

  /**
   * Admin 展示
   *
   */
  public List<LoanUserRiskTraceRecord> listTraceByAccountId(Long accountId, Integer limit, Integer offset) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.TRACE_ID.isNotNull())
        .orderBy(TABLE.TIME_CREATED.asc())
        .offset(offset)
        .limit(limit)
        .fetch();
  }

  /**
   * Admin 展示
   *
   * @param loanAccountId
   * @return
   */
  public Long countTraceByAccountId(Long loanAccountId) {
    return countTraceByAccountId(loanAccountId, null);
  }

  /**
   * Admin 展示计数；{@code excludedRiskTypes} 非空时排除清单内 riskType，
   * 且 riskType 为空的行保留（US3-4，避免 SQL NOT IN 误杀 NULL）。
   */
  public Long countTraceByAccountId(Long loanAccountId, Collection<String> excludedRiskTypes) {
    return create()
        .selectCount()
        .from(TABLE)
        .where(buildAccountIdConditionWithOptionalRiskTypeExclusion(loanAccountId, excludedRiskTypes))
        .fetchOneInto(Long.class);
  }

  public String findRiskTypeByOrderId(Long orderId) {
    return create()
        .select(TABLE.RISK_TYPE)
        .from(TABLE)
        .where(TABLE.ORDER_ID.eq(orderId))
        .fetchOneInto(String.class);
  }

  public LoanUserRiskTraceRecord findByOrderId(Long orderId) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ORDER_ID.eq(orderId))
        .limit(1)
        .fetchOne();
  }

  public List<LoanUserRiskTraceRecord> findByIdRangeAndCreditsStatusTypes(Long startId, Long endId, List<LoanUserRiskType> riskTypes, List<String> creditsStatus) {
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.ge(startId))
        .and(TABLE.ID.le(endId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .and(TABLE.CREDITS_STATUS.in(creditsStatus))
        .fetch();
  }

  public List<Long> findUsedIdsByRangeAndCreditsStatusTypesAndTime(Long startId, Long endId, List<LoanUserRiskType> riskTypes, Long maxTimeUpdated) {
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectDistinct(TABLE.USER_ID)
        .from(TABLE)
        .where(TABLE.USER_ID.ge(startId))
        .and(TABLE.USER_ID.le(endId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .and(TABLE.TIME_UPDATED.lessOrEqual(maxTimeUpdated))
        .fetchInto(Long.class);
  }

  /**
   * 单批查询：每个 account 在排除指定 riskType 后的最大 risk_trace 主键 ID。
   */
  public List<Long> listMaxIdsByAccountIdsExcludingRiskTypes(
      Collection<Long> accountIds, Collection<String> excludedRiskTypeCodes) {
    if (CollectionUtils.isEmpty(accountIds)) {
      return Collections.emptyList();
    }
    return create()
        .select(max(TABLE.ID))
        .from(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(accountIds))
        .and(TABLE.RISK_TYPE.notIn(excludedRiskTypeCodes))
        .groupBy(TABLE.LOAN_ACCOUNT_ID)
        .fetchInto(Long.class);
  }

  public List<LoanUserRiskTraceRecord> findByAccountIdsAndStatusAndTypes(List<Long> accountIds, LoanCreditsStatus status, List<LoanUserRiskType> riskTypes) {
    if (CollectionUtils.isEmpty(accountIds)) {
      return Collections.emptyList();
    }
    if (accountIds.size() <= MAX_IN_BATCH_SIZE) {
      return findByAccountIdsAndStatusAndTypesSingleBatch(accountIds, status, riskTypes);
    }
    return Lists.partition(new ArrayList<>(accountIds), MAX_IN_BATCH_SIZE).stream()
        .flatMap(batch -> findByAccountIdsAndStatusAndTypesSingleBatch(batch, status, riskTypes).stream())
        .collect(Collectors.toList());
  }

  private List<LoanUserRiskTraceRecord> findByAccountIdsAndStatusAndTypesSingleBatch(
      List<Long> accountIds, LoanCreditsStatus status, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(accountIds))
        .and(TABLE.CREDITS_STATUS.eq(status.code))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .orderBy(TABLE.TRACE_ID)
        .fetch();
  }

  public List<LoanUserRiskTraceRecord> fetchByStartIdAndEndTimeAndRiskTypesWithLimit(Long startId, Long endTimeStamp, List<LoanUserRiskType> riskTypes, Integer limit) {
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.ge(startId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .and(TABLE.TIME_CREATED.le(endTimeStamp))
        .orderBy(TABLE.ID.asc())
        .limit(limit)
        .fetch();
  }

  public Map<Long, LoanUserRiskTraceRecord> findLoanAccountIdAndLastedRiskTypeMap(List<Long> loanAccountIdList) {
    List<Long> recordIdList = create()
        .select(TABLE.ID.max())
        .from(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(loanAccountIdList))
        .and(TABLE.TRACE_ID.isNotNull())
        .groupBy(TABLE.LOAN_ACCOUNT_ID)
        .fetchInto(Long.class);
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.in(recordIdList))
        .fetch().stream()
        .collect(Collectors.toMap(LoanUserRiskTraceRecord::getLoanAccountId, vo -> vo));
  }

  public LoanUserRiskTraceRecord findFirstTraceByAccountIdAndRiskTypes(Long accountId, List<LoanUserRiskType> firstLoanRiskTypes) {
    List<String> firstLoanRiskTypeCodes = firstLoanRiskTypes.stream().map(loanUserRiskType -> loanUserRiskType.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.equal(accountId))
        .and(TABLE.RISK_TYPE.in(firstLoanRiskTypeCodes))
        .orderBy(TABLE.TIME_CREATED.asc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findLastedFinishedByUserIdAndRiskType(Long userId, List<LoanUserRiskType> calcCreditsRiskTypes) {
    List<String> codes = calcCreditsRiskTypes.stream().map(loanUserRiskType -> loanUserRiskType.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.equal(userId))
        .and(TABLE.RISK_TYPE.in(codes))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.ACCEPTED.code))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }


  public List<Long> fetchByAccountIdAndEndTimeWithLimit(Long loanAccountId, Long endTimeStamp, Integer limit) {
    return create()
        .select(TABLE.TRACE_ID)
        .from(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(loanAccountId))
        .and(TABLE.TIME_CREATED.le(endTimeStamp))
        .and(TABLE.TRACE_ID.isNotNull())
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(limit)
        .fetch(TABLE.TRACE_ID);
  }

  public List<LoanUserRiskTraceRecord> fetchByCondition(LoanUserRiskTraceCondition loanUserRiskTraceCondition) {
    if (CollectionUtils.isNotEmpty(loanUserRiskTraceCondition.loanAccountIds)
        && loanUserRiskTraceCondition.loanAccountIds.size() > MAX_IN_BATCH_SIZE) {
      return Lists.partition(new ArrayList<>(loanUserRiskTraceCondition.loanAccountIds), MAX_IN_BATCH_SIZE).stream()
          .flatMap(batch -> fetchByConditionInternal(buildConditionWithAccountIds(loanUserRiskTraceCondition, batch)).stream())
          .collect(Collectors.toList());
    }
    return fetchByConditionInternal(loanUserRiskTraceCondition);
  }

  private LoanUserRiskTraceCondition buildConditionWithAccountIds(
      LoanUserRiskTraceCondition source, List<Long> accountIds) {
    return LoanUserRiskTraceCondition.builder()
        .loanAccountIds(accountIds)
        .riskTypes(source.riskTypes)
        .status(source.status)
        .creditsStatus(source.creditsStatus)
        .orderIds(source.orderIds)
        .userIds(source.userIds)
        .traceIds(source.traceIds)
        .eventId(source.eventId)
        .startTimeCreated(source.startTimeCreated)
        .endTimeCreated(source.endTimeCreated)
        .startTimeUpdated(source.startTimeUpdated)
        .endTimeUpdated(source.endTimeUpdated)
        .build();
  }

  private List<LoanUserRiskTraceRecord> fetchByConditionInternal(LoanUserRiskTraceCondition loanUserRiskTraceCondition) {
    Condition condition = buildCondition(loanUserRiskTraceCondition);
    return create()
        .selectFrom(TABLE)
        .where(condition)
        .fetch();
  }

  public List<Long> fetchTraceIdByCondition(LoanUserRiskTraceCondition loanUserRiskTraceCondition) {
    Condition condition = buildCondition(loanUserRiskTraceCondition);
    return create()
        .select(TABLE.TRACE_ID)
        .from(TABLE)
        .where(condition)
        .fetchInto(Long.class);
  }

  public List<Long> fetchRiskFlowIdsUsedWithinTimeFrame(Long startTime, Long endTime) {
    if (startTime == null || endTime == null || startTime > endTime) {
      return Collections.emptyList();
    }
    Set<Long> riskFlowIds = new LinkedHashSet<>();
    long cursor = startTime;
    while (cursor <= endTime) {
      long chunkEnd = Math.min(cursor + RISK_FLOW_TIME_CHUNK_MILLIS - 1, endTime);
      riskFlowIds.addAll(create()
          .selectDistinct(TABLE.RISK_FLOW_ID)
          .from(TABLE)
          .where(TABLE.TIME_CREATED.ge(cursor))
          .and(TABLE.TIME_CREATED.le(chunkEnd))
          .fetchInto(Long.class));
      cursor = chunkEnd + 1;
    }
    return new ArrayList<>(riskFlowIds);
  }

  private Condition buildCondition(LoanUserRiskTraceCondition loanUserRiskTraceCondition) {
    Condition condition = DSL.trueCondition();
    if (CollectionUtils.isNotEmpty(loanUserRiskTraceCondition.loanAccountIds)) {
      condition = condition.and(TABLE.LOAN_ACCOUNT_ID.in(loanUserRiskTraceCondition.loanAccountIds));
    }

    if (CollectionUtils.isNotEmpty(loanUserRiskTraceCondition.riskTypes)) {
      condition = condition.and(TABLE.RISK_TYPE.in(loanUserRiskTraceCondition.riskTypes));
    }

    if (StringUtils.isNotEmpty(loanUserRiskTraceCondition.status)) {
      condition = condition.and(TABLE.STATUS.eq(loanUserRiskTraceCondition.status));
    }

    if (CollectionUtils.isNotEmpty(loanUserRiskTraceCondition.orderIds)) {
      condition = condition.and(TABLE.ORDER_ID.in(loanUserRiskTraceCondition.orderIds));
    }

    if (CollectionUtils.isNotEmpty(loanUserRiskTraceCondition.userIds)) {
      condition = condition.and(TABLE.USER_ID.in(loanUserRiskTraceCondition.userIds));
    }

    if (CollectionUtils.isNotEmpty(loanUserRiskTraceCondition.traceIds)) {
      condition = condition.and(TABLE.TRACE_ID.in(loanUserRiskTraceCondition.traceIds));
    }

    if (loanUserRiskTraceCondition.eventId != null) {
      condition = condition.and(TABLE.EVENT_ID.eq(loanUserRiskTraceCondition.eventId));
    }

    if (loanUserRiskTraceCondition.creditsStatus != null) {
      condition = condition.and(TABLE.CREDITS_STATUS.eq(loanUserRiskTraceCondition.creditsStatus));
    }

    if (loanUserRiskTraceCondition.startTimeCreated != null) {
      condition = condition.and(TABLE.TIME_CREATED.greaterOrEqual(loanUserRiskTraceCondition.startTimeCreated));
    }

    if (loanUserRiskTraceCondition.endTimeCreated != null) {
      condition = condition.and(TABLE.TIME_CREATED.lessOrEqual(loanUserRiskTraceCondition.endTimeCreated));
    }

    if (loanUserRiskTraceCondition.startTimeUpdated != null) {
      condition = condition.and(TABLE.TIME_UPDATED.greaterOrEqual(loanUserRiskTraceCondition.startTimeUpdated));
    }

    if (loanUserRiskTraceCondition.endTimeUpdated != null) {
      condition = condition.and(TABLE.TIME_UPDATED.lessOrEqual(loanUserRiskTraceCondition.endTimeUpdated));
    }

    return condition;
  }

  public LoanUserRiskTraceRecord insertWithPreInitTraceStatus(Long userId, Long accountId, Long orderId,
                                                              LoanUserRiskType riskType, TriggerType triggerType, Long eventId,
                                                              Long riskFlowId, RiskFlowTraceStatusV2 status,
                                                              SourceType sourceType, TriggerSubType triggerSubType,
                                                              LoanUserRiskTriggerSource triggerSource,
                                                              String triggerSourceExternalId,
                                                              LoanRiskCreditStage creditStage) {
    LoanUserRiskTraceRecord record = create().newRecord(TABLE);
    record.setUserId(userId);
    record.setLoanAccountId(accountId);
    record.setOrderId(orderId);
    record.setRiskType(riskType.code);
    if (sourceType != null) {
      record.setSourceType(sourceType.name());
    }
    record.setTriggerType(triggerType.code);
    record.setEventId(eventId);
    record.setRiskFlowId(riskFlowId);
    record.setStatus(status.code);
    long now = Clock.now();
    record.setTimeCreated(now);
    record.setTimeUpdated(now);
    record.setTriggerSubType(triggerSubType.code);
    if (triggerSource != null) {
      record.setTriggerSource(triggerSource.name());
    }
    if (triggerSourceExternalId != null) {
      record.setTriggerSourceExternalId(triggerSourceExternalId);
    }
    if (creditStage != null) {
      record.setCreditStage(creditStage.name());
    }
    record.insert();
    return record;
  }

  public LoanUserRiskTraceRecord updateStatusAndTraceId(LoanUserRiskTraceRecord record, RiskFlowTraceStatusV2 statusV2, Long traceId) {
    record.setTraceId(traceId);
    long now = Clock.now();
    record.setStatus(statusV2.code);
    record.setTimeUpdated(now);
    record.update();
    return record;
  }

  public List<LoanUserRiskTraceRecord> fetchUnprocessedRiskTraceByTimeCreated(Long startTime, Long endTime, Collection<Long> traceIdsInWhitelist, Collection<Long> loanAccountIdsInWhitelist) {
    Condition condition = DSL.trueCondition()
        .and(
            (TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code).and(TABLE.CREDITS_STATUS.eq(LoanCreditsStatus.MANUAL_REVIEW.code)))
                .or(TABLE.STATUS.eq(RiskFlowTraceStatusV2.INIT.code))
        )
        .and(TABLE.TIME_CREATED.between(startTime, endTime));

    if (CollectionUtils.isNotEmpty(traceIdsInWhitelist)) {
      condition = condition.and(TABLE.TRACE_ID.notIn(traceIdsInWhitelist));
    }
    if (CollectionUtils.isNotEmpty(loanAccountIdsInWhitelist)) {
      condition = condition.and(TABLE.LOAN_ACCOUNT_ID.notIn(loanAccountIdsInWhitelist));
    }

    return create()
        .selectFrom(TABLE)
        .where(condition)
        .orderBy(TABLE.ID.desc())
        .limit(500)
        .fetch();
  }

  public List<LoanUserRiskTraceRecord> findFinishedTraceByRiskTypeListAndTimeCreatedRange(
      List<LoanUserRiskType> riskTypeList,
      Long startTime,
      Long endTime
  ) {
    List<String> riskTypeCodeList = riskTypeList.stream().map(r -> r.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.RISK_TYPE.in(riskTypeCodeList))
        .and(TABLE.STATUS.eq(RiskFlowTraceStatusV2.FINISH.code))
        .and(TABLE.TIME_CREATED.between(startTime, endTime))
        .fetch();
  }

  public Map<Long, LoanUserRiskTraceRecord> fetchLatestMapByUserIds(Collection<Long> userIds, List<LoanUserRiskType> riskTypes) {
    List<String> riskTypeCodes = riskTypes.stream().map(type -> type.code).collect(Collectors.toList());
    Map<Long, Long> traceIdMap = create()
        .select(TABLE.USER_ID, max(TABLE.ID))
        .from(TABLE)
        .where(TABLE.USER_ID.in(userIds))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .groupBy(TABLE.USER_ID)
        .fetchMap(TABLE.USER_ID, max(TABLE.ID));
    List<Long> traceIds = traceIdMap.values().stream().collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.in(traceIds))
        .fetchMap(TABLE.USER_ID, record -> record);
  }

  public List<Long> fetchTraceIdsByLoanAccountIdsAndRiskType(List<Long> loanAccountIds, LoanUserRiskType riskType) {
    return create()
        .select(TABLE.TRACE_ID)
        .from(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(loanAccountIds))
        .and(TABLE.RISK_TYPE.ne(riskType.code))
        .fetchInto(Long.class);
  }

  public List<Long> fetchTraceIdsByLoanAccountIdsAndWithoutRiskType(List<Long> loanAccountIds, LoanUserRiskType riskType) {
    return create()
        .select(TABLE.TRACE_ID)
        .from(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.in(loanAccountIds))
        .and(TABLE.RISK_TYPE.ne(riskType.code))
        .fetchInto(Long.class);
  }

  public List<LoanUserRiskTraceRecord> findByLoanAccountIdAndStatus(Long loanAccountId, RiskFlowTraceStatusV2 traceStatusV2) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(loanAccountId))
        .and(TABLE.STATUS.eq(traceStatusV2.code))
        .orderBy(TABLE.TRACE_ID)
        .fetch();
  }

  public List<LoanUserRiskTraceRecord> fetchByRiskTypesAndCreditsStatus(Long startId, Long endId, List<LoanUserRiskType> riskTypes, LoanCreditsStatus creditsStatus) {
    List<String> riskTypeCodes = riskTypes.stream().map(o -> o.code).collect(Collectors.toList());
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.ge(startId))
        .and(TABLE.ID.le(endId))
        .and(TABLE.RISK_TYPE.in(riskTypeCodes))
        .and(TABLE.CREDITS_STATUS.eq(creditsStatus.code))
        .fetch();
  }

  public LoanUserRiskTraceRecord findLatestByUserId(Long userId) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .orderBy(TABLE.ID.desc())
        .limit(1)
        .fetchOne();
  }

  public LoanUserRiskTraceRecord findLatestRecordByAccountId(Long accountId) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(1)
        .fetchOne();
  }

  public List<LoanUserRiskTraceRecord> findByAccountIdWithPagination(Long accountId, int offset, int limit) {
    return findByAccountIdWithPagination(accountId, offset, limit, null);
  }

  /**
   * Admin 分页；{@code excludedRiskTypes} 非空时排除清单内 riskType，
   * 且 riskType 为空的行保留（US3-4）。
   */
  public List<LoanUserRiskTraceRecord> findByAccountIdWithPagination(
      Long accountId,
      int offset,
      int limit,
      Collection<String> excludedRiskTypes
  ) {
    return create()
        .selectFrom(TABLE)
        .where(buildAccountIdConditionWithOptionalRiskTypeExclusion(accountId, excludedRiskTypes))
        .orderBy(TABLE.ID.desc())
        .limit(limit)
        .offset(offset)
        .fetch();
  }

  /**
   * 取该账户早于 {@code beforeId} 的 trace，按主键倒序取 {@code limit} 条（由近及远）。
   *
   * <p>供「从某条 trace 往前逐批回溯」使用：下一批传入本批最后一条的主键即可，
   * 走游标而非 offset，每批都是索引定位后顺序读，不必扫过并丢弃前面的行。
   */
  public List<LoanUserRiskTraceRecord> findByAccountIdBeforeIdWithLimit(Long accountId, Long beforeId, int limit) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.LOAN_ACCOUNT_ID.eq(accountId))
        .and(TABLE.ID.lt(beforeId))
        .orderBy(TABLE.ID.desc())
        .limit(limit)
        .fetch();
  }

  /**
   * 构造 accountId 条件，并在隐藏清单非空时附加 riskType 排除（空 riskType 不误杀）。
   * package-visible 供单测断言 SQL 语义。
   */
  static Condition buildAccountIdConditionWithOptionalRiskTypeExclusion(
      Long accountId,
      Collection<String> excludedRiskTypes
  ) {
    Condition condition = TABLE.LOAN_ACCOUNT_ID.eq(accountId);
    if (CollectionUtils.isNotEmpty(excludedRiskTypes)) {
      condition = condition.and(
          TABLE.RISK_TYPE.isNull().or(TABLE.RISK_TYPE.notIn(excludedRiskTypes)));
    }
    return condition;
  }

}
