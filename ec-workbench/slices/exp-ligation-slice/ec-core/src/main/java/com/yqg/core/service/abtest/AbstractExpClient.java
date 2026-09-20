package com.yqg.core.service.abtest;

import static com.yqg.core.service.abtest.enums.ExperimentPlatformGroupType.CONTROL_GROUP;
import static com.yqg.core.service.abtest.enums.ExperimentPlatformGroupType.EXPERIMENT_GROUP;
import static com.yqg.core.util.scope.ImpliedContextUtils.build;
import static com.yqg.core.util.scope.ImpliedContextUtils.deviceToken;
import static com.yqg.core.util.scope.ImpliedContextUtils.sourceType;
import static com.yqg.core.util.scope.ImpliedContextUtils.userId;
import static com.yqg.ec.common.exception.EcExceptionType.COMMON_VALIDATE_PARAM_INVALID;

import com.yqg.core.service.abtest.enums.ExperimentPlatformGroupType;
import com.yqg.core.service.abtest.enums.SourceTypeResolveMode;
import com.yqg.core.service.sourcetype.SourceTypeService;
import com.yqg.core.util.scope.Scope;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.experiment.client.service.IExperimentRpcService;
import com.yqg.experiment.common.domain.ExperimentParams;
import com.yqg.experiment.common.domain.ExperimentParamsExtra;
import com.yqg.experiment.common.domain.vo.ExperimentDataVO;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Description 对外暴露的实验平台接口
 * @Author: lihancock
 * @Email: wenyaoli@fintopia.tech
 * @Date: 2025/9/17 11:54
 */
@Slf4j
public abstract class AbstractExpClient {

  public static final String BLANK_GROUP = "BLANK_GROUP";

  @Autowired
  private IExperimentRpcService experimentRpcService;

  @Autowired
  private ABTestConfig abTestConfig;

  @Autowired
  private AbTestMonitorService abTestMonitorService;

  @Autowired
  private SourceTypeService sourceTypeService;

  @Autowired
  private ExperimentForceResultResolver experimentForceResultResolver;

  abstract protected ExperimentDataVO callExperimentPlatform(IExperimentRpcService experimentRpcService, ExperimentParams experimentParams);

  /**
   * 实验组和对照组返回实验平台配置值，其他情况默认返回BLANK_GROUP
   * @param expKey
   * @param expUser
   * @return
   */
  @Nonnull
  public final String getResult(String expKey, ExpUser expUser) {
    try {
      String res = getString(expKey, expUser);
      return res == null ? BLANK_GROUP : res;
    } catch (Exception e) {
      log.error("experiment-error client:{}, expKey:{}, expUser:{}", this.getClass().getSimpleName(), expKey, expUser, e);
      return BLANK_GROUP;
    }
  }

  /**
   * 线程内自动注入用户信息, 目前支持ec-api接口中强登录校验的，线程池中支持首页动态线程池homePageExecutor，未注入要手动构造ExpUser
   * @param expKey 实验key
   * @return 实验结果
   * @throws EcException 这个方法不是完全fail-safe的，避免在非ec-api场景下误用，非ec-api会抛出异常
   */
  @Nonnull
  public final String getResult(String expKey) throws EcException {
    ExpUser expUser = getExpUserFromCurrentThread();
    return getResult(expKey, expUser);
  }

  /**
   * 字符串接口入口：与改造前的语义完全保持一致——任何兜底（参数 / 渠道 / 版本 / 异常 / 实验下线 / 实验平台真命中 BLANK_GROUP / 未识别 groupType）一律返回 {@code null}，由上层 {@link #getResult},
   * {@link #getString(String, ExpUser, String)}, {@link #getBoolean(String, ExpUser, boolean)}, {@link #getInt(String, ExpUser, int)}
   * 各自决定如何兜底。80+ 处现网调用的"BLANK_GROUP→默认值"行为不变。
   *
   * <p><b>实现说明：</b>本方法对 {@link #getRawStringWithBlankMarker(String, ExpUser)} 的输出做"BLANK_GROUP→null"
   * 收敛封装；底层 facade 缓存里仍保留真假可区分的 BLANK_GROUP marker，供 {@link #diversion(String, ExpUser)} 解读。
   */
  protected final String getString(String expKey, ExpUser expUser) {
    String raw = getRawStringWithBlankMarker(expKey, expUser);
    return BLANK_GROUP.equals(raw) ? null : raw;
  }

  /**
   * 内部"原始"分流入口：执行参数/渠道/版本前置兜底 + 实验平台调用 + {@link #processExpRes} 解析， 返回经解析的字面量——含"实验平台真命中 BLANK_GROUP"的固定 marker {@link #BLANK_GROUP}；
   * 任何前置兜底返回 {@code null}。
   *
   * <p><b>用途：</b>{@link #getString(String, ExpUser)} 在出口处把 BLANK_GROUP marker 收敛为 {@code null}（向后兼容），
   * {@link #diversion(String, ExpUser)} 则把 BLANK_GROUP marker 视为 hit（4.0 灌券"空白组也发券"）。两者复用同一份 facade 请求级缓存（cacheKey 一致），既区分真假
   * BLANK_GROUP，又共享缓存。
   */
  @Nullable
  private String getRawStringWithBlankMarker(String expKey, ExpUser expUser) {
    if (StringUtils.isBlank(expKey) || Objects.isNull(expUser)) {
      return null;
    }

    SourceTypeResolveMode mode = abTestConfig.getSourceTypeResolveMode();
    if (mode == SourceTypeResolveMode.ENFORCE) {
      expUser = enrichSourceType(expKey, expUser);
    } else {
      observeSourceTypeDiff(expKey, expUser);
    }

    final ExpUser finalExpUser = expUser;
    String expClientName = this.getClass().getSimpleName();
    // api渠道场景，不在白名单的实验不走实验
    //
    // ⚠️ 警告：此处的 isBlockingMarketingResource() 基于 SourceType.API_CHANNEL_BLOCKING_MARKETING_RESOURCE_LIST，
    //   **仅覆盖 3 个**营销屏蔽源（GOPAY / LAZADA_BUYER / INDOSAT_CL2），不是完整的 API 渠道排除。
    //   若业务规格要求「API 请求不入组」（如 PRD 写"API 不参与"），**禁止仅依赖此门禁**：
    //
    //   正确做法：在调用 expDiversionClient.getResult(...) / expLastResultRunningClient.getResult(...)
    //   **之前**显式调用 com.yqg.core.userflow.infrastructure.utils.RequestSourceUtil.isApiChannel() 短路，
    //   覆盖 SourceType.API_CHANNEL_LIST 全集 8 个 source。
    //
    //   参考：.cursor/rules/ec-abtest-integration.mdc / AuthInfoComplianceExpService（TAPD-355542）
    if (Optional.ofNullable(finalExpUser.getSourceType()).map(SourceType::isBlockingMarketingResource).orElse(false)
        && !abTestConfig.getApiChannelValidExpKeyAllowList().contains(expKey)) {
      log.info("experiment-is-not-called for sourceType, client:{}, expKey:{}, expUser:{}", expClientName, expKey, finalExpUser);
      return null;
    }
    // 实验未配置版本号不走实验
    if (!abTestConfig.getExpKeyValidBuildMap().containsKey(expKey)) {
      log.error("experiment-is-not-called for build not config, client:{}, expKey:{}, expUser:{}", expClientName, expKey, finalExpUser);
      return null;
    }
    // 请求无版本不走实验
    if (Objects.isNull(finalExpUser.getVersionBuild())) {
      log.error("experiment-is-not-called for request no build, client:{}, expKey:{}, expUser:{}", expClientName, expKey, finalExpUser);
      return null;
    }
    // 版本不在实验配置区间内不走实验
    if (!abTestConfig.getExpKeyValidBuildMap().get(expKey).contains(finalExpUser.getVersionBuild())) {
      log.info("experiment-is-not-called for build not match, client:{}, expKey:{}, expUser:{}", expClientName, expKey, finalExpUser);
      return null;
    }

    // LiveDemo：强制优先于 build/渠道门禁，直接返回配置 groupResult，不调实验平台
    Optional<String> forced = experimentForceResultResolver.forceGroupResult(
        expKey, expUser.getUserId(), expUser.getDeviceToken(), expUser.getVersionBuild());
    if (forced.isPresent()) {
      log.info("experiment-force LiveDemo client:{}, expKey:{}, expUser:{}, result:{}",
          this.getClass().getSimpleName(), expKey, expUser, forced.get());
      return forced.get();
    }
    // 将缓存 key 构建、跨客户端调用监控、干跑/读缓存分支统一委托给 AbTestRequestCacheFacade
    return AbTestRequestCacheFacade.execute(expClientName, expKey, finalExpUser, abTestConfig.isUseRequestCacheForAbtest(), () -> {
      ExperimentParams experimentParams = doBuildParam(expKey, finalExpUser);
      ExperimentDataVO experimentDataVO = callExperimentPlatform(experimentRpcService, experimentParams);
      log.info("experiment-result client:{}, expKey:{}, expUser:{}, result:{}", expClientName, expKey, finalExpUser, experimentDataVO);
      return processExpRes(finalExpUser, experimentDataVO);
    }, abTestMonitorService);
  }

  /**
   * 结构化分流入口：在不影响 {@link #getString(String, ExpUser)} / {@link #getResult(String, ExpUser)} 等
   * 字符串接口语义的前提下，让调用方区分"真命中分组"与"兜底"两种语义。
   *
   * <p><b>实现策略：</b>与 {@code getString} 共享 {@link #getRawStringWithBlankMarker(String, ExpUser)}：
   * 同一份 facade 请求级缓存（cacheKey 一致）下，{@code getString} 在出口把 BLANK_GROUP marker 收敛为 {@code null}
   * 维持向后兼容；本方法不做收敛，把 marker 视为 hit({@code "BLANK_GROUP"}) 让 4.0 灌券"空白组也发券"等场景正确发券。
   * {@code null} 来自前置 5 道兜底（参数 / 渠道 / 版本配置 / 上送版本 / 版本范围）或实验下线 / 未命中分流 / 异常，
   * 一律收敛为 fallback。
   *
   * <p><b>注意：</b>本方法不区分具体的兜底原因（参数非法 / 渠道阻断 / 版本错配 / 实验下线 / 异常），调用方对所有兜底
   * 路径采取统一动作（通常是不发券 / 走默认分支）。如需区分原因，请扩展专用方法，避免侵蚀本方法。
   *
   * @param expKey  实验 key
   * @param expUser 参与分流的用户信息
   * @return 不可变的分流决策；任何兜底或异常路径都收敛为 {@code isFallback()=true}，绝不返回 null
   */
  @Nonnull
  public final ExpDiversionDecision diversion(String expKey, ExpUser expUser) {
    try {
      String raw = getRawStringWithBlankMarker(expKey, expUser);
      return raw == null ? ExpDiversionDecision.fallback() : ExpDiversionDecision.hit(raw);
    } catch (Exception e) {
      log.error("[exp-diversion] failed, client:{}, expKey:{}, expUser:{}",
          this.getClass().getSimpleName(), expKey, expUser, e);
      return ExpDiversionDecision.fallback();
    }
  }

  /**
   * 构建请求实验平台的参数
   * @param expKey 实验key
   * @param expUser 参与分流的用户信息
   * @return 请求参数
   */
  @NotNull
  private ExperimentParams doBuildParam(String expKey, ExpUser expUser) {
    ExperimentParamsExtra extra = new ExperimentParamsExtra();
    extra.setDeviceTokenOrigin(expUser.getDeviceToken());
    ExperimentParams experimentParams = new ExperimentParams();
    experimentParams.setToken(abTestConfig.getExperimentPlatformToken());
    experimentParams.setUserId(expUser.getUserId());
    experimentParams.setDeviceToken(StringUtils.isBlank(expUser.getDeviceToken()) ? null : DigestUtils.md5Hex(expUser.getDeviceToken()));
    experimentParams.setIdNumberMD5(StringUtils.isBlank(expUser.getNik()) ? null : DigestUtils.md5Hex(expUser.getNik()));
    experimentParams.setExperimentName(expKey);
    experimentParams.setExtra(extra);
    return experimentParams;
  }

  /**
   * 解析实验平台返回值为缓存层字符串（含真假可区分的 BLANK_GROUP marker）：
   * <ul>
   *   <li>{@code groupType ∈ {EXPERIMENT_GROUP, CONTROL_GROUP}}：实验组优先取 {@code fullOrZeroGroupResult}，否则 {@code groupResult}</li>
   *   <li>{@code groupType == BLANK_GROUP}：返回固定字面量 {@link #BLANK_GROUP} marker（不取 {@code groupResult}），
   *       用于 {@link #diversion(String, ExpUser)} 区分"真命中 BLANK"与"前置兜底"；
   *       {@link #getString(String, ExpUser)} 在出口处再把该 marker 收敛为 {@code null} 以保证向后兼容</li>
   *   <li>其他（未识别 groupType / null vo）：返回 {@code null}</li>
   * </ul>
   *
   * <p><b>为何 BLANK_GROUP 不取 {@code groupResult}：</b>实验平台 BLANK_GROUP 的 {@code groupResult}
   * 默认就是字面量 {@code "BLANK_GROUP"}，但若运营在配置侧给 BLANK_GROUP 设置了非默认 groupResult，
   * 缓存值会偏离 marker，导致出口处 BLANK_GROUP→null 的收敛失效，进而破坏现网 80+ 处 {@code getString(..., default)}
   * 的"BLANK_GROUP→默认值"行为。固定字面量 marker 保证 BLANK_GROUP 命中在缓存层有唯一可识别的取值。
   */
  @Nullable
  private String processExpResOld(ExperimentDataVO experimentDataVO) {
    if (experimentDataVO == null || experimentDataVO.groupType == null) {
      return null;
    }
    // groupType不为null，一定是进入了实验的分流
    ExperimentPlatformGroupType groupType = ExperimentPlatformGroupType.valueOfOrNull(experimentDataVO.groupType.name());
    if (groupType == EXPERIMENT_GROUP || groupType == CONTROL_GROUP) {
      String fullOrZero = experimentDataVO.fullOrZeroGroupResult;
      String groupResult = experimentDataVO.groupResult;
      return StringUtils.isBlank(fullOrZero) ? groupResult : fullOrZero;
    }
    if (groupType == ExperimentPlatformGroupType.BLANK_GROUP) {
      return BLANK_GROUP;
    }
    return null;
  }

  @Nullable
  private String dryRunProcessExpRes(ExperimentDataVO experimentDataVO) {
    if (Objects.isNull(experimentDataVO)) {
      return null;
    }
    ExperimentPlatformGroupType groupType = Optional.ofNullable(experimentDataVO.getGroupType())
        .map(Enum::name).map(ExperimentPlatformGroupType::valueOfOrNull).orElse(null);
    if (groupType == ExperimentPlatformGroupType.EXPERIMENT_GROUP
        || groupType == ExperimentPlatformGroupType.CONTROL_GROUP) {
      return experimentDataVO.getGroupResult();
    }
    if (groupType == ExperimentPlatformGroupType.BLANK_GROUP) {
      return BLANK_GROUP;
    }
    return null;
  }

  private String processExpRes(ExpUser expUser, ExperimentDataVO experimentDataVO) {
    String oldResult = processExpResOld(experimentDataVO);
    String newResult = dryRunProcessExpRes(experimentDataVO);
    if (!StringUtils.equals(oldResult, newResult)) {
      log.info("dry-run processExpRes results are not equal expUser:{}, oldResult:{}, newResult:{}, experimentDataVO:{}, client:{}",
          expUser, oldResult, newResult, experimentDataVO, this.getClass().getSimpleName());
    }
    return abTestConfig.isUseNewParsingResult() ? newResult : oldResult;
  }

  /**
   * 线程内自动注入用户信息, 目前支持ec-api接口中强登录校验的，线程池中支持首页动态线程池homePageExecutor，未注入要手动构造ExpUser
   * @param expKey 实验key
   * @param defaultValue 获取实验默认值
   * @return 实验结果
   * @throws EcException 这个方法不是完全fail-safe的，避免在非ec-api场景下误用，非ec-api会抛出异常
   */
  public final boolean getBoolean(String expKey, boolean defaultValue) throws EcException {
    ExpUser expUser = getExpUserFromCurrentThread();
    return getBoolean(expKey, expUser, defaultValue);
  }

  public final boolean getBoolean(String expKey, ExpUser expUser, boolean defaultValue) {
    Boolean booleanOrNull = getBooleanOrNull(expKey, expUser, defaultValue);
    return booleanOrNull == null ? defaultValue : booleanOrNull;
  }

  public final Boolean getBooleanOrNull(String expKey, ExpUser expUser, Boolean defaultValue) {
    try {
      String res = getString(expKey, expUser);
      return res == null ? null : Boolean.parseBoolean(res);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * 线程内自动注入用户信息, 目前支持ec-api接口中强登录校验的，线程池中支持首页动态线程池homePageExecutor，未注入要手动构造ExpUser
   * @param expKey 实验key
   * @param defaultValue 获取实验默认值
   * @return 实验结果
   * @throws EcException 这个方法不是完全fail-safe的，避免在非ec-api场景下误用，非ec-api会抛出异常
   */
  public final int getInt(String expKey, int defaultValue) throws EcException {
    ExpUser expUser = getExpUserFromCurrentThread();
    return getInt(expKey, expUser, defaultValue);
  }

  public final int getInt(String expKey, ExpUser expUser, int defaultValue) {
    try {
      String res = getString(expKey, expUser);
      return res == null ? defaultValue : Integer.parseInt(res);
    } catch (Exception e) {
      return defaultValue;
    }
  }

  /**
   * 线程内自动注入用户信息, 目前支持ec-api接口中强登录校验的，线程池中支持首页动态线程池homePageExecutor，未注入要手动构造ExpUser
   * @param expKey 实验key
   * @param defaultValue 获取实验默认值
   * @return 实验结果
   * @throws EcException 这个方法不是完全fail-safe的，避免在非ec-api场景下误用，非ec-api会抛出异常
   */
  public final String getString(String expKey, String defaultValue) throws EcException {
    ExpUser expUser = getExpUserFromCurrentThread();
    return getString(expKey, expUser, defaultValue);
  }

  protected final ExpUser getExpUserFromCurrentThread() {
    if (Objects.isNull(Scope.getCurrentScope())) {
      throw EcException.error(COMMON_VALIDATE_PARAM_INVALID, "experiment Scope.getCurrentScope() is null!");
    }
    return ExpUser.builder().userId(userId()).deviceToken(deviceToken()).sourceType(sourceType()).versionBuild(build()).build();
  }

  public final String getString(String expKey, ExpUser expUser, String defaultValue) {
    try {
      String res = getString(expKey, expUser);
      return res == null ? defaultValue : res;
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private ExpUser enrichSourceType(String expKey, ExpUser expUser) {
    try {
      SourceType resolved = sourceTypeService.resolveSourceType(expUser.getUserId(), expUser.getSourceType());
      if (resolved != null && resolved != expUser.getSourceType()) {
        return expUser.toBuilder().sourceType(resolved).build();
      }
    } catch (Exception e) {
      log.warn("Failed to resolve sourceType in ENFORCE mode, expKey:{}, userId:{}", expKey, expUser.getUserId(), e);
    }
    return expUser;
  }

  private void observeSourceTypeDiff(String expKey, ExpUser expUser) {
    try {
      SourceType resolved = sourceTypeService.resolveSourceType(expUser.getUserId(), expUser.getSourceType());
      if (!Objects.equals(resolved, expUser.getSourceType())) {
        log.info("sourceType-diff client:{}, expKey:{}, userId:{}, oldSourceType:{}, newSourceType:{}",
            this.getClass().getSimpleName(), expKey, expUser.getUserId(), expUser.getSourceType(), resolved);
      }
    } catch (Exception e) {
      log.warn("Failed to resolve sourceType in SHADOW mode, expKey:{}, userId:{}", expKey, expUser.getUserId(), e);
    }
  }
}
