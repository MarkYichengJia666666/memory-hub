package com.yqg.core.service.riskprocessor.loan;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author shubo
 * @date 2022/5/31 3:04 下午
 */
@Service
public class BatchTriggerLoanRiskProcessor extends BaseLoanApplyCalcCreditsRiskProcessor {

  @Autowired
  private LoanUserCreditsService loanUserCreditsService;

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.BATCH_TRIGGER_LOAN;
  }

  @Override
  protected void preAdditionalProcess(RiskProcessParam param){
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReapplyCreditsApplicationForCreditsStatus(creditsInfoRecord);
  }
  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    checkLoanReapply(creditsInfoRecord, LoanUserRiskType.getLoanCalcRiskType(), param.extraInfo.submitRisk);
  }

  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReapplyCreditsApplication(creditsInfoRecord, traceVO.id);
  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    loanUserCreditsService.resetReapplyTime(param.accountId);
  }
}
