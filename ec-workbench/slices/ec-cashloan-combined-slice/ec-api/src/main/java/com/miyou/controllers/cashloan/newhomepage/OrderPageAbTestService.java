package com.miyou.controllers.cashloan.newhomepage;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.order.OrderPageConfig;
import com.miyou.controllers.cashloan.response.v5.order.OrderPageVersion;
import com.miyou.controllers.cashloan.response.v5.pagev3.OrderPageAmountInputAreaPattern;
import com.miyou.controllers.cashloan.response.v5.pagev3.OrderPageExperimentContext;
import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.abtest.h12026.ExpConditionFor2026H1Service;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.HomepageV5Config.OrderPageCarouselContentConfig;
import com.yqg.core.service.cashloan.auth.BindCardDelayService;
import com.yqg.core.service.experiment.InterestSplitExpService;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.marketing.channel.AdChannelAcquisitionService;
import com.yqg.core.util.IdnAmountFormatter;
import com.yqg.ec.common.constant.ExperimentKeyConstants;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.utils.EcAsserts;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderPageAbTestService {

  @Autowired
  private BindCardDelayService bindCardDelayService;
  @Resource
  private HomepageV5Config homepageV5Config;
  @Resource
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private ExpConditionFor2026H1Service expConditionFor2026H1Service;
  @Autowired
  private InterestSplitExpService interestSplitExpService;
  @Autowired
  private AdChannelAcquisitionService adChannelAcquisitionService;

  public OrderPageConfig fetchOrderPageConfigResponse(Long userId, Long build, SDKType sdkType, RequestClientType clientType, HomePageContext homePageContext) {
    if (Objects.isNull(userId)) {
      return OrderPageConfig.defaultConfig();
    }

    EcAsserts.assertNotNull(homePageContext.getLoanAccountVO(), "getOrderPageVersion fail, loanAccountVO is null, userId={}", userId);
    boolean isReloan = homePageContext.getLoanAccountVO().getLoanTimes() > 0;
    OrderPageVersion version = getOrderPageVersion(userId, build, sdkType, homePageContext, isReloan);

    String result = "A";
    boolean agreementCheckbox = false;

    boolean bankAccountDelay = bindCardDelayService.isMissingBankCard(userId, sdkType);

    List<OrderPageCarouselContentConfig> carouselContentConfigs = getCarouselContentConfigs(userId);

    OrderPageAmountInputAreaPattern amountInputAreaPattern = this.buildAmountInputPattern(homePageContext);

    SourceType sourceTypeForExtraLimit = homePageContext.getUserDeviceContextVO() != null
        ? homePageContext.getUserDeviceContextVO().getSourceType() : null;
    String deviceToken = homePageContext.getUserDeviceContextVO() != null
        ? homePageContext.getUserDeviceContextVO().getDeviceToken() : null;
    boolean showExtraLimit = isShowExtraLimit(userId, build, sourceTypeForExtraLimit, clientType, sdkType);

    // 千1 承接（过件后路径）：实验组 + 未首借 + 真实日利率 ≤ 0.1%，显式透传 deviceToken
    boolean qian1Hit = adChannelAcquisitionService.isQian1Hit(
        userId, deviceToken, build, sourceTypeForExtraLimit, sdkType);
    // T013 互斥：命中千1 时不展示下单涨额度横条（spec US3-7）
    if (qian1Hit) {
      showExtraLimit = false;
    }

    return OrderPageConfig.create(version, agreementCheckbox, result, bankAccountDelay, amountInputAreaPattern, showExtraLimit, carouselContentConfigs)
        .setShowOneThousandthRate(qian1Hit);
  }

  /**
   * 命中息费结构 3.0（excludePpn）时下发首屏安全认证轮播；不再依赖下单页 V5 实验。
   */
  private List<OrderPageCarouselContentConfig> getCarouselContentConfigs(Long userId) {
    try {
      InterestSplitExpService.ExcludeFeeFromLastResult excludeFee =
          interestSplitExpService.resolveExcludeFeeFromLastResult(userId);
      boolean excludePpn = excludeFee.isExcludePpn();
      if (!excludePpn) {
        return null;
      }
      return homepageV5Config.getOrderPageCarouselContentConfigForInterestSplitV3();
    } catch (Exception e) {
      log.error("getCarouselContentConfigs error, userId:{}", userId, e);
      return null;
    }
  }

  private @NotNull OrderPageVersion getOrderPageVersion(Long userId, Long build, SDKType sdkType, HomePageContext homePageContext,
      boolean isReloan) {
    ExpUser expUser = ExpUser.builder().userId(userId).sourceType(homePageContext.getUserDeviceContextVO().getSourceType()).versionBuild(
        build).build();
    String expValForReloanV5;

    if (isReloan) {
      expValForReloanV5 = expDiversionClient.getResult(UserFlowExperimentEnum.ORDER_PAGE_V5_FOR_RELOAN.getKey(), expUser);
    } else {
      if (expConditionFor2026H1Service.matchesFirstLoanLaneCondition(userId, sdkType, build)) {
        expValForReloanV5 = expDiversionClient.getResult(UserFlowExperimentEnum.ORDER_PAGE_V5_FOR_FIRST_LOAN.getKey(), expUser);
      } else {
        expValForReloanV5 = UserFlowConstants.BLANK_GROUP;
      }
    }

    if (StringUtils.equals(UserFlowConstants.EXPERIMENT_GROUP_ONE, expValForReloanV5)) {
      return OrderPageVersion.V5_EXP1;
    }
    if (StringUtils.equals(UserFlowConstants.EXPERIMENT_GROUP_TWO, expValForReloanV5)) {
      return OrderPageVersion.V5_EXP2;
    }

    String expVal = expDiversionClient.getResult(ExperimentKeyConstants.LOAN_PAGE_V4_COUPON_UPGRADE_AREA, expUser);
    // 命中实验组则返回V4版本
    if (expVal.equals(CommonABTestResultGroup.B.desc) || expVal.equals(CommonABTestResultGroup.C.desc)) {
      return OrderPageVersion.V4;
    }
    return OrderPageVersion.V1;
  }

  private OrderPageAmountInputAreaPattern buildAmountInputPattern(HomePageContext homePageContext) {
    // 不是可下单状态 或不是进入二级下单页时 context为空
    OrderPageExperimentContext orderPageExperimentContext = homePageContext.getOrderPageExperimentContext();
    if (orderPageExperimentContext == null) {
      return null;
    }

    OrderPageAmountInputAreaPattern pattern = new OrderPageAmountInputAreaPattern();

    CommonABTestResultGroup amountInputAreaPattern = orderPageExperimentContext.getAmountInputAreaPatternExpResult();

    pattern.setPatternResult(amountInputAreaPattern.name());

    // if control group, fast return
    if (Objects.equals(amountInputAreaPattern, CommonABTestResultGroup.A)) {
      return pattern;
    }

    // build temp credits info
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    EcAsserts.assertNotNull(creditsInfoVO, "buildAmountInputPattern fail, creditsInfoVO is null, userId={}", homePageContext.getUserId());

    BigDecimal tempCreditsForCoupon = creditsInfoVO.getTempCreditsForCoupon();
    Long tempCreditsExpiredTime = creditsInfoVO.getTempCreditsExpiredTime();

    pattern.setWorkedTempCreditsValue(tempCreditsForCoupon);
    pattern.setWorkedTempCreditsExpireTimeStamp(tempCreditsExpiredTime);

    // is exp group 1
    BigDecimal enableVirtualCredits = homePageContext.getUserProductVO().getEnableVirtualCredits();
    if (Objects.equals(amountInputAreaPattern, CommonABTestResultGroup.B) && enableVirtualCredits.compareTo(homepageV5Config.getEnableCreditsThreshold()) > 0) {

      List<BigDecimal> guideAmountRateList = homepageV5Config.getOrderPageAmountFastInputRateList();
      BigDecimal stepAmount = cashLoanConfig.getStepAmount(SDKType.IDN_YQD);

      for (BigDecimal guideAmountRate : guideAmountRateList) {
        // calculate guide amount
        BigDecimal guideAmount = enableVirtualCredits.multiply(guideAmountRate);
        BigDecimal roundedGuideAmount = AmountFormatter.roundByStepAmount(guideAmount, stepAmount);
        // format amount to string
        String guideAmountStr = IdnAmountFormatter.formatAmountWithSimpleSuffix(roundedGuideAmount, RoundingMode.DOWN, homepageV5Config.getSimpleAmountTextFormatPattern());

        pattern.addAmountFastInputButton(guideAmountStr, roundedGuideAmount);
      }
    }

    return pattern;
  }

  private boolean isShowExtraLimit(Long userId, Long build, SourceType sourceType, RequestClientType clientType, SDKType sdkType) {
    if (sourceType == null) {
      return false;
    }
    if (!RequestClientType.isAndroid(clientType) || RequestClientType.isWholeProcess(clientType) || sourceType.isApiChannelSourceType()) {
      return false;
    }
    if (!expConditionFor2026H1Service.matchesFirstLoanLaneCondition(userId, sdkType, build)) {
      return false;
    }
    String expRes = expDiversionClient.getResult(ExperimentKeyConstants.FIRST_LOAN_BUTTON_LOAN_LIMIT_AMOUNT,
        ExpUser.builder().userId(userId).sourceType(sourceType).versionBuild(build).build());
    return "EXPERIMENT_GROUP".equals(expRes);
  }

}
