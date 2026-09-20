package com.yqg.core.service.cashloan.repayment;

import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.cashloan.repayment.enums.RepaymentAccountSourceType;
import com.yqg.core.service.cashloan.repayment.enums.RepaymentAccountUsageType;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountContext;
import com.yqg.core.service.cashloan.repayment.vo.RepaymentAccountResVO;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.secure.check.context.SecureCheckUserContext;
import com.yqg.ec.common.exception.EcException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RepaymentAccountFactory {

  private static final ConcurrentHashMap<RepaymentAccountUsageType, RepaymentAccountHandler> MAP = new ConcurrentHashMap<>();

  public static void registerHandler(RepaymentAccountUsageType type, RepaymentAccountHandler handler) {
    MAP.putIfAbsent(type, handler);
  }

  /**
   * 获取VA的统一入口方法，传入希望生成的VA类型sourceType，和生成VA所必须的入参，生成对应的VA
   *
   * @param sourceType
   * @param channel
   * @param couponId
   * @param amount
   * @param encodeInstalmentIds
   * @param mobileNumber
   * @param encodeJBPOrderIds
   * @param userContext
   * @return
   */
  public static RepaymentAccountResVO getRepaymentAccountByChannel(RepaymentAccountSourceType sourceType, String channel, Long couponId,
      BigDecimal amount, List<String> encodeInstalmentIds, String mobileNumber, List<String> encodeJBPOrderIds, RepayStyleVersion repayStyleVersion,
      SecureCheckUserContext userContext) {
    //1.构建上下文
    RepaymentAccountContext context = RepaymentAccountContext.from(channel, couponId, amount, encodeInstalmentIds, mobileNumber,
        encodeJBPOrderIds, repayStyleVersion, userContext);
    //2.选择Handler实现类
    RepaymentAccountHandler handler = chooseVaHandler(sourceType, context);
    //3.调用获取VA方法
    return handler.getRepaymentAccountByChannelV3(context);
  }

  /**
   * 获取OVO动态VA的统一入口方法，传入希望生成的VA类型sourceType，和生成VA所必须的入参，生成对应的VA
   *
   * @param sourceType
   * @param channel
   * @param couponId
   * @param amount
   * @param encodeInstalmentIds
   * @param mobileNumber
   * @param encodeJBPOrderIds
   * @param userContext
   * @return
   */
  public static RepaymentAccountResVO getOVORepaymentAccountByChannel(RepaymentAccountSourceType sourceType, String channel, Long couponId,
      BigDecimal amount, List<String> encodeInstalmentIds, String mobileNumber, List<String> encodeJBPOrderIds, RepayStyleVersion repayStyleVersion,
      SecureCheckUserContext userContext) {
    //1.构建上下文
    RepaymentAccountContext context = RepaymentAccountContext.from(channel, couponId, amount, encodeInstalmentIds, mobileNumber,
        encodeJBPOrderIds, repayStyleVersion, userContext);
    //2.选择Handler实现类
    RepaymentAccountHandler ovoHandler = chooseVaHandler(sourceType, context);
    //3.调用获取OVO的VA方法
    return ovoHandler.getOVORepaymentAccountByChannel(context);
  }

  /**
   * 根据content和最终期望生成VA的类型选择handler
   *
   * @param sourceType
   * @param context
   * @return
   */
  private static RepaymentAccountHandler chooseVaHandler(RepaymentAccountSourceType sourceType, RepaymentAccountContext context) {
    //根据希望最终生成的VA类型，和context，选择出还款VA的用途
    RepaymentAccountUsageType usageType = sourceType.getChooseUsageType().apply(context);
    return getHandler(usageType);
  }

  /**
   * 获取待支付金额的统一入口方法
   *
   * @param userId
   * @param payEventType
   * @param account
   * @return
   */
  public static Long getOwnedAmount(Long userId, PayEventType payEventType, PaymentAccount account) {
    //1.根据payEventType选择获取bca金额的handler
    RepaymentAccountHandler handler = chooseGetOwnedAmountHandler(payEventType);
    //2.调用获取金额方法
    return handler.getOwnedAmount(userId, payEventType, account);
  }

  /**
   * 根据payEventType选择获取bca金额handler
   *
   * @param payEventType
   * @return
   */
  private static RepaymentAccountHandler chooseGetOwnedAmountHandler(PayEventType payEventType) {
    RepaymentAccountUsageType usageType;
    if (payEventType == PayEventType.JBP_RECEIPT) {
      usageType = RepaymentAccountUsageType.JBP_VA;
    } else {
      usageType = RepaymentAccountUsageType.UNION_REPAY_EC_VA;
    }
    return getHandler(usageType);
  }

  private static RepaymentAccountHandler getHandler(RepaymentAccountUsageType usageType) {
    RepaymentAccountHandler handler = MAP.get(usageType);
    if (handler == null) {
      throw EcException.error("RepaymentAccountFactory handler not found for type: {}", usageType);
    }
    return handler;
  }
}
