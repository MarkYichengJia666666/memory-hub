package com.yqg.core.userflow.infrastructure.repository.config.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 印尼 817 国庆端内氛围（TAPD-1371279）配置 VO。
 * <p>对应配置 key {@code cash_loan.national_day_config}，JSON 示例：
 * {@code {"enabled":false,"activity_start":"2026-08-15 00:00:00","activity_end":"2026-08-30 23:59:59"}}
 * <p>起止时间为**印尼时间**（Asia/Jakarta）闭区间，格式 {@code yyyy-MM-dd HH:mm:ss}。
 */
@Data
public class NationalDay817ConfigVO {

  private static final String DEFAULT_ACTIVITY_START = "2026-08-15 00:00:00";
  private static final String DEFAULT_ACTIVITY_END = "2026-08-30 23:59:59";

  /** 功能总开关，默认关闭：H5 素材就绪后再打开 */
  private boolean enabled;

  /** 活动开始时间（印尼时间，含） */
  @JsonProperty("activity_start")
  private String activityStart = DEFAULT_ACTIVITY_START;

  /** 活动结束时间（印尼时间，含） */
  @JsonProperty("activity_end")
  private String activityEnd = DEFAULT_ACTIVITY_END;

  public static NationalDay817ConfigVO defaultConfig() {
    return new NationalDay817ConfigVO();
  }
}
