package com.yqg.core.service.loan.repayment.account;

import static com.yqg.core.util.scope.ImpliedContextUtils.build;
import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.loader.RepaymentChannelAccountRouteConfigLoader;
import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountCallerType;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountDisplayConfig;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.userflow.infrastructure.utils.RequestSourceUtil;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.overseas.client.spring.api.payment.IOverseasRepaymentRouteService;
import com.yqg.overseas.spring.response.receipt.RepaymentChannelRouteResponse;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 支付渠道配置获取与 Provider 选择服务 负责：1. 获取渠道配置  2. 根据业务规则选择合适的 PaymentProvider
 */
@Slf4j
@Service
public class PaymentProviderSelectorService {

  private static final long CHANNEL_DISPLAY_LEGACY_BUILD_THRESHOLD = 37913L;

  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  private IOverseasRepaymentRouteService repaymentRouteService;
  @Autowired
  private RepaymentChannelAccountRouteConfigLoader routeConfigLoader;
  @Autowired
  private ExpDiversionClient expDiversionClient;

  /**
   * 获取所有渠道的配置 Map
   */
  public Map<String, RepaymentAccountDisplayConfig> getChannelDisplayMap(PaymentAccount paymentAccount, Long userId) {
    boolean isNewRouteSwitchEnabled = repaymentAccountConfig.getRepaymentAccountChannelNewRouteSwitch();
    boolean isWhitelistedUser = repaymentAccountConfig.getWhitelistUserIdForRepaymengConfig().contains(userId);
    boolean isNullUserAndNewRouteSwitch = Objects.isNull(userId) && isNewRouteSwitchEnabled;

    if (isNullUserAndNewRouteSwitch || isWhitelistedUser || isNewRouteSwitchEnabled) {
      return getRouteFromNewRouteNew(PayEventType.convertPayAccountToPayEventType(paymentAccount));
    }

    // 印尼账户优先走配置映射,白名单配置
    if (paymentAccount == PaymentAccount.IDN) {
      Map<String, RepaymentAccountDisplayConfig> channelConfig = repaymentAccountConfig.getPaymentProviderByChannel(userId);
      if (MapUtils.isNotEmpty(channelConfig)) {
        return channelConfig;
      }
    }

    // 非H5、API渠道、版本低的用户后续不迭代更新，固定一个版本
    Long clientBuild = build();
    if ((clientBuild != null && clientBuild < CHANNEL_DISPLAY_LEGACY_BUILD_THRESHOLD) || RequestSourceUtil.isApiChannel()
        || RequestSourceUtil.isH5WholeProcess()) {
      return repaymentAccountConfig.getChannelDisplayMapForApiH5LowBuild(paymentAccount);
    }
    return repaymentAccountConfig.getChannelDisplayMapByAccount(paymentAccount);
  }

  /**
   * 获取指定渠道的配置，并根据调用来源决定是否应用 Provider 分流选择逻辑
   *
   * @param channel    支付渠道
   * @param userId     用户ID
   * @param account    支付账户
   * @param callerType 调用来源类型，决定是否应用分流逻辑
   * @return 最终选择的 RepaymentAccountDisplayConfig（可能修改了 provider）
   * @throws EcException 当渠道不支持时
   */
  public RepaymentAccountDisplayConfig selectProvider(String channel, Long userId, PaymentAccount account,
      RepaymentAccountCallerType callerType) {
    // 1. 获取渠道配置 Map
    Map<String, RepaymentAccountDisplayConfig> channelConfigMap = getChannelDisplayMap(account, userId);

    // 2. 获取并校验指定渠道配置
    RepaymentAccountDisplayConfig config = channelConfigMap.get(channel);
    if (config == null) {
      throw EcException.error("unsupported channel: " + channel + ", available: " + channelConfigMap.keySet());
    }

    // 3. 根据调用来源决定是否应用 Provider 分流选择逻辑
    if (callerType == null || !callerType.shouldApplyProviderSelector()) {
      return config;
    }

    // 4. 应用 Provider 选择逻辑
    PaymentProvider selectedProvider = selectProviderByChannel(channel, userId, config.provider);
    if (selectedProvider != null && selectedProvider != config.provider) {
      return RepaymentAccountDisplayConfig.copy(config, selectedProvider);
    }
    return config;
  }

  /**
   * 根据渠道选择 Provider 当前支持 BNI 渠道的分流逻辑，后续其他渠道的分流可在此扩展
   */
  private PaymentProvider selectProviderByChannel(String channel, Long userId, PaymentProvider defaultProvider) {
    if (Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)) {
      return defaultProvider;
    }
    switch (channel) {
      case "BNI":
        return selectBNIProvider(userId, defaultProvider);
      default:
        return defaultProvider;
    }
  }

  /**
   * BNI 渠道的 Provider 选择逻辑 根据用户是否逾期走不同的分流实验
   */
  private PaymentProvider selectBNIProvider(Long userId, PaymentProvider defaultProvider) {
    String result = expDiversionClient.getString("bill_normal_26h1-before_overdue-abroad-loan_all-Midtrans_BNI_normal_0112_V1", "A");
    if ("B".equals(result)) {
      return PaymentProvider.IRIS;
    }
    return defaultProvider;
  }

  public Map<String, RepaymentAccountDisplayConfig> getRouteFromNewRouteNew(PayEventType payEventType) {
    String configRouteConfigStr = routeConfigLoader.get(payEventType);
    if (StringUtils.isNotBlank(configRouteConfigStr)) {
      return RepaymentAccountConfig.copyChannelDisplayMap(
          JsonUtils.fromOrException(configRouteConfigStr, new TypeReference<Map<String, RepaymentAccountDisplayConfig>>() {
          }));
    }
    Map<String, RepaymentAccountDisplayConfig> channelToRouteConfigMap = repaymentRouteService.getRepaymentChannelRoute(
            payEventType.name()).stream()
        .collect(Collectors.toMap(RepaymentChannelRouteResponse::getChannel, RepaymentAccountDisplayConfig::from));
    routeConfigLoader.set(payEventType, JsonUtils.toString(channelToRouteConfigMap));
    return RepaymentAccountConfig.copyChannelDisplayMap(channelToRouteConfigMap);
  }
}

