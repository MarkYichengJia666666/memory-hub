package com.miyou.controllers.cashloan.newhomepage.processormap.paying;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

@Service
public class PayingSelector extends AbstractPayingSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.PAYING;
  }
}
