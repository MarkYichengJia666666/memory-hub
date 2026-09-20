package com.miyou.controllers.cashloan.newhomepage.processormap.orderbizcheck.debt;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

@Service
public class DebtCheckSelector extends AbstractDebtOrderCheckProcessor {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.DEBT_CHECK;
  }
}
