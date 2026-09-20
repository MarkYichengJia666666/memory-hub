package com.yqg.core.service.riskprocessor.multi;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatus;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatusChangeSource;
import com.yqg.core.model.sql.loanusertrace.TriggerType;
import com.yqg.core.service.cashloan.CashLoanCalcCreditsService;
import com.yqg.core.service.cashloan.enums.CashLoanCalcCreditsStatus;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.infos.SubmitCreditsInfo;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.event.RiskEventService;
import com.yqg.core.service.risk.event.RiskEventType;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public abstract class BaseMultiLoanCalcCreditsRiskProcessor extends BaseRiskProcessor {
  @Autowired
  protected SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private RiskEventService riskEventService;
  @Autowired
  private CashLoanCalcCreditsService calcCreditsService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;

  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReloanCreditsApplicationForCreditsStatus(creditsInfoRecord);
    MultiLoanStatusChangeSource statusChangeSource = TriggerType.MANUAL == param.triggerType ? MultiLoanStatusChangeSource.MANUAL_CALC : MultiLoanStatusChangeSource.AUTO_CALC;
    multiLoanStatusService.updateStatus(param.accountId, statusChangeSource);
  }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord record, RiskProcessParam param) {
    assertCreditsStatus(record, LoanCreditsStatus.ACCEPTED);

    // 判断是否有未完成订单(INIT、RESERVE、CHECK)
    int uncompletedOrder = ecOrderService.countOrder(record.getLoanAccountId(), CashLoanOrderStatus.UNDONE_STATUSES_WITHOUT_READY);
    if (uncompletedOrder > 0) {
      throw EcException.error("can not calc credits with undone orders,accountId is {}", record.getLoanAccountId());
    }

    MultiLoanStatus multiLoanStatus = multiLoanStatusService.getStatusOrThrow(record.getLoanAccountId());
    if (MultiLoanStatus.INIT != multiLoanStatus) {
      throw EcException.error("invalid multi loan credits, expect is {}, cur is {}, accountId is {}", MultiLoanStatus.INIT, multiLoanStatus, record.getLoanAccountId());
    }

    CashLoanCalcCreditsStatus calcCreditsStatus = calcCreditsService.getMultiLoanCalcCreditsStatus(record.getLoanAccountId());
    if (calcCreditsStatus != CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("网络异常，请稍后再试"), "current credits status do not expired, accountId is {}", record.getLoanAccountId());
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

    LoanUserCreditsInfoVO loanUserCreditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(param.accountId);
    riskEventService.publishEvent(RiskEventType.CREDIT_CALC_INIT, getLoanUserRiskType(), traceVO.id, loanUserCreditsInfoVO);

    if (param.needSaveContextInfo && Objects.nonNull(param.terminalInfo) && Objects.nonNull(param.environmentInfo)) {
      SubmitCreditsInfo submitCreditsInfo = new SubmitCreditsInfo();
      submitCreditsInfo.terminalInfo = param.terminalInfo;
      submitCreditsInfo.environmentInfo = param.environmentInfo;
      submitCreditsAdditionalInfoService.saveContextInfoIgnoreException(param.accountId, traceVO.getId(), submitCreditsInfo, getLoanUserRiskType());
    }
  }
}
