package com.yqg.core.service.cashloan.repayment;

import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.service.cashloan.repayment.enums.RepaymentAccountUsageType;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountContext;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountResVO;
import com.yqg.core.service.payment.PaymentAccount;
import javax.annotation.PostConstruct;

/**
 * ⚠️⚠️⚠️ 此类中的三个方法具有业务依赖，修改其一的实现请同步检查是否需要修改其他方法 ⚠️⚠️⚠️
 */
public interface RepaymentAccountHandler {

  @PostConstruct
  default void init() {
    RepaymentAccountFactory.registerHandler(getRepaymentAccountUsageType(), this);
  }

  RepaymentAccountUsageType getRepaymentAccountUsageType();

  /**
   * 获取 VA
   *
   * @param context
   * @return
   */
  RepaymentAccountResVO getRepaymentAccountByChannelV3(RepaymentAccountContext context);

  /**
   * 获取OVO VA
   *
   * @param context
   * @return
   */
  RepaymentAccountResVO getOVORepaymentAccountByChannel(RepaymentAccountContext context);

  /**
   * BCA获取还款金额
   * @param userId
   * @param payEventType
   * @param account
   * @return
   */
  Long getOwnedAmount(Long userId, PayEventType payEventType, PaymentAccount account);
}