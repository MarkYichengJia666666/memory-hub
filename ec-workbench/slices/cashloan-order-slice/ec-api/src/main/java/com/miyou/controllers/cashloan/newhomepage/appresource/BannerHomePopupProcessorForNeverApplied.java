package com.miyou.controllers.cashloan.newhomepage.appresource;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.AppResourceProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomeAppResourceResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BannerHomePopupProcessorForNeverApplied extends BannerHomePopupProcessor {

  @Override
  public AppResourceProcessorType getProcessorType() {
    return AppResourceProcessorType.BANNER_HOME_POP_UP_FOR_NEVER_APPLIED;
  }

  @Override
  public void process(HomeAppResourceResponse fieldsInfo, HomePageContext homePageContext) {
    if (BooleanUtils.isTrue(homePageContext.isNeedOpenAuthentication(homePageContext))) {
      log.info("jump to authentication page cause not construct banner home popup");
      return;
    }

    super.doProcess(fieldsInfo, homePageContext);
  }

}
