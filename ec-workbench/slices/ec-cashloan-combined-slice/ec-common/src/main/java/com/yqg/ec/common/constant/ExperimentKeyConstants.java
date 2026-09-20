package com.yqg.ec.common.constant;

public class ExperimentKeyConstants {

  public static final String KTP_VISUAL_OPTIMIZATION_EXPERIMENT_KEY = "product_operation_auth-auth-abroad-loan-ktp_UI_1015";

  public static final String REPAYMENT_ACCOUNT_EXP_KEY = "technology-repayment-abroad-loan_all-repay_va_refactor_tech_exp";

  public static final String LOAN_PAGE_V4_COUPON_UPGRADE_AREA = "product_operation_order-lending-abroad-loan_all-loan_page_v4_coupon_upgrade_area";

  public static final String PRODUCT_DETAIL_CHANGE_POPUP_PUSH_COUPON_V3 = "reloan_order_26h1-lending-abroad-renew-PUSH_COUPON_V3_strategy_26H1";

  public static final String JBP_ADD_NEW_PAYMENT_CHANNEL = "pretii-other-abroad-loan_all-jbp_add_new_payment_channel";

  public static final String JBP_PAYMENT_CHANNEL_DISPLAY_ORDER = "pretii-other-abroad-loan_all-jbp_payment_channel_display_order";

  public static final String FLIP_FULL_PROCESS_OR_LANDING_PAGE = "technology-install-abroad-loan-flip_h5FullProcessOrLandingPageExp";

  public static final String T0_RISK_ACCEPT_GRANT_CUT_COUPON_LOW_WILL = "first_loan_product_26h1-loan_not_withdraw-abroad-loan-Coupon5_0_low_will";

  public static final String T0_RISK_ACCEPT_GRANT_CUT_COUPON_HIGH_WILL = "first_loan_product_26h1-loan_not_withdraw-abroad-loan-Coupon3_0_high_will";

  public static final String RISK_ACCEPT_GRANT_CUT_COUPON_FATHER = "product_operation_order-not_withdraw-abroad-loan-first_loan_prom_order_father_exp";

  public static final String OVERDUE_REPAY_AC = "product_operation_bill_overdue-after_overdue-abroad-loan_all-reward_test";

  public static final String TENCENT_LIVING_SOURCE_FOR_H5 = "technology-other-abroad-loan-tencent_living_for_h5";

  public static final String AUTH_DECR_INTEREST_STYLE = "auth_product_26h1-auth-abroad-loan-interest_0_auth_0107";

  public static final String MIDTRANS_PERMATA_CIMB_DANAMON_NORMAL = "bill_normal_26h1-before_overdue-abroad-loan_all-Midtrans_Permata_Cimb_Danamon_normal";

  public static final String SEABANK_CHANNEL_NORMAL = "bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_Seabank";

  /**
   * 首贷长期临额实验key
   */
  public static final String FIRST_LOAN_LONG_TERM_CREDITS_EXP_KEY = "first_loan_product_26h1-not_withdraw-abroad-loan-firstloan_limit_coupon_parent_pro_26h1_copy";

  /**
   * 首贷优惠券长期实验 key
   */
  public static final String FIRST_LOAN_COUPON_LONG_TERM_EXPERIMENT =
      "first_loan_product_26h1-not_withdraw-abroad-loan-COUPON_LONG_TERM_EXPERIMEN_fist_loan";

  /**
   * 复贷长期临额实验key
   */
  public static final String RELOAN_LONG_TERM_CREDITS_EXP_KEY = "reloan_order_26h1-not_withdraw-abroad-loan_all-reloan_limit_coupon_parent_pro_26h1";

  /**
   * 复贷头部低意愿用户灌券实验（意愿模型）
   */
  @Deprecated
  public static final String RELOAN_TOP_LOW_WILL_COUPON =
      "reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v1";

  public static final String EASY_PLUS_PAY_V = "pretii-other-abroad-loan_all-EASYPLUS_PAY_V_V2";

  public static final String EASY_PLUS_PAY_DEBIT_GOPAY = "pretii-other-abroad-loan_all-jbp_debit_gopay";

  /**
   * 下单页区域限定券包实验key
   */
  public static final String SUB_HOME_PAGE_COUPON_PACKAGE = "reloan_order_26h1-lending-abroad-renew-RatingA_Regional_Coupon_Bundle";
  /**
   * 下单页接口重构陪跑实验
   * 实验组：B - 开启陪跑对比
   * 对照组：A - 不开启陪跑
   */
  public static final String PRODUCT_DETAIL_REFACTOR_COMPARE = "technology-lending-abroad-loan_all-product_detail_refactor_compare";

  /**
   * 下单页接口重构灰度切换实验
   * 实验组：B - 使用新逻辑
   * 对照组：A - 使用旧逻辑
   */
  public static final String PRODUCT_DETAIL_REFACTOR_SWITCH = "technology-lending-abroad-loan_all-product_detail_refactor_switch";
  /**
   * 复贷铺脸弹窗取消增发临额实验
   * 实验组：B - 不发临额
   * 对照组：A - 保持原逻辑发临额
   */
  public static final String RELOAN_NO_TEMP_CREDIT = "reloan_order_26h1-not_withdraw-abroad-loan_all-no_limit_coupon";

  public static final String GRANT_CREDITS_COUPON_FOR_OPEN_APP_TRACE_TIME = "reloan_order_26h1-not_withdraw-abroad-loan_all-reloan_limit_coupon_no_time";

  public static final String DIRECT_ACTIVITY_FOR_HOME_MESSAGE_NORMAL = "bill_normal_26h1-before_overdue-abroad-loan_all-Add_authorization_guide_entry";
  public static final String DIRECT_ACTIVITY_FOR_HOME_BANNER_NORMAL = "bill_normal_26h1-before_overdue-abroad-loan_all-Add_authorization_guide_banner";
  public static final String DIRECT_ACTIVITY_FOR_REPAY_NORMAL = "bill_normal_26h1-before_overdue-abroad-loan_all-Add_cashback";

  public static final String KTP_POPUP_STYLE_OPTIMIZATION_V1 = "auth_product_26h1-auth-abroad-loan-KTP_popup_V1_0303";

  /**
   * 首贷假提额 H5 全流程下单页承接专属主实验 key
   *
   * <p><b>为何独立：</b>端内沿用已推全的 {@code T0_order_page_popup_1224}，H5 与端内的入组分流必须完全隔离，
   * 避免 H5 流量稀释端内推全状态、且实验关量节奏可独立把控。
   *
   * <p><b>使用方：</b>同一个 key 同时被弹窗（{@code FirstLoanPopupDecisionSnapshot} 实时分流 ExpDiversionClient）
   * 与下单页文案（{@code T0LoanAcceptService} 粘性读 ExpLastResultRunningClient）使用，两端必须保持一致，
   * 否则会出现"弹窗按新实验弹、下单页 0 息文案不出"的不一致 bug。
   *
   * <p><b>分组返回值规范：</b>实验平台配置 {@code "CONTROL_GROUP"}（对照）/ {@code "EXPERIMENT_GROUP"}（实验）/
   * {@code BLANK_GROUP} 三档；而端内老实验 {@code T0_order_page_popup_1224} 返回 "A"/"B"。
   * 两套字面量不兼容，调用方在 H5 路径上必须经业务侧 {@code T0OrderPagePopupH5ExperimentHelper#normalizeGroupResult}
   * 统一规范为 "A"/"B"，复用既有"A 对照、B 实验"判定逻辑（含下游 {@code T0OrderPageInfoVO#isShow}），实现下游零改动。
   */
  public static final String T0_ORDER_PAGE_POPUP_H5 = "technology-loan_not_withdraw-abroad-loan-Interest_Free_Credit_Limit_h5";

  /** 首贷假提额弹窗频率提升实验 - 首贷常规头部 */
  public static final String T0_POPUP_FREQ_HEAD = "first_loan_product_26h1-not_withdraw-abroad-loan-fake_pop_up_order_page_0303";

  /** 首贷假提额弹窗频率提升实验 - 首贷常规中部 */
  public static final String T0_POPUP_FREQ_MIDDLE = "first_loan_product_26h1-not_withdraw-abroad-loan-fake_pop_up_order_page_0303_CD";

  /** 首贷假提额弹窗频率提升实验 - 尾部/回捞 */
  public static final String T0_POPUP_FREQ_TAIL = "first_loan_product_26h1-not_withdraw-abroad-loan-fake_pop_up_order_page_0303_Other";

  /** 首贷首页下单活动-奖励前置实验 Key */
  public static final String FIRST_LOAN_HOME_REWARD_EXP_KEY = "first_loan_product_26h1-not_withdraw-abroad-loan-firstloan_home_page_order_pre_reward_0317";

  /**
   * 【贷超】逾期分层实验
   */
  public static final String LOAN_MARKET_OVERDUE_TIER = "braavos-after_overdue-abroad-after_loan-OverdueUserAdjustmentto7";


  public static final String JIGUANG_IP_REGISTER = "register_26h1-register-abroad-loan-IP_registration_supplier_change";

  public static final String BANK_CARD_UNIQUE_KEY_WITH_BANK_CODE = "technology-auth-abroad-loan_all-bank_card_unique_key_with_bank_code";

  public static final String FIRST_LOAN_BUTTON_LOAN_LIMIT_AMOUNT = "first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_limit_amount_0407";

  /** ios 落地页 4.0 Meta(fb) 渠道样式实验 key */
  public static final String IOS_LANDING_PAGE_STYLE_FB = "technology-other-abroad-loan-metalandingpage4_0";

  /** ios 落地页 4.0 TikTok(tt) 渠道样式实验 key */
  public static final String IOS_LANDING_PAGE_STYLE_TT = "technology-other-abroad-loan-ttlandingpage4_0";
  /** 首贷真提额免息弹窗 2.0 实验 key */
  public static final String FIRST_LOAN_GENUINE_CREDIT_INCREASE_POPUP = "first_loan_product_26h1-not_withdraw-abroad-loan-Genuine_credit_limit_increase_interest_free_pop_up_0416";

  /** 首贷头部+中部低意愿用户还款计划发券实验 key */
  public static final String FIRST_LOAN_HEAD_MIDDLE_REPAY_PLAN_COUPON = "first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_abcd_repayment_plan_0416";

  /** 首贷头部+中部低意愿用户还款计划发券 0630 子实验 key：决定发新券（实验组）还是老券（对照组） */
  public static final String FIRST_LOAN_HEAD_MIDDLE_REPAY_PLAN_COUPON_0630 = "first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_abcd_repayment_plan_0630";

  /**
   * 首贷 T1 头中部回端加码发券实验 key（短期实验，TAPD-1364147）。
   *
   * <p>命中实验组的用户在 openApp 回端灌券入口按「当前是否有降息券 + 最高减免上限阈值 + 头/中部客群」
   * 分档发放 6 款券之一；对照组 / BLANK_GROUP / fallback 全部按现有 T0/TN 四分类链路继续走。
   */
  public static final String FIRST_LOAN_T1_HEAD_MIDDLE_MORE_DISCOUNT_0707 =
      "first_loan_product_26h1-not_withdraw-abroad-loan-morediscount0707";

  /**
   * 首贷下单页 Back 挽留加码灌券实验 key（短期实验，TAPD-1153182677001368547 / 实验 ID 13941）。
   *
   * <p>命中实验组的用户在下单页点击返回键、复用现有资源位接口 {@code /api/appResource} 处理链路时，
   * 按「头/中部低意愿客群 + 当前是否有降息券 + 历史最高减免上限阈值」分档发放新批次加码券
   * （可升不可降、最多 +1 档、防重复发）；对照组 / BLANK_GROUP / 空串 fallback 全部保持现网 Back 行为。
   */
  public static final String FIRST_LOAN_ORDER_BACK_BOOST_0729 =
      "first_loan_product_26h1-not_withdraw-abroad-loan-orderback0729";

  /**
   * 首贷 T1 头中部下单页「券翻倍」扑脸弹窗承接实验 key（子实验 13933，TAPD-1367558）。
   *
   * <p>父实验为 {@link #FIRST_LOAN_T1_HEAD_MIDDLE_MORE_DISCOUNT_0707}（13823，已全量）。
   * 门控只读父实验身份（{@code abResultByUserId}）、对本子实验入组分流（{@code abTestByUserId}）；
   * 命中实验组的用户在下单页展示「券翻倍」扑脸弹窗，不发券、不改券面。
   */
  public static final String FIRST_LOAN_T1_HEAD_MIDDLE_MORE_DISCOUNT_POPUP_0728 =
      "first_loan_product_26h1-not_withdraw-abroad-loan-morediscountPopup0728";


  /** 复贷下单页默认金额实验 key */
  public static final String RELOAN_DEFAULT_AMOUNT = "reloan_order_26h1-lending-abroad-renew-default_amount_strategy_V1";

  public static final String EC_SHOPEEPAY_DIRECT = "technology-lending-abroad-loan_all-EC_shopeepay_direct";

  /** 还款详情页 OVO 推荐标实验 */
  public static final String EC_OVO_TUIJIAN = "technology-lending-abroad-loan_all-EC_OVO_tuijian";

  public static final String DANA_REPAY_DIRECT_DEBIT_KEY = "bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_dana_normal";

  public static final String EXP_KEY_SHOPEEPAY_BALANCE_FIRST = "bill_normal_26h1-before_overdue-abroad-loan_all-repaymentchannel_shopeepay_directpay";

  /**
   * 复贷一键借款后置新样式 A/B 实验 key（TAPD-1353291）
   *
   * <p>嵌套于上层 {@code reloan_order_26h1-lending-abroad-renew-one_click_loan_more_new_v1} 实验之后，
   * 用于在已确定入组一键借款后置弹窗的人群中，再做"权益清单 / 小票 / 真诚版"样式分流。
   *
   * <p>分组返回值规范：CONTROL_GROUP（真诚版基线）/ EXPERIMENT_GROUP_ONE（权益清单）/
   * EXPERIMENT_GROUP_TWO（小票）/ BLANK_GROUP（空白对照）。
   */
  public static final String RELOAN_ONE_CLICK_LOAN_MORE_STYLE_V1 =
      "reloan_order_26h1-lending-abroad-renew-new_one_click_loan_more_style_v1";

  /**
   * OTP 兜底「选择验证方式」入口实验 key（TAPD-356251·技术泳道，前缀 {@code technology-other-abroad-loan_all-}）。
   *
   * <p><b>覆盖场景：</b>银行卡绑定 / 注销用户 / 修改手机号 / 设置密码（含设置登录密码 + 设置交易密码）5 个敏感操作的
   * OTP 验证页。所有场景<b>共享同一实验 key</b>，实验只决定能否看见「选择验证方式」入口，scene 仅作前端入参用于
   * 拼弹窗 URL 与选 generalConfig key，不作为实验分流维度。
   *
   * <p><b>使用方（DIVERSION 入组 + LAST_RESULT_RUNNING 粘性读配对）：</b>同一个 key 同时被两端使用——
   * 实验入组接口 {@code OtpAlternativeVerificationService} 用 {@code ExpDiversionClient#getResult} 做<b>唯一一次实际入组</b>；
   * generalConfig {@code SensitiveSceneVerificationConfigProvider} 用 {@code ExpLastResultRunningClient#getResult}
   * <b>粘性读已入组结果</b>，不重复入组。前端须先调入组接口、再调 generalConfig，否则读不到入组结果。
   *
   * <p><b>分组返回值规范：</b>实验平台配置 {@code "CONTROL_GROUP"}（对照）/ {@code "EXPERIMENT_GROUP"}（实验）/
   * {@code BLANK_GROUP}（空白对照）三档；命中判定为 {@code getResult(...) == "EXPERIMENT_GROUP"}，其余一律按未命中
   * 返回空 dialogUrl / 不下发方式（NFR-001 安全降级）。
   *
   * <p><b>版本门禁：</b>App 版本 ≥ 39000 才入组，由实验中台 build 区间（{@code ABTestConfig#getExpKeyValidBuildMap}）
   * 配置判定，<b>代码侧不写死</b>。
   */
  public static final String OTP_ALTERNATIVE_VERIFICATION_ENTRY =
      "technology-other-abroad-loan_all-OTP_alternative_verification_entry";

  /**
   * 语音 OTP「选择验证方式」语音子实验 key（TAPD-1358587·技术泳道，前缀 {@code technology-other-abroad-loan_all-}）。
   *
   * <p><b>覆盖场景：</b>在 615 父实验 {@link #OTP_ALTERNATIVE_VERIFICATION_ENTRY} 基础上，决定「选择验证方式」列表
   * 是否追加语音（{@code VerificationNotifType.IVR}）。服务<b>场景 1（下单）/ 场景 2（615 四场景）/ 场景 4（借贷钱包提现）</b>
   * 三类有 userId 的登录态入口；<b>场景 3（端内 native 登录前）与端外场景 5-8</b> 无可靠 userId / 无 App build，
   * <b>不入组本实验</b>，改由开关控制（见 plan Decision 1/2）。
   *
   * <p><b>与父实验关系：</b>
   * <ul>
   *   <li><b>场景 2</b>：在 615 父实验 {@code EXPERIMENT_GROUP} 命中（已展示入口）之后，再用本 key 分流决定是否追加语音，
   *       <b>不跳过父实验</b>——未命中父实验者无入口、不进入语音判断。</li>
   *   <li><b>场景 1 / 场景 4</b>：无父实验，登录态有 userId，<b>直接 DIVERSION 分流</b>本 key 决定是否追加语音。</li>
   * </ul>
   *
   * <p><b>使用方（DIVERSION 入组 + LAST_RESULT_RUNNING 粘性读配对，与 615 同范式）：</b>
   * 场景 1/4 在各自 generalConfig provider 内用 {@code ExpDiversionClient#getResult} 做实时入组分流；
   * 场景 2 入组在 {@code OtpAlternativeVerificationService#getVerificationDialog}（615 父实验入组之后再对本 key 入组），
   * generalConfig {@code SensitiveSceneVerificationConfigProvider} 用 {@code ExpLastResultRunningClient#getResult}
   * <b>粘性读已入组结果</b>，不重复入组，保证同一用户分组稳定。判定统一收敛到 {@code OtpVoiceOptionService}。
   *
   * <p><b>分组返回值规范：</b>实验平台配置 {@code "CONTROL_GROUP"}（对照）/ {@code "EXPERIMENT_GROUP"}（实验）/
   * {@code BLANK_GROUP}（空白对照）三档；命中判定为 {@code getResult(...) == "EXPERIMENT_GROUP"}，其余一律按未命中
   * 不追加语音（NFR-001 安全降级）。
   *
   * <p><b>版本门禁：</b>App 版本 ≥ 39100（高于 615 入口 39000）才入组，由实验中台 build 区间
   * （{@code ABTestConfig#getExpKeyValidBuildMap}）配置判定，<b>代码侧不写死</b>；端外场景无 App build、不走版本门禁。
   */
  public static final String OTP_VOICE_VERIFICATION_OPTION =
      "technology-other-abroad-loan_all-OTP_voice_verification_option";

  /**
   * 下单 WA OTP 自动回填 - 首贷下单实验 key（TAPD-356514，实验 ID 13758，首贷产品转化泳道）。
   *
   * <p>按 {@code userId} 分流，客群为「首贷未下单」（{@code isReloanByUserId == false}）。命中实验组时
   * createOrderCheck 下发 {@code autoBackfillOtp=true}，下单发码粘性读同 key 携带 {@code autoInput=true}。
   *
   * <p>仅 Android、排除 API/H5 全流程渠道；版本门槛由 {@code ABTestConfig#getExpKeyValidBuildMap}
   * 的 build 区间（≥ 39200）控制，代码侧不写死。命中判定为 {@code getResult(...) == "EXPERIMENT_GROUP"}，
   * 其余（对照/空白/未入组/异常）一律按未命中处理（fail-safe）。
   */
  public static final String FIRST_LOAN_WA_OTP_AUTOFILL =
      "first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_auto_waotp0625";

  /**
   * 下单 WA OTP 自动回填 - 复贷下单实验 key（TAPD-356514，实验 ID 13759，复贷下单泳道）。
   *
   * <p>按 {@code userId} 分流，客群为「复贷未下单」（{@code isReloanByUserId == true}）。命中实验组时
   * createOrderCheck 下发 {@code autoBackfillOtp=true}，下单发码粘性读同 key 携带 {@code autoInput=true}。
   *
   * <p>生效条件、命中判定、fail-safe 同 {@link #FIRST_LOAN_WA_OTP_AUTOFILL}。
   */
  public static final String RELOAN_WA_OTP_AUTOFILL =
      "reloan_order_26h1-not_withdraw-abroad-renew-auto_waotp0625";

  /**
   * 下单生物识别验证 - 首贷下单实验 key（TAPD-364294，首贷产品泳道 / lending 场景）。
   *
   * <p>按 {@code userId} 分流，客群为「首贷未下单」（{@code isReloanByUserId == false}）。命中实验组时
   * createOrderCheck 下发 {@code needBiometricVerify=true}，App 侧改用生物识别替代 OTP 完成下单验证；
   * 未开启生物识别的命中用户走引导弹窗（US2）。
   *
   * <p>仅 Android、排除 API 渠道与 H5 全流程，App 版本 build ≥
   * {@code OrderBiometricVerifyService.BIOMETRIC_ORDER_VERIFY_MIN_BUILD}，且总开关
   * {@code user.biometric_credential_order_verify_enable} 打开——上述门控全部由业务侧
   * {@code OrderBiometricVerifyService} 在分流前显式判定（{@code IExperimentAdapter} 不做内建门禁）。
   *
   * <p>命中判定为分流结果 {@code == "EXPERIMENT_GROUP"}，其余（对照 / 空白 / 未入组 / 异常）
   * 一律按未命中处理（fail-safe，NFR-003 / NFR-004）。按
   * {@code .cursor/rules/ec-abtest-integration.mdc} §3，上线时不要把本 key 加进 zk 项
   * {@code ab_test.api_channel_valid_exp_key_allow_list}。
   */
  public static final String FIRST_LOAN_ORDER_BIOMETRIC_VERIFY =
      "first_loan_product_26h1-lending-abroad-loan-Biometrik";

  /**
   * 下单生物识别验证 - 复贷下单实验 key（TAPD-364294，复贷下单泳道 / lending 场景）。
   *
   * <p>按 {@code userId} 分流，客群为「复贷未下单」（{@code isReloanByUserId == true}）。
   * 生效条件、命中判定、fail-safe 同 {@link #FIRST_LOAN_ORDER_BIOMETRIC_VERIFY}。
   */
  public static final String RELOAN_ORDER_BIOMETRIC_VERIFY =
      "reloan_order_26h1-lending-abroad-renew-Biometrik";

  /**
   * 注册登录 WA OTP SDK 升级实验 key（TAPD-356514，实验 ID 13761，技术泳道）。
   *
   * <p>按 {@code deviceToken} 分流，客群为「未注册登录用户」。命中实验组时 GET unionMobilePreCheck
   * 通过独立新字段 {@code waOtpSDKEnable=true} 下发「使用新 SDK 方案」，不影响原 {@code needReadWaCode} 开关。
   *
   * <p>额外入组门槛：{@code nextPage ∈ {LOGIN_WITH_OTP, REGISTER}} 且 Android 操作系统版本 ≥ 7；
   * 仅 Android、排除 API/H5 全流程渠道；版本门槛由 build 区间（≥ 39200）控制。命中判定与 fail-safe 同上。
   */
  public static final String REGISTER_WA_OTP_SDK =
      "technology-register-abroad-loan-waotpsdk";

  /**
   * 回捞/重审「非首笔放款 90 天管制期」用户级 AB 实验 key（TAPD-364257）。
   *
   * <p>按 {@code userId} 分流，Kafka 服务端 {@code ExpDiversionClient} 入组；命中实验组(B) 才赋 90 天管制期。
   */
  public static final String RETRIEVAL_REAPPLY_NON_FIRST_90D_LOCK =
      "risk_decision_userid-other-abroad-risk_decision-retrieval_reapply_non_first_90d_lock";

  /**
   * 防结清长期 holdout 实验 key（TAPD-1364396）。
   *
   * <p>分组：80% {@code EXPERIMENT_GROUP} / 20% {@code CONTROL_GROUP}；写路径 {@code ExpDiversionClient}，
   * 读路径 {@code ExpLastResultRunningClient} 粘性只读。
   */
  public static final String ANTI_SETTLEMENT_LONG_TERM_EXPERIMENT =
      "26h2_11_reloan_mkt-lending-abroad-renew-COUPON_ANTI_LOSS_LONG_TERM_EXP";

  /**
   * 防结清场景无门槛静默灌券 V1（TAPD-1364841）。
   */
  public static final String ANTI_SETTLEMENT_SILENT_COUPON_V1 =
      "26h2_11_reloan_mkt-lending-abroad-renew-anti_loss_Silent_Coupon_V1";

  /**
   * 复贷防结清 —— 关怀对话式挽留弹窗 2.0 本试验 key（TAPD-365113）。
   *
   * <p>命中判定严格：仅 {@link com.yqg.core.common.UserFlowConstants#EXPERIMENT_GROUP} 视为命中；
   * {@code null} / {@code CONTROL_GROUP} / {@code BLANK_GROUP} / {@code EXPERIMENT_GROUP_ONE} 均视为未命中。
   *
   * <p>长期实验复用 {@link #ANTI_SETTLEMENT_LONG_TERM_EXPERIMENT}（防结清长期 holdout），
   * 在 filter 侧串联在本试验命中之后判定。
   */
  public static final String RELOAN_RETAIN_POP_V2_KEY =
      "26h2_11_reloan_mkt-lending-abroad-renew-anti_loss_Questionnaire";

  /**
   * API 渠道首笔借款用户回到端内补件测额实验。
   */
  public static final String API_RETURN_IN_APP_CREDIT =
      "reloan_order_26h1-not_withdraw-abroad-renew-APIgetlimit";

  /**
   * 按时还款成长积分体系 MVP 阶段实验 key（TAPD-1346811）。
   *
   * <p>历史存量 key，从 {@code PointEligibilityService} 内部私有常量迁移而来；本次重开（TAPD-370748）
   * 不再复用此 key 做新增入组判断，仅供 {@code PointVisibilityService} 只读识别「旧实验（MVP）阶段已入组」用户身份。
   */
  public static final String USER_POINTS_MVP_EXPERIMENT_KEY =
      "bill_normal_26h1-other-abroad-loan_all-user_points";

  /**
   * 按时还款成长积分体系重开实验 key（调整入组人群，仅限还款新用户，TAPD-370748）。
   *
   * <p>分组：50% {@code EXPERIMENT_GROUP} / 50% {@code CONTROL_GROUP}；写路径 {@code IExperimentAdapter#abTestByUserId}。
   * 入组条件：还款新用户（放款笔数=1 且当前在贷）+ 未命中防结清长期实验（{@link #ANTI_SETTLEMENT_LONG_TERM_EXPERIMENT}）对照组。
   */
  public static final String POINTS_REPAY_NEW_USER_COHORT_EXPERIMENT =
      "bill_normal_26h1-other-abroad-loan_all-user_points_newbill";

  /**
   * 首贷常规 E 评级授信 10min 未下单离线灌券实验 key（TAPD-1371553）。
   *
   * <p>新建实验，分流键 user_id；分组仅对照组 + 实验组，50/50，不设空白组。
   * 实验组字面量 {@code EXPERIMENT_GROUP} / {@code EXPERIMENT_GROUP_ONE} 发本实验券。
   */
  public static final String FIRST_LOAN_ERANK_10MIN_COUPON =
      "first_loan_product_26h1-loan_not_withdraw-abroad-loan-Coupon7_0_E_level";
}
