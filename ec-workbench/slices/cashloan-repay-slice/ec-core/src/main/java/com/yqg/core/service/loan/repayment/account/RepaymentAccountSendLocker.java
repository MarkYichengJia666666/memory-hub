package com.yqg.core.service.loan.repayment.account;

import com.yqg.core.util.redislocker.AbstractRedisLocker;
import com.yqg.core.util.redislocker.YqgLockNameSpace;
import org.springframework.stereotype.Component;

@Component
public class RepaymentAccountSendLocker extends AbstractRedisLocker {
  public RepaymentAccountSendLocker() {
    super(String.valueOf(YqgLockNameSpace.REPAYMENT_ACCOUNT.code), 60, 10);
  }
}