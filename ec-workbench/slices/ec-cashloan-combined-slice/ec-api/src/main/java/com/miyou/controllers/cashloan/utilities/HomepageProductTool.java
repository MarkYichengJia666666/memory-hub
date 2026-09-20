package com.miyou.controllers.cashloan.utilities;


import static com.yqg.core.model.sql.loan.product.enums.TimeUnit.MONTH;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.miyou.controllers.cashloan.response.home.LoanInfoDetail;
import com.miyou.controllers.cashloan.response.home.LoanInfoDetailV4;
import com.miyou.controllers.cashloan.response.home.PaymentCredentialDetail;
import com.miyou.controllers.cashloan.response.home.SeaDiscountDetail;
import com.miyou.controllers.cashloan.response.home.SeaHomeGeneralResponse;
import com.miyou.controllers.cashloan.response.home.SeaProductDisplayData;
import com.miyou.controllers.cashloan.response.home.SeaProductFeeData;
import com.miyou.controllers.cashloan.response.home.SeaProductFeeData.RepayPlanBanner;
import com.miyou.controllers.cashloan.response.home.SeaProductFeeData.RepayPlanDisplay;
import com.miyou.controllers.cashloan.response.v5.order.OrderPageVersion;
import com.miyou.controllers.cashloan.utilities.threadlocal.ProductDetailThreadLocal;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.loan.product.enums.CalcFeeType;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.ABTestVersionConfigService;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.ExpFacade.ClientType;
import com.yqg.core.service.abtest.ExpLastResultClient;
import com.yqg.core.service.abtest.h12026.ExpConditionFor2026H1Service;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.HomePageLoanInfoConfig;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.RepayPlanGrantCouponService;
import com.yqg.core.service.cashloan.enums.RepayPlanReduceGroup;
import com.yqg.core.service.cashloan.fee.enums.CalcFeeScale;
import com.yqg.core.service.cashloan.fee.enums.CalcFeeScaleMapper;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.utilities.StandardInterestUtil;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.SeaCouponDetail;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.SeaHomeInstalmentPlan;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.SeaHomeOrderInstalmentPlan;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.SeaInstalmentPlanVO;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.SeaProductFeeCache;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.T0FreeInterestShowVo;
import com.yqg.core.service.cashloan.loanproduct.LoanUserProductService;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalmentPlan;
import com.yqg.core.service.cashloan.util.discount.DiscountCalculationUtil;
import com.yqg.core.service.cashloan.util.rate.ProductRateUtil;
import com.yqg.core.service.experiment.InterestSplitExpService;
import com.yqg.core.service.marketing.channel.AdChannelAcquisitionService;
import com.yqg.core.service.cashloan.vo.IdnProductAdditionalInfoVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.coupongrantrule.CouponGrantRuleService;
import com.yqg.core.service.coupongrantrule.config.BaseRuleGrantConfig;
import com.yqg.core.service.coupongrantrule.vo.CouponGrantRuleVO;
import com.yqg.core.service.cashloan.homepage.utilities.builder.T0InterestRateReductionPerTermBuilder;
import com.yqg.core.service.cashloan.homepage.utilities.builder.impl.OrderPageInterestFreeDaysServiceBuilder;
import com.yqg.core.service.cashloan.homepage.utilities.builder.impl.T0OrderPageInfoVOBuilder;
import com.yqg.core.service.loan.account.vo.T0OrderPageInfoVO;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.config.FixedDayPercentCutInterestRuleGrantConfig;
import com.yqg.core.service.loan.coupon.vo.CutInterestVO;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.loan.discount.RepaymentPlanBuilder;
import com.yqg.core.service.loan.discount.DisCountFeeExperimentService;
import com.yqg.core.service.loan.discount.vo.DiscountDetailResult;
import com.yqg.core.service.loan.coupon.vos.LoanCutInterestCouponVO;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.userflow.domain.loan.model.discounts.OrderDiscounts;
import com.yqg.core.userflow.domain.loan.service.discounts.IOrderDiscountsService;
import com.yqg.core.util.IdnAmountFormatter;
import com.yqg.core.util.common.NumberFormatter;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
 * 具体内容都写到
 *
 * @see EcHomePageProductTool
 */
@Deprecated
@Slf4j
@Component
@EnableScheduling
public class HomepageProductTool {

  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private StandardInterestUtil standardInterestUtil;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private HomePageLoanInfoConfig loanInfoConfig;
  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private LoanUserProductService loanUserProductService;
  @Autowired
  private ProductConfigService productConfigService;
  @Autowired
  private ABTestVersionConfigService abTestVersionConfigService;
  @Autowired
  private CouponGrantRuleService couponGrantRuleService;
  @Autowired
  private RepayPlanGrantCouponService repayPlanGrantCouponService;
  @Autowired
  private OrderPageInterestFreeDaysServiceBuilder orderPageInterestFreeDaysServiceBuilder;
  @Autowired
  private ExpLastResultClient expLastResultClient;
  @Autowired
  private InterestSplitExpService interestSplitExpService;
  @Autowired
  private AdChannelAcquisitionService adChannelAcquisitionService;
  @Autowired
  private ExpConditionFor2026H1Service expConditionFor2026H1Service;
  @Autowired
  private RepaymentPlanBuilder repaymentPlanBuilder;
  @Autowired
  private IOrderDiscountsService orderDiscountsService;
  @Autowired
  private DisCountFeeExperimentService disCountFeeExperimentService;

  /**
   * 读取首页展示的产品
   *
   * @param products          产品配置
   * @param enableCredits     可用授信额度
   * @param build             app版本
   * @param maxTermsProductId
   * @param styleFor367
   * @return 首页显示的产品
   */
  public List<SeaProductDisplayData> listDisplayProduct(Long userId,
      List<LoanProductConfigVO> products,
      BigDecimal enableCredits,
      Long build,
      HomeDisplayStrategy use49Version,
      Long maxTermsProductId,
      HomeDisplayStrategy styleFor367,
      Boolean joinFreeTerms) {
    Map<String, List<String>> rateDiscountMap = new HashMap<>();
    AtomicBoolean hasNotLowLowInterestProduct = new AtomicBoolean(false);
    HomeDisplayStrategy productPageStyleFor371 = abTestVersionConfigService.getProductPageStyleFor371(userId, build);

    //按照产品期限排倒序
    BigDecimal standardInterestRate = standardInterestUtil.executeStandardInterestAbTest(userId);
    // 千1承接实验命中：对低息产品覆盖 rateDiscountCornerMark 为千1文案
    boolean qian1ExperimentHit = adChannelAcquisitionService.isQian1ExperimentGroup(
        userId, build, ImpliedContextUtils.sourceType());
    InterestSplitExpService.ExcludeFeeFromLastResult excludeFee = interestSplitExpService.resolveExcludeFeeFromLastResult(userId);
    boolean excludePlatformFee = excludeFee.isExcludePlatformFee();
    boolean excludePpn = excludeFee.isExcludePpn();
    List<SeaProductDisplayData> seaProductDisplayDataList = products.stream()
        .map(vo -> {
          SeaProductDisplayData data = new SeaProductDisplayData();
          data.productId = YqgHashids.encode(vo.id);
          data.totalDays = vo.fetchDays();
          data.displayTitle = genDisplayTitleContent(vo);
          data.stepAmount = cashLoanConfig.getStepAmount(vo.sdkType);
          data.lowerLimit = vo.minCredits.getAmountInYuan();
          data.upperLimit = BigDecimalHelper.min(vo.maxCredits.getAmountInYuan(), enableCredits);
          //是否是低息产品
          BigDecimal dayInterestRate = ProductRateUtil.genDayInterestRate(vo);
          data.actualDayInterestRate = dayInterestRate;
          data.lowInterestProduct = ProductRateUtil.isLowInterestProduct(vo, standardInterestRate);
          data.termsInterestRate = BigDecimalHelper.divideRoundDown(dayInterestRate.multiply(new BigDecimal(vo.fetchDays())), new BigDecimal(vo.terms)).setScale(6, RoundingMode.HALF_UP);

          //是否是扭曲产品
          IdnProductAdditionalInfoVO additionalInfoVO = vo.genIdnAdditional();
          data.irregularProduct = additionalInfoVO != null && CollectionUtils.isNotEmpty(additionalInfoVO.instalmentPercentPlan);

          data.interestContent = TT.gen("{0}/day", NumberFormatter.percentFormat(vo.sdkType.getLocale(), dayInterestRate));
          data.timePeriodContent = vo.genPeriodContent();
          data.terms = vo.terms;
          data.termsContent = genTermsContent(vo, productPageStyleFor371, joinFreeTerms);
          data.timePeriod = vo.timePeriod;
          data.periodContent = vo.genPeriodContent();
          data.timeUnit = vo.timeUnit;
          data.productPaymentPlan = genProductPaymentPlanContent(vo, build);
          data.timePeriodInDayUnit = vo.fetchTimePeriodInDayUnit();
          data.errorToastStr = genErrorToastStr();
          showInstalmentAmountLessThanBuild(data, build, vo,standardInterestRate);

          if (styleFor367 == HomeDisplayStrategy.C && Objects.equals(maxTermsProductId, vo.id)) {
            data.maxTermsCornerMark = TT.gen("还款少");
          }
          if (data.lowInterestProduct) {
            //TODO(gxs)rateDiscountCornerMark和rateDiscountCornerMarkUrl只在ios低版本还在用，本次不适配免一期逻辑，后续删除
            BigDecimal rateDiscount = DiscountCalculationUtil.calcLowInterestDiscountRate(dayInterestRate, standardInterestRate,
                vo.fetchDays(), vo.postPlatformFeeRate, excludePlatformFee, vo.ppnRate, excludePpn, 2, RoundingMode.DOWN);
            addInfoToRateDiscountMap(rateDiscountMap, data.terms, data.timePeriod, NumberFormatter.percentFormat(vo.sdkType.getLocale(), rateDiscount));
            data.rateDiscountCornerMark = TT.gen("{0}OFF", NumberFormatter.percentFormat(vo.sdkType.getLocale(), rateDiscount));
            data.rateDiscountCornerMarkUrl = StringUtils.isNotBlank(cashLoanConfig.getRateDiscountCornerMarkUrl()) ? cashLoanConfig.getRateDiscountCornerMarkUrl() : null;
          }
          if (!data.lowInterestProduct) {
            hasNotLowLowInterestProduct.set(true);
          }

          return data;
        })
        .sorted((o1, o2) -> ComparisonChain.start()
            .compare(o2.upperLimit, o1.upperLimit)
            .compare(o2.totalDays, o1.totalDays)
            .compare(getTimePeriodInDayForSort(o1), getTimePeriodInDayForSort(o2))
            .result())
        .collect(Collectors.toList());

//      新版本里面所有的产品都是低息产品且角标都相同的话就不要展示了
    if (use49Version == HomeDisplayStrategy.C && !hasNotLowLowInterestProduct.get()) {
      seaProductDisplayDataList = getSeaProductDisplayData(rateDiscountMap, seaProductDisplayDataList);
    }
    return seaProductDisplayDataList;
  }

  private void showInstalmentAmountLessThanBuild(
      SeaProductDisplayData data,
      Long build,
      LoanProductConfigVO vo,
      BigDecimal standardInterestRate) {
    if (build >= homepageV5Config.getLargeInstalmentAmountOwnedBuild()) {
      return;
    }
    List<SeaProductFeeCache> seaProductFeeCacheList = ecHomePageProductTool.fetchSeaProductFeeCacheDataList(vo.id).stream().filter(fee -> data.upperLimit.compareTo(fee.loanAmount) >= 0).collect(Collectors.toList());
    List<SeaProductFeeData> seaProductFeeDataList = seaProductFeeCacheList.stream().map(SeaProductFeeData::new).collect(Collectors.toList());
    data.instalmentOwedAmountMap = seaProductFeeDataList
        .stream()
        .collect(Collectors.toMap(fee -> fee.loanAmount, fee -> fee.instalmentPlanList.get(0).owedAmount));
    if (data.lowInterestProduct) {
      data.instalmentOriginAmountMap = seaProductFeeDataList
          .stream()
          .collect(Collectors.toMap(fee -> fee.loanAmount, fee -> getInstalmentOriginAmount(vo, fee.instalmentPlanList.get(0).receivedAmount,standardInterestRate)));
    }
  }

  /**
   * 判断一下（下单页优惠百分比角标）是否展示
   *
   * @param rateDiscountMap
   * @param seaProductDisplayDataList
   * @return
   */
  private static List<SeaProductDisplayData> getSeaProductDisplayData(Map<String, List<String>> rateDiscountMap,
      List<SeaProductDisplayData> seaProductDisplayDataList) {
    seaProductDisplayDataList = seaProductDisplayDataList
        .stream()
        .peek(vo -> {
              String key = String.valueOf(vo.timePeriod);
              List<String> rateDiscountList = rateDiscountMap.getOrDefault(key, new ArrayList<>());
              Set<String> rateDiscountSet = new HashSet<>(rateDiscountList);
              if (rateDiscountList.size() != 1 && rateDiscountSet.size() == 1) {
                vo.rateDiscountCornerMark = null;
                vo.rateDiscountCornerMarkUrl = null;
              }
            }
        ).collect(Collectors.toList());
    return seaProductDisplayDataList;
  }

  private void addInfoToRateDiscountMap(Map<String, List<String>> rateDiscountMap, Integer terms, Integer timePeriod, String s) {
    String key = String.valueOf(timePeriod);
    if (rateDiscountMap.containsKey(key)) {
      rateDiscountMap.get(key).add(s);
    } else {
      List<String> tempString = new ArrayList<>();
      tempString.add(s);
      rateDiscountMap.put(key, tempString);
    }

  }

  //这里简单地使用31天作为月产品的天数参与排序
  private Integer getTimePeriodInDayForSort(SeaProductDisplayData data) {
    switch (data.timeUnit) {
      case DAY:
        return data.timePeriod;
      case MONTH:
        return data.timePeriod * 31;
      default:
        throw EcException.error("Unknown time unit {}", data.timeUnit);
    }
  }

  private TT genTermsContent(LoanProductConfigVO vo, HomeDisplayStrategy homeDisplayStrategy, Boolean joinFreeTerms) {
    Integer terms = Objects.nonNull(joinFreeTerms) && joinFreeTerms ? vo.terms + 1 : vo.terms;
    if (homeDisplayStrategy != HomeDisplayStrategy.D) {
      return TT.gen("{0}period", terms);
    }
    return TT.gen("分{0}期", terms);
  }

  private TT genProductPaymentPlanContent(LoanProductConfigVO vo, Long build) {
    //todo (tianbaoliu, T00000) 版本升级多个版本之后这里的逻辑直接删了就行了
    if (build < cashLoanConfig.getCreateOrderWithoutOtpVersion()) {
      switch (vo.timeUnit) {
        case DAY:
          return TT.gen("{0}Days x {1} Period", vo.timePeriod, vo.terms);
        case MONTH:
          return TT.gen("{0}Months x {1} Period", vo.timePeriod, vo.terms);
        default:
          throw EcException.error("Unknown time unit {}", vo.timeUnit);
      }
    } else {
      switch (vo.timeUnit) {
        case DAY:
          return TT.gen("{0} Period x {1}Days", vo.terms, vo.timePeriod);
        case MONTH:
          return TT.gen("{0} Period x {1}Months", vo.terms, vo.timePeriod);
        default:
          throw EcException.error("Unknown time unit {}", vo.timeUnit);
      }
    }
  }

  private TT genDisplayTitleContent(LoanProductConfigVO vo) {
    switch (vo.timeUnit) {
      case DAY:
        return TT.gen("{0}Days x {1}", vo.timePeriod, vo.terms);
      case MONTH:
        return TT.gen("{0}Months x {1}", vo.timePeriod, vo.terms);
      default:
        throw EcException.error("Unknown time unit {}", vo.timeUnit);
    }
  }

  private BigDecimal getInstalmentOriginAmount(LoanProductConfigVO vo, BigDecimal instalmentReceived, BigDecimal standardInterestRate) {
    assertEPIProduct(vo);
    //单期总利息(分期到手金额*配置日利率*订单总天数)
    BigDecimal instalmentTotalInterest =
        instalmentReceived.multiply(standardInterestRate).multiply(BigDecimal.valueOf(vo.fetchDays()));
    CalcFeeScale feeScale = CalcFeeScaleMapper.getScaleBySdk(vo.sdkType);
    return instalmentReceived.add(instalmentTotalInterest).setScale(feeScale.receivedAmountScale, feeScale.receivedAmountRoundType);
  }

  public void assertEPIProduct(LoanProductConfigVO vo) {
    if (!CalcFeeType.EQUAL_PRINCIPAL_AND_INTEREST_LIST.contains(vo.calcFeeType)) {
      throw EcException.error("错误的计息方式，请检查配置 productId is {}", vo.id);
    }
  }

  public void checkProductDisplayList(HomepageUserParamsVO paramsVO,
      LoanUserTypeVO loanUserTypeVO,
      SDKType sdkType) {
    checkProductIsNearTheUserCredits(paramsVO);
    if (CollectionUtils.isNotEmpty(paramsVO.productConfigList)) {
      return;
    }
    List<String> ignoreProductLoanUserTypeList = homepageV5Config.getIgnoreProductLoanUserTypeList();
    if (ignoreProductLoanUserTypeList.contains(loanUserTypeVO.name)) {
      return;
    }
    loanUserProductService.checkProductMonitor(paramsVO.accountVO.id, paramsVO.loanUserProductTypeVO, sdkType, loanUserTypeVO);
  }

  private void checkProductIsNearTheUserCredits(HomepageUserParamsVO paramsVO) {
    if (paramsVO.isNear) {
      loanUserProductService.alertUserCreditsExceedProductLimitToMonitorService(paramsVO.getUserId(), paramsVO.accountVO.loanUserTypeVO);
    }
  }


  public Pair<SeaProductFeeData, SeaCouponDetail> matchAndGenProductFeeData(Long userId, SDKType sdkType, Long productId,
      Long couponId, BigDecimal amount, Long build,
      PlatformType platformType,
      HomeDisplayStrategy feeDetailDisplayComplianceOptimizationStrategy,
      HomeDisplayStrategy orderPlanStyleFor363, Long selectedBlackCardProductId,
      SourceType sourceType,T0OrderPageInfoVO t0OrderPageInfoVO) {
    SeaProductFeeCache feeCache = ecHomePageProductTool.getProductFeeCache(productId, amount);
    return genFeeDataForSelectedProduct(userId, couponId, feeCache, build, platformType,
        feeDetailDisplayComplianceOptimizationStrategy, orderPlanStyleFor363, selectedBlackCardProductId, sourceType, t0OrderPageInfoVO);
  }

  public Pair<SeaProductFeeData, SeaCouponDetail> genFeeDataForSelectedProduct(Long userId, Long couponId,
      SeaProductFeeCache feeCache, Long build,
      PlatformType platformType,
      HomeDisplayStrategy feeDetailDisplayComplianceOptimizationStrategy,
      HomeDisplayStrategy orderPlanStyleFor363, Long selectedBlackCardProductId,
      SourceType sourceType,T0OrderPageInfoVO t0OrderPageInfoVO) {
    LoanProductConfigVO productVO = feeCache.productConfigVO;
    assertEPIProduct(productVO);
    CalcFeeScale feeScale = CalcFeeScaleMapper.getScaleBySdk(productVO.sdkType);

    // 获取可用优惠券信息
    //前端首页首次调用的时候，couponId为null，会自动找一张优惠券
    //如果couponId不为null，则找对应的券
    Pair<SeaCouponDetail, CutInterestVO> couponPair = ecHomePageProductTool.getAvailableCouponPair(feeCache, couponId, userId, selectedBlackCardProductId);
    SeaCouponDetail couponDetail = couponPair == null ? null : couponPair.getLeft();
    // 获取用户最大可用优惠券
    if (Objects.nonNull(couponDetail)) {
      // 如果用户选择的优惠券为null，则本张优惠券就是最优优惠券
      if (Objects.isNull(couponId)) {
        couponDetail.optimalCoupon = couponDetail;
      } else {
        Pair<SeaCouponDetail, CutInterestVO> optimalCoupon = ecHomePageProductTool.getAvailableCouponPair(feeCache, null, userId, null);
        couponDetail.optimalCoupon = Optional.ofNullable(optimalCoupon).map(Pair::getLeft).orElse(null);
      }
    }

    // 订单优惠走 OrderDiscounts；三还款计划走 RepaymentPlanBuilder（couponPair 中的券 VO，勿直接传 couponId，可能是 mock 券）
    LoanCutInterestCouponVO couponVO = Objects.nonNull(couponDetail) ? couponDetail.couponVO : null;
    CutInterestVO cutInterestVO = Objects.nonNull(couponPair) ? couponPair.getRight() : null;

    OrderDiscounts orderDiscounts = null;
    if (userId != null) {
      orderDiscounts = orderDiscountsService.getOrderDiscounts(userId, feeCache.loanAmount, feeCache, cutInterestVO);
    }

    DiscountDetailResult discountDetail = repaymentPlanBuilder.buildWithFeeCache(
        userId, feeCache, feeCache.loanAmount, couponVO, cutInterestVO, null);

    HomeDisplayStrategy productPageStyleFor371 = abTestVersionConfigService.getProductPageStyleFor371(userId, build);
    ProductDetailThreadLocal.putAbResult("productPageStyleFor371", productPageStyleFor371);
    // 是否命中免息天数包装2.0实验组
    boolean interestFreeDaysPackagingV2Exp = false;
    try {
      Object abResult = ProductDetailThreadLocal.get().getAbResult().get(UserFlowExperimentEnum.NO_INTEREST_POPUP_V2.getKey());
      if (abResult != null) {
        interestFreeDaysPackagingV2Exp = Boolean.parseBoolean(abResult.toString());
      }
    } catch (Exception e) {
      log.warn("Failed to get interest free days packaging experiment discountDetail", e);
    }

    boolean joinFreeTerms = disCountFeeExperimentService.isExpGroupFromLastResult(userId);
    // 由 buildFeeDisplayData 根据 DiscountDetailResult + feeCache 一次性构建完整 SeaProductFeeData
    SeaProductFeeData data = buildFeeDisplayData(
        userId,
        productVO,
        feeCache,
        feeScale,
        discountDetail,
        feeDetailDisplayComplianceOptimizationStrategy,
        orderPlanStyleFor363,
        productPageStyleFor371,
        t0OrderPageInfoVO,
        interestFreeDaysPackagingV2Exp,
        couponId,
        joinFreeTerms,
        orderDiscounts
    );

    //build fixed interest and tech amount
    buildFixedInterestAndTechFeeAmount(data, feeScale);

    checkPostInterestAmount(data, feeScale);

    checkTotalRepaymentAmount(data);

    // 计算低息让利、使用用优惠券之后的费率详情信息
    buildFeeAmountList(productVO, feeScale, data, userId, build, couponDetail, discountDetail.getActualPlan(),
        discountDetail.getStandardPlanForFreeTerms(), feeDetailDisplayComplianceOptimizationStrategy, orderPlanStyleFor363, platformType, sourceType);

    // 还款计划banner
    buildRepayPlanBanner(data, userId);

    //还款计划强化优惠
    LoanCutInterestCouponVO loanCutInterestCouponVO = Optional.ofNullable(couponDetail).map(r -> r.couponVO).orElse(null);
    data.repayPlanDisplay = buildRepayPlanDisplay(discountDetail, userId, build, productVO.sdkType, loanCutInterestCouponVO, joinFreeTerms,
        orderDiscounts);

    buildT0InterestRateReduction(data, t0OrderPageInfoVO);

    return Pair.of(data, couponDetail);
  }

  private void buildT0InterestRateReduction(SeaProductFeeData data, T0OrderPageInfoVO t0OrderPageInfoVO) {
    if(Objects.nonNull(t0OrderPageInfoVO) && t0OrderPageInfoVO.isShow() && ("top".equals(t0OrderPageInfoVO.getAfRankT0Style()) || "mid".equals(t0OrderPageInfoVO.getAfRankT0Style()))){
      data.t0InterestRateReduction = T0FreeInterestShowVo.builder().title(String.format("0%% Bunga %d hari",t0OrderPageInfoVO.getInterestFreeDays())).content("-Rp" +
          IdnAmountFormatter.formatIntegerWithDot(t0OrderPageInfoVO.getTotalInterestFeeDifference().intValue())).build();
    }
  }

  /**
   * 构建每期T0利率减免展示信息
   * 根据 interestFreeDaysPackagingExp 参数选择不同的构建策略
   *
   * 构建策略选择逻辑：
   * 1. 如果 interestFreeDaysPackagingExp 为 "1"，使用 OrderPageInterestFreeDaysService 构建
   * 2. 否则，如果满足原有条件（isShow() && afRankT0Style 是 "top" 或 "mid"），使用 T0OrderPageInfoVO 构建
   *
   * @param userId 用户ID
   * @param productVO 产品配置
   * @param amount 借款金额
   * @param couponId 优惠券ID
   * @param t0OrderPageInfoVO T0下单页信息
   * @param interestFreeDaysPackagingExp 免息天数包装实验参数
   * @return T0FreeInterestShowVo，如果构建失败返回 null
   */
  public T0FreeInterestShowVo buildT0InterestRateReductionPerTerm(Long userId, LoanProductConfigVO productVO,
      BigDecimal amount, Long couponId, T0OrderPageInfoVO t0OrderPageInfoVO, boolean interestFreeDaysPackagingExp) {
    if (t0OrderPageInfoVO == null) {
      return null;
    }

    try {
      T0InterestRateReductionPerTermBuilder builder = null;

      if (interestFreeDaysPackagingExp) {
        // 需要验证是否有有效数据（通过构建器内部验证）
        builder = orderPageInterestFreeDaysServiceBuilder;
      }
      // 否则，如果满足原有条件，使用 T0OrderPageInfoVO 构建
      else if (isOriginalT0ConditionMet(t0OrderPageInfoVO)) {
        builder = new T0OrderPageInfoVOBuilder();
      } else {
        return null;
      }

      if (builder == null) {
        return null;
      }

      return builder.build(userId, productVO.sdkType, amount, couponId, productVO, t0OrderPageInfoVO);
    } catch (Exception e) {
      log.warn("Failed to build T0 interest rate reduction per term for userId: {}, productId: {}",
          userId, productVO != null ? productVO.getId() : null, e);
      return null;
    }
  }

  /**
   * 判断是否满足原有T0展示条件
   * 条件：isShow() 为 true && afRankT0Style 是 "top" 或 "mid"
   */
  private boolean isOriginalT0ConditionMet(T0OrderPageInfoVO t0OrderPageInfoVO) {
    if (t0OrderPageInfoVO == null || !t0OrderPageInfoVO.isShow()) {
      return false;
    }
    String afRankT0Style = t0OrderPageInfoVO.getAfRankT0Style();
    return "top".equals(afRankT0Style) || "mid".equals(afRankT0Style);
  }

  /**
   * 此对象放回null对于前端是有含义的
   * @param discountDetail
   * @param userId
   * @param build
   * @param sdkType
   * @param loanCutInterestCouponVO
   * @param joinFreeTerms
   * @param orderDiscounts
   * @return
   */
  public RepayPlanDisplay buildRepayPlanDisplay(DiscountDetailResult discountDetail, Long userId, Long build, SDKType sdkType,
                                                LoanCutInterestCouponVO loanCutInterestCouponVO, boolean joinFreeTerms,
                                                OrderDiscounts orderDiscounts) {
    EcAsserts.assertNotNull(discountDetail, "discountDetail is null for buildRepayPlanDisplay, userId is {}", userId);

    try {
      List<TT> repayPlanCellList = new ArrayList<>();
      RepayPlanReduceGroup repayPlanV1ExpRes = repayPlanGrantCouponService.fetchExperimentLastV1Res(userId, sdkType, build);
      ProductDetailThreadLocal.putAbResult("repayPlanV1ExpRes", repayPlanV1ExpRes);
      String repayCouponV3Result = expLastResultClient.getResult("reloan_order_26h1-lending-abroad-renew-repay_plan_coupon_v3");

      Long couponExpiredTime = null;
      if (repayPlanV1ExpRes == RepayPlanReduceGroup.CUT_INTEREST_COUPON || "1".equals(repayCouponV3Result)) {
        BigDecimal lowInterestRate = BigDecimal.ZERO;
        if (orderDiscounts != null && orderDiscounts.getLowInterestDiscountRatio() != null) {
          lowInterestRate = orderDiscounts.getLowInterestDiscountRatio().setScale(2, RoundingMode.UP);
        }

        repayPlanCellList.add(TT.gen("Diskon Bunga {0}", NumberFormatter.percentFormat(sdkType.getLocale(), lowInterestRate)));
        if (loanCutInterestCouponVO != null && loanCutInterestCouponVO.configVO != null && loanCutInterestCouponVO.configVO.name != null) {
          repayPlanCellList.add(TT.gen(loanCutInterestCouponVO.configVO.name));
        }
        couponExpiredTime = getCouponExpiredTime(loanCutInterestCouponVO, userId);
      } else if (repayPlanV1ExpRes == RepayPlanReduceGroup.CUT_INTEREST) {
        BigDecimal totalDiscountRate = BigDecimal.ZERO;
        if (orderDiscounts != null && orderDiscounts.getTotalDiscountRatio() != null) {
          totalDiscountRate = orderDiscounts.getTotalDiscountRatio().setScale(2, RoundingMode.UP);
        }

        repayPlanCellList.add(TT.gen("Diskon Bunga {0}", NumberFormatter.percentFormat(sdkType.getLocale(), totalDiscountRate)));
      } else {
        return null;
      }
      if (joinFreeTerms) {
        repayPlanCellList.add(TT.gen("免一期"));
      }
      return RepayPlanDisplay.from(repayPlanCellList, couponExpiredTime);
    } catch (Exception e) {
      log.error("buildRepayPlanCellList error", e);
      return null;
    }
  }

  private Long getCouponExpiredTime(LoanCutInterestCouponVO couponVO, Long userId) {
    if (couponVO == null) {
      return null;
    }
    try {
      Pair<Boolean, Long> pair = loanUserCouponService.hasNoCouponFromRule(ImmutableList.of(couponVO.id),
          homepageV5Config.getRepayPlanCutInterestCouponId(expConditionFor2026H1Service.loanTimeLtOne(userId)));
      if (!pair.getLeft()) {
        Long couponTimeExpired = couponVO.timeExpired;
        if (couponTimeExpired == null) {
          return null;
        }
        Long now = Clock.now();
        if (couponTimeExpired > now) {
          //过期时间+30秒，防止定时失效job还未执行，最长倒计时1小时
          couponTimeExpired = Math.min(couponTimeExpired + 30 * Clock.MILLS_PER_SECOND, now + Clock.MILLS_PER_HOUR);
        }
        return couponTimeExpired;
      }
    } catch (Exception e) {
      log.error("getCouponExpiredTime error", e);
    }
    return null;
  }

  private void buildRepayPlanBanner(SeaProductFeeData data, Long userId) {
    try {
      List<Long> ruleIds = cashLoanConfig.getRepayPlanBannerCouponGrantRuleIds();
      if (CollectionUtils.isEmpty(ruleIds)) {
        return;
      }
      for (Long ruleId : ruleIds) {
        CouponGrantRuleVO couponGrantRuleVO = couponGrantRuleService.get(ruleId);
        if (couponGrantRuleVO == null || couponGrantRuleVO.ruleConfig == null || couponGrantRuleVO.ruleConfig.config == null) {
          continue;
        }
        BaseRuleGrantConfig baseConfig = couponGrantRuleVO.ruleConfig.config;
        if (baseConfig instanceof FixedDayPercentCutInterestRuleGrantConfig) {
          FixedDayPercentCutInterestRuleGrantConfig config = (FixedDayPercentCutInterestRuleGrantConfig) baseConfig;
          LoanUserCouponVO couponVO = loanUserCouponService.findOneValidByUserIdAndConfigId(userId, config.couponConfigId);
          if (couponVO == null) {
            continue;
          }
          RepayPlanBanner repayPlanBanner = new RepayPlanBanner();
          repayPlanBanner.setText(String.format("Nikmati Bunga Rp0 selama %s Hari", config.discountDays));
          repayPlanBanner.setCouponExpiredTime(couponVO.getTimeExpired());
          data.repayPlanBanner = repayPlanBanner;
          return;
        }
      }
    } catch (Exception e) {
      log.error("buildRepayPlanBanner error", e);
    }
  }

  /**
   * 某个产品，用户选择的借款金额最大时，折扣金额最大的优惠券
   * 这里的最大借款金额为 用户剩余额度和产品额度上限的最小值
   */
  public LoanCutInterestCouponVO getCurrentMaxDiscountCouponVO(Long userId, BigDecimal remainingCredits, Long productId) {

    LoanProductConfigVO productConfigVO = productConfigService.getProductVO(productId);
    BigDecimal maxLoanAmount = remainingCredits.min(productConfigVO.maxCredits.getAmountInYuan());

    SeaProductFeeCache feeCache = ecHomePageProductTool.getProductFeeCache(productId, maxLoanAmount);
    OrderInstalmentPlan oiPlan = feeCache.oiPlan;
    SDKType sdkType = feeCache.productConfigVO.sdkType;
    //如果后置利息为0，则直接返回
    if (BigDecimalHelper.equals(oiPlan.order.postInterest, BigDecimal.ZERO)) {
      return null;
    }
    return loanUserCouponService.fetchMaxCutInterestCoupon(userId, oiPlan.order.principal, oiPlan, sdkType);
  }


  /**
   * 根据 DiscountDetailResult 与 feeCache 一次性构建完整 SeaProductFeeData（含基础信息、费用展示、分期计划、displayData）。
   */
  private SeaProductFeeData buildFeeDisplayData(Long userId,
      LoanProductConfigVO productVO,
      SeaProductFeeCache feeCache,
      CalcFeeScale feeScale,
      DiscountDetailResult discountDetail,
      HomeDisplayStrategy feeDetailDisplayComplianceOptimizationStrategy,
      HomeDisplayStrategy orderPlanStyleFor363,
      HomeDisplayStrategy productPageStyleFor371,
      T0OrderPageInfoVO t0OrderPageInfoVO,
      boolean interestFreeDaysPackagingExp,
      Long couponId,
      Boolean joinFreeTerms,
      OrderDiscounts orderDiscounts) {
    SeaHomeOrderInstalmentPlan basePlan = discountDetail.getBasePlan();
    SeaHomeOrderInstalmentPlan actualPlan = discountDetail.getActualPlan();
    SeaHomeOrderInstalmentPlan standardPlan = discountDetail.getStandardPlan();
    SeaHomeOrderInstalmentPlan standardPlanForFreeTerms = discountDetail.getStandardPlanForFreeTerms();

    SeaProductFeeData data = SeaProductFeeData.from(
        productVO.id,
        feeCache.loanAmount,
        feeCache.terms,
        feeCache.days,
        feeCache.timePeriodInDayUnit,
        feeCache.receivedAmount,
        feeCache.firstRepayTime,
        feeCache.ppn,
        discountDetail.getDayInterestRate(),
        discountDetail.getStandardInterestRate()
    );
    data.excludePlatformFee = discountDetail.isExcludePlatformFee();
    data.excludePpn = discountDetail.isExcludePpn();
    data.postPlatformFeeRate = discountDetail.getPostPlatformFeeRate();
    data.lowInterestProduct = discountDetail.isLowInterestProduct();

    YqgLocale locale = productVO.sdkType.getLocale();
    data.totalFee = actualPlan.totalFee.toString();
    data.totalFeeAmount = actualPlan.totalFee;
    data.totalFeeAmountBeforeDeduct = standardPlanForFreeTerms.totalFee;
    data.totalFeeBeforeDeduct = data.totalFeeAmountBeforeDeduct.toString();
    data.timePeriodContent = getTimePeriodContent(productVO, productPageStyleFor371, joinFreeTerms);
    data.postInterestAmountBeforeDeduct = standardPlanForFreeTerms.postFee;
    data.postInterestAmount = actualPlan.postFee;
    data.repayAmount = actualPlan.repayAmount;
    data.repayAmountBeforeDeduct = standardPlanForFreeTerms.repayAmount;
    data.firstRepayAmount =
        NumberFormatter.format(locale, actualPlan.instalmentPlanList.get(0).owedAmount.setScale(feeScale.interestScale, feeScale.interestRoundType));
    data.firstRepayAmountNumber =
        actualPlan.instalmentPlanList.get(0).owedAmount.setScale(feeScale.interestScale, feeScale.interestRoundType);
    data.firstRepayStandardAmount =
        NumberFormatter.format(locale, standardPlan.instalmentPlanList.get(0).owedAmount.setScale(feeScale.interestScale, feeScale.interestRoundType));
    data.firstRepayStandardAmountNumber =
        standardPlan.instalmentPlanList.get(0).owedAmount.setScale(feeScale.interestScale, feeScale.interestRoundType);
    data.totalDeductAmount = resolveTotalDeductAmount(orderDiscounts, discountDetail);
    data.totalDeductAmountExcludeCoupon = resolveLowInterestDiscountAmount(orderDiscounts, discountDetail);
    data.productPaymentPlan = genDisplayTitleContent(productVO);
    data.additionalInfos = new ArrayList<>();
    if (data.terms == 1) {
      data.additionalInfos.add(SeaHomeGeneralResponse.fromNormal(TT.gen("还款日"), TT.gen("{0}", data.firstRepayTime)));
      data.additionalInfos.add(SeaHomeGeneralResponse.fromNormal(TT.gen("还款金额"), TT.gen("Rp{0}", data.firstRepayAmount)));
    } else {
      data.additionalInfos.add(SeaHomeGeneralResponse.fromNormal(TT.gen("首期还款日"), TT.gen("{0}", data.firstRepayTime)));
      data.additionalInfos.add(SeaHomeGeneralResponse.fromNormal(TT.gen("首期还款金额"), TT.gen("Rp{0}", data.firstRepayAmount)));
    }

    if (data.lowInterestProduct) {
      BigDecimal ratePreferentialFee = resolveLowInterestDiscountAmount(orderDiscounts, discountDetail);
      data.ratePreferentialStr =
          NumberFormatter.format(locale, ratePreferentialFee.setScale(feeScale.principalScale, feeScale.principalRoundType));
      data.ratePreferentialFee = ratePreferentialFee;
      data.ratePreferential = TT.gen("利率优惠: -Rp{0}", data.ratePreferentialStr);
    }

    int size = basePlan.instalmentPlanList.size();
    List<SeaInstalmentPlanVO> instalmentPlanResponseList = new ArrayList<>();
    Map<Integer, SeaHomeInstalmentPlan> standardInstalmentPlanMap =
        standardPlan.instalmentPlanList.stream().collect(Collectors.toMap(p -> p.termIdx, p -> p));
    Map<Integer, SeaHomeInstalmentPlan> actualInstalmentPlanMap =
        actualPlan.instalmentPlanList.stream().collect(Collectors.toMap(p -> p.termIdx, p -> p));
    Boolean useNewPeriodContent = productPageStyleFor371 == HomeDisplayStrategy.D ? Boolean.TRUE : Boolean.FALSE;

    T0FreeInterestShowVo t0InterestRateReductionPerTerm = buildT0InterestRateReductionPerTerm(
        userId, productVO, data.loanAmount, couponId, t0OrderPageInfoVO, interestFreeDaysPackagingExp);

    for (int i = 0; i < size; i++) {
      SeaHomeInstalmentPlan baseInstalment = basePlan.instalmentPlanList.get(i);
      SeaHomeInstalmentPlan actualInstalment = actualInstalmentPlanMap.get(baseInstalment.termIdx);
      SeaHomeInstalmentPlan standardInstalment = standardInstalmentPlanMap.get(baseInstalment.termIdx);
      SeaInstalmentPlanVO instalmentResponse = SeaInstalmentPlanVO.from(baseInstalment, actualInstalment, standardInstalment,
          feeDetailDisplayComplianceOptimizationStrategy, orderPlanStyleFor363, useNewPeriodContent, t0InterestRateReductionPerTerm,
          data.excludePlatformFee, data.excludePpn
      );
      instalmentPlanResponseList.add(instalmentResponse);
    }
    data.instalmentPlanList =
        instalmentPlanResponseList.stream().sorted(Comparator.comparingInt(v -> v.termIdx)).collect(Collectors.toList());

    SeaProductFeeData.FeeDisplayData feeDisplayData = new SeaProductFeeData.FeeDisplayData();
    feeDisplayData.dayRatePercentFormat = TT.gen("{0}", NumberFormatter.percentFormat(locale, data.dayInterestRate));
    feeDisplayData.standardRatePercentFormat =
        TT.gen("{0}", NumberFormatter.percentFormat(locale, data.standardInterestRate));

    feeDisplayData.termsRatePercentFormat = TT.gen("{0}", NumberFormatter.percentFormat(locale, data.getTermsInterestRate(homepageV5Config.getMaxTermsInterestRate())));
    feeDisplayData.standardTermsRatePercentFormat =
        TT.gen("{0}", NumberFormatter.percentFormat(locale, data.getStandardTermsRate(homepageV5Config.getMaxTermsInterestRate())));

    feeDisplayData.formattedTotalDeductAmount = TT.gen("Rp{0}", data.totalDeductAmount);

    SeaInstalmentPlanVO firstInstalmentPlanResponse = data.instalmentPlanList.get(0);
    BigDecimal firstInstalmentOwedAmount =
        firstInstalmentPlanResponse.owedAmount.setScale(feeScale.interestScale, feeScale.interestRoundType);
    BigDecimal firstInstalmentOwedAmountBeforeDeduct =
        firstInstalmentPlanResponse.owedAmountBeforeDeduct.setScale(feeScale.interestScale, feeScale.interestRoundType);
    if (firstInstalmentPlanResponse.owedAmount.compareTo(firstInstalmentPlanResponse.owedAmountBeforeDeduct) < 0) {
      feeDisplayData.formattedInitialPeriodRepayAmount =
          TT.gen("Rp{0}", NumberFormatter.format(locale, firstInstalmentOwedAmountBeforeDeduct));
    }
    feeDisplayData.formattedActualPeriodRepayAmount =
        TT.gen("Rp{0}", NumberFormatter.format(locale, firstInstalmentOwedAmount));

    HomeDisplayStrategy displayStrategyV7 = HomeDisplayStrategy.A;
    if (displayStrategyV7 == HomeDisplayStrategy.B && productVO.hasInterest()) {
      feeDisplayData.firstRepayTimeContent = TT.gen("{0}", data.firstRepayTime);
      feeDisplayData.firstRepayAmountContent = TT.gen("Rp{0}", data.firstRepayAmount);
    } else if (data.terms == 1) {
      feeDisplayData.firstRepayTimeContent = TT.gen("还款日{0}", data.firstRepayTime);
      feeDisplayData.firstRepayAmountContent = TT.gen("还款金额Rp{0}", data.firstRepayAmount);
    } else {
      feeDisplayData.firstRepayTimeContent = TT.gen("首期还款日{0}", data.firstRepayTime);
      feeDisplayData.firstRepayAmountContent = TT.gen("首期还款金额Rp{0}", data.firstRepayAmount);
    }

    if (data.lowInterestProduct) {
      BigDecimal rateDiscount = resolveLowInterestDiscountRate(orderDiscounts, discountDetail).setScale(2, RoundingMode.UP);
      feeDisplayData.rateDiscountContent = TT.gen("日利率{0}OFF", NumberFormatter.percentFormat(locale, rateDiscount));
    }

    feeDisplayData.monthProductPrompt = productVO.timeUnit == MONTH ? homepageV5Config.getMonthProductPrompt() : null;

    data.displayData = feeDisplayData;
    data.interestAmount = actualPlan.preFee;
    data.noInterestAmountExperiment = data.loanAmount.multiply(homepageV5Config.getNoInterestExperimentRate());

    return data;
  }

  private static BigDecimal resolveTotalDeductAmount(OrderDiscounts orderDiscounts, DiscountDetailResult discountDetail) {
    if (orderDiscounts != null && orderDiscounts.getTotalDiscountsAmount() != null) {
      return orderDiscounts.getTotalDiscountsAmount();
    }
    return discountDetail.calcTotalDiscountAmount();
  }

  private static BigDecimal resolveLowInterestDiscountAmount(OrderDiscounts orderDiscounts, DiscountDetailResult discountDetail) {
    if (orderDiscounts != null && orderDiscounts.getLowInterestDiscountAmount() != null) {
      return orderDiscounts.getLowInterestDiscountAmount();
    }
    return discountDetail.calcLowInterestDiscountAmount();
  }

  private static BigDecimal resolveLowInterestDiscountRate(OrderDiscounts orderDiscounts, DiscountDetailResult discountDetail) {
    if (orderDiscounts != null && orderDiscounts.getLowInterestDiscountRatio() != null) {
      return orderDiscounts.getLowInterestDiscountRatio();
    }
    return discountDetail.calcLowInterestDiscountRate();
  }

  private static TT getTimePeriodContent(LoanProductConfigVO productVO, HomeDisplayStrategy productPageStyleFor371, Boolean joinFreeTerms) {
    if (productPageStyleFor371 == null || productPageStyleFor371 == HomeDisplayStrategy.A) {
      return productVO.genTimePeriodContent();
    }
    if (productPageStyleFor371 != HomeDisplayStrategy.D) {
      return productVO.genTimePeriodContentFor371Style(joinFreeTerms);
    }
    return productVO.genTimeCicilanContent(joinFreeTerms);
  }

  private void buildFixedInterestAndTechFeeAmount(
      SeaProductFeeData data,
      CalcFeeScale feeScale
  ) {

    data.fixedInterestAmountBeforeDeduct = BigDecimalHelper.bigDecimalOrZero(data.receivedAmount)
        .multiply(loanInfoConfig.getFixedDailyInterestRate())
        .multiply(BigDecimal.valueOf(data.days))
        .setScale(feeScale.postInterestScale, feeScale.postInterestRoundType);

    if (BigDecimalHelper.greaterThan(data.fixedInterestAmountBeforeDeduct, data.postInterestAmountBeforeDeduct)) {
      data.fixedInterestAmountBeforeDeduct = data.postInterestAmountBeforeDeduct;
    }

    BigDecimal deductedFixedInterestAmount = BigDecimalHelper.divideRoundHalfUpToScale(
        data.totalDeductAmount.multiply(data.fixedInterestAmountBeforeDeduct),
        data.postInterestAmountBeforeDeduct, feeScale.postInterestScale);

    data.fixedInterestAmount = data.fixedInterestAmountBeforeDeduct
        .subtract(deductedFixedInterestAmount)
        .setScale(feeScale.postInterestScale, feeScale.postInterestRoundType);

    data.techFeeAmountBeforeDeduct = data.postInterestAmountBeforeDeduct
        .subtract(data.fixedInterestAmountBeforeDeduct)
        .setScale(feeScale.postInterestScale, feeScale.postInterestRoundType);

    if (BigDecimalHelper.lessThan(data.techFeeAmountBeforeDeduct, BigDecimal.ZERO)) {
      data.techFeeAmountBeforeDeduct = BigDecimal.ZERO;
    }

    data.techFeeAmount = data.postInterestAmount
        .subtract(data.fixedInterestAmount)
        .setScale(feeScale.postInterestScale, feeScale.postInterestRoundType);

    if (BigDecimalHelper.lessThan(data.techFeeAmount, BigDecimal.ZERO)) {
      data.techFeeAmount = BigDecimal.ZERO;
    }

  }

  private void checkPostInterestAmount(SeaProductFeeData data, CalcFeeScale feeScale) {
    BigDecimal totalPostInterestBeforeDeduct = data.fixedInterestAmountBeforeDeduct
        .add(data.techFeeAmountBeforeDeduct)
        .setScale(feeScale.postInterestScale, feeScale.postInterestRoundType);

    BigDecimal totalPostInterestAfterDeduct = data.fixedInterestAmount
        .add(data.techFeeAmount)
        .setScale(feeScale.postInterestScale, feeScale.postInterestRoundType);

    if (!BigDecimalHelper.equals(totalPostInterestBeforeDeduct, data.postInterestAmountBeforeDeduct)) {
      log.error("The postInterestBeforeDeduct does not match with the fixedInterest {} and techFee {}, expected {}, actual {}",
          data.fixedInterestAmountBeforeDeduct,
          data.techFeeAmountBeforeDeduct,
          data.postInterestAmountBeforeDeduct,
          totalPostInterestBeforeDeduct);
    }

    if (!BigDecimalHelper.equals(totalPostInterestAfterDeduct, data.postInterestAmount)) {
      log.error("The postInterestAfterDeduct does not match with the fixedInterest {} and techFee {}, expected {}, actual {}",
          data.fixedInterestAmount,
          data.techFeeAmount,
          data.postInterestAmount,
          totalPostInterestAfterDeduct);
    }
  }

  private void checkTotalRepaymentAmount(SeaProductFeeData data) {
    BigDecimal loanAmount = BigDecimalHelper.bigDecimalOrZero(data.receivedAmount).add(data.interestAmount);
    BigDecimal repayAmountBeforeDeduct = loanAmount
        .add(data.techFeeAmountBeforeDeduct)
        .add(data.fixedInterestAmountBeforeDeduct);

    BigDecimal repayAmountAfterDeduct = loanAmount
        .add(data.techFeeAmount)
        .add(data.fixedInterestAmount);

    if (!BigDecimalHelper.equals(repayAmountBeforeDeduct, data.repayAmountBeforeDeduct)) {
      log.error("The repayAmountBeforeDeduct does not match with the " +
              "receivedAmount {}, interestAmount {}, fixedInterest {} and techFee {}, " +
              "expected {}, actual {}",
          data.receivedAmount,
          data.interestAmount,
          data.fixedInterestAmountBeforeDeduct,
          data.techFeeAmountBeforeDeduct,
          data.repayAmountBeforeDeduct,
          repayAmountBeforeDeduct);
    }

    if (!BigDecimalHelper.equals(repayAmountAfterDeduct, data.repayAmount)) {
      log.error("The repayAmountAfterDeduct does not match with the " +
              "receivedAmount {}, interestAmount {}, fixedInterest {} and techFee {}, " +
              "expected {}, actual {}",
          data.receivedAmount,
          data.interestAmount,
          data.fixedInterestAmount,
          data.techFeeAmount,
          data.repayAmount,
          repayAmountAfterDeduct);
    }
  }

  private void buildFeeAmountList(LoanProductConfigVO productVO,
      CalcFeeScale feeScale,
      SeaProductFeeData data,
      Long userId,
      Long build,
      SeaCouponDetail couponDetail,
      SeaHomeOrderInstalmentPlan actualPlan,
      SeaHomeOrderInstalmentPlan standardPlan,
      HomeDisplayStrategy feeDetailDisplayComplianceOptimizationStrategy,
      HomeDisplayStrategy orderPlanStyleFor363,
      PlatformType platformType,
      SourceType sourceType) {
    if (userId == null) {
      return;
    }

    if (orderPlanStyleFor363 == HomeDisplayStrategy.B) {
      BigDecimal discountAmount = standardPlan.calcInterestSplitDiscountAmount(actualPlan);

      String feeCalculationDetailUrl =
          SourceType.isApiChannelFromH5(sourceType) ? homepageV5Config.getFeeCalculationDetailUrlForH5() :
              homepageV5Config.getFeeCalculationDetailUrl();
      if(RequestClientType.isWholeProcess(ImpliedContextUtils.requestClientType()) && homepageContentTool.isInH5WholeProcessOrderUIExp(userId)){
        //命中H5全流程下单对齐UI实验，协议链接跟api渠道保持一致，仅有文件域名不同。
        feeCalculationDetailUrl = homepageV5Config.getFeeCalculationDetailUrlForH5();
      }
      //totalFee是费用明细浮层的总费用，不从中剔除服务费，单独处理一下，这里面的其他字段前端处理不展示了
      BigDecimal totalFee = actualPlan.totalFeeBeforePlatformFeeExclusion;
      data.orderFeeDataDetail = SeaProductFeeData.OrderFeeDataDetail.from(homepageV5Config.getFeeCalculationDetailTitle(),
          feeCalculationDetailUrl, data.receivedAmount, totalFee, discountAmount, actualPlan.totalPlatformFee,
          actualPlan.totalFee.subtract(actualPlan.totalPlatformFee));
    }

    //进feeDetailDisplayComplianceOptimizationStrategy实验，分流结果为B的，统一走此方法
    if (feeDetailDisplayComplianceOptimizationStrategy == HomeDisplayStrategy.B) {
      buildFeeAmountListForComplianceOptimizationStrategyB(productVO.sdkType, feeScale, data, actualPlan, standardPlan, couponDetail, build, orderPlanStyleFor363, platformType);
      return;
    }
    buildFeeAmountListForNoPreInterestProduct(productVO.sdkType.getLocale(), feeScale, data, actualPlan, standardPlan, build, platformType);
  }

  private void buildFeeAmountListForComplianceOptimizationStrategyB(SDKType sdkType,
      CalcFeeScale feeScale,
      SeaProductFeeData data,
      SeaHomeOrderInstalmentPlan actualPlan,
      SeaHomeOrderInstalmentPlan standardPlan,
      SeaCouponDetail couponDetail,
      Long build,
      HomeDisplayStrategy orderPlanStyleFor363,
      PlatformType platformType) {
    YqgLocale locale = sdkType.getLocale();

    TT bubbleContent =
        actualPlan.preFee.compareTo(BigDecimal.ZERO) == 0 ? null : TT.gen("其中包含服务费{0} 借款金额{1}",
            AmountFormatter.format(locale.currency, actualPlan.preFee),
            AmountFormatter.format(locale.currency, actualPlan.loanAmount)
        );

    data.feeAmountList = new ArrayList<>();
    if (orderPlanStyleFor363 != HomeDisplayStrategy.B) {
      data.feeAmountList.add(SeaHomeGeneralResponse.fromBoldNormal(TT.gen("到账金额"), bubbleContent, TT.gen("Rp{0}",
          NumberFormatter.format(locale, data.receivedAmount.setScale(feeScale.principalScale, feeScale.principalRoundType)))));
    }

    if (data.lowInterestProduct) {
      // 有低息产品时，标准利息在实际利率后划线展示
      data.feeAmountList.add(SeaHomeGeneralResponse.fromPrompt(TT.gen("日利率(包含服务费)"), TT.gen("{0}", data.displayData.dayRatePercentFormat), TT.gen("{0}", data.displayData.standardRatePercentFormat)));
    } else {
      data.feeAmountList.add(SeaHomeGeneralResponse.fromNormal(TT.gen("日利率(包含服务费)"), TT.gen("{0}", data.displayData.dayRatePercentFormat)));
    }

    String totalFeeStr =
        NumberFormatter.format(locale, standardPlan.totalFee.setScale(feeScale.postInterestScale, feeScale.postInterestRoundType));
    addInterestFeeContent(feeScale, data, standardPlan, build, locale, totalFeeStr, platformType);
    if (data.lowInterestProduct) {
      data.feeAmountList.add(SeaHomeGeneralResponse.fromHighLight(TT.gen("利率优惠"), TT.gen("-Rp{0}", data.ratePreferentialStr)));
    }
    if (couponDetail != null) {
      data.feeAmountList.add(SeaHomeGeneralResponse.fromHighLight(TT.gen("Coupon"), TT.gen("-Rp{0}", NumberFormatter.format(locale, couponDetail.amount))));
    }
    data.feeAmountList.add(SeaHomeGeneralResponse.fromDivideLine());
    data.feeAmountList.add(SeaHomeGeneralResponse.fromBoldNormal(TT.gen("应还金额"), TT.gen("Rp{0}", NumberFormatter.format(locale, actualPlan.repayAmount.setScale(feeScale.principalScale, feeScale.principalRoundType)))));
    String feeCalculationDetailTitle = homepageV5Config.getFeeCalculationDetailTitle();
    String feeCalculationDetailUrl = homepageV5Config.getFeeCalculationDetailUrl();
    //息费详情说明
    if (build >= 32400 && StringUtils.isNotEmpty(feeCalculationDetailTitle) && StringUtils.isNotEmpty(feeCalculationDetailUrl)) {
      data.feeAmountList.add(SeaHomeGeneralResponse.fromTipLink(TT.gen(feeCalculationDetailTitle), feeCalculationDetailUrl));
    }
  }

  private void addInterestFeeContent(CalcFeeScale feeScale, SeaProductFeeData data, SeaHomeOrderInstalmentPlan standardPlan, Long build, YqgLocale locale, String totalFeeStr, PlatformType platformType) {
    if (needDisplayNewInterestFeeContent(build, platformType)) {
      String totalPlatformFee = AmountFormatter.format(locale.currency, standardPlan.totalPlatformFee.setScale(feeScale.postInterestScale, feeScale.postInterestRoundType));
      String totalInterestExcludeFee = AmountFormatter.format(locale.currency, standardPlan.totalFee.subtract(standardPlan.totalPlatformFee).setScale(feeScale.postInterestScale, feeScale.postInterestRoundType));
      data.feeAmountList.add(SeaHomeGeneralResponse.fromBoldNormalRight(TT.gen("综合息费"),
          null,
          TT.gen("综合息费包含借款服务费：{0}借款利息：{1}", totalPlatformFee, totalInterestExcludeFee),
          TT.gen("Rp{0}", totalFeeStr)));
    } else {
      data.feeAmountList.add(SeaHomeGeneralResponse.fromNormal(TT.gen("综合息费"), TT.gen("Rp{0}", totalFeeStr)));
    }
  }

  private boolean needDisplayNewInterestFeeContent(Long build, PlatformType platformType) {
    if (platformType.isWeb()) {
      return homepageV5Config.getProductDetailInterestChangeSwitchForH5();
    }
    return build.compareTo(homepageV5Config.getProductDetailInterestChangeStartBuild()) >= 0;
  }

  //无砍头息产品的费用详情
  private void buildFeeAmountListForNoPreInterestProduct(YqgLocale locale, CalcFeeScale feeScale, SeaProductFeeData data, SeaHomeOrderInstalmentPlan actualPlan, SeaHomeOrderInstalmentPlan standardPlan, Long build, PlatformType platformType) {

    data.feeAmountList = new ArrayList<>();
    data.feeAmountList.add(SeaHomeGeneralResponse.fromBoldNormal(TT.gen("到账金额"), TT.gen("{0}", AmountFormatter.format(locale.currency, actualPlan.receivedAmount))));
    //无砍头息用户，需要展示一个假的前置利息减免，固定百分之10
    BigDecimal standardPreFeeForV6B = actualPlan.loanAmount.multiply(new BigDecimal("0.1"));
    data.feeAmountList.add(SeaHomeGeneralResponse.fromPrompt(TT.gen("放款费"), TT.gen("{0}", AmountFormatter.format(locale.currency, actualPlan.preFee)), TT.gen("{0}", AmountFormatter.format(locale.currency, standardPreFeeForV6B))));

    data.feeAmountList.add(SeaHomeGeneralResponse.fromDivideLine());

    data.feeAmountList.add(SeaHomeGeneralResponse.fromBoldBold(TT.gen("应还金额"), null));
    data.feeAmountList.add(SeaHomeGeneralResponse.fromBoldNormal(TT.gen("本金"), TT.gen("{0}", AmountFormatter.format(locale.currency, actualPlan.loanAmount))));
    BigDecimal discountAmount = standardPlan.calcInterestSplitDiscountAmount(actualPlan);
    TT bubbleContent = BigDecimalHelper.lessThanOrEqual(discountAmount, BigDecimal.ZERO) ? null : TT.gen("已优惠{0}", AmountFormatter.format(locale.currency, discountAmount));

    addInterestFeeAmountForLowInterestProduct(locale, data, actualPlan, build, bubbleContent, platformType);

    data.feeAmountList.add(SeaHomeGeneralResponse.fromDivideLine());

    String feeCalculationDetailTitle = homepageV5Config.getFeeCalculationDetailTitle();
    String feeCalculationDetailUrl = homepageV5Config.getFeeCalculationDetailUrl();
    //息费详情说明
    if (build >= 32400 && StringUtils.isNotEmpty(feeCalculationDetailTitle) && StringUtils.isNotEmpty(feeCalculationDetailUrl)) {
      data.feeAmountList.add(SeaHomeGeneralResponse.fromTipLink(TT.gen(feeCalculationDetailTitle), feeCalculationDetailUrl));
    }
  }

  private void addInterestFeeAmountForLowInterestProduct(YqgLocale locale, SeaProductFeeData data, SeaHomeOrderInstalmentPlan actualPlan, Long build, TT bubbleContent, PlatformType platformType) {
    SeaHomeGeneralResponse repayInterestResponse;
    if (needDisplayNewInterestFeeContent(build, platformType)) {
      String totalPlatformFee = AmountFormatter.format(locale.currency, actualPlan.totalPlatformFee);
      String totalInterestExcludeFee = AmountFormatter.format(locale.currency, actualPlan.totalFee.subtract(actualPlan.totalPlatformFee));
      repayInterestResponse = SeaHomeGeneralResponse.fromBoldNormalRight(TT.gen("综合息费"),
          bubbleContent,
          TT.gen("综合息费包含借款服务费：{0}借款利息：{1}", totalPlatformFee, totalInterestExcludeFee),
          TT.gen("{0}", AmountFormatter.format(locale.currency, actualPlan.totalFee)));
    } else {
      repayInterestResponse = bubbleContent == null
          ? SeaHomeGeneralResponse.fromBoldNormal(TT.gen("综合息费"), TT.gen("{0}", AmountFormatter.format(locale.currency, actualPlan.totalFee)))
          : SeaHomeGeneralResponse.fromBoldNormalRight(TT.gen("综合息费"), bubbleContent, null, TT.gen("{0}", AmountFormatter.format(locale.currency, actualPlan.totalFee)));
    }
    data.feeAmountList.add(repayInterestResponse);
  }

  public TT genErrorToastStr() {
    return TT.gen("借款金额不满足分期条件");
  }
}
