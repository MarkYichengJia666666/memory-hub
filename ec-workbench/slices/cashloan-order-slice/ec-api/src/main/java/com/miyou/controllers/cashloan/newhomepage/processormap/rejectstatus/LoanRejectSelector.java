package com.miyou.controllers.cashloan.newhomepage.processormap.rejectstatus;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

@Service
public class LoanRejectSelector extends AbstractRejectStatusProcessorSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.REJECTED;
  }
}
