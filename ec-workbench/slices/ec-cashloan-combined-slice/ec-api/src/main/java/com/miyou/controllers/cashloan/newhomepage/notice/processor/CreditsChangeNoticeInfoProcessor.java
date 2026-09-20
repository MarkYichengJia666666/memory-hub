package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2024/12/5
 */
@Service
public class CreditsChangeNoticeInfoProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    noticeInfo
        .setData(Lists.newArrayList(TT.gen("您的借款额度被更新，请重新确认下单信息。")));
  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }


  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.CREDITS_CHANGE_NOTICE_INFO_PROCESSOR;
  }
}
