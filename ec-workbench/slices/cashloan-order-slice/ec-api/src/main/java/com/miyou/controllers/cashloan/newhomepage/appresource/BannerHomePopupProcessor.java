package com.miyou.controllers.cashloan.newhomepage.appresource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.miyou.controllers.cashloan.newhomepage.IHomePageFieldProcessor;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.AppResourceProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomeAppResourceResponse;
import com.miyou.controllers.cashloan.response.v5.banner.BannerListResponse;
import com.miyou.controllers.cashloan.response.v5.orderpushpopup.OrderPagePushPopupResponse;
import com.miyou.controllers.cashloan.response.v5.popupwindow.PopupWindowListResponse;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.utilities.AppResourceResponseVO;
import com.miyou.utilities.McAppResourceConvertUtil;
import com.yqg.core.model.loader.SecondOrderForRecallLoader;
import com.yqg.core.model.loader.SecondOrderForReduceLoader;
import com.yqg.core.model.loader.T0OrderPagePopupSeenLoader;
import com.yqg.core.model.sql.loan.creditsaware.strategy.CreditAwareStrategyApplyResult;
import com.yqg.core.model.sql.loan.creditsaware.strategy.NoCouponDialogStyle;
import com.yqg.core.model.sql.loan.creditsaware.strategy.FirstLoanPopupDecisionSnapshot;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigType;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.alert.FraudAlertService;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.risk.RiskRejectOrderService;
import com.yqg.core.service.general.mcresource.AppResourceManagerService;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.general.pageconfig.vo.GeneralPageConfigShowLogVO;
import com.yqg.core.service.loan.account.T0LoanAcceptService;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.ec.common.constant.AppResourceExtraParam;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.mc.common.vo.appresource.AppResourceBaseInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BannerHomePopupProcessor implements IHomePageFieldProcessor<HomeAppResourceResponse, AppResourceProcessorType> {
  @Autowired
  private AppResourceManagerService appResourceManagerService;
  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private SecondOrderForRecallLoader secondOrderForRecallLoader;
  @Autowired
  private SecondOrderForReduceLoader secondOrderForReduceLoader;
  @Autowired
  private T0LoanAcceptService t0LoanAcceptService;
  @Autowired
  private FraudAlertService fraudAlertService;
  @Autowired
  private RiskRejectOrderService riskRejectOrderService;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private T0OrderPagePopupSeenLoader t0OrderPagePopupSeenLoader;
  @Autowired
  private LoanMarketDialogService loanMarketDialogService;

  @Override
  public AppResourceProcessorType getProcessorType() {
    return AppResourceProcessorType.BANNER_HOME_POP_UP;
  }

  @Override
  public void process(HomeAppResourceResponse fieldsInfo, HomePageContext homePageContext) {
    this.doProcess(fieldsInfo, homePageContext);
  }

  protected void doProcess(HomeAppResourceResponse fieldsInfo, HomePageContext homePageContext) {
    boolean subHomePage = homePageContext.getHomePageType() != HomePageType.HOME_PAGE_FOR_LEVEL_1;
    List<GeneralPageConfigType> resourceTypeList =
        subHomePage
            ? Lists.newArrayList(GeneralPageConfigType.SUB_HOME_MESSAGE, GeneralPageConfigType.ORDER_PAGE_PUSH_POPUP)
            : getProcessorType().resourceTypeList;
    UserDeviceContextVO userDeviceContextVO = homePageContext.getUserDeviceContextVO();
    HashMap<String, Object> params = Maps.newHashMap();
    AppResourceExtraParam.PLATFORM_TYPE.setValue(params, userDeviceContextVO.getPlatformType().name());
    AppResourceExtraParam.APP_CURRENT_OPEN_TIME.setValue(params, userDeviceContextVO.getAppCurrentOpenTime());
    AppResourceExtraParam.CREDITS_DETAILS_AWARE_POPUP_TYPE.setValue(params, homePageContext.getCreditsDetailsAwarePopupType().getPopupType());
    fillAppResourceExtraParam(params, homePageContext);

    FirstLoanPopupDecisionSnapshot firstLoanPopupDecisionSnapshot = homePageContext.getFirstLoanPopupDecisionSnapshot();
    Optional.ofNullable(firstLoanPopupDecisionSnapshot.getFirstLoanCouponDialogStyle())
        .ifPresent(style -> params.put("firstLoanCouponDialogStyle", style));
    String t0CreditEnhance = firstLoanPopupDecisionSnapshot.getCreditEnhanceBucket();
    NoCouponDialogStyle noCouponDialogStyle = firstLoanPopupDecisionSnapshot.getFirstLoanNoCouponDialogStyle();
    if (noCouponDialogStyle.isEffective()) {
      params.put("firstLoanNoCouponDialogStyle", noCouponDialogStyle.name());
    }
    if (!"A".equals(t0CreditEnhance)) {
      params.put("t0CreditEnhance", t0CreditEnhance);
    }


    AppResourceExtraParam.HOME_PAGE_SCENE.setValue(params, homePageContext.getHomePageScene().name());
    AppResourceExtraParam.REQUEST_CLIENT_TYPE.setValue(params, userDeviceContextVO.getClientType());
    if (homePageContext.getStatus().canCreateOrder() && secondOrderForRecallLoader.exists(homePageContext.getUserId())) {
      AppResourceExtraParam.SECOND_ORDER_FOR_RECALL.setValue(params, "B");
    }
    if (homePageContext.getStatus().canCreateOrder() && riskRejectOrderService.isUserInOrderRejectProcessWithOrderId(
        homePageContext.getLoanAccountId(), null)) {
      Optional.ofNullable(homePageContext.getUserCreditsContext())
          .map(UserCreditsContext::getLatestUserRiskTraceVO)
          .map(vo -> vo.riskType)
          .ifPresent(riskType -> {
            if (secondOrderForReduceLoader.exists(homePageContext.getUserId())) {
              log.info("second order for recall result exists, userId is {}", homePageContext.getUserId());
              return;
            }
            // 如果是首贷则全部入组，复贷则判断逻辑
            if (!ecOrderService.existOrder(homePageContext.getLoanAccountId(), CashLoanOrderStatus.PAYOUT_STATUSES)) {
              AppResourceExtraParam.SECOND_ORDER_FOR_REDUCE.setValue(params, "B");
              log.info("second order for recall result is {}, userId is {}, loan", params, homePageContext.getUserId());
            } else {
              String abResult = expDiversionClient.getString("technology-lending-abroad-loan_all-limit_and_rate_adjustment_fudai_pro_v2", "A");
              AppResourceExtraParam.SECOND_ORDER_FOR_REDUCE.setValue(params, abResult);
              log.info("second order for recall result is {}, userId is {}, reloan", params, homePageContext.getUserId());
            }
          });
    } else {
      secondOrderForReduceLoader.del(homePageContext.getUserId());
    }
    boolean shouldShowFraudAlert = fraudAlertService.shouldShowFraudAlertPopup(homePageContext.getUserId(),homePageContext.getLoanAccountId());
    AppResourceExtraParam.FRAUD_ALERT_POPUP.setValue(params, shouldShowFraudAlert);
    params.putAll(homePageContext.getResourceParamMap());
    loanMarketDialogService.setLoanMarketUpdateDialogParam(homePageContext, params);
    fillReloanDefaultAmountForMc(params, homePageContext);
    Map<String, List<AppResourceBaseInfo>> appResourceMap =
        appResourceManagerService.getResourceFromMc(
            GeneralPageConfigParam.fromWithTriggerSource(homePageContext.getUserId(), homePageContext.getSdkType(),
                userDeviceContextVO.getBuild(), homePageContext.getStatus(), userDeviceContextVO.getDeviceToken(),
                userDeviceContextVO.getSourceType(), homePageContext.getTriggerSource(), params), resourceTypeList);

    AppResourceResponseVO bannerResource = McAppResourceConvertUtil.buildResponse(appResourceMap, GeneralPageConfigType.BANNER);
    Object banner = BannerListResponse.from(bannerResource.resource);
    fieldsInfo.existBanner = CollectionUtils.isNotEmpty(bannerResource.sourceIds);

    AppResourceResponseVO homePopUp = McAppResourceConvertUtil.buildResponse(appResourceMap,
        homePageContext.getHomePageType() == HomePageType.HOME_PAGE_FOR_LEVEL_1 ?GeneralPageConfigType.HOME_MESSAGE : GeneralPageConfigType.SUB_HOME_MESSAGE);
    Object popup = PopupWindowListResponse.from(homePopUp, homepageContentTool.getAppRefreshPopupWindowIntervalMilisecond(userDeviceContextVO.getBuild()));

    fieldsInfo.setBanner(banner);
    fieldsInfo.setPopup(popup);
    // US3 下单页首次获额扑脸（TAPD-369589）：独立位置码结果透传独立字段 orderPagePushPopup（精简 DTO 单对象，
    // 非数组、不复用 PopupWindowResponse、不混入 SUB_HOME_MESSAGE）；未下发为 null（前端不展示、不阻断下单）；首页(一楼)不下发
    if (subHomePage) {
      AppResourceResponseVO orderPagePushResource =
          McAppResourceConvertUtil.buildResponse(appResourceMap, GeneralPageConfigType.ORDER_PAGE_PUSH_POPUP);
      fieldsInfo.setOrderPagePushPopup(OrderPagePushPopupResponse.firstFrom(orderPagePushResource));
    }
    List<Long> listId = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(bannerResource.sourceIds)) {
      listId.addAll(bannerResource.sourceIds);
    }
    if (CollectionUtils.isNotEmpty(homePopUp.sourceIds)) {
      listId.addAll(homePopUp.sourceIds);
    }
    if (CollectionUtils.isNotEmpty(listId)) {
      DwLogUtil.log(LogBusinessType.GENERAL_PAGE_CONFIG_SHOW_LOG,
          GeneralPageConfigShowLogVO.from(
              homePageContext.getHomePageType() == HomePageType.HOME_PAGE_FOR_LEVEL_1 ? "/api/v5/cashloan/home" : "/api/v5/cashloan/subhome",
              homePageContext.getUserId(),
              listId,
              homePageContext.getStatus(),
              "",
              userDeviceContextVO.getDeviceToken(),
              userDeviceContextVO.getPlatformType().name(),
              userDeviceContextVO.getBuild(),
              homePageContext.getSdkType()
          ));
    }
  }


  /**
   * 写入感知相关参数
   */
  private void fillAppResourceExtraParam(Map<String, Object> paramMap, HomePageContext homePageContext) {
    Map<String, String> result = new HashMap<>();
    CreditAwareStrategyApplyResult applyResult = homePageContext.getCreditsDetailsAwarePopupType();
    BigDecimal beforeMaxAmount = applyResult.getBeforeMaxAmount();
    BigDecimal maxAmount = homePageContext.getUserProductVO().getMaxAmount();
    switch (applyResult.getPopupType()) {
      case MARKETING_CREDIT_POPUP: {
        BigDecimal increaseAmount = maxAmount.subtract(beforeMaxAmount).setScale(0, RoundingMode.DOWN);
        LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
        assert creditsInfoVO != null;
        long remainMills = Clock.getMilliSecondsBetween(Clock.now(), creditsInfoVO.tempCreditsExpiredTimeForCoupon);
        if (remainMills > Clock.MILLS_PER_DAY) {
          // 此处不走翻译，因为其他文案是在UI配死印尼语
          result.put("formattedDate", String.format("Berakhir dalam %d hari", remainMills / Clock.MILLS_PER_DAY));
        } else {
          result.put("formattedTime", String.valueOf(remainMills));
        }

        String unit = StringUtils.EMPTY;
        switch (CommonABTestResultGroup.valueOf(applyResult.getAbResult())) {
          case B:
            if (increaseAmount.compareTo(BigDecimal.valueOf(1_000_000)) < 0) {
              increaseAmount = increaseAmount.divide(BigDecimal.valueOf(1000), 0, RoundingMode.DOWN);
              unit = "RIBU";
            } else {
              increaseAmount = increaseAmount.divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.DOWN);
              unit = "JUTA";
            }
            break;
          case C:
            if (increaseAmount.compareTo(BigDecimal.valueOf(1_000_000)) < 0) {
              increaseAmount = increaseAmount.setScale(0, RoundingMode.DOWN);
              unit = StringUtils.EMPTY;
            } else {
              increaseAmount = increaseAmount.divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.DOWN);
              unit = "JUTA";
            }
        }
        result.put("unit", unit);
        result.put("amount", increaseAmount.toString());
        break;
      }

      case CREDIT_INCREASED_V2_POPUP:
        switch (CommonABTestResultGroup.valueOf(applyResult.getAbResult())) {
          case B:
          case C: {
            BigDecimal beforeInt = beforeMaxAmount.setScale(0, RoundingMode.DOWN);
            BigDecimal maxInt = maxAmount.setScale(0, RoundingMode.DOWN);
            result.put("abResult", applyResult.getAbResult());
            result.put("beforeMaxAmount", beforeInt.toString());
            result.put("maxAmount", maxInt.toString());
            result.put("unit", StringUtils.EMPTY);
            break;
          }
          case D: {
            BigDecimal beforeMillion = beforeMaxAmount.divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.DOWN);
            BigDecimal maxMillion = maxAmount.divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.DOWN);
            result.put("abResult", "D");
            result.put("beforeMaxAmount", beforeMillion.toString());
            result.put("maxAmount", maxMillion.toString());
            result.put("unit", "JUTA");
            break;
          }
        }
    }
    paramMap.put("creditAwarePopupParam", JsonUtils.toString(result));
  }

  /**
   * 将复贷下单页默认金额写入营销中心扩展参数，与 {@link HomePageContext#getReloanDefaultAmount()} / 产品列表 {@code defaultAmount} 同源；
   * 无默认金额时不写入。
   */
  private void fillReloanDefaultAmountForMc(Map<String, Object> params, HomePageContext homePageContext) {
    BigDecimal defaultAmount = homePageContext.getReloanDefaultAmount();
    if (defaultAmount != null) {
      AppResourceExtraParam.USER_INPUT_AMOUNT.setValue(params, defaultAmount);
    }
  }

}
