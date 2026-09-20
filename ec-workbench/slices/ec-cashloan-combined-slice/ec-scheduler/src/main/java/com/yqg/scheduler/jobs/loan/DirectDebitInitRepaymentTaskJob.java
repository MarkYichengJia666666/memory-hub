package com.yqg.scheduler.jobs.loan;

import com.yqg.ec.common.exception.EcException;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import org.springframework.stereotype.Service;

/**
 * 批量初始化代扣还款任务
 *
 * @author chaoye
 * @date 2024/9/5
 */
@Service
public class DirectDebitInitRepaymentTaskJob extends YqgBaseJob {

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    // job下线，执行报错
    throw EcException.error("The job went offline and an execution error occurred.");
  }

}
