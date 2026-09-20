package com.yqg.core.model.sql.loanusertrace;

public enum LoanUserRiskTriggerSource {
  // 用户端
  AUTH_COMPLETE("完件"),
  CREATE_ORDER("用户手动创建订单", "订单ID"),
  CREATE_ACTIVITY_ORDER("用户手动创建活动订单", "活动订单ID"),
  INCREASE_CREDITS_MATERIAL_DONE("用户增信提额资料填写完成", "增信资料表主键ID"),
  MANUAL("用户手动触发"),

  // 内部系统
  PAYOUT_SUCCESS("打款成功", "订单ID"),
  REPAY_PARTIAL_INSTALMENTS("还款分期成功（至少还完一期）", "还款记录ID"),
  ORDER_COMPLETED("用户结清订单", "订单ID"),
  ORDER_COMPLETED_COMPENSATE_JOB("结清补偿测额定时任务", "订单ID"),
  EXIT_REVOLVING("流程退出循环"),
  FORCE_EXIT_REVOLVING("强制退出循环"),
  REVOLVING_AUTO_JOB("循环贷定时任务"),
  BATCH_RISK("跑批", "跑批任务log表ID"),
  PRE_RISK_REJECT("前置风控被拒", "前置风控trace的traceID"),
  PRE_RISK_CONTROL("前置风控管制", "前置风控trace的traceID"),
  NORMAL_REJECT_RETRIEVAL_ACCEPT_AUTO_ORDER("主营二次风控被拒，回捞一次风控通过后自动创建订单", "订单ID"),
  /** 端外 API 白名单用户回端补件后触发续借测额（TAPD-1364251）。 */
  API_RETURN_SUPPLEMENT("端外API渠道用户回端补件后触发续借测额风控"),
  APP_RETURN_AUTO("用户回端自动戳额"),

  // 其它
  @Deprecated
  OTHER_SYSTEM_AUTO("其余情况的系统自动触发"),
  @Deprecated
  DEGRADE_BY_PRE_RISK("前置风控导致自动降级"),
  ;

  public String desc;
  /** 触发来源外部 id 所指向的对象类型描述；无外部对象的来源为 null。 */
  public String externalIdDesc;

  LoanUserRiskTriggerSource(String desc) {
    this(desc, null);
  }

  LoanUserRiskTriggerSource(String desc, String externalIdDesc) {
    this.desc = desc;
    this.externalIdDesc = externalIdDesc;
  }
}
