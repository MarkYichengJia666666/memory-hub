package com.yqg.core.service.riskprocessor.loan;

import com.google.common.collect.ImmutableList;
import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.generated.tables.records.RiskIncreaseCreditsReviewLogRecord;
import com.yqg.core.model.sql.risk.RiskIncreaseCreditsReviewLogModel;
import com.yqg.core.model.sql.risk.enums.RiskIncreaseCreditsReviewStatus;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.infos.SubmitCreditsInfo;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.event.RiskEventService;
import com.yqg.core.service.risk.event.RiskEventType;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/8/25 11:29 上午
 */
@Service
public class ReapplyRiskProcessor extends BaseLoanApplyCalcCreditsRiskProcessor {
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private RiskEventService riskEventService;
  @Autowired
  private SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private RiskIncreaseCreditsReviewLogModel riskIncreaseCreditsReviewLogModel;

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.REAPPLY;
  }

  @Override
  protected void preAdditionalProcess(RiskProcessParam param){
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReapplyCreditsApplicationForCreditsStatus(creditsInfoRecord);
  }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    checkLoanReapply(creditsInfoRecord, ImmutableList.of(getLoanUserRiskType()), param.extraInfo.submitRisk);
  }

  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReapplyCreditsApplication(creditsInfoRecord, traceVO.id);
  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    loanUserCreditsService.resetReapplyTime(param.accountId);
    LoanUserCreditsInfoVO loanUserCreditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(param.accountId);
    riskEventService.publishEvent(RiskEventType.REAPPLY, getLoanUserRiskType(), traceVO.id, loanUserCreditsInfoVO);
    if (param.needSaveContextInfo && Objects.nonNull(param.environmentInfo) && Objects.nonNull(param.terminalInfo)) {
      submitCreditsAdditionalInfoService.saveContextInfoIgnoreException(param.accountId, traceVO.getId(), SubmitCreditsInfo.from(param.environmentInfo, param.terminalInfo), getLoanUserRiskType());
    }
    if (Objects.nonNull(param.reviewLogRecordId)) {
      RiskIncreaseCreditsReviewLogRecord reviewLogRecord = riskIncreaseCreditsReviewLogModel.findById(param.reviewLogRecordId);
      riskIncreaseCreditsReviewLogModel.updateStatusAndTraceId(reviewLogRecord, RiskIncreaseCreditsReviewStatus.SUBMIT_RISK, traceVO.id);
    }
  }
}
