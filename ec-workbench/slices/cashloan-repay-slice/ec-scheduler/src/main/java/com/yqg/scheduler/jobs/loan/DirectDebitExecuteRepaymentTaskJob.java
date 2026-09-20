package com.yqg.scheduler.jobs.loan;

import com.yqg.ec.common.exception.EcException;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import org.springframework.stereotype.Service;

/**
 * 批量执行代扣还款任务
 *
 * @author chaoye
 * @date 2024/9/5
 */
@Service
public class DirectDebitExecuteRepaymentTaskJob extends YqgBaseJob {

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    throw EcException.error("The job went offline and an execution error occurred.");
  }
}
