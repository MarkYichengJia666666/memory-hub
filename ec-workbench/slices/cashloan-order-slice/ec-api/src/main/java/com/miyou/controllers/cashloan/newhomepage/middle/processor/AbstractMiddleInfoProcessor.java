package com.miyou.controllers.cashloan.newhomepage.middle.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleListResponse;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleResponse;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.controllers.cashloan.utilities.HomepageMiddleTool;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.middle.MiddleConfigVO;
import com.yqg.ec.common.serialization.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractMiddleInfoProcessor implements IHomePageFieldProcessor<MiddleListResponse, HomepageMiddleProcessorType> {

  @Autowired
  protected HomepageMiddleTool homepageMiddleTool;
  @Autowired
  protected HomepageContentTool homepageContentTool;
  @Autowired
  protected HomepageV5Config homepageV5Config;


  protected String getMiddleMessage(HomePageContext homePageContext, String middleMessage, String middleMessageForNewHomepage) {
    if (homePageContext.newHomePageUI() && StringUtils.isNotBlank(middleMessageForNewHomepage)) {
      return middleMessageForNewHomepage;
    }
    return middleMessage;
  }

  protected List<MiddleResponse> convertMiddleConfigVOsToRespList(List<MiddleConfigVO> data) {
    if (data == null || data.isEmpty()) {
      return Collections.emptyList();
    }

    return data.stream()
        .map(MiddleResponse::from)
        .collect(Collectors.toList());
  }

  protected List<MiddleConfigVO> convertJsonToClass(String middleMessage) {
    if (middleMessage == null || middleMessage.isEmpty()) {
      return Collections.emptyList();
    }

    return JsonUtils.from(middleMessage, new TypeReference<List<MiddleConfigVO>>() {
    });
  }
}