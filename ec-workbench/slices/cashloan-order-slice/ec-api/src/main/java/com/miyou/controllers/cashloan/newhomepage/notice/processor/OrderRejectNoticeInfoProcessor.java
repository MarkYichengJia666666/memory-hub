package com.miyou.controllers.cashloan.newhomepage.notice.processor;

import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageNoticeProcessorType;
import com.miyou.controllers.cashloan.response.v5.notice.FontColor;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.yqg.ec.common.enums.order.CashLoanOrderRejectReason;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2024/12/4
 */
@Service
public class OrderRejectNoticeInfoProcessor extends AbstractNoticeInfoProcessor{

  @Override
  public void doProcessorForNewHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {
    if (!checkPreCondition(homePageContext)) {
      return;
    }

    CashLoanOrderRejectReason cashLoanOrderRejectReason = CashLoanOrderRejectReason.valueOf(homePageContext.getUserCashLoanOrderContext().getOrderRejectReasonVO().info);
    if (cashLoanOrderRejectReason == CashLoanOrderRejectReason.CANCEL_EXPIRED_CHECK_ORDER) {
      noticeInfo.setData(Lists.newArrayList(TT.gen("您的借款协议未确认，订单超时被取消，请重新借款。")));
    }
    if (cashLoanOrderRejectReason == CashLoanOrderRejectReason.RISK_REJECT_ORDER) {
      noticeInfo.setData(Lists.newArrayList(TT.gen("订单取消，请重新确认下单信息。")));
    }
    noticeInfo.setColor(FontColor.GRAY);
    noticeInfo.setRedirectUrl(null);
  }

  @Override
  public void doProcessorForOldHomepage(NoticeListResponse noticeInfo, HomePageContext homePageContext) {

  }

  @Override
  protected HomepageNoticeProcessorType getNoticeProcessorType() {
    return HomepageNoticeProcessorType.ORDER_REJECT_NOTICE_INFO_PROCESSOR;
  }

  private boolean checkPreCondition(HomePageContext homePageContext) {
    if (homePageContext.getUserCashLoanOrderContext() == null) {
      return false;
    }
    if (homePageContext.getUserCashLoanOrderContext().getLatestOrderVO() == null) {
      return false;
    }
    if (homePageContext.getUserCashLoanOrderContext().getLatestOrderVO().status != CashLoanOrderStatus.REJECT) {
      return false;
    }
    return homePageContext.getUserCashLoanOrderContext().getOrderRejectReasonVO() != null;
  }
}
