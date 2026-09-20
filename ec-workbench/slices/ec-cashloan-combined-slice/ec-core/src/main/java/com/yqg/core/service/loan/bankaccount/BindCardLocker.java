package com.yqg.core.service.loan.bankaccount;

import com.yqg.core.util.redislocker.AbstractRedisLocker;
import com.yqg.core.util.redislocker.YqgLockNameSpace;
import org.springframework.stereotype.Component;

/**
 * Created by YZY on 17/4/11.
 */
@Component
public class BindCardLocker extends AbstractRedisLocker {
  public BindCardLocker() {
    super(String.valueOf(YqgLockNameSpace.BIND_CARD.code), 3 * 60, 60);
  }
}
