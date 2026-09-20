package com.miyou.controllers.cashloan.newhomepage.processormap.orderbizcheck.grab;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

@Service
public class ReloanGrabCheckSelector extends AbstractGrabOrderCheckSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.RELOAN_GRAB_CHECK;
  }
}
