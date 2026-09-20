package com.miyou.controllers.cashloan.newhomepage.processormap.orderbizcheck.grab;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

@Service
public class GrabCheckSelector extends AbstractGrabOrderCheckSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.GRAB_CHECK;
  }
}
