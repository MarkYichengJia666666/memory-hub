package com.yqg.core.service.risk.facade;

import com.yqg.core.model.sql.loanusertrace.LoanRiskCreditStage;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.notification.NotificationService;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.notification.param.system.NotifCreditAndOrderParam;
import com.yqg.core.service.notification.param.system.NotifCreditWithTraceIdParam;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author chaoye
 * @date 2025/10/13
 */
@Slf4j
@Service
public class RiskMessageTool {

  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private NotificationService notificationService;
  @Autowired
  private RiskFacadeTool riskFacadeTool;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private EcOrderService ecOrderService;

  public void sendMultiLoanChangeCreditsSuccessMessage(LoanAccountVO accountVO, Long traceId, LoanUserTagData data) {

    boolean hasRemainCredits = hasRemainCredits(accountVO);
    if (!hasRemainCredits) {
      log.info("sendMultiLoanChangeCreditsSuccessMessage accountId : {}, traceId : {}, 无可用额度，不发送续借额度授信成功消息", accountVO.id, traceId);
      return;
    }

    NotifCreditWithTraceIdParam creditWithTraceIdParam = getCreditWithTraceIdParamForMultiLoanCalc(accountVO, traceId, data);
    sendMultiLoanChangeCreditsSuccessMessageV2(creditWithTraceIdParam);
  }

  public void sendMultiLoanChangeCreditsSuccessMessageV2(NotifCreditWithTraceIdParam creditWithTraceIdParam) {

    notificationService.pushSystemNotifSync(SystemNotifScene.MULTI_LOAN_CREDITS_CHANGE_SUCCESS, creditWithTraceIdParam);
  }

  public boolean hasRemainCredits(LoanAccountVO accountVO) {
    BigDecimal totalRemainCredits = loanUserCreditsService.getTotalRemainCredits(accountVO.id);
    if (totalRemainCredits.compareTo(BigDecimal.ZERO) <= 0) {
      return false;
    }
    return true;
  }


  public boolean sendMultiLoanChangeCreditsRejectMessage(Long accountId, Long traceId, LoanUserTagData data) {
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(accountId);

    NotifCreditWithTraceIdParam creditWithTraceIdParam = getCreditWithTraceIdParamForMultiLoanCalc(accountVO, traceId, data);

    notificationService.pushSystemNotif(SystemNotifScene.MULTI_LOAN_CREDITS_CHANGE_FAILED, creditWithTraceIdParam);
    return true;
  }

  private NotifCreditWithTraceIdParam getCreditWithTraceIdParamForMultiLoanCalc(LoanAccountVO accountVO,
                                                                                Long traceId,
                                                                                LoanUserTagData data) {
    LoanRiskCreditStage originLoanUserRiskCategory = riskFacadeTool.getOriginLoanUserRiskCategoryForMultiCalc(traceId);
    LoanUserRiskTriggerSource originLoanUserRiskTriggerSource = riskFacadeTool.getOriginTriggerSourceForMultiCalc(traceId);
    return new NotifCreditWithTraceIdParam(
        accountVO.userId,
        accountVO.sdkType,
        traceId,
        data.multiLoanReductionInterestTag,
        riskFacadeTool.getTriggerSubType(traceId),
        originLoanUserRiskCategory,
        originLoanUserRiskTriggerSource
    );
  }

  public NotifCreditAndOrderParam buildSendNotifParam(Long accountId, Long traceId) {
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(accountId);
    CashLoanOrderVO orderVO = ecOrderService.getLatestOrderVO(accountVO.id);
    return new NotifCreditAndOrderParam(accountVO.userId, accountVO.sdkType, orderVO.id, traceId);
  }

}
