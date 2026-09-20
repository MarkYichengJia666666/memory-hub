package com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.IHomepageHomeContextProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderPageAmountInputSimplifyContextProcessor implements IHomepageHomeContextProcessor {

  @Override
  public void processHomeContext(HomePageContext homePageContext) {
    homePageContext.buildAndSetOrderPageExperimentContext();
  }

  @Override
  public HomePageContextProcessorType getType() {
    return HomePageContextProcessorType.ORDER_PAGE_AMOUNT_INPUT_SIMPLIFY;
  }

}
