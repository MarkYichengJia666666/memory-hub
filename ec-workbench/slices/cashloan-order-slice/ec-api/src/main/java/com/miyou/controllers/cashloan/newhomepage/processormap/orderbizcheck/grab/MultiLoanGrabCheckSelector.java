package com.miyou.controllers.cashloan.newhomepage.processormap.orderbizcheck.grab;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.enums.AlertProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.IAlertProcessorSelector;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MultiLoanGrabCheckSelector extends AbstractGrabOrderCheckSelector implements IAlertProcessorSelector {
  @Override
  public IDNHomepageLoanStatusV5 getStatus() {
    return IDNHomepageLoanStatusV5.MULTI_LOAN_GRAB_CHECK;
  }

  @Override
  public List<AlertProcessorType> getAlertProcessorList() {
    return Lists.newArrayList(
        AlertProcessorType.LENDING_GUIDANCE_ALERT_PROCESSOR,
        AlertProcessorType.REPAYMENT_GUIDANCE_ALERT_PROCESSOR
    );
  }
}
