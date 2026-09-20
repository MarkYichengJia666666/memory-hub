package com.yqg.scheduler.jobs.risk;

import com.google.common.util.concurrent.RateLimiter;
import com.yqg.core.model.generated.tables.records.CashLoanOrderRecord;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.sql.cashloan.order.CashLoanOrderModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.service.cashloan.CashLoanCalcCreditsService;
import com.yqg.core.service.cashloan.event.CashLoanEventType;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.riskprocessor.infra.RiskTriggerSourceInfo;
import com.yqg.core.service.user.UserService;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author shubo
 * @date 2022/10/14 10:35 上午
 */
@Service
@Slf4j
public class RiskAutoSubmitCalcCreditsJob extends YqgBaseJob {
  @Autowired
  private CashLoanCalcCreditsService cashLoanCalcCreditsService;
  @Autowired
  private LoanAccountService accountService;
  @Autowired
  private CashLoanOrderModel cashLoanOrderModel;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private UserService userService;

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    Param param = getParam(context, Param.class);
    Long currentTime = Clock.now();
    long minMillisOfDay = Clock.getMinMillisOfDay(currentTime, SDKType.IDN_YQD.getTimeZone());
    long submitCalcCreditsTime = minMillisOfDay + param.startTime * Clock.MILLS_PER_HOUR;
    if (submitCalcCreditsTime > currentTime) {
      return;
    }
    long startTime = Clock.getMinMillisOfPlusDays(submitCalcCreditsTime, -1, SDKType.IDN_YQD.getTimeZone());
    long endTime = Clock.getMaxMillisOfPlusDays(submitCalcCreditsTime, -1, SDKType.IDN_YQD.getTimeZone());
    //获取前天完结订单的用户
    List<Long> loanAccountIdList = cashLoanOrderModel.listAccountIdsBySdkAndStatusAndTimeCompleted(SDKType.IDN_YQD,
        CashLoanOrderStatus.COMPLETE, startTime, endTime);
    RateLimiter rateLimiter = RateLimiter.create(param.permitsPerSecond / 60.0);

    for (Long loanAccountId : loanAccountIdList) {
      if (isNotExistCompleteOrderByIntervalTime(endTime + 1, currentTime, loanAccountId)
          && cashLoanCalcCreditsService.isCreditsInvalid(loanAccountId)
          && !isDeletedUser(loanAccountId)) {
        rateLimiter.acquire();
        //限制一下提交风控的速率；外部 id 取结清日（前一日）最后一笔结清订单 id
        CashLoanOrderRecord lastCompletedOrder = cashLoanOrderModel.findLatestCompletedOrderByAccountIdAndEndTime(loanAccountId, endTime + 1);
        Long lastCompletedOrderId = lastCompletedOrder == null ? null : lastCompletedOrder.getId();
        accountService.tryAutoSubmitCalcCreditsApplication(loanAccountId, SDKType.IDN_YQD, CashLoanEventType.ORDER_COMPLETED,
            RiskTriggerSourceInfo.of(LoanUserRiskTriggerSource.ORDER_COMPLETED_COMPENSATE_JOB, lastCompletedOrderId));
      }
    }
  }

  //考虑用户存在两笔订单，前天20点完成了，第二天8点50分第二笔也完结了，现在是不跑
  private Boolean isNotExistCompleteOrderByIntervalTime(Long startTime, Long endTime, Long loanAccountId) {
    List<CashLoanOrderRecord> cashLoanOrderRecordList = cashLoanOrderModel.listCompletedOrderListByAccountIdAndIntervalTime(startTime, endTime,
        loanAccountId, CashLoanOrderStatus.COMPLETE);
    return cashLoanOrderRecordList.size() == 0;
  }

  private Boolean isDeletedUser(long loanAccountId) {
    LoanAccountRecord loanAccountRecord = loanAccountModel.findById(loanAccountId);
    return userService.isDeleted(loanAccountRecord.getUserId());
  }

  public static class Param {
    public Integer startTime;
    // 每分钟最多多少个请求,默认15个
    public Double permitsPerSecond = 15.0D;
  }
}
