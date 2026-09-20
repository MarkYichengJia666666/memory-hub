package com.yqg.core.service.loan.repayment.account;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentType;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountDisplayConfig;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.pm.pmenum.VirtualAccountChannel;
import com.yqg.ec.common.configuration.Conf;
import com.yqg.ec.common.configuration.ISiteVars;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.serialization.JsonUtils;
import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by wenbincao on 20/03/16.
 */
@Component
public class RepaymentAccountConfig {
  @Autowired
  private ISiteVars ecSiteVars;

  private static final String CHANNEL_PROVIDERS_DISPLAY_MAP = "repayment_account.channel_providers_display_map_byAccount";
  /** 低版本 API / H5 全流程请求使用的渠道展示配置，按 PaymentAccount.code 分账户，结构与 channel_providers_display_map_byAccount 一致 */
  private static final String CHANNEL_PROVIDERS_DISPLAY_MAP_API_H5_LOW_BUILD = "repayment_account.channel_providers_display_map_api_h5_low_build";
  private static final String KEY_METHOD_PREFIX = "repayment_account.methodPrefixByAccount";
  private static final String ENABLE_COPY = "repayment_account.enable_copy";
  private static final String ENABLE_COPY_BUILD = "repayment_account.enable_copy_build";
  private static final String REPAYMENT_ANNOUNCEMENT_SEND_LIMIT = "cash_loan.repayment_account_send_limit";
  private static final String REPAYMENT_ANNOUNCEMENT_TEXT = "repayment_account.repayment_announcement_text";
  private static final String REPAYMENT_CHANNEL_DISPLAY_RULE = "repayment_account.channel_display_rule";
  private static final String JBP_REPAYMENT_CHANNEL_DISPLAY_RULE = "repayment_account.jbp_channel_display_rule";
  private static final String JBP_REPAYMENT_CHANNEL_DISPLAY_RULE_WITH_NEW_EWALLET_CHANNEL = "repayment_account.jbp_channel_display_rule_with_new_ewallet_channel";
  private static final String JBP_DEFAULT_CHANNEL = "repayment_account.jbp_default_channel";

  private static final String REPAYMENT_CHANNEL_TYPE_PERCENT_MAP = "repayment_account.repayment_channel_type_percent_map";

  private static final String CASH_PAYMENT_LOGO_MAP = "repayment_account.cash_payment_logo_map";

  private static final String REPAYMENT_CHANNEL_SUB_CHANNEL_EXTRA_LOGO_MAP = "repayment_account.sub_channel_extra_logo_map";

  private static final String REPAYMENT_CHANNEL_TYPE_PERCENT_MAP_V1 = "repayment_account.repayment_channel_type_percent_map_v1";

  private static final String REPAYMENT_DANA_STATIC_VA_PAYMENT_PROVIDER = "repayment_account.dana_static_va_payment_provider";
  private static final String REPAYMENT_DANA_STATIC_VA_CHANNEL = "repayment_account.dana_static_va_channel";
  private static final String REPAYMENT_DANA_DYNAMIC_EXPIRY_TIME = "repayment_account.dana_dynamic_va_expiry_time";

  private static final String REPAYMENT_CHANNEL_TO_REPAYMENT_RELATED_INFO_INTERNAL_OPEN_MAP = "repayment_account.repayment_channel_to_related_info_internal_open_map";
  private static final String REPAYMENT_XENDIT_DYNAMIC_ACCOUNT_WHITELIST_USER_ID_LIST_TRIAL = "xendit.dynamic_account_whitelist_user_id_list_trial";
  private static final String WHITELIST_USER_ID_REPAYMENT_CONFIG_TRIAL = "repayment_account.whitelist_user_id_list_trial";

  private static final String EXCLUDED_REPAYMENT_CHANNELS_FOR_INTERNAL_API = "repayment_account.excluded_repayment_channels_for_internal_api";
  private static final String REPAYMENT_ACCOUNT_CHANNEL_NEW_ROUTE_SWITCH = "repayment_account.channel_new_route_switch";
  private static final String REPAYMENT_CHANNEL_DEEP_LINK_MAP = "repayment_account.repayment_channel_deep_link_map";
  private static final String REPAYMENT_CHANNEL_PROVIDER_WHITE_CONFIG_V2 = "repayment_account.repayment_channel_provider_white_config_v2";
  private static final String REPAYMENT_CHANNEL_H5_PAGE_URL = "repayment_account.repayment_channel_h5_page_url";
  private static final String REPAYMENT_CHANNEL_H5_PAGE_URL_FOR_WHOLE_PROCESS = "repayment_account.repayment_channel_h5_page_url_for_whole_process";
  private static final String REPAYMENT_CHANNEL_H5_PAGE_URL_MAP_BY_SOURCE_TYPE = "repayment_account.repayment_channel_h5_page_url_map_by_source_type";
  private static final String REPAYMENT_CHANNEL_SAMPLE_LIMIT = "repayment_account.repayment_channel_sample_limit";
  private static final String REPAYMENT_CHANNEL_MIN_REPAYMENT_AMOUNT = "repayment_account.repayment_channel_min_repayment_amount";
  private static final String BILL_PAGE_H5_EXP_URL = "repayment_account.bill_page_h5_exp_url";
  private static final String REPAYMENT_CHANNEL_FOR_API_CHANNEL = "repayment_account.channels_for_api_channel";

  public static final String UNION_REPAYMENT_BUILD = "repayment_account.union_repayment_build";
  public static final String UNION_REPAYMENT_WEIGHT_MAP = "repayment_account.union_repayment_weight_map";
  //获取VA和BCA金额绑定重构降级开关
  private static final String REPAYMENT_ACCOUNT_REFACTOR_SWITCH = "repayment_account.repayment_account_refactor_switch";
  //alfmart是否生效
  private static final String ALF_MART_SWITCH = "repayment_account.alf_mart_switch";
  private static final String REPAYMENT_CHANNEL_SHOW_BADGE = "repayment_account.repayment_channel_show_badge";
  private static final String OTHER_BANK_TARGET_CHANNEL = "repayment_account.other_bank_target_channel";

  @PostConstruct
  private void init() {
    ecSiteVars.registerConf(CHANNEL_PROVIDERS_DISPLAY_MAP, new Conf<Map<String, RepaymentAccountDisplayConfig>>() {
    });
    ecSiteVars.registerConf(CHANNEL_PROVIDERS_DISPLAY_MAP_API_H5_LOW_BUILD, new Conf<Map<String, RepaymentAccountDisplayConfig>>() {
    });
    ecSiteVars.registerConf(KEY_METHOD_PREFIX, new Conf<String>() {
    });
    ecSiteVars.registerConf(ENABLE_COPY, new Conf<Boolean>() {
    });
    ecSiteVars.registerConf(REPAYMENT_ANNOUNCEMENT_TEXT, new Conf<String>() {
    });
    ecSiteVars.registerConf(REPAYMENT_CHANNEL_TYPE_PERCENT_MAP, new Conf<String>() {
    });
    ecSiteVars.registerConf(CASH_PAYMENT_LOGO_MAP, new Conf<String>() {
    });
    ecSiteVars.registerConf(REPAYMENT_CHANNEL_SUB_CHANNEL_EXTRA_LOGO_MAP, new Conf<String>() {
    });
    ecSiteVars.registerConf(REPAYMENT_CHANNEL_TO_REPAYMENT_RELATED_INFO_INTERNAL_OPEN_MAP, new Conf<Boolean>() {
    });
    ecSiteVars.registerConf(REPAYMENT_CHANNEL_DEEP_LINK_MAP, new Conf<String>() {
    });
    ecSiteVars.registerConf(REPAYMENT_CHANNEL_PROVIDER_WHITE_CONFIG_V2, new Conf<Map<String, RepaymentAccountDisplayConfig>>() {
    });
    ecSiteVars.registerConf(UNION_REPAYMENT_WEIGHT_MAP, new Conf<Integer>() {
    });
  }

  /**
   * 返回渠道展示配置的可变副本，避免调用方 remove 污染 Conf 内存缓存。
   */
  static Map<String, RepaymentAccountDisplayConfig> copyChannelDisplayMap(
      Map<String, RepaymentAccountDisplayConfig> source) {
    return source == null ? Maps.newHashMap() : Maps.newHashMap(source);
  }

  public Map<String, RepaymentAccountDisplayConfig> getChannelDisplayMapByAccount(PaymentAccount paymentAccount) {
    return copyChannelDisplayMap(ecSiteVars.getConfVal(CHANNEL_PROVIDERS_DISPLAY_MAP, paymentAccount.code));
  }

  public Map<String, RepaymentAccountDisplayConfig> getChannelDisplayMapForApiH5LowBuild(PaymentAccount paymentAccount) {
    return copyChannelDisplayMap(ecSiteVars.getConfVal(CHANNEL_PROVIDERS_DISPLAY_MAP_API_H5_LOW_BUILD, paymentAccount.code));
  }

  public String getKeyMethodPrefix(PaymentAccount paymentAccount) {
    return ecSiteVars.getConfVal(KEY_METHOD_PREFIX, paymentAccount.code);
  }

  public Boolean getEnableCopyWithBuild(String channel, Long build) {
    if (build < ecSiteVars.getLong(ENABLE_COPY_BUILD, 31900L) && channel.equals(VirtualAccountChannel.BRI.name())) {
      return false;
    }
    return ecSiteVars.getConfVal(ENABLE_COPY, channel, true);
  }

  public int getRepaymentAccountLimitOneDay() {
    return ecSiteVars.getInt(REPAYMENT_ANNOUNCEMENT_SEND_LIMIT);
  }

  public String getRepaymentAnnouncementText(String channelType) {
    return ecSiteVars.getConfVal(REPAYMENT_ANNOUNCEMENT_TEXT, channelType, "Dear user, We recommend you to make a payment of {0} pesos, by visiting: {1}, payment code：{2}");
  }

  public String getDanaStaticVAPaymentProvider() {
    return ecSiteVars.getString(REPAYMENT_DANA_STATIC_VA_PAYMENT_PROVIDER, "INSTAMONEY");
  }

  public String getDanaStaticVAChannel() {
    return ecSiteVars.getString(REPAYMENT_DANA_STATIC_VA_CHANNEL, "BNI");
  }

  public int getExpiryTimeDanaDynamicVA() {
    return ecSiteVars.getInt(REPAYMENT_DANA_DYNAMIC_EXPIRY_TIME, 20);
  }

  public Map<String, Integer> getRepaymentChannelDisplayRule() {
    String displayStr = ecSiteVars.getString(REPAYMENT_CHANNEL_DISPLAY_RULE, "");
    Map<String, Integer> ruleMap = new HashMap<>();
    if (StringUtils.isBlank(displayStr)) {
      return ruleMap;
    }
    List<String> displayRule = Arrays.stream(displayStr.split(",")).collect(Collectors.toList());
    for (int i = 0; i < displayRule.size(); i++) {
      ruleMap.put(displayRule.get(i), i);
    }
    return ruleMap;
  }

  public Map<String, Integer> getJbpRepaymentChannelDisplayRule() {
    String displayStr = ecSiteVars.getString(JBP_REPAYMENT_CHANNEL_DISPLAY_RULE, "");
    Map<String, Integer> ruleMap = new HashMap<>();
    if (StringUtils.isBlank(displayStr)) {
      return ruleMap;
    }
    List<String> displayRule = Arrays.stream(displayStr.split(",")).collect(Collectors.toList());
    for (int i = 0; i < displayRule.size(); i++) {
      ruleMap.put(displayRule.get(i), i);
    }
    return ruleMap;
  }

  public Map<String, Integer> getJbpRepaymentChannelDisplayRuleWithNewEwalletChannel() {
    String displayStr = ecSiteVars.getString(JBP_REPAYMENT_CHANNEL_DISPLAY_RULE_WITH_NEW_EWALLET_CHANNEL, "");
    Map<String, Integer> ruleMap = new HashMap<>();
    if (StringUtils.isBlank(displayStr)) {
      return ruleMap;
    }
    List<String> displayRule = Arrays.stream(displayStr.split(",")).collect(Collectors.toList());
    for (int i = 0; i < displayRule.size(); i++) {
      ruleMap.put(displayRule.get(i), i);
    }
    return ruleMap;
  }


  public List<String> getExcludedRepaymentChannelsForInternalApi() {
    String list = ecSiteVars.getString(EXCLUDED_REPAYMENT_CHANNELS_FOR_INTERNAL_API, "[]");
    return JsonUtils.fromOrException(list, new TypeReference<List<String>>() {
    });
  }

  public Boolean getRepaymentRelatedInfoInternalOpenByChannel(String channel) {
    return ecSiteVars.getConfVal(REPAYMENT_CHANNEL_TO_REPAYMENT_RELATED_INFO_INTERNAL_OPEN_MAP, channel, false);
  }

  public Boolean getRepaymentAccountChannelNewRouteSwitch() {
    return ecSiteVars.getBoolean(REPAYMENT_ACCOUNT_CHANNEL_NEW_ROUTE_SWITCH, false);
  }

  public String getRepaymentChannelDeepLink(String channel) {
    return ecSiteVars.getConfVal(REPAYMENT_CHANNEL_DEEP_LINK_MAP, channel, "");
  }

  public List<Long> getRepaymentXenditDynamicAccountWhitelistUserIdList() {
    String list = ecSiteVars.getString(REPAYMENT_XENDIT_DYNAMIC_ACCOUNT_WHITELIST_USER_ID_LIST_TRIAL, "[]");
    return JsonUtils.fromOrException(list, new TypeReference<List<Long>>() {
    });
  }

  public List<Long> getWhitelistUserIdForRepaymengConfig() {
    String list = ecSiteVars.getString(WHITELIST_USER_ID_REPAYMENT_CONFIG_TRIAL, "[]");
    return JsonUtils.fromOrException(list, new TypeReference<List<Long>>() {
    });
  }

  public Map<String, RepaymentAccountDisplayConfig> getPaymentProviderByChannel(Long userId) {
    return copyChannelDisplayMap(ecSiteVars.getConfVal(REPAYMENT_CHANNEL_PROVIDER_WHITE_CONFIG_V2, userId.toString(), Maps.newHashMap()));
  }

  public String getRepaymentChannelH5PageUrl() {
    return ecSiteVars.getString(REPAYMENT_CHANNEL_H5_PAGE_URL, null);
  }

  public String getRepaymentChannelH5PageUrlForWholeProcess() {
    return ecSiteVars.getString(REPAYMENT_CHANNEL_H5_PAGE_URL_FOR_WHOLE_PROCESS, null);
  }

  public String getBillPageToH5ExpUrl() {
    return ecSiteVars.getString(BILL_PAGE_H5_EXP_URL, null);
  }

  public int getRepaymentChannelSampleLimit() {
    return ecSiteVars.getInt(REPAYMENT_CHANNEL_SAMPLE_LIMIT, 10);
  }

  public BigDecimal getRepaymentChannelMinRepaymentAmount() {
    return ecSiteVars.getBigDecimal(REPAYMENT_CHANNEL_MIN_REPAYMENT_AMOUNT, BigDecimal.valueOf(10_0000L));
  }

  public Long getUnionRepaymentBuild() {
    return ecSiteVars.getLong(UNION_REPAYMENT_BUILD, 37400L);
  }

  public Integer getUnionRepaymentWeight(UnionRepaymentType unionRepaymentType) {
    return ecSiteVars.getConfVal(UNION_REPAYMENT_WEIGHT_MAP, unionRepaymentType.name(), Integer.MIN_VALUE);
  }

  public String getRepaymentChannelH5PageUrlBySourceType(SourceType sourceType) {
    return Optional.ofNullable(sourceType)
        .map(type -> JsonUtils.fromOrException(
            ecSiteVars.getString(REPAYMENT_CHANNEL_H5_PAGE_URL_MAP_BY_SOURCE_TYPE, "{}"),
            new TypeReference<Map<String, String>>() {}
        ).get(type.name()))
        .orElse(null);
  }

  public Set<String> getRepaymentChannelsByApiChannel() {
    String list = ecSiteVars.getString(REPAYMENT_CHANNEL_FOR_API_CHANNEL, "[]");
    return JsonUtils.fromOrException(list, new TypeReference<Set<String>>() {
    });
  }

  public boolean getRepaymentAccountRefactorSwitch() {
    return ecSiteVars.getBoolean(REPAYMENT_ACCOUNT_REFACTOR_SWITCH, false);
  }

  public Set<String> getJbpDefaultChannel() {
    String configStr = ecSiteVars.getString(JBP_DEFAULT_CHANNEL, "");
    if (StringUtils.isBlank(configStr)) {
      return new HashSet<>();
    }
    return Arrays.stream(configStr.split(",")).collect(Collectors.toSet());
  }

  public boolean getAlfMartSwitch() {
    return ecSiteVars.getBoolean(ALF_MART_SWITCH, true);
  }

  /**
   * 「其它银行」入口对应的真实还款渠道，默认 SEABANK；
   * 配置值需为 DynamicAccountChannel 枚举名（如 SEABANK、BCA 等）。
   */
  public String getOtherBankTargetChannel() {
    return ecSiteVars.getString(OTHER_BANK_TARGET_CHANNEL, "SEABANK");
  }

  /**
   * 将 OTHER 渠道解析为配置中心指定的真实渠道；非 OTHER 原样返回。
   */
  public String resolveOtherBankChannel(String channel) {
    if (CHANNEL_OTHER_BANK.equals(channel)) {
      return getOtherBankTargetChannel();
    }
    return channel;
  }

  public static final String CHANNEL_OTHER_BANK = "OTHER";
}
