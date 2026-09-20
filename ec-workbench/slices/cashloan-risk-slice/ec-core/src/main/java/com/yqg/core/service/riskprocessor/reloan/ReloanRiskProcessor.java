package com.yqg.core.service.riskprocessor.reloan;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.risk.event.RiskEventService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/29 10:45 上午
 */
@Service
public class ReloanRiskProcessor extends BaseRiskProcessor {
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private RiskEventService riskEventService;

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.RELOAN;
  }

  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReloanCreditsApplicationForCreditsStatus(creditsInfoRecord);
  }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    assertCreditsStatus(creditsInfoRecord, LoanCreditsStatus.ACCEPTED);
  }

  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReloanCreditsApplication(creditsInfoRecord, traceVO.id);
  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    ecOrderService.updateTraceId(param.orderId, traceVO.id);
  }
}
