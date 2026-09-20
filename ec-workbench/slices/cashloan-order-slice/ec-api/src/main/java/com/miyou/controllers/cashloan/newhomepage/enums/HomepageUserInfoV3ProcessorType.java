package com.miyou.controllers.cashloan.newhomepage.enums;

public enum HomepageUserInfoV3ProcessorType implements HomepageBaseInfoProcessorType {
  DEBT_BIZ_CHECK_V3,
  FUND_ORDER_BIZ_CHECK_V3,
  FUND_PAYOUT_BIZ_CHECK_V3,
  GRAB_ORDER_BIZ_CHECK_V3,
  SIGNATURE_BIZ_CHECK_V3,
  DIRECT_DEBIT_BANK_V3,
  HOME_PAGE_GUIDE_BUBBLE_V3,
  REPAYMENT_INFO_V3,
  /**
   * 页面跳转信息
   */
  PAGE_JUMP_V3,
  /**
   * 贷超分流信息
   */
  LOAN_MARKET_INFO_V3,
  /**
   * 自动跳转账单页--未逾期
   */
  AUTO_JUMP_BILL_PAGE_FOR_NOT_OVERDUE,
  /**
   * 自动跳转账单页--逾期
   */
  AUTO_JUMP_BILL_PAGE_FOR_OVERDUE,
  ;

    @Override
    public HomepageProcessorType getProcessorType() {
        return HomepageProcessorType.PAGE_USER_INFO_V3;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public Class<? extends HomepageBaseInfoProcessorType> getTypeClazz() {
        return HomepageBaseInfoProcessorType.class;
    }
}
