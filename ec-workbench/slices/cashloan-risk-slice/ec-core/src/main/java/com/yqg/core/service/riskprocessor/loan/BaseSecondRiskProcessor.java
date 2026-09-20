package com.yqg.core.service.riskprocessor.loan;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.loan.LoanAssertion;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author shubo
 * @date 2022/8/12 2:47 下午
 */
public abstract class BaseSecondRiskProcessor extends BaseRiskProcessor {
  @Autowired
  private EcOrderService ecOrderService;

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    LoanAssertion.assertAccountAndAppInCreditsStatus(creditsInfoRecord, LoanCreditsStatus.ACCEPTED);
  }

  @Override
  // 二次风控不更新credits_info的traceId
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitSecondCreditsApplication(creditsInfoRecord);
  }

  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {

  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    ecOrderService.updateTraceId(param.orderId, traceVO.id);
 }
}
