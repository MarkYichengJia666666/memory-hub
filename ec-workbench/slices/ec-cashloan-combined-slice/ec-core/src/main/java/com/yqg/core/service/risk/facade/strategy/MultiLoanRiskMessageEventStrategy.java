package com.yqg.core.service.risk.facade.strategy;

import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.submitadditional.vo.SubmitCreditsAdditionalInfoVO;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author chenxianrui
 * @date 2025/8/22
 */
@Service
@Slf4j
public class MultiLoanRiskMessageEventStrategy extends BaseRiskMessageEventStrategy {

  @Override
  public boolean supports(LoanUserRiskType riskType) {
    return LoanUserRiskType.getAllMultiLoanRiskType().contains(riskType);
  }

  @Override
  protected void sendChangeCreditsSuccessMessage(LoanUserRiskTraceVO traceVO, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO) {
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(traceVO.accountId);
    riskMessageTool.sendMultiLoanChangeCreditsSuccessMessage(accountVO, traceVO.traceId, data);
  }

  @Override
  protected boolean isValidSecondRiskType(SubmitCreditsAdditionalInfoVO additionalInfo) {
    // 如果前前一笔订单不是续借的类型，则不走该回捞逻辑
    LoanUserRiskTraceVO prePreTrace = loanUserRiskTraceService.findByIdOrThrow(additionalInfo.getRecord().getPreLoanUserRiskId());
    return LoanUserRiskType.getMultiLoanRiskType().contains(prePreTrace.riskType) || LoanUserRiskType.REVOLVING_LOAN_SECOND == prePreTrace.riskType;
  }

  @Override
  protected boolean validateParamPreconditions(LoanUserTagData data) {
    if (data == null) {
      log.error("data can not be null when multi risk");
      return false;
    }
    return true;
  }

  @Override
  protected void sendChangeCreditsRejectionNotification(LoanUserRiskTraceVO traceVO, LoanUserTagData data) {
    riskMessageTool.sendMultiLoanChangeCreditsRejectMessage(traceVO.accountId, traceVO.traceId, data);
    log.info("拒绝且未提交降级风控，发送拒绝消息，traceId:{}", traceVO.traceId);
  }

  @Override
  protected void sendOrderRiskSuccessNotification(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data) {
    CashLoanOrderVO orderVO = ecOrderService.getOrderVO(userTraceVO.orderId);
    if (orderVO.status == CashLoanOrderStatus.REJECT) {
      notificationService.pushSystemNotif(SystemNotifScene.MULTI_LOAN_CREDIT_SUCCESS_WITH_REJECT_ORDER, riskMessageTool.buildSendNotifParam(userTraceVO.accountId, userTraceVO.traceId));
      return;
    }
    // 续借订单风控被拒，且没有提交降级风控才会发消息
    notificationService.pushSystemNotif(SystemNotifScene.RELOAN_CREDIT_SUCCESS_WITH_ACCEPT_ORDER, riskMessageTool.buildSendNotifParam(userTraceVO.accountId, userTraceVO.traceId));
  }

  @Override
  protected void sendOrderRiskRejectNotification(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data) {
    notificationService.pushSystemNotif(SystemNotifScene.MULTI_LOAN_CREDIT_SUCCESS_WITH_REJECT_ORDER, riskMessageTool.buildSendNotifParam(userTraceVO.accountId, userTraceVO.traceId));
  }
}