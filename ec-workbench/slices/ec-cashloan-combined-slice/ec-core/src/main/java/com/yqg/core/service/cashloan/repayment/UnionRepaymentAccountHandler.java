package com.yqg.core.service.cashloan.repayment;

import com.google.common.collect.Lists;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.model.sql.loan.coupon.enums.LoanCouponStatus;
import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.RepaymentAccountModeService;
import com.yqg.core.service.cashloan.funding.CashLoanFundingService;
import com.yqg.core.service.cashloan.funding.CashLoanFundingVO;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.repay.CashLoanRepaymentService;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.cashloan.repayment.enums.RepaymentAccountUsageType;
import com.yqg.core.service.cashloan.repayment.vo.CashLoanRepaymentAccountResVO;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountContext;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountResVO;
import com.yqg.core.service.cashloan.repayment.vo.VirtualAccountResVO;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.financing.transaction.FinancingTransactionService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountMonitorService;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountService;
import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountCallerType;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.loan.repayment.experiment.RepaymentExperimentSupport;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.vo.AccountRelatedInfo.Type;
import com.yqg.core.service.secure.check.context.SecureCheckUserContext;
import com.yqg.core.service.loan.repayment.account.RepaymentChannelMonitorService;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.service.statictext.StaticTextService;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionLogLevel;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UnionRepaymentAccountHandler implements RepaymentAccountHandler {

  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private RepaymentAccountService repaymentAccountService;
  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  private RepaymentAccountMonitorService repaymentAccountMonitorService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private CashLoanFundingService fundingService;
  @Autowired
  private RepaymentAccountModeService modeService;
  @Autowired
  private StaticTextService textService;
  @Autowired
  private RepaymentExperimentSupport repaymentExperimentSupport;
  @Autowired
  private CashLoanRepaymentService cashLoanRepaymentService;
  @Autowired
  private FinancingTransactionService financingTransactionService;
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService instalmentCutInterestCouponDeductDetailService;
  @Autowired
  private RepaymentChannelMonitorService repaymentChannelMonitorService;

  @Override
  public RepaymentAccountUsageType getRepaymentAccountUsageType() {
    return RepaymentAccountUsageType.UNION_REPAY_EC_VA;
  }

  @Override
  public RepaymentAccountResVO getRepaymentAccountByChannelV3(RepaymentAccountContext context) {
    //EC还款计划非空则走获取账单EC VA
    return processRepaymentAccountForEC(context);
  }

  @Override
  public RepaymentAccountResVO getOVORepaymentAccountByChannel(RepaymentAccountContext context) {
    //EC还款计划非空则走老逻辑获取VA
    return processOVORepaymentAccountForEC(context);
  }

  /**
   * 获取EC VA，适用于同时还EC账单和JBP订单的情况
   */
  private RepaymentAccountResVO processRepaymentAccountForEC(RepaymentAccountContext context) {
    //V3接口，取最早到期的分期对应的order
    CashLoanInstalmentVO instalmentVO = instalmentCutInterestCouponDeductDetailService.getViewInstalment(checkParamAndGetInstalmentVO(context));
    CashLoanOrderVO orderVO = instalmentCutInterestCouponDeductDetailService.getViewOrder(ecOrderService.getOrderVO(Objects.requireNonNull(instalmentVO).orderId));

    //V3接口，传过来的amount是已使用优惠券后的金额
    return getRepaymentAccountResultV2(context.getChannel(), context.getAmount(), orderVO, context.getUserContext(), null, null, context.getRepayStyleVersion());
  }

  /**
   * 获取EC VA，适用于同时还EC账单和JBP订单的情况
   */
  private RepaymentAccountResVO processOVORepaymentAccountForEC(RepaymentAccountContext context) {
    CashLoanInstalmentVO instalmentVO = instalmentCutInterestCouponDeductDetailService.getViewInstalment(checkParamAndGetInstalmentVO(context));
    checkOVORepaymentParam(context);
    CashLoanOrderVO orderVO = instalmentCutInterestCouponDeductDetailService.getViewOrder(ecOrderService.getOrderVO(Objects.requireNonNull(instalmentVO).orderId));

    //V3接口，传过来的amount是已使用优惠券后的金额
    return getRepaymentAccountResultV2(context.getChannel(), context.getAmount(), orderVO, context.getUserContext(),
        context.getMobileNumber(), instalmentVO.id, context.getRepayStyleVersion());
  }

  private void checkOVORepaymentParam(RepaymentAccountContext request) {
    SecureCheckUserContext userContext = request.getUserContext();
    if (!request.getChannel().equals("OVO")) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("还款渠道错误"));
    }
    if (!MobileConverter.isValidNationalNumber(request.getMobileNumber(), userContext.sdkType.getLocale())) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("还款手机号格式错误"));
    }
  }

  private CashLoanInstalmentVO checkParamAndGetInstalmentVO(RepaymentAccountContext context) {
    SecureCheckUserContext userContext = context.getUserContext();
    List<Long> instalmentIds = context.getEncodeInstalmentIds().stream().map(YqgHashids::decode).collect(Collectors.toList());
    List<CashLoanInstalmentVO> instalmentVOs = ecOrderService.getInstalmentVOs(instalmentIds);

    //检查下分期的userId
    instalmentVOs.forEach(instalmentVO -> EcAsserts.assertTrue(instalmentVO.userId.equals(userContext.userId),
        "instalmentId: {},instalment userId : {}, apply userId : {}", instalmentVO.id, instalmentVO.userId, userContext.userId));

    if (instalmentVOs.stream().anyMatch(instalmentVO -> instalmentVO.status != CashLoanInstalmentStatus.INIT)) {
      throw EcException.warn(EcExceptionType.CASH_LOAN_INSTALMENT_STATUS_CHANGED, TT.gen("您的未还账单已变更，请重新选择账单进行还款。"));
    }

    Long loanAccountId = loanAccountService.getAccountIdByUserId(userContext.userId, userContext.sdkType);
    if (context.getCouponId() != null) {
      LoanUserCouponVO loanUserCouponVO = loanUserCouponService.findByIdWithConfig(context.getCouponId());
      loanUserCouponService.checkSelfCoupon(loanUserCouponVO, loanAccountId);
      if (loanUserCouponVO.status != LoanCouponStatus.PENDING) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("优惠券不满足使用条件，请重新选择"),
            "coupon status is not PENDING for getRepaymentAccountByChannel V3,couponId :{}, coupon status:{}", context.getCouponId(),
            loanUserCouponVO.status);
      }
    } else {
      loanUserCouponService.unbindAllMoneyOffCoupon(loanAccountId);
    }

    //V3接口，取最早到期的分期对应的order
    return instalmentVOs.stream()
        .min(Comparator.comparing(CashLoanInstalmentVO::getBillingDate).thenComparing(CashLoanInstalmentVO::getTimeCreated)).orElse(null);
  }

  private CashLoanRepaymentAccountResVO getRepaymentAccountResultV2(String channel, BigDecimal actualAmount, CashLoanOrderVO orderVO,
      SecureCheckUserContext userContext, String mobileNumber, Long instalmentId, RepayStyleVersion repayStyleVersion) {
    repaymentChannelMonitorService.logChannelSelect(userContext.userId, channel, PaymentProvider.NONE);
    //单独获取还款账号时，可以让用户自行等待。如失败，则通过warning提示用户，同时打出error日志。
    boolean result = true;
    boolean needLog = Boolean.TRUE;
    try {
      PaymentAccount paymentAccount = getPaymentAccount(orderVO);
      if (channel.equals("OVO")) {
        return getOVORepaymentAccountResponse(channel, orderVO, userContext, paymentAccount, actualAmount, mobileNumber, instalmentId, repayStyleVersion);
      }

      return getRepaymentAccountResponse(channel, orderVO, userContext, paymentAccount, actualAmount, repayStyleVersion);
    } catch (Exception e) {
      result = false;
      if (e instanceof EcException) {
        if (EcExceptionType.CASH_LOAN_GET_REPAYMENT_ACCOUNT_EXCEED_AMOUNT_LIMIT == ((EcException) e).exceptionType) {
          needLog = Boolean.FALSE;
        }
        if (((EcException) e).exceptionType.logLevel == EcExceptionLogLevel.WARNING) {
          throw e;
        }
      }
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("当前还款渠道不可用，请稍后重试或尝试其他渠道。"), e);
    } finally {
      if (needLog) {
        repaymentAccountMonitorService.log(channel, result, orderVO.sdkType);
      }
    }
  }

  private PaymentAccount getPaymentAccount(CashLoanOrderVO orderVO) {
    if (orderVO == null) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("订单还款完成，请刷新首页"));
    }

    CashLoanFundingVO fundingVO = fundingService.getFundingVO(orderVO.mhtOrderNo);
    return PaymentAccount.from(fundingVO.providerType);
  }

  private CashLoanRepaymentAccountResVO getOVORepaymentAccountResponse(String channel, CashLoanOrderVO orderVO,
      SecureCheckUserContext userContext, PaymentAccount paymentAccount, BigDecimal actualAmount, String mobileNumber, Long instalmentId, RepayStyleVersion repayStyleVersion) {
    Map<String, String> extraData = new HashMap<>();
    extraData.put("account", mobileNumber);
    RepaymentAccountVO repaymentAccountVO = repaymentAccountService.getOVORepaymentAccountVO(actualAmount, extraData, paymentAccount,
        orderVO, instalmentId);
    trackAccountGeneratedSuccess(userContext.userId, channel, repaymentAccountVO);
    return buildRepaymentAccountResponse(repaymentAccountVO, channel, userContext, repayStyleVersion);
  }

  private CashLoanRepaymentAccountResVO getRepaymentAccountResponse(String channel, CashLoanOrderVO orderVO,
      SecureCheckUserContext userContext, PaymentAccount paymentAccount, BigDecimal actualAmount, RepayStyleVersion repayStyleVersion) {
    // 传入 UNION_REPAYMENT_HANDLER 调用来源，以应用 Provider 分流选择逻辑
    RepaymentAccountVO repaymentAccountVO = repaymentAccountService.getRepaymentAccountByChannel(userContext.sdkType, paymentAccount,
        channel, actualAmount, orderVO, RepaymentAccountCallerType.UNION_REPAYMENT_HANDLER);
    trackAccountGeneratedSuccess(userContext.userId, channel, repaymentAccountVO);
    if (StringUtils.equals(channel, DynamicAccountChannel.XENDIT_QRIS.name())) {
      return buildQrisRepaymentAccountResponse(repaymentAccountVO, channel, userContext);
    }
    return buildRepaymentAccountResponse(repaymentAccountVO, channel, userContext, repayStyleVersion);
  }

  private void trackAccountGeneratedSuccess(Long userId, String channel, RepaymentAccountVO repaymentAccountVO) {
    if (repaymentAccountVO == null) {
      return;
    }
    PaymentProvider provider = repaymentAccountVO.getProvider() != null
        ? repaymentAccountVO.getProvider()
        : PaymentProvider.NONE;
    repaymentChannelMonitorService.logAccountGenerated(userId, channel, provider);
  }

  private CashLoanRepaymentAccountResVO buildRepaymentAccountResponse(RepaymentAccountVO repaymentAccountVO, String channel,
      SecureCheckUserContext userContext,  RepayStyleVersion repayStyleVersion) {
    if (repaymentAccountVO == null) {
      throw EcException.error("can not get virtualAccount, userId : {}, channel : {}", userContext.userId, channel);
    }
    List<CashLoanRepaymentAccountResVO.RelatedInfo> relatedInfo = getRelatedInfo(repaymentAccountVO);
    List<CashLoanRepaymentAccountResVO.Mode> modes = getModes(repaymentAccountVO, userContext.sdkType, userContext.build);
    VirtualAccountResVO virtualAccountResVO = VirtualAccountResVO.from(repaymentAccountVO.getAccount(), repaymentAccountVO.getChannel(),
        repaymentAccountVO.getChannelDesc(), cashLoanConfig.channelLogoMap(repayStyleVersion, repaymentAccountVO.getChannel()));
    Boolean canCopy = repaymentAccountConfig.getEnableCopyWithBuild(channel, userContext.build);
    return CashLoanRepaymentAccountResVO.from(virtualAccountResVO, modes, relatedInfo, canCopy, repaymentAccountVO);
  }

  private CashLoanRepaymentAccountResVO buildQrisRepaymentAccountResponse(RepaymentAccountVO repaymentAccountVO, String channel,
      SecureCheckUserContext userContext) {
    if (repaymentAccountVO == null || repaymentAccountVO.getRelatedInfo() == null || repaymentAccountVO.getRelatedInfo().size() != 1
        || !repaymentAccountVO.getRelatedInfo().containsKey(Type.QR_CODE) || repaymentAccountVO.getExpireTime() == null) {
      throw EcException.error("can not get QR code, userId : {}, channel : {}", userContext.userId, channel);
    }
    List<CashLoanRepaymentAccountResVO.Mode> modes = cashLoanConfig.getQrisRepaymentDisplayModes().stream()
        .map(vo -> new CashLoanRepaymentAccountResVO.Mode(vo.getMode(), vo.getDesc())).collect(Collectors.toList());
    String qrCodeLink = repaymentAccountVO.getRelatedInfo().get(Type.QR_CODE);
    Long qrCodeExpireTime = repaymentAccountVO.getExpireTime();
    return CashLoanRepaymentAccountResVO.fromQrCode(DynamicAccountChannel.XENDIT_QRIS, modes, qrCodeLink, qrCodeExpireTime);
  }

  private List<CashLoanRepaymentAccountResVO.RelatedInfo> getRelatedInfo(RepaymentAccountVO data) {
    Boolean internalOpen = repaymentAccountConfig.getRepaymentRelatedInfoInternalOpenByChannel(data.getChannel());
    return Optional.ofNullable(data.getRelatedInfo()).map(Map::entrySet).map(
        set -> set.stream().map(e -> new CashLoanRepaymentAccountResVO.RelatedInfo(e.getKey(), e.getValue(), internalOpen))
            .collect(Collectors.toList())).orElse(null);
  }

  private List<CashLoanRepaymentAccountResVO.Mode> getModes(RepaymentAccountVO data, SDKType sdkType, Long build) {
    return modeService.getModes(data, sdkType, build).stream().map(vo -> new CashLoanRepaymentAccountResVO.Mode(vo.mode, vo.desc))
        .collect(Collectors.toList());
  }


  @Override
  public Long getOwnedAmount(Long userId, PayEventType payEventType, PaymentAccount account) {
    //todo yapengchen cashLoanRepaymentService.getOwnedAmount方法内部耦合比较严重，需要重构
    if (payEventType == PayEventType.FIN_RDL_RECEIPT) {
      throw EcException.error("not support getOwnedAmount for FIN_RDL_RECEIPT, userId is {}", userId);
    }

    switch (account) {
      case IDN_FIN:
        return financingTransactionService.findEarliestInitTopUpByUserId(userId);
      case IDN:
        return cashLoanRepaymentService.getOwnedAmount(userId);
      default:
        throw EcException.error("unsupported paymentAccount when get ownedAmount for bcaPaymentProvider! PaymentAccount = {}", account);
    }
  }

}
