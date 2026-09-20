package com.miyou.controllers.cashloan.utilities;

import com.google.common.collect.ImmutableList;
import com.miyou.controllers.cashloan.IDNHomepageV5Controller;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.service.app.AppStartupService;
import com.yqg.core.service.cashloan.CashLoanCalcCreditsService;
import com.yqg.core.service.cashloan.homepage.UserHomepageStatusLogService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.vo.CalcCreditsInfoLogVO;
import com.yqg.core.service.cashloan.vo.CashLoanCalcCreditsVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.monitor.homepage.HomePageStatusMonitorService;
import com.yqg.core.util.ScopeTaskWrapper;
import com.yqg.core.util.common.RenamedThreadFactory;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrapper;
import org.dromara.dynamictp.core.support.task.wrapper.TaskWrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoanHomePageLogService {

  @Autowired
  private AppStartupService appStartupService;
  @Autowired
  private CashLoanCalcCreditsService calcCreditsService;
  @Autowired
  private EcOrderService orderService;
  @Autowired
  private HomePageStatusMonitorService homePageStatusMonitorService;
  @Autowired
  private UserHomepageStatusLogService userHomepageStatusLogService;
  private static final ExecutorService logStatusExecutor = new ThreadPoolExecutor(5, 10, 5,
      TimeUnit.MINUTES, new LinkedBlockingQueue<>(100),
      new RenamedThreadFactory(IDNHomepageV5Controller.class.getSimpleName()), new ThreadPoolExecutor.CallerRunsPolicy());
  private static final TaskWrapper SCOPE_TASK_WRAPPER = TaskWrappers.getInstance().getByNames(ImmutableList.of("scopeTaskWrapper")).stream()
      .filter(t -> t instanceof ScopeTaskWrapper).findFirst().get();

  public void asyncLogUserInfo(HomepageUserParamsVO paramsVO, IDNHomepageLoanStatusV5 status) {
    /**
     * 不要取threadLocal值
     */
    RequestClientType requestClientType = ImpliedContextUtils.requestClientType();
    Runnable task = () -> {
      doAsyncLog(() -> userHomepageStatusLogService.tryToAddLastStatus(paramsVO.getUserId(), paramsVO.getLoanAccountId(), status));
      // 记录额度测算变更信息
      doAsyncLog(() -> logCalcCreditsInfo(paramsVO.accountVO));
      // 监控首页输出的用户登录后所处的具体状态
      doAsyncLog(() -> monitorHomepageStatusAfterLogin(status, paramsVO.accountVO, paramsVO.getEnableCredits(), requestClientType));
      doAsyncLog(() -> appStartupService.processAppStartupEvent(paramsVO.getUserId(), requestClientType,"/api/v5/cashloan/home"));
    };

    Runnable wrappedTask = SCOPE_TASK_WRAPPER.wrap(task);
    logStatusExecutor.submit(wrappedTask);
  }

  /**
   * 防止某一方法报错影响其他处理
   * @param task
   */
  private void doAsyncLog(Runnable task) {
    try {
      task.run();
    } catch (Exception e) {
      log.error("Async task error", e);  // 打印日志，但不会影响其他任务
    }
  }


  private void logCalcCreditsInfo(LoanAccountVO accountVO) {
    Long loanAccountId = accountVO.id;

    CashLoanCalcCreditsVO cashLoanCalcCreditsVO = calcCreditsService.getCalcCreditsStatus(loanAccountId);
    if (cashLoanCalcCreditsVO == null) {
      return;
    }

    boolean isCompleted = orderService.existOrder(loanAccountId, CashLoanOrderStatus.COMPLETE);
    // 不存在已完成的订单,则说明在首贷状态下,首贷额度测算的状态没有变更 不记录
    if (!isCompleted) {
      return;
    }

    CalcCreditsInfoLogVO logVO = calcCreditsService.findLatestLog(loanAccountId);
    if (logVO == null || logVO.status != cashLoanCalcCreditsVO.calcCreditsStatus) {
      calcCreditsService.insertLog(loanAccountId, accountVO.sdkType, cashLoanCalcCreditsVO.calcCreditsStatus);
    }
  }

  private void monitorHomepageStatusAfterLogin(IDNHomepageLoanStatusV5 status, LoanAccountVO accountVO,
      BigDecimal credits, RequestClientType requestClientType) {
    try {
      homePageStatusMonitorService.logStatusAfterLogin(status.name(), accountVO.userId, accountVO.sdkType, credits, requestClientType);
    } catch (Exception ex) {
      log.error("monitor homepage status failed", ex);
    }
  }
}
