package com.yqg.core.model.sql.bankaccount;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.BindCardRequest;
import com.yqg.core.model.generated.tables.records.BindCardRequestRecord;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.i18n.time.Clock;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Created by jiewu on 19/08/22.
 */
@Repository
public class BindCardRequestModel extends YqgBaseModel {
  private static final BindCardRequest TABLE = Tables.BIND_CARD_REQUEST;

  public Pair<Integer, Long> init(Long userId,
                                  String accountNumber,
                                  BankType bankType,
                                  SDKType sdkType) {
    BindCardRequestRecord record = create().newRecord(TABLE);
    record.setUserId(userId);
    record.setAccountNumber(accountNumber);
    record.setBankCode(bankType.name());
    record.setSdkType(sdkType.code);
    record.setTimeCreated(Clock.now());
    int execution = create().insertInto(TABLE)
        .set(record)
        .onDuplicateKeyIgnore()
        .execute();
    return Pair.of(execution, record.getTimeCreated());
  }

  public List<BindCardRequestRecord> batchFindBindCardRequestRecord(long index, int limit) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.gt(index)) // 使用 ID 字段进行分页查询
        .orderBy(TABLE.ID.asc()) // 按 ID 升序排序
        .limit(limit)
        .fetch();
  }

  public List<BindCardRequestRecord> findByUserId(Long userId, SDKType sdkType, Long endTime) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.TIME_CREATED.le(endTime))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .fetch();
  }

  public List<BindCardRequestRecord> findByAccountNumbers(Collection<String> accountNumbers, Long endTime) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.in(accountNumbers))
        .and(TABLE.TIME_CREATED.le(endTime))
        .fetch();
  }

  public List<BindCardRequestRecord> findByAccountNumbersIdn(Collection<String> accountNumbers, Long endTime, List<SDKType> sdkTypeList) {
    List<String> sdkTypeCodes = SDKType.listSDKTypeToCodes(sdkTypeList);
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.in(accountNumbers))
        .and(TABLE.TIME_CREATED.le(endTime))
        .and(TABLE.SDK_TYPE.in(sdkTypeCodes))
        .fetch();
  }
}
