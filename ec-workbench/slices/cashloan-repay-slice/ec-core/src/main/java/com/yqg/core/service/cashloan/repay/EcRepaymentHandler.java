package com.yqg.core.service.cashloan.repay;

import com.yqg.core.aop.RunInTransaction;
import com.yqg.core.model.generated.tables.records.RepaymentSplitUnitRecord;
import com.yqg.core.model.sql.cashloan.RepaymentSplitUnitModel;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.cashloan.repay.vo.DeductIntentionVO;
import com.yqg.core.service.cashloan.repay.vo.UnionRepaymentVO;
import com.yqg.core.service.payment.vo.PaymentProcessResult;
import com.yqg.ec.common.exception.EcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EcRepaymentHandler extends UnionRepaymentHandler {
  @Autowired
  private CashLoanService cashLoanService;
  @Autowired
  private RepaymentSplitUnitModel repaymentSplitUnitModel;

  @Override
  public UnionRepaymentType getSupportedType() {
    return UnionRepaymentType.EC_REPAY;
  }

  // 模板方法：处理请求
  @RunInTransaction
  public PaymentProcessResult handleRepayment(UnionRepaymentVO unionRepaymentVO, Long repayUnitId, DeductIntentionVO deductIntentionVO) {
    PaymentProcessResult paymentProcessResult = cashLoanService.repayByUnion(unionRepaymentVO);
    RepaymentSplitUnitRecord record = repaymentSplitUnitModel.fetchById(repayUnitId);
    if (paymentProcessResult.processStatus != ProcessStatus.UNPROCESSED) {
      repaymentSplitUnitModel.updateStatusByRecord(record, paymentProcessResult.processStatus);
      return paymentProcessResult;
    }
    throw EcException.error("handle Ec repayment failed! TransId:{},userId:{},", unionRepaymentVO.transNo, unionRepaymentVO.userId);
  }
}