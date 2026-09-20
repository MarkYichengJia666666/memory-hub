package com.miyou.controllers.cashloan.newhomepage.elementmodel;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageCardInfoV3Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ModelElementResponseFactory extends AbstractHomePageResponseFactory<PageCardV3VO, PageCardV3ProcessorType, AbstractPageCardV3Processor> {

  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, PageCardV3VO cardV3VO) {
    PageCardInfoV3Response pageCardInfoV3Response = new PageCardInfoV3Response();
    pageCardInfoV3Response.moduleCards = cardV3VO.getModelCards();
    if (MapUtils.isEmpty(pageCardInfoV3Response.moduleCards)) {
      log.error("No main card element found when setting value to homepage response");
    }
    homepageResponseV5.setHomepageCardInfoV3Response(pageCardInfoV3Response);
  }

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.PAGE_CARD_INFO_V3;
  }
}
