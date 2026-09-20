package com.yqg.scheduler.jobs.loan;

import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by xiuqichenyang on 17/7/15.
 */
@Service
public class LoanBankAccountStatusUpdateJob extends YqgBaseJob {
  @Autowired
  private LoanBankAccountService loanBankAccountService;

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    while (!isInterrupted()) {
      log.info("LoanBankAccountStatusUpdateJob starts");

      Long now = Clock.now();
      Integer timeInterval = getParam(context, Param.class).timeInterval;
      loanBankAccountService.updateStatusBy3rdPartyResult(now - timeInterval * Clock.MILLS_PER_MINUTE, now);

      Thread.sleep(2000L);
    }
  }

  public static class Param {
    public Integer timeInterval = 60;
  }
}
