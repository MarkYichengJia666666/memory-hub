package com.miyou.controllers.cashloan.newhomepage.notice;

import com.miyou.controllers.cashloan.newhomepage.AbstractHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.newhomepage.notice.processor.AbstractNoticeInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HomePageNoticeResponseFactory extends AbstractHomePageResponseFactory<NoticeListResponse, HomepageNoticeProcessorType, AbstractNoticeInfoProcessor> {

  @Override
  public HomepageProcessorType getFactoryType() {
    return HomepageProcessorType.NOTICE_INFO;
  }


  @Override
  public void setValue(HomepageResponseV5 homepageResponseV5, NoticeListResponse value) {
    homepageResponseV5.setNotice(value);
  }

}
