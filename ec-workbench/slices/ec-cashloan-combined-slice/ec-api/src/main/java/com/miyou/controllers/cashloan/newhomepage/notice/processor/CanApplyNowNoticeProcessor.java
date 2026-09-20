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
public class CanApplyNowNoticeProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    noticeInfo
        .setRedirectUrl(null)
        .setColor(FontColor.GRAY)
        .setData(Lists.newArrayList(TT.gen("提交申请，查看您的资金额度")));
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.CAN_APPLY_NOW_NOTICE_INFO_PROCESSOR;
  }
}