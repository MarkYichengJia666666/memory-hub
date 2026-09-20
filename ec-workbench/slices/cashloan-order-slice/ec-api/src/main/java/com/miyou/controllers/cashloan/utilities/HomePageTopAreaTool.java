package com.miyou.controllers.cashloan.utilities;

import com.miyou.controllers.cashloan.response.v5.product.ProductListResponse;
import com.miyou.controllers.cashloan.response.v5.toparea.TopAreaResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.userflow.domain.loan.model.discounts.OrderDiscounts;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.fee.enums.CalcFeeScale;
import com.yqg.core.service.cashloan.fee.enums.CalcFeeScaleMapper;
import com.yqg.core.service.cashloan.homepage.config.HomePageTopAreaConfig;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.vo.enums.DiscountDetailTopAreaVipUIDisplayStrategy;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.util.common.NumberFormatter;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * @author chaoye
 * @date 2024/2/28
 */
@Slf4j
@Component
public class HomePageTopAreaTool {

  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private HomepageCommonTool homepageCommonTool;
  @Autowired
  private HomePageTopAreaConfig homePageTopAreaConfig;
  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  public TopAreaResponse getTopArea(IDNHomepageLoanStatusV5 status, HomepageUserParamsVO paramsVO, UserResponse userInfo,
                                    ProductListResponse product) {
    try {
      DiscountDetailTopAreaVipUIDisplayStrategy strategy = homepageCommonTool.getDiscountDetailTopAreaVipUIDisplayStrategy(paramsVO.getUserId(), paramsVO.build);
      if (strategy.removeTopArea()) {
        return null;
      }

      if (paramsVO.newHomePageUI()) {
        return getTopAreaResponseForV2HomePage(status, paramsVO, userInfo, product);
      }
      return getTopAreaResponseForV1HomePage(status, paramsVO, userInfo, product);
    } catch (Exception e) {
      log.error("getTopArea error", e);
      return null;
    }

  }

  private TopAreaResponse getTopAreaResponseForV2HomePage(IDNHomepageLoanStatusV5 status,
                                                          HomepageUserParamsVO paramsVO,
                                                          UserResponse userInfo,
                                                          ProductListResponse product) {
    switch (status) {
      case NOT_LOGIN:
      case NEVER_APPLIED:
      case IN_REVIEW:
      case IMAGE_REVIEW_REJECTED:
      case REUPLOAD_FINISHED:
      case REJECTED:
      case CANCELLED:
      case CAN_REAPPLY_NOW:
      case INCREASE_REVIEW_NEVER_REAPPLIED:
      case NEED_SUPPLEMENT:
      case WAITING_SUPPLEMENT:
      case FINISH_SUPPLEMENT:
        return getDefaultTopArea();
      case RELOAN_CAN_REAPPLY_NOW:
      case LOAN_CREDITS_EXPIRED:
      case RELOAN_IN_REVIEW:
      case RELOAN_CALC_CREDITS_IN_REVIEW:
      case RELOAN_REJECTED:
      case CALC_CREDITS_EXPIRED:
      case MULTI_LOAN_INIT:
      case MULTI_LOAN_CALC_CREDITS_IN_REVIEW:
        return getWelcomeBackTopArea(paramsVO);
      case CAN_REAPPLY_IN_FUTURE:
      case INCREASE_REVIEW_REJECT:
        if (paramsVO.firstLoan) {
          return getDefaultTopArea();
        }
        return getWelcomeBackTopArea(paramsVO);
      case ACCEPTED:
      case LOAN_CREDITS_DECREASE:
      case RELOAN_INIT:
      case RELOAN_CREDITS_DECREASE:
      case MULTI_LOAN_CREDITS_ACCEPTED:
      case PAYOUT_FAILED:
        return getEnableCrateOrderTopArea(paramsVO, userInfo, product);
      case FUND_CHECK:
      case GRAB_CHECK:
      case FUND_PAYING:
      case DEBT_CHECK:
      case ORDER_PRE_CHECK:
      case PAYING:
      case RELOAN_FUND_CHECK:
      case RELOAN_GRAB_CHECK:
      case RELOAN_FUND_PAYING:
      case RELOAN_DEBT_CHECK:
      case RELOAN_ORDER_PRE_CHECK:
      case RELOAN_PAYING:
      case MULTI_LOAN_FUND_CHECK:
      case MULTI_LOAN_GRAB_CHECK:
      case MULTI_LOAN_FUND_PAYING:
      case MULTI_LOAN_DEBT_CHECK:
      case MULTI_LOAN_ORDER_PRE_CHECK:
      case MULTI_LOAN_IN_REVIEW:
        return getOrderHandlingTopArea(paramsVO);
      case READY:
      case RELOAN_READY:
      case MULTI_LOAN_PAYING:
        return getWelcomeTopArea(paramsVO);
      case OVERDUE:
      case RELOAN_OVERDUE:
        return getWelcomeTopArea(paramsVO);
      default:
        return null;
    }
  }

  private TopAreaResponse getOrderHandlingTopArea(HomepageUserParamsVO paramsVO) {
    DiscountDetailTopAreaVipUIDisplayStrategy strategy = homepageCommonTool.getDiscountDetailTopAreaVipUIDisplayStrategy(paramsVO.getUserId(), paramsVO.build);
    if (strategy.removeTopArea()) {
      return null;
    }
    String orderHandlingTopArea = homePageTopAreaConfig.getOrderHandlingTopArea();
    if (StringUtils.isBlank(orderHandlingTopArea)) {
      return null;
    }
    TopAreaResponse res = JsonUtils.from(orderHandlingTopArea, TopAreaResponse.class);
    if (res == null) {
      return null;
    }
    res.updateBoldText(paramsVO.accountVO.name);
    res.updateIconColor(homePageTopAreaConfig.getTopBarIconColor(res.toolBarColor));
    return res;
  }

  private TopAreaResponse getWelcomeBackTopArea(HomepageUserParamsVO paramsVO) {
    String welcomeBackTopArea = homePageTopAreaConfig.getWelcomeBackTopArea();
    if (StringUtils.isBlank(welcomeBackTopArea)) {
      return null;
    }
    TopAreaResponse res = JsonUtils.from(welcomeBackTopArea, TopAreaResponse.class);
    if (res == null) {
      return null;
    }
    res.updateBoldText(paramsVO.accountVO.name);
    res.updateIconColor(homePageTopAreaConfig.getTopBarIconColor(res.toolBarColor));
    return res;
  }

  @Nullable
  private TopAreaResponse getTopAreaResponseForV1HomePage(IDNHomepageLoanStatusV5 status, HomepageUserParamsVO paramsVO, UserResponse userInfo, ProductListResponse product) {
    //未登录不展示头图
    if (paramsVO == null || paramsVO.accountVO == null || paramsVO.accountVO.id == null) {
      return null;
    }

    //版本号需要大于等于35213
    if (paramsVO.build < homepageV5Config.getDiscountDetailAnimationBuild()) {
      return null;
    }

    return getEnableCrateOrderTopArea(paramsVO, userInfo, product);
  }

  @Nullable
  private TopAreaResponse getEnableCrateOrderTopArea(HomepageUserParamsVO paramsVO, UserResponse userInfo, ProductListResponse product) {
    TopAreaResponse res = null;
    if (product.hasLowInterestProduct()) {
      res = getLowInterestTopArea(paramsVO, userInfo, product);
    } else {
      res = getCutInterestCouponTopAreaResponse(paramsVO);
    }
    if (Objects.isNull(res)) {
      res = getDefaultOrderPageTopArea(paramsVO.newHomePageUI(), paramsVO.accountVO.name);
    }

    return res;
  }


  public TopAreaResponse getDefaultOrderPageTopArea(boolean newHomePageUi, String accountName) {
    if (!newHomePageUi) {
      return null;
    }
    String config = homePageTopAreaConfig.getEnableCreateOrderTopAreaConfig();
    if (StringUtils.isBlank(config)) {
      return null;
    }
    TopAreaResponse res = JsonUtils.from(config, TopAreaResponse.class);
    if (res == null) {
      return null;
    }
    res.show = true;
    res.updateBoldText(accountName);
    res.updateIconColor(homePageTopAreaConfig.getTopBarIconColor(res.toolBarColor));
    return res;
  }

  private TopAreaResponse getWelcomeTopArea(HomepageUserParamsVO paramsVO) {

    String welcomeTopAreaConfig = homePageTopAreaConfig.getWelcomeTopAreaConfig();
    if (StringUtils.isBlank(welcomeTopAreaConfig)) {
      return null;
    }
    TopAreaResponse res = JsonUtils.from(welcomeTopAreaConfig, TopAreaResponse.class);
    if (Objects.isNull(res)) {
      return null;
    }
    res.updateBoldText(paramsVO.accountVO.name);
    res.updateIconColor(homePageTopAreaConfig.getTopBarIconColor(res.toolBarColor));
    return res;
  }

  private TopAreaResponse getLowInterestTopArea(HomepageUserParamsVO paramsVO, UserResponse userInfo, ProductListResponse product) {
    //35513以上使用V3
    //35513以下使用V2
    if (paramsVO.build >= homepageV5Config.getDiscountDetailAnimationV3Build()) {
      if (!product.allLowInterestProduct()) {
        return null;
      }

    } else if (userInfo == null
        || userInfo.discountDetailAnimationResponse == null
        || userInfo.discountDetailAnimationResponse.discountDetailAnimationStrategyNew == null) {
      return null;
    }

    TopAreaResponse response = new TopAreaResponse();
    response.imageUrl = homepageV5Config.getDiscountDetailAnimationTopAreaImageUrl();
    response.toolBarColor = homepageV5Config.getDiscountDetailAnimationTopAreaToolBarColor();


    response.boldText = MessageFormat.format(homepageV5Config.getDiscountDetailAnimationTopAreaBoldText(),
        userInfo.minActualInterestRateFormat,
        userInfo.maxDaysLowInterestProductDiscountAmountFormat);

    response.normalText = homepageV5Config.getDiscountDetailAnimationTopAreaNormalText();
    response.toolBarIconColor = homePageTopAreaConfig.getTopBarIconColor(response.toolBarColor);
    return response;
  }

  public TopAreaResponse getCutInterestCouponTopAreaResponse(HomepageUserParamsVO paramsVO) {
    //先看是否有符合条件的优惠券
    OrderDiscounts orderDiscounts = ecHomePageProductTool.getOrderDiscountsForOptimalByEnableCredits(paramsVO.getEnableCredits(),
        paramsVO.accountVO.userId, paramsVO.getSDKType());
    BigDecimal couponDiscountAmount = orderDiscounts != null ? orderDiscounts.getCouponDiscountAmount() : BigDecimal.ZERO;
    if (BigDecimalHelper.compareTo(couponDiscountAmount, BigDecimal.ZERO) <= 0) {
      LoanProductConfigVO maxDaysProductVO = Optional.ofNullable(paramsVO.productConfigList)
          .orElse(new ArrayList<>())
          .stream()
          .max(Comparator.comparing(LoanProductConfigVO::fetchDays))
          .orElse(null);
      Long productId = maxDaysProductVO == null ? null : maxDaysProductVO.id;
      log.info("no cut interest coupon for max day product with max amount, userId:{}, remainingCredits:{}, max days productId:{}", paramsVO.accountVO.userId, paramsVO.remainingCredits, productId);
      return null;
    }

    TopAreaResponse response = new TopAreaResponse();
    response.show = true;
    response.imageUrl = homepageV5Config.getCutInterestCouponTopAreaImageUrl();
    response.toolBarColor = homepageV5Config.getCutInterestCouponTopAreaToolBarColor();
    CalcFeeScale feeScale = CalcFeeScaleMapper.getScaleBySdk(paramsVO.getSDKType());
    String couponAmountStr =
        NumberFormatter.format(paramsVO.getSDKType().getLocale(), couponDiscountAmount.setScale(feeScale.principalScale, feeScale.principalRoundType));
    String maxCutInterestCouponAmountFormat = String.format("Rp%s", couponAmountStr);

    response.boldText = MessageFormat.format(homepageV5Config.getCutInterestCouponTopAreaBoldText(),
        maxCutInterestCouponAmountFormat);
    response.normalText = homepageV5Config.getCutInterestCouponTopAreaNormalText();
    response.toolBarIconColor = homePageTopAreaConfig.getTopBarIconColor(response.toolBarColor);
    return response;
  }


  private TopAreaResponse getDefaultTopArea() {

    String defaultTopAreaConfig = homePageTopAreaConfig.getDefaultTopAreaConfig();
    if (StringUtils.isBlank(defaultTopAreaConfig)) {
      return null;
    }
    TopAreaResponse res = JsonUtils.from(defaultTopAreaConfig, TopAreaResponse.class);
    if (res == null) {
      return null;
    }
    res.show = true;
    res.toolBarIconColor = homePageTopAreaConfig.getTopBarIconColor(res.toolBarColor);
    return res;
  }
}
