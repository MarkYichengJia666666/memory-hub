package com.miyou.controllers.cashloan.newhomepage.elementmodel;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.commonelement.CommonElement;
import org.springframework.stereotype.Component;

@Component
public class CommonCardProcessor extends AbstractPageCardV3Processor {

  @Override
  public void process(PageCardV3VO fieldsInfo, HomePageContext homePageContext) {
    fieldsInfo.addElementForMainCard(CommonElement.TOP_TIP);
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.COMMON_TYPE;
  }
}
