package com.yqg.core.userflow.infrastructure.repository.config;

import com.yqg.core.userflow.infrastructure.repository.config.vo.NationalDay817ConfigVO;
import com.yqg.ec.common.configuration.ISiteVars;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.ec.common.i18n.time.EcTimeZone;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 印尼 817 国庆端内氛围（TAPD-1371279）配置。
 * <p>
 * 总开关与活动期合并为一个 JSON 配置 {@code cash_loan.national_day_config}，结构见
 * {@link NationalDay817ConfigVO}。下单页素材随 H5 上线、首页 banner 素材是营销中台下发的 URL，
 * 两侧都不依赖 app 版本，因此**不设 build 门控**；「前端素材未就绪就开始入组」的时序问题由总开关承担：
 * H5 上线后再打开开关。
 * <p>
 * 活动期配置值一律按**印尼时间**（{@link EcTimeZone#JAKARTA}）解析，不随 JVM 默认时区
 * （{@code Asia/Shanghai}）漂移；判定统一走 epoch 毫秒比较。
 */
@Service
@Slf4j
public class NationalDay817Config {

  /** 国庆氛围配置（JSON 串），未配置时按默认值（关闭 + 8/15~8/30）处理 */
  private static final String NATIONAL_DAY_CONFIG = "cash_loan.national_day_config";

  @Autowired
  private ISiteVars ecSiteVars;

  /**
   * 国庆氛围是否开启。未配置时默认关闭，避免素材未就绪时提前入组污染实验样本。
   */
  public boolean isEnabled() {
    return getConfig().isEnabled();
  }

  /**
   * 给定时刻（epoch 毫秒）是否落在活动期内（印尼时间闭区间）。配置格式非法时按不在活动期处理。
   */
  public boolean inActivityPeriod(long nowMillis) {
    NationalDay817ConfigVO config = getConfig();
    Long start = parseOrNull(config.getActivityStart());
    Long end = parseOrNull(config.getActivityEnd());
    if (start == null || end == null) {
      return false;
    }
    return nowMillis >= start && nowMillis <= end;
  }

  public NationalDay817ConfigVO getConfig() {
    return parseConfig(ecSiteVars.getString(NATIONAL_DAY_CONFIG, ""));
  }

  NationalDay817ConfigVO parseConfig(String configStr) {
    if (StringUtils.isBlank(configStr)) {
      return NationalDay817ConfigVO.defaultConfig();
    }
    try {
      NationalDay817ConfigVO parsed = JsonUtils.from(configStr, NationalDay817ConfigVO.class);
      if (parsed == null) {
        log.error("[NationalDay817] parseConfig parsed null, configStr={}", configStr);
        return NationalDay817ConfigVO.defaultConfig();
      }
      return parsed;
    } catch (Exception e) {
      log.error("[NationalDay817] parseConfig error, configStr={}", configStr, e);
      return NationalDay817ConfigVO.defaultConfig();
    }
  }

  /**
   * 把印尼时间字符串解析为 epoch 毫秒；空串 / 格式非法返回 null（{@code Clock.dateStringToLong}
   * 对空串返回 0，需按非法处理，否则会被当成 1970 年从而永远落在活动期内）。
   */
  private Long parseOrNull(String value) {
    try {
      long millis = Clock.dateStringToLong(value, DateFormatter.yyyy_MM_dd___HH_mm_ss, EcTimeZone.JAKARTA.tz);
      if (millis <= 0L) {
        log.warn("[NationalDay817] blank activity period config, value={}", value);
        return null;
      }
      return millis;
    } catch (Exception e) {
      log.warn("[NationalDay817] illegal activity period config, value={}", value, e);
      return null;
    }
  }
}
