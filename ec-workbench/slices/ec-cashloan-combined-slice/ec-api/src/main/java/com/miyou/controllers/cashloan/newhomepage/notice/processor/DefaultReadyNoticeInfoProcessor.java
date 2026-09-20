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
public class DefaultReadyNoticeInfoProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    if (CollectionUtils.isNotEmpty(noticeInfo.data)) {
      return;
    }
    if (CollectionUtils.isEmpty(homePageContext.getUserCashLoanOrderContext().getReadyOrderList())) {
      return;
    }
    noticeInfo
        .setData(Lists.newArrayList(TT.gen("按时还款，解锁高额低息产品")))
        .setRedirectUrl(homepageV5Config.getRepaymentOrderRedirectUrl(homePageContext.getUserCashLoanOrderContext().getReadyOrderList()))
        .setColor(FontColor.GREEN);
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.DEFAULT_READY_NOTICE_INFO_PROCESSOR;
  }
}
