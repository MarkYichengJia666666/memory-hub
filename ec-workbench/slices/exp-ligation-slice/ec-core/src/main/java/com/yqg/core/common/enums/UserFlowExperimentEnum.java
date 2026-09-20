package com.yqg.core.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户流程相关实验
 */
@Getter
@AllArgsConstructor
public enum UserFlowExperimentEnum {

  // --- 首页相关实验 ---
  HOMEPAGE_FRAUD_WARNING("technology-other-abroad-loan_all-homepage_fraud_warning", "首页反黑产宣传语实验"),
  // 额度专项1.0-首页大卡&额度中心改版（TAPD-361491，首贷/复贷 × 首页大卡/额度中心 4 个实验）
  FIRST_LOAN_AMOUNT_HOME_CARD("first_loan_product_26h1-not_withdraw-abroad-loan-1_0cardandcredit0701", "首贷-额度专项1.0-首页大卡改版"),
  RELOAN_AMOUNT_HOME_CARD( "reloan_order_26h1-not_withdraw-abroad-renew-1_0cardandcredit0701", "复贷-额度专项1.0-首页大卡改版"),
  FIRST_LOAN_AMOUNT_CENTER("first_loan_product_26h1-not_withdraw-abroad-loan-1_0LimitCenter0701", "首贷-额度专项1.0-额度中心改版"),
  RELOAN_AMOUNT_CENTER("reloan_order_26h1-not_withdraw-abroad-renew-1_0LimitCenter070","复贷-额度专项1.0-额度中心改版"),
  // 首页安全模块相关实验
  HOME_PAGE_SAFETY_MODULE_UNIFIED("ops_product_top_26h1-register-abroad-loan_all-home_page_safety", "首页安全资质模块-整合版"),
  /** @deprecated 已整合至 {@link #HOME_PAGE_SAFETY_MODULE_UNIFIED} */
  @Deprecated
  HOME_PAGE_SAFETY_MODULE("reloan_order_26h1-lending-abroad-renew-home_page_safety_V1", "复贷首页安全资质模块"),
  /** @deprecated 已整合至 {@link #HOME_PAGE_SAFETY_MODULE_UNIFIED} */
  @Deprecated
  HOME_PAGE_SAFETY_MODULE_NOT_LOGIN("register_26h1-register-abroad-loan-without_login_home_page_safe_0401", "未登录首页安全资质模块"),
  /** @deprecated 已整合至 {@link #HOME_PAGE_SAFETY_MODULE_UNIFIED} */
  @Deprecated
  HOME_PAGE_SAFETY_MODULE_FIRST_LOAN("first_loan_auth_26h1-not_withdraw-abroad-loan-auth_firstloan_home_page_safe_0401", "首贷首页安全资质模块"),

  // 印尼 817 国庆端内氛围
  NATIONAL_DAY_817_FIRST_LOAN("first_loan_product_26h1-lending-abroad-loan-Rayakan_Kemerdekaan", "817国庆氛围-首贷产品泳道"),
  NATIONAL_DAY_817_RELOAN("reloan_order_26h1-lending-abroad-loan_all-Rayakan_Kemerdekaan", "817国庆氛围-复贷下单泳道"),

  // --- 首页相关实验 ---

  // --- 协议相关实验 ---
  // 下单页月收入弱化实验（TAPD-1153182677001368555）
  ORDER_PAGE_DE_EMPHASIZE(
      "technology-not_withdraw-abroad-loan_all-De_emphasize",
      "下单页月收入弱化实验：实验组下单页不展示月收入声明，弹窗协议保持不变"),
  // --- 协议相关实验 ---

  // --- APP灌券相关实验 ---
  RELOAN_HEAD_INSTANT_GRANT_COUPON_NORMAL_V1("26h2_11_reloan_mkt-lending-abroad-renew-instant_grant_coupon_normal_v1", "复贷常规-头部-回端灌首期无门槛券"),
  RELOAN_MID_INSTANT_GRANT_COUPON_SUB_NORMAL_V1("26h2_11_reloan_mkt-lending-abroad-renew-instant_grant_coupon_sub_normal_v1", "复贷常规-中部-回端灌首期无门槛券"),
  // --- APP灌券相关实验 ---

  // 下单页相关实验
  FIRST_LOAN_RECOMMENDED_LOAN("first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_recommended_loan", "首贷下单页-推荐借款方案弹窗"),
  RELOAN_PROTOCOL_CONFIRM_FULL_SCREEN("reloan_order_26h1-lending-abroad-renew-protocol_confirm_full_screen", "复贷强化协议浮层全屏版实验"),
  DUPLICATE_LOAN_INTERCEPT(
      "technology-lending-abroad-loan_all-duplicate_loan_intercept",
      "短时间内重复借款拦截弹窗实验"),
  QUICK_CONFIRM_REMINDER("technology-lending-abroad-loan_all-loan_quick_tips", "确认前零交互+极速确认+当日重复进页 温馨提示二次确认弹窗实验"),
  @Deprecated
  DISCOUNT_MODEL("technology-renew_not_withdraw-abroad-renew-discount_expe", "优惠模型实验分流"),
  HIDE_INCREASE_CREDIT_ENTRANCE_FOR_FIRSTLOAN_RETRIEVAL("technology-other-abroad-loan_all-hide_increase_credit_entrance_for_firstloan_retrieval", "首贷回捞用户隐藏增信提额入口"),
  FIRST_LOAN_ORDER_TO_BOOST("first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_Order_to_Boost_0529", "【首贷】账户成长2.0:下单提额降息"),
  RELOAN_LIMIT_COUPON_NO_LIMIT_V2("reloan_order_26h1-not_withdraw-abroad-loan_all-reloan_limit_coupon_no_limit_v2", "复贷登录APP静默时发临额-放开下单资格限制2.0"),
  RELOAN_LIMIT_COUPON_NO_LIMIT_V3("reloan_order_26h1-not_withdraw-abroad-loan_all-reloan_limit_coupon_no_limit_v3", "复贷登录APP静默时发临额-放开无风控临额上限人群"),
  ORDER_PAGE_PUSH_COUPON_V4("reloan_order_26h1-lending-abroad-renew-orderpage_push_coupon_v4", "下单页-修改金额发券4.0"),
  NO_INTEREST_POPUP_V2("reloan_order_26h1-lending-abroad-renew-bebas_bunga_V2_explore_bonus", "下单页-免息天数包装2.0"),
  ORDER_RETAIN_POPUP("reloan_order_26h1-lending-abroad-renew-retain_paypal_design", "复贷下单页-挽留弹窗实验"),
  RELOAN_RETAIN_FREQUENCY_LIMIT_V1("reloan_order_26h1-lending-abroad-renew-retention_limit_v1", "复贷下单页-挽留弹窗限频实验"),
  ONE_CLICK_SUPER_SUBSIDY_V1("reloan_credit_use_26h1-lending-abroad-loan_all-one_click_super_subisidy_v1", "下单激励-超级补贴v1"),
  ONE_CLICK_BENEFIT_LIST_EXP("reloan_credit_use_26h1-lending-abroad-loan_all-one_click_bonus_list_26H1", "一键借款后置权益清单实验"),
  RELOAN_TOP_WILLING_COUPON_V2("reloan_order_26h1-lending-abroad-renew-top_willing_coupon_v2", "复贷头部低意愿用户灌券-策略V2/V3共用"),
  @Deprecated
  RELOAN_AGENT_COUPON_TRAFFIC_STEERING("reloan_order_26h1-lending-abroad-renew-Coupon_agent_TrafficSteering_V1", "复贷头部低意愿回端灌券-Agent圈人策略子实验(v1.0，已下线)"),
  RELOAN_AGENT_COUPON_V1_1_EXCLUDE("26h2_11_reloan_mkt-lending-abroad-renew-Coupon_agent_TrafficSteering_V1_1_exclude", "复贷头部低意愿回端灌券-Agent排除策略子实验v1.1"),
  @Deprecated
  ANTI_SETTLEMENT_PAYOFF_CHURN_EXCLUDE_AGENT("26h2_11_reloan_mkt-lending-abroad-renew-anti_loss_Silent_Coupon_V1_exclude_agent", "防结清静默灌券-payoff_churn Agent排除策略子实验v1(已被v2替换，保留声明供历史监控追溯)"),
  ANTI_SETTLEMENT_PAYOFF_CHURN_EXCLUDE_AGENT_V2("26h2_11_reloan_mkt-lending-abroad-renew-anti_loss_Silent_Coupon_V2_exclude_agent", "防结清静默灌券-payoff_churn Agent排除策略迭代实验v2"),
  ORDER_PAGE_RATE_DISCOUNT_FIRST_LOAN("first_loan_product_26h1-lending-abroad-loan-bunga_free_one", "下单页-免一期首贷"),
  ORDER_PAGE_RATE_DISCOUNT_RE_LOAN("reloan_order_26h1-lending-abroad-renew-bunga_free_one", "下单页-免一期复贷"),
  INTEREST_SPLIT_FIRST_LOAN("first_loan_product_26h1-lending-abroad-loan-bunga_Restructuring", "息费结构包装1.0-首贷"),
  INTEREST_SPLIT_RE_LOAN("reloan_order_26h1-lending-abroad-renew-bunga_Restructuring", "息费结构包装1.0-复贷"),
  INTEREST_SPLIT_V2_FIRST_LOAN("first_loan_product_26h1-lending-abroad-loan-bunga_Restructuring2", "息费结构包装2.0/3.0-首贷"),
  INTEREST_SPLIT_V2_RE_LOAN("reloan_order_26h1-lending-abroad-renew-bunga_Restructuring2", "息费结构包装2.0/3.0-复贷"),
  INTEREST_SPLIT_V4_FIRST_LOAN("first_loan_product_26h1-lending-abroad-loan-bunga_Restructuring4", "息费结构包装4.0-首贷"),
  INTEREST_SPLIT_V4_RE_LOAN("reloan_order_26h1-lending-abroad-renew-bunga_Restructuring4", "息费结构包装4.0-复贷"),
  INTEREST_SPLIT_V5_FIRST_LOAN("first_loan_product_26h1-lending-abroad-loan-bunga_Restructuring5", "息费结构包装5.0-首贷"),
  INTEREST_SPLIT_V5_RE_LOAN("reloan_order_26h1-lending-abroad-renew-bunga_Restructuring5", "息费结构包装5.0-复贷"),
  HOME_INTEREST_FREE_CARD_REDEEM_LIMIT("reloan_order_26h1-lending-abroad-renew-bebas_bunga_card_limit_v1", "首页免息卡核销频控实验"),
  HOME_INTEREST_FREE_CARD_V2("26h2_11_reloan_mkt-lending-abroad-renew-homepage_bebas_bunga_card_V2", "首页免息卡实验V2（实验ID 13781）"),
  HOME_INTEREST_FREE_CARD_V3_HEAD(
      "26h2_11_reloan_mkt-lending-abroad-renew-homepage_bebas_bunga_card_head_normal",
      "首页免息卡实验V3头部"),
  HOME_INTEREST_FREE_CARD_V3_MIDDLE(
      "26h2_11_reloan_mkt-lending-abroad-renew-homepage_bebas_bunga_card_middle_normal",
      "首页免息卡实验V3中部"),
  ORDER_PAGE_PUSH_COUPON_FIRST_LOAN("first_loan_product_26h1-loan_not_withdraw-abroad-loan-changeandcoupon_rankAB", "下单页-首贷修改金额发券-头部用户"),
  ORDER_PAGE_PUSH_COUPON_FIRST_LOAN_MID_LOW_WILL("first_loan_product_26h1-loan_not_withdraw-abroad-loan-changeandcoupon_rankCD_low_will", "下单页-首贷修改金额发券-中部低意愿用户"),
  ONE_CLICK_CROWD_LIMIT_V1("reloan_credit_use_26h1-lending-abroad-loan_all-one_click_crowd_limit_v1", "一键借款人群限流实验V1"),
  RELOAN_REPAY_PLAN_GRANT_COUPON_V2("reloan_order_26h1-lending-abroad-renew-repayment_plan_coupon_V2", "复贷下单-还款计划发券V2"),
  SUB_HOME_PAGE_COUPON_PACKAGE_V2("reloan_order_26h1-lending-abroad-renew-RatingA_Regional_Coupon_Bundle_strategy_V3", "下单页区域限定包发券实验"),
  ORDER_PAGE_V5_FOR_RELOAN("reloan_order_26h1-lending-abroad-renew-order_page_v5_new", "复贷下单页5.0实验"),
  ORDER_PAGE_V5_FOR_FIRST_LOAN("first_loan_product_26h1-not_withdraw-abroad-loan-firstloan_order_page_V5_0409", "首贷下单页5.0实验"),
  TENURE_UPGRADE_BANNER("reloan_order_26h1-lending-abroad-renew-extra_loan_more", "下单页升档横条实验"),
  RELOAN_ORDER_CICILAN_DISCOUNT_POP_V1("reloan_order_26h1-lending-abroad-renew-cicilan_discount_pop_v1", "复贷下单页-首期月供折扣弹窗"),
  RETAIN_POPUP_BOOST("reloan_order_26h1-lending-abroad-renew-retain_pop_up_coupon", "复贷头部-挽留弹窗加码"),
  RETAIN_POPUP_BOOST_COUPON_STRATEGY(
      "reloan_order_26h1-lending-abroad-renew-retain_pop_up_coupon_upgrade_strategy",
      "复贷头部挽留弹窗加码-券策略子实验"),
  FIRST_LOAN_ORDER_RETAIN_POPUP_V2(
      "first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_back_popup_0509_v2",
      "首贷下单页-挽留弹窗 2.0_强化月供降低"),
  QUOTA_EXPIRE_RETAIN_POPUP(
      "first_loan_product_26h1-not_withdraw-abroad-loan-loan_back0714",
      "首贷下单页-额度失效预警挽留3.0"),
  RELOAN_ORDER_LIMIT_EXPIRATION_WARNING(
      "reloan_order_26h1-lending-abroad-renew-limit_expiration_warning_pop",
      "复贷下单页-额度失效预警挽留（头中部）"),
  ORDER_PAGE_AWARD_REASON_POPUP(
      "26h2_11_reloan_mkt-lending-abroad-renew-reason_for_award_pop_up_v1",
      "复贷常规-下单页发奖理由承接弹窗"),
  BILL_PAGE_MERGE_REPAYMENT_V1(
      "bill_normal_26h1-repayment-abroad-loan_all-merge_repayment_v1",
      "账单页 V2 合并还款实验"),
  FIRST_LOAN_REPAY_PLAN_POPUP_SUB_EXP(
      "first_loan_product_26h1-not_withdraw-abroad-loan-first_loan_repayment_plan_popup_0526",
      "首贷还款计划弹窗 AB 子实验（父实验 FIRST_LOAN_HEAD_MIDDLE_REPAY_PLAN_COUPON 实验组后再分流）"),
  RELOAN_REPAY_PLAN_GRANT_COUPON_V3(
      "reloan_order_26h1-lending-abroad-renew-repayment_plan_coupon_V3_Top",
      "复贷下单-还款计划发券V3头部"),

  // 鉴权相关实验
  AUTH_INFO_COMPLIANCE_V1(
      "auth_product_26h1-auth-abroad-loan-compliance_optimization",
      "鉴权信息项合规优化（出生地/月收入/公司名服务端硬校验，TAPD-355542）"),
  RELOAN_NORMAL_EXPANSION_NO_OTP(
      "reloan_order_26h1-not_withdraw-abroad-loan_all-No_OTP_test_v2",
      "借贷下单免OTP二期——高质量复贷用户定向豁免"),

  // 被拒承接页（TAPD-370114 二期：首贷/复贷 × 授信通过/未通过 四个实验，每人至多命中其一）
  REJECTED_CREDIT_ACCEPTANCE_FIRST_LOAN_ACCEPTED(
      "first_loan_auth_26h1-lending-abroad-loan-not_reject_forever_accepted",
      "被拒承接页-首贷·授信通过（材料走增信提额）"),
  REJECTED_CREDIT_ACCEPTANCE_RELOAN_ACCEPTED(
      "reloan_order_26h1-not_withdraw-abroad-renew-not_reject_forever_accepted",
      "被拒承接页-复贷·授信通过（材料走增信提额）"),
  REJECTED_CREDIT_ACCEPTANCE_FIRST_LOAN_NOT_ACCEPTED(
      "first_loan_auth_26h1-lending-abroad-loan-not_reject_forever_accepted_temp",
      "被拒承接页-首贷·授信未通过（材料仅留存）"),
  REJECTED_CREDIT_ACCEPTANCE_RELOAN_NOT_ACCEPTED(
      "reloan_order_26h1-not_withdraw-abroad-renew-not_reject_forever_accepted_temp",
      "被拒承接页-复贷·授信未通过（材料仅留存）"),
  @Deprecated
  REJECTED_CREDIT_ACCEPTANCE_FIRST_LOAN(
      "auth_product_26h1-auth-abroad-loan-not_reject_forever",
      "被拒承接页-首贷产品泳道"),
  @Deprecated
  REJECTED_CREDIT_ACCEPTANCE_RELOAN(
      "reloan_order_26h1-not_withdraw-abroad-renew-not_reject_forever",
      "被拒承接页-复贷下单泳道"),

  // 千1（0.1% 日息）端内承接实验：入组只看素材利益点标签，不限投放渠道（TAPD-1370968 放开渠道限制）
  // key 与常量名沿用上一期，改动等于换实验会破坏历史分组，禁止修改
  META_QIAN1_ACQUISITION(
      "technology-register-abroad-loan-meta_0001interest",
      "千1(0.1%日息)端内承接素材实验（TAPD-359348，TAPD-1370968 放开渠道限制）"),

  // 放款账户相关实验
  PAYOUT_CHANNEL_LIMIT_INTERCEPT(
      "technology-lending-abroad-loan_all-Large_Loan_EWallet_Collection_Guide",
      "放款渠道限额提示实验（命中后按渠道限额置灰不可选，TAPD-364191）"),

  // 额度失效用户回端自动戳额（TAPD-1365670，首贷/复贷两个实验按人群分流）
  AUTO_SUBMIT_CREDITS_ON_RETURN_FIRST_LOAN(
      "first_loan_product_26h1-measurement-abroad-loan-get_limit",
      "额度失效回端自动戳额-首贷未放款（实验ID 13868）"),
  AUTO_SUBMIT_CREDITS_ON_RETURN_RELOAN(
      "reloan_order_26h1-measurement-abroad-renew-get_limit11",
      "额度失效回端自动戳额-复贷/结清/M0在贷（实验ID 13869）"),

  // 鉴权全流程 0 息端内承接实验
  AUTH_FLOW_ZERO_INTEREST_ACQUISITION(
      "auth_product_26h1-auth-abroad-loan-0bungaplus",
      "鉴权全流程0息承接（素材标签「利益点→免息」承接，TAPD-367689）"),

  // 万一素材端内全流程承接实验
  WANY_ACQUISITION(
      "auth_product_26h1-auth-abroad-loan-0001bungplus",
      "万一素材端内全流程承接（素材标签「利益点→万一」，TAPD-1369319）"),

  // 万一登录页 banner 设备号实验（TAPD-372482，实验 ID 14027）
  WANY_LOGIN_BANNER(
      "register_26h1-register-abroad-loan-001insterestregister",
      "万一承接注册登录页banner设备号实验（TAPD-372482，实验ID 14027）"),

  /**
   * 防结清 —— 还款计划模块强调首期优惠感 UI 样式实验 key（TAPD-367516）。
   *
   * <p>是唯一实际入组点：在通过版本 / 渠道 / 客群 / 目标人群 / 命中首期抵扣券等前置门槛后,
   * 先对完整目标人群调 {@code abTestByUserId} 做本实验的首次分流入组;仅命中
   * {@code EXPERIMENT_GROUP} 才继续放行到后置 gate ——长期 holdout 走
   * {@code AntiSettlementLongTermExpService#passesHoldout}（后置入组）,
   * 命中对照组则拦截,否则(实验组 / 空白 / 未入组 /
   * 分流失败返回空串)一律放行。
   *
   * <p>版本门槛由服务内部常量 {@code MIN_BUILD}(≥ 37300)直接判断,不登记入
   * {@code ABTestConfig#DEFAULT_EXPERIMENT_BUILD_MAP}。
   */
  ANTI_SETTLEMENT_CICILAN_PROMO_UPGRADE(
      "26h2_11_reloan_mkt-lending-abroad-renew-anti_loss_cicilan_promotion_upgrade",
      "防结清-还款计划模块强调首期优惠感UI样式实验"),

  // 首贷未下单-首页小额借款推荐弹窗（TAPD-367267）
  FIRST_LOAN_HOME_RECOMMEND_POPUP(
      "first_loan_product_26h1-not_withdraw-abroad-loan-Breakthrough_Small_Loan",
      "首贷未下单用户首页小额借款推荐弹窗"),

  /**
   * 复贷非防结清 —— 还款计划模块强调首期优惠感 UI 样式实验一 key（TAPD-368277）。
   *
   * <p>仓库调用实验平台只传 key 字符串，不传数字 ID；实验 ID 仅用于本注释追溯。
   */
  ANTI_SETTLEMENT_CICILAN_PROMO_UPGRADE_NORMAL(
      "26h2_11_reloan_mkt-lending-abroad-renew-anti_loss_cicilan_promotion_upgrade_normal",
      "复贷非防结清-还款计划模块强调首期优惠感UI样式实验（实验一，实验ID 13942）"),

  /**
   * 首贷未放款 —— 还款计划模块强调首期优惠感 UI 样式实验二 key（TAPD-368277）。
   */
  FIRST_LOAN_CICILAN_PROMO_NOT_WITHDRAW(
      "first_loan_product_26h1-not_withdraw-abroad-loan-firstdiscount",
      "首贷未放款-还款计划模块强调首期优惠感UI样式实验（实验二，实验ID 13936）"),



  // 启动自动跳下单页豁免（TAPD-365607）：命中方案二豁免规则的会跳人群，实验组不自动跳下单页
  AUTO_JUMP_ORDER_EXEMPTION(
      "reloan_order_26h1-not_withdraw-abroad-renew-noautojump",
      "启动自动跳下单页豁免逻辑1.0（实验ID 13946）"),

  // 复贷下单页-优惠券底部承接横条（TAPD-369579）
  COUPON_WELCOME_BANNER(
      "26h2_11_reloan_mkt-lending-abroad-renew-normal_coupon_welcome_v1",
      "复贷下单页-优惠券底部承接横条"),

  /**
   * 防结清节日加码灌券实验 key（TAPD-371451）。
   */
  ANTI_SETTLEMENT_FESTIVAL_BOOST(
      "26h2_11_reloan_mkt-lending-abroad-renew-antiloss_National_day_instant_coupon_v1",
      "防结清节日加码灌券"),

  /**
   * 首贷发券6.0-头&中部高意愿子实验 key（TAPD-371176）。
   *
   * <p>父实验为发券3.0高意愿（{@code ExperimentKeyConstants#T0_RISK_ACCEPT_GRANT_CUT_COUPON_HIGH_WILL}，
   * 实验 13764），父实验命中 {@code CONTROL_GROUP}/{@code BLANK_GROUP} 目标分组后再对本 key 分流。
   */
  FIRST_LOAN_COUPON_6_0_HIGH_WILL(
      "first_loan_product_26h1-loan_not_withdraw-abroad-loan-Coupon6_0_high_will",
      "首贷发券6.0-头&中部高意愿子实验"),

  /**
   * 首贷发券6.0-头&中部低意愿子实验 key（TAPD-371176）。
   *
   * <p>父实验为发券5.0低意愿（{@code ExperimentKeyConstants#T0_RISK_ACCEPT_GRANT_CUT_COUPON_LOW_WILL}，
   * 实验 13677），父实验命中 {@code EXPERIMENT_GROUP_2} 目标分组后再对本 key 分流。
   */
  FIRST_LOAN_COUPON_6_0_LOW_WILL(
      "first_loan_product_26h1-loan_not_withdraw-abroad-loan-Coupon6_0_low_will",
      "首贷发券6.0-头&中部低意愿子实验"),

  /**
   * 首贷头中部确认借款后引导多借提额补贴弹窗实验 key（TAPD-371433）。
   *
   * <p>客群：首贷头/中部（af_rank A/B/C/D）、Android build > 36900、持有可用正序降息券。
   * 命中实验组（{@code EXPERIMENT_GROUP}）才展示「提额补贴弹窗」，下发加借推荐金额 D 及首期还款对比。
   */
  FIRST_LOAN_BORROW_MORE_POPUP(
      "first_loan_product_26h1-loan_not_withdraw-abroad-loan-usemore_1",
      "首贷加借推荐弹窗实验"),
  // 增信提额 v1.0 链路优化（TAPD-369783）：增信提额首页页面级 A/B 分流，首贷/复贷互斥两个 key，
  // 实验组下发新版展示口径（styleVersion=V1）。版本门槛 build>=37300 由业务代码判断，不登记 ABTestConfig。
  INCREASE_CREDIT_OPTIMIZE_FIRST_LOAN(
      "first_loan_product_26h1-not_withdraw-abroad-loan-Raise_Limit",
      "增信提额v1.0链路优化-首贷"),
  INCREASE_CREDIT_OPTIMIZE_RELOAN(
      "reloan_order_26h1-lending-abroad-renew-credit_enhancement_optimization_v1",
      "增信提额v1.0链路优化-复贷"),

  /**
   * 首贷 A/B 低意愿回端发 50% 降息券实验 key（14004，TAPD-372051/TAPD-374383，父实验 13823 子实验）。
   * 本实验没有独立发券能力，只在父实验 13823 已判定要发放某档加码券的基础上，
   * 满足更严格条件时把该档券替换为本实验的 50% 券（工具ID=2101）；未命中时维持13823原决策不变。
   */
  FIRST_LOAN_AB_LOWWILL_HALFOFF_COUPON(
      "first_loan_product_26h1-loan_not_withdraw-abroad-loan-coupon0_1interest",
      "首贷A/B低意愿D+1回端50%降息券（14004，父实验13823子实验）"),

  // 额度专项2.0 · 增强额度获得感（TAPD-369589，3 实验按「人群 + 场景」互斥短路路由，
  // 见 CreditGainPerceptionExpService）
  CREDIT_GAIN_FIRST_LOAN_FIRST_CREDIT(
      "auth_product_26h1-measurement-abroad-loan-getcreditplus",
      "额度专项2.0-首次获额强化（实验一）"),
  CREDIT_GAIN_RELOAN_RECREDIT(
      "reloan_order_26h1-not_withdraw-abroad-renew-repayandother",
      "额度专项2.0-还款/额度失效获额强化（实验二）"),
  CREDIT_GAIN_FIRST_LOAN_EXPIRE_RECREDIT(
      "first_loan_auth_26h1-not_withdraw-abroad-loan-recovercredit",
      "额度专项2.0-额度失效获额强化_首贷（实验三）"),
  ;

  /**
   * 实验key
   */
  private final String key;
  /**
   * 描述
   */
  private final String desc;
}
