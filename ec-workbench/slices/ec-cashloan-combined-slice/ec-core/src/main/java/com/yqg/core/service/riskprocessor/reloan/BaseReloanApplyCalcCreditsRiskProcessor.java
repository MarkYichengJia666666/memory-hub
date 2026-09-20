package com.yqg.core.service.riskprocessor.reloan;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.cashloan.vo.CashLoanCalcCreditsVO;
import com.yqg.core.service.loan.LoanAssertion;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

public abstract class BaseReloanApplyCalcCreditsRiskProcessor extends BaseRiskProcessor {

  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;

  protected void checkReloanReapplyCredits(LoanUserCreditsInfoRecord creditsInfoRecord, List<LoanUserRiskType> loanUserRiskTypeList, BooleanType submitRisk) {
    LoanCreditsStatus creditsStatus;
    if (Objects.isNull(creditsInfoRecord.getReloanStatus())) {
      creditsStatus = LoanCreditsStatus.fromCode(creditsInfoRecord.getCreditsStatus());
    } else {
      creditsStatus = LoanCreditsStatus.fromCode(creditsInfoRecord.getReloanStatus());
    }

    if (creditsStatus == LoanCreditsStatus.ACCEPTED) {
      CashLoanCalcCreditsVO cashLoanCalcCreditsVO = checkAndGetCashLoanCalcCreditsVO(creditsInfoRecord);
      if (isCreditsExpired(cashLoanCalcCreditsVO, submitRisk, loanUserRiskTypeList)) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("网络异常，请稍后再试"), "can not calc credits with wrong riskType,accountId is {}", creditsInfoRecord.getLoanAccountId());
      }
    } else if (creditsStatus == LoanCreditsStatus.REJECTED) {
      LoanAssertion.assertAccountAndAppInReloanCreditsStatus(creditsInfoRecord, LoanCreditsStatus.REJECTED);
      EcAsserts.assertTrue(checkSubmitRisk(submitRisk) || (Clock.now() > creditsInfoRecord.getTimeReapply() && creditsInfoRecord.getTimeReapply() > 0),
          "It's not the time to {} yet! Account id is {}.", getLoanUserRiskType().name(), creditsInfoRecord.getLoanAccountId());
    } else {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("请勿重复提交"), "current user can't submit risk application, accountId is {}", creditsInfoRecord.getLoanAccountId());
    }
    loanAccountRevolvingService.checkUserInRevolvingLoanProcessCanSubmitRisk(creditsInfoRecord.getLoanAccountId(), getLoanUserRiskType());
  }

  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReloanCreditsApplicationForCreditsStatus(creditsInfoRecord);
  }

  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReloanCreditsApplication(creditsInfoRecord, traceVO.id);
  }
}
