package com.yqg.core.service.loan.collision;

import com.yqg.core.util.redislocker.AbstractRedisLocker;
import com.yqg.core.util.redislocker.YqgLockNameSpace;
import org.springframework.stereotype.Component;

/**
 * @author chenxianrui
 * @date 2025/2/7
 */
@Component
public class CheckExistUserLocker extends AbstractRedisLocker {
  public static final String CHECK_EXIST_USER_LOCK_KEY = "check_exist_user_lock_key";
  public CheckExistUserLocker() {
    super(String.valueOf(YqgLockNameSpace.CHECK_EXIST_USER_LOCKER.code), 1, 1);
  }

  public static String getLockKey(String phoneMd5) {
    return String.format("%s:%s", CHECK_EXIST_USER_LOCK_KEY, phoneMd5);
  }
}
