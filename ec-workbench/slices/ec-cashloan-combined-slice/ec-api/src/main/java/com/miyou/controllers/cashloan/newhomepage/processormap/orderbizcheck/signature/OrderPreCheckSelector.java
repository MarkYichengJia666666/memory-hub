package com.miyou.controllers.cashloan.newhomepage.processormap.orderbizcheck.signature;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

@Service
public class OrderPreCheckSelector extends AbstractSignatureOrderCheckSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.ORDER_PRE_CHECK;
  }
}
