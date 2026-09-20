package com.miyou.controllers.cashloan.newhomepage.processormap.orderbizcheck.fund;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

@Service
public class FundCheckSelector extends AbstractFundOrderCheckSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.FUND_CHECK;
  }
}
