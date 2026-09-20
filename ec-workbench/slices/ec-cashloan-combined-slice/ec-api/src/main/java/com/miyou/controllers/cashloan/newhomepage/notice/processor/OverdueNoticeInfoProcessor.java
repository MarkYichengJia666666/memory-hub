package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.FontColor;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.miyou.controllers.cashloan.response.v5.order.OrderTipResponse;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chenxianrui
 * @date 2024/12/4
 */
@Service
public class OverdueNoticeInfoProcessor  extends AbstractNoticeInfoProcessor{

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
    if (!tip.isOverdue()) {
      return;
    }
    noticeInfo
        .setData(Lists.newArrayList(TT.gen("您的订单已逾期，快去还款，避免影响后续借款！")))
        .setRedirectUrl(homepageV5Config.getRepaymentOrderRedirectUrl(readyOrderList))
        .setColor(FontColor.RED);
  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.OVERDUE_NOTICE_INFO_PROCESSOR;
  }
}