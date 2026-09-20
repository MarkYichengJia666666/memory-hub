package com.yqg.scheduler.jobs.risk.batchtrigger;

import com.yqg.core.model.generated.tables.records.BatchTriggerRiskTaskRecord;
import com.yqg.core.model.sql.risk.BatchTriggerRiskTaskModel;
import com.yqg.core.service.risk.batchtrigger.RiskBatchTriggerService;
import com.yqg.core.service.risk.batchtrigger.enums.BatchTriggerTaskStatus;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by ZiP on 2022/4/21.
 */
@Service
@Slf4j
public class RiskBatchTriggerTaskHandleNewJob extends YqgBaseJob {
  @Autowired
  private BatchTriggerRiskTaskModel batchTriggerRiskTaskModel;
  @Autowired
  private RiskBatchTriggerService riskBatchTriggerService;

  @Override
  protected Boolean allowParallel() {
    return false;
  }

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    Param param = getParam(context, Param.class);
    scanRunningTask(param.taskIds);
    scanAcceptedTask(param.taskIds);
  }

  private void scanAcceptedTask(List<Long> taskIds) {
    List<BatchTriggerRiskTaskRecord> records;
    if (CollectionUtils.isNotEmpty(taskIds)) {
      records = batchTriggerRiskTaskModel.fetchByIdsAndStatus(taskIds, BatchTriggerTaskStatus.ACCEPT);
    } else {
      records = batchTriggerRiskTaskModel.fetchAllByStatus(BatchTriggerTaskStatus.ACCEPT);
    }

    records.forEach(o -> {
      try {
        if(isInterrupted()){
          return;
        }
        riskBatchTriggerService.handleTask(o);
      } catch (Exception e) {
        log.error("sth wrong with scan accepted task, task id = {}.", o.getId(), e);
      }
    });
  }

  private void scanRunningTask(List<Long> taskIds) {
    List<BatchTriggerRiskTaskRecord> records;
    if (CollectionUtils.isNotEmpty(taskIds)) {
      records = batchTriggerRiskTaskModel.fetchByIdsAndStatus(taskIds, BatchTriggerTaskStatus.WAITING_FOR_RESULT);
    } else {
      records = batchTriggerRiskTaskModel.fetchAllByStatus(BatchTriggerTaskStatus.WAITING_FOR_RESULT);
    }

    records.forEach(o -> {
      try {
        if(isInterrupted()){
          return;
        }
        riskBatchTriggerService.checkTaskFinished(o);
      } catch (Exception e) {
        log.error("sth wrong with scan running task, task id = {}.", o.getId(), e);
      }
    });
  }

  public static class Param {
    public List<Long> taskIds;
  }
}
