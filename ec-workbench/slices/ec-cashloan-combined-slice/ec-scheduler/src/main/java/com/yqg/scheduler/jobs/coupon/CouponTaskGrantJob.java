package com.yqg.scheduler.jobs.coupon;

import com.yqg.core.model.sql.notif.enums.NotifCouponGrantTaskStatus;
import com.yqg.core.service.notif.NotifCouponGrantTaskService;
import com.yqg.core.service.notif.vo.NotifCouponGrantTaskVO;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author fudongyi
 * @date 2022/7/18
 */
@Service
public class CouponTaskGrantJob extends YqgBaseJob {
  @Autowired
  private NotifCouponGrantTaskService notifCouponGrantTaskService;

  private static final Integer BATCH_SIZE = 500;

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    Param param = getParam(context, Param.class);
    NotifCouponGrantTaskStatus status;
    if (Objects.nonNull(param) && Objects.nonNull(param.retryException) && param.retryException) {
      status = NotifCouponGrantTaskStatus.EXCEPTION;
    } else {
      status = NotifCouponGrantTaskStatus.INIT;
    }

    List<NotifCouponGrantTaskVO> taskList;
    do {
      taskList = notifCouponGrantTaskService.listByStatusAndLimit(status, BATCH_SIZE);
      notifCouponGrantTaskService.batchDoTask(taskList);
    } while (taskList.size() > 0 && !isInterrupted() && status == NotifCouponGrantTaskStatus.INIT);  // 防止死循环，只有是INIT的时候，才循环发券
  }

  static final class Param {
    public Boolean retryException;
  }
}
