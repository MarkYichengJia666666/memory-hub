package com.miyou.controllers.cashloan.newhomepage.middle.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleListResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class EmptyMiddleInfoProcessor extends AbstractMiddleInfoProcessor {

  @Override
  public void process(MiddleListResponse middleListResponse, HomePageContext homePageContext) {
    middleListResponse
        .setData(Collections.emptyList());
  }


  @Override
  public HomepageMiddleProcessorType getProcessorType() {
    return HomepageMiddleProcessorType.EMPTY_MIDDLE_INFO_PROCESSOR;
  }
}