package com.yqg.core.service.kafka.consumer;

import com.yqg.core.service.kafka.consumer.context.*;


/**
 * Created by xiahonggao on 2017/3/7.
 */
public enum ConsumerGroup {

  TEST(TestConsumerGroupContext.class),
  CASH_LOAN_EVENT_LOAN_ACCOUNT(LoanAccountConsumerGroupContext.class),
  CASH_LOAN_EVENT_LOAN_ACCOUNT_RISK(LoanAccountRiskConsumerGroupContext.class),
  CASH_LOAN_EVENT_COL(ColConsumerGroupContext.class),
  CASH_LOAN_EVENT_TELESALES(TelesalesCashLoanEventConsumerGroupContext.class),
  CASH_LOAN_ACTIVITY(LoanActivityConsumerGroupContext.class),
  CASH_LOAN_EVENT_PAYMENT(PaymentCashLoanEventConsumerGroupContext.class),
  CASH_LOAN_EVENT_CAPITAL(CapitalLoanOrderConsumerGroupContext.class),
  CASH_LOAN_EVENT_DOWNSTREAM_SYNC(DownstreamOrderRepaySyncConsumerGroupContext.class),
  CASH_LOAN_EVENT_OUTDOOR_TELESALES(OutdoorTelesalesCashLoanEventConsumerGroupContext.class),
  USER_TELESALES(TelesalesUserEventConsumerGroupContext.class),
  USER_OUTDOOR_TELESALES(OutdoorTelesalesUserEventConsumerGroupContext.class),
  USER_EASYCASH(EasycashUserEventConsumerGroupContext.class),
  USER_ACTIVITY(LoanActivityUserEventConsumerGroupContext.class),
  USER_EASYCASH_RISK_FOR_INVITATION_REWARD(ActivityRiskConsumerGroupContext.class),
  USER_COLLECTION(ColConsumerUserEventGroupContext.class),
  USER_NOTIF_MC(NotifUserMCEventConsumerGroupContext.class),
  @Deprecated
  INDIA_DATA_UPLOAD(null),
  @Deprecated
  SERASA_BLACKLIST(null),
  FINANCING_ASSET_EVENT_TELESALES(TelesalesFinancingAssetEventConsumerGroupContext.class),
  FINANCING_ASSET_EVENT_FINANCING_INFO(FinancingInfoConsumerGroupContext.class),
  RISK_EVENT_TELESALES(TelesalesRiskEventConsumerGroupContext.class),
  RISK_EVENT_OUTDOOR_TELESALES(OutdoorTelesalesRiskEventConsumerGroupContext.class),
  RISK_EASYCASH(EasycashRiskConsumerGroupContext.class),
  LOAN_MARKET_CASH_LOAN(LoanMarketCashLoanConsumerGroupContext.class),
  LOAN_MARKET_RISK(LoanMarketRiskConsumerGroupContext.class),
  NOTIF_TELESALES(TelesalesNotifEventConsumerGroupContext.class),
  RISK_USER_LEVEL_CASH_LOAN(RiskUserLevelCashLoanConsumerGroupContext.class),
  RISK_USER_LEVEL_RISK(RiskUserLevelRiskConsumerGroupContext.class),
  NOTIF_DATA_WAREHOUSE(NotifDataWarehouseConsumerGroupContext.class),
  FINANCING_REDEEM_WITH_REPAYMENT(FinancingRedeemWithRepaymentConsumerGroupContext.class),
  FINANCING_AUTO_DEBT_MATCH(FinancingAutoDebtMatchConsumerGroupContext.class),
  BIZ_CHECK(BizCheckEventConsumerGrouoContext.class),
  RISK_USER_AB_TEST(RiskUserAbTestConsumerGroupContext.class),
  @Deprecated
  CREDITS_DECREASE_QUICK_ORDER(null),
  BIZ_CHECK_TELESALES(TelesalesBizCheckEventConsumerGroupContext.class),
  LOAN_ACTIVITY_ORDER(LoanActivityOrderConsumerGroupContext.class),
  LOAN_MARKET_USER_EVENT(LoanMarketUserEventConsumerGroupContext.class),
  INVESTOR_FEE_SETTLE(InvestorFeeSettleConsumerGroupContext.class),
  INVESTOR_CALC_EARNINGS(InvestorCalcEarningsConsumerGroupContext.class),
  CAPITAL_FEE_DEDUCT(CapitalFeeDeductConsumerGroupContext.class),
  USER_TRIGGER_REPAY_EVENT(FundRepayEventConsumerGroupContext.class),
  MARKETING_CENTER_INCREASE_CREDIT_EVENT_GROUP(IncreaseCreditsEventGroupContext.class),
  UPLOAD_RISK_EVENT(UploadRiskEventConsumerGroupContext.class),
  EC_JBP_ORDER_STATUS_LOG_GROUP(JbpOrderStatusLogConsumerGroupContext.class),
  APP_STARTUP_AT_LEAST_ONCE_EVENT_GROUP(AppStartupAtLeastOnceEventConsumerGroupContext.class),
  EC_COMBINED_REPAY_CALL_BACK_EVENT_GROUP(EcCombinedRepayEventGroupContext.class),
  EC_RISK_CASH_LOAN_EVENT(RiskCashLoanEventConsumerGroupContext.class),
  GRANT_COUPON_RISK_GROUP(GrantCouponRiskConsumerGroupContext.class),
  APP_STARTUP_GRANT_COUPON_GROUP(AppStartUpGrantCouponConsumerGroupContext.class),
  OPS_RANDOM_GRANT_COUPON_GROUP(OpsRandomGrantCouponConsumerGroupContext.class),
  NOTIF_BLACKLIST_TO_EC(NotifBlacklistToEcConsumerGroupContext.class),
  LOAN_MARKET_PRODUCT_UV_VALUE(LoanMarketProductUvValueDailyConsumerGroupContext.class),
  LOAN_MARKET_PARTNER_EVENT(LoanMarketPartnerEventConsumerGroupContext.class),
  CASH_LOAN_EVENT_NOTIF_BLACK_LIST(NotifBlacklistConsumerGroupContext.class),
  ;

  private Class<? extends IConsumerGroupContext> contextClass;

  ConsumerGroup(Class<? extends IConsumerGroupContext> contextClass) {
    this.contextClass = contextClass;
  }

  public Class<? extends IConsumerGroupContext> getContextClass() {
    return this.contextClass;
  }
}
