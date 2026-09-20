package com.miyou.controllers.cashloan.newhomepage.processormap;

import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;

import java.util.List;

public interface INoticeProcessorSelector extends IStatusProcessorSelector{
  List<HomepageNoticeProcessorType> getNoticeProcessorList();
}