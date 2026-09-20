package com.miyou.controllers.cashloan.newhomepage.alert.processor;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.enums.AlertProcessorType;
import com.miyou.controllers.cashloan.response.v5.alert.AlertResponse;

public abstract class AbstractAlertInfoProcessor implements IHomePageFieldProcessor<AlertResponse, AlertProcessorType> {


  @Override
  public AlertProcessorType getProcessorType() {
    return getAlertProcessorType();
  }

  protected abstract AlertProcessorType getAlertProcessorType();



}