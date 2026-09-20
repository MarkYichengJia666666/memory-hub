package com.yqg.scheduler.jobs;

import com.google.common.collect.Lists;
import com.yqg.core.service.cashloan.ordercenter.CashLoanActivityInstalmentService;
import com.yqg.core.service.cashloan.ordercenter.EcActivityOrderService;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.EcTimeZone;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.TimeZone;

/**
 * @author yuchenghuang
 * @date 2023/12/21
 */
@Service
@Slf4j
public class CashLoanActivityOrderRepayJob extends YqgBaseJob {
  @Autowired
  private CashLoanActivityInstalmentService cashLoanActivityInstalmentService;
  @Autowired
  private EcActivityOrderService ecActivityOrderService;

  private static final TimeZone TIME_ZONE = EcTimeZone.JAKARTA.tz;

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    Param param = getParam(context, Param.class);
    Long startTime = param.startTime == null ? Clock.getMinMillisOfDay(Clock.now(), TIME_ZONE) : param.startTime;
    Long endTime = param.endTime == null ? Clock.getMaxMillisOfDay(Clock.now(), TIME_ZONE) : param.endTime;
    List<Long> needRepayIds = cashLoanActivityInstalmentService.fetchNeedRepayInstalments(startTime, endTime);
    Lists.partition(needRepayIds, param.limit).forEach(list -> ecActivityOrderService.repayInstalments(list, endTime));
  }

  public static class Param {
    public Long startTime;
    public Long endTime;
    public Integer limit = 500;
  }
}
