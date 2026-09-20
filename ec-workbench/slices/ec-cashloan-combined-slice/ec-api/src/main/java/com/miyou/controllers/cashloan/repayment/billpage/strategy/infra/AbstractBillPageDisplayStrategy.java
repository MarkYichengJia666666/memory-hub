package com.miyou.controllers.cashloan.repayment.billpage.strategy.infra;

import static com.miyou.controllers.cashloan.enums.InstalmentDisplayStatus.UNPAID;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.BEIGE;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.BLACK_000;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.BROWN;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.FOREST_GREEN;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.GREEN;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.PALE_RED;
import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.SCARLET;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.miyou.controllers.cashloan.enums.InstalmentDisplayStatus;
import com.miyou.controllers.cashloan.repayment.billpage.strategy.BillPageContext;
import com.miyou.controllers.cashloan.repayment.billpage.strategy.BillPageDisplayableInstalment;
import com.miyou.controllers.cashloan.repayment.billpage.strategy.BillPageParam;
import com.miyou.controllers.cashloan.response.RepaymentAccountResponse;
import com.miyou.controllers.cashloan.service.RepaymentAccountResponseBuilder;
import com.miyou.controllers.cashloan.response.home.SeaHomeGeneralResponse;
import com.miyou.controllers.cashloan.response.instalment.InstalmentIndexContentResponse;
import com.miyou.controllers.cashloan.response.instalment.InstalmentListResponse;
import com.miyou.controllers.cashloan.response.instalment.InstalmentResponse;
import com.miyou.controllers.cashloan.response.instalment.LoanOrderAmountResponse;
import com.miyou.controllers.cashloan.response.instalment.TimeToBillingResponse;
import com.miyou.controllers.cashloan.response.instalment.UnionRepaymentInfoResponse;
import com.miyou.controllers.cashloan.response.instalment.details.CollectionReductionGuideResponse;
import com.miyou.controllers.cashloan.response.instalment.details.FastPaymentButtonResponse;
import com.miyou.controllers.cashloan.response.instalment.details.InstalmentFeeAmountDetailsResponse;
import com.miyou.controllers.cashloan.response.instalment.details.ReminderGuideResponse;
import com.miyou.controllers.cashloan.response.instalment.details.RepayButtonResponse;
import com.miyou.controllers.cashloan.response.pointgrowth.bill.PointDeductionInfoResponse;
import com.miyou.service.pointgrowth.PointBillPreviewService;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.controllers.cashloan.utilities.IDNHomepageInstalmentExpandUtil;
import com.miyou.controllers.cashloan.utilities.RepaymentChannelTool;
import com.miyou.controllers.directdebit.response.DirectDebitAccountResponse;
import com.miyou.controllers.directdebit.response.DirectDebitBankResponse;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitAccountStatus;
import com.yqg.core.model.sql.payment.enums.PaymentTransType;
import com.yqg.core.service.abtest.enums.GoldCardVersion;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.JbpConfig;
import com.yqg.core.service.cashloan.RepaymentConfig;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.CashLoanRepayStrategyService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanInstalmentService;
import com.yqg.core.service.cashloan.repay.UnionRepaymentService;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.cashloan.vo.orderview.CashLoanInstalmentViewVO;
import com.yqg.core.service.directdebit.DirectDebitAccountService;
import com.yqg.core.service.directdebit.enums.DirectDebitAuthorizationBusinessType;
import com.yqg.core.service.directdebit.enums.DirectDebitProvider;
import com.yqg.core.service.directdebit.vo.DirectDebitBankInfo;
import com.yqg.core.service.directdebit.vo.DirectDebitLinkAccountVO;
import com.yqg.core.service.homepage.display.dto.HomePageButtonInfo;
import com.yqg.core.service.jbp.goldencard.JbpCardService;
import com.yqg.core.service.jbp.goldencard.vo.JbpOrderVO;
import com.yqg.core.service.jbp.goldencard.vo.JbpProductVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionDetail;
import com.yqg.core.service.loan.repayment.account.PaymentProviderSelectorService;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountService;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountDisplayConfig;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import com.yqg.core.service.loan.repayment.billpage.BillPageInstalmentItem;
import com.yqg.core.service.loan.repayment.billpage.BillPageInstalmentItemCalculator;
import com.yqg.core.service.loan.repayment.education.RepayEducationSupport;
import com.yqg.core.service.loan.repayment.experiment.RepaymentExperimentSupport;
import com.yqg.core.service.loan.repayment.status.RepaymentDisplayStatus;
import com.yqg.core.service.loan.repayment.status.RepaymentStatusSupport;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.PaymentBusinessNameMapper;
import com.yqg.core.service.loan.repayment.account.RepaymentChannelMonitorService;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.pmenum.RepaymentChannelGroup;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.core.service.sourcetype.SourceTypeService;
import com.yqg.core.userflow.infrastructure.adapter.compare.RepaymentChannelRouteCompareService;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.jbp.common.enums.OrderSource;
import com.yqg.jbp.common.enums.OrderStatus;
import com.yqg.jbp.dto.order.GetOrderRequest;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractBillPageDisplayStrategy {

  @Autowired
  protected HomepageV5Config homepageV5Config;
  @Autowired
  protected JbpConfig jbpConfig;
  @Autowired
  protected RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  protected RepaymentConfig repaymentConfig;
  @Autowired
  protected JbpCardService jbpCardService;
  @Autowired
  protected UnionRepaymentService unionRepaymentService;
  @Autowired
  protected SourceTypeService sourceTypeService;
  @Autowired
  protected LoanAccountService loanAccountService;
  @Autowired
  protected HomepageStatusTool homepageStatusTool;
  @Autowired
  protected HomepageContentTool homepageContentTool;
  @Autowired
  protected CashLoanService cashLoanService;
  @Autowired
  protected CashLoanRepayStrategyService cashLoanRepaymentStrategyService;
  @Autowired
  protected CashLoanInstalmentService cashLoanInstalmentService;
  @Autowired
  private RepaymentExperimentSupport repaymentExperimentSupport;
  @Autowired
  private RepayEducationSupport repayEducationSupport;
  @Autowired
  private RepaymentStatusSupport repaymentStatusSupport;
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService cutInterestCouponDeductDetailService;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private RepaymentChannelTool repaymentChannelTool;
  @Autowired
  private PaymentProviderSelectorService paymentProviderSelectorService;
  @Autowired
  private RepaymentChannelRouteCompareService repaymentChannelRouteCompareService;
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private DirectDebitAccountService directDebitAccountService;
  @Autowired
  private RepaymentAccountService repaymentAccountService;
  @Autowired
  private RepaymentAccountResponseBuilder repaymentAccountResponseBuilder;
  @Autowired
  private RepaymentChannelMonitorService repaymentChannelMonitorService;
  @Autowired
  private PointBillPreviewService pointBillPreviewService;

  /**
   * 账单页展示策略枚举
   */
  public abstract BillPageDisplayStrategyKey getKey();

  /**
   * 创建账单页响应体
   */
  public InstalmentListResponse createInstalmentListResponse(BillPageParam param) {
    // 1. 初始化账单页上下文
    BillPageContext billPageContext = initBillPageContext(param);

    // 2. 计算要展示的账单列表
    BillPageDisplayableInstalment billPageInstalment = calculateSortedBillPageInstalmentList(billPageContext);

    // 3. 根据上下文和账单列表，构建响应体
    InstalmentListResponse instalmentListResponse = buildBillPageResponse(billPageContext, billPageInstalment);

    // 4、账单页强化信息构建
    buildBillPageEnhancementResponse(billPageContext, instalmentListResponse);

    return instalmentListResponse;
  }

  private BillPageContext initBillPageContext(BillPageParam param) {
    List<CashLoanInstalmentViewVO> allInstalmentList =  cutInterestCouponDeductDetailService.getViewInstalments(cashLoanInstalmentService.fetchByUserIdAndStatusList(param.userId, CashLoanInstalmentStatus.PAYOUT_STATUSES));
    return BillPageContext.builder()
        .billPageParam(param)
        .unpaidInstalmentList(allInstalmentList.stream().filter(c -> c.status == CashLoanInstalmentStatus.INIT).collect(Collectors.toList()))
        .paidInstalmentList(allInstalmentList.stream().filter(c -> c.status == CashLoanInstalmentStatus.COMPLETE).collect(Collectors.toList()))
        .homepageLoanStatus(homepageStatusTool.getStatus(param.loanAccountId, param.build, param.sdkType))
        .deductAmountMap(initCutInterestCouponDeductAmountMap(allInstalmentList))
        .complianceStrategy(homepageContentTool.getExistFeeDetailDisplayComplianceOptimizationStrategy(param.userId, param.loanAccountId, param.build, param.sdkType))
        .reloanRejectedBubbleStrategy(() -> homepageContentTool.getReloanRejectedBubbleStrategy(param.userId, param.build))
        .repaymentReminderStrategy(repaymentExperimentSupport.getRepaymentReminderStrategy(param.userId, param.build))
        .repaymentDisplayStatus(repaymentStatusSupport.calculateRepaymentStatus(param.userId))
        .build();
  }

  private Map<Long /* instalmentId */, BigDecimal /* deductAmount */> initCutInterestCouponDeductAmountMap(List<CashLoanInstalmentViewVO> allInstalmentList) {
    return allInstalmentList.stream()
        .map(CashLoanInstalmentVO::getOrderId).distinct()
        .map(cashLoanService::getCutInterestMapByCoupon)
        .reduce(Maps.newHashMap(), (m1, m2) -> {
          m1.putAll(m2);
          return m1;
        });
  }

  protected BillPageDisplayableInstalment calculateSortedBillPageInstalmentList(BillPageContext ctx) {
    return BillPageDisplayableInstalment.builder()
        .unpaidInstalmentList(ctx.unpaidInstalmentList.stream().sorted(RepaymentStatusSupport.UNPAID_INSTALMENT_COMPARATOR).collect(Collectors.toList()))
        .paidInstalmentList(ctx.paidInstalmentList.stream().sorted(RepaymentStatusSupport.PAID_INSTALMENT_COMPARATOR).collect(Collectors.toList()))
        .build();
  }

  private InstalmentListResponse buildBillPageResponse(BillPageContext ctx, BillPageDisplayableInstalment billPageDisplayableInstalment) {
    List<CashLoanInstalmentVO> instalmentList;
    InstalmentDisplayStatus instalmentDisplayStatus = ctx.billPageParam.instalmentDisplayStatus;
    if (UNPAID == instalmentDisplayStatus) {
      instalmentList = billPageDisplayableInstalment.unpaidInstalmentList;
    } else {
      instalmentList = billPageDisplayableInstalment.paidInstalmentList;
    }
    InstalmentListResponse response = new InstalmentListResponse();
    response.totalUnpaidAmount = calculateTotalUnpaidAmount(ctx, billPageDisplayableInstalment.unpaidInstalmentList);
    response.instalmentList = buildInstalmentResponseList(ctx, instalmentList);
    fillRepayTaskIcons(ctx, response.instalmentList);
    if (instalmentDisplayStatus == UNPAID
        && CollectionUtils.isNotEmpty(instalmentList)
        && !ctx.homepageLoanStatus.canCreateOrder()
        && ctx.reloanRejectedBubbleStrategy.get().isStrategyB()
    ) {
      response.setInstalmentPageGuideBubbleInfo(InstalmentListResponse.convertBubbleVoToInstalmentGuideResp(homepageV5Config.getReloanRejectedBubbleVO()));
    }

    response.collectionReductionGuide = buildCollectionReductionGuide(ctx);

    UnionRepaymentInfoResponse unionRepaymentInfoResponse = buildUnionRepaymentInfoResponse(ctx.billPageParam, CollectionUtils.isNotEmpty(response.instalmentList));
    if (Objects.nonNull(unionRepaymentInfoResponse)) {
      response.unionRepaymentInfoResponseList.add(unionRepaymentInfoResponse);
    }
    response.jbpOrderList = buildJbpOrderList(ctx.billPageParam, instalmentDisplayStatus, ctx.homepageLoanStatus);
    response.fraudAlert = ctx.billPageParam.fraudAlert;
    response.setTotal(response.instalmentList.size() + (CollectionUtils.isEmpty(response.jbpOrderList) ? 0 : response.jbpOrderList.size()));
    response.repayButtonInfo = RepayButtonResponse.from(GREEN);
    response.fastPaymentButtonInfo = buildFastPaymentButton(ctx);
    return response;
  }

  /**
   * 计算总待还金额（展示在账单页顶部的待还金额）
   */
  private BigDecimal calculateTotalUnpaidAmount(BillPageContext ctx, List<CashLoanInstalmentVO> unpaidInstalmentList) {
    return unpaidInstalmentList.stream()
        .map(i -> BillPageInstalmentItemCalculator.getOwedAmount(i, ctx.getCollectionReductionDetailByInstalmentId(i.id).orElse(null)))
        .map(i -> i.currentAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private List<InstalmentResponse> buildInstalmentResponseList(BillPageContext billPageContext, List<CashLoanInstalmentVO> instalmentList) {
    return instalmentList.stream()
        .map(billPageInstalment -> buildInstalmentResponse(billPageContext, billPageInstalment))
        .collect(Collectors.toList());
  }

  /**
   * 为账单列表补充按时还款教育 icon 展示位；未命中时显式回填 false，保持旧接口兼容。
   *
   * @param billPageContext 账单页上下文
   * @param instalmentResponses 账单响应列表
   */
  private void fillRepayTaskIcons(BillPageContext billPageContext, List<InstalmentResponse> instalmentResponses) {
    if (CollectionUtils.isEmpty(instalmentResponses)) {
      return;
    }

    if (billPageContext.billPageParam.instalmentDisplayStatus != UNPAID) {
      return;
    }

    RepayEducationSupport.RepayEducationContext repayEducationContext = repayEducationSupport.resolveRepayEducationContext(
        billPageContext.billPageParam.userId,
        billPageContext.billPageParam.build);
    if (!repayEducationContext.isShouldShowMainFlow()) {
      return;
    }

    int iconCount = Math.min(repayEducationContext.getRemainingCompletedTimes(), instalmentResponses.size());
    for (int index = 0; index < iconCount; index++) {
      instalmentResponses.get(index).setRepayTaskIcon(true);
    }
  }

  protected InstalmentResponse buildInstalmentResponse(BillPageContext billPageContext, CashLoanInstalmentVO instalment) {
    SDKType sdkType = billPageContext.billPageParam.sdkType;
    BigDecimal cutInterestCouponDeductAmount = billPageContext.deductAmountMap.getOrDefault(instalment.id, BigDecimal.ZERO);
    HomeDisplayStrategy complianceStrategy = billPageContext.complianceStrategy;
    Optional<ManualReductionDetail.InstalmentReductionDetail> reductionDetail = billPageContext.getCollectionReductionDetailByInstalmentId(instalment.id);
    BillPageInstalmentItem repaymentAmount = BillPageInstalmentItemCalculator.getRepaymentAmount(instalment, cutInterestCouponDeductAmount, reductionDetail.orElse(null));
    long remainMillisToRepaymentDueDate = BillPageInstalmentItemCalculator.getRemainMillisToRepaymentDueDate(instalment);

    InstalmentResponse response = new InstalmentResponse();
    response.orderId = instalment.orderId;
    response.instalmentId = instalment.id;
    response.amount = repaymentAmount.currentAmount;
    response.billingDate = instalment.billingDate;
    response.billingDateDesc = TT.gen("账单日{0}", BillPageInstalmentItemCalculator.getFormattedRepaymentDueDate(instalment, sdkType));
    response.millisToBillingDate = remainMillisToRepaymentDueDate;
    response.timeToBillingDesc = IDNHomepageInstalmentExpandUtil.getTimeToBillingDateDesc(remainMillisToRepaymentDueDate);
    response.feeAmountList = IDNHomepageInstalmentExpandUtil.generateIdnFeeAmountListV2(instalment, sdkType, true, cutInterestCouponDeductAmount, complianceStrategy);
    response.feeAmountDetails = buildInstalmentFeeAmountDetailsResponse(billPageContext, instalment);
    response.timeCompleted = BillPageInstalmentItemCalculator.getTimeCompleted(instalment);
    response.timeCreated = instalment.timeCreated;
    response.index = instalment.index;

    postBuildInstalmentResponse(response, billPageContext, instalment);
    return response;
  }

  protected void postBuildInstalmentResponse(InstalmentResponse instalmentResponse, BillPageContext billPageContext, CashLoanInstalmentVO instalment) {
  }

  private InstalmentFeeAmountDetailsResponse buildInstalmentFeeAmountDetailsResponse(BillPageContext ctx, CashLoanInstalmentVO instalment) {
    ManualReductionDetail.InstalmentReductionDetail reductionDetail = ctx.getCollectionReductionDetailByInstalmentId(instalment.id).orElse(null);
    BigDecimal cutInterestCouponDeductAmount = ctx.deductAmountMap.getOrDefault(instalment.id, BigDecimal.ZERO);
    BillPageInstalmentItem totalAmount = BillPageInstalmentItemCalculator.getTotalAmount(instalment, cutInterestCouponDeductAmount, reductionDetail);
    BillPageInstalmentItem repaidAmount = BillPageInstalmentItemCalculator.getRepaidAmount(instalment, cutInterestCouponDeductAmount);
    BillPageInstalmentItem owedAmount = BillPageInstalmentItemCalculator.getOwedAmount(instalment, reductionDetail);
    return InstalmentFeeAmountDetailsResponse.builder()
        .totalAmount(SeaHomeGeneralResponse.fromBoldNormalRight(TT.gen("还款总金额"), TT.gen("{0}", totalAmount.getFormattedCurrentAmount())))
        .totalAmountList(buildTotalAmountItemList(ctx, instalment))
        .button(HomePageButtonInfo.builder().show(true).title(TT.gen("查看费用计算说明")).build())
        .paidAmount(SeaHomeGeneralResponse.fromBoldNormalRight(TT.gen("已还金额1"), TT.gen("{0}", repaidAmount.getFormattedCurrentAmount())))
        .unpaidAmount(SeaHomeGeneralResponse.fromBoldNormalRight(TT.gen("剩余待还金额"), TT.gen("{0}", owedAmount.getFormattedCurrentAmount())))
        .build();
  }

  private List<SeaHomeGeneralResponse> buildTotalAmountItemList(BillPageContext ctx, CashLoanInstalmentVO instalment) {
    ManualReductionDetail.InstalmentReductionDetail collectionReductionDetail = ctx.getCollectionReductionDetailByInstalmentId(instalment.id).orElse(null);
    BigDecimal cutInterestCouponDeductAmount = ctx.deductAmountMap.getOrDefault(instalment.id, BigDecimal.ZERO);
    List<SeaHomeGeneralResponse> totalAmountList = Lists.newArrayListWithCapacity(4);
    if (instalment.principal.compareTo(BigDecimal.ZERO) > 0) {
      BillPageInstalmentItem principal = BillPageInstalmentItemCalculator.getPrincipal(instalment, ctx.complianceStrategy, collectionReductionDetail);
      if (principal.collectionReductionFlag) {
        totalAmountList.add(SeaHomeGeneralResponse.fromSmallBlackPadding5(TT.gen("本金"), TT.gen("{0}", principal.getFormattedCurrentAmount()), TT.gen("{0}", principal.getFormattedOriginalAmount()), TT.gen("已享减免")));
      } else {
        totalAmountList.add(SeaHomeGeneralResponse.fromSmallBlackPadding5(TT.gen("本金"), TT.gen("{0}", principal.getFormattedCurrentAmount())));
      }
    }
    if (instalment.postInterest.compareTo(BigDecimal.ZERO) > 0) {
      BillPageInstalmentItem postInterest = BillPageInstalmentItemCalculator.getPostInterest(instalment, ctx.complianceStrategy, cutInterestCouponDeductAmount, collectionReductionDetail);
      if (postInterest.collectionReductionFlag) {
        totalAmountList.add(SeaHomeGeneralResponse.fromSmallBlackPadding5(TT.gen("综合息费"), TT.gen("{0}", postInterest.getFormattedCurrentAmount()), TT.gen("{0}", postInterest.getFormattedOriginalAmount()), TT.gen("已享减免")));
      } else {
        totalAmountList.add(SeaHomeGeneralResponse.fromSmallBlackPadding5(TT.gen("综合息费"), TT.gen("{0}", postInterest.getFormattedCurrentAmount())));
      }
    }
    if (instalment.overdueInterest.compareTo(BigDecimal.ZERO) > 0) {
      BillPageInstalmentItem overdueInterest = BillPageInstalmentItemCalculator.getOverdueInterest(instalment, collectionReductionDetail);
      if (overdueInterest.collectionReductionFlag) {
        totalAmountList.add(SeaHomeGeneralResponse.fromSmallBlackPadding5(TT.gen("逾期费"), TT.gen("{0}", overdueInterest.getFormattedCurrentAmount()), TT.gen("{0}", overdueInterest.getFormattedOriginalAmount()), TT.gen("已享减免")));
      } else {
        totalAmountList.add(SeaHomeGeneralResponse.fromSmallBlackPadding5(TT.gen("逾期费"), TT.gen("{0}", overdueInterest.getFormattedCurrentAmount())));
      }
    }
    if (instalment.penalty.compareTo(BigDecimal.ZERO) > 0) {
      BillPageInstalmentItem penalty = BillPageInstalmentItemCalculator.getPenalty(instalment, collectionReductionDetail);
      if (penalty.collectionReductionFlag) {
        totalAmountList.add(SeaHomeGeneralResponse.fromSmallBlackPadding5(TT.gen("催收费"), TT.gen("{0}", penalty.getFormattedCurrentAmount()), TT.gen("{0}", penalty.getFormattedOriginalAmount()), TT.gen("已享减免")));
      } else {
        totalAmountList.add(SeaHomeGeneralResponse.fromSmallBlackPadding5(TT.gen("催收费"), TT.gen("{0}", penalty.getFormattedCurrentAmount())));
      }
    }
    return totalAmountList;
  }

  protected CollectionReductionGuideResponse buildCollectionReductionGuide(BillPageContext ctx) {
    return null;
  }

  // 用户必须是登录状态+待还页面+有待还账单+用户存在未生效中收订单，才可以进入到合并支付分流
  private UnionRepaymentInfoResponse buildUnionRepaymentInfoResponse(BillPageParam userContext, boolean hadUnpaidBills) {
    if (!userContext.isLogin) {
      return null;
    }
    if (userContext.instalmentDisplayStatus != InstalmentDisplayStatus.UNPAID) {
      return null;
    }
    if (!hadUnpaidBills) {
      return null;
    }

    if (userContext.build < repaymentAccountConfig.getUnionRepaymentBuild()) {
      return null;
    }

    GoldCardVersion goldCardVersion = jbpCardService.fetchExistGoldenCardVersion(userContext.userId, userContext.build, null);
    if (goldCardVersion == GoldCardVersion.V2) {
      return null;
    }
    JbpOrderVO jbpOrderVO = jbpCardService.queryUnpaidLatestOrder(userContext.userId, userContext.sdkType, OrderSource.GOLD_CARD);
    if (Objects.isNull(jbpOrderVO)) {
      return null;
    }
    if (!unionRepaymentService.fetchUnionRepaymentResult(userContext.userId)) {
      return null;
    }
    return UnionRepaymentInfoResponse.from(UnionRepaymentType.JBP,
        jbpOrderVO.getOrderId(), jbpOrderVO.getNeedPayAmount(), jbpConfig.getGoldenCardUnionRepaymentAgreementUrl()
        , jbpConfig.getGoldenCardUnionRepaymentReward());
  }

  private List<InstalmentListResponse.JbpUnpaidOrderResponse> buildJbpOrderList(
      BillPageParam userContext,
      InstalmentDisplayStatus status,
      IDNHomepageLoanStatusV5 homepageLoanStatusV5
  ) {
    SourceType sourceType = sourceTypeService.getCurrentOrRegisterSourceType(userContext.userId);
    if (Optional.ofNullable(sourceType).map(SourceType::isBlockingMarketingResource).orElse(false)) {
      return Collections.emptyList();
    }

    if (Objects.isNull(userContext.userId)) {
      return Collections.emptyList();
    }
    if (homepageLoanStatusV5.overdueStatus() && !jbpConfig.showUnpaidOrderToOverdueUser()) {
      return Collections.emptyList();
    }
    if (userContext.build < jbpConfig.getGoldenCardV2StartBuild()) {
      return null;
    }
    try {
      JbpOrderVO orderVO = jbpCardService.getOrderByStatus(
          GetOrderRequest.builder().orderSource(Objects.equals(jbpConfig.getBlackDemoBuild(), userContext.build) ? OrderSource.BLACK_CARD : OrderSource.GOLD_CARD)
              .userId(userContext.userId)
              .orderStatus(OrderStatus.VALID_UNPAID)
              .sdkType(userContext.sdkType.name())
              .build());
      if (Objects.isNull(orderVO)) {
        return Collections.emptyList();
      }
      if (!orderVO.needPayNowForValidNotPay(jbpConfig.getNeedPayDay())) {
        return Collections.emptyList();
      }
      // 屏蔽开关打开(非演示版本)，不展示中收待还订单
      if (jbpCardService.disPlayBlock(userContext.userId, userContext.build)) {
        return Collections.emptyList();
      }
      JbpProductVO productById = jbpCardService.getProductById(orderVO.getProductId());
      // 体验期展示：若产品配置了 trialPeriod，则仅在激活后 trialPeriod 天内展示；过期返回空
      if (!orderVO.canDisplayInTrialPeriod(productById, userContext.sdkType.getTimeZone())) {
        return Collections.emptyList();
      }
      return Collections.singletonList(InstalmentListResponse.JbpUnpaidOrderResponse.from(orderVO, productById));
    } catch (Exception e) {
      log.error("Failed to get jbp order list for user {} and build {}.", userContext.userId, userContext.build, e);
      return Collections.emptyList();
    }
  }

  protected void buildBillPageEnhancementResponse(BillPageContext billPageContext, InstalmentListResponse instalmentListResponse) {
    BillPageParam billPageParam = billPageContext.billPageParam;

    if (billPageParam.userId != null) {
      try {
        PointDeductionInfoResponse deductionInfo = pointBillPreviewService.buildPointDeductionInfo(
            billPageParam.userId,
            instalmentListResponse.totalUnpaidAmount);
        instalmentListResponse.setPointDeductionInfo(deductionInfo);
      } catch (Exception e) {
        log.warn("bill page point deduction enrich failed userId={}", billPageParam.userId, e);
      }
    }

    // 账单页 V2 合并还款实验入组：
    //   - 放在所有 short-return 之前，保证 PAID / reminderStrategy.A|B 等任意分支用户都能拿到该字段
    //   - "有待还订单" 包含未还分期 与 中收（JBP/Easyplus）未支付订单任一非空；jbpOrderList 已在
    //     buildBillPageResponse 中组装到 instalmentListResponse 上，这里直接复用，避免重复查询
    boolean hasUnpaidOrder = CollectionUtils.isNotEmpty(billPageContext.unpaidInstalmentList)
        || CollectionUtils.isNotEmpty(instalmentListResponse.jbpOrderList);
    instalmentListResponse.mergeRepaymentDueWithinDays =
        repaymentExperimentSupport.getMergeRepaymentDueWithinDays(
            billPageParam.userId, billPageParam.build, hasUnpaidOrder);

    // 已结清时不做处理
    if (InstalmentDisplayStatus.PAID == billPageParam.instalmentDisplayStatus) {
      return;
    }

    // 1、逾期和未逾期实验
    instalmentListResponse.reminderExpResult = billPageContext.repaymentReminderStrategy.name();
    if (billPageContext.repaymentReminderStrategy.isAorB()) {
      return;
    }

    // 3、待还账单强化提醒V2版本字段处理
    instalmentListResponse.indexToContentMap = buildInstalmentIndices(billPageContext, instalmentListResponse);
    instalmentListResponse.amountResponse = buildLoanOrderAmountResponse(billPageContext, instalmentListResponse);
    instalmentListResponse.reminderGuide = buildRepaymentReminderGuide(billPageContext);
    instalmentListResponse.repayButtonInfo = buildRepayButtonInfo(billPageContext);
    enrichInstalmentDisplayFields(billPageContext, instalmentListResponse);
    instalmentListResponse.instalmentPageGuideBubbleInfo = null;
    //中收账单
    enrichJbpOrderDisplayFields(billPageParam, instalmentListResponse);
  }

  protected Map<Integer /* instalment index */, InstalmentIndexContentResponse> buildInstalmentIndices(
      BillPageContext billPageContext,
      InstalmentListResponse instalmentListResponse
  ) {
    List<InstalmentResponse> instalmentList = instalmentListResponse.instalmentList;
    if (CollectionUtils.isEmpty(instalmentList)) {
      return Collections.emptyMap();
    }
    long now = Clock.now();
    for (int i = 0; i < instalmentList.size(); i++) {
      if (shouldIncludeInstalmentByStatus(billPageContext, now, billPageContext.repaymentDisplayStatus, instalmentList.get(i))) {
        return Collections.singletonMap(i, InstalmentIndexContentResponse.from(TT.gen("剩余待还账单")));
      }
    }
    return Collections.emptyMap();
  }

  protected boolean shouldIncludeInstalmentByStatus(BillPageContext billPageContext, Long now, RepaymentDisplayStatus status, InstalmentResponse instalment) {
    int daysBetween = Clock.getCalenderDaysBetween(now, instalment.billingDate, billPageContext.billPageParam.sdkType.getTimeZone());

    switch (status) {
      case NO_UNPAID_INSTALMENT_IN_X_DAYS:
        return false;

      case HAS_UNPAID_INSTALMENT_IN_X_DAYS:
        return repaymentConfig.getRepaymentReminderShowDaysWhenHasUnpaidInstalment() < daysBetween;

      case HAS_UNPAID_INSTALMENT_WITHIN_24H:
        return 0 < daysBetween;

      case OVERDUE_ONE_DAY:
      case OVERDUE_BEYOND_X_DAYS:
      case OVERDUE_WITHIN_X_DAYS:
        return 0 <= daysBetween;

      default:
        throw EcException.error("shouldIncludeInstalmentByStatus not include enum:{} ", status.name());
    }
  }

  /**
   * 根据还款状态计算对应的分期金额总和
   */
  protected LoanOrderAmountResponse buildLoanOrderAmountResponse(BillPageContext billPageContext, InstalmentListResponse instalmentListResponse) {
    List<InstalmentResponse> instalmentList = instalmentListResponse.instalmentList;
    long currentTime = Clock.now();
    TimeZone timeZone = billPageContext.billPageParam.sdkType.getTimeZone();
    EcCurrency currency = billPageContext.billPageParam.sdkType.getCurrency();
    BigDecimal urgentUnpaidAmount;

    switch (billPageContext.repaymentDisplayStatus) {
      case NO_OUTSTANDING:
        return null;
      case NO_UNPAID_INSTALMENT_IN_X_DAYS:
        urgentUnpaidAmount = calculateNearestDueDateTotalAmount(instalmentList);
        return LoanOrderAmountResponse.from(TT.gen("总待还"), FOREST_GREEN, TT.gen("近日待还"), AmountFormatter.format(currency, urgentUnpaidAmount), BLACK_000);
      case HAS_UNPAID_INSTALMENT_IN_X_DAYS:
        int warningDays = repaymentConfig.getRepaymentReminderShowDaysWhenHasUnpaidInstalment();
        urgentUnpaidAmount = calculateAmountWithinWarningPeriod(warningDays, instalmentList, currentTime, timeZone);
        return LoanOrderAmountResponse.from(TT.gen("总待还"), FOREST_GREEN, TT.gen("近{0}日待还", warningDays), AmountFormatter.format(currency, urgentUnpaidAmount), BLACK_000);
      case HAS_UNPAID_INSTALMENT_WITHIN_24H:
        urgentUnpaidAmount = calculateAmountDueToday(instalmentList, currentTime, timeZone);
        return LoanOrderAmountResponse.from(TT.gen("总待还"), FOREST_GREEN, TT.gen("今日待还。"), AmountFormatter.format(currency, urgentUnpaidAmount), BLACK_000);
      case OVERDUE_ONE_DAY:
      case OVERDUE_BEYOND_X_DAYS:
      case OVERDUE_WITHIN_X_DAYS:
        urgentUnpaidAmount = calculateOverdueAmount(instalmentList, currentTime, timeZone);
        return LoanOrderAmountResponse.from(TT.gen("总待还"), SCARLET, TT.gen("已逾期"), AmountFormatter.format(currency, urgentUnpaidAmount), SCARLET);
      default:
        throw EcException.error("Unsupported repaymentDisplayStatus: {} ", billPageContext.repaymentDisplayStatus);
    }
  }


  protected RepayButtonResponse buildRepayButtonInfo(BillPageContext billPageContext) {
    switch (billPageContext.repaymentDisplayStatus) {
      case NO_OUTSTANDING:
        return null;
      case NO_UNPAID_INSTALMENT_IN_X_DAYS:
      case HAS_UNPAID_INSTALMENT_IN_X_DAYS:
      case HAS_UNPAID_INSTALMENT_WITHIN_24H:
        return RepayButtonResponse.from(GREEN);
      case OVERDUE_ONE_DAY:
      case OVERDUE_BEYOND_X_DAYS:
      case OVERDUE_WITHIN_X_DAYS:
        return RepayButtonResponse.from(SCARLET);
      default:
        throw EcException.error("Unsupported repaymentDisplayStatus: {} ", billPageContext.repaymentDisplayStatus);
    }
  }

  /**
   * 计算最近到期日的分期总金额
   */
  private BigDecimal calculateNearestDueDateTotalAmount(List<InstalmentResponse> instalmentList) {
    Long nearestBillingDate = instalmentList.get(0).billingDate;
    // 计算该日期所有分期的总金额
    return instalmentList.stream()
        .filter(instalment -> instalment.billingDate.equals(nearestBillingDate))
        .map(instalment -> instalment.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * 计算预警期内的分期总金额
   */
  private BigDecimal calculateAmountWithinWarningPeriod(int warningDays, List<InstalmentResponse> instalmentList, long currentTime, TimeZone timeZone) {
    return instalmentList.stream()
        .filter(instalment -> {
          int daysBetween = Clock.getCalenderDaysBetween(currentTime, instalment.billingDate, timeZone);
          return daysBetween <= warningDays && daysBetween >= 0;
        })
        .map(instalment -> instalment.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * 计算今天到期的分期总金额
   */
  private BigDecimal calculateAmountDueToday(List<InstalmentResponse> instalmentList, long currentTime, TimeZone timeZone) {
    return instalmentList.stream()
        .filter(instalment -> {
          int daysBetween = Clock.getCalenderDaysBetween(currentTime, instalment.billingDate, timeZone);
          return daysBetween == 0;
        })
        .map(instalment -> instalment.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * 计算已逾期的分期总金额
   */
  private BigDecimal calculateOverdueAmount(List<InstalmentResponse> instalmentList, long currentTime, TimeZone timeZone) {
    return instalmentList.stream()
        .filter(instalment -> {
          int daysBetween = Clock.getCalenderDaysBetween(currentTime, instalment.billingDate, timeZone);
          return daysBetween < 0;
        })
        .map(instalment -> instalment.amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  protected ReminderGuideResponse buildRepaymentReminderGuide(BillPageContext billPageContext) {
    String iconUrl;
    switch (billPageContext.repaymentDisplayStatus) {
      case HAS_UNPAID_INSTALMENT_WITHIN_24H:
        iconUrl = repaymentConfig.getRepaymentOrderCardReminderIconUrl().get(RepaymentDisplayStatus.HAS_UNPAID_INSTALMENT_WITHIN_24H);
        return ReminderGuideResponse.from(TT.gen("逾期将影响您的借款资质,请及时还款!"), BROWN, iconUrl, BEIGE);
      case OVERDUE_ONE_DAY:
      case OVERDUE_BEYOND_X_DAYS:
      case OVERDUE_WITHIN_X_DAYS:
        iconUrl = repaymentConfig.getRepaymentOrderCardReminderIconUrl().get(RepaymentDisplayStatus.OVERDUE_ONE_DAY);
        return ReminderGuideResponse.from(TT.gen("逾期将影响您的借款资质,请及时还款!"), SCARLET, iconUrl, PALE_RED);
      default:
        return null;
    }
  }

  private void enrichInstalmentDisplayFields(BillPageContext billPageContext, InstalmentListResponse instalmentListResponse) {
    List<InstalmentResponse> instalmentList = instalmentListResponse.instalmentList;
    if (CollectionUtils.isEmpty(instalmentList)) {
      return;
    }
    SDKType sdkType = billPageContext.billPageParam.sdkType;
    for (InstalmentResponse instalment : instalmentList) {
      instalment.amountToBillingWithDesc = TT.gen("第{0}期 {1}", instalment.index, AmountFormatter.format(sdkType.getCurrency(), instalment.amount));
      instalment.timeToBillingResponse = buildTimeToBillingResponse(billPageContext, instalmentListResponse, instalment);
      instalment.loanOrderCreatedDateDesc = TT.gen("下一个还款日：{0}", Clock.dateTimeStringFromTimestampWithLocale(instalment.billingDate, DateFormatter.dd___MMM___yyyy, sdkType.getTimeZone(), sdkType.getLocale()));
    }
  }

  protected TimeToBillingResponse buildTimeToBillingResponse(BillPageContext billPageContext, InstalmentListResponse instalmentListResponse, InstalmentResponse instalment) {
    Long millisToBillingDate = instalment.millisToBillingDate;
    if (millisToBillingDate == null) {
      return null;
    }
    int days = (int) (millisToBillingDate / Clock.MILLS_PER_DAY);

    if (millisToBillingDate >= 0) {
      if (days > 0) {
        return TimeToBillingResponse.from(TT.gen("剩余{0}天。", days), TT.gen("{0}", String.valueOf(days)), FOREST_GREEN);
      }
      if (days == 0) {
        return TimeToBillingResponse.from(TT.gen("今天到期"), null, FOREST_GREEN);
      }
    }
    // 已逾期
    return TimeToBillingResponse.from(TT.gen("逾期{0}天", Math.abs(days) + 1), null, SCARLET);
  }

  private void enrichJbpOrderDisplayFields(BillPageParam billPageParam, InstalmentListResponse instalmentListResponse) {
    List<InstalmentListResponse.JbpUnpaidOrderResponse> jbpOrderList = instalmentListResponse.jbpOrderList;
    for (InstalmentListResponse.JbpUnpaidOrderResponse jbpUnpaidOrder : jbpOrderList) {
      String createdDate = Clock.dateTimeStringFromTimestampWithLocale(jbpUnpaidOrder.getTimeCreated(), DateFormatter.dd___MMM___yyyy, billPageParam.sdkType.getTimeZone(), billPageParam.sdkType.getLocale());
      jbpUnpaidOrder.timeCreatedDateDesc = TT.gen("借款日期: {0}", createdDate);
      jbpUnpaidOrder.amountToBillingWithDesc = TT.gen("Easyplus {0}", AmountFormatter.format(billPageParam.sdkType.getCurrency(), jbpUnpaidOrder.owedAmount));
    }
  }

  private FastPaymentButtonResponse buildFastPaymentButton(BillPageContext billPageContext) {
    BillPageParam userContext = billPageContext.billPageParam;

    // 前置检查：如果不满足条件则返回空
    if (!isButtonDisplayable(userContext)) {
      return FastPaymentButtonResponse.empty();
    }

    // 获取最近还款记录
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(userContext.sdkType);
    PaymentVO latestRepayment = paymentService.getLatestPayment(userContext.userId, businessName, PaymentTransType.REPAY, PaymentMethod.VA_AND_DD_PAYMENT_METHOD_LIST);

    // 根据是否有还款记录选择不同路径
    FastPaymentButtonResponse result;
    try {
      result = Objects.isNull(latestRepayment) ? buildFirstPaymentButton(userContext, businessName)
          : buildSubsequentPaymentButton(userContext, latestRepayment);
    } catch (Exception e) {
      log.warn("构建快速支付按钮 e:", e);
      result = FastPaymentButtonResponse.empty();
    }
    trackBillPageChannelListExposure(userContext, result);
    return result;
  }

  private void trackBillPageChannelListExposure(BillPageParam userContext, FastPaymentButtonResponse response) {
    if (response == null || response.getLogoUrl() == null) {
      return;
    }
    String channel = extractChannelFromFastPaymentButton(response);
    if (channel == null) {
      return;
    }
    repaymentChannelMonitorService.logListExposure(userContext.userId, channel,
        resolveRepaymentPaymentProvider(response),
        RepaymentChannelMonitorService.PAGE_SOURCE_BILL);
  }

  private PaymentProvider resolveRepaymentPaymentProvider(FastPaymentButtonResponse response) {
    if (response.getData() instanceof RepaymentAccountResponse) {
      return ((RepaymentAccountResponse) response.getData()).resolveRepaymentPaymentProvider();
    }
    return PaymentProvider.NONE;
  }

  private String extractChannelFromFastPaymentButton(FastPaymentButtonResponse response) {
    if (response.getData() instanceof RepaymentAccountResponse) {
      return ((RepaymentAccountResponse) response.getData()).getChannelType();
    }
    return null;
  }

  /**
   * 检查是否应该显示快速还款按钮
   */
  private boolean isButtonDisplayable(BillPageParam userContext) {
    if (!userContext.isLogin) {
      return false;
    }
    if (userContext.instalmentDisplayStatus != InstalmentDisplayStatus.UNPAID) {
      return false;
    }
    String expResult = repaymentExperimentSupport.getRepayChannelNewDisplayResult(userContext.userId, userContext.build);
    return "B".equals(expResult);
  }

  /**
   * 构建首次还款按钮响应
   */
  private FastPaymentButtonResponse buildFirstPaymentButton(BillPageParam userContext, PaymentBusinessName businessName) {
    // 获取最近的提现记录作为首次还款参考
    PaymentVO payoutRecord = paymentService.getLatestPayment(userContext.userId, businessName, PaymentTransType.PAYOUT, ImmutableList.of(PaymentMethod.BANKCARD));
    if (Objects.isNull(payoutRecord)) {
      return createDefaultResponse(userContext);
    }

    String channel = paymentService.getChannelNameFromPaymentVO(payoutRecord);

    // 验证渠道是否支持
    RepaymentAccountDisplayConfig channelConfig = getChannelConfig(userContext, channel);
    if (channelConfig == null) {
      log.info("Unsupported repayment channel for first payment: {}", channel);
      return createDefaultResponse(userContext);
    }

    String repaymentChannelGroup = repaymentChannelTool.resolvePaymentMethodByChannelType(channel);
    RepaymentAccountResponse data = repaymentAccountResponseBuilder.buildOne(RepaymentAccountVO.from(channel, channelConfig),
        userContext.userId, userContext.sdkType, userContext.build, RepayStyleVersion.V1);
    return FastPaymentButtonResponse.fromFirstRepay(data.getLogoUrl(), repaymentChannelGroup, data);
  }

  /**
   * 构建非首次还款按钮响应
   */
  private FastPaymentButtonResponse buildSubsequentPaymentButton(BillPageParam userContext, PaymentVO paymentVO) {
    // 根据支付方式处理不同逻辑
    if (PaymentMethod.DIRECT_DEBIT.equals(paymentVO.getPaymentMethod())) {
      return buildDirectDebitResponse(userContext, paymentVO);
    } else {
      String channel = paymentService.getChannelNameFromPaymentVO(paymentVO);
      return buildOtherPaymentResponse(userContext, channel);
    }
  }

  /**
   * 获取渠道配置信息
   */
  private RepaymentAccountDisplayConfig getChannelConfig(BillPageParam userContext, String channel) {
    PaymentAccount paymentAccount = cashLoanService.getPaymentAccount(userContext.loanAccountId);
    Map<String, RepaymentAccountDisplayConfig> repaymentChannelMap = paymentProviderSelectorService.getChannelDisplayMap(paymentAccount, userContext.userId);
    // 陪跑对比取 remove 之前的原始路由结果，实验去渠道属调用方展示策略、不在 adapter 语义内
    repaymentChannelRouteCompareService.compareIfEnabled(RepaymentChannelRouteCompareService.SCENE_BILL_PAGE,
        paymentAccount, userContext.userId, repaymentChannelMap);
    // 是否移除 PERMATA、CIMB、DANAMON 还款渠道（根据实验判断）
    if (repaymentAccountService.canNotDisplayMidtransBankChannels(userContext.userId)) {
      repaymentChannelMap.remove(DynamicAccountChannel.PERMATA.name());
      repaymentChannelMap.remove(DynamicAccountChannel.CIMB.name());
      repaymentChannelMap.remove(DynamicAccountChannel.DANAMON.name());
    }
    if (repaymentAccountService.canNotDisplaySeaBankChannels(userContext.userId)) {
      repaymentChannelMap.remove(DynamicAccountChannel.SEABANK.name());
    }
    repaymentChannelMap.remove(DynamicAccountChannel.XENDIT_QRIS.name());
    return repaymentChannelMap.get(channel);
  }

  /**
   * 构建默认响应
   */
  private FastPaymentButtonResponse createDefaultResponse(BillPageParam userContext) {
    String defaultChannel = "BCA";
    RepaymentAccountDisplayConfig channelConfig = getChannelConfig(userContext, defaultChannel);
    RepaymentAccountResponse data = repaymentAccountResponseBuilder.buildOne(RepaymentAccountVO.from(defaultChannel, channelConfig),
        userContext.userId, userContext.sdkType, userContext.build, RepayStyleVersion.V1);
    return FastPaymentButtonResponse.fromFirstRepay(data.getLogoUrl(), RepaymentChannelGroup.VIRTUAL_ACCOUNT.name(), data);
  }

  /**
   * 构建代扣还款响应
   */
  private FastPaymentButtonResponse buildDirectDebitResponse(BillPageParam userContext, PaymentVO paymentVO) {
    // 获取代扣账户信息
    DirectDebitLinkAccountVO accountVO = directDebitAccountService.findAccountVOByIdOrThrow(paymentVO.getPaymentCredential().getId());
    if (accountVO == null) {
      return createDefaultResponse(userContext);
    }

    if (accountVO.provider == DirectDebitProvider.SHOPEE_PAY && accountVO.bankType == BankType.SHOPEE_PAY) {
      return buildOtherPaymentResponse(userContext, DynamicAccountChannel.SHOPEE.name());
    }

    if(accountVO.status != DirectDebitAccountStatus.ENABLED) {
      return createDefaultResponse(userContext);
    }
    String repaymentChannelGroup = RepaymentChannelGroup.DIRECT_DEBIT.name();
    DirectDebitBankInfo bankInfo = directDebitAccountService.getSupportedBankInfoByBiz(DirectDebitAuthorizationBusinessType.EC,
        accountVO.provider, accountVO.bankType);
    Object data = DirectDebitAccountResponse.from(accountVO, DirectDebitBankResponse.from(bankInfo));

    return FastPaymentButtonResponse.fromNonFirstRepay(bankInfo.logoUrl, repaymentChannelGroup, data);
  }

  /**
   * 构建其他支付方式响应
   */
  private FastPaymentButtonResponse buildOtherPaymentResponse(BillPageParam userContext, String channel) {
    RepaymentAccountDisplayConfig channelConfig = getChannelConfig(userContext, channel);
    if (channelConfig == null) {
      log.info("Unsupported repayment channel: {}", channel);
      return createDefaultResponse(userContext);
    }
    String repaymentChannelGroup = repaymentChannelTool.resolvePaymentMethodByChannelType(channel);
    RepaymentAccountResponse data = repaymentAccountResponseBuilder.buildOne(RepaymentAccountVO.from(channel, channelConfig), userContext.userId,
        userContext.sdkType, userContext.build, RepayStyleVersion.V1);

    return FastPaymentButtonResponse.fromNonFirstRepay(data.getLogoUrl(), repaymentChannelGroup, data);
  }
}
