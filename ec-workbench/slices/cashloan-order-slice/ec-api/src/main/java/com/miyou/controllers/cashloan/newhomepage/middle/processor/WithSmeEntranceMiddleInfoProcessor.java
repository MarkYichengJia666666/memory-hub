package com.miyou.controllers.cashloan.newhomepage.middle.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageMiddleProcessorType;
import com.miyou.controllers.cashloan.response.v5.middle.MiddleListResponse;
import com.yqg.core.service.cashloan.homepage.middle.MiddleConfigVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WithSmeEntranceMiddleInfoProcessor extends AbstractMiddleInfoProcessor {

  @Override
  public void process(MiddleListResponse middleListResponse, HomePageContext homePageContext) {
    String middleMessage = super.getMiddleMessage(homePageContext, homepageV5Config.getMiddleMessageWithSmeEntrance(), homepageV5Config.getMiddleMessageWithSmeEntranceForNewHomepage());

    List<MiddleConfigVO> configVOS = homepageMiddleTool.filterConfigVoList(super.convertJsonToClass(middleMessage), homePageContext.getLoanAccountId(), homePageContext.getStatus(), homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());

    List<MiddleConfigVO> middleConfigVOList = homepageMiddleTool.handleInviteUrlToMiddleVo(configVOS, homePageContext.getUserId());

    middleListResponse
        .setData(super.convertMiddleConfigVOsToRespList(middleConfigVOList));
  }


  @Override
  public HomepageMiddleProcessorType getProcessorType() {
    return HomepageMiddleProcessorType.WITH_SME_ENTRANCE_MIDDLE_INFO_PROCESSOR;
  }
}