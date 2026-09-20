package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.FontColor;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2024/12/4
 */
@Service
public class MultiLoanCreditsAcceptedNoticeInfoProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    if (CollectionUtils.isNotEmpty(noticeInfo.data)) {
      return;
    }
    String redirectUrl = homepageV5Config.getMultiLoanGuideRedirectUrl();
    noticeInfo
        .setData(Lists.newArrayList(TT.gen("您已解锁再借一笔机会，限时有效")))
        .setRedirectUrl(redirectUrl)
        .setColor(FontColor.GREEN);
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.MULTI_LOAN_CREDITS_ACCEPTED_NOTICE_INFO_PROCESSOR;
  }
}
