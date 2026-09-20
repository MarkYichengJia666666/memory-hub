package com.miyou.controllers.cashloan.newhomepage.elementmodel;

import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.yqg.core.service.cashloan.homepage.config.HomepageV3ElementConfig;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.util.HomePageLeaveJudgeTool;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.util.HomePageV3CardTool;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.yqg.core.service.cashloan.HomepageV5Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public abstract class AbstractPageCardV3Processor implements IHomePageFieldProcessor<PageCardV3VO, PageCardV3ProcessorType> {
  @Autowired
  protected HomepageV3ElementConfig elementConfig;
  @Autowired
  protected HomePageV3CardTool homepageV3CardTool;
  @Autowired
  protected HomepageV5Config homepageV5Config;
  @Autowired
  protected HomePageLeaveJudgeTool homePageLeaveJudgeTool;
}
