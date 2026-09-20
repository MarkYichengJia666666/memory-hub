package com.yqg.core.service.apichannel;

import com.yqg.core.model.loader.ApiMarketingChannelDailyCheckTimesLoader;
import com.yqg.core.service.loan.collision.vo.CheckFailedUserVO;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 精准营销渠道每日撞库配额门禁。
 *
 * <p>配额按送检手机号数量累计（本批 {@code phoneMd5List.size()}）。语义为「先读后写」：
 * 只有被放行的请求才会消耗配额，被拒请求不计数，因此运营调高阈值后剩余配额立即可用。
 * 是否抛异常由调用方（接入层）决定。
 */
@Service
@Slf4j
public class ApiMarketingCollisionLimitService {

  /** 超限时写入 DW 的失败原因，数仓按此值统计被拒量 */
  public static final String CHANNEL_DAILY_LIMIT_EXCEEDED_REASON = "超过渠道每日撞库上限";

  @Autowired
  private ApiMarketingConfig apiMarketingConfig;
  @Autowired
  private ApiMarketingChannelDailyCheckTimesLoader dailyCheckTimesLoader;

  /**
   * 尝试占用该渠道当日撞库配额；已达上限时逐号打 DW 记录失败原因。
   *
   * @param apiChannel   渠道标识
   * @param phoneMd5List 本次请求的手机号 md5 列表；放行时按 size 累加配额，超限时按号打 DW
   * @return true 放行（未配置上限 / 未达上限 / Redis 异常降级）；false 已达上限，调用方须整批拒绝
   */
  public boolean tryAcquireDailyQuota(String apiChannel, Set<String> phoneMd5List) {
    Integer limit = apiMarketingConfig.getChannelDailyCheckTimesLimit(apiChannel);
    if (limit == null) {
      return true;
    }
    int used = dailyCheckTimesLoader.getTodayTimes(apiChannel);
    if (used >= limit) {
      log.info("api marketing collision daily limit exceeded, channel: {}, used: {}, limit: {}", apiChannel, used, limit);
      logLimitExceeded(apiChannel, phoneMd5List);
      return false;
    }
    int delta = phoneMd5List == null ? 0 : phoneMd5List.size();
    dailyCheckTimesLoader.incrTodayTimes(apiChannel, delta);
    return true;
  }

  private void logLimitExceeded(String apiChannel, Set<String> phoneMd5List) {
    if (phoneMd5List == null) {
      return;
    }
    for (String phoneMd5 : phoneMd5List) {
      DwLogUtil.newLog(LogBusinessType.CHECK_EXIST_USER_FAILED_LOG,
          CheckFailedUserVO.from(phoneMd5, apiChannel, CHANNEL_DAILY_LIMIT_EXCEEDED_REASON));
    }
  }
}
