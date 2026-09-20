package com.miyou.controllers.admin.risk;

import com.miyou.controllers.admin.loan.creditsbyapp.response.ListRiskFlowItemResponse;
import com.miyou.controllers.admin.loan.creditsbyapp.response.ListRiskFlowsResponse;
import com.miyou.controllers.admin.risk.request.CreateAutoBatchTriggerTaskRequest;
import com.miyou.controllers.admin.risk.request.CreateBatchTriggerTaskRequest;
import com.miyou.controllers.admin.risk.response.ManualBatchTaskResponse;
import com.miyou.controllers.core.YqgBaseController;
import com.yqg.core.model.sql.loanusertrace.TriggerSubType;
import com.yqg.core.service.cashloan.risk.vo.EventTypeVO;
import com.yqg.core.service.loan.account.LoanUserEventService;
import com.yqg.core.service.loan.account.enums.BatchTriggerReason;
import com.yqg.core.service.risk.batchtrigger.RiskAutoBatchTriggerService;
import com.yqg.core.service.risk.batchtrigger.RiskBatchTriggerService;
import com.yqg.core.service.risk.batchtrigger.auto.enums.AutoBatchTriggerTaskStatus;
import com.yqg.core.service.risk.batchtrigger.auto.vo.AutoTaskDataVO;
import com.yqg.core.service.risk.batchtrigger.auto.vo.AutoTaskOperateVO;
import com.yqg.core.service.risk.batchtrigger.auto.vo.TaskOperateVO;
import com.yqg.core.service.risk.batchtrigger.filter.enums.TriggerFilter;
import com.yqg.core.service.risk.batchtrigger.vo.CreateAutoTaskRequest;
import com.yqg.core.service.risk.batchtrigger.vo.CreateTaskResponse;
import com.yqg.core.service.risk.batchtrigger.extrafunction.enums.ExtraFunctionType;
import com.yqg.core.service.risk.batchtrigger.vo.*;
import com.yqg.core.service.risk.riskflow.RiskFlowSwitchService;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowVO;
import com.yqg.core.service.risk.riskflowcheck.utils.UtilParamVO;
import com.yqg.core.service.risk.utilparam.RiskUtilParamTool;
import com.yqg.core.service.tagdata.TagDataService;
import com.yqg.core.service.tagdata.response.TagDataGroup;
import com.yqg.ec.common.enums.Label;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by ZiP on 2022/5/31.
 */
@RestController
public class BatchTriggerRiskController extends YqgBaseController {
  @Autowired
  private RiskBatchTriggerService riskBatchTriggerService;
  @Autowired
  private RiskAutoBatchTriggerService riskAutoBatchTriggerService;
  @Autowired
  private TagDataService tagDataService;
  @Autowired
  private LoanUserEventService loanUserEventService;
  @Autowired
  private RiskFlowSwitchService riskFlowSwitchService;
  @Autowired
  private RiskUtilParamTool riskUtilParamTool;
  @Autowired
  private EcRiskFlowService ecRiskFlowService;

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/listTask")
  public Result listTask(@RequestParam(required = false, name = "beginTime") Long beginTime,
                         @RequestParam(required = false, name = "endTime") Long endTime) {
    List<TaskDataVO> res = riskBatchTriggerService.listTasks(beginTime, endTime, getAdminUserEmail());
    return EcResponseUtil.generate(res);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/listUtil")
  public Result listUtil(@RequestParam("isReloan") boolean isReloan) {
    if (isReloan) {
      return EcResponseUtil.generate(TriggerFilter.reloanFilters().stream().map(UtilDescription::from).collect(Collectors.toList()));
    } else {
      return EcResponseUtil.generate(TriggerFilter.loanFilters().stream().map(UtilDescription::from).collect(Collectors.toList()));
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/utilParam")
  public Result genUtilParam(@RequestParam("utilId") int utilId) {
    TriggerFilter filter = TriggerFilter.fromCode(utilId);
    if (filter == TriggerFilter.TAG_DATA_USER_GROUP || filter == TriggerFilter.TAG_DATA_SQL_USER_GROUP || filter == TriggerFilter.IGNORE_STATUS_TO_SUBMIT_RISK) {
      List<UtilParamVO> from = null;
      return EcResponseUtil.generate(from);
    }
    return EcResponseUtil.generate(riskUtilParamTool.generateUtilParamVO(filter.filterConfig.paramClass()));
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @PostMapping(path = "/admin/operation/riskBatchTrigger/createTask")
  public Result createTask(@RequestBody @Valid CreateBatchTriggerTaskRequest createBatchTriggerTaskRequest) {
    CreateTaskResponse response = JsonUtils.fromOrException(createBatchTriggerTaskRequest.taskParam, CreateTaskResponse.class);
    response.adminUserEmail = getAdminUserEmail();
    riskBatchTriggerService.checkForCreateTask(response);
    riskBatchTriggerService.initTask(response);
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/extraFunctionType")
  public Result<List<Label<String>>> extraFunctionType() {
    List<Label<String>> res = Arrays.asList(ExtraFunctionType.values())
        .stream()
        .map(e -> Label.gen(e.getDescription(), e.name()))
        .collect(Collectors.toList());
    return EcResponseUtil.generate(res);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/extraFunctionParam")
  public Result<List<UtilParamVO>> extraFunctionParam(@RequestParam("extraFunctionType") ExtraFunctionType extraFunctionType) {

    return EcResponseUtil.generate(riskUtilParamTool.generateUtilParamVO(extraFunctionType.getParamClass()));
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/acceptTask")
  public Result acceptTask(@RequestParam("id") long id) {
    riskBatchTriggerService.acceptTask(id, getAdminUserEmail(), getAdminUserName());
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.MANAGE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/rejectTask")
  public Result rejectTask(@RequestParam("id") long id) {
    riskBatchTriggerService.rejectTask(id, getAdminUserEmail());
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/cancelTask")
  public Result cancelTask(@RequestParam("id") long id) {
    riskBatchTriggerService.cancelTask(id);
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/listRiskType")
  public Result listRiskType(@RequestParam("isReloan") boolean isReloan) {
    return EcResponseUtil.generate(riskBatchTriggerService.listRiskType(isReloan));
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/taskById/{taskId}")
  public Result getTaskById(@PathVariable("taskId") Long taskId) {
    TaskDataVO taskDataVO = riskBatchTriggerService.getTaskById(taskId);
    ManualBatchTaskResponse manualBatchTaskResponse = ManualBatchTaskResponse.from(taskDataVO);
    return EcResponseUtil.generate(manualBatchTaskResponse);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/manualListOperateLog")
  public Result manualListOperateLog(@RequestParam("taskId") Long taskId,
                                     @RequestParam(required = false, name = "beginTime") Long beginTime,
                                     @RequestParam(required = false, name = "endTime") Long endTime) {
    List<TaskOperateVO> taskOperateVOs = riskBatchTriggerService.listOperateLog(taskId, beginTime, endTime);
    return EcResponseUtil.generate(taskOperateVOs);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @PostMapping(path = "/admin/operation/riskBatchTrigger/createAutoTask")
  public Result createAutoTask(@RequestBody @Valid CreateAutoBatchTriggerTaskRequest createAutoBatchTriggerTaskRequest) {
    CreateAutoTaskRequest request = JsonUtils.fromOrException(createAutoBatchTriggerTaskRequest.autoTaskParam, CreateAutoTaskRequest.class);
    request.adminUserEmail = getAdminUserEmail();
    if (Objects.isNull(request.description) || request.description.length() > 50) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("策略名称长度不符合要求，请修改后保存。"));
    }

    riskBatchTriggerService.checkForExtraFunction(request.extraFunctions, request.riskType);
    riskAutoBatchTriggerService.initAutoTask(request);
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.MANAGE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/modifyAutoTaskStatus")
  public Result modifyAutoTaskStatus(@RequestParam("id") long id, @RequestParam("status") AutoBatchTriggerTaskStatus status) {
    riskAutoBatchTriggerService.modifyAutoTaskStatus(id, getAdminUserEmail(), status);
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/listOperateLog")
  public Result listOperateLog(@RequestParam(name = "autoTaskId") Long autoTaskId) {
    List<AutoTaskOperateVO> res = riskAutoBatchTriggerService.listOperateLog(autoTaskId);
    return EcResponseUtil.generate(res);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/listAutoTask")
  public Result listAutoTask(@RequestParam(required = false, name = "beginTime") Long beginTime,
                             @RequestParam(required = false, name = "endTime") Long endTime,
                             @RequestParam(required = false, name = "status") AutoBatchTriggerTaskStatus status) {
    List<AutoTaskDataVO> res = riskAutoBatchTriggerService.listAutoTask(beginTime, endTime, status);
    return EcResponseUtil.generate(res);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/getTagDataGroups")
  public Result getTagDataGroups(@RequestParam(required = false, name = "isSQLTag", defaultValue = "false") boolean isSQLTag) {
    if (isSQLTag) {
      List<TagDataGroup> res = tagDataService.getSQLGroups();
      return EcResponseUtil.generate(res);
    } else {
      List<TagDataGroup> res = tagDataService.getGroups();
      return EcResponseUtil.generate(res);
    }
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/getRiskFlow")
  public Result getRiskTypeToRiskFlowList(@RequestParam(name = "loanUserRiskType") LoanUserRiskType loanUserRiskType) {
    List<EventTypeVO> allEventTypeList = loanUserEventService.getAllEventTypeList();
    allEventTypeList = allEventTypeList.stream()
        .filter(eventTypeVO -> eventTypeVO.riskType == loanUserRiskType)
        .collect(Collectors.toList());
    List<RiskFlowVO> riskFlowVOS = riskFlowSwitchService.findByEventTypes(allEventTypeList);
    ListRiskFlowsResponse response = new ListRiskFlowsResponse();
    List<ListRiskFlowItemResponse> itemList = ecRiskFlowService.generateListRiskFlowItemResponses(riskFlowVOS, getSDKType());
    response.responses.addAll(itemList);
    return EcResponseUtil.generate(response);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/listSourcePlatform")
  public Result listSourcePlatform() {
    List<Label<String>> labelList = TriggerSubType.SOURCE_PLATFORM_LIST
        .stream()
        .map(o -> Label.gen(o.desc, o.name()))
        .collect(Collectors.toList());

    return EcResponseUtil.generate(labelList);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/listSourcePlatformWithoutMarketing")
  public Result listSourcePlatformWithoutMarketing() {
    List<Label<String>> labelList = TriggerSubType.SOURCE_PLATFORM_LIST_WITHOUT_MARKETING
        .stream()
        .map(o -> Label.gen(o.desc, o.name()))
        .collect(Collectors.toList());

    return EcResponseUtil.generate(labelList);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.BATCH_TRIGGER.CREATE')")
  @GetMapping(path = "/admin/operation/riskBatchTrigger/getBatchTriggerReason")
  public Result getBatchTriggerReason() {
    List<BatchTriggerReason> batchTriggerReasons = BatchTriggerReason.CREDITS_NO_EXPIRED_AND_REAPPLY;
    List<Label<String>> labelList = batchTriggerReasons
        .stream()
        .map(o -> Label.gen(o.desc, o.name()))
        .collect(Collectors.toList());
    return EcResponseUtil.generate(labelList);
  }
}
