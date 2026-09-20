package com.yqg.core.model.sql.cashloan;

import com.yqg.core.model.core.YqgBaseModel;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.CashLoanRepayEvent;
import com.yqg.core.model.generated.tables.records.CashLoanRepayEventRecord;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentType;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class CashLoanRepayEventModel extends YqgBaseModel {

  private static final CashLoanRepayEvent TABLE = Tables.CASH_LOAN_REPAY_EVENT;

  public CashLoanRepayEventRecord insert(
      Long loanAccountId,
      Long repaymentId,
      EcCurrency currency,
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
      CashLoanRepaymentType type) {

    CashLoanRepayEventRecord record = create().newRecord(TABLE);
    record.setLoanAccountId(loanAccountId);
    record.setCurrency(currency.name());
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
    record.setRepaymentId(repaymentId);
    record.setOverflowAmount(overflowAmount);
    long now = Clock.now();
    record.setTimeCreated(now);
    record.setTimeUpdated(now);
    record.setType(type.code);
    record.store();
    return record;
  }
}
