package com.yqg.core.service.riskprocessor.multi;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatus;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatusChangeSource;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.exception.EcException;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseMultiLoanRiskProcessor extends BaseRiskProcessor {
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;

  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReloanCreditsApplicationForCreditsStatus(creditsInfoRecord);
    multiLoanStatusService.updateStatus(param.accountId, MultiLoanStatusChangeSource.MANUAL_ORDER);
  }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord record, RiskProcessParam param) {
    assertCreditsStatus(record, LoanCreditsStatus.ACCEPTED);
    MultiLoanStatus multiLoanStatus = multiLoanStatusService.getStatusOrThrow(record.getLoanAccountId());
    if (MultiLoanStatus.CALC_ACCEPTED != multiLoanStatus) {
      throw EcException.error("invalid multi loan credits, expect is init, expect is {}, cur is {}, accountId is {}", MultiLoanStatus.CALC_ACCEPTED, multiLoanStatus, record.getLoanAccountId());
    }
    loanAccountRevolvingService.checkUserInRevolvingLoanProcessCanSubmitRisk(record.getLoanAccountId(), getLoanUserRiskType());

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
