package com.yqg.core.service.cashloan.repay;

import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repay.vo.DeductIntentionVO;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentVO;
import com.yqg.core.service.payment.vo.PaymentProcessResult;
import org.springframework.stereotype.Component;

@Component
public abstract class UnionRepaymentHandler {

  // 抽象方法：获取当前处理器支持的还款类型
  public abstract UnionRepaymentType getSupportedType();

  // 抽象方法：计算欠款金额
  //protected abstract BigDecimal calculateOwedAmount(RepaymentRequest request);

  // 模板方法：处理请求
  public abstract PaymentProcessResult handleRepayment(UnionRepaymentVO unionRepaymentVO, Long repayUnitId, DeductIntentionVO deductIntentionVO);
}
