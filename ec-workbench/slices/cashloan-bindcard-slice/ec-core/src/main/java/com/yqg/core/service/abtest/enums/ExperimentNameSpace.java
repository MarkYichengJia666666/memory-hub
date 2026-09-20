package com.yqg.core.service.abtest.enums;

import com.google.common.collect.ImmutableSet;
import com.yqg.common.spring.util.enums.DescriptionBaseEnum;
import com.yqg.common.util.type.BooleanType;
import com.yqg.core.common.enums.BooleanEnum;
import com.yqg.core.model.sql.abtest.enums.DiversionKeyType;
import com.yqg.core.service.cashloan.auth.step.enums.BindCardDelayGroup;
import com.yqg.core.service.cashloan.enums.RepayPlanReduceGroup;
import com.yqg.core.service.cashloan.repay.enums.GoPayDirectDebitStrategy;
import com.yqg.core.service.cashloan.repay.enums.RepaymentChannelPageDisplayStrategy;
import com.yqg.core.service.cashloan.repay.enums.RepaymentReminderStrategy;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentDisplayStrategy;
import com.yqg.core.service.cashloan.vo.enums.DiscountDetailTopAreaVipUIDisplayStrategy;
import com.yqg.core.service.directdebit.enums.DirectDebitGuidePageStrategy;
import com.yqg.core.service.directdebit.enums.DirectDebtDisplayStrategy;
import com.yqg.core.service.jbp.creditreport.enums.JbpCreditReportPopupResultGroup;
import com.yqg.core.service.jbp.creditreport.enums.JbpCreditReportResultGroup;
import com.yqg.core.service.jbp.goldencard.enums.GoldenCardStrategy;
import com.yqg.core.service.jbp.goldencard.enums.GoldenCardV2DisplayStrategy;
import com.yqg.core.service.loan.intention.types.LoanIntentionDisplayStrategy;
import com.yqg.core.service.loan.repayment.account.enums.DanaExperimentResult;
import com.yqg.core.service.orderpage.quickorder.emums.QuickOrderResultGroup;
import com.yqg.core.service.payment.factory.collectioninfo.processors.enums.CollectInformationDuringCreateOrderABTestResultGroup;
import com.yqg.core.service.user.thirdpartylogin.UserLoginProvider;
import com.yqg.ec.common.enums.jbp.JbpCreditReportPriceResultGroup;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.yqg.core.service.loan.infos.enums.LoanUserLivingSource.ADVANCE_H5;
import static com.yqg.core.service.loan.infos.enums.LoanUserLivingSource.TENCENT_H5;

public enum ExperimentNameSpace implements DescriptionBaseEnum {
  @Deprecated
  H5_LANDING_PAGE_ACCURATE_LOGIN("H5落地页精准登录",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  LOGIN_INPUT_DISPLAY_TYPE("借贷注册登录半蒙层优化",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  AUTO_DELAY_SEND_VERIFICATION("自动延迟发送验证码",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  @Deprecated
  FORCE_MERGE_ACCOUNT("强制换绑账号",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.NIK,
      0L,
      Long.MAX_VALUE),
  @Deprecated
  LOOSE_CHANGE_BIND("宽松条件强制换绑账号",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.NIK,
      0L,
      Long.MAX_VALUE),
  APPLIST_PRE_REGISTER_OR_LOGIN_SHOW_DIALOG("APPList弹窗是否展示",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name(), "TURE"),
      BooleanType.TRUE.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  APPLIST_DIALOG_DISPLAY_TYPE("APPList弹窗样式",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  REGISTER_ACTIVITY_FOR_APP("注册活动利益点--app",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  REGISTER_ACTIVITY_FOR_H5("注册活动利益点--h5",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  SKIP_GUIDE_PAGE("跳过引导页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  H5_LOADING_OPTIMIZATION_PRECISE("h5加载时长优化(精准)",
      () -> ImmutableSet.of(BooleanEnum.TRUE.name(), BooleanEnum.FALSE.name(), BooleanEnum.NONE.name()),
      BooleanEnum.NONE.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  H5_LOADING_OPTIMIZATION_WHOLE_PROCESS("h5加载时长优化(全流程)",
      () -> ImmutableSet.of(BooleanEnum.TRUE.name(), BooleanEnum.FALSE.name(), BooleanEnum.NONE.name()),
      BooleanEnum.NONE.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  APP_NSR_LOAD("APP NSR加载",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  H5_KEEP_USER_LOGIN_STATUS("h5保持登录态",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  H5_VERIFICATION_COUNT_DOWN_SECONDS("h5验证码倒计时秒数",
      () -> ImmutableSet.of("60", "10", "20", "30"),
      "60",
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  LANDING_PAGE_SUPPORT_DELETE_USER_REGISTER("落地页支持注销手机号重新注册",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  H5_SEND_VERIFICATION_OPT("h5验证码发送体验优化",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  /**
   * abtest scene迁移至中台
   */
  @Deprecated
  ORDER_PAGE_COUPON_PROMPT("367下单页优惠券提示",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  /**
   * abtest scene迁移至中台
   */
  ORDER_GUIDE_ANIMATION_368("368版本下单引导动画实验",
      () -> ImmutableSet.of(
          BooleanType.TRUE.name(),
          BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  JBP_GOLDEN_CARD("金卡实验",
      () -> Arrays.stream(GoldenCardStrategy.values()).map(Enum::name).collect(Collectors.toSet()),
      GoldenCardStrategy.NONE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  JBP_BLACK_CARD_AB("黑卡demo ab",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  JBP_GOLDEN_REPAYMENT_NOT_V("中收用户金卡不可借&无生效订单实验",
      () -> Arrays.stream(GoldenCardStrategy.values()).map(Enum::name).collect(Collectors.toSet()),
      GoldenCardStrategy.NONE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  PRODUCT_SHOW_BUBBLE_CONTENT_FOR_369("369版本是否展示下单页产品引导气泡",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  @Deprecated
  H5_CAN_CREATE_ORDER_PAGE("native迁移H5可下单页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  @Deprecated
  WEB_AUTH_BIND_BANK_CARD("native迁移H5鉴权绑卡页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  @Deprecated
  IMMEDIATE_CONTACTINFO_BEFORE_CREATE_ORDER("下单前置校验之前填写紧急联系人",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  @Deprecated
  IMMEDIATE_CONTACTINFO_WHEN_CREATE_ORDER("下单校验填写紧急联系人",
      () -> {
        return Arrays.stream(CollectInformationDuringCreateOrderABTestResultGroup.values()).map(Enum::name).collect(Collectors.toSet());
      },
      CollectInformationDuringCreateOrderABTestResultGroup.NOT_COLLECT.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  @Deprecated
  IMMEDIATE_CONTACTINFO_NOT_CREATE_ORDER("在贷不能下单填写紧急联系人",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  RISK_CHECKING_IMMEDIATE_CONTACT_BEFORE_CREATE_ORDER("Risk Checking Immediate Contact before create order",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  RISK_CHECKING_IMMEDIATE_CONTACT_NOT_CREATE_ORDER("Risk Checking Immediate Contact not create order",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  CREATE_ORDER_CHECK_SWITCH("新老下单前置校验接口",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  JBP_GOLDEN_CARD_INCREASE_CREDIT_POP_UP("金卡实验--提额分流弹窗样式实验",
      () -> ImmutableSet.of(GoldenCardV2DisplayStrategy.GOLDEN_CARD_V2_INCREASE_CREDIT_HALF_MODAL.name(),
          GoldenCardV2DisplayStrategy.GOLDEN_CARD_V2_INCREASE_CREDIT_POP_UP_WINDOW.name(), GoldenCardV2DisplayStrategy.GOLDEN_CARD_V2_INCREASE_CREDIT_ORIGINAL_POP_UP_WINDOW.name())
      , GoldenCardV2DisplayStrategy.GOLDEN_CARD_V2_INCREASE_CREDIT_ORIGINAL_POP_UP_WINDOW.name(),
      DiversionKeyType.NIK,
      0L,
      Long.MAX_VALUE),

  JBP_GOLDEN_CARD_SPEED_UP_POP_UP("金卡实验--加速打款弹窗样式实验",
      () -> ImmutableSet.of(GoldenCardV2DisplayStrategy.GOLDEN_CARD_V2_SPEED_UP_POP_UP_WINDOW.name(),
          GoldenCardV2DisplayStrategy.GOLDEN_CARD_V2_SPEED_UP_HALF_MODAL.name(), GoldenCardV2DisplayStrategy.GOLDEN_CARD_V2_SPEED_UP_ORIGINAL_POP_UP_WINDOW.name()),
      GoldenCardV2DisplayStrategy.GOLDEN_CARD_V2_SPEED_UP_ORIGINAL_POP_UP_WINDOW.name(),
      DiversionKeyType.NIK,
      0L,
      Long.MAX_VALUE),

  APP_MAX_FONT_SCALE("app大字体实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.C.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  ORDER_PAGE_QUICK_ORDER_BG("下单页快速下单半蒙层实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  ORDER_PAGE_QUICK_ORDER_BG_NATIVE("下单页快速下单半蒙层实验-native",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  LOAN_USER_LOAN_INTENTION_V3("授信即下单新开实验V3",
      () -> ImmutableSet.of(
          LoanIntentionDisplayStrategy.A0.name(),
          LoanIntentionDisplayStrategy.A2.name(),
          LoanIntentionDisplayStrategy.A3.name(),
          LoanIntentionDisplayStrategy.A4.name()
      ),
      LoanIntentionDisplayStrategy.A0.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  LOAN_USER_LOAN_INTENTION_V3_B("授信即下单新开实验V3_B",
      () -> ImmutableSet.of(
          LoanIntentionDisplayStrategy.A0.name(),
          LoanIntentionDisplayStrategy.A2.name(),
          LoanIntentionDisplayStrategy.A3.name(),
          LoanIntentionDisplayStrategy.A4.name(),
          LoanIntentionDisplayStrategy.B0.name(),
          LoanIntentionDisplayStrategy.B1.name(),
          LoanIntentionDisplayStrategy.B2.name()
      ),
      LoanIntentionDisplayStrategy.B0.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  LOAN_USER_LOAN_INTENTION_PARENT_EXPERIMENT("授信即下单父实验",
      () -> ImmutableSet.of(
          BooleanType.TRUE.name(),
          BooleanType.FALSE.name()
      ),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      37013L,
      Long.MAX_VALUE
  ),

  LOAN_USER_LOAN_INTENTION_V4("授信即下单实验V4",
      () -> ImmutableSet.of(
          LoanIntentionDisplayStrategy.D0.name(),
          LoanIntentionDisplayStrategy.D1.name(),
          LoanIntentionDisplayStrategy.D2.name()
      ),
      LoanIntentionDisplayStrategy.D0.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),

  @Deprecated
  AUTH_WEB_CONTACT_INFO("h5迁移鉴权紧急联系人",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  LOAN_KTP_OCR_PROVIDER(
      "借款端 KTP OCR 渠道实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.E.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  CAN_ORDER_PAGE_V370_DISCOUNT_STYLE("370版本下单页折扣优惠样式",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  @Deprecated
  CHANGE_USER_WITH_EKYC_CHECK("换绑ekyc校验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.NIK,
      0L,
      Long.MAX_VALUE),
  XENDIT_DIRECT_DEBT("xendit api转化实验",
      () -> ImmutableSet.of(
          DirectDebtDisplayStrategy.REPAY_AFTER_DUE_DATE.name(),
          DirectDebtDisplayStrategy.REPAY_ANYTIME.name(),
          DirectDebtDisplayStrategy.NONE.name()),
      DirectDebtDisplayStrategy.NONE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  INCREASE_CREDIT_DECREASE_INTEREST_POPUP_WINDOW(
      "提额降息扑脸弹窗实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37300L,
      Long.MAX_VALUE
  ),

  AYO_CONNECT_DIRECT_DEBT("AyoConnect api转化实验",
      () -> ImmutableSet.of(
          DirectDebtDisplayStrategy.REPAY_AFTER_DUE_DATE.name(),
          DirectDebtDisplayStrategy.REPAY_ANYTIME.name(),
          DirectDebtDisplayStrategy.NONE.name()),
      DirectDebtDisplayStrategy.NONE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  BCA_DIRECT_DEBT("BCA api 转化实验",
      () -> ImmutableSet.of(
          DirectDebtDisplayStrategy.REPAY_AFTER_DUE_DATE.name(),
          DirectDebtDisplayStrategy.REPAY_ANYTIME.name(),
          DirectDebtDisplayStrategy.NONE.name()),
      DirectDebtDisplayStrategy.NONE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_V3("优惠模块，头图，vip皮肤展示分流V3",
      () -> Arrays.stream(DiscountDetailTopAreaVipUIDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet()),
      DiscountDetailTopAreaVipUIDisplayStrategy.CONTROL_GROUP.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  LOAN_ORDER_PAGE_SHOW_TERMS("下单页借款期限展示&引导借长期",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  COUPON_LIST_STYLE_371("优惠券列表样式，371版本",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  @Deprecated
  HOME_API_OPTIMIZATION_TEST("首页API优化实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  KTP_MARKETING_POPUP_DISPLAY("ktp环节营销奖励活动",
      () -> ImmutableSet.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  KTP_MARKETING_POPUP_DISPLAY_V3("ktp环节营销奖励活动V3",
      () -> ImmutableSet.of("C", "C1", "C2", "C3"),
      "C",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  KTP_MARKETING_POPUP_DISPLAY_V3_AUTO_JUMP("ktp弹窗自动跳转",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  CREATE_ORDER_INCENTIVE_ACTIVITY_AB_H5("下单激励活动AB实验H5",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  CREATE_ORDER_INCENTIVE_ACTIVITY_AB_V2(
      "下单激励活动AB实验H5分流V2",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      DiversionKeyType.USER_ID,
      37300L,
      Long.MAX_VALUE),
  CREATE_ORDER_INCENTIVE_ACTIVITY_AB_V3(
      "下单激励活动AB实验H5分流V3",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "D",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  @Deprecated
  CREATE_ORDER_INCENTIVE_ACTIVITY_AB_V4(
      "下单激励活动AB实验H5分流V4",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      DiversionKeyType.USER_ID,
      37300L,
      Long.MAX_VALUE
  ),
  CREATE_ORDER_INCENTIVE_ACTIVITY_AB_NATIVE("下单激励活动AB实验NATIVE",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  ORDER_PAGE_RETURN_BUTTON_QUICK_ORDER_POPUP_FOR_CASH("下单页返回按钮快速下单弹窗",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  ORDER_PAGE_RETURN_BUTTON_QUICK_ORDER_POPUP_FOR_CASH_NATIVE("下单页返回按钮快速下单弹窗",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  HOME_PAGE_POPUP_FOR_CASH("首页弹窗现金实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  USER_IP_REGISTER_PROVIDER("用户ip注册供应商实验",
      () -> ImmutableSet.of(
          UserLoginProvider.EASY_CASH.name(),
          UserLoginProvider.IPIFICATION.name(),
          UserLoginProvider.ALIBABA_IP.name()
      ),
      UserLoginProvider.IPIFICATION.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  SHOPEE_ONLINE_SHOPPING_INFO_AB("shopee电商信息实验",
      () -> {
        return ImmutableSet.of("NONE", "WE_BANK");
      },
      "NONE",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  USER_REGISTER_PAGE_AUTHORIZATION_CONTENT("注册页授权文案",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.DEVICE_TOKEN, 0L, Long.MAX_VALUE),

  USER_REGISTER_PAGE_BANK_AUTHORIZATION_CONTENT("注册页银行营销授权文案",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.DEVICE_TOKEN, 0L, Long.MAX_VALUE),

  ME_PAGE_WALLET_URL(
      "用户页钱包按钮分流实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),

  USER_AUTHORIZATION_POPUP_WINDOW(
      "用户授权弹窗展示实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  NOT_SHOW_SPLASH_SCREEN(
      "不展示开屏页",
      () -> ImmutableSet.of(
          "A", "B"
      ),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  RETURN_BUTTON_COUPON_POP_UP_EXPERIMENT(
      "挽留弹窗（有优惠券）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  REGISTER_MARKETING_POINT(
      "注册头图实验",
      () -> ImmutableSet.of(
          "A", "B", "C", "A1", "A2", "A3", "B1", "B2", "B3"
      ),
      "A",
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE
  ),
  RETURN_BUTTON_CREDIT_POP_UP_EXPERIMENT(
      "挽留弹窗（有临时额度）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  RETURN_BUTTON_NO_COUPON_CREDIT_AND_LIMIT_AUTH_POP_UP_EXPERIMENT(
      "挽留弹窗（无优惠券、和临时额度且完件三天内）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  RETURN_BUTTON_NO_COUPON_CREDIT_AND_AUTH_LONG_POP_UP_EXPERIMENT(
      "挽留弹窗（无优惠券、临时额度且完件4天以上）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  QUICK_ORDER_LOAN_AMOUNT_EXPERIMENT(
      "快速下单额度测试实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  ACCEPT_BUT_CAN_NOT_LOAN(
      "授信通过但不可解",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  AUTO_JUMP_LEVEL2_FREQUENCY("首页自动跳转二级下单页频次限制",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  QUICK_ORDER_RELOAN_EXPERIMENT_H5(
      "快速下单-H5实验（有借款）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  QUICK_ORDER_FIRST_LOAN_EXPERIMENT_H5(
      "快速下单-H5实验（无借款）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  QUICK_ORDER_RELOAN_EXPERIMENT_NATIVE(
      "快速下单-NATIVE实验（有借款）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  @Deprecated
  QUICK_ORDER_FIRST_LOAN_EXPERIMENT_NATIVE(
      "快速下单-NATIVE实验（无借款）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  QUICK_ORDER_H5_STYLE_EXPERIMENT(
      "快速下单1.0（H5版）样式测试",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  REGISTER_MARKETING_POINT_SUB1(
      "注册头图实验-子实验1",
      () -> ImmutableSet.of(
          "A1", "A2", "A3"
      ),
      "A1",
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE
  ),
  REGISTER_MARKETING_POINT_SUB2(
      "注册头图实验-子实验2",
      () -> ImmutableSet.of(
          "B1", "B2", "B3"
      ),
      "B1",
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE
  ),
  LOGIN_SEND_VERIFICATION_AGAIN("登录otp重发",
      () -> ImmutableSet.of(
          "A",
          "B",
          "C"
      ),
      "A",
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE
  ),
  ORDER_SEND_VERIFICATION_AGAIN("下单otp重发",
      () -> ImmutableSet.of(
          "A",
          "D",
          "E"
      ),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  BACK_UP_HOST_EXPERIMENT("获取域名列表实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE),
  APP_NOTIFICATION_POPUP_DISPLAY_EXP("注册登陆-系统通知权限弹窗时机后置实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE
  ),
  APP_NOTIFICATION_POPUP_DISPLAY_EXP_SUB("注册登陆-系统通知权限弹窗时机后置实验-子实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.B.name(),
      DiversionKeyType.DEVICE_TOKEN,
      0L,
      Long.MAX_VALUE
  ),
  DIRECT_DEBIT_GUIDE_PAGE_EXPERIMENT("代扣引导页样式实验",
      () -> ImmutableSet.of(
          DirectDebitGuidePageStrategy.ORIGINAL_PAGE.name(),
          DirectDebitGuidePageStrategy.PAGE_A.name(),
          DirectDebitGuidePageStrategy.PAGE_B.name()),
      DirectDebitGuidePageStrategy.ORIGINAL_PAGE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  EASYPLUS_POP_UP_V1("账单页增加中收铺脸弹窗",
      () -> ImmutableSet.of(
          "A",
          "B",
          "C"
      ),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  STANDARD_INTEREST_RATE_AB("标准利率",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  RISK_BATCH_TASK_MULTI_LOAN_DIVERSION("风控续借跑批分流",
      () -> ImmutableSet.of("A", "B", "C", "D", "E"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  AUTH_NEED_POB("鉴权出生地填写分流",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  VERIFY_LIVING_METHOD_H5("借贷H5鉴权活体采集分流",
      () -> ImmutableSet.of(ADVANCE_H5.name(), TENCENT_H5.name()),
      ADVANCE_H5.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  KTP_SELECT_PHOTO_BUTTON_DISPLAY("ktp照片从相册获取",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  @Deprecated
  REJECT_LOAN_MARKET_INFO_NOT_INCREASE("被拒贷超实验--不能增信重申",
      () -> {
        return ImmutableSet.of("A", "B");
      },
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  @Deprecated
  REJECT_LOAN_MARKET_INFO_CAN_INCREASE("被拒贷超实验--能增信重申",
      () -> {
        return ImmutableSet.of("A", "B");
      },
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  ORDER_STEP_NEED_POB("下单前出生地填写分流",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  VIRTUAL_CREDITS("降额虚拟额度分流",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),

  UNION_REPAYMENT_EXPR("中收合并还款展示分流",
      () -> Arrays.stream(UnionRepaymentDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet()),
      UnionRepaymentDisplayStrategy.ORIGINAL_PAGE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  QUICK_ORDER_OPTIONAL_LOAN_AMOUNT_EXPERIMENT(
      "快速下单额度可选择实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  PARTIAL_REPAYMENT_EXPERIMENT(
      "部分还款实验",
      () -> ImmutableSet.of(
          RepaymentChannelPageDisplayStrategy.A.name(),
          RepaymentChannelPageDisplayStrategy.B.name(),
          RepaymentChannelPageDisplayStrategy.C.name(),
          RepaymentChannelPageDisplayStrategy.D.name()
      ),
      RepaymentChannelPageDisplayStrategy.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  HOMEPAGE_V3_AUTH_UNFINISHED_EXPERIMENT("未完件用户/状态的新老首页实验",
      () -> ImmutableSet.of("V1", "V2", "V3_INCENTIVE", "V3_PROGRESSBAR"),
      "V2",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  HOMEPAGE_V3_AUTH_FINISHED_EXPERIMENT("已完件用户/状态的新老首页实验",
      () -> ImmutableSet.of("V1", "V2", "V3"),
      "V2",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  AUTH_WEB_LIVING_INFO("鉴权web迁移",
      () -> ImmutableSet.of("TRUE", "FALSE"),
      "FALSE",
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),

  REPAYMENT_UI_ENHANCEMENT(
      "还款页面样式强化实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37500L, Long.MAX_VALUE),
  IDN_DANA_EWALLET_REPAYMENT_METHOD_V2("dana 还款方式的分流",
      () -> ImmutableSet.of("DYNAMIC", "STATIC_VA", "NONE"),
      "NONE",
      DiversionKeyType.USER_ID, 35300L, Long.MAX_VALUE),
  HOME_V3_LOAN_COUPON("新首页优惠券样式",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  QUICK_ORDER_CASH_ACTIVITY_EXPERIMENT(
      "快速下单3.0与现金奖励融合实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),
  QUICK_ORDER_RETAIN_DIALOG_UI("下单页挽留弹窗+一键借款+现金奖励1.0-UI",
      () -> ImmutableSet.of("C", "C1", "C2"),
      "C",
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),
  ORDER_PAGE_NEW_STYLE_EXPERIMENT(
      "下单页新样式重构2.0实验",
      () -> ImmutableSet.of(
          "V1", "V2_AGREEMENT", "V2_NO_AGREEMENT"
      ),
      "V1",
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),
  AUTH_RETURN_AC("鉴权返回页挽留参与活动",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "A",
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  REGISTER_MARKETING_POINT_V2_TYPE_1(
      "注册头图实验V2-Type1",
      () -> ImmutableSet.of(
          "A0", "A1", "A2", "A3", "B1", "B2", "B3"
      ),
      "B2",
      DiversionKeyType.DEVICE_TOKEN,
      0L, Long.MAX_VALUE),
  REGISTER_MARKETING_POINT_V2_TYPE_2(
      "注册头图实验V2-Type2",
      () -> ImmutableSet.of(
          "A0", "A1", "A2", "A3", "B1", "B2", "B3"
      ),
      "A0",
      DiversionKeyType.DEVICE_TOKEN,
      0L, Long.MAX_VALUE),
  HOMEPAGE_WHATSAPP(
      "首页填写whatsapp",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  ORDER_SUCCESS_WHATSAPP(
      "下单成功后填写whatsapp",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),
  QUICK_ORDER_REOPEN_EXPERIMENT(
      "一键借款实验策略调整",
      () -> Arrays.stream(QuickOrderResultGroup.values()).map(Enum::name).collect(Collectors.toSet()),
      QuickOrderResultGroup.CONTROL_GROUP.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  ORDER_STEP_NEED_COMPANY_NAME(
      "Order Step Need Company Name",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  AUTH_NEED_COMPANY_NAME_COMPANY_NUMBER(
      "Auth Need Company Name Company Number",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  AUTO_JUMP_AUTH("自动跳转鉴权页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  LOAN_T0_CREDIT_SENSITIVITY(
      "首贷过件T0额度感知",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 36900L, Long.MAX_VALUE),
  @Deprecated
  ORDER_PAGE_RETAIN_V3_P(
      "下单页挽留V3-父实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  ORDER_PAGE_RETAIN_V3_S(
      "下单页挽留V3-子实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  @Deprecated
  CREDIT_INCREASED_AFTER_REPAY_AWARE_POPUP(
      "还款后额度恢复/提升弹窗展示实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),
  JBP_CREDIT_REPORT_ME_DISPLAY_EXPERIMENT(
      "中收信用报告产品me频道页信用报告可见父实验",
      () -> Arrays.stream(JbpCreditReportResultGroup.values()).map(Enum::name).collect(Collectors.toSet()),
      JbpCreditReportResultGroup.HIDE.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  MOTHER_LAST_NAME_CHECK_RULE_KTP_CONFIRM_PAGE(
      "母亲姓氏校验规则-KTP确认页",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  MOTHER_LAST_NAME_CHECK_RULE_ORDER_SUPPLEMENT_PAGE(
      "母亲姓氏校验规则-下单补件页",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  SECOND_RISK_REJECT_DISPLAY(
      "首页二次风控拒绝展示",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 38200L, Long.MAX_VALUE),
  MARKETING_CREDIT_SENSITIVITY(
      "营销临额感知",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 36900L, Long.MAX_VALUE),
  @Deprecated
  FIXED_CREDIT_SENSITIVITY(
      "固定额度感知",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  JBP_CREDIT_REPORT_HOMEPAGE_POPUP_EXPERIMENT(
      "中收信用报告产品首页信用报告弹窗子实验",
      () -> Arrays.stream(JbpCreditReportPopupResultGroup.values()).map(Enum::name).collect(Collectors.toSet()),
      JbpCreditReportPopupResultGroup.HIDE.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  JBP_CREDIT_REPORT_ME_POPUP_EXPERIMENT(
      "中收信用报告产品me频道页信用报告弹窗子实验",
      () -> Arrays.stream(JbpCreditReportPopupResultGroup.values()).map(Enum::name).collect(Collectors.toSet()),
      JbpCreditReportPopupResultGroup.HIDE.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  USER_ENTER_REVOLVING_CREDITS_V2(
      "用户是否进入循环额度流程-重开",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  USER_RECALL_TRIGGER_SECOND_RISK_ORDER(
      "回捞场景自动触发二次风控下单",
      () -> ImmutableSet.of("A", "B", "C", "D", "E"),
      "A",
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  NATIONAL_DAY_2025_ORDER_PAGE_BACK_BUTTON_RETENTION_POP_UP(
      "国庆 2025 下单页返回降息挽留弹窗",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),
  NATIONAL_DAY_2025_HOMEPAGE_INTEREST_RATE_CUT_POP_UP(
      "国庆 2025 首页降息动效弹窗",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),
  NATIONAL_DAY_2025_ORDER_PAGE_INTEREST_RATE_CUT_POP_UP(
      "国庆 2025 下单页降息动效弹窗",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L, Long.MAX_VALUE),
  JBP_CREDIT_REPORT_PRICE_EXPERIMENT(
      "中收信用报告产品价格子实验",
      () -> Arrays.stream(JbpCreditReportPriceResultGroup.values()).map(Enum::name).collect(Collectors.toSet()),
      JbpCreditReportPriceResultGroup.ONE.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  BILL_PAGE_TO_H5_NOT_OVERDUE(
      "未逾期用户Bill页切H5实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  BILL_PAGE_TO_H5_OVERDUE(
      "逾期用户Bill页切H5实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  GOLD_CARD_V2("中收V2实验--先付后用",
      () -> Arrays.stream(GoldCardVersion.values()).map(Enum::name).collect(Collectors.toSet()),
      GoldCardVersion.V1.name(),
      DiversionKeyType.USER_ID, 38700L, Long.MAX_VALUE),
  QUICK_ORDER_CASH_ACTIVITY_STYLE_EXPERIMENT(
      "一键借款样式文案",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37300L, Long.MAX_VALUE),
  QUICK_ORDER_CASH_ACTIVITY_RIGHTS(
      "一键借款权益",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37300L, Long.MAX_VALUE),
  REPAY_PLAN_INTEREST_COUPON(
      "还款计划免息券",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      36900L, Long.MAX_VALUE),
  REGISTER_LOGIN_DYNAMIC_H5_RESOURCE(
      "注册登录页资源位做局部动态H5化",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN, 37700L, Long.MAX_VALUE),
  SUB_REGISTER_LOGIN_DYNAMIC_H5_RESOURCE(
      "注册登录页资源位做局部动态H5化-子实验极简版",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.DEVICE_TOKEN, 37800L, Long.MAX_VALUE),
  REPAYMENT_PARTIAL_REPAYMENT_NORMAL(
      "部分还款实验-正常状态",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37413L, Long.MAX_VALUE,
      CommonABTestResultGroup.B.name()
  ),
  REPAYMENT_PARTIAL_REPAYMENT_OVERDUE(
      "部分还款实验-逾期状态",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37413L, Long.MAX_VALUE,
      CommonABTestResultGroup.B.name()
  ),
  REPAYMENT_REMINDER_NORMAL(
      "还款强化样式实验-正常状态",
      () -> ImmutableSet.of(
          RepaymentReminderStrategy.A.name(),
          RepaymentReminderStrategy.B.name(),
          RepaymentReminderStrategy.C.name()
      ),
      RepaymentReminderStrategy.B.name(),
      DiversionKeyType.USER_ID,
      38113L, Long.MAX_VALUE
  ),
  REPAYMENT_REMINDER_OVERDUE(
      "还款强化样式实验-逾期状态",
      () -> ImmutableSet.of(
          RepaymentReminderStrategy.A.name(),
          RepaymentReminderStrategy.B.name(),
          RepaymentReminderStrategy.C.name()
      ),
      RepaymentReminderStrategy.B.name(),
      DiversionKeyType.USER_ID,
      38113L, Long.MAX_VALUE
  ),
  REPAYMENT_CHANNEL_DANA_NORMAL(
      "DANA 还款渠道实验-正常状态",
      () -> ImmutableSet.of(
          DanaExperimentResult.NONE.name(),
          DanaExperimentResult.STATIC_VA.name(),
          DanaExperimentResult.DYNAMIC.name()
      ),
      DanaExperimentResult.NONE.name(),
      DiversionKeyType.USER_ID,
      35300L, Long.MAX_VALUE
  ),
  REPAYMENT_CHANNEL_DANA_OVERDUE(
      "DANA 还款渠道实验-逾期状态",
      () -> ImmutableSet.of(
          DanaExperimentResult.NONE.name(),
          DanaExperimentResult.STATIC_VA.name(),
          DanaExperimentResult.DYNAMIC.name()
      ),
      DanaExperimentResult.NONE.name(),
      DiversionKeyType.USER_ID,
      35300L, Long.MAX_VALUE
  ),
  USER_RECALL_TRIGGER_SECOND_RISK_ORDER_FOR_LOAN(
      "回捞场景自动触发二次风控下单（首贷场景）",
      () -> ImmutableSet.of("A", "B", "C", "D", "E"),
      "A",
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  USER_RECALL_TRIGGER_SECOND_RISK_ORDER_FOR_RELOAN(
      "回捞场景自动触发二次风控下单（复贷场景）",
      () -> ImmutableSet.of("A", "B", "C", "D", "E"),
      "A",
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  @Deprecated
  ORDER_AGREEMENT_FLOATING_LAYER(
      "下单协议浮层实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  LOAN_AMOUNT_DISPLAY_LOGIC_WHEN_ENTER_ORDER_PAGE(
      "进入下单页时额度展示逻辑",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 36900L, Long.MAX_VALUE),
  CAN_NOT_SKIP_OCR(
      "ocr环节不可跳过实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID, 37113L, Long.MAX_VALUE),
  ORDER_PAGE_POPUP_PRIORITY(
      "下单页扑脸弹窗优先级调整实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 36899L, Long.MAX_VALUE),
  ORDER_PAGE_POPUP_MATERIAL_OPTIMIZATION(
      "下单页扑脸弹窗样式优化实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 36900L, Long.MAX_VALUE),
  @Deprecated
  REPAYMENT_DIRECT_DEBIT_STRATEGY(
      "代扣规则实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  GOLDEN_CARD_CAN_BORROW_STYLE(
      "中收金卡可借用户弹窗素材实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 36899L, Long.MAX_VALUE),
  LOAN_180_DAYS_REAPPLY(
      "首贷180+被拒重审",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),

  SUPERBANK_CHANNEL_USER_BINDING_CARD("Superbank渠道用户绑卡",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      DiversionKeyType.USER_ID,
      36900L,
      Long.MAX_VALUE),
  EC_PLUS_BUTTON("中收会员button",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.USER_ID,
      36900L,
      Long.MAX_VALUE),
  REGISTER_LOGIN_RESOURCE_V3_FOR_REGISTER(
      "注册登录页-营销利益点v3-注册",
      () -> ImmutableSet.of("A2", "C1", "C2"),
      "A2",
      DiversionKeyType.DEVICE_TOKEN, 37900L, Long.MAX_VALUE),
  REGISTER_LOGIN_RESOURCE_V3_FOR_LOGIN(
      "注册登录页-营销利益点v3-登录",
      () -> ImmutableSet.of("A2", "C1", "C2"),
      "A2",
      DiversionKeyType.DEVICE_TOKEN, 37900L, Long.MAX_VALUE),
  BIND_CARD_DELAY("绑卡后置实验",
      () -> Arrays.stream(BindCardDelayGroup.values()).map(Enum::name).collect(Collectors.toSet()),
      BindCardDelayGroup.CONTROL_GROUP.name(),
      DiversionKeyType.USER_ID,
      37900L,
      Long.MAX_VALUE),
  REGISTER_LOGIN_RETAIN_DIALOG_V2("注册登录挽留弹窗实验v2",
      () -> ImmutableSet.of("D", "E", "F"),
      "D",
      DiversionKeyType.DEVICE_TOKEN,
      37400L,
      Long.MAX_VALUE),
  CASH_REWARD_EDITION_ADDS_BORROW_OTHER_AMOUNT_BUTTON(
      "一键借款5.0-现金奖励版增加【借其他金额按钮】",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      36900L,
      Long.MAX_VALUE
  ),
  BILL_AUTO_JUMP_LEVEL2_FREQUENCY(
      "账单即将到期用户自动跳转下单页降频",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.USER_ID,
      37200L,
      Long.MAX_VALUE
  ),
  REPAYMENT_CHANNEL_GOPAY_NORMAL(
      "GoOay 还款渠道展示实验-正常状态",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      31900L, Long.MAX_VALUE
  ),
  REPAYMENT_CHANNEL_GOPAY_OVERDUE(
      "GoOay 还款渠道展示实验-逾期状态",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      31900L, Long.MAX_VALUE
  ),
  JBP_CARD_CREDIT_GUIDED_PAYMENT_APP(
      "中收会员账单页先用后付支付引导弹窗-app",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37900L,
      Long.MAX_VALUE
  ),
  JBP_CARD_CREDIT_GUIDED_PAYMENT_H5(
      "中收会员账单页先用后付支付引导弹窗-H5",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37900L,
      Long.MAX_VALUE
  ),
  ORDER_LOAN_PUSH_COUPON(
      "下单页PUSH发券实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 36900L, Long.MAX_VALUE),
  SECOND_ORDER_AUTO_JUMP_AND_POPUP_FOR_RECALL(
      "二次下单自动跳转和引导弹窗-回捞流转",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      36900L,
      Long.MAX_VALUE
  ),
  SECOND_ORDER_AUTO_JUMP_AND_POPUP_FOR_QUICK_REDUCE(
      "二次下单自动跳转和引导弹窗-降额快速下单",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      38000L,
      Long.MAX_VALUE
  ),
  KTP_POINT_V4("ktp环节营销奖励活动V4",
      () -> ImmutableSet.of("C2", "D1", "D2"),
      "C2",
      DiversionKeyType.USER_ID,
      36900L,
      Long.MAX_VALUE),
  H5_WHOLE_PROCESS_AUTH_UI_JUMP(
      "h5全流程ui对齐完件跳转UI实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      35300L,
      Long.MAX_VALUE,
      CommonABTestResultGroup.A.name()
  ),
  H5_WHOLE_PROCESS_ORDER_UI_JUMP(
      "h5全流程ui对齐下单跳转UI实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      35300L,
      Long.MAX_VALUE,
      CommonABTestResultGroup.A.name()
  ),
  //  MARKET_COUPON_GRANT(
//      "营销临额发券实验-首贷",
//      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
//      CommonABTestResultGroup.A.name(),
//      DiversionKeyType.USER_ID,
//      0L,
//      Long.MAX_VALUE
//  ),
  //⚠️⚠️⚠️ 这个实验用到了 getLastResult 的逻辑，该逻辑及时实验关量了也会真的取上次实验的结果
  RISK_COUPON_LIMIT_LOAN(
      "营销临额上限实验-首贷",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  CREDIT_USAGE_RATE_EXPERIMENT(
      "额度使用率长期实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  COUPON_LONG_TERM_EXPERIMENT(
      "优惠券长期实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  MARKET_COUPON_GRANT(
      "营销临额发券实验-首贷",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  JBP_VALID_UNPAID_POPUP(
      "中收会员开通未支付弹窗实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37300L,
      Long.MAX_VALUE
  ),
  RISK_BATCH_RELOAN_FIRST_MULTI_LOAN_DIVERSION("风控复贷首续跑批分流",
      () -> ImmutableSet.of("A", "B"),
      "A",
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  QUICK_ORDER_CASH_ACTIVITY_V2_EXPERIMENT(
      "一键借款5.0-改版实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37300L,
      Long.MAX_VALUE
  ),
  AUTH_NEED_COMPANY_NAME(
      "Auth Need Company Name",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID, 0L, Long.MAX_VALUE),
  MARKET_COUPON_GRANT_RELOAN(
      "营销临额发券实验-复贷",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  @Deprecated
  TOTAL_COUPON_CREDIT_LIMIT_RELOAN(
      "营销临额上限实验-复贷",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  INCREASE_CREDIT_DECREASE_INTEREST_POPUP_WINDOW_LOAN(
      "提额降息扑脸弹窗实验_首贷",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37300L,
      Long.MAX_VALUE
  ),
  INCREASE_CREDIT_DECREASE_INTEREST_POPUP_WINDOW_RELOAN(
      "提额降息扑脸弹窗实验_复贷",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      37300L,
      Long.MAX_VALUE
  ),
  @Deprecated
  GUIDE_USER_BORROW_MORE(
      "引导用户借更多实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      36900L,
      Long.MAX_VALUE
  ),
  @Deprecated
  POST_LOAN_AGREEMENT_FLOATING_LAYER(
      "借款后置协议浮层实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE),
  REPAY_PLAN_REDUCE(
      "还款计划强化月还款降低实验",
      () -> Arrays.stream(RepayPlanReduceGroup.values()).map(Enum::name).collect(Collectors.toSet()),
      RepayPlanReduceGroup.CONTROL_GROUP.name(),
      DiversionKeyType.USER_ID,
      36900L,
      Long.MAX_VALUE
  ),
  CREDIT_INCREASED_SENSITIVITY_V2(
      "提额感知v2",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      38200L,
      Long.MAX_VALUE
  ),
  INIT_USER_TYPE(
      "用户类型初始化实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  @Deprecated
  CREDIT_LIMIT_ADD_SCENE(
      "额度上限场景规则实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  COUPON_LONG_TERM_SUB_EXPERIMENT(
      "优惠券长期子实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  SHOW_CUSTOMER_GUIDE_POPUP(
      "账单页截屏转客服实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      38213L,
      Long.MAX_VALUE
  ),
  GOPAY_DIRECT_DEBIT_OVERDUE(
      "GOPAY 代扣实验（逾期状态）",
      () -> ImmutableSet.of(GoPayDirectDebitStrategy.A.name(), GoPayDirectDebitStrategy.B.name()),
      GoPayDirectDebitStrategy.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  GOPAY_DIRECT_DEBIT_NORMAL(
      "GOPAY 代扣实验（正常状态）",
      () -> ImmutableSet.of(GoPayDirectDebitStrategy.A.name(), GoPayDirectDebitStrategy.B.name()),
      GoPayDirectDebitStrategy.A.name(),
      DiversionKeyType.USER_ID,
      0L,
      Long.MAX_VALUE
  ),
  REPAY_CHANNEL_SORT_NORMAL(
      "详情页还款渠道排序-正常",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      38000L,
      Long.MAX_VALUE
  ),
  REPAY_CHANNEL_SORT_OVERDUE(
      "详情页还款渠道排序-逾期",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      DiversionKeyType.USER_ID,
      38000L,
      Long.MAX_VALUE
  ),
  ;

  public static final Set<String> EXP_NAME_SET = Arrays.stream(ExperimentNameSpace.values())
      .map(Enum::name).collect(Collectors.toSet());

  //长期实验集合
  public static final Set<ExperimentNameSpace> LONG_TERM_EXPERIMENT_SET = ImmutableSet.of(CREDIT_USAGE_RATE_EXPERIMENT,
      COUPON_LONG_TERM_EXPERIMENT);
  /**
   * 场景支持的选项
   */
  public final Supplier<Set<String>> options;
  /**
   * web展示用名称
   */
  public final String experimentDefaultName;
  /**
   * 支持的diversionKeyType
   */
  public final DiversionKeyType diversionKeyType;
  /**
   * 场景类型默认的场景，用于代码上线admin还没配置的情况
   */
  public final String defaultScene;
  /**
   * 最小版本号限制
   */
  public final Long minVersion;
  /**
   * 最大版本号限制
   */
  public final Long maxVersion;

  /**
   * API默认场景
   */
  public final String apiChannelDefaultScene;

  ExperimentNameSpace(String experimentDefaultName, Supplier<Set<String>> options, String defaultScene, DiversionKeyType supportKeyType, Long minVersion, Long maxVersion) {
    this(experimentDefaultName, options, defaultScene, supportKeyType, minVersion, maxVersion, defaultScene);
  }

  ExperimentNameSpace(String experimentDefaultName, Supplier<Set<String>> options, String defaultScene, DiversionKeyType supportKeyType, Long minVersion, Long maxVersion, String apiChannelDefaultScene) {
    this.experimentDefaultName = experimentDefaultName;
    this.options = options;
    this.diversionKeyType = supportKeyType;
    this.defaultScene = defaultScene;
    this.minVersion = minVersion;
    this.maxVersion = maxVersion;
    this.apiChannelDefaultScene = apiChannelDefaultScene;
  }

  @Override
  public String getDescription() {
    return experimentDefaultName;
  }

  public Boolean containsScene(String scene) {
    return options.get().contains(scene);
  }
}
