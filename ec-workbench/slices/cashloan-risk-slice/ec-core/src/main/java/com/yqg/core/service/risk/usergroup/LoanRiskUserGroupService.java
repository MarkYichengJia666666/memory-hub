package com.yqg.core.service.risk.usergroup;

import com.alibaba.excel.util.CollectionUtils;
import com.yqg.core.aop.RunInTransaction;
import com.yqg.core.model.generated.tables.records.LoanRiskUserGroupLogRecord;
import com.yqg.core.model.generated.tables.records.LoanRiskUserGroupRecord;
import com.yqg.core.model.sql.loan.account.LoanRiskUserGroupLogModel;
import com.yqg.core.model.sql.loan.account.LoanRiskUserGroupModel;
import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.usergroup.enums.UserGroupLockState;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupLogVO;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.service.risk.usergroup.vo.UserGroupChangeSnapshot;
import com.yqg.ec.common.i18n.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ⚠️⚠️⚠️非风控核心代码请不要直接使用LoanRiskUserGroupService，其他模块需要使用RiskUserGroupEntranceService 完成业务功能
 */
@Service
@Slf4j
public class LoanRiskUserGroupService {
  @Autowired
  private LoanRiskUserGroupModel loanRiskUserGroupModel;
  @Autowired
  private LoanRiskUserGroupLogModel loanRiskUserGroupLogModel;
  @Autowired
  private LoanRiskUserGroupMonitorService loanRiskUserGroupMonitorService;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private UserGroupCreditsSnapshotService userGroupCreditsSnapshotService;

  private static final Long DEFAULT_EXPIRE_TIME = -1L;

  public boolean isUserGroupExpire(LoanRiskUserGroupVO loanRiskUserGroupVO) {
    if (Objects.isNull(loanRiskUserGroupVO)) {
      return false;
    }
    if (loanRiskUserGroupVO.expireTime.equals(DEFAULT_EXPIRE_TIME)) {
      return false;
    }
    return Clock.now() > loanRiskUserGroupVO.expireTime;
  }

  public boolean isUserGroupHavingExpireTimeByAccountId(LoanRiskUserGroupVO loanRiskUserGroupVO) {
    if (Objects.isNull(loanRiskUserGroupVO)) {
      return false;
    }
    return !loanRiskUserGroupVO.expireTime.equals(DEFAULT_EXPIRE_TIME);
  }

  /**
   * 是否处于生效中的管制期（有管制期且未到期）。
   * <p>已到期的管制期视为上一轮已闭环，不再算「生效中」，可被新一轮打款赋锁覆盖。
   */
  public boolean isUserGroupInActiveControlPeriod(LoanRiskUserGroupVO loanRiskUserGroupVO) {
    return isUserGroupHavingExpireTimeByAccountId(loanRiskUserGroupVO)
        && !isUserGroupExpire(loanRiskUserGroupVO);
  }

  /**
   * 当前生效管制期是否来源于回捞/重审打款锁（结清保级判定）。
   * <p>受专用开关 {@code risk.retrieval_reapply_control_period_read_by_source_switch} 控制新旧两套读取：
   * <ul>
   *   <li>开关开且当前记录 {@code expire_time_source_log_id} 非空（历史已回刷）：直读源头 log 的 reason，
   *       源头 ∈ {首笔打款, 非首笔打款} 即保级——纳入非首笔源头，使非首笔实验组期内结清同样保级（US3-1/US3-6）；</li>
   *   <li>开关关或源头 id 为空（历史未回刷）：退回旧逻辑，按 {@code (account_id, expire_time, 首笔打款)} 反查 log。
   *       该反查依赖 expire_time（「打款时间 + 管制天数」毫秒戳）按 genesis 唯一，命中即说明源头为首笔打款。</li>
   * </ul>
   * 「回捞被拒降级重审后结清」场景下当前记录 reason 已被改写为 PRE_RISK_REJECT，两套逻辑均不受当前 reason 影响。
   */
  public boolean isCurrentControlPeriodFromRetrievalAndReapplyFirstOrderPayout(LoanRiskUserGroupVO currentVO) {
    if (Objects.isNull(currentVO) || Objects.equals(currentVO.expireTime, DEFAULT_EXPIRE_TIME)) {
      return false;
    }
    if (riskConfig.retrievalReapplyControlPeriodReadBySourceSwitch() && currentVO.expireTimeSourceLogId != null) {
      LoanRiskUserGroupLogRecord sourceLog = loanRiskUserGroupLogModel.findById(currentVO.expireTimeSourceLogId);
      if (sourceLog != null) {
        return isRetrievalAndReapplyPayoutReason(
            LoanRiskUserGroupChangeReason.valueOf(sourceLog.getReason()));
      }
    }
    return loanRiskUserGroupLogModel.findByAccountIdAndExpireTimeAndReason(
        currentVO.accountId,
        currentVO.expireTime,
        LoanRiskUserGroupChangeReason.RETRIEVAL_AND_REAPPLY_FIRST_ORDER_PAYOUT_SUCCESS) != null;
  }

  /**
   * 判定当前生效管制期的源头是否来自回捞/重审打款锁（首笔或非首笔）。
   * <p>用于「被拒降级继承」判定：优先读 {@code expire_time_source_log_id} 指向的源头 log 的 reason，
   * 使源头不因当前记录 reason 被改写（如 PRE_RISK_REJECT）而丢失（US3-3 / US5-2）；
   * 源头 id 为空（历史记录尚未回刷）时退回按当前 reason 兜底判定，兼容存量首笔锁。
   */
  public boolean isControlPeriodSourcedFromRetrievalAndReapplyPayout(LoanRiskUserGroupVO currentVO) {
    if (Objects.isNull(currentVO) || Objects.equals(currentVO.expireTime, DEFAULT_EXPIRE_TIME)) {
      return false;
    }
    if (currentVO.expireTimeSourceLogId != null) {
      LoanRiskUserGroupLogRecord sourceLog = loanRiskUserGroupLogModel.findById(currentVO.expireTimeSourceLogId);
      if (sourceLog != null) {
        return isRetrievalAndReapplyPayoutReason(
            LoanRiskUserGroupChangeReason.valueOf(sourceLog.getReason()));
      }
    }
    return isRetrievalAndReapplyPayoutReason(currentVO.reason);
  }

  private static boolean isRetrievalAndReapplyPayoutReason(LoanRiskUserGroupChangeReason reason) {
    return reason == LoanRiskUserGroupChangeReason.RETRIEVAL_AND_REAPPLY_FIRST_ORDER_PAYOUT_SUCCESS
        || reason == LoanRiskUserGroupChangeReason.RETRIEVAL_AND_REAPPLY_NON_FIRST_ORDER_PAYOUT_SUCCESS;
  }

  //找到当前等级变更历史的前一条记录
  public LoanRiskUserGroupLogVO getPreLoanRiskUserGroupLogVOByAccountIdOrNull(Long accountId) {
    List<LoanRiskUserGroupLogRecord> records = loanRiskUserGroupLogModel.findAllByAccountIdOrderByTimeCreatedDesc(accountId);
    if (CollectionUtils.isEmpty(records) || records.size() < 2) {
      return null;
    }
    return LoanRiskUserGroupLogVO.from(records.get(1));
  }

  public List<LoanRiskUserGroupLogVO> getGroupLogVOByAccountId(Long accountId) {
    List<LoanRiskUserGroupLogRecord> records = loanRiskUserGroupLogModel.findAllByAccountIdOrderByTimeCreatedDesc(accountId);
    if (CollectionUtils.isEmpty(records) || records.size() < 2) {
      return new ArrayList<>();
    }
    return records.stream()
        .map(LoanRiskUserGroupLogVO::from)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * 查询该用户在该时间戳之前的最近一条变更记录（time_created < timestamp）。
   * 用于特征平台接表 - 用户级别变更日志表查询接口。
   *
   * @return 最近一条记录，无则 null
   */
  public LoanRiskUserGroupLogVO getGroupLogVOByAccountIdAndTimeCreatedBefore(Long accountId, Long timestamp) {
    LoanRiskUserGroupLogRecord record = loanRiskUserGroupLogModel.getLastByTimeCreated(accountId, timestamp);
    return LoanRiskUserGroupLogVO.from(record);
  }

  public LoanRiskUserGroupVO getLoanRiskUserGroupVOByAccountIdOrNull(Long accountId) {
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.findByAccountId(accountId);
    return LoanRiskUserGroupVO.from(loanRiskUserGroupRecord);
  }

  public Map<Long, LoanRiskUserGroupVO> getLoanRiskUserGroupVOsByAccountIds(List<Long> accountIds) {
    return loanRiskUserGroupModel.findByAccountIds(accountIds).stream()
        .collect(Collectors.toMap(
            record -> record.getAccountId(),
            record -> LoanRiskUserGroupVO.from(record)
        ));
  }

  public LoanRiskUserGroupVO getLoanRiskUserGroupVOByAccountIdOrThrow(Long accountId) {
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.findByAccountIdOrThrow(accountId);
    return LoanRiskUserGroupVO.from(loanRiskUserGroupRecord);
  }

  @RunInTransaction
  public LoanRiskUserGroupVO updateLoanRiskUserGroupWithNoExpire(Long accountId,
                                                                 LoanRiskUserGroupEnum userGroup,
                                                                 Long traceId,
                                                                 LoanRiskUserGroupChangeReason reason) {
    return updateLoanRiskUserGroupWithNoExpire(accountId, userGroup, traceId, reason, null);
  }

  /**
   * 无管制期变更（expire_time=-1，{@code expire_time_source_log_id} 恒为 null）。
   * <p>{@code externalId} 记录本次变更的外部来源：TRACE 类 reason 传本次 traceId 字符串，NONE 类留 null。
   */
  @RunInTransaction
  public LoanRiskUserGroupVO updateLoanRiskUserGroupWithNoExpire(Long accountId,
                                                                 LoanRiskUserGroupEnum userGroup,
                                                                 Long traceId,
                                                                 LoanRiskUserGroupChangeReason reason,
                                                                 String externalId) {
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.findByAccountIdOrThrow(accountId);
    UserGroupChangeSnapshot before = buildChangeSnapshot(loanRiskUserGroupRecord);
    loanRiskUserGroupModel.update(loanRiskUserGroupRecord, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME, externalId, null);
    logAndMonitorUserGroup(loanRiskUserGroupRecord.getUserId(), accountId, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME, externalId, null, before);
    return LoanRiskUserGroupVO.from(loanRiskUserGroupRecord);
  }

  @RunInTransaction
  public void updateLoanRiskUserGroupWithExpireTime(Long accountId,
                                                    LoanRiskUserGroupEnum userGroup,
                                                    Long traceId,
                                                    LoanRiskUserGroupChangeReason reason,
                                                    Long expireTime) {
    updateLoanRiskUserGroupWithExpireTime(accountId, userGroup, traceId, reason, expireTime, null, null);
  }

  /**
   * 继承场景：降级但保留管制期时，透传管制期源头。
   * <p>{@code externalId} 记录本次变更的外部来源（被拒降级传本次 traceId），
   * {@code expireTimeSourceLogId} 透传上一条源头 log id，使源头信息不因当前 reason 被改写而丢失。
   */
  @RunInTransaction
  public void updateLoanRiskUserGroupWithExpireTime(Long accountId,
                                                    LoanRiskUserGroupEnum userGroup,
                                                    Long traceId,
                                                    LoanRiskUserGroupChangeReason reason,
                                                    Long expireTime,
                                                    String externalId,
                                                    Long expireTimeSourceLogId) {
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.findByAccountIdOrThrow(accountId);
    UserGroupChangeSnapshot before = buildChangeSnapshot(loanRiskUserGroupRecord);
    loanRiskUserGroupModel.update(loanRiskUserGroupRecord, userGroup, traceId, reason, expireTime, externalId, expireTimeSourceLogId);
    logAndMonitorUserGroup(loanRiskUserGroupRecord.getUserId(), accountId, userGroup, traceId, reason, expireTime, externalId, expireTimeSourceLogId, before);
  }

  /**
   * 诞生场景（新造管制期，非幂等）：每次都重设管制期，并把本次记录登记为管制期源头（自引用）。
   * <p>用于循环管制 / 多头被拒等每次都新造管制期的降级；{@code externalId} 传本次风控 traceId，
   * 写入拿到 logId 后回填 log 自身与主表 {@code expire_time_source_log_id = 本次 logId}。
   */
  @RunInTransaction
  public void updateLoanRiskUserGroupWithExpireTimeAsSource(Long accountId,
                                                            LoanRiskUserGroupEnum userGroup,
                                                            Long traceId,
                                                            LoanRiskUserGroupChangeReason reason,
                                                            Long expireTime,
                                                            String externalId) {
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.findByAccountIdOrThrow(accountId);
    UserGroupChangeSnapshot before = buildChangeSnapshot(loanRiskUserGroupRecord);
    loanRiskUserGroupModel.update(loanRiskUserGroupRecord, userGroup, traceId, reason, expireTime, externalId, null);
    Long sourceLogId = logAndMonitorUserGroup(loanRiskUserGroupRecord.getUserId(), accountId,
        userGroup, traceId, reason, expireTime, externalId, null, before);
    backfillGenesisSourceSelfRef(loanRiskUserGroupRecord, externalId, sourceLogId);
  }

  /**
   * 诞生场景：无生效管制期时赋予管制期（首笔/非首笔打款赋锁）。
   * <p>幂等边界为「本轮管制期」而非「历史是否锁过」：
   * <ul>
   *   <li>当前管制期仍生效（未到期）：本轮已闭环，直接返回，不续期、不写 log、不改 reason；</li>
   *   <li>无管制期或上一轮管制期已到期：赋一轮新管制期，写等级变更 log（记录新到期时间与 reason），
   *       使非首笔打款锁可被多次赋予。</li>
   * </ul>
   * <p>写入顺序保持「先主表、后 log」，拿到 logId 后回填源头自引用：
   * log 记录 {@code expire_time_source_log_id = 自身 logId}，主表 {@code expire_time_source_log_id = logId}，
   * 两表 {@code external_id} 记录本次打款订单 id（{@code String.valueOf(订单id)}）。
   */
  @RunInTransaction
  public LoanRiskUserGroupVO setUserGroupExpireTimeIfNoActiveControlPeriod(Long accountId,
                                                                          LoanRiskUserGroupChangeReason reason,
                                                                          Long expireTime,
                                                                          String externalId) {
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.findByAccountIdOrThrow(accountId);
    if (isUserGroupInActiveControlPeriod(LoanRiskUserGroupVO.from(loanRiskUserGroupRecord))) {
      return LoanRiskUserGroupVO.from(loanRiskUserGroupRecord);
    }

    UserGroupChangeSnapshot before = buildChangeSnapshot(loanRiskUserGroupRecord);
    loanRiskUserGroupModel.updateExpireTime(loanRiskUserGroupRecord, reason, expireTime, externalId, null);
    Long sourceLogId = logAndMonitorUserGroup(loanRiskUserGroupRecord.getUserId(), accountId,
        LoanRiskUserGroupEnum.valueOf(loanRiskUserGroupRecord.getUserGroup()),
        loanRiskUserGroupRecord.getTraceId(), reason, expireTime, externalId, null, before);
    backfillGenesisSourceSelfRef(loanRiskUserGroupRecord, externalId, sourceLogId);
    return LoanRiskUserGroupVO.from(loanRiskUserGroupRecord);
  }

  /**
   * 诞生场景源头自引用回填：拿到诞生 log 的 id 后，把该 log 记录自身与主表的
   * {@code expire_time_source_log_id} 均指向该 log（自引用），{@code external_id} 同步落库。
   */
  private void backfillGenesisSourceSelfRef(LoanRiskUserGroupRecord loanRiskUserGroupRecord,
                                            String externalId,
                                            Long sourceLogId) {
    loanRiskUserGroupLogModel.updateSourceFields(sourceLogId, externalId, sourceLogId);
    loanRiskUserGroupModel.updateSourceFields(loanRiskUserGroupRecord, externalId, sourceLogId);
  }

  @RunInTransaction
  public LoanRiskUserGroupVO insertWithNoExpire(Long userId,
                                                Long accountId,
                                                LoanRiskUserGroupEnum userGroup,
                                                Long traceId,
                                                LoanRiskUserGroupChangeReason reason) {
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.insert(userId, accountId, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME);
    logAndMonitorUserGroup(userId, accountId, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME, UserGroupChangeSnapshot.empty());
    return LoanRiskUserGroupVO.from(loanRiskUserGroupRecord);
  }

  /**
   * 写入用户组变更日志并触发监控打点。
   *
   * @param before 变更前状态快照，供监控还原「从哪个等级、什么锁状态变过来」
   * @return 新写入的 loan_risk_user_group_log 记录 ID
   */
  public Long logAndMonitorUserGroup(Long userId,
                                     Long accountId,
                                     LoanRiskUserGroupEnum userGroup,
                                     Long traceId,
                                     LoanRiskUserGroupChangeReason reason,
                                     Long expireTime,
                                     UserGroupChangeSnapshot before) {
    return logAndMonitorUserGroup(userId, accountId, userGroup, traceId, reason, expireTime, null, null, before);
  }

  /**
   * 写入用户组变更日志（含管制期源头字段）并触发监控打点。
   *
   * @param externalId              触发来源外部业务 id（打款类=订单 id，风控类=traceId，字符串存储）
   * @param expireTimeSourceLogId   当前 expire_time 的源头 log 记录 id（诞生场景由调用方拿到 logId 后回填）
   * @param before                  变更前状态快照，须由调用方在写库前构造（写库后原记录已被原地改写）
   * @return 新写入的 loan_risk_user_group_log 记录 ID
   */
  public Long logAndMonitorUserGroup(Long userId,
                                     Long accountId,
                                     LoanRiskUserGroupEnum userGroup,
                                     Long traceId,
                                     LoanRiskUserGroupChangeReason reason,
                                     Long expireTime,
                                     String externalId,
                                     Long expireTimeSourceLogId,
                                     UserGroupChangeSnapshot before) {
    LoanRiskUserGroupLogRecord logRecord = loanRiskUserGroupLogModel.insert(userId, accountId, userGroup, traceId, reason, expireTime, externalId, expireTimeSourceLogId);
    loanRiskUserGroupMonitorService.logUserGroupChangeRecord(userId, accountId, userGroup, reason, expireTime, before);
    return logRecord.getId();
  }

  /**
   * 构造等级变更前状态快照，供监控区分「回流主营」的来源等级、锁状态、锁源头与额度状态。
   *
   * <p>必须在调用 {@code loanRiskUserGroupModel.update/updateExpireTime} <b>之前</b>执行：
   * 两者都在同一 jOOQ record 上原地改字段后 store()，写库后已取不到变更前状态；额度失效判定同样读
   * 等级表的 {@code expire_time}，写库后拿到的是变更后状态。
   *
   * <p>额度状态仅在变更前存在管制期时采集，无锁与首次建档场景零额外查询。
   *
   * @param recordBeforeUpdate 变更前的等级记录，首次 insert 场景可为 null
   */
  private UserGroupChangeSnapshot buildChangeSnapshot(LoanRiskUserGroupRecord recordBeforeUpdate) {
    if (Objects.isNull(recordBeforeUpdate)) {
      return UserGroupChangeSnapshot.empty();
    }
    LoanRiskUserGroupEnum oldUserGroup = LoanRiskUserGroupEnum.valueOf(recordBeforeUpdate.getUserGroup());
    Long expireTimeBefore = recordBeforeUpdate.getExpireTime();
    if (Objects.isNull(expireTimeBefore) || Objects.equals(expireTimeBefore, DEFAULT_EXPIRE_TIME)) {
      return UserGroupChangeSnapshot.of(oldUserGroup, UserGroupLockState.NO_LOCK, null, DEFAULT_EXPIRE_TIME, null);
    }
    UserGroupLockState lockState = Clock.now() > expireTimeBefore
        ? UserGroupLockState.EXPIRED : UserGroupLockState.IN_LOCK;
    return UserGroupChangeSnapshot.of(oldUserGroup, lockState,
        resolveLockSourceReason(recordBeforeUpdate), expireTimeBefore,
        userGroupCreditsSnapshotService.resolveBeforeChange(recordBeforeUpdate.getAccountId()));
  }

  /**
   * 推导变更前管制期的源头变更原因：优先读 {@code expire_time_source_log_id} 指向的源头 log，
   * 源头不可达（历史记录尚未回刷）时退回变更前记录自身的 reason，与保级读取的兜底口径保持一致。
   *
   * <p>仅在确实存在管制期时调用，最多一次主键查询。
   */
  private LoanRiskUserGroupChangeReason resolveLockSourceReason(LoanRiskUserGroupRecord record) {
    if (Objects.nonNull(record.getExpireTimeSourceLogId())) {
      LoanRiskUserGroupLogRecord sourceLog = loanRiskUserGroupLogModel.findById(record.getExpireTimeSourceLogId());
      if (Objects.nonNull(sourceLog)) {
        return LoanRiskUserGroupChangeReason.valueOf(sourceLog.getReason());
      }
    }
    return LoanRiskUserGroupChangeReason.valueOf(record.getReason());
  }

  // 新增或更新用户等级
  @RunInTransaction
  public LoanRiskUserGroupVO insertOrUpdateLoanRiskUserGroupWithNoExpire(Long userId,
                                                                         Long accountId,
                                                                         LoanRiskUserGroupEnum userGroup,
                                                                         Long traceId,
                                                                         LoanRiskUserGroupChangeReason reason) {
    // 检查是否存在用户的贷款风险记录
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.findByAccountId(accountId);
    UserGroupChangeSnapshot before = buildChangeSnapshot(loanRiskUserGroupRecord);

    if (loanRiskUserGroupRecord != null) {
      loanRiskUserGroupModel.update(loanRiskUserGroupRecord, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME);
    } else {
      loanRiskUserGroupRecord = loanRiskUserGroupModel.insert(userId, accountId, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME);
    }
    logAndMonitorUserGroup(userId, accountId, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME, before);
    return LoanRiskUserGroupVO.from(loanRiskUserGroupRecord);
  }

  /**
   * Admin 批量修改用户等级专用：完成等级更新后返回新写入的 loan_risk_user_group_log 记录 ID，
   * 供调用方将其作为 operation_log 的 objId，实现审计日志与 domain log 的精确关联。
   *
   * @return 新写入的 loan_risk_user_group_log 记录 ID
   */
  @RunInTransaction
  public Long insertOrUpdateLoanRiskUserGroupWithNoExpireAndGetLogId(Long userId,
                                                                     Long accountId,
                                                                     LoanRiskUserGroupEnum userGroup,
                                                                     Long traceId,
                                                                     LoanRiskUserGroupChangeReason reason) {
    LoanRiskUserGroupRecord loanRiskUserGroupRecord = loanRiskUserGroupModel.findByAccountId(accountId);
    UserGroupChangeSnapshot before = buildChangeSnapshot(loanRiskUserGroupRecord);
    if (loanRiskUserGroupRecord != null) {
      loanRiskUserGroupModel.update(loanRiskUserGroupRecord, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME);
    } else {
      loanRiskUserGroupModel.insert(userId, accountId, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME);
    }
    return logAndMonitorUserGroup(userId, accountId, userGroup, traceId, reason, DEFAULT_EXPIRE_TIME, before);
  }

}
