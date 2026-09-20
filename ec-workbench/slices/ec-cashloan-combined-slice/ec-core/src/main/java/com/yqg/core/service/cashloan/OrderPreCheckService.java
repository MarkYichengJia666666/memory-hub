package com.yqg.core.service.cashloan;

import com.google.common.collect.Maps;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigType;
import com.yqg.core.service.abtest.ExpLastResultRunningClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.activity.ActivityClientService.LoanConditionCheckResult;
import com.yqg.core.service.apichannel.ApiChannelOrderCheckService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.SeaProductFeeCache;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanUserSupplementCreateOrderService;
import com.yqg.core.service.cashloan.postpose.PostposeRewardTierStrategy;
import com.yqg.core.service.cashloan.postpose.PostposeRewardTierVO;
import com.yqg.core.service.cashloan.vo.BioAuthProcessTextVO;
import com.yqg.core.service.cashloan.vo.CreateOrderOtpCheckResult;
import com.yqg.core.service.cashloan.vo.MotivateLoanActivityCopyConfigVO;
import com.yqg.core.service.cashloan.vo.OrderAgreementVO;
import com.yqg.core.service.general.mcresource.AppResourceManagerService;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.coupon.vo.CutInterestVO;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.loan.discount.CutInterestVOResolver;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.core.service.mobile.verification.WaOtpAutofillExpService;
import com.yqg.core.service.payment.factory.collectioninfo.CollectInformationService;
import com.yqg.core.service.payment.factory.collectioninfo.processors.enums.CollectInformationScene;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.secure.UserSecureConfig;
import com.yqg.core.service.user.enums.CreateOrderPopupType;
import com.yqg.core.service.orderpage.vo.BorrowMorePopupParams;
import com.yqg.core.userflow.domain.loan.model.discounts.InstalmentDiscounts;
import com.yqg.core.userflow.domain.loan.service.discounts.IBillDiscountsService;
import com.yqg.core.userflow.domain.loan.service.style.IOrderCheckExpeService;
import com.yqg.core.userflow.domain.order.model.RecentSimilarPayoutInfo;
import com.yqg.core.util.IdnAmountFormatter;
import com.yqg.core.util.OppoUserCheckUtil;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.constant.AppResourceExtraParam;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.MaskUtils;
import com.yqg.mc.common.spring.response.appresource.AppResourceResponse;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author shubo
 * @date 17/04/25 16.10
 */
@Service
@Slf4j
public class OrderPreCheckService {

  // 重复借款拦截弹窗透传给 MC 资源位的 extraParams key；MC 侧由 CustomSingleValueFilterRuleProcessor
  // 依据 DUPLICATE_LOAN_INTERCEPT_SHOW_KEY 做展示过滤，金额/时间字段供弹窗模板直接展示（包级可见供同包测试引用）。
  static final String DUPLICATE_LOAN_INTERCEPT_SHOW_KEY = "duplicateLoanInterceptShow";
  static final String DUPLICATE_LOAN_LAST_PAYOUT_AMOUNT_KEY = "duplicateLoanLastPayoutAmount";
  static final String DUPLICATE_LOAN_LAST_PAYOUT_TIME_KEY = "duplicateLoanLastPayoutTime";

  // 温馨提示二次确认弹窗（TAPD-363868）透传给 MC 资源位的 extraParams key；金额/期限/首期应还
  // 直接复用 OrderAgreementService.fetchOrderAgreementExp 返回值，与协议页展示口径一致（包级可见供同包测试引用）。
  static final String QUICK_CONFIRM_REMINDER_SHOW_KEY = "quickConfirmReminderShow";
  static final String QUICK_CONFIRM_REMINDER_LOAN_AMOUNT_KEY = "quickConfirmReminderLoanAmount";
  static final String QUICK_CONFIRM_REMINDER_TENOR_KEY = "quickConfirmReminderTenor";
  static final String QUICK_CONFIRM_REMINDER_FIRST_REPAY_KEY = "quickConfirmReminderFirstRepay";

  /** 加借推荐弹窗 MC 过滤信号（TAPD-371433） */
  static final String BORROW_MORE_SHOW_KEY = "firstLoanBorrowMoreShow";
  /** 加借推荐弹窗整包数据 JSON（含 couponId / targetAmount / 展示金额） */
  static final String BORROW_MORE_POPUP_DATA_KEY = "firstLoanBorrowMorePopupData";

  @Autowired
  private AppResourceManagerService appResourceManagerService;
  @Autowired
  private CollectInformationService collectInformationService;
  @Autowired
  private LoanUserOrderService loanUserOrderService;
  @Autowired
  private HomepageStatusTool homepageStatusTool;
  @Autowired
  private CashLoanUserSupplementCreateOrderService supplementCreateOrderService;
  @Autowired
  private ProductConfigService productConfigService;
  @Autowired
  private ApiChannelOrderCheckService apiChannelOrderCheckService;
  @Autowired
  private LoanBankAccountService loanBankAccountService;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private LoanCreditsQuotaService loanCreditsQuotaService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private OppoUserCheckUtil oppoOrderCheck;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private ExpLastResultRunningClient expLastResultRunningClient;
  @Autowired
  private OrderAgreementService orderAgreementService;
  @Autowired
  private IBillDiscountsService billDiscountsService;
  @Autowired
  private CutInterestVOResolver cutInterestVOResolver;
  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private PostposeRewardTierStrategy postposeRewardTierStrategy;
  @Autowired
  private IOrderCheckExpeService orderCheckExpeService;
  @Autowired
  private WaOtpAutofillExpService waOtpAutofillExpService;
  @Autowired
  private OrderBiometricVerifyService orderBiometricVerifyService;
  @Autowired
  private UserSecureConfig userSecureConfig;

  /** 复贷新样式日供换算除数（30 天/月），抽常量避免 magic value（TAPD-1353291 FR-009）。 */
  private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);
  /** 日供增量字符串前缀，含 {@code /hari} 单位后缀（v2.3 印尼语硬编码约束）。 */
  private static final String DAILY_PAYMENT_DELTA_ZERO = "+Rp0/hari";
  private static final String DAILY_PAYMENT_DELTA_SUFFIX = "/hari";
  /** 到手金额零/极端兜底字符串。 */
  private static final String DISBURSE_AMOUNT_ZERO = "+Rp0";
  /** 多借金额零兜底字符串。 */
  private static final String EXTRA_LOAN_AMOUNT_ZERO = "Rp 0";
  /** 脱敏放款账号分隔符（PRD §4.2.1 / spec FR-006）。 */
  private static final String DISBURSE_ACCOUNT_MASK = "**";

  /**
   * 拉取下单页一键借款后置资源位字段。
   *
   * @param viewerContext 用户上下文
   * @param feeDeductAmount 优惠金额
   * @param loanAmount 用户下单金额
   * @param selectedProductId 选定产品 hash id
   * @param expResult 实验入组判定结果（含上层 systemDesignStyle 与新样式实验 styleGroup）
   * @param couponId 用户当前选中的券 id（可为 null）
   * @param hashPaymentCredentialId 本次下单选定的放款账号 hash id（可为 null），TAPD-1353291 透传供
   * 后续 buildDisburseAccountDisplay 取卡使用；目前 Phase 1 不消费，Phase 2 起按 styleGroup 决定是否下发脱敏字段
   * @param hasInteracted 本次 session 确认前是否发生过有效交互（TAPD-363868），true=发生过交互，false/null=确认前零交互
   * @param stayDurationMs 本次 session 首次进入下单页至点击确认借款的耗时（毫秒，TAPD-363868），可为 null
   * @return MC 返回的资源位响应
   */
  public AppResourceResponse fetchResourceInfo(LoanApiViewerContext viewerContext, BigDecimal feeDeductAmount, BigDecimal loanAmount,
      String selectedProductId, LoanConditionCheckResult expResult, Long couponId, Long hashPaymentCredentialId,
      Boolean hasInteracted, Long stayDurationMs) {

    HashMap<@Nullable String, @Nullable Object> params = Maps.newHashMap();

    AppResourceExtraParam.APP_CURRENT_OPEN_TIME.setValue(params, viewerContext.appCurrentOpenTime);
    List<String> resourceTypes = getResourceTypesForCollectInfo(viewerContext.sourceType, viewerContext.userId, viewerContext.build, viewerContext.loanAccountId, params);

    setResourceParamForLoanActivity(loanAmount, selectedProductId, feeDeductAmount, params, resourceTypes, expResult, viewerContext, couponId,
        hashPaymentCredentialId, hasInteracted, stayDurationMs);

    if (resourceTypes.isEmpty()) {
      return new AppResourceResponse();
    }

    GeneralPageConfigParam generalPageConfigParam = GeneralPageConfigParam.from(viewerContext, params);

    return appResourceManagerService.getResourceFromMcForApp(generalPageConfigParam, resourceTypes);
  }

  public List<String> getResourceTypesForCollectInfo(SourceType sourceType, Long userId, Long build, Long loanAccountId, HashMap<@Nullable String, @Nullable Object> params) {
    List<String> resourceTypes = new ArrayList<>();
    if (sourceType == SourceType.IOS) {
      return resourceTypes;
    }
    Boolean showMotherLastName = collectInformationService.showMotherLastName(loanAccountId, userId, build, CollectInformationScene.BEFORE_CREATE_ORDER);
    Boolean pobByUser = collectInformationService.showPobUser(loanAccountId, userId, build, CollectInformationScene.BEFORE_CREATE_ORDER);
    Boolean showCompanyName = collectInformationService.showCompanyName(loanAccountId, userId, build, CollectInformationScene.BEFORE_CREATE_ORDER);

    if (showMotherLastName || pobByUser || showCompanyName) {
      // 使用单值规则判断是否需要填充母亲name or pob
      params.put("needFillMotherLastName", showMotherLastName);
      params.put("needFillPobByUser", pobByUser);
      params.put("needFillCompanyName", showCompanyName);
      resourceTypes.add(GeneralPageConfigType.COLLECT_INFORMATION.name());
      log.info("current user have resourceTypes, resourceTypes is  {}", JsonUtils.toString(resourceTypes));
    }
    return resourceTypes;
  }

  private void setResourceParamForLoanActivity(BigDecimal loanAmount, String selectedProductId, BigDecimal feeDeductAmount,
      HashMap<@Nullable String, @Nullable Object> params, List<String> resourceTypes, LoanConditionCheckResult expResult,
      LoanApiViewerContext viewerContext, Long couponId, Long hashPaymentCredentialId, Boolean hasInteracted, Long stayDurationMs) {
    if (Objects.isNull(loanAmount) || StringUtils.isBlank(selectedProductId)) {
      return;
    }
    boolean inActivity = Boolean.TRUE.equals(expResult.getInActivity());
    Optional<RecentSimilarPayoutInfo> duplicateLoanPayoutInfo = orderCheckExpeService.abTestDuplicateLoanInterceptExpe(
        viewerContext.userId, viewerContext.loanAccountId, viewerContext.build, loanAmount,
        UserFlowExperimentEnum.DUPLICATE_LOAN_INTERCEPT.getKey());
    boolean shouldShowDupDialog = duplicateLoanPayoutInfo.isPresent();
    // TAPD-363868：确认前零交互+极速确认+当日重复进页 温馨提示；PRD §3.3 规定重复借款拦截优先，命中时不再判定本规则
    // TAPD-366796：该实验入组新增首贷/复贷下单金额门槛判断，逻辑内聚在 abTestQuickConfirmReminderExpe 内部
    boolean shouldShowQuickConfirmReminder = !shouldShowDupDialog && orderCheckExpeService.abTestQuickConfirmReminderExpe(
        viewerContext, loanAmount, hasInteracted, stayDurationMs,
        UserFlowExperimentEnum.QUICK_CONFIRM_REMINDER.getKey());
    Optional<BorrowMorePopupParams> borrowMoreResult = orderCheckExpeService.abTestFirstLoanBorrowMoreExpe(
        viewerContext, loanAmount, decodeSelectedProductId(selectedProductId),
        UserFlowExperimentEnum.FIRST_LOAN_BORROW_MORE_POPUP.getKey());
    boolean shouldShowBorrowMore = borrowMoreResult.isPresent();
    log.debug("[BorrowMorePopup] createOrderCheck extraParam decision, userId={}, show={}",
        viewerContext.userId, shouldShowBorrowMore);

    if (!inActivity && !shouldShowDupDialog && !shouldShowQuickConfirmReminder && !shouldShowBorrowMore) {
      return;
    }
    String incentiveActivityType = GeneralPageConfigType.CREATE_ORDER_INCENTIVE_ACTIVITY.name();
    if (!resourceTypes.contains(incentiveActivityType)) {
      resourceTypes.add(incentiveActivityType);
    }
    if (shouldShowDupDialog) {
      setDuplicateLoanInterceptParams(params, duplicateLoanPayoutInfo.get());
    }
    if (shouldShowQuickConfirmReminder) {
      setQuickConfirmReminderParams(params, viewerContext, loanAmount, selectedProductId, couponId);
    }
    if (shouldShowBorrowMore) {
      setBorrowMorePopupParams(params, borrowMoreResult.get(), viewerContext.sdkType);
    }
    if (!inActivity) {
      return;
    }

    Long productId = YqgHashids.decode(selectedProductId);
    // 产品ID（首/复贷通用）
    AppResourceExtraParam.SELECTED_PRODUCT_ID.setValue(params, productId);
    // 下单金额（首/复贷通用）
    AppResourceExtraParam.USER_INPUT_AMOUNT.setValue(params, loanAmount);
    // 优惠券ID（首/复贷通用）
    AppResourceExtraParam.COUPON_ID.setValue(params, couponId);
    // 激励金额阈值（首/复贷通用）
    params.put("motivateLoanThreshold", expResult.getLoanThreshold());
    // 样式实验（首/复贷通用）
    params.put("systemDesignStyle", expResult.getSystemDesignStyle());
    // 复贷一键借款后置新样式 A/B 实验分组（TAPD-1353291）：始终透传，对照/空白/未入组场景下值可能为 null，
    // Phase 2 据此决定是否下发 disburseAccountDisplay / extraLoanAmountFormatted 等新字段
    params.put("styleGroup", expResult.getStyleGroup());

    // 激励文案（仅复贷用）
    MotivateLoanActivityCopyConfigVO motivateCopy = cashLoanConfig.getMotivateLoanActivityCopyConfig();
    // motivateLoanActivityDesc：保留旧"+30%"标签注入不动（CONTROL_GROUP 完全兼容；新档前端弹窗不渲染该字段）
    params.put("motivateLoanActivityDesc", TT.gen(motivateCopy.getActivityDescTemplate(), motivateCopy.getActivityExtraPercent()));
    // motivateLoanActivityRewardDesc：按 diff 选档；命中返回 tier.rewardDesc；未命中（CONTROL_GROUP / diff 不在 4 档）fallback 到旧模板
    PostposeRewardTierVO tier = postposeRewardTierStrategy.resolveByAmounts(expResult.getLoanThreshold(), loanAmount);
    Object rewardDesc = (tier != null)
        ? tier.rewardDesc
        : TT.gen(motivateCopy.getRewardDescTemplate(), motivateCopy.getRewardCashbackPercent(), motivateCopy.getRewardCouponText());
    params.put("motivateLoanActivityRewardDesc", rewardDesc);
    // 激励前还款计划（仅复贷用）：当前输入金额 + 用户当前选中的券
    OrderAgreementVO orderAgreementVOBefore = orderAgreementService.fetchOrderAgreementExp(viewerContext, couponId, loanAmount, selectedProductId);
    params.put("orderAgreementVOBefore", orderAgreementVOBefore);
    params.put("formattedLoanAmountBefore", Optional.ofNullable(orderAgreementVOBefore).map(OrderAgreementVO::getFormattedLoanAmount).orElse(null));
    params.put("productTimeContentBefore", Optional.ofNullable(orderAgreementVOBefore).map(OrderAgreementVO::getProductTimeContent).orElse(null));
    // 激励后还款计划（仅复贷用）：loanThreshold + 该金额下最优减息券（内部单次 feeCache）
    OrderAgreementVO orderAgreementVOAfter = orderAgreementService.fetchOrderAgreementExpWithOptimalCoupon(viewerContext, expResult.getLoanThreshold(), selectedProductId);
    params.put("orderAgreementVOAfter", orderAgreementVOAfter);
    params.put("formattedLoanAmountAfter", Optional.ofNullable(orderAgreementVOAfter).map(OrderAgreementVO::getFormattedLoanAmount).orElse(null));
    params.put("productTimeContentAfter", Optional.ofNullable(orderAgreementVOAfter).map(OrderAgreementVO::getProductTimeContent).orElse(null));
    // 复贷一键借款后置 - 新旧素材分流互斥标识（TAPD-1353291 / 单值规则 key 互斥方案）。
    // 旧素材 724 (策略 1098 / rule 962) 绑定 motivateLoanActivityForReLoan == "true"；
    // 新素材（v2.6 新建）绑定 motivateLoanActivityForReLoanStyleV1 == "true"。
    // 互斥规则：命中新样式实验组 1/2 → 新 key=true、旧 key=false（旧素材被 rule 962 筛掉）；
    // 其它（对照组 / 空白组 / null / 未入组）→ 旧 key=true、新 key=false，保持现网真诚版下发。
    boolean motivateLoanActivityHit = expResult.isReloan() && expResult.getInActivity();
    boolean styleExperimentHit = motivateLoanActivityHit && isStyleExperimentHit(expResult.getStyleGroup());
    params.put("motivateLoanActivityForReLoan", motivateLoanActivityHit && !styleExperimentHit);
    params.put("motivateLoanActivityForReLoanStyleV1", styleExperimentHit);

    // 复贷一键借款后置新样式 A/B 实验（TAPD-1353291）：仅在复贷 + 命中实验组 1/2 时下发 4 类格式化字段；
    // 对照组 / 空白组 / null 保持现网真诚版字段不变（spec FR-012、NFR-002）。
    if (styleExperimentHit) {
      // TAPD-1353291 [US5]：升档前 / 后产品跳变检测，仅 WARN 日志、不阻塞、不降级（FR-014）；
      // 字段下发流程不受跳变影响，无论命中与否都按原顺序写入下游 4 类 KV。
      SeaProductFeeCache feeBefore = safeGetProductFeeCache(productId, loanAmount);
      SeaProductFeeCache feeAfter = safeGetProductFeeCache(productId, expResult.getLoanThreshold());
      checkProductDriftAndWarn(feeBefore, feeAfter, viewerContext.userId, loanAmount, expResult.getLoanThreshold());

      BigDecimal extraAmount = expResult.getLoanThreshold().subtract(loanAmount);
      params.put("disburseAccountDisplay", buildDisburseAccountDisplay(hashPaymentCredentialId));
      params.put("extraLoanAmountFormatted", formatExtraLoanAmount(extraAmount));
      params.put("disburseAmountFormatted", formatDisburseAmount(extraAmount));
      FirstRepayPair firstRepayPair = calcFirstRepayPair(viewerContext, productId, loanAmount,
          expResult.getLoanThreshold(), couponId);
      params.put("dailyPaymentDeltaFormatted",
          formatDailyPaymentDelta(firstRepayPair.before, firstRepayPair.after));
    }

    // 激励金额格式化(仅首贷用)
    params.put("motivateLoanThresholdDesc", IdnAmountFormatter.formatAmountWithRbAndJuta(expResult.getLoanThreshold()));
    // 总优惠金额（仅首贷用）
    params.put("feeDeductAmount", feeDeductAmount);
    // 本单已享总优惠金额（仅首贷用）
    params.put("feeDeductAmountForThisOrder", feeDeductAmount.add(new BigDecimal("1000000")));
    // 上涨优惠金额(（仅首贷用）
    params.put("increaseFeeDeductAmount", feeDeductAmount.add(new BigDecimal("2000000")));
    // 是否参加激励活动（仅首贷用）
    params.put("motivateLoanActivityForLoan", !expResult.isReloan() && expResult.getInActivity());
    // 客服半弹层实验（仅首贷用）
    params.put("csDialog", TT.gen("Hi {0}，感谢你的信任！给你更大优惠！", MaskUtils.maskNicknameByLetterLength(viewerContext.name)).toString(viewerContext.locale.locale));
  }

  /**
   * 温馨提示二次确认弹窗展示字段（TAPD-363868）：直接复用协议页已有的 fetchOrderAgreementExp 计算逻辑
   * （优惠券减息、金额格式化、期限文案），不新写并行口径，保证弹窗与协议页/下单页展示数值完全一致。
   */
  private void setQuickConfirmReminderParams(HashMap<@Nullable String, @Nullable Object> params,
      LoanApiViewerContext viewerContext, BigDecimal loanAmount, String selectedProductId, Long couponId) {
    OrderAgreementVO agreementVO = orderAgreementService.fetchOrderAgreementExp(viewerContext, couponId, loanAmount, selectedProductId);
    if (agreementVO == null || agreementVO.getFormattedLoanAmount() == null) {
      // 协议数据获取失败或不完整时不展示弹窗，避免前端渲染缺字段的半成品提示
      return;
    }
    params.put(QUICK_CONFIRM_REMINDER_SHOW_KEY, "true");
    params.put(QUICK_CONFIRM_REMINDER_LOAN_AMOUNT_KEY, agreementVO.getFormattedLoanAmount());
    params.put(QUICK_CONFIRM_REMINDER_TENOR_KEY, agreementVO.getProductTimeContent());
    params.put(QUICK_CONFIRM_REMINDER_FIRST_REPAY_KEY, agreementVO.getFormattedFirstRepayAmount());
  }

  /**
   * 首贷加借推荐弹窗 extraParams（TAPD-371433）：整包 JSON + 展示开关。
   * 金额展示走 {@link AmountFormatter#format}，与协议页 Rupiah 口径一致。
   */
  private void setBorrowMorePopupParams(
      HashMap<@Nullable String, @Nullable Object> params, BorrowMorePopupParams popup, SDKType sdkType) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("couponId", popup.couponId);
    payload.put("targetAmount", popup.recommendedAmount.longValue());
    SDKType currencyType = sdkType == null ? SDKType.IDN_YQD : sdkType;
    payload.put("deltaAmount", AmountFormatter.format(currencyType.getCurrency(), popup.deltaAmount));
    payload.put("originalAmount", AmountFormatter.format(currencyType.getCurrency(), popup.originalAmount));
    payload.put("recommendedAmount", AmountFormatter.format(currencyType.getCurrency(), popup.recommendedAmount));
    payload.put("firstRepayOriginal", AmountFormatter.format(currencyType.getCurrency(), popup.firstRepayOriginal));
    payload.put("firstRepayRecommended", AmountFormatter.format(currencyType.getCurrency(), popup.firstRepayRecommended));
    params.put(BORROW_MORE_POPUP_DATA_KEY, JsonUtils.toString(payload));
    params.put(BORROW_MORE_SHOW_KEY, "true");
    log.debug("[BorrowMorePopup] write extraParam, couponId={}, targetAmount={}, payload={}",
        popup.couponId, popup.recommendedAmount, params.get(BORROW_MORE_POPUP_DATA_KEY));
  }

  private Long decodeSelectedProductId(String selectedProductId) {
    try {
      Long productId = YqgHashids.decode(selectedProductId);
      log.debug("[BorrowMorePopup] decode selectedProductId ok, hashId={}, productId={}",
          selectedProductId, productId);
      return productId;
    } catch (Exception e) {
      log.warn("[BorrowMorePopup] decode selectedProductId 失败, hashId={}", selectedProductId, e);
      return null;
    }
  }

  private void setDuplicateLoanInterceptParams(HashMap<@Nullable String, @Nullable Object> params,
      RecentSimilarPayoutInfo duplicateLoanPayoutInfo) {
    params.put(DUPLICATE_LOAN_INTERCEPT_SHOW_KEY, "true");
    params.put(DUPLICATE_LOAN_LAST_PAYOUT_AMOUNT_KEY,
        duplicateLoanPayoutInfo.getFormattedPayoutAmount());
    params.put(DUPLICATE_LOAN_LAST_PAYOUT_TIME_KEY,
        duplicateLoanPayoutInfo.getFormattedPayoutTime());
  }

  /**
   * 判断当前 styleGroup 是否命中复贷新样式实验组 1 / 2；对照组 / 空白组 / null 返回 false。
   */
  private boolean isStyleExperimentHit(String styleGroup) {
    return UserFlowConstants.EXPERIMENT_GROUP_ONE.equals(styleGroup)
        || UserFlowConstants.EXPERIMENT_GROUP_TWO.equals(styleGroup);
  }

  /**
   * 按 PRD §4.2.1 / spec FR-006 拼接脱敏放款账号字符串，形如 {@code B**0004}。
   *
   * <p><b>数据安全（NFR-003）</b>：本方法及调用链严禁向业务日志输出原始 {@code accountNumber}；脱敏后字符串
   * 只允许放入下游 MC 资源位 params Map，且不在 EC 后端业务日志中打印该 Map。
   *
   * @param credentialId 放款账号 id（来自 Controller 透传），为 {@code null} 或查无记录时返回 {@code null}
   * @return 脱敏字符串；上游字段缺失时返回 {@code null}（spec [US1-4] 兜底）
   */
  private String buildDisburseAccountDisplay(Long credentialId) {
    if (credentialId == null) {
      return null;
    }
    LoanBankAccountVO vo = loanBankAccountService.findVOById(credentialId);
    if (vo == null || vo.bankType == null) {
      return null;
    }
    return vo.bankType.name().charAt(0) + DISBURSE_ACCOUNT_MASK + MaskUtils.getLastFourNum(vo.accountNumber);
  }

  /**
   * 试算升档前 / 后首月月供（剔服务费口径），仅作后端内部派生 {@code dailyPaymentDeltaFormatted} 的中间量，
   * <b>不</b>写入 MC 资源位 params。
   *
   * <p>升档前：复用用户当前选中的 {@code couponId}；升档后：通过
   * {@code orderAgreementVOAfter} 走的 {@code fetchOrderAgreementExpWithOptimalCoupon} 同口径选券）。
   *
   * @return 含 before / after BigDecimal 的中间量对；任一计算异常时对应字段为 {@code null}（由
   *         {@link #formatDailyPaymentDelta} 走兜底字符串）
   */
  private FirstRepayPair calcFirstRepayPair(LoanApiViewerContext viewerContext, Long productId, BigDecimal loanAmount,
      BigDecimal threshold, Long couponId) {
    return new FirstRepayPair(
        calcFirstRepayWithCoupon(viewerContext, productId, loanAmount, couponId),
        calcFirstRepayWithOptimalCoupon(viewerContext, productId, threshold));
  }

  private BigDecimal calcFirstRepayWithCoupon(LoanApiViewerContext viewerContext, Long productId, BigDecimal amount,
      @Nullable Long couponId) {
    try {
      SeaProductFeeCache productFee = ecHomePageProductTool.getProductFeeCache(productId, amount);
      CutInterestVO cutInterestVO = cutInterestVOResolver.resolve(
          viewerContext.userId, viewerContext.sdkType, couponId, productFee);
      // 统一走展示态收口：excludePpn 反加逻辑由 getBillDiscounts 内部按实验组灌好展示态，
      // 入口不再自建 plan / 分支（US1 补丁清理）。
      InstalmentDiscounts billDiscounts = billDiscountsService.getBillDiscounts(
          viewerContext.userId, amount, productFee, cutInterestVO);
      return firstRepayOf(billDiscounts);
    } catch (Exception e) {
      log.error("OrderPreCheck calcFirstRepayWithCoupon failed, userId={}, productId={}, amount={}, couponId={}",
          viewerContext.userId, productId, amount, couponId, e);
      return null;
    }
  }

  private BigDecimal calcFirstRepayWithOptimalCoupon(LoanApiViewerContext viewerContext, Long productId,
      BigDecimal amount) {
    try {
      SeaProductFeeCache productFee = ecHomePageProductTool.getProductFeeCache(productId, amount);
      CutInterestVO cutInterestVO = cutInterestVOResolver.resolve(
          viewerContext.userId, viewerContext.sdkType, null, productFee);
      // 统一走展示态收口：同 calcFirstRepayWithCoupon，excludePpn 反加由 getBillDiscounts 内部处理。
      InstalmentDiscounts billDiscounts = billDiscountsService.getBillDiscounts(
          viewerContext.userId, amount, productFee, cutInterestVO);
      return firstRepayOf(billDiscounts);
    } catch (Exception e) {
      log.error("OrderPreCheck calcFirstRepayWithOptimalCoupon failed, userId={}, productId={}, amount={}",
          viewerContext.userId, productId, amount, e);
      return null;
    }
  }

  private BigDecimal firstRepayOf(InstalmentDiscounts billDiscounts) {
    if (billDiscounts == null || CollectionUtils.isEmpty(billDiscounts.getInstalmentDiscountList())) {
      return null;
    }
    // 对客展示首期综合月供：excludePpn=true → 含服务费+PPN；false → 回退剔除态 owedAmount（R1 防翻倍）。
    return billDiscounts.getFirstTermDiscount().getOwedAmount4Show();
  }

  /**
   * 按 PRD §4.2.4 双格式规则返回多借金额展示字符串：
   * <ul>
   *   <li>{@code null} 或 {@code < 0} → {@code null}（异常输入，让前端走 PRD §4.2.2 字段缺失降级；
   *       负数违反 {@code tryCalculateShowAmount} 保证的 {@code loanThreshold ≥ loanAmount} 数学约束，
   *       展示 {@code "Rp 0"} 或 {@code "Tambah Rp 0 Lagi"} 会误导用户）</li>
   *   <li>{@code == 0} → {@code "Rp 0"}（spec [US1-7] 兜底；理论上 threshold ≥ loanAmount 不应到此分支）</li>
   *   <li>{@code < 1 juta} → {@code Rp 950.000}（带 {@code Rp } 前缀、千分位 {@code .}、HALF_UP 1 位小数）</li>
   *   <li>{@code ≥ 1 juta} → {@code 1,1 JUTA}（HALF_UP、整数化去尾、小数点 {@code ,}）</li>
   * </ul>
   */
  private String formatExtraLoanAmount(BigDecimal extraAmount) {
    if (extraAmount == null || extraAmount.signum() < 0) {
      return null;
    }
    if (extraAmount.signum() == 0) {
      return EXTRA_LOAN_AMOUNT_ZERO;
    }
    return IdnAmountFormatter.formatAmountByDefaultShowRuleHalfUp(extraAmount);
  }

  /**
   * 按 PRD §4.2.6 / §5.2.3 返回到手金额展示字符串，带 {@code +} 号：
   * <ul>
   *   <li>{@code null} 或 {@code < 0} → {@code null}（与 {@link #formatExtraLoanAmount} 同口径，
   *       数值同源；负数为异常上游数据，避免渲染 {@code "Jumlah Pencairan: -Rp100.000"} 误导文案）</li>
   *   <li>{@code == 0} → {@code "+Rp0"}（兜底）</li>
   *   <li>{@code > 0} → {@code +Rp1.000.000}</li>
   * </ul>
   */
  private String formatDisburseAmount(BigDecimal extraAmount) {
    if (extraAmount == null || extraAmount.signum() < 0) {
      return null;
    }
    if (extraAmount.signum() == 0) {
      return DISBURSE_AMOUNT_ZERO;
    }
    return "+" + IdnAmountFormatter.formatAmountAsRpInteger(extraAmount);
  }

  /**
   * 按 PRD §4.2.7 计算并格式化日供增量。
   *
   * <p>算法：{@code dailyDelta = (afterMonthly - beforeMonthly) ÷ 30}，
   * {@code scale = 0}，{@code RoundingMode.HALF_UP}；再按符号拼接：
   * <ul>
   *   <li>正数：{@code +RpXXX.XXX/hari}</li>
   *   <li>负数：{@code -RpXXX.XXX/hari}（spec [US1-6]）</li>
   *   <li>零或上游缺失：{@code +Rp0/hari}（spec [US1-3] / [US1-7] 兜底）</li>
   * </ul>
   */
  private String formatDailyPaymentDelta(BigDecimal beforeMonthly, BigDecimal afterMonthly) {
    if (beforeMonthly == null || afterMonthly == null) {
      return DAILY_PAYMENT_DELTA_ZERO;
    }
    BigDecimal monthlyDelta = afterMonthly.subtract(beforeMonthly);
    BigDecimal dailyInt = monthlyDelta.divide(DAYS_PER_MONTH, 0, RoundingMode.HALF_UP);
    if (dailyInt.signum() == 0) {
      return DAILY_PAYMENT_DELTA_ZERO;
    }
    String body = IdnAmountFormatter.formatAmountAsRpInteger(dailyInt.abs());
    return (dailyInt.signum() > 0 ? "+" : "-") + body + DAILY_PAYMENT_DELTA_SUFFIX;
  }

  /**
   * 包装 {@link EcHomePageProductTool#getProductFeeCache} 调用，避免因 amount 与缓存档位不匹配抛出
   * {@code EcException} 中断字段下发链路。
   *
   * <p>跳变检测属于<b>观测性增强</b>（spec FR-013 / FR-014）：上游 feeCache 缺失时按 spec [US5-2] 静默跳过，
   * <b>不</b>写 WARN 日志；同时不影响后续 4 类 KV 的下发。
   *
   * @return feeCache；上游异常或返回值缺失时返回 {@code null}
   */
  private SeaProductFeeCache safeGetProductFeeCache(Long productId, BigDecimal amount) {
    if (productId == null || amount == null) {
      return null;
    }
    try {
      return ecHomePageProductTool.getProductFeeCache(productId, amount);
    } catch (Exception ex) {
      // 静默跳过：跳变检测不应因 feeCache 取数异常而干扰主链路或污染日志（spec [US5-2]）
      return null;
    }
  }

  /**
   * 升档前 / 后产品配置跳变检测，按 spec [US5-1] / FR-013 / FR-014 仅记 WARN 日志，<b>不</b>抛异常、<b>不</b>阻塞、
   * <b>不</b>降级。
   *
   * <p>判定口径：产品 ID 不一致 <b>或</b> {@link LoanProductConfigVO#isSamePeriodAndRateConfig} 返回 {@code false}
   * （期数 / 周期 / 利率 / 平台服务费率任一不同）时视为跳变。
   *
   * <p>feeBefore / feeAfter 或其 {@code productConfigVO} 任一为 {@code null} 时按 spec [US5-2] 静默跳过、不写日志，
   * 避免在 feeCache 上游不稳定时污染告警通道。
   *
   * @param feeBefore 升档前 fee cache
   * @param feeAfter 升档后 fee cache
   * @param userId 业务标识，allowed to log（CLAUDE.md data-security 红线）
   * @param loanAmountBefore 升档前金额
   * @param loanAmountAfter 升档后金额（即 loanThreshold）
   */
  private void checkProductDriftAndWarn(SeaProductFeeCache feeBefore, SeaProductFeeCache feeAfter,
      Long userId, BigDecimal loanAmountBefore, BigDecimal loanAmountAfter) {
    if (feeBefore == null || feeAfter == null) {
      return;
    }
    LoanProductConfigVO before = feeBefore.productConfigVO;
    LoanProductConfigVO after = feeAfter.productConfigVO;
    if (before == null || after == null) {
      return;
    }
    boolean sameProductId = Objects.equals(before.id, after.id);
    boolean sameConfig = LoanProductConfigVO.isSamePeriodAndRateConfig(before, after);
    if (sameProductId && sameConfig) {
      return;
    }
    log.warn("[product-drift] userId={} productIdBefore={} productIdAfter={} termsBefore={} termsAfter={} "
            + "rateBefore={} rateAfter={} loanAmountBefore={} loanAmountAfter={}",
        userId, before.id, after.id, before.terms, after.terms,
        before.totalInterestRate, after.totalInterestRate, loanAmountBefore, loanAmountAfter);
  }

  /** 升档前 / 后首月月供数值对（剔服务费口径），仅作内部中间量，不写入 params。 */
  private static final class FirstRepayPair {
    private final BigDecimal before;
    private final BigDecimal after;

    private FirstRepayPair(BigDecimal before, BigDecimal after) {
      this.before = before;
      this.after = after;
    }
  }

  /**
   * 执行创建订单的各项检查和数据准备
   */
  public CreateOrderCheckData performCreateOrderChecks(LoanApiViewerContext viewerContext, PaymentMethod paymentMethod,
      String hashPaymentCredentialId, BigDecimal loanAmount, String selectedProductId, Long couponId) {
    oppoOrderCheck.oppoOrderCheck(viewerContext.sourceType, viewerContext.channel, viewerContext.userId, viewerContext.sdkType);
    checkPaymentCredentialForOrderCheck(paymentMethod, hashPaymentCredentialId, viewerContext);
    checkRevolvingCreditForOrderCheck(viewerContext);

    // 是否需要otp校验
    CreateOrderOtpCheckResult otpCheckResult = loanUserOrderService.getCreateOrderWithoutOtp(viewerContext.userId, viewerContext.loanAccountId,
        viewerContext.build, viewerContext.sourceType, loanAmount, viewerContext.sdkType);
    boolean createOrderWithoutOtp = otpCheckResult.isCreateOrderWithoutOtp();

    CreateOrderPopupType createOrderPopupType = getCreateOrderPopupType(createOrderWithoutOtp);
    boolean needShowContract = loanUserOrderService.needShowContract(createOrderWithoutOtp, viewerContext.userId, viewerContext.build,
        viewerContext.sourceType);

    IDNHomepageLoanStatusV5 status = homepageStatusTool.getStatus(viewerContext.loanAccountId, viewerContext.build, viewerContext.sdkType);
    OrderLimitPopupResponse orderLimitPopupInfo = loanUserOrderService.getOrderLimitPopupInfo(status, viewerContext.loanAccountId,
        viewerContext.build, viewerContext.sourceType);
    boolean needSupplementProcess = supplementCreateOrderService.checkNeedSupplementProcess(viewerContext.userId, viewerContext.build,
        viewerContext.sourceType);
    Long decodedSelectProductId = StringUtils.isNotBlank(selectedProductId) ? YqgHashids.decode(selectedProductId) : null;
    LoanProductConfigVO productVO =
        Objects.nonNull(decodedSelectProductId) ? productConfigService.getProductVO(decodedSelectProductId) : null;
    OrderAmountSplitResponse orderAmountSplitResponse = buildOrderAmountSplitResponse(viewerContext.userId, viewerContext.loanAccountId,
        loanAmount, viewerContext.sdkType, productVO, couponId);
    BigDecimal realLoanAmount = Objects.nonNull(orderAmountSplitResponse) ? orderAmountSplitResponse.effectiveAmount : loanAmount;
    Boolean apichannelOrderCheckResult = apiChannelOrderCheckService.checkApiChannelOrderCanBeContinue(viewerContext, productVO,
        ImpliedContextUtils.sourceType());
    if (!apichannelOrderCheckResult) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("很抱歉，您的借款申请未通过"));
    }

    // TAPD-356514：WA OTP 自动回填实验分流（按 userId 选首贷/复贷 key），命中下发 autoBackfillOtp；fail-safe 不阻断下单
    boolean autoBackfillOtp = waOtpAutofillExpService.isOrderAutoBackfillHit(viewerContext.userId, viewerContext.build);

    // TAPD-364294：下单生物识别验证资格判定。本就免 OTP 时直接短路为 false——没有 OTP 可替代，
    // 且不能让这部分流量进入实验分流污染实验结论（US1-6）；服务内部 fail-safe，不阻断下单。
    boolean needBiometricVerify = createOrderWithoutOtp ? false
        : orderBiometricVerifyService.isBiometricVerifyEnabledForOrder(viewerContext.userId, ImpliedContextUtils.deviceToken(),
            viewerContext.build);
    // 仅在本次确实启用生物识别时才下发文案：needBiometricVerify == false 时 App 不会调起系统弹窗，
    // 下发文案纯属冗余，且能省掉 4 次 zk 读取（TC-072）
    BioAuthProcessTextVO bioAuthProcessText = needBiometricVerify
        ? BioAuthProcessTextVO.from(userSecureConfig.getBioAuthTitle(), userSecureConfig.getBioAuthSubtitle(),
            userSecureConfig.getBioAuthDesc(), userSecureConfig.getBioAuthNegativeText())
        : null;

    return CreateOrderCheckData.from(createOrderWithoutOtp, createOrderPopupType, otpCheckResult.isAllowSkipOtp(), needShowContract, status,
        orderLimitPopupInfo, needSupplementProcess, decodedSelectProductId, productVO, orderAmountSplitResponse, realLoanAmount,
        autoBackfillOtp, needBiometricVerify, bioAuthProcessText);
  }

  private CreateOrderPopupType getCreateOrderPopupType(Boolean createOrderWithoutOtp) {
    // 订单需要OTP, 展示OTP弹窗
    if (BooleanUtils.isNotTrue(createOrderWithoutOtp)) {
      return CreateOrderPopupType.OTP_POPUP;
    }

    return CreateOrderPopupType.NO_POPUP;
  }

  private void checkPaymentCredentialForOrderCheck(PaymentMethod paymentMethod, String hashPaymentCredentialId,
      LoanApiViewerContext viewerContext) {
    if (SDKType.IDN_YQD != viewerContext.sdkType || paymentMethod == null || StringUtils.isBlank(hashPaymentCredentialId)) {
      return;
    }
    loanBankAccountService.checkPaymentCredential(paymentMethod, hashPaymentCredentialId, viewerContext.sdkType,
        viewerContext.loanAccountId, viewerContext.sourceType);
  }

  private void checkRevolvingCreditForOrderCheck(LoanApiViewerContext viewerContext) {
    //判断是否是循环额度用户
    if (!loanAccountRevolvingService.checkUserInRevolvingLoanProcess(viewerContext.loanAccountId)) {
      return;
    }
    RemainCreditsVO remainCreditsVO = loanCreditsQuotaService.getHomepageRemainCredits(viewerContext.loanAccountId);
    BigDecimal revolvingLoanMinRemainCredits = homepageV5Config.getRevolvingLoanMinRemainCredits();
    if (remainCreditsVO.remainingCredits.compareTo(revolvingLoanMinRemainCredits) < 0) {
      // 循环额度用户应该在调用接口前，前端在首页拦截，保险起见，在这里也拦一下
      log.error("revolving user can not create order! credits too low. accountId:{}", viewerContext.loanAccountId);
      throw EcException.warn(EcExceptionType.REVOLVING_LOAN_REJECT_AND_HAS_REVOLVING_TAG, TT.gen("系统正在维护，暂时无法申请借款，请稍后再试。"));
    }
    boolean canCreateOrder = loanAccountRevolvingService.checkUserInRevolvingLoanProcessAndNoOverdueAndNoControl(
        viewerContext.loanAccountId);
    if (!canCreateOrder) {
      // 循环额度用户应该在调用接口前，前端在首页拦截，保险起见，在这里也拦一下
      log.error("revolving user can not create order! accountId:{}", viewerContext.loanAccountId);
      throw EcException.warn(EcExceptionType.REVOLVING_LOAN_REJECT_AND_HAS_REVOLVING_TAG, TT.gen("系统正在维护，暂时无法申请借款，请稍后再试。"));
    }
  }

  private OrderAmountSplitResponse buildOrderAmountSplitResponse(Long userId, Long loanAccountId, BigDecimal loanAmount, SDKType sdkType,
      LoanProductConfigVO productVO, Long couponId) {
    //h5全流程用户调这个接口不传金额
    if (Objects.isNull(loanAmount) || Objects.isNull(productVO)) {
      log.warn("loanAmount is null when createOrderCheck, userId: {}", loanAccountId);
      return null;
    }
    LoanUserCreditsInfoVO loanUserCreditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(loanAccountId);
    if (loanUserCreditsInfoVO.totalRemainCredits.compareTo(loanAmount) >= 0) {
      return null;
    }
    //如果用户不存在虚拟额度不拆单
    if (loanUserCreditsInfoVO.virtualRiskFixedCredits.compareTo(loanUserCreditsInfoVO.creditsQuota) == 0) {
      return null;
    }
    ExpUser expUser = ExpUser.builder().userId(userId).versionBuild(99999L).build();
    CommonABTestResultGroup parentExperimentResult = CommonABTestResultGroup.valueOf(
        expLastResultRunningClient.getString("product_operation_order-not_withdraw-abroad-loan_all-KeepCreditLimit_v1_1", expUser, "A"));

    BigDecimal effectiveAmount = OrderAmountSplitResponse.processAmount(loanUserCreditsInfoVO.totalRemainCredits, cashLoanConfig.getSplitLoanAmountPercent(),
        productVO.getMinCredits().getAmountInYuan(), cashLoanConfig.getStepAmount(sdkType), parentExperimentResult);

    return OrderAmountSplitResponse.from(loanAmount, cashLoanConfig.getReduceCreditsSplitOrderHintTitle(), cashLoanConfig.getReduceCreditsSplitOrderHintContent(),
        getReduceCreditsCouponPromptContent(couponId, effectiveAmount), effectiveAmount);
  }

  private String getReduceCreditsCouponPromptContent(Long couponId, BigDecimal effectiveAmount) {
    if (Objects.isNull(couponId)) {
      return cashLoanConfig.getReduceCreditsCouponPromptContentForUse();
    }
    couponId = loanUserOrderService.checkAndGetCouponIdForReduceCredits(couponId, effectiveAmount);
    return Objects.isNull(couponId) ? cashLoanConfig.getReduceCreditsCouponPromptContentForIgnore() : cashLoanConfig.getReduceCreditsCouponPromptContentForUse();
  }
}
