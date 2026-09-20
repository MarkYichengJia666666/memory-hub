package com.yqg.core.userflow.infrastructure.adapter;

import com.yqg.experiment.common.enums.ResultGetType;

/**
 * userflow 实验防腐层：按 expKey + userId / deviceToken 调用实验中台 RPC，
 * 返回经白名单 / fullOrZero 收敛后的实验分组结果字符串。
 */
public interface IExperimentAdapter {

  /**
   * 正式分流并入组，底层调用 {@code abTest}。
   *
   * @return 实验分组结果；参数无效或 RPC 失败时返回空字符串
   */
  String abTestByUserId(String expKey, Long userId);

  /**
   * 正式分流并入组，底层调用 {@code abTest}。
   *
   * @return 实验分组结果；参数无效或 RPC 失败时返回空字符串
   */
  String abTestByDeviceToken(String expKey, String deviceToken);

  /**
   * 查实验结果，底层调用 {@code abResult}，默认 {@link ResultGetType#LAST_RESULT}。
   *
   * @return 实验分组结果；参数无效或 RPC 失败时返回空字符串
   */
  String abResultByUserId(String expKey, Long userId);

  /**
   * 查实验结果，底层调用 {@code abResult}；{@code resultGetType} 为空时默认 {@link ResultGetType#LAST_RESULT}。
   *
   * @return 实验分组结果；参数无效或 RPC 失败时返回空字符串
   */
  String abResultByUserId(String expKey, Long userId, ResultGetType resultGetType);

  /**
   * 查实验结果，底层调用 {@code abResult}（{@link ResultGetType#LAST_RESULT}），并按占比态开关选择返回的分组结果：
   * <ul>
   *   <li>未入组（平台 {@code groupType == null}）→ 返回未入组原因字面量（如 {@code DIVERSION_BLANK_GROUP}），
   *       不因实验全量/关量注入 {@code fullOrZeroGroupResult}</li>
   *   <li>{@code ignoreFullPercentage} 与 {@code ignoreZeroOrClosed} 均为 true → 直接返回上一次分流结果</li>
   *   <li>{@code ignoreFullPercentage} 为 false 且实验已全量 → 返回全量后的组（仅已入组）</li>
   *   <li>{@code ignoreZeroOrClosed} 为 false 且实验已关量或已关闭 → 返回关闭后的组（仅已入组）</li>
   * </ul>
   *
   * @return 实验分组结果或未入组原因；参数无效或 RPC 失败时返回空字符串
   */
  String abResultByUserId(String expKey, Long userId, boolean ignoreFullPercentage, boolean ignoreZeroOrClosed);

  /**
   * 查实验结果，底层调用 {@code abResult}，默认 {@link ResultGetType#LAST_RESULT}。
   *
   * @return 实验分组结果；参数无效或 RPC 失败时返回空字符串
   */
  String abResultByDeviceToken(String expKey, String deviceToken);

  /**
   * 查实验结果，底层调用 {@code abResult}；{@code resultGetType} 为空时默认 {@link ResultGetType#LAST_RESULT}。
   *
   * @return 实验分组结果；参数无效或 RPC 失败时返回空字符串
   */
  String abResultByDeviceToken(String expKey, String deviceToken, ResultGetType resultGetType);
}
