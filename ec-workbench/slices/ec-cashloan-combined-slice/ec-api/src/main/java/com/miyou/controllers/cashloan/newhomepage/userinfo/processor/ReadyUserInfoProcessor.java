package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.ReserveLoanCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReadyUserInfoProcessor extends AbstractUserInfoProcessor {

  @Override
  public void process(UserResponse fieldsInfo, HomePageContext homePageContext) {
    HomeDisplayStrategy homeDisplayStrategy = abTestVersionConfigService.getReserveLoanStyle(homePageContext.getUserDeviceContextVO().getBuild(), homePageContext.getUserId());
    ReserveLoanCardInfo reserveLoanCardInfo = homepageContentTool.buildReserveLoanCardInfo(homeDisplayStrategy);
    fieldsInfo
          .setReserveLoanCardInfo(reserveLoanCardInfo);
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.READY_USER_INFO;
  }

}
