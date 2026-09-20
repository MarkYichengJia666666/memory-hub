package com.yqg.scheduler.base;

import com.google.common.collect.Sets;
import com.yqg.core.service.monitor.SchedulerMonitor;
import com.yqg.core.util.Interruptible;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.scheduler.executor.job.BaseJob;
import com.yqg.scheduler.locker.JobParallelLocker;
import com.yqg.scheduler.vo.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

public abstract class YqgBaseJob extends BaseJob implements Interruptible {
  protected Logger log;
  @Autowired
  private SchedulerMonitor schedulerMonitor;
  @Autowired
  private JobParallelLocker locker;

  private static final Set<String> RUNNING_JOB_NAME_SET = Sets.newConcurrentHashSet();

  public YqgBaseJob() {
    log = LoggerFactory.getLogger(getClass());
  }

  public abstract void exec(JobExecutionContext context) throws Exception;

  protected Boolean allowParallel() {
    return true;
  }

  protected Integer expireSeconds() {
    return 60 * 15;
  }

  protected Set<String> getRedisLockKeySet() {
    return RUNNING_JOB_NAME_SET;
  }

  @Override
  public void doExecute(JobExecutionContext context) throws Exception {
    if (allowParallel()) {
      doTask(context);
    } else {
      doTaskWithRedisLock(context);
    }
  }

  private void doTaskWithRedisLock(JobExecutionContext context) {
    String generateKey = generateKey(context.jobName);
    try {
      RUNNING_JOB_NAME_SET.add(generateKey);
      locker.lockWithExpireAndRunUntilFinishedOrExceptionally(generateKey, expireSeconds(), () -> doTask(context));
    } catch (EcException e) {
      if (e.exceptionType == EcExceptionType.COMMON_REDIS_LOCKER_GET_ERROR) {
        schedulerMonitor.logAcquireLockFailed(this.getClass().getSimpleName(), context.jobName);
        log.warn("job acquire redis lock failed, job: {}", this.getClass().getSimpleName());
      } else {
        log.error("job execute failed, job is {}", this.getClass().getSimpleName(), e);
      }
    } finally {
      RUNNING_JOB_NAME_SET.remove(generateKey);
    }
  }

  private String generateKey(String contextJobName) {
    return contextJobName + "_" + this.getClass().getSimpleName();
  }

  private void doTask(JobExecutionContext context) {
    long startTime = Clock.now();
    boolean hasException = false;
    schedulerMonitor.monitorSchedulerTrigger(this.getClass().getSimpleName(), context.jobName, startTime - context.timeStamp);
    try {
      exec(context);
    } catch (Exception e) {
      hasException = true;
      log.error("job execute fail. job: {}", this.getClass().getSimpleName(), e);
    } finally {
      schedulerMonitor.monitorScheduler(this.getClass().getSimpleName(), context.jobName, Clock.now() - startTime, hasException);
    }
  }
}
