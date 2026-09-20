package com.yqg.core.service.cashloan.repay.vo;

import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.sql.payment.enums.PaymentStatus;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.i18n.EcCurrency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UnionRepaymentVO {
  public Long userId;
  public Long accountId;
  public Long transactionTime;
  public PaymentProvider paymentProvider;
  public PaymentStatus paymentStatus;
  public UnionRepaymentType deductType;
  public String transNo;
  public CurrencyAmount amount;
  public SDKType sdkType;

  public static UnionRepaymentVO from(RepaySplitUnitVO repaySplitUnitVO, PaymentVO paymentVO, LoanAccountRecord loanAccountRecord) {
    UnionRepaymentVO unionRepaymentVO = new UnionRepaymentVO();
    unionRepaymentVO.userId = paymentVO.getUserId();
    unionRepaymentVO.accountId = loanAccountRecord.getId();
    unionRepaymentVO.transactionTime = paymentVO.getTransactionTime();
    unionRepaymentVO.paymentProvider = paymentVO.getPaymentProvider();
    unionRepaymentVO.paymentStatus = paymentVO.status;
    unionRepaymentVO.deductType = repaySplitUnitVO.deductType;
    unionRepaymentVO.transNo = paymentVO.thirdPartyPayOrderId;
    unionRepaymentVO.amount = CurrencyAmount.fromYuan(EcCurrency.IDR, repaySplitUnitVO.amount);
    unionRepaymentVO.sdkType = SDKType.fromCode(loanAccountRecord.getSdkType());
    return unionRepaymentVO;
  }

  /**
   * 根据 EC 还款信息构造 UnionRepaymentVO 对象（仅 UT 使用）
   */
  public static UnionRepaymentVO from(PaymentVO paymentVO, Long loanAccountId, SDKType sdkType) {
    UnionRepaymentVO unionRepaymentVO = new UnionRepaymentVO();
    unionRepaymentVO.userId = paymentVO.getUserId();
    unionRepaymentVO.accountId = loanAccountId;
    unionRepaymentVO.transactionTime = paymentVO.getTransactionTime();
    unionRepaymentVO.paymentProvider = paymentVO.getPaymentProvider();
    unionRepaymentVO.paymentStatus = paymentVO.status;
    unionRepaymentVO.deductType = UnionRepaymentType.EC_REPAY;
    unionRepaymentVO.transNo = paymentVO.thirdPartyPayOrderId;
    unionRepaymentVO.amount = paymentVO.amount;
    unionRepaymentVO.sdkType = sdkType;
    return unionRepaymentVO;
  }
}