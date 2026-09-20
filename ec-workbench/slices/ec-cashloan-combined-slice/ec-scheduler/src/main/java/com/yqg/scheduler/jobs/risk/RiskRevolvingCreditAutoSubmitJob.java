package com.yqg.scheduler.jobs.risk;

import com.google.common.util.concurrent.RateLimiter;
import com.yqg.core.model.sql.loan.account.RevolvingChangeSource;
import com.yqg.core.model.sql.loan.account.RevolvingStatus;
import com.yqg.core.service.cashloan.event.CashLoanEventType;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.riskprocessor.infra.RiskTriggerSourceInfo;
import com.yqg.core.service.loan.account.RevolvingUserGroupAbtestService;
import com.yqg.core.service.loan.account.RevolvingUserGroupService;
import com.yqg.core.service.loan.account.vo.LoanAccountRevolvingCreditVO;
import com.yqg.core.service.loan.account.vo.RevolvingV2MonitorVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaoye
 * @date 2025/3/10
 */
@Service
@Slf4j
public class RiskRevolvingCreditAutoSubmitJob extends YqgBaseJob {

  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private RevolvingUserGroupService revolvingUserGroupService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private RevolvingUserGroupAbtestService revolvingUserGroupAbtestService;

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    Param param = getParam(context, Param.class);
    RateLimiter rateLimiter = RateLimiter.create(param.rateLimiter);

    List<Long> accountIds = loanAccountRevolvingService.findNeedAutoSubmitIgnoringControl();
    log.info("[Revolving] AutoSubmitJob find accountIds size={}, ids={}", accountIds.size(), JsonUtils.toString(accountIds));
    processNormalAccounts(accountIds, rateLimiter);
  }

  private void processNormalAccounts(List<Long> accountIds, RateLimiter rateLimiter) {
    for (Long accountId : accountIds) {
      if (isInterrupted()) {
        break;
      }
      rateLimiter.acquire();
      if (shouldExitRevolvingByOldReloan(accountId)) {
        exitRevolvingAndTriggerNonRevolvingRisk(accountId);
      } else {
        loanAccountService.tryAutoSubmitRevolvingCreditRiskAfterUserAction(accountId, SDKType.IDN_YQD, null);
      }

    }
  }

  /**
   * 仅当「实时账龄判定为老客复贷」且「为纯新客」时才退出。
   */
  private boolean shouldExitRevolvingByOldReloan(Long accountId) {
    if (!revolvingUserGroupService.isOldReloanUserByAccountAge(accountId)) {
      return false;
    }
    if (!revolvingUserGroupService.isPureNewUser(accountId)) {
      return false;
    }
    log.info("[Revolving] AutoSubmitJob accountId={} is old reloan user by account age and entered revolving as pure new user, exit revolving process", accountId);
    return true;
  }

  private void exitRevolvingAndTriggerNonRevolvingRisk(Long accountId) {
    if (loanAccountService.checkUserDeletedOrInReview(accountId)) {
      log.info("[Revolving] AutoSubmitJob accountId={} is deleted or in review, skip old reloan exit process", accountId);
      return;
    }
    LoanAccountRevolvingCreditVO revolvingCreditVO = loanAccountRevolvingService.fetchByLoanAccountId(accountId);
    Long userId = revolvingCreditVO != null ? revolvingCreditVO.getUserId() : null;
    try {
      loanAccountRevolvingService.updateRevolvingLoanStatus(accountId, null, RevolvingChangeSource.OLD_RELOAN_USER_EXIT, RevolvingStatus.EXIT);
      int uncompletedOrderCount = ecOrderService.countUncompletedOrder(accountId);
      if (uncompletedOrderCount == 0) {
        loanAccountService.tryAutoSubmitCalcCreditsApplication(accountId, SDKType.IDN_YQD, CashLoanEventType.ORDER_COMPLETED,
            RiskTriggerSourceInfo.of(LoanUserRiskTriggerSource.EXIT_REVOLVING, null));
        log.info("[Revolving] AutoSubmitJob accountId={} exit revolving, trigger calc credits C success", accountId);
      } else {
        loanAccountService.handleMultiLoanWhenRepayCompletedTerms(accountId, CashLoanEventType.REPAYMENT_SUCCEED_NEW,
            RiskTriggerSourceInfo.of(LoanUserRiskTriggerSource.EXIT_REVOLVING, null));
        log.info("[Revolving] AutoSubmitJob accountId={} exit revolving, trigger repay risk calc success", accountId);
      }
      revolvingUserGroupAbtestService.monitorRevolvingV2(RevolvingV2MonitorVO.fromWithOldReloanAutoExit(userId, accountId));
    } catch (Exception e) {
      log.error("[Revolving] AutoSubmitJob accountId={} exit revolving and trigger non-revolving risk failed", accountId, e);
    }
  }

  public static class Param {
    public Double rateLimiter = 5.0;
  }

}
