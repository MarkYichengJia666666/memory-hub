package com.yqg.core.service.cashloan.observer;


import com.yqg.core.service.cashloan.observer.vo.RepaymentKafkaMessageVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.RepaymentVO;

/**
 * Created by bickey on 16/7/19.
 */

public interface ICashLoanOrderObserver {
  default void onOrderOverdue(CashLoanOrderVO orderVO) {}

  default void onRepaymentSucceed(RepaymentVO repaymentVO, RepaymentKafkaMessageVO repaymentKafkaMessageVO) {}

  default void onRepaymentFailed(RepaymentVO repaymentVO, RepaymentKafkaMessageVO repaymentKafkaMessageVO) {}

  default void onOrderReady(CashLoanOrderVO orderVO) {}

  default void onOrderReserved(CashLoanOrderVO orderVO) {}

  default void onOrderRejected(CashLoanOrderVO orderVO) {}

  default void onOrderCompleted(CashLoanOrderVO orderVO) {}

  default void onOrderSigned(CashLoanOrderVO orderVO, Long timeUpdated) {}

  default void onOrderInitial(CashLoanOrderVO orderVO) {}

  default void onOrderChecked(CashLoanOrderVO orderVO) {}

  default void onOrderPostInterestUpdated(CashLoanOrderVO orderVO) {}

  default void onOrderStopInterestCreated(CashLoanOrderVO orderVO) {}
}
