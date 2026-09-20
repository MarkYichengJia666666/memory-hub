package com.yqg.core.model.sql.cashloan;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.CashLoanRepaymentUnit;
import com.yqg.core.model.generated.tables.records.CashLoanRepaymentUnitRecord;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentStatus;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentType;
import com.yqg.core.service.cashloan.vo.RepaymentUnitVO;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.max;

@Repository
public class CashLoanRepaymentUnitModel extends YqgBaseModel {
    private static final CashLoanRepaymentUnit TABLE = Tables.CASH_LOAN_REPAYMENT_UNIT;

    public List<CashLoanRepaymentUnitRecord> findByRepaymentId(Long repaymentId) {
        return create()
                .selectFrom(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.equal(repaymentId))
                .fetch();
    }

    public List<CashLoanRepaymentUnitRecord> findByRepaymentIds(Collection<Long> repaymentIds) {
        return create()
                .selectFrom(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.in(repaymentIds))
                .fetch();
    }

    public List<CashLoanRepaymentUnitRecord> findByRepaymentIdAndOrderId(Long repaymentId, Long orderId) {
        return create()
                .selectFrom(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.eq(repaymentId))
                .and(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.eq(orderId))
                .fetch();
    }

    public List<Long> findOrderIdsOrThrow(Long repaymentId) {
        List<Long> orderIds = create()
                .selectDistinct(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID)
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.eq(repaymentId))
                .fetchInto(Long.class);
        if (CollectionUtils.isEmpty(orderIds)) {
            throw EcException.error("can't find order through repayment_id: {}", repaymentId);
        }
        return orderIds;
    }

    public Long findLastedOrderIdOrThrow(Long repaymentId) {
        Long lastedOrderId = create().select(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID)
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.eq(repaymentId))
                .orderBy(Tables.CASH_LOAN_REPAYMENT_UNIT.ID.desc())
                .limit(1)
                .fetchOneInto(Long.class);
        if (lastedOrderId == null) {
            throw EcException.error("can't find lastedOrderId through repayment_id: {}", repaymentId);
        }
        return lastedOrderId;
    }

    public CashLoanRepaymentUnitRecord init(
            Long loanAccountId,
            Long userId,
            Long repaymentId,
            Long orderId,
            Long instalmentId,
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
            BigDecimal penalty) {

        CashLoanRepaymentUnitRecord record = create().newRecord(Tables.CASH_LOAN_REPAYMENT_UNIT);
        record.setAccountId(loanAccountId);
        record.setUserId(userId);
        record.setRepaymentId(repaymentId);
        record.setOrderId(orderId);
        record.setInstalmentId(instalmentId);
        record.setCurrency(currency.name());
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
        record.setAmount(amount);
        record.setUserId(userId);
        record.setTimeCreated(Clock.now());
        record.setTimeUpdated(Clock.now());
        record.store();
        return record;
    }

    public List<RepaymentUnitVO> findFullInfoByRepaymentIds(Collection<Long> repaymentIds) {
        return findFullInfoByRepaymentIds(repaymentIds, null);
    }

    public List<RepaymentUnitVO> findFullInfoByRepaymentIds(Collection<Long> repaymentIds, Long orderId) {
        Condition orderIdCondition = DSL.trueCondition();
        if (orderId != null) {
            orderIdCondition = orderIdCondition.and(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.eq(orderId));
        }

        Result<Record> result = create()
                .select()
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .join(Tables.CASH_LOAN_REPAYMENT)
                .on(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.eq(Tables.CASH_LOAN_REPAYMENT.ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.in(repaymentIds))
                .and(orderIdCondition)
                .fetch();
        return result.stream()
                .map(record -> RepaymentUnitVO.from(
                        record.into(Tables.CASH_LOAN_REPAYMENT_UNIT), record.into(Tables.CASH_LOAN_REPAYMENT)))
                .collect(Collectors.toList());
    }

    public List<RepaymentUnitVO> getRepaymentUnitVOsByOrderIdsAndStatus(Collection<Long> orderIds, List<CashLoanRepaymentStatus> statuses) {
        List<String> statusCodes = statuses.stream().map(status -> status.code).collect(Collectors.toList());
        Result<Record> result = create()
                .select()
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .join(Tables.CASH_LOAN_REPAYMENT)
                .on(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.eq(Tables.CASH_LOAN_REPAYMENT.ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.in(orderIds))
                .and(Tables.CASH_LOAN_REPAYMENT.STATUS.in(statusCodes))
                .fetch();
        return result.stream()
                .map(record -> RepaymentUnitVO.from(
                        record.into(Tables.CASH_LOAN_REPAYMENT_UNIT), record.into(Tables.CASH_LOAN_REPAYMENT)))
                .collect(Collectors.toList());
    }

    public List<RepaymentUnitVO> getRepaymentUnitVOsByInstalmentIdAndType(Long instalmentId, CashLoanRepaymentType type) {
        Result<Record> result = create()
                .select()
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .join(Tables.CASH_LOAN_REPAYMENT)
                .on(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.eq(Tables.CASH_LOAN_REPAYMENT.ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.INSTALMENT_ID.eq(instalmentId))
                .and(Tables.CASH_LOAN_REPAYMENT.TYPE.eq(type.code))
                .fetch();
        return result.stream()
                .map(record -> RepaymentUnitVO.from(
                        record.into(Tables.CASH_LOAN_REPAYMENT_UNIT), record.into(Tables.CASH_LOAN_REPAYMENT)))
                .collect(Collectors.toList());
    }

    public List<CashLoanRepaymentUnitRecord> findByOrderIds(List<Long> orderIds) {
        return create()
                .selectFrom(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.in(orderIds))
                .fetch();
    }

    public List<CashLoanRepaymentUnitRecord> findByOrderIdsAndLeTimeCreated(List<Long> orderIds, Long timeCreated) {
        return create()
                .selectFrom(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.in(orderIds))
                .and(Tables.CASH_LOAN_REPAYMENT_UNIT.TIME_CREATED.le(timeCreated))
                .fetch();
    }

    /**
     * Returns all repayment-unit records for the given instalment IDs with
     * {@code time_created <= timeCreated}.  Used by SLIK F01 to reconstruct the
     * as-of-month-end repaid amounts for instalments that were only fully settled
     * after the reporting month ended (partial pre-month-end repayments exist).
     */
    public List<CashLoanRepaymentUnitRecord> findByInstalmentIdsAndLeTimeCreated(
            Collection<Long> instalmentIds, Long timeCreated) {
        if (instalmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        return create()
                .selectFrom(TABLE)
                .where(TABLE.INSTALMENT_ID.in(instalmentIds))
                .and(TABLE.TIME_CREATED.le(timeCreated))
                .fetch();
    }

    public List<CashLoanRepaymentUnitRecord> findByOrderIdsAndGtTimeCreated(List<Long> orderIds, Long timeCreated) {
        return create()
                .selectFrom(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.in(orderIds))
                .and(Tables.CASH_LOAN_REPAYMENT_UNIT.TIME_CREATED.gt(timeCreated))
                .fetch();
    }

    public List<CashLoanRepaymentUnitRecord> findByOrderIdAndType(long orderId, CashLoanRepaymentType type) {
        return create()
                .select(Tables.CASH_LOAN_REPAYMENT_UNIT.fields())
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .innerJoin(Tables.CASH_LOAN_REPAYMENT)
                .on(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.eq(Tables.CASH_LOAN_REPAYMENT.ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.eq(orderId))
                .and(Tables.CASH_LOAN_REPAYMENT.TYPE.eq(type.code))
                .fetchInto(CashLoanRepaymentUnitRecord.class);
    }

    public List<CashLoanRepaymentUnitRecord> findByOrderIdAndType(long orderId, List<CashLoanRepaymentType> type) {
        return create()
                .select(Tables.CASH_LOAN_REPAYMENT_UNIT.fields())
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .innerJoin(Tables.CASH_LOAN_REPAYMENT)
                .on(Tables.CASH_LOAN_REPAYMENT_UNIT.REPAYMENT_ID.eq(Tables.CASH_LOAN_REPAYMENT.ID))
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.eq(orderId))
                .and(Tables.CASH_LOAN_REPAYMENT.TYPE.in(type.stream().map(t -> t.code).toArray(String[]::new)))
                .fetchInto(CashLoanRepaymentUnitRecord.class);
    }

    public CashLoanRepaymentUnitRecord findById(long id) {
        return create()
                .selectFrom(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ID.eq(id))
                .fetchOne();
    }

    public Map<Long, Long> getMaxRepaymentUnitIdByOrderIds(Collection<Long> orderIds) {
        Field<Long> maxUnitId = max(Tables.CASH_LOAN_REPAYMENT_UNIT.ID);
        return create()
                .select(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID, maxUnitId)
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID.in(orderIds))
                .groupBy(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID)
                .fetchMap(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID, maxUnitId);
    }

    public List<Long> findOrderIdsByIds(Collection<?> ids) {
        return create()
                .select(Tables.CASH_LOAN_REPAYMENT_UNIT.ORDER_ID)
                .from(Tables.CASH_LOAN_REPAYMENT_UNIT)
                .where(Tables.CASH_LOAN_REPAYMENT_UNIT.ID.in(ids))
                .fetchInto(Long.class);
    }

    public List<RepaymentUnitVO> fetchLatestRepaymentUnitByInstalmentIds(List<Long> instalmentIds) {
        Result<CashLoanRepaymentUnitRecord> result = create()
                .selectFrom(TABLE)
                .where(TABLE.ID.in(
                        create().select(TABLE.ID.max())
                                .from(TABLE)
                                .where(TABLE.INSTALMENT_ID.in(instalmentIds))
                                .fetch())
                )
                .fetch();

        return result.stream()
                .map(record -> RepaymentUnitVO.from(
                        record.into(Tables.CASH_LOAN_REPAYMENT_UNIT)))
                .collect(Collectors.toList());
    }

    public List<Long> fetchDistinctInstalmentIdByTimeUpdated(long startTime, long endTime) {
        return create()
                .select(TABLE.INSTALMENT_ID)
                .from(TABLE)
                .where(TABLE.TIME_UPDATED.between(startTime, endTime))
                .fetchInto(Long.class);
    }
}
