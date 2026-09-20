package com.miyou.controllers.cashloan.utilities;


import com.miyou.controllers.cashloan.enums.IncreaseCreditsEntranceDisplayLocation;
import com.miyou.controllers.cashloan.response.v5.notice.FontColor;
import com.miyou.controllers.cashloan.response.v5.notice.NoticeListResponse;
import com.miyou.controllers.cashloan.response.v5.notice.SingleNoticeResponse;
import com.miyou.controllers.cashloan.response.v5.order.OrderTipResponse;
import com.miyou.controllers.cashloan.response.v5.toparea.TopAreaResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.CashLoanOrderLimitService;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.OrderOverLimitLevel;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.UserOrderLimitType;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.order.CashLoanOrderRejectReason;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class HomepageNoticeTool {
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private HomepageCommonTool homepageCommonTool;
  @Autowired
  private HomePageCacheService homePageCacheService;
  @Autowired
  private CashLoanOrderLimitService cashLoanOrderLimitService;


  public NoticeListResponse getNotice(IDNHomepageLoanStatusV5 status,
                                      LoanApiViewerContext viewerContext,
                                      HomepageUserParamsVO paramsVO,
                                      boolean hasLowInterestProduct,
                                      UserResponse userInfo,
                                      TopAreaResponse topArea) {
    try {

      if (paramsVO.newHomePageUI()) {
        return getNoticeForNewHomePage(status, paramsVO, userInfo);
      }
      //有头图就不展示notice
      if (topArea != null) {
        return null;
      }
      SDKType sdkType = viewerContext.sdkType;
      NoticeListResponse noticeListResponse = getNoticeListResponseByStatus(status, viewerContext, paramsVO, hasLowInterestProduct, sdkType);
      NoticeListResponse res = Optional.ofNullable(noticeListResponse).orElse(NoticeListResponse.empty());
      // 多头三家限制，需要重置轮播
      res = buildOrderLimitNoticeResponse(userInfo, res, status);
      return res;
    } catch (Exception e) {
      log.error("getNotice error, userId:{}, build:{}, sourceType:{}", viewerContext.userId, viewerContext.build, viewerContext.sourceType, e);
      return null;
    }
  }

  private NoticeListResponse getNoticeForNewHomePage(IDNHomepageLoanStatusV5 status, HomepageUserParamsVO paramsVO, UserResponse userInfo) {
    switch (status) {
      case RELOAN_INIT:
      case ACCEPTED:
      case MULTI_LOAN_CREDITS_ACCEPTED:
        return getOrderRejectNotice(paramsVO);
      case PAYOUT_FAILED:
        return NoticeListResponse.fromSingle(TT.gen("订单打款失败，重新下单时请确认你的收款银行卡信息是否正确。"));
      case RELOAN_CREDITS_DECREASE:
      case LOAN_CREDITS_DECREASE:
        return getCreditsChangeNotice();
    }
    return null;
  }

  private NoticeListResponse getOrderRejectNotice(HomepageUserParamsVO paramsVO) {
    if (paramsVO.latestOrderVO == null) {
      return null;
    }
    if (paramsVO.latestOrderVO.status != CashLoanOrderStatus.REJECT) {
      return null;
    }
    if (paramsVO.orderRejectReasonVO == null) {
      return null;
    }
    CashLoanOrderRejectReason cashLoanOrderRejectReason = CashLoanOrderRejectReason.valueOf(paramsVO.orderRejectReasonVO.info);
    if (cashLoanOrderRejectReason == CashLoanOrderRejectReason.CANCEL_EXPIRED_CHECK_ORDER) {
      return NoticeListResponse.fromSingle(TT.gen("您的借款协议未确认，订单超时被取消，请重新借款。"));
    }
    if (cashLoanOrderRejectReason == CashLoanOrderRejectReason.RISK_REJECT_ORDER) {
      return NoticeListResponse.fromSingle(TT.gen("订单取消，请重新确认下单信息。"));
    }
    return null;
  }

  private NoticeListResponse getCreditsChangeNotice() {
    return NoticeListResponse.fromSingle(TT.gen("您的借款额度被更新，请重新确认下单信息。"));
  }

  @Nullable
  private NoticeListResponse getNoticeListResponseByStatus(IDNHomepageLoanStatusV5 status,
                                                           LoanApiViewerContext viewerContext,
                                                           HomepageUserParamsVO paramsVO,
                                                           boolean hasLowInterestProduct,
                                                           SDKType sdkType) {
    switch (status) {
      case NOT_LOGIN:
      case NEVER_APPLIED:
        BigDecimal principalThreshold = homepageV5Config.getPrincipalThreshold();
        return homePageCacheService.getRecentlyOrdersNotice(sdkType, principalThreshold);
      case IN_REVIEW:
      case RELOAN_IN_REVIEW:
      case CAN_REAPPLY_IN_FUTURE:
      case FUND_PAYING:
      case RELOAN_FUND_PAYING:
      case MULTI_LOAN_FUND_PAYING:
      case PAYING:
      case RELOAN_PAYING:
      case MULTI_LOAN_PAYING:
      case MULTI_LOAN_IN_REVIEW:
      case RELOAN_CALC_CREDITS_IN_REVIEW:
      case MULTI_LOAN_CALC_CREDITS_IN_REVIEW:
      case FUND_CHECK:
      case GRAB_CHECK:
      case RELOAN_FUND_CHECK:
      case RELOAN_GRAB_CHECK:
      case MULTI_LOAN_FUND_CHECK:
      case MULTI_LOAN_GRAB_CHECK:
      case REJECTED:
      case RELOAN_REJECTED:
      case IMAGE_REVIEW_REJECTED:
      case DEBT_CHECK:
      case RELOAN_DEBT_CHECK:
      case MULTI_LOAN_DEBT_CHECK:
      case ORDER_PRE_CHECK:
      case RELOAN_ORDER_PRE_CHECK:
      case MULTI_LOAN_ORDER_PRE_CHECK:
      case CANCELLED:
      case REUPLOAD_FINISHED:
      case RELOAN_READY:
      case ACCEPTED_BUT_CAN_NOT_LOAN:
      case RELOAN_INIT:
      case CALC_CREDITS_EXPIRED:
      case RELOAN_CREDITS_DECREASE:
      case MINIMALIST_ACCEPT:
      case MINIMALIST_IN_REVIEW:
      case WAITING_SUPPLEMENT:
      case NEED_SUPPLEMENT:
      case FINISH_SUPPLEMENT:
        return NoticeListResponse.empty();
      case READY:
        return getDefaultReadyOrderNotice(paramsVO.readyOrderList);
      case ACCEPTED:
        return getExtraDataOrLowInterestNoticeOrNull(status, viewerContext, paramsVO, hasLowInterestProduct);
      case LOAN_CREDITS_DECREASE:
        return getLowInterestProductNoticeOrNull(hasLowInterestProduct);
      case CAN_REAPPLY_NOW:
      case RELOAN_CAN_REAPPLY_NOW:
      case INCREASE_REVIEW_NEVER_REAPPLIED:
      case INCREASE_REVIEW_REJECT:
        return getCanApplyNowNotice();
      case PAYOUT_FAILED:
        return getPayoutFailedNotice();
      case MULTI_LOAN_CREDITS_ACCEPTED:
        return getRepaymentOrProductNoticeInMuliLoanAccepted(sdkType, paramsVO.readyOrderList, hasLowInterestProduct);
      case OVERDUE:
      case RELOAN_OVERDUE:
        return getRepaymentNotice(sdkType, status, viewerContext, paramsVO);
      case LOAN_CREDITS_EXPIRED:
        return getLoanCreditsExpiredNotice();
      case MULTI_LOAN_INIT:
        return getMultiLoanInitNotice(sdkType, paramsVO.readyOrderList);
      default:
        throw EcException.error("unhandled status->" + status.name());
    }
  }

  private NoticeListResponse buildOrderLimitNoticeResponse(UserResponse userInfo,
                                                           NoticeListResponse res,
                                                           IDNHomepageLoanStatusV5 status) {
    // 可下单状态则需要考虑多头notice
    NoticeListResponse orderLimitNoticeResponse = getOrderLimitNoticeResponse(userInfo.userOrderLimitType, userInfo.orderLimitLevel);
    if (CollectionUtils.isNotEmpty(orderLimitNoticeResponse.data)) {
      return orderLimitNoticeResponse;
    }
    if (status.canCreateOrder()) {
      String antiFraudCarouselText = homepageV5Config.getAntiFraudCarouselText();
      if (StringUtils.isNotBlank(antiFraudCarouselText)) {
        res.data.add(TT.gen(antiFraudCarouselText));
      }
    }
    return res;
  }

  private NoticeListResponse getOrderLimitNoticeResponse(UserOrderLimitType limitType, OrderOverLimitLevel orderOverLimitLevel) {
    String promptJsonInfo = cashLoanOrderLimitService.getPromptJsonInfo(limitType, orderOverLimitLevel);
    if (promptJsonInfo == null) {
      return NoticeListResponse.empty();
    }
    SingleNoticeResponse noticeResponse = JsonUtils.fromOrNull(promptJsonInfo, SingleNoticeResponse.class);
    if (noticeResponse == null) {
      return NoticeListResponse.empty();
    }
    if (StringUtils.isBlank(noticeResponse.noticeData)) {
      return NoticeListResponse.empty();
    }
    return NoticeListResponse.fromSingle(TT.gen(noticeResponse.noticeData), noticeResponse.noticeRedirectUrl, noticeResponse.noticeColor);
  }

  private NoticeListResponse getLoanCreditsExpiredNotice() {
    TT message = TT.gen("您获得一次激活新额度的机会");
    return NoticeListResponse.fromSingle(message);
  }

  private NoticeListResponse getRepaymentNotice(SDKType sdkType, IDNHomepageLoanStatusV5 status, LoanApiViewerContext viewerContext, HomepageUserParamsVO paramsVO) {
    NoticeListResponse riskExtraDataNotice = getRiskExtraDataNotice(status, viewerContext.userId, paramsVO.accountVO.id, viewerContext.build);
    if (riskExtraDataNotice != null) {
      return riskExtraDataNotice;
    }
    return getReadyOrdersNotice(sdkType, paramsVO.readyOrderList);
  }

  private NoticeListResponse getRiskExtraDataNotice(IDNHomepageLoanStatusV5 status, Long userId, Long loanAccountId, Long build) {
    Boolean needIncreaseCreditsEntrance = homepageCommonTool.shouldDisplayIncreaseCreditsEntrance(IncreaseCreditsEntranceDisplayLocation.NOTICE, status, userId, loanAccountId, build);
    // 如果填完了就不显示
    if (!needIncreaseCreditsEntrance) {
      return null;
    }
    String noticeContentByIncreaseCredits = homepageV5Config.getNoticeContentByIncreaseCredits();
    String redirectUrl = homepageV5Config.getRiskRedirectUrl();
    return NoticeListResponse.fromSingle(TT.gen(noticeContentByIncreaseCredits), redirectUrl, FontColor.GREEN);
  }

  private NoticeListResponse getCanApplyNowNotice() {
    TT message = TT.gen("提交申请，查看您的资金额度");
    return NoticeListResponse.fromSingle(message);
  }

  private NoticeListResponse getPayoutFailedNotice() {
    TT message = TT.gen("订单打款失败，请确认你的银行卡信息是否正确");
    String redirectUrl = homepageV5Config.getBankCardRedirectUrl();
    return NoticeListResponse.fromSingle(message, redirectUrl, FontColor.GREEN);
  }

  private NoticeListResponse getDefaultReadyOrderNotice(List<OrderInstalment> readyOrderVOList) {
    return NoticeListResponse.fromSingle(
        TT.gen("按时还款，解锁高额低息产品"),
        homepageV5Config.getRepaymentOrderRedirectUrl(readyOrderVOList),
        FontColor.GREEN);
  }

  private NoticeListResponse getReadyOrdersNotice(SDKType sdkType, List<OrderInstalment> readyOrderVOList) {
    int dayThreshold = homepageV5Config.getBillingOrderDayThreshold();
    OrderTipResponse tip = OrderTipResponse.from(readyOrderVOList, dayThreshold);
    if (tip.isOverdue()) {
      return NoticeListResponse.fromSingle(
          TT.gen("您的订单已逾期，快去还款，避免影响后续借款！"),
          homepageV5Config.getRepaymentOrderRedirectUrl(readyOrderVOList),
          FontColor.RED);
    } else if (tip.repayingOrderCount > 0) {
      Long latestBillingDate = readyOrderVOList.stream().map(OrderInstalment::getLatestBillingDate).min(Long::compare)
          .orElse(null);
      int minDiffDay = Clock.getAbsCalenderDaysBetween(Clock.now(), latestBillingDate, sdkType.getTimeZone());
      return NoticeListResponse.fromSingle(
          TT.gen("您的订单{0}天后要到期啦，马上还款！", minDiffDay == 0 ? 1 : minDiffDay),
          homepageV5Config.getRepaymentOrderRedirectUrl(readyOrderVOList),
          FontColor.ORANGE);
    }
    return getDefaultReadyOrderNotice(readyOrderVOList);
  }

  private NoticeListResponse getMultiLoanInitNotice(SDKType sdkType, List<OrderInstalment> readyOrderVOList) {
    NoticeListResponse readyOrdersNotice = getReadyOrdersNotice(sdkType, readyOrderVOList);

    if (readyOrdersNotice != null) {
      return readyOrdersNotice;
    }
    String redirectUrl = homepageV5Config.getMultiLoanGuideRedirectUrl();
    return NoticeListResponse.fromSingle(TT.gen("您已解锁再借一笔机会，限时有效"), redirectUrl, FontColor.GREEN);
  }

  private NoticeListResponse getRepaymentOrProductNotice(SDKType sdkType,
                                                         List<OrderInstalment> readyOrderVOList,
                                                         boolean hasLowInterestProduct) {
    NoticeListResponse readyOrdersNotice = getReadyOrdersNotice(sdkType, readyOrderVOList);
    // 先判断待还
    // 再判断优惠产品
    // 剩下的看状态
    if (readyOrdersNotice != null) {
      return readyOrdersNotice;
    }
    if (hasLowInterestProduct) {
      return NoticeListResponse.fromSingle(TT.gen("已为您提供优惠产品，限时有效"));
    } else {
      return null;
    }
  }

  private NoticeListResponse getExtraDataOrLowInterestNoticeOrNull(IDNHomepageLoanStatusV5 status,
                                                                   LoanApiViewerContext viewerContext,
                                                                   HomepageUserParamsVO paramsVO,
                                                                   boolean hasLowInterestProduct) {
    NoticeListResponse riskExtraDataNotice = getRiskExtraDataNotice(status, viewerContext.userId, paramsVO.accountVO.id, viewerContext.build);
    if (riskExtraDataNotice != null) {
      return riskExtraDataNotice;
    }
    return getLowInterestProductNoticeOrNull(hasLowInterestProduct);
  }

  private NoticeListResponse getLowInterestProductNoticeOrNull(boolean hasLowInterestProduct) {
    return hasLowInterestProduct ? NoticeListResponse.fromSingle(TT.gen("已为您提供优惠产品，限时有效")) : null;
  }

  private NoticeListResponse getRepaymentOrProductNoticeInMuliLoanAccepted(SDKType sdkType,
                                                                           List<OrderInstalment> readyOrderVOList,
                                                                           boolean hasLowInterestProduct) {
    NoticeListResponse repaymentOrProductNotice = getRepaymentOrProductNotice(sdkType, readyOrderVOList, hasLowInterestProduct);
    if (repaymentOrProductNotice == null) {
      String redirectUrl = homepageV5Config.getMultiLoanGuideRedirectUrl();
      return NoticeListResponse.fromSingle(TT.gen("您已解锁再借一笔机会，限时有效"), redirectUrl, FontColor.GREEN);
    } else {
      return repaymentOrProductNotice;
    }
  }
}
