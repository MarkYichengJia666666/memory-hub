package com.miyou.controllers.cashloan.newhomepage.processormap.orderbizcheck.fundpayout;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

@Service
public class ReloanFundPayingSelector extends AbstractFundPayoutOrderCheckSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.RELOAN_FUND_PAYING;
  }
}
