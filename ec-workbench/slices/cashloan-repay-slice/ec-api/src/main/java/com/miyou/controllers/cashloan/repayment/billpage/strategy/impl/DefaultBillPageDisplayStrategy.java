package com.miyou.controllers.cashloan.repayment.billpage.strategy.impl;

import com.miyou.controllers.cashloan.repayment.billpage.strategy.infra.AbstractBillPageDisplayStrategy;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultBillPageDisplayStrategy extends AbstractBillPageDisplayStrategy {
  @Override
  public BillPageDisplayStrategyKey getKey() {
    return BillPageDisplayStrategyKey.DEFAULT_STRATEGY;
  }
}
