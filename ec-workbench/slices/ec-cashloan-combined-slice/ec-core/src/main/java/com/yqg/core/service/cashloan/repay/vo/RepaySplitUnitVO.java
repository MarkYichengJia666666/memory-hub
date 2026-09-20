package com.yqg.core.service.cashloan.repay.vo;

import com.yqg.core.model.generated.tables.records.RepaymentSplitUnitRecord;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RepaySplitUnitVO {
  public Long id;
  public Long userId;
  public UnionRepaymentType deductType;
  public String transNo;
  public ProcessStatus repayStatus;
  public BigDecimal amount;
  public Long timeCreated;
  public Long timeUpdated;

  public static RepaySplitUnitVO buildUnprocessedUnit(UnionRepaymentType deductType, String transNo, BigDecimal amount, Long userId) {
    RepaySplitUnitVO vo = new RepaySplitUnitVO();
    vo.deductType = deductType;
    vo.transNo = transNo;
    vo.amount = amount;
    vo.userId = userId;
    vo.repayStatus = ProcessStatus.UNPROCESSED;
    return vo;
  }

  public static RepaySplitUnitVO from(RepaymentSplitUnitRecord record) {
    RepaySplitUnitVO vo = new RepaySplitUnitVO();
    vo.deductType = UnionRepaymentType.valueOf(record.getDeductType());
    vo.transNo = record.getTranNo();
    vo.id = record.getId();
    vo.amount = record.getAmount();
    vo.userId = record.getUserId();
    vo.repayStatus = ProcessStatus.fromCode(record.getRepayStatus());
    vo.timeCreated = record.getTimeCreated();
    vo.timeUpdated = record.getTimeUpdated();
    return vo;
  }
}