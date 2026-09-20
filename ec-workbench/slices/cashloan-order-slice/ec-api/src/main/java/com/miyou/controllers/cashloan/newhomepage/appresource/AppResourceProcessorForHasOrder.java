package com.miyou.controllers.cashloan.newhomepage.appresource;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.AppResourceProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomeAppResourceResponse;
import com.yqg.core.service.cashloan.homepage.JumpBillPageTool;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 有账单资源位处理
 */
@Service
@Slf4j
public class AppResourceProcessorForHasOrder extends BannerHomePopupProcessor{

  @Autowired
  private JumpBillPageTool jumpBillPageTool;

  @Override
  public AppResourceProcessorType getProcessorType() {
    return AppResourceProcessorType.BANNER_HOME_POP_UP_FOR_HAS_ORDER;
  }

  @Override
  public void process(HomeAppResourceResponse fieldsInfo, HomePageContext homePageContext) {
    if (homePageContext.getHomePageType() == HomePageType.HOME_PAGE_FOR_LEVEL_1
        && (jumpBillPageTool.canJumpBillPageForNotOverdue(homePageContext.getUserId()))
    ) {
      log.info("jump to bill page cause not construct banner home popup");
      return;
    }

    super.doProcess(fieldsInfo, homePageContext);
  }

}
