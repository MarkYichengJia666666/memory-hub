package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.AlertProcessorType;

import java.util.List;

public interface IAlertProcessorSelector extends IStatusProcessorSelector{
  List<AlertProcessorType> getAlertProcessorList();
}