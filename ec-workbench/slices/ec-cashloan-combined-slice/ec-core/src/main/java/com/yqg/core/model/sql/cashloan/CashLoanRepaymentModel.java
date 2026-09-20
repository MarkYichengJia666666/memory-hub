package com.yqg.core.model.sql.cashloan;

import com.google.common.collect.Lists;
import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.CashLoanRepayment;
import com.yqg.core.model.generated.tables.records.CashLoanRepaymentRecord;
import com.yqg.core.model.sql.cashloan.condition.CashLoanRepaymentCondition;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentStatus;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentType;
import com.yqg.core.service.cashloan.vo.CashRepaymentOrderVO;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

@Repository
public class CashLoanRepaymentModel extends YqgBaseModel {
    private static final CashLoanRepayment TABLE = Tables.CASH_LOAN_REPAYMENT;

    public CashLoanRepaymentRecord findByPaymentTransId(String transId) {
        return create()
                .selectFrom(TABLE)
                .where(TABLE.PAYMENT_TRANS_ID.equal(transId))
                .fetchOne();
    }

    public CashLoanRepaymentRecord findByPaymentTransIdOrThrow(String transId) {
        CashLoanRepaymentRecord record = create()
                .selectFrom(TABLE)
                .where(TABLE.PAYMENT_TRANS_ID.equal(transId))
                .fetchOne();
        if (record == null) {
            throw EcException.error("repayment not found, payment_trans_id=" + transId);
        }
        return record;
    }

    public List<CashLoanRepaymentRecord> find(Long loanAccountId, List<CashLoanRepaymentType> typeList, Long timeBegin, Long timeEnd, int offset, int limit) {
        List<String> typeCodeList = typeList.stream().map(o -> o.code).collect(Collectors.toList());
        return create()
                .selectFrom(TABLE)
                .where(TABLE.ACCOUNT_ID.eq(loanAccountId))
                .and(TABLE.TYPE.in(typeCodeList))
                .and(timeBegin != null ? TABLE.TIME_CREATED.ge(timeBegin) : DSL.trueCondition())
                .and(timeEnd != null ? TABLE.TIME_CREATED.le(timeEnd) : DSL.trueCondition())
                .limit(limit)
                .offset(offset)
                .fetch();
    }

    public List<CashLoanRepaymentRecord> find(Long loanAccountId, List<CashLoanRepaymentType> typeList, int offset, int limit) {
        List<String> typeCodeList = typeList.stream().map(o -> o.code).collect(Collectors.toList());
        return create()
                .selectFrom(TABLE)
                .where(TABLE.ACCOUNT_ID.eq(loanAccountId))
                .and(TABLE.TYPE.in(typeCodeList))
                .orderBy(TABLE.TIME_CREATED.desc())
                .limit(limit)
                .offset(offset)
                .fetch();
    }

    public CashLoanRepaymentRecord init(
            Long loanAccountId,
            Long userId,
            EcCurrency currency,
            BigDecimal amount,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal prePlatformFee,
            BigDecimal preInterestExcludeFee,
            BigDecimal postInterest,
            BigDecimal postPlatformFee,
            BigDecimal postInterestExcludeFee,
            BigDecimal ppn,
            BigDecimal overdueInterest,
            BigDecimal penalty,
            BigDecimal overflowAmount,
            String paymentTransId,
            Long timeRepaid,
            CashLoanRepaymentStatus status,
            Long userOpt,
            CashLoanRepaymentType type,
            String extraData,
            Long couponId,
            Long transactionTime
    ) {
        CashLoanRepaymentRecord record = create().newRecord(TABLE);
        record.setAccountId(loanAccountId);
        record.setUserId(userId);
        record.setCurrency(currency.name());
        record.setPaymentTransId(paymentTransId);
        record.setStatus(status.code);
        record.setUserOpt(userOpt);
        record.setType(type.code);
        record.setExtraData(extraData);
        record.setCouponId(couponId);
        record.setAmount(amount);
        record.setPrincipal(principal);
        record.setInterest(interest);
        record.setPrePlatformFee(prePlatformFee);
        record.setPreInterestExcludeFee(preInterestExcludeFee);
        record.setPostInterest(postInterest);
        record.setPostPlatformFee(postPlatformFee);
        record.setPostInterestExcludeFee(postInterestExcludeFee);
        record.setPpn(ppn == null ? BigDecimal.ZERO : ppn);
        record.setOverdueInterest(overdueInterest);
        record.setPenalty(penalty);
        record.setOverflowAmount(overflowAmount);
        record.setTimeRepaid(timeRepaid);
        record.setTransactionTime(transactionTime);
        long now = Clock.now();
        record.setTimeCreated(now);
        record.setTimeUpdated(now);
        record.store();
        return record;
    }

    public void updateStatus(CashLoanRepaymentRecord record, CashLoanRepaymentStatus status, Long transactionTime) {
        record.setStatus(status.code);
        record.setTimeUpdated(Clock.now());
        if (record.getTimeRepaid() == null && (status == CashLoanRepaymentStatus.SUCCEED || status == CashLoanRepaymentStatus.FAIL)) {
            record.setTimeRepaid(transactionTime);
        }
        record.update();
    }

    public CashLoanRepaymentRecord findByIdOrThrow(Long id) {
        CashLoanRepaymentRecord record = create()
                .selectFrom(TABLE)
                .where(TABLE.ID.equal(id))
                .fetchOne();
        if (record == null) {
            throw EcException.error("repayment not found. id=" + id);
        }
        return record;
    }

    public List<CashLoanRepaymentRecord> findByOrderId(Long orderId) {
        return findByOrderIdAndStatuses(orderId);
    }

    public List<CashLoanRepaymentRecord> fetch(Long orderId, CashLoanRepaymentType... types) {
        List<String> typeCodes = Lists.newArrayList(types).stream()
                .map(type -> type.code)
                .collect(Collectors.toList());
        return create()
                .select(TABLE.fields())
                .from(TABLE)
                .join(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .on(TABLE.ID.eq(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.eq(orderId))
                .and(typeCodes.isEmpty() ? DSL.trueCondition() : TABLE.TYPE.in(typeCodes))
                .groupBy(TABLE.ID)
                .fetchInto(CashLoanRepaymentRecord.class);
    }

    public List<CashLoanRepaymentRecord> findByOrderIdAndType(Long orderId, CashLoanRepaymentType type) {
        return create()
                .select(TABLE.fields())
                .from(TABLE)
                .join(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .on(TABLE.ID.eq(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.eq(orderId))
                .and(TABLE.TYPE.eq(type.code))
                .fetchInto(CashLoanRepaymentRecord.class);
    }


    public List<CashLoanRepaymentRecord> findByOrderIdAndStatuses(Long orderId, CashLoanRepaymentStatus... statuses) {
        List<String> statusCodes = Lists.newArrayList();
        for (CashLoanRepaymentStatus status : statuses) {
            statusCodes.add(status.code);
        }

        return create()
                .select(TABLE.fields())
                .from(TABLE)
                .join(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .on(TABLE.ID.eq(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.eq(orderId))
                .and(CollectionUtils.isNotEmpty(statusCodes) ? TABLE.STATUS.in(statusCodes) : DSL.trueCondition())
                .groupBy(TABLE.ID)
                .fetchInto(CashLoanRepaymentRecord.class);
    }

    public List<CashLoanRepaymentRecord> findByInstalmentIdsAndStatuses(Collection<Long> instalmentIds, CashLoanRepaymentStatus... statuses) {
        List<String> statusCodes = Lists.newArrayList();
        for (CashLoanRepaymentStatus status : statuses) {
            statusCodes.add(status.code);
        }

        return create()
                .select(TABLE.fields())
                .from(TABLE)
                .join(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .on(TABLE.ID.eq(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.INSTALMENT_ID.in(instalmentIds))
                .and(CollectionUtils.isNotEmpty(statusCodes) ? TABLE.STATUS.in(statusCodes) : DSL.trueCondition())
                .groupBy(TABLE.ID)
                .fetchInto(CashLoanRepaymentRecord.class);
    }

    public List<CashLoanRepaymentRecord> find(CashLoanRepaymentStatus status) {
        return create().selectFrom(TABLE)
                .where(TABLE.STATUS.eq(status.code))
                .fetch();
    }

    public Map<Long, CashLoanRepaymentRecord> fetchMapByIdsAndType(Collection<Long> ids, CashLoanRepaymentType type) {
        return create()
                .selectFrom(TABLE)
                .where(TABLE.ID.in(ids))
                .and(TABLE.TYPE.eq(type.code))
                .fetchMap(TABLE.ID);
    }

    public List<CashLoanRepaymentRecord> fetchByIds(List<Long> ids) {
        return create()
                .selectFrom(TABLE)
                .where(TABLE.ID.in(ids))
                .fetch();
    }

    public Map<Long, String> fetchTransIdMapByIds(Set<Long> ids) {
        return create()
                .selectFrom(TABLE)
                .where(TABLE.ID.in(ids))
                .and(TABLE.STATUS.eq(CashLoanRepaymentStatus.SUCCEED.code))
                .fetchMap(TABLE.ID, TABLE.PAYMENT_TRANS_ID);
    }

    public CashLoanRepaymentRecord fetchNextRepayment(Long userId, Long timeCreated) {
        return create()
                .selectFrom(TABLE)
                .where(TABLE.USER_ID.eq(userId))
                .and(TABLE.TIME_CREATED.gt(timeCreated))
                .orderBy(TABLE.TIME_CREATED.asc())
                .limit(1)
                .fetchOne();
    }

    public List<CashLoanRepaymentRecord> findAllOverFlowRepaymentRecord(Long accountId) {
        return create()
                .selectFrom(TABLE)
                .where(TABLE.ACCOUNT_ID.eq(accountId))
                .and(TABLE.OVERFLOW_AMOUNT.gt(BigDecimal.ZERO))
                .fetch();
    }

    public List<CashLoanRepaymentRecord> getByCondition(CashLoanRepaymentCondition searchCondition) {
        Condition condition = getCondition(searchCondition);

        return create()
                .selectFrom(TABLE)
                .where(condition)
                .fetch();
    }

    private static Condition getCondition(CashLoanRepaymentCondition searchCondition) {
        Condition condition = DSL.trueCondition();
        if (CollectionUtils.isNotEmpty(searchCondition.ids)) {
            condition = condition.and(TABLE.ID.in(searchCondition.ids));
        }
        if (CollectionUtils.isNotEmpty(searchCondition.statusCodes)) {
            condition = condition.and(TABLE.STATUS.in(searchCondition.statusCodes));
        }
        if (Objects.nonNull(searchCondition.accountId)) {
            condition = condition.and(TABLE.ACCOUNT_ID.eq(searchCondition.accountId));
        }
        if (CollectionUtils.isNotEmpty(searchCondition.typeCodes)) {
            condition = condition.and(TABLE.TYPE.in(searchCondition.typeCodes));
        }
        if (Objects.nonNull(searchCondition.startTimeCreated)) {
            condition = condition.and(TABLE.TIME_CREATED.ge(searchCondition.startTimeCreated));
        }
        if (Objects.nonNull(searchCondition.endTimeCreated)) {
            condition = condition.and(TABLE.TIME_CREATED.le(searchCondition.endTimeCreated));
        }
        return condition;
    }

    public List<CashLoanRepaymentRecord> findByOrderIdsAndStatuses(List<Long> orderIds, List<CashLoanRepaymentStatus> statuses) {
        List<String> statusCodes = Lists.newArrayList();
        for (CashLoanRepaymentStatus status : statuses) {
            statusCodes.add(status.code);
        }

        return create()
                .select(TABLE.fields())
                .from(TABLE)
                .join(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .on(TABLE.ID.eq(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.in(orderIds))
                .and(CollectionUtils.isNotEmpty(statusCodes) ? TABLE.STATUS.in(statusCodes) : DSL.trueCondition())
                .groupBy(TABLE.ID)
                .fetchInto(CashLoanRepaymentRecord.class);
    }

    public List<CashRepaymentOrderVO> fetch(List<Long> orderIds, List<CashLoanRepaymentType> types) {
        List<String> typeCodes = Lists.newArrayList(types).stream()
                .map(type -> type.code)
                .collect(Collectors.toList());
        return create()
                .select(TABLE.AMOUNT, Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID)
                .from(TABLE)
                .join(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .on(TABLE.ID.eq(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.in(orderIds))
                .and(typeCodes.isEmpty() ? DSL.trueCondition() : TABLE.TYPE.in(typeCodes))
                .groupBy(TABLE.ID)
                .fetchInto(CashRepaymentOrderVO.class);
    }

    public int existRepaySuccessByTime(Long userId, Long startTime) {
        return create()
                .selectCount()
                .from(TABLE)
                .where(TABLE.TIME_CREATED.between(startTime, Clock.now()))
                .and(TABLE.STATUS.eq(CashLoanRepaymentStatus.SUCCEED.code))
                .and(TABLE.TYPE.eq(CashLoanRepaymentType.NORMAL.code))
                .and(TABLE.USER_ID.eq(userId))
                .fetchOne(0, Integer.class);
    }

    public CashLoanRepaymentRecord getLastSucceedRepaymentOrderAfterTime(Long userId, Long startTime) {
        return create()
                .selectFrom(TABLE)
                .where(TABLE.USER_ID.eq(userId))
                .and(TABLE.TIME_REPAID.gt(startTime))
                .and(TABLE.STATUS.eq(CashLoanRepaymentStatus.SUCCEED.code))
                .and(TABLE.TYPE.eq(CashLoanRepaymentType.NORMAL.code))
                .orderBy(TABLE.TIME_REPAID.asc())
                .limit(1)
                .fetchOne();
    }

    public List<CashLoanRepaymentRecord> fetchByAccountIdAndLimit(Long accountId, int limit){
      return create()
          .selectFrom(TABLE)
          .where(TABLE.ACCOUNT_ID.eq(accountId))
          .orderBy(TABLE.TIME_CREATED.desc())
          .limit(limit)
          .fetch();
    }

    public int countRepaySuccess(Long userId) {
        return create()
            .selectCount()
            .from(TABLE)
            .where(TABLE.STATUS.eq(CashLoanRepaymentStatus.SUCCEED.code))
            .and(TABLE.TYPE.eq(CashLoanRepaymentType.NORMAL.code))
            .and(TABLE.USER_ID.eq(userId))
            .fetchOne(0, Integer.class);
    }

    /**
     * Lists repayments by TIME_REPAID half-open window. Does not change getByCondition.
     *
     * @param timeFrom inclusive TIME_REPAID
     * @param timeTo exclusive TIME_REPAID
     * @param status repayment status
     * @param types repayment types
     * @return matching records, empty when types is empty
     */
    public List<CashLoanRepaymentRecord> fetchByTimeRepaidAndStatusAndTypes(
            Long timeFrom, Long timeTo, CashLoanRepaymentStatus status, Collection<CashLoanRepaymentType> types) {
        if (CollectionUtils.isEmpty(types) || timeFrom == null || timeTo == null) {
            return Lists.newArrayList();
        }
        List<String> typeCodes = types.stream().map(type -> type.code).collect(Collectors.toList());
        return create()
                .selectFrom(TABLE)
                .where(TABLE.TIME_REPAID.ge(timeFrom))
                .and(TABLE.TIME_REPAID.lt(timeTo))
                .and(TABLE.STATUS.eq(status.code))
                .and(TABLE.TYPE.in(typeCodes))
                .fetch();
    }
}
