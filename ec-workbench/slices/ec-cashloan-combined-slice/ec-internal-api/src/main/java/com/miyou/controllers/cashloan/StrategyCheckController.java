package com.miyou.controllers.cashloan;

import com.miyou.utilities.RequestUtil;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.general.pageconfig.StrategyMonitorService;
import com.yqg.core.service.general.pageconfig.filterstrategy.GeneralPageConfigFilterStrategyService;
import com.yqg.core.service.general.pageconfig.filterstrategy.threadlocal.GeneralPageConfigFilterThreadLocal;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.mc.appresource.AppResourceConfig;
import com.yqg.core.service.sourcetype.SourceTypeService;
import com.yqg.core.service.user.UserGeneralPageConfigFilterService;
import com.yqg.core.service.user.UserService;
import com.yqg.core.util.scope.Scope;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.spring.request.CheckStrategyRequest;
import com.yqg.ec.common.spring.response.CheckStrategyResponse;
import com.yqg.ec.common.spring.response.CheckStrategyResponse.CheckStrategyResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.yqg.core.configure.devconfig.DynamicThreadExecutorConfiguration.CHECK_STRATEGY_EXECUTOR;

@Slf4j
@RestController
public class StrategyCheckController {
  @Autowired
  private UserGeneralPageConfigFilterService userGeneralPageConfigFilterService;
  @Autowired
  private GeneralPageConfigFilterStrategyService generalPageConfigFilterStrategyService;
  @Autowired
  private StrategyMonitorService strategyMonitorService;
  @Autowired
  private AppResourceConfig appResourceConfig;
  @Resource(name = CHECK_STRATEGY_EXECUTOR)
  private ExecutorService checkStrategyExecutor;
  @Autowired
  private UserService userService;
  @Autowired
  private SourceTypeService sourceTypeService;


  @GetMapping("/ecInternalApi/checkStrategy/userId")
  public Boolean checkStrategyUserId(@RequestParam("strategyId") Long strategyId, @RequestParam("userId") Long userId) {
    if (strategyId == null || userId == null) {
      return false;
    }
    return userGeneralPageConfigFilterService.hitStrategy(strategyId, userId);
  }

  @GetMapping("/ecInternalApi/checkStrategy/deviceId")
  public Boolean checkStrategyDeviceId(@RequestParam("strategyId") Long strategyId, @RequestParam("deviceId") String deviceId) {
    if (strategyId == null || deviceId == null) {
      return false;
    }
    return userGeneralPageConfigFilterService.hitStrategyDeviceToken(strategyId, deviceId);
  }

  /**
   * 只支持对一个用户的策略检查
   *
   * @param checkStrategyRequest
   * @return
   */
  @PostMapping("/ecInternalApi/checkStrategy")
  public CheckStrategyResponse checkStrategy(@RequestBody CheckStrategyRequest checkStrategyRequest,
                                             @RequestHeader(value = "fintopia-source-app", defaultValue = "UN_KNOW") String sourceApp) {
    if (CollectionUtils.isEmpty(checkStrategyRequest.strategyIds)) {
      return new CheckStrategyResponse();
    }

    try {
      SDKType sdkType = getSDKType(checkStrategyRequest.sdkType, checkStrategyRequest.extraParams);
      Long build = MapUtils.getLong(checkStrategyRequest.extraParams, "build", -1L);
      //如果注销用户，则直接返回false。
      if (userService.checkUserDeleted(checkStrategyRequest.userId)) {
        log.info("delete user warn, userId:{}", checkStrategyRequest.userId);
        Map<Long, Boolean> result = checkStrategyRequest.strategyIds
            .stream()
            .collect(Collectors.toMap(strategyId -> strategyId, strategyId -> false));
        return CheckStrategyResponse.builder()
            .strategyResult(result)
            .build();
      }
      IDNHomepageLoanStatusV5 idnHomepageLoanStatusV5 = getLoanStatus(MapUtils.getString(checkStrategyRequest.extraParams, "idnHomepageLoanStatusV5"));
      SourceType sourceType = getSourceType(checkStrategyRequest.extraParams);
      RequestClientType requestClientType = getRequestClientType(checkStrategyRequest.extraParams);
      setToScope(checkStrategyRequest.userId, sourceType, requestClientType, build, checkStrategyRequest.deviceToken);
      GeneralPageConfigParam param = GeneralPageConfigParam.from(checkStrategyRequest.userId,
          sdkType,
          build,
          idnHomepageLoanStatusV5,
          checkStrategyRequest.deviceToken,
          sourceType,
          checkStrategyRequest.extraParams);
      ConcurrentHashMap<Long, Long> ruleCostMap = new ConcurrentHashMap<>(checkStrategyRequest.strategyIds.size() * 3);

      ConcurrentHashMap<Long, String> expHitResultMap = new ConcurrentHashMap<>(checkStrategyRequest.strategyIds.size());

      ConcurrentHashMap<Long, Boolean> ruleResultCache = new ConcurrentHashMap<>(checkStrategyRequest.strategyIds.size());
      Map<Long, Boolean> res =
          getCheckStrategyResult(checkStrategyRequest.strategyIds, ruleCostMap, ruleResultCache, param, sourceApp, expHitResultMap);
      if (appResourceConfig.logStrategy()) {
        log.info("param:{}, strategyCostLog:{},strategyRuleResultLog:{},expHitResultMap:{}",
            JsonUtils.toString(param),
            JsonUtils.toString(ruleCostMap),
            JsonUtils.toString(ruleResultCache),
            JsonUtils.toString(expHitResultMap));
      }
      return CheckStrategyResponse.builder()
          .strategyResult(res)
          .expHitResultMap(expHitResultMap)
          .build();
    } catch (Exception e) {
      log.info("checkStrategy warn, strategyIds:{}", checkStrategyRequest.strategyIds, e);
      return new CheckStrategyResponse();
    }
  }

  private static void setToScope(Long userId, SourceType sourceType, RequestClientType requestClientType, Long build, String deviceToken) {
    RequestUtil.setSourceTypeToScope(userId, sourceType);
    RequestUtil.setRequestClientTypeToScope(requestClientType);
    RequestUtil.setBuildToScope(userId, build);
    RequestUtil.setDeviceTokenToScope(deviceToken);
  }

  //sdkType
  // 若为营销中心调用，从sdkTypeStr中取
  // 若为实验中台调用，则从extraParams中取
  private SDKType getSDKType(String sdkTypeStr, Map<String, Object> extraParams) {
    if (StringUtils.isNotEmpty(sdkTypeStr)) {
      return SDKType.valueOf(sdkTypeStr);
    }
    String paramSdkTypeStr = MapUtils.getString(extraParams, "sdkType");
    if (StringUtils.isEmpty(paramSdkTypeStr)) {
      return SDKType.IDN_YQD;
    }
    return SDKType.valueOf(paramSdkTypeStr);
  }

  @NotNull
  private Map<Long, Boolean> getCheckStrategyResult(Set<Long> strategyIds,
                                                    ConcurrentHashMap<Long, Long> ruleCostMap,
                                                    ConcurrentHashMap<Long, Boolean> ruleResultCache,
                                                    GeneralPageConfigParam finalParam, String sourceApp,
                                                    ConcurrentHashMap<Long, String> allExpHitResultMap) {

    List<CompletableFuture<CheckStrategyResponse.CheckStrategyResult>> futureList = strategyIds.stream()
        // 规则实际执行在 checkStrategyExecutor 线程池独立线程上，与请求线程不是同一个线程，请求线程写入的 Scope 读不到。
        // 下游（如千1设备维度实验）会从 ImpliedContextUtils 兜底取 sourceType/build/deviceToken，这里在任务线程统一补写，
        // 与请求线程保持一致。beginScope 前判空避免线程池遗留 Scope 抛异常；仅本任务自己 begin 时才 endScope，避免污染线程池复用。
        .map(strategyId -> CompletableFuture.supplyAsync(() -> {
          long now = Clock.now();
          boolean scopeBegunHere = false;
          try {
            if (Scope.getCurrentScope() == null) {
              Scope.beginScope();
              scopeBegunHere = true;
            }
            setToScope(finalParam.userId, finalParam.sourceType, finalParam.requestClientType, finalParam.build, finalParam.deviceToken);
            boolean res = generalPageConfigFilterStrategyService.hitStrategy(strategyId, finalParam, ruleResultCache);
            return CheckStrategyResult.builder()
                .strategyId(strategyId)
                .res(res)
                .build();
          } catch (Exception e) {
            log.warn("checkStrategy error, strategyId:{}", strategyId, e);
            return null;
          } finally {
            if (scopeBegunHere) {
              Scope.endScope();
            }
            strategyMonitorService.strategyCheckCost(strategyId, Clock.now() - now, sourceApp);
            ruleCostMap.putAll(GeneralPageConfigFilterThreadLocal.getRuleCost());
            Optional.ofNullable(GeneralPageConfigFilterThreadLocal.getExpHitResult()).ifPresent(s -> allExpHitResultMap.put(strategyId, s));
            GeneralPageConfigFilterThreadLocal.remove();
          }
        }, checkStrategyExecutor))
        .collect(Collectors.toList());
    // 等待所有任务完成
    CompletableFuture<Void> allFutures = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));

    // 获取结果并转换为 Map
    return allFutures.thenApply(v -> futureList.stream()
            .map(CompletableFuture::join)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(k -> k.strategyId, v1 -> v1.res)))
        .join();
  }

  private SourceType getSourceType(Map<String, Object> extraParams) {
    String sourceType = MapUtils.getString(extraParams, "sourceType");
    if (StringUtils.isEmpty(sourceType)) {
      return null;
    }
    return SourceType.valueOf(sourceType);
  }

  // ec-internal-api 没有像 ec-api RequestScopeFilter 那样的统一 Filter 自动解析 requestClientType，
  // 需从调用方透传的 extraParams 中显式提取，避免 RequestSourceUtil#isH5WholeProcess() 因 Scope 缺失而恒为 false。
  private RequestClientType getRequestClientType(Map<String, Object> extraParams) {
    String requestClientType = MapUtils.getString(extraParams, "requestClientType");
    return RequestClientType.getRequestClientType(requestClientType);
  }

  private IDNHomepageLoanStatusV5 getLoanStatus(String idnHomepageLoanStatusV5) {
    if (StringUtils.isNotBlank(idnHomepageLoanStatusV5)) {
      return IDNHomepageLoanStatusV5.valueOf(idnHomepageLoanStatusV5);
    }
    return null;
  }
}
