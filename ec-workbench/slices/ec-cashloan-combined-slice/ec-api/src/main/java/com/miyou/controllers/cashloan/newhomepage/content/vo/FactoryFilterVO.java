package com.miyou.controllers.cashloan.newhomepage.content.vo;

import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;

public class FactoryFilterVO {
  private HomePageType homePageType;
  private HomepageVersion pageVersion;

  public FactoryFilterVO(HomePageContext homePageContext) {
    this.homePageType = homePageContext.getHomePageType();
    this.pageVersion = homePageContext.getHomepageV3ExperimentContext().getHomepageDisplayVersion();
  }


  public boolean isLevel1Page() {
    return homePageType == HomePageType.HOME_PAGE_FOR_LEVEL_1;
  }

  public boolean pageVersion3() {
    return pageVersion == HomepageVersion.V3;
  }
}
