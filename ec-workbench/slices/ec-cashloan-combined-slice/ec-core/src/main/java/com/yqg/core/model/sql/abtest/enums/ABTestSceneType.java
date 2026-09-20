package com.yqg.core.model.sql.abtest.enums;

import static com.yqg.core.service.loan.infos.enums.LoanUserLivingSource.ADVANCE;
import static com.yqg.core.service.loan.infos.enums.LoanUserLivingSource.ADVANCE_H5;
import static com.yqg.core.service.loan.infos.enums.LoanUserLivingSource.FACEPP;
import static com.yqg.core.service.loan.infos.enums.LoanUserLivingSource.FACEPP_V5;
import static com.yqg.core.service.loan.infos.enums.LoanUserLivingSource.TENCENT_H5;
import static com.yqg.core.util.thirdparty.megvii.enums.LivenessType.FLASH;
import static com.yqg.core.util.thirdparty.megvii.enums.LivenessType.MEGLIVE;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.yqg.common.spring.util.enums.DescriptionBaseEnum;
import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.sql.financing.enums.FinancingInstalmentMatchMode;
import com.yqg.core.model.sql.signature.enums.HandWrittenSignatureDivisionStrategy;
import com.yqg.core.model.sql.signature.enums.VidaSignatureDivisionStrategy;
import com.yqg.core.model.sql.version.enums.VersionConfigType;
import com.yqg.core.service.abtest.enums.ABTestParentGroupType;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.enums.CreditQuotaTotalEnum;
import com.yqg.core.service.cashloan.repay.enums.GoPayDirectDebitStrategy;
import com.yqg.core.service.cashloan.repay.enums.RepaymentChannelPageDisplayStrategy;
import com.yqg.core.service.cashloan.repay.enums.UnionRepaymentDisplayStrategy;
import com.yqg.core.service.cashloan.vo.enums.CheckUpdateSceneType;
import com.yqg.core.service.cashloan.vo.enums.DiscountDetailAnimationStrategy;
import com.yqg.core.service.cashloan.vo.enums.DiscountDetailTopAreaVipUIDisplayStrategy;
import com.yqg.core.service.cashloan.vo.enums.ExperimentDisplayStrategy;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.cashloan.vo.enums.LoanVipStrategy;
import com.yqg.core.service.directdebit.enums.DirectDebitGuidePageStrategy;
import com.yqg.core.service.directdebit.enums.DirectDebtDisplayStrategy;
import com.yqg.core.service.general.appconfig.enums.IdentitySelectionType;
import com.yqg.core.service.general.appconfig.enums.NormalAuthLivingInfoPageDisplayType;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.PopupFrequencyTypeEnum;
import com.yqg.core.service.loan.intention.types.LoanIntentionDisplayStrategy;
import com.yqg.core.service.orderpage.quickorder.emums.QuickOrderResultGroup;
import com.yqg.core.service.payment.factory.collectioninfo.processors.enums.CollectInformationDuringCreateOrderABTestResultGroup;
import com.yqg.core.service.user.enums.VerificationDisplayConfigType;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.ec.common.utils.annotation.AnnotationsUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 该类用于维护abtest支持的场景，与web和app的交互也基于此类，增改前先跟app沟通下
 * 对于只有两个选择的场景，比如展示/不展示等，options使用true/false，其他场景结合业务和app商定
 */
@Slf4j
@Deprecated
/*
  该方法已废弃，后续统一使用下面方法
  com.yqg.core.service.abtest.ExperimentPlatformClientService.fetchResultFallBackWithDefaultScene(com.yqg.core.service.abtest.vo.ABTestBaseRequestVO, java.lang.Long)
*/
public enum ABTestSceneType implements DescriptionBaseEnum {
  DEMO("这是demo",
      () -> {
        return ImmutableSet.of("A", "B", "C");
      },
      "A",
      DiversionKeyType.DEFAULT_TYPES),

  DEMO_2("这是demo2",
      () -> {
        return ImmutableSet.of("A", "B", "C");
      },
      "A",
      DiversionKeyType.DEFAULT_TYPES),

  OPERATION_MARKETING_GROUP_2024H2("2024_H2留白组",
      () -> ImmutableSet.of(
          ABTestParentGroupType.EXPERIMENTAL_GROUP.name(),
          ABTestParentGroupType.CONTROL_GROUP.name(),
          ABTestParentGroupType.CONTROL_GROUP_2.name()
      ),
      ABTestParentGroupType.CONTROL_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      true,
      ABTestLaneBusinessType.REGISTER),
  @Deprecated
  OPERATION_MARKETING_GROUP_DEVICE_TOKEN_2025H1("2025_H1 device token留白组",
      () -> ImmutableSet.of(
          ABTestParentGroupType.EXPERIMENTAL_GROUP.name(),
          ABTestParentGroupType.CONTROL_GROUP.name()
      ),
      ABTestParentGroupType.CONTROL_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  OPERATION_MARKETING_GROUP_2024H2_AUTH("2024_H2留白组_完件中间层",
      () -> ImmutableSet.of(
          ABTestParentGroupType.EXPERIMENTAL_GROUP.name(),
          ABTestParentGroupType.CONTROL_GROUP.name()
      ),
      ABTestParentGroupType.CONTROL_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      true,
      ABTestLaneBusinessType.REGISTER),

  OPERATION_MARKETING_GROUP_2024H2_ACTIVITY("2024_H2留白组_活动中间层",
      () -> ImmutableSet.of(
          ABTestParentGroupType.EXPERIMENTAL_GROUP.name(),
          ABTestParentGroupType.CONTROL_GROUP.name()
      ),
      ABTestParentGroupType.CONTROL_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      true,
      ABTestLaneBusinessType.REGISTER),

  OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER("2024_H2留白组_下单中间层",
      () -> ImmutableSet.of(
          ABTestParentGroupType.EXPERIMENTAL_GROUP.name(),
          ABTestParentGroupType.CONTROL_GROUP.name()
      ),
      ABTestParentGroupType.CONTROL_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      true,
      ABTestLaneBusinessType.RISK_LOAN_ACCEPTED),

  REGISTER_LOGIN_PAGE_DISPLAY_RETURN_BUTTON("注册登录页返回按钮展示",
      () -> {
        //A展示，B不展示
        return ImmutableSet.of("true", "false");
      },
      "true",
      DiversionKeyType.NOT_LOGGED_IN_TYPES),

  REGISTER_LOGIN_PAGE_NEXT_STEP_CONTENT("注册登录页下一步文案",
      () -> {
        //A下一步，B立即查看
        return ImmutableSet.of("下一步", "立即查看");
      },
      "立即查看",
      DiversionKeyType.NOT_LOGGED_IN_TYPES),
  REGISTER_LOGIN_PAGE_VERIFY_METHOD("注册登录页验证方式",
      () -> {
        // AUTO_SELECT 旧的默认验证方式  MANUAL_SELECT 新的用户自主选择的验证方式
        return ImmutableSet.of("AUTO_SELECT", "MANUAL_SELECT");
      },
      "AUTO_SELECT",
      DiversionKeyType.NOT_LOGGED_IN_TYPES),
  AGREEMENT_READ_METHOD("协议展示方式",
      () -> {
        // 所有协议的展示方式，包括注册页，理财借贷下单页
        // CAN_SKIP 旧的方式，可以跳过阅读协议
        // FORCE_READ 新的方式，用户必须阅读到底
        return ImmutableSet.of("CAN_SKIP", "FORCE_READ");
      },
      "CAN_SKIP",
      DiversionKeyType.NOT_LOGGED_IN_TYPES),
  @Deprecated
  LOAN_DISCOUNT_DETAIL("首贷下单页优惠展示",
      () -> {
        //A旧下单页，B有优惠展示的新下单页
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  RELOAN_DISCOUNT_DETAIL("复贷下单页优惠展示",
      () -> {
        //A旧下单页，B有优惠展示的新下单页
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  MULTI_DISCOUNT_DETAIL("续借下单页优惠展示",
      () -> {
        //A旧下单页，B有优惠展示的新下单页
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  LOAN_PRODUCT_AMOUNT_INPUT("首贷产品金额输入方式分流",
      () -> {
        //A旧的滚动条方式，B新的用户自主输入金额方式
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  RELOAN_PRODUCT_AMOUNT_INPUT("复贷产品金额输入方式分流",
      () -> {
        //A旧的滚动条方式，B新的用户自主输入金额方式
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  MULTI_PRODUCT_AMOUNT_INPUT("续借产品金额输入方式分流",
      () -> {
        //A旧的滚动条方式，B新的用户自主输入金额方式
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  SHOW_RECEIVED_AMOUNT_CORNER_MARK("是否展示到账金额上方的角标文案",
      () -> {
        return ImmutableSet.of("true", "false");
      },
      "true",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  REGISTER_LOGIN_PAGE_DISPLAY_IPIFICATION("注册登录页展示ipification",
      () -> {
        return ImmutableSet.of("true", "false");
      },
      "true",
      DiversionKeyType.NOT_LOGGED_IN_TYPES),

  MODIFY_MOBILE_NUMBER_DISPLAY("是否展示修改手机号入口",
      () -> {
        //true：展示，false：隐藏
        return ImmutableSet.of("true", "false");
      },
      "false",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  APP_REFRESH_POPUP_WINDOW_REGULARLY("app是否定时刷新并展示弹窗",
      () -> {
        return ImmutableSet.of("true", "false");
      },
      "false",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  RISK_REDUCE_CREDITS("风控降额",
      () -> {
        //首页展示风控降额的文案
        return ImmutableSet.of("true", "false");
      },
      "false",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  MODIFY_MOBILE_NUMBER_CHECK("修改手机号审核方式",
      () -> {
        //AUTO：自动审核，MANUAL：人工审核
        return ImmutableSet.of("AUTO", "MANUAL");
      },
      "AUTO",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  SHOW_NEW_INVITATION_COMPAIGN("展示新版本的邀请活动样式",
      () -> {
        //首页展示风控降额的文案
        return ImmutableSet.of("true", "false");
      },
      "false",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  INVITATION_GROUP("邀请活动分组",
      () -> {
        //A、B组代表不同的活动id
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  TREE_ACTIVITY_GROUP("种树活动分组",
      () -> {
        //A、B组代表不同的活动id
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  ANNIVERSARY_ACTIVITY_GROUP("7周年庆活动分组",
      () -> {
        //A、B组代表不同的活动id
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  UPLOAD_ID_CARD_WITH_ALBUM_DISPLAY("上传KTP图片是否显示相册上传",
      () -> {
        //
        return ImmutableSet.of("true", "false");
      },
      "false",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  MERGE_ACCOUNT_DISPLAY("是否显示合并账号",
      () -> {
        //
        return ImmutableSet.of("true", "false");
      },
      "false",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  FINANCING_NEW_USER_NEED_REAL_NAME_VERIFICATION("理财新用户是否需要实名校验",
      () -> {
        return ImmutableSet.of("TRUE", "FALSE");
      },
      "FALSE",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  DISPLAY_ORDER_PAGE_QUICK_INPUT_AMOUNT("下单页快捷输入金额方式，是否展示下方快捷输入金额按钮",
      () -> {
        return ImmutableSet.of("TRUE", "FALSE");
      },
      "TRUE",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  DISPLAY_LOAN_MARKET_PRODUCT_PAGE("被拒用户是否展示贷超页面",
      () -> {
        return ImmutableSet.of("TRUE", "FALSE");
      },
      "FALSE",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  IS_RDL_USER("理财RDL用户分流",
      () -> ImmutableSet.of("true", "false"),
      "false",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  MANDIRI_RDL_PROVIDER_DIVERSE("MANDIRI RDL用户分流",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  BCA_RDL_PROVIDER_DIVERSE("BCA RDL用户分流",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  IDN_LOAN_HAND_WRITTEN_SIGNATURE_DIVISION_STRATEGY("印尼借贷手写签名分流 - 首贷用户",
      () -> {
        return Arrays.stream(HandWrittenSignatureDivisionStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      "CLICK_HAND_WRITTEN_SIGNATURE",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  IDN_RELOAN_HAND_WRITTEN_SIGNATURE_DIVISION_STRATEGY("印尼借贷手写签名分流 - 复贷用户",
      () -> {
        return Arrays.stream(HandWrittenSignatureDivisionStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      "CLICK_HAND_WRITTEN_SIGNATURE",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  VIDA_SIGNATURE_DIVISION("印尼借贷签名分流 - VIDA签名版本",
      () -> {
        return Arrays.stream(VidaSignatureDivisionStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      VidaSignatureDivisionStrategy.INDEPENDENT_DEVELOPED_CLICK_SIGNATURE_WHEN_CREATE_ORDER.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  CREATE_ORDER_PAGE_DISCOUNT_DETAIL_ANIMATION_STRATEGY("下单页强化差异定价利率优惠感知动画展示--废弃",
      () -> {
        return Arrays.stream(DiscountDetailAnimationStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      "NO_ANIMATION",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  CREATE_ORDER_PAGE_DISCOUNT_DETAIL_ANIMATION_STRATEGY_V2("下单页强化差异定价利率优惠感知动画展示V2",
      () -> {
        return ImmutableSet.of("A", "B", "C", "D");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  CREATE_ORDER_PAGE_DISCOUNT_DETAIL_ANIMATION_STRATEGY_V3("下单页强化差异定价利率优惠感知动画展示V3",
      () -> {
        return ImmutableSet.of("A", "B", "C");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  CREATE_ORDER_PAGE_CUT_INTEREST_COUPON_TOP_AREA_STRATEGY("下单页普通利率用户降息券优惠头图展示",
      () -> {
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  FACE_PP_VERIFY_LIVENESS("facePP活体的方式",
      () -> ImmutableSet.of(MEGLIVE.name(), FLASH.name()),
      MEGLIVE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  VERIFY_LIVENESS_METHOD("活体识别渠道分流",
      () -> ImmutableSet.of(ADVANCE.name(), FACEPP.name(), FACEPP_V5.name()),
      FACEPP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  PERMISSION_PAGE_FREQUENCY("权限页展示规则",
      () -> {
        return ImmutableSet.of("ONLY_ONCE", "NONE", "ONCE_EVERY_START");
      },
      "ONLY_ONCE",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  @Deprecated
  //分流下线，结果为true
  IDN_LOAN_AUTH_LIVING_INFO_AND_FIELD_CHOOSE_OPTIMAL("印尼借贷完件活体FACEPP优化分流",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_1("运营实验1",
      () -> {
        return ImmutableSet.of("A", "B", "C", "D");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_2("运营实验2",
      () -> {
        return ImmutableSet.of("A", "B", "C", "D");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_3("运营实验3",
      () -> {
        return ImmutableSet.of("A", "B", "C", "D");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_4("运营实验4",
      () -> {
        return ImmutableSet.of("A", "B", "C", "D");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_5("运营实验5",
      () -> {
        return ImmutableSet.of("A", "B", "C", "D");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_6("运营实验6", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_7("运营实验7", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_8("运营实验8", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_9("运营实验9", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_10("运营实验10", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_11("运营实验11", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_12("运营实验12", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_13("运营实验13", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_14("运营实验14", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_15("运营实验15", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  OPERATION_EXPERIMENT_16("运营实验16", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  OPERATION_EXPERIMENT_17("运营实验17", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  OPERATION_EXPERIMENT_18("运营实验18", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  OPERATION_EXPERIMENT_19("运营实验19", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID)),
  OPERATION_EXPERIMENT_20("运营实验20", () -> {
    return ImmutableSet.of("A", "B", "C", "D");
  }, "A", ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),

  LIVING_INFO_ACTION_NUMBER("鉴权步骤活体检测动作数量",
      () -> ImmutableSet.of("1", "2", "3"),
      "3",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  TOTAL_CREDITS_QUOTA_DISPLAY("APP额度管理卡片总额度取值",
      () -> ImmutableSet.of(CreditQuotaTotalEnum.MAX.name(), CreditQuotaTotalEnum.LATEST.name()),
      CreditQuotaTotalEnum.LATEST.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  /**
   * result = false
   */
  @Deprecated
  IDN_LOAN_WITHDRAW_PAGE("印尼提现页面改版分流",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  OPERATION_MARKETING_GROUP("运营营销分组",
      () -> ImmutableSet.of(BooleanType.TRUE.charCode, BooleanType.FALSE.charCode),
      BooleanType.TRUE.charCode,
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  IDN_LOAN_AUTH_COMPLETE_AWARD("印尼借贷完件奖励分流",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  IND_RECALL_BY_DEVICE("印尼通过设备召回",
      () -> ImmutableSet.of("A", "B", "BLANK"),
      "BLANK",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IND_RECALL_BY_MEDIA_DEVICE("印尼通过渠道进行设备召回",
      () -> ImmutableSet.of("A", "B", "BLANK"),
      "BLANK",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  OPEN_LOGIN_PAGE_AFTER_LAUNCH("非首次打开app直接进入登录注册一体页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  SDK_PAGE_BACK_TO_REGISTER("选择理财借贷页返回进入登录注册一体页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  @Deprecated
  CREATE_ORDER_WITHOUT_OTP_CHECK("下单不需要验证码",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_ADSET_GOOGLE("信息流投放发奖试验Google：0727GG_WJJL",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_ADSET_TIKTOK("信息流投放发奖试验Tiktok：0727TT_WJJL",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_ADSET_FACEBOOK("信息流投放发奖试验Facebook：LR_AddCart",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_CHANNEL_OPPO("信息流投放发奖试验oppo渠道",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_CHANNEL_SHALL("信息流投放发奖试验shall渠道",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_CHANNEL_VIVO("信息流投放发奖试验vivo渠道",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_CHANNEL_GOOGLE("信息流投放发奖试验Google渠道",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_CHANNEL_TIKTOK("信息流投放发奖试验Tiktok渠道",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_CHANNEL_FACEBOOK("信息流投放发奖试验Facebook渠道",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_CAMPAIGN_XIAOMI("信息流投放发奖励实验xiaomi：yunduan",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  IDN_LOAN_AUTH_COMPLETE_AWARD_BY_CHANNEL_SAMSUNG("信息流投放发奖励实验Samsung",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  USER_BIND_BANK_CARD_FILTER("绑定银行卡过滤渠道",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),
  @Deprecated
  USER_IDN_BIND_BANK_CARD_NEW_PAGE("印尼用户是否进入新绑卡页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),
  BIND_BANK_ACCOUNT_NEW_PAGE("印尼新绑卡页实验",
      () -> ImmutableSet.of("A", "B", "C"),
      "B",
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),
  IDN_H5_AFTER_REGISTER("H5注册后分流",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  USER_VERIFICATION_DISPLAY_CONFIG("用户注册登录页、下单页默认短信/whatsapp方式，以及是否弹窗提示",
      () -> ImmutableSet.of(
          VerificationDisplayConfigType.SMS_WITHOUT_DIALOG.name(),
          VerificationDisplayConfigType.WHATSAPP_WITHOUT_DIALOG.name(),
          VerificationDisplayConfigType.SMS_WITH_DIALOG.name(),
          VerificationDisplayConfigType.WHATSAPP_WITH_DIALOG.name()
      ),
      VerificationDisplayConfigType.SMS_WITHOUT_DIALOG.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  USER_VERIFICATION_SMART_DISPLAY_CONFIG("智能选取验证码发送渠道实验",
      () -> ImmutableSet.of(
          VerificationDisplayConfigType.SMS_WITH_DIALOG.name(),
          VerificationDisplayConfigType.WHATSAPP_WITH_DIALOG.name(),
          VerificationDisplayConfigType.SMART_WITH_DIALOG.name()
      ),
      VerificationDisplayConfigType.WHATSAPP_WITH_DIALOG.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  @Deprecated
  CREATE_ORDER_NEED_POP_UP_WHEN_NO_OTP("下单页不走OTP流程时是否需要弹窗(34000版本及其以上)",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  SHOW_VIRTUAL_INCREASE_CREDITS_CONTENT("展示虚拟提额",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  FEE_DETAIL_DISPLAY_COMPLIANCE_OPTIMIZATION_WITH_PRE_INTEREST("息费展示合规优化 - 有砍头息",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  FEE_DETAIL_DISPLAY_COMPLIANCE_OPTIMIZATION_WITHOUT_PRE_INTEREST("息费展示合规优化 - 无砍头息",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  LOW_INTEREST_FEE_DETAIL_DISPLAY_COMPLIANCE_OPTIMIZATION_WITH_PRE_INTEREST("息费展示合规优化 - 有砍头息(低息用户)",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  LOW_INTEREST_FEE_DETAIL_DISPLAY_COMPLIANCE_OPTIMIZATION_WITHOUT_PRE_INTEREST("息费展示合规优化 - 无砍头息(低息用户)",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  CREATE_ORDER_LOAN_CARD_FEE_DISPLAY_EXPERIMENT("下单页借款信息费用外显及利息拆分实验",
      () -> ImmutableSet.of(ExperimentDisplayStrategy.FEE_COMPARE_GROUP.name(),
          ExperimentDisplayStrategy.POST_INTEREST_FEE_GROUP.name(),
          ExperimentDisplayStrategy.FAKE_INTEREST_AND_TECH_FEE_GROUP.name(),
          ExperimentDisplayStrategy.FAKE_INTEREST_AND_TECH_FEE_EXCHANGE_GROUP.name(),
          ExperimentDisplayStrategy.SPLIT_INTEREST_AND_TECH_FEE_BASED_ON_POST_INTEREST_PERCENTAGE.name(),
          ExperimentDisplayStrategy.HIDE_TOTAL_REPAYMENT.name()),
      ExperimentDisplayStrategy.FEE_COMPARE_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  CREATE_NO_INTEREST_AMOUNT_DISPLAY_EXPERIMENT("无砍头息分流展示实验",
      () -> ImmutableSet.of(ExperimentDisplayStrategy.HIDE_INTEREST_AMOUNT_AND_TOTAL_REPAYMENT_GROUP.name(),
          ExperimentDisplayStrategy.SHOW_WAIVED_INTEREST_AMOUNT_AND_HIDE_TOTAL_AMOUNT_GROUP.name(),
          ExperimentDisplayStrategy.RECEIVE_PROCESS_FEE_GROUP.name()),
      ExperimentDisplayStrategy.RECEIVE_PROCESS_FEE_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  CREATE_ORDER_LOAN_CARD_RECEIVE_DISPLAY_AB("下单页借款信息到账金额样式AB分流(已失效)",
      () -> ImmutableSet.of(ExperimentDisplayStrategy.NO_RECEIVE_GROUP.name(),
          ExperimentDisplayStrategy.RECEIVE_PROCESS_FEE_GROUP.name()),
      ExperimentDisplayStrategy.NO_RECEIVE_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  REJECTED_USER_INCREASE_CREDIT_REVIEW("授信被拒用户增信重审实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  REJECTED_USER_HOME_DISPLAY_CONTENT(
      "授信被拒用户首页展示文案",
      () -> ImmutableSet.of(HomeDisplayStrategy.A.name(), HomeDisplayStrategy.B.name()),
      HomeDisplayStrategy.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH
  ),

  PASSED_QUITE_PERIOD_USER_CAN_REAPPLY(
      "已度过静默期用户增信重审实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH
  ),

  INCREASE_REAPPLY_EXTRA_INFO_TYPE_LIST(
      "增信重审列表实验",
      () -> ImmutableSet.of(HomeDisplayStrategy.A1.name(), HomeDisplayStrategy.A2.name()),
      HomeDisplayStrategy.A1.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH
  ),

  @Deprecated
  USE_RISK_MULTIPLE_USE_INTERFACE("【废弃】调用风控空头多头结果接口",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  SHORT_USER_HIT_PROCESS("空头用户进入极简流程", () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  LONG_USE_HIT_PROCESS("多头用户进入极简流程", () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  MIGRATION_AUTH_UNCOMPLETED("用户迁移-注册未完件",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.NORMALIZED_MOBILE_NUMBER)),

  @Deprecated
  MIGRATION_FIRST_REJECTED("用户迁移-首贷被拒",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.NORMALIZED_MOBILE_NUMBER)),

  @Deprecated
  MIGRATION_FIRST_ACCEPT_BUT_NO_ORDER("用户迁移-首贷授信未借款",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.NORMALIZED_MOBILE_NUMBER)),

  @Deprecated
  MIGRATION_COMPLETED_NO_RELOAN("用户迁移-结清未续借",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.NORMALIZED_MOBILE_NUMBER)),

  MIGRATION_RELOAN_REJECT("用户迁移-复贷拒绝",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.NORMALIZED_MOBILE_NUMBER)),

  @Deprecated
  MINIMALIST_PROCESS_SHOW_FILL_AUTH_STEP("【废弃】极简流程提交风控之前，展示补充其他鉴权步骤",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  IDN_LOAN_QUOTA_CENTER("印尼额度中心",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  @Deprecated
  //实验结果为V3
  IDN_LOAN_AUTH_LIVING_INFO_GENERAL_VERSION("Indonesia Auth Living Display For General Version",
      () -> ImmutableSet.of(NormalAuthLivingInfoPageDisplayType.V3.name(), NormalAuthLivingInfoPageDisplayType.V2.name()),
      NormalAuthLivingInfoPageDisplayType.V3.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  IDN_DANA_EWALLET_REPAYMENT("Repayment EWallet Indonesia - Dana",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  CHECK_EWALLET_WHEN_CREATE_ORDER("下单时进行电子钱包拦截",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),

  H5_LOAN_BIND_CARD("H5下单跳转绑卡",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  @Deprecated
  IDN_GOPAY_EWALLET_PAYOUT("Payout EWallet Indonesia - Gopay",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  IDN_LINKAJA_EWALLET_PAYOUT("Payout EWallet Indonesia - Linkaja",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  IDN_EWALLET_BIND_DISPLAY_FILTER("绑卡电子钱包展示过滤",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  WORK_INFORMATION_DISPLAY_INCOME_SOURCE("Work Information Display Income Source",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  SHOW_PRODUCT_BY_CREDITS_OR_TOTAL_CREDITS_FOR_47("userType是47的用户是否根据用户固定额度还是可借额度来分流",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  SHOW_PRODUCT_BY_CREDITS_OR_TOTAL_CREDITS_FOR_62("userType是62的用户是否根据用户固定额度还是可借额度来分流",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  BORROWER_CUSTOMER_SERVICE_CENTER_PAGE_URL("Customer Service Center Page Url for Borrower",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  LENDER_CUSTOMER_SERVICE_CENTER_PAGE_URL("Customer Service Center Page Url for Lender",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  @Deprecated
  IDENTITY_SELECTION_HOME_PAGE("Home Page Identity Selection",
      () -> ImmutableSet.of(
          IdentitySelectionType.NORMAL.name(),
          IdentitySelectionType.WITH_IDENTITY_SELECTION.name(),
          IdentitySelectionType.WITHOUT_IDENTITY_SELECTION.name()),
      IdentitySelectionType.NORMAL.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  @Deprecated
  IDN_SUBMIT_KTP_DISPLAY_TYPE("提交ktp页面样式",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  IDN_SUBMIT_KTP_DISPLAY_TYPE_V2("提交ktp页面样式V2",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  SHOW_INCREASE_CREDITS_ENTRANCE("展示增信提额入口",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  SHOW_INCREASE_CREDITS_COMPETITOR("【废弃】增信提额展示竞品额度",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  SHOW_AUTH_TONG_DUN("完件流程是否展示同盾认证页面",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  SHOW_AUTH_EMPLOYMENT_WORK_CARD("完件流程工作信息页面是否展示工卡",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  CUSTOM_PAGE_AFTER_USER_REGISTER("注册/登录后直接接入鉴权",
      () -> ImmutableSet.of("AUTH", "HOME"),
      "HOME",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  //已下线
  @Deprecated
  ORDER_FEE_HIDE_DECAPITATE_DISPLAY_STRATEGY("Order Fee Hide Decapitate Display Strategy",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  NEW_COUPON_DISPLAY("下单页优惠信息优化展示",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  SHOW_VIRTUAL_INCREASE_CREDITS_CONTENT_V2("展示虚拟提额--v2版本",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  VERIFY_LIVENESS_METHOD_IOS("IOS 活体识别渠道分流【借款端-鉴权】",
      () -> ImmutableSet.of(ADVANCE.name(), FACEPP.name(), FACEPP_V5.name()),
      FACEPP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  VERIFY_LIVENESS_METHOD_ANDROID("ANDROID 活体识别渠道分流【借款端-鉴权】",
      () -> ImmutableSet.of(ADVANCE.name(), FACEPP.name(), FACEPP_V5.name()),
      FACEPP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  VERIFY_LIVING_METHOD_H5("借贷H5鉴权活体采集分流",
      () -> ImmutableSet.of(ADVANCE_H5.name(), TENCENT_H5.name()),
      ADVANCE_H5.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  VERIFY_LIVING_SOURCE_IOS("IOS 人脸比对渠道分流【借款端-非鉴权】",
      () -> ImmutableSet.of(ADVANCE.name(), FACEPP.name(), FACEPP_V5.name()),
      FACEPP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  VERIFY_LIVING_SOURCE_ANDROID("ANDROID 人脸比对渠道分流【借款端-非鉴权】",
      () -> ImmutableSet.of(ADVANCE.name(), FACEPP.name(), FACEPP_V5.name()),
      FACEPP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  FIN_LIVING_SOURCE_IOS("IOS 活体渠道分流【理财端】",
      () -> ImmutableSet.of(FACEPP.name(), FACEPP_V5.name()),
      FACEPP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  COLLECT_IOS_AUTH_MEDIA_SOURCE("IOS 是否展示渠道来源配置",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  FIN_LIVING_SOURCE_ANDROID("ANDROID 活体渠道分流【理财端】",
      () -> ImmutableSet.of(FACEPP.name(), FACEPP_V5.name()),
      FACEPP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  SHOW_INCREASE_VOLUME_NOTIFICATION_IN_FACE_VERIFICATION_PAGE("在人脸验证页面显示增加音量通知",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  USER_NEW_PICKER_COMPONENT("使用新的picker组件",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  ANDROID_AUTO_SELECT_PHONE_NUMBER("安卓自动选择手机号",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  ANDROID_AUTO_SELECT_PHONE_NUMBER_V2("安卓自动选择手机号_V2",
      () -> ImmutableSet.of("AUTO", "CLICK", "NONE"),
      "NONE",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  @Deprecated
  ANDROID_AUTO_INPUT_CODE("安卓自动填充验证码--废弃",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  FACEPP_V2_SENSOR_CLOSE("faceppV2版本是否去掉陀螺仪检测",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  NEW_COUPON_DISPLAY_TIPS("优惠券展示tips",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  ORDER_PAGE_INPUT_AMOUNT_AND_TIP("新下单页：输入框+额度提示",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  ORDER_PAGE_TEMP_AMOUNT_TIP("临时额度提示强化--废弃",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  AUTO_TAKE_KTP_TIME("获取KTP等待时间，-1代表手动",
      () -> ImmutableSet.of("-1", "10", "20"),
      "20",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  FIRST_LOAN_CREDIT_INVALID_RISK("首贷额度失效触发风控",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  RELOAN_CREDIT_INVALID_RISK("复贷额度失效触发风控",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  SHRINK_UNCALC_CREDIT_RISK("缩库未测额触发风控",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  MINIMALIST_LOW_LEVEL_USER_CREDIT_TIP("极简低等级用户展示额度页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  MINIMALIST_LOW_LEVEL_USER_NEW_UI("【废弃】极简低等级用户展示补充信息页",
      () -> ImmutableSet.of("A1", "A2", "A3"),
      "A1",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  ORDER_PAGE_USER_IDLE_POPUP("订单页用户空闲弹窗(优惠券)",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  ORDER_PAGE_USER_IDLE_POPUP_V2("订单页用户空闲弹窗V2(利率优惠或优惠券)",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  CHANGE_AUTH_STEP_FOR_AFTER_REGISTER_LONG_TIME_NOT_START_AUTH("注册以后长时间没有开始提交鉴权",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  CHANGE_AUTH_STEP_FOR_AFTER_IDENTITY_LONG_TIME_NOT_START_AUTH("提交ktp以后长时间没有走下一步",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  CHANGE_AUTH_STEP_FOR_AFTER_LIVING_LONG_TIME_NOT_START_AUTH("活体检测以后长时间没有走下一步",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  AUTH_COLLECT_EMAIL("鉴权收集邮箱",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  APPLIST_PER_GET("applist前置获取实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  ORDER_PAGE_AMOUNT_RECEIVED_STYLE("首贷指定用户类型下单页借款信息到账金额样式", () -> ImmutableSet.of(ExperimentDisplayStrategy.NO_RECEIVE_GROUP.name(),
      ExperimentDisplayStrategy.RECEIVE_PROCESS_FEE_GROUP.name()),
      ExperimentDisplayStrategy.NO_RECEIVE_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  LIVING_IGNORE_PHONE_VERTICAL("活体识别忽略手机竖直实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  H5_AUTH_STEP("H5半流程鉴权步骤实验,A为对照组，B是实验组1，C是实验组2",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  LOGIN_DOWNLOAD_BUTTON("登录&下载按钮二合一",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  IDN_DANA_EWALLET_REPAYMENT_METHOD("Dana Repayment Method ABTest",
      () -> ImmutableSet.of("DYNAMIC", "STATIC_VA", "NONE"),
      "NONE",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  BIOMETRIC_CREDENTIAL_LOGIN("生物认证凭证登录",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),

  @Deprecated
  SHOW_DIRECT_DEBIT_ENTRANCE("是否展示代扣入口",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),

  SHOW_DIRECT_DEBIT_LINK_ACCOUNT_ENTRANCE("是否展示代扣绑定账户入口",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),

  SHOW_DIRECT_DEBIT_REPAY_ENTRANCE("是否可以在app发起代扣还款",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),

  @Deprecated
  USER_REGISTER_LOGIN_PAGE_V2("用户注册登录页V2",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)
  ),

  @Deprecated
  SHOW_BIRTH_DATE_FIELD_IN_OCR_RESULT("Showing Birth Date Field in OCR Result",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),
  @Deprecated
  CREDITS_DECREASE_QUICK_ORDER("降额快速下单实验，A(对照组), B(实验组1)，C(实验组二)",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",

      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),
  USE_NEW_COMPANY_PHONE_CONTENT_IN_AUTH_STEP("APP常规完件流程是否使用新的公司电话文案",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),

      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),
  LOAN_ACTIVITY_ORDER("活动占坑订单分流", () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  APP_NETWORK_FALLBACKDNSIP("app网络优化--DNS兜底方案", () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  APP_NETWORK_PREPARSEDNS("app网络优化--DNS预解析和定时刷新", () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  /**
   * 全量放开app版本需要控制--35613 版本可能会弹优惠券金额是0
   */
  ORDER_PAGE_BACK_BUTTON("订单页返回按钮实验", () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  LOAN_VIP("借贷vip", () -> ImmutableSet.of(LoanVipStrategy.NOT_BECOME.name(),
      LoanVipStrategy.CLICK_BECOME.name(),
      LoanVipStrategy.AUTO_BECOME.name()),
      LoanVipStrategy.NOT_BECOME.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  RE_LOAN_VIP("借贷vip--复贷", () -> ImmutableSet.of(LoanVipStrategy.NOT_BECOME.name(),
      LoanVipStrategy.CLICK_BECOME.name(),
      LoanVipStrategy.AUTO_BECOME.name()),
      LoanVipStrategy.NOT_BECOME.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY("优惠模块，头图，vip皮肤展示分流",
      () -> {
        return Arrays.stream(DiscountDetailTopAreaVipUIDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      DiscountDetailTopAreaVipUIDisplayStrategy.CONTROL_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),

  @Deprecated
  REDUCE_INTEREST_RATE_PERCEPTION_FOR_MONTH_RATIO("降息感知--月息文案", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  ORDER_PAGE_GET_HIGHER_LIMIT("点击获取更高额度",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  REDUCE_INTEREST_RATE_PERCEPTION_FOR_MONTH_RATIO_MULTI("降息感知--月息文案(for multi loan)", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  ANDROID_AUTO_INPUT_WA_CODE("安卓自动填充wa验证码",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  REDUCE_INTEREST_RATE_BLANK_TEST_FOR_LOAN("产运留白实验-首贷【降息相关实验】", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  REDUCE_INTEREST_RATE_BLANK_TEST_FOR_RE_LOAN("产运留白实验-复贷【降息相关实验】", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  KTP_STAGE_PRE_ASK_CAMERA_PERMISSION("在ktp阶段预先询问相机权限弹出窗口",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  @Deprecated
  TEMP_CREDITS_LIMIT_USE_ADMIN_CONFIG("【废弃】临时额度上限使用Admin配置",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  COUPON_CREDITS_LIMIT_USE_ADMIN_CONFIG("【废弃】运营额度上限使用Admin配置",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  /**
   * A:手动拍摄
   * B:自动识别10s后切换手动拍摄
   * C:手自一体拍摄
   */
  KTP_PHOTO_TYPE("KTP手动自动一体拍摄",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  NEW_HOME_PAGE_UI("新首页--首页UI实验", () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  NEW_HOME_PAGE_FLOATING_ICON("新首页--首页挂件曝光实验", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  NEW_HOME_PAGE_REDUCE_INTEREST_RATE_POPUP_POSITION("新首页--降息感知弹窗曝光位置", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  NEW_HOME_PAGE_TOP_AREA("新首页--头图展示", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  H5_USER_OBTAIN_AUTHORIZATION_BEFORE_REGISTRATION("H5用户在注册前获取授权",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  @Deprecated
  AUTO_CALC_CREDIT("Auto Calculation Credit after order completed, True: T+1, False: T+0",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  LOAN_USER_CREATE_ORDER_WHEN_CREDITS_ACCEPT("用户授信通过即下单",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  LOAN_USER_LOAN_INTENTION(
      "用户借款意愿展示实验",
      () -> ImmutableSet.of(
          LoanIntentionDisplayStrategy.A0.name(),
          LoanIntentionDisplayStrategy.A1.name(),
          LoanIntentionDisplayStrategy.A2.name(),
          LoanIntentionDisplayStrategy.A3.name(),
          LoanIntentionDisplayStrategy.A4.name(),
          LoanIntentionDisplayStrategy.A5.name()
      ),
      LoanIntentionDisplayStrategy.A0.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),

  APP_RESOURCE_TEST("app资源测试", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  BORROW_WARNING_DISPLAY_CONFIG("Whether APP/IOS displays Borrow Warning Text",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  ORDER_PAGE_BACK_BUTTON_POPUP("订单页返回按钮弹窗",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  NEW_UI_CREATE_ORDER_PAGE_BUTTON_TEXT("新UI下单页按钮文案",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  ORDER_PAGE_USER_IDLE_POPUP_V3("订单页用户空闲弹窗V3(利率优惠或优惠券)",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),

  CREATE_PAGE_CREDITS_AREA("可下单状态额度区域样式实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),

  NO_POP_UP_WINDOW_AUTH_PAGE_CLICK_BACK("Pop Up Window during Auth Page Click Back",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  NO_PRE_INTEREST_PRODUCT_DETAIL("No Pre Interest Product Detail Test",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),

  @Deprecated
  APP_WEB_RESOURCE_PRE_LOAD("app web资源预加载",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  PERSONAL_CENTER_SHOW_AVAILABLE_LIMIT("Showing available limit for personal center",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  LOAN_USER_SUPPLEMENT_BEFORE_CREATE_ORDER("下单前补件",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  MINIMALIST_INCREASE_CREDIT_RISK(
      "极简用户提额风控实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),
  USER_ENTER_REVOLVING_CREDITS(
      "用户是否进入循环额度流程",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),
  @Deprecated
  MULTI_LOAN_NO_REPAY_CALC_CREDIT(
      "复贷续借用户还款后不触发测额",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),
  IP_DIRECT_CONNECTION_IOS_CONFIG(
      "IOS Whether to use the direct configuration of the IP",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)
  ),
  CHECK_USER_INPUT_ADDRESS(
      "Check user input address",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH
  ),
  DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_V2("优惠模块，头图，vip皮肤展示分流V2",
      () -> Arrays.stream(DiscountDetailTopAreaVipUIDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet()),
      DiscountDetailTopAreaVipUIDisplayStrategy.ENHANCE_DISCOUNT_REMOVE_TOP_AREA_AND_VIP_UI.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  LOAN_TERM_PERIOD_DISPLAY_STRATEGY(
      "Loan Term Period Display Strategy",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),
  CUT_INTEREST_POP_UP("降息感知弹窗实验",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "B",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  USE_H5_PRE_LOAN("使用H5承接授信即放款页面",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  POP_UP_TYPE_AB("弹窗展示方式实验",
      () -> ImmutableSet.of("Native", "Web"),
      "Native", ImmutableSet.of(DiversionKeyType.USER_ID)),
  POP_UP_TYPE_AB_HTML("弹窗展示方式实验V2",
      () -> ImmutableSet.of("Native", "Web"),
      "Native", ImmutableSet.of(DiversionKeyType.USER_ID)),
  VERSION_CONFIG_NOTIF("强更通知ab实验",
      () -> ImmutableSet.of(CheckUpdateSceneType.FORCE_UPDATE.name(), CheckUpdateSceneType.UPDATE_NOTICE.name(), CheckUpdateSceneType.NONE.name()),
      VersionConfigType.FORCE_UPDATE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  VERSION_CONFIG_VERSION_INFO_DESC("版本更新通知 - 文案ab实验",
      () -> {
        return ImmutableSet.of("A", "B", "C", "D", "E", "F", "G");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  H5_HEAD_IMAGE_TEST("落地页头图实验",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "A",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  COUPON_STYLE_AB("362优惠券样式ab", () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  H5_REGISTER_OR_LOGIN_PAGE_VERIFICATION_TYPE("H5注册登录页发送验证码类型",
      () -> ImmutableSet.of("SMS", "WHATSAPP"),
      "SMS",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  WEB_COUPON_PAGE("web优惠券页",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  AMOUNT_SLIDE_BAR("下单页滑杆实验",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  ORDER_PLAN_STYLE_FOR_363("363版本还款计划实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  CREATE_ORDER_SIGN_FORMAT_DIVISION("下单页签名流程优化实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  RELOAN_REJECTED_BUBBLE_GUIDE("363版本用户处于续借不可借状态气泡提示",
      () -> ImmutableSet.of("A", "B"),
      "B",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  RESERVE_LOAN_STYLE("预约借款模块分流实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  PRIORITIZE_FAQ_OR_USE_EXISTING("prioritize using faq or use existing",
      () -> ImmutableSet.of("EXPERIMENT", "NON_EXPERIMENT"),
      "NON_EXPERIMENT",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  @Deprecated
  APP_MAX_FONT_SCALE("app大字体实验",
      () -> ImmutableSet.of("1.0", "1.1", "2.0"),
      "2.0",
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  AUTH_LIVING_ADDRESS_CHECK("鉴权居住地check",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  AUTH_LIVING_ADDRESS_RETURN("鉴权居住地址返显",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  AUTH_MOM_NAME_CHECK("鉴权母亲姓名check",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  LOGIN_SECURE_FACEID_SHOW_BACK("活体校验（登录）展示返回键",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  AUTH_BIRTHDAY_INFO_RETURN("鉴权生日信息返显",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  SHOW_SCREENSHOT_REMIND_POP_UP("展示截屏提醒弹窗",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  @Deprecated
  DEFAULT_USER_PRODUCT("默认用户产品",
      () -> ImmutableSet.of(UserProductAbResultEnum.PRODUCT_FROM_RISK.name(), UserProductAbResultEnum.PRODUCT_FROM_MAX_DAYS.name()),
      UserProductAbResultEnum.PRODUCT_FROM_RISK.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  COUPON_INVALID_STYLE("优惠券失效",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  ORDER_PAGE_FLOAT("下单页展示挂件",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  REJECT_HOME_PAGE_STYLE("授信被拒首页样式分流",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  REAPPLY_HOME_PAGE_STYLE("增信重审首页样式分流",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  REPAYMENT_HOME_PAGE_STYLE("待还款状态首页样式",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  AUTH_WEB_CASH_LOAN_EMPLOYMENT_INFO_NEW("h5鉴权工作信息",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  MOTHER_LAST_NAME_CHECK_POPUP("母亲姓氏校验弹窗",
      () -> ImmutableSet.of(PopupFrequencyTypeEnum.ONCE_A_DAY.name(), PopupFrequencyTypeEnum.NONE.name()),
      PopupFrequencyTypeEnum.NONE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  SKIP_GUIDE_PAGE("跳过引导页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),

  LIVING_FAILED_TRY_ALL_PROCESS_AGAIN("活体失败重新走活体流程",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  RETRIEVAL_RISK_SUBMIT("回捞一次风控触发流程",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  KTP_PHOTO_FLOATING_LAYER("h5 KTP拍照浮层",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  H5_KTP_PAGE_UPGRADE("h5 KTP页面简化及照片格式优化",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.B.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  AUTH_WEB_KTP("h5鉴权ktp",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  @Deprecated
  LOGIN_INPUT_DISPLAY_TYPE("借贷注册登录半蒙层优化",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  ORDER_LIMIT_CONFIRM("在待三家确认实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  FINANCING_INSTALMENT_MATCH_MODE("自动债匹新旧模式",
      () -> {
        return Arrays.stream(FinancingInstalmentMatchMode.values()).map(Enum::name).collect(Collectors.toSet());
      },
      FinancingInstalmentMatchMode.AUTO_INSTALMENT_MATCH.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  UNSUPPORTED_BANK("不支持的银行卡实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  NEW_HOME_PAGE_366_STYLE("新首页366样式",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  @Deprecated
  H5_LANDING_PAGE_ACCURATE_LOGIN("H5落地页精准登录",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.DEVICE_TOKEN)),
  H5_KTP_KEEP_FLOW("H5 KTP保留进度",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.TRUE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  H5_NIK_POSITION_CHECK("H5 Nik位置校验",
      () -> ImmutableSet.of("A", "B"),
      "B",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  @Deprecated
  ORDER_PAGE_COUPON_PROMPT("367下单页优惠券提示",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER, true),
  OPERATOR_CONFIG_INCREASE_CREDITS("增信提额三方信息",
      () -> {
        return ImmutableSet.of("NONE", "JI_TUI", "LING_FEI_DIAN");
      },
      "NONE",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  KTP_SELECT_PHOTO_BUTTON_DISPLAY("ktp照片从相册获取",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  REJECT_LOAN_MARKET_INFO_NOT_INCREASE("被拒贷超实验--不能增信重申",
      () -> {
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  REJECT_LOAN_MARKET_INFO_CAN_INCREASE("被拒贷超实验--能增信重申",
      () -> {
        return ImmutableSet.of("A", "B");
      },
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),
  NEW_HOME_REVIEW_PAGE_JUMP_TO_ORDER_PAGE("新首页审核页跳转下单页",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  REPAYMENT_HINT("还款VA页顶部提醒文案",
      () -> ImmutableSet.of("A", "B"),
      "B",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),

  @Deprecated
  REPAYMENT_DISPLAY_DETAILS(
      "还款页计费说明展示实验",
      () -> ImmutableSet.of(HomeDisplayStrategy.A.name(), HomeDisplayStrategy.B.name()),
      HomeDisplayStrategy.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),
  @Deprecated
  ORDER_GUIDE_ANIMATION_367("367版本下单引导动画实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  ORDER_GUIDE_ANIMATION_368("368版本下单引导动画实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  PRODUCT_STYLE_FOR_367("367产品选择页样式",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),

  KTP_FINISHED_ACTIVITY_AB("ktp挽留活动",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_ACTIVITY),

  REVOLVING_LOAN_HOME(
      "循环贷首页样式",
      () -> ImmutableSet.of(HomeDisplayStrategy.A.name(), HomeDisplayStrategy.B.name()),
      HomeDisplayStrategy.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),

  KTP_MARKETING_POPUP_DISPLAY("ktp环节营销奖励活动",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),

      OPERATION_MARKETING_GROUP_2024H2_ACTIVITY,
      true),

  CREATE_ORDER_INCENTIVE_ACTIVITY_AB_H5("下单激励活动AB实验H5",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),

      OPERATION_MARKETING_GROUP_2024H2_ACTIVITY,
      true
  ),

  @Deprecated
  CREATE_ORDER_INCENTIVE_ACTIVITY_AB_V2("下单激励活动AB实验H5分流V2",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),

      OPERATION_MARKETING_GROUP_2024H2_ACTIVITY,
      true
  ),

  CREATE_ORDER_INCENTIVE_ACTIVITY_AB_NATIVE("下单激励活动AB实验NATIVE",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),

      OPERATION_MARKETING_GROUP_2024H2_ACTIVITY,
      true
  ),

  AUTH_H5_POPUP_TEST("鉴权h5弹窗",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH),

  JBP_BLACK_CARD_AB("黑卡demo ab",
      () -> ImmutableSet.of("A", "B", "C"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER),
  PRODUCT_SHOW_BUBBLE_CONTENT_FOR_369("369版本是否展示下单页产品引导气泡",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  @Deprecated
  H5_CAN_CREATE_ORDER_PAGE("native迁移H5可下单页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  @Deprecated
  WEB_AUTH_BIND_BANK_CARD("native迁移H5鉴权绑卡页",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH,
      true),

  CREATE_ORDER_CHECK_SWITCH("新老下单前置校验接口",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER, true),

  @Deprecated
  IMMEDIATE_CONTACTINFO_BEFORE_CREATE_ORDER("下单前置校验之前填写紧急联系人",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER, true),

  @Deprecated
  IMMEDIATE_CONTACTINFO_WHEN_CREATE_ORDER("下单校验填写紧急联系人",
      () -> {
        return Arrays.stream(CollectInformationDuringCreateOrderABTestResultGroup.values()).map(Enum::name).collect(Collectors.toSet());
      },
      CollectInformationDuringCreateOrderABTestResultGroup.NOT_COLLECT.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER, true),

  @Deprecated
  IMMEDIATE_CONTACTINFO_NOT_CREATE_ORDER("在贷不能下单填写紧急联系人",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER, true),

  RISK_CHECKING_IMMEDIATE_CONTACT_BEFORE_CREATE_ORDER("Risk Checking Immediate Contact before create order",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name(), CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER, true),

  RISK_CHECKING_IMMEDIATE_CONTACT_NOT_CREATE_ORDER("Risk Checking Immediate Contact not create order",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(), CommonABTestResultGroup.D.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER, true),

  ORDER_PAGE_QUICK_ORDER_BG("下单页快速下单半蒙层实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  ORDER_PAGE_QUICK_ORDER_BG_NATIVE("下单页快速下单半蒙层实验-native",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),

  @Deprecated
  LOAN_USER_LOAN_INTENTION_V3("授信即下单新开实验V3",
      () -> ImmutableSet.of(
          LoanIntentionDisplayStrategy.A0.name(),
          LoanIntentionDisplayStrategy.A2.name(),
          LoanIntentionDisplayStrategy.A3.name(),
          LoanIntentionDisplayStrategy.A4.name()
      ),
      LoanIntentionDisplayStrategy.A0.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH,
      true),

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
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH,
      true),
  @Deprecated
  AUTH_WEB_CONTACT_INFO("h5迁移鉴权紧急联系人",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH,
      true),
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
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH,
      true
  ),
  CAN_ORDER_PAGE_V370_DISCOUNT_STYLE("370版本下单页折扣优惠样式",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  XENDIT_DIRECT_DEBT("xendit api转化实验",
      () -> {
        return Arrays.stream(DirectDebtDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      DirectDebtDisplayStrategy.NONE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  AYO_CONNECT_DIRECT_DEBT("AyoConnect api转化实验",
      () -> {
        return Arrays.stream(DirectDebtDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      DirectDebtDisplayStrategy.NONE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  BCA_DIRECT_DEBT("BCA api转化实验",
      () -> {
        return Arrays.stream(DirectDebtDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      DirectDebtDisplayStrategy.NONE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  GOPAY_DIRECT_DEBIT_NORMAL("gopay代扣未逾期 api转化实验",
      () -> {
        return Arrays.stream(GoPayDirectDebitStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      GoPayDirectDebitStrategy.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  GOPAY_DIRECT_DEBIT_OVERDUE("gopay代扣逾期 api转化实验",
      () -> {
        return Arrays.stream(GoPayDirectDebitStrategy.values()).map(Enum::name).collect(Collectors.toSet());
      },
      GoPayDirectDebitStrategy.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  DISCOUNT_DETAIL_TOP_AREA_VIP_UI_DISPLAY_STRATEGY_V3("优惠模块，头图，vip皮肤展示分流V3",
      () -> Arrays.stream(DiscountDetailTopAreaVipUIDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet()),
      DiscountDetailTopAreaVipUIDisplayStrategy.CONTROL_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  LOAN_ORDER_PAGE_SHOW_TERMS("下单页借款期限展示&引导借长期",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  COUPON_LIST_STYLE_371("优惠券列表样式，371版本",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  @Deprecated
  HOME_API_OPTIMIZATION_TEST("首页API优化实验",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH,
      true),

  ORDER_PAGE_RETURN_BUTTON_QUICK_ORDER_POPUP_FOR_CASH("下单页返回按钮快速下单弹窗",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  ORDER_PAGE_RETURN_BUTTON_QUICK_ORDER_POPUP_FOR_CASH_NATIVE("下单页返回按钮快速下单弹窗",
      () -> ImmutableSet.of("A", "B", "C", "D"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  HOME_PAGE_POPUP_FOR_CASH("首页弹窗现金实验",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  SHOPEE_ONLINE_SHOPPING_INFO_AB("shopee电商信息实验",
      () -> {
        return ImmutableSet.of("NONE", "WE_BANK");
      },
      "NONE",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  USER_AUTHORIZATION_POPUP_WINDOW(
      "用户授权弹窗展示实验",
      () -> ImmutableSet.of(
          BooleanType.TRUE.name(),
          BooleanType.FALSE.name()
      ),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  NOT_SHOW_SPLASH_SCREEN(
      "不展示开屏页",
      () -> ImmutableSet.of(
          "A", "B"
      ),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2,
      true),
  RETURN_BUTTON_COUPON_POP_UP_EXPERIMENT(
      "挽留弹窗（有优惠券）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  RETURN_BUTTON_CREDIT_POP_UP_EXPERIMENT(
      "挽留弹窗（有临时额度）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  RETURN_BUTTON_NO_COUPON_CREDIT_AND_LIMIT_AUTH_POP_UP_EXPERIMENT(
      "挽留弹窗（无优惠券、临时额度且过件4天内）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  RETURN_BUTTON_NO_COUPON_CREDIT_AND_AUTH_LONG_POP_UP_EXPERIMENT(
      "挽留弹窗（无优惠券、临时额度且过件4天以上）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  QUICK_ORDER_LOAN_AMOUNT_EXPERIMENT(
      "快速下单额度测试实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  AUTO_JUMP_LEVEL2_FREQUENCY("首页自动跳转二级下单页频次限制",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  QUICK_ORDER_RELOAN_EXPERIMENT_H5(
      "快速下单-H5实验（有借款）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
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
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
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
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
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
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  QUICK_ORDER_H5_STYLE_EXPERIMENT(
      "快速下单1.0（H5版）样式测试",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),

  ACCEPT_BUT_CAN_NOT_LOAN("授信通过但不可借分流",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true),
  @Deprecated
  DIRECT_DEBIT_GUIDE_PAGE_EXPERIMENT("代扣引导页样式实验",
      () -> ImmutableSet.of(
          DirectDebitGuidePageStrategy.ORIGINAL_PAGE.name(),
          DirectDebitGuidePageStrategy.PAGE_A.name(),
          DirectDebitGuidePageStrategy.PAGE_B.name()),
      DirectDebitGuidePageStrategy.ORIGINAL_PAGE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  STANDARD_INTEREST_RATE_AB("标准利率",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true),
  RISK_BATCH_TASK_MULTI_LOAN_DIVERSION("风控续借跑批分流",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true),
  AUTH_NEED_POB("鉴权出生地填写分流",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_AUTH,
      true),
  ORDER_STEP_NEED_POB("下单前分流",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  UNION_REPAYMENT_EXPR("中收合并还款展示分流",
      () -> Arrays.stream(UnionRepaymentDisplayStrategy.values()).map(Enum::name).collect(Collectors.toSet()),
      UnionRepaymentDisplayStrategy.ORIGINAL_PAGE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true),
  QUICK_ORDER_OPTIONAL_LOAN_AMOUNT_EXPERIMENT(
      "快速下单额度可选择实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
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
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true
  ),
  VIRTUAL_CREDITS("降额虚拟额度分流",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name(), CommonABTestResultGroup.C.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true),
  HOMEPAGE_V3_AUTH_UNFINISHED_EXPERIMENT("未完件用户/状态的新老首页实验",
      () -> ImmutableSet.of("V1", "V2", "V3_INCENTIVE", "V3_PROGRESSBAR"),
      "V2",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true),
  HOMEPAGE_V3_AUTH_FINISHED_EXPERIMENT("已完件用户/状态的新老首页实验",
      () -> ImmutableSet.of("V1", "V2", "V3"),
      "V2",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true),
  AUTH_WEB_LIVING_INFO("鉴权web迁移",
      () -> ImmutableSet.of("TRUE", "FALSE"),
      "FALSE",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true),
  IDN_DANA_EWALLET_REPAYMENT_METHOD_V2("dana 还款方式的分流",
      () -> ImmutableSet.of("DYNAMIC", "STATIC_VA", "NONE"),
      "NONE",
      ImmutableSet.of(DiversionKeyType.USER_ID), null, true),
  HOME_V3_LOAN_COUPON("新首页优惠券样式",
      () -> ImmutableSet.of("A", "B"),
      "A",
      ImmutableSet.of(DiversionKeyType.USER_ID), null, true),
  QUICK_ORDER_CASH_ACTIVITY_EXPERIMENT(
      "快速下单3.0与现金奖励融合实验",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  @Deprecated
  ORDER_PAGE_NEW_STYLE_EXPERIMENT(
      "下单页新样式重构2.0实验",
      () -> ImmutableSet.of(
          "V1", "V2_AGREEMENT", "V2_NO_AGREEMENT"
      ),
      "V1",
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  @Deprecated
  QUICK_ORDER_REOPEN_EXPERIMENT(
      "一键借款实验策略调整",
      () -> Arrays.stream(QuickOrderResultGroup.values()).map(Enum::name).collect(Collectors.toSet()),
      QuickOrderResultGroup.CONTROL_GROUP.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER,
      true
  ),
  USER_ENTER_REVOLVING_CREDITS_V2(
      "用户是否进入循环额度流程-重开",
      () -> ImmutableSet.of(BooleanType.TRUE.name(), BooleanType.FALSE.name()),
      BooleanType.FALSE.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),
  USER_RECALL_TRIGGER_SECOND_RISK_ORDER(
      "回捞场景自动触发二次风控下单",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),
  BILL_PAGE_TO_H5_NOT_OVERDUE(
      "未逾期用户Bill页切H5实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  BILL_PAGE_TO_H5_OVERDUE(
      "逾期用户Bill页切H5实验",
      () -> ImmutableSet.of(CommonABTestResultGroup.A.name(), CommonABTestResultGroup.B.name()),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID)),
  USER_RECALL_TRIGGER_SECOND_RISK_ORDER_FOR_LOAN(
      "回捞场景自动触发二次风控下单（首贷场景）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),
  USER_RECALL_TRIGGER_SECOND_RISK_ORDER_FOR_RELOAN(
      "回捞场景自动触发二次风控下单（复贷场景）",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name(),
          CommonABTestResultGroup.C.name(),
          CommonABTestResultGroup.D.name(),
          CommonABTestResultGroup.E.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      OPERATION_MARKETING_GROUP_2024H2_CREATE_ORDER
  ),
  RISK_BATCH_RELOAN_FIRST_MULTI_LOAN_DIVERSION("风控复贷首续跑批分流",
      () -> ImmutableSet.of(
          CommonABTestResultGroup.A.name(),
          CommonABTestResultGroup.B.name()
      ),
      CommonABTestResultGroup.A.name(),
      ImmutableSet.of(DiversionKeyType.USER_ID),
      null,
      true),
  ;
  /*
  该方法已废弃，后续统一使用下面方法
  com.yqg.core.service.abtest.ExperimentPlatformClientService.fetchResultFallBackWithDefaultScene(com.yqg.core.service.abtest.vo.ABTestBaseRequestVO, java.lang.Long)
 */


  /**
   * 场景支持的选项
   */
  public final Supplier<Set<String>> options;
  /**
   * web展示用名称
   */
  public final String displayName;
  /**
   * 支持的diversionKeyTypes
   */
  public final Set<DiversionKeyType> supportKeyTypes;
  /**
   * 场景类型默认的场景，用于代码上线admin还没配置的情况
   */
  public final String defaultScene;

  /**
   * 是否为泳道
   */
  public final boolean isLane;

  /**
   * 泳道业务节点
   */
  public final ABTestLaneBusinessType laneBusinessType;

  /**
   * 关联子泳道
   */
  public final ABTestSceneType childLane;

  /**
   * 是否使用实验中台
   */
  public final boolean useExperimentPlatform;

  public static final String BLANK_SCENE = "BLANK_SCENE";

  ABTestSceneType(String displayName, Supplier<Set<String>> options, String defaultScene, Set<DiversionKeyType> supportKeyTypes) {
    this.displayName = displayName;
    this.options = options;
    this.supportKeyTypes = supportKeyTypes;
    this.defaultScene = defaultScene;
    this.isLane = false;
    this.laneBusinessType = null;
    this.childLane = null;
    this.useExperimentPlatform = false;
  }

  ABTestSceneType(String displayName, Supplier<Set<String>> options, String defaultScene, Set<DiversionKeyType> supportKeyTypes, ABTestSceneType childLane) {
    this.displayName = displayName;
    this.options = options;
    this.supportKeyTypes = supportKeyTypes;
    this.defaultScene = defaultScene;
    this.isLane = false;
    this.laneBusinessType = null;
    this.childLane = childLane;
    this.useExperimentPlatform = false;
  }

  ABTestSceneType(String displayName, Supplier<Set<String>> options, String defaultScene, Set<DiversionKeyType> supportKeyTypes, ABTestSceneType childLane, boolean useExperimentPlatform) {
    this.displayName = displayName;
    this.options = options;
    this.supportKeyTypes = supportKeyTypes;
    this.defaultScene = defaultScene;
    this.isLane = false;
    this.laneBusinessType = null;
    this.childLane = childLane;
    this.useExperimentPlatform = useExperimentPlatform;
  }

  ABTestSceneType(String displayName, Supplier<Set<String>> options, String defaultScene, Set<DiversionKeyType> supportKeyTypes, boolean isLane, ABTestLaneBusinessType laneBusinessType) {
    this.displayName = displayName;
    this.options = options;
    this.supportKeyTypes = supportKeyTypes;
    this.defaultScene = defaultScene;
    this.isLane = isLane;
    this.laneBusinessType = laneBusinessType;
    this.childLane = null;
    this.useExperimentPlatform = false;
  }

  public Boolean containsScene(String scene) {
    return getScenes().contains(scene);
  }

  public String getSceneOrThrow(String scene) {
    EcAsserts.assertTrue(getScenes().contains(scene), "default scene error, type : {}, scene : {}", this, scene);
    return scene;
  }

  public boolean isDeprecated() {
    return AnnotationsUtils.isDeprecatedEnum(this);
  }

  private static final Set<ABTestSceneType> forTestUseSceneTypes = ImmutableSet.of(DEMO, DEMO_2);

  public static boolean forTestUse(ABTestSceneType sceneType) {
    return forTestUseSceneTypes.contains(sceneType);
  }

  /**
   * 通用的运营场景
   */
  public static final List<ABTestSceneType> COMMON_OPERATION_EXPERIMENT = Lists.newArrayList(
      OPERATION_EXPERIMENT_1,
      OPERATION_EXPERIMENT_2,
      OPERATION_EXPERIMENT_3,
      OPERATION_EXPERIMENT_4,
      OPERATION_EXPERIMENT_5,
      OPERATION_EXPERIMENT_6,
      OPERATION_EXPERIMENT_7,
      OPERATION_EXPERIMENT_8,
      OPERATION_EXPERIMENT_9,
      OPERATION_EXPERIMENT_10,
      OPERATION_EXPERIMENT_11,
      OPERATION_EXPERIMENT_12,
      OPERATION_EXPERIMENT_13,
      OPERATION_EXPERIMENT_14,
      OPERATION_EXPERIMENT_15,
      OPERATION_EXPERIMENT_16,
      OPERATION_EXPERIMENT_17,
      OPERATION_EXPERIMENT_18,
      OPERATION_EXPERIMENT_19,
      OPERATION_EXPERIMENT_20
  );

  /**
   * 留白组集合
   */
  public static final List<ABTestSceneType> ALL_USER_DIVERSE_EXPERIMENT = Lists.newArrayList(
      OPERATION_MARKETING_GROUP_2024H2,
      OPERATION_MARKETING_GROUP
  );

  /**
   * 业务节点对应的子泳道
   */
  public static List<ABTestSceneType> getChildLaneListByBusinessType(ABTestLaneBusinessType businessType) {
    return Arrays
        .stream(ABTestSceneType.values())
        .filter(o -> {
          return o.isLane && o.laneBusinessType == businessType && o != OPERATION_MARKETING_GROUP_2024H2;
        })
        .collect(Collectors.toList());
  }

  @Override
  public String getDescription() {
    return displayName;
  }

  public Set<String> getScenes() {
    Set<String> set = new HashSet<>(options.get());
    set.add(BLANK_SCENE);
    return set;
  }
}
