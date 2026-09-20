package com.yqg.core.service.risk.facade.strategy;

import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.notification.param.system.NotifCreditAndOrderParam;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.submitadditional.vo.SubmitCreditsAdditionalInfoVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2025/8/22
 */
@Service
public class ReloanRiskMessageEventStrategy extends BaseRiskMessageEventStrategy {

  @Override
  public boolean supports(LoanUserRiskType riskType) {
    return LoanUserRiskType.RELOAN == riskType || LoanUserRiskType.getReloanCalcCreditWithoutRevolvingRiskType().contains(riskType);
  }

  @Override
  protected boolean isValidSecondRiskType(SubmitCreditsAdditionalInfoVO additionalInfo) {
    // 如果前前一笔订单不是B的类型，则说不走该回捞逻辑
    LoanUserRiskTraceVO prePreTrace = loanUserRiskTraceService.findByIdOrThrow(additionalInfo.getRecord().getPreLoanUserRiskId());
    return LoanUserRiskType.RELOAN == prePreTrace.riskType || LoanUserRiskType.REVOLVING_LOAN_SECOND == prePreTrace.riskType;
  }

  @Override
  protected boolean validateParamPreconditions(LoanUserTagData data) {
    return true;
  }

  @Override
  protected void sendChangeCreditsSuccessMessage(LoanUserRiskTraceVO traceVO, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO) {
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(traceVO.accountId);
    cashLoanCreditsService.sendNotificationForNotReloanRetrievalAndCalcCreditsAccept(accountVO, traceVO.traceId);
  }

  @Override
  protected void sendChangeCreditsRejectionNotification(LoanUserRiskTraceVO traceVO, LoanUserTagData data) {
    loanAccountService.notifCreditFailed(traceVO.userId, traceVO.accountId, SDKType.IDN_YQD, traceVO.traceId);
  }

  @Override
  protected void sendOrderRiskSuccessNotification(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data) {
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(userTraceVO.accountId);
    if (loanUserTypeService.isPendUserType(accountVO.loanUserTypeVO.name)) {
      return;
    }
    CashLoanOrderVO orderVO = ecOrderService.getOrderVO(userTraceVO.orderId);
    NotifCreditAndOrderParam param = new NotifCreditAndOrderParam(accountVO.userId, accountVO.sdkType, orderVO.id, userTraceVO.traceId);
    if (orderVO.status == CashLoanOrderStatus.REJECT) {
      notificationService.pushSystemNotif(SystemNotifScene.RELOAN_CREDIT_SUCCESS_WITH_REJECT_ORDER, param);
    } else {
      notificationService.pushSystemNotif(SystemNotifScene.RELOAN_CREDIT_SUCCESS_WITH_ACCEPT_ORDER, param);
    }
    // todo chenxianrui (未来降额发消息的代码都迁移到此处) com.yqg.core.service.cashloan.CashLoanService.pushMessageForInsufficientQuota
  }

  @Override
  protected void sendOrderRiskRejectNotification(LoanUserRiskTraceVO traceVO, LoanUserTagData data) {
    loanAccountService.notifCreditFailed(traceVO.userId, traceVO.accountId, SDKType.IDN_YQD, traceVO.traceId);
  }
}
