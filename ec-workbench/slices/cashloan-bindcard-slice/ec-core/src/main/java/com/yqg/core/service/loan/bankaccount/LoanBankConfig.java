package com.yqg.core.service.loan.bankaccount;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.bankconfig.vo.BankAttributeVO;
import com.yqg.core.service.config.TestWhiteListConfig;
import com.yqg.core.service.loan.bankaccount.enums.VerifyEWalletAccountMethod;
import com.yqg.core.service.loan.infos.BindBankInitWritingInfo;
import com.yqg.ec.common.configuration.Conf;
import com.yqg.ec.common.configuration.ISiteVars;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.serialization.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yuchenghuang
 * @date 2021/12/20
 */
@Component
public class LoanBankConfig {
  @Autowired
  private ISiteVars ecSiteVars;

  @Autowired
  private TestWhiteListConfig testWhiteListConfig;

  private static final String UNSUPPORTED_VALIDATION_SDK_TYPE = "LOAN_BANK.UNSUPPORTED_VALIDATION_SDK_TYPE";
  private static final String LOAN_BANK_DISPLAY_RULE_FOR_NEW_AUTH = "LOAN_BANK.DISPLAY_RULE_FOR_NEW_AUTH";
  private static final String FINANCING_BANK_DISPLAY_RULE = "LOAN_BANK.FINANCING.DISPLAY_RULE";
  private static final String LOAN_BANK_NEED_CHECK_NAME_MATCH = "LOAN_BANK.NEED_CHECK_NAME_MATCH";
  private static final String VERIFY_GO_PAY_ACCOUNT_METHOD = "LOAN_BANK.VERIFY_GO_PAY_ACCOUNT_METHOD";
  private static final String NAME_MATCH_SIMILAR_MIN_SCORE = "LOAN_BANK.NAME_MATCH_SIMILAR_MIN_SCORE";
  private static final String LOAN_BANK_UNSUPPORTED_BANK = "LOAN_BANK.UNSUPPORTED_BANK";
  private static final String LOAN_BANK_ILLEGAL_EWALLET_BANK_TYPE = "LOAN_BANK.ILLEGAL_EWALLET_BANK_TYPE";
  private static final String LOAN_BANK_LEGAL_EWALLET_BANK_TYPE = "LOAN_BANK.LEGAL_EWALLET_BANK_TYPE";
  private static final String ADD_BANK_ACCOUNT_MAX_NOT_MATCH_TIMES = "LOAN_BANK.ADD_BANK_ACCOUNT_MAX_NOT_MATCH_TIMES";
  private static final String ADD_BANK_ACCOUNT_PERIOD_SECONDS = "LOAN_BANK.ADD_BANK_ACCOUNT_PERIOD_SECONDS";
  private static final String SUPPORT_MULTIPLE_CHECK_FOR_NON_PROD = "LOAN_BANK.SUPPORT_MULTIPLE_CHECK_FOR_NON_PROD";
  private static final String SUPPORT_MAX_ACCOUNT_NUMBER_LENGTH = "LOAN_BANK.SUPPORT_MAX_ACCOUNT_NUMBER_LENGTH";
  private static final String LOAN_ATTRIBUTE_BANK = "LOAN_BANK.ATTRIBUTE_BANK";
  private static final String LOAN_FILTER_BANK_TYPE = "LOAN_BANK.FILTER_BANK_TYPE";
  private static final String LOAN_FILTER_BANK_TYPE_WHITE_USERID = "LOAN_BANK.FILTER_BANK_TYPE_WHITE_USERID";
  private static final String LOAN_EWALLET_FILTER_BANK_TYPE = "LOAN_BANK.EWALLET_FILTER_BANK_TYPE";
  private static final String BIND_BANK_INIT_TEXT = "LOAN_BANK.BIND_BANK_INIT_TEXT";
  private static final String BANK_APP_PACKAGE_LIST = "LOAN_BANK.BANK_APP_PACKAGE_LIST";
  private static final String USE_OLD_BANK_ACCOUNT_PAGE = "LOAN_BANK.USE_OLD_BANK_ACCOUNT_PAGE";
  private static final String BANK_DISPLAY_SWITCH = "LOAN_BANK.BANK_DISPLAY_SWITCH";
  private static final String UNSUPPORTED_VALIDATION_USER_ID = "LOAN_BANK.UNSUPPORTED_VALIDATION_USER_ID";
  private static final String LOAN_FILTER_BANK_TYPE_MEDIA_SOURCE_ALLOWLIST = "LOAN_BANK.FILTER_BANK_TYPE_MEDIA_SOURCE_ALLOWLIST";
  private static final String BANK_CARD_VALIDATION_CHECK = "LOAN_BANK.BANK_CARD_VALIDATION_CHECK";
  private static final String BANK_CARD_UNIQUE_KEY_WITH_BANK_CODE = "LOAN_BANK.BANK_CARD_UNIQUE_KEY_WITH_BANK_CODE";
  private static final String LOAN_BANK_PAYOUT_LIMIT_CONFIG = "LOAN_BANK.PAYOUT_LIMIT_CONFIG";
  private static final String MARKETING_BIND_CARD_EVENT_ENABLED = "LOAN_BANK.MARKETING_BIND_CARD_EVENT_ENABLED";

  @PostConstruct
  private void registerConfig() {
    ecSiteVars.registerConf(SUPPORT_MAX_ACCOUNT_NUMBER_LENGTH, new Conf<Integer>() {
    });
    ecSiteVars.registerConf(BANK_CARD_VALIDATION_CHECK, new Conf<Map<String, String>>() {
    });
  }


  public Boolean noNeedValidation(SDKType sdkType) {
    String sdkTypes = ecSiteVars.getString(UNSUPPORTED_VALIDATION_SDK_TYPE);
    return sdkTypes.contains(sdkType.name());
  }

  public Boolean noNeedValidationByUserId(Long userId) {
    try {
      String userIdString = ecSiteVars.getString(UNSUPPORTED_VALIDATION_USER_ID);
      if (StringUtils.isBlank(userIdString)) {
        return false;
      }

      List<Long> userIds = JsonUtils.from(userIdString, new TypeReference<List<Long>>() {
      });

      return userIds.contains(userId);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 获取银行卡配置
   *
   * @param configKey 配置后缀
   * @return 银行展示顺序配置 key:银行类型 value:顺序 e.g BRI->1 表示bri的展示顺序为第一位
   */
  public Map<BankType, Integer> getDisplayRule(String configKey) {
    String typeStr = ecSiteVars.getString(configKey, "");
    Map<BankType, Integer> resultMap = new HashMap<>();
    if (StringUtils.isBlank(typeStr)) {
      return resultMap;
    }
    List<BankType> bankTypeList = Arrays.stream(typeStr.split(",")).map(BankType::valueOf).collect(Collectors.toList());
    for (int i = 0; i < bankTypeList.size(); i++) {
      resultMap.put(bankTypeList.get(i), i);
    }
    return resultMap;
  }

  public Map<BankType, BankAttributeVO> getBankAttributeVOMap() {
    String bankAttributeConfig = ecSiteVars.getString(LOAN_ATTRIBUTE_BANK, "{}");
    return JsonUtils.from(bankAttributeConfig, new TypeReference<Map<BankType, BankAttributeVO>>() {
    });
  }

  /**
   * 新增了一个sdkType参数 区分理财|借贷
   *
   * @param sdkType 客户端类型
   * @return 银行展示规则
   */
  public Map<BankType, Integer> getDisplayRule(SDKType sdkType) {
    // 借贷保持借贷的展示规则
    if (sdkType.isLoanSDKType()) {
      return getDisplayRule(LOAN_BANK_DISPLAY_RULE_FOR_NEW_AUTH);
    }
    return getDisplayRule(FINANCING_BANK_DISPLAY_RULE);
  }


  public boolean getNeedCheckNameMatch() {
    return ecSiteVars.getBoolean(LOAN_BANK_NEED_CHECK_NAME_MATCH, false);
  }

  public VerifyEWalletAccountMethod getVerifyGoPayAccountMethod() {
    String str = ecSiteVars.getString(VERIFY_GO_PAY_ACCOUNT_METHOD, "MOBILE_NUMBER");
    return VerifyEWalletAccountMethod.valueOf(str);
  }

  public Integer getNameMatchSimilarMinScore() {
    return ecSiteVars.getInt(NAME_MATCH_SIMILAR_MIN_SCORE, 70);
  }

  public List<BankType> getUnsupportedBankType() {
    String bankTypeStr = ecSiteVars.getString(LOAN_BANK_UNSUPPORTED_BANK, "");
    if (StringUtils.isEmpty(bankTypeStr)) {
      return new ArrayList<>();
    }
    return Arrays.stream(bankTypeStr.split(",")).map(BankType::valueOf).collect(Collectors.toList());
  }

  public List<BankType> getIllegalEWalletBankType() {
    String bankTypeStr = ecSiteVars.getString(LOAN_BANK_ILLEGAL_EWALLET_BANK_TYPE, "");
    if (StringUtils.isEmpty(bankTypeStr)) {
      return new ArrayList<>();
    }
    return Arrays.stream(bankTypeStr.split(",")).map(BankType::valueOf).collect(Collectors.toList());
  }

  public List<BankType> getLegalEWalletBankType() {
    String bankTypeStr = ecSiteVars.getString(LOAN_BANK_LEGAL_EWALLET_BANK_TYPE, "");
    if (StringUtils.isEmpty(bankTypeStr)) {
      return new ArrayList<>();
    }
    return Arrays.stream(bankTypeStr.split(",")).map(BankType::valueOf).collect(Collectors.toList());
  }

  public Long getAddBankAccountMaxNotMatchTimes() {
    return ecSiteVars.getLong(ADD_BANK_ACCOUNT_MAX_NOT_MATCH_TIMES, 2L);
  }

  public Long getAddBankAccountPeriodSeconds() {
    return ecSiteVars.getLong(ADD_BANK_ACCOUNT_PERIOD_SECONDS, 24 * 60 * 60L);
  }

  //方便测试环境测试
  public boolean supportMultipleCheckForNonProd() {
    return ecSiteVars.getBoolean(SUPPORT_MULTIPLE_CHECK_FOR_NON_PROD, false);
  }

  public Integer getSupportMaxAccountNumberLength(BankType bankType) {
    return ecSiteVars.getConfVal(SUPPORT_MAX_ACCOUNT_NUMBER_LENGTH, bankType.name(), 50);
  }

  public List<BankType> getFilterBankType() {
    String filterBankType = ecSiteVars.getString(LOAN_FILTER_BANK_TYPE, "");
    if (StringUtils.isBlank(filterBankType)) {
      return new ArrayList<>();
    }
    return Arrays.stream(filterBankType.split(",")).map(BankType::valueOf).collect(Collectors.toList());
  }

  public List<BankType> getFilterEWalletType() {
    String filterEWalletType = ecSiteVars.getString(LOAN_EWALLET_FILTER_BANK_TYPE, "");
    if (StringUtils.isBlank(filterEWalletType)) {
      return new ArrayList<>();
    }
    return Arrays.stream(filterEWalletType.split(",")).map(BankType::valueOf).collect(Collectors.toList());
  }

  public List<Long> getFilterBankTypeWhiteUserId() {
    String filterBankType = ecSiteVars.getString(LOAN_FILTER_BANK_TYPE_WHITE_USERID, "");
    return Arrays.stream(filterBankType.split(",")).filter(StringUtils::isNotBlank).map(Long::valueOf).collect(Collectors.toList());
  }

  public List<String> getFilterBankTypeMediaSourceAllowList() {
    String filterMediaSource = ecSiteVars.getString(LOAN_FILTER_BANK_TYPE_MEDIA_SOURCE_ALLOWLIST, "");
    return Arrays.stream(filterMediaSource.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
  }

  public BindBankInitWritingInfo getBindBankInitText() {
    String bindBankText = ecSiteVars.getString(BIND_BANK_INIT_TEXT, "{}");
    return JsonUtils.from(bindBankText, BindBankInitWritingInfo.class);
  }

  public Set<String> getBankAppPackageSet() {
    String conf = ecSiteVars.getString(BANK_APP_PACKAGE_LIST, "[]");
    return new HashSet<>(JsonUtils.fromOrException(conf, new TypeReference<List<String>>() {
    }));
  }

  public boolean useOldBankAccountPage() {
    return ecSiteVars.getBoolean(USE_OLD_BANK_ACCOUNT_PAGE, false);
  }

  public boolean bankDisplaySwitch() {
    return ecSiteVars.getBoolean(BANK_DISPLAY_SWITCH, false);
  }

  public List<String> getAuthCheckBankAccountNumberWhitelist() {
    return testWhiteListConfig.getAuthCheckBankAccountNumberWhitelist();
  }

  public Map<String, String> getBankCardVallidationCheck(BankType bankType) {
    return ecSiteVars.getConfVal(BANK_CARD_VALIDATION_CHECK, bankType.name(), null);
  }

  //todo @chenyapeng 功能降级开关，上线1周后删除
  public boolean isBankCardUniqueKeyWithBankCode() {
    return ecSiteVars.getBoolean(BANK_CARD_UNIQUE_KEY_WITH_BANK_CODE, false);
  }

  /**
   * 放款渠道限额配置（TAPD-364191）。一份 JSON map 同时承载各渠道的单笔限额与超限提示文案。
   *
   * <p>配置示例：
   * <pre>{@code
   * {
   *   "DANA":  {"maxPayoutAmount": 2000000, "listHint": "...{0}...", "badgeHint": "..."},
   *   "OVO":   {"maxPayoutAmount": 2000000, "listHint": "...{0}...", "badgeHint": "..."}
   * }
   * }</pre>
   *
   * <p>整表返回，由调用方（{@link PayoutAccountLimitService}）取一次后在循环内复用，避免逐账户重复反序列化。
   *
   * @return 渠道限额配置 map；未配置或解析失败返回空 map，绝不返回 {@code null}
   */
  public Map<BankType, PayoutLimitConfigVO> getPayoutLimitConfigMap() {
    String config = ecSiteVars.getString(LOAN_BANK_PAYOUT_LIMIT_CONFIG, "{}");
    Map<BankType, PayoutLimitConfigVO> configMap = JsonUtils.from(config,
        new TypeReference<Map<BankType, PayoutLimitConfigVO>>() {
        });
    return configMap == null ? Collections.emptyMap() : configMap;
  }

  /**
   * 营销平台绑卡事件上报开关（TAPD-1374374）
   * 用于线上灰度和紧急回滚
   *
   * @return true=开启上报（默认），false=关闭上报
   */
  public boolean isMarketingBindCardEventEnabled() {
    return ecSiteVars.getBoolean(MARKETING_BIND_CARD_EVENT_ENABLED, false);
  }
}
