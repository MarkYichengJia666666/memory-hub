package com.miyou.controllers.cashloan.newhomepage.elementmodel.order;

import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.FOREST_GREEN;

import com.google.common.collect.ImmutableMap;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.constant.CustomerElementType;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.ButtonElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.CustomerElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.service.cashloan.creditgain.CreditGainExpContext;
import com.yqg.core.service.cashloan.creditgain.CreditGainExpResult;
import com.yqg.core.service.cashloan.creditgain.CreditGainOriginTraceService;
import com.yqg.core.service.cashloan.creditgain.CreditGainPerceptionExpService;
import com.yqg.core.service.cashloan.creditgain.CreditGainRiskTraceDigest;
import com.yqg.core.service.cashloan.creditgain.CreditGainScene;
import com.yqg.core.service.cashloan.creditgain.CreditGainSeenService;
import com.yqg.core.service.cashloan.homepage.config.NativePathConstant;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.utilities.StandardInterestUtil;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.SeaProductFeeCache;
import com.yqg.core.service.cashloan.risk.AppListDialogSuppressService;
import com.yqg.core.service.cashloan.util.rate.ProductRateUtil;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vos.LoanCutInterestCouponVO;
import com.yqg.core.service.orderpage.LoanCouponDisplayType;
import com.yqg.core.userflow.domain.loan.model.amount.CreditGainResultCard;
import com.yqg.core.userflow.domain.loan.model.discounts.OrderDiscounts;
import com.yqg.core.userflow.domain.loan.service.amount.ICreditGainDisplayService;
import com.yqg.core.userflow.domain.user.service.IUserInfoService;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.marketing.channel.HomeMainCardQian1Service;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class LoanProductProcessor extends AbstractPageCardV3Processor {
  @Autowired
  protected EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private StandardInterestUtil standardInterestUtil;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private HomeMainCardQian1Service homeMainCardQian1Service;
  @Autowired
  private CreditGainPerceptionExpService creditGainPerceptionExpService;
  @Autowired
  private CreditGainSeenService creditGainSeenService;
  @Autowired
  private CreditGainOriginTraceService creditGainOriginTraceService;
  @Autowired
  private IUserInfoService userInfoService;
  @Autowired
  private ICreditGainDisplayService creditGainDisplayService;
  @Autowired
  private AppListDialogSuppressService appListDialogSuppressService;

  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.TITLE)
        .text(TT.gen("剩余可借金额"))
        .build());
    BigDecimal loanAmount = getLoanAmount(homePageContext);
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.AMOUNT_TEXT)
        .text(TT.gen("{0}", AmountFormatter.formatForIDNNoRP(loanAmount)))
        .build());
    boolean qian1SubTitleHit = homeMainCardQian1Service.shouldShowMarketingBar(
        homePageContext.getUserId(),
        homePageContext.getUserDeviceContextVO().getDeviceToken(),
        homePageContext.getUserDeviceContextVO().getBuild(),
        homePageContext.getUserDeviceContextVO().getSourceType(),
        homePageContext.getSdkType(), true);
    // 先获取进度条，再统一决定是否展示SUB_TITLE：进度条命中时不展示包含总额度的文案，临时额度文案不受影响
    Optional<IElement> amountProgressBar = homepageV3CardTool.buildAmountProgressBarElement(homePageContext);
    // TAPD-369589 US2：命中「实验组 + 本次获额未展示过」时下发 Baru! 角标并把副标题替换为新额度文案，
    // 与既有临额/总额度副标题互斥；非命中回退既有展示。角标与副标题共用同一频控 key、同出同消费（US2-4）。
    if (!tryRenderNewCreditGainBadgeAndSubTitle(pageCardV3VO, homePageContext)) {
      boolean isTotalAmountSubTitle = !qian1SubTitleHit && !hasTempCredits(homePageContext);
      if (!amountProgressBar.isPresent() || !isTotalAmountSubTitle) {
        TT subTitleText = qian1SubTitleHit
            ? TT.gen(HomeMainCardQian1Service.SUB_TITLE_TEXT_AUTH_PASSED)
            : getSubTitleText(homePageContext);
        TextElement.TextParam subTitleParam = qian1SubTitleHit
            ? TextElement.TextParam.builder()
                .highlightMap(ImmutableMap.of(HomeMainCardQian1Service.SUB_TITLE_BOLD_PLACEHOLDER,
                    HomeMainCardQian1Service.SUB_TITLE_BOLD_DISPLAY))
                .highlightStyle(TextElement.TextStyle.BOLD)
                .highlightColor(FOREST_GREEN)
                .build()
            : TextElement.TextParam.builder().build();
        pageCardV3VO.addElementForMainCard(ElementBuilder.text()
            .id(MainCardElementId.SUB_TITLE)
            .text(subTitleText)
            .elementParam(subTitleParam)
            .build());
      }
    }
    OrderDiscounts orderDiscounts = getOrderDiscounts(homePageContext);

    HomeDisplayStrategy couponStyle = HomeDisplayStrategy.A;
    if (orderDiscounts != null && orderDiscounts.hasDiscount() && !appListDialogSuppressService.shouldSuppress(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getDeviceToken(), homePageContext.getUserDeviceContextVO().getBuild())) {
      // 有优惠 需要分流
      couponStyle = HomeDisplayStrategy.A;
      addCouponInfo(pageCardV3VO, couponStyle, homePageContext, orderDiscounts);
    }

    doCreateButton(pageCardV3VO, couponStyle, homePageContext);

    homepageV3CardTool.tryAddIncreaseCreditsLink(pageCardV3VO, homePageContext);

    // 大卡顶部额度进度条（已在上方提前获取，此处直接挂载，同时移除TOP_TIP）
    amountProgressBar.ifPresent(bar -> {
      pageCardV3VO.addElementForMainCard(bar);
      pageCardV3VO.deleteFromElementMap(MainCardElementId.TOP_TIP);
    });
  }

  /**
   * 主卡「立即申请」入口；子类若需前置条件（如渠道是否展示下载按钮），可覆盖并在通过校验后调用 {@link #appendMainApplyButton}。
   */
  protected void doCreateButton(PageCardV3VO pageCardV3VO, HomeDisplayStrategy couponStyle, HomePageContext homePageContext) {
    appendMainApplyButton(pageCardV3VO, couponStyle, homePageContext);
  }

  /**
   * 向主卡追加「立即申请」按钮；与 {@link #doCreateButton} 拆分为避免子类复制整段 Element 组装逻辑。
   */
  protected void appendMainApplyButton(PageCardV3VO pageCardV3VO, HomeDisplayStrategy couponStyle, HomePageContext homePageContext) {
    ButtonElement.ButtonColor buttonColor = couponStyle == HomeDisplayStrategy.A ? ButtonElement.ButtonColor.GREEN : ButtonElement.ButtonColor.YELLOW;

    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.MAIN_BUTTON)
        .text(TT.gen("立即申请"))
        .action(new RedirectAction(RedirectAction.LinkActionParam.builder().redirectUrl(elementConfig.getRedirectPage(getSubHomeLoanPage(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild()))).build()))
        .elementParam(ButtonElement.ButtonParam.builder().colorType(buttonColor).build())
        .build());
  }

  @NotNull
  protected NativePathConstant getSubHomeLoanPage(Long userId, Long build) {
    return NativePathConstant.SUB_HOME_LOAN_PAGE;
  }

  protected OrderDiscounts getOrderDiscounts(HomePageContext homePageContext) {
    // 触发分流
    standardInterestUtil.executeStandardInterestAbTest(homePageContext.getUserId());
    return ecHomePageProductTool.getOrderDiscountsForOptimalCouponConsiderRemainCreditLimit(
        homePageContext.getLoanAccountVO(),
        homePageContext.getUserProductVO().getEnableVirtualCredits(),
        true);
  }

  private void addCouponInfo(PageCardV3VO pageCardV3VO, HomeDisplayStrategy strategy,
      HomePageContext homePageContext, OrderDiscounts orderDiscounts) {
    SeaProductFeeCache feeCache = ecHomePageProductTool.getProductFeeCacheForOptimalCouponConsiderRemainCreditLimit(
        homePageContext.getLoanAccountVO(),
        homePageContext.getUserProductVO().getEnableVirtualCredits(),
        true);
    boolean lowInterestProduct = feeCache != null && feeCache.productConfigVO != null
        && ProductRateUtil.isLowInterestProduct(feeCache.productConfigVO,
            standardInterestUtil.getStandardInterestRate(homePageContext.getUserId()));
    LoanCutInterestCouponVO couponVO = SeaProductFeeCache.validCheck(feeCache)
        ? loanUserCouponService.fetchMaxCutInterestCoupon(
            homePageContext.getUserId(), feeCache.loanAmount, feeCache.oiPlan, feeCache.productConfigVO.sdkType)
        : null;
    LoanCouponDisplayType displayType = lowInterestProduct ? LoanCouponDisplayType.LOW_INTEREST
        : (couponVO == null ? LoanCouponDisplayType.NOT_DISCOUNT
            : LoanCouponDisplayType.fromConfigType(couponVO.configVO.type));
    pageCardV3VO.addElementForMainCard(ElementBuilder.customerElement(CustomerElementType.COUPON_CARD)
        .id(MainCardElementId.CUT_COUPON_CARD)
        .elementParam(CustomerElement.CouponCardParam.from(strategy, orderDiscounts, displayType, couponVO))
        .build());
  }

  private TT getSubTitleText(HomePageContext homePageContext) {
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    if (BigDecimalHelper.greaterThan(creditsInfoVO.tempCredits, BigDecimal.ZERO)) {
      return TT.gen("临时额度 {0}", AmountFormatter.format(EcCurrency.IDR, creditsInfoVO.tempCredits));
    }
    return TT.gen("总额度 {0}", AmountFormatter.format(EcCurrency.IDR, creditsInfoVO.getTotalFixedCreditsForVirtual()));
  }

  private boolean hasTempCredits(HomePageContext homePageContext) {
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    return BigDecimalHelper.greaterThan(creditsInfoVO.tempCredits, BigDecimal.ZERO);
  }

  /**
   * TAPD-369589 US2：结果态「实验组 + 本次获额未展示过」时，在额度数字旁下发 {@code Baru!} 角标并把副标题
   * 替换为新额度文案；角标与副标题基于同一频控 key（{@code userId + traceId}）判定，同出同消费（US2-4）。
   *
   * <p>{@link MultiLoanProductProcessor}（续借结果态）继承 {@link #process} 时复用此同一入口，
   * 判组/频控口径完全一致。
   *
   * @return true 表示已命中并下发角标 + 新额度副标题（本次获额标记被消费）；false 表示未命中，调用方回退既有副标题
   */
  private boolean tryRenderNewCreditGainBadgeAndSubTitle(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    // 先过本地廉价业务门槛（本次获额事件 traceId），再把实验入组 RPC 作为最后一道短路——
    // 保证只有真正会命中新样式的用户才发起分流，避免在不下发新样式的场景把用户灌进实验样本（评审 2026-08-11）。
    // 每道门槛各留一条 debug：角标不下发时若全程静默，线上只能靠比对响应体反推是哪一步挡的。
    LoanUserRiskTraceVO originTrace = creditGainOriginTraceService.resolveOriginTrace(
        homePageContext.getUserCreditsContext().getLatestUserRiskTraceVO());
    String eventKey = resolveCreditGainEventKey(originTrace);
    if (eventKey == null) {
      log.debug("[CreditGainResult] skip: no risk trace, userId={}, originRiskTrace={}",
          homePageContext.getUserId(), CreditGainRiskTraceDigest.of(originTrace));
      return false;
    }
    if (!hitCreditGainExperiment(homePageContext, originTrace)) {
      return false;
    }
    if (homePageLeaveJudgeTool.willLeaveHomePage(homePageContext)) {
      log.debug("[CreditGainResult] skip: will leave home page, userId={}, eventKey={}",
          homePageContext.getUserId(), eventKey);
      return false;
    }
    if (!creditGainSeenService.tryConsumeNewCreditBadge(homePageContext.getUserId(), eventKey)) {
      log.debug("[CreditGainResult] skip: seen quota already consumed, userId={}, eventKey={}",
          homePageContext.getUserId(), eventKey);
      return false;
    }
    CreditGainResultCard resultCard = creditGainDisplayService.getResultCard(originTrace);
    log.debug("[CreditGainResult] render badge and new sub title, userId={}, eventKey={}, badgeBg={}, badgeIcon={}",
        homePageContext.getUserId(), eventKey,
        resultCard.getBadgeBackgroundImageUrl(), resultCard.getBadgeIconUrl());
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.NEW_CREDIT_BADGE)
        .text(resultCard.getBadgeText())
        .elementParam(buildBadgeParam(resultCard))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.SUB_TITLE)
        .text(resultCard.getSubTitle())
        .elementParam(TextElement.TextParam.builder().build())
        .build());
    return true;
  }

  /** 角标素材参数：两个 url 均未配置时返回 null，不下发空的 {@code elementParam} */
  private TextElement.BadgeParam buildBadgeParam(CreditGainResultCard resultCard) {
    if (StringUtils.isBlank(resultCard.getBadgeBackgroundImageUrl())
        && StringUtils.isBlank(resultCard.getBadgeIconUrl())) {
      return null;
    }
    return TextElement.BadgeParam.builder()
        .backgroundImageUrl(resultCard.getBadgeBackgroundImageUrl())
        .iconUrl(resultCard.getBadgeIconUrl())
        .build();
  }

  /**
   * 是否命中「增强额度获得感」结果态实验组；任何异常 fail-open 返回 false（NFR-002 不阻断既有链路）。
   */
  private boolean hitCreditGainExperiment(HomePageContext homePageContext, LoanUserRiskTraceVO originTrace) {
    try {
      CreditGainExpContext expContext = buildExpContext(homePageContext, originTrace);
      CreditGainExpResult result = creditGainPerceptionExpService.resolve(expContext);
      log.debug("[CreditGainResult] resolve experiment, userId={}, scene={}, reloan={}, hitExpKey={}, "
              + "experimentGroup={}, originRiskTrace={}",
          homePageContext.getUserId(), expContext.getScene(), expContext.isReloan(),
          result.getHitExpKey(), result.isExperimentGroup(), CreditGainRiskTraceDigest.of(originTrace));
      return result.isExperimentGroup();
    } catch (Exception e) {
      log.warn("[CreditGainResult] resolve experiment failed, fail-open to online style, userId={}",
          homePageContext.getUserId(), e);
      return false;
    }
  }

  /**
   * 本次获额事件维度的频控 key：取<b>源头</b> trace 的主键 {@code id}。
   *
   * <p>不用 {@code traceId}：源头 trace 在风控出结果前尚未分配 riskFlowTrace（实测审核中态 {@code traceId=null}），
   * 而被拒回捞时最新那条有 traceId、源头那条没有——用 traceId 当 key 会让同一次获额事件在回捞前后取到不同值，
   * 角标频控形同虚设。主键 id 全程稳定非空。
   */
  private String resolveCreditGainEventKey(LoanUserRiskTraceVO originTrace) {
    if (originTrace == null || originTrace.id == null) {
      return null;
    }
    return String.valueOf(originTrace.id);
  }

  /**
   * 组装结果态判组入参：场景由<b>源头</b> trace 推导（{@link CreditGainScene#resolveByTrace}），
   * <b>不得</b>写死。结果态首页状态 {@code ACCEPTED} 分不出本次额度由完件首授（场景①）、还款重获（场景②）
   * 还是失效戳额（场景③）得来，写死成「首次授信」会让失效戳额的首贷用户恒判到实验一、拿不到 Baru! 角标。
   *
   * <p>人群（是否复贷）改用领域服务 {@link IUserInfoService#isReloanUserByAccountId}（打款成功次数&gt;0 为复贷），
   * 替换已 Deprecated 的 {@code HomepageUserParamsVO.firstLoan}；场景与人群解耦，入组由服务端「场景 + 人群」双条件裁决。
   */
  private CreditGainExpContext buildExpContext(HomePageContext homePageContext, LoanUserRiskTraceVO originTrace) {
    UserDeviceContextVO deviceContext = homePageContext.getUserDeviceContextVO();
    return CreditGainExpContext.builder()
        .userId(homePageContext.getUserId())
        .deviceToken(deviceContext == null ? null : deviceContext.getDeviceToken())
        .sourceType(deviceContext == null ? null : deviceContext.getSourceType())
        .build(deviceContext == null ? null : deviceContext.getBuild())
        .reloan(userInfoService.isReloanUserByAccountId(homePageContext.getLoanAccountId()))
        .scene(CreditGainScene.resolveByTrace(originTrace))
        .build();
  }

  private BigDecimal getLoanAmount(HomePageContext homePageContext) {
    return homePageContext.getUserProductVO().getEnableVirtualCredits();
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.LOAN_PRODUCT_INFO;
  }


  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class TextParam {
    public TT value;
    public String amount;
  }
}
