package com.miyou.controllers.cashloan.newhomepage.toparea.processor;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageTopAreaProcessorType;
import com.miyou.controllers.cashloan.response.v5.toparea.TopAreaResponse;
import com.miyou.controllers.cashloan.utilities.HomePageTopAreaTool;
import com.miyou.controllers.cashloan.utilities.HomepageCommonTool;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.config.HomePageTopAreaConfig;
import com.yqg.core.service.cashloan.vo.enums.DiscountDetailTopAreaVipUIDisplayStrategy;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractTopAreaProcessor implements IHomePageFieldProcessor<TopAreaResponse, HomepageTopAreaProcessorType> {
  @Autowired
  protected HomepageCommonTool homepageCommonTool;
  @Autowired
  protected HomePageTopAreaTool homePageTopAreaTool;
  @Autowired
  protected HomepageV5Config homepageV5Config;
  @Autowired
  protected HomePageTopAreaConfig homePageTopAreaConfig;
  @Autowired
  protected HomepageContentTool homepageContentTool;


  @Override
  public void process(TopAreaResponse fieldsInfo, HomePageContext homePageContext) {
    DiscountDetailTopAreaVipUIDisplayStrategy strategy = homepageCommonTool.getDiscountDetailTopAreaVipUIDisplayStrategy(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());
    if (strategy.removeTopArea()) {
      return;
    }
    doProcessor(fieldsInfo, homePageContext);
  }

  abstract void doProcessor(TopAreaResponse fieldsInfo, HomePageContext homePageContext);

}
