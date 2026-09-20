package com.yqg.core.service.risk.facade.strategy;

import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.enums.RevolvingCreditNotifResult;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.notification.param.system.RevolvingCreditsCalculateResultParam;
import com.yqg.core.service.notification.param.system.RevolvingCreditsSecondResultParam;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.submitadditional.vo.SubmitCreditsAdditionalInfoVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2025/8/22
 */
@Service
@Slf4j
public class RevolvingRiskMessageEventStrategy extends BaseRiskMessageEventStrategy {

  @Override
  public boolean supports(LoanUserRiskType riskType) {
    return LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(riskType);
  }

  @Override
  protected boolean isValidSecondRiskType(SubmitCreditsAdditionalInfoVO additionalInfo) {
    return false;
  }

  @Override
  protected boolean validateParamPreconditions(LoanUserTagData data) {
    return true;
  }

  @Override
  protected void sendChangeCreditsSuccessMessage(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO) {
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(userTraceVO.accountId);
    RevolvingCreditNotifResult result = RevolvingCreditNotifResult.FIRST_ACCEPT;
    long traceId = userTraceVO.traceId;
    //优先看管制
    try {
      if (loanAccountRevolvingService.checkRevolvingUserInControlProcess(accountVO.id)) {
        result = RevolvingCreditNotifResult.IN_CONTROL;
      }
    } catch (Exception e) {
      log.error("checkRevolvingUserInControlProcess error,accountId is {}, traceId is {}", accountVO.id, traceId, e);
    }

    //没触发回捞，发消息
    marketingCenterClientService.publishSystemEvent(SystemNotifScene.REVOLVING_CREDITS_CALCULATE_RESULT,
        new RevolvingCreditsCalculateResultParam(accountVO.userId, accountVO.sdkType, traceId, riskFacadeTool.getTriggerSubType(traceId),
            result));
  }

  @Override
  protected void sendChangeCreditsRejectionNotification(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data) {
    long traceId = userTraceVO.traceId;
    marketingCenterClientService.publishSystemEvent(SystemNotifScene.REVOLVING_CREDITS_CALCULATE_RESULT,
        new RevolvingCreditsCalculateResultParam(userTraceVO.userId, SDKType.IDN_YQD, traceId, riskFacadeTool.getTriggerSubType(traceId), RevolvingCreditNotifResult.FIRST_REJECT));
  }

  @Override
  protected void sendOrderRiskSuccessNotification(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data) {
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(userTraceVO.accountId);
    if (loanUserTypeService.isPendUserType(accountVO.loanUserTypeVO.name)) {
      return;
    }
    CashLoanOrderVO orderVO = ecOrderService.getOrderVO(userTraceVO.orderId);

    RevolvingCreditNotifResult result = cashLoanCreditsService.getRevolvingCreditNotifResultForReloanSecondAccept(accountVO, orderVO);
    marketingCenterClientService.publishSystemEvent(SystemNotifScene.REVOLVING_CREDITS_SECOND_RESULT,
        new RevolvingCreditsSecondResultParam(orderVO.userId, orderVO.sdkType, orderVO.id, result, userTraceVO.traceId));
  }

  @Override
  protected void sendOrderRiskRejectNotification(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data) {
    long traceId = userTraceVO.traceId;
    marketingCenterClientService.publishSystemEvent(SystemNotifScene.REVOLVING_CREDITS_SECOND_RESULT,
        new RevolvingCreditsCalculateResultParam(userTraceVO.userId, SDKType.IDN_YQD, traceId, riskFacadeTool.getTriggerSubType(traceId),
            RevolvingCreditNotifResult.SECOND_REJECT));
  }
}
