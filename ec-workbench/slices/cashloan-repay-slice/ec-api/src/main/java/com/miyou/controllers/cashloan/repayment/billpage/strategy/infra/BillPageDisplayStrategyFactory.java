package com.miyou.controllers.cashloan.repayment.billpage.strategy.infra;

import com.google.common.collect.ImmutableMap;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import com.yqg.overseas.common.utils.EcAsserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;


@Component
public class BillPageDisplayStrategyFactory {

  @Autowired
  private List<AbstractBillPageDisplayStrategy> strategyList;

  private static ImmutableMap<BillPageDisplayStrategyKey, AbstractBillPageDisplayStrategy> BILL_PAGE_DISPLAY_STRATEGY_MAP;

  @PostConstruct
  public void init() {
    ImmutableMap.Builder<BillPageDisplayStrategyKey, AbstractBillPageDisplayStrategy> builder = new ImmutableMap.Builder<>();
    for (AbstractBillPageDisplayStrategy strategy : strategyList) {
      builder.put(strategy.getKey(), strategy);
    }
    BILL_PAGE_DISPLAY_STRATEGY_MAP = builder.build();
  }

  public static AbstractBillPageDisplayStrategy getStrategyByKey(BillPageDisplayStrategyKey displayStrategy) {
    EcAsserts.assertTrue(BILL_PAGE_DISPLAY_STRATEGY_MAP.containsKey(displayStrategy), "BillPageDisplayStrategy {} is not supported!", displayStrategy);
    return BILL_PAGE_DISPLAY_STRATEGY_MAP.get(displayStrategy);
  }
}
