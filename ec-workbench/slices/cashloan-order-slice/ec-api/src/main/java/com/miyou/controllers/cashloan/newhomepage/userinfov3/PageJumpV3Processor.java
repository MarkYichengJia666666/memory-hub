package com.miyou.controllers.cashloan.newhomepage.userinfov3;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageUserInfoV3Response;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.PageJumpInfoV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author: lihancock
 * @Email: wenyaoli@fintopia.tech
 * @Date: 2025/7/28 16:59
 */
@Slf4j
@Component
public class PageJumpV3Processor extends AbstractHomepageUserInfoV3Processor {

  @Override
  public void process(PageUserInfoV3Response fieldsInfo, HomePageContext homePageContext) {
    try {
      Boolean needOpen = homePageContext.isNeedOpenAuthentication(homePageContext);
      if (needOpen != null) {
        fieldsInfo.setUserPage(PageJumpInfoV3.builder().needOpenAuthentication(needOpen).build());
      }
    } catch (Exception e) {
      log.warn("PageJumpV3Processor process error, ", e);
    }
  }

  @Override
  public HomepageUserInfoV3ProcessorType getProcessorType() {
    return HomepageUserInfoV3ProcessorType.PAGE_JUMP_V3;
  }
}
