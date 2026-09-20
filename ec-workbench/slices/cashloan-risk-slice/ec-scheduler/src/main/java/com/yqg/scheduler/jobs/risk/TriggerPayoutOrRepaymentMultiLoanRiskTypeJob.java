package com.yqg.scheduler.jobs.risk;

import com.google.common.util.concurrent.RateLimiter;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.riskprocessor.infra.RiskTriggerSourceInfo;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 模拟用户触发还款续借or打款续借
 */
@Service
@Slf4j
public class TriggerPayoutOrRepaymentMultiLoanRiskTypeJob extends YqgBaseJob {
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private LoanAccountModel loanAccountModel;


  @Override
  public void exec(JobExecutionContext context) throws Exception {
    Param param = getParam(context, Param.class);
    if (CollectionUtils.isEmpty(param.accountIdList)) {
      return;
    }
    RateLimiter rateLimiter = RateLimiter.create(param.rateLimiterCount);
    for (Long accountId : param.accountIdList) {
      if (isInterrupted()) {
        break;
      }
      log.info("TriggerPayoutOrRepaymentMultiLoanRiskTypeJob start accountId:{}", accountId);
      tryToTriggerMultiLoan(accountId, param.mockPayoutSuccess, param.mockRepaymentSuccess, rateLimiter);
      log.info("TriggerPayoutOrRepaymentMultiLoanRiskTypeJob finish userId:{}", accountId);
    }
  }

  private void tryToTriggerMultiLoan(Long accountId, boolean mockPayoutSuccess, boolean mockRepaymentSuccess, RateLimiter rateLimiter) {

    rateLimiter.acquire();
    threadTransactionalModel.transaction((configuration) -> {
      //先加锁
      LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVOForUpdate(accountId);
      if (mockPayoutSuccess) {
        loanAccountService.tryAutoSubmitCalcCreditsByTriggerSource(loanAccountVO.id, loanAccountVO.userId, loanAccountVO.sdkType, RiskTriggerSourceInfo.of(LoanUserRiskTriggerSource.PAYOUT_SUCCESS, null));
        log.info("mockPayoutSuccess accountId:{}", accountId);
      }
      if (mockRepaymentSuccess) {
        loanAccountService.tryAutoSubmitCalcCreditsByTriggerSource(loanAccountVO.id, loanAccountVO.userId, loanAccountVO.sdkType, RiskTriggerSourceInfo.of(LoanUserRiskTriggerSource.REPAY_PARTIAL_INSTALMENTS, null));
        log.info("mockRepaymentSuccess accountId:{}", accountId);
      }
    });
  }

  public static class Param {
    public List<Long> accountIdList = new ArrayList<>();
    public boolean mockPayoutSuccess = false;
    public boolean mockRepaymentSuccess = false;
    public float rateLimiterCount = 1;
  }
}
