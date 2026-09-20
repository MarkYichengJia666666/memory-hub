package com.miyou.controllers.cashloan.repayment.calculate;

import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.translation.client.utils.TT;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
public class RepaymentCollectionReductionCalculateResponse {

  /**
   * 是否命中催收减免策略
   */
  public final boolean enabled;

  /**
   * 减免提示文案（可为null，如未命中策略时无文案）
   */
  public final TT tip;

  /**
   * 调整后金额（可为null，如未命中策略时无调整金额）
   */
  public final BigDecimal adjustedAmount;

  public static RepaymentCollectionReductionCalculateResponse noCollectionReductionResponse() {
    return RepaymentCollectionReductionCalculateResponse.builder()
        .enabled(false)
        .tip(null)
        .adjustedAmount(null)
        .build();
  }

  public static RepaymentCollectionReductionCalculateResponse allCoveredResponse() {
    return RepaymentCollectionReductionCalculateResponse.builder()
        .enabled(true)
        .tip(TT.gen("利息和逾期利息已享减免"))
        .build();
  }

  public static RepaymentCollectionReductionCalculateResponse partialCoveredResponse(EcCurrency currency, BigDecimal uncoveredAmount, BigDecimal adjustedAmount) {
    return RepaymentCollectionReductionCalculateResponse.builder()
        .enabled(true)
        .tip(TT.gen("再还 {0} 可立即享受限时减免活动", AmountFormatter.format(currency, uncoveredAmount)))
        .adjustedAmount(adjustedAmount)
        .build();
  }

}
