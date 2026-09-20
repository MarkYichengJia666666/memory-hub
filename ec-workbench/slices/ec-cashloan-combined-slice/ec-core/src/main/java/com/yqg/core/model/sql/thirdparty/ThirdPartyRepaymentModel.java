package com.yqg.core.model.sql.thirdparty;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.ThirdpartyRepayment;
import com.yqg.core.model.generated.tables.records.ThirdpartyRepaymentRecord;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.model.sql.thirdparty.enums.ReasonCode;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.i18n.time.Clock;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Created by xiuqichenyang on 17/7/27.
 */
@Repository
public class ThirdPartyRepaymentModel extends YqgBaseModel {

  private static final ThirdpartyRepayment TABLE = Tables.THIRDPARTY_REPAYMENT;

  public ThirdpartyRepaymentRecord insert(
      Long userId, PaymentBusinessName businessName, PaymentProvider provider, Long thirdPartyPaymentId, String paymentTransId) {
    ThirdpartyRepaymentRecord record = create().newRecord(TABLE);
    record.setUserId(userId);
    if (businessName != null) {
      record.setBusinessName(businessName.code);
    }
    record.setPaymentProvider(provider.getCode());
    record.setThirdpartyPaymentId(thirdPartyPaymentId);
    record.setStatus(ProcessStatus.UNPROCESSED.code);
    record.setPaymentTransId(paymentTransId);
    Long current = Clock.now();
    record.setTimeCreated(current);
    record.setTimeUpdated(current);
    record.store();
    return record;
  }

  public List<ThirdpartyRepaymentRecord> fetchByUserIdAndBusinessAndStatus(Long userId, PaymentBusinessName businessName, ProcessStatus status) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .and(TABLE.STATUS.eq(status.code))
        .fetch();
  }

  public List<ThirdpartyRepaymentRecord> fetchUnprocessedRecords(Long endTimeStamp) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.STATUS.eq(ProcessStatus.UNPROCESSED.code))
        .and(TABLE.TIME_CREATED.le(endTimeStamp))
        .fetch();
  }

  public void updateStatus(ThirdpartyRepaymentRecord record, ProcessStatus status, ReasonCode reasonCode) {
    record.setStatus(status.code);
    if (reasonCode != null) {
      record.setReasonCode(reasonCode.code);
    }
    record.setTimeUpdated(Clock.now());
    record.update();
  }

  public List<ThirdpartyRepaymentRecord> fetch(List<String> transIds) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.PAYMENT_TRANS_ID.in(transIds))
        .fetch();
  }

  public ThirdpartyRepaymentRecord fetchByTransId(String transNo) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.PAYMENT_TRANS_ID.eq(transNo))
        .fetchOne();
  }

  public List<Long> findExpiredProcessedIds(long cutoffTime, long minId, int limit) {
    return create()
        .select(TABLE.ID)
        .from(TABLE)
        .where(TABLE.ID.gt(minId))
        .and(TABLE.STATUS.eq(ProcessStatus.PROCESSED.code))
        .and(TABLE.TIME_UPDATED.lt(cutoffTime))
        .orderBy(TABLE.ID.asc())
        .limit(limit)
        .fetchInto(Long.class);
  }

  public int deleteProcessedByIds(Collection<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return 0;
    }
    return create()
        .deleteFrom(TABLE)
        .where(TABLE.ID.in(ids))
        .and(TABLE.STATUS.eq(ProcessStatus.PROCESSED.code))
        .execute();
  }

}
