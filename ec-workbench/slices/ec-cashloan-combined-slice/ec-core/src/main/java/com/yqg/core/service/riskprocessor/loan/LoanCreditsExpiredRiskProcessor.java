package com.yqg.core.service.riskprocessor.loan;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.infos.SubmitCreditsInfo;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author shubo
 * @date 2022/6/15 03:52 下午
 */
@Service
public class LoanCreditsExpiredRiskProcessor extends BaseRiskProcessor {
  @Autowired
  private SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private LoanAccountService loanAccountService;

  @Override
  public LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.LOAN_RE_CREDITS;
  }

  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReapplyCreditsApplicationForCreditsStatus(creditsInfoRecord);
  }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    if (loanAccountService.isReloan(creditsInfoRecord.getLoanAccountId())) {
      throw EcException.error("the user of credits status is reloan,cannot submit {}, accountId is {}", getLoanUserRiskType().name(), creditsInfoRecord.getLoanAccountId());
    }

    //需要使用其中的检查
    checkAndGetCashLoanCalcCreditsVO(creditsInfoRecord);
  }

  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReapplyCreditsApplication(creditsInfoRecord, traceVO.id);
  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    if (param.needSaveContextInfo && Objects.nonNull(param.terminalInfo) && Objects.nonNull(param.environmentInfo)) {
      SubmitCreditsInfo submitCreditsInfo = new SubmitCreditsInfo();
      submitCreditsInfo.terminalInfo = param.terminalInfo;
      submitCreditsInfo.environmentInfo = param.environmentInfo;
      submitCreditsAdditionalInfoService.saveContextInfoIgnoreException(param.accountId, traceVO.getId(), submitCreditsInfo, getLoanUserRiskType());
    }
  }
}
