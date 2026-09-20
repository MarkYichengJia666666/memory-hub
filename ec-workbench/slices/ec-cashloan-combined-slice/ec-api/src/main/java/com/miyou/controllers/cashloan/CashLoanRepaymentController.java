package com.miyou.controllers.cashloan;

import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;
import static com.yqg.ec.common.enums.order.CashLoanOrderStatus.READY;

import com.google.common.collect.ImmutableList;
import com.miyou.controllers.directdebit.response.DirectDebitBankResponse;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.miyou.controllers.cashloan.repayment.RepaymentPointUseInfoRequest;
import com.miyou.controllers.cashloan.repayment.RepaymentPointUseInfoResponse;
import com.miyou.controllers.cashloan.repayment.RepaymentPreCheckRequest;
import com.miyou.controllers.cashloan.repayment.RepaymentPreCheckResponse;
import com.miyou.controllers.cashloan.repayment.calculate.RepaymentCalculateRequest;
import com.miyou.controllers.cashloan.repayment.calculate.RepaymentCalculateResponse;
import com.miyou.controllers.cashloan.repayment.calculate.infra.RepaymentCalculator;
import com.miyou.controllers.cashloan.repayment.calculate.infra.RepaymentCalculatorFactory;
import com.miyou.controllers.cashloan.request.CashLoanGetOVORepaymentAccountRequest;
import com.miyou.controllers.cashloan.request.CashLoanGetRepaymentAccountRequest;
import com.miyou.controllers.cashloan.service.RepaymentAccountResponseBuilder;
import com.miyou.controllers.cashloan.utilities.RepaymentChannelTool;
import com.miyou.controllers.cashloan.response.*;
import com.miyou.controllers.cashloan.response.repayment.UserRepaymentResponse;
import com.miyou.controllers.directdebit.DirectDebitAccountApiService;
import com.miyou.controllers.directdebit.response.DirectDebitAccountListResponse;
import com.miyou.controllers.directdebit.response.DirectDebitAccountResponse;
import com.miyou.controllers.loan.BaseLoanController;
import com.miyou.controllers.secure.SecureCheckUserContextFactory;
import com.miyou.utilities.secureapi.ECSecuredApi;
import com.miyou.utilities.secureapi.RequestParser;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitAccountStatus;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.cashloan.enums.OrderType;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.repay.UnionRepaymentService;
import com.yqg.core.service.cashloan.repay.enums.RepaymentChannelPageDisplayStrategy;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repay.vo.DeductIntentionVO;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentPlanVO;
import com.yqg.core.service.cashloan.repayment.RepaymentAccountFactory;
import com.yqg.core.service.cashloan.repayment.enums.RepaymentAccountSourceType;
import com.yqg.core.service.cashloan.util.rate.CalcFeeUtil;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.orderview.CashLoanOrderViewVO;
import com.yqg.core.service.directdebit.DirectDebitAccountService;
import com.yqg.core.service.directdebit.activity.DirectActivityRewardService;
import com.yqg.core.service.directdebit.enums.DirectDebitProvider;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.infos.AppInfoVO;
import com.yqg.core.service.loan.repayment.account.PaymentProviderSelectorService;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountService;
import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountCallerType;
import com.yqg.core.service.loan.repayment.account.vo.OVORepaymentInfoVO;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountDisplayConfig;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.loan.repayment.analysis.RepaymentAnalysisInitContext;
import com.yqg.core.service.loan.repayment.analysis.RepaymentAnalysisService;
import com.yqg.core.service.loan.repayment.experiment.OvoRecommendLogoDecision;
import com.yqg.core.service.loan.repayment.experiment.RepaymentExperimentSupport;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.mobile.verification.VerificationService;
import com.yqg.core.service.loan.repayment.account.RepaymentChannelMonitorService;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.PaymentBusinessNameMapper;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.secure.check.context.SecureCheckUserContext;
import com.yqg.core.service.sourcetype.SourceTypeService;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class CashLoanRepaymentController extends BaseLoanController {
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private RepaymentAccountService repaymentAccountService;
  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  private VerificationService verificationService;
  @Autowired
  private SecureCheckUserContextFactory userContextFactory;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private DirectDebitAccountApiService directDebitAccountApiService;
  @Autowired
  private UnionRepaymentService unionRepaymentService;
  @Autowired
  private RepaymentAnalysisService repaymentAnalysisService;
  @Autowired
  private RepaymentExperimentSupport repaymentExperimentSupport;
  @Autowired
  private SourceTypeService sourceTypeService;
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService instalmentCutInterestCouponDeductDetailService;
  @Autowired
  private DirectDebitAccountService directDebitAccountService;
  @Autowired
  private RepaymentChannelTool repaymentChannelTool;
  @Autowired
  private RepaymentChannelMonitorService repaymentChannelMonitorService;
  @Autowired
  private CashLoanService cashLoanService;
  @Autowired
  private DirectActivityRewardService directActivityRewardService;
  @Autowired
  private PaymentProviderSelectorService paymentProviderSelectorService;
  @Autowired
  private RepaymentAccountResponseBuilder repaymentAccountResponseBuilder;

  @PostMapping(path = "/api/cashloan/assertNoInstalmentCompleted")
  @ECSecuredApi
  public Result assertNoInstalmentCompleted(@RequestBody RepaymentPreCheckRequest request) {
    checkRepaymentPreCheckRequest(request, false);
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    List<Long> instalmentIds = request.encodeInstalmentIds.stream().map(YqgHashids::decode).collect(Collectors.toList());
    List<CashLoanInstalmentVO> instalmentList = ecOrderService.getInstalmentVOs(instalmentIds);
    checkInstalmentParam(viewerContext, instalmentList);
    buildUnionRepaymentPlan(viewerContext, request);
    return EcResponseUtil.generateSuccess();
  }

  @ECSecuredApi
  @PostMapping(path = "/api/cashloan/instalment/repayment/preCheck")
  public Result<RepaymentPreCheckResponse> instalmentRepaymentPreCheck(@RequestBody RepaymentPreCheckRequest request) {
    checkRepaymentPreCheckRequest(request, true);
    LoanApiViewerContext ctx = getViewerContextFromRequest();
    repaymentChannelMonitorService.logPreCheck(ctx.userId);
    //降级开关，走老逻辑部分还款; 若encodeInstalmentIds非空，走老逻辑部分还款
    if (CollectionUtils.isNotEmpty(request.encodeInstalmentIds)) {
      return processInstalmentRepaymentRequest(ctx, request);
    }
    //encodeInstalmentIds为空，则入参只有联合支付
    return processUnionOnlyRepaymentRequest(ctx, request);
  }

  private void checkRepaymentPreCheckRequest(RepaymentPreCheckRequest request, boolean supportsPartialRepayment) {
    //降级开关
    if (!supportsPartialRepayment) {
      EcAsserts.assertTrueOrThrowWarn(
          CollectionUtils.isNotEmpty(request.encodeInstalmentIds),
          EcExceptionType.COMMON_SERVER_ERROR, TT.gen("分期Id不能为空")
      );
      return;
    }
    EcAsserts.assertTrueOrThrowWarn(
        CollectionUtils.isNotEmpty(request.encodeInstalmentIds) || CollectionUtils.isNotEmpty(request.unionRepaymentRequests),
        EcExceptionType.COMMON_SERVER_ERROR, TT.gen("分期Id和联合支付列表必填其一")
    );
  }

  /**
   * 处理包含EC分期还款请求
   */
  private Result<RepaymentPreCheckResponse> processInstalmentRepaymentRequest(LoanApiViewerContext ctx, RepaymentPreCheckRequest request) {
    List<Long> instalmentIds = request.encodeInstalmentIds.stream().map(YqgHashids::decode).collect(Collectors.toList());
    List<CashLoanInstalmentVO> instalmentList = ecOrderService.getInstalmentVOs(instalmentIds);
    checkInstalmentParam(ctx, instalmentList);
    buildUnionRepaymentPlan(ctx, request);
    return EcResponseUtil.generate(buildRepaymentPreCheckResponse(ctx, instalmentList, request));
  }

  /**
   * 处理仅包含联合支付还款请求
   */
  private Result<RepaymentPreCheckResponse> processUnionOnlyRepaymentRequest(LoanApiViewerContext ctx, RepaymentPreCheckRequest request) {
    buildUnionRepaymentPlanForEcEmpty(ctx, request);
    //只进行联合支付还款，不支持部分支付，返回默认的H5不支持部分支付策略
    return EcResponseUtil.generate(buildRepaymentPreCheckResponseForH5Default());
  }

  private void checkInstalmentParam(LoanApiViewerContext ctx, List<CashLoanInstalmentVO> instalmentList) {
    EcAsserts.assertTrue(
        instalmentList.stream().allMatch(instalmentVO -> Objects.equals(instalmentVO.userId, ctx.userId)),
        "The payer corresponding to the unpaid instalment is inconsistent with the current payer, payer userId = {}, unpaid instalmentList = {}", ctx.userId, instalmentList
    );
    EcAsserts.assertTrueOrThrowWarn(
        instalmentList.stream().allMatch(instalmentVO -> CashLoanInstalmentStatus.INIT == instalmentVO.status),
        EcExceptionType.CASH_LOAN_INSTALMENT_STATUS_CHANGED, TT.gen("您的未还账单已变更，请重新选择账单进行还款。")
    );
  }

  private DeductIntentionVO buildUnionRepaymentPlan(LoanApiViewerContext viewerContext, RepaymentPreCheckRequest request) {
    List<UnionRepaymentPlanVO> unionRepaymentPlanVOList = new ArrayList<>();
    //先构造EC_REPAY还款计划
    unionRepaymentPlanVOList.add(deserializeRepaymentPlan(request.encodeInstalmentIds, UnionRepaymentType.EC_REPAY));
    //再构造中收还款计划
    if (Objects.nonNull(request.unionRepaymentRequests)) {
      request.unionRepaymentRequests.forEach(unionRepaymentRequest -> unionRepaymentPlanVOList.add(deserializeRepaymentPlan(unionRepaymentRequest.encodeBusinessIds, UnionRepaymentType.valueOf(unionRepaymentRequest.unionRepaymentType))));
    }
    DeductIntentionVO deductIntentionVO = unionRepaymentService.initiateUnionRepaymentPlan(viewerContext.userId, unionRepaymentPlanVOList);

    repaymentAnalysisService.initRepaymentAnalysisRecord(RepaymentAnalysisInitContext.builder()
        .userId(viewerContext.userId)
        .loanAccountId(viewerContext.loanAccountId)
        .instalmentIdList(request.encodeInstalmentIds.stream().map(YqgHashids::decode).collect(Collectors.toList()))
        .deductIntentionLogId(deductIntentionVO.id)
        .build());
    return deductIntentionVO;
  }

  private void buildUnionRepaymentPlanForEcEmpty(LoanApiViewerContext viewerContext, RepaymentPreCheckRequest request) {
    List<UnionRepaymentPlanVO> unionRepaymentPlanVOList = new ArrayList<>();
    //只构造中收还款计划
    request.unionRepaymentRequests.forEach(unionRepaymentRequest -> unionRepaymentPlanVOList.add(deserializeRepaymentPlan(unionRepaymentRequest.encodeBusinessIds, UnionRepaymentType.valueOf(unionRepaymentRequest.unionRepaymentType))));
    unionRepaymentService.initiateUnionRepaymentPlan(viewerContext.userId, unionRepaymentPlanVOList);
  }

  private UnionRepaymentPlanVO deserializeRepaymentPlan(List<String> encodedBusinessIds, UnionRepaymentType unionRepaymentType) {
    UnionRepaymentPlanVO unionRepaymentPlanVO = new UnionRepaymentPlanVO();
    unionRepaymentPlanVO.unionRepaymentType = unionRepaymentType;
    unionRepaymentPlanVO.businessIds = encodedBusinessIds.stream().map(YqgHashids::decode).collect(Collectors.toList());
    unionRepaymentPlanVO.weight = repaymentAccountConfig.getUnionRepaymentWeight(unionRepaymentType);
    return unionRepaymentPlanVO;
  }

  private RepaymentPreCheckResponse buildRepaymentPreCheckResponse(LoanApiViewerContext ctx, List<CashLoanInstalmentVO> instalmentList,
      RepaymentPreCheckRequest request) {
    Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
    RepaymentChannelPageDisplayStrategy displayStrategy = isWholeProcess ? RepaymentChannelPageDisplayStrategy.B :
        repaymentExperimentSupport.getRepaymentChannelPageDisplayStrategy(ctx);
    SourceType sourceType = sourceTypeService.getCurrentOrRegisterSourceType(ctx.userId);
    String webPage = repaymentAccountConfig.getRepaymentChannelH5PageUrlBySourceType(sourceType);
    String h5RepaymentUrl;
    if (isWholeProcess) {
      h5RepaymentUrl = repaymentAccountConfig.getRepaymentChannelH5PageUrlForWholeProcess();
    } else {
      h5RepaymentUrl =
          displayStrategy.supportsWebPage ? Objects.nonNull(webPage) ? webPage : repaymentAccountConfig.getRepaymentChannelH5PageUrl()
              : null;
    }
    RepaymentPreCheckResponse response = RepaymentPreCheckResponse.builder()
        .h5RepaymentUrl(h5RepaymentUrl)
        .repaymentChannelPageDisplayStrategy(displayStrategy)
        .canPartialRepayment(displayStrategy.isConditionalSupportPartialRepaymentStrategy()
            ? instalmentList.stream().anyMatch(i -> i.overdueDays > 0)
            : displayStrategy.supportsPartialRepayment
        ).build();
    return response;
  }

  private RepaymentPreCheckResponse buildRepaymentPreCheckResponseForH5Default() {
    RepaymentChannelPageDisplayStrategy displayStrategy = RepaymentChannelPageDisplayStrategy.B;
    Boolean isWholeProcess = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false);
    String h5RepaymentUrl;
    if (isWholeProcess) {
      h5RepaymentUrl = repaymentAccountConfig.getRepaymentChannelH5PageUrlForWholeProcess();
    } else {
      h5RepaymentUrl = displayStrategy.supportsWebPage ? repaymentAccountConfig.getRepaymentChannelH5PageUrl() : null;
    }
    return RepaymentPreCheckResponse.builder()
        .h5RepaymentUrl(h5RepaymentUrl)
        .repaymentChannelPageDisplayStrategy(displayStrategy)
        .canPartialRepayment(displayStrategy.supportsPartialRepayment)
        .build();
  }
  /**
   * 获取虚拟还款账号
   */
  @GetMapping(path = "/api/v3/cashloan/getRepayVirtualAccount")
  public Result<List<RepaymentAccountResponse>> getRepayVirtualAccountV3(
      @RequestParam(value = "mobileNumber", required = false) String mobileNumber
  ) {
    SecureCheckUserContext userContext = userContextFactory.createUserContextForSelfServiceQueryRepaymentPurpose(ecRequest(), mobileNumber);
    final Long loanAccountId = loanAccountService.getAccountIdByUserIdOrThrow(userContext.userId, userContext.sdkType);

    List<RepaymentAccountResponse> responses = getRepaymentAccountResponse(userContext.userId,
        loanAccountId,
        userContext.build,
        userContext.sdkType,
        userContext.isLogin,
        null,
        RepayStyleVersion.V1);
    return EcResponseUtil.generate(responses);
  }

  public List<RepaymentAccountResponse> getRepaymentAccountResponse(Long userId,
                                                                    Long accountId,
                                                                    Long build,
                                                                    SDKType sdkType,
                                                                    boolean isLogin,
                                                                    OrderType orderType,
                                                                    RepayStyleVersion repayStyleVersion) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    PlatformType platformType = PlatformType.fromString(RequestParser.getPlatformType(ecRequest()));
    AppInfoVO appInfoVo = new AppInfoVO(businessName, platformType, build, sdkType);
    // 因为other和seabank实验一致，查一次透传，减少重复调用
    boolean hideSeaBankChannels = isLogin && repaymentAccountService.canNotDisplaySeaBankChannels(userId);
    // 获取还款渠道
    List<RepaymentAccountVO> repaymentAccountVOS;
    if (isLogin) {
      repaymentAccountVOS = repaymentAccountService.getDisplayRepaymentAccount(Objects.equals(orderType, OrderType.JBP) ? PaymentAccount.IDN : cashLoanService.getPaymentAccount(accountId), userId, appInfoVo, hideSeaBankChannels);
    } else {
      repaymentAccountVOS = cashLoanConfig.getUnpaidPaymentChannels()
          .stream()
          .map(channel -> repaymentAccountService.getBorrowerStaticRepaymentAccountVO(userId, sdkType, cashLoanService.getPaymentAccount(accountId), channel))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }
    List<RepaymentAccountResponse> responses = repaymentAccountResponseBuilder.buildFromAccountVos(repaymentAccountVOS, userId, sdkType, build, repayStyleVersion);
    String otherBankTargetChannel = repaymentAccountConfig.getOtherBankTargetChannel();
    boolean hasOtherBankTargetChannel = repaymentAccountVOS.stream()
        .anyMatch(account -> otherBankTargetChannel.equals(account.getChannel()));
    if (isLogin && !hideSeaBankChannels && hasOtherBankTargetChannel) {
      responses.add(RepaymentAccountResponse.fromOtherResponse(cashLoanConfig.otherBankLogoUrl(repayStyleVersion)));
    }
    return responses;
  }

  public RepaymentAccountResponse getRepaymentQrCode(Long userId, Long accountId, Long build, OrderType orderType, RepayStyleVersion repayStyleVersion) {
    String expResult = repaymentExperimentSupport.getRepaymentQrCodeExpResult(userId, build, orderType);
    if (!StringUtils.equals(expResult, "B")) {
      return null;
    }
    String channel = DynamicAccountChannel.XENDIT_QRIS.name();
    PaymentAccount paymentAccount = cashLoanService.getPaymentAccount(accountId);
    try {
      RepaymentAccountDisplayConfig config = paymentProviderSelectorService.selectProvider(channel, userId, paymentAccount, RepaymentAccountCallerType.DEFAULT);
      String logoUrl = repayStyleVersion == RepayStyleVersion.V2 ? null
          : cashLoanConfig.channelLogoMap(RepayStyleVersion.V1, channel);
      return RepaymentAccountResponse.fromRepaymentAccountForQrCode(RepaymentAccountVO.from(channel, config), logoUrl);
    } catch (Exception e) {
      log.info("Failed to get repayment account for QR code, userId: {}, accountId: {}, channel: {}, error: {}", userId, accountId, channel, e.getMessage(), e);
      return null;
    }
  }

  /**
   * 获取所有类型的还款账号，包括<br/>
   * 1. 虚拟还款账号<br/>
   * 2. 代扣绑定的银行账号<br/>
   */
  @GetMapping(path = "/api/v4/cashloan/getRepayAccount")
  @ECSecuredApi
  public Result<RepaymentAccountResponseV4> getRepayAccount(
      @RequestParam(required = false) OrderType orderType,
      @RequestParam(required = false) RepayStyleVersion repayStyleVersion) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    repayStyleVersion = repayStyleVersion == null ? RepayStyleVersion.V1 : repayStyleVersion;

    // 获取用户在贷金额
    List<CashLoanOrderVO> readyOrders = ecOrderService.getCashLoanOrdersByLoanAccountIdAndStatuses(viewerContext.loanAccountId, READY);
    List<CashLoanOrderViewVO> viewOrders = instalmentCutInterestCouponDeductDetailService.getViewOrders(readyOrders);
    BigDecimal outstandingAmount = viewOrders.stream().map(CalcFeeUtil::getOwedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    UserRepaymentResponse userRepaymentResponse = UserRepaymentResponse.builder()
        .minAmount(repaymentAccountConfig.getRepaymentChannelMinRepaymentAmount())
        .maxAmount(outstandingAmount)
        .build();

    // 获取二维码
    RepaymentAccountResponse qrCode = getRepaymentQrCode(viewerContext.userId, viewerContext.loanAccountId, viewerContext.build, orderType, repayStyleVersion);

    //获取虚拟还款账号
    List<RepaymentAccountResponse> repaymentAccountResponseList = getRepaymentAccountResponse(viewerContext.userId, viewerContext.loanAccountId, viewerContext.build, viewerContext.sdkType, true, orderType, repayStyleVersion);
    postProcessEWalletRecommend(viewerContext, repaymentAccountResponseList);

    //获取代扣账号
    DirectDebitAccountListResponse directDebitAccountListResponse = directDebitAccountApiService.getDirectDebitAccountListResponse(viewerContext.userId, viewerContext.build, viewerContext.sdkType, true);
    directDebitAccountListResponse = postProcessDirectDebitAccountListForRepay(viewerContext, directDebitAccountListResponse);

    Map<String, Object> repaymentChannels = repayStyleVersion == RepayStyleVersion.V1 ? null
        : repaymentChannelTool.buildGroupedChannelsByLatestChannel(viewerContext.userId, viewerContext.sdkType, repaymentAccountResponseList, directDebitAccountListResponse, repayStyleVersion);

    trackRepayPageChannelListExposure(viewerContext.userId, repaymentAccountResponseList, qrCode);

    return EcResponseUtil.generate(
        RepaymentAccountResponseV4.from(userRepaymentResponse, qrCode, repaymentAccountResponseList, directDebitAccountListResponse,
            repaymentChannels, repayStyleVersion));
  }

  /**
   * 电子钱包推荐渠道后置处理：<br/>
   * Shopee 命中直连（{@link RepaymentAccountResponse#getNextAction()} 非空）时，Shopee 置为推荐；<br/>
   * 否则按 OVO 推荐标实验分流设置 OVO 或 DANA（含 DANA_V2）推荐；未入组时回退 DANA。
   * <p><b>为何：</b>直连 Shopee 优先；OVO 实验组展示 OVO 推荐标，对照/空白保持 DANA，与线上一致。</p>
   *
   * @param repaymentAccountResponseList 渠道列表，按引用原地修改
   */
  void postProcessEWalletRecommend(LoanApiViewerContext viewerContext,
      List<RepaymentAccountResponse> repaymentAccountResponseList) {
    if (CollectionUtils.isEmpty(repaymentAccountResponseList)) {
      return;
    }
    repaymentAccountResponseList.forEach(resp -> resp.setRecommend(false));
    String shopeeChannel = DynamicAccountChannel.SHOPEE.name();
    RepaymentAccountResponse shopee = repaymentAccountResponseList.stream().filter(resp -> shopeeChannel.equals(resp.getChannelType()))
        .findFirst().orElse(null);
    if (shopee != null && shopee.getNextAction() != null) {
      shopee.setRecommend(true);
      return;
    }

    String ovoChannel = DynamicAccountChannel.OVO.name();
    boolean hasVisibleOvo = repaymentAccountResponseList.stream()
        .anyMatch(resp -> ovoChannel.equals(resp.getChannelType()));
    OvoRecommendLogoDecision decision = repaymentExperimentSupport.resolveOvoRecommendLogo(viewerContext, hasVisibleOvo);
    if (decision == OvoRecommendLogoDecision.OVO) {
      repaymentAccountResponseList.stream()
          .filter(resp -> ovoChannel.equals(resp.getChannelType()))
          .forEach(resp -> resp.setRecommend(true));
      return;
    }
    if (decision == OvoRecommendLogoDecision.DANA || decision == OvoRecommendLogoDecision.SKIP) {
      recommendDanaChannels(repaymentAccountResponseList);
    }
  }

  private void recommendDanaChannels(List<RepaymentAccountResponse> repaymentAccountResponseList) {
    String danaChannel = DynamicAccountChannel.DANA.name();
    repaymentAccountResponseList.stream()
        .filter(resp -> danaChannel.equals(resp.getChannelType()))
        .forEach(resp -> resp.setRecommend(true));
  }

  /**
   * 还款页代扣账号列表后置处理：营销/全流程场景清空、过滤 DISABLED 绑定、排除 DANA 渠道、代扣奖励活动字段。
   */
  private DirectDebitAccountListResponse postProcessDirectDebitAccountListForRepay(LoanApiViewerContext viewerContext,
      DirectDebitAccountListResponse directDebitAccountListResponse) {
    if (Optional.ofNullable(viewerContext.sourceType).map(SourceType::isBlockingMarketingResource).orElse(false)
        || Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)) {
      return null;
    }
    if (directDebitAccountListResponse == null) {
      return null;
    }

    DirectDebitAccountListResponse processed = DirectDebitAccountListResponse.builder()
        .linkedAccountList(filterLinkedAccountsForRepay(viewerContext, directDebitAccountListResponse.linkedAccountList))
        .supportBankList(filterSupportBanksForRepay(directDebitAccountListResponse.supportBankList))
        .provider(directDebitAccountListResponse.provider)
        .guidePageStrategy(directDebitAccountListResponse.guidePageStrategy)
        .build();

//    enrichSupportBankMotivationInfo(viewerContext.userId, viewerContext.build, processed);
    return processed;
  }

  /**
   * 还款页已绑定代扣账号：过滤 DISABLED 状态，并排除 DANA 渠道。
   * <p><b>为何：</b>DISABLED 需待前端兼容后下线过滤；DANA 代扣在还款页暂不展示。</p>
   */
  private List<DirectDebitAccountResponse> filterLinkedAccountsForRepay(LoanApiViewerContext viewerContext,
      List<DirectDebitAccountResponse> linkedAccountList) {
    if (CollectionUtils.isEmpty(linkedAccountList)) {
      return linkedAccountList;
    }
    Set<Long> disabledAccountIdSet = directDebitAccountService.getDirectDebitLinkAccountVOList(viewerContext.userId,
            viewerContext.sdkType, ImmutableList.of(DirectDebitAccountStatus.DISABLED)).stream()
        .map(account -> account.id)
        .collect(Collectors.toSet());
    return linkedAccountList.stream()
        .filter(account -> !disabledAccountIdSet.contains(account.accountId))
        .filter(this::isRepayVisibleDirectDebitAccount)
        .collect(Collectors.toList());
  }

  /** 还款页可展示的支持银行：排除 DANA 渠道。 */
  private List<DirectDebitBankResponse> filterSupportBanksForRepay(List<DirectDebitBankResponse> supportBankList) {
    if (CollectionUtils.isEmpty(supportBankList)) {
      return supportBankList;
    }
    return supportBankList.stream()
        .filter(this::isRepayVisibleSupportBank)
        .collect(Collectors.toList());
  }

  private boolean isRepayExcludedDirectDebitProvider(DirectDebitProvider provider) {
    return provider != DirectDebitProvider.DANA;
  }

  private boolean isRepayVisibleSupportBank(DirectDebitBankResponse bank) {
    return isRepayExcludedDirectDebitProvider(bank.provider);
  }

  private boolean isRepayVisibleDirectDebitAccount(DirectDebitAccountResponse account) {
    return account.bankBasicInfo == null || isRepayExcludedDirectDebitProvider(account.bankBasicInfo.provider);
  }

  /**
   * 对 supportBankList 中每个银行设置 canMotivation=true，并对第一个银行设置 displayRedDot=true
   */
  private void enrichSupportBankMotivationInfo(Long userId, Long build, DirectDebitAccountListResponse directDebitAccountListResponse) {
    if (directDebitAccountListResponse == null || CollectionUtils.isEmpty(directDebitAccountListResponse.supportBankList)) {
      return;
    }
    // h5全流程屏蔽快捷支付
    if (Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)) {
      log.info("direct reward activity is disabled for whole process, userId is {}", userId);
      return;
    }

    if (!directActivityRewardService.hitExperimentForRepay(userId, build)) {
      return;
    }
    List<DirectDebitBankResponse> supportBankList = directDebitAccountListResponse.supportBankList;
    for (DirectDebitBankResponse bank : supportBankList) {
      bank.canMotivation = true;
    }
    supportBankList.get(0).displayRedDot = true;
  }

  /**
   * 重要重要重要
   * ⚠️⚠️⚠️
   * 本方法和com.miyou.controllers.payment.PaymentReceiptAccountCallbackController#getRepaymentInformation(java.lang.String)
   * 具有业务依赖，BCA渠道会调用该方法获取金额，修改本方法请考虑是否需要同步修改上面的方法
   * ⚠️⚠️⚠️
   *
   * @param request
   * @return
   */
  @PostMapping(path = "/api/v3/cashloan/getRepaymentAccountByChannel")
  public Result getRepaymentAccountByChannelV3(@RequestBody @Validated CashLoanGetRepaymentAccountRequest request) {
    SecureCheckUserContext userContext = userContextFactory.createUserContextForSelfServiceQueryRepaymentPurpose(ecRequest(), request.mobileNumber);
    String resolvedChannel = repaymentAccountConfig.resolveOtherBankChannel(request.channel);
    return EcResponseUtil.generate(
        RepaymentAccountFactory.getRepaymentAccountByChannel(RepaymentAccountSourceType.EC, resolvedChannel, request.couponId,
            request.amount, request.encodeInstalmentIds, request.mobileNumber, request.encodeJBPOrderIds, request.repayStyleVersion, userContext));
  }

  @PostMapping(path = "/api/v3/cashloan/getOVORepaymentAccountByChannel")
  public Result getOVORepaymentAccountByChannel(@RequestBody @Validated CashLoanGetOVORepaymentAccountRequest request) {
    SecureCheckUserContext userContext = userContextFactory.createUserContextForSelfServiceQueryRepaymentPurpose(ecRequest(), request.mobileNumber);
    //TODO(liuzhao,T75413)目前后端没有记录次数，不能完全防止重复发起还款请求，有待后续优化
    if (request.verificationPurpose != null) {
      verificationService.checkMobileVerificationCode(request.mobileNumber, request.verificationCode, request.verificationPurpose, userContext.sdkType);
    }
      return EcResponseUtil.generate(
          RepaymentAccountFactory.getOVORepaymentAccountByChannel(RepaymentAccountSourceType.EC, request.channel, request.couponId,
              request.amount, request.encodeInstalmentIds, request.mobileNumber, request.encodeJBPOrderIds, request.repayStyleVersion, userContext));
  }

  @GetMapping(path = "/api/cashloan/getOVORepaymentInfo")
  @ECSecuredApi
  public Result getOVORepaymentInfo(@RequestParam("amount") BigDecimal amount,
                                    @RequestParam(value = "instalmentIds", required = false) List<String> encodeInstalmentIds,
                                    @RequestParam(value = "encodeJBPOrderIds", required = false) List<String> encodeJBPOrderIds) {
    if (CollectionUtils.isEmpty(encodeInstalmentIds) && CollectionUtils.isEmpty(encodeJBPOrderIds)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("need select order"));
    }
    if (CollectionUtils.isNotEmpty(encodeInstalmentIds)) {
      List<Long> instalmentIds = encodeInstalmentIds.stream().map(YqgHashids::decode).collect(Collectors.toList());
      List<CashLoanInstalmentVO> instalmentVOs = ecOrderService.getInstalmentVOs(instalmentIds);
      CashLoanInstalmentVO instalmentVO = instalmentVOs.stream()
          .min(java.util.Comparator.comparing(CashLoanInstalmentVO::getBillingDate).thenComparing(CashLoanInstalmentVO::getTimeCreated))
          .orElse(null);
      CashLoanOrderVO orderVO = ecOrderService.getOrderVO(Objects.requireNonNull(instalmentVO).orderId);
      OVORepaymentInfoVO ovoRepaymentInfoVO = repaymentAccountService.getUserHistoryOVORepaymentInfo(amount, orderVO.userId, orderVO.id,
          instalmentVO.id);
      return EcResponseUtil.generate(OVORepaymentInfoResponse.from(ovoRepaymentInfoVO));
    } else {
      String jbpOrderId = encodeJBPOrderIds.get(0);
      OVORepaymentInfoVO ovoRepaymentInfoVO = repaymentAccountService.getUserHistoryOVORepaymentInfo(amount, ImpliedContextUtils.userId(),
          YqgHashids.decode(jbpOrderId), -1L);
      return EcResponseUtil.generate(OVORepaymentInfoResponse.from(ovoRepaymentInfoVO));
    }
  }

  @ECSecuredApi
  @PostMapping("/api/cashloan/instalment/repayment/calculate")
  public Result<RepaymentCalculateResponse> repaymentCalculate(@RequestBody @Valid RepaymentCalculateRequest request) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    RepaymentCalculator repaymentCalculator = RepaymentCalculatorFactory.getRepaymentCalculatorByScene(request.scene);
    RepaymentCalculateResponse.RepaymentCalculateResponseBuilder builder = RepaymentCalculateResponse.builder();
    repaymentCalculator.calculateRepaymentInfo(request, viewerContext, builder);
    return EcResponseUtil.generate(builder.build());
  }

  private void trackRepayPageChannelListExposure(Long userId, List<RepaymentAccountResponse> repaymentAccountResponseList,
      RepaymentAccountResponse qrCode) {
    if (CollectionUtils.isNotEmpty(repaymentAccountResponseList)) {
      for (RepaymentAccountResponse accountResponse : repaymentAccountResponseList) {
        repaymentChannelMonitorService.logListExposure(userId, accountResponse.getChannelType(),
            accountResponse.resolveRepaymentPaymentProvider(),
            RepaymentChannelMonitorService.PAGE_SOURCE_REPAY);
      }
    }
    if (qrCode != null) {
      repaymentChannelMonitorService.logListExposure(userId, qrCode.getChannelType(),
          qrCode.resolveRepaymentPaymentProvider(), RepaymentChannelMonitorService.PAGE_SOURCE_REPAY);
    }
  }
}
