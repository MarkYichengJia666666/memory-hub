package com.yqg.core.model.sql.cashloan;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.CashLoanRepaymentUnitDetail;
import com.yqg.core.model.generated.tables.records.CashLoanRepaymentUnitDetailRecord;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentDetailType;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * @author: YuchengHuang
 * @date: Created on 2020/8/19
 * @modified By:
 */
@Repository
public class CashLoanRepaymentDetailModel extends YqgBaseModel {

  private static final CashLoanRepaymentUnitDetail TABLE = Tables.CASH_LOAN_REPAYMENT_UNIT_DETAIL;

  public CashLoanRepaymentUnitDetailRecord init(
      Long userId,
      Long loanAccountId,
      Long orderId,
      Long repaymentId,
      Long instalmentId,
      EcCurrency currency,
      Long relatedId,
      BigDecimal interest,
      BigDecimal prePlatformFee,
      BigDecimal preInterestExlcudeFee,
      BigDecimal overdueInterest,
      BigDecimal penalty,
      BigDecimal postInterest,
      BigDecimal postPlatformFee,
      BigDecimal postInterestExcludeFee,
      BigDecimal ppn,
      BigDecimal principal,
      BigDecimal amount,
      CashLoanRepaymentDetailType type
  ) {
    CashLoanRepaymentUnitDetailRecord record = create().newRecord(TABLE);
    record.setUserId(userId);
    record.setLoanAccountId(loanAccountId);
    record.setOrderId(orderId);
    record.setRepaymentId(repaymentId);
    record.setInstalmentId(instalmentId);
    record.setCurrency(currency.name());
    record.setRelatedId(relatedId);
    record.setInterest(interest);
    record.setPrePlatformFee(prePlatformFee);
    record.setPreInterestExcludeFee(preInterestExlcudeFee);
    record.setOverdueInterest(overdueInterest);
    record.setPenalty(penalty);
    record.setPostInterest(postInterest);
    record.setPostPlatformFee(postPlatformFee);
    record.setPostInterestExcludeFee(postInterestExcludeFee);
    record.setPpn(ppn == null ? BigDecimal.ZERO : ppn);
    record.setPrincipal(principal);
    record.setAmount(amount);
    record.setType(type.code);
    Long now = Clock.now();
    record.setTimeCreated(now);
    record.setTimeUpdated(now);
    record.store();
    return record;
  }

  public List<CashLoanRepaymentUnitDetailRecord> findByRelatedIdAndType(Long relatedId, CashLoanRepaymentDetailType type) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.RELATED_ID.eq(relatedId))
        .and(TABLE.TYPE.eq(type.code))
        .fetch();
  }

  public List<CashLoanRepaymentUnitDetailRecord> findByOrderIdsAndType(Collection<Long> orderIds, CashLoanRepaymentDetailType type) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ORDER_ID.in(orderIds))
        .and(TABLE.TYPE.eq(type.code))
        .fetch();
  }

  public List<CashLoanRepaymentUnitDetailRecord> fetchByOrderIdAndTypes(List<Long> orderIds, List<String> types) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.ORDER_ID.in(orderIds))
        .and(TABLE.TYPE.in(types))
        .fetch();
  }

  public List<CashLoanRepaymentUnitDetailRecord> fetchInstalmentIdAndCouponType(Long instalmentId, CashLoanRepaymentDetailType couponType) {
    return create()
        .selectFrom(TABLE)
        .where(TABLE.INSTALMENT_ID.eq(instalmentId))
        .and(TABLE.TYPE.eq(couponType.code))
        .fetch();
  }
}
