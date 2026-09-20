package com.miyou.controllers.cashloan.newhomepage.homepageresponseprocessor;

import com.miyou.controllers.cashloan.newhomepage.IHomepageResponsePostProcessor;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 在老首页中，Notice和头图是不共存的，优先有头图
 */
@Component
public class NoticeTopAreaPostProcessor implements IHomepageResponsePostProcessor {
  @Override
  public void afterProcess(HomepageResponseV5 responseV5, HomePageContext homePageContext) {
    if (homePageContext.newHomePageUI()) {
      return;
    }
    if (homePageContext.getHomePageType() != HomePageType.HOME_PAGE_FOR_LEVEL_1) {
      return;
    }
    if (responseV5.getTopArea() != null && responseV5.getTopArea().show) {
      responseV5.setNotice(null);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
