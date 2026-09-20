package com.yqg.core.service.cashloan.repay;

import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class NonEcAllocationItem {

  private UnionRepaymentType type;
  private List<Long> businessIds;
  private BigDecimal owedAmount;
  private BigDecimal requestedAmount;

  /**
   * 最终拆账规则按业务给出的可分配金额处理；若未单独传入，则默认回退到应还金额。
   */
  public BigDecimal resolveRequestedAmount() {
    if (Objects.nonNull(requestedAmount)) {
      return requestedAmount;
    }
    if (Objects.nonNull(owedAmount)) {
      return owedAmount;
    }
    return BigDecimal.ZERO;
  }

  /**
   * 只有类型明确且金额为正，才允许该非 EC 项进入最终拆账路径。
   */
  public String invalidReason() {
    if (Objects.isNull(type)) {
      return "type is missing";
    }
    if (resolveRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
      return "requested amount is not positive";
    }
    return null;
  }

  /**
   * 供最终拆账逻辑在构建输入项时快速过滤脏数据，避免主流程里出现额外分支判断。
   */
  public boolean isValidForSplit() {
    return Objects.isNull(invalidReason());
  }
}
