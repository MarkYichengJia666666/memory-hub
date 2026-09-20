package com.yqg.core.service.cashloan.repay;

import com.yqg.core.util.redislocker.AbstractRedisLocker;
import com.yqg.core.util.redislocker.YqgLockNameSpace;
import org.springframework.stereotype.Component;

/**
 * @author chenxianrui
 * @date 2025/2/7
 */
@Component
public class UnionRepaymentLocker extends AbstractRedisLocker {
  public static final String UNION_REPAYMENT = "union_repayment";

  public UnionRepaymentLocker() {
    super(String.valueOf(YqgLockNameSpace.UNION_REPAYMENT_LOCK.code), 60, 30);
  }

  public static String getLockKey(String reviewType) {
    return String.format("%s:%s", UNION_REPAYMENT, reviewType);
  }
}
