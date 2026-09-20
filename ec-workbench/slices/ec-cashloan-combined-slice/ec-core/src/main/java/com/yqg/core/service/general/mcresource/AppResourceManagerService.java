package com.yqg.core.service.general.mcresource;

import com.google.common.collect.Maps;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigType;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.mc.appresource.AppResourceConfig;
import com.yqg.core.service.user.UserService;
import com.yqg.ec.common.constant.AppResourceExtraParam;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.mc.client.spring.api.IAppResourceService;
import com.yqg.mc.common.enums.SDKType;
import com.yqg.mc.common.spring.request.appresource.AppResourceRequest;
import com.yqg.mc.common.spring.response.appresource.AppResourceResponse;
import com.yqg.mc.common.vo.appresource.AppResourceBaseInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.yqg.core.util.scope.ImpliedContextUtils.*;

@Service
@Slf4j
public class AppResourceManagerService {
  @Autowired
  private IAppResourceService appResourceService;
  @Autowired
  private UserService userService;
  @Autowired
  private AppResourceConfig appResourceConfig;


  public Map<String, List<AppResourceBaseInfo>> getResourceFromMc(GeneralPageConfigParam param,
                                                                  Collection<GeneralPageConfigType> configTypes) {
    // 屏蔽api渠道
    if (Optional.ofNullable(sourceType()).map(SourceType::isBlockingMarketingResource).orElse(false)) {
      return Maps.newHashMap();
    }
    // 屏蔽h5全流程
    if (Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)) {
      configTypes = configTypes.stream()
         .filter(item -> appResourceConfig.getH5WholeProcessTemplateCodeAllowList().contains(item.name()))
         .collect(Collectors.toList());
      if (CollectionUtils.isEmpty(configTypes)) {
        return Maps.newHashMap();
      }
    }
    try {
      AppResourceRequest request = buildRequest(param, configTypes.stream().map(item -> item.name()).collect(Collectors.toList()));
      AppResourceResponse appResource = appResourceService.getAppResource(request);
      appResource = processAppResourceResponse(appResource, param.sdkType.name());
      return Optional.ofNullable(appResource).map(item -> item.appResourceInfo).orElse(new HashMap<>());
    } catch (Exception e) {
      log.error("getResourceFromMc error， param:{}, configTypes:{}", JsonUtils.toString(param), configTypes, e);
      return new HashMap<>();
    }
  }

  public AppResourceResponse getResourceFromMcForApp(GeneralPageConfigParam param,
                                                     Collection<String> configTypes) {
    // 屏蔽api渠道
    if (Optional.ofNullable(sourceType()).map(SourceType::isBlockingMarketingResource).orElse(false)) {
      return new AppResourceResponse();
    }
    // 屏蔽h5全流程
    if (Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)) {
      configTypes = configTypes.stream()
         .filter(item -> appResourceConfig.getH5WholeProcessTemplateCodeAllowList().contains(item))
         .collect(Collectors.toList());
      if (CollectionUtils.isEmpty(configTypes)) {
        return new AppResourceResponse();
      }
    }
    try {
      AppResourceRequest request = buildRequest(param, configTypes);
      AppResourceResponse response = appResourceService.getAppResource(request);
      return processAppResourceResponse(response, param.sdkType.name());
    } catch (Exception e) {
      log.error("getResourceFromMc error， param:{}, configTypes:{}", JsonUtils.toString(param), configTypes, e);
      return new AppResourceResponse();
    }
  }

  @NotNull
  private AppResourceRequest buildRequest(GeneralPageConfigParam param, Collection<String> configTypes) {
    AppResourceRequest request = new AppResourceRequest();
    request.configTypes = configTypes.stream().distinct().collect(Collectors.toList());
    request.build = param.build;
    request.sdkType = SDKType.valueOf(param.sdkType.name());
    request.deviceToken = param.deviceToken;
    request.userId = param.userId;
    request.normalizedMobileNumber = Objects.nonNull(param.userId) ? userService.getNormalizedMobileNumberByUserId(param.userId) : null;
    Map<String, Object> extraInfo = Optional.ofNullable(param.extraParams).orElse(new HashMap<>());
    AppResourceExtraParam.IDN_HOMEPAGE_LOAN_STATUS_V5.setValue(extraInfo, param.idnHomepageLoanStatusV5);
    AppResourceExtraParam.BUILD.setValue(extraInfo, param.build);
    AppResourceExtraParam.SOURCE_TYPE.setValue(extraInfo, param.sourceType);
    AppResourceExtraParam.USER_ID.setValue(extraInfo, param.userId);
    AppResourceExtraParam.DEVICE_TOKEN.setValue(extraInfo, param.deviceToken);
    AppResourceExtraParam.SDK_TYPE.setValue(extraInfo, param.sdkType);
    AppResourceExtraParam.TRIGGER_SOURCE.setValue(extraInfo, param.triggerSource);
    request.extraInfo = extraInfo;
    return request;
  }

  /**
   * Process the AppResourceResponse to set business_name to sdkType and requirementId to sitevars dynamic configuration number in pointMap
   *
   * @param response The AppResourceResponse to process
   * @param sdkType  The SDK type
   */
  private AppResourceResponse processAppResourceResponse(AppResourceResponse response, String sdkType) {
    if (response == null || response.appResourceInfo == null) {
      return response;
    }

    // Process each AppResourceBaseInfo
    for (List<AppResourceBaseInfo> infoList : response.appResourceInfo.values()) {
      if (infoList == null) {
        continue;
      }

      for (AppResourceBaseInfo info : infoList) {
        if (info == null) {
          continue;
        }
        info.pointMap = processPointMap(info.pointMap, sdkType);
      }
    }
    return response;
  }

  public Map<String, String> processPointMap(Map<String, String> pointMap) {
    return processPointMap(pointMap, Optional.ofNullable(sdkType()).map(Enum::name).orElse(null));
  }

  private Map<String, String> processPointMap(Map<String, String> pointMap, String sdkType) {
    Map<String, String> res = Maps.newHashMap();
    if (pointMap != null) {
      res.putAll(pointMap);
    }

    // Get the sitevars dynamic configuration number for requirementId
    Long requirementId = appResourceConfig.getRequirementId();

    // Get the key override configuration
    Map<String, String> keyOverride = appResourceConfig.getKeyOverride();

    // Set bizName to sdkType
    res.put("bizName", sdkType);

    // Set requirementId to sitevars dynamic configuration number
    res.put("requirementId", String.valueOf(requirementId));

    if (MapUtils.isNotEmpty(keyOverride)) {
      keyOverride.forEach((originKey, newKey) -> {
        String originVal = res.remove(originKey);
        if (StringUtils.isNotEmpty(originVal)) {
          res.put(newKey, originVal);
        }
      });
    }

    Map<String, String> filteredRes = new HashMap<>();
    for (Entry<String, String> entry : res.entrySet()) {
      if (Objects.isNull(entry) || Objects.isNull(entry.getKey())) {
        continue;
      }
      if (appResourceConfig.getTrackFilterKeys().contains(entry.getKey())) {
        continue;
      }
      filteredRes.put(entry.getKey(), entry.getValue());
    }
    return filteredRes;
  }

}
