package com.miyou.controllers.cashloan.newhomepage.toparea;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageTopAreaProcessorType;
import com.miyou.controllers.cashloan.newhomepage.toparea.processor.AbstractTopAreaProcessor;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.toparea.TopAreaResponse;
import org.springframework.stereotype.Service;

@Service
public class TopAreaResponseFactory  extends AbstractHomePageResponseFactory<TopAreaResponse, HomepageTopAreaProcessorType, AbstractTopAreaProcessor> {
  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.TOP_AREA;
  }

  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, TopAreaResponse value) {
    homepageResponseV5.setTopArea(value);
  }
}
