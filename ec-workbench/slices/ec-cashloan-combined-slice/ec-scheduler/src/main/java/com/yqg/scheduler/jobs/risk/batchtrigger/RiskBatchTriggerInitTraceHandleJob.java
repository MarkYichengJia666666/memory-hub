package com.yqg.scheduler.jobs.risk.batchtrigger;

import com.yqg.core.model.generated.tables.records.BatchTriggerRiskLogRecord;
import com.yqg.core.model.generated.tables.records.BatchTriggerRiskTaskRecord;
import com.yqg.core.model.sql.risk.BatchTriggerRiskLogModel;
import com.yqg.core.model.sql.risk.BatchTriggerRiskTaskModel;
import com.yqg.core.service.risk.batchtrigger.RiskBatchTriggerService;
import com.yqg.core.service.risk.batchtrigger.enums.BatchTriggerLogStatus;
import com.yqg.core.service.risk.batchtrigger.enums.BatchTriggerTaskStatus;
import com.yqg.core.service.risk.batchtrigger.vo.TaskDataVO;
import com.yqg.core.util.common.RenamedThreadFactory;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.risk.orm.RiskFlowModel;
import com.yqg.risk.orm.sql.tables.records.RiskFlowRecord;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RiskBatchTriggerInitTraceHandleJob extends YqgBaseJob {
  @Autowired
  private BatchTriggerRiskLogModel batchTriggerRiskLogModel;
  @Autowired
  private RiskBatchTriggerService riskBatchTriggerService;
  @Autowired
  private BatchTriggerRiskTaskModel batchTriggerRiskTaskModel;
  @Autowired
  private RiskFlowModel riskFlowModel;

  static Set<Long> idSet = Collections.synchronizedSet(new HashSet<>());

  static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20, 50, 1L, TimeUnit.MINUTES,
      new LinkedBlockingQueue<>(5000),
      new RenamedThreadFactory(RiskBatchTriggerInitTraceHandleJob.class.getSimpleName()),
      new ThreadPoolExecutor.AbortPolicy());


  @Override
  protected Boolean allowParallel() {
    return false;
  }

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    Param param = getParam(context, Param.class);
    scanInitLog(param);
  }

  private void scanInitLog(Param param) {
    log.info("scan init log begin!");
    List<BatchTriggerRiskLogRecord> initRecords = getInitRecordsSortByTaskLogCount(param);

    log.info("initRecords :{} ", initRecords.stream().map(BatchTriggerRiskLogRecord::getId).collect(Collectors.toList()));
    if (CollectionUtils.isNotEmpty(initRecords)) {
      ExecutorService executor = getThreadPoolExecutor(param);
      Map<Long, Long> taskRiskFlowIdMap = getTaskRiskFlowIdMap(initRecords);
      Map<Long, Long> riskFlowEventMap = getRiskFlowEventMap(taskRiskFlowIdMap);
      for (BatchTriggerRiskLogRecord record : initRecords) {
        try {
          if (idSet.contains(record.getId())) {
            continue;
          }
          idSet.add(record.getId());
          executor.execute(() -> {
            try {
              Long riskFlowId = taskRiskFlowIdMap.get(record.getTaskId());
              riskBatchTriggerService.handleTrace(record, riskFlowId, getEventTypeId(riskFlowId, riskFlowEventMap));
            } catch (Exception e) {
              log.error("RiskBatchTriggerTraceHandleJob task execute error! ", e);
            } finally {
              idSet.remove(record.getId());
            }
          });
        } catch (RejectedExecutionException rejectException) {
          log.info("RiskBatchTriggerTraceHandleJob submit task rejected,id is {}", record.getId());
          idSet.remove(record.getId());
        } catch (Exception ex) {
          log.error("RiskBatchTriggerTraceHandleJob submit task error,id is {}", record.getId(), ex);
          idSet.remove(record.getId());
        }
      }
    }
    log.info("scan init log end!");
  }

  public List<BatchTriggerRiskLogRecord> getInitRecordsSortByTaskLogCount(Param param) {
    log.info("log init begin with getInitRecordsSortByTaskLogCount");
    Integer runningRecordCount = batchTriggerRiskLogModel.fetchByStatusAndLimit(BatchTriggerLogStatus.RUNNING, (Clock.now() - (param.withinDays * Clock.MILLS_PER_DAY)));
    if (runningRecordCount >= param.maxBatch) {
      log.info("log init end with too much running log!");
      return new ArrayList<>();
    }
    int restBatchSize = param.maxBatch - runningRecordCount;
    List<BatchTriggerRiskLogRecord> initRecords = new ArrayList<>();

    if (CollectionUtils.isNotEmpty(param.taskIdList)) {
      List<Long> taskIdList = param.taskIdList;
      for (Long taskId : taskIdList) {
        if (initRecords.size() >= restBatchSize) {
          return initRecords;
        }
        initRecords.addAll(batchTriggerRiskLogModel.fetchByTaskIdAndStatusAndLimit(BatchTriggerLogStatus.INIT, restBatchSize - initRecords.size(), (Clock.now() - (param.withinDays * Clock.MILLS_PER_DAY)), Collections.singleton(taskId)));
      }
    }
    if (initRecords.size() >= restBatchSize) {
      return initRecords;
    }

    List<BatchTriggerRiskTaskRecord> waitingTaskList = batchTriggerRiskTaskModel.fetchAllByStatus(BatchTriggerTaskStatus.WAITING_FOR_RESULT);
    List<TaskDataVO> taskDataVOList = waitingTaskList.stream()
        .map(vo -> riskBatchTriggerService.getTaskById(vo.getId()))
        .sorted(Comparator.comparingInt(TaskDataVO::getExpectedUserCount))
        .collect(Collectors.toList());

    for (TaskDataVO taskDataVO : taskDataVOList) {
      if (initRecords.size() >= restBatchSize) {
        return initRecords;
      }
      List<BatchTriggerRiskLogRecord> taskInitRecords = batchTriggerRiskLogModel.fetchByTaskIdAndStatusAndLimit(BatchTriggerLogStatus.INIT,
          restBatchSize - initRecords.size(),
          (Clock.now() - (param.withinDays * Clock.MILLS_PER_DAY)),
          Collections.singleton(taskDataVO.getId()));
      if (CollectionUtils.isNotEmpty(taskInitRecords)) {
        initRecords.addAll(taskInitRecords);
      }
    }
    return initRecords;
  }

  public Map<Long, Long> getTaskRiskFlowIdMap(List<BatchTriggerRiskLogRecord> records) {
    List<Long> taskIds = records.stream().map(BatchTriggerRiskLogRecord::getTaskId).distinct().collect(Collectors.toList());
    Map<Long, Long> taskRiskFlowIdMap = batchTriggerRiskTaskModel.findMapByTaskId(taskIds);
    taskRiskFlowIdMap.values().removeIf(RiskBatchTriggerService.DEFAULT_RISK_FLOW_ID::equals);
    return taskRiskFlowIdMap;
  }

  public Map<Long, Long> getRiskFlowEventMap(Map<Long, Long> taskRiskFlowIdMap) {
    List<Long> riskFlowIds = taskRiskFlowIdMap
        .values()
        .stream()
        .filter(t -> !RiskBatchTriggerService.DEFAULT_RISK_FLOW_ID.equals(t) && Objects.nonNull(t))
        .collect(Collectors.toList());

    List<RiskFlowRecord> riskFlowList = riskFlowModel.findByIds(riskFlowIds);
    return riskFlowList.stream().collect(HashMap::new, (k, v) -> k.put(v.getId(), v.getEventId()), HashMap::putAll);
  }

  private Long getEventTypeId(Long riskFlowId, Map<Long, Long> riskFlowEventMap) {
    if (RiskBatchTriggerService.DEFAULT_RISK_FLOW_ID.equals(riskFlowId) || Objects.isNull(riskFlowId)) {
      return null;
    }
    return riskFlowEventMap.get(riskFlowId);
  }

  public static class Param {
    public int coreSize = 20;
    public int maxSize = 50;
    public Integer maxBatch = 500;
    public Integer withinDays = 10;
    public List<Long> taskIdList = new ArrayList<>();
  }

  /**
   * Generate a thread pool based on the configuration
   */
  private static ThreadPoolExecutor getThreadPoolExecutor(Param params) {
    int corePoolSize = params.coreSize;
    int maximumPoolSize = params.maxSize;
    if (corePoolSize != threadPoolExecutor.getCorePoolSize() || maximumPoolSize != threadPoolExecutor.getMaximumPoolSize()) {
      threadPoolExecutor.setCorePoolSize(corePoolSize);
      threadPoolExecutor.setMaximumPoolSize(maximumPoolSize);
    }
    return threadPoolExecutor;
  }
}