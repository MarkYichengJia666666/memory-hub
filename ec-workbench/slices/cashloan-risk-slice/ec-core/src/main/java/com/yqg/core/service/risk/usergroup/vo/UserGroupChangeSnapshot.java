package com.yqg.core.service.risk.usergroup.vo;

import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.service.cashloan.enums.CashLoanCalcCreditsStatus;
import com.yqg.core.service.cashloan.enums.CreditsExpireType;
import com.yqg.core.service.cashloan.vo.CashLoanCalcCreditsVO;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.risk.usergroup.enums.UserGroupLockState;

import java.util.Objects;

/**
 * 等级变更前的状态快照，仅供监控打点还原「这次变更是从哪来的」。
 *
 * <p>必须在写库之前构造：主表 Model 的 update 是在同一 jOOQ record 上原地改字段后 store()，
 * 写库后已无法取回变更前状态。
 */
public class UserGroupChangeSnapshot {

  private static final Long NO_EXPIRE_TIME = -1L;

  private static final UserGroupChangeSnapshot EMPTY = new UserGroupChangeSnapshot(
      null, UserGroupLockState.NO_LOCK, null, NO_EXPIRE_TIME, null);

  /** 变更前等级；无历史等级记录（首次 insert）时为 null */
  public final LoanRiskUserGroupEnum oldUserGroup;

  /** 变更前管制期状态 */
  public final UserGroupLockState lockStateBefore;

  /**
   * 变更前管制期的源头变更原因；变更前无管制期时为 null。
   *
   * <p>取值口径与保级判定一致：优先取 {@code expire_time_source_log_id} 指向的源头 log 的 reason，
   * 源头不可达（历史记录尚未回刷）时退回变更前记录自身的 reason。
   */
  public final LoanRiskUserGroupChangeReason lockSourceReasonBefore;

  /** 变更前失效时间，无管制期时为默认值 */
  public final Long expireTimeBefore;

  /**
   * 变更前额度状态；变更前无管制期（不采集）或采集失败时为 null。
   *
   * <p>仅在变更前存在管制期时采集：无锁场景的额度归因对 90 天锁观测没有意义，省下的是等级变更事务内的
   * 一整次额度状态计算（涉及订单、风控 trace、授信多表查询）。
   */
  public final CashLoanCalcCreditsStatus calcCreditsStatusBefore;

  /** 变更前额度失效归因；额度未失效、未采集或采集失败时为 null */
  public final CreditsExpireType creditsExpireTypeBefore;

  private UserGroupChangeSnapshot(LoanRiskUserGroupEnum oldUserGroup,
                                  UserGroupLockState lockStateBefore,
                                  LoanRiskUserGroupChangeReason lockSourceReasonBefore,
                                  Long expireTimeBefore,
                                  CashLoanCalcCreditsVO creditsVOBefore) {
    this.oldUserGroup = oldUserGroup;
    this.lockStateBefore = lockStateBefore;
    this.lockSourceReasonBefore = lockSourceReasonBefore;
    this.expireTimeBefore = expireTimeBefore;
    this.calcCreditsStatusBefore = Objects.isNull(creditsVOBefore) ? null : creditsVOBefore.calcCreditsStatus;
    this.creditsExpireTypeBefore = Objects.isNull(creditsVOBefore) ? null : creditsVOBefore.creditsExpireType;
  }

  /**
   * @param creditsVOBefore 变更前额度状态，未采集或采集失败时传 null
   */
  public static UserGroupChangeSnapshot of(LoanRiskUserGroupEnum oldUserGroup,
                                           UserGroupLockState lockStateBefore,
                                           LoanRiskUserGroupChangeReason lockSourceReasonBefore,
                                           Long expireTimeBefore,
                                           CashLoanCalcCreditsVO creditsVOBefore) {
    return new UserGroupChangeSnapshot(oldUserGroup, lockStateBefore, lockSourceReasonBefore,
        expireTimeBefore, creditsVOBefore);
  }

  /** 无变更前状态（首次 insert，或调用方拿不到历史记录） */
  public static UserGroupChangeSnapshot empty() {
    return EMPTY;
  }
}
