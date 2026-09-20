package com.miyou.controllers.cashloan.repayment.calculate.infra;

import com.google.common.collect.ImmutableMap;
import com.yqg.overseas.common.utils.EcAsserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class RepaymentCalculatorFactory {
  @Autowired
  private List<RepaymentCalculator> calculatorList;

  private static ImmutableMap<RepaymentCalculateScene, RepaymentCalculator> REPAYMENT_CALCULATOR_MAP;


  @PostConstruct
  public void init() {
    ImmutableMap.Builder<RepaymentCalculateScene, RepaymentCalculator> builder = new ImmutableMap.Builder<>();
    for (RepaymentCalculator calculator : calculatorList) {
      builder.put(calculator.getScene(), calculator);
    }
    REPAYMENT_CALCULATOR_MAP = builder.build();
  }

  public static RepaymentCalculator getRepaymentCalculatorByScene(RepaymentCalculateScene scene) {
    EcAsserts.assertTrue(REPAYMENT_CALCULATOR_MAP.containsKey(scene), "Repayment scene {} is not supported!", scene);
    return REPAYMENT_CALCULATOR_MAP.get(scene);
  }
}
