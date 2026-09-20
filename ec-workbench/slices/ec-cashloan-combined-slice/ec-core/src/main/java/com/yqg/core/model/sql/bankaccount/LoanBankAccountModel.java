package com.yqg.core.model.sql.bankaccount;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.LoanBankAccount;
import com.yqg.core.model.generated.tables.records.BankConfigRecord;
import com.yqg.core.model.generated.tables.records.LoanBankAccountRecord;
import com.yqg.core.model.sql.bankaccount.enums.BankAccountAvailableStatus;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.loan.account.enums.BankAccountType;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.core.service.payment.PaymentBusinessNameMapper;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xiahonggao on 2/16/16.
 */
@Repository
public class LoanBankAccountModel extends YqgBaseModel {

  public static final LoanBankAccount TABLE = Tables.LOAN_BANK_ACCOUNT;

  public LoanBankAccountRecord findById(Long id) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.equal(id))
        .fetchOne();
  }

  public LoanBankAccountRecord findByIdOrThrow(Long id) {
    LoanBankAccountRecord record = findById(id);
    if (record == null) {
      throw EcException.error(EcExceptionType.LOAN_BANK_ACCOUNT_NOT_FOUND, "id=" + id);
    }
    return record;
  }

  public LoanBankAccountRecord findByIdForUpdateOrThrow(Long id) {
    LoanBankAccountRecord record = create()
        .selectFrom(TABLE)
        .where(TABLE.ID.equal(id))
        .forUpdate()
        .fetchOne();
    if (record == null) {
      throw EcException.error(EcExceptionType.LOAN_BANK_ACCOUNT_NOT_FOUND, "id=" + id);
    }
    return record;
  }

  public List<LoanBankAccountRecord> findUserIdByBankCodeAndBankAccountNoLimit1000(String bankCode, String accountNumber) {
    return create().selectFrom(TABLE)
        .where(TABLE.BANK_CODE.eq(bankCode))
        .and(TABLE.ACCOUNT_NUMBER.eq(accountNumber))
        .limit(1000)
        .fetch();
  }

  public LoanBankAccountVO findVOById(Long id) {
    Record record = create()
        .select()
        .from(TABLE)
        .join(Tables.BANK_CONFIG)
        .on(TABLE.BANK_CODE.equal(Tables.BANK_CONFIG.BANK_CODE))
        .where(TABLE.ID.eq(id))
        .fetchOne();

    if (record == null) {
      return null;
    }

    LoanBankAccountRecord bankAccountRecord = record.into(TABLE);
    BankConfigRecord bankConfigRecord = record.into(Tables.BANK_CONFIG);

    return LoanBankAccountVO.from(bankAccountRecord, bankConfigRecord);
  }

  public LoanBankAccountVO findVOByIdOrThrow(Long id) {
    LoanBankAccountVO bankAccountVO = findVOById(id);
    if (bankAccountVO == null) {
      throw EcException.error(EcExceptionType.LOAN_BANK_ACCOUNT_NOT_FOUND, "id=" + id);
    }
    return bankAccountVO;
  }

  public List<LoanBankAccountRecord> findByUserIdAndBusiness(Long userId, PaymentBusinessName businessName, Long timeStarted, Long timeEnded) {
    Condition finalCondition = DSL.trueCondition();
    if (timeStarted != null) {
      finalCondition = finalCondition.and(TABLE.TIME_CREATED.greaterOrEqual(timeStarted));
    }
    if (timeEnded != null) {
      finalCondition = finalCondition.and(TABLE.TIME_CREATED.lessOrEqual(timeEnded));
    }
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.equal(userId))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .and(finalCondition)
        .orderBy(TABLE.LAST_TIME_USED.desc())
        .fetch();
  }

  public List<LoanBankAccountRecord> findAvailableByUserIdAndBusiness(Long userId, PaymentBusinessName businessName) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.equal(userId))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .and(TABLE.AVAILABLE_STATUS.equal(BankAccountAvailableStatus.AVAILABLE.charCode))
        .orderBy(TABLE.LAST_TIME_USED.desc())
        .fetch();
  }

  public Integer countAvailableByUserIdAndBusiness(Long userId, PaymentBusinessName businessName) {
    return create()
        .selectCount()
        .from(TABLE)
        .where(TABLE.USER_ID.equal(userId))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .and(TABLE.AVAILABLE_STATUS.equal(BankAccountAvailableStatus.AVAILABLE.charCode))
        .fetchOneInto(Integer.class);
  }

  public Integer countByAccountNumber(BankType bankType, String accountNumber) {
    return create()
        .selectCount()
        .from(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.eq(accountNumber))
        .and(bankType.isEWallet ? TABLE.BANK_CODE.eq(bankType.name()) : DSL.trueCondition())
        .fetchOne()
        .into(Integer.class);
  }

  public Integer countByAccountNumberAndBankCode(BankType bankType, String accountNumber) {
    return create()
        .selectCount()
        .from(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.eq(accountNumber))
        .and(TABLE.BANK_CODE.eq(bankType.name()))
        .fetchOne()
        .into(Integer.class);
  }

  public LoanBankAccountRecord initAllowMultipleBindCard(
      Long userId,
      SDKType sdkType,
      BankType bankType,
      BankAccountType bankAccountType,
      String accountNumber,
      String name,
      BankAccountAvailableStatus status,
      String validationId
  ) {

    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    LoanBankAccountRecord record = create().newRecord(TABLE);
    record.setUserId(userId);
    record.setSdkType(sdkType.code);
    record.setBusinessName(businessName.code);
    record.setBankCode(bankType.name());
    record.setBankAccountType(bankAccountType == null ? null : bankAccountType.name());
    record.setAccountNumber(accountNumber);
    record.setName(name);
    record.setAvailableStatus(status.charCode);
    record.setValidationId(validationId);
    record.setTimeCreated(Clock.now());
    record.setTimeUpdated(Clock.now());
    record.setLastTimeUsed(Clock.now());
    record.store();
    return record;
  }

  public LoanBankAccountRecord updateName(LoanBankAccountRecord record, String name) {
    record.setName(name);
    record.setTimeUpdated(Clock.now());
    record.update();
    return record;
  }

  public void updateLastTimeUsed(LoanBankAccountRecord record, Long lastTimeUsed) {
    record.setLastTimeUsed(lastTimeUsed);
    record.setTimeUpdated(Clock.now());
    record.update();
  }

  public LoanBankAccountRecord updateAvailableStatus(LoanBankAccountRecord record, BankAccountAvailableStatus status) {
    record.setAvailableStatus(status.charCode);
    record.setTimeUpdated(Clock.now());
    record.update();
    return record;
  }

  public void updateAvailableStatus(Long accountId, BankAccountAvailableStatus status) {
    create()
        .update(TABLE)
        .set(TABLE.AVAILABLE_STATUS, status.charCode)
        .set(TABLE.TIME_UPDATED, Clock.now())
        .where(TABLE.ID.eq(accountId))
        .execute();
  }

  public LoanBankAccountRecord updateByValidationResult(LoanBankAccountRecord record, BankAccountAvailableStatus status, String name) {
    record.setAvailableStatus(status.charCode);
    record.setName(name);
    record.setTimeUpdated(Clock.now());
    record.update();
    return record;
  }

  public void updateValidationId(LoanBankAccountRecord record, BankType bankType, String validationId) {
    record.setBankCode(bankType.name());
    record.setValidationId(validationId);
    record.setTimeUpdated(Clock.now());
    record.update();
  }

  public LoanBankAccountRecord findByUserIdAndAccountNumber(Long userId, PaymentBusinessName businessName, String accountNumber) {
    return create().
        selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.eq(accountNumber))
        .and(TABLE.USER_ID.eq(userId))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .fetchAny();
  }

  public LoanBankAccountRecord findByUserIdAndAccountNumberAndBankCode(Long userId, PaymentBusinessName businessName, String accountNumber, String bankCode) {
    return create().
        selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.eq(accountNumber))
        .and(TABLE.USER_ID.eq(userId))
        .and(TABLE.BANK_CODE.eq(bankCode))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .fetchAny();
  }

  public List<LoanBankAccountRecord> findPendingAccountListInUpdatedTimeWindow(Long startTime, Long endTime) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.AVAILABLE_STATUS.eq(BankAccountAvailableStatus.PENDING.charCode))
        .and(TABLE.TIME_UPDATED.between(startTime, endTime))
        .fetch();
  }

  /**
   * use {@link this#fetchByIds} instead
   */
  @Deprecated
  public List<LoanBankAccountRecord> fetch(Set<Long> credentialIds) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.in(credentialIds))
        .fetch();
  }

  public List<LoanBankAccountRecord> fetchByIds(Set<Long> ids) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ID.in(ids))
        .fetch();
  }

  public List<LoanBankAccountRecord> findByAccountNumbers(List<String> accountNumbers, Long endTime) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.in(accountNumbers))
        .and(TABLE.TIME_CREATED.le(endTime))
        .fetch();
  }

  public List<LoanBankAccountRecord> findByAccountNumbers(Collection<String> accountNumbers) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.in(accountNumbers))
        .fetch();
  }

  public List<LoanBankAccountRecord> findAvailableByUserIdAndBankCode(Long userId, BankType bankType, SDKType sdkType) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.BANK_CODE.eq(bankType.name()))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .and(TABLE.AVAILABLE_STATUS.eq(BankAccountAvailableStatus.AVAILABLE.charCode))
        .fetch();
  }

  public Long countByAvailableAndUserIdAndBankCode(Long userId, BankType bankType, SDKType sdkType) {
    return create()
        .selectCount()
        .from(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.BANK_CODE.eq(bankType.name()))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .and(TABLE.AVAILABLE_STATUS.eq(BankAccountAvailableStatus.AVAILABLE.charCode))
        .fetchOneInto(Long.class);
  }

  public List<LoanBankAccountRecord> fetchByUserIdAndBusinessName(Long userId, PaymentBusinessName businessName) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .fetch();
  }

  public LoanBankAccountRecord findByUserIdJoinLatestOrder(Long userId) {
    return create()
        .select(TABLE.fields())
        .from(TABLE)
        .innerJoin(Tables.CASH_LOAN_ORDER)
        .on(TABLE.ID.equal(Tables.CASH_LOAN_ORDER.PAYMENT_CREDENTIAL_ID))
        .where(Tables.CASH_LOAN_ORDER.USER_ID.eq(userId))
        .orderBy(Tables.CASH_LOAN_ORDER.TIME_CREATED.desc())
        .limit(1)
        .fetchOneInto(LoanBankAccountRecord.class);
  }

  public List<LoanBankAccountRecord> findByUserIds(Collection<Long> userIds) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.in(userIds))
        .fetch();
  }

  /**
   * 批量按 userId 集合 + businessName 查询绑定银行卡，供 {@link BankCodeProvider} 合并账号批量取数。
   *
   * <p><b>核心逻辑</b>：userId IN + businessName 等值过滤，避免对每个连通 userId 单独查询。</p>
   */
  public List<LoanBankAccountRecord> findByUserIdsAndBusinessName(Collection<Long> userIds, PaymentBusinessName businessName) {
    if (userIds == null || userIds.isEmpty()) {
      return Collections.emptyList();
    }
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.in(userIds))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .fetch();
  }

  public List<LoanBankAccountRecord> findByUserIdAndSDKAndBankType(Long userId, SDKType sdkType, List<BankType> bankTypes) {
    List<String> bankNames = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(bankTypes)) {
      bankNames = bankTypes.stream().map(Enum::name).collect(Collectors.toList());
    }
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .and(TABLE.BANK_CODE.in(bankNames))
        .fetch();
  }

  public List<LoanBankAccountRecord> fetchByAccountNumbersAndBusiness(List<String> accountNumbers, PaymentBusinessName businessName) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.in(accountNumbers))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .fetchInto(LoanBankAccountRecord.class);
  }

  public List<LoanBankAccountRecord> findListByUserIdAndAccountNumberAndBankCode(Long userId, PaymentBusinessName businessName, String accountNumber, String bankCode) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.BUSINESS_NAME.eq(businessName.code))
        .and(TABLE.ACCOUNT_NUMBER.eq(accountNumber))
        .and(TABLE.BANK_CODE.eq(bankCode))
        .fetch();
  }

  public LoanBankAccountRecord getByUserIdAndBankNumberAndType(Long userId, SDKType sdkType, String accountNumber, BankType bankType) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .and(TABLE.ACCOUNT_NUMBER.eq(accountNumber))
        .and(TABLE.BANK_CODE.eq(bankType.name()))
        .fetchOne();
  }

  public List<LoanBankAccountRecord> findListByAccountNumberAndBankCodeAndAvailableStatusAndTimeCreate(SDKType sdkType,
                                                                                                       String accountNumber,
                                                                                                       BankType bankType,
                                                                                                       List<BankAccountAvailableStatus> availableStatusList,
                                                                                                       Long timeCreated) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.eq(accountNumber))
        .and(TABLE.BANK_CODE.eq(bankType.name()))
        .and(TABLE.AVAILABLE_STATUS.in(availableStatusList.stream().map(vo -> vo.charCode).collect(Collectors.toList())))
        .and(TABLE.TIME_CREATED.gt(timeCreated))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .fetch();
  }

  public List<LoanBankAccountRecord> queryByBankCodesAndAccountNumbers(List<String> bankCodes, List<String> accountNumbers) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.BANK_CODE.in(bankCodes))
        .and(TABLE.ACCOUNT_NUMBER.in(accountNumbers))
        .fetchInto(LoanBankAccountRecord.class);
  }

  public List<LoanBankAccountRecord> fetchByCondition(LoanBankAccountSearchCondition searchCondition) {
    Condition condition = getRiskCondition(searchCondition);

    return create()
        .selectFrom(TABLE)
        .where(condition)
        .fetch();
  }

  private static Condition getRiskCondition(LoanBankAccountSearchCondition searchCondition) {
    Condition condition = DSL.trueCondition();
    if (CollectionUtils.isNotEmpty(searchCondition.bankCodes)) {
      condition = condition.and(TABLE.BANK_CODE.in(searchCondition.bankCodes));
    }
    if (CollectionUtils.isNotEmpty(searchCondition.accountNumbers)) {
      condition = condition.and(TABLE.ACCOUNT_NUMBER.in(searchCondition.accountNumbers));
    }
    if (CollectionUtils.isNotEmpty(searchCondition.availableStatusCodes)) {
      condition = condition.and(TABLE.AVAILABLE_STATUS.in(searchCondition.availableStatusCodes));
    }
    if (CollectionUtils.isNotEmpty(searchCondition.userIds)) {
      condition = condition.and(TABLE.USER_ID.in(searchCondition.userIds));
    }
    if (Objects.nonNull(searchCondition.startTimeCreated)) {
      condition = condition.and(TABLE.TIME_CREATED.between(searchCondition.startTimeCreated, searchCondition.endTimeCreated));
    } else {
      condition = condition.and(TABLE.TIME_CREATED.lt(searchCondition.endTimeCreated));
    }
    return condition;
  }

  public List<LoanBankAccountRecord> findByUserIdAndCreateTimeDescLimit(Long userId, Integer limitCount) {
    return create().selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(limitCount)
        .fetch();
  }

  public List<LoanBankAccountRecord> findByUserIdAndStatusAndSdkType(Long userId, BankAccountAvailableStatus status, SDKType sdkType) {
    return create().selectFrom(TABLE)
        .where(TABLE.USER_ID.eq(userId))
        .and(TABLE.AVAILABLE_STATUS.eq(status.charCode))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .fetch();
  }

  public List<LoanBankAccountRecord> findByAccountNumbersAndStatusExcludeUserId(Collection<String> accountNumbers, BankAccountAvailableStatus status, Long excludeUserId, Integer limitCount, SDKType sdkType) {
    return create().selectFrom(TABLE)
        .where(TABLE.ACCOUNT_NUMBER.in(accountNumbers))
        .and(TABLE.AVAILABLE_STATUS.eq(status.charCode))
        .and(TABLE.USER_ID.ne(excludeUserId))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .orderBy(TABLE.ID.desc())
        .limit(limitCount)
        .fetch();
  }

  public List<LoanBankAccountRecord> findByBankCardCombinationsAndStatusExcludeUserId(List<Condition> cardConditions, BankAccountAvailableStatus status, Long excludeUserId, Integer limitCount, SDKType sdkType) {
    return create().selectFrom(TABLE)
        .where(DSL.or(cardConditions))
        .and(TABLE.AVAILABLE_STATUS.eq(status.charCode))
        .and(TABLE.USER_ID.ne(excludeUserId))
        .and(TABLE.SDK_TYPE.eq(sdkType.code))
        .orderBy(TABLE.TIME_CREATED.desc())
        .limit(limitCount)
        .fetch();
  }
}
