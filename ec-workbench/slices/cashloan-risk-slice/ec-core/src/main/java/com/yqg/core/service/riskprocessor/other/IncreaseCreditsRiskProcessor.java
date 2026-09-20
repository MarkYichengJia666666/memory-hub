package com.yqg.core.service.riskprocessor.other;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.generated.tables.records.LoanUserExtraInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.image.LoanUserExtraInfoModel;
import com.yqg.core.service.loan.image.LoanUserExtraInfoReviewService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.extrainfo.ExtraInfoReviewStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author shubo
 * @date 2023/7/18 10:39 上午
 */
@Service
public class IncreaseCreditsRiskProcessor extends BaseRiskProcessor {
  @Autowired
  private LoanUserExtraInfoReviewService loanUserExtraInfoReviewService;
  @Autowired
  private LoanUserExtraInfoModel loanUserExtraInfoModel;

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.INCREASE_CREDITS;
  }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    if (creditsInfoRecord == null) {
      throw EcException.error("the user of credits status is null,cannot submit {}", getLoanUserRiskType().name());
    }
    if (LoanCreditsStatus.fromCodeOrNull(creditsInfoRecord.getCreditsStatus()) == LoanCreditsStatus.REJECTED
        || LoanCreditsStatus.fromCodeOrNull(creditsInfoRecord.getReloanStatus()) == LoanCreditsStatus.REJECTED) {
      throw EcException.warn(EcExceptionType.LOAN_ACCOUNT_INCREASE_CREDITS_REJECT, TT.gen("user credits status is reject"));
    }
  }

  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserExtraInfoRecord extraInfoRecord = loanUserExtraInfoModel.fetchById(param.loanUserExtraInfoId);
    loanUserExtraInfoReviewService.updateStatus(extraInfoRecord, ExtraInfoReviewStatus.RISK_PENGING);
  }

  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserExtraInfoRecord extraInfoRecord = loanUserExtraInfoModel.fetchById(param.loanUserExtraInfoId);
    loanUserExtraInfoReviewService.updateTraceId(extraInfoRecord, traceVO.id);
  }
}
