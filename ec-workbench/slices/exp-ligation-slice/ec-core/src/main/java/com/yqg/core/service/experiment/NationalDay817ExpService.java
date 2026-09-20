package com.yqg.core.service.experiment;

import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.userflow.domain.user.service.IUserInfoService;
import com.yqg.core.userflow.infrastructure.adapter.IExperimentAdapter;
import com.yqg.core.userflow.infrastructure.repository.config.NationalDay817Config;
import com.yqg.core.userflow.infrastructure.utils.RequestSourceUtil;
import com.yqg.ec.common.i18n.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 印尼 817 国庆端内氛围实验（TAPD-1371279）。
 * <p>
 * 两条泳道按客群独立分流：首贷走 {@link UserFlowExperimentEnum#NATIONAL_DAY_817_FIRST_LOAN}，
 * 复贷走 {@link UserFlowExperimentEnum#NATIONAL_DAY_817_RELOAN}。本服务是唯一入组点
 * （{@code abTestByUserId}），命中实验组才下发国庆氛围。
 * <p>
 * 渠道 / 开关 / 活动期门槛一律短路在入组之前（{@link #passesGate}），避免污染实验样本；
 * 分流失败（适配器返回空串）或异常一律按未命中处理：异常不外抛、不阻断首页与下单页，氛围默认不加。
 * <p>
 * 非防结清场景，不接长期 holdout。
 */
@Service
@Slf4j
public class NationalDay817ExpService {

  /** 下发给前端的样式标记键：命中即置 "true"，前端据此渲染国庆氛围 */
  public static final String MARK_ATMOSPHERE = "nationalDay817Atmosphere";

  @Autowired
  private NationalDay817Config nationalDay817Config;
  @Autowired
  private IExperimentAdapter experimentAdapter;
  @Autowired
  private IUserInfoService userInfoService;

  /**
   * 入组前置门槛：api 渠道与 H5 全流程不入组，且需总开关开启并落在活动期内。
   * <p>
   * 渠道判定走 {@link RequestSourceUtil}（读请求 Scope）。资源位链路（{@code POST /ecInternalApi/checkStrategy}）
   * 在请求线程与策略执行线程都显式补写了 sourceType / requestClientType 到 Scope，故此处能读到真实来源。
   */
  public boolean passesGate(Long userId) {
    if (userId == null) {
      return false;
    }
    if (RequestSourceUtil.isApiChannel() || RequestSourceUtil.isH5WholeProcess()) {
      return false;
    }
    // 活动期按印尼时间判定（口径见 NationalDay817Config），Clock.now() 取 epoch 毫秒
    return nationalDay817Config.isEnabled() && nationalDay817Config.inActivityPeriod(Clock.now());
  }

  /**
   * 是否命中国庆氛围实验组。内部先过 {@link #passesGate}，通过后按客群选泳道入组。
   */
  public boolean isAtmosphereGroup(Long userId) {
    if (!passesGate(userId)) {
      return false;
    }
    try {
      boolean reloan = userInfoService.isReloanUserByUserId(userId);
      String expKey = reloan
          ? UserFlowExperimentEnum.NATIONAL_DAY_817_RELOAN.getKey()
          : UserFlowExperimentEnum.NATIONAL_DAY_817_FIRST_LOAN.getKey();
      String group = experimentAdapter.abTestByUserId(expKey, userId);
      // 本实验只有单一实验组，精确匹配 EXPERIMENT_GROUP；
      // 不用 UserFlowConstants.isExperimentGroup（它会把 _ONE/_TWO/_THREE 多实验组一并算命中）
      boolean hit = UserFlowConstants.EXPERIMENT_GROUP.equals(group);
      log.info("[NationalDay817] userId={}, reloan={}, expKey={}, group={}, hit={}",
          userId, reloan, expKey, group, hit);
      return hit;
    } catch (Exception e) {
      log.warn("[NationalDay817] diversion failed, treat as not hit, userId={}", userId, e);
      return false;
    }
  }
}
