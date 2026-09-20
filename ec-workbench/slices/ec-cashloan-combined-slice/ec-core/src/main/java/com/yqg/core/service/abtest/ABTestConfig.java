package com.yqg.core.service.abtest;

import static com.yqg.core.service.abtest.enums.ExperimentNameSpace.LONG_TERM_EXPERIMENT_SET;
import static com.yqg.core.service.general.appconfig.provider.DeductDescPopupStyleProvider.EXP_KEY_NOT_OVERDUE;
import static com.yqg.core.service.user.UserMobileChangeService.FORCE_MERGE_PRE_VERIFY_EXPERIMENT_KEY;
import static com.yqg.ec.common.constant.ExperimentKeyConstants.FIRST_LOAN_COUPON_LONG_TERM_EXPERIMENT;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.yqg.config.annotation.Refreshable;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.abtest.enums.SourceTypeResolveMode;
import com.yqg.core.service.general.pageconfig.filterstrategy.processor.OrderPageCustomerBroadcastFilterRuleProcessor;
import com.yqg.core.service.loanmarket.config.LoanMarketConfig;
import com.yqg.ec.common.configuration.Conf;
import com.yqg.ec.common.configuration.ISiteVars;
import com.yqg.ec.common.constant.ExperimentKeyConstants;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.experiment.common.enums.ResultGetType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author chaoye
 * @date 2023/10/19
 */

@Slf4j
@Component
@Refreshable
public class ABTestConfig {

  @Resource
  private ISiteVars ecSiteVars;

  private static final String NOTIF_OLD_CONSISTENCY_MARKER = "notif.old_consistency_marker_list";
  private static final String LIST_AB_TEST_GENERATE_RESULT = "ab_test.list_ab_test_generate_result";
  private static final String DEVICE_TOKEN_AB_TEST_GENERATE_RESULT = "ab_test.device_token_ab_test_generate_result";
  private static final String CONTROL_GROUP_EFFECTIVE = "ab_test.control_group_effective";
  private static final String RESULT_SYNC_TASK_NUM = "ab_test.result_sync_task_num";
  private static final String REGISTER_TIME_AB_MIN_ID = "ab_test.register_time_ab_min_id";
  private static final String REGISTER_TIME_AB_PUSH_SWITCH = "ab_test.register_time_ab_push_switch";
  private static final String UPDATE_EXPERIMENT_DELAY_SECOND = "ab_test.update_experiment_delay_second";
  private static final String ADMIN_CONTROL_GROUP_EFFECTIVE_TIMESTAMP = "ab_test.admin_control_group_effective_timestamp";
  private static final String IGNORE_CONTROL_GROUP_SCENE_TYPE_LIST = "ab_test.ignore_control_group_scene_type_list";
  private static final String ALLOW_VERSION_CONFIG_AB_TEST_IDS = "ab_test.allow_version_config_ab_test_ids";
  private static final String VERSION_CONFIG_VERSION_INFO_DESC_MAPPING = "ab_test.version_config_version_info_desc_mapping";
  private static final String H2_CONTROL_GROUP_EXPERIMENT_ID = "ab_test.2024_h2_control_group_experiment_id";
  private static final String H2_CONTROL_GROUP_EXPERIMENT_GROUP_ID = "ab_test.2024_h2_control_group_experiment_group_id";
  private static final String H2_CONTROL_GROUP_EXPERIMENT_CONTROL_GROUP_2_ID = "ab_test.2024_h2_control_group_experiment_control_group_2_id";

  private static final String H2_CONTROL_GROUP_2_ONLINE_TIME = "ab_test.2024_h2_control_group_2_online_time";
  private static final String AUTO_CREATE_TRAFFIC_LAYER = "ab_test.auto_create_traffic_layer";
  private static final String EXPERIMENT_PLATFORM_TOKEN = "ab_test.experiment_platform_token";
  private static final String CHILD_LINE_TO_SUB_SCENE_LIST_MAP = "ab_test.child_line_to_sub_scene_list_map";

  private static final String FETCH_RESULT_NO_CHECK_LANE_BUSINESS_SCENE_LIST = "ab_test.fetch_result_no_check_lane_business_scene_list";
  private static final String REGISTER_AB_BACK_FILL_CONFIG = "ab_test.register_ab_backfill_config";
  private static final String ORDER_AB_BACK_FILL_CONFIG = "ab_test.order_ab_backfill_config";
  private static final String CHILD_LANE_RELATION_HARDCODED_SWITCH = "ab_test.childLaneRelationHardCoded";
  private static final String DEVICE_TOKEN_USE_EXPERIMENT_PLATFORM = "ab_test.device_token_use_experiment_platform";
  private static final String EXPERIMENT_NAME_CONFIG_MAP = "ab_test.experiment_name_config_map";
  private static final String EXPERIMENT_NAME_CONFIG_MAP_V2 = "ab_test.experiment_name_config_map_v2";
  private static final String APP_CONFIG_AB_START_BUILD = "ab_test.app_config_ab_start_build";
  private static final String APP_CONFIG_AB_MIN_VERSION = "ab_test.app_config_ab_min_version";
  private static final String APP_CONFIG_AB_MAX_VERSION = "ab_test.app_config_ab_max_version";
  private static final String EXPERIMENT_PLATFORM_V2_KEY = "ab_test.experiment_platform_v2_key";
  private static final String LANE_KEY_CONFIG_MAP = "ab_test.lane_key_config_map";
  private static final String MC_RESOLVE_AB_RESULT_TYPE = "ab_test.mc_resolve_ab_result_type";
  private static final String API_CHANNEL_NEED_ABTEST_KEY = "ab_test.api_channel_need_abtest_key";
  private static final String MC_RESOLVE_AB_RESULT_WHITELIST = "ab_test.mc_resolve_ab_result_whitelist";
  // 是否在实验客户端启用“请求级缓存”的读取。默认 false：仅写缓存但返回实时结果（dry-run）。
  private static final String USE_REQUEST_CACHE_FOR_ABTEST = "ab_test.use_request_cache_for_abtest";
  private static final String SOURCE_TYPE_RESOLVE_MODE = "ab_test.source_type_resolve_mode";
  /** LiveDemo：expKey → 强制 groupResult 字符串（JSON Map） */
  private static final String LIVE_DEMO_FORCE_RESULT_MAP = "ab_test.live_demo_force_result_map";

  /**
   * 不写枚举了，短期实验平台不支持配置，我们先在代码中初始化
   */
  private static final Map<String, Range<Long>> DEFAULT_EXPERIMENT_BUILD_MAP = ImmutableMap.<String, Range<Long>>builder()
      .put("experiment-example-key1", Range.closed(37900L, 99999L))
      .put("experiment-example-key2", Range.closed(0L, 36900L))
      .put("product_operation_auth-auth-abroad-loan-authentication_benefits_1_0928", Range.closed(36900L, 99999L))
      .put("pretii-other-abroad-loan_all-GOLDEN_CARD_STYLE_1014", Range.closed(36900L, 99999L))
      .put("pretii-other-abroad-loan_all-EASYPLUS_PAY_V3", Range.closed(37800L, 99999L))
      .put(ExperimentKeyConstants.KTP_VISUAL_OPTIMIZATION_EXPERIMENT_KEY, Range.closed(36900L, 99999L))
      .put("technology-repayment-abroad-loan_all-repayment_prompt_new", Range.closed(36900L, 99999L))
      .put("technology-lending-abroad-loan_all-loan_prompt_new", Range.closed(36900L, 99999L))
      .put("product_operation_auth-auth-abroad-loan-credit_loan_V5_100juta", Range.closed(36400L, 99999L))
      .put("root_register-register-abroad-loan-login_page_V4_8year_login", Range.closed(37900L, 99999L))
      .put("root_register-register-abroad-loan-login_page_V4_8year_login_1103", Range.closed(37900L, 99999L))
      .put("product_operation_auth-not_withdraw-abroad-loan-auth_order_V5_parent_1024", Range.closed(36400L, 99999L))
      .put("product_operation_auth-not_withdraw-abroad-loan-auth_order_V5_sub1_1024", Range.closed(36400L, 99999L))
      .put("product_operation_auth-not_withdraw-abroad-loan-auth_order_V5_sub2_1024", Range.closed(36400L, 99999L))
      .put("auth_product_26h1-auth-abroad-loan-auth_abroad_loan_auth_order_V54_0121", Range.closed(36400L, 99999L))
      .put("technology-other-abroad-loan_all-subHomeResourceRefreshLimitConfigFlag", Range.closed(38300L, 99999L))
      .put("technology-other-abroad-loan_all-enableHomeResourceRefreshLimit", Range.closed(38300L, 99999L))
      .put("technology-other-abroad-loan_all-enableResourceV2RefreshLimit", Range.closed(38300L, 99999L))
      .put(ExperimentKeyConstants.REPAYMENT_ACCOUNT_EXP_KEY, Range.closed(0L, 99999L))
      .put("product_operation_auth-auth-abroad-loan-auth_process_UI_1113", Range.closed(37500L, 99999L))
      .put("technology-auth-abroad-loan-tencentH5upgrade", Range.closed(0L, 99999L))
      .put("technology-lending-abroad-loan_all-cut_coupon_post_use", Range.closed(0L, 999999L))
      .put(ExperimentKeyConstants.LOAN_PAGE_V4_COUPON_UPGRADE_AREA, Range.closed(36900L, 999999L))
      .put("product_operation_order-lending-abroad-loan_all-back_UPGRADED_V2", Range.closed(36900L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-back_UPGRADED_V3", Range.closed(36900L, 99999L))
      .put("technology-lending-abroad-loan_all-agreement_plus_V2", Range.closed(36900L, 99999L))
      .put("product_operation_order-lending-abroad-loan-T0_no_interest_popup_1121", Range.closed(36900L, 99999L))
      .put(LoanMarketConfig.LOAN_MARKET_DISPLAY_STRETEGY_EXPR_KEY, Range.closed(38310L, 99999L))
      .put(LoanMarketConfig.LOAN_MARKET_DISPLAY_IN_H5_EXPR_KEY, Range.closed(38310L, 99999L))
      .put("test_user_id-auth-abroad-after_loan-loanMarketCardBusinessExpr", Range.closed(38310L, 99999L))
      .put("test_user_id-auth-abroad-after_loan-loanMarketCardTechExpr", Range.closed(38310L, 99999L))
      .put("technology-lending-abroad-loan_all-material_style_config_1201", Range.closed(38300L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-limit_and_rate_adjustment", Range.closed(36900L, 99999L))
      .put("root_register-register-abroad-loan-login_page_back_v3_1127", Range.closed(38300L, 99999L))
      .put("braavos-auth-abroad-loan_all-loanmarketentranceexpand", Range.closed(38310L, 99999L))
      .put("technology-auth-abroad-loan_all-loanmarketentranceexpand_tech", Range.closed(38310L, 99999L))
      .put("technology-lending-abroad-loan_all-limit_and_rate_adjustment_fudai_pro_v2", Range.closed(37213L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-old_loan_page_Discount_Countdown_24h", Range.closed(0L, 99999L))
      .put(ExperimentKeyConstants.PRODUCT_DETAIL_CHANGE_POPUP_PUSH_COUPON_V3, Range.closed(36900L, 99999L))
      .put("product_operation_auth-auth-abroad-loan-homepage_popup_1120", Range.closed(38013L, 999999L))
      .put("product_operation_bill_normal-repayment-abroad-loan_all-jumptoorderpage_reducefrequency_V2", Range.closed(37200L, 99999L))
      .put("product_operation_bill_normal-repayment-abroad-loan_all-Auto_redirect_billing_normal", Range.closed(38400L, 999999L))
      .put(ExperimentKeyConstants.JBP_ADD_NEW_PAYMENT_CHANNEL, Range.closed(37800L, 99999L))
      .put(ExperimentKeyConstants.JBP_PAYMENT_CHANNEL_DISPLAY_ORDER, Range.closed(37800L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-Midtrans_BNI_normal_0112_V1", Range.closed(0L, 99999L))
      .put(ExperimentKeyConstants.MIDTRANS_PERMATA_CIMB_DANAMON_NORMAL, Range.closed(0L, 99999L))
      .put("product_operation_auth-auth-abroad-loan-auth_order_V5_sub3_1208", Range.closed(36400L, 999999L))
      .put(ExperimentKeyConstants.SEABANK_CHANNEL_NORMAL, Range.closed(37413L, 99999L))
      .put("technology-auth-abroad-loan-auth_company_name_1215", Range.closed(36900L, 99999L))
      // TAPD-355542 鉴权信息项合规优化（出生地/月收入/公司名服务端硬校验），PRD 要求 App version > 36900
      .put(UserFlowExperimentEnum.AUTH_INFO_COMPLIANCE_V1.getKey(), Range.closed(36901L, 99999L))
      .put(ExperimentKeyConstants.FLIP_FULL_PROCESS_OR_LANDING_PAGE, Range.closed(0L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-post_loan_threshold_3_1", Range.closed(37300L, 99999L))
      .put("product_operation_order-coupon-abroad-loan_all-one_click_system_design", Range.closed(37300L, 99999L))
      .put(ExperimentKeyConstants.T0_RISK_ACCEPT_GRANT_CUT_COUPON_LOW_WILL, Range.closed(0L, 99999L))
      .put(ExperimentKeyConstants.T0_RISK_ACCEPT_GRANT_CUT_COUPON_HIGH_WILL, Range.closed(0L, 99999L))
      // TAPD-1371553 首贷常规 E 评级 10min 离线灌券；build 由 Observer/OpenApp 前置路径判断
      .put(ExperimentKeyConstants.FIRST_LOAN_ERANK_10MIN_COUPON, Range.closed(0L, 99999L))
      .put(ExperimentKeyConstants.RELOAN_TOP_LOW_WILL_COUPON, Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_TOP_WILLING_COUPON_V2.getKey(), Range.closed(0L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_AGENT_COUPON_V1_1_EXCLUDE.getKey(), Range.closed(0L, 99999L))
      .put(ExperimentKeyConstants.RISK_ACCEPT_GRANT_CUT_COUPON_FATHER, Range.closed(0L, 99999L))
      .put("product_operation_order-not_withdraw-abroad-loan_all-keep_credit_limit_v1_2_enroll_in", Range.closed(37313L, 999999L))
      .put("product_operation_order-not_withdraw-abroad-loan_all-keep_credit_limit_v1_2_enroll_out", Range.closed(37313L, 999999L))
      .put("product_operation_order-not_withdraw-abroad-loan_all-KeepCreditLimit_v1_1", Range.closed(37313L, 999999L))
      .put("root_register-register-abroad-loan-login_pageV5_new_device_1205", Range.closed(38400L, 99999L))
      .put("root_register-register-abroad-loan-login_pageV5_active_device_1205", Range.closed(38400L, 99999L))
      .put(ExperimentKeyConstants.LOAN_MARKET_OVERDUE_TIER, Range.closed(38410L, 99999L))
      .put("root_register-register-abroad-loan-homepage_v3", Range.closed(37813L, 99999L))
      .put("register_26h1-register-abroad-loan_all-before_login_reg_home_page_0106", Range.closed(37813L, 99999L))
      .put(ExperimentKeyConstants.OVERDUE_REPAY_AC,Range.closed(38200L, 999999L))
      .put("product_operation_order-not_withdraw-abroad-loan-T0_order_page_popup_1224", Range.closed(37213L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_REPAY_PLAN_GRANT_COUPON_V3.getKey(), Range.closed(37300L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_BCA_normal_0112", Range.closed(37300L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_gopay_normal_0112", Range.closed(37300L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_Mandiri_normal", Range.closed(37300L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_BNI_normal", Range.closed(37300L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-directdebit_Xendit_normal", Range.closed(37300L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_shopeepay_normal", Range.closed(37300L, 99999L))
      .put("reloan_credit_use_26h1-lending-abroad-loan_all-Order_incentive_V2_26H1", Range.closed(37300L, 99999L))
      .put("reloan_credit_use_26h1-lending-abroad-loan_all-post_loan_threshold_3_1_26H1", Range.closed(37300L, 99999L))
      .put("reloan_credit_use_26h1-lending-abroad-loan_all-one_click_system_design_26H1", Range.closed(37300L, 99999L))
      .put("reloan_credit_use_26h1-lending-abroad-loan_all-one_click_super_subisidy_v1", Range.closed(37300L, 99999L))
      .put("reloan_credit_use_26h1-lending-abroad-loan_all-one_click_crowd_limit_v1", Range.closed(37300L, 99999L))
      .put("first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_one_click_0121", Range.closed(37300L, 99999L))
      .put("first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_one_click_UI_0121_copy", Range.closed(37300L, 99999L))
      .put("reloan_order_26h1-lending-abroad-loan_all-repayment_interest_popup1030_26H1_reloan", Range.closed(36900L, 99999L))
      .put("first_loan_auth_26h1-lending-abroad-loan-repayment_interest_popup1030_26H1_first", Range.closed(36900L, 99999L))
      .put("technology-not_withdraw-abroad-loan-first_loan_coupon_son_pro", Range.closed(0L, 99999L))
      .put("product_operation_order-not_withdraw-abroad-loan_all-04InterestRate_0725_copy", Range.closed(0L, 99999L))
      .put("product_operation_order-not_withdraw-abroad-loan_all-HomePageV3_loan_0723", Range.closed(0L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-repayment_interest_popup1030_V1", Range.closed(36900L, 99999L))
      .put("pretii-other-abroad-loan_all-EASYPLUS_tab3", Range.closed(36900L, 99999L))
      .put("technology-register-abroad-loan_all-wallet_split_2", Range.closed(0L, 99999L))
      .put("product_operation_auth-auth-abroad-loan-Card_binding_1_0928", Range.closed(37900L, 99999L))
      .put("technology-lending-abroad-loan_all-h5_ui_new_order_1215", Range.closed(35300L, 99999L))
      .put("risk_decision_userid-other-abroad-risk_decision-loan_reapply180_init", Range.closed(0L, 99999L))
      .put("risk_decision_userid-other-abroad-risk_decision-multi_loan_normal_retrieval_v2", Range.closed(0L, 99999L))
      .put(ExperimentKeyConstants.TENCENT_LIVING_SOURCE_FOR_H5, Range.closed(0L, 99999L))
      .put("technology-auth-abroad-loan-card_page_notification_remove", Range.closed(0L, 99999L))
      .put(ExperimentKeyConstants.AUTH_DECR_INTEREST_STYLE, Range.closed(37500L, 99999L))
      .put("pretii-other-abroad-loan_all-EASYPLUS_PAY_V4", Range.closed(37800L, 99999L))
      .put(ExperimentKeyConstants.EASY_PLUS_PAY_V, Range.closed(37300L, 99999L))
      .put("reloan_order_26h1-lending-abroad-renew-bebas_bunga_V1", Range.closed(36900L, 99999L))
      .put("reloan_order_26h1-lending-abroad-renew-back_UPGRADED_26H1", Range.closed(36900L, 99999L))
      .put("reloan_order_26h1-not_withdraw-abroad-loan_all-reloan_limit_coupon_parent_pro_26h1", Range.closed(36900L, 99999L))
      .put("first_loan_product_26h1-lending-abroad-loan-limit_coupon_V1", Range.closed(0L, 99999L))
      .put("reloan_order_26h1-not_withdraw-abroad-loan_all-reloan_limit_coupon_V1", Range.closed(0L, 99999L))
      .put("reloan_order_26h1-lending-abroad-renew-dont_leave_alternative_V5", Range.closed(37300L, 99999L))
      .put("first_loan_auth_26h1-measurement-abroad-loan-auth_order_animation_quota_V2_0128", Range.closed(36400L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-REPAY_CHANNEL_SORT_NORMAL_V3", Range.closed(38223L, 99999L))
      .put(ExperimentKeyConstants.PRODUCT_DETAIL_REFACTOR_COMPARE, Range.closed(0L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_ORDER_CICILAN_DISCOUNT_POP_V1.getKey(), Range.closed(37300L, 99999L))
      .put(ExperimentKeyConstants.PRODUCT_DETAIL_REFACTOR_SWITCH, Range.closed(0L, 99999L))
      .put("register_26h1-register-abroad-loan_all-login_page_auto_play_banner_0130", Range.closed(38600L, 99999L))
      .put(UserFlowExperimentEnum.FIRST_LOAN_ORDER_TO_BOOST.getKey(), Range.closed(36913L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_LIMIT_COUPON_NO_LIMIT_V2.getKey(), Range.closed(0L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_LIMIT_COUPON_NO_LIMIT_V3.getKey(), Range.closed(0L, 99999L))
      .put(UserFlowExperimentEnum.ORDER_PAGE_PUSH_COUPON_V4.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.ORDER_PAGE_PUSH_COUPON_FIRST_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.ORDER_PAGE_PUSH_COUPON_FIRST_LOAN_MID_LOW_WILL.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.NO_INTEREST_POPUP_V2.getKey(), Range.closed(37300L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_RETAIN_FREQUENCY_LIMIT_V1.getKey(), Range.closed(37300L, 99999L))
      .put(UserFlowExperimentEnum.ONE_CLICK_BENEFIT_LIST_EXP.getKey(), Range.closed(37300L, 99999L))
      .put(UserFlowExperimentEnum.DISCOUNT_MODEL.getKey(), Range.closed(0L, 99999L))
      .put(UserFlowExperimentEnum.HIDE_INCREASE_CREDIT_ENTRANCE_FOR_FIRSTLOAN_RETRIEVAL.getKey(), Range.closed(0L, 99999L))
      .put(UserFlowExperimentEnum.HOMEPAGE_FRAUD_WARNING.getKey(), Range.closed(38900L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_PROTOCOL_CONFIRM_FULL_SCREEN.getKey(), Range.closed(37300L, 99999L))
      .put(ExperimentKeyConstants.GRANT_CREDITS_COUPON_FOR_OPEN_APP_TRACE_TIME, Range.closed(0L, 99999L))
      .put("reloan_order_26h1-lending-abroad-renew-ladder_coupon_bundle", Range.closed(37300L, 99999L))
      .put("braavos-other-abroad-loan_all-loanmarketupgradeAPPpopup", Range.range(37013L, BoundType.CLOSED, 38313L, BoundType.OPEN))
      .put(ExperimentKeyConstants.RELOAN_NO_TEMP_CREDIT, Range.closed(36900L, 999999L))
      .put("first_loan_product_26h1-not_withdraw-abroad-loan-ab_user_order_page_0209",  Range.closed(37213L, 99999L))
      .put(EXP_KEY_NOT_OVERDUE, Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.HOME_INTEREST_FREE_CARD_REDEEM_LIMIT.getKey(), Range.closed(37300L, 99999L))
      .put(OrderPageCustomerBroadcastFilterRuleProcessor.EXP_KEY, Range.closed(37300L, 99999L))
      .put(ExperimentKeyConstants.DIRECT_ACTIVITY_FOR_HOME_MESSAGE_NORMAL, Range.closed(38223L, 99999L))
      .put(ExperimentKeyConstants.DIRECT_ACTIVITY_FOR_HOME_BANNER_NORMAL, Range.closed(38223L, 99999L))
      .put(ExperimentKeyConstants.DIRECT_ACTIVITY_FOR_REPAY_NORMAL, Range.closed(38223L, 99999L))
      .put(ExperimentKeyConstants.FIRST_LOAN_GENUINE_CREDIT_INCREASE_POPUP, Range.closed(37213L, 99999L))
      .put(UserFlowExperimentEnum.INTEREST_SPLIT_FIRST_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.INTEREST_SPLIT_RE_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.INTEREST_SPLIT_V2_FIRST_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.INTEREST_SPLIT_V2_RE_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.INTEREST_SPLIT_V4_FIRST_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.INTEREST_SPLIT_V4_RE_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.INTEREST_SPLIT_V5_FIRST_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.INTEREST_SPLIT_V5_RE_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-overdue_long_test", Range.closed(0L, 99999L))
      .put("bill_overdue_26h1-after_overdue-abroad-loan_all-repaymentchannel_job_overdue_0227", Range.closed(0L, 99999L))
      .put("bill_overdue_26h1-after_overdue-abroad-loan_all-repaymentchannel_gopay_overdue_0113_JOB", Range.closed(0L, 99999L))
      .put("bill_normal_26h1-after_overdue-abroad-loan_all-repaymentchannel_job_overdue_V2", Range.closed(0L, 99999L))
      .put("product_operation_bill_overdue-lending-abroad-loan_all-Auto_redirect_billing_Overdue",Range.closed(38400L, 999999L))
      .put(ExperimentKeyConstants.KTP_POPUP_STYLE_OPTIMIZATION_V1, Range.closed(36720L, 99999L))
      .put(ExperimentKeyConstants.T0_POPUP_FREQ_HEAD, Range.closed(37200L, 99999L))
      .put(ExperimentKeyConstants.T0_POPUP_FREQ_MIDDLE, Range.closed(37200L, 99999L))
      .put(ExperimentKeyConstants.T0_POPUP_FREQ_TAIL, Range.closed(37200L, 99999L))
      .put(ExperimentKeyConstants.T0_ORDER_PAGE_POPUP_H5, Range.closed(37713L, 99999L))
      .put(ExperimentKeyConstants.JIGUANG_IP_REGISTER, Range.closed(38700L, 99999L))
      .put(UserFlowExperimentEnum.ORDER_PAGE_RATE_DISCOUNT_FIRST_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.ORDER_PAGE_RATE_DISCOUNT_RE_LOAN.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.SUB_HOME_PAGE_COUPON_PACKAGE_V2.getKey(), Range.closed(37300L, 99999L))
      .put(UserFlowExperimentEnum.ORDER_PAGE_V5_FOR_RELOAN.getKey(), Range.closed(37300L, 99999L))
      .put(ExperimentKeyConstants.FIRST_LOAN_HOME_REWARD_EXP_KEY, Range.closed(36900L, 99999L))
      .put(ExperimentKeyConstants.BANK_CARD_UNIQUE_KEY_WITH_BANK_CODE, Range.closed(0L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_QRIS", Range.closed(38230L, 99999L))
      .put(UserFlowExperimentEnum.RELOAN_REPAY_PLAN_GRANT_COUPON_V2.getKey(), Range.closed(37300L, 99999L))
      .put(UserFlowExperimentEnum.TENURE_UPGRADE_BANNER.getKey(), Range.closed(37300L, 99999L))
      .put(UserFlowExperimentEnum.RETAIN_POPUP_BOOST.getKey(), Range.closed(37300L, 99999L))
      .put(UserFlowExperimentEnum.RETAIN_POPUP_BOOST_COUPON_STRATEGY.getKey(), Range.closed(37300L, 99999L))
      .put("technology-register-abroad-loan_all-loginpage_h5_0820", Range.closed(37700L, 99999L))
      .put("risk_decision_userid-auth-abroad-loan-init_usertype_source", Range.closed(0L, 99999L))
      .put("risk_decision_userid-not_withdraw-abroad-risk_decision-credits_limitrules_test", Range.closed(0L, 99999L))
      .put("product_operation_bill_normal-repayment-abroad-loan_all-REPAY_CHANNEL_SORT_NORMAL", Range.closed(38000L, 99999L))
      .put("product_operation_bill_overdue-after_overdue-abroad-loan_all-REPAY_CHANNEL_SORT_OVERDUE_copy", Range.closed(38000L, 99999L))
      .put("product_operation_bill-repayment-abroad-loan_all-repaymentchanneldananormal", Range.closed(35300L, 99999L))
      .put("product_operation_bill-repayment-abroad-loan_all-repaymentchanneldanaoverdue", Range.closed(35300L, 99999L))
      .put("technology-repayment-abroad-loan_all-BillpagetoH5_reopen", Range.closed(0L, 99999L))
      .put("technology-repayment-abroad-loan_all-BillpagetoH5overdue_reopen", Range.closed(0L, 99999L))
      .put("technology-auth-abroad-loan-superbank_rename_0908", Range.closed(36900L, 99999L))
      .put("technology-auth-abroad-loan-ktpocr_failure_0901", Range.closed(37113L, 99999L))
      .put("technology-auth-abroad-loan-MOTHER_LAST_NAME_CHECK_RULE_KTP_CONFIRM_PAGE_PRO", Range.closed(0L, 99999L))
      .put("technology-not_withdraw-abroad-loan_all-MOTHER_LAST_NAME_CHECK_RULE_ORDER_SUPPLEMENT_PAGE_PRO", Range.closed(0L, 99999L))
      .put("product_operation_order-not_withdraw-abroad-loan_all-marketing_coupons_0814", Range.closed(36900L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-QUICK_ORDER_LOAN_H5_0721", Range.closed(0L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-back_Quick_ordering_V3", Range.closed(0L, 99999L))
      .put("product_operation_order-not_withdraw-abroad-loan-T0_amount_0805", Range.closed(36900L, 99999L))
      .put("product_operation_order-edit_user-abroad-loan_all-add_wa_popup_0903", Range.closed(0L, 99999L))
      .put("product_operation_order-edit_user-abroad-loan_all-add_wa_page_0903", Range.closed(0L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-CREDIT_USAGE_RATE_EXPERIMENT", Range.closed(0L, 99999L))
      .put("long_nik-install-abroad-idle_event-Gold_new_test_0616", Range.closed(0L, 99999L))
      .put("long_nik-install-abroad-idle_event-gold_popup_V2_new", Range.closed(0L, 99999L))
      .put("pretii-other-abroad-loan_all-GOLDEN_CARD_CAN_BORROW_STYLE", Range.closed(36899L, 99999L))
      .put("pretii-other-abroad-loan_all-CreditReport_V1", Range.closed(0L, 99999L))
      .put("pretii-other-abroad-loan_all-CreditReport_PRICE", Range.closed(0L, 99999L))
      .put("pretii-other-abroad-loan_all-CreditReport_POPUP", Range.closed(0L, 99999L))
      .put("pretii-other-abroad-loan_all-CreditReport_POPUP_ME", Range.closed(0L, 99999L))
      .put("pretii-other-abroad-loan_all-bill_jbp_unpaid_guide_h5_1009", Range.closed(37900L, 99999L))
      .put("pretii-other-abroad-loan_all-bill_jbp_unpaid_guide_APP", Range.closed(37900L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-repayment_interest_popup", Range.closed(36900L, 99999L))
      .put("root_register-register-abroad-loan_all-mkt_authorization_check_box", Range.closed(0L, 99999L))
      .put("root_register-register-abroad-loan-bank_prom_1022", Range.closed(0L, 99999L))
      .put("root_register-register-abroad-loan_all-back_retention_v2_0922_copy", Range.closed(37400L, 99999L))
      .put("technology-auth-abroad-loan-h5_ui_new_auth_1215", Range.closed(35300L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-QUICK_ORDER_CASH_ACTIVITY_V2_EXPERIMENT", Range.closed(37300L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-COUPON_LONG_TERM_SUB_EXPERIMENT", Range.closed(0L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-ORDER_LOAN_PUSH_COUPON", Range.closed(36900L, 99999L))
      .put("technology-not_withdraw-abroad-loan-first_loan_coupon_parent_pro", Range.closed(0L, 99999L))
      .put("technology-lending-abroad-settle_reloan-market_coupon_grant_reloan", Range.closed(0L, 99999L))
      .put("technology-lending-abroad-settle_reloan-total_coupon_credit_limit_reloan", Range.closed(0L, 99999L))
      .put("product_operation_order-lending-abroad-loan-popup_window_increase_credit_decrease_interest_loan", Range.closed(37300L, 99999L))
      .put("product_operation_order-lending-abroad-settle_reloan-popup_window_increase_credit_decrease_interest_reloan",
          Range.closed(37300L, 99999L))
      .put("product_operation_order-not_withdraw-abroad-loan_all-CREDIT_INCREASED_SENSITIVITY_V2", Range.closed(38200L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-back_Quick_ordering_UI_V1_0714", Range.closed(0L, 99999L))
      .put("product_operation_auth-auth-abroad-loan-KTP_incentive_activities_v3_1_popup", Range.closed(0L, 99999L))
      .put("product_operation_auth-auth-abroad-loan-ktp_back_popup_v2_reopen", Range.closed(0L, 99999L))
      .put("product_operation_order-lending-abroad-loan_all-back_Quick_ordering_0627", Range.closed(0L, 99999L))
      .put("product_operation_auth-register-abroad-loan-login_page_benefits_V3_Register_0924_copy", Range.closed(37900L, 99999L))
      .put("product_operation_auth-register-abroad-loan-login_page_benefits_V3_login_0924_copy", Range.closed(37900L, 99999L))
      .put("root_register-register-abroad-loan-LOGIN_INPUT_DISPLAY_TYPE", Range.closed(0L, 99999L))
      .put("product_operation_auth-register-abroad-loan-REGISTER_ACTIVITY_FOR_H5", Range.closed(0L, 99999L))
      .put("product_operation_auth-register-abroad-loan-REGISTER_ACTIVITY_FOR_APP", Range.closed(0L, 99999L))
      .put("technology-other-abroad-idle_event-back_up_host_experiment", Range.closed(37900L, 99999L))
      .put("test_run-not_withdraw-abroad-loan_all-ORDER_GUIDE_ANIMATION_368", Range.closed(36800L, 99999L))
      .put("braavos-other-abroad-loan_all-loanmarketcanretry90", Range.closed(36799L, 99999L))
      .put("braavos-other-abroad-loan_all-loanmarketcannotretry90", Range.closed(36799L, 99999L))
      .put("technology-after_overdue-abroad-loan_all-billpagescreenshottocs_V2", Range.closed(38213L, 99999L))
      .put("bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_BRI_XENDIT2", Range.closed(37300L, 99999L))
      .put("reloan_order_26h1-lending-abroad-renew-one_click_loan_more_new_v1", Range.closed(37300L, 99999L))
      .put(ExperimentKeyConstants.RELOAN_ONE_CLICK_LOAN_MORE_STYLE_V1, Range.closed(37300L, 99999L))
      .put(ExperimentKeyConstants.FIRST_LOAN_BUTTON_LOAN_LIMIT_AMOUNT, Range.closed(36900L, 99999L))
      .put(ExperimentKeyConstants.FIRST_LOAN_HEAD_MIDDLE_REPAY_PLAN_COUPON, Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.FIRST_LOAN_REPAY_PLAN_POPUP_SUB_EXP.getKey(), Range.closed(36900L, 99999L))
      .put(UserFlowExperimentEnum.HOME_PAGE_SAFETY_MODULE.getKey(), Range.closed(38800L, 99999L))
      .put(UserFlowExperimentEnum.HOME_PAGE_SAFETY_MODULE_NOT_LOGIN.getKey(), Range.closed(38800L, 99999L))
      .put(UserFlowExperimentEnum.HOME_PAGE_SAFETY_MODULE_FIRST_LOAN.getKey(), Range.closed(38800L, 99999L))
      .put(UserFlowExperimentEnum.ORDER_PAGE_V5_FOR_FIRST_LOAN.getKey(), Range.closed(37300L, 99999L))
      .put(ExperimentKeyConstants.IOS_LANDING_PAGE_STYLE_FB, Range.closed(0L, 999999L))
      .put(ExperimentKeyConstants.IOS_LANDING_PAGE_STYLE_TT, Range.closed(0L, 999999L))
      .put(ExperimentKeyConstants.EC_SHOPEEPAY_DIRECT, Range.closed(0L, 999999L))
      .put(ExperimentKeyConstants.EC_OVO_TUIJIAN, Range.closed(0L, 999999L))
      .put(ExperimentKeyConstants.RELOAN_DEFAULT_AMOUNT, Range.closed(37300L, 999999L))
      .put(ExperimentKeyConstants.DANA_REPAY_DIRECT_DEBIT_KEY, Range.closed(39000L, 999999L))
      .put(ExperimentKeyConstants.OTP_ALTERNATIVE_VERIFICATION_ENTRY, Range.closed(39000L, 999999L))
      .put(ExperimentKeyConstants.OTP_VOICE_VERIFICATION_OPTION, Range.closed(39100L, 999999L))
      .put(ExperimentKeyConstants.EXP_KEY_SHOPEEPAY_BALANCE_FIRST, Range.closed(0L, 99999L))
      .put(FIRST_LOAN_COUPON_LONG_TERM_EXPERIMENT, Range.closed(0L, 99999L))
      .put(FORCE_MERGE_PRE_VERIFY_EXPERIMENT_KEY, Range.closed(36700L, 99999L))
      // TAPD-364257 回捞/重审非首笔放款 90 天管制期：Kafka 服务端分流，ExpUser.versionBuild=0
      .put(ExperimentKeyConstants.RETRIEVAL_REAPPLY_NON_FIRST_90D_LOCK, Range.closed(0L, 999999L))
      .put(UserFlowExperimentEnum.FIRST_LOAN_ORDER_RETAIN_POPUP_V2.getKey(), Range.openClosed(36900L, 99999L))
      .put(UserFlowExperimentEnum.BILL_PAGE_MERGE_REPAYMENT_V1.getKey(), Range.closed(38300L, 99999L))
      .put("first_loan_auth_26h1-auth-abroad-loan-auth_order_V5_sub4_0610", Range.closed(37413L, 99999L))
      // TAPD-356514 WA OTP 自动回填能力升级：下单首贷/复贷 + 注册登录 SDK 升级三实验共用版本门槛 ≥ 39200（NFR-004），
      // 可经 Zookeeper ab_test.exp_key_valid_build_map 动态覆盖；未登记的 key 会被 build 门禁拦截而永不生效。
      .put(ExperimentKeyConstants.FIRST_LOAN_WA_OTP_AUTOFILL, Range.closed(39200L, 999999L))
      .put(ExperimentKeyConstants.RELOAN_WA_OTP_AUTOFILL, Range.closed(39200L, 999999L))
      .put(ExperimentKeyConstants.REGISTER_WA_OTP_SDK, Range.closed(39200L, 999999L))
      // TAPD-1364147 首贷 T1 头中部回端加码发券实验：短期实验，Android > 36900（36901 起）；iOS 排除由 isVersionEligible 完成。
      .put(ExperimentKeyConstants.FIRST_LOAN_T1_HEAD_MIDDLE_MORE_DISCOUNT_0707, Range.closed(36901L, 99999L))
      .build();

  @PostConstruct
  private void registerConf() {
    ecSiteVars.registerConf(EXPERIMENT_NAME_CONFIG_MAP, new Conf<String>() {
    });
    ecSiteVars.registerConf(EXPERIMENT_NAME_CONFIG_MAP_V2, new Conf<String>() {
    });
    ecSiteVars.registerConf(APP_CONFIG_AB_START_BUILD, new Conf<Long>() {
    });
    ecSiteVars.registerConf(LANE_KEY_CONFIG_MAP, new Conf<String>() {
    });
    ecSiteVars.registerConf(MC_RESOLVE_AB_RESULT_TYPE, new Conf<String>() {
    });
    ecSiteVars.registerConf(APP_CONFIG_AB_MIN_VERSION, new Conf<Long>() {
    });
    ecSiteVars.registerConf(APP_CONFIG_AB_MAX_VERSION, new Conf<Long>() {
    });
  }

  @Getter
  private Set<String> apiChannelValidExpKeyAllowList;
  /**
   * 实验key对应生效的客户端版本号
   */
  @Getter
  private Map<String, Range<Long>> expKeyValidBuildMap;

  @Getter
  private Map<String, Range<Long>> generalConfigExpBuildMap;

  @Getter
  @Value("${ab_test.use_new_parsing_result:false}")
  private boolean useNewParsingResult;

  @Value("${ab_test.api_channel_valid_exp_key_allow_list:}")
  public void setApiChannelValidExpKeyAllowList(String configStr) {
    log.info("ABTestConfig.setApiChannelValidExpKeyAllowList:{}", configStr);
    try {
      if (StringUtils.isBlank(configStr)) {
        apiChannelValidExpKeyAllowList = new HashSet<>();
      } else {
        apiChannelValidExpKeyAllowList = Arrays.stream(configStr.split(",")).collect(Collectors.toSet());
      }
    } catch (Exception e) {
      log.error("ABTestConfig.setApiChannelValidExpKeyAllowList error", e);
    }
  }

  /**
   * 优先级 动态配置 > 静态配置，懒得配动态配置就写静态的DEFAULT_EXPERIMENT_BUILD_MAP
   * 动态配置解析失败不做任何事
   *
   * @param configStr
   */
  @Value("${ab_test.exp_key_valid_build_map:{}}")
  public void setExpKeyValidBuildMap(String configStr) {
    log.info("ABTestConfig.setExpKeyValidBuildMap:{}", configStr);
    try {
      Map<String, String> tmpConfigMap = JsonUtils.from(configStr, new TypeReference<Map<String, String>>() {
      });
      Map<String, Range<Long>> tmpValidMap = new HashMap<>(DEFAULT_EXPERIMENT_BUILD_MAP);
      if (MapUtils.isNotEmpty(tmpConfigMap)) {
        Map<String, Range<Long>> transformedMap = Maps.transformValues(tmpConfigMap, v -> {
          String[] range = StringUtils.split(v, ",");
          if (ArrayUtils.getLength(range) != 2) {
            return Range.closed(0L, 0L);
          }
          return Range.closed(Long.parseLong(range[0]), Long.parseLong(range[1]));
        });
        tmpValidMap.putAll(transformedMap);
      }
      tmpValidMap.putAll(buildLongTermExperimentVersionRangeMap());
      expKeyValidBuildMap = tmpValidMap;
    } catch (Exception e) {
      log.error("ABTestConfig.setExpKeyValidBuildMap error", e);
    }
  }

  @Value("${ab_test.general_config_exp_build_map:{}}")
  public void setGeneralConfigExpBuildMap(String configStr) {
    log.info("ABTestConfig.setGeneralConfigExpBuildMap:{}", configStr);
    try {
      Map<String, String> tmpConfigMap = JsonUtils.from(configStr, new TypeReference<Map<String, String>>() {
      });
      if (MapUtils.isNotEmpty(tmpConfigMap)) {
        generalConfigExpBuildMap = Maps.transformValues(tmpConfigMap, v -> {
          String[] range = StringUtils.split(v, ",");
          if (ArrayUtils.getLength(range) != 2) {
            return Range.closed(0L, 0L);
          }
          return Range.closed(Long.parseLong(range[0]), Long.parseLong(range[1]));
        });
      } else {
        generalConfigExpBuildMap = new HashMap<>();
      }
    } catch (Exception e) {
      log.error("ABTestConfig.setGeneralConfigExpBuildMap error", e);
      generalConfigExpBuildMap = new HashMap<>();
    }
  }

  /**
   * 长期实验的keyMap
   */
  private Map<String, Range<Long>> buildLongTermExperimentVersionRangeMap() {
    Map<String, Range<Long>> validMap = new HashMap<>();
    try {
      for (ExperimentNameSpace experimentNameSpace : LONG_TERM_EXPERIMENT_SET) {
        String experimentKey = getExperimentNameV2FromString(experimentNameSpace.name());
        if (experimentKey == null || experimentKey.isEmpty()) {
          experimentKey = getExperimentNameV1FromString(experimentNameSpace.name());
        }
        if (StringUtils.isBlank(experimentKey)) {
          log.error("experimentKey is blank, experimentKey: {} experimentNameSpace: {}", experimentKey, experimentNameSpace);
          continue;
        }
        // 优先读取配置中的版本号限制
        Long minVersion = getAppConfigAbMinVersionFromString(experimentNameSpace.name());
        Long maxVersion = getAppConfigAbMaxVersionFromString(experimentNameSpace.name());

        // 如果配置中没有设置，则使用默认值
        if (minVersion == -1L) {
          minVersion = experimentNameSpace.minVersion;
        }
        if (maxVersion == -1L) {
          maxVersion = experimentNameSpace.maxVersion;
        }
        validMap.put(experimentKey, Range.closed(minVersion, maxVersion));
      }
    } catch (Exception e) {
      log.error("ABTestConfig.buildLongTermExperimentVersionRangeMap error", e);
    }
    return validMap;
  }

  private String getExperimentNameV1FromString(String experimentNameSpace) {
    Map<String, String> experimentNameMapV1 = Optional.ofNullable(
        JsonUtils.from(ecSiteVars.getString(EXPERIMENT_NAME_CONFIG_MAP, "{}"), new TypeReference<Map<String, String>>() {
        })).orElse(new HashMap<>());
    return experimentNameMapV1.get(experimentNameSpace);
  }

  private String getExperimentNameV2FromString(String experimentNameSpace) {
    Map<String, String> experimentNameMapV2 = Optional.ofNullable(
        JsonUtils.from(ecSiteVars.getString(EXPERIMENT_NAME_CONFIG_MAP_V2, "{}"), new TypeReference<Map<String, String>>() {
        })).orElse(new HashMap<>());
    return experimentNameMapV2.get(experimentNameSpace);
  }

  private Long getAppConfigAbMinVersionFromString(String experimentNameSpace) {
    Map<String, Long> appConfigMinVersionMap = Optional.ofNullable(
        JsonUtils.from(ecSiteVars.getString(APP_CONFIG_AB_MIN_VERSION, "{}"), new TypeReference<Map<String, Long>>() {
        })).orElse(new HashMap<>());
    return Optional.ofNullable(appConfigMinVersionMap.get(experimentNameSpace)).orElse(-1L);
  }

  private Long getAppConfigAbMaxVersionFromString(String experimentNameSpace) {
    Map<String, Long> appConfigMaxVersionMap = Optional.ofNullable(
        JsonUtils.from(ecSiteVars.getString(APP_CONFIG_AB_MAX_VERSION, "{}"), new TypeReference<Map<String, Long>>() {
        })).orElse(new HashMap<>());
    return Optional.ofNullable(appConfigMaxVersionMap.get(experimentNameSpace)).orElse(-1L);
  }

  public Boolean isOpenChildLaneRelationHardcoded() {
    return ecSiteVars.getBoolean(CHILD_LANE_RELATION_HARDCODED_SWITCH, false);
  }

  public Set<String> getOldConsistencyMarkerList() {
    String configStr = ecSiteVars.getString(NOTIF_OLD_CONSISTENCY_MARKER, "");
    if (StringUtils.isBlank(configStr)) {
      return new HashSet<>();
    }
    return Arrays.stream(configStr.split(",")).collect(Collectors.toSet());
  }

  public Boolean getConsistencyMarkerAllowMurmurHash() {
    return ecSiteVars.getBoolean("notif.consistency_marker_allow_murmur_hash", false);
  }

  public List<ABTestSceneType> getListAbTestGenerateResult() {
    String configStr = ecSiteVars.getString(LIST_AB_TEST_GENERATE_RESULT, "[]");
    return JsonUtils.from(configStr, new TypeReference<List<ABTestSceneType>>() {
    });
  }

  public List<ABTestSceneType> getDeviceTokenAbTestGenerateResult() {
    String configStr = ecSiteVars.getString(DEVICE_TOKEN_AB_TEST_GENERATE_RESULT, "[]");
    return JsonUtils.from(configStr, new TypeReference<List<ABTestSceneType>>() {
    });
  }

  public boolean getAllowProductUpdateControlGroupScene() {
    return ecSiteVars.getBoolean("ab_test.allow_product_update_control_group_scene", false);
  }

  public boolean getControlGroupEffecitve() {
    return ecSiteVars.getBoolean(CONTROL_GROUP_EFFECTIVE, false);
  }

  public Long getRegisterTimeABMinId() {
    return ecSiteVars.getLong(REGISTER_TIME_AB_MIN_ID, 999999L);
  }

  public Boolean registerTimeABPushSwitch() {
    return ecSiteVars.getBoolean(REGISTER_TIME_AB_PUSH_SWITCH, true);
  }

  public Integer getResultSyncTaskNum() {
    return ecSiteVars.getInt(RESULT_SYNC_TASK_NUM, 1);
  }

  public Long getUpdateExperimentDelayTime() {
    return ecSiteVars.getInt(UPDATE_EXPERIMENT_DELAY_SECOND, 30) * 1000L;
  }

  public Long getAdminControlGroupEffecitveTimeStamp() {
    return ecSiteVars.getLong(ADMIN_CONTROL_GROUP_EFFECTIVE_TIMESTAMP, 1722445200000L);
  }

  public List<ABTestSceneType> getIgnoreControlGroupSceneTypeList() {
    String configStr = ecSiteVars.getString(IGNORE_CONTROL_GROUP_SCENE_TYPE_LIST, "[]");
    return JsonUtils.from(configStr, new TypeReference<List<ABTestSceneType>>() {
    });
  }

  public Set<Long> getAllowVersionConfigABTestIds() {
    String configStr = ecSiteVars.getString(ALLOW_VERSION_CONFIG_AB_TEST_IDS, "");
    if (StringUtils.isBlank(configStr)) {
      return new HashSet<>();
    }
    return Arrays.stream(configStr.split(",")).map(Long::valueOf).collect(Collectors.toSet());
  }

  public Map<String, Long> getVersionConfigVersionInfoDescMapping() {
    String configStr = ecSiteVars.getString(VERSION_CONFIG_VERSION_INFO_DESC_MAPPING, "");
    if (StringUtils.isBlank(configStr)) {
      return new HashMap<>();
    }
    return JsonUtils.from(configStr, new TypeReference<Map<String, Long>>() {
    });
  }

  public Long getH2ControlGroupExperimentId() {
    return ecSiteVars.getLong(H2_CONTROL_GROUP_EXPERIMENT_ID, 310L);
  }

  public Long getH2ControlGroup2OnlineTime() {
    return ecSiteVars.getLong(H2_CONTROL_GROUP_2_ONLINE_TIME, Long.MAX_VALUE);
  }

  public Long getH2ControlGroupExperimentGroupId() {
    return ecSiteVars.getLong(H2_CONTROL_GROUP_EXPERIMENT_GROUP_ID, 644L);
  }

  public Long getH2ControlGroupExperimentControlGroup2Id() {
    return ecSiteVars.getLong(H2_CONTROL_GROUP_EXPERIMENT_CONTROL_GROUP_2_ID, 0L);
  }

  public String getExperimentPlatformToken() {
    return ecSiteVars.getString(EXPERIMENT_PLATFORM_TOKEN, "7A559D557F2DAA72");
  }

  public Map<ABTestSceneType, List<ABTestSceneType>> getChildLineToSubSceneListMap() {
    String configStr = ecSiteVars.getString(CHILD_LINE_TO_SUB_SCENE_LIST_MAP, "");
    if (StringUtils.isBlank(configStr)) {
      return new HashMap<>();
    }
    return JsonUtils.from(configStr, new TypeReference<Map<ABTestSceneType, List<ABTestSceneType>>>() {
    });
  }

  public List<ABTestSceneType> getFetchResultNoCheckLaneBusinessSceneList() {
    String configStr = ecSiteVars.getString(FETCH_RESULT_NO_CHECK_LANE_BUSINESS_SCENE_LIST, "");
    if (StringUtils.isBlank(configStr)) {
      return new ArrayList<>();
    }
    return JsonUtils.from(configStr, new TypeReference<List<ABTestSceneType>>() {
    });
  }

  public String getRegisterAbBackfillConfig() {
    return ecSiteVars.getString(REGISTER_AB_BACK_FILL_CONFIG, "[]");
  }

  public String getOrderAbBackfillConfig() {
    return ecSiteVars.getString(ORDER_AB_BACK_FILL_CONFIG, "[]");
  }

  public boolean getDeviceTokenUseExperimentPlatform() {
    return ecSiteVars.getBoolean(DEVICE_TOKEN_USE_EXPERIMENT_PLATFORM, false);
  }

  @Deprecated
  public String getExperimentName(String experimentNameSpace) {
    return ecSiteVars.getConfVal(EXPERIMENT_NAME_CONFIG_MAP, experimentNameSpace, "");
  }

  public String getExperimentNameV2(String experimentNameSpace) {
    return ecSiteVars.getConfVal(EXPERIMENT_NAME_CONFIG_MAP_V2, experimentNameSpace, "");
  }


  public Long getAppConfigAbStartBuild(ABTestSceneType sceneType) {
    return ecSiteVars.getConfVal(APP_CONFIG_AB_START_BUILD, sceneType.name(), -1L);
  }

  public Long getAppConfigAbMinVersion(String experimentNameSpace) {
    return ecSiteVars.getConfVal(APP_CONFIG_AB_MIN_VERSION, experimentNameSpace, -1L);
  }

  public Long getAppConfigAbMaxVersion(String experimentNameSpace) {
    return ecSiteVars.getConfVal(APP_CONFIG_AB_MAX_VERSION, experimentNameSpace, -1L);
  }

  public Boolean useExperimentPlatformV2(String sceneType) {
    String configStr = ecSiteVars.getString(EXPERIMENT_PLATFORM_V2_KEY, "");
    if (StringUtils.isBlank(configStr)) {
      return false;
    }
    List<String> abTestSceneTypes = JsonUtils.fromOrException(configStr, new TypeReference<List<String>>() {
    });
    return abTestSceneTypes.contains(sceneType);
  }


  /**
   * api渠道是否需要走ab实验
   */
  public Boolean apiChannelNeedAbtest(String experimentNameSpace) {
    return Optional.ofNullable(ecSiteVars.getString(API_CHANNEL_NEED_ABTEST_KEY, ""))
        .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
        .map(config -> JsonUtils.fromOrException(config, new TypeReference<List<String>>() {
        }))
        .map(types -> types.contains(experimentNameSpace))
        .orElse(false);
  }


  public String getLaneKey(String laneName) {
    return ecSiteVars.getConfVal(LANE_KEY_CONFIG_MAP, laneName, "");
  }

  /**
   * 是否启用请求级缓存读取：
   * - false（默认）：不读缓存，仍会写入缓存并返回实时结果，同时可用于监控缓存一致性（dry-run 阶段）。
   * - true：读缓存（命中直接返回；未命中调用下游并写入）。
   */
  public boolean isUseRequestCacheForAbtest() {
    return ecSiteVars.getBoolean(USE_REQUEST_CACHE_FOR_ABTEST, false);
  }

  /**
   * 根据实验key反向查找对应的实验枚举
   * 从EXPERIMENT_NAME_CONFIG_MAP查找
   *
   * @param experimentKey 实验key
   * @return 实验枚举，如果找不到返回null
   */
  @Nullable
  public ABTestSceneType getExperimentNameSpaceByKey(String experimentKey) {
    if (StringUtils.isBlank(experimentKey)) {
      return null;
    }

    String confStr = ecSiteVars.getString(EXPERIMENT_NAME_CONFIG_MAP);
    Map<String, String> configMap = JsonUtils.from(confStr, new TypeReference<Map<String, String>>() {
    });
    if (MapUtils.isEmpty(configMap)) {
      return null;
    }
    for (Map.Entry<String, String> entry : configMap.entrySet()) {
      if (experimentKey.equals(entry.getValue())) {
        try {
          log.info("use exp V1, scene type : {}", entry.getValue());
          return ABTestSceneType.valueOf(entry.getKey());
        } catch (Exception e) {
          log.warn("unknown experiment map, key:{}, value:{}", entry.getKey(), entry.getValue(), e);
          return null;
        }
      }
    }
    log.info("unknown experiment, experiment key:{}", experimentKey);
    return null;
  }

  public ResultGetType getAppResourceAbGetType(String abName) {
    String res = ecSiteVars.getConfVal(MC_RESOLVE_AB_RESULT_TYPE, abName, null);
    return StringUtils.isBlank(res) ? null : ResultGetType.valueOf(res);
  }

  public boolean isInMcResolveAbResultWhitelist(String abName) {
    if (StringUtils.isBlank(abName)) {
      return false;
    }
    String configStr = ecSiteVars.getString(MC_RESOLVE_AB_RESULT_WHITELIST, "");
    if (StringUtils.isBlank(configStr)) {
      return false;
    }
    List<String> whitelist = JsonUtils.from(configStr, new TypeReference<List<String>>() {
    });
    return whitelist != null && whitelist.contains(abName);
  }

  public SourceTypeResolveMode getSourceTypeResolveMode() {
    String value = ecSiteVars.getString(SOURCE_TYPE_RESOLVE_MODE, "SHADOW");
    return SourceTypeResolveMode.fromStringOrDefault(value);
  }

  /**
   * LiveDemo 强制实验结果映射：expKey → groupResult 字面量。默认空 Map。
   * 配置非法 JSON 时返回空 Map，避免阻断实验主路径。
   */
  public Map<String, String> getLiveDemoForceResultMap() {
    try {
      String configStr = ecSiteVars.getString(LIVE_DEMO_FORCE_RESULT_MAP, "{}");
      if (StringUtils.isBlank(configStr)) {
        return new HashMap<>();
      }
      Map<String, String> map = JsonUtils.from(configStr, new TypeReference<Map<String, String>>() {
      });
      return map == null ? new HashMap<>() : map;
    } catch (Exception e) {
      log.error("ABTestConfig.getLiveDemoForceResultMap parse error, return empty map", e);
      return new HashMap<>();
    }
  }
}
