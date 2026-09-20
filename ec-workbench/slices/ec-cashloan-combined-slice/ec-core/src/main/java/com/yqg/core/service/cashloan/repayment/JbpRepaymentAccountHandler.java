package com.yqg.core.service.cashloan.repayment;

import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.service.cashloan.repayment.enums.RepaymentAccountUsageType;
import com.yqg.core.service.cashloan.repayment.vo.CashLoanRepaymentAccountResVO;
import com.yqg.core.service.cashloan.repayment.vo.JbpRepaymentAccountResVO;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountContext;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountResVO;
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
import com.yqg.jbp.client.api.repayment.IRepaymentApi;
import com.yqg.jbp.common.utils.JbpBaseResponse;
import com.yqg.jbp.dto.repayment.QueryUnpaidAmountRequest;
import com.yqg.jbp.dto.repayment.QueryUnpaidAmountResponse;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JbpRepaymentAccountHandler implements RepaymentAccountHandler {

  @Autowired
  private RepaymentAccountService repaymentAccountService;
  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  private RepaymentAccountMonitorService repaymentAccountMonitorService;
  @Autowired
  private IRepaymentApi repaymentApi;

  @Override
  public RepaymentAccountUsageType getRepaymentAccountUsageType() {
    return RepaymentAccountUsageType.JBP_VA;
  }

  @Override
  public RepaymentAccountResVO getRepaymentAccountByChannelV3(RepaymentAccountContext context) {
    return getRepaymentAccountResult(YqgHashids.decode(context.getEncodeJBPOrderIds().get(0)), context.getChannel(),
        context.getAmount(), context.getUserContext(), context.getMobileNumber());
  }

  /**
   * jbp不需要调用这个方法，getRepaymentAccountByChannelV3可以支持OVO
   *
   * @param context
   * @return
   */
  @Override
  public RepaymentAccountResVO getOVORepaymentAccountByChannel(RepaymentAccountContext context) {
    throw EcException.error("JbpRepaymentAccountHandler don't need getOVORepaymentAccountByChannel");
  }

  private RepaymentAccountResVO getRepaymentAccountResult(Long orderId, String channel, BigDecimal actualAmount,
      SecureCheckUserContext userContext, String mobileNumber) {
    Map<String, String> extraData = checkOVORepaymentParamForJBP(channel, mobileNumber, userContext.sdkType);
    //单独获取还款账号时，可以让用户自行等待。如失败，则通过warning提示用户，同时打出error日志。
    boolean result = true;
    boolean needLog = Boolean.TRUE;
    try {
      //todo:(Yicheng Jia, T00000)SHOPEE后面写
      JbpRepaymentAccountResVO response = getRepaymentAccountResponse(orderId, channel, userContext, actualAmount, extraData);
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
        repaymentAccountMonitorService.logJbp(channel, result, userContext.sdkType);
      }
    }
  }

  private JbpRepaymentAccountResVO getRepaymentAccountResponse(Long orderId, String channel, SecureCheckUserContext userContext,
      BigDecimal actualAmount, Map<String, String> extraData) {
    RepaymentAccountVO repaymentAccountVO = repaymentAccountService.getRepaymentAccountByChannelForJbp(userContext.userId,
        userContext.sdkType, orderId, channel, actualAmount, extraData, PayEventType.JBP_RECEIPT);
    if (repaymentAccountVO == null) {
      throw EcException.error("can not get virtualAccount, userId : {}, channel : {}", userContext.userId, channel);
    }
    List<CashLoanRepaymentAccountResVO.RelatedInfo> relatedInfo = repaymentAccountService.getRelatedInfo(repaymentAccountVO);
    String deepLink = repaymentAccountConfig.getRepaymentChannelDeepLink(repaymentAccountVO.getChannel());
    return JbpRepaymentAccountResVO.from(repaymentAccountVO, actualAmount, deepLink, relatedInfo);
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
    if (payEventType == PayEventType.JBP_RECEIPT) {
      JbpBaseResponse<List<QueryUnpaidAmountResponse>> responseList = repaymentApi.queryUnpaidAmount(
          QueryUnpaidAmountRequest.builder().userId(userId).payEventType(payEventType.name()).build());
      return responseList.body.stream().map(QueryUnpaidAmountResponse::getAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO)
          .longValue();
    }
    throw EcException.error("JbpRepaymentAccountHandler only support JBP_RECEIPT payEventType");
  }
}
