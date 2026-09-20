package com.miyou.controllers.cashloan;

import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;
import static com.yqg.core.util.scope.ImpliedContextUtils.sourceType;

import com.miyou.configure.aop.CreateOrderEcExceptionPoint;
import com.miyou.controllers.cashloan.enums.CreateOrderMonitorStep;
import com.miyou.controllers.cashloan.request.CashLoanCreateActivityOrderRequest;
import com.miyou.controllers.cashloan.request.CashLoanCreateOrderRequest;
import com.miyou.controllers.cashloan.request.CashLoanCreateWillingOrderRequest;
import com.miyou.controllers.cashloan.request.CashLoanInitCheckOrderRequest;
import com.miyou.controllers.cashloan.request.ReportJumpLevel2Request;
import com.miyou.controllers.cashloan.request.SubmitFormDataRequest;
import com.miyou.controllers.cashloan.request.SubmitFormDataRequestV2;
import com.miyou.controllers.cashloan.request.TrackWhatsappNumberRequest;
import com.miyou.controllers.cashloan.request.WillingToBorrowRequest;
import com.miyou.controllers.cashloan.response.ActivityInfoForH5Response;
import com.miyou.controllers.cashloan.response.AfterCreateOrderCheckResponse;
import com.miyou.controllers.cashloan.response.BillPageResponse;
import com.miyou.controllers.cashloan.response.CashLoanOrderResponseV2;
import com.miyou.controllers.cashloan.response.CashLoanRepaymentResponseV2;
import com.miyou.controllers.cashloan.response.CreateOrderCheckResponse;
import com.miyou.controllers.cashloan.response.CreateOrderResponse;
import com.miyou.controllers.cashloan.response.ListOrdersResponseV2;
import com.miyou.controllers.cashloan.response.ListRepaymentResponseV2;
import com.miyou.controllers.cashloan.response.CreateOrderSignFormatResponse;
import com.miyou.controllers.cashloan.response.SignFormatResponse;
import com.miyou.controllers.cashloan.response.OrderAgreementResponse;
import com.miyou.controllers.cashloan.response.v5.order.CashLoanActivityOrderResponseV2;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.userpage.AutoJumpPageEnum;
import com.miyou.controllers.loan.BaseLoanController;
import com.miyou.utilities.secureapi.ECSecuredApi;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.loader.LoanAmountCache;
import com.yqg.core.model.loader.LoanAmountCache.LoanAmountScene;
import com.yqg.core.model.loader.LoanAmountLoader;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.activity.ActivityClientService;
import com.yqg.core.service.agreement.AgreementService;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.userflow.domain.loan.service.style.IOrderCheckExpeService;
import com.yqg.core.service.agreement.enums.AgreementType;
import com.yqg.core.service.activity.ActivityClientService.LoanConditionCheckResult;
import com.yqg.core.service.bizcheck.BizCheckConfig;
import com.yqg.core.service.bizcheck.signature.SignatureConfig;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.CashLoanMonitorService;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.cashloan.CreateOrderCheckData;
import com.yqg.core.service.cashloan.LoanUserOrderService;
import com.yqg.core.service.cashloan.OrderAgreementService;
import com.yqg.core.service.cashloan.OrderPreCheckService;
import com.yqg.core.service.cashloan.ordersource.OrderFlowPolicy;
import com.yqg.core.service.cashloan.activity.CashLoanActivityOrderService;
import com.yqg.core.service.cashloan.enums.OrderAgreementSceneType;
import com.yqg.core.service.cashloan.funding.CashLoanFundingService;
import com.yqg.core.service.cashloan.funding.CashLoanFundingVO;
import com.yqg.core.service.cashloan.homepage.AutoJumpBillPageFrequencyLoader;
import com.yqg.core.service.cashloan.homepage.AutoJumpLevel2FrequencyLoader;
import com.yqg.core.service.cashloan.homepage.JumpAuthFrequencyLoader;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanUserSupplementCreateOrderService;
import com.yqg.core.service.cashloan.ordercenter.EcActivityOrderService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.vo.CashLoanActivityOrderVO;
import com.yqg.core.service.cashloan.vo.CashLoanCreateOrderRequestVO;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.OrderAgreementVO;
import com.yqg.core.service.cashloan.vo.RepaymentUnitVO;
import com.yqg.core.service.cashloan.vo.RepaymentVO;
import com.yqg.core.service.directdebit.DirectDebitRepaymentService;
import com.yqg.core.service.directdebit.vo.DirectDebitPaymentVO;
import com.yqg.core.service.jbp.blackcard.BlackCardDemoService;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.loan.repayment.experiment.RepaymentExperimentSupport;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.payment.PaymentCredential;
import com.yqg.core.service.payment.factory.collectioninfo.CollectInformationProcessorFactory;
import com.yqg.core.service.payment.factory.collectioninfo.CollectInformationService;
import com.yqg.core.service.payment.factory.collectioninfo.processors.ICollectInformationProcessor;
import com.yqg.core.service.payment.factory.collectioninfo.processors.enums.CollectInformationScene;
import com.yqg.core.service.payment.factory.collectioninfo.processors.vo.UpdateLoanAddtionalInfoVO;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.restructure.LoanRestructureService;
import com.yqg.core.service.restructure.RestructureApplicationStatus;
import com.yqg.core.service.restructure.vo.LoanRestructureApplicationVO;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.user.OrderBiometricAvailabilityService;
import com.yqg.core.service.user.UserService;
import com.yqg.core.util.OppoUserCheckUtil;
import com.yqg.core.util.env.EnvironmentInfo;
import com.yqg.core.util.env.TerminalInfo;
import com.yqg.core.util.thirdparty.loan.LoanCreditService;
import com.yqg.core.util.thirdparty.loan.vo.TongDunNodeTriggerParam;
import com.yqg.ec.common.enums.LoanCouponUsageType;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.mobile.GlobalMobileNumValidator;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.mc.common.spring.response.appresource.AppResourceResponse;
import com.yqg.translation.client.utils.TT;
import com.yqg.whatopia.client.spring.api.IWhatopiaUserAccountService;
import com.yqg.whatopia.common.mvc.response.AccountCanBindResponse;
import com.yqg.whatopia.common.mvc.response.BaseResponse;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by ember on 16/4/6.
 */
@Slf4j
@RestController
public class CashLoanController extends BaseLoanController {
    @Autowired
    private CashLoanService cashLoanService;
    @Autowired
    private CashLoanConfig cashLoanConfig;
    @Autowired
    private PaymentCredentialResponseService paymentCredentialResponseService;
    @Autowired
    private CashLoanFundingService fundingService;
    @Autowired
    private EcOrderService ecOrderService;
    @Autowired
    private LoanUserCouponService loanUserCouponService;
    @Autowired
    private ProductConfigService productService;
    @Autowired
    private RepaymentAccountConfig repaymentAccountConfig;
    @Autowired
    private SignatureConfig signatureConfig;
    @Autowired
    private OrderPreCheckService orderPreCheckService;
    @Autowired
    private OrderAgreementService orderAgreementService;
    @Autowired
    private CollectInformationService collectInformationService;
    @Autowired
    private LoanBankAccountService loanBankAccountService;
    @Autowired
    private BizCheckConfig bizCheckConfig;
    @Autowired
    private CashLoanActivityOrderService cashLoanActivityOrderService;
    @Autowired
    private EcActivityOrderService activityOrderService;
    @Autowired
    private LoanRestructureService loanRestructureService;
    @Autowired
    private CashLoanMonitorService cashLoanMonitorService;
    @Autowired
    private LoanUserOrderService loanUserOrderService;
    @Autowired
    private LoanCreditService loanCreditService;
    @Autowired
    private DirectDebitRepaymentService directDebitService;
    @Autowired
    private LoanCreditsQuotaService loanCreditsQuotaService;
    @Autowired
    private CashLoanUserSupplementCreateOrderService cashLoanUserSupplementCreateOrderService;
    @Autowired
    private RiskConfig riskConfig;
    @Autowired
    private CollectInformationProcessorFactory collectInformationProcessorFactory;
    @Autowired
    private OppoUserCheckUtil oppoOrderCheck;
    @Autowired
    private BlackCardDemoService blackCardDemoService;
    @Autowired
    private AutoJumpLevel2FrequencyLoader autoJumpLevel2FrequencyLoader;
    @Resource
    private ActivityClientService activityClientService;
    @Autowired
    private JumpAuthFrequencyLoader jumpAuthFrequencyLoader;
    @Autowired
    private IWhatopiaUserAccountService iWhatopiaUserAccountService;
    @Autowired
    private UserService userService;
    @Autowired
    private RepaymentExperimentSupport repaymentExperimentSupport;
    @Autowired
    private LoanAmountLoader loanAmountLoader;
    @Autowired
    private InstalmentCutInterestCouponDeductDetailService instalmentCutInterestCouponDeductDetailService;
    @Autowired
    private AutoJumpBillPageFrequencyLoader autoJumpBillPageFrequencyLoader;
    @Autowired
    private AgreementService agreementService;
    @Autowired
    private IOrderCheckExpeService orderCheckExpeService;
    @Autowired
    private OrderBiometricAvailabilityService orderBiometricAvailabilityService;


    @GetMapping(path = "/api/v2/cashloan/listRepayments")
    @ResponseBody
    @ECSecuredApi
    public Result listRepaymentsV2(@RequestParam("offset") int offset, @RequestParam("limit") int limit) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        ListRepaymentResponseV2 response = new ListRepaymentResponseV2();
        List<RepaymentVO> repaymentVOList = cashLoanService.listDisplayNormalRepayment(viewerContext.loanAccountId, offset, limit);
        if (CollectionUtils.isEmpty(repaymentVOList)) {
            return EcResponseUtil.generate(response);
        }

        List<Long> couponIds = repaymentVOList.stream().map(vo -> vo.couponId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, LoanUserCouponVO> couponVOMap = loanUserCouponService.listByIdsAndUsageType(couponIds, LoanCouponUsageType.MONEY_OFF)
                .stream()
                .collect(Collectors.toMap(r -> r.id, r -> r));
        repaymentVOList.sort((c1, c2) -> Long.compare(c2.timeCreated, c1.timeCreated));

        List<String> transIds = repaymentVOList.stream().map(o -> o.paymentTransId).collect(Collectors.toList());
        List<DirectDebitPaymentVO> directDebitPaymentVOList = directDebitService.findByTransNoList(transIds);
        Map<String, DirectDebitPaymentVO> transNoToDirectDebitPaymentVO = directDebitPaymentVOList.stream().collect(Collectors.toMap(o -> o.transNo, o -> o));

        for (RepaymentVO repaymentVO : repaymentVOList) {
            List<RepaymentUnitVO> repaymentUnits = cashLoanService.getRepaymentUnitsByRepaymentIds(Collections.singletonList(repaymentVO.id))
                    .stream()
                    .sorted(Comparator.comparingLong(RepaymentUnitVO::getId).reversed())
                    .collect(Collectors.toList());

            List<Long> instalmentIds = repaymentUnits
                    .stream()
                    .map(vo -> vo.instalmentId)
                    .collect(Collectors.toList());
            Map<Long, CashLoanInstalmentVO> instalmentVOMap = ecOrderService.getIdToInstalmentVOMap(instalmentIds);

            CashLoanRepaymentResponseV2 singleResponse = CashLoanRepaymentResponseV2.from(repaymentVO, transNoToDirectDebitPaymentVO.get(repaymentVO.paymentTransId), couponVOMap.get(repaymentVO.couponId), repaymentUnits, instalmentVOMap, viewerContext.sdkType);
            response.repayments.add(singleResponse);
        }
        return EcResponseUtil.generate(response);
    }

    private CashLoanCreateOrderRequestVO getCashLoanCreateOrderRequestVOFromRequest(CashLoanCreateOrderRequest request,
                                                                                    EnvironmentInfo environmentInfo, TerminalInfo terminalInfo,
                                                                                    SourceType sourceType, Long build,
                                                                                    Long loanAccountId, SDKType sdkType, LoanUserTypeVO loanUserTypeVO, BigDecimal loanAmount) {
        RemainCreditsVO homepageRemainCredits = loanCreditsQuotaService.getHomepageRemainCredits(loanAccountId);
        CashLoanCreateOrderRequestVO requestVO = CashLoanCreateOrderRequestVO.from(
                homepageRemainCredits.remainingCreditsForVirtual,
                loanAmount,
                request.principal,
                request.days,
                request.paymentMethod,
                request.paymentCredentialId,
                request.verificationCode,
                request.certificateInfo,
                request.productId,
                request.couponId,
                request.scene,
                request.livingVerifyCredentialId,
                request.needCreateActivityOrder,
                request.terms,
                environmentInfo,
                terminalInfo,
                sourceType,
                build,
                loanAccountId,
                sdkType,
                loanUserTypeVO,
                request.loanAllAmount,
                false);
        // TAPD-364294 US1：生物识别凭证串显式赋值，不进 from(...) 参数列表（已 23 参，AGENTS.md §8.2）
        requestVO.biometricCredential = request.biometricCredential;
        return requestVO;
    }

    @PostMapping(path = "/api/v2/cashloan/createOrder")
    @ResponseBody
    @ECSecuredApi(invalidForFrozenUser = true)
    @CreateOrderEcExceptionPoint(value = CreateOrderMonitorStep.CREATE_ORDER)
    public Result createOrderV2(@RequestBody @Valid CashLoanCreateOrderRequest request) {
        LoanApiViewerContext viewerContextFromRequest = getViewerContextFromRequest();
        // 顺路上报设备生物识别可用性（TAPD-364294 US3-1）：放在 Controller 方法体内、不侵入
        // checkCanCreateOrder（plan Module 4 决策）。上报内部静默降级，不影响下单主流程。
        orderBiometricAvailabilityService.reportAvailabilityIfPresent(viewerContextFromRequest.userId,
            viewerContextFromRequest.deviceToken, request.deviceBiometricAvailable,
            viewerContextFromRequest.environmentInfo.platformType,
            viewerContextFromRequest.environmentInfo.operationSysVersion);
        oppoOrderCheck.oppoOrderCheck(viewerContextFromRequest.sourceType, viewerContextFromRequest.channel, viewerContextFromRequest.userId, viewerContextFromRequest.sdkType);
        BigDecimal loanAmount = loanUserOrderService.validateCreateOrderAmountData(viewerContextFromRequest, request.getEffectiveAmount());
        try {
            EcAsserts.assertTrue(request.productId != null, "product id can not be null");
            LoanApiViewerContext viewerContext = getViewerContextFromRequest();
            Long blackCardCouponId = blackCardDemoService.createBlackCardOrderAndGrantCoupon(viewerContext.userId, viewerContextFromRequest.sdkType, request.selectedBlackCardProductId, request.blackCardAmount);
            request.updateCouponId(blackCardCouponId);
            return checkAndCreateOrder(getCashLoanCreateOrderRequestVOFromRequest(request, viewerContext.environmentInfo,
                viewerContext.terminalInfo, viewerContext.sourceType, viewerContext.build,
                viewerContext.loanAccountId, viewerContext.sdkType, viewerContext.loanUserTypeVO, loanAmount));
        } catch (EcException e) {
            LoanApiViewerContext viewerContext = getViewerContextFromRequest();
            cashLoanMonitorService.logCreateOrder(viewerContext.sdkType, viewerContext.sourceType, viewerContext.loanAccountId, viewerContext.build, loanAmount, request.days, e.exceptionType);
            throw e;
        }
    }

    @PostMapping("/api/cashloan/creditsDecreaseQuickOrder")
    @ResponseBody
    @ECSecuredApi(invalidForFrozenUser = true)
    @CreateOrderEcExceptionPoint(value = CreateOrderMonitorStep.CREATE_DECREASE_QUICK_ORDER)
    public Result creditsDecreaseQuickOrder(@RequestBody @Valid CashLoanCreateOrderRequest request) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        oppoOrderCheck.oppoOrderCheck(viewerContext.sourceType, viewerContext.channel, viewerContext.userId, viewerContext.sdkType);
        try {
            EcAsserts.assertTrue(request.productId != null, "product id can not be null");
            return checkAndCreateOrder(getCashLoanCreateOrderRequestVOFromRequest(request, viewerContext.environmentInfo,
                    viewerContext.terminalInfo, viewerContext.sourceType, viewerContext.build,
                    viewerContext.loanAccountId, viewerContext.sdkType, viewerContext.loanUserTypeVO, request.getEffectiveAmount()));
        } catch (EcException e) {
            cashLoanMonitorService.logCreateOrder(viewerContext.sdkType, viewerContext.sourceType, viewerContext.loanAccountId, viewerContext.build, request.getEffectiveAmount(), request.days, e.exceptionType);
            throw e;
        }
    }

    @Deprecated
    @PostMapping("/api/cashloan/createWillingnessOrder")
    @ECSecuredApi(invalidForFrozenUser = true)
    public Result createWillingnessOrder(@RequestBody @Valid CashLoanCreateWillingOrderRequest request) {
        return EcResponseUtil.generateSuccess();
    }


    @GetMapping("/api/cashloan/showActivityInfo")
    @ResponseBody
    @ECSecuredApi(invalidForFrozenUser = true)
    public Result showActivityInfo() {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        CashLoanActivityOrderVO unDoneActivityOrder = cashLoanActivityOrderService.findUnDoneActivityOrderByAccountId(viewerContext.loanAccountId);
        boolean existUndoneActivityOrder = false;
        TT claimButtonContent = TT.gen(cashLoanConfig.getClaimButtonDefaultContent());
        if (Objects.nonNull(unDoneActivityOrder)) {
            existUndoneActivityOrder = true;
            claimButtonContent = TT.gen(cashLoanConfig.getClaimButtonContent(), AmountFormatter.format(unDoneActivityOrder.currency, unDoneActivityOrder.principal));
        }
        return EcResponseUtil.generate(ActivityInfoForH5Response.from(existUndoneActivityOrder, claimButtonContent));
    }

    @PostMapping(path = "/api/cashloan/createActivityOrder")
    @ResponseBody
    @ECSecuredApi(invalidForFrozenUser = true)
    public Result createActivityOrder(@RequestBody @Valid CashLoanCreateActivityOrderRequest request) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        if (viewerContext.sdkType == null) {
            throw EcException.error("sdk is null");
        }

        loanBankAccountService.checkPaymentCredential(request.paymentMethod, request.paymentCredentialId,
                viewerContext.sdkType, viewerContext.loanAccountId, viewerContext.sourceType);

        //检查是否符合策略
        cashLoanActivityOrderService.checkStrategyIsRight(viewerContext.loanAccountId, viewerContext.userId);
        Long paymentCredentialId = YqgHashids.decode(request.paymentCredentialId);
        try {
            cashLoanActivityOrderService.createCashLoanActivityOrder(
                    viewerContext.loanAccountId,
                    new PaymentCredential(request.paymentMethod, paymentCredentialId),
                    request.principal,
                    viewerContext.sdkType,
                    StringUtils.isNotBlank(request.productId) ? YqgHashids.decode(request.productId) : null,
                    viewerContext.sourceType
            );
            return EcResponseUtil.generateSuccess();
        } catch (EcException e) {
            if (e.exceptionType == EcExceptionType.LOAN_ACCOUNT_UNEXPECTED_CREDITS_STATUS) {
                log.warn("loan account unexpected credits status!", e);
                throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("网络异常，请稍后再试"));
            } else {
                throw e;
            }
        }
    }

    @ECSecuredApi
    @ResponseBody
    @GetMapping(path = "/api/cashloan/getActivityOrder")
    public Result getActivityOrder() {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();

        List<CashLoanActivityOrderVO> orderRecords = activityOrderService.findByAccountIdAndStatus(viewerContext.loanAccountId, CashLoanOrderStatus.UNDONE_STATUSES_WITHOUT_READY);
        if (CollectionUtils.isEmpty(orderRecords)) {
            return EcResponseUtil.generate(new CashLoanActivityOrderResponseV2());
        }
        CashLoanActivityOrderVO record = orderRecords.get(0);
        // 银行卡信息
        Object credential = paymentCredentialResponseService.convertFromBankCardCredential(record.paymentCredential);
        return EcResponseUtil.generate(CashLoanActivityOrderResponseV2.from(record, credential));
    }

    @PostMapping(path = "/api/cashloan/initCheckOrder")
    @ResponseBody
    @ECSecuredApi(invalidForFrozenUser = true)
    public Result initCheckOrder(@RequestBody @Valid CashLoanInitCheckOrderRequest request) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        Long orderId = bizCheckConfig.getBusinessId(request.businessType, request.businessId, viewerContext.build);
        CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
        EcAsserts.assertTrue(orderVO.userId.equals(viewerContext.userId), "order userId : {}, apply userId : {}", orderVO.userId, viewerContext.userId);
        if (CashLoanOrderStatus.INIT == orderVO.status || CashLoanOrderStatus.READY == orderVO.status) {
            return EcResponseUtil.generateSuccess();
        }
        if (CashLoanOrderStatus.CHECK != orderVO.status) {
            throw EcException.error("unexpected order status when init check order, id: {}, status: {}", orderVO.id, orderVO.status);
        }
        if (!cashLoanService.bizCheckSuccess(orderVO.id, orderVO.accountId)) {
            throw EcException.error("biz check status not valid when init check order, id: {}", orderVO.id);
        }
        cashLoanService.initCheckedOrder(orderVO.accountId, orderVO.id);
        return EcResponseUtil.generateSuccess();
    }

    private Result checkAndCreateOrder(CashLoanCreateOrderRequestVO request) {
        try {
            LoanApiViewerContext viewerContext = getViewerContextFromRequest();
            if (request.scene != null
                    && riskConfig.getQuickOrderToastSwitch().contains(request.scene.name())
                    && cashLoanUserSupplementCreateOrderService.existSupplementInfoInProcess(viewerContext.userId)) {
                throw EcException.warn(EcExceptionType.LOAN_QUICK_ORDER_IN_PROCESS_ERROR, TT.gen("请返回主页并再次尝试申请。"));
            }
            CreateOrderResponse response = getCheckAndCreateOrderResponse(request);
            response.order.id = null;
            return EcResponseUtil.generate(response);
        } catch (EcException e) {
            if (e.exceptionType == EcExceptionType.LOAN_ACCOUNT_UNEXPECTED_CREDITS_STATUS) {
                log.warn("loan account unexpected credits status!", e);
                throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("网络异常，请稍后再试"));
            } else {
                throw e;
            }
        }
    }

    @NotNull
    private CreateOrderResponse getCheckAndCreateOrderResponse(CashLoanCreateOrderRequestVO request) {

        CashLoanOrderVO vo = loanUserOrderService.checkAndCreateOrder(request, true);
        Object credential = paymentCredentialResponseService.convertFromBankCardCredential(vo.paymentCredential);

        loanCreditService.triggerCreateOrder(TongDunNodeTriggerParam.ofCreateOrder(
            request.sdkType, request.build, request.sourceType, request.loanAccountId, request.environmentInfo));
        return CreateOrderResponse.from(vo, credential);
    }

    @GetMapping(path = "/api/v2/cashloan/listOrders")
    @ResponseBody
    @ECSecuredApi
    public Result listOrdersV2(Integer offset, Integer limit) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        ListOrdersResponseV2 response = new ListOrdersResponseV2();
        List<OrderInstalment> orderInstalments = ecOrderService.listOrderInstalment(viewerContext.loanAccountId, offset, limit, CashLoanOrderStatus.PAYOUT_STATUSES);
        if (CollectionUtils.isEmpty(orderInstalments)) {
            return EcResponseUtil.generate(response);
        }
        orderInstalments = instalmentCutInterestCouponDeductDetailService.getViewOrderInstalments(orderInstalments);

        List<String> mhtOrderNo = orderInstalments.stream()
                .map(oi -> oi.orderVO.mhtOrderNo)
                .filter(StringUtils::isNoneBlank)
                .collect(Collectors.toList());
        Map<String, CashLoanFundingVO> fundingVOMap = fundingService.getFundingVOMap(mhtOrderNo);
        List<Long> orderIds = orderInstalments.stream()
                .map(o -> o.orderVO.id)
                .collect(Collectors.toList());

        Map<Long, LoanRestructureApplicationVO> orderIdToLoanApplicationVOMap = loanRestructureService.findByPostOrderIdAndStatus(orderIds, RestructureApplicationStatus.COMPLETE)
                .stream()
                .collect(Collectors.toMap(LoanRestructureApplicationVO::getPostOrderId, vo -> vo));

        Map<Long, Long> agreementVOMap = agreementService.getOnlyOneAgreementByOrderIds(orderIds, AgreementType.DEBT_TRANSFER_EXPLAIN, viewerContext.userId, viewerContext.sdkType);
        for (OrderInstalment orderInstalment : orderInstalments) {
            CashLoanOrderVO orderVO = orderInstalment.orderVO;
            Object credential = paymentCredentialResponseService.convertFromBankCardCredential(orderVO.paymentCredential);
            CashLoanFundingVO fundingVO = fundingVOMap.get(orderVO.mhtOrderNo);
            BigDecimal deductInterestAmount = cashLoanService.getCutInterestAmount(orderVO.id);
            // 减免金额
            BigDecimal initDeductAmount = instalmentCutInterestCouponDeductDetailService.calcInitCutAmountByOrderId(orderVO.id);
            LoanRestructureApplicationVO loanRestructureApplicationVO = orderIdToLoanApplicationVOMap.get(orderVO.id);

          CashLoanOrderResponseV2 singleResponse = CashLoanOrderResponseV2.from(orderVO, fundingVO, credential,
              productService.getProductVO(orderVO.productId), loanRestructureApplicationVO, deductInterestAmount, initDeductAmount,
              cashLoanConfig.agreementUrl(viewerContext.sdkType), signatureConfig.getSignatureWebPdfAgreementUrl(viewerContext.sdkType),
              getDebtAgreementFileUrl(viewerContext.userId, agreementVOMap.get(orderVO.id)));
            response.orders.add(singleResponse);
        }
        return EcResponseUtil.generate(response);
    }

    // 需要拼接为webview/agreement-pdf?id=1931345&isfromMail=true
    public String getDebtAgreementFileUrl(Long userId, Long agreementFileId) {
      if (!cashLoanConfig.useSecuredDebtTransferUrl(userId)) {
        return null;
      }
      if (agreementFileId == null) {
        return null;
      }
      return String.format(signatureConfig.getDebtAgreementFileUrl(), agreementFileId);
    }


    //This switch controls whether a user can continue without providing Env Info
    //True - user can't continue without location  False - user can continue without location
    @GetMapping(path = "/api/cashloan/blockWithoutLocation")
    @ResponseBody
    public Result blockWithoutLocation() {
        return EcResponseUtil.generate(cashLoanConfig.blockWithoutLocation());
    }


    /**
     * 把意愿页分模块
     * part-1.头部提示
     * part-2.额度填写模块
     * part-3.分期模块
     * part-4.还款计划模块
     * part-5.尾部提示
     */
    @Deprecated
    @PostMapping(path = "/api/cashloan/willingnessToBorrow")
    @ECSecuredApi(invalidForFrozenUser = true)
    public Result willingnessToBorrow(@RequestBody @Valid WillingToBorrowRequest request) {
        return EcResponseUtil.generateSuccess();
    }


    @GetMapping(path = "/api/cashloan/createOrderCheck")
    @ECSecuredApi(invalidForFrozenUser = true)
    @CreateOrderEcExceptionPoint(value = CreateOrderMonitorStep.CREATE_ORDER_CHECK)
    public Result createOrderCheck(
            @RequestParam(value = "paymentMethod", required = false) PaymentMethod paymentMethod,
            @RequestParam(value = "paymentCredentialId", required = false) String hashPaymentCredentialId,
            @RequestParam(value = "loanAmount", required = false) BigDecimal loanAmount,
            @RequestParam(value = "selectedProductId", required = false) String selectedProductId,
            @RequestParam(value = "feeDeductAmount", required = false) BigDecimal feeDeductAmount,
            @RequestParam(value = "couponId", required = false) Long couponId,
            @RequestParam(value = "ignoreOrderActivity", required = false) Boolean ignoreOrderActivity,
            @RequestParam(value = "hasInteracted", required = false) Boolean hasInteracted,
            @RequestParam(value = "stayDurationMs", required = false) Long stayDurationMs,
            @RequestParam(value = "orderSource", required = false) String orderSource,
            @RequestParam(value = "deviceBiometricAvailable", required = false) Boolean deviceBiometricAvailable
    ) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();

        // 顺路上报设备生物识别可用性（TAPD-364294 US3-1）：必须在 performCreateOrderChecks 之前，
        // 才能让本次请求内的 needBiometricVerify 判定读到刚上报的可用性（plan Module 5 决策）。
        // 上报内部静默降级，不影响下单检查主流程。
        orderBiometricAvailabilityService.reportAvailabilityIfPresent(viewerContext.userId, viewerContext.deviceToken,
            deviceBiometricAvailable, viewerContext.environmentInfo.platformType,
            viewerContext.environmentInfo.operationSysVersion);

        CreateOrderCheckData checkData = orderPreCheckService.performCreateOrderChecks(viewerContext, paymentMethod, hashPaymentCredentialId, loanAmount, selectedProductId, couponId);

        // 按下单来源埋点归因（TAPD-367267 US3-4/D17）：本期仅埋点、不落库，orderSource 缺省/非法均归一为 NORMAL
        cashLoanMonitorService.logOrderCheckBySource(orderSource);

        AppResourceResponse appResourceResponse = null;
        // 免营销弹窗决策归并点（KDD-10/M9）：按来源策略（新增）与既有「跳过营销资源位」判定为 OR 短路，
        // 任一命中即不下发下单页营销弹窗；orderSource 缺省/非法时 OrderFlowPolicy.of 返回全 false，行为与改造前一致。
        if (OrderFlowPolicy.of(orderSource).suppressMarketingPopup()
            || Optional.ofNullable(sourceType()).map(SourceType::isBlockingMarketingResource).orElse(false)
            || Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)) {
            CreateOrderCheckResponse response = CreateOrderCheckResponse.from(cashLoanConfig.getCreateOrderWithSignSmsContentInApp(),
                checkData.needShowContract, checkData.createOrderWithoutOtp, checkData.allowSkipOtp, checkData.createOrderPopupType, checkData.orderLimitPopupInfo, checkData.needSupplementProcess, appResourceResponse, checkData.orderAmountSplitResponse, checkData.autoBackfillOtp, checkData.needBiometricVerify, checkData.bioAuthProcessText);
            assembleCreateOrderCheckExpeResult(response, viewerContext.userId, viewerContext.build);
            return EcResponseUtil.generate(response);
        }
        try {
          feeDeductAmount = Optional.ofNullable(feeDeductAmount).orElse(BigDecimal.ZERO);
          LoanConditionCheckResult expResult = activityClientService.validateLoanApplicationConditions(viewerContext,
                checkData.realLoanAmount, checkData.decodedSelectProductId, Boolean.TRUE.equals(ignoreOrderActivity));
          // 透传放款账号 credentialId（TAPD-1353291）：供 OrderPreCheckService Phase 2 取脱敏放款账号字段；
          // 接口入参为 hash 字符串，此处解码为内部 Long；为空时透传 null，下游兜底返回 null
          Long decodedPaymentCredentialId = StringUtils.isBlank(hashPaymentCredentialId)
              ? null : YqgHashids.decode(hashPaymentCredentialId);
          appResourceResponse = orderPreCheckService.fetchResourceInfo(viewerContext, feeDeductAmount, checkData.realLoanAmount,
              selectedProductId, expResult, couponId, decodedPaymentCredentialId, hasInteracted, stayDurationMs);
        } catch (Exception e) {
            log.error("appResourceResponse get error", e);
        }

        loanAmountLoader.set(viewerContext.userId, LoanAmountCache.of(checkData.realLoanAmount, LoanAmountScene.PRE_CHECK));
        CreateOrderCheckResponse response = CreateOrderCheckResponse.from(cashLoanConfig.getCreateOrderWithSignSmsContentInApp(),
                checkData.needShowContract, checkData.createOrderWithoutOtp, checkData.allowSkipOtp, checkData.createOrderPopupType, checkData.orderLimitPopupInfo, checkData.needSupplementProcess, appResourceResponse, checkData.orderAmountSplitResponse, checkData.autoBackfillOtp, checkData.needBiometricVerify, checkData.bioAuthProcessText);
        assembleCreateOrderCheckExpeResult(response, viewerContext.userId, viewerContext.build);
        return EcResponseUtil.generate(response);
    }

    private void assembleCreateOrderCheckExpeResult(CreateOrderCheckResponse response, Long userId, Long build) {
        try {
            String key = UserFlowExperimentEnum.RELOAN_PROTOCOL_CONFIRM_FULL_SCREEN.getKey();
            String result = orderCheckExpeService.abTestReloanProtocolFullScreenExpe(userId, build, key);
            response.getExpeResultMap().put(key, result);
        } catch (Exception e) {
            log.error("assembleCreateOrderCheckExpeResult error, userId={}", userId, e);
        }
    }

    @Deprecated
    /**
     * @param com.miyou.controllers.cashloan.response.CreateOrderCheckResponseV2
     * @return
     * @responseClass com.yqg.ec.common.spring.response.EcResponse<com.miyou.controllers.cashloan.response.CreateOrderCheckResponseV2>
     * @desc 下单校验的V2版本
     */
    @GetMapping(path = "/api/v2/cashloan/createOrderCheck")
    @ECSecuredApi(invalidForFrozenUser = true)
    @CreateOrderEcExceptionPoint(value = CreateOrderMonitorStep.CREATE_ORDER_CHECK)
    public Result createOrderCheckV2(
            @RequestParam(value = "paymentMethod", required = false) PaymentMethod paymentMethod,
            @RequestParam(value = "paymentCredentialId", required = false) String hashPaymentCredentialId,
            @RequestParam(value = "loanAmount", required = false) BigDecimal loanAmount,
            @RequestParam(value = "selectedProductId", required = false) String selectedProductId
    ) {
        throw EcException.error("api/v2/cashloan/createOrderCheck is deprecated");
    }

    /**
     * 下单成功后进行检查
     *
     * @return
     */
    @PostMapping(path = "/api/cashloan/afterCreateOrderCheck")
    @ECSecuredApi(invalidForFrozenUser = true)
    public Result<AfterCreateOrderCheckResponse> afterCreateOrderCheck() {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        Boolean showUserImmediateContact = collectInformationService.showUserImmediateContact(viewerContext.loanAccountId, viewerContext.userId, viewerContext.build, CollectInformationScene.AFTER_CREATE_ORDER);
        return EcResponseUtil.generate(AfterCreateOrderCheckResponse.from(showUserImmediateContact));
    }


    @PostMapping(path = "/api/cashloan/submitFormData")
    @ResponseBody
    @ECSecuredApi
    public Result<Boolean> submitFormData(@RequestBody SubmitFormDataRequest submitFormDataRequest) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        ICollectInformationProcessor collectInformationProcessor = collectInformationProcessorFactory.getCollectInformationProcessorByTypeOrThrow(submitFormDataRequest.getCollectInformationTypeEnum());
        Boolean result = collectInformationProcessor.updateInformation(UpdateLoanAddtionalInfoVO.from(viewerContext.loanAccountId, submitFormDataRequest.getSubmitData(), viewerContext.sourceType, viewerContext.sdkType, viewerContext.name));
        if (!result) {
            throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("请输入正确信息"));
        }
        return EcResponseUtil.generateSuccess();
    }

    @PostMapping(path = "/api/cashloan/submitFormDataV2")
    @ResponseBody
    @ECSecuredApi
    public Result<Boolean> submitFormData(@RequestBody SubmitFormDataRequestV2 submitFormDataRequest) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        submitFormDataRequest.data.forEach(data -> {
            ICollectInformationProcessor collectInformationProcessor = collectInformationProcessorFactory.getCollectInformationProcessorByTypeOrThrow(data.type);
            UpdateLoanAddtionalInfoVO vo = UpdateLoanAddtionalInfoVO.from(viewerContext.loanAccountId, data.type,
                    data.data, viewerContext.sourceType, data.verificationCode, data.verificationPurposeType, viewerContext.sdkType, viewerContext.name);
            Boolean result = collectInformationProcessor.updateInformation(vo);
            if (!result) {
                throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("请输入正确信息"));
            }
        });
        return EcResponseUtil.generateSuccess();
    }

    @PostMapping(path = "/api/cashloan/trackWhatsappNumber")
    @ResponseBody
    @ECSecuredApi
    public Result<Boolean> trackWhatsappNumber(@RequestBody TrackWhatsappNumberRequest request) {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        Long userId = userService.fetchIdByNormalizedMobile(request.whatsapp);
        if (Objects.nonNull(userId)) {
            if (Objects.equals(userId, viewerContext.userId)) {
                throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("请勿填写当前登录号码"));
            } else {
                throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("该号码已注册账号"));
            }
        }

        String normalizedMobileNumber = userService.getNormalizedMobileNumberByUserId(viewerContext.userId);
        String nationalMobileNumber = MobileConverter.normalizedToNationalOrThrow(viewerContext.sdkType.getLocale(), normalizedMobileNumber);
        String whatsappNumber = MobileConverter.normalizedToNationalOrThrow(viewerContext.sdkType.getLocale(), request.whatsapp);
        GlobalMobileNumValidator.throwWhenNotValid(viewerContext.sdkType.getLocale(), whatsappNumber);
        BaseResponse<AccountCanBindResponse> response = iWhatopiaUserAccountService.checkCanBindAccount(nationalMobileNumber, whatsappNumber);
        if (!response.body.canBind) {
            switch (response.body.reason) {
                case ALREADY_BOUND:
                    throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("该号码已注册账号"));
                case SAME_AS_LOGIN:
                    throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("请勿填写曾经使用的号码"));
            }
        }

        return EcResponseUtil.generateSuccess();
    }

    /**
     * 上报自动跳转的行为
     *
     * @return 成功状态的结果
     * @throws EcException 如果发生错误
     */
    @PostMapping(path = "/api/cashloan/reportAutoJumpLevel2")
    @ResponseBody
    @ECSecuredApi
    public Result reportAutoJumpLevel2(ReportJumpLevel2Request request) throws EcException {

        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        Long userId = viewerContext.userId;
      Long appCurrentOpenTime = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)
          ? AutoJumpLevel2FrequencyLoader.WHOLE_PROCESS_OPEN_TIME
          : viewerContext.appCurrentOpenTime;
      AutoJumpPageEnum jumpToPage = request.getPage();
        switch (jumpToPage) {
            case AUTHENTICATION:
                jumpAuthFrequencyLoader.recordJump(userId);
                break;
          case BILL_PAGE:
            autoJumpBillPageFrequencyLoader.recordJump(userId, appCurrentOpenTime);
                break;
            case ORDER:
            default:
              // 默认自动跳转二级下单页使用
              autoJumpLevel2FrequencyLoader.recordJump(userId, appCurrentOpenTime);
        }

        return EcResponseUtil.generateSuccess();
    }

    @GetMapping(path = "/api/cashloan/fetchBillPageH5Result")
    @ECSecuredApi
    public Result<BillPageResponse> fetchBillPageH5Result() {
        LoanApiViewerContext viewerContext = getViewerContextFromRequest();
        CommonABTestResultGroup result = repaymentExperimentSupport.getBillPageStrategy(viewerContext.userId, viewerContext.build);
        String url = CommonABTestResultGroup.B == result ? repaymentAccountConfig.getBillPageToH5ExpUrl() : "";
        return EcResponseUtil.generate(BillPageResponse.from(result.name(), url));
    }

  @GetMapping(path = "/api/cashloan/fetchOrderAgreementExp")
  @ResponseBody
  @ECSecuredApi
  public Result fetchOrderAgreementExp(
      @RequestParam(value = "couponId", required = false) Long couponId,
      @RequestParam(value = "loanAmount") BigDecimal loanAmount,
      @RequestParam(value = "selectedProductId") String selectedProductId,
      @RequestParam(value = "sceneType", required = false) OrderAgreementSceneType sceneType) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    OrderAgreementVO orderAgreementVO = orderAgreementService.fetchOrderAgreementExp(viewerContext, couponId, loanAmount,
        selectedProductId);
    return EcResponseUtil.generate(buildOrderAgreementResponse(orderAgreementVO));
  }

  /**
   * 将 OrderAgreementVO 转换为 OrderAgreementResponse；无论 AB 分流结果如何，均附带 signFormat 字段。
   *
   * @param vo 来自 OrderAgreementService 的协议 VO，signFormat 字段已由 MonthlyIncomeSignFormatService 填充
   * @return 含协议场景、金额及月收入声明格式的响应体
   */
  private OrderAgreementResponse buildOrderAgreementResponse(OrderAgreementVO vo) {
    OrderAgreementResponse response;
    if (vo.getFormattedLoanAmount() == null) {
      response = OrderAgreementResponse.from(vo.getExperimentResult());
    } else {
      response = OrderAgreementResponse.from(vo.getExperimentResult(), vo.getFormattedLoanAmount(),
          vo.getProductTimeContent(), vo.getFormattedFirstRepayAmount());
    }
    if (vo.getMonthlyIncomeSignFormat() != null) {
      SignFormatResponse signFormatResponse = new SignFormatResponse();
      signFormatResponse.orderPageSignFormat = CreateOrderSignFormatResponse.from(
          vo.getMonthlyIncomeSignFormat().orderPageSignFormat);
      signFormatResponse.dialogPageSignFormat = CreateOrderSignFormatResponse.from(
          vo.getMonthlyIncomeSignFormat().dialogPageSignFormat);
      response.setSignFormatResponse(signFormatResponse);
    }
    return response;
  }

}
