package com.yqg.core.service.cashloan.repay.vo;

import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UnionRepaymentPlanVO {
  /**
   * 业务ID
   */
  public List<Long> businessIds;
  /**
   * 联合支付类型
   */
  public UnionRepaymentType unionRepaymentType;
  /**
   * 支付权重
   */
  public Integer weight;
}