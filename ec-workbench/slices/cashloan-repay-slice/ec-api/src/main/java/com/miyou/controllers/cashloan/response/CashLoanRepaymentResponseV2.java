package com.miyou.controllers.cashloan.response;

import com.miyou.controllers.cashloan.CashLoanRepaymentDisplayStatus;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitPaymentType;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.RepaymentUnitVO;
import com.yqg.core.service.cashloan.vo.RepaymentVO;
import com.yqg.core.service.directdebit.vo.DirectDebitPaymentVO;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.DateFormatter;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 还款信息
 * Created by shihao on 17/12/28.
 */
public class CashLoanRepaymentResponseV2 {
  public String details;

  public static CashLoanRepaymentResponseV2 from(RepaymentVO repaymentVO,
                                                 DirectDebitPaymentVO directDebitPaymentVO,
                                                 LoanUserCouponVO couponVO,
                                                 List<RepaymentUnitVO> repaymentUnits,
                                                 Map<Long, CashLoanInstalmentVO> instalmentVOMap,
                                                 SDKType sdkType) {
    String lineSeparator = "\n";
    String keyValueSeparator = " : ";

    YqgLocale locale = sdkType.getLocale();
    String dateFormatter = DateFormatter.getDateFormatter(locale, DateFormatter.yyyy_MM_dd___HH_mm_ss);
    StringBuilder builder = new StringBuilder();

    Long timePaid = repaymentVO.timeRepaid != null ? repaymentVO.timeRepaid : repaymentVO.timeUpdated;
    builder.append(TT.gen("还款时间").toString(locale.locale))
        .append(keyValueSeparator)
        .append(Clock.dateTimeStringFromTimestamp(timePaid, dateFormatter, sdkType.getTimeZone()))
        .append(lineSeparator)
        .append(TT.gen("还款总额").toString(locale.locale))
        .append(keyValueSeparator)
        .append(AmountFormatter.format(sdkType.getCurrency(), repaymentVO.amount))
        .append(lineSeparator);
    if (directDebitPaymentVO != null) {
      builder.append(TT.gen("还款方式").toString(locale.locale))
          .append(keyValueSeparator)
          .append(TT.gen(getDirectDebitTypeDesc(directDebitPaymentVO.type)).toString(locale.locale))
          .append(lineSeparator);
    }

    BigDecimal couponDeductAmount = couponVO == null ? BigDecimal.ZERO : couponVO.allAffectedMoney;
    //优惠券相关展示文案
    if (couponDeductAmount.compareTo(BigDecimal.ZERO) > 0) {
      builder.append(TT.gen("优惠券金额").toString(locale.locale))
          .append(keyValueSeparator);

      BigDecimal actualAmount = repaymentVO.amount.subtract(couponDeductAmount);
      builder.append(AmountFormatter.format(sdkType.getCurrency(), couponDeductAmount))
          .append(lineSeparator)
          .append(TT.gen("实还金额").toString(locale.locale))
          .append(keyValueSeparator)
          .append(AmountFormatter.format(sdkType.getCurrency(), actualAmount))
          .append(lineSeparator);
    }

    CashLoanRepaymentDisplayStatus displayStatus = CashLoanRepaymentDisplayStatus.from(repaymentVO.status);
    builder.append(TT.gen("还款状态").toString(locale.locale))
        .append(keyValueSeparator)
        .append(TT.gen(getStatus(displayStatus)).toString(locale.locale))
        .append(lineSeparator)
        .append(lineSeparator);

    for (RepaymentUnitVO repaymentUnit : repaymentUnits) {
      builder.append(TT.gen("订单编号").toString(locale.locale))
          .append(keyValueSeparator)
          .append(YqgHashids.encode(repaymentUnit.orderId))
          .append(lineSeparator)

          .append(TT.gen("还款期数").toString(locale.locale))
          .append(keyValueSeparator)
          .append(instalmentVOMap.get(repaymentUnit.instalmentId).index)
          .append(lineSeparator)

          .append(TT.gen("还款金额").toString(locale.locale))
          .append(keyValueSeparator)
          .append(AmountFormatter.format(sdkType.getCurrency(), repaymentUnit.amount))
          .append(lineSeparator);
    }

    CashLoanRepaymentResponseV2 response = new CashLoanRepaymentResponseV2();
    response.details = builder.toString();
    return response;
  }

  private static String getStatus(CashLoanRepaymentDisplayStatus status) {
    switch (status) {
      case SUCCEEDED:
        return "还款成功";
      case PROCESSING:
        return "还款处理中";
      case FAILED:
        return "还款失败";
      case HANGING:
        return "挂起";
      case REFUNDED:
        return "已退款";
      default:
        throw EcException.error("unsupported repaymentDisplayStatus: " + status);
    }
  }

  private static String getDirectDebitTypeDesc(DirectDebitPaymentType type) {
    switch (type) {
      case USER_ACTION:
      case DIRECT_CONNECT:
        return "快捷支付";
      case AUTO_BATCH_TRIGGER:
        return "系统扣款（自动）";
      case COLLECTION_TRIGGER:
        return "系统扣款（人工）";
      default:
        throw EcException.error("unsupported DirectDebitPaymentType: " + type);
    }
  }
}
