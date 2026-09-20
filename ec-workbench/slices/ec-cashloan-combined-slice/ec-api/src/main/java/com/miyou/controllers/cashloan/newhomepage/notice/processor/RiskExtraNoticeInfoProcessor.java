package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.enums.IncreaseCreditsEntranceDisplayLocation;
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
public class RiskExtraNoticeInfoProcessor extends AbstractNoticeInfoProcessor{
  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    boolean needIncreaseCreditsEntrance = homepageCommonTool.shouldDisplayIncreaseCreditsEntrance(IncreaseCreditsEntranceDisplayLocation.NOTICE,
        homePageContext.getStatus(), homePageContext.getUserId(), homePageContext.getLoanAccountId(), homePageContext.getUserDeviceContextVO().getBuild());
    // 如果填完了就不显示
    if (!needIncreaseCreditsEntrance) {
      return;
    }
    String noticeContentByIncreaseCredits = homepageV5Config.getNoticeContentByIncreaseCredits();
    String redirectUrl = homepageV5Config.getRiskRedirectUrl();
    noticeInfo
        .setData(Lists.newArrayList(TT.gen(noticeContentByIncreaseCredits)))
        .setRedirectUrl(redirectUrl)
        .setColor(FontColor.GREEN);
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.RISK_EXTRA_NOTICE_INFO_PROCESSOR;
  }
}