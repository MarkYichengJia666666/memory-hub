package com.miyou.controllers.cashloan.newhomepage.appresource;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.AppResourceProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomeAppResourceResponse;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.activity.HomeInterestFreeCardService;
import com.yqg.core.service.cashloan.homepage.JumpBillPageTool;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BannerHomePopupProcessorForCanCreateOrder extends BannerHomePopupProcessor {

  @Autowired
  private JumpBillPageTool jumpBillPageTool;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private HomeInterestFreeCardService homeInterestFreeCardService;

  @Override
  public AppResourceProcessorType getProcessorType() {
    return AppResourceProcessorType.BANNER_HOME_POP_UP_FOR_CAN_CREATE_ORDER;
  }


  @Override
  public void process(HomeAppResourceResponse fieldsInfo, HomePageContext homePageContext) {
    if (homePageContext.getHomePageType() == HomePageType.HOME_PAGE_FOR_LEVEL_1
        && (homePageContext.hitJumpLevel2Strategy()
        || jumpBillPageTool.canJumpBillPageForNotOverdue(homePageContext.getUserId()))
    ) {
      log.info("jump to bill page cause not construct banner home popup");
      return;
    }
    super.doProcess(fieldsInfo, homePageContext);
  }

}
