package com.yqg.core.service.risk.usergroup.enums;

/**
 * 等级变更前的管制期状态，作为 {@code user_group_monitor} 打点的分组维度。
 *
 * <p>用于区分「回流主营」发生时用户是否带锁、锁是否已到期，支撑「90 天锁到期回流」与
 * 「90 天锁内回流」两类指标的拆分。
 */
public enum UserGroupLockState {

  /** 变更前无管制期（expire_time 为默认值），或无历史等级记录 */
  NO_LOCK,

  /** 变更前处于管制期内，尚未到失效时间 */
  IN_LOCK,

  /** 变更前存在管制期但已过失效时间 */
  EXPIRED,
  ;
}
