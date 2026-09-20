package com.yqg.core.service.kafka;

import com.yqg.ec.common.exception.EcException;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

import static com.yqg.core.service.kafka.KafkaTopic.PatternStr.REGEX;
import static com.yqg.core.service.kafka.KafkaTopic.PatternStr.STR;

/**
 * Created by xiahonggao on 2017/3/16.
 */
public enum KafkaTopic {
  TEST("test"),
  TEST2("test2"),
  CASH_LOAN_EVENT("cash_loan_event"),
  DEBT_CHANGE("debt_change"),
  CONTRACT_SIGN("contract_sign"),
  DEBT_MATCH_EVENT("debt_match_event"),
  USER_EVENT("user_event"),
  BUSINESS_PAY_EVENT("business_pay_event"),
  FINANCING_ASSET_EVENT("financing_asset_event"),
  RISK_EVENT("risk_event"),
  /**
   * 风控创建 Trace
   */
  RISK_TRACE("risk_trace"),
  NOTIF_EVENT("notif_event"),
  NOTIF_DATA_WAREHOUSE_EVENT("notif_data_warehouse_event"),
  THIRD_PARTY_DATA_EVENT("third_party_data_event"),
  RISK_DATA_EVENT("risk_data_event"),
  BIZ_CHECK_EVENT("biz_check_event"),
  EC_DEVICE_INFO("ec_device_info"),
  ADMIN_OPERATE_EVENT("admin_operation_event"),
  /**
   * 营销中心接收元事件的topic
   */
  MARKETING_EVENT_REPORT("marketing_event_report"),
  /**
   * 市场回传事件
   */
  AD_UPLOAD_EVENT("ad_upload_event"),
  /**
   * 市场归因计算
   */
  AD_ATTRIBUTION("ad_attribution"),
  /**
   * 市场广告数据
   */
  AD_RAW_DATA_EVENT("ad_raw_data_event"),
  /**
   * 息费拆分二次入账抵扣明细
   */
  CAPITAL_FEE_DEDUCT("capital_fee_deduct"),
  /**
   * 用户还款写入机构资金还款事件
   */
  USER_TRIGGER_REPAY_EVENT("user_trigger_repay_event"),
  /**
   * 出借人账单计息
   */
  INVESTOR_CALC_EARNINGS_EVENT("investor_calc_earnings_event"),
  /**
   * 增信提额事件通知，kafka通知
   */
  INCREASE_CREDIT_EVENT("increase_credit_event"),
  /**
   * 协议应签事件topic
   */
  CONTRACT_NEED_SIGN("contract_need_sign"),
  /**
   * 协议实签事件topic
   */
  CONTRACT_ACTUAL_SIGN("contract_actual_sign"),
  /**
   * Jbp订单状态事件topic
   */
  JBP_ORDER_EVENT("jbp_order_event"),
  /**
   * App启动事件topic (at least once delivery)
   */
  APP_STARTUP_AT_LEAST_ONCE_EVENT("app_startup_at_least_once_event"),
  /**
   * EC组合支付事件
   */
  EC_COMBINED_REPAY_EVENT("ec_combined_repay_event"),
  /**
   * 贷超产品UV价值天维度数据
   */
  LOAN_MARKET_PRODUCT_UV_VALUE("loan_market_product_uv_value"),
  /**
   * 贷超合作商事件数据
   */
  LOAN_MARKET_PARTNER_EVENT("loan_market_partner_event"),
  /**
   * 用户行为数据主动同步（按用户+时间范围快照推送下游）
   */
  USER_BEHAVIOUR_SYNC("ec_user_behaviour_sync")
  ;

  public String str;
  public PatternStr patternStr = STR;

  public enum PatternStr {
    STR, REGEX
  }

  private Pattern pattern;

  KafkaTopic(String str) {
    this.str = str;
  }

  public static KafkaTopic fromStr(String str) {
    for (KafkaTopic topic : KafkaTopic.values()) {
      switch (topic.patternStr) {
        case STR:
          if (topic.str.equals(str)) {
            return topic;
          }
          break;
        case REGEX:
          if (topic.pattern.matcher(StringUtils.lowerCase(str)).matches()) {
            return topic;
          }
          break;
      }

    }

    throw EcException.error("unexpected str " + str);
  }
}
