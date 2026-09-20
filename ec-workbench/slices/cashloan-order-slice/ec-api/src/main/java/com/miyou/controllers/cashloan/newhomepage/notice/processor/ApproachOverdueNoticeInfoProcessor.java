package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.FontColor;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.miyou.controllers.cashloan.response.v5.order.OrderTipResponse;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chenxianrui
 * @date 2024/12/4
 */
@Service
public class ApproachOverdueNoticeInfoProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    if (CollectionUtils.isNotEmpty(noticeInfo.data)) {
      return;
    }

    List<OrderInstalment> readyOrderList = homePageContext.getUserCashLoanOrderContext().getReadyOrderList();
    int dayThreshold = homepageV5Config.getBillingOrderDayThreshold();
    OrderTipResponse tip = OrderTipResponse.from(readyOrderList, dayThreshold);
    if (tip.repayingOrderCount <= 0) {
      return;
    }

    Long latestBillingDate = readyOrderList.stream().map(OrderInstalment::getLatestBillingDate).min(Long::compare)
        .orElse(null);
    int minDiffDay = Clock.getAbsCalenderDaysBetween(Clock.now(), latestBillingDate, homePageContext.getUserDeviceContextVO().getSdkType().getTimeZone());
    noticeInfo
        .setData(Lists.newArrayList(TT.gen("您的订单{0}天后要到期啦，马上还款！", minDiffDay == 0 ? 1 : minDiffDay)))
        .setRedirectUrl(homepageV5Config.getRepaymentOrderRedirectUrl(readyOrderList))
        .setColor(FontColor.ORANGE);
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.APPROACH_OVERDUE_NOTICE_INFO_PROCESSOR;
  }
}