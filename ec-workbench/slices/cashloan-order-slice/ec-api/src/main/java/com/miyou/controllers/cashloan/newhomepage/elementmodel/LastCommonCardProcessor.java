package com.miyou.controllers.cashloan.newhomepage.elementmodel;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.util.HomePageV3CardTool;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementModuleType;
import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.userflow.domain.home.service.IHomeSafeMoudelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LastCommonCardProcessor extends AbstractPageCardV3Processor {

  @Autowired
  private HomePageV3CardTool homepageV3CardTool;

  @Autowired
  private IHomeSafeMoudelService homeSafeMoudelService;

  @Override
  public void process(PageCardV3VO fieldsInfo, HomePageContext homePageContext) {
    String expGroup = homeSafeMoudelService.homePageSafetyModuleExpe(
        homePageContext.getUserDeviceContextVO(),
        homePageContext.getHomepageV3ExperimentContext() != null && homePageContext.getHomepageV3ExperimentContext().isV3(),
        homePageContext.getUserId());
    if (UserFlowConstants.EXPERIMENT_GROUP.equals(expGroup)) {
      fieldsInfo.addAllElements(ElementModuleType.MAIN_CARD, homepageV3CardTool.buildMainCardSafetyText());
      fieldsInfo.addAllElements(ElementModuleType.REGULATOR_CARD, homepageV3CardTool.buildRegulatorElements());
      fieldsInfo.addAllElements(ElementModuleType.MITRA_CARD, homepageV3CardTool.buildMitraElements());
    } else {
      fieldsInfo.addAllElements(ElementModuleType.TKB_CARD, homepageV3CardTool.buildTkbElements());
    }
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.LAST_COMMON_CARD;
  }
}
