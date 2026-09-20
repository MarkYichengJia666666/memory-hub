package com.miyou.controllers.admin.risk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.miyou.controllers.admin.loan.creditsbyapp.request.SetRiskFlowRequest;
import com.miyou.controllers.admin.loan.creditsbyapp.request.UpdateRiskFlowRequest;
import com.miyou.controllers.admin.loan.creditsbyapp.response.*;
import com.miyou.controllers.admin.review.decorator.RiskFlowConfigParamDecorator;
import com.miyou.controllers.admin.risk.request.BatchSetRiskFlowObserverRequest;
import com.miyou.controllers.admin.risk.response.RiskFlowResponse;
import com.miyou.controllers.core.YqgBaseController;
import com.yqg.chidori.client.spring.internalReview.PreReview;
import com.yqg.chidori.client.spring.internalReview.ReviewType;
import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.core.AdminUserIDThreadLocal;
import com.yqg.core.model.core.RevType;
import com.yqg.core.model.generated.tables.records.RiskFlowAudRecord;
import com.yqg.core.service.adminuser.AdminUserCoreService;
import com.yqg.core.service.adminuser.vo.AdminUserVO;
import com.yqg.core.service.cashloan.risk.vo.EventTypeVO;
import com.yqg.core.service.loan.account.LoanUserEventService;
import com.yqg.core.service.ly.LyAdminService;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.RiskFLowNotifyService;
import com.yqg.core.service.risk.riskflow.RiskFlowAudService;
import com.yqg.core.service.risk.riskflow.RiskFlowSwitchService;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowAudVO;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowInfoVO;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowVO;
import com.yqg.core.service.risk.riskuser.RiskIdnLocalEmployeeService;
import com.yqg.core.util.changeevent.ChangeEventUtils;
import com.yqg.core.util.changeevent.ChangeEventVO;
import com.yqg.core.util.changeevent.ConfigScene;
import com.yqg.core.util.changeevent.EventPlatform;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.event.enums.ChangeEventStatus;
import com.yqg.event.enums.ChangeEventType;
import com.yqg.overseasrisk.client.mesh.api.ruleenginereborn.IEngineRiskFlowService;
import com.yqg.overseasrisk.common.mvc.ruleenginereborn.riskflow.EngineRiskFlowIdResponse;
import com.yqg.overseasrisk.common.mvc.ruleenginereborn.riskflow.UpdateRiskFlowEngineIdRequest;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ember on 2017/1/16.
 */
@Slf4j
@RestController
public class RiskFlowController extends YqgBaseController {
  @Autowired
  private RiskFlowSwitchService riskFlowSwitchService;
  @Autowired
  private RiskFlowAudService riskFlowAudService;
  @Autowired
  private AdminUserCoreService adminUserService;
  @Autowired
  private RiskFLowNotifyService riskFLowNotifyService;
  @Autowired
  private LoanUserEventService loanUserEventService;
  @Autowired
  private RiskIdnLocalEmployeeService localEmployeeService;
  @Autowired
  private IEngineRiskFlowService engineRiskFlowService;
  @Autowired
  private LyAdminService lyAdminService;
  @Autowired
  private EcRiskFlowService ecRiskFlowService;
  @Autowired
  private RiskConfig riskConfig;


  private static final Long NULL_ENGINE_RISK_FLOW_ID = -1L;
  private static final String NO_PERMISSION = "Your account has no permission!";
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.CREATE')")
  @PostMapping(path = "/admin/operation/loan/setRiskFlow")
  @PreReview(type = ReviewType.SPECIFIC_PRIVILEGE, decoratorBean = RiskFlowConfigParamDecorator.class, template = "risk_flow_config.ftl")
  public Result setRiskFlow(@RequestBody @Valid SetRiskFlowRequest request) {
    if (riskConfig.getStopSaveDataToRiskEngine()) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("此功能正在维护中，预计5分钟后恢复，请稍后再试"));
    }
    long beginTime = Clock.now();
    if (isIdnLocalEmployeeNotPermission()) {
      return EcResponseUtil.generate(EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen(NO_PERMISSION)));
    }
    EventTypeVO eventTypeVO = loanUserEventService.fromEventTypeName(request.eventType);

    preCheckRiskFlow(request, eventTypeVO);
    if (Objects.nonNull(request.errorCode)) {
      return EcResponseUtil.generate(request);
    }

    if (!hasOtherDefaultRiskFlow(eventTypeVO.eventId, null)) {
      request.isDefault = BooleanType.TRUE;
    } else if (request.isDefault == BooleanType.TRUE) {
      setOtherRiskFlowsNonDefault(eventTypeVO.eventId, null);
    }
    String states = StringUtils.isNotEmpty(StringUtils.deleteWhitespace(request.states)) ? request.states : riskConfig.getDefaultRiskFlowStates();
    Long riskFlowId = riskFlowSwitchService.setRiskFlow(request.percentage.divide(BigDecimal.valueOf(100L), 4, BigDecimal.ROUND_HALF_UP), states, request.enabled, request.name, request.isDefault, eventTypeVO, request.comment, request.priority);
    try {
      UpdateRiskFlowEngineIdRequest updateRiskFlowEngineIdRequest = new UpdateRiskFlowEngineIdRequest();
      updateRiskFlowEngineIdRequest.riskFlowId = riskFlowId;
      updateRiskFlowEngineIdRequest.engineRiskFlowId = request.engineRiskFlowId;
      updateRiskFlowEngineIdRequest.engineRiskFlowVersion = request.engineRiskFlowVersion;
      engineRiskFlowService.updateRiskFlowEngineId(updateRiskFlowEngineIdRequest);
    } catch (Exception e) {
      return EcResponseUtil.generate(EcException.error(EcExceptionType.RISK_FLOW_ENGINE_MAPPING_ERROR, "新老引擎id-版本映射配置异常，请核对后重试"));
    }
    RiskFlowVO riskFlowVO = riskFlowSwitchService.getRiskFlow(riskFlowId);
    RiskFlowAudRecord riskFlowAudRecord = riskFlowAudService.insertRiskFlowAud(riskFlowVO, AdminUserIDThreadLocal.get(), RevType.CREATE);
    riskFLowNotifyService.sendRiskFlowOperationByEmail(riskFlowVO, AdminUserIDThreadLocal.get(), RevType.CREATE);
    ChangeEventVO changeEventVO = ChangeEventVO.from(
        ConfigScene.RISK_AUTO_REVIEW_STANDARD_CONFIG,
        ChangeEventType.ConfigChange,
        EventPlatform.EASYCASH_ADMIN,
        beginTime,
        Clock.now(),
        getAdminUserEmail(),
        ChangeEventStatus.Done,
        null,
        RiskFlowInfoVO.from(riskFlowVO, riskFlowAudRecord)
    );
    ChangeEventUtils.convertVOToChangeEventAndSentEvent(changeEventVO);
    return EcResponseUtil.generateSuccess();
  }


  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.CREATE')")
  @PostMapping(path = "/admin/operation/loan/updateRiskFlow")
  @PreReview(type = ReviewType.SPECIFIC_PRIVILEGE, decoratorBean = RiskFlowConfigParamDecorator.class, template = "risk_flow_config.ftl")
  public Result updateRiskFlow(@RequestBody @Valid UpdateRiskFlowRequest request) {
    if (riskConfig.getStopSaveDataToRiskEngine()) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("此功能正在维护中，预计5分钟后恢复，请稍后再试"));
    }
    long beginTime = Clock.now();
    EventTypeVO eventTypeVO = loanUserEventService.fromEventTypeName(request.eventType);
    preCheckUpdateRiskFlow(request, eventTypeVO);
    if (Objects.nonNull(request.errorCode)) {
      return EcResponseUtil.generate(request);
    }
    if (isIdnLocalEmployeeNotPermission(request.id)) {
      return EcResponseUtil.generate(EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen(NO_PERMISSION)));
    }

    if (!hasOtherDefaultRiskFlow(eventTypeVO.eventId, request.id)) {
      request.isDefault = BooleanType.TRUE;
    } else if (request.isDefault == BooleanType.TRUE) {
      setOtherRiskFlowsNonDefault(eventTypeVO.eventId, request.id);
    }

    String states = StringUtils.isNotEmpty(StringUtils.deleteWhitespace(request.states)) ? request.states : riskConfig.getDefaultRiskFlowStates();
    try {
      UpdateRiskFlowEngineIdRequest updateRiskFlowEngineIdRequest = new UpdateRiskFlowEngineIdRequest();
      updateRiskFlowEngineIdRequest.riskFlowId = request.id;
      updateRiskFlowEngineIdRequest.engineRiskFlowId = request.engineRiskFlowId;
      updateRiskFlowEngineIdRequest.engineRiskFlowVersion = request.engineRiskFlowVersion;
      engineRiskFlowService.updateRiskFlowEngineId(updateRiskFlowEngineIdRequest);
    } catch (Exception e) {
      return EcResponseUtil.generate(EcException.error(EcExceptionType.RISK_FLOW_ENGINE_MAPPING_ERROR, "新老引擎id-版本映射配置异常，请核对后重试"));
    }
    RiskFlowInfoVO beforeRiskFlowInfoVO = getRiskFlowInfoVOById(request.id);
    riskFlowSwitchService.updateRiskFlow(request.id, request.name, request.percentage.divide(BigDecimal.valueOf(100L), 4, BigDecimal.ROUND_HALF_UP), states, request.enabled, request.isDefault, eventTypeVO, request.comment, request.priority);
    RiskFlowVO riskFlowVO = riskFlowSwitchService.getRiskFlow(request.id);
    RiskFlowAudRecord riskFlowAudRecord = riskFlowAudService.insertRiskFlowAud(riskFlowVO, AdminUserIDThreadLocal.get(), RevType.UPDATE);
    RiskFlowInfoVO afterRiskFlowInfoVO = getRiskFlowInfoVOById(riskFlowVO.id);
    afterRiskFlowInfoVO.audList = ImmutableList.of(RiskFlowAudVO.from(riskFlowAudRecord));
    riskFLowNotifyService.sendRiskFlowOperationByEmail(riskFlowVO, AdminUserIDThreadLocal.get(), RevType.UPDATE);
    ChangeEventVO changeEventVO = ChangeEventVO.from(
        ConfigScene.RISK_AUTO_REVIEW_STANDARD_CONFIG,
        ChangeEventType.ConfigChange,
        EventPlatform.EASYCASH_ADMIN,
        beginTime,
        Clock.now(),
        getAdminUserEmail(),
        ChangeEventStatus.Done,
        beforeRiskFlowInfoVO,
        afterRiskFlowInfoVO
    );
    ChangeEventUtils.convertVOToChangeEventAndSentEvent(changeEventVO);
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.QUERY')")
  @GetMapping(path = "/admin/operation/loan/getRiskFlow")
  public Result getRiskFlow(@RequestParam("id") Long id) {
    if (isIdnLocalEmployeeNotPermission(id)) {
      return EcResponseUtil.generate(EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen(NO_PERMISSION)));
    }
    RiskFlowVO riskFlowVO = riskFlowSwitchService.getRiskFlow(id);
    RiskFlowResponse response = RiskFlowResponse.from(riskFlowVO);
    List<EngineRiskFlowIdResponse> engineRiskFlowIdResponses = engineRiskFlowService.queryRiskFlowEngineIds(Collections.singletonList(id));
    if (CollectionUtils.isNotEmpty(engineRiskFlowIdResponses)) {
      response.engineRiskFlowId = engineRiskFlowIdResponses.get(0).engineRiskFlowId;
      response.engineRiskFlowVersion = engineRiskFlowIdResponses.get(0).engineRiskFlowVersion;
    }
    return EcResponseUtil.generate(response);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.QUERY')")
  @GetMapping(path = "/admin/operation/loan/listRiskFlows")
  public Result listRiskFlows() {
    List<ListRiskFlowItemResponse> itemList = fetchAndPrepareRiskFlowItems();
    Map<Long, List<ListRiskFlowItemResponse>> groupedByRiskEngineFlowId = itemList.stream()
            .collect(Collectors.groupingBy(item -> item.engineRiskFlowId));
    List<ListRiskFlowsResponseV2> response = buildAndSortResponse(groupedByRiskEngineFlowId);
    return EcResponseUtil.generate(response);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.QUERY')")
  @GetMapping(path = "/admin/operation/loan/listRiskFlowsV2")
  public Result listRiskFlowsV2() {
    ListRiskFlowsResponse response = new ListRiskFlowsResponse();
    List<RiskFlowVO> riskFlowVOs = riskFlowSwitchService.listRiskFlow();
    List<RiskFlowVO> displayRiskFlowVOList = lyAdminService.getRiskFlowVOList(riskFlowVOs);
    List<ListRiskFlowItemResponse> itemList = ecRiskFlowService.generateListRiskFlowItemResponses(displayRiskFlowVOList, getSDKType());
    response.responses.addAll(itemList);
    translateRiskFlowName(response);
    return EcResponseUtil.generate(response);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.QUERY')")
  @GetMapping(path = "/admin/operation/loan/listRiskFlowsV3")
  public Result listRiskFlowsV3(@RequestParam(value = "enabled", required = false) BooleanType enabled,
                                @RequestParam(value = "isDefault", required = false) BooleanType isDefault,
                                @RequestParam(value = "category", required = false) LoanUserRiskType riskType,
                                @RequestParam(value = "eventType", required = false) String eventType,
                                @RequestParam(value = "engineRiskFlowId", required = false) Long engineRiskFlowId) {
    List<ListRiskFlowItemResponse> itemList = fetchAndPrepareRiskFlowItemsWithoutEngineRiskFlow();
    itemList = filterListRiskFlows(itemList, enabled, isDefault, riskType, eventType, engineRiskFlowId);
    itemList.sort(Comparator.comparing(ListRiskFlowItemResponse::getId, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ListRiskFlowItemResponse::getEngineRiskFlowId, Comparator.nullsLast(Comparator.reverseOrder())));
    return EcResponseUtil.generate(ListRiskFlowsResponseV3.from(itemList));
  }

  private List<ListRiskFlowItemResponse> filterListRiskFlows(List<ListRiskFlowItemResponse> itemList,
                                                             BooleanType enabled,
                                                             BooleanType isDefault,
                                                             LoanUserRiskType riskType,
                                                             String eventType,
                                                             Long engineRiskFlowId) {
    return itemList.stream()
            .filter(f -> Objects.isNull(enabled) || enabled.bool.equals(f.enabled.bool))
            .filter(f -> Objects.isNull(isDefault) || isDefault.bool.equals(f.isDefault.bool))
            .filter(f -> Objects.isNull(riskType) || riskType.name().equals(f.eventTypeCategory))
            .filter(f -> Objects.isNull(eventType) || eventType.equals(f.eventType))
            .filter(f -> Objects.isNull(engineRiskFlowId) || engineRiskFlowId.equals(f.engineRiskFlowId))
            .collect(Collectors.toList());
  }

  private void translateRiskFlowName(ListRiskFlowsResponse response) {
    response.responses.forEach(
        listRiskFlowItemResponse ->
            listRiskFlowItemResponse.name = TT.gen(listRiskFlowItemResponse.name).toString());
  }
  
  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.QUERY')")
  @GetMapping(path = "/admin/operation/loan/listRiskFlowAuds")
  public Result listRiskFlowAuds(@RequestParam("id") Long id) {
    if (isIdnLocalEmployeeNotPermission(id)) {
      return EcResponseUtil.generate(EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen(NO_PERMISSION)));
    }
    ListRiskFlowAudsResponse response = new ListRiskFlowAudsResponse();
    List<RiskFlowAudRecord> riskFlowAudRecords = riskFlowAudService.getRiskFlowAuds(id);
    Set<Long> userOptIds = riskFlowAudRecords.stream().map(RiskFlowAudRecord::getUserOpt).collect(Collectors.toSet());
    Map<Long, AdminUserVO> userOptMap = adminUserService.getUserInfoByIds(userOptIds);
    response.responses = riskFlowAudRecords.stream()
        .map(record -> ListRiskFlowAudsItemResponse.from(record, userOptMap.get(record.getUserOpt())))
        .collect(Collectors.toList());
    return EcResponseUtil.generate(response);
  }

  private boolean isIdnLocalEmployeeNotPermission(Long riskFlowId) {
    String userMail = localEmployeeService.getLocalAdminUserMail();
    return localEmployeeService.isNotPermissionMulti(userMail, () -> {
      // 获取配置id列表，当配置不生效返回null
      List<Long> configRiskFlowIds = localEmployeeService.operateByRiskFlowIdsConfig(config -> CollectionUtils.isNotEmpty(config.riskFlowIds) ? config.riskFlowIds : Collections.emptyList());
      // 配置列表不包含该id，返回true，无权限；配置列表包含该id，返回false，有权限
      return configRiskFlowIds != null && !configRiskFlowIds.contains(riskFlowId);
    });
  }

  private boolean isIdnLocalEmployeeNotPermission() {
    String userMail = localEmployeeService.getLocalAdminUserMail();
    // 本地风控雇员不开放该接口
    return localEmployeeService.isNotPermissionMulti(userMail, () -> true);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.CREATE')")
  @PostMapping(path = "/admin/operation/loan/batchSetObserver")
  public Result batchSetObserver(@RequestBody @Valid BatchSetRiskFlowObserverRequest request) {
    if (riskConfig.getStopSaveDataToRiskEngine()) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("此功能正在维护中，预计5分钟后恢复，请稍后再试"));
    }
    long beginTime = Clock.now();
    if (isIdnLocalEmployeeNotPermission()) {
      return EcResponseUtil.generate(EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen(NO_PERMISSION)));
    }
    List<ChangeEventVO> changeEventVOS = Lists.newArrayList();
    request.riskFlowIds.forEach(o -> {
      RiskFlowInfoVO originRiskFlowInfoVO = getRiskFlowInfoVOById(o);
      RiskFlowVO riskFlowVO = riskFlowSwitchService.getRiskFlow(o);
      Map<String, Object> configMap = JsonUtils.fromOrException(riskFlowVO.states, new TypeReference<LinkedHashMap<String, Object>>() {
      });
      List<Long> extraRuleSetIds = (List<Long>) configMap.getOrDefault("extraRuleSetIds", Lists.newArrayList());
      extraRuleSetIds = ListUtils.sum(extraRuleSetIds, request.ruleSetIds);
      configMap.put("extraRuleSetIds", extraRuleSetIds);
      riskFlowVO.states = JsonUtils.toString(configMap);
      riskFlowSwitchService.updateRiskFlow(riskFlowVO.id, riskFlowVO.name, riskFlowVO.percentage.divide(BigDecimal.valueOf(100), 4,
          BigDecimal.ROUND_HALF_UP), riskFlowVO.states, riskFlowVO.enabled, riskFlowVO.isDefault, riskFlowVO.eventType, riskFlowVO.comment);
      RiskFlowAudRecord riskFlowAudRecord = riskFlowAudService.insertRiskFlowAud(riskFlowVO, AdminUserIDThreadLocal.get(), RevType.UPDATE);
      changeEventVOS.add(ChangeEventVO.from(
          ConfigScene.RISK_AUTO_REVIEW_OBSERVER_CONFIG,
          ChangeEventType.ConfigChange,
          EventPlatform.EASYCASH_ADMIN,
          beginTime,
          Clock.now(),
          getAdminUserEmail(),
          ChangeEventStatus.Done,
          originRiskFlowInfoVO,
          RiskFlowInfoVO.from(riskFlowVO, riskFlowAudRecord)
          )
      );
    });
    riskFLowNotifyService.sendRiskFlowBatchSetObserverByEmail(request.riskFlowIds, request.ruleSetIds, AdminUserIDThreadLocal.get());
    ChangeEventUtils.convertVOListToChangeEventListAndSentEvent(changeEventVOS);
    return EcResponseUtil.generateSuccess();
  }

  private boolean hasOtherDefaultRiskFlow(Long eventId, Long currRiskFlowId) { // currRiskFlowId is null when creating new risk flow
    return riskFlowSwitchService.findEnabledWithEventType(() -> eventId).stream()
        .anyMatch(o -> !o.id.equals(currRiskFlowId) && o.isDefault == BooleanType.TRUE);
  }

  private void setOtherRiskFlowsNonDefault(Long eventId, Long currRiskFlowId) { // currRiskFlowId is null when creating new risk flow
    List<RiskFlowVO> riskFlowVOs = riskFlowSwitchService.findEnabledWithEventType(() -> eventId);
    for (RiskFlowVO riskFlowVO : riskFlowVOs) {
      if (!riskFlowVO.id.equals(currRiskFlowId) && riskFlowVO.isDefault == BooleanType.TRUE) {
        riskFlowSwitchService.updateRiskFlow(riskFlowVO.id, riskFlowVO.name, riskFlowVO.percentage.divide(BigDecimal.valueOf(100L), 4, BigDecimal.ROUND_HALF_UP),
            riskFlowVO.states, riskFlowVO.enabled, BooleanType.FALSE, riskFlowVO.eventType, riskFlowVO.comment, riskFlowVO.priority);
        RiskFlowVO updatedRiskFlowVO = riskFlowSwitchService.getRiskFlow(riskFlowVO.id);
        riskFlowAudService.insertRiskFlowAud(updatedRiskFlowVO, AdminUserIDThreadLocal.get(), RevType.UPDATE);
      }
    }
  }

  private RiskFlowInfoVO getRiskFlowInfoVOById(Long riskFlowId) {
    RiskFlowVO riskFlowVO = riskFlowSwitchService.getRiskFlow(riskFlowId);
    return RiskFlowInfoVO.from(riskFlowVO);
  }

  private String isEnabledAndDefaultRiskFlowExist(List<RiskFlowVO> riskFlowVOList,
                                                  SetRiskFlowRequest riskFlowRequest) {

    if (riskFlowRequest.enabled.bool &&
        riskFlowRequest.isDefault.bool &&
        CollectionUtils.isNotEmpty(riskFlowVOList)) {
      return riskFlowVOList
          .stream()
          .map(riskFlowVO -> riskFlowVO.id.toString())
          .collect(Collectors.joining(","));
    }
    return null;
  }

  private boolean isDisableButDefault(SetRiskFlowRequest riskFlowRequest) {

    return !riskFlowRequest.enabled.bool && riskFlowRequest.isDefault.bool;
  }

  private void preCheckRiskFlow(SetRiskFlowRequest request, EventTypeVO eventTypeVO) {

    if (Objects.nonNull(request.needCheck) && !request.needCheck) {
      return;
    }

    if (isDisableButDefault(request)) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("禁用状态不可选为默认riskflow，请修改后保存"));
    }

    List<RiskFlowVO> riskFlowVOList = riskFlowSwitchService.findEnabledByEventTypes(Collections.singletonList(eventTypeVO))
        .stream()
        .filter(f -> f.isDefault.bool)
        .collect(Collectors.toList());

    // TODO (Hafiz, TAPD 1103190)
    // if (riskFlowVOList.size() == 0 && !request.isDefault.bool)

    String existRiskFlowIds = isEnabledAndDefaultRiskFlowExist(riskFlowVOList, request);
    if (Objects.nonNull(existRiskFlowIds)) {
      request.errorCode = EcExceptionType.UPDATE_RISK_FLOW_WARN_REPLACE_CURRENT_ENABLED_DEFAULT.code;
      request.errorMessage = TT.gen("此eventype下已有默认分流（riskflowid={0}）请确认是否保存", existRiskFlowIds).toString();
    }
  }

  private void preCheckUpdateRiskFlow(UpdateRiskFlowRequest request, EventTypeVO eventTypeVO) {
    preCheckRiskFlow(request, eventTypeVO);
    List<RiskFlowVO> riskFlowVOList = riskFlowSwitchService.findEnabledByEventTypes(Collections.singletonList(eventTypeVO))
        .stream()
        .filter(f -> f.isDefault.bool)
        .collect(Collectors.toList());

    if (riskFlowVOList.size() == 1 &&
        !request.isDefault.bool &&
        riskFlowVOList.get(0).id.equals(request.id)) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("此eventtype下无默认分流请修改后继续保存"));
    }
  }

  private List<ListRiskFlowItemResponse> fetchAndPrepareRiskFlowItems() {
    List<RiskFlowVO> riskFlowVOs = riskFlowSwitchService.listRiskFlow();
    List<ListRiskFlowItemResponse> itemList = ecRiskFlowService.generateListRiskFlowItemResponses(riskFlowVOs, getSDKType());
    itemList.forEach(item -> {
          if (Objects.isNull(item.engineRiskFlowId)) {
            item.engineRiskFlowId = NULL_ENGINE_RISK_FLOW_ID;
          }
        });
    return itemList;
  }

  private List<ListRiskFlowItemResponse> fetchAndPrepareRiskFlowItemsWithoutEngineRiskFlow() {
    List<RiskFlowVO> riskFlowVOs = riskFlowSwitchService.listRiskFlow();
    return ecRiskFlowService.generateListRiskFlowItemResponses(riskFlowVOs, getSDKType());
  }

  private List<ListRiskFlowsResponseV2> buildAndSortResponse(
          Map<Long, List<ListRiskFlowItemResponse>> groupedByRiskEngineFlowId) {
    List<ListRiskFlowsResponseV2> listRiskFlowsResponseV2s =
            groupedByRiskEngineFlowId.entrySet().stream()
                    .map(entry -> ListRiskFlowsResponseV2.from(entry.getKey(), entry.getValue()))
                    .sorted(
                            Comparator.comparing(
                                    ListRiskFlowsResponseV2::getEngineRiskFlowId,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
    listRiskFlowsResponseV2s.stream()
            .filter(listRiskFlowsResponseV2 -> CollectionUtils.isNotEmpty(listRiskFlowsResponseV2.items))
            .flatMap(listRiskFlowsResponseV2 -> listRiskFlowsResponseV2.items.stream())
            .filter(item -> Objects.nonNull(item) && Objects.nonNull(item.engineRiskFlowId) && NULL_ENGINE_RISK_FLOW_ID.equals(item.engineRiskFlowId))
            .forEach(item -> item.engineRiskFlowId = null);
    return listRiskFlowsResponseV2s;
  }
}