package com.yqg.core.service.risk.facade.strategy;

import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.submitadditional.vo.SubmitCreditsAdditionalInfoVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2025/8/22
 */
@Service
@Slf4j
public class LoanRiskMessageEventStrategy extends BaseRiskMessageEventStrategy {

  @Override
  public boolean supports(LoanUserRiskType riskType) {
    return LoanUserRiskType.getLoanOrderRiskTypes().contains(riskType) ||
        LoanUserRiskType.getLoanCalcRiskType().contains(riskType);
  }

  @Override
  protected boolean isValidSecondRiskType(SubmitCreditsAdditionalInfoVO additionalInfo) {
    LoanUserRiskTraceVO prePreTrace = loanUserRiskTraceService.findByIdOrThrow(additionalInfo.getRecord().getPreLoanUserRiskId());
    return LoanUserRiskType.getLoanOrderRiskTypes().contains(prePreTrace.riskType);
  }

  @Override
  protected boolean validateParamPreconditions(LoanUserTagData data) {
    return true;
  }

  @Override
  protected void sendChangeCreditsSuccessMessage(LoanUserRiskTraceVO traceVO, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO) {
    LoanAccountRecord loanAccountRecord = loanAccountModel.findByIdForUpdateOrThrow(traceVO.accountId);
    cashLoanCreditsService.acceptCreditsApplicationMsg(traceVO.traceId, true, loanAccountRecord, null, traceVO.userId);
  }

  @Override
  protected void sendChangeCreditsRejectionNotification(LoanUserRiskTraceVO traceVO, LoanUserTagData data) {
    loanAccountService.notifCreditFailed(traceVO.userId, traceVO.accountId, SDKType.IDN_YQD, traceVO.traceId);
  }

  @Override
  protected void sendOrderRiskSuccessNotification(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data) {
    if (data == null) {
      return;
    }
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(userTraceVO.accountId);
    if (loanUserTypeService.isPendUserType(accountVO.loanUserTypeVO.name)) {
      return;
    }
    cashLoanCreditsService.processFirstLoanOrderAndSendMessage(accountVO, BooleanUtils.isTrue(data.rejectOrder), userTraceVO.traceId);
    // todo chenxianrui (未来降额发消息的代码都迁移到此处) com.yqg.core.service.cashloan.CashLoanService.pushMessageForInsufficientQuota
  }

  @Override
  protected void sendOrderRiskRejectNotification(LoanUserRiskTraceVO traceVO, LoanUserTagData data) {
    loanAccountService.notifCreditFailed(traceVO.userId, traceVO.accountId, SDKType.IDN_YQD, traceVO.traceId);
  }
}

