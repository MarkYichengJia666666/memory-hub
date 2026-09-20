package com.miyou.controllers.admin.general.pageconfig;

import com.miyou.controllers.admin.general.pageconfig.request.GeneralPageConfigFilterRuleRequest;
import com.miyou.controllers.admin.general.pageconfig.request.GeneralPageConfigFilterStrategyRequest;
import com.miyou.controllers.admin.general.pageconfig.request.GeneralPageConfigUpdateFilterStatus;
import com.miyou.controllers.admin.general.pageconfig.response.*;
import com.miyou.controllers.core.YqgBaseController;
import com.miyou.utilities.TableResp;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigStatus;
import com.yqg.core.service.general.pageconfig.GeneralPageConfig;
import com.yqg.core.service.general.pageconfig.filterstrategy.GeneralPageConfigFilterRuleService;
import com.yqg.core.service.general.pageconfig.filterstrategy.GeneralPageConfigFilterStrategyService;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.GeneralPageConfigFilterRuleType;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigFilterRuleVO;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigFilterStrategyVO;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.payload.BaseFilterRulePayload;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.ec.common.utils.annotation.AnnotationsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@PreAuthorize("hasAnyAuthority('MARKETING.GENERAL_PAGE_CONFIG')")
@RestController
public class GeneralPageConfigFilterController extends YqgBaseController {

  @Autowired
  private GeneralPageConfigFilterRuleService generalPageConfigFilterRuleService;
  @Autowired
  private GeneralPageConfigFilterStrategyService generalPageConfigFilterStrategyService;
  @Autowired
  private GeneralPageConfig generalPageConfig;

  @PostMapping(path = "/admin/operation/general/filter/rule")
  public Result createRule(@RequestBody GeneralPageConfigFilterRuleRequest request) {
    if (!request.pushCreate && generalPageConfig.createStrategySwitch()) {
      throw EcException.error("创建操作请联系管理员");
    }
    GeneralPageConfigFilterRuleVO ruleVO = generalPageConfigFilterRuleService.create(request.name, request.type, request.payload, request.description, getAdminUserEmail());
    return EcResponseUtil.generate(GeneralPageConfigFilterRuleResponse.from(ruleVO));
  }

  @GetMapping(path = "/admin/operation/general/filter/rulePayloadConfig")
  public Result getRulePayloadConfig(@RequestParam(name = "type") GeneralPageConfigFilterRuleType type) {
    TableResp<BaseFilterRulePayload> payloadTableResp = new TableResp<>();
    BaseFilterRulePayload payload;
    try {
      payload = generalPageConfigFilterRuleService.getPayloadClass(type).newInstance();
      payload.description = generalPageConfig.getRuleDescription(type);
      payloadTableResp.add(payload);
      return EcResponseUtil.generate(payloadTableResp);
    } catch (Exception e) {
      throw EcException.error("get rule payload config error ! ", e);
    }
  }

  @GetMapping(path = "/admin/operation/general/filter/ruleTypes")
  public Result listRuleTypes() {
    Map<String, String> resultMap = Arrays.stream(GeneralPageConfigFilterRuleType.values())
        .filter(item -> !AnnotationsUtils.isDeprecatedEnum(item))
        .collect(Collectors.toMap(GeneralPageConfigFilterRuleType::name, o -> o.desc));
    return EcResponseUtil.generate(resultMap);
  }

  @GetMapping(path = "/admin/operation/general/filter/rules")
  public Result listRules(
      @RequestParam(name = "type", required = false) GeneralPageConfigFilterRuleType type,
      @RequestParam(name = "name", required = false) String name,
      @RequestParam(name = "operator", required = false) String operator,
      @RequestParam(name = "pageSize") Integer pageSize,
      @RequestParam(name = "pageNo") Integer pageNo,
      @RequestParam(name = "filterStatus", required = false) GeneralPageConfigStatus filterStatus
  ) {
    Integer offset = pageSize * (pageNo - 1);
    Integer limit = pageSize;
    List<GeneralPageConfigFilterRuleType> typeList = type == null ? Arrays.asList(GeneralPageConfigFilterRuleType.values()) :
        Collections.singletonList(type);
    List<GeneralPageConfigFilterRuleVO> filterRuleVOList = generalPageConfigFilterRuleService.fetchByCondition(typeList, name, operator, filterStatus, offset, limit);
    Integer count = generalPageConfigFilterRuleService.countByCondition(typeList, name, operator, filterStatus);
    return EcResponseUtil.generate(GeneralPageConfigFilterRuleListResponse.from(filterRuleVOList, count));
  }

  @GetMapping(path = "/admin/operation/general/filter/ruleTree")
  public Result getRuleTree(@RequestParam(name = "name", required = false) String name) {
    List<GeneralPageConfigFilterRuleVO> filterRuleVOList = generalPageConfigFilterRuleService.fetchByNameAndEnabledForAdmin(name);
    return EcResponseUtil.generate(GeneralPageConfigFilterRuleTreeResponse.from(filterRuleVOList));
  }

  @PostMapping(path = "/admin/operation/general/filter/strategy")
  public Result createStrategy(@RequestBody @Valid GeneralPageConfigFilterStrategyRequest request) {
    if (!request.pushCreate && generalPageConfig.createStrategySwitch()) {
      throw EcException.error("创建操作请联系管理员");
    }
    GeneralPageConfigFilterStrategyVO strategyVO = generalPageConfigFilterStrategyService.create(request.name, request.strategyContainer, request.description, request.diversionInfo, getAdminUserEmail());
    return EcResponseUtil.generate(GeneralPageConfigFilterStrategyResponse.from(strategyVO));
  }

  @GetMapping(path = "/admin/operation/general/filter/strategyList")
  public Result listStrategyList(
      @RequestParam(name = "id", required = false) Long id,
      @RequestParam(name = "name", required = false) String name,
      @RequestParam(name = "operator", required = false) String operator,
      @RequestParam(name = "pageSize", required = false) Integer pageSize,
      @RequestParam(name = "pageNo", required = false) Integer pageNo,
      @RequestParam(name = "filterStatus", required = false) GeneralPageConfigStatus filterStatus
  ) {
    Integer offset;
    Integer limit;
    if (pageNo == null || pageSize == null) {
      offset = 0;
      limit = Integer.MAX_VALUE;
    } else {
      offset = pageSize * (pageNo - 1);
      limit = pageSize;
    }
    List<GeneralPageConfigFilterStrategyVO> filterStrategyVOList = generalPageConfigFilterStrategyService.fetchByCondition(id, name, operator, filterStatus, offset, limit);
    Integer count = generalPageConfigFilterStrategyService.countByCondition(id, name, operator, filterStatus);
    return EcResponseUtil.generate(GeneralPageConfigFilterStrategyListResponse.from(filterStrategyVOList, count));
  }

  @PostMapping(path = "/admin/operation/general/filter/strategy/updateFilterStatus")
  public Result updateStrategyFilterStatus(@RequestBody GeneralPageConfigUpdateFilterStatus request) {
    generalPageConfigFilterStrategyService.updateFilterStatus(request.id, request.filterStatus, getAdminUserEmail());
    return EcResponseUtil.generateSuccess();
  }

  @PostMapping(path = "/admin/operation/general/filter/rule/updateFilterStatus")
  public Result updateRuleFilterStatus(@RequestBody GeneralPageConfigUpdateFilterStatus request) {
    generalPageConfigFilterRuleService.updateFilterStatus(request.id, request.filterStatus, getAdminUserEmail());
    return EcResponseUtil.generateSuccess();
  }
}
