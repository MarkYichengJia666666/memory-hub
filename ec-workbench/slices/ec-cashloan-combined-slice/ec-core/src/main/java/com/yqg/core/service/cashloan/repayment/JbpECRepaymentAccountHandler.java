package com.yqg.core.service.cashloan.repayment;

import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.RepaymentAccountModeService;
import com.yqg.core.service.cashloan.repay.CashLoanRepaymentService;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.cashloan.repayment.enums.RepaymentAccountUsageType;
import com.yqg.core.service.cashloan.repayment.vo.CashLoanRepaymentAccountResVO;
import com.yqg.core.service.cashloan.repayment.vo.JbpRepaymentAccountResVO;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountContext;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountResVO;
import com.yqg.core.service.cashloan.repayment.vo.VirtualAccountResVO;
import com.yqg.core.service.financing.transaction.FinancingTransactionService;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountMonitorService;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountService;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.secure.check.context.SecureCheckUserContext;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionLogLevel;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JbpECRepaymentAccountHandler implements RepaymentAccountHandler {

  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private RepaymentAccountService repaymentAccountService;
  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  private RepaymentAccountMonitorService repaymentAccountMonitorService;
  @Autowired
  private RepaymentAccountModeService modeService;
  @Autowired
  private CashLoanRepaymentService cashLoanRepaymentService;
  @Autowired
  private FinancingTransactionService financingTransactionService;

  @Override
  public RepaymentAccountUsageType getRepaymentAccountUsageType() {
    return RepaymentAccountUsageType.JBP_EC_VA;
  }

  @Override
  public RepaymentAccountResVO getRepaymentAccountByChannelV3(RepaymentAccountContext context) {
    //EC还款计划为空，则jbpOrderIds一定不为空，进入单独获取jbp的EC VA
    return processRepaymentAccountForJBP(context);
  }

  @Override
  public RepaymentAccountResVO getOVORepaymentAccountByChannel(RepaymentAccountContext context) {
    //EC还款计划为空，则jbpOrderIds一定不为空
    return processOVORepaymentAccountForJBP(context);
  }

  /**
   * 获取JBP订单的EC VA，适用于只还JBP订单的情况
   */
  private RepaymentAccountResVO processRepaymentAccountForJBP(RepaymentAccountContext context) {
    List<Long> jbpOrderIds = context.getEncodeJBPOrderIds().stream().map(YqgHashids::decode).collect(Collectors.toList());
    Long jbpOrderId = jbpOrderIds.get(0);
    return getRepaymentAccountResultForJBP(jbpOrderId, context.getChannel(), context.getAmount(), context.getUserContext(),
        context.getMobileNumber(), true);
  }

  /**
   * 获取JBP订单的EC VA，适用于只还JBP订单的情况
   */
  private JbpRepaymentAccountResVO processOVORepaymentAccountForJBP(RepaymentAccountContext context) {
    List<Long> jbpOrderIds = context.getEncodeJBPOrderIds().stream().map(YqgHashids::decode).collect(Collectors.toList());
    Long jbpOrderId = jbpOrderIds.get(0);
    return getRepaymentAccountResultForJBP(jbpOrderId, context.getChannel(), context.getAmount(), context.getUserContext(),
        context.getMobileNumber());
  }

  private JbpRepaymentAccountResVO getRepaymentAccountResponse(Long orderId, String channel, SecureCheckUserContext userContext,
      BigDecimal actualAmount, Map<String, String> extraData, boolean fullInfo) {
    RepaymentAccountVO repaymentAccountVO = repaymentAccountService.getRepaymentAccountByChannelForJbp(userContext.userId,
        userContext.sdkType, orderId, channel, actualAmount, extraData, null);
    if (repaymentAccountVO == null) {
      throw EcException.error("can not get virtualAccount, userId : {}, channel : {}", userContext.userId, channel);
    }
    String deepLink = repaymentAccountConfig.getRepaymentChannelDeepLink(repaymentAccountVO.getChannel());
    List<CashLoanRepaymentAccountResVO.RelatedInfo> relatedInfo = repaymentAccountService.getRelatedInfo(repaymentAccountVO);
    if (fullInfo) {
      List<CashLoanRepaymentAccountResVO.Mode> modes = getModes(repaymentAccountVO, userContext.sdkType, userContext.build);
      VirtualAccountResVO virtualAccountResVO = VirtualAccountResVO.from(repaymentAccountVO.getAccount(),
          repaymentAccountVO.getChannel(), repaymentAccountVO.getChannelDesc(),
          cashLoanConfig.channelLogoMap(RepayStyleVersion.V1, repaymentAccountVO.getChannel()));
      Boolean canCopy = repaymentAccountConfig.getEnableCopyWithBuild(channel, userContext.build);
      return JbpRepaymentAccountResVO.from(virtualAccountResVO, modes, relatedInfo, canCopy, repaymentAccountVO, actualAmount,
          deepLink);
    } else {
      return JbpRepaymentAccountResVO.from(repaymentAccountVO, actualAmount, deepLink, relatedInfo);
    }
  }

  private List<CashLoanRepaymentAccountResVO.Mode> getModes(RepaymentAccountVO data, SDKType sdkType, Long build) {
    return modeService.getModes(data, sdkType, build).stream().map(vo -> new CashLoanRepaymentAccountResVO.Mode(vo.mode, vo.desc))
        .collect(Collectors.toList());
  }

  /**
   * 为JBP订单获取EC的VA
   */
  private JbpRepaymentAccountResVO getRepaymentAccountResultForJBP(Long orderId, String channel, BigDecimal actualAmount,
      SecureCheckUserContext userContext, String mobileNumber, boolean fullInfo) {
    Map<String, String> extraData = checkOVORepaymentParamForJBP(channel, mobileNumber, userContext.sdkType);
    //单独获取还款账号时，可以让用户自行等待。如失败，则通过warning提示用户，同时打出error日志。
    boolean result = true;
    boolean needLog = Boolean.TRUE;
    try {
      //todo:(Yicheng Jia, T00000)SHOPEE后面写
      JbpRepaymentAccountResVO response = getRepaymentAccountResponse(orderId, channel, userContext, actualAmount, extraData, fullInfo);
      return response;
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
        repaymentAccountMonitorService.log(channel, result, userContext.sdkType);
      }
    }
  }

  private JbpRepaymentAccountResVO getRepaymentAccountResultForJBP(Long orderId, String channel, BigDecimal actualAmount,
      SecureCheckUserContext userContext, String mobileNumber) {
    return getRepaymentAccountResultForJBP(orderId, channel, actualAmount, userContext, mobileNumber, false);
  }

  private Map<String, String> checkOVORepaymentParamForJBP(String channel, String mobileNumber, SDKType sdkType) {
    if (!channel.equals("OVO")) {
      return null;
    }
    if (StringUtils.isBlank(mobileNumber)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("还款手机号格式错误"));
    }
    if (!MobileConverter.isValidNationalNumber(mobileNumber, sdkType.getLocale())) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("还款手机号格式错误"));
    }
    Map<String, String> extraData = new HashMap<>();
    extraData.put("account", mobileNumber);
    return extraData;
  }

  @Override
  public Long getOwnedAmount(Long userId, PayEventType payEventType, PaymentAccount account) {
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
