package com.yqg.core.service.cashloan.repayment.enums;

import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountContext;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;

/**
 * 还款VA最终的生成类型
 */
@Getter
@AllArgsConstructor
public enum RepaymentAccountSourceType {
  EC("EC的VA，OP回调EC进行抵扣", context -> {
    //EC还款计划非空则生成联合支付的EC_VA
    if (CollectionUtils.isNotEmpty(context.getEncodeInstalmentIds())) {
      return RepaymentAccountUsageType.UNION_REPAY_EC_VA;
    } else {
      //否则encodeJBPOrderIds必不为空，获取还款JBP订单的EC_VA
      return RepaymentAccountUsageType.JBP_EC_VA;
    }
  }),
  JBP("JBP的VA，OP直接回调JBP进行抵扣", content -> RepaymentAccountUsageType.JBP_VA),
  ;

  private final String desc;

  /**
   * 根据希望最终生成的VA类型，和context，选择出还款VA的用途
   */
  private final Function<RepaymentAccountContext, RepaymentAccountUsageType> chooseUsageType;

}
