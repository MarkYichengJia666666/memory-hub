package com.yqg.core.service.cashloan;

import com.google.common.collect.Maps;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.common.util.type.BooleanType;
import com.yqg.core.aop.RunInTransaction;
import com.yqg.core.configure.EcExecutorConfig;
import com.yqg.core.model.core.AdminUserIDThreadLocal;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.*;
import com.yqg.core.model.loader.CashLoanPayoutTimesLoader;
import com.yqg.core.model.mongo.MongoLoanOrderInfoModel;
import com.yqg.core.model.mongo.VO.MongoLoanAccountExtraInfoPojo;
import com.yqg.core.model.sql.adminuser.AdminUserModel;
import com.yqg.core.model.sql.cashloan.*;
import com.yqg.core.model.sql.cashloan.enums.*;
import com.yqg.core.model.sql.cashloan.funding.CashLoanFundingModel;
import com.yqg.core.model.sql.loan.account.LoanAccountAdditionalInfoModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.LoanUserCreditsInfoModel;
import com.yqg.core.model.sql.loan.account.enums.*;
import com.yqg.core.model.sql.loan.manualreduction.ManualReductionTaskModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.lottery.enums.LoanLotteryWinningType;
import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.model.sql.payment.enums.PaymentStatus;
import com.yqg.core.model.sql.payment.enums.PaymentTransType;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.model.sql.thirdparty.enums.ReasonCode;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.agreement.enums.AgreementType;
import com.yqg.core.service.agreement.platform.AgreementPlatformService;
import com.yqg.core.service.bizcheck.BizCheckListService;
import com.yqg.core.service.bizcheck.BizCheckTool;
import com.yqg.core.service.bizcheck.enums.BizCheckEventType;
import com.yqg.core.service.bizcheck.fund.FundOrderService;
import com.yqg.core.service.bizcheck.fund.FundTradingMethod;
import com.yqg.core.service.bizcheck.fund.config.FundConfig;
import com.yqg.core.service.bizcheck.fund.enums.FundProvider;
import com.yqg.core.service.bizcheck.fund.vo.FundOrderVO;
import com.yqg.core.service.bizcheck.signature.ElecSignatureService;
import com.yqg.core.service.bizcheck.signature.HandWrittenSignatureService;
import com.yqg.core.service.cashloan.activity.CashLoanActivityOrderService;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.CashLoanRepayStrategyService;
import com.yqg.core.service.cashloan.creditgain.CreditGainBeforeRepaySnapshotService;
import com.yqg.core.service.cashloan.enums.CashLoanCalcCreditsStatus;
import com.yqg.core.service.cashloan.enums.CreateOrderScene;
import com.yqg.core.service.cashloan.enums.ReductionScene;
import com.yqg.core.service.cashloan.fee.plan.OIPlanFactory;
import com.yqg.core.service.cashloan.fee.plan.OIPlanGenerator;
import com.yqg.core.service.cashloan.funding.*;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.instalmentcutcoupon.vo.InstalmentCutInterestCouponDeductDetailVO;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.manualdeduction.reduction.ReductionFactory;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanOrderAdditionalInfoService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.CashLoanOrderAdditionalInfoVO;
import com.yqg.core.service.cashloan.ordercenter.vo.LoanActivityOrderAdditionalInfo;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalmentPlan;
import com.yqg.core.service.cashloan.repay.CashLoanRepaymentService;
import com.yqg.core.service.cashloan.repay.repaystrategy.CashLoanRepayContext;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentVO;
import com.yqg.core.service.cashloan.risk.RiskRejectOrderService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.util.rate.CalcFeeUtil;
import com.yqg.core.service.cashloan.vo.*;
import com.yqg.core.service.cashloan.vo.enums.OrderRejectDealScene;
import com.yqg.core.service.creditsdecreasequickorderinfo.CreditsDecreaseQuickOrderInfoService;
import com.yqg.core.service.creditsdecreasequickorderinfo.enums.CreditsDecreaseQuickOrderStatus;
import com.yqg.core.service.creditsdecreasequickorderinfo.vo.CreditsDecreaseQuickOrderInfoVO;
import com.yqg.core.service.fsa.FsaDebtService;
import com.yqg.core.service.insurance.InsuranceProcessEventService;
import com.yqg.core.service.insurance.config.CreditInsuranceConfig;
import com.yqg.ec.common.enums.insurance.InsuranceBusinessStatus;
import com.yqg.ec.common.enums.insurance.InsuranceBusinessType;
import com.yqg.core.service.jbp.blackcard.BlackCardDemoService;
import com.yqg.core.service.jbp.vo.BlackCardOrderVO;
import com.yqg.core.service.loan.LoanAssertion;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.LoanUserTypeService;
import com.yqg.core.service.loan.activity.LoanActivityService;
import com.yqg.core.service.loan.activity.LoanLotteryService;
import com.yqg.core.service.loan.activity.enums.LoanLotteryChanceActionType;
import com.yqg.core.service.loan.activity.enums.LoanLotteryChanceType;
import com.yqg.core.service.loan.additioanalinfo.LoanUserAdditionalInfoService;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.moneyoffcoupon.CouponServiceManager;
import com.yqg.core.service.loan.coupon.moneyoffcoupon.IMoneyOffCouponService;
import com.yqg.core.service.loan.coupon.moneyoffcoupon.IRepayDeductCouponService;
import com.yqg.core.service.loan.coupon.parameter.CalcRepayAmountParam;
import com.yqg.core.service.loan.coupon.vo.CutInterestInstalmentVO;
import com.yqg.core.service.loan.coupon.vo.CutInterestVO;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.loan.coupon.vos.LoanCutInterestCouponVO;
import com.yqg.core.service.loan.coupon.vos.LoanMoneyOffCouponVO;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.creditsdetails.CreditChangedEventType;
import com.yqg.core.service.loan.creditsdetails.handler.infra.CreditChangedEventDispatchContext;
import com.yqg.core.service.loan.creditsdetails.handler.infra.CreditChangedEventDispatcher;
import com.yqg.core.service.loan.extrainfo.LoanAccountExtraInfoService;
import com.yqg.core.service.loan.infos.LoanCreateOrderInfoPojo;
import com.yqg.core.service.loan.infos.ids.IndonesiaBaseIdInfo;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.loan.vo.UserInfoVO;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.core.service.ly.LyAdminService;
import com.yqg.core.service.ly.LyConfig;
import com.yqg.core.service.notification.NotificationService;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.notification.param.system.*;
import com.yqg.core.service.operationlog.OperationLogService;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.PaymentCredential;
import com.yqg.core.service.payment.TriggerPayEventMsgUtil;
import com.yqg.core.service.payment.UserPaymentCredential;
import com.yqg.core.service.payment.monitor.PaymentMonitor;
import com.yqg.core.service.payment.pm.pmenum.PaymentErrorCode;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.service.payment.vo.PaymentProcessResult;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.thirdparty.ThirdPartyEventService;
import com.yqg.core.service.sdktype.IdnLoanSDKTypeConstants;
import com.yqg.core.service.tool.IdGeneratorService;
import com.yqg.core.service.vida.vo.VidaCreateParam;
import com.yqg.core.util.actiontimes.DailyUserActionTimesLoader;
import com.yqg.core.util.actiontimes.MonthlyUserActionTimesLoader;
import com.yqg.core.util.actiontimes.UserActionEnum;
import com.yqg.ec.common.enums.LoanAccountAdditionalTypeEnum;
import com.yqg.ec.common.enums.LoanCouponUsageType;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import com.yqg.ec.common.enums.order.CashLoanOrderRejectReason;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.order.LoanUserTypeTag;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.yqg.core.model.sql.cashloan.enums.OrderAdditionalInfoType.REJECT_REASON;
import static java.math.BigDecimal.ZERO;

/**
 * Created by ember on 16/4/5.
 */
@Service
@Slf4j
public class CashLoanService {
  @Autowired
  private CashLoanOverdueEventModel overdueEventModel;
  @Autowired
  private CashLoanCalcFeeEventModel calcFeeEventModel;
  @Autowired
  private CashLoanPayoutEventModel payoutEventModel;
  @Autowired
  private CashLoanRepayEventModel repayEventModel;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private CreditGainBeforeRepaySnapshotService creditGainBeforeRepaySnapshotService;
  @Autowired
  private CashLoanFundingModel fundingModel;
  @Autowired
  private LoanAccountModel accountModel;
  @Autowired
  private CashLoanRepaymentModel repaymentModel;
  @Autowired
  private CashLoanRepaymentUnitModel repaymentUnitModel;
  @Autowired
  private LoanUserCreditsInfoModel creditsInfoModel;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private CashLoanMonitorService cashLoanMonitorService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private AdminUserModel adminUserModel;
  @Autowired
  private OperationLogService operationLogService;
  @Autowired
  private LoanUserCreditsInfoModel userCreditsInfoModel;
  @Autowired
  private CashLoanPayoutTimesLoader cashLoanPayoutTimesLoader;
  @Autowired
  private MonthlyUserActionTimesLoader monthlyUserActionTimesLoader;
  @Autowired
  private DailyUserActionTimesLoader dailyUserActionTimesLoader;
  @Autowired
  private CashLoanFundingService cashLoanFundingService;
  @Autowired
  private CashLoanEventService eventService;
  @Autowired
  private ProductConfigService productConfigService;
  @Autowired
  private IdGeneratorService idGeneratorService;
  @Autowired
  private CashLoanOverdueRevertEventModel overdueRevertEventModel;
  @Autowired
  private LoanUserAdditionalInfoService userAdditionalInfoService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private BizCheckListService checkListService;
  @Autowired
  private LoanAccountAdditionalInfoModel additionalInfoModel;
  @Autowired
  private MongoLoanOrderInfoModel mongoLoanOrderInfoModel;
  @Autowired
  private LoanUserCreditsService creditsService;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;
  @Autowired
  private LoanBankAccountService bankAccountService;
  @Autowired
  private CashLoanRepaymentDetailModel cashLoanRepaymentDetailModel;
  @Autowired
  private NotificationService notificationService;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private LoanUserTypeService loanUserTypeService;
  @Autowired
  private PaymentMonitor paymentMonitor;
  @Autowired
  private LoanLotteryService loanLotteryService;
  @Autowired
  private LoanActivityService loanActivityService;
  @Autowired
  private CashLoanOrderAdditionalInfoService cashLoanOrderAdditionalInfoService;
  @Autowired
  private CashLoanRepaymentService cashLoanRepaymentService;
  @Autowired
  private FundOrderService fundOrderService;
  @Autowired
  private FundTradingMethod fundTradingMethod;
  @Autowired
  private CashLoanInstalmentModel cashLoanInstalmentModel;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  private LoanAccountExtraInfoService loanAccountExtraInfoService;
  @Autowired
  private CashLoanCalcCreditsService cashLoanCalcCreditsService;
  @Autowired
  private ThirdPartyEventService thirdPartyEventService;
  @Autowired
  private FsaDebtService fsaDebtService;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private HandWrittenSignatureService handWrittenSignatureService;
  @Autowired
  private TriggerPayEventMsgUtil triggerPayEventMsgUtil;
  @Autowired
  private CashLoanRepayStrategyService cashLoanRepayStrategyService;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private BizCheckTool bizCheckTool;
  @Autowired
  private CreditsDecreaseQuickOrderInfoService creditsDecreaseQuickOrderInfoService;
  @Autowired
  private FundConfig fundConfig;
  @Autowired
  private CashLoanActivityOrderService cashLoanActivityOrderService;
  @Autowired
  private CashLoanFundingModel cashLoanFundingModel;
  @Autowired
  private CreditChangedEventDispatcher creditChangedEventDispatcher;
  @Autowired
  private AgreementPlatformService agreementPlatformService;
  @Resource
  private CashLoanRepayHelper cashLoanRepayHelper;
  @Autowired
  private ElecSignatureService elecSignatureService;
  @Resource(name = EcExecutorConfig.INIT_PAYOUT_EXECUTOR)
  private Executor initPayoutPoolExecutor;
  @Autowired
  private LyConfig lyConfig;
  @Autowired
  private LyAdminService lyAdminService;
  @Autowired
  private CashLoanRejectOrderService rejectCashLoanOrderService;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private ManualReductionTaskModel manualReductionTaskModel;
  @Autowired
  private BlackCardDemoService blackCardDemoService;
  @Autowired
  private JbpConfig jbpConfig;
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService instalmentCutInterestCouponDeductDetailService;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private CashLoanOrderConfig cashLoanOrderConfig;
  @Autowired
  private RiskRejectOrderService riskRejectOrderService;
  @Resource
  private CouponServiceManager couponServiceManager;
  @Autowired
  private InsuranceProcessEventService insuranceProcessEventService;

  public RepayAmountVO extractRepayAmount(String transNo, List<Long> sortedInstalmentIds,
                                          CurrencyAmount repayAmount, BigDecimal couponDeductAmount,
                                          BigDecimal repayChannelDeductAmount,
                                          Map<Long, InstalmentCutInterestCouponDeductDetailVO> couponDeductMap) {
    if (repayAmount.amount <= 0) {
      throw EcException.error("还款数额不可 <= 0, paymentTrans = " + transNo);
    }
    Map<Long, CashLoanInstalmentVO> idToInstalmentVOMap = ecOrderService.getIdToInstalmentVOMap(sortedInstalmentIds);
    List<CashLoanInstalmentVO> sortedInstalmentVOs = sortedInstalmentIds.stream().map(idToInstalmentVOMap::get).collect(Collectors.toList());
    return RepayAmountVO.from(sortedInstalmentVOs, repayAmount, couponDeductAmount, repayChannelDeductAmount, couponDeductMap);
  }

  private RepayAmountVO extractRepayAmount(String transId,
                                           List<Long> sortedInstalmentIds,
                                           CurrencyAmount repayAmount,
                                           CashLoanRepaymentType deductType,
                                           List<DeductInfoVO> deductInfoVOs) {
    if (CashLoanRepaymentType.BUSINESS_REDUCTION_TYPES.contains(deductType)) {
      return extractCsDeductRepayAmount(sortedInstalmentIds.get(0), repayAmount, deductInfoVOs.get(0));
    }
    return extractRepayAmount(transId, sortedInstalmentIds, repayAmount, BigDecimal.ZERO, BigDecimal.ZERO, Collections.emptyMap());
  }

  private RepayAmountVO extractCsDeductRepayAmount(Long instalmentId, CurrencyAmount repayAmount, DeductInfoVO deductInfoVO) {
    if (repayAmount.amount <= 0) {
      throw EcException.error("还款数额不可 <= 0, instalmentId:{} ", instalmentId);
    }
    CashLoanInstalmentVO cashLoanInstalmentVO = ecOrderService.getInstalmentVO(instalmentId);
    return RepayAmountVO.fromCsDeduct(cashLoanInstalmentVO, repayAmount, deductInfoVO);
  }

  public PaymentProcessResult repayByUnion(UnionRepaymentVO unionRepaymentVO) {
    CashLoanRepaymentRecord repaymentRecord = repaymentModel.findByPaymentTransId(unionRepaymentVO.transNo);
    if (repaymentRecord != null) {
      return handleRepayment(repaymentRecord);
    }

    CalcRepayAmountParam param = cashLoanRepayHelper.buildCalcRepayAmountParamWhenDeduct(unionRepaymentVO.accountId, unionRepaymentVO.transNo);
    if (!param.isParamBuildSuccessFlag()) {
      return PaymentProcessResult.from(ProcessStatus.FAILED, ReasonCode.LOAN_ORDER_PROCESS_FAILED);
    }

    List<CashLoanInstalmentVO> sortedInstalmentVOs = param.getSortedInstalmentVOList();
    List<Long> sortedInstalmentIds = param.getSortedInstalmentIds();
    Map<Long, Integer> actualOverdueDaysMap = param.getActualOverdueDaysMap();
    Map<Long, InstalmentCutInterestCouponDeductDetailVO> cutInterestCouponDeductMap = param.getCutInterestCouponDeductMap();

    if (doBusinessDeduct(unionRepaymentVO, sortedInstalmentVOs, cutInterestCouponDeductMap)) {
      // 业务减免还款（客服｜催收）
      repayByNormalType(param.getSortedInstalmentIds(), unionRepaymentVO, MoneyOffDeductVO.forNormal(ZERO), param.getCutInterestCouponDeductMap(), null);
    } else {
      // 1.根据实还时间判断是否延迟超过30天，是否可以享受自动减免
      Boolean canAutoDeduct = Clock.getDaysBetween(unionRepaymentVO.transactionTime, Clock.now(), unionRepaymentVO.sdkType.getTimeZone()) < cashLoanConfig.getAutoDeductSkipDays();
      CashLoanRepayContext context = buildRepayContext(param, unionRepaymentVO, canAutoDeduct);
      // 2.获取减免券及减免明细
      MoneyOffDeductVO moneyOffDeductDetail = getMoneyOffDeductDetail(context);
      // 3.做自动减免和降息(3里关于满减券的逻辑需要替换为直接用2加工好的)
      autoDeduct(unionRepaymentVO, sortedInstalmentVOs, cutInterestCouponDeductMap, actualOverdueDaysMap, moneyOffDeductDetail.getDeductAmount());
      // 4.用户实还处理
      repayByNormalType(sortedInstalmentIds, unionRepaymentVO, moneyOffDeductDetail, cutInterestCouponDeductMap, context.getCouponVO());
    }

    // 假逾期状态回滚
    instalmentCutInterestCouponDeductDetailService.revertFakeOverdueStatusIfNeeded(unionRepaymentVO.accountId, sortedInstalmentIds, unionRepaymentVO.sdkType);

    return PaymentProcessResult.from(ProcessStatus.PROCESSED, ReasonCode.NO_ERROR);
  }

  public CashLoanRepayContext buildRepayContext(CalcRepayAmountParam param, UnionRepaymentVO unionRepaymentVO, Boolean canAutoDeduct) {
    CashLoanRepayContext context = new CashLoanRepayContext();

    Map<Long, BigDecimal> delayFeeMap = param.getSortedInstalmentVOList()
        .stream()
        .collect(Collectors.toMap(
            CashLoanInstalmentVO::getId,
            instalment -> cashLoanRepayHelper.getUnpaidOverCalcFee(instalment, unionRepaymentVO.transactionTime, unionRepaymentVO.sdkType),
            (a, b) -> a
        ));

    LoanMoneyOffCouponVO couponVO = (LoanMoneyOffCouponVO) loanUserCouponService.getAtPresentPendingCoupon(param.getRepaymentParam().waitRepayOrderId, unionRepaymentVO.accountId, LoanCouponUsageType.MONEY_OFF);

    context.setUnionRepaymentVO(unionRepaymentVO);
    context.setTransNo(unionRepaymentVO.transNo);
    context.setTransactionTime(unionRepaymentVO.transactionTime);
    context.setLoanAccountId(unionRepaymentVO.accountId);
    context.setCurrencyAmount(unionRepaymentVO.getAmount());
    context.setUserRepayAmount(unionRepaymentVO.amount.getAmountInYuan());
    context.setSdkType(unionRepaymentVO.sdkType);
    context.setRepaymentParam(param.getRepaymentParam());
    context.setSortedInstalmentVOList(param.getSortedInstalmentVOList());
    context.setSortedInstalmentIds(param.getSortedInstalmentIds());
    context.setActualOverdueDaysMap(param.getActualOverdueDaysMap());
    context.setCutInterestCouponDeductMap(param.getCutInterestCouponDeductMap());
    context.setDelayFeeMap(delayFeeMap);
    context.setCouponVO(couponVO);
    context.setCanAutoDeduct(canAutoDeduct);

    return context;
  }

  @RunInTransaction
  private MoneyOffDeductVO getMoneyOffDeductDetail(CashLoanRepayContext context) {
    LoanMoneyOffCouponVO couponVO = context.getCouponVO();
    // 没有可使用满减券，减免金额为0
    if (Objects.isNull(couponVO)) {
      return MoneyOffDeductVO.forNormal(ZERO);
    }

    loanUserCouponService.findByIdForUpdate(couponVO.id);

    IMoneyOffCouponService iMoneyOffCouponService = couponServiceManager.routeMoneyOffService(couponVO);

    // 获取优惠券减免明细
    MoneyOffDeductVO moneyOffDeductVO = iMoneyOffCouponService.calcMoneyOffDeductAmount(context);

    // 判断券是否可用
    boolean couponCanUse = checkCouponUse(couponVO, iMoneyOffCouponService, context, moneyOffDeductVO);

    couponCanUse = loanUserCouponService.commonLogicForCouponCanUse(couponCanUse, couponVO, moneyOffDeductVO.getDeductAmount());

    if (!couponCanUse) {
      return MoneyOffDeductVO.forNormal(ZERO);
    }

    cashLoanMonitorService.logCouponDeductDetail(moneyOffDeductVO, context);

    return moneyOffDeductVO;
  }

  public boolean checkCouponUse(LoanUserCouponVO couponVO, IMoneyOffCouponService iMoneyOffCouponService, CashLoanRepayContext context, MoneyOffDeductVO moneyOffDeductVO) {
    return loanUserCouponService.checkStatusBeforeUseMoneyOffCoupon(couponVO) && iMoneyOffCouponService.checkCouponCanUseInRepayDeduct(context, moneyOffDeductVO);
  }

  /**
   * 业务减免
   *
   * @param unionRepaymentVO
   * @param sortedInstalmentVOs
   * @return
   */
  @NotNull
  private boolean doBusinessDeduct(UnionRepaymentVO unionRepaymentVO, List<CashLoanInstalmentVO> sortedInstalmentVOs, Map<Long, InstalmentCutInterestCouponDeductDetailVO> couponDeductMap) {
    //获取业务分期类型，默认是客服减免
    ManualReductionTaskRecord pendingTask = manualReductionTaskModel.findPendingTaskByLoanAccountId(unionRepaymentVO.accountId);
    return pendingTask != null && ReductionFactory.getReductionInstance(ReductionScene.fromCode(pendingTask.getTaskType()))
        .doDeduct(unionRepaymentVO, sortedInstalmentVOs, couponDeductMap);
  }

  /**
   * 分期自动减免和账单减免状态修改
   *
   * @param unionRepaymentVO
   * @param instalmentVOs
   * @return
   */
  private void autoDeduct(
      UnionRepaymentVO unionRepaymentVO, List<CashLoanInstalmentVO> instalmentVOs,
      Map<Long, InstalmentCutInterestCouponDeductDetailVO> cutInterestCouponDeductMap, Map<Long, Integer> actualOverdueDaysMap,
      BigDecimal moneyOffDeductAmount
  ) {
    BigDecimal amount = unionRepaymentVO.amount.getAmountInYuan().add(moneyOffDeductAmount);

    AutoDeductPlan plan = getAutoDeductPlan(unionRepaymentVO.sdkType, amount, instalmentVOs, cutInterestCouponDeductMap, unionRepaymentVO.transactionTime);

    // 先进行逾期抵扣
    autoDeduct(instalmentVOs, plan.getAutoDeductAmountMap(), actualOverdueDaysMap);

    // 再进行优惠券抵扣
    plan.couponDeductDetailVOList.forEach(instalmentVO -> {
      useCutInterestCouponForOverdueCannotUse(instalmentVO.currency, cutInterestCouponDeductMap.get(instalmentVO.id), unionRepaymentVO.transactionTime);
      cutInterestCouponDeductMap.remove(instalmentVO.id);
    });
  }

  @RunInTransaction
  public void doBusinessDeductDeduct(CashLoanInstalmentVO instalmentVO, DeductInfoVO deductInfoVO, CashLoanRepaymentType cashLoanRepaymentType) {
    EcAsserts.assertTrue(BigDecimalHelper.compareTo(deductInfoVO.getAmount(), BigDecimal.ZERO) > 0,
        "business deduct amount should be greater than zero, strategyId is {}", deductInfoVO.getStrategyId());
    Long instalmentId = instalmentVO.id;
    if (instalmentVO.status != CashLoanInstalmentStatus.INIT) {
      log.warn("instalmentStatus is not init, cannot do cs deduct; id: {}", instalmentId);
      return;
    }

    CurrencyAmount deductAmount = CurrencyAmount.fromYuan(instalmentVO.currency, deductInfoVO.getAmount());
    repayByNotNormalType(instalmentVO.orderId, instalmentVO.loanAccountId,
        Collections.singletonList(instalmentId), deductAmount,
        cashLoanRepaymentType, null, null,
        Collections.singletonList(deductInfoVO));
    log.info("do business Deduct for orderId: {}, instalmentId: {}, deduct amount: {}, strategyId is {}",
        instalmentVO.orderId, instalmentId, deductInfoVO.getAmount(), deductInfoVO.getStrategyId());
  }

  public void doInvalidStrategy(Long accountId) {
    cashLoanRepayStrategyService.updateInvalidByAccountId(accountId);
  }

  private PaymentProcessResult handleRepayment(CashLoanRepaymentRecord repaymentRecord) {
    CashLoanRepaymentStatus status = CashLoanRepaymentStatus.fromCode(repaymentRecord.getStatus());
    switch (status) {
      case SUCCEED:
        return PaymentProcessResult.from(ProcessStatus.PROCESSED, ReasonCode.NO_ERROR);
      case FAIL:
        return PaymentProcessResult.from(ProcessStatus.FAILED, ReasonCode.LOAN_ORDER_PROCESS_FAILED);
      default:
        throw EcException.error("unknown repayment status code for repay: " + status.code);
    }

  }

  @RunInTransaction
  private CashLoanRepaymentRecord initRepay(long accountId, long userId, String paymentTransId, RepayAmountVO amountVO, Long couponId, Long transactionTime) {
    return createRepay(accountId, userId, paymentTransId, amountVO, null, CashLoanRepaymentStatus.INIT, CashLoanRepaymentType.NORMAL, userId, null, couponId, transactionTime);
  }

  @RunInTransaction
  public void initRepaymentDetail(
      CashLoanRepaymentRecord cashLoanRepaymentRecord, List<RepaymentUnitAmountVO> repaymentUnitAmountVOS,
      BigDecimal couponAmount, CashLoanRepaymentDetailType type, MoneyOffDeductVO moneyOffDeductDetail
  ) {
    Long couponId = cashLoanRepaymentRecord.getCouponId();
    if (couponId == null) {
      return;
    }

    IRepayDeductCouponService iRepayDeductCouponService = couponServiceManager.routeRepayDeductService(type, moneyOffDeductDetail);
    List<CashLoanRepaymentDetailVO> cashLoanRepaymentDetailVOS = iRepayDeductCouponService.splitCouponDeductAmountInfoDetail(cashLoanRepaymentRecord, repaymentUnitAmountVOS, couponAmount, type, moneyOffDeductDetail);

    cashLoanRepaymentDetailVOS.forEach(this::createRepaymentDetail);
  }

  @RunInTransaction
  private void createRepaymentDetail(CashLoanRepaymentDetailVO cashLoanRepaymentDetailVO) {
    if (cashLoanRepaymentDetailVO.amount.compareTo(BigDecimal.ZERO) == 0) {
      return;
    }
    cashLoanRepaymentDetailModel.init(
        cashLoanRepaymentDetailVO.userId,
        cashLoanRepaymentDetailVO.accountId,
        cashLoanRepaymentDetailVO.orderId,
        cashLoanRepaymentDetailVO.repaymentId,
        cashLoanRepaymentDetailVO.installmentId,
        cashLoanRepaymentDetailVO.currency,
        cashLoanRepaymentDetailVO.relatedId,
        cashLoanRepaymentDetailVO.interest,
        cashLoanRepaymentDetailVO.prePlatformFee,
        cashLoanRepaymentDetailVO.preInterestExcludeFee,
        cashLoanRepaymentDetailVO.overdueInterest,
        cashLoanRepaymentDetailVO.penalty,
        cashLoanRepaymentDetailVO.postInterest,
        cashLoanRepaymentDetailVO.postPlatformFee,
        cashLoanRepaymentDetailVO.postInterestExcludeFee,
        cashLoanRepaymentDetailVO.ppn,
        cashLoanRepaymentDetailVO.principal,
        cashLoanRepaymentDetailVO.amount,
        cashLoanRepaymentDetailVO.type
    );
  }

  @RunInTransaction
  public CashLoanRepaymentRecord createRepay(
      long accountId,
      long userId,
      String paymentTransId,
      RepayAmountVO amountVO,
      Long timeRepaid,
      CashLoanRepaymentStatus repayStatus,
      CashLoanRepaymentType repaymentType,
      Long userOpt,
      String extraData,
      Long couponId,
      Long transactionTime
  ) {
    if (CollectionUtils.isEmpty(amountVO.repaymentUnitAmountVOs)) {
      throw EcException.error("repayment unit is empty. user_id: " + userId);
    }
    BigDecimal amountPpn = amountVO.ppn == null ? BigDecimal.ZERO : amountVO.ppn;
    CashLoanRepaymentRecord repaymentRecord = repaymentModel.init(
        accountId,
        userId,
        amountVO.currency,
        amountVO.amount,
        amountVO.principal,
        amountVO.interest,
        amountVO.prePlatformFee,
        amountVO.preInterestExcludeFee,
        amountVO.postInterest,
        amountVO.postPlatformFee,
        amountVO.postInterestExcludeFee,
        amountPpn,
        amountVO.overdueInterest,
        amountVO.penalty,
        amountVO.overflowAmount,
        paymentTransId,
        timeRepaid,
        repayStatus,
        userOpt,
        repaymentType,
        extraData,
        couponId,
        transactionTime
    );
    for (RepaymentUnitAmountVO unitAmountVO : amountVO.repaymentUnitAmountVOs) {
      BigDecimal unitPpn = unitAmountVO.ppn == null ? BigDecimal.ZERO : unitAmountVO.ppn;
      repaymentUnitModel.init(
          accountId,
          unitAmountVO.userId,
          repaymentRecord.getId(),
          unitAmountVO.orderId,
          unitAmountVO.instalmentId,
          unitAmountVO.currency,
          unitAmountVO.amount,
          unitAmountVO.principal,
          unitAmountVO.interest,
          unitAmountVO.prePlatformFee,
          unitAmountVO.preInterestExcludeFee,
          unitAmountVO.postInterest,
          unitAmountVO.postPlatformFee,
          unitAmountVO.postInterestExcludeFee,
          unitPpn,
          unitAmountVO.overdueInterest,
          unitAmountVO.penalty
      );
    }
    creditChangedEventDispatcher.dispatchEvent(CreditChangedEventDispatchContext.builder()
        .eventType(CreditChangedEventType.REPAY)
        .relatedId(repaymentRecord.getId())
        .userId(userId)
        .loanAccountId(accountId)
        .build());
    return repaymentRecord;
  }

  public void onRepaymentSucceed(PaymentProvider provider, PaymentStatus status, String transId, Long transactionTime, BigDecimal actualCouponDeductAmount) {
    LoanAssertion.assertPaymentInStatus(status, PaymentStatus.SUCCEED);
    CashLoanRepaymentRecord beforeRepayRecord = repaymentModel.findByPaymentTransIdOrThrow(transId);
    Long userId = beforeRepayRecord.getUserId();
    // 还款前剩余可借快照：必须早于事务内 deduct() 扣减本金，且限 INIT 状态，否则快照会变成还款后的值。
    // 比 code 而非用 fromCode()：后者遇空状态抛异常，快照是辅助功能不得打断还款。
    if (CashLoanRepaymentStatus.INIT.code.equals(beforeRepayRecord.getStatus())) {
      creditGainBeforeRepaySnapshotService.recordBeforeRepayRemaining(beforeRepayRecord.getAccountId());
    }
    CashLoanRepaymentRecord repaymentRecord = threadTransactionalModel.transactionResult(
        configuration -> processRepaymentInTransaction(userId, transId, transactionTime)
    );
    postProcessRepayment(repaymentRecord, userId, provider, transId, transactionTime, actualCouponDeductAmount);
  }

  /**
   * 事务内处理还款成功逻辑
   * 包含: 加锁、状态更新、扣款、刷新优惠券、发布事件
   */
  @RunInTransaction
  CashLoanRepaymentRecord processRepaymentInTransaction(Long userId, String transId, Long transactionTime) {
    // 加锁
    LoanAccountRecord loanAccountRecord = accountModel.findByUserIdForUpdateOrThrow(userId);
    CashLoanRepaymentRecord cashLoanRepaymentRecord = repaymentModel.findByPaymentTransIdOrThrow(transId);
    CashLoanAssertion.assertRepaymentInStatus(cashLoanRepaymentRecord, CashLoanRepaymentStatus.INIT);
    List<Long> orderIds = repaymentUnitModel.findOrderIdsOrThrow(cashLoanRepaymentRecord.getId());
    repaymentModel.updateStatus(cashLoanRepaymentRecord, CashLoanRepaymentStatus.SUCCEED, transactionTime);
    RepayCompletedTermsVO termsVo = deduct(loanAccountRecord.getId(), RepaymentVO.from(cashLoanRepaymentRecord));
    cashLoanRepaymentRecord.refresh();
    loanUserCouponService.refreshUserDynamicCouponAfterRepay(userId, loanAccountRecord.getId());
    // 通知repaymentSucceed事件
    publishAfterRepay(orderIds, RepaymentVO.from(cashLoanRepaymentRecord, termsVo.repayCompletedTerms, termsVo.repayCompletedTimelyTerms));
    return cashLoanRepaymentRecord;
  }

  /**
   * 还款成功后的后处理逻辑
   * 包含: 记录监控事件、发送推送通知
   */
  void postProcessRepayment(CashLoanRepaymentRecord repaymentRecord, Long userId,
                            PaymentProvider provider, String transId,
                            Long transactionTime, BigDecimal actualCouponDeductAmount) {
    CashLoanRepaymentStatus finalStatus = CashLoanRepaymentStatus.fromCode(repaymentRecord.getStatus());
    LoanAccountRecord loanAccountRecord = accountModel.findByUserId(userId);
    SDKType sdkType = SDKType.fromCode(loanAccountRecord.getSdkType());
    // 记录事件
    cashLoanMonitorService.logRepayment(
        repaymentRecord.getAccountId(),
        repaymentRecord.getAmount(),
        CashLoanRepaymentType.NORMAL,
        finalStatus,
        sdkType
    );
    //还款不成功，不发送短信和push
    if (finalStatus != CashLoanRepaymentStatus.SUCCEED) {
      return;
    }
    //该笔还款处理完的时间减去用户真正发起还款的时间
    Long repayProcessTime = Clock.now() - transactionTime;
    paymentMonitor.logRepaymentProcessTime(provider, transId, repayProcessTime);
    sendAppPushAndSms(userId, sdkType, repaymentRecord, actualCouponDeductAmount);
  }

  private void sendAppPushAndSms(Long userId, SDKType sdkType, CashLoanRepaymentRecord repaymentRecord, BigDecimal actualCouponDeductAmount) {
    RepaymentVO repaymentVO = RepaymentVO.from(repaymentRecord);
    List<CashLoanInstalmentVO> initInstalmentVOs = cashLoanInstalmentModel.findEarliestByAccountId(repaymentVO.accountId)
        .stream()
        .map(CashLoanInstalmentVO::from).collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(initInstalmentVOs)) {
      Long now = Clock.now();
      List<CashLoanInstalmentVO> overDueInstalmentVOs = initInstalmentVOs.stream().filter(instalmentRecord -> instalmentRecord.billingDate < now).collect(Collectors.toList());
      if (CollectionUtils.isNotEmpty(overDueInstalmentVOs)) {
        BigDecimal overDueAmount = CalcFeeUtil.getOwedAmount(overDueInstalmentVOs);
        Integer overDueInstalmentCount = overDueInstalmentVOs.size();
        NotifRepaymentSuccessExistOverDueOrderParam param = new NotifRepaymentSuccessExistOverDueOrderParam(userId, sdkType, repaymentVO, overDueAmount, actualCouponDeductAmount, overDueInstalmentCount);
        notificationService.pushSystemNotif(SystemNotifScene.REPAYMENT_SUCCESS_EXIST_OVERDUE_ORDER, param);
        return;
      }
      NotifRepaymentSuccessExistPendingOrderParam param = new NotifRepaymentSuccessExistPendingOrderParam(userId, sdkType, repaymentVO, initInstalmentVOs.get(0).billingDate, actualCouponDeductAmount);
      notificationService.pushSystemNotif(SystemNotifScene.REPAYMENT_SUCCESS_EXIST_PENDING_ORDER, param);
      return;
    }
    NotifRepaymentSuccessParam param = new NotifRepaymentSuccessParam(userId, sdkType, repaymentVO, actualCouponDeductAmount);
    notificationService.pushSystemNotif(SystemNotifScene.REPAYMENT_SUCCESS_CLEAR_ALL_ORDER, param);
  }

  @RunInTransaction
  public RepayCompletedTermsVO deduct(Long accountId, RepaymentVO repaymentVO) {
    List<CashLoanRepaymentUnitRecord> allRepaymentUnitRecords = repaymentUnitModel.findByRepaymentId(repaymentVO.id);
    List<RepaymentUnitAmountVO> allRepaymentUnitAmountVOs = allRepaymentUnitRecords
        .stream()
        .map(RepaymentUnitAmountVO::from)
        .collect(Collectors.toList());
    // 本次还款总的还款期数
    AtomicInteger repayCompletedTerms = new AtomicInteger(0);
    // 本次还款没有逾期期数
    AtomicInteger repayCompletedTimelyTerms = new AtomicInteger(0);
    Map<Long, List<RepaymentUnitAmountVO>> orderIdToRepaymentUnitAmountVOsMap = allRepaymentUnitAmountVOs.stream().collect(Collectors.groupingBy(vo -> vo.orderId));
    orderIdToRepaymentUnitAmountVOsMap.forEach((orderId, repaymentUnitAmountVOs) -> {
      List<CashLoanInstalmentVO> completedInstalments = repaymentUnitAmountVOs.stream()
          .map(unitAmountVO -> ecOrderService.repayInstalment(unitAmountVO))
          .filter(CashLoanInstalmentVO::isComplete)
          .collect(Collectors.toList());
      repayCompletedTerms.getAndAdd(completedInstalments.size());
      List<CashLoanInstalmentVO> completedTimelyInstalments = completedInstalments.stream()
          .filter(e -> e.billingDate > Clock.now())
          .collect(Collectors.toList());
      repayCompletedTimelyTerms.getAndAdd(completedTimelyInstalments.size());
      repayOrder(orderId, repaymentUnitAmountVOs, completedInstalments);
    });
    BigDecimal eventPpn = repaymentVO.ppn == null ? BigDecimal.ZERO : repaymentVO.ppn;
    repayEventModel.insert(
        accountId,
        repaymentVO.id,
        repaymentVO.currency,
        repaymentVO.principal,
        repaymentVO.interest,
        repaymentVO.prePlatformFee,
        repaymentVO.preInterestExcludeFee,
        repaymentVO.postInterest,
        repaymentVO.postPlatformFee,
        repaymentVO.postInterestExcludeFee,
        eventPpn,
        repaymentVO.overdueInterest,
        repaymentVO.penalty,
        repaymentVO.overflowAmount,
        repaymentVO.type
    );
    // 返还用户超还的数目
    recoverUserOverFlowAmount(accountId, repaymentVO.overflowAmount);
    return RepayCompletedTermsVO.builder().
        repayCompletedTerms(repayCompletedTerms.get()).
        repayCompletedTimelyTerms(repayCompletedTimelyTerms.get()).
        build();
  }

  @RunInTransaction
  private void repayOrder(Long orderId,
                          List<RepaymentUnitAmountVO> repaymentUnitAmountVOs,
                          List<CashLoanInstalmentVO> completedInstalments) {
    RepayAmountVO orderRepayAmountVO = RepayAmountVO.from(repaymentUnitAmountVOs);
    CashLoanOrderVO orderVO = ecOrderService.repayOrder(orderId, orderRepayAmountVO, completedInstalments.size());
    // 机构单根据被还分期和被还订单 决定是提前结清、按期结清
    fundTradingMethod.syncRepaymentFromCashLoanOrder(orderVO, completedInstalments);
    // 发布订单完成事件和打点
    if (orderVO.status == CashLoanOrderStatus.COMPLETE) {
      eventService.publishOrderStatusEvent(orderVO);
      cashLoanMonitorService.logOrderStatus(orderVO, !ecOrderService.isFirstOrder(orderVO.accountId));
    }
  }

  // 返还用户超还的数目
  @RunInTransaction
  private void recoverUserOverFlowAmount(Long accountId, BigDecimal overflowAmount) {
    LoanUserCreditsInfoRecord creditsInfoRecord = creditsInfoModel.findByAccountIdOrThrow(accountId);
    if (BigDecimalHelper.greaterThan(overflowAmount, BigDecimal.ZERO)) {
      userCreditsInfoModel.addOverflowAmount(creditsInfoRecord, overflowAmount);
    }
  }

  @RunInTransaction
  public void publishAfterRepay(List<Long> orderIds, RepaymentVO repaymentVO) {
    List<OrderInstalment> orderInstalmentList = ecOrderService.getOrderInstalmentList(orderIds);
    eventService.publishRepaymentStatusNewEvent(orderIds, orderInstalmentList, repaymentVO);
  }

  public List<RepaymentVO> listDisplayNormalRepayment(Long accountId, int offset, int limit) {
    List<CashLoanRepaymentRecord> repaymentRecords =
        repaymentModel.find(accountId, CashLoanRepaymentType.DISPLAY_NORMAL_REPAYMENT_TYPES, offset, limit);
    return convertToVO(repaymentRecords);
  }

  public List<RepaymentVO> listDisplayNormalRepaymentWithTime(Long accountId, Long timeBegin, Long timeEnd, int offset, int limit) {
    List<CashLoanRepaymentRecord> repaymentRecords =
        repaymentModel.find(accountId, CashLoanRepaymentType.DISPLAY_NORMAL_REPAYMENT_TYPES, timeBegin, timeEnd, offset, limit);
    return convertToVO(repaymentRecords);
  }


  public List<RepaymentVO> getDisplayNormalRepaymentWithLyAdminFilter(Long accountId, Long timeBegin, Long timeEnd, int offset, int limit, String adminEmail) {

    List<RepaymentVO> repaymentVOs = this.listDisplayNormalRepaymentWithTime(accountId, timeBegin, timeEnd, offset, limit);

    // Remove the repayment corresponding to orders within the ly illegal time period
    boolean isLyAdminUser = lyConfig.isLyAdminUser(adminEmail);
    if (isLyAdminUser) {
      repaymentVOs = lyAdminService.getLyValidRepaymentVOs(repaymentVOs, accountId);
    }
    return repaymentVOs;
  }

  private List<RepaymentVO> convertToVO(List<CashLoanRepaymentRecord> repaymentRecords) {
    return repaymentRecords
        .stream()
        .map(RepaymentVO::from)
        .collect(Collectors.toList());
  }

  /**
   * @return map[ repayment_id, List<order_id> ]
   */
  public Map<Long, List<Long>> getRepaymentOrderIdsMap(List<Long> repaymentIds) {
    List<CashLoanRepaymentUnitRecord> repayUnits = repaymentUnitModel.findByRepaymentIds(repaymentIds);
    return repayUnits
        .stream()
        .collect(Collectors.groupingBy(
            CashLoanRepaymentUnitRecord::getRepaymentId,
            Collectors.mapping(CashLoanRepaymentUnitRecord::getOrderId, Collectors.toList())
        ));
  }

  /**
   * @return map[ order_id, List<repayment_id> ]
   */
  public Map<Long, List<Long>> getOrderIdToRepaymentIdsMap(List<Long> repaymentIds) {
    List<CashLoanRepaymentUnitRecord> repayUnits = repaymentUnitModel.findByRepaymentIds(repaymentIds);
    return repayUnits
        .stream()
        .collect(Collectors.groupingBy(
            CashLoanRepaymentUnitRecord::getOrderId,
            Collectors.mapping(CashLoanRepaymentUnitRecord::getRepaymentId, Collectors.toList())
        ));
  }

  public void adminRejectOrderWithMsg(Long orderId) {
    //TODO(LTB,T000000) 订单取消 弹窗看看怎么加一下
    rejectOrderWithMsg(orderId, CashLoanOrderRejectReason.REJECT_ORDER_BY_ADMIN);
    operationLogService.insert(null, null, null, LogEventType.ORDER_REJECT_PAYOUT, ObjType.CASH_LOAN_ORDER, orderId, AdminUserIDThreadLocal.get());
  }

  public CashLoanOrderVO cancelOrderWithMsg(Long accountId, Long orderId) {
    CashLoanOrderVO rejectOrderVO = threadTransactionalModel.transactionResult(configuration -> {
      accountModel.findByIdForUpdateOrThrow(accountId);
      CashLoanOrderVO vo = ecOrderService.getOrderVO(orderId);
      if (CashLoanOrderStatus.CHECK != vo.status) {
        log.warn("订单状态应为CHECK, 实际为:" + vo.status);
        return vo;
      }
      return doRejectOrder(orderId, CashLoanOrderRejectReason.CANCEL_EXPIRED_CHECK_ORDER);
    });
    NotifCancelOrderParam notifCancelOrderParam = new NotifCancelOrderParam(rejectOrderVO.userId, rejectOrderVO.sdkType, orderId);
    notificationService.pushSystemNotif(SystemNotifScene.CANCEL_EXPIRED_ORDER, notifCancelOrderParam);
    return rejectOrderVO;
  }

  private void rejectOrderWithMsg(Long orderId, CashLoanOrderRejectReason rejectReason) {
    boolean rejectRiskInReviewOrder = rejectCashLoanOrderService.tryCancelReserveOrderFromAdmin(orderId, rejectReason);
    if (rejectRiskInReviewOrder) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("取消订单申请已提交，取消成功后会通知用户。"));
    }
    CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
    riskCheckBeforeRejectOrder(orderVO);
    CashLoanOrderVO order = threadTransactionalModel.transactionResult(configuration -> {
      accountModel.findByIdForUpdateOrThrow(orderVO.accountId);
      return rejectOrder(orderId, rejectReason);
    });
    LoanBankAccountVO loanBankAccountVO = bankAccountService.findVOById(orderVO.paymentCredential.getId());
    if (CashLoanOrderRejectReason.REJECT_ORDER_BY_ADMIN.equals(rejectReason)) {
      notificationService.pushSystemNotif(SystemNotifScene.CANCEL_LOAN, new NotifCancelOrderParam(order.userId, order.sdkType, orderId));
    } else if (orderVO.sdkType == SDKType.IDN_YQD && loanBankAccountVO.bankType.isEWallet) {
      //印尼现金贷，并且是电子钱包类型,发电子钱包失败通知
      notificationService.pushSystemNotif(SystemNotifScene.EWALLET_PAYOUT_FAILED, new NotifCancelOrderParam(order.userId, order.sdkType, orderId));
    } else {
      notificationService.pushSystemNotif(SystemNotifScene.PAYOUT_FAILED, new NotifCancelOrderParam(order.userId, order.sdkType, orderId));
    }
  }

  private void riskCheckBeforeRejectOrder(CashLoanOrderVO orderVO) {
    if (Objects.isNull(orderVO.traceId)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("订单当前处于风控中，不能取消。如您需要了解具体原因，请咨询风控团队"));
    }
    LoanUserRiskTraceVO userRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrNull(orderVO.traceId);
    if (Objects.isNull(userRiskTraceVO)) {
      throw EcException.error(EcExceptionType.RISK_TRACE_ID_NOT_FOUND, "Reject Order Failed - Risk Trace Id not found , order :" + orderVO.status);
    }
    if (RiskFlowTraceStatusV2.FINISH.equals(userRiskTraceVO.status)
        && LoanCreditsStatus.ACCEPTED.equals(userRiskTraceVO.creditsStatus)) {
      // continue the reject order
      return;
    }
    if (RiskFlowTraceStatusV2.FINISH.equals(userRiskTraceVO.status)
        && LoanCreditsStatus.REJECTED.equals(userRiskTraceVO.creditsStatus)) {
      throw EcException.error(EcExceptionType.LOAN_ORDER_REJECTED_DUE_TO_FORCE_MAJEURE, "Reject Order Failed - Loan Order is rejected");
    }
    throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("订单当前处于风控中，不能取消。如您需要了解具体原因，请咨询风控团队"));
  }

  private void unbindCouponWhenRejectOrder(Long orderId, Long accountId) {
    LoanUserCouponVO couponVO = loanUserCouponService.getAtPresentPendingCoupon(orderId, accountId, LoanCouponUsageType.CUT_INTEREST);
    if (couponVO != null) {
      loanUserCouponService.unbindCutInterestCoupon(couponVO);
    }
  }

  private CashLoanOrderVO rejectOrder(Long orderId, CashLoanOrderRejectReason rejectReason) {
    CashLoanOrderVO orderVO = doRejectOrder(orderId, rejectReason);
    cashLoanMonitorService.logPayoutReject(orderVO.accountId, orderVO.principal, orderVO.sdkType);
    return orderVO;
  }

  CashLoanOrderVO doRejectOrder(Long orderId, CashLoanOrderRejectReason rejectReason) {
    Long accountId = ecOrderService.getOrderVO(orderId).accountId;
    return threadTransactionalModel.transactionResult(configuration -> {
      accountModel.findByIdForUpdateOrThrow(accountId);
      CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
      // 如果正在打款，必须等到打款结果出来
      if (orderVO.mhtOrderNo != null) {
        CashLoanFundingStatus status = cashLoanFundingService.getPayoutResult(orderVO.mhtOrderNo).getStatus();
        if (status != CashLoanFundingStatus.FAILED) {
          throw EcException.warn(EcExceptionType.CASH_LOAN_ORDER_HAS_PROCESSING_DEPOSIT, TT.gen("打款处理中，请等待处理完毕后再提交"));
        }
      }
      //拒绝订单时释放优惠券
      unbindCouponWhenRejectOrder(orderId, accountId);
      //如果是资方订单，进行退款
      fundOrderService.cancelFundOrder(orderId);
      checkListService.cancelPendingOrderCheck(orderId, BusinessType.ORDER_CHECK);
      fsaDebtService.updateDebtStatusWithLoanReject(orderVO);
      return ecOrderService.rejectOrder(orderId, rejectReason);
    });
  }

  public void adminPayout(Long orderId) {
    initPayout(orderId);
    CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
    CashLoanPayoutResult payout = CashLoanPayoutResult.from(fundingModel.findByMhtOrderNo(orderVO.mhtOrderNo));
    operationLogService.insert(null, null, JsonUtils.toString(payout), LogEventType.ORDER_PAYOUT, ObjType.CASH_LOAN_ORDER, orderId, AdminUserIDThreadLocal.get());
  }

  public CashLoanFundingStatus initPayout(Long orderId) {
    Long accountId = ecOrderService.getOrderVO(orderId).accountId;
    return initPayout(orderId, accountId);
  }

  public CashLoanFundingStatus initPayout(Long orderId, Long accountId) {
    return threadTransactionalModel.transactionResult(configuration -> {
      boolean isRetry = false;
      accountModel.findByIdForUpdateOrThrow(accountId);
      CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
      CashLoanAssertion.assertOrderInStatus(orderVO, CashLoanOrderStatus.INIT);
      if (orderVO.mhtOrderNo != null) {
        CashLoanFundingStatus cashLoanFundingStatus = checkAndGetCashLoanFundingStatus(orderVO);
        if (Objects.nonNull(cashLoanFundingStatus)) {
          return cashLoanFundingStatus;
        }
        isRetry = true;
      }
      String mhtOrderNo = idGeneratorService.genId(IdGeneratorService.Type.FUNDING);
      // 关联payout和order
      orderVO = ecOrderService.updateMhtOrderNo(orderId, mhtOrderNo);
      // 黑卡打款砍头逻辑
      if (jbpConfig.getBlackCardUser(orderVO.userId)) {
        List<BlackCardOrderVO> blackCardOrderList = blackCardDemoService.getOrderList(orderVO.userId);
        if (!CollectionUtils.isEmpty(blackCardOrderList)) {
          Collections.reverse(blackCardOrderList);
          BlackCardOrderVO lasterOrder = blackCardOrderList.get(0);
          orderVO.updateHeadCutAmount(lasterOrder.amount);
        }
      }
      CurrencyAmount payoutAmount = CalcFeeUtil.getPayoutAmount(orderVO);
      FundingRouteData routeData = new FundingRouteData(orderVO.sdkType, loanAccountService.isReloan(accountId));
      triggerPayEventMessageIfInExperiment(orderVO, payoutAmount, routeData, isRetry);
      return cashLoanFundingService.initPayout(orderId, mhtOrderNo, payoutAmount, routeData);
    });
  }

  public void asyncInitPayout(Long orderId, Long accountId) {
    initPayoutPoolExecutor.execute(() -> {
      initPayout(orderId, accountId);
    });
  }

  /**
   * 直接返回的几种场景：
   * 1 现有funding status不为failure
   * 2 是failure但不应重试
   * 3 超过了重试次数
   * 4 命中重试黑名单
   */
  private CashLoanFundingStatus checkAndGetCashLoanFundingStatus(CashLoanOrderVO cashLoanOrderVO) {
    CashLoanFundingResult cashLoanFundingResult = cashLoanFundingService.getPayoutResult(cashLoanOrderVO.mhtOrderNo);
    if (
        cashLoanFundingResult.getStatus() != CashLoanFundingStatus.FAILED
        || !cashLoanFundingResult.getErrorCode().shouldRetry
        || cashLoanPayoutTimesLoader.getOrDefault(cashLoanOrderVO.id, 0L) >= cashLoanConfig.maxPayoutRetryTimesPerOrder()
        || cashLoanConfig.isPayoutRetryBlocked(cashLoanOrderVO.id)
    ) {
      log.info("should not retry payout for mhtOrderNo = {}", cashLoanOrderVO.mhtOrderNo);
      return cashLoanFundingResult.getStatus();
    }
    return null;

  }

  @RunInTransaction
  private void triggerPayEventMessageIfInExperiment(CashLoanOrderVO orderVO, CurrencyAmount payoutAmount, FundingRouteData routeData, boolean isRetry) {
    FundOrderVO fundOrderVO = fundOrderService.findReadyFundOrderOrNull(orderVO.id);
    FundProvider fundProvider = fundOrderVO != null ? fundOrderVO.getProvider() : null;
    if (FundProvider.SEA_BANK == fundProvider) {
      if (!fundConfig.getSeaBankPayoutNewApiSwitch()) {
        fundProvider = null;
      }
      if (fundConfig.getSeaBankPayoutRetryWithOwnChannel()) {
        if (isRetry) {
          fundProvider = null;
        }
      }
    }
    List<FundProvider> fundProvidersUseOwnChannel = fundConfig.getFundProvidersUseOwnChannel();
    if (fundProvidersUseOwnChannel.contains(fundProvider)) {
      fundProvider = null;
    }
    triggerPayEventMsgUtil.triggerPayoutEventMsg(PayEventType.CASH_LOAN_BANK_PAYOUT,
        PaymentTransType.PAYOUT,
        orderVO.mhtOrderNo,
        orderVO.userId,
        payoutAmount,
        orderVO.paymentCredential,
        orderVO.sdkType,
        fundProvider);
  }

  private void handlePayoutResult(CashLoanFundingResult result, Long orderId, Long accountId) {
    switch (result.getStatus()) {
      case FAILED:
        onPayoutFailed(orderId, result.getErrorCode());
        break;
      case PAID:
        onPayoutSucceed(orderId, accountId);
        break;
      default:
        break;
    }
  }

  /**
   * 已弃用
   */
  @Deprecated
  @RunInTransaction
  public void onRestructureSucceed(CashLoanOrderVO preLoanOrderVO, CashLoanOrderVO postLoanOrderVO, LoanProductConfigVO productVO) {
    Long now = Clock.now();
    OrderInstalmentPlan oiPlan = OIPlanFactory.getInstance(preLoanOrderVO.sdkType).genPlan(postLoanOrderVO.principal, productVO, now);
    Integer seq = loanAccountService.increaseLoanTimes(preLoanOrderVO.accountId);
    ecOrderService.readyOrder(postLoanOrderVO.id, oiPlan, seq);
    //复制下上一笔订单的TraceId
    ecOrderService.updateTraceId(postLoanOrderVO.id, preLoanOrderVO.traceId);
    //生成mhtOrderNum，直接更新在order表中
    String mhtOrderNo = idGeneratorService.genId(IdGeneratorService.Type.FUNDING);
    ecOrderService.updateMhtOrderNoWithoutStatusCheck(postLoanOrderVO.id, mhtOrderNo);
    CashLoanFundingRecord record = cashLoanFundingModel.init(postLoanOrderVO.id, FundingProviderMapper.getProviderBySdk(postLoanOrderVO.sdkType), mhtOrderNo, CalcFeeUtil.getPayoutAmount(postLoanOrderVO));
    cashLoanFundingModel.setStatus(record, CashLoanFundingStatus.PAID, PaymentErrorCode.NO_ERROR);
  }

  private void onPayoutSucceed(Long orderId, Long accountId) {
    CashLoanOrderVO cashLoanOrderVO = threadTransactionalModel.transactionResult(configuration -> {
      accountModel.findByIdForUpdateOrThrow(accountId);
      CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
      CashLoanFundingResult fundingResult = cashLoanFundingService.getPayoutResult(orderVO.mhtOrderNo);
      EcAsserts.assertTrue(fundingResult.getStatus() == CashLoanFundingStatus.PAID, "funding status should be paid");
      if (orderVO.timePayout == null) {
        orderVO.timePayout = Clock.now();
      }
      LoanProductConfigVO productVO = productConfigService.getProductVO(orderVO.productId);
      OIPlanGenerator oiPlanGenerator = OIPlanFactory.getInstance(orderVO.sdkType);
      OrderInstalmentPlan oiPlan = oiPlanGenerator.genPlan(orderVO.principal, productVO, orderVO.timePayout);
      Integer seq = loanAccountService.increaseLoanTimes(orderVO.accountId);
      OrderInstalment orderInstalment = ecOrderService.readyOrder(orderVO.id, oiPlan, seq);
      // 记录创建保单事件（仅印尼保险适用的 sdkType，与 InitInsurancePolicyJob 口径保持一致）
      if (CreditInsuranceConfig.insuranceProcessEventWriteSwitch() && IdnLoanSDKTypeConstants.IDN_ALL_LOAN_SDK_TYPE_LIST.contains(orderVO.sdkType)) {
        try {
          insuranceProcessEventService.insertIfNotExists(
              orderId, InsuranceBusinessType.CREATE_INSURANCE, InsuranceBusinessStatus.INIT, null);
        } catch (Exception e) {
          log.error("insertIfNotExists CREATE_INSURANCE event failed, orderId={}", orderId, e);
        }
      }
      fsaDebtService.updateDebtStatusWithLoanReady(orderVO);
      orderVO = orderInstalment.orderVO;
      // 记录打款事件
      CashLoanPayoutEventRecord payoutEventRecord = payoutEventModel.insert(orderVO.accountId, orderVO.id, orderVO.currency, orderVO.principal);
      creditChangedEventDispatcher.dispatchEvent(CreditChangedEventDispatchContext.builder()
          .eventType(CreditChangedEventType.PAYOUT)
          .relatedId(payoutEventRecord.getId())
          .userId(orderVO.userId)
          .loanAccountId(orderVO.accountId)
          .build());
      if (needRepaidCut(orderVO)) {
        orderInstalment.instalmentVOS.forEach(vo -> repaidCut(vo.id));
      }
      // 使用降息券
      doUseCutInterestCoupon(orderId, accountId, orderInstalment, orderVO);
      generateLoanAgreement(orderVO);
      return orderVO;
    });
    CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
    TimeZone timeZone = orderVO.sdkType.getTimeZone();
    dailyUserActionTimesLoader.set(
        UserActionEnum.LOAN_TIMES_DAILY,
        accountId,
        dailyUserActionTimesLoader.get(UserActionEnum.LOAN_TIMES_DAILY, accountId) + 1,
        ((Long) (Clock.getRemainingMillisOfDay(timeZone) / 1000)).intValue()
    );
    monthlyUserActionTimesLoader.set(
        UserActionEnum.LOAN_TIMES_MONTHLY,
        accountId,
        monthlyUserActionTimesLoader.get(UserActionEnum.LOAN_TIMES_MONTHLY, accountId) + 1,
        ((Long) (Clock.getRemainingMillisOfMonth(timeZone) / 1000)).intValue()
    );
    cashLoanMonitorService.logPayout(
        accountId,
        orderVO.principal,
        loanAccountService.isReloan(accountId),
        orderVO.sdkType,
        Clock.getSecondsBetween(orderVO.timeCreated, orderVO.timePayout));
    // 发送message到third_party_data_event
    thirdPartyEventService.publishPayoutEvent(accountId, orderVO);
    // 发送短信
    NotifPayoutParam param = new NotifPayoutParam(orderVO.userId, orderVO.sdkType, orderId);
    if (IdnLoanSDKTypeConstants.isIdnAllLoanSDKType(orderVO.sdkType) && !filledMotherLastName(orderVO.accountId)) {
      notificationService.pushSystemNotif(SystemNotifScene.PAYOUT_SUCCESS_NOT_FILL_MOTHER_LAST_NAME, param);
    } else {
      notificationService.pushSystemNotif(SystemNotifScene.PAYOUT_SUCCESS, param);
    }
    threadTransactionalModel.transaction(configuration -> {
      loanAccountModel.findByIdForUpdateOrThrow(accountId);
      loanLotteryService.sendLoanLotteryChanceForIDNOrThrow(
          orderVO.sdkType,
          orderVO.accountId,
          1,
          LoanLotteryChanceType.AUTO,
          LoanLotteryChanceActionType.PAYOUT_SUCCESS,
          orderVO.id.toString(),
          LoanLotteryWinningType.PAYMENT_SUCCESS);
    });
    threadTransactionalModel.transaction(configuration -> {
      loanAccountModel.findByIdForUpdateOrThrow(accountId);
      loanActivityService.sendLoanActivityChanceForIDNOrThrow(
          orderVO.accountId,
          cashLoanOrderVO.principal,
          LoanLotteryChanceType.AUTO,
          LoanLotteryChanceActionType.PAYOUT_SUCCESS,
          orderVO.id.toString(),
          LoanLotteryWinningType.PAYMENT_SUCCESS_AMOUNT);
    });
  }

  private void doUseCutInterestCoupon(Long orderId, Long accountId, OrderInstalment orderInstalment, CashLoanOrderVO orderVO) {
    LoanUserCouponVO couponVO = loanUserCouponService.getAtPresentPendingCoupon(orderId, accountId, LoanCouponUsageType.CUT_INTEREST);
    if (Objects.isNull(couponVO)) {
      return;
    }
    CutInterestVO cutInterestVO = ((LoanCutInterestCouponVO) couponVO).genCutInterestOIPlan(orderInstalment, orderVO.sdkType);
    String abResult = expDiversionClient.getString("technology-lending-abroad-loan_all-cut_coupon_post_use", ExpUser.builder().userId(orderVO.userId).versionBuild(99999L).build(), "A");

    Boolean overdueCanUse = ((LoanCutInterestCouponVO) couponVO).overdueCanUse;
    if (StringUtils.equals(abResult, "A") || Objects.isNull(overdueCanUse) || overdueCanUse) {
      orderInstalment.instalmentVOS.forEach(vo -> repaidCutInterest(vo, cutInterestVO.instalmentVOMap.get(vo.index), couponVO.id));
      loanUserCouponService.useCutInterestCoupon(couponVO, cutInterestVO.orderVO.deductPostInterest);
      loanUserCouponService.updateMutexCouponStatus(orderVO.userId, orderVO.sdkType, couponVO.id);
    } else {
      // 逾期不可用
      instalmentCutInterestCouponDeductDetailService.storeDeductInfo(orderInstalment, cutInterestVO, couponVO.id);
      loanUserCouponService.useCutInterestCoupon(couponVO, BigDecimal.ZERO);
    }
  }

  @RunInTransaction
  public void useCutInterestCouponForOverdueCannotUse(EcCurrency currency, InstalmentCutInterestCouponDeductDetailVO couponDeductDetailVO, Long transactionTime) {
    repayByNotNormalType(
        couponDeductDetailVO.orderId,
        couponDeductDetailVO.loanAccountId,
        Collections.singletonList(couponDeductDetailVO.instalmentId),
        CurrencyAmount.fromYuan(currency, couponDeductDetailVO.deductAmount),
        CashLoanRepaymentType.DEDUCT_COUPON,
        "",
        couponDeductDetailVO.loanUserCouponId
    );
    instalmentCutInterestCouponDeductDetailService.updateDeductStatus(couponDeductDetailVO, transactionTime);
    loanUserCouponService.useCutInterestCoupon(couponDeductDetailVO.loanUserCouponId, couponDeductDetailVO.deductAmount);
  }

  private void generateLoanAgreement(CashLoanOrderVO orderVO) {
    VidaCreateParam vidaCreateParam = VidaCreateParam.from(BusinessType.ORDER_CHECK, AgreementType.LOAN_ORDER_CHECK,
        orderVO.id, orderVO.userId, YqgHashids.encode(orderVO.id), orderVO.sdkType);
//    vidaAgreementHelper.tryInsertVidaAgreementFileNew(vidaCreateParam);
    elecSignatureService.insertAgreementFile(vidaCreateParam);
    agreementPlatformService.publishLoanPayoutSignEvent(orderVO);
  }

  private boolean filledMotherLastName(Long accountId) {
    MongoLoanAccountExtraInfoPojo accountExtraInfoPojo = loanAccountExtraInfoService.getExtraInfoPojo(accountId);
    if (accountExtraInfoPojo != null && StringUtils.isNotEmpty(accountExtraInfoPojo.motherLastName)) {
      return true;
    }
    IndonesiaBaseIdInfo indonesiaBaseIdInfo = (IndonesiaBaseIdInfo) loanAccountDetailsService.getAuthFinishedDetailsVoByAccountId(accountId).detailsPojo.idInfo;
    if (StringUtils.isNotEmpty(indonesiaBaseIdInfo.motherLastName)) {
      return true;
    }
    return false;
  }

  private boolean needRepaidCut(CashLoanOrderVO order) {
    return !BigDecimalHelper.equals(order.interest, BigDecimal.ZERO);
  }

  @RunInTransaction
  private void repaidCut(Long instalmentId) {
    CashLoanInstalmentVO vo = ecOrderService.getInstalmentVO(instalmentId);
    CurrencyAmount cutAmount = CurrencyAmount.fromYuan(vo.currency, vo.interest);
    EcCurrency currency = vo.currency;
    calcFeeEventModel.insert(
        vo.loanAccountId,
        vo.orderId,
        instalmentId,
        CalcFeeUtil.getOwedPrincipal(vo),
        currency,
        vo.interest,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO
    );
    repayByNotNormalType(
        vo.orderId,
        vo.loanAccountId,
        Collections.singletonList(instalmentId),
        cutAmount,
        CashLoanRepaymentType.CUT,
        "",
        null
    );
  }

  @RunInTransaction //此时前置利息还完了，刚好抵扣后置利息
  private void repaidCutInterest(CashLoanInstalmentVO instalmentVO, CutInterestInstalmentVO cutInterestInstalmentVO, Long couponId) {
    //如果某一期抵扣金额为0，则直接返回
    if (BigDecimalHelper.compareTo(cutInterestInstalmentVO.deductPostInterest, ZERO) == 0) {
      return;
    }
    CurrencyAmount cutAmount = CurrencyAmount.fromYuan(instalmentVO.currency, cutInterestInstalmentVO.deductPostInterest);
    repayByNotNormalType(
        instalmentVO.orderId,
        instalmentVO.loanAccountId,
        Collections.singletonList(instalmentVO.id),
        cutAmount,
        CashLoanRepaymentType.DEDUCT_COUPON,
        "",
        couponId
    );
  }

  private void onPayoutFailed(Long orderId, PaymentErrorCode errorCode) {
    if (errorCode.shouldRetry) {
      // 暂时维持现有逻辑
      Long failedTimes = cashLoanPayoutTimesLoader.getOrDefault(orderId, 0L);
      CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
      if (
          failedTimes >= cashLoanConfig.maxPayoutRetryTimesPerOrder()
          || Clock.now() > orderVO.timeCreated + cashLoanConfig.getRetryPayoutMaxDaysSinceCreated() * Clock.MILLS_PER_DAY
          || cashLoanConfig.isPayoutRetryBlocked(orderId)
      ) {
        rejectOrderWithMsg(orderId, CashLoanOrderRejectReason.PAYOUT_FAILED);
      }
    } else {
      rejectOrderWithMsg(orderId, CashLoanOrderRejectReason.PAYOUT_FAILED);
    }
  }

  public boolean checkRetryPayout(Long orderId, PaymentErrorCode errorCode) {
    if (Boolean.TRUE.equals(errorCode.shouldRetry)) {
      Long failedTimes = cashLoanPayoutTimesLoader.getOrDefault(orderId, 0L);
      CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
      return failedTimes < cashLoanConfig.maxPayoutRetryTimesPerOrder()
          && Clock.now() <= orderVO.timeCreated + cashLoanConfig.getRetryPayoutMaxDaysSinceCreated() * Clock.MILLS_PER_DAY
          && !cashLoanConfig.isPayoutRetryBlocked(orderId)
          ;
    } else {
      return false;
    }
  }

  public CashLoanOrderVO createReloanOrder(
      Long accountId,
      PaymentCredential paymentCredential,
      BigDecimal principal,
      LoanProductConfigVO productVO,
      OrderInstalmentPlan plan,
      Long build,
      SourceType sourceType,
      CreateOrderScene scene,
      Long preOrderId) {
    dailyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_DAILY, accountId, cashLoanConfig.maxDailyLoanTimes());
    monthlyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_MONTHLY, accountId, cashLoanConfig.maxMonthlyLoanTimes());
    Long orderId = threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord loanAccountRecord = accountModel.findByIdForUpdateOrThrow(accountId);
      CashLoanOrderVO latestOrderVO = ecOrderService.getLatestOrderVO(accountId);
      EcAsserts.assertTrue(ecOrderService.existOrder(accountId), "loanAccountId: {} is not reloan", accountId);
      SDKType sdkType = productVO.sdkType;
      boolean isMultiLoan = multiLoanStatusService.hasMultiLoanQualify(accountId);
      // 检查用户，金额以及贷款时间是否符合要求
      assertUserCanCreateOrder(loanAccountRecord, principal, productVO.id, isMultiLoan);
      CashLoanOrderVO order = isMultiLoan
          ? ecOrderService.createMultiOrder(accountId, paymentCredential, plan, sourceType, build)
          : ecOrderService.createReservedOrder(accountId, paymentCredential, plan, sourceType, build);
      processHandWrittenSignature(scene, order);
      // 用户第一笔订单由于打款失败被拒绝，重新下单走到这时，isReloan为false
      boolean isReloan = loanAccountService.isReloan(accountId);
      processWhenLoanTimesGreaterThanZeroV2(accountId, latestOrderVO, build, sdkType, order.id, sourceType, isReloan, isMultiLoan, preOrderId);
      return order.id;
    });
    return ecOrderService.getOrderVO(orderId);
  }

  private void processWhenLoanTimesGreaterThanZeroV2(Long accountId,
                                                     CashLoanOrderVO latestOrderVO,
                                                     Long build,
                                                     SDKType sdkType,
                                                     Long newOrderId,
                                                     SourceType sourceType,
                                                     boolean isReloan,
                                                     boolean isMultiLoan,
                                                     Long preOrderId) {
    boolean lastedOrderRejected = riskRejectOrderService.isUserInOrderRejectProcessWithOrderId(accountId, latestOrderVO.id);
    if (!lastedOrderRejected && isReloan) {
      loanAccountService.submitReloanCredits(accountId, newOrderId, sdkType, sourceType, build, preOrderId);
      return;
    }
    if (!lastedOrderRejected && !isReloan) {
      loanAccountService.submitSecondCredits(accountId, newOrderId, sdkType, sourceType, build, preOrderId);
      return;
    }
    log.info("lasted order is rejected, skip submit reloan credits, accountId: {}", accountId);
    ecOrderService.updateTraceId(newOrderId, latestOrderVO.traceId);
    if (isMultiLoan) {
      multiLoanStatusService.updateStatus(accountId, MultiLoanStatusChangeSource.MANUAL_ORDER);
      // 修改续借状态--因为没有进行风控，所以此处需要先改成MANUAL_ORDER，再改成ORDER_RISK_ACCEPT
      multiLoanStatusService.updateStatus(accountId, MultiLoanStatusChangeSource.ORDER_RISK_ACCEPT);
    }
    initOrCheckReservedOrderIfExistWithBuild(accountId, build);
  }

  /**
   * 需求背景，此次新增两个二次风控，需要针对对应的情况来决定是否走风控，以及走什么风控
   * 根据最后的风控类型riskType来确定当前的有效期内是否存在订单，不存在订单，需要走风控，存在订单，不走风控，直接下单
   *
   * @param lastOrderVo
   * @return
   */
  private boolean getNeedSubmitSecondCreditsForRiskRejectLatestOrder(CashLoanOrderVO lastOrderVo) {
    // 如果用户没有额度测算记录
    LoanUserRiskTraceRecord userRiskTraceRecord = loanUserRiskTraceModel.findLastedByAccountIdAndRiskType(lastOrderVo.accountId, LoanUserRiskType.getLoanCalcRiskType());
    if (userRiskTraceRecord == null) {
      throw EcException.error("do not have  calc risk type, loanAccountId is {}", lastOrderVo.accountId);
    }
    // 看下最新的额度测算或者首贷重审额度测算是不是已经超过配置时间
    CashLoanCalcCreditsVO cashLoanCalcCreditsAndRiskTypeVO = cashLoanCalcCreditsService.getCalcCreditsStatus(lastOrderVo.accountId);
    EcAsserts.assertTrue(cashLoanCalcCreditsAndRiskTypeVO.calcCreditsStatus == CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, "calc credits expired ,loanAccountId is {} ", lastOrderVo.accountId);
    //最后一次的风控记录的riskType的创建时间比订单时间大，证明当前有效期内没有订单，需要走风控
    if (userRiskTraceRecord.getTimeCreated() > lastOrderVo.timeCreated) {
      return true;
    }

    CashLoanOrderAdditionalInfoVO latestOrderAdditionalInfoVO = cashLoanOrderAdditionalInfoService.findByOrderIdAndType(lastOrderVo.id, REJECT_REASON);
    CashLoanOrderRejectReason rejectReason = CashLoanOrderRejectReason.valueOf(latestOrderAdditionalInfoVO.info);
    return CashLoanOrderRejectReason.ORDER_REJECT_REASON_LIST.contains(rejectReason);
  }

  public CashLoanOrderVO createSecondCreditsOrder(
      Long accountId,
      PaymentCredential paymentCredential,
      BigDecimal principal,
      LoanProductConfigVO productVO,
      OrderInstalmentPlan plan,
      SourceType sourceType,
      Long build,
      Long preOrderId
  ) {
    dailyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_DAILY, accountId, cashLoanConfig.maxDailyLoanTimes());
    monthlyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_MONTHLY, accountId, cashLoanConfig.maxMonthlyLoanTimes());
    Long orderId = threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord loanAccountRecord = accountModel.findByIdForUpdateOrThrow(accountId);
      EcAsserts.assertTrue(!ecOrderService.existOrder(accountId),
          "should not create second credits order for loanAccountId: {}", accountId);
      // 检查用户，金额以及贷款时间是否符合要求
      assertUserCanCreateOrder(loanAccountRecord, principal, productVO.id, false);
      CashLoanOrderVO order = ecOrderService.createReservedOrder(accountId, paymentCredential, plan, sourceType, build);
      loanAccountService.submitSecondCredits(accountId, order.id, plan.order.sdk, sourceType, build, preOrderId);
      return order.id;
    });
    return ecOrderService.getOrderVO(orderId);
  }

  public CashLoanOrderVO createReloanOrderWithCoupon(
      Long accountId,
      PaymentCredential paymentCredential,
      BigDecimal principal,
      SDKType sdkType,
      Long productId,
      Long couponId,
      Long build,
      SourceType sourceType,
      CreateOrderScene scene,
      Boolean needCreateActivityOrder,
      Long preOrderId) {
    LoanProductConfigVO productVO = productConfigService.getProductVO(productId);
    OrderInstalmentPlan oiPlan = OIPlanFactory.getInstance(sdkType).genPlan(principal, productVO);
    return threadTransactionalModel.transactionResult(configuration -> {
      CashLoanOrderVO orderVO = createReloanOrder(accountId, paymentCredential, principal, productVO, oiPlan, build, sourceType, scene, preOrderId);
      addCreateActivityOrderAdditionalInfo(orderVO.id, orderVO.accountId, needCreateActivityOrder);
      bindCutInterestCoupon(couponId, orderVO, principal);
      return orderVO;
    });
  }

  public CashLoanOrderVO createSecondCreditsOrderWithCoupon(
      Long accountId,
      PaymentCredential paymentCredential,
      BigDecimal principal,
      SDKType sdkType,
      Long productId,
      Long couponId,
      SourceType sourceType,
      CreateOrderScene scene,
      Long build,
      Boolean needCreateActivityOrder,
      Long preOrderId) {
    LoanProductConfigVO productVO = productConfigService.getProductVO(productId);
    OrderInstalmentPlan oiPlan = OIPlanFactory.getInstance(sdkType).genPlan(principal, productVO);
    return threadTransactionalModel.transactionResult(configuration -> {
      CashLoanOrderVO orderVO = createSecondCreditsOrder(accountId, paymentCredential, principal, productVO, oiPlan, sourceType, build, preOrderId);
      addCreateActivityOrderAdditionalInfo(orderVO.id, orderVO.accountId, needCreateActivityOrder);
      bindCutInterestCoupon(couponId, orderVO, principal);
      //由于创建的是reverse订单，所以可以在这个地方创建手写签名记录
      processHandWrittenSignature(scene, orderVO);
      return orderVO;
    });
  }

  public CashLoanOrderVO createInitOrder(
      Long accountId,
      PaymentCredential paymentCredential,
      BigDecimal principal,
      LoanProductConfigVO productVO,
      OrderInstalmentPlan oiPlan,
      SourceType sourceType,
      Long build) {
    dailyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_DAILY, accountId, cashLoanConfig.maxDailyLoanTimes());
    monthlyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_MONTHLY, accountId, cashLoanConfig.maxMonthlyLoanTimes());
    return threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord loanAccountRecord = accountModel.findByIdForUpdateOrThrow(accountId);
      assertUserCanCreateOrder(loanAccountRecord, principal, productVO.id, false);
      return ecOrderService.createInitOrder(accountId, paymentCredential, oiPlan, sourceType, build);
    });
  }

  public CashLoanOrderVO createInitOrCheckOrder(
      Long accountId,
      PaymentCredential paymentCredential,
      BigDecimal principal,
      Long productId,
      Long build,
      SourceType sourceType
  ) {
    dailyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_DAILY, accountId, cashLoanConfig.maxDailyLoanTimes());
    monthlyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_MONTHLY, accountId, cashLoanConfig.maxMonthlyLoanTimes());
    LoanProductConfigVO productVO = productConfigService.getProductVO(productId);
    OrderInstalmentPlan oiPlan = OIPlanFactory.getInstance(productVO.sdkType).genPlan(principal, productVO);
    return threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord loanAccountRecord = accountModel.findByIdForUpdateOrThrow(accountId);
      return processCreateInitOrCheckOrder(
          loanAccountRecord,
          productVO,
          paymentCredential,
          oiPlan,
          build,
          sourceType,
          null
      );
    });
  }

  public CashLoanOrderVO createInitOrCheckOrderWithCoupon(
      Long accountId,
      PaymentCredential paymentCredential,
      BigDecimal principal,
      Long productId,
      Long build,
      Long couponId,
      SourceType sourceType,
      CreateOrderScene scene,
      Boolean needCreateActivityOrder) {
    dailyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_DAILY, accountId, cashLoanConfig.maxDailyLoanTimes());
    monthlyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_MONTHLY, accountId, cashLoanConfig.maxMonthlyLoanTimes());
    LoanProductConfigVO productVO = productConfigService.getProductVO(productId);
    OrderInstalmentPlan oiPlan = OIPlanFactory.getInstance(productVO.sdkType).genPlan(principal, productVO);
    return threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord loanAccountRecord = accountModel.findByIdForUpdateOrThrow(accountId);
      CashLoanOrderVO orderVO = processCreateInitOrCheckOrder(
          loanAccountRecord,
          productVO,
          paymentCredential,
          oiPlan,
          build,
          sourceType,
          scene
      );
      addCreateActivityOrderAdditionalInfo(orderVO.id, orderVO.accountId, needCreateActivityOrder);
      bindCutInterestCoupon(couponId, orderVO, principal);
      return orderVO;
    });
  }

  //下单后自动签名，调用此方法必须在check开始之前
  public void processHandWrittenSignature(CreateOrderScene scene, CashLoanOrderVO orderVO) {
    if (scene == CreateOrderScene.AGREE_AGREEMENT) {
      handWrittenSignatureService.createSignedRecordWhenCreateLoanOrder(orderVO);
    }
  }

  @RunInTransaction
  private CashLoanOrderVO processCreateInitOrCheckOrder(
      LoanAccountRecord accountRecord,
      LoanProductConfigVO productVO,
      PaymentCredential paymentCredential,
      OrderInstalmentPlan oiPlan,
      Long build,
      SourceType sourceType,
      CreateOrderScene scene) {
    assertUserCanCreateOrder(accountRecord, oiPlan.order.principal, productVO.id, false);
    CashLoanOrderVO orderVO = ecOrderService.createReservedOrder(accountRecord.getId(), paymentCredential, oiPlan, sourceType, build);
    processHandWrittenSignature(scene, orderVO);
    triggerLoanNextCheckStep(orderVO.accountId, orderVO, build);
    return queryCheck(orderVO);
  }

  public CashLoanOrderVO initOrCheckReservedOrderIfExist(Long accountId) {
    return initOrCheckReservedOrderIfExistWithBuild(accountId, null);
  }

  public CashLoanOrderVO initOrCheckReservedOrderIfExistWithBuild(Long accountId, Long build) {
    CashLoanOrderVO orderVO = threadTransactionalModel.transactionResult(configuration -> {
      accountModel.findByIdForUpdateOrThrow(accountId);
      CashLoanOrderVO vo = ecOrderService.getReservedOrder(accountId);
      if (vo == null) {
        return null;
      }
      try {
        assertOrderStatus(accountId, vo);
      } catch (EcException e) {
        if (e.exceptionType == EcExceptionType.LOAN_ORDER_NOT_SATISFY_PRODUCT) {
          // 用户借的钱大于风控输出额度  或者用户的产品ID 发生了变化
          log.warn("loan order not satisfy product, accountId: {}, order: {}", accountId, vo, e);
          if (cashLoanOrderConfig.getRejectOrderSceneSwitchVOV2() == OrderRejectDealScene.THROW_EXCEPTION) {
            throw e;
          }
          if (cashLoanOrderConfig.getRejectOrderSceneSwitchVOV2() == OrderRejectDealScene.REJECT_ORDER) {
            rejectReservedOrderIfExistWithMsg(accountId, CashLoanOrderRejectReason.INSUFFICIENT_QUOTA);
            return ecOrderService.getOrderVO(vo.id);
          }
        } else {
          throw e;
        }
      }
      triggerLoanNextCheckStep(vo.accountId, vo, build);
      return vo;
    });
    if (orderVO == null) {
      return null;
    }
    return ecOrderService.getOrderVO(orderVO.id);
  }

  public void pushMessageForInsufficientQuota(CashLoanOrderVO cashLoanOrderVO) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = cashLoanOrderVO.traceId == null ? null : loanUserRiskTraceService.findByTraceIdOrThrow(cashLoanOrderVO.traceId);
    NotifCreditAndOrderParam param = new NotifCreditAndOrderParam(cashLoanOrderVO.userId, cashLoanOrderVO.sdkType, cashLoanOrderVO.id, cashLoanOrderVO.traceId);
    if (loanUserRiskTraceVO == null || LoanUserRiskType.getLoanOrderRiskTypes().contains(loanUserRiskTraceVO.riskType)) {
      notificationService.pushSystemNotif(SystemNotifScene.LOAN_USER_CREDITS_DECREASE, param);
    } else if (loanUserRiskTraceVO.riskType == LoanUserRiskType.RELOAN) {
      notificationService.pushSystemNotif(SystemNotifScene.RELOAN_USER_CREDITS_DECREASE, param);
    }
  }

  public void queryAllCheck() {
    List<CashLoanOrderVO> orderVOs = ecOrderService.fetchAllByStatuses(CashLoanOrderStatus.CHECK);
    for (CashLoanOrderVO orderVO : orderVOs) {
      try {
        queryCheck(orderVO);
      } catch (Exception ex) {
        log.error("hit error while order query check result, order id:{}", orderVO.id, ex);
      }
    }
  }

  //TODO(gxs, T000000)等check都迁移到mq处理后进行优化
  public CashLoanOrderVO queryCheck(CashLoanOrderVO orderVO) {
    BizCheckStatus status = getLoanOrderFinalCheckStatus(orderVO.id);
    switch (status) {
      case INIT:
      case PENDING:
        return orderVO;
      case SUCCESS:
        return initCheckedOrder(orderVO.accountId, orderVO.id);
      case EXPIRED:
      case FAILED:
        return cancelOrderWithMsg(orderVO.accountId, orderVO.id);
      default:
        throw EcException.error("unexpected status for order check,status is {},order id is {}", status, orderVO.id);
    }
  }

  private BizCheckStatus getLoanOrderFinalCheckStatus(Long orderId) {
    boolean failed = rejectCashLoanOrderService.getOrderCanceledByAdmin(orderId);
    if (failed) {
      log.info("order is rejected by admin. orderId is {}", orderId);
      return BizCheckStatus.FAILED;
    }
    return checkListService.getFinalCheckStatus(orderId, BusinessType.ORDER_CHECK);
  }

  public Boolean bizCheckSuccess(Long orderId, Long accountId) {
    BizCheckStatus finalCheckStatus = getLoanOrderFinalCheckStatus(orderId);
    return BizCheckStatus.SUCCESS == finalCheckStatus;
  }

  public CashLoanOrderVO initCheckedOrder(Long accountId, Long orderId) {
    return threadTransactionalModel.transactionResult(configuration -> {
      accountModel.findByIdForUpdateOrThrow(accountId);
      CashLoanOrderVO vo = ecOrderService.getOrderVO(orderId);
      if (CashLoanOrderStatus.CHECK != vo.status) {
        log.warn("订单状态应为CHECK, 实际为: {}, orderId is {}", vo.status, orderId);
        return vo;
      }
      return ecOrderService.initCheckOrder(orderId);
    });
  }

  public Long getCheckOrderBuildInfo(Long accountId, CashLoanOrderVO orderVO) {
    //贷超产品不含build
    if (orderVO.sdkType.isIdnLoanSDKType() && orderVO.sourceType.isApiChannelSourceType()) {
      //TODO(yuchenghuang, T00000)后面看下版本号处理
      return Long.MAX_VALUE;
    }
    LoanCreateOrderInfoPojo pojo = getLoanCreateOrderInfoPojo(accountId, orderVO.id);
    if (pojo == null) {
      log.error("user create order info pojo is null, account id: {}, orderId: {}", accountId, orderVO.id);
      return null;
    }
    return pojo.build;
  }

  public LoanCreateOrderInfoPojo getLoanCreateOrderInfoPojo(Long accountId, Long orderId) {
    LoanAccountAdditionalInfoRecord additionalInfoRecord = additionalInfoModel.find(
        accountId,
        LoanAccountAdditionalTypeEnum.ORDER_INFO_ON_CREATE_ORDER,
        orderId.toString()
    );
    if (additionalInfoRecord == null || StringUtils.isBlank(additionalInfoRecord.getValue())) {
      return null;
    }
    return mongoLoanOrderInfoModel.findByObjectIdOrThrow(additionalInfoRecord.getValue());
  }

  public void rejectReservedOrderIfExistWithMsg(Long accountId, CashLoanOrderRejectReason rejectReason) {
    CashLoanOrderVO orderVO = ecOrderService.getReservedOrder(accountId);
    if (orderVO == null) {
      return;
    }
    doRejectOrder(orderVO.id, rejectReason);
    if (rejectReason == CashLoanOrderRejectReason.INSUFFICIENT_QUOTA) {
      //todo ltb 迁移走
      pushMessageForInsufficientQuota(orderVO);
    }
  }

  public void rejectCheckOrder(CashLoanOrderVO cashLoanOrderVO, CashLoanOrderRejectReason rejectReason) {
    EcAsserts.assertTrue(cashLoanOrderVO.status == CashLoanOrderStatus.CHECK, "order status is not check, order id is {}", cashLoanOrderVO.id);
    doRejectOrder(cashLoanOrderVO.id, rejectReason);
  }

  // 检查该用户是否有未还完的订单
  @RunInTransaction
  private void assertUserAllOrderFinished(Long accountId, boolean isMultiLoan) {
    //循环额度只要非在途即可
    boolean revolvingLoanUser = loanAccountRevolvingService.checkUserInRevolvingLoanProcess(accountId);
    if (revolvingLoanUser) {
      if (ecOrderService.existOrder(accountId, CashLoanOrderStatus.UNDONE_STATUSES_WITHOUT_READY)) {
        throw EcException.warn(EcExceptionType.CASH_LOAN_ACCOUNT_HAS_INIT_ORDER, TT.gen("您有一笔借款正在处理中，请等待处理完成再借下一笔"));
      }
      return;
    }
    if (!isMultiLoan) {
      if (ecOrderService.countUncompletedOrder(accountId) > 0) {
        throw EcException.warn(EcExceptionType.CASH_LOAN_ACCOUNT_HAS_INIT_ORDER, TT.gen("您有一笔借款正在处理中，请等待处理完成再借下一笔"));
      }
    } else {
      int uncompletedOrder = ecOrderService.countOrder(accountId, CashLoanOrderStatus.UNDONE_STATUSES_WITHOUT_READY);
      if (uncompletedOrder > 0) {
        throw EcException.warn(EcExceptionType.CASH_LOAN_ACCOUNT_HAS_INIT_ORDER, TT.gen("您有一笔借款正在处理中，请等待处理完成再借下一笔"));
      }
    }
  }

  @RunInTransaction
  private void assertUserCanCreateOrder(LoanAccountRecord accountRecord,
                                        BigDecimal principal,
                                        Long productId,
                                        boolean isMultiLoan) {
    //TODO(T00000, cwb)后续迁移到user表上
    Boolean deleted = BooleanType.fromCharCode(accountRecord.getDeleted()).bool;
    if (deleted) {
      log.warn("此账户已被注销, user_id: {}", accountRecord.getUserId());
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("此账户已被注销"));
    }
    LoanAssertion.assertDecimalPostive(principal);
    LoanAssertion.assertDecimalInteger(principal);
    // 取record
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(accountRecord.getId());
    //检查授信状态
    CashLoanAssertion.assertCreditsStatus(creditsInfoRecord, LoanCreditsStatus.ACCEPTED);
    //如果是复贷检查复贷状态
    if (creditsInfoRecord.getReloanStatus() != null) {
      CashLoanAssertion.assertReloanCreditsStatus(creditsInfoRecord, LoanCreditsStatus.ACCEPTED);
    }
    // 检查该用户是否有状态为READY或INIT的订单
    assertUserAllOrderFinished(accountRecord.getId(), isMultiLoan);
    //检查用户对产品的权限
    LoanUserTypeVO userTypeVO = loanUserTypeService.fromUserTypeCode(accountRecord.getUserType());
    if (LoanUserTypeTag.PLATFORMTYPE_INIT_USERTYPE.contains(userTypeVO.tag)) {
      throw EcException.error(EcExceptionType.CASH_LOAN_LOAN_USER_TYPE_MISMATCH_PRODUCT, "用户类型不符合产品的申请权限");
    }
    LoanProductConfigVO loanProductConfigVO = productConfigService.getProductVO(productId);
    checkProductInvalid(loanProductConfigVO);
    //检查是否符合产品要求
    BigDecimal totalRemainCredits = creditsService.getTotalRemainCredits(accountRecord.getId());
    if (!BigDecimalHelper.lessThanOrEqual(principal, loanProductConfigVO.maxCredits.getAmountInYuan()) ||
        !BigDecimalHelper.greaterThanOrEqual(principal, loanProductConfigVO.minCredits.getAmountInYuan()) ||
        !BigDecimalHelper.lessThanOrEqual(principal, totalRemainCredits)
    ) {
      log.info("assertUserCanCreateOrder productId:{}, principal:{}, product maxCredits:{}, minCredits:{}, totalRemainCredits:{}",
          productId, principal, loanProductConfigVO.maxCredits.getAmountInYuan(), loanProductConfigVO.minCredits.getAmountInYuan(), totalRemainCredits);
      throw EcException.warn(EcExceptionType.LOAN_ORDER_NOT_SATISFY_PRODUCT, TT.gen("不满足该产品的条件"));
    }
    loanAccountRevolvingService.revolvingLoanUserOrderCheck(accountRecord.getId());
  }

  private void checkProductInvalid(LoanProductConfigVO loanProductConfigVO) {
    Long productExpiredTime = riskConfig.getProductExpiredTime();
    boolean isInvalid = !loanProductConfigVO.enabled && (loanProductConfigVO.timeUpdated + productExpiredTime) < Clock.now();
    if (isInvalid || riskConfig.getInvalidProductIdList().contains(loanProductConfigVO.id)) {
      throw EcException.warn(EcExceptionType.LOAN_ORDER_NOT_SATISFY_PRODUCT, TT.gen("你选择的借款产品已下线，请在当前页面下拉刷新后选择新的借款产品"));
    }
  }

  @RunInTransaction
  void bindCutInterestCoupon(Long couponId, CashLoanOrderVO orderVO, BigDecimal principal) {
    if (couponId == null) {
      return;
    }
    loanUserCouponService.bindCutInterestCoupon(orderVO, couponId, principal);
  }

  @RunInTransaction
  public void assertOrderStatus(Long accountId, CashLoanOrderVO orderVO) {
    LoanAssertion.assertDecimalPostive(orderVO.principal);
    //检查授信状态
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(accountId);
    LoanProductConfigVO product = productConfigService.getProductVO(orderVO.productId);
    CashLoanAssertion.assertCreditsStatus(creditsInfoRecord, LoanCreditsStatus.ACCEPTED);
    boolean checkProductId = riskRejectOrderService.checkOrderProductId(accountId, orderVO.id);
    //用户当前可见产品不包含订单的产品 ID，抛出异常
    if (!checkProductId && cashLoanOrderConfig.getOrderProductIdCheckSwitch()) {
      log.info("因为用户产品ID不包含订单产品ID，不满足该产品的条件, orderProductId:{}, orderProductIdInAccount:{}", orderVO.productId, product.id);
      throw EcException.warn(EcExceptionType.LOAN_ORDER_NOT_SATISFY_PRODUCT, TT.gen("因为用户产品ID不包含订单产品ID，不满足该产品的条件"));
    }
    // 检查用户贷款额度是否超出范围
    //已有订单的情况下，使用下单时候的临时额度和个人额度之和作为比较
    BigDecimal totalCredits = creditsService.getTotalCreditsWhenPlaceOrder(orderVO.userId, orderVO.accountId, orderVO.timeCreated);
    if (!checkPrincipalInProductRange(orderVO.principal, product, totalCredits)) {
      log.info("不满足该产品的条件, orderProductId:{}, orderProductIdInAccount:{}", orderVO.productId, product.id);
      throw EcException.warn(EcExceptionType.LOAN_ORDER_NOT_SATISFY_PRODUCT, TT.gen("不满足该产品的条件"));
    }
    checkExistOrdersStatus(accountId);
  }

  /**
   * 下单的时候检查用户的额度和产品信息是否符合条件，不符合的要刷新 APP 页面
   */
  public void assertOrderStatusInCreateOrderRequest(Long accountId, Long productId, BigDecimal principal) {
    boolean checkProductId = riskRejectOrderService.checkOrderProductIdValid(accountId, productId);
    if (!checkProductId) {
      log.info("因为用户产品ID不包含订单产品ID，不满足该产品的条件, orderProductId:{}", productId);
      if (!cashLoanOrderConfig.getAssertOrderStatusInCreateOrderRequestSwitch()) {
        return;
      }
      throw EcException.warn(EcExceptionType.CASH_LOAN_CREDITS_EXPIRED, TT.gen("因为用户产品ID不包含订单产品ID，不满足该产品的条件"));
    }
    LoanProductConfigVO product = productConfigService.getProductVO(productId);
    LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(accountId);
    BigDecimal totalCredits = creditsService.getTotalCreditsWhenPlaceOrder(loanAccountVO.userId, accountId, Clock.now());
    if (!checkPrincipalInProductRange(principal, product, totalCredits)) {
      log.info("不满足该产品的条件, accountId:{}, orderProductIdInAccount:{}", accountId, productId);
      if (!cashLoanOrderConfig.getAssertOrderStatusInCreateOrderRequestSwitch()) {
        return;
      }
      throw EcException.warn(EcExceptionType.CASH_LOAN_CREDITS_EXPIRED, TT.gen("不满足该产品的条件"));
    }
  }

  /**
   * 检查借款金额是否在产品允许范围内
   *
   * @param principal    借款金额
   * @param product      产品配置
   * @param totalCredits 用户可用总额度
   * @return true表示满足条件，false表示不满足条件
   */
  private boolean checkPrincipalInProductRange(BigDecimal principal, LoanProductConfigVO product, BigDecimal totalCredits) {
    return BigDecimalHelper.lessThanOrEqual(principal, product.maxCredits.getAmountInYuan()) &&
        BigDecimalHelper.greaterThanOrEqual(principal, product.minCredits.getAmountInYuan()) &&
        BigDecimalHelper.lessThanOrEqual(principal, totalCredits);
  }

  //检查用户已有订单的状态。
  // 1、是否只有一个reserve/check-order,
  // 2、ready-order存在时是否满足续借申请条件，以及readyOrder数量是否达到上限
  private void checkExistOrdersStatus(Long accountId) {

    MultiLoanStatus currentMultiLoanStatus = multiLoanStatusService.getStatusOrNull(accountId);
    List<CashLoanOrderVO> orderVOs = ecOrderService.findByAccountIdAndStatuses(accountId, CashLoanOrderStatus.UNDONE_STATUSES);
    Map<CashLoanOrderStatus, List<CashLoanOrderVO>> statusListMap = orderVOs.stream().collect(Collectors.groupingBy(vo -> vo.status));
    //循环贷用户不校验前置订单状态，下单的时候已经校验了
    if (loanAccountRevolvingService.checkUserInRevolvingLoanProcess(accountId)) {
      return;
    }
    if (CollectionUtils.isNotEmpty(statusListMap.get(CashLoanOrderStatus.INIT))) {
      throw EcException.warn(EcExceptionType.CASH_LOAN_ACCOUNT_HAS_INIT_ORDER, TT.gen("您有一笔借款正在处理中，请等待处理完成再借下一笔"));
    }
    if (CollectionUtils.isNotEmpty(statusListMap.get(CashLoanOrderStatus.READY))) {
      if (MultiLoanStatus.ORDER_ACCEPTED != currentMultiLoanStatus && MultiLoanStatus.INVALID != currentMultiLoanStatus) {
        throw EcException.warn(EcExceptionType.CASH_LOAN_ACCOUNT_HAS_READY_ORDER, TT.gen("用户续借订单初始化时续借状态不满足下单条件！当前续借状态为：{0}", currentMultiLoanStatus));
      }
    }
    if (CollectionUtils.size(statusListMap.get(CashLoanOrderStatus.CHECK)) > 1
        || CollectionUtils.size(statusListMap.get(CashLoanOrderStatus.RESERVE)) > 1
        || (CollectionUtils.size(statusListMap.get(CashLoanOrderStatus.CHECK)) + CollectionUtils.size(statusListMap.get(CashLoanOrderStatus.RESERVE))) > 1) {
      throw EcException.warn(EcExceptionType.CASH_LOAN_MORE_THAN_ONE_RESERVE_ORDER, TT.gen("鉴权保留订单数目大于一"));
    }
  }

  public List<RepaymentVO> getRepaymentsByOrderId(Long orderId) {
    return repaymentModel.findByOrderId(orderId)
        .stream()
        .map(RepaymentVO::from)
        .sorted((r2, r1) -> r1.id.compareTo(r2.id))
        .collect(Collectors.toList());
  }

  public List<RepaymentUnitVO> getRepaymentUnitsByRepaymentIds(Collection<Long> repaymentIds) {
    return repaymentUnitModel.findByRepaymentIds(repaymentIds).stream().map(RepaymentUnitVO::from).collect(Collectors.toList());
  }

  public List<RepaymentUnitVO> listRepaymentUnitsByRepaymentIds(Collection<Long> repaymentIds) {
    return repaymentUnitModel.findFullInfoByRepaymentIds(repaymentIds);
  }

  public List<RepaymentUnitVO> listRepaymentUnitsByRepaymentIdsForSingleOrder(Collection<Long> repaymentIds, Long orderId) {
    return repaymentUnitModel.findFullInfoByRepaymentIds(repaymentIds, orderId);
  }

  public List<RepaymentUnitVO> getRepaymentUnitsByInstalmentIdAndType(Long instalmentId, CashLoanRepaymentType type) {
    return repaymentUnitModel.getRepaymentUnitVOsByInstalmentIdAndType(instalmentId, type);
  }


  public List<CashLoanOrderDetailVO> searchCashLoanOrdersByConditions(CashLoanSearchCondition searchCondition) {
    return searchCashLoanOrdersByConditions(0, Integer.MAX_VALUE, searchCondition);
  }

  public List<CashLoanOrderDetailVO> searchCashLoanOrdersByConditions(Integer offset, Integer limit, CashLoanSearchCondition searchCondition) {
    List<Long> orderIds = ecOrderService.searchCashLoanOrdersByConditions(offset, limit, searchCondition);
    return getCashLoanOrderDetailVOsByOrderIds(orderIds);
  }

  public CashLoanOrderDetailVO getCashLoanOrderDetailVObyOrderId(Long orderId) {
    if (orderId == null) {
      return null;
    }
    List<CashLoanOrderDetailVO> orderDetailVOs = getCashLoanOrderDetailVOsByOrderIds(Collections.singletonList(orderId));
    return orderDetailVOs.isEmpty() ? null : orderDetailVOs.get(0);
  }

  public List<CashLoanOrderDetailVO> getCashLoanOrderDetailVOsByOrderIds(List<Long> orderIds) {
    List<CashLoanOrderDetailVO> orderDetailVOList = new ArrayList<>();
    Map<Long, CashLoanOrderVO> orderVOMap = ecOrderService.getOrderVOMap(orderIds);
    List<Long> accountIds = new ArrayList<>();
    orderIds.forEach(orderId -> accountIds.add(orderVOMap.get(orderId).accountId));
    Map<Long, LoanAccountRecord> accountRecordMap = accountModel.fetchMapByIds(accountIds);
    Map<Long, UserInfoVO> userInfoVOMap = userAdditionalInfoService.genLoanUserInfoVOMap(accountIds);
    Map<Long, List<RepaymentUnitVO>> repaymentUnitRecordsMap = getRepaymentUnitVOsByOrderIdsAndStatus(orderIds, CashLoanRepaymentStatus.SUCCEED);
    Map<Long, Integer> overdueDaysMap = ecOrderService.getMaxOverdueDays(orderIds);
    Map<Long, BigDecimal> couponDeductAmountMap = loanUserCouponService.getCouponAffectedMoneyToOrderMap(orderIds, LoanCouponUsageType.MONEY_OFF);
    orderIds.forEach(orderId -> {
      CashLoanOrderDetailVO orderDetailVO = new CashLoanOrderDetailVO();
      CashLoanOrderVO orderVO = orderVOMap.get(orderId);
      orderDetailVO.cashLoanOrderVO = orderVO;
      orderDetailVO.cashLoanOrderForViewVO = instalmentCutInterestCouponDeductDetailService.getViewOrder(orderVO);
      LoanAccountRecord loanAccountRecord = accountRecordMap.get(orderVO.accountId);
      UserInfoVO userInfoVO = userInfoVOMap.get(loanAccountRecord.getId());
      orderDetailVO.loanAccountVO = LoanAccountVO.from(userInfoVO, loanAccountRecord, loanUserTypeService.fromUserTypeCode(loanAccountRecord.getUserType()));
      List<RepaymentUnitVO> repaymentUnitRecords = repaymentUnitRecordsMap.get(orderId);
      if (repaymentUnitRecords != null) {
        orderDetailVO.repaymentUnitVOs = repaymentUnitRecords;
        RepaymentUnitVO repaymentUnitVO = repaymentUnitRecords.get(0);//取最后更新的还款记录
        orderDetailVO.lastRepaymentUpdateTime = repaymentUnitVO.repaymentTimeUpdated;
        //获取还款信息操作人姓名,如果为正常还款直接取借款人姓名,其他情况从ADMIN_USER表取
        CashLoanRepaymentType repaymentType = CashLoanRepaymentType.fromCode(repaymentUnitVO.type);
        switch (repaymentType) {
          case NORMAL:
          case CUT:
          case RESTRUCTURE:
            orderDetailVO.lastRepaymentOperatorName = userInfoVO.name;
            break;
          default:
            AdminUserRecord adminUserRecord = adminUserModel.findById(repaymentUnitVO.userOpt);
            orderDetailVO.lastRepaymentOperatorName = adminUserRecord == null ? "" : adminUserRecord.getFullName();
        }
      }
      Integer overdueDays = overdueDaysMap.get(orderId);
      orderDetailVO.isOverdue = overdueDays != null && overdueDays != 0;
      orderDetailVO.overdueDays = overdueDays;
      orderDetailVO.couponDeductTotalAmount = couponDeductAmountMap.get(orderId);
      orderDetailVOList.add(orderDetailVO);
    });
    return orderDetailVOList;
  }

  public Map<Long, List<RepaymentUnitVO>> getRepaymentUnitVOsByOrderIdsAndStatus(Collection<Long> orderIds, CashLoanRepaymentStatus status) {
    List<RepaymentUnitVO> repaymentUnitVOs = repaymentUnitModel.getRepaymentUnitVOsByOrderIdsAndStatus(orderIds, Collections.singletonList(status));
    repaymentUnitVOs.sort((o1, o2) -> o2.repaymentTimeUpdated.compareTo(o1.repaymentTimeUpdated));
    return repaymentUnitVOs
        .stream()
        .collect(Collectors.groupingBy(
            vo -> vo.orderId,
            Collectors.mapping(vo -> vo, Collectors.toList())));
  }

  @RunInTransaction
  private void repayByNormalType(
      List<Long> sortedInstalmentIds, UnionRepaymentVO unionRepaymentVO, MoneyOffDeductVO moneyOffDeductDetail,
      Map<Long, InstalmentCutInterestCouponDeductDetailVO> cutInterestCouponDeductMap, LoanMoneyOffCouponVO moneyOffCouponVO
  ) {
    // 墨西哥走下渠道减免券
    BigDecimal channelDeduct = ZERO;

    RepayAmountVO amountVO;
    Long couponId;

    if (BigDecimalHelper.greaterThan(moneyOffDeductDetail.getDeductAmount(), ZERO)) {
      amountVO = extractRepayAmount(unionRepaymentVO.transNo, sortedInstalmentIds, unionRepaymentVO.amount, moneyOffDeductDetail.getDeductAmount(), channelDeduct, cutInterestCouponDeductMap);
      couponId = moneyOffCouponVO.getId();
    } else {
      amountVO = extractRepayAmount(unionRepaymentVO.transNo, sortedInstalmentIds, unionRepaymentVO.amount, BigDecimal.ZERO, channelDeduct, cutInterestCouponDeductMap);
      couponId = null;
    }

    CashLoanRepaymentRecord cashLoanRepaymentRecord = initRepay(unionRepaymentVO.accountId, unionRepaymentVO.userId, unionRepaymentVO.transNo, amountVO, couponId, unionRepaymentVO.transactionTime);

    initRepaymentDetail(cashLoanRepaymentRecord, amountVO.repaymentUnitAmountVOs, amountVO.actualCouponDeductAmount, CashLoanRepaymentDetailType.COUPON, moneyOffDeductDetail);

    onRepaymentSucceed(unionRepaymentVO.paymentProvider, unionRepaymentVO.paymentStatus, unionRepaymentVO.transNo, unionRepaymentVO.transactionTime, amountVO.actualCouponDeductAmount);
  }

  @RunInTransaction  // 调用方锁loanAccountId
  public RepaymentVO repayByNotNormalType(
      Long orderId,
      Long accountId,
      List<Long> instalmentIds,
      CurrencyAmount repayAmount,
      CashLoanRepaymentType repaymentType,
      String extraData,
      Long couponId) {
    return repayByNotNormalType(orderId, accountId, instalmentIds, repayAmount, repaymentType, extraData, couponId, null);
  }

  @RunInTransaction  // 调用方锁loanAccountId
  public RepaymentVO repayByNotNormalType(
      Long orderId,
      Long accountId,
      List<Long> instalmentIds,
      CurrencyAmount repayAmount,
      CashLoanRepaymentType repaymentType,
      String extraData,
      Long couponId,
      List<DeductInfoVO> deductInfoVOs) {
    // 检查有没有处理中的还款
    if (cashLoanRepaymentService.hasProcessingRepayment(orderId)) {
      throw EcException.warn(EcExceptionType.CASH_LOAN_HAS_PROCESSING_REPAYMENT, TT.gen("上一笔还款正在处理中，请等待处理完毕后再提交"));
    }
    CashLoanOrderVO orderVO = ecOrderService.getOrderVO(orderId);
    CashLoanAssertion.assertOrderInStatus(orderVO, CashLoanOrderStatus.READY);
    RepayAmountVO amountVO = extractRepayAmount(orderId.toString(), instalmentIds, repayAmount, repaymentType, deductInfoVOs);
    CashLoanRepaymentRecord repaymentRecord = createRepay(
        accountId,
        orderVO.userId,
        null,
        amountVO,
        Clock.now(),
        CashLoanRepaymentStatus.SUCCEED,
        repaymentType,
        AdminUserIDThreadLocal.get(),
        extraData,
        couponId,
        null
    );
    RepayCompletedTermsVO termsVo = deduct(accountId, RepaymentVO.from(repaymentRecord));
    if (repaymentType == CashLoanRepaymentType.DEDUCT_COUPON) {
      initRepaymentDetail(repaymentRecord, amountVO.repaymentUnitAmountVOs, repaymentRecord.getAmount(), CashLoanRepaymentDetailType.DEDUCT_COUPON, null);
    }
    //log并且事件通知
    cashLoanMonitorService.logRepayment(accountId, repayAmount.getAmountInYuan(), repaymentType, CashLoanRepaymentStatus.SUCCEED, orderVO.sdkType);
    repaymentRecord.refresh();
    //通知repaymentSucceed事件
    RepaymentVO repaymentVO = RepaymentVO.from(repaymentRecord, termsVo.repayCompletedTerms, termsVo.repayCompletedTimelyTerms);
    publishAfterRepay(Collections.singletonList(orderId), repaymentVO);
    return repaymentVO;
  }

  public void queryAllPayout() {
    List<CashLoanOrderVO> orderVOs = ecOrderService.fetchAllByStatuses(CashLoanOrderStatus.INIT);
    for (CashLoanOrderVO orderVO : orderVOs) {
      Long orderId = orderVO.id;
      try {
        String mhtOrderNo = orderVO.mhtOrderNo;
        if (mhtOrderNo == null) {
          continue;
        }
        CashLoanFundingResult fundingResult = cashLoanFundingService.getPayoutResult(mhtOrderNo);
        handlePayoutResult(fundingResult, orderId, orderVO.accountId);
      } catch (Exception ex) {
        log.error("hit error while query cash loan order id:{}", orderId, ex);
      }
    }
  }

  public void checkCredentialInUse(UserPaymentCredential credential) {
    // 当order状态为RESERVE或INIT 意味order已创建且打款未结束
    if (ecOrderService.existOrder(credential, Arrays.asList(CashLoanOrderStatus.UNDONE_STATUSES_WITHOUT_READY)) || cashLoanActivityOrderService.existOrder(credential, Arrays.asList(CashLoanOrderStatus.UNDONE_STATUSES_WITHOUT_READY))) {
      throw EcException.warn(EcExceptionType.CASH_LOAN_ORDER_IN_PROCESSING_DELETE_PAYMENT_CREDENTIAL,
          TT.gen("借款正在申请中，暂时不允许更新银行账户信息"));
    }
  }

  /**
   * 新：计算自动减免的金额分配与涉及的券明细
   */
  private AutoDeductPlan getAutoDeductPlan(SDKType sdkType, BigDecimal amount, List<CashLoanInstalmentVO> instalmentVOs,
                                           Map<Long, InstalmentCutInterestCouponDeductDetailVO> couponDeductMap, Long transactionTime) {
    Map<Long, BigDecimal> autoDeductAmountMap = new HashMap<>();
    List<CashLoanInstalmentVO> couponDeductDetailList = new ArrayList<>();
    if (Clock.getDaysBetween(transactionTime, Clock.now(), sdkType.getTimeZone()) >= cashLoanConfig.getAutoDeductSkipDays()) {
      return buildCutInterestPlanWithoutAutoDeduct(amount, instalmentVOs, couponDeductMap);
    }
    BigDecimal repaidAmount = amount;
    BigDecimal errorThreshold = cashLoanConfig.autoDeductErrorThresholdBySdk(sdkType);

    for (CashLoanInstalmentVO instalmentVO : instalmentVOs) {
      if (BigDecimalHelper.lessThanOrEqual(repaidAmount, BigDecimal.ZERO)) {
        break;
      }
      // 还款减息券抵扣金额
      InstalmentCutInterestCouponDeductDetailVO couponDeductDetailVO = couponDeductMap.get(instalmentVO.id);
      BigDecimal couponDeductAmount = Objects.isNull(couponDeductDetailVO) ? BigDecimal.ZERO : couponDeductDetailVO.deductAmount;
      // 实际欠款金额
      BigDecimal owedAmount = CalcFeeUtil.getOwedAmount(instalmentVO);
      BigDecimal overCalcFee = cashLoanRepayHelper.getUnpaidOverCalcFee(instalmentVO, transactionTime, sdkType);
      BigDecimal actualOwedAmount = owedAmount.subtract(overCalcFee);
      // 部分还款不减免
      BigDecimal diffAmount = actualOwedAmount.subtract(couponDeductAmount).subtract(repaidAmount);
      if (BigDecimalHelper.greaterThan(diffAmount, errorThreshold)) {
        break;
      }

      BigDecimal autoDeductAmount = overCalcFee.add(BigDecimalHelper.max(diffAmount, BigDecimal.ZERO));
      repaidAmount = repaidAmount.subtract(owedAmount.subtract(autoDeductAmount).subtract(couponDeductAmount));
      log.info("id:{}, owedAmount:{}, overCalcFee:{}, actualOwedAmount:{}, diffAmount:{},repaidAmount:{} ", instalmentVO.id,
          owedAmount, overCalcFee, actualOwedAmount, diffAmount, repaidAmount);
      if (Objects.nonNull(couponDeductDetailVO)) {
        couponDeductDetailList.add(instalmentVO);
      }

      autoDeductAmountMap.put(instalmentVO.id, autoDeductAmount);
    }
    return new AutoDeductPlan(autoDeductAmountMap, couponDeductDetailList);
  }

  public AutoDeductPlan buildCutInterestPlanWithoutAutoDeduct(
      BigDecimal amount, List<CashLoanInstalmentVO> instalmentVOs, Map<Long, InstalmentCutInterestCouponDeductDetailVO> couponDeductMap
  ) {
    List<CashLoanInstalmentVO> couponDeductDetailList = new ArrayList<>();

    BigDecimal repaidAmount = amount;

    for (CashLoanInstalmentVO instalmentVO : instalmentVOs) {
      if (BigDecimalHelper.lessThanOrEqual(repaidAmount, BigDecimal.ZERO)) {
        break;
      }

      // 还款减息券抵扣金额
      InstalmentCutInterestCouponDeductDetailVO couponDeductDetailVO = instalmentVO.isOverdue() ? null : couponDeductMap.get(instalmentVO.id);
      BigDecimal couponDeductAmount = Objects.isNull(couponDeductDetailVO) ? BigDecimal.ZERO : couponDeductDetailVO.deductAmount;

      // 实际欠款金额
      BigDecimal actualOwedAmount = CalcFeeUtil.getOwedAmount(instalmentVO);
      // 部分还款不减免
      BigDecimal diffAmount = actualOwedAmount.subtract(couponDeductAmount).subtract(repaidAmount);
      if (BigDecimalHelper.greaterThan(diffAmount, BigDecimal.ZERO)) {
        break;
      }

      repaidAmount = repaidAmount.subtract(actualOwedAmount.subtract(couponDeductAmount));
      log.info("buildCutInterestPlanWithoutAutoDeduct, id:{}, owedAmount:{}, actualOwedAmount:{}, diffAmount:{},repaidAmount:{} ",
          instalmentVO.id, actualOwedAmount, actualOwedAmount, diffAmount, repaidAmount);

      if (Objects.nonNull(couponDeductDetailVO)) {
        couponDeductDetailList.add(instalmentVO);
      }
    }

    return new AutoDeductPlan(Collections.emptyMap(), couponDeductDetailList);
  }

  /**
   * 自动减免结果：金额分配 + 券明细列表
   */
  @Data
  static class AutoDeductPlan {

    private final Map<Long, BigDecimal> autoDeductAmountMap;
    private final List<CashLoanInstalmentVO> couponDeductDetailVOList;

    AutoDeductPlan(Map<Long, BigDecimal> autoDeductAmountMap, List<CashLoanInstalmentVO> couponDeductDetailVOList) {
      this.autoDeductAmountMap = autoDeductAmountMap;
      this.couponDeductDetailVOList = couponDeductDetailVOList;
    }
  }

  @RunInTransaction
  public void autoDeduct(List<CashLoanInstalmentVO> instalmentVOs, Map<Long, BigDecimal> autoDeductAmountMap, Map<Long, Integer> actualOverdueDaysMap) {
    for (CashLoanInstalmentVO instalmentVO : instalmentVOs) {
      autoDeduct(instalmentVO, autoDeductAmountMap.get(instalmentVO.id), actualOverdueDaysMap.get(instalmentVO.id));
    }
  }

  @RunInTransaction
  public void autoDeduct(CashLoanInstalmentVO instalmentVO, BigDecimal autoDeductAmount, int actualOverdueDays) {
    if (autoDeductAmount == null || BigDecimalHelper.lessThanOrEqual(autoDeductAmount, BigDecimal.ZERO)) {
      return;
    }
    Long instalmentId = instalmentVO.id;
    if (instalmentVO.status != CashLoanInstalmentStatus.INIT) {
      log.warn("instalmentStatus is not init, cannot do autoDeduct; id: {}", instalmentId);
      return;
    }
    // 自动减免
    CurrencyAmount deductAmount = CurrencyAmount.fromYuan(instalmentVO.currency, autoDeductAmount);
    repayByNotNormalType(instalmentVO.orderId, instalmentVO.loanAccountId,
        Collections.singletonList(instalmentId), deductAmount,
        CashLoanRepaymentType.AUTO_REDUCE, null, null);
    // 重置逾期天数
    List<CashLoanOverdueEventRecord> mistakenEventRecords = overdueEventModel.findByInstalmentIdAndGTOverdueDays(instalmentId, actualOverdueDays);
    if (!mistakenEventRecords.isEmpty()) {
      if (actualOverdueDays != instalmentVO.overdueDays - mistakenEventRecords.size()) {
        throw EcException.error("instalmentVO, overdueEventRecord and computed overdueDays don't match, instalmentId = " + instalmentId);
      }
      ecOrderService.updateInstalmentOverdueDays(instalmentId, actualOverdueDays);
      for (CashLoanOverdueEventRecord overdueEventRecord : mistakenEventRecords) {
        overdueRevertEventModel.init(overdueEventRecord.getId());
      }
    }
    log.info("do autoDeduct for orderId: {}, instalmentId: {}, autoDeduct amount: {}", instalmentVO.orderId, instalmentId, autoDeductAmount);
  }

  public void checkReloanCeased(SDKType sdkType, LoanUserTypeVO loanUserTypeVO) {
    if (cashLoanConfig.getCeaseReloanUserTypeCodes(sdkType).contains(loanUserTypeVO.code)) {
      throw EcException.warn(EcExceptionType.LOAN_ORDER_REJECTED_DUE_TO_FORCE_MAJEURE, TT.gen("Please bear with us as we temporarily cease some of Loan Processing."));
    }
  }

  public Map<Long, BigDecimal> getCutInterestMapByRepayment(Long orderId) {
    return repaymentUnitModel.findByOrderIdAndType(orderId, CashLoanRepaymentType.DEDUCT_COUPON)
        .stream()
        .collect(Collectors.toMap(CashLoanRepaymentUnitRecord::getInstalmentId, CashLoanRepaymentUnitRecord::getAmount));
  }

  public Map<Long /* instalmentId */, BigDecimal /* deductAmount */> getCutInterestMapByCoupon(Long orderId) {
    List<InstalmentCutInterestCouponDeductDetailVO> interestCouponDeductDetailVOS = instalmentCutInterestCouponDeductDetailService.getByOrderId(orderId);
    if (CollectionUtils.isNotEmpty(interestCouponDeductDetailVOS)) {
      return interestCouponDeductDetailVOS.stream()
          .filter(item -> !item.invalid())
          .collect(Collectors.toMap(InstalmentCutInterestCouponDeductDetailVO::getInstalmentId, InstalmentCutInterestCouponDeductDetailVO::getDeductAmount));
    }
    OrderInstalment oi = ecOrderService.getOrderInstalment(orderId);
    LoanUserCouponVO couponVO = loanUserCouponService.findByOrderId(orderId, LoanCouponUsageType.CUT_INTEREST);
    if (couponVO == null) {
      return Maps.newHashMap();
    }
    CutInterestVO cutInterestVO = ((LoanCutInterestCouponVO) couponVO).genCutInterestOIPlan(oi, oi.orderVO.sdkType);
    return cutInterestVO.instalmentVOMap.values()
        .stream()
        .collect(Collectors.toMap(vo -> vo.instalmentId, vo -> vo.deductPostInterest));
  }

  public Map<Long, BigDecimal> getCutInterestMapByCoupon(OrderInstalmentPlan oiPlan, Long couponId) {
    LoanUserCouponVO couponVO = loanUserCouponService.findByIdWithConfigOrNull(couponId);
    if (couponVO == null || LoanCouponUsageType.CUT_INTEREST != couponVO.usageType) {
      return Maps.newHashMap();
    }
    CutInterestVO cutInterestVO = ((LoanCutInterestCouponVO) couponVO).genCutInterestOIPlan(oiPlan, oiPlan.order.sdk);
    return cutInterestVO.instalmentVOMap.values()
        .stream()
        .collect(Collectors.toMap(vo -> (long) vo.termIdx, vo -> vo.deductPostInterest));
  }

  public BigDecimal getCutInterestAmount(Long orderId) {
    List<CashLoanRepaymentUnitRecord> repaymentUnitRecords = repaymentUnitModel.findByOrderIdAndType(orderId, CashLoanRepaymentType.DEDUCT_COUPON);
    if (CollectionUtils.isEmpty(repaymentUnitRecords)) {
      return ZERO;
    }
    return repaymentUnitRecords
        .stream()
        .map(CashLoanRepaymentUnitRecord::getAmount)
        .reduce(BigDecimal::add)
        .orElse(ZERO);
  }

  @RunInTransaction
  public void triggerLoanNextCheckStep(Long accountId, CashLoanOrderVO checkOrderVO, Long build) {
    bizCheckTool.pushCheckEvent(checkOrderVO.id, BusinessType.ORDER_CHECK, BizCheckEventType.FIRST_TRIGGER);
  }

  public CashLoanOrderVO createCreditsDecreaseQuickOrder(Long accountId, PaymentCredential paymentCredential,
                                                         BigDecimal principal,
                                                         SDKType sdkType, Long productId, Long couponId,
                                                         SourceType sourceType,
                                                         Long build,
                                                         Long creditsDecreaseQuickOrderInfoId) {
    LoanProductConfigVO productVO = productConfigService.getProductVO(productId);
    dailyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_DAILY, accountId, cashLoanConfig.maxDailyLoanTimes());
    monthlyUserActionTimesLoader.checkMaximalTimes(UserActionEnum.LOAN_TIMES_MONTHLY, accountId, cashLoanConfig.maxMonthlyLoanTimes());
    OrderInstalmentPlan oiPlan = OIPlanFactory.getInstance(sdkType).genPlan(principal, productVO);
    return threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord loanAccountRecord = accountModel.findByIdForUpdateOrThrow(accountId);
      CreditsDecreaseQuickOrderInfoVO creditsDecreaseQuickOrderInfoVO = creditsDecreaseQuickOrderInfoService.fetchByIdOrThrow(creditsDecreaseQuickOrderInfoId);
      if (creditsDecreaseQuickOrderInfoVO.status == CreditsDecreaseQuickOrderStatus.ORDERED) {
        return ecOrderService.getOrderVO(creditsDecreaseQuickOrderInfoVO.orderId);
      }
      if (creditsDecreaseQuickOrderInfoVO.status != CreditsDecreaseQuickOrderStatus.INIT) {
        throw EcException.warn(EcExceptionType.CASH_LOAN_CREDITS_EXPIRED, TT.gen("您的额度已失效，请重新下单"));
      }
      // 检查用户，金额以及贷款时间是否符合要求
      boolean isMultiLoan = multiLoanStatusService.hasMultiLoanQualify(accountId);
      assertUserCanCreateOrder(loanAccountRecord, principal, productVO.id, isMultiLoan);
      CashLoanOrderVO orderVO = null;
      if (isMultiLoan) {
        orderVO = createMultiLoanCreditsDecreaseQuickOrder(accountId, paymentCredential, sourceType, build, oiPlan);
      } else {
        orderVO = ecOrderService.createReservedOrder(accountId, paymentCredential, oiPlan, sourceType, build);
      }
      CashLoanOrderVO lastOrder = ecOrderService.getOrderVO(creditsDecreaseQuickOrderInfoVO.lastOrderId);
      ecOrderService.updateTraceId(orderVO.id, lastOrder.traceId);
      CashLoanOrderAdditionalInfoVO cashLoanOrderAdditionalInfoVO = cashLoanOrderAdditionalInfoService.findByOrderIdAndType(creditsDecreaseQuickOrderInfoVO.lastOrderId, OrderAdditionalInfoType.CREATE_ACTIVITY_ORDER_INFO);
      boolean needCreateActivityOrder = !Objects.isNull(cashLoanOrderAdditionalInfoVO) && StringUtils.isNotBlank(cashLoanOrderAdditionalInfoVO.info);
      addCreateActivityOrderAdditionalInfo(orderVO.id, orderVO.accountId, needCreateActivityOrder);
      bindCutInterestCoupon(couponId, orderVO, principal);
      // 修改降额快速下单状态
      creditsDecreaseQuickOrderInfoService.createOrderSuccess(orderVO.id, creditsDecreaseQuickOrderInfoVO.id);
      if (handWrittenSignatureService.handWrittenSignatureSuccess(creditsDecreaseQuickOrderInfoVO.lastOrderId, BusinessType.ORDER_CHECK)) {
        handWrittenSignatureService.createSignedRecordWhenCreateLoanOrder(orderVO);
      }
      triggerLoanNextCheckStep(accountId, orderVO, build);
      return orderVO;
    });
  }

  @NotNull
  private CashLoanOrderVO createMultiLoanCreditsDecreaseQuickOrder(Long accountId, PaymentCredential paymentCredential, SourceType sourceType, Long build, OrderInstalmentPlan oiPlan) {
    CashLoanOrderVO orderVO;
    orderVO = ecOrderService.createMultiOrder(accountId, paymentCredential, oiPlan, sourceType, build);
    // 修改续借状态--因为没有进行风控，所以此处需要先改成MANUAL_ORDER，再改成ORDER_RISK_ACCEPT
    multiLoanStatusService.updateStatus(orderVO.accountId, MultiLoanStatusChangeSource.MANUAL_ORDER);
    multiLoanStatusService.updateStatus(orderVO.accountId, MultiLoanStatusChangeSource.ORDER_RISK_ACCEPT);
    return orderVO;
  }

  @RunInTransaction
  private void addCreateActivityOrderAdditionalInfo(Long orderId, Long accountId, Boolean needCreateActivityOrder) {
    if (needCreateActivityOrder != Boolean.TRUE) {
      return;
    }
    LoanActivityOrderAdditionalInfo loanActivityOrderAdditionalInfo = cashLoanActivityOrderService.getLoanActivityOrderAdditionalInfo(accountId);
    if (Objects.isNull(loanActivityOrderAdditionalInfo)) {
      log.info("addCreateActivityOrderAdditionalInfo, loanActivityOrderAdditionalInfo is null, accountId:{}", accountId);
      return;
    }
    cashLoanOrderAdditionalInfoService.insertOrIgnore(orderId,
        OrderAdditionalInfoType.CREATE_ACTIVITY_ORDER_INFO,
        JsonUtils.toString(loanActivityOrderAdditionalInfo));
  }

  public List<CashLoanRepaymentUnitDetailVO> getByOrderIdAndTypes(List<Long> orderIds, List<String> types) {
    List<CashLoanRepaymentUnitDetailRecord> repaymentDetailRecords = cashLoanRepaymentDetailModel.fetchByOrderIdAndTypes(orderIds, types);
    return repaymentDetailRecords.stream().map(CashLoanRepaymentUnitDetailVO::from).collect(Collectors.toList());
  }

  public PaymentAccount getPaymentAccount(Long loanAccountId) {
    CashLoanOrderVO orderVO = ecOrderService.getEarliestDueOrder(loanAccountId);
    return getPaymentAccount(orderVO);
  }

  private PaymentAccount getPaymentAccount(CashLoanOrderVO orderVO) {
    if (orderVO == null) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("订单还款完成，请刷新首页"));
    }

    CashLoanFundingVO fundingVO = cashLoanFundingService.getFundingVO(orderVO.mhtOrderNo);
    return PaymentAccount.from(fundingVO.providerType);
  }
}
