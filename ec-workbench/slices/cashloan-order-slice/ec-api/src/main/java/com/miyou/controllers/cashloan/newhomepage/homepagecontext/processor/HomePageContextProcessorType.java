package com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HomePageContextProcessorType {

  ACCEPT_BUT_CAN_NOT_LOAN("授信通过但不可解"),
  SECOND_RISK_REJECT_DISPLAY_STRATEGY("二次风控拒绝展示分流"),
  ORDER_PAGE_AMOUNT_INPUT_SIMPLIFY("下单页额度输入区简化"),
  CHANNEL_MERGE_DETECTION("渠道合并前置检测"),
  ;

  public final String desc;

}
