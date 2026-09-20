package com.yqg.core.service.loan.bankaccount;

import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.abtest.enums.ExperimentPlatformGroupType;
import com.yqg.core.userflow.infrastructure.adapter.IExperimentAdapter;
import com.yqg.core.userflow.infrastructure.utils.RequestSourceUtil;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 放款渠道限额提示（TAPD-364191）：判断用户是否命中实验、按渠道限额生成超限提示文案的收口服务。
 *
 * <p>方案不在下单等主链路做硬拦截，只在查询接口（productDetail 下单页账户模块、listPaymentCredentials
 * 放款账户列表）返回超限标识与文案，命中实验的用户由前端将超限渠道置灰不可选。
 *
 * <p>实验分流统一经 {@link IExperimentAdapter} 防腐层，按 {@code userId} 调用
 * {@link IExperimentAdapter#abResultByUserId(String, Long)} 读取分组结果；限额与文案统一取自
 * {@link LoanBankConfig#getPayoutLimitConfigMap()} 的单份 JSON map 配置，运营可按渠道维护。
 *
 * <p>API 渠道 / H5 全流程请求没有原生"置灰不可选"UI 承载能力，两处实验入口（{@link #isLimitInterceptHitByEnroll}
 * 入组 / {@link #isLimitInterceptHitByResult} 查结果）统一按 {@link RequestSourceUtil#isApiChannel()} /
 * {@link RequestSourceUtil#isH5WholeProcess()} 屏蔽，不入组也不下发命中结果。
 *
 * <p>全程 fail-safe：RPC 异常 / 返回空 / 未配置一律按未命中或不置灰处理，绝不阻断查询与主流程。
 */
@Service
@Slf4j
public class PayoutAccountLimitService {

  private static final String FALLBACK_LOG_KEYWORD = "PAYOUT_LIMIT_INTERCEPT_FALLBACK";

  @Autowired
  private LoanBankConfig loanBankConfig;
  @Autowired
  private IExperimentAdapter experimentAdapter;

  /**
   * 仅按渠道限额配置判断当前借款金额是否超限，不涉及实验分流。
   *
   * <p>下单页 productDetail 须先调用本方法确认默认渠道确实超限，再决定是否入组
   * （{@link #isLimitInterceptHitByEnroll}），从而把入组人群收紧到"默认渠道超限"的用户，
   * 避免未超限用户被无意义地计入实验分组。
   *
   * @param bankType 渠道
   * @param amount   借款金额，为空（如历史/未传金额场景）时视为不超限
   */
  public boolean isPayoutLimitExceeded(BankType bankType, BigDecimal amount) {
    return getExceededConfig(bankType, amount, loanBankConfig.getPayoutLimitConfigMap()) != null;
  }

  /**
   * 一次性预取放款渠道限额整表配置，供 listPaymentCredentials 循环前取一次、循环内复用，
   * 避免对每个账户重复反序列化整份 JSON map（配合 {@link #getListLimitHint(BankType, BigDecimal, Map)}）。
   */
  public Map<BankType, PayoutLimitConfigVO> getPayoutLimitConfigMap() {
    return loanBankConfig.getPayoutLimitConfigMap();
  }

  /**
   * 下单页 productDetail 唯一入组点：实时分流并入组（{@code abTestByUserId}），返回是否命中实验组。
   *
   * <p>调用前必须先用 {@link #isPayoutLimitExceeded} 确认默认渠道确实超限，收紧入组人群；
   * 不应对所有访问下单页的用户无条件入组。
   *
   * <p>API 渠道 / H5 全流程场景没有对应的原生"置灰不可选"交互承载能力，直接跳过入组，
   * 避免把这类无法响应置灰效果的请求计入实验分组、稀释实验信噪比（对齐 {@code isApiChannel}/
   * {@code isH5WholeProcess} 在本仓库其它端外场景屏蔽的既有约定，如 {@link com.yqg.core.service.cashloan.LoanUserOrderService}）。
   *
   * <p>fail-safe：userId 为空 / RPC 异常一律按未命中返回 {@code false}，不影响下单页展示。
   */
  public boolean isLimitInterceptHitByEnroll(Long userId) {
    if (userId == null) {
      return false;
    }
    if (isApiChannelOrH5WholeProcess()) {
      return false;
    }
    try {
      String result = experimentAdapter.abTestByUserId(
          UserFlowExperimentEnum.PAYOUT_CHANNEL_LIMIT_INTERCEPT.getKey(), userId);
      return ExperimentPlatformGroupType.EXPERIMENT_GROUP.name().equals(result);
    } catch (Exception e) {
      log.warn("{} isLimitInterceptHitByEnroll fallback to false, userId:{}", FALLBACK_LOG_KEYWORD, userId, e);
      return false;
    }
  }

  /**
   * listPaymentCredentials 放款账户列表：查已入组结果（{@code abResultByUserId}），不重复入组，
   * 与下单页 productDetail 的入组结果保持同组。
   *
   * <p>API 渠道 / H5 全流程场景同样没有原生"置灰不可选"交互承载能力，与 {@link #isLimitInterceptHitByEnroll}
   * 保持一致地跳过，避免这类请求即便曾被计入实验组也照样收到超限文案却无法置灰的错位体验。
   *
   * <p>fail-safe：userId 为空 / RPC 异常一律按未命中返回 {@code false}，不影响列表查询。
   */
  public boolean isLimitInterceptHitByResult(Long userId) {
    if (userId == null) {
      return false;
    }
    if (isApiChannelOrH5WholeProcess()) {
      return false;
    }
    try {
      String result = experimentAdapter.abResultByUserId(
          UserFlowExperimentEnum.PAYOUT_CHANNEL_LIMIT_INTERCEPT.getKey(), userId);
      return ExperimentPlatformGroupType.EXPERIMENT_GROUP.name().equals(result);
    } catch (Exception e) {
      log.warn("{} isLimitInterceptHitByResult fallback to false, userId:{}", FALLBACK_LOG_KEYWORD, userId, e);
      return false;
    }
  }

  /**
   * API 渠道 / H5 全流程请求没有原生"置灰不可选"UI 承载能力，两处实验入口（入组 + 查结果）统一收口于此判断。
   */
  private boolean isApiChannelOrH5WholeProcess() {
    return RequestSourceUtil.isApiChannel() || RequestSourceUtil.isH5WholeProcess();
  }

  /**
   * listPaymentCredentials 放款账户列表（单渠道入口）：内部取一次整表后委托批量版本。
   *
   * <p>返回非空即代表该渠道需置灰不可选，前端据此渲染。文案取自配置的中文模板并经 {@link TT} 翻译。
   *
   * @param bankType 渠道
   * @param amount   借款金额，为空（列表未传金额）时不触发置灰
   */
  public TT getListLimitHint(BankType bankType, BigDecimal amount) {
    return getListLimitHint(bankType, amount, loanBankConfig.getPayoutLimitConfigMap());
  }

  /**
   * listPaymentCredentials 放款账户列表（批量入口）：复用调用方预取的 {@code configMap}，
   * 循环内逐账户判定而不重复反序列化整表；借款金额超出该渠道限额时返回列表长文案，否则返回 {@code null}。
   *
   * @param bankType  渠道
   * @param amount    借款金额，为空（列表未传金额）时不触发置灰
   * @param configMap 调用方预取的渠道限额整表配置
   */
  public TT getListLimitHint(BankType bankType, BigDecimal amount, Map<BankType, PayoutLimitConfigVO> configMap) {
    PayoutLimitConfigVO config = getExceededConfig(bankType, amount, configMap);
    return config == null ? null : genHint(config.listHint, config.maxPayoutAmount);
  }

  /**
   * productDetail 下单页账户模块：借款金额超出该渠道限额时返回短标签文案，否则返回 {@code null}。
   * 单渠道调用，内部自取一次整表后走统一判定逻辑。
   */
  public TT getBadgeLimitHint(BankType bankType, BigDecimal amount) {
    PayoutLimitConfigVO config = getExceededConfig(bankType, amount, loanBankConfig.getPayoutLimitConfigMap());
    return config == null ? null : genHint(config.badgeHint, config.maxPayoutAmount);
  }

  /**
   * 统一判定逻辑：仅当 {@code configMap} 中已配置该渠道限额且 {@code amount} 超过限额时返回配置，否则返回 {@code null}。
   * 单渠道场景内部现取整表、批量场景复用外部整表，二者只是取数时机不同，判定逻辑收口于此。
   */
  private PayoutLimitConfigVO getExceededConfig(BankType bankType, BigDecimal amount,
      Map<BankType, PayoutLimitConfigVO> configMap) {
    if (amount == null || bankType == null || configMap == null) {
      return null;
    }
    PayoutLimitConfigVO config = configMap.get(bankType);
    if (config == null || config.maxPayoutAmount == null) {
      return null;
    }
    return amount.compareTo(config.maxPayoutAmount) > 0 ? config : null;
  }

  /**
   * 用配置的中文文案模板 + 格式化后的限额金额生成可翻译文案；模板为空时返回 {@code null}。
   * 模板中的 {@code {0}} 占位符由 {@link TT} 用格式化金额填充（无占位符时该参数被忽略）。
   */
  private TT genHint(String template, BigDecimal maxPayoutAmount) {
    if (StringUtils.isBlank(template)) {
      return null;
    }
    return TT.gen(template, AmountFormatter.format(EcCurrency.IDR, maxPayoutAmount));
  }
}
