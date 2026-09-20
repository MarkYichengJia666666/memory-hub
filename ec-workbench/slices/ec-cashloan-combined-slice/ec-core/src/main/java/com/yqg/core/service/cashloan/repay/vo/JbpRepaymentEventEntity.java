package com.yqg.core.service.cashloan.repay.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JbpRepaymentEventEntity {
  private Long userId;
  private BigDecimal amount;
  private String payEventType;// 支付事件类型
  private String transId; // 支付流水号
  private String unionRepaymentResultType; // 组合支付还款结果类型
  /**
   * 给JBP抵扣用
   **/
  private Long orderId; // 中收订单号
  private Long transTime; // 支付时间（抵扣用）

  public final static String LAUNCH = "LAUNCH";
  public final static String EC_RECEIPT = "EC_RECEIPT";

  public static JbpRepaymentEventEntity build(UnionRepaymentVO unionRepaymentVO, Long orderId) {
    JbpRepaymentEventEntity jbpRepaymentEventEntity = new JbpRepaymentEventEntity();
    jbpRepaymentEventEntity.userId = unionRepaymentVO.getUserId();
    jbpRepaymentEventEntity.orderId = orderId;
    jbpRepaymentEventEntity.transTime = unionRepaymentVO.getTransactionTime();
    jbpRepaymentEventEntity.payEventType = EC_RECEIPT;
    jbpRepaymentEventEntity.unionRepaymentResultType = LAUNCH;
    jbpRepaymentEventEntity.transId = unionRepaymentVO.transNo;
    jbpRepaymentEventEntity.amount = unionRepaymentVO.amount.getAmountInYuan();
    return jbpRepaymentEventEntity;
  }
}
