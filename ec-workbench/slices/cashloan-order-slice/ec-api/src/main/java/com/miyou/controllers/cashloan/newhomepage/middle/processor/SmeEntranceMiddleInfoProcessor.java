package com.miyou.controllers.cashloan.newhomepage.middle.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleListResponse;
import com.yqg.core.service.cashloan.homepage.middle.MiddleConfigVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmeEntranceMiddleInfoProcessor extends AbstractMiddleInfoProcessor {

  @Override
  public void process(MiddleListResponse middleListResponse, HomePageContext homePageContext) {
    String middleMessage = super.getMiddleMessage(homePageContext, homepageV5Config.getMiddleMessageWithoutSmeEntrance(), homepageV5Config.getMiddleMessageWithoutSmeEntranceForNewHomepage());

    List<MiddleConfigVO> middleList = homepageMiddleTool.handleInviteUrlToMiddleVo(super.convertJsonToClass(middleMessage), homePageContext.getUserId());

    middleListResponse
        .setData(super.convertMiddleConfigVOsToRespList(middleList));
  }


  @Override
  public HomepageMiddleProcessorType getProcessorType() {
    return HomepageMiddleProcessorType.SME_ENTRANCE_MIDDLE_INFO_PROCESSOR;
  }
}