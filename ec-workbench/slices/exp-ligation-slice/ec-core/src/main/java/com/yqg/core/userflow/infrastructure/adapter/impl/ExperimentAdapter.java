package com.yqg.core.userflow.infrastructure.adapter.impl;

import com.yqg.core.service.abtest.ABTestConfig;
import com.yqg.core.service.abtest.ExperimentForceResultResolver;
import com.yqg.core.userflow.infrastructure.adapter.IExperimentAdapter;
import com.yqg.core.userflow.infrastructure.adapter.vo.ExperimentAdapterResultVO;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.experiment.client.service.IExperimentRpcService;
import com.yqg.experiment.common.domain.ExperimentParams;
import com.yqg.experiment.common.domain.ExperimentParamsExtra;
import com.yqg.experiment.common.domain.vo.ExperimentDataVO;
import com.yqg.experiment.common.enums.ResultGetType;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * {@link IExperimentAdapter} 默认实现：直接调用 {@link IExperimentRpcService}。
 */
@Service
@Slf4j
public class ExperimentAdapter implements IExperimentAdapter {

  private static final String EMPTY_RESULT = "";
  private static final String RPC_METHOD_AB_TEST = "abTest";
  private static final String RPC_METHOD_AB_RESULT = "abResult";

  @Autowired
  private IExperimentRpcService experimentRpcService;

  @Autowired
  private ABTestConfig abTestConfig;

  @Autowired
  private ExperimentForceResultResolver experimentForceResultResolver;

  @Override
  public String abTestByUserId(String expKey, Long userId) {
    if (userId == null || StringUtils.isBlank(expKey)) {
      return EMPTY_RESULT;
    }
    return callAbTest(expKey, userId, null);
  }

  @Override
  public String abTestByDeviceToken(String expKey, String deviceToken) {
    if (StringUtils.isBlank(deviceToken) || StringUtils.isBlank(expKey)) {
      return EMPTY_RESULT;
    }
    return callAbTest(expKey, null, deviceToken);
  }

  @Override
  public String abResultByUserId(String expKey, Long userId) {
    return abResultByUserId(expKey, userId, ResultGetType.LAST_RESULT);
  }

  @Override
  public String abResultByDeviceToken(String expKey, String deviceToken) {
    return abResultByDeviceToken(expKey, deviceToken, ResultGetType.LAST_RESULT);
  }

  @Override
  public String abResultByUserId(String expKey, Long userId, ResultGetType resultGetType) {
    if (userId == null || StringUtils.isBlank(expKey) || resultGetType == null) {
      return EMPTY_RESULT;
    }
    return getResult(callAbResult(expKey, userId, null, resultGetType));
  }

  @Override
  public String abResultByUserId(String expKey, Long userId, boolean ignoreFullPercentage, boolean ignoreZeroOrClosed) {

    if (userId == null || StringUtils.isBlank(expKey)) {
      return EMPTY_RESULT;
    }
    return getResult(
        callAbResult(expKey, userId, null, ResultGetType.LAST_RESULT),
        ignoreFullPercentage,
        ignoreZeroOrClosed);
  }

  @Override
  public String abResultByDeviceToken(String expKey, String deviceToken, ResultGetType resultGetType) {
    if (StringUtils.isBlank(deviceToken) || StringUtils.isBlank(expKey) || resultGetType == null) {
      return EMPTY_RESULT;
    }
    return getResult(callAbResult(expKey, null, deviceToken, resultGetType));
  }

  /**
   * 按占比态开关选择返回的分组结果：
   * <ul>
   *   <li>实验白名单 → {@code groupResult}</li>
   *   <li>{@code groupType} 为空（未入组）→ 一律返回 {@code groupResult}
   *       （平台未入组原因，如 {@code DIVERSION_BLANK_GROUP} / {@code EXPERIMENT_NOT_IN}），
   *       <b>不</b>注入 {@code fullOrZeroGroupResult}，避免「泳道空白 + 全量」被误判为实验组</li>
   *   <li>{@code ignoreFullPercentage} 为 false 且实验已全量 → 返回全量后的组（{@code fullOrZeroGroupResult}）</li>
   *   <li>{@code ignoreZeroOrClosed} 为 false 且实验已关量或已关闭 → 返回关闭后的组（{@code fullOrZeroGroupResult}）</li>
   *   <li>其余情况（含两者均为 true）→ 返回上一次分流结果（{@code groupResult}）</li>
   * </ul>
   *
   * @param ignoreFullPercentage 为 true 时忽略"已全量"态，不注入全量组
   * @param ignoreZeroOrClosed   为 true 时忽略"已关量/已关闭"态，不注入关闭组
   */
  String getResult(ExperimentAdapterResultVO result, boolean ignoreFullPercentage, boolean ignoreZeroOrClosed) {
    if (result == null) {
      return EMPTY_RESULT;
    }
    if (result.isExperimentWhiteList() && StringUtils.isNotBlank(result.groupResult)) {
      return result.groupResult;
    }
    // 未入组
    if (result.groupType == null) {
      return StringUtils.defaultString(result.groupResult);
    }
    if (!ignoreFullPercentage && result.isExperimentFullPercentage()) {
      return StringUtils.defaultString(result.fullOrZeroGroupResult);
    }
    if (!ignoreZeroOrClosed && result.isExperimentZeroOrClosed()) {
      return StringUtils.defaultString(result.fullOrZeroGroupResult);
    }
    return StringUtils.defaultString(result.groupResult);
  }

  /**
   * 等价于 {@link #getResult(ExperimentAdapterResultVO, boolean, boolean)
   * getResult(result, false, false)}：感知全量/关量态并注入 {@code fullOrZeroGroupResult}。
   */
  String getResult(ExperimentAdapterResultVO result) {
    return getResult(result, false, false);
  }

  private String callAbTest(String expKey, Long userId, String deviceToken) {
    Optional<String> forced = resolveForcedGroupResult(expKey, userId, deviceToken);
    if (forced.isPresent()) {
      return forced.get();
    }
    ExperimentParams params = buildParams(expKey, userId, deviceToken);
    try {
      ExperimentDataVO vo = experimentRpcService.abTest(params);
      return StringUtils.defaultString(handleRpcSuccess(RPC_METHOD_AB_TEST, expKey, userId, deviceToken, null, vo).groupResult);
    } catch (Exception e) {
      logRpcFailure(RPC_METHOD_AB_TEST, expKey, userId, deviceToken, null, e);
      return EMPTY_RESULT;
    }
  }

  private ExperimentAdapterResultVO callAbResult(
      String expKey, Long userId, String deviceToken, ResultGetType resultGetType) {
    Optional<String> forced = resolveForcedGroupResult(expKey, userId, deviceToken);
    if (forced.isPresent()) {
      ExperimentAdapterResultVO forcedVo = ExperimentAdapterResultVO.empty();
      forcedVo.groupResult = forced.get();
      return forcedVo;
    }
    ExperimentParams params = buildParams(expKey, userId, deviceToken);
    try {
      ExperimentDataVO vo = experimentRpcService.abResult(params, resultGetType);
      return handleRpcSuccess(RPC_METHOD_AB_RESULT, expKey, userId, deviceToken, resultGetType, vo);
    } catch (Exception e) {
      logRpcFailure(RPC_METHOD_AB_RESULT, expKey, userId, deviceToken, resultGetType, e);
      return ExperimentAdapterResultVO.empty();
    }
  }

  private Optional<String> resolveForcedGroupResult(String expKey, Long userId, String deviceToken) {
    String tokenForSuppress = StringUtils.isNotBlank(deviceToken) ? deviceToken : ImpliedContextUtils.deviceToken();
    Long build = ImpliedContextUtils.build();
    if (build == null || build <= 0) {
      log.info("LiveDemo force experiment result build is null or build <= 0, expKey={}", expKey);
    }
    return experimentForceResultResolver.forceGroupResult(
        expKey, userId, tokenForSuppress, build);
  }

  private ExperimentAdapterResultVO handleRpcSuccess(
      String rpcMethod,
      String expKey,
      Long userId,
      String deviceToken,
      ResultGetType resultGetType,
      ExperimentDataVO vo) {
    ExperimentAdapterResultVO mapped = ExperimentAdapterResultVO.from(vo);
    log.info(
        "ExperimentAdapter rpc success, rpcMethod={}, expKey={}, userId={}, deviceToken={}, resultGetType={}, vo={}",
        rpcMethod, expKey, userId, deviceToken, resultGetType, JsonUtils.toString(vo));
    return mapped;
  }

  private void logRpcFailure(
      String rpcMethod,
      String expKey,
      Long userId,
      String deviceToken,
      ResultGetType resultGetType,
      Exception e) {
    log.error(
        "ExperimentAdapter rpc failed, rpcMethod={}, expKey={}, userId={}, deviceToken={}, resultGetType={}",
        rpcMethod, expKey, userId, deviceToken, resultGetType, e);
  }

  private ExperimentParams buildParams(String expKey, Long userId, String deviceToken) {
    ExperimentParamsExtra extra = new ExperimentParamsExtra();
    extra.setDeviceTokenOrigin(deviceToken);
    ExperimentParams experimentParams = new ExperimentParams();
    experimentParams.setToken(abTestConfig.getExperimentPlatformToken());
    experimentParams.setUserId(userId);
    experimentParams.setDeviceToken(StringUtils.isBlank(deviceToken) ? null : DigestUtils.md5Hex(deviceToken));
    experimentParams.setExperimentName(expKey);
    experimentParams.setExtra(extra);
    return experimentParams;
  }
}
