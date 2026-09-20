package com.yqg.core.model.sql.bankaccount;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.LoanBankAccountDelayUserLog;
import com.yqg.core.model.generated.tables.records.LoanBankAccountDelayUserLogRecord;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Repository;

/**
 * Model class for loan_bank_account_delay_user_log table
 * 绑卡后置用户日志表
 */
@Repository
public class LoanBankAccountDelayUserLogModel extends YqgBaseModel {
  private static final LoanBankAccountDelayUserLog TABLE = Tables.LOAN_BANK_ACCOUNT_DELAY_USER_LOG;

  /**
   * Find record by user ID
   *
   * @param userId user ID
   * @return record or null if not found
   */
  public LoanBankAccountDelayUserLogRecord findByUserId(Long userId) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .fetchOne();
  }

  /**
   * Initialize a new record
   *
   * @param userId user ID
   * @return the created record
   */
  public int insertOrIgnore(Long userId, String experimentRes) {
    LoanBankAccountDelayUserLogRecord record = create().newRecord(TABLE);
    record.setUserId(userId);
    record.setExperimentRes(experimentRes);
    record.setTimeCreated(Clock.now());
    record.setTimeUpdated(Clock.now());
    return create()
        .insertInto(TABLE)
        .set(record)
        .onDuplicateKeyIgnore()
        .execute();
  }

  /**
   * Update binding time for a user
   *
   * @param userId   user ID
   * @param timeBind new binding time
   * @return number of affected rows
   */
  public int updateTimeBind(Long userId, Long timeBind) {
    return create()
        .update(TABLE)
        .set(TABLE.TIME_BIND, timeBind)
        .set(TABLE.TIME_UPDATED, Clock.now())
        .where(TABLE.USER_ID.eq(userId))
        .execute();
  }

}