package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.FontColor;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2024/12/4
 */
@Service
@Slf4j
public class LowInterestProductNoticeInfoProcessor extends AbstractNoticeInfoProcessor{
  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    if (CollectionUtils.isNotEmpty(noticeInfo.data)) {
      return;
    }
    if (!homePageContext.getUserProductVO().isHasLowInterestProduct()) {
      return;
    }
    noticeInfo
        .setData(Lists.newArrayList(TT.gen("已为您提供优惠产品，限时有效")))
        .setRedirectUrl(null)
        .setColor(FontColor.GRAY);
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.LOW_INTEREST_PRODUCT_NOTICE_INFO_PROCESSOR;
  }
}
