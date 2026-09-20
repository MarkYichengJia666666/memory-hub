package com.miyou.controllers.cashloan.newhomepage.homepagecontext;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor.HomePageContextProcessorType;

/**
 * HomeContext处理器
 */
public interface IHomepageHomeContextProcessor {
  void processHomeContext(HomePageContext homePageContext);

  HomePageContextProcessorType getType();
}
