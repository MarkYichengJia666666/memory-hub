package com.yqg.core.service.riskprocessor.reloan;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author shubo
 * @date 2022/5/31 2:24 下午
 */
@Service
public class BatchTriggerReloanRiskProcessor extends BaseReloanApplyCalcCreditsRiskProcessor {
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.BATCH_TRIGGER_RELOAN;
  }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    //复贷跑批需要考虑复贷的额度测算等风控类型
    if (LoanCreditsStatus.fromCode(creditsInfoRecord.getCreditsStatus()) == LoanCreditsStatus.ACCEPTED && creditsInfoRecord.getReloanStatus() == null) {
      return;
    }
    checkReloanReapplyCredits(creditsInfoRecord, LoanUserRiskType.getReLoanCalcRiskType(), param.extraInfo.submitRisk);
  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    loanUserCreditsService.resetReapplyTime(param.accountId);
  }
}
