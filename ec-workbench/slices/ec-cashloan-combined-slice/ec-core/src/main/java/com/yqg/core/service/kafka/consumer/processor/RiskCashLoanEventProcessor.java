package com.yqg.core.service.kafka.consumer.processor;

import com.yqg.core.service.cashloan.observer.ICashLoanOrderObserver;
import com.yqg.core.service.cashloan.observer.RiskCashLoanEventObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 风控专用现金贷事件处理器
 *
 * @author generated
 */
@Component
public class RiskCashLoanEventProcessor extends BaseCashLoanEventProcessor {

  @Autowired
  private RiskCashLoanEventObserver riskObserver;

  @Override
  protected ICashLoanOrderObserver getObserver() {
    return riskObserver;
  }
}