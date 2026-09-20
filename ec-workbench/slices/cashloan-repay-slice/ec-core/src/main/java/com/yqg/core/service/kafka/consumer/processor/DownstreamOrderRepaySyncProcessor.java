package com.yqg.core.service.kafka.consumer.processor;

import com.yqg.core.service.cashloan.observer.DownstreamOrderRepaySyncObserver;
import com.yqg.core.service.cashloan.observer.ICashLoanOrderObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 下游订单/还款四表同步专用 Processor，仅消费 ORDER_READY、REPAYMENT_SUCCEED 做同步推送。
 */
@Component
public class DownstreamOrderRepaySyncProcessor extends BaseCashLoanEventProcessor {

  @Autowired
  private DownstreamOrderRepaySyncObserver observer;

  @Override
  protected ICashLoanOrderObserver getObserver() {
    return observer;
  }
}
