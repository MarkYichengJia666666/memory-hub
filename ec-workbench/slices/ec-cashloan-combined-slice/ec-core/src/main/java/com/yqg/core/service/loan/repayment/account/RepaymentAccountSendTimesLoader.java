package com.yqg.core.service.loan.repayment.account;

import com.yqg.core.util.cache.YqgCacheNamespace;
import com.yqg.core.util.cache.redis.AbstractRedisCache;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

@Component
public class RepaymentAccountSendTimesLoader extends AbstractRedisCache<Integer> {

  public RepaymentAccountSendTimesLoader() {
    super(YqgCacheNamespace.REPAYMENT_ACCOUNT_SEND_TIMES);
  }

  public void incrOneDayTimes(Long orderId) {
    String key = generateKey(orderId);
    int expirationInSeconds = 60 * 60 * 24;
    incrAndExpire(key, expirationInSeconds);
  }

  public Integer getOneDayTimes(Long orderId) {
    String key = generateKey(orderId);
    return super.get(key);
  }

  private String generateKey(Long orderId) {
    String nowDate = Clock.dateTimeStringFromTimestamp(Clock.now(), "dd", TimeZone.getDefault());
    return orderId + "_" + nowDate + "_ONE_DAY";
  }

}
