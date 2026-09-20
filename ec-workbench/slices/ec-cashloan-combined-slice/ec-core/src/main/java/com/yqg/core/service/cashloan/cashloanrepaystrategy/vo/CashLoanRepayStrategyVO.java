package com.yqg.core.service.cashloan.cashloanrepaystrategy.vo;

import com.yqg.core.model.generated.tables.records.CashLoanRepaymentStrategyRecord;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.enums.CashLoanRepayStrategyStatus;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionDetail;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashLoanRepayStrategyVO {
  private Long id;
  private Long loanAccountId;
  private Long orderId;
  private Long instalmentId;
  private CashLoanRepayStrategyStatus status;
  private BigDecimal deductAmount;
  private RepaymentAccountInfoVO accountInfo;
  private ManualReductionDetail.DeductAmountDetail reductionDetail;
  private Long expiredTime;
  private Long timeCreated;
  private Long timeUpdated;

  public static CashLoanRepayStrategyVO from(CashLoanRepaymentStrategyRecord record) {
    if (Objects.isNull(record)) {
      return null;
    }
    return CashLoanRepayStrategyVO.builder()
        .id(record.getId())
        .loanAccountId(record.getLoanAccountId())
        .orderId(record.getOrderId())
        .instalmentId(record.getInstalmentId())
        .expiredTime(record.getExpiredTime())
        .deductAmount(record.getDeductAmount())
        .accountInfo(JsonUtils.from(record.getAccountInfo(), RepaymentAccountInfoVO.class))
        .reductionDetail(Optional.ofNullable(JsonUtils.fromOrNull(record.getDeductDetail(), ManualReductionDetail.DeductAmountDetail.class))
            .orElse(new ManualReductionDetail.DeductAmountDetail()))
        .status(CashLoanRepayStrategyStatus.fromCode(record.getStatus()))
        .timeCreated(record.getTimeCreated())
        .timeUpdated(record.getTimeUpdated())
        .build();
  }

}
