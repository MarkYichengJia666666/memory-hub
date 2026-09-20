package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.FontColor;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2024/12/4
 */
@Service
public class LoanCreditsDecreaseNoticeInfoProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    noticeInfo
        .setRedirectUrl(null)
        .setColor(FontColor.GRAY)
        .setData(Lists.newArrayList(TT.gen("您获得一次激活新额度的机会")));
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.LOAN_CREDITS_DECREASE_NOTICE_INFO_PROCESSOR;
  }
}