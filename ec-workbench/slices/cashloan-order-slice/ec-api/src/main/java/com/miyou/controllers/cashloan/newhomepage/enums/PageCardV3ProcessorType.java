package com.miyou.controllers.cashloan.newhomepage.enums;

import lombok.AllArgsConstructor;

/**
 * 被拒状态
 */
@AllArgsConstructor
public enum PageCardV3ProcessorType implements HomepageBaseInfoProcessorType {
  COMMON_TYPE("通用的组件"),
  REJECT_STATUS("被拒状态统一处理"),
  RELOAD_KTP("重传ktp"),
  ORDER_PROGRESS_STEP("下单后状态流转步骤"),
  ORDER_NEED_SUPPLEMENT_LIVING_INFO("下单待补件"),
  ORDER_SUPPLEMENT_REVIEW("补件审核"),
  REVIEW_FOR_RISK("风控审核"),
  NOT_LOGIN("未登录"),
  NEVER_APPLIED("未鉴权"),
  CREDITS_EXPIRED("额度失效"),
  LOAN_PRODUCT_INFO("借款产品页面"),
  CAN_REAPPLY("可重新提交授信"),
  REPAYMENT_CARD("还款卡片统一处理"),
  MAIN_CARD_REPAYMENT_INFO("主卡片还款信息"),
  CAN_NOT_LOAN_CREDIT("不可借额度卡片"),
  MANUAL_REVIEW("人工审核卡片"),
  LOAN_MARKET("贷超卡片"),
  MULTI_LOAN_PRODUCT_INFO("续借状态借款产品页面"),
  OVERDUE_REPAYMENT_CARD("逾期场景还款卡片"),
  CREDIT_GAIN_UPGRADE_POPUP("还款升额首页弹层（增强额度获得感 US4，仅结果态命中实验二升额时下发 PopUpAction）"),
  LAST_COMMON_CARD("末位通用组件（安全模块/TKB 等所有状态统一后置内容）"),
  ;

  public final String desc;

  @Override
  public HomepageProcessorType getProcessorType() {
    return HomepageProcessorType.PAGE_CARD_INFO_V3;
  }

  @Override
  public String getName() {
    return this.name();
  }

  @Override
  public Class<? extends HomepageBaseInfoProcessorType> getTypeClazz() {
    return PageCardV3ProcessorType.class;
  }
}
