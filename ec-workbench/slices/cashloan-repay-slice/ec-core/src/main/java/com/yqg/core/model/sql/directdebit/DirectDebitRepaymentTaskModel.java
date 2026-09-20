package com.yqg.core.model.sql.directdebit;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.DirectDebitRepaymentTask;
import com.yqg.core.model.generated.tables.records.DirectDebitRepaymentTaskRecord;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitRepaymentTaskCompleteReason;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitRepaymentTaskStatus;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitRepaymentTaskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author chaoye
 * @date 2024/9/9
 */
@Repository
public class DirectDebitRepaymentTaskModel extends YqgBaseModel {

  public static final DirectDebitRepaymentTask TABLE = Tables.DIRECT_DEBIT_REPAYMENT_TASK;

  public DirectDebitRepaymentTaskRecord insert(
      DirectDebitRepaymentTaskType taskType,
      Long userId,
      Long linkAccountId,
      DirectDebitRepaymentTaskStatus status,
      BigDecimal expectAmount,
      BigDecimal actualAmount,
      Long timeCreated,
      Long timeUpdated
  ) {

    DirectDebitRepaymentTaskRecord record = create().newRecord(TABLE);
    record.setTaskType(taskType.name());
    record.setUserId(userId);
    record.setLinkAccountId(linkAccountId);
    record.setExpectAmount(expectAmount);
    record.setActualAmount(actualAmount);
    record.setStatus(status.name());
    record.setTimeCreated(timeCreated);
    record.setTimeUpdated(timeUpdated);
    record.store();
    return record;
  }

  public DirectDebitRepaymentTaskRecord findByUserIdAndTimeCreated(Long userId, Long startTime, Long endTime) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.TIME_CREATED.between(startTime, endTime))
        .and(TABLE.USER_ID.eq(userId))
        .fetchOne();
  }

  public List<DirectDebitRepaymentTaskRecord> fetchByStatusAndLimitAndMinIdOrderByIdAsc(DirectDebitRepaymentTaskStatus status, Long minId, int limit) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.STATUS.eq(status.name()))
        .and(TABLE.ID.greaterThan(minId))
        .orderBy(TABLE.ID.asc())
        .limit(limit)
        .fetch();
  }

  public DirectDebitRepaymentTaskRecord findByIdOrThrow(Long id) {
    DirectDebitRepaymentTaskRecord record = create()
        .selectFrom(TABLE)
        .where(TABLE.ID.eq(id))
        .fetchOne();
    if (record == null) {
      throw EcException.error("DirectDebitRepaymentTaskRecord is null, id :{}", id);
    }
    return record;
  }

  public DirectDebitRepaymentTaskRecord findByIdForUpdate(Long id) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.eq(id))
        .forUpdate()
        .fetchOne();
  }

  public DirectDebitRepaymentTaskRecord updateStatus(DirectDebitRepaymentTaskRecord record, DirectDebitRepaymentTaskStatus status, DirectDebitRepaymentTaskCompleteReason reason) {
    record.setStatus(status.name());
    record.setReason(reason.name());
    record.setTimeUpdated(Clock.now());
    record.update();
    return record;
  }

  public DirectDebitRepaymentTaskRecord completeTask(DirectDebitRepaymentTaskRecord record, BigDecimal amount) {
    record.setActualAmount(amount);
    record.setTimeUpdated(Clock.now());
    record.setStatus(DirectDebitRepaymentTaskStatus.COMPLETE.name());
    record.setReason(DirectDebitRepaymentTaskCompleteReason.DEDUCT_SUCCESS.name());
    record.update();
    return record;
  }

  /**
   * 可查余额重试的代扣金额更新
   * @param record
   * @param amount
   * @return
   */
  public DirectDebitRepaymentTaskRecord updateActualAmountWithBalanceRetry(DirectDebitRepaymentTaskRecord record, BigDecimal amount) {
    if(amount.compareTo(record.getExpectAmount()) == 0) {
      return completeTask(record, amount);
    }
    record.setActualAmount(amount);
    record.setTimeUpdated(Clock.now());
    record.update();
    return record;
  }

  public List<DirectDebitRepaymentTaskRecord> findByUserIdAndStatus(Long userId, DirectDebitRepaymentTaskStatus status) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.STATUS.eq(status.name()))
        .and(TABLE.USER_ID.eq(userId))
        .fetch();
  }
}
