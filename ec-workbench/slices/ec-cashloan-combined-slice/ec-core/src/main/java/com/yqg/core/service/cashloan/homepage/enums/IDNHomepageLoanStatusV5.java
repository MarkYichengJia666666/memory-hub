package com.yqg.core.service.cashloan.homepage.enums;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.yqg.common.spring.util.enums.DescriptionBaseEnum;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 首页 -> 续借、复贷、首贷
 * <p>
 * ⚠️⚠️⚠️
 * 这里添加枚举检查下com.yqg.core.service.cashloan.homepage.vo.HomePointHolder#getCanLoanStatus(com.yqg.core.service.cashloan.homepage.vo.HomePointHolder)是不是要适配
 */
public enum IDNHomepageLoanStatusV5 implements DescriptionBaseEnum {

  // 首贷
  ACCEPTED("首贷授信通过", IDNHomepageDisplayStatusV5.ENABLE_CREATE_ORDER),
  MINIMALIST_ACCEPT("极简授信通过", IDNHomepageDisplayStatusV5.MINIMALIST_ACCEPT),
  NEVER_APPLIED("首贷未授信", IDNHomepageDisplayStatusV5.NOT_APPLIED),
  /**
   * 此状态除了授信状态，还可能是order的Reserve状态（订单二次风控）
   */
  IN_REVIEW("首贷授信中", IDNHomepageDisplayStatusV5.REVIEW),
  MINIMALIST_IN_REVIEW("极简首贷授信中", IDNHomepageDisplayStatusV5.MINIMALIST_IN_REVIEW),
  IMAGE_REVIEW_REJECTED("人工审核被驳回", IDNHomepageDisplayStatusV5.IMAGE_REUPLOAD),
  REUPLOAD_FINISHED("驳回后重传完成", IDNHomepageDisplayStatusV5.REVIEW),
  REJECTED("首贷授信拒绝", IDNHomepageDisplayStatusV5.REJECTED),
  CANCELLED("首贷授信取消", IDNHomepageDisplayStatusV5.REJECTED),
  GRAB_CHECK("首贷抢单环节中", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  FUND_CHECK("首贷资方审核中", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  FUND_PAYING("首贷资方打款中", IDNHomepageDisplayStatusV5.PAYING),
  DEBT_CHECK("首贷债匹", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  ORDER_PRE_CHECK("首贷打款前置校验", IDNHomepageDisplayStatusV5.ORDER_PRE_CHECK),
  PAYING("首贷打款中", IDNHomepageDisplayStatusV5.PAYING),
  READY("首贷已打款", IDNHomepageDisplayStatusV5.REPAYMENT),
  OVERDUE("首贷已逾期", IDNHomepageDisplayStatusV5.REPAYMENT),
  CAN_REAPPLY_NOW("当前可重新提交授信", IDNHomepageDisplayStatusV5.ENABLE_REAPPLIED),
  LOAN_CREDITS_EXPIRED("首贷额度失效", IDNHomepageDisplayStatusV5.ENABLE_CALCULATE_CREDITS),
  LOAN_CREDITS_DECREASE("首贷风控额度下降", IDNHomepageDisplayStatusV5.ENABLE_CREATE_ORDER),
  @Deprecated
  LOAN_CREDITS_DECREASE_QUICK_ORDER("首贷降额快速下单", IDNHomepageDisplayStatusV5.CREDITS_DECREASE_QUICK_ORDER),

  // 复贷
  RELOAN_INIT("复贷未借款", IDNHomepageDisplayStatusV5.ENABLE_CREATE_ORDER),
  RELOAN_IN_REVIEW("复贷授信中", IDNHomepageDisplayStatusV5.REVIEW),
  RELOAN_CALC_CREDITS_IN_REVIEW("复贷额度测算中", IDNHomepageDisplayStatusV5.REVIEW),
  RELOAN_REJECTED("复贷授信拒绝", IDNHomepageDisplayStatusV5.REJECTED),
  RELOAN_FUND_CHECK("复贷资方审核中", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  RELOAN_GRAB_CHECK("复贷抢单环节中", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  RELOAN_FUND_PAYING("复贷资方打款中", IDNHomepageDisplayStatusV5.PAYING),
  RELOAN_DEBT_CHECK("复贷债匹", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  RELOAN_ORDER_PRE_CHECK("复贷打款前置校验", IDNHomepageDisplayStatusV5.ORDER_PRE_CHECK),
  RELOAN_PAYING("复贷打款中", IDNHomepageDisplayStatusV5.PAYING),
  RELOAN_READY("复贷已打款", IDNHomepageDisplayStatusV5.REPAYMENT),
  RELOAN_OVERDUE("复贷已逾期", IDNHomepageDisplayStatusV5.REPAYMENT),
  RELOAN_CAN_REAPPLY_NOW("当前可重新提交授信", IDNHomepageDisplayStatusV5.ENABLE_REAPPLIED),
  CALC_CREDITS_EXPIRED("复贷额度失效", IDNHomepageDisplayStatusV5.ENABLE_CALCULATE_CREDITS),
  RELOAN_CREDITS_DECREASE("复贷风控额度下降", IDNHomepageDisplayStatusV5.ENABLE_CREATE_ORDER),
  @Deprecated
  RELOAN_CREDITS_DECREASE_QUICK_ORDER("复贷降额快速下单", IDNHomepageDisplayStatusV5.CREDITS_DECREASE_QUICK_ORDER),


  // 续借
  MULTI_LOAN_INIT("可以续借额度测算", IDNHomepageDisplayStatusV5.ENABLE_CALCULATE_CREDITS),
  MULTI_LOAN_CALC_CREDITS_IN_REVIEW("续借额度测算中", IDNHomepageDisplayStatusV5.REVIEW),
  MULTI_LOAN_CREDITS_ACCEPTED("续借额度测算通过", IDNHomepageDisplayStatusV5.ENABLE_CREATE_ORDER),
  MULTI_LOAN_FUND_CHECK("续借订单资方审核中", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  MULTI_LOAN_GRAB_CHECK("续借订单抢单环节中", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  MULTI_LOAN_FUND_PAYING("续借订单资方打款中", IDNHomepageDisplayStatusV5.PAYING),
  MULTI_LOAN_DEBT_CHECK("续借债匹", IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING),
  MULTI_LOAN_ORDER_PRE_CHECK("续借打款前置校验", IDNHomepageDisplayStatusV5.ORDER_PRE_CHECK),
  MULTI_LOAN_IN_REVIEW("续借订单审核中", IDNHomepageDisplayStatusV5.REVIEW),
  MULTI_LOAN_PAYING("续借订单打款中", IDNHomepageDisplayStatusV5.PAYING),
  @Deprecated
  MULTI_LOAN_CREDITS_DECREASE_QUICK_ORDER("续借快速下单", IDNHomepageDisplayStatusV5.CREDITS_DECREASE_QUICK_ORDER),


  // 额度测算
  CAN_REAPPLY_IN_FUTURE("未来可重新提交授信", IDNHomepageDisplayStatusV5.REJECTED),

  PAYOUT_FAILED("打款失败", IDNHomepageDisplayStatusV5.ENABLE_CREATE_ORDER),

  // 未登录
  NOT_LOGIN("未登录", IDNHomepageDisplayStatusV5.NOT_LOGIN),

  @Deprecated
  // vip待领取
  VIP_CONFIRM("vip待领取", IDNHomepageDisplayStatusV5.LOAN_VIP_CONFIRM),

  INCREASE_REVIEW_NEVER_REAPPLIED("授信被拒度过静默期后，未提交增信重审资料", IDNHomepageDisplayStatusV5.ENABLE_REAPPLIED),

  INCREASE_REVIEW_REJECT("授信被拒度过静默期后，已提交增信重审资料", IDNHomepageDisplayStatusV5.REVIEW),
  //补件状态
  WAITING_SUPPLEMENT("待补件", IDNHomepageDisplayStatusV5.REVIEW),
  NEED_SUPPLEMENT("需补件", IDNHomepageDisplayStatusV5.NEED_SUPPLEMENT),
  FINISH_SUPPLEMENT("补件审核", IDNHomepageDisplayStatusV5.REVIEW),

  ACCEPTED_BUT_CAN_NOT_LOAN("有在贷授信通过但不可借", IDNHomepageDisplayStatusV5.REPAYMENT),
  ;

  public static List<IDNHomepageLoanStatusV5> LOAN_INCREASE_CREDITS = ImmutableList.of(ACCEPTED, READY, LOAN_CREDITS_EXPIRED, RELOAN_INIT
      , RELOAN_READY, CALC_CREDITS_EXPIRED, MULTI_LOAN_INIT);

  public static List<IDNHomepageLoanStatusV5> SECOND_RISK_REJECT_DISPLAY = ImmutableList.of(READY, RELOAN_READY, CAN_REAPPLY_IN_FUTURE);

  public static List<String> CAN_ORDER_STATUS_NAME = Arrays.stream(IDNHomepageLoanStatusV5.values())
      .filter(IDNHomepageLoanStatusV5::canCreateOrderOrQuickOrder)
      .map(IDNHomepageLoanStatusV5::name)
      .collect(ImmutableList.toImmutableList());


  public String desc;
  public IDNHomepageDisplayStatusV5 displayStatusV5;

  IDNHomepageLoanStatusV5(String desc, IDNHomepageDisplayStatusV5 displayStatusV5) {
    this.desc = desc;
    this.displayStatusV5 = displayStatusV5;
  }

  public static final Map<IDNHomepageLoanStatusV5, String> HomepageLoanStatusToAdminDescriptionMap = ImmutableMap.<IDNHomepageLoanStatusV5, String>builder()
      // 首贷
      .put(ACCEPTED, "首贷_授信通过_可下单")
      .put(MINIMALIST_ACCEPT, "首贷_极简授信通过_可下单")
      .put(NEVER_APPLIED, "首贷_未授信通过")
      .put(IN_REVIEW, "首贷_授信中")
      .put(MINIMALIST_IN_REVIEW, "首贷_极简授信中")
      .put(REJECTED, "首贷_授信被拒")
      .put(CANCELLED, "首贷_授信取消")
      .put(FUND_CHECK, "首贷_资方审核中")
      .put(GRAB_CHECK, "首贷_资方审核中")
      .put(FUND_PAYING, "首贷_资方打款中")
      .put(DEBT_CHECK, "首贷_债匹")
      .put(ORDER_PRE_CHECK, "首贷_打款前置校验")
      .put(PAYING, "首贷_打款中")
      .put(READY, "首贷_已打款")
      .put(OVERDUE, "首贷_已逾期")
      .put(CAN_REAPPLY_NOW, "首贷_可重新提交授信")
      .put(LOAN_CREDITS_EXPIRED, "首贷_额度失效")
      .put(LOAN_CREDITS_DECREASE, "首贷_额度下降")
      // 复贷
      .put(RELOAN_INIT, "复贷_未借款_可下单")
      .put(RELOAN_IN_REVIEW, "复贷_授信中")
      .put(RELOAN_CALC_CREDITS_IN_REVIEW, "复贷_额度测算中")
      .put(RELOAN_REJECTED, "复贷_授信拒绝")
      .put(RELOAN_FUND_CHECK, "复贷_资方审核中")
      .put(RELOAN_GRAB_CHECK, "复贷_资方审核中")
      .put(RELOAN_FUND_PAYING, "复贷_资方打款中")
      .put(RELOAN_DEBT_CHECK, "复贷_债匹")
      .put(RELOAN_ORDER_PRE_CHECK, "复贷_打款前置校验")
      .put(RELOAN_PAYING, "复贷_打款中")
      .put(RELOAN_READY, "复贷_已打款")
      .put(RELOAN_OVERDUE, "复贷_已逾期")
      .put(RELOAN_CAN_REAPPLY_NOW, "复贷_可重新提交授信")
      .put(CALC_CREDITS_EXPIRED, "复贷_额度失效")
      .put(RELOAN_CREDITS_DECREASE, "复贷_风控额度下降")
      // 续借
      .put(MULTI_LOAN_INIT, "续借_额度测算")
      .put(MULTI_LOAN_CALC_CREDITS_IN_REVIEW, "续借_额度测算中")
      .put(MULTI_LOAN_CREDITS_ACCEPTED, "续借_额度测算通过_可续借")
      .put(MULTI_LOAN_FUND_CHECK, "续借_资方审核中")
      .put(MULTI_LOAN_GRAB_CHECK, "续借_资方审核中")
      .put(MULTI_LOAN_FUND_PAYING, "续借_资方打款中")
      .put(MULTI_LOAN_DEBT_CHECK, "续借_债匹")
      .put(MULTI_LOAN_ORDER_PRE_CHECK, "续借_打款前置校验")
      .put(MULTI_LOAN_IN_REVIEW, "续借_审核中")
      .put(MULTI_LOAN_PAYING, "续借_打款中")
      // 额度测算
      .put(CAN_REAPPLY_IN_FUTURE, "未来可重新提交授信")
      // 其它
      .put(PAYOUT_FAILED, "打款失败")
      .put(IMAGE_REVIEW_REJECTED, "人审驳回")
      .put(REUPLOAD_FINISHED, "额度测算中")
      // 未登录
      .put(NOT_LOGIN, "未登录")
      //TODO（shubo,T000000）翻译后续再说
      .put(INCREASE_REVIEW_NEVER_REAPPLIED, "授信被拒度过静默期_未提交增信重审资料")
      .put(INCREASE_REVIEW_REJECT, "授信被拒度过静默期后_已提交增信重审资料")
      .put(WAITING_SUPPLEMENT, "待补件")
      .put(NEED_SUPPLEMENT, "需补件")
      .put(FINISH_SUPPLEMENT, "补件审核")
      .put(ACCEPTED_BUT_CAN_NOT_LOAN, ACCEPTED_BUT_CAN_NOT_LOAN.desc)
      .build();

  @Override
  public String getDescription() {
    return desc;
  }

  /**
   * 可下单的statuses中剔除降额快速下单的statuses
   * @return 是否可下单
   */
  public boolean canCreateOrder() {
    return this.displayStatusV5 == IDNHomepageDisplayStatusV5.ENABLE_CREATE_ORDER;
  }

  public boolean acceptedButCanNotLoan() {
    return this == ACCEPTED_BUT_CAN_NOT_LOAN;
  }

  public boolean payingOrder() {
    return this.displayStatusV5 == IDNHomepageDisplayStatusV5.PAYING;
  }

  public boolean commonOnCheckingOrder() {
    return this.displayStatusV5 == IDNHomepageDisplayStatusV5.COMMON_ON_CHECKING;
  }

  public boolean repaymentOrder() {
    return this.displayStatusV5 == IDNHomepageDisplayStatusV5.REPAYMENT;
  }

  /**
   * 真正可下单的statuses
   * @return 是否可下单或降额快速下单
   */
  public boolean canCreateOrderOrQuickOrder() {
    return this.displayStatusV5 == IDNHomepageDisplayStatusV5.ENABLE_CREATE_ORDER
        || this.displayStatusV5 == IDNHomepageDisplayStatusV5.CREDITS_DECREASE_QUICK_ORDER;
  }

  public boolean reviewStatus() {
    return this.displayStatusV5 == IDNHomepageDisplayStatusV5.REVIEW;
  }

  public String getAdminDescriptionFrom() {
    if (HomepageLoanStatusToAdminDescriptionMap.containsKey(this)) {
      return HomepageLoanStatusToAdminDescriptionMap.get(this);
    }
    return null;
  }

  public boolean canTimeReapplyNow() {
    return this == CAN_REAPPLY_NOW || this == RELOAN_CAN_REAPPLY_NOW;
  }

  public boolean overdueStatus() {
    return this == OVERDUE || this == RELOAN_OVERDUE;
  }
}