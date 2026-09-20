package com.yqg.core.service.cashloan;

import static com.yqg.core.service.cashloan.enums.HitConsistentHashPrefix.HOME_API_DIFF;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.util.Lists;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.sql.signature.enums.SignatureProvider;
import com.yqg.core.service.abtest.enums.ABTestGroupType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.vo.CreateOrderSignFormatConfigVO;
import com.yqg.core.service.cashloan.vo.HomeInterestFreeCardRewardVO;
import com.yqg.core.service.cashloan.vo.ReloanRejectedBubbleVO;
import com.yqg.core.util.EcHashUtil;
import com.yqg.core.util.validator.PercentConfig;
import com.yqg.ec.common.configuration.Conf;
import com.yqg.ec.common.configuration.ISiteVars;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import com.yqg.core.service.cashloan.vo.HeadMiddleCouponConfig;
import com.yqg.ec.common.serialization.JsonUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HomepageV5Config {
  private static final String PREFIX = "homepage_v5_config.";

  private static final String CREDITS_TITLE = PREFIX + "credits_title";
  private static final String CREDITS_TIP = PREFIX + "credits_tip";
  private static final String PRODUCT_RATE_TITLE = PREFIX + "product_rate_title";
  private static final String PRODUCT_RATE_CONTENT = PREFIX + "product_rate_content";
  private static final String PRODUCT_RATE_CONTENT_FOR_WHITE_DEVICE = PREFIX + "product_rate_content_for_white_device";
  private static final String PRODUCT_PERIOD_TITLE = PREFIX + "product_period_title";
  private static final String PRODUCT_PERIOD_CONTENT_V2 = PREFIX + "product_period_content_v2";
  private static final String LY_PRODUCT_PERIOD_CONTENT = PREFIX + "ly_product_period_content";
  private static final String RISK_IN_REVIEW_CONTENT = PREFIX + "risk_in_review_content";
  private static final String AB_TEST_HOME_STRATEGY_V3 = PREFIX + "ab_test_home_strategy_v3";
  private static final String AB_TEST_PRODUCT_AMOUNT_INPUT = PREFIX + "ab_test_product_amount_input";
  private static final String PRODUCT_AMOUNT_QUICK_INPUT_RATIO = PREFIX + "product_amount_quick_input_ratio";
  private static final String AGREEMENT_WEB_URL_PRE_FIX = PREFIX + "agreement_web_url_pre_fix";
  private static final String AGREEMENT_KEY = PREFIX + "agreement_key";
  private static final String CARD_BUTTON_NAME = PREFIX + "button_name";
  private static final String LOW_RATE_AND_30_DAYS_POPUP_STRATEGY = PREFIX + "low_rate_and_30_days_popup_strategy";
  private static final String MULTI_LOAN_GUIDE_REDIRECT_URL = PREFIX + "multi_loan_guide_redirect_url";
  private static final String DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_START_VERSION = PREFIX + "discount_detail_top_area_vip_ui_display_strategy_start_version";
  private static final String DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_START_VERSION_V3 = PREFIX + "discount_detail_top_area_vip_ui_display_strategy_start_version_v3";
  private static final String BUTTON_ANIMATION_EFFECT_PERCENT = PREFIX + "button_animation_effect_percent";
  private static final String DISCOUNT_DETAIL_TITLE = PREFIX + "discount_detail_title";
  private static final String FEE_CALCULATION_DETAIL_TITLE = PREFIX + "fee_calculation_detail_title";
  private static final String FEE_CALCULATION_DETAIL_URL = PREFIX + "fee_calculation_detail_url";
  private static final String FEE_CALCULATION_DETAIL_URL_FOR_API_CHANNEL = PREFIX + "fee_calculation_detail_url_for_api_channel";
  private static final String MONTH_PRODUCT_PROMPT = PREFIX + "month_product_prompt";
  private static final String REDUCE_CREDITS_CONTENT = PREFIX + "reduce_credits_content";
  private static final String REDUCE_CREDITS_DETAIL_TITLE = PREFIX + "reduce_credits_detail_title";
  private static final String REDUCE_CREDITS_DETAIL_CONTENT = PREFIX + "reduce_credits_detail_content";
  private static final String REDUCE_CREDITS_REDIRECT_URL = PREFIX + "reduce_credits_redirect_url";
  private static final String REDUCE_CREDITS_INPUT_LOAN_AMOUNT = PREFIX + "reduce_credits_input_loan_amount";
  private static final String REDUCE_CREDITS_SCROLL_LOAN_AMOUNT = PREFIX + "reduce_credits_scroll_loan_amount";
  private static final String PAYING_STATUS_CONTENT = PREFIX + "paying_status_content";
  private static final String VIRTUAL_INCREASE_CREDITS_CONTENT = PREFIX + "virtual_increase_credits_content";
  private static final String VIRTUAL_INCREASE_CREDITS_CONTENT_LAST_HOUR = PREFIX + "virtual_increase_credits_content_last_hour";
  private static final String FROM_REGISTER_TO_AUTH_FINISH_TIME_THRESHOLD = PREFIX + "from_register_to_auth_finish_time_threshold";
  private static final String VIRTUAL_INCREASE_CREDITS_PERCENT = PREFIX + "virtual_increase_credits_percent";
  private static final String TEMP_CREDITS_EXPIRED_DAY_THRESHOLD = PREFIX + "temp_credits_expired_day_threshold";


  private static final String INCREASE_CREDITS_ENTRANCE = PREFIX + "increase_credits_entrance";
  private static final String INCREASE_CREDITS_ENTRANCE_URL = PREFIX + "increase_credits_entrance_url";
  private static final String INCREASE_CREDITS_REVIEW_SWITCH = PREFIX + "increase_credits_review_switch";
  private static final String INCREASE_CREDITS_REVIEW_HOME_BUTTON_TITLE = PREFIX + "increase_credits_review_home_button_title";
  private static final String INCREASE_CREDITS_REVIEWING_HOME_BUTTON_TITLE = PREFIX + "increase_credits_reviewing_home_button_title";
  private static final String INCREASE_CREDITS_REVIEW_HOME_BUTTON_NAME = PREFIX + "increase_credits_review_home_button_name";
  private static final String INCREASE_CREDITS_REVIEW_HOME_CONTENT = PREFIX + "increase_credits_review_home_content";
  private static final String INCREASE_CREDITS_REVIEW_HOME_BUTTON_JUMP_URL = PREFIX + "increase_credits_review_home_button_jump_url";
  private static final String INCREASE_CREDITS_REVIEW_HOME_BUTTON_SKIPPABLE_URL = PREFIX + "increase_credits_review_home_button_skippable_url";
  private static final String INCREASE_CREDITS_REVIEW_REJECTED_BUTTON_NAME = PREFIX + "increase_credits_review_rejected_button_name";
  private static final String INCREASE_CREDITS_REVIEW_REJECTED_CONTENT = PREFIX + "increase_credits_review_rejected_content";
  private static final String INCREASE_CREDITS_REVIEWING_DISPLAY_CONTENT = PREFIX + "increase_credits_reviewing_display_content";
  private static final String INCREASE_CREDITS_REVIEW_FOR_PASSED_QUITE_PERIOD_USER_BUILD = PREFIX + "increase_credits_review_for_passed_quite_period_user_build";


  private static final String MINIMALIST_PROCESS_USER_SUBMIT_RISK_FROM_INIT_TO_FINISH_THRESHOLD_TIME = PREFIX + "minimalist_process_user_submit_risk_from_init_to_finish_threshold_time";
  private static final String MINIMALIST_PROCESS_USER_INCREASE_CREDITS_FRONT_CONTENT = PREFIX + "minimalist_process_user_increase_credits_front_content";
  private static final String MINIMALIST_PROCESS_USER_INCREASE_CREDITS_BACKEND_CONTENT = PREFIX + "minimalist_process_user_increase_credits_backend_content";
  private static final String MINIMALIST_PROCESS_USER_INCREASE_CREDITS_ICON_URL = PREFIX + "minimalist_process_user_increase_credits_icon_url";
  private static final String MINIMALIST_PROCESS_USER_ACCEPT_TOOL_BAR_COLOR = PREFIX + "minimalist_process_user_accept_tool_bar_color";
  private static final String MINIMALIST_PROCESS_USER_ACCEPT_BACKGROUND_IMAGE_URL = PREFIX + "minimalist_process_user_accept_background_image_url";

  private static final String REGISTER_USER_NOT_ENOUGH_RATE_TITLE_TEXT_BY_PLATFORM_TYPE = PREFIX + "register_user_not_enough_rate_title_text_by_platform_type";
  private static final String REGISTER_USER_NOT_ENOUGH_RATE_TEXT_BY_PLATFORM_TYPE = PREFIX + "register_user_not_enough_rate_text_by_platform_type";
  private static final String REGISTER_USER_NOT_ENOUGH_PERIOD_TEXT_BY_PLATFORM_TYPE = PREFIX + "register_user_not_enough_period_text_by_platform_type";

  private static final String DISCOUNT_DETAIL_ANIMATION_BUILD = PREFIX + "discount_detail_animation_build";
  private static final String DISCOUNT_DETAIL_ANIMATION_V3_BUILD = PREFIX + "discount_detail_animation_v3_build";
  private static final String DISCOUNT_DETAIL_ANIMATION_TOP_AREA_IMAGE_URL = PREFIX + "discount_detail_animation_top_area_image_url";
  private static final String DISCOUNT_DETAIL_ANIMATION_TOP_AREA_TOOL_BAR_COLOR = PREFIX + "discount_detail_animation_top_area_tool_bar_color";
  private static final String DISCOUNT_DETAIL_ANIMATION_TOP_AREA_BOLD_TEXT = PREFIX + "discount_detail_animation_top_area_bold_text";
  private static final String DISCOUNT_DETAIL_ANIMATION_TOP_AREA_NORMAL_TEXT = PREFIX + "discount_detail_animation_top_area_normal_text";

  private static final String CUT_INTEREST_COUPON_TOP_AREA_IMAGE_URL = PREFIX + "cut_interest_coupon_top_area_image_url";
  private static final String CUT_INTEREST_COUPON_TOP_AREA_TOOL_BAR_COLOR = PREFIX + "cut_interest_coupon_top_area_tool_bar_color";
  private static final String CUT_INTEREST_COUPON_TOP_AREA_BOLD_TEXT = PREFIX + "cut_interest_top_coupon_area_bold_text";
  private static final String CUT_INTEREST_COUPON_TOP_AREA_NORMAL_TEXT = PREFIX + "cut_interest_top_coupon_area_normal_text";
  private static final String SUBMIT_ADDITIONAL_INFO_BUBBLE_CONTENT_DISPLAY_VERSION = PREFIX + "submit_additional_info_bubble_content_display_version";

  /**
   * 极简用户首页优化，背景图片地址配置
   */
  private static final String MINIMALIST_PROCESS_USER_ACCEPT_BACKGROUND_IMAGE_URL_V2 = PREFIX + "minimalist_process_user_accept_background_image_url_v2";
  private static final String MINIMALIST_PROCESS_USER_VERSION = PREFIX + "minimalist_process_user_version";
  public static final String LOAN_INFO_NO_INTEREST_EXPERIMENT_RATE = PREFIX + "no_interest_experiment_rate";
  /**
   * 极简用户首页副标题文案
   */
  private static final String MINIMALIST_PROCESS_USER_SUB_CONTENT_INFO = PREFIX + "minimalist_process_user_sub_content_info";
  /**
   * 极简用户首页主标题文案
   */
  private static final String MINIMALIST_PROCESS_USER_TOP_CONTENT_PREFIX_INFO = PREFIX + "minimalist_process_user_top_content_info";
  /**
   * 极简用户新版本文本生效卡下版本号
   */
  private static final String MINIMALIST_PROCESS_USER_NEW_CONTENT_INFO_VERSION = PREFIX + "minimalist_process_user_new_content_info_version";

  private static final String DISPLAY_COUPON_TIPS_STRATEGY_START_VERSION = PREFIX + "display_coupon_tips_strategy_start_version";

  private static final String ORDER_PAGE_INPUT_AMOUNT_AND_TIP_DISPLAY_STRATEGY_START_BUILD = PREFIX + "order_page_input_amount_and_tip_display_strategy_start_build";

  private static final String INCREASE_CREDITS_JUMP_URL = PREFIX + "increase_credits_jump_url";
  private static final String INCREASE_CREDITS_CONTENT = PREFIX + "increase_credits_content";

  private static final String NEED_ORDER_PAGE_USER_IDLE_POPUP_START_VERSION = PREFIX + "need_order_page_user_idle_popup_start_version";
  private static final String NEED_ORDER_PAGE_USER_IDLE_POPUP_V2_START_VERSION = PREFIX + "need_order_page_user_idle_popup_v2_start_version";
  private static final String ORDER_PAGE_USER_IDLE_POPUP_COUPON_HINT = PREFIX + "order_page_user_idle_popup_coupon_hint";

  private static final String APP_REFRESH_POPUP_WINDOW_INTERVAL_MILLISECOND_NEW = PREFIX + "app_refresh_popup_window_interval_millisecond_new";
  private static final String APP_REFRESH_POPUP_WINDOW_INTERVAL_MILLISECOND_OLD = PREFIX + "app_refresh_popup_window_interval_millisecond_old";
  private static final String APP_REFRESH_POPUP_WINDOW_NEW_START_VERSION = PREFIX + "app_refresh_popup_window_new_start_version";

  private static final String ANTI_FRAUD_CAROUSEL_TEXT = PREFIX + "anti_fraud_carousel_text";
  private static final String CREDITS_DECREASE_QUICK_ORDER_PAGE_TITLE = PREFIX + "quick_order_page_title";
  private static final String CREDITS_DECREASE_QUICK_ORDER_CREDIT_UPDATE_CONTENT = PREFIX + "quick_order_credit_update_content";
  private static final String CREDITS_DECREASE_QUICK_ORDER_TIPS_CONTENT_SIGN_BEFORE_RISK = PREFIX + "credits_decrease_quick_order_tips_content_sign_before_risk";
  private static final String CREDITS_DECREASE_QUICK_ORDER_TIPS_CONTENT_SIGN_AFTER_RISK = PREFIX + "credits_decrease_quick_order_tips_content_sign_after_risk";
  private static final String CREDITS_DECREASE_QUICK_ORDER_INCREASE_CREDITS_CONTENT = PREFIX + "credits_decrease_quick_order_increase_credits_content";
  private static final String CREDITS_DECREASE_QUICK_ORDER_INCREASE_CREDITS_URL = PREFIX + "credits_decrease_quick_order_increase_credits_url";
  private static final String LARGE_INSTALMENT_AMOUNT_OWNED_BUILD = PREFIX + "large_instalment_amount_owned_build";
  private static final String ACTIVITY_ORDER_INFO_START_VERSION = PREFIX + "activity_order_info_start_version";
  private static final String OPEN_LOAN_ACTIVITY_ORDER = PREFIX + "open_loan_activity_order";
  private static final String ACTIVITY_ORDER_AGREEMENT_CONTENT = PREFIX + "activity_order_agreement_content";
  private static final String ACTIVITY_ORDER_AGREEMENT_LINK = PREFIX + "activity_order_agreement_link";
  private static final String ACTIVITY_CONTENT_LINK = PREFIX + "activity_content_link";
  private static final String ACTIVITY_CONTENT = PREFIX + "activity_content";
  private static final String ACTIVITY_HIGHLIGHT_MAP = PREFIX + "activity_highlight_map";
  private static final String ACTIVITY_AWARD_AMOUNT = PREFIX + "activity_award_amount";
  private static final String ACTIVITY_ORDER_DAYS = PREFIX + "activity_order_days";
  private static final String OPEN_MULTI_LOAN_CREDITS_DECREASE_QUICK_ORDER = PREFIX + "open_multi_loan_credits_decrease_quick_order";
  private static final String ORDER_PAGE_BACK_BUTTON_BUILD = PREFIX + "order_page_back_button_build";
  private static final String MAX_TERMS_INTEREST_RATE = PREFIX + "max_terms_interest_rate";

  private static final String VIP_PAGE_MAX_DISCOUNT_AMOUNT_TEXT = PREFIX + "vip_page_max_discount_amount_text";
  private static final String VIP_PAGE_MIN_ACTUAL_INTEREST_RATE_TEXT = PREFIX + "vip_page_min_actual_interest_rate_text";
  private static final String VIP_PAGE_CONTENT = PREFIX + "vip_page_content";
  private static final String VIP_PAGE_BUTTON_TEXT = PREFIX + "vip_page_button_text";
  private static final String VIP_PAGE_BACKGROUND_PHOTO_URL = PREFIX + "vip_page_background_photo_url";
  private static final String VIP_PAGE_ICON_LIST = PREFIX + "vip_page_icon_list";
  private static final String VIP_PAGE_TITLE = PREFIX + "vip_page_title";
  private static final String DISCOUNT_DETAIL_ANIMATION_TOP_AREA_BOLD_TEXT_FOR_VIP_TEXT = PREFIX + "discount_detail_animation_top_area_bold_text_for_vip_text";
  private static final String DISCOUNT_DETAIL_ANIMATION_TOP_AREA_TOOL_BAR_COLOR_FOR_VIP = PREFIX + "discount_detail_animation_top_area_tool_bar_color_for_vip";
  private static final String DISCOUNT_DETAIL_ANIMATION_TOP_AREA_IMAGE_URL_FOR_VIP = PREFIX + "discount_detail_animation_top_area_image_url_for_vip";
  private static final String VIP_PAGE_HOME_TITLE = PREFIX + "vip_page_home_title";
  private static final String VIP_PAGE_HOME_CONTENT = PREFIX + "vip_page_home_content";
  private static final String VIP_PAGE_TIMEOUT = PREFIX + "vip_page_timeout";
  private static final String DISCOUNT_DETAIL_ANIMATION_TOP_AREA_NORMAL_TEXT_FOR_VIP_NO_LOW_INTEREST_TEXT = PREFIX + "discount_detail_animation_top_area_normal_text_for_vip_no_low_interest_text";
  private static final String CREDITS_COUPON_ACTIVITY_ID_LIST = PREFIX + "credits_coupon_activity_id_list";
  private static final String REPAY_EDUCATION_ACTIVITY_ID_LIST = PREFIX + "repay_education_activity_id_list";
  private static final String REPAY_EDUCATION_STATIC_TEXT_CONFIG = PREFIX + "repay_education_static_text_config";
  private static final String DEFAULT_REPAY_EDUCATION_STATIC_TEXT_CONFIG =
      "{"
          + "\"homepage\":{\"title\":\"Bayar Tepat Waktu & Dapatkan\"},"
          + "\"banner\":{"
          + "\"firstLineTextWhenNoProgress\":\"Bayar Tepat Waktu {{%targetCount%x}} & Dapatkan\","
          + "\"firstLineText\":\"Bayar Tepat Waktu {{%leftTimes%x}} Lagi & Dapatkan\","
          + "\"secondLineText\":\"\","
          + "\"benefit1Desc\":\"Paket Diskon\","
          + "\"benefit1Info\":\"1 JUTA\","
          + "\"benefit2Desc\":\"Naik Limit\","
          + "\"benefit2Info\":\"500 RB\""
          + "},"
          + "\"iconPopup\":{"
          + "\"firstLineText\":\"Bayar Tepat Waktu\","
          + "\"secondLineText\":\"%targetCount%x & Dapatkan\","
          + "\"benefit1Desc\":\"Paket Diskon\","
          + "\"benefit1Info\":\"1 JUTA\","
          + "\"benefit2Desc\":\"Naik Limit\","
          + "\"benefit2Info\":\"500 RB\""
          + "},"
          + "\"rewardPopup\":{"
          + "\"firstLineText\":\"Selamat!\","
          + "\"secondLineText\":\"Anda Sudah Menerima\","
          + "\"benefit1Desc\":\"Paket Diskon\","
          + "\"benefit1Info\":\"1 JUTA\","
          + "\"benefit1BottomInfo\":\"Diskon Bunga 5%\","
          + "\"benefit2Desc\":\"Limit Saat Ini\","
          + "\"benefit2Info\":\"100 RB\""
          + "}"
          + "}";
  private static final String CREDITS_COUPON_ACTIVITY_DISPLAY_AMOUNT_PERCENT = PREFIX + "credits_coupon_activity_display_amount_percent";
  private static final String CUT_INTEREST_BLANK_AB_TEST_SWITCH = PREFIX + "cut_interest_blank_ab_test_switch";
  private static final String CUT_INTEREST_BLANK_AB_TEST_EXECUTE_SWITCH = PREFIX + "cut_interest_blank_ab_test_execute_switch";
  private static final String VIP_PAGE_MIN_ACTUAL_INTEREST_RATE_FOR_DAILY_UNIT = PREFIX + "vip_page_min_actual_interest_rate_for_daily_unit";
  private static final String NEW_HOMEPAGE_START_BUILD = PREFIX + "new_homepage_start_build";
  private static final String NEW_HOMEPAGE_START_BUILD_WEB = PREFIX + "new_homepage_start_build_web";
  private static final String NEED_ORDER_PAGE_USER_IDLE_POPUP_V3_START_VERSION = PREFIX + "need_order_page_user_idle_popup_v3_start_version";
  private static final String VIP_NORMAL_TEXT_FOR_VIP_NOT_CAN_ORDER = PREFIX + "vip_normal_text_for_vip_not_can_order";
  private static final String NEW_HOME_PAGE_UI_FOR_NOT_LOGIN = PREFIX + "new_home_page_ui_for_not_login";
  private static final String CREATE_ORDER_PAGE_STYLE_START_BUILD = PREFIX + "create_order_page_style_start_build";
  private static final String NEED_UPLOAD_POINT = PREFIX + "need_upload_point";
  private static final String DISCOUNT_AREA_OLD_VERSION = PREFIX + "discount_area_old_version";
  private static final String CUT_INTEREST_POPUP_START_VERSION = PREFIX + "cut_interest_popup_start_version";
  private static final String GENERAL_RULE_BY_DB = PREFIX + "general_rule_by_db";
  private static final String HOME_V5_API_DIFF = PREFIX + "home_v5_api_diff";
  private static final String HOME_V5_GET_RESPONSE_STATUS = PREFIX + "home_v5_get_response_status";

  private static final String HOME_V5_GET_TOP_AREA_RESPONSE_STATUS = PREFIX + "home_v5_get_top_area_response_status";
  private static final String HOME_V5_CREATE_ORDER_SIGN_FORMAT_VERSION = PREFIX + "create_order_sign_format_version";
  private static final String HOME_V5_CREATE_ORDER_SIGN_FORMAT_MAP = PREFIX + "create_order_sign_format_config_map";
  private static final String HOME_V5_CREATE_ORDER_SIGN_FORMAT_MAP_FOR_API_CHANNEL =
      PREFIX + "create_order_sign_format_config_map_for_api_channel";
  private static final String HOME_V5_DIALOG_SIGN_FORMAT_CONFIG_MAP = PREFIX + "dialog_sign_format_config_map";
  private static final String HOME_V5_DIALOG_SIGN_FORMAT_CONFIG_MAP_FOR_API_CHANNEL =
      PREFIX + "dialog_sign_format_config_map_for_api_channel";
  private static final String MONTHLY_INCOME_SIGN_TEXT = PREFIX + "monthly_income_sign_text";
  private static final String MONTHLY_INCOME_SIGN_TEXT_DEFAULT =
      "Saya menyatakan pendapatan bulanan > Rp3.000.000 dan pendapatan yang dapat digunakan"
      + " > 3× total angsuran bulanan seluruh pinjaman berjalan.";
  private static final String RELOAN_REJECTED_BUBBLED_DETAIL = PREFIX + "reloan_rejected_bubble_detail";

  private static final String RESERVE_LOAN_TITLE = PREFIX + "reserve_loan_title";
  private static final String RESERVE_LOAN_URL = PREFIX + "reserve_loan_url";
  /**
   * 下单详情页利息->综合息费版本控制
   */
  private static final String HOME_V5_PRODUCT_DETAIL_INTEREST_CHANGE_START_BUILD = PREFIX + "home_v5_product_detail_interest_change_start_build";
  /**
   * 下单详情页利息->h5综合息费新文案开关
   */
  private static final String HOME_V5_PRODUCT_DETAIL_INTEREST_CHANGE_SWITCH_FOR_H5 = PREFIX + "home_v5_product_detail_interest_change_switch_for_h5";
  private static final String HOME_V5_GET_RESPONSE_STATUS_WHITE_LIST = PREFIX + "home_v5_get_response_status_white_list";

  /**
   * Reject New Homepage, Other Loan Apps Configs
   */
  private static final String HOME_V5_LOAN_MARKET_REJECT_NEW_HOMEPAGE_WHITELIST = PREFIX + "loan_market_reject_new_homepage_whitelist";
  private static final String HOME_V5_LOAN_MARKET_REJECT_NEW_HOMEPAGE_TITLE = PREFIX + "loan_market_reject_new_homepage_title";
  private static final String HOME_V5_LOAN_MARKET_REJECT_NEW_HOMEPAGE_CONTENT = PREFIX + "loan_market_reject_new_homepage_content";
  private static final String HOME_V5_LOAN_MARKET_REJECT_NEW_HOMEPAGE_BUTTON_TITLE = PREFIX + "loan_market_reject_new_homepage_button_title";
  private static final String HOME_V5_LOAN_MARKET_REJECT_NEW_JUMP_URL = PREFIX + "loan_market_reject_new_jump_url";
  private static final String HOME_V5_QUOTE_MANUAL_REVIEW_CONTENT = PREFIX + "quote_manual_review_content";
  private static final String HOME_V5_QUOTE_AUTO_REVIEW_TIME = PREFIX + "quote_auto_review_time";
  private static final String HOME_V5_REJECT_HOME_DISPLAY_STRATEGY_START_VERSION = PREFIX + "reject_home_display_strategy_start_version";
  private static final String HOME_V5_CANCEL_LOAN_TIPS_POP_UP = PREFIX + "cancel_loan_tips_pop_up";
  private static final String HOME_V5_APPLY_FOR_FUTURE_TIPS_POP_UP = PREFIX + "apply_for_future_tips_pop_up";
  private static final String HOME_V5_REAPPLY_PAGE_TIPS = PREFIX + "reapply_page_tips";
  private static final String HOME_V5_REAPPLY_PAGE_ICON = PREFIX + "reapply_page_icon";
  private static final String HOME_V5_REPAYMENT_DISPLAY_STRATEGY_BUILD = PREFIX + "repayment_display_strategy_build";
  private static final String HOME_V5_OVERDUE_LOAN_TIPS_POP_UP = PREFIX + "overdue_loan_tips_pop_up";
  private static final String HOME_V5_REPAYMENT_LOAN_TIPS_POP_UP = PREFIX + "repayment_loan_tips_pop_up";
  private static final String VERSION_UPGRADE_POPUP_EXPOSURE_SWITCH = PREFIX + "version_upgrade_popup_exposure_switch";
  private static final String VERSION_UPGRADE_POPUP_EXPOSURE_WHITE_LIST = PREFIX + "version_upgrade_popup_exposure_white_list";
  private static final String HOME_V5_MOTHER_LAST_NAME_VERSION = PREFIX + "mother_last_name_version";
  private static final String HOME_V5_PRODUCT_DISCOUNT_CORNER_MARK_SWITCH = PREFIX + "product_discount_corner_mark_switch";
  private static final String HOME_V5_ORDER_LIMIT_CONFIRM_START_VERSION = PREFIX + "order_limit_confirm_start_version";
  private static final String HOME_V5_UNAVAILABLE_LOAN_ACCOUNT_VERSION = PREFIX + "unavailable_loan_account_version";
  private static final String HOME_V5_366_DISPLAY_BUILD = PREFIX + "home_v5_366_display_build";
  private static final String HOME_V5_COUPON_EXPIRE_DISPLAY_STRATEGY_VERSION = PREFIX + "home_v5_coupon_expire_display_strategy_version";
  private static final String HOME_V5_REJECT_LOAN_MARKET_INFO_START_VERSION = PREFIX + "home_v5_reject_loan_market_info_start_version";

  private static final String HOME_V5_REJECT_LOAN_MARKET_INFO_URL = PREFIX + "home_v5_reject_loan_market_info_url";
  private static final String HOME_V5_ORDER_GUIDE_ANIMATION_VERSION = PREFIX + "home_v5_order_guide_animation_version";
  private static final String HOME_V5_ORDER_GUIDE_ANIMATION_SHOW_TIMES = PREFIX + "home_v5_order_guide_animation_show_times";
  private static final String HOME_V5_ORDER_WEB_BUILD = PREFIX + "home_v5_order_web_build";
  private static final String HOME_V5_ORDER_WEB_URL = PREFIX + "home_v5_order_web_url";

  /**
   * 循环贷所有首页相关配置
   */
  private static final String HOME_V5_REVOLVING_LOAN_MIN_CREDITS = PREFIX + "home_v5_revolving_loan_min_credits";
  private static final String HOME_V5_LOAN_LIMIT_AMOUNT_OVER_STEP_VALUE = PREFIX + "home_v5_loan_limit_amount_over_step_value";
  private static final String HOME_V5_REVOLVING_LOAN_OVERDUE_LOAN_TIPS_POP_UP = PREFIX + "revolving_loan_overdue_loan_tips_pop_up";
  private static final String HOME_V5_REVOLVING_LOAN_REPAYMENT_TIPS_POP_UP = PREFIX + "revolving_loan_repayment_tips_pop_up";
  private static final String REVOLVING_LOAN_REMAIN_CREDITS_NOT_ENOUGH_AND_NO_OVERDUE_WITH_ZERO_CREDITS = PREFIX + "revolving_loan_remain_credits_not_enough_and_no_overdue_with_zero_credits";
  private static final String REVOLVING_LOAN_REMAIN_CREDITS_NOT_ENOUGH_AND_NO_OVERDUE = PREFIX + "revolving_loan_remain_credits_not_enough_and_no_overdue";
  private static final String REVOLVING_LOAN_OVERDUE = PREFIX + "revolving_loan_overdue";
  private static final String REVOLVING_LOAN_REPAYMENT_HIGH_LIGHT_WHEN_OVERDUE = PREFIX + "revolving_loan_repayment_high_light_when_overdue";
  private static final String TIPS_ICON_FOR_REVOLVING_LOAN = PREFIX + "tips_icon_fro_revolving_loan";
  private static final String REVOLVING_LOAN_TIPS_POP_UP_WHEN_PAYING = PREFIX + "revolving_loan_tips_pop_up_when_paying";
  private static final String REVOLVING_LOAN_TIPS_POP_UP_WHEN_REVIEW = PREFIX + "revolving_loan_tips_pop_up_when_review";
  private static final String REVOLVING_LOAN_ORDER_PROCESS_TITLE_OF_MAIN_CARD = PREFIX + "revolving_loan_order_process_title_of_main_card";
  private static final String REVOLVING_LOAN_ORDER_PROCESS_STEPS_MAP = PREFIX + "revolving_loan_order_process_steps_map";
  private static final String HOME_V5_367_DISPLAY_BUILD = PREFIX + "home_v5_367_display_build";
  private static final String HOME_V5_REVOLVING_LOAN_APP_VERSION = PREFIX + "home_v5_revolving_loan_app_version";
  private static final String PRODUCT_BUBBLE_CONTENT_DISPLAY_VERSION = PREFIX + "product_bubble_content_display_version";
  private static final String HOME_V5_USER_IMMEDIATE_CONTACT_VERSION = PREFIX + "user_immediate_contact_version";
  private static final String HOME_V5_CAN_ORDER_PAGE_REPAY_DISCOUNT_STYLE_VERSION = PREFIX + "can_order_page_repay_discount_style_version";
  private static final String HOME_V5_NOT_LOAN_AMOUNT_RATE = PREFIX + "not_loan_amount_rate";
  private static final String HOME_V5_LOAN_MAX_AMOUNT_RATE = PREFIX + "loan_max_amount_rate";
  private static final String HOME_V5_HIGH_CREDITS_NOT_LOAN_AMOUNT_RATE = PREFIX + "high_credits_not_loan_amount_rate";
  private static final String HOME_V5_QUICK_ORDER_HIGH_CREDITS_THRESHOLD = PREFIX + "quick_order_high_credits_threshold";
  private static final String HOME_V5_PRODUCT_PAGE_STYLE_FOR_371_VERSION = PREFIX + "product_page_style_for_371_version";
  private static final String CAN_ORDER_PAGE_RETURN_BUTTON_POP_UP_CREDITS_ACCEPT_LIMIT_DAYS = PREFIX + "can_order_page_return_button_pop_up_credits_accept_limit_days";
  private static final String JUMP_LEVEL_2_ORDER_PAGE_LIMITATION = PREFIX + "jump_level_2_order_page_limitation";
  private static final String JUMP_AUTH_AFTER_LOGIN_SECONDS = PREFIX + "jump_auth_after_login_seconds";

  private static final String USE_LIFE_CYCLE_SINGLE_JUMP_LIMIT_SWITCH = PREFIX + "use_life_cycle_single_jump_limit_switch";
  private static final String USE_LIFE_CYCLE_SINGLE_JUMP_LIMIT_WHITE_LIST = PREFIX + "use_life_cycle_single_jump_limit_white_list";
  private static final String LIFE_CYCLE_SINGLE_JUMP_LIMIT_EXPIRE_TIME_IN_SEC = PREFIX + "life_cycle_single_jump_limit_expire_time_in_sec";
  private static final String INTENTION_INFO_EXPIRE_TIME_IN_SEC = PREFIX + "intention_info_expire_time_in_sec";

  private static final String HOME_PAGE_POP_UP_START_BUILD = PREFIX + "home_page_pop_up_start_build";
  private static final String OLD_HOMEPAGE_MIDDLE_STRUCT_SWITCH = PREFIX + "old_homepage_middle_struct_switch";
  //快速下单额度可选择实验，主推产品额度倍率
  private static final String HOME_V5_OPTIONAL_AMOUNT_EXP_RATE_MAIN_PRODUCT = PREFIX + "optional_amount_exp_rate_main_product";
  //快速下单额度可选择实验，还款压力小产品额度倍率
  private static final String HOME_V5_OPTIONAL_AMOUNT_EXP_RATE_LOW_REPAY_PRODUCT = PREFIX + "optional_amount_exp_rate_low_repay_product";
  //快速下单额度可选择实验，上次借过产品额度倍率
  private static final String HOME_V5_OPTIONAL_AMOUNT_EXP_RATE_RELOAN_PRODUCT = PREFIX + "optional_amount_exp_rate_reloan_product";
  //快速下单额度可选择实验，范围
  private static final String HOME_V5_OPTIONAL_AMOUNT_EXP_SCOPE_MAX = PREFIX + "optional_amount_exp_scope_max";
  //快速下单额度可选择实验，范围
  private static final String HOME_V5_OPTIONAL_AMOUNT_EXP_SCOPE_MIN = PREFIX + "optional_amount_exp_scope_min";
  //新首页支持版本
  private static final String HOME_V5_NEW_HOMEPAGE_VERSION = PREFIX + "new_homepage_version";
  //未完件用户新首页实验重开支持版本
  private static final String HOME_V5_NEW_HOMEPAGE_AUTH_UNFINISHED_VERSION = PREFIX + "new_homepage_auth_unfinished_version";
  //新首页未登录用户默认展示首页版本，对应com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion
  private static final String HOME_V5_NEW_HOMEPAGE_NOT_LOGIN_USER_HOMEPAGE_VERSION = PREFIX + "new_homepage_not_login_user_homepage_version";
  //快速下单3.0-与现金奖励融合实验， 额度倍率
  private static final String HOME_V5_CASH_ACTIVITY_EXP_RATE = PREFIX + "cash_activity_exp_rate";
  //快速下单 额度不可选 历史最高借款金额倍率
  private static final String HOME_V5_FIXED_AMOUNT_RATE = PREFIX + "fixed_amount_rate";
  //下单页H5的app起始版本，大于等于该版本下单页为H5
  private static final String HOME_V5_ORDER_PAGE_H5_START_VERSION = PREFIX + "order_page_h5_start_version";
  //账单即将到期用户自动跳转下单页降频实验起始版本
  private static final String BILL_JUMP_LEVEL_2_ORDER_PAGE_VERSION = PREFIX + "bill_jump_level_2_order_page_version";
  //首页反诈提醒banner展示时间
  private static final String HOME_PAGE_FRAUD_ALERT_BANNER_SHOW_TIME = PREFIX + "home_page_fraud_alert_banner_show_time";
  private static final String HOME_PAGE_FRAUD_ALERT_BANNER_SWITCH = PREFIX + "home_page_fraud_alert_banner_switch";
  private static final String HOME_PAGE_FRAUD_ALERT_BANNER_WHITE_LIST = PREFIX + "home_page_fraud_alert_banner_white_list";
  //下单页还款计划浮层发降息券发券工具id
  private static final String REPAY_PLAN_CUT_INTEREST_COUPON_ID = PREFIX + "repay_plan_cut_interest_coupon_id";
  private static final String REPAY_PLAN_CUT_INTEREST_COUPON_ID_V2 = PREFIX + "repay_plan_cut_interest_coupon_id_v2";
  private static final String REPAY_PLAN_CUT_INTEREST_COUPON_ID_V3 = PREFIX + "repay_plan_cut_interest_coupon_id_v3";
  private static final String REPAY_PLAN_CUT_INTEREST_COUPON_ID_FIRST_LOAN = PREFIX + "repay_plan_cut_interest_coupon_id_first_loan";
  // 还款计划发券 V3：无券用户发固定折扣券 A 的发券工具 ID
  private static final String REPAY_PLAN_V3_FIXED_COUPON_TOOL_ID = PREFIX + "repay_plan_v3_fixed_coupon_tool_id";
  // 还款计划发券 V3：有券用户发锚定膨胀券 B 的发券工具 ID
  private static final String REPAY_PLAN_V3_ANCHOR_COUPON_TOOL_ID = PREFIX + "repay_plan_v3_anchor_coupon_tool_id";
  private static final String HOME_INTEREST_FREE_CARD_REWARD_INFO = PREFIX + "home_interest_free_card_reward_info";
  // 首页免息卡玩法 V2：核销后参与冷静期天数
  private static final String HOME_INTEREST_FREE_CARD_REDEEM_COOLDOWN_DAYS = PREFIX + "home_interest_free_card_redeem_cooldown_days";

  private static final String HOME_INTEREST_FREE_CARD_DRAW_CYCLE_DAYS = PREFIX + "home_interest_free_card_draw_cycle_days";

  private static final String HOME_INTEREST_FREE_CARD_NEW_GRANT_RULE_ID = PREFIX + "home_interest_free_card_new_grant_rule_id";

  private static final String HOME_INTEREST_FREE_CARD_ANCHOR_UPGRADE_RULE_ID = PREFIX + "home_interest_free_card_anchor_upgrade_rule_id";

  private static final String ORDER_PAGE_AMOUNT_FAST_INPUT_RATE_LIST = PREFIX + "order_page_amount_fast_input_rate_list";

  private static final String ORDER_PAGE_AMOUNT_FAST_INPUT_CREDITS_THRESHOLD = PREFIX + "order_page_amount_fast_input_credits_threshold";

  private static final String ORDER_PAGE_AMOUNT_FAST_INPUT_TEXT_FORMAT = PREFIX + "order_page_amount_fast_input_text_format";
  private static final String SUB_HOME_PAGE_FLOATING_ICON_DISPLAY_SWITCH = PREFIX + "sub_home_page_floating_icon_display_switch";
  private static final String ORDER_PAGE_CAROUSEL_CONTENT_CONFIG_FOR_RELOAN = PREFIX + "order_page_carousel_content_config_for_reloan";
  private static final String ORDER_PAGE_CAROUSEL_CONTENT_CONFIG_FOR_FIRST_LOAN = PREFIX + "order_page_carousel_content_config_for_first_loan";
  private static final String ORDER_PAGE_CAROUSEL_CONTENT_CONFIG_FOR_FIRST_LOAN_T0_CONTENT = PREFIX + "order_page_carousel_content_config_for_first_loan_t0_content";
  private static final String ORDER_PAGE_CAROUSEL_CONTENT_CONFIG_FOR_INTEREST_SPLIT_V3 =
      PREFIX + "order_page_carousel_content_config_for_interest_split_v3";

  /** 头部用户券配置 JSON（含 A/B/C 三个等级的 threshold、couponToolId、priority） */
  private static final String REPAY_PLAN_HEAD_COUPON_CONFIG = PREFIX + "repay_plan_head_coupon_config";
  /** 中部低意愿用户券配置 JSON（含 D/E/F 三个等级） */
  private static final String REPAY_PLAN_MIDDLE_LOW_WILLING_COUPON_CONFIG = PREFIX + "repay_plan_middle_low_willing_coupon_config";
  /** 头部+中部低意愿新实验入组所需的过件时间阈值（分钟） */
  private static final String REPAY_PLAN_HEAD_MIDDLE_ACCEPTANCE_MINUTES = PREFIX + "repay_plan_head_middle_acceptance_minutes";
  /** 0630 子实验头部用户实验组券配置 JSON（含 A1/B1/C1 三个等级） */
  private static final String REPAY_PLAN_HEAD_EXP_COUPON_CONFIG = PREFIX + "repay_plan_head_exp_coupon_config";
  /** 0630 子实验中部低意愿用户实验组券配置 JSON（含 D1/E1/F 三个等级） */
  private static final String REPAY_PLAN_MIDDLE_LOW_WILLING_EXP_COUPON_CONFIG = PREFIX + "repay_plan_middle_low_willing_exp_coupon_config";
  /** 首贷头/中部入组所需最小可借产品日利率阈值，默认 0.0015（即 0.15%） */
  private static final String REPAY_PLAN_HEAD_MIDDLE_MIN_DAILY_RATE_THRESHOLD = PREFIX + "repay_plan_head_middle_min_daily_rate_threshold";
  private static final String SAFETY_MODULE_START_BUILD = PREFIX + "safety_module_start_build";
  private static final String REGULATOR_ITEMS = PREFIX + "regulator_items";
  private static final String MITRA_ITEMS = PREFIX + "mitra_items";

  @Resource
  private ISiteVars ecSiteVars;

  @PostConstruct
  private void registerConf() {
    ecSiteVars.registerConf(CREDITS_TITLE, new Conf<String>() {
    });
    ecSiteVars.registerConf(CREDITS_TIP, new Conf<String>() {
    });
    ecSiteVars.registerConf(PRODUCT_RATE_TITLE, new Conf<String>() {
    });
    ecSiteVars.registerConf(REGISTER_USER_NOT_ENOUGH_RATE_TITLE_TEXT_BY_PLATFORM_TYPE, new Conf<String>() {
    });
    ecSiteVars.registerConf(REGISTER_USER_NOT_ENOUGH_RATE_TEXT_BY_PLATFORM_TYPE, new Conf<String>() {
    });
    ecSiteVars.registerConf(REGISTER_USER_NOT_ENOUGH_PERIOD_TEXT_BY_PLATFORM_TYPE, new Conf<String>() {
    });
    ecSiteVars.registerConf(PRODUCT_RATE_CONTENT, new Conf<String>() {
    });
    ecSiteVars.registerConf(PRODUCT_RATE_CONTENT_FOR_WHITE_DEVICE, new Conf<String>() {
    });
    ecSiteVars.registerConf(PRODUCT_PERIOD_TITLE, new Conf<String>() {
    });
    ecSiteVars.registerConf(RISK_IN_REVIEW_CONTENT, new Conf<String>() {
    });
    ecSiteVars.registerConf(AB_TEST_HOME_STRATEGY_V3, new Conf<String>() {
    });
    ecSiteVars.registerConf(AB_TEST_PRODUCT_AMOUNT_INPUT, new Conf<String>() {
    });
    ecSiteVars.registerConf(CARD_BUTTON_NAME, new Conf<String>() {
    });
    ecSiteVars.registerConf(LOW_RATE_AND_30_DAYS_POPUP_STRATEGY, new Conf<String>() {
    });
    ecSiteVars.registerConf(BUTTON_ANIMATION_EFFECT_PERCENT, new Conf<Double>() {
    });
    ecSiteVars.registerConf(INCREASE_CREDITS_ENTRANCE, new Conf<Map<String, Boolean>>() {
    });
    ecSiteVars.registerConf(MINIMALIST_PROCESS_USER_SUBMIT_RISK_FROM_INIT_TO_FINISH_THRESHOLD_TIME, new Conf<Long>() {
    });
    ecSiteVars.registerConf(HOME_V5_API_DIFF, new Conf<Double>() {
    });
    ecSiteVars.registerConf(HOME_V5_GET_RESPONSE_STATUS, new Conf<Double>() {
    });
    ecSiteVars.registerConf(HOME_V5_GET_TOP_AREA_RESPONSE_STATUS, new Conf<Double>() {
    });
    ecSiteVars.registerConf(JUMP_LEVEL_2_ORDER_PAGE_LIMITATION, new Conf<String>() {
        }
    );
    ecSiteVars.registerConf(HOME_INTEREST_FREE_CARD_REWARD_INFO, new Conf<String>() {
    });
  }

  public String getCreditsTitle(String statusName) {
    return ecSiteVars.getConfVal(CREDITS_TITLE, statusName, "最高可借（Rp）");
  }

  public String getCreditsTip(String statusName) {
    return ecSiteVars.getConfVal(CREDITS_TIP, statusName, "");
  }

  public String getProductRateTitle(String statusName) {
    return ecSiteVars.getConfVal(PRODUCT_RATE_TITLE, statusName, "利率");
  }

  public String getProductRateContent(String key) {
    return ecSiteVars.getConfVal(PRODUCT_RATE_CONTENT, key, "低至9%");
  }

  //TODO(gxs, --task=1190923 --user=郭晓帅 降息线上走查白名单能力-后端 https://www.tapd.cn/53182677/s/4580175)降息走查白名单，上线后删除
  public String getProductRateContentForWhiteDevice(String key) {
    return ecSiteVars.getConfVal(PRODUCT_RATE_CONTENT_FOR_WHITE_DEVICE, key, "低至9%");
  }

  public String getRegisterUserNotEnoughRateTitleText(PlatformType platformType) {
    return ecSiteVars.getConfVal(REGISTER_USER_NOT_ENOUGH_RATE_TITLE_TEXT_BY_PLATFORM_TYPE, platformType.name(), "利率");
  }

  public String getRegisterUserNotEnoughPeriodText(PlatformType platformType) {
    return ecSiteVars.getConfVal(REGISTER_USER_NOT_ENOUGH_PERIOD_TEXT_BY_PLATFORM_TYPE, platformType.name(), "3-12 Bulan");
  }

  public String getRegisterUserNotEnoughRateText(PlatformType platformType) {
    return ecSiteVars.getConfVal(REGISTER_USER_NOT_ENOUGH_RATE_TEXT_BY_PLATFORM_TYPE, platformType.name(), "低至12%");
  }

  public String getProductPeriodTitle(String statusName) {
    return ecSiteVars.getConfVal(PRODUCT_PERIOD_TITLE, statusName, "分期期限");
  }

  public String getProductPeriodContent() {
    return ecSiteVars.getString(PRODUCT_PERIOD_CONTENT_V2, "3-12 Bulan");
  }

  public String getLyProductPeriodContent() {
    return ecSiteVars.getString(LY_PRODUCT_PERIOD_CONTENT, "2-12 Bulan");
  }

  public BigDecimal getStandardInterestRateForLowInterestTip() {
    return ecSiteVars.getBigDecimal(PREFIX + "standard_rate_tip", BigDecimal.valueOf(0.003));
  }

  public String getSecondRiskRejectSubTitleProductPeriodContent() {
    return ecSiteVars.getString(PREFIX + "second_risk_reject_sub_title_product_period_content", "1-12");
  }

  public BigDecimal getStandardInterestRateForMarket() {
    return ecSiteVars.getBigDecimal(PREFIX + "standard_rate_for_market", BigDecimal.valueOf(0.004));
  }

  public String getMiddleMessageWithoutSmeEntrance() {
    return ecSiteVars.getString(PREFIX + "middle_message_without_sme", "[]");
  }

  public String getMiddleMessageWithSmeEntrance() {
    return ecSiteVars.getString(PREFIX + "middle_message_with_sme", "[]");
  }

  public BigDecimal getPrincipalThreshold() {
    return ecSiteVars.getBigDecimal(PREFIX + "not_login_notice_order_principal", new BigDecimal("5000000"));
  }

  public Integer getOrderLimit(int defaultValue) {
    return ecSiteVars.getInt(PREFIX + "not_login_notice_order_limit", defaultValue);
  }

  public Integer getBillingOrderDayThreshold() {
    return ecSiteVars.getInt(PREFIX + "billing_order_day_threshold", 3);
  }

  public String getRiskRedirectUrl() {
    return ecSiteVars.getString(PREFIX + "risk_redirect_url");
  }

  public String getNoticeContentByIncreaseCredits() {
    return ecSiteVars.getString(PREFIX + "notice_content_increase_credits", "补充信息最高可提额到Rp10.000.000");
  }

  public String getRiskInReviewContent(LoanUserRiskType loanUserRiskType) {
    return ecSiteVars.getConfVal(RISK_IN_REVIEW_CONTENT, loanUserRiskType.name(), "预计1分钟内审核完成，请耐心等待");
  }

  public String getPayingStatusContent() {
    return ecSiteVars.getString(PAYING_STATUS_CONTENT, "放款金额 {0} 将转至后4位 {1} 的 {2} 账户。");
  }

  public String getBankCardRedirectUrl() {
    return ecSiteVars.getString(PREFIX + "bank_card_redirect_url");
  }

  public List<String> getIgnoreProductLoanUserTypeList() {
    String listConfig = ecSiteVars.getString(PREFIX + "ignore_product_loan_user_type_list", "[]");
    return JsonUtils.from(listConfig, new TypeReference<List<String>>() {
    });
  }

  public List<String> getProductAmountQuickInputRatioList() {
    String configStr = ecSiteVars.getString(PRODUCT_AMOUNT_QUICK_INPUT_RATIO, "");
    if (StringUtils.isBlank(configStr)) {
      return new ArrayList<>();
    }
    return Arrays.stream(configStr.split(",")).collect(Collectors.toList());
  }

  public String getRepaymentOrderRedirectUrl(List<OrderInstalment> readyOrderVOList) {
    int size = readyOrderVOList.size();
    String redirectUrl = ecSiteVars.get(PREFIX + "repayment_order_redirect_url");
    // 单个待还就跳转到具体的还款
    if (size == 1) {
      return redirectUrl + "?orderId=" + YqgHashids.encode(readyOrderVOList.get(0).orderVO.id);
    }
    return redirectUrl;
  }

  public String getAgreementWebUrlPreFix() {
    return ecSiteVars.getString(AGREEMENT_WEB_URL_PRE_FIX, "webview/agreement/");
  }

  public String getAgreementKey() {
    return ecSiteVars.getString(AGREEMENT_KEY, "EASYCASH_IDN_INSTALMENT_LOAN");
  }

  public String getButtonName(String key) {
    return ecSiteVars.getConfVal(CARD_BUTTON_NAME, key, "查看额度");
  }

  public Long getUserLimitForProductRateDisplay() {
    return ecSiteVars.getLong(PREFIX + "user_limit_for_display_rate", 100L);
  }

  public List<PercentConfig> getLowRateAnd30DaysPopupStrategy(ABTestGroupType groupType) {
    String configStr = ecSiteVars.getConfVal(LOW_RATE_AND_30_DAYS_POPUP_STRATEGY, groupType.name(), "[]");
    if (StringUtils.isBlank(configStr)) {
      return Collections.emptyList();
    }
    return JsonUtils.fromOrException(configStr, new TypeReference<List<PercentConfig>>() {
    });
  }

  public String getMultiLoanGuideRedirectUrl() {
    return ecSiteVars.getString(MULTI_LOAN_GUIDE_REDIRECT_URL);
  }

  public Long getDiscountDetailTopAreaVipUIDisplayStrategyVersion() {
    return ecSiteVars.getLong(DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_START_VERSION, 36213L);
  }

  public Long getDiscountDetailTopAreaVipUIDisplayStrategyV3() {
    return ecSiteVars.getLong(DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_START_VERSION_V3, 37100L);
  }

  public Boolean needIncreaseCreditsEntrance(String location, String status) {
    HashMap<String, Boolean> needIncreaseCreditsEntranceMap = ecSiteVars.getConfVal(INCREASE_CREDITS_ENTRANCE, location, new HashMap<>());
    return needIncreaseCreditsEntranceMap.getOrDefault(status, false);
  }

  public BigDecimal getNoInterestExperimentRate() {
    return ecSiteVars.getBigDecimal(LOAN_INFO_NO_INTEREST_EXPERIMENT_RATE, BigDecimal.valueOf(0.10));
  }

  public String getDiscountDetailTitle() {
    return ecSiteVars.getString(DISCOUNT_DETAIL_TITLE, "");
  }

  public String getFeeCalculationDetailTitle() {
    return ecSiteVars.getString(FEE_CALCULATION_DETAIL_TITLE, "");
  }

  public String getFeeCalculationDetailUrl() {
    return ecSiteVars.getString(FEE_CALCULATION_DETAIL_URL, "");
  }

  public String getFeeCalculationDetailUrlForH5() {
    return ecSiteVars.getString(FEE_CALCULATION_DETAIL_URL_FOR_API_CHANNEL, "");
  }

  public String getMonthProductPrompt() {
    return ecSiteVars.getString(MONTH_PRODUCT_PROMPT, null);
  }

  public String getReduceCreditsContent() {
    return ecSiteVars.getString(REDUCE_CREDITS_CONTENT, "");
  }

  public String getReduceCreditsDetailTitle() {
    return ecSiteVars.getString(REDUCE_CREDITS_DETAIL_TITLE, "");
  }

  public String getReduceCreditsDetailContent() {
    return ecSiteVars.getString(REDUCE_CREDITS_DETAIL_CONTENT, "");
  }

  public String getReduceCreditsRedirectUrl() {
    return ecSiteVars.getString(REDUCE_CREDITS_REDIRECT_URL, "");
  }

  public String getReduceCreditsInputLoanAmount() {
    return ecSiteVars.getString(REDUCE_CREDITS_INPUT_LOAN_AMOUNT, "借款金额(Rp)");
  }

  public String getReduceCreditsScrollLoanAmount() {
    return ecSiteVars.getString(REDUCE_CREDITS_SCROLL_LOAN_AMOUNT, "借款金额");
  }

  public String getVirtualIncreaseCreditsContent() {
    return ecSiteVars.getString(VIRTUAL_INCREASE_CREDITS_CONTENT, "距离得到15%提额还剩{0}小时");
  }

  public Integer getFromRegisterToAuthFinishTimeThreshold() {
    return ecSiteVars.getInt(FROM_REGISTER_TO_AUTH_FINISH_TIME_THRESHOLD, 72);
  }

  public String getVirtualIncreaseCreditsPercent() {
    return ecSiteVars.getString(VIRTUAL_INCREASE_CREDITS_PERCENT, "1.15");
  }

  public Long getTempCreditsExpiredDayThreshold() {
    return ecSiteVars.getLong(TEMP_CREDITS_EXPIRED_DAY_THRESHOLD, 7L);
  }

  public boolean getIncreaseCreditsReviewSwitch() {
    return ecSiteVars.getBoolean(INCREASE_CREDITS_REVIEW_SWITCH, false);
  }

  public String getIncreaseCreditsReviewHomeButtonTitle() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEW_HOME_BUTTON_TITLE, "审核未通过");
  }

  public String getIncreaseCreditsReviewingHomeButtonTitle() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEWING_HOME_BUTTON_TITLE, "资料审核中");
  }

  public String getIncreaseCreditsReviewHomeButtonName() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEW_HOME_BUTTON_NAME, "获得额度");
  }

  public String getIncreaseCreditsReviewHomeButtonJumpUrl() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEW_HOME_BUTTON_JUMP_URL, "");
  }

  public String getIncreaseCreditsReviewHomeButtonSkippableUrl() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEW_HOME_BUTTON_SKIPPABLE_URL, "");
  }


  public String getIncreaseCreditsReviewHomeContent() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEW_HOME_CONTENT, "您点击获得额度按钮，就有机会获得额度");
  }

  public String getReviewRejectedButtonName() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEW_REJECTED_BUTTON_NAME, "提交资料");
  }

  public String getReviewRejectedContent() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEW_REJECTED_CONTENT, "您有重审资料被驳回，请重新提交");
  }

  public String getReviewingDisplayContent() {
    return ecSiteVars.getString(INCREASE_CREDITS_REVIEWING_DISPLAY_CONTENT, "资料审核中，预计一个工作日返回结果");
  }

  public boolean supportPassedQuitePeriodUserToReapply(Long build) {
    return build >= ecSiteVars.getLong(INCREASE_CREDITS_REVIEW_FOR_PASSED_QUITE_PERIOD_USER_BUILD, 35913L);
  }

  /**
   * 获取极简用户倒计时读秒时长（单位：秒）
   */
  public Long getMinimalistUserCountdownDuration(boolean neverOrdered) {
    BooleanType neverCreateOrder = neverOrdered ? BooleanType.TRUE : BooleanType.FALSE;
    return ecSiteVars.getConfVal(MINIMALIST_PROCESS_USER_SUBMIT_RISK_FROM_INIT_TO_FINISH_THRESHOLD_TIME, neverCreateOrder.name(), 60L);
  }

  public String getIncreaseCreditsEntranceUrl() {
    return ecSiteVars.getString(INCREASE_CREDITS_ENTRANCE_URL, "");
  }

  public String getMinimalistProcessUserIncreaseCreditsFrontContent() {
    return ecSiteVars.getString(MINIMALIST_PROCESS_USER_INCREASE_CREDITS_FRONT_CONTENT, "");
  }

  public String getMinimalistProcessUserIncreaseCreditsBackendContent() {
    return ecSiteVars.getString(MINIMALIST_PROCESS_USER_INCREASE_CREDITS_BACKEND_CONTENT, "");
  }

  public String getMinimalistProcessUserAcceptToolBarColor() {
    return ecSiteVars.getString(MINIMALIST_PROCESS_USER_ACCEPT_TOOL_BAR_COLOR, "");
  }

  public String getMinimalistProcessUserAcceptBackgroundImageUrl() {
    return ecSiteVars.getString(MINIMALIST_PROCESS_USER_ACCEPT_BACKGROUND_IMAGE_URL, "");
  }

  public String getMinimalistProcessUserAcceptBackgroundImageUrlV2() {
    return ecSiteVars.getString(MINIMALIST_PROCESS_USER_ACCEPT_BACKGROUND_IMAGE_URL_V2, "");
  }

  public String getMinimalistProcessUserSubContentInfo() {
    return ecSiteVars.getString(MINIMALIST_PROCESS_USER_SUB_CONTENT_INFO, "");
  }

  public String getMinimalistProcessUserTopContentPrefixInfo() {
    return ecSiteVars.getString(MINIMALIST_PROCESS_USER_TOP_CONTENT_PREFIX_INFO, "Hi ");
  }

  public String getMinimalistProcessUserIncreaseCreditsIconUrl() {
    return ecSiteVars.getString(MINIMALIST_PROCESS_USER_INCREASE_CREDITS_ICON_URL, "");
  }

  public Long getMinimalistProcessUserVersion() {
    return ecSiteVars.getLong(MINIMALIST_PROCESS_USER_VERSION, 34513L);
  }

  public long getMinimalistProcessUserContentInfoV2Version() {
    return ecSiteVars.getLong(MINIMALIST_PROCESS_USER_NEW_CONTENT_INFO_VERSION, 31800L);
  }

  public String getVirtualIncreaseCreditsContentForLastHour() {
    return ecSiteVars.getString(VIRTUAL_INCREASE_CREDITS_CONTENT_LAST_HOUR, "距离得到15%提额还剩不到1小时");
  }

  public Long getDiscountDetailAnimationBuild() {
    return ecSiteVars.getLong(DISCOUNT_DETAIL_ANIMATION_BUILD, 35213L);
  }

  public Long getDiscountDetailAnimationV3Build() {
    return ecSiteVars.getLong(DISCOUNT_DETAIL_ANIMATION_V3_BUILD, 35513L);
  }


  public String getDiscountDetailAnimationTopAreaImageUrl() {
    return ecSiteVars.getString(DISCOUNT_DETAIL_ANIMATION_TOP_AREA_IMAGE_URL, "");
  }

  public String getDiscountDetailAnimationTopAreaToolBarColor() {
    return ecSiteVars.getString(DISCOUNT_DETAIL_ANIMATION_TOP_AREA_TOOL_BAR_COLOR, "");
  }

  public String getDiscountDetailAnimationTopAreaBoldText() {
    return ecSiteVars.getString(DISCOUNT_DETAIL_ANIMATION_TOP_AREA_BOLD_TEXT, "Diskon maksimum {0} sudah digunakan");
  }

  public String getDiscountDetailAnimationTopAreaNormalText() {
    return ecSiteVars.getString(DISCOUNT_DETAIL_ANIMATION_TOP_AREA_NORMAL_TEXT, "segera ajukan pinjaman");
  }

  public String getCutInterestCouponTopAreaImageUrl() {
    return ecSiteVars.getString(CUT_INTEREST_COUPON_TOP_AREA_IMAGE_URL, "");
  }

  public String getCutInterestCouponTopAreaToolBarColor() {
    return ecSiteVars.getString(CUT_INTEREST_COUPON_TOP_AREA_TOOL_BAR_COLOR, "");
  }

  public String getCutInterestCouponTopAreaBoldText() {
    return ecSiteVars.getString(CUT_INTEREST_COUPON_TOP_AREA_BOLD_TEXT, "Gunakan KUPON untuk HEMAT {0}");
  }

  public String getCutInterestCouponTopAreaNormalText() {
    return ecSiteVars.getString(CUT_INTEREST_COUPON_TOP_AREA_NORMAL_TEXT, "Gunakan & Pinjam Sekarang");
  }

  public Long getDisplayCouponTipsStrategyStartVersion() {
    return ecSiteVars.getLong(DISPLAY_COUPON_TIPS_STRATEGY_START_VERSION, 35113L);
  }

  public Long getOrderPageInputAmountAndTipDisplayStrategyStartBuild() {
    return ecSiteVars.getLong(ORDER_PAGE_INPUT_AMOUNT_AND_TIP_DISPLAY_STRATEGY_START_BUILD, 35113L);
  }

  public String getIncreaseCreditsJumpUrl() {
    return ecSiteVars.getString(INCREASE_CREDITS_JUMP_URL, "https://easycash.id/webview/additional-info?v=amountArea");
  }

  public String getIncreaseCreditsContent() {
    return ecSiteVars.getString(INCREASE_CREDITS_CONTENT, "点击获取更高额度");
  }


  public Long getNeedOrderPageUserIdlePopupStartVersion() {
    return ecSiteVars.getLong(NEED_ORDER_PAGE_USER_IDLE_POPUP_START_VERSION, 35213L);
  }

  public String getOrderPageUserIdlePopupCouponHint() {
    return ecSiteVars.getString(ORDER_PAGE_USER_IDLE_POPUP_COUPON_HINT, "还有降息券可以叠加使用!");
  }

  public Long getAppRefreshPopupWindowIntervalMilisecondNew() {
    return ecSiteVars.getLong(APP_REFRESH_POPUP_WINDOW_INTERVAL_MILLISECOND_NEW, 1000L);
  }

  public Long getAppRefreshPopupWindowIntervalMilisecondOld() {
    return ecSiteVars.getLong(APP_REFRESH_POPUP_WINDOW_INTERVAL_MILLISECOND_OLD, 600 * 1000L);
  }

  public Long getAppRefreshPopupWindowNewStartVersion() {
    return ecSiteVars.getLong(APP_REFRESH_POPUP_WINDOW_NEW_START_VERSION, 36213L);
  }

  public Long getNeedOrderPageUserIdlePopupV2StartVersion() {
    return ecSiteVars.getLong(NEED_ORDER_PAGE_USER_IDLE_POPUP_V2_START_VERSION, 35513L);
  }

  public String getAntiFraudCarouselText() {
    return ecSiteVars.getString(ANTI_FRAUD_CAROUSEL_TEXT, "Easycash提醒您小心欺诈，避免受欺诈者指使产生非本人意愿的借款行为。");
  }

  public String getQuickOrderPageTitle() {
    return ecSiteVars.getString(CREDITS_DECREASE_QUICK_ORDER_PAGE_TITLE, "借款信息更新");
  }

  public String getQuickOrderCreditUpdateContent() {
    return ecSiteVars.getString(CREDITS_DECREASE_QUICK_ORDER_CREDIT_UPDATE_CONTENT, "借款额度调整为");
  }

  public String getQuickOrderTipsContentSignBeforeRisk() {
    return ecSiteVars.getString(CREDITS_DECREASE_QUICK_ORDER_TIPS_CONTENT_SIGN_BEFORE_RISK, "除上述信息外,合同中的信息不会发生变化");
  }

  public String getQuickOrderTipsContentSignAfterRisk() {
    return ecSiteVars.getString(CREDITS_DECREASE_QUICK_ORDER_TIPS_CONTENT_SIGN_AFTER_RISK, "除上述信息外，其他信息不发生变化");
  }

  public String getQuickOrderIncreaseCreditsContent() {
    return ecSiteVars.getString(CREDITS_DECREASE_QUICK_ORDER_INCREASE_CREDITS_CONTENT, "对额度不满意？提交资料提升额度 >>");
  }

  public String getQuickOrderIncreaseCreditsUrl() {
    return ecSiteVars.getString(CREDITS_DECREASE_QUICK_ORDER_INCREASE_CREDITS_URL, "");
  }

  public Long getLargeInstalmentAmountOwnedBuild() {
    return ecSiteVars.getLong(LARGE_INSTALMENT_AMOUNT_OWNED_BUILD, 35213L);
  }

  public Long getActivityOrderInfoStartVersion() {
    return ecSiteVars.getLong(ACTIVITY_ORDER_INFO_START_VERSION, 35513L);
  }

  public boolean openLoanActivityOrder() {
    return ecSiteVars.getBoolean(OPEN_LOAN_ACTIVITY_ORDER, false);
  }

  public String getActivityOrderAgreementContent() {
    return ecSiteVars.getString(ACTIVITY_ORDER_AGREEMENT_CONTENT, "我已阅读ACTIVITY_RELU并同意参加本次活动");
  }

  public String getActivityOrderAgreementLink() {
    return ecSiteVars.getString(ACTIVITY_ORDER_AGREEMENT_LINK, "");
  }

  public String getActivityContentLink() {
    return ecSiteVars.getString(ACTIVITY_CONTENT_LINK, "");
  }

  public String getActivityContent() {
    return ecSiteVars.getString(ACTIVITY_CONTENT, "最高 AWARD_AMOUNT 印尼盾，{0}天免息礼物");
  }

  public BigDecimal getActivityAwardAmount() {
    return ecSiteVars.getBigDecimal(ACTIVITY_AWARD_AMOUNT, new BigDecimal("500000"));
  }

  public Map<String, String> getActivityHighLightMap() {
    return JsonUtils.from(ecSiteVars.getString(ACTIVITY_HIGHLIGHT_MAP, "{\"ACTIVITY_RELU\":\"活动条款\"}"), new TypeReference<Map<String, String>>() {
    });
  }

  public int getActivityOrderDays() {
    return ecSiteVars.getInt(ACTIVITY_ORDER_DAYS, 360);
  }

  public boolean openMultiLoanCreditsDecreaseQuickOrder() {
    return ecSiteVars.getBoolean(OPEN_MULTI_LOAN_CREDITS_DECREASE_QUICK_ORDER, false);
  }

  public Long getOrderPageBackButtonBuild() {
    return ecSiteVars.getLong(ORDER_PAGE_BACK_BUTTON_BUILD, 35613L);
  }


  public BigDecimal getMaxTermsInterestRate() {
    return ecSiteVars.getBigDecimal(MAX_TERMS_INTEREST_RATE, new BigDecimal("9.0")).setScale(2);
  }

  public List<BigDecimal> getOrderPageAmountFastInputRateList() {
    String rateListStr = ecSiteVars.getString(ORDER_PAGE_AMOUNT_FAST_INPUT_RATE_LIST, "0.5,0.8,1");
    return Arrays.stream(rateListStr.split(","))
        .map(BigDecimal::new)
        .collect(Collectors.toList());
  }

  public BigDecimal getEnableCreditsThreshold() {
    return ecSiteVars.getBigDecimal(ORDER_PAGE_AMOUNT_FAST_INPUT_CREDITS_THRESHOLD, new BigDecimal("1000000"));
  }

  public String getSimpleAmountTextFormatPattern() {
    return ecSiteVars.getString(ORDER_PAGE_AMOUNT_FAST_INPUT_TEXT_FORMAT, "%s%s");
  }

  //=======VIP 页面=========

  public List<Long> getCreditsCouponActivityIdList() {
    String activityIdListStr = ecSiteVars.getString(CREDITS_COUPON_ACTIVITY_ID_LIST, "[]");
    return JsonUtils.from(activityIdListStr, new TypeReference<List<Long>>() {
    });
  }

  public List<Long> getRepayEducationActivityIdList() {
    String activityIdListStr = ecSiteVars.getString(REPAY_EDUCATION_ACTIVITY_ID_LIST, "[]");
    return JsonUtils.from(activityIdListStr, new TypeReference<List<Long>>() {
    });
  }

  /**
   * 获取按时还款教育首页任务标题。
   *
   * @return 首页任务标题文案
   */
  public String getRepayEducationHomepageTitle() {
    RepayEducationStaticTextConfig config = getRepayEducationStaticTextConfig();
    return config.getHomepage().getTitle();
  }

  /**
   * 获取按时还款教育 banner 静态文案。
   *
   * @return banner 文案配置
   */
  public RepayEducationResourceTextConfig getRepayEducationBannerTextConfig() {
    return getRepayEducationStaticTextConfig().getBanner();
  }

  /**
   * 获取按时还款教育进度弹窗静态文案。
   *
   * @return 进度弹窗文案配置
   */
  public RepayEducationResourceTextConfig getRepayEducationIconPopupTextConfig() {
    return getRepayEducationStaticTextConfig().getIconPopup();
  }

  /**
   * 获取按时还款教育完成通知资源位静态文案。
   *
   * @return 完成通知资源位文案配置
   */
  public RepayEducationResourceTextConfig getRepayEducationRewardPopupTextConfig() {
    return getRepayEducationStaticTextConfig().getRewardPopup();
  }

  private RepayEducationStaticTextConfig getRepayEducationStaticTextConfig() {
    String configStr = ecSiteVars.getString(REPAY_EDUCATION_STATIC_TEXT_CONFIG, DEFAULT_REPAY_EDUCATION_STATIC_TEXT_CONFIG);
    RepayEducationStaticTextConfig config = parseRepayEducationStaticTextConfig(configStr);
    return config == null ? parseRepayEducationStaticTextConfig(DEFAULT_REPAY_EDUCATION_STATIC_TEXT_CONFIG) : config;
  }

  private RepayEducationStaticTextConfig parseRepayEducationStaticTextConfig(String configStr) {
    return JsonUtils.from(configStr, new TypeReference<RepayEducationStaticTextConfig>() {
    });
  }

  public BigDecimal getCreditsCouponActivityDisplayAmountPercent() {
    return ecSiteVars.getBigDecimal(CREDITS_COUPON_ACTIVITY_DISPLAY_AMOUNT_PERCENT, BigDecimal.valueOf(0.1));
  }

  public boolean getCutInterestBlankAbTestSwitch() {
    return ecSiteVars.getBoolean(CUT_INTEREST_BLANK_AB_TEST_SWITCH, false);
  }

  public boolean getCutInterestBlankAbTestExecuteSwitch() {
    return ecSiteVars.getBoolean(CUT_INTEREST_BLANK_AB_TEST_EXECUTE_SWITCH, false);
  }

  public String getVipPageMinActualInterestRateForDailyUnit() {
    return ecSiteVars.getString(VIP_PAGE_MIN_ACTUAL_INTEREST_RATE_FOR_DAILY_UNIT, "/ hari");
  }

  public List<Long> getWhiteList() {
    String configStr = ecSiteVars.getString(PREFIX + "vip_confirm_white_list", "");
    if (StringUtils.isBlank(configStr)) {
      return new ArrayList<>();
    }
    return Arrays.stream(configStr.split(",")).map(Long::valueOf).collect(Collectors.toList());
  }
  //=======VIP 页面=========


  public long getNewHomepageStartBuild() {
    return ecSiteVars.getLong(NEW_HOMEPAGE_START_BUILD, 35813L);
  }

  public long getNewHomepageStartBuildWeb() {
    return ecSiteVars.getLong(NEW_HOMEPAGE_START_BUILD_WEB, 35313L);
  }

  public Long getNeedOrderPageUserIdlePopupV3StartVersion() {
    return ecSiteVars.getLong(NEED_ORDER_PAGE_USER_IDLE_POPUP_V3_START_VERSION, 35713L);
  }

  public String getMiddleMessageWithoutSmeEntranceForNewHomepage() {
    return ecSiteVars.getString(PREFIX + "middle_message_without_sme_for_new_homepage", "");
  }

  public String getMiddleMessageWithSmeEntranceForNewHomepage() {
    return ecSiteVars.getString(PREFIX + "middle_message_with_sme_for_new_homepage", "");
  }

  public String getVipNormalTextForVipNotCanOrder() {
    return ecSiteVars.getString(VIP_NORMAL_TEXT_FOR_VIP_NOT_CAN_ORDER, "");
  }

  public boolean isNewHomePageUIForNotLogin() {
    return ecSiteVars.getBoolean(NEW_HOME_PAGE_UI_FOR_NOT_LOGIN, true);
  }

  public boolean getActivityCountExcludeMgm() {
    return ecSiteVars.getBoolean(PREFIX + "activity_count_exclude_mgm", true);
  }

  public Long getCreateOrderPageStyleStartBuild() {
    return ecSiteVars.getLong(CREATE_ORDER_PAGE_STYLE_START_BUILD, 36013L);
  }

  public Long getDiscountAreaOldVersion() {
    return ecSiteVars.getLong(DISCOUNT_AREA_OLD_VERSION, 35913L);
  }

  public boolean needUploadPoint() {
    return ecSiteVars.getBoolean(NEED_UPLOAD_POINT, false);
  }

  public Long getCutInterestPopupStartVersion() {
    return ecSiteVars.getLong(CUT_INTEREST_POPUP_START_VERSION, 36113L);
  }

  /**
   * 用于测试环境控制规则获取方式
   *
   * @return
   */
  public boolean getGenneralRuleByDb() {
    return ecSiteVars.getBoolean(GENERAL_RULE_BY_DB, false);
  }

  public boolean needDiffHomepage(String deviceToken, IDNHomepageLoanStatusV5 status) {
    try {
      Double ratio = ecSiteVars.getConfVal(HOME_V5_API_DIFF, status.name(), 0.0d);
      return EcHashUtil.hitConsistentHashWithMurmurHash(HOME_API_DIFF,
          deviceToken, BigDecimal.valueOf(ratio));
    } catch (Exception e) {
      log.error("needDiffHomepage error", e);
      return false;
    }
  }

  /**
   * 走新首页代码获取首页数据
   *
   * @param deviceToken
   * @param status
   * @return
   */
  public boolean getHomeResponseForNewCode(String deviceToken, IDNHomepageLoanStatusV5 status) {
    try {
      String whiteList = ecSiteVars.getString(HOME_V5_GET_RESPONSE_STATUS_WHITE_LIST, "");
      if (Arrays.stream(whiteList.split(",")).anyMatch(item -> StringUtils.equalsIgnoreCase(item, deviceToken))) {
        return true;
      }
      Double ratio = ecSiteVars.getConfVal(HOME_V5_GET_RESPONSE_STATUS, status.name(), 0.0d);
      return EcHashUtil.hitConsistentHashWithMurmurHash(HOME_API_DIFF,
          deviceToken, BigDecimal.valueOf(ratio));
    } catch (Exception e) {
      log.error("getHomeResponseForNewCode error", e);
      return false;
    }
  }


  public List<String> ignoreUserInfoFields() {
    return JsonUtils.from(ecSiteVars.getString(PREFIX + "ignore_fields", "[\"tempRemainingTime\", \"remainTimeEndRisk\"]"), new TypeReference<List<String>>() {
    });
  }

  public Long getCouponStyleStartVersion() {
    return ecSiteVars.getLong(PREFIX + "couponStyleStartVersion", 36213L);
  }

  public boolean utDiff() {
    return false;
  }

  public boolean diffHomePoint() {
    return ecSiteVars.getBoolean(PREFIX + "diffHomePoint", false);
  }

  public Long getOrderPageAmountSlideBarVersion() {
    return ecSiteVars.getLong(PREFIX + "orderPageAmountSlideBarVersion", 36313L);
  }

  public long getAmountSlideBarTimeInterval() {
    return ecSiteVars.getLong(PREFIX + "amountSlideBarTimeInterval", 500L);
  }

  public Long getOrderPlanStyleFor363StartVersion() {
    return ecSiteVars.getLong(PREFIX + "orderPlanStyleFor363StartVersion", 36300L);
  }

  public Long getReserveLoanStyleButtonVersion() {

    return ecSiteVars.getLong(PREFIX + "reserveLoanStyleButtonVersion", 36300L);
  }

  public Long getCouponInvalidStyleVersion() {
    return ecSiteVars.getLong(PREFIX + "couponInvalidStyleButtonVersion", 36500L);
  }

  public Long getCreateOrderSignFormatVersion() {
    return ecSiteVars.getLong(HOME_V5_CREATE_ORDER_SIGN_FORMAT_VERSION, 36313L);
  }

  public Map<SignatureProvider, CreateOrderSignFormatConfigVO> getCreateOrderSignFormatConfigMap() {
    String config = ecSiteVars.getString(HOME_V5_CREATE_ORDER_SIGN_FORMAT_MAP, "{}");
    return JsonUtils.from(config, new TypeReference<Map<SignatureProvider, CreateOrderSignFormatConfigVO>>() {
    });
  }

  public Map<SignatureProvider, CreateOrderSignFormatConfigVO> getCreateOrderSignFormatConfigMapForH5() {
    String config = ecSiteVars.getString(HOME_V5_CREATE_ORDER_SIGN_FORMAT_MAP_FOR_API_CHANNEL, "{}");
    return JsonUtils.from(config, new TypeReference<Map<SignatureProvider, CreateOrderSignFormatConfigVO>>() {
    });
  }

  public Map<SignatureProvider, CreateOrderSignFormatConfigVO> getDialogSignFormatConfigMap() {
    String config = ecSiteVars.getString(HOME_V5_DIALOG_SIGN_FORMAT_CONFIG_MAP, "{}");
    return JsonUtils.from(config, new TypeReference<Map<SignatureProvider, CreateOrderSignFormatConfigVO>>() {
    });
  }

  public Map<SignatureProvider, CreateOrderSignFormatConfigVO> getDialogSignFormatConfigMapForH5() {
    String config = ecSiteVars.getString(HOME_V5_DIALOG_SIGN_FORMAT_CONFIG_MAP_FOR_API_CHANNEL, "{}");
    return JsonUtils.from(config, new TypeReference<Map<SignatureProvider, CreateOrderSignFormatConfigVO>>() {
    });
  }

  /**
   * 下单页月收入合规声明文案（OJK 年审固定句，服务端直出裸印尼语）；缺配置时以默认固定句兜底。
   */
  public String getMonthlyIncomeSignText() {
    return ecSiteVars.getString(MONTHLY_INCOME_SIGN_TEXT, MONTHLY_INCOME_SIGN_TEXT_DEFAULT);
  }

  public Long getProductDetailInterestChangeStartBuild() {
    return ecSiteVars.getLong(HOME_V5_PRODUCT_DETAIL_INTEREST_CHANGE_START_BUILD, Long.MAX_VALUE);
  }

  public String getReserveLoanTitle() {
    return ecSiteVars.getString(RESERVE_LOAN_TITLE, "有紧急资金需求，点击申请");
  }

  public String getReserveLoanUrl() {
    return ecSiteVars.getString(RESERVE_LOAN_URL, "");
  }

  public Long getReloanRejectedBubbleGuideVersion() {
    return ecSiteVars.getLong(PREFIX + "reloanRejectedBubbleGuideVersion", 36300L);
  }

  public ReloanRejectedBubbleVO getReloanRejectedBubbleVO() {
    String configStr = ecSiteVars.getString(HomepageV5Config.RELOAN_REJECTED_BUBBLED_DETAIL, "");
    return JsonUtils.from(configStr, ReloanRejectedBubbleVO.class);
  }

  public Boolean getProductDetailInterestChangeSwitchForH5() {
    return ecSiteVars.getBoolean(HOME_V5_PRODUCT_DETAIL_INTEREST_CHANGE_SWITCH_FOR_H5, Boolean.FALSE);
  }

  public Set<String> getHomeV5LoanMarketRejectNewHomepageWhitelist() {
    return Stream.of(ecSiteVars.getString(HOME_V5_LOAN_MARKET_REJECT_NEW_HOMEPAGE_WHITELIST, StringUtils.EMPTY).split(","))
        .filter(StringUtils::isNotBlank)
        .map(StringUtils::trim)
        .collect(Collectors.toSet());
  }

  public String getHomeV5LoanMarketRejectNewHomepageJumpUrl(YqgLocale locale, String normalizedMobileNumber) {
    String jumpUrl = ecSiteVars.getString(HOME_V5_LOAN_MARKET_REJECT_NEW_JUMP_URL, "");
    String nationalNumber = MobileConverter.normalizedToNationalOrNull(locale, normalizedMobileNumber);
    return String.format(jumpUrl, nationalNumber);
  }

  public String getHomeV5LoanMarketRejectNewHomepageTitle() {
    return ecSiteVars.getString(HOME_V5_LOAN_MARKET_REJECT_NEW_HOMEPAGE_TITLE, "");
  }

  public String getHomeV5LoanMarketRejectNewHomepageContent() {
    return ecSiteVars.getString(HOME_V5_LOAN_MARKET_REJECT_NEW_HOMEPAGE_CONTENT, "");
  }

  public String getHomeV5LoanMarketRejectNewHomepageButtonTitle() {
    return ecSiteVars.getString(HOME_V5_LOAN_MARKET_REJECT_NEW_HOMEPAGE_BUTTON_TITLE, "");
  }

  public String getQuoteManualReviewContent() {
    return ecSiteVars.getString(HOME_V5_QUOTE_MANUAL_REVIEW_CONTENT, "进入人工审核，可能会在9 - 18点收到Easycash电话核实");
  }

  public Long getAutoReviewTextTime() {
    return ecSiteVars.getLong(HOME_V5_QUOTE_AUTO_REVIEW_TIME, 30 * 1000L);
  }

  public Long getRejectHomeDisplayStrategyStartVersion() {
    return ecSiteVars.getLong(HOME_V5_REJECT_HOME_DISPLAY_STRATEGY_START_VERSION, 36500L);
  }

  public String getCancelLoanTipsPopUp() {
    return ecSiteVars.getString(HOME_V5_CANCEL_LOAN_TIPS_POP_UP, "{}");
  }

  public String getApplyForFutureTipsPopUp() {
    return ecSiteVars.getString(HOME_V5_APPLY_FOR_FUTURE_TIPS_POP_UP, "{}");
  }

  public String getReapplyPageTips() {
    return ecSiteVars.getString(HOME_V5_REAPPLY_PAGE_TIPS, "额度正在评估：需要补充个人资料");
  }

  public String getReapplyPageIcon() {
    return ecSiteVars.getString(HOME_V5_REAPPLY_PAGE_ICON, "");
  }

  public Long getRepaymentDisplayStrategyBuild() {
    return ecSiteVars.getLong(HOME_V5_REPAYMENT_DISPLAY_STRATEGY_BUILD, 36500L);
  }

  public String getOverdueLoanTipsPopUp() {
    return ecSiteVars.getString(HOME_V5_OVERDUE_LOAN_TIPS_POP_UP, "{}");
  }

  public String getRepaymentLoanTipsPopUp() {
    return ecSiteVars.getString(HOME_V5_REPAYMENT_LOAN_TIPS_POP_UP, "{}");
  }


  public boolean versionUpgradePopupExposureSwitch() {
    return ecSiteVars.getBoolean(VERSION_UPGRADE_POPUP_EXPOSURE_SWITCH, false);
  }

  public List<Long> versionUpgradePopupExposureWhiteList() {
    String configStr = ecSiteVars.getString(VERSION_UPGRADE_POPUP_EXPOSURE_WHITE_LIST, "");
    if (StringUtils.isBlank(configStr)) {
      return Lists.newArrayList();
    }
    return Arrays.stream(configStr.split(",")).map(Long::valueOf).collect(Collectors.toList());
  }

  public Long getMotherLastNameVersion() {
    return ecSiteVars.getLong(HOME_V5_MOTHER_LAST_NAME_VERSION, 36500L);
  }

  public Long getUserImmediateContactVersion() {
    return ecSiteVars.getLong(HOME_V5_USER_IMMEDIATE_CONTACT_VERSION, 36913L);
  }


  public boolean needShowProductDiscountCornerMark() {
    return ecSiteVars.getBoolean(HOME_V5_PRODUCT_DISCOUNT_CORNER_MARK_SWITCH, false);
  }

  public Long getOrderLimitConfirmStartVersion() {
    return ecSiteVars.getLong(HOME_V5_ORDER_LIMIT_CONFIRM_START_VERSION, 36300L);
  }

  public Long getUnavailableLoanAccountVersion() {
    return ecSiteVars.getLong(HOME_V5_UNAVAILABLE_LOAN_ACCOUNT_VERSION, 36600L);
  }

  public Long getHome366DisplayBuild() {
    return ecSiteVars.getLong(HOME_V5_366_DISPLAY_BUILD, 36600L);
  }

  public Long getCouponExpireDisplayStrategyVersion() {
    return ecSiteVars.getLong(HOME_V5_COUPON_EXPIRE_DISPLAY_STRATEGY_VERSION, 36700L);
  }

  public BigDecimal getRevolvingLoanMinRemainCredits() {
    return BigDecimal.valueOf(ecSiteVars.getLong(HOME_V5_REVOLVING_LOAN_MIN_CREDITS, 500000L));
  }

  public BigDecimal getLoanLimitAmountOverStepValue() {
    return BigDecimal.valueOf(ecSiteVars.getLong(HOME_V5_LOAN_LIMIT_AMOUNT_OVER_STEP_VALUE, 500000L));
  }

  public Long getRejectLoanMarketInfoStartVersion() {
    return ecSiteVars.getLong(HOME_V5_REJECT_LOAN_MARKET_INFO_START_VERSION, 36700L);
  }


  public String getRejectLoanMarketInfoUrl() {
    return ecSiteVars.getString(HOME_V5_REJECT_LOAN_MARKET_INFO_URL, "");
  }

  public String getRevolvingLoanRemainCreditsNotEnoughAndNoOverdueWithZeroCredits() {
    return ecSiteVars.getString(REVOLVING_LOAN_REMAIN_CREDITS_NOT_ENOUGH_AND_NO_OVERDUE_WITH_ZERO_CREDITS, "{}");
  }

  public String getRevolvingLoanRemainCreditsNotEnoughAndNoOverdue() {
    return ecSiteVars.getString(REVOLVING_LOAN_REMAIN_CREDITS_NOT_ENOUGH_AND_NO_OVERDUE, "{}");
  }

  public String getRevolvingLoanOverdue() {
    return ecSiteVars.getString(REVOLVING_LOAN_OVERDUE, "{}");
  }


  public String getRevolvingLoanHighLightTestWhenRemainCreditsNotEnough() {
    return ecSiteVars.getString(REVOLVING_LOAN_REPAYMENT_HIGH_LIGHT_WHEN_OVERDUE, " | 还款后可恢复额度");
  }

  public String getTipsIconForRevolvingLoan() {
    return ecSiteVars.getString(TIPS_ICON_FOR_REVOLVING_LOAN, "");
  }

  /**
   * 循环贷---审核中中/放款中---弹窗提示
   */
  public String getRevolvingLoanTipsPopUpWhenPaying() {
    return ecSiteVars.getString(REVOLVING_LOAN_TIPS_POP_UP_WHEN_PAYING, "{}");
  }

  /**
   * 循环贷---审核中中---弹窗提示
   */
  public String getRevolvingLoanCalcReviewLoanTipsPopUp() {
    return ecSiteVars.getString(REVOLVING_LOAN_TIPS_POP_UP_WHEN_REVIEW, "{}");
  }

  public String getOrderProcessTitleOfMainCard() {
    return ecSiteVars.getString(REVOLVING_LOAN_ORDER_PROCESS_TITLE_OF_MAIN_CARD, "订单流程");
  }

  public Map<String, String> getOrderProcessStepsOfMainCard() {
    String config = ecSiteVars.getString(REVOLVING_LOAN_ORDER_PROCESS_STEPS_MAP, "{}");
    return JsonUtils.from(config, new TypeReference<Map<String, String>>() {
    });
  }

  //循环贷用户，点击去还款之后弹窗配置
  public String getRevolvingLoanOverdueLoanTipsPopUp() {
    return ecSiteVars.getString(HOME_V5_REVOLVING_LOAN_OVERDUE_LOAN_TIPS_POP_UP, "{}");
  }

  public String getRevolvingRepaymentLoanTipsPopUp() {
    return ecSiteVars.getString(HOME_V5_REVOLVING_LOAN_REPAYMENT_TIPS_POP_UP, "{}");
  }

  public Long getHome367DisplayBuild() {
    return ecSiteVars.getLong(HOME_V5_367_DISPLAY_BUILD, 36700L);
  }

  public Long getRevolvingLoanAppVersion() {
    return ecSiteVars.getLong(HOME_V5_REVOLVING_LOAN_APP_VERSION, 36713L);
  }


  public Long getShowOrderGuideAnimationVersion() {
    return ecSiteVars.getLong(HOME_V5_ORDER_GUIDE_ANIMATION_VERSION, 36800L);
  }

  public Integer getShowOrderGuideAnimationTimes() {
    return ecSiteVars.getInt(HOME_V5_ORDER_GUIDE_ANIMATION_SHOW_TIMES, 3);
  }

  public Long getProductBubbleContentVersion() {
    return ecSiteVars.getLong(PRODUCT_BUBBLE_CONTENT_DISPLAY_VERSION, 36900L);
  }

  public Long getSubmitAdditionalBubbleContentVersion() {
    return ecSiteVars.getLong(SUBMIT_ADDITIONAL_INFO_BUBBLE_CONTENT_DISPLAY_VERSION, 36913L);
  }

  public Long getWebCreateOrderBuild() {
    return ecSiteVars.getLong(HOME_V5_ORDER_WEB_BUILD, 36900L);
  }

  public String getWebCreateOrderUrl() {
    return ecSiteVars.getString(HOME_V5_ORDER_WEB_URL, "");
  }

  public Long getHomeV5CanOrderPageRepayDiscountStyleVersion() {
    return ecSiteVars.getLong(HOME_V5_CAN_ORDER_PAGE_REPAY_DISCOUNT_STYLE_VERSION, 37000L);
  }

  public Long getHomePagePopUpStartBuild() {
    return ecSiteVars.getLong(HOME_PAGE_POP_UP_START_BUILD, 37200L);
  }

  public BigDecimal getNotLoanAmountRate() {
    return ecSiteVars.getBigDecimal(HOME_V5_NOT_LOAN_AMOUNT_RATE, new BigDecimal("0.9"));
  }

  public BigDecimal getLoanMaxAmountRate() {
    return ecSiteVars.getBigDecimal(HOME_V5_LOAN_MAX_AMOUNT_RATE, new BigDecimal("1.1"));
  }

  public BigDecimal getHighCreditsNotLoanAmountRate() {
    return ecSiteVars.getBigDecimal(HOME_V5_HIGH_CREDITS_NOT_LOAN_AMOUNT_RATE, new BigDecimal("0.6"));
  }

  public BigDecimal getQuickOrderHighCreditsThreshold() {
    return ecSiteVars.getBigDecimal(HOME_V5_QUICK_ORDER_HIGH_CREDITS_THRESHOLD, new BigDecimal("5000000"));
  }

  public Long getProductPageStyleFor371Version() {
    return ecSiteVars.getLong(HOME_V5_PRODUCT_PAGE_STYLE_FOR_371_VERSION, 37100L);
  }

  public boolean isHomeApiOptimizationWhiteUser(Long userId) {
    if (userId == null) {
      return false;
    }
    String whiteUserIds = ecSiteVars.getString(PREFIX + "home_api_optimization_white_list", "");
    if (StringUtils.isBlank(whiteUserIds)) {
      return false;
    }
    String[] ids = whiteUserIds.split(",");
    return Arrays.stream(ids).anyMatch(id -> id.equals(userId.toString()));
  }

  public Integer getCanOrderPageReturnButtonCreditsAcceptLimitDays() {
    return ecSiteVars.getInt(CAN_ORDER_PAGE_RETURN_BUTTON_POP_UP_CREDITS_ACCEPT_LIMIT_DAYS, 4);
  }

  public String getJumpOrderPageFrequencyLimitation(String sceneValue) {
    return ecSiteVars.getConfVal(JUMP_LEVEL_2_ORDER_PAGE_LIMITATION, sceneValue, "NONE");
  }

  public boolean checkUseLifeCycleSingleJumpLimit(Long userId) {
    return this.checkUseLifeCycleSingleJumpLimitSwitchOpen() || this.checkInUseLifeCycleSingleJumpLimitWhiteList(userId);
  }

  private boolean checkUseLifeCycleSingleJumpLimitSwitchOpen() {
    return ecSiteVars.getBoolean(USE_LIFE_CYCLE_SINGLE_JUMP_LIMIT_SWITCH, false);
  }

  private boolean checkInUseLifeCycleSingleJumpLimitWhiteList(Long userId) {
    return userId != null && ecSiteVars.getString(USE_LIFE_CYCLE_SINGLE_JUMP_LIMIT_WHITE_LIST, "").contains(userId.toString());
  }

  public long getLifeCycleExpireTimeInSec() {
    return ecSiteVars.getLong(LIFE_CYCLE_SINGLE_JUMP_LIMIT_EXPIRE_TIME_IN_SEC, 3 * 60 * 60L);
  }

  public String getLoanTipsPopupForAcceptButCanNotLoan() {
    return ecSiteVars.getString(PREFIX + "loan_tips_popup_for_can_not_loan", "{\"content\":\"可借额度不足，最低借款金额为{0}，请保持良好还款习惯，有助于恢复借款额度。\",\"buttonContent\":\"知道了\"}");
  }

  public boolean oldHomepageMiddleStructSwitch() {
    return ecSiteVars.getBoolean(OLD_HOMEPAGE_MIDDLE_STRUCT_SWITCH, true);
  }

  public BigDecimal getOptionalExpRateMainProduct() {
    return ecSiteVars.getBigDecimal(HOME_V5_OPTIONAL_AMOUNT_EXP_RATE_MAIN_PRODUCT, new BigDecimal("0.9"));
  }

  public BigDecimal getOptionalExpRateLowRepayProduct() {
    return ecSiteVars.getBigDecimal(HOME_V5_OPTIONAL_AMOUNT_EXP_RATE_LOW_REPAY_PRODUCT, new BigDecimal("0.8"));
  }

  public BigDecimal getOptionalExpRateReloanProduct() {
    return ecSiteVars.getBigDecimal(HOME_V5_OPTIONAL_AMOUNT_EXP_RATE_RELOAN_PRODUCT, new BigDecimal("0.5"));
  }

  public BigDecimal getOptionalExpScopeMax() {
    return ecSiteVars.getBigDecimal(HOME_V5_OPTIONAL_AMOUNT_EXP_SCOPE_MAX, new BigDecimal("10000000"));
  }

  public int getIntentionInfoExpireTimeInSeconds() {
    return ecSiteVars.getInt(INTENTION_INFO_EXPIRE_TIME_IN_SEC, 5 * 60);
  }

  public BigDecimal getOptionalExpScopeMin() {
    return ecSiteVars.getBigDecimal(HOME_V5_OPTIONAL_AMOUNT_EXP_SCOPE_MIN, new BigDecimal("5000000"));
  }

  public Long getNewHomepageVersion() {
    return ecSiteVars.getLong(HOME_V5_NEW_HOMEPAGE_VERSION, 37400L);
  }

  public Long getNewHomepageAuthUnFinishedVersion() {
    return ecSiteVars.getLong(HOME_V5_NEW_HOMEPAGE_AUTH_UNFINISHED_VERSION, 38000L);
  }

  public String getNewHomepageNotLoginUserDefaultHomepageVersion() {
    return ecSiteVars.getString(HOME_V5_NEW_HOMEPAGE_NOT_LOGIN_USER_HOMEPAGE_VERSION, "V2");
  }

  public BigDecimal getCashActivityExpRate() {
    return ecSiteVars.getBigDecimal(HOME_V5_CASH_ACTIVITY_EXP_RATE, new BigDecimal("0.9"));
  }

  public boolean needCheckRiskTag() {
    return ecSiteVars.getBoolean(PREFIX + "need_check_risk_tag", true);
  }

  public BigDecimal getFixedAmountRate() {
    return ecSiteVars.getBigDecimal(HOME_V5_FIXED_AMOUNT_RATE, new BigDecimal("1.1"));
  }

  public Long getOrderPageH5StartVersion() {
    return ecSiteVars.getLong(HOME_V5_ORDER_PAGE_H5_START_VERSION, 36900L);
  }

  public Long jumpAuthAfterLoginSeconds() {
    return ecSiteVars.getLong(JUMP_AUTH_AFTER_LOGIN_SECONDS, 60L);
  }

  public List<String> getRejectStatusList() {

    String statusList = ecSiteVars.getString(PREFIX + "last_status_rejected_list", "[]");
    return JsonUtils.from(statusList, new TypeReference<List<String>>() {
    });
  }

  public Long getBillJumpLevel2OrderPageVersion() {
    return ecSiteVars.getLong(BILL_JUMP_LEVEL_2_ORDER_PAGE_VERSION, 37200L);
  }

  public Long getHomePageFraudAlertBannerShowTime() {
    return ecSiteVars.getLong(HOME_PAGE_FRAUD_ALERT_BANNER_SHOW_TIME, 1440L);
  }

  public List<Long> getHomePageFraudAlertWhiteList() {
    String configStr = ecSiteVars.getString(HOME_PAGE_FRAUD_ALERT_BANNER_WHITE_LIST, "");
    if (StringUtils.isBlank(configStr)) {
      return Lists.newArrayList();
    }
    return Arrays.stream(configStr.split(",")).map(Long::valueOf).collect(Collectors.toList());
  }

  public Boolean getHomePageFraudAlertSwitch() {
    return ecSiteVars.getBoolean(HOME_PAGE_FRAUD_ALERT_BANNER_SWITCH, false);
  }

  public Long getRepayPlanCutInterestCouponId(boolean firstLoan) {
    if (firstLoan) {
      return ecSiteVars.getLong(REPAY_PLAN_CUT_INTEREST_COUPON_ID_FIRST_LOAN);
    } else {
      return ecSiteVars.getLong(REPAY_PLAN_CUT_INTEREST_COUPON_ID);
    }
  }

  public Long getJumpBillPageLimitVersion() {
    return ecSiteVars.getLong(PREFIX + "jump_bill_page_limit_version", 38400L);
  }

  /**
   * 获取首页免息卡功能开关
   * 开关关闭时，白名单用户仍然可以使用该功能
   *
   * @return 功能开关状态，默认关闭
   */
  public Boolean getHomeInterestFreeCardSwitch() {
    return ecSiteVars.getBoolean(PREFIX + "home_interest_free_card_switch", false);
  }

  /**
   * 获取首页免息卡白名单用户列表
   * 白名单用户可以跳过筛选条件，直接进入免息弹窗流程
   *
   * @return 白名单用户ID列表
   */
  public List<Long> getHomeInterestFreeCardWhiteList() {
    String configStr = ecSiteVars.getString(PREFIX + "home_interest_free_card_white_list", "");
    if (StringUtils.isBlank(configStr)) {
      return Lists.newArrayList();
    }
    return Arrays.stream(configStr.split(",")).map(Long::valueOf).collect(Collectors.toList());
  }

  /**
   * 检查用户是否在首页免息卡白名单中
   *
   * @param userId 用户ID
   * @return 是否在白名单中
   */
  public boolean isInHomeInterestFreeCardWhiteList(Long userId) {
    if (userId == null) {
      return false;
    }
    return getHomeInterestFreeCardWhiteList().contains(userId);
  }

  /**
   * 获取首页免息卡奖励信息（合并配置）
   * {
   * "effectiveTimeValue": 1,
   * "maxAmount": 10000000,
   * "days": 1
   * }
   */
  public HomeInterestFreeCardRewardVO getHomeInterestFreeCardRewardInfo() {
    String configStr = ecSiteVars.getString(HOME_INTEREST_FREE_CARD_REWARD_INFO, "{}");
    return Optional.ofNullable(JsonUtils.from(configStr, new TypeReference<HomeInterestFreeCardRewardVO>() {
    })).orElse(new HomeInterestFreeCardRewardVO());
  }

  public Long getHomeInterestFreeCardEffectiveTimeValue() {
    Long value = getHomeInterestFreeCardRewardInfo().getEffectiveTimeValue();
    return value == null ? 1L : value;
  }

  public String getHomeInterestFreeCardMaxAmount() {
    return getHomeInterestFreeCardRewardInfo().getMaxAmount();
  }

  public Integer getHomeInterestFreeCardDays() {
    Integer value = getHomeInterestFreeCardRewardInfo().getDays();
    return value == null ? 1 : value;
  }

  /**
   * 首页免息卡玩法 V2：核销后参与冷静期天数（默认14天，替代原核销限频子实验永久拦截规则）。
   *
   * @return 冷静期天数，默认 14
   */
  public Integer getHomeInterestFreeCardRedeemCooldownDays() {
    return ecSiteVars.getInt(HOME_INTEREST_FREE_CARD_REDEEM_COOLDOWN_DAYS, 14);
  }

  /**
   * 首页免息卡"已开奖"状态机滚动窗口天数（默认3天，与首页弹窗3天展示频控对齐）。
   * 用于 {@code HomeInterestFreeCardCacheLoader} 判断用户是否仍处于本轮"已开奖"周期内（阶段1 vs 阶段2/3），
   * 替代 V1 按自然日重置的"当天"缓存窗口。
   */
  public Integer getHomeInterestFreeCardDrawCycleDays() {
    return ecSiteVars.getInt(HOME_INTEREST_FREE_CARD_DRAW_CYCLE_DAYS, 3);
  }

  /**
   * 首页免息卡玩法 V2：动态 ruleId 决策 —— 用户当前借款方案下无可叠加最优券时的"新发券"发券工具 ID。
   * 上线前须在配置中心写入 key: home_interest_free_card_new_grant_rule_id=2042
   *
   * @return 新发券 ruleId，默认 2042
   */
  public Long getHomeInterestFreeCardNewGrantRuleId() {
    return ecSiteVars.getLong(HOME_INTEREST_FREE_CARD_NEW_GRANT_RULE_ID, 2042L);
  }

  /**
   * 首页免息卡玩法 V2：动态 ruleId 决策 —— 用户当前借款方案下已有可叠加最优券时的"锚定升级券"发券工具 ID。
   * 上线前须在配置中心写入 key: home_interest_free_card_anchor_upgrade_rule_id=2043
   *
   * @return 锚定升级券 ruleId，默认 2043
   */
  public Long getHomeInterestFreeCardAnchorUpgradeRuleId() {
    return ecSiteVars.getLong(HOME_INTEREST_FREE_CARD_ANCHOR_UPGRADE_RULE_ID, 2043L);
  }

  public boolean getSubHomePageFloatingIconDisplaySwitch() {
    return ecSiteVars.getBoolean(SUB_HOME_PAGE_FLOATING_ICON_DISPLAY_SWITCH, false);
  }

  public BigDecimal getHomepageDefaultInterestTip() {
    return ecSiteVars.getBigDecimal(PREFIX + "homepage_default_interest_tip", new BigDecimal("0.0001"));
  }

  public String getRepayPlanCutInterestCouponIdV2() {
    return ecSiteVars.getString(REPAY_PLAN_CUT_INTEREST_COUPON_ID_V2, "[{\"min\":0,\"couponToolId\":1875},{\"min\":3,\"couponToolId\":1876},"
        + "{\"min\":4,\"couponToolId\":1877},{\"min\":5,\"couponToolId\":1878},{\"min\":6,\"couponToolId\":1879},{\"min\":7,\"couponToolId\":1880},"
        + "{\"min\":8,\"couponToolId\":1881},{\"min\":9,\"couponToolId\":1882}]");
  }

  public String getRepayPlanCutInterestCouponIdV3() {
    return ecSiteVars.getString(REPAY_PLAN_CUT_INTEREST_COUPON_ID_V3, "[{\"min\":0,\"couponToolId\":1975},{\"min\":3,\"couponToolId\":1979},"
        + "{\"min\":4,\"couponToolId\":1980},{\"min\":5,\"couponToolId\":1981},{\"min\":6,\"couponToolId\":1982},{\"min\":7,\"couponToolId\":1983},"
        + "{\"min\":8,\"couponToolId\":1984},{\"min\":9,\"couponToolId\":1985}]");
  }

  public Long getSafetyModuleStartBuild() {
    return ecSiteVars.getLong(SAFETY_MODULE_START_BUILD, 38800L);
  }

  /**
   * 安全证照列表，JSON 格式: [{"name":"OJK & AFPI","subtitle":"Berizin dan diawasi","logoUrl":"https://..."},...]
   */
  private static final String REGULATOR_ITEMS_DEFAULT = "["
      + "{\"name\":\"OJK & AFPI\",\"subtitle\":\"Berizin dan diawasi\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/FkCzw3FVuv3jueCHxW32WFINwgDN.png\"},"
      + "{\"name\":\"KOMINFO\",\"subtitle\":\"Terdaftar di\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/Fi207oY1grJVylPqzAK3GWokdUHE.png\"},"
      + "{\"name\":\"Cyber Security\",\"subtitle\":\"Tersertifikasi dengan\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/FjQ_6YWFBTx2eolhI5xYfM8B6_GU.png\"},"
      + "{\"name\":\"LAPS SJK\",\"subtitle\":\"Anggota dari\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/FhSzJC5T-DNdC_UYXIg6c4dFqKSj.png\"}"
      + "]";

  public List<Map<String, String>> getRegulatorItems() {
    String json = ecSiteVars.getString(REGULATOR_ITEMS, REGULATOR_ITEMS_DEFAULT);
    return JsonUtils.from(json, new TypeReference<List<Map<String, String>>>() {
    });
  }

  private static final String MITRA_ITEMS_DEFAULT = "["
      + "{\"name\":\"Alfamart\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/Fr-kdXvzIymTL9gNX7P_NZHZ4SeM.png\"},"
      + "{\"name\":\"BRI\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/Fl-gE9ci8rGVpvv-QNPuyxVnJ2n4.png\"},"
      + "{\"name\":\"BNC\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/FpHK2oOKhh1SaiY3hQFlGFA_MZne.png\"},"
      + "{\"name\":\"BNI\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/Fs4uwU4mhEnDyVv9aeHdPvuFP8kY.png\"},"
      + "{\"name\":\"OVO\",\"logoUrl\":\"https://ec-cdn.easycash.id/upload/admin/FhA4-rFTDjDqFvkTIa9XkHa0Aqwo.png\"}"
      + "]";

  /**
   * 合作伙伴列表，JSON 格式: [{"name":"Alfamart","logoUrl":"https://..."},...]
   */
  public List<Map<String, String>> getMitraItems() {
    String json = ecSiteVars.getString(MITRA_ITEMS, MITRA_ITEMS_DEFAULT);
    return JsonUtils.from(json, new TypeReference<List<Map<String, String>>>() {
    });
  }

  /**
   * 获取头部用户券配置列表（从配置中心 JSON 反序列化）
   *
   * @return 头部用户券配置列表，配置为空或解析失败时返回空列表
   */
  public List<HeadMiddleCouponConfig> getRepayPlanHeadCouponConfigs() {
    String json = ecSiteVars.getString(REPAY_PLAN_HEAD_COUPON_CONFIG, "[]");
    List<HeadMiddleCouponConfig> configs = JsonUtils.from(json, new TypeReference<List<HeadMiddleCouponConfig>>() {
    });
    return configs == null ? Collections.emptyList() : configs;
  }

  /**
   * 头部+中部低意愿新实验入组所需过件时间阈值（分钟），默认 10 分钟
   *
   * @return 分钟阈值
   */
  public Long getRepayPlanHeadMiddleAcceptanceMinutes() {
    return ecSiteVars.getLong(REPAY_PLAN_HEAD_MIDDLE_ACCEPTANCE_MINUTES, 10L);
  }

  /**
   * 获取中部低意愿用户券配置列表（从配置中心 JSON 反序列化）
   *
   * @return 中部低意愿用户券配置列表，配置为空或解析失败时返回空列表
   */
  public List<HeadMiddleCouponConfig> getRepayPlanMiddleLowWillingCouponConfigs() {
    String json = ecSiteVars.getString(REPAY_PLAN_MIDDLE_LOW_WILLING_COUPON_CONFIG, "[]");
    List<HeadMiddleCouponConfig> configs = JsonUtils.from(json, new TypeReference<List<HeadMiddleCouponConfig>>() {
    });
    return configs == null ? Collections.emptyList() : configs;
  }

  /**
   * 获取 0630 子实验头部用户实验组券配置列表（A1/B1/C1）
   *
   * @return 实验组头部券配置列表，配置为空或解析失败时返回空列表
   */
  public List<HeadMiddleCouponConfig> getRepayPlanHeadExpCouponConfigs() {
    String json = ecSiteVars.getString(REPAY_PLAN_HEAD_EXP_COUPON_CONFIG, "[]");
    List<HeadMiddleCouponConfig> configs = JsonUtils.from(json, new TypeReference<List<HeadMiddleCouponConfig>>() {
    });
    return configs == null ? Collections.emptyList() : configs;
  }

  /**
   * 获取 0630 子实验中部低意愿用户实验组券配置列表（D1/E1/F）
   *
   * @return 实验组中部低意愿券配置列表，配置为空或解析失败时返回空列表
   */
  public List<HeadMiddleCouponConfig> getRepayPlanMiddleLowWillingExpCouponConfigs() {
    String json = ecSiteVars.getString(REPAY_PLAN_MIDDLE_LOW_WILLING_EXP_COUPON_CONFIG, "[]");
    List<HeadMiddleCouponConfig> configs = JsonUtils.from(json, new TypeReference<List<HeadMiddleCouponConfig>>() {
    });
    return configs == null ? Collections.emptyList() : configs;
  }

  /**
   * 首贷头/中部入组所需最小可借产品日利率阈值，默认 0.0015（即 0.15%）
   *
   * @return 日利率阈值
   */
  public BigDecimal getRepayPlanHeadMiddleMinDailyRateThreshold() {
    return ecSiteVars.getBigDecimal(REPAY_PLAN_HEAD_MIDDLE_MIN_DAILY_RATE_THRESHOLD, new BigDecimal("0.0015"));
  }

  /**
   * 获取还款计划发券 V3 固定折扣券 A 的发券工具 ID（无可用降息券用户路径）。
   * 上线前须在配置中心写入 key: repay_plan_v3_fixed_coupon_tool_id=2014
   *
   * @return 发券工具 ID，默认 2014
   */
  public Long getRepayPlanV3FixedCouponToolId() {
    return ecSiteVars.getLong(REPAY_PLAN_V3_FIXED_COUPON_TOOL_ID, 2014L);
  }

  /**
   * 获取还款计划发券 V3 锚定膨胀券 B 的发券工具 ID（有可用降息券用户路径）。
   * 上线前须在配置中心写入 key: repay_plan_v3_anchor_coupon_tool_id=2015
   *
   * @return 发券工具 ID，默认 2015
   */
  public Long getRepayPlanV3AnchorCouponToolId() {
    return ecSiteVars.getLong(REPAY_PLAN_V3_ANCHOR_COUPON_TOOL_ID, 2015L);
  }


  @Data
  @NoArgsConstructor
  public static class RepayEducationStaticTextConfig {
    private RepayEducationHomepageTextConfig homepage;
    private RepayEducationResourceTextConfig banner;
    private RepayEducationResourceTextConfig iconPopup;
    private RepayEducationResourceTextConfig rewardPopup;
  }

  @Data
  @NoArgsConstructor
  public static class RepayEducationHomepageTextConfig {
    private String title;
  }

  @Data
  @NoArgsConstructor
  public static class RepayEducationResourceTextConfig {
    private String firstLineTextWhenNoProgress;
    private String firstLineText;
    private String secondLineText;
    private String benefit1Desc;
    private String benefit1Info;
    private String benefit1BottomInfo;
    private String benefit2Desc;
    private String benefit2Info;
  }

  public List<OrderPageCarouselContentConfig> getOrderPageCarouselContentConfigForReloan() {
    String configStr = ecSiteVars.getString(ORDER_PAGE_CAROUSEL_CONTENT_CONFIG_FOR_RELOAN);
    if (StringUtils.isBlank(configStr)) {
      throw EcException.error("order_page_carousel_content_config_for_reloan is blank");
    }
    return JsonUtils.from(configStr, new TypeReference<List<OrderPageCarouselContentConfig>>() {
    });
  }

  public List<OrderPageCarouselContentConfig> getOrderPageCarouselContentConfigForFirstLoanStatic() {
    String configStr = ecSiteVars.getString(ORDER_PAGE_CAROUSEL_CONTENT_CONFIG_FOR_FIRST_LOAN);
    if (StringUtils.isBlank(configStr)) {
      throw EcException.error("order_page_carousel_content_config_for_first_loan is blank");
    }
    return JsonUtils.from(configStr, new TypeReference<List<OrderPageCarouselContentConfig>>() {
    });
  }

  public OrderPageCarouselContentConfig getOrderPageCarouselContentConfigForFirstLoanT0Content() {
    String configStr = ecSiteVars.getString(ORDER_PAGE_CAROUSEL_CONTENT_CONFIG_FOR_FIRST_LOAN_T0_CONTENT);
    if (StringUtils.isBlank(configStr)) {
      throw EcException.error("order_page_carousel_content_config_for_first_loan_t0_content is blank");
    }
    return JsonUtils.from(configStr, new TypeReference<OrderPageCarouselContentConfig>() {
    });
  }

  /**
   * 息费结构 3.0 首屏安全认证轮播文案（统一配置，不区分首贷/复贷）。
   */
  public List<OrderPageCarouselContentConfig> getOrderPageCarouselContentConfigForInterestSplitV3() {
    String configStr = ecSiteVars.getString(ORDER_PAGE_CAROUSEL_CONTENT_CONFIG_FOR_INTEREST_SPLIT_V3);
    if (StringUtils.isBlank(configStr)) {
      throw EcException.error("order_page_carousel_content_config_for_interest_split_v3 is blank");
    }
    return JsonUtils.from(configStr, new TypeReference<List<OrderPageCarouselContentConfig>>() {
    });
  }

  public static class OrderPageCarouselContentConfig {
    public String content;
    public List<String> args;
    public String iconUrl;
    public String color;//背景颜色
    public String textColor;//文本颜色
  }
}
