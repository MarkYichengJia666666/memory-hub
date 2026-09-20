package com.yqg.core.service.general.pageconfig.filterstrategy.enums;

import com.google.common.collect.Lists;
import com.yqg.ec.common.exception.EcException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public enum GeneralPageConfigFilterRuleType {
  USER_ID_END_NUMBER("U", "userId尾号"),
  IDN_HOMEPAGE_LOAN_STATUS("I", "印尼首页用户状态"),
  HOMEPAGE_SCENE("HS", "首页场景"),
  GENERAL_ACTIVITY("A", "通用活动参与状态"),

  GENERAL_ACTIVITY_TOKEN("AT", "活动有未使用的抽奖次数"),
  ACCEPTED_NOT_ORDER("O", "可提现n天后未下单"),
  ACCEPTED_NOT_ORDER_V2("H", "可提现n天后未下单(上次成功打款后最早通过的提现风控)"),
  USER_RDL_STATUS("R", "RDL用户状态"),
  FINANCING_USER_AUTH_STATUS("F", "理财用户鉴权状态"),
  FINANCING_USER_RDL_BANK_STATUS("B", "理财用户rdl相关绑卡状态"),
  FINANCING_USER_BANK_TYPE("FUBT", "理财用户绑卡银行"),
  FINANCING_USER_CREATE_ORDER_FOR_PRODUCT_TYPE("D", "理财用户下单的产品类型"),
  LOAN_USER_FILL_MOTHER_NAME("C", "借贷用户填写母亲姓氏"),
  LOAN_USER_FILL_EMAIL("E", "借贷用户填写电子邮箱"),
  SHOW_VIRTUAL_INCREASE_CREDITS("G", "展示虚拟提额的banner"),
  LOAN_TIMES("LT", "借款次数"),
  LOAN_MAX_OVERDUE_DAYS("LMOD", "借贷用户历史最大逾期天数"),
  BILL_JBP_UNPAID_GUIDE("BJUG", "账单页中收未支付引导弹窗"),
  JOIN_ACTIVITY_CENTER_ACTIVITY_NUM("JAC", "能够参加活动数量"),
  USER_REGISTER_TIME("URT", "用户注册时间"),
  USER_REGISTER_CHANNEL("URC", "用户注册渠道"),
  USER_ACTIVATION_CHANNEL("UAC", "用户激活渠道"),
  USER_HAS_RDL_ACCOUNT("UHRA", "用户是否存在 RDL 账户开户记录"),
  MGM_AREA_LIMIT("MAL", "MGM活动区域"),
  RISK_USER_LEVEL("LEV", "用户风险等级"),
  SHOW_INCREASE_CREDITS_ENTRANCE("SICE", "展示增信提额入口"),
  FIN_NOT_FILL_INDUSTRY_OR_PROCESSION("FNFIOP", "理财未填行业或职业信息"),
  USER_CURRENT_USER_TYPE("UT", "用户类型"),
  USER_CURRENT_REJECT_STATUS("UCRS", "用户当前被拒状态"),
  USER_CURRENT_LOAN_STATUS("UCLS", "用户当前借贷状态"),
  USER_ID_AB_TEST_RESULT("ATR", "userId Ab test结果"),
  USER_ID_AB_TEST_GENERATE_RESULT("UIATGR", "根据userId实时触发AB实验分流并获取分流结果"),
  @Deprecated
  AB_TEST_BY_EXPERIMENT_PLATFORM_GENERATE_RESULT("ATBEPGR", "根据实验中台触发AB实验分流并获取分流结果"),
  AB_TEST_BY_EXPERIMENT_PLATFORM_GENERATE_RESULT_V2("ATBEPGR2","根据实验中台触发AB实验分流并获取分流结果V2"),
  EXTRA_INFO_NOT_SUBMIT("EINS", "用户额外信息未提交"),
  USER_DIFFERENTIAL_PRICING_RATE_DISCOUNT("UDPRD", "用户差异定价利率优惠值---低息用户"),
  LOAN_USER_VALID_COUPON_TYPE("LUVCT", "借贷用户是否有有效优惠券类型"),
  LOAN_USER_VALID_COUPON_RULE_ID("LUVCRI", "借贷用户有效优惠券对应的发券工具id"),
  DEEPLINK_CHANNEL("DC", "deeplink渠道"),
  USER_ID_POINT_AB_TEST("UPOINT", "积分活动分流结果"),
  USER_ID_CHOICE_ONE_AB_TEST("UCHOICE", "N选1活动用户是否选奖"),
  USER_ID_CHOICE_ONE_TASK_AB_TEST("UCOTASK", "N选1活动是否完成任务"),
  USER_ID_CHOICE_ONE_COMPLETE_AB_TEST("UCOMPLETE", "N选1活动完成任务在x天内"),
  @Deprecated
  USER_ID_CHOICE_ACTIVITY_AB_TEST("UCPART", "N选1活动用户有参加资格"),
  USER_ID_ORDER_ACTIVITY_TASK("UOAT", "下单页活动用户是否有未完成的任务"),
  USER_ID_ORDER_ACTIVITY_REWARD("UOAR", "下单页活动用户是否有奖励"),
  USER_ID_MULTI_ACTIVITY_REWARD("UMAR", "种树活动用户有未领取三方奖励"),
  USER_ELIGIBLE_AND_NOT_COMPLETE_TASK_FOR_ACTIVITY("UEANCTFA", "用户当前有活动资格并且未完成任务"),
  USER_ID_ALL_ACTIVITY_REWARD("UAAR", "用户活动是否有奖励"),
  @Deprecated
  USER_HAS_MULITI_LOAN_ORDER("UHML", "用户最小续借单数"),
  ACTIVITY_USER_GROUP_RELATION("AUGR", "活动人群命中规则"),

  USER_MULTI_LOAN_ORDER_NUMBER("UMLON", "用户续借订单数规则"),
  MINIMALIST_USER_INCREASE_CREDIT("MUIC", "极简用户提额"),
  SOURCE_TYPE_RULE("STR", "sourceType类型规则"),
  USER_CREDIT_QUOTA_RANGE("UCQR", "用户额度范围"),
  DEVICE_TOKEN_AB_TEST_GENERATE_RESULT("DTABGR", "根据deviceToken实时触发AB实验分流并获取分流结果"),
  CUSTOM_SINGLE_VALUE_RULE("CSV", "自定义单值规则"),
  INCREASE_CREDIT_CHECK("ICC", "满额提现提额试算"),
  FOREIGNER_FIN_USER("FFU", "外籍理财用户判断"),
  FOREIGNER_AUTH_REJECT("FAR", "外籍用户最新鉴权工单被拒绝"),
  RECENT_N_DAYS_SUPPLEMENT_TELESALES_COMPETITIVE_MATERIALS_ACTION("RNDSTCMA", "最近N天电销下发竞品资料补件"),
  QUOTA_UTILIZATION_RATE_RULE("QURR", "额度使用率规则"),
  CAN_JOIN_ACTIVITY("CJA", "是否能参加活动"),
  LOAN_DISPLAY_PRODUCT_BECOME_LONGER("LDPBL", "借贷用户最近一次首页展示产品周期变长"),
  MOTHER_LAST_NAME_CHECK("MLNC", "母亲姓氏校验"),
  USER_JOIN_ACTIVITY_AND_HAS_UNFINISHED_TASK("UJAUFT", "用户参加活动并有未完成任务"),
  USER_CAN_DRIVER_TASK("UCDT", "用户是否能够创建任务"),
  USER_HAS_TASK_AND_NOT_COMPLETE_AND_EXPIRE_IN_N_DAYS("UHTANCAND", "用户有任务且未完成且N天内到期"),
  @Deprecated
  GOLDEN_CARD_DISPLAY("GC", "金卡展示规则"),
  GOLDEN_CARD_UNAVAILABLE("GCA", "金卡不可用判断"),
  SHOW_COLLECT_INFORMATION("GCD", "是否展示对应的收集信息"),
  @Deprecated
  GOLDEN_CARD_V2_DISPLAY("GCV2", "金卡V2版本展示规则"),
  LOAN_ORDER_PAGE_QUICK_ORDER("LOPQO", "下单页半蒙层快速下单规则"),
  QUICK_ORDER_FOR_ORDER_PAGE_RETURN_BUTTON("QOFOB", "挽留弹窗快速下单"),
  TODAY_REPAY_AND_CAN_LOAN("TRCL", "今日还款且可以借款"),
  USER_AUTHORIZATION_STATUS_RULE("UASR", "用户授权状态"),
  USER_AUTHORIZATION_POPUP_WINDOW_EXPERIMENT_RULE("UAPWER", "用户授权弹窗实验"),
  USER_AUTHORIZATION_REVOKED_MORE_THAN_N_DAYS("UARMTND", "用户取消授权超过 N 天"),
  USER_INTEREST_AND_TEMP_CREDITS_PRODUCTS_RULE("UIATCPR", "用户降息+提额产品规则"),
  LATEST_JBP_ORDER_STATUS("LJOS", "存在指定状态最近中收会员订单"),
  EFFECTIVE_LATEST_JBP_ORDER("ELJO", "存在生效中的会员订单(包含INIT部分支付)"),
  AUTH_FINISHED("AF", "用户已完件"),
  LOAN_IN_PROGRESS("LIP", "用户在贷"),
  FIRST_LOAN_PAYOUT("FLP", "用户首次放款"),
  PERMANENTLY_REJECTED("PR", "用户永久被拒"),
  USER_DELETED("UD", "账号已注销"),
  LOAN_SUPERMARKET_DIVERSION("LSD", "满足贷超分流"),
  LANE_AB_TEST("LAT", "泳道AB测试"),
  ACTIVITY_USER_TASK_STATUS("AUTS", "用户存在指定状态活动任务"),
  IMMEDIATE_CONTACT("IC", "紧急联系人"),
  WHATSAPP("WA", "whatsapp"),
  USER_ELIGIBLE_FOR_ACTIVITY("UEFA", "用户有资格参加活动"),
  AB_ACTIVITY_RULE("ATA", "ab&入组活动规则"),
  ACTIVITY_CREATE_USER("ACCU", "活动创建人群"),
  POSTPOSE_REWARD_TIER("PRT", "一键借款后置-按diff分层入组"),
  T0_LOAN_LIMIT_PERCEPTION("T0LP", "T0首贷额度感知"),
  ORDER_PAGE_RETAIN_POP_UP_HB("OPRPHB","下单页挽留-红兵"),
  CREDITS_DETAILS_AWARE_POPUP("CDAP", "额度感知弹窗类型"),
  JBP_CREDIT_REPORT_POPUP("JCRP","中收信用报告子实验弹窗"),
  JBP_CARD_V2("JBP_CARD_V2", "jbp v2 版本"),
  JBP_ORDER_VALID_CHECK("JOVC","jbporder生效后策略"),
  HAS_AVAILABLE_BANK_ACCOUNT("HABA", "用户是否有可用银行卡"),
  @Deprecated
  // 废弃但不删除 后续可能有规则需要嵌合这个规则
  COUPON_TEMP_CREDIT_REACH_REQUIRE_PERCENTAGE_OR_GRANT_COUPON_SUCCESS("CTCRRPOGCS", "券临时额度达到阈值比例或发放临时额度券成功"),
  LOAN_ORDER_COUNT_IN_SPECIFIED_NATURAL_DAY_RANGE("LOCISNDR", "指定自然天区间内的借款单数目"),
  BILL_PAGE_DISPLAY_STRATEGY("BPDS", "账单页展示策略"),
  ORDER_PAGE_INC_CREDIT_DECR_INTEREST_POPUP("OPICDIP","下单页提额降息扑脸弹窗规则"),
//  USER_IN_DIVERSION_GROUP("UIDG","用户是否在实验域"),
  COMPARE_WITH_WORKED_COUPON_TEMP_CREDITS("CWWCTC", "与已生效券临额对比"),
  COMPARE_WITH_WORKED_COUPON_DECREASE_INTEREST_RATIO_WITH_VIRTUAL("CWWCDIRWV", "与已生效券降息比例对比（含虚拟额度）"),
  RETAIN_WINDOW_V4_RIGHT_EXPAND("RWV4RE", "挽留弹窗V4-权益膨胀相关规则"),
  USER_AUTH_AC_RULE("UAACR", "用户完件活动V4"),
  CAN_ORDER_HOMEPAGE_STATUS("COHS", "可下单首页状态"),
  @Deprecated
  LONG_TERM_EXPERIMENT("LTE", "长期实验"),
  USER_IN_DIVERSION_GROUP("UIDG","用户是否在实验域"),
  EXPERIMENT_RULE("EXPR", "用户实验规则"),
  JBP_PAY_STATUS("JPS","jbp最近一笔订单支付状态"),
  REPAY_PLAN_REDUCE_POPUP("RPRP","还款计划强化月还款金额降低弹窗规则"),
  @Deprecated
  REPAY_PLAN_REDUCE_POPUP_V2("RPRPT","还款计划强化月还款金额降低弹窗规则2.0"),
  JBP_SYS_POPUP("JSP","中收系统风弹窗"),
  LOAN_OVERDUE("LO","有在贷的用户是否逾期"),
  USER_HAS_LOAN("UHL", "用户是否有在贷"),
  @Deprecated
  PRODUCT_API_TRIGGER_POPUP("PTRP","产品详情金额修改弹窗策略"),
  PRODUCT_DETAIL_MODIFY_AMOUNT_POPUP_V4("PTRP_V4","下单页-修改金额发券4.0弹窗规则"),

  ORDER_PAGE_INC_CREDIT_DECR_INTEREST_POPUP_STYLE("OPICDIPS","下单页提额降息扑脸弹窗样式"),
  OVERDUE_AC_RULE("OAR", "逾期活动规则"),
  OVERDUE_AC_CLAIM_RULE("OACR", "逾期活动奖励领取规则"),
  COUPON_GUIDE_TO_REDUCE_PAYMENT("CGTRP","引导用券降月供规则"),
  //*****2026 H1 泳道统一判断规则
  H1_2026_AB_RULE_FOR_SWIM_LANE("2026H1RSW", "2026H1泳道规则"),
  HOME_INTEREST_FREE_CARD_POPUP("HIFCP", "首页免息卡-下单页弹窗规则"),
  HOME_INTEREST_FREE_CARD_HOME_POPUP("HIFCHP", "首页免息卡-首页弹窗规则"),
  HOME_INTEREST_FREE_CARD_FLOATING_ICON("HIFCFI", "首页免息卡-挂件展示规则"),
  NO_INTEREST_POPUP_V1("NIPV1","免息天数包装1.0"),
  @Deprecated
  NO_INTEREST_POPUP_V2("NIPV2","免息天数包装2.0规则"),
  HOMEPAGE_LEVEL_ONE_ONLY("FILOO", "仅一级首页展示规则"),
  @Deprecated
  TIERED_COUPON("TIERED_COUPON", "阶梯券包规则"),
  @Deprecated
  ORDER_PAGE_REGION_LIMITED_COUPON_PACKAGE("OPRLCP", "下单页-区域限定券包规则"),
  REPAY_TASK("RTP", "按时还款教育进度资源位规则"),
  REPAY_TASK_REWARD_POPUP("RTRP", "按时还款教育奖励弹窗规则"),
  DIRECT_REWARD_FOR_HOME_MESSAGE("DRFHM", "代扣授权首页弹窗规则"),
  DIRECT_REWARD_FOR_HOME_BANNER("DRFHB", "代扣授权首页banner规则"),
  INTEREST_SPLIT_RULE("ISR","息费结构包装1.0规则"),
  ORDER_PAGE_CUSTOMER_BROADCAST("OPCB", "下单页-客服权益播报"),
  ORDER_RETAIN_POPUP("ORP", "复贷下单页-挽留弹窗"),
  RETAIN_POPUP_BOOST("RPB", "复贷头部-挽留弹窗加码"),
  PRODUCT_DETAIL_MODIFY_AMOUNT_POPUP_FIRST_LOAN("PTRP_FL", "下单页-首贷修改金额发券弹窗规则"),
  FIRST_LOAN_HOME_REWARD("FLHR", "首贷首页奖励前置规则"),
  FIRST_LOAN_RECOMMENDATION_POPUP("FLRP", "首贷下单页推荐借款方案弹窗规则"),
  DATA_WAREHOUSE_USER_DEFINE_RULE("DWUDR", "数仓用户自定义规则(标签)"),
  DATA_WAREHOUSE_METRICS("DWM", "数仓指标规则"),
  CHANNEL_MERGE_CHECK("CMC", "渠道合并账号检测"),
  RELOAN_RETAIN_FREQUENCY_GROUP("RRLFG", "复贷挽留限频组规则"),
  FIRST_LOAN_NORMAL_HEAD_MIDDLE_CROWD("FLNHMC", "首贷常规头中部客群"),
  FIRST_LOAN_ORDER_RETAIN_POPUP_V2("FLORPV2", "首贷下单页-挽留弹窗 2.0_强化月供降低"),
  @Deprecated
  ORDER_PAGE_BILL_DISCOUNT_POPUP("OBDP", "下单页-首期月供折扣弹窗规则"),
  REPAY_PLAN_REDUCE_POPUP_V3("RPRPTV3", "还款计划发券弹窗V3规则"),
  QIAN1_ACQUISITION("Q1ACQ", "千1(0.1%)承接通用规则"),
  QIAN1_ACQUISITION_NOT("Q1ACQN", "非千1(0.1%)承接通用规则"),
  USER_ID_EXPERIMENT_HIT_GROUP("UEHG", "根据userId进入并查询实验命中组"),
  USER_ID_PARENT_CHILD_EXPERIMENT_HIT_GROUP("UPCEHG", "根据userId进入并查询实验命中组(父子实验)"),
  QUOTA_EXPIRE_RETAIN_POPUP("QERP", "首贷下单页-额度失效预警挽留3.0"),
  RELOAN_QUOTA_EXPIRE_RETAIN_POPUP("RQERP", "复贷下单页-额度失效预警挽留（头中部）"),
  RELOAN_RETAIN_POP_V2("RRPV2", "复贷防结清-关怀对话式挽留弹窗2.0"),
  ZERO_INTEREST_ACQUISITION("ZIACQ", "鉴权全流程0息承接规则"),
  FIRST_LOAN_HOME_RECOMMEND_POPUP("FLHRP", "首贷未下单用户首页小额借款推荐弹窗规则"),
  REJECTED_CREDIT_ACCEPTANCE_HOME("RCAH", "被拒信用分承接页-首页入口"),
  PRODUCT_DETAIL_COUPON_X2_POPUP("PTC_X2", "下单页-券翻倍扑脸弹窗规则"),
  ORDER_PAGE_AWARD_REASON_POPUP("OARP", "下单页-发奖理由承接弹窗规则"),
  BIOMETRIC_GUIDE_ENABLE("BGE", "引导开启生物识别弹窗规则"),
  WANY_ACQUISITION("WYACQ", "万一承接通用规则"),
  NATIONAL_DAY_817_HOME_BANNER("ND817HB", "国庆氛围展示规则"),
  ORDER_PAGE_FIRST_CREDIT_GAIN_POPUP("OPFCG", "下单页-首次获额扑脸（首贷首次获额人群+实验一判组）"),
  ;

  public final String code;
  public final String desc;

  /**
   * 不给实验中台使用的类型
   */
  public static final List<GeneralPageConfigFilterRuleType> NO_USED_BY_EXPERIMENT_PLATFORM = Lists.newArrayList(
      AB_TEST_BY_EXPERIMENT_PLATFORM_GENERATE_RESULT
  );

  private static final Map<String, GeneralPageConfigFilterRuleType> CODE_TO_ENUM_MAP = new HashMap<>();

  static {
    for (GeneralPageConfigFilterRuleType type : GeneralPageConfigFilterRuleType.values()) {
      CODE_TO_ENUM_MAP.put(type.code, type);
    }
  }

  GeneralPageConfigFilterRuleType(String code, String desc) {
    this.code = code;
    this.desc = desc;
  }

  public static GeneralPageConfigFilterRuleType fromCode(String code) {
    for (GeneralPageConfigFilterRuleType type : values()) {
      if (type.code.equals(code)) {
        return type;
      }
    }
    throw EcException.error("GeneralPageConfigFilterRuleType code could not found. code = " + code);
  }

  public static GeneralPageConfigFilterRuleType getByCodeOrNull(String code) {
    GeneralPageConfigFilterRuleType type = CODE_TO_ENUM_MAP.get(code);

    if (type == null) {
      log.info("GeneralPageConfigFilterRuleType getByCodeOrNull fail, not find type by code, code={}", code);
    }

    return type;
  }

}
