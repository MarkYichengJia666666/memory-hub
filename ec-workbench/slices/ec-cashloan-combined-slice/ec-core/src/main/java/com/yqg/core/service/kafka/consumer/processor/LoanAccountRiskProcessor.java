package com.yqg.core.service.kafka.consumer.processor;

import com.yqg.core.service.cashloan.observer.IRiskObserver;
import com.yqg.core.service.cashloan.observer.LoanAccountObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoanAccountRiskProcessor extends BaseRiskEventProcessor {
  @Autowired
  private LoanAccountObserver observer;

  @Override
  protected IRiskObserver getObserver() {
    return observer;
  }
}
