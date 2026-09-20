package com.miyou.controllers.admin.risk;

import com.miyou.controllers.admin.risk.request.RiskFlowCheckConfigUpdateRequest;
import com.miyou.controllers.admin.risk.response.RiskFlowCheckConfigHistoryResponse;
import com.miyou.controllers.admin.risk.response.RiskFlowCheckConfigListResponse;
import com.miyou.controllers.core.YqgBaseController;
import com.yqg.core.service.risk.riskflowcheck.enums.PreCheckSceneType;
import com.yqg.core.service.risk.utilparam.RiskUtilParamTool;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.enums.Label;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.core.model.core.AdminUserIDThreadLocal;
import com.yqg.core.model.core.OperationLogRecordVo;
import com.yqg.core.model.sql.loan.account.enums.LogEventType;
import com.yqg.core.model.sql.loan.account.enums.ObjType;
import com.yqg.core.service.adminuser.AdminUserCoreService;
import com.yqg.core.service.adminuser.vo.AdminUserVO;
import com.yqg.core.service.operationlog.OperationLogService;
import com.yqg.core.service.risk.riskflowcheck.RiskFlowCheckConfigService;
import com.yqg.core.service.risk.riskflowcheck.enums.PreCheckUtilType;
import com.yqg.core.service.risk.riskflowcheck.enums.RiskFlowCheckType;
import com.yqg.core.service.risk.riskflowcheck.vo.ConfigUpdateResponse;
import com.yqg.core.service.risk.riskflowcheck.vo.ConfigUpdateResultResponse;
import com.yqg.core.service.risk.riskflowcheck.vo.ConfigUpdateVO;
import com.yqg.core.service.risk.riskflowcheck.vo.RiskFlowCheckConfigVO;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ZiP on 2021/11/19.
 */
@RestController
public class RiskPreCheckController extends YqgBaseController {
  @Autowired
  private RiskFlowCheckConfigService riskFlowCheckConfigService;
  @Autowired
  private OperationLogService operationLogService;
  @Autowired
  private RiskUtilParamTool riskUtilParamTool;
  @Autowired
  private AdminUserCoreService adminUserService;

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.QUERY')")
  @GetMapping(path = "/admin/operation/loan/getRiskFlowCheckType")
  public Result getRiskFlowCheckType() {
    List<RiskFlowCheckType> types = RiskFlowCheckType.NORMAL_TYPE;
    List<Label<TT>> labels = types.stream()
        .filter(type -> type.sceneType == PreCheckSceneType.MULTI_LOAN)
        .map(o -> Label.gen(TT.gen(o.desc), o.code))
        .collect(Collectors.toList());
    return EcResponseUtil.generate(labels);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.CREATE')")
  @GetMapping(path = "/admin/operation/loan/getPreCheckUtilType")
  public Result getPreCheckUtilType() {
    List<PreCheckUtilType> types = Arrays.asList(PreCheckUtilType.values());
    List<Label<TT>> labels = types.stream()
        .filter(o -> o.sceneTypes.contains(PreCheckSceneType.MULTI_LOAN))
        .map(o -> Label.gen(TT.gen(o.desc), String.valueOf(o.code)))
        .collect(Collectors.toList());
    return EcResponseUtil.generate(labels);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.QUERY')")
  @GetMapping(path = "/admin/operation/loan/queryByRiskFlowCheckType")
  public Result queryByRiskFlowCheckType(
      @RequestParam(value = "checkType") String checkType,
      @RequestParam(value = "sdkType", required = false) String sdkType,
      @RequestParam(value = "enable", required = false) Boolean enable,
      @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
      @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
  ) {
    SDKType sdk = sdkType == null ? SDKType.IDN_YQD : SDKType.valueOf(sdkType);
    RiskFlowCheckType type = RiskFlowCheckType.fromCode(checkType);

    List<RiskFlowCheckConfigVO> configVOs =
        riskFlowCheckConfigService.getConfigByTypePaged(pageNo, pageSize, type, sdk, enable);
    Integer total = riskFlowCheckConfigService.countConfigByType(type, sdk, enable);

    RiskFlowCheckConfigListResponse response =
        new RiskFlowCheckConfigListResponse().from(configVOs, total);
    return EcResponseUtil.generate(response);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.CREATE')")
  @PostMapping(path = "/admin/operation/loan/updateRiskFlowCheckConfig")
  public Result update(@RequestBody @Valid RiskFlowCheckConfigUpdateRequest request) {
    ConfigUpdateResponse response = JsonUtils.from(request.checkConfig, ConfigUpdateResponse.class);
    ConfigUpdateVO vo = ConfigUpdateVO.from(response);
    ConfigUpdateResultResponse resultResponse = riskFlowCheckConfigService.update(vo);
    if (resultResponse.success) {
      operationLogService.insert("change risk flow check config", null,
          request.checkConfig,
          LogEventType.SAVE_RISK_FLOW_CHECK_CONFIG,
          ObjType.RISK_FLOW_CHECK_CONFIG,
          resultResponse.configId,
          AdminUserIDThreadLocal.get());
    }
    return EcResponseUtil.generate(resultResponse);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.QUERY')")
  @GetMapping(path = "/admin/operation/loan/listRiskFlowCheckConfigHistory")
  public Result listHistory(@RequestParam("id") Long configId) {
    List<OperationLogRecordVo> logRecords = operationLogService.findByObj(ObjType.RISK_FLOW_CHECK_CONFIG, configId);
    Set<Long> adminUserIds = logRecords.stream().map(OperationLogRecordVo::getUserOpt).collect(Collectors.toSet());
    Map<Long, AdminUserVO> adminUserMap = adminUserService.getUserInfoByIds(adminUserIds);

    List<RiskFlowCheckConfigHistoryResponse> responseList = logRecords.stream()
        .map(record -> new RiskFlowCheckConfigHistoryResponse(
            record.getTimeCreated(),
            adminUserMap.get(record.getUserOpt()),
            JsonUtils.fromOrException(record.getContentAfter(), ConfigUpdateResponse.class)
        ))
        .collect(Collectors.toList());
    return EcResponseUtil.generate(responseList);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.RISK.CONFIG.CREATE')")
  @GetMapping(path = "/admin/operation/loan/queryRiskFlowCheckUtilParam")
  public Result queryParam(@RequestParam("code") int utilCode) {
    return EcResponseUtil.generate(riskUtilParamTool.generateUtilParamVO(PreCheckUtilType.fromCode(utilCode).paramClass));
  }
}
