package com.yqg.core.service.cashloan.homepage.utilities;

import static com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5.ACCEPTED;
import static com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5.MINIMALIST_ACCEPT;
import static com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5.NEVER_APPLIED;
import static com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5.READY;
import static com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5.RELOAN_READY;
import static com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5.RELOAN_REJECTED;

import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.LoginStatusCacheRecord;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatus;
import com.yqg.core.model.sql.user.LoginStatusCacheModel;
import com.yqg.core.service.bizcheck.resultvo.BizCheckCommonResultVO;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.auth.AuthService;
import com.yqg.core.service.cashloan.enums.CashLoanCalcCreditsStatus;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.HomePageStatusConfirmVO;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.ordercenter.CashLoanUserSupplementCreateOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.CashLoanOrderAdditionalInfoVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.homepage.display.IncreaseReviewNeverReappliedSupplier;
import com.yqg.core.service.homepage.display.IncreaseReviewRejectSupplier;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.monitor.homepage.HomePageStatusMonitorService;
import com.yqg.core.service.reupload.RiskReuploadService;
import com.yqg.core.service.reupload.vo.ReuploadStatusVO;
import com.yqg.core.service.risk.longshortuser.UserMinimalistJudgeService;
import com.yqg.core.service.sdktype.IdnLoanSDKTypeConstants;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.order.CashLoanOrderRejectReason;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.utils.EcAsserts;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HomepageStatusTool {
  @Autowired
  private RiskReuploadService riskReuploadService;
  @Autowired
  private HomepageParamTool homepageParamTool;
  @Autowired
  private HomePageStatusMonitorService homePageStatusMonitorService;
  @Autowired
  private UserMinimalistJudgeService userMinimalistJudgeService;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private IncreaseReviewRejectSupplier increaseReviewRejectSupplier;
  @Autowired
  private IncreaseReviewNeverReappliedSupplier increaseReviewNeverReappliedSupplier;
  @Autowired
  private CashLoanUserSupplementCreateOrderService supplementCreateOrderService;
  @Autowired
  private LoginStatusCacheModel loginStatusCacheModel;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private AuthService authService;

  /**
   * 通用的获取首页状态的方法
   */
  public IDNHomepageLoanStatusV5 getStatus(Long loanAccountId, Long build, SDKType sdkType) {
    // TODO(jiajun): 确认下这里是否还需要该校验
    if (!IdnLoanSDKTypeConstants.isIdnAllLoanSDKType(sdkType)) {
      return null;
    }
    if (loanAccountId == null) {
      return IDNHomepageLoanStatusV5.NOT_LOGIN;
    }
    HomePageStatusConfirmVO paramsVO = homepageParamTool.getHomePageStatusConfirmVO(loanAccountId);
    return confirmStatusInMultiLoanFirst(paramsVO, build);
  }

  /**
   * 通用的获取首页状态的方法
   */
  public IDNHomepageLoanStatusV5 getStatusByUserId(Long userId, Long build, SDKType sdkType) {
    LoanAccountRecord loanAccountRecord = loanAccountModel.findByUserId(userId, sdkType);
    return getStatus(loanAccountRecord == null ? null : loanAccountRecord.getId(), build, sdkType);
  }

  /**
   * 等后续兼容完代码删除
   */
  @Deprecated
  public IDNHomepageLoanStatusV5 confirmStatus(HomepageUserParamsVO paramsVO, Long build) {
    return confirmStatusInMultiLoanFirst(paramsVO.convertToHomePageStatusConfirmVO(), build);
  }

  /**
   * 通用的获取首页状态的方法
   * 用于非api服务调用，根据用户历史登录信息获取版本号
   */
  public IDNHomepageLoanStatusV5 getStatusByLoanAccountIdAndUserId(Long loanAccountId, Long userId, SDKType sdkType) {
    // TODO(jiajun): 确认下这里是否还需要该校验
    if (!IdnLoanSDKTypeConstants.isIdnAllLoanSDKType(sdkType)) {
      return null;
    }
    if (loanAccountId == null) {
      return IDNHomepageLoanStatusV5.NOT_LOGIN;
    }

    LoginStatusCacheRecord loginStatusCacheRecord = loginStatusCacheModel.findLatestByUserId(userId);
    if (Objects.isNull(loginStatusCacheRecord)) {
      log.info("The user has never logged in EasyCash APP, may be user is from api channel? userId = {}", userId);
    }

    // 如果没有找 loginStatusCache 则将 build 赋值为最小版本
    // 采用一个保守的首页用户状态，不影响后续的关案问题
    Long build = Objects.isNull(loginStatusCacheRecord)
        ? 0L
        : loginStatusCacheRecord.getBuild();
    HomePageStatusConfirmVO paramsVO = homepageParamTool.getHomePageStatusConfirmVO(loanAccountId);
    return confirmStatusInMultiLoanFirst(paramsVO, build);
  }

  /**
   * 优先确认续借状态
   *
   * @param paramsVO 决定用户首页状态的所有参数
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInMultiLoanFirst(HomePageStatusConfirmVO paramsVO, Long build) {
    MultiLoanStatus multiLoanStatus = paramsVO.getMultiLoanStatus();
    //下单补件状态优先校验，因为补件是可能发生在首贷，复贷，续借所有环节的
    if (Objects.nonNull(paramsVO.getCreditsInfoVO())) {
      IDNHomepageLoanStatusV5 supplementStatus = supplementCreateOrderService.fetchHomepageStatusV5(paramsVO.getLoanAccountId(), paramsVO.getLoanUserSupplementStatus());
      if (Objects.nonNull(supplementStatus)) {
        return supplementStatus;
      }
    }
    //用户循环贷优先校验，因为循环贷可能发生在复贷和续借环节
    if (paramsVO.getRevolvingLoanUser()) {
      return confirmHomeStatusInRevolving(paramsVO, build);
    }
    IDNHomepageLoanStatusV5 res;
    switch (multiLoanStatus) {
      case INVALID:
      case CALC_REJECTED:
      case ORDER_REJECTED:
        // 续借不可用 优先判断复贷
        res = confirmStatusInReloanOrFirstLoan(paramsVO, build);
        break;
      case INIT:
        res = IDNHomepageLoanStatusV5.MULTI_LOAN_INIT;
        break;
      case CALC_STARTED:
      case ORDER_CHECKING:
        ReuploadStatusVO reuploadStatusVO = riskReuploadService.getStatusVO(paramsVO.getLoanAccountId());
        //先判断证件重传
        if (reuploadStatusVO.enterReUploadProcess()) {
          return confirmStatusInReUpload(paramsVO.getLoanAccountId(), reuploadStatusVO);
        }
        res = multiLoanStatus == MultiLoanStatus.CALC_STARTED ? IDNHomepageLoanStatusV5.MULTI_LOAN_CALC_CREDITS_IN_REVIEW : IDNHomepageLoanStatusV5.MULTI_LOAN_IN_REVIEW;
        break;
      case CALC_ACCEPTED:
        //续借额度失效（实际这里是回捞额度失效）
        if (paramsVO.getCashLoanCalcCreditsVO().calcCreditsStatus == CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED) {
          return IDNHomepageLoanStatusV5.MULTI_LOAN_INIT;
        }
        res = changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.MULTI_LOAN_CREDITS_ACCEPTED, IDNHomepageLoanStatusV5.RELOAN_READY);
        break;
      case ORDER_ACCEPTED:
        // 获取latestOrderVO代码逻辑会返回null 但是实际上如果返回null 续借状态不会为ORDER_ACCEPTED 而是INVALID 所以这里不判空
        res = confirmStatusInMultiLoanOrderAccepted(paramsVO.getLatestOrderVO(), paramsVO.getCommonResultVO(), build);
        break;
      default:
        throw EcException.error("unknown multiLoanStatus:" + multiLoanStatus);
    }

    return res;
  }


  /**
   * 需要修改可借的首页状态
   *
   * @param paramsVO
   * @param currentStatus 当前首页状态
   * @param defaultStatus 不符合修改条件 默认的修改状态
   * @return
   */
  private IDNHomepageLoanStatusV5 changeHomeStatusForCanLoanStatus(HomePageStatusConfirmVO paramsVO,
                                                                   IDNHomepageLoanStatusV5 currentStatus,
                                                                   IDNHomepageLoanStatusV5 defaultStatus) {
    // 只有大于可借产品&&有可借产品才返回可借状态
    boolean returnCanOrderStatus = !paramsVO.getCreditsInfoVO().lessThanMinCanLoanCredits(paramsVO.getHomeConfigVO()) && paramsVO.getRemainCreditsVO().existLoanProduct();
    // 满足可借条件
    if (returnCanOrderStatus) {
      return currentStatus;
    }
    // 判断是否是风控人群
    if (homepageV5Config.needCheckRiskTag() && !homepageParamTool.needCheckAcceptButCannotLoan(paramsVO.getLoanAccountId())) {
      log.info("可借状态，但是不能借款, userId:{},currentStatus:{}", paramsVO.getUserId(), currentStatus);
      return currentStatus;
    }
    return paramsVO.hasReadyOrders() ? IDNHomepageLoanStatusV5.ACCEPTED_BUT_CAN_NOT_LOAN : defaultStatus;
  }


  /**
   * 返回循环贷首页状态
   * 1.余额不足 - 未逾期状态   REPAYMENT
   * 2.循环额度 - 可下单  ENABLE_CREATE_ORDER
   * 3.循环额度 - 二次风控审核中 / 放款中  REVIEW/PAYING
   * 4.循环额度 - 额度测算（未逾期状态） REVIEW
   * 5.循环额度 - 账单逾期 REPAYMENT
   * 6.循环额度 - 账号管制 REJECTED
   * 注：所有的参数情况使用 paramsVo里面的，尽量不要再次查询数据库，避免出现首页状态异常
   * 注：逾期 ＞低额度 ＞ 管制
   */
  public IDNHomepageLoanStatusV5 confirmHomeStatusInRevolving(HomePageStatusConfirmVO paramsVO, Long build) {

    LoanUserCreditsInfoVO creditsInfoVO = paramsVO.getCreditsInfoVO();
    if (creditsInfoVO == null) {
      // 没有授信就是处于未授信状态
      log.error("confirmHomeStatusInRevolving do not have creditsInfo.userId is {}", paramsVO.getAccountVO().userId);
      return NEVER_APPLIED;
    }

    if (creditsInfoVO.reloanStatus == null) {
      // 没有复贷授信则按首贷判断
      log.error("confirmHomeStatusInRevolving error,user can not be loan.userId is {}", paramsVO.getAccountVO().userId);
      return confirmStatusInFirstLoan(paramsVO, build);
    }

    switch (creditsInfoVO.reloanStatus) {
      case ACCEPTED:
        return confirmStatusInCreditsAcceptedForRevolvingLoan(paramsVO);
      case REJECTED:
        log.error("confirmHomeStatusInRevolving error,user has been reject.userId is {}", paramsVO.getAccountVO().userId);
        return IDNHomepageLoanStatusV5.RELOAN_REJECTED;
      case IN_REVIEW:
      case MANUAL_REVIEW:
        return confirmStatusInReloanReview(paramsVO.getLatestUserRiskTraceVO());
      default:
        throw EcException.error("unsupported ReloanCreditsStatus:" + creditsInfoVO.reloanStatus);
    }
  }

  private IDNHomepageLoanStatusV5 confirmStatusInCreditsAcceptedForRevolvingLoan(HomePageStatusConfirmVO paramsVO) {
    CashLoanOrderVO latestOrderVO = paramsVO.getLatestOrderVO();
    // 最后一笔订单的状态可以确认大多数状态
    switch (latestOrderVO.status) {
      case RESERVE:
        return IDNHomepageLoanStatusV5.RELOAN_IN_REVIEW;
      case CHECK:
        return confirmStatusInOrderCheckedWithCommonResultVO(latestOrderVO.id, paramsVO.getCommonResultVO(), true);
      case INIT:
        return IDNHomepageLoanStatusV5.RELOAN_PAYING;
      case REJECT:
        return confirmStatusInOrderRejectedForRevolvingLoan(latestOrderVO.mhtOrderNo, paramsVO);
      case READY:
        return confirmStatusInOrderReadyForRevolvingLoan(paramsVO);
      case COMPLETE:
        return confirmStatusInOrderCompletedForRevolvingLoan(paramsVO);
      default:
        throw EcException.error("unexpected order status: " + latestOrderVO.status);
    }
  }

  private IDNHomepageLoanStatusV5 confirmStatusInOrderRejectedForRevolvingLoan(String mhtOrderNo,
                                                                               HomePageStatusConfirmVO paramsVO) {
    boolean havingReadyOrder = paramsVO.hasReadyOrders();
    IDNHomepageLoanStatusV5 homepageLoanStatusV5 = paramsVO.getHomepageStatusByCreditsExpired();
    // 虽然最后一笔是拒绝 但可能存在其他在还订单 判断是否有待还订单
    if (havingReadyOrder) {
      // 这里就回到根据ready态的订单决定首页状态
      return confirmStatusInOrderReadyForRevolvingLoan(paramsVO);
    } else {
      // 首贷与复贷都存在过期的情况,但是循环不存在，打印 error
      if (homepageLoanStatusV5 != null) {
        log.error("confirmStatusInOrderRejectedForRevolvingLoan error accountId is {},homepageLoanStatusV5 is {}",
            paramsVO.getLoanAccountId(), homepageLoanStatusV5);
        return homepageLoanStatusV5;
      }
      IDNHomepageLoanStatusV5 creditsAndControlStatus = revolvingLoanCreditsNotEnoughOrControlled(paramsVO);
      if (Objects.nonNull(creditsAndControlStatus)) {
        return creditsAndControlStatus;
      }
      if (mhtOrderNo != null) {
        return changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.PAYOUT_FAILED, RELOAN_REJECTED);
      }
      if (paramsVO.getOrderRejectReasonVO() != null
          && CashLoanOrderRejectReason.valueOf(paramsVO.getOrderRejectReasonVO().info).equals(CashLoanOrderRejectReason.INSUFFICIENT_QUOTA)) {
        return changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.RELOAN_CREDITS_DECREASE, RELOAN_REJECTED);
      }
      //单独在 processer 确定循环用户的输出
      return changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.RELOAN_INIT, RELOAN_REJECTED);
    }
  }

  public IDNHomepageLoanStatusV5 revolvingLoanCreditsNotEnoughOrControlled(HomePageStatusConfirmVO paramsVO) {
    //管制用户逾期或者有未完成的订单则还款，都没有则被拒
    boolean userControl = paramsVO.getLoanAccountRevolvingCreditVO().getUserControl();
    if (userControl) {
      return paramsVO.hasReadyOrders() ? IDNHomepageLoanStatusV5.RELOAN_READY : IDNHomepageLoanStatusV5.RELOAN_REJECTED;
    }
    //余额不足适配
    BigDecimal revolvingLoanMinRemainCredits = homepageV5Config.getRevolvingLoanMinRemainCredits();
    BigDecimal stepAmount = cashLoanConfig.getStepAmount(paramsVO.getAccountVO().sdkType);
    BigDecimal remainingCredits = paramsVO.getCreditsInfoVO().getRemainingCreditsForDisplay(stepAmount);
    log.info("confirmHomeStatusInRevolving revolvingLoanMinRemainCredits is {},remain is {}，userId is {},account id is {}",
        revolvingLoanMinRemainCredits.toString(),
        remainingCredits.toString(), paramsVO.getAccountVO().userId, paramsVO.getAccountVO().id);
    if (revolvingLoanMinRemainCredits.compareTo(remainingCredits) > 0) {
      return paramsVO.hasReadyOrders() ? IDNHomepageLoanStatusV5.RELOAN_READY : IDNHomepageLoanStatusV5.RELOAN_REJECTED;
    }
    return null;
  }

  private IDNHomepageLoanStatusV5 confirmStatusInOrderReadyForRevolvingLoan(HomePageStatusConfirmVO paramsVO) {
    if (paramsVO.isOverdue()) {
      return IDNHomepageLoanStatusV5.RELOAN_OVERDUE;
    } else {
      IDNHomepageLoanStatusV5 creditsAndControlStatus = revolvingLoanCreditsNotEnoughOrControlled(paramsVO);
      if (Objects.nonNull(creditsAndControlStatus)) {
        return creditsAndControlStatus;
      }
      return changeHomeStatusForCanLoanStatus(paramsVO,
          IDNHomepageLoanStatusV5.RELOAN_INIT,
          getStatusOfChangeStatus(true, paramsVO.hasReadyOrders()));
    }
  }

  private IDNHomepageLoanStatusV5 confirmStatusInOrderCompletedForRevolvingLoan(HomePageStatusConfirmVO paramsVO) {
    if (paramsVO.hasReadyOrders()) {
      return confirmStatusInOrderReadyForRevolvingLoan(paramsVO);
    } else {
      if (paramsVO.getHomepageStatusByCreditsExpired() != null) {
        return paramsVO.getHomepageStatusByCreditsExpired();
      }
      IDNHomepageLoanStatusV5 creditsAndControlStatus = revolvingLoanCreditsNotEnoughOrControlled(paramsVO);
      if (Objects.nonNull(creditsAndControlStatus)) {
        return creditsAndControlStatus;
      }
      return changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.RELOAN_INIT, RELOAN_REJECTED);
    }
  }

  /**
   * 优先确认复贷状态
   *
   * @param paramsVO 决定用户首页状态的所有参数
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInReloanOrFirstLoan(HomePageStatusConfirmVO paramsVO, Long build) {
    LoanUserCreditsInfoVO creditsInfoVO = paramsVO.getCreditsInfoVO();
    if (creditsInfoVO == null) {
      // 没有授信就是处于未授信状态
      return NEVER_APPLIED;
    }

    if (creditsInfoVO.reloanStatus == null) {
      // 没有复贷授信则按首贷判断
      return confirmStatusInFirstLoan(paramsVO, build);
    }
    switch (creditsInfoVO.reloanStatus) {
      case ACCEPTED:
        return confirmStatusInCreditsAccepted(paramsVO, true, build);
      case REJECTED:
        return confirmStatusInCreditsRejected(creditsInfoVO, true, build);
      case IN_REVIEW:
      case MANUAL_REVIEW:
        return confirmStatusInReloanReview(paramsVO.getLatestUserRiskTraceVO());
      default:
        throw EcException.error("unsupported ReloanCreditsStatus:" + creditsInfoVO.reloanStatus);
    }
  }

  /**
   * 确认首贷状态
   *
   * @param paramsVO 决定用户首页状态的所有参数
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInFirstLoan(HomePageStatusConfirmVO paramsVO, Long build) {
    LoanUserCreditsInfoVO creditsInfoVO = paramsVO.getCreditsInfoVO();
    switch (creditsInfoVO.creditsStatus) {
      case IN_REVIEW:
      case MANUAL_REVIEW:
        return confirmStatusInFirstReview(paramsVO.getAccountVO().id);
      case ACCEPTED:
        return confirmStatusInCreditsAccepted(paramsVO, false, build);
      case REJECTED:
        return confirmStatusInCreditsRejected(creditsInfoVO, false, build);
      case NOT_APPLIED:
        return NEVER_APPLIED;
      case CANCELLED:
        return IDNHomepageLoanStatusV5.CANCELLED;
      default:
        throw EcException.error("unsupported loanCreditsStatus: " + creditsInfoVO.creditsStatus);
    }
  }

  /**
   * 确认续借订单审核通过以后的首页状态
   *
   * @param orderVO 最新一笔订单
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInMultiLoanOrderAccepted(CashLoanOrderVO orderVO, BizCheckCommonResultVO commonResultVO, Long build) {
    switch (orderVO.status) {
      case INIT:
      case REJECT:
        return IDNHomepageLoanStatusV5.MULTI_LOAN_PAYING;
      case READY:
        // 其中出现 打款成功（READY）状态的，这个时候正确的续借状态应该是INIT或者INVALID，但是因为续借状态是通过kafka消息异步更新的，没有及时更新，
        // 虽然用户续借状态还停留在 ORDER_ACCEPTED ，但是用户订单信息已经更新，待还订单中可以直接看到这一笔已经打出去的订单了，因此展示复贷已打款
        return IDNHomepageLoanStatusV5.RELOAN_READY;
      case CHECK:
        return confirmMultiStatusWhenOrderCheckedWithCommonResultVO(orderVO.id, commonResultVO);
      case RESERVE:
        return IDNHomepageLoanStatusV5.MULTI_LOAN_IN_REVIEW;
      default:
        // 其余订单状态按理说不会出现 因为这里是逆向判断 先判断用户的续借状态
        // 能够得出ORDER_ACCEPTED这个续借状态 用户的订单一定是 前置校验 打款中 打款失败 打款成功 这四个其中一个
        // 甚至REJECT状态都不大会出现 因为一旦多次打款失败了 那么用户的续借状态就会变为CALC_ACCEPTED
        // 如果不在这四个状态的话 那反之用户不会处于ORDER_ACCEPTED状态

        //但是可能由于kafka消费延迟，导致用户续借状态还没来得及更新，进入到这里，这种情况打点观察一下
        homePageStatusMonitorService.logUnexpectedOrderStatusForMultiLoanAccepted(orderVO.status.toString(), orderVO.userId, orderVO.sdkType);
        log.info("unexpected status->" + orderVO.status);
        return IDNHomepageLoanStatusV5.MULTI_LOAN_IN_REVIEW;
    }
  }


  @NotNull
  private IDNHomepageLoanStatusV5 confirmMultiStatusWhenOrderCheckedWithCommonResultVO(Long orderId, BizCheckCommonResultVO commonResultVO) {
    EcAsserts.assertNotNull(commonResultVO, "latest common check resultVO is null, orderId is {}", orderId);
    switch (commonResultVO.checkType.checkGroup) {
      case SIGNATURE:
        return IDNHomepageLoanStatusV5.MULTI_LOAN_ORDER_PRE_CHECK;
      case GRAB:
        return IDNHomepageLoanStatusV5.MULTI_LOAN_GRAB_CHECK;
      case FUND:
        return IDNHomepageLoanStatusV5.MULTI_LOAN_FUND_CHECK;
      case DEBT:
        return IDNHomepageLoanStatusV5.DEBT_CHECK;
      case FUND_PAYOUT:
        return IDNHomepageLoanStatusV5.MULTI_LOAN_FUND_PAYING;
      default:
        throw EcException.error("latest common check resultVO is error, checkType is {}, orderId is {}", commonResultVO.checkType, commonResultVO.businessId);
    }
  }

  /**
   * 确认授信通过后的首页状态
   *
   * @param paramsVO 决定用户首页状态的所有参数
   * @param isReloan 是否复贷
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInCreditsAccepted(HomePageStatusConfirmVO paramsVO, boolean isReloan, Long build) {
    CashLoanOrderVO latestOrderVO = paramsVO.getLatestOrderVO();
    if (latestOrderVO == null) {
      if (isReloan) {
        // 复贷必须存在订单
        throw EcException.error("error when get home page status, latestOrderVO is null!");
      } else {
        // 首贷没下单保持 已授信状态
        IDNHomepageLoanStatusV5 homepageLoanStatusV5 = paramsVO.getHomepageStatusByCreditsExpired();
        homepageLoanStatusV5 = homepageLoanStatusV5 != null && homepageLoanStatusV5.canTimeReapplyNow()
            ? getIDNHomepageLoanStatusV5ForTimeReapply(paramsVO.getUserId(), paramsVO.getLoanAccountId(), build, isReloan)
            : homepageLoanStatusV5;
        return homepageLoanStatusV5 == null
            ? getIDNHomepageLoanStatusV5ForFirstLoanAccept(paramsVO, build)
            : homepageLoanStatusV5;
      }
    }
    // 最后一笔订单的状态可以确认大多数状态
    switch (latestOrderVO.status) {
      case RESERVE:
        log.info("status hack log, order status is RESERVE, order id is {}", latestOrderVO.id);
        //kafka消费延迟，导致授信是ACCEPT，但是订单状态还是RESERVE，返回授信中
        return isReloan ? IDNHomepageLoanStatusV5.RELOAN_IN_REVIEW : IDNHomepageLoanStatusV5.IN_REVIEW;
      case CHECK:
        //加了资方后，需要根据当前check步骤区分(当前处于资方check，返回资方审核中，电子签名返回打款前置校验)
        return confirmStatusInOrderCheckedWithCommonResultVO(latestOrderVO.id, paramsVO.getCommonResultVO(), isReloan);
      case INIT:
        return isReloan ? IDNHomepageLoanStatusV5.RELOAN_PAYING : IDNHomepageLoanStatusV5.PAYING;
      case REJECT:
        // 要考虑多订单状态
        return confirmStatusInOrderRejected(paramsVO, isReloan);
      case READY:
        // 但是当最后一笔订单是READY的时候 逾期场景要考虑多订单
        return confirmStatusInOrderReady(paramsVO.isOverdue(), isReloan);
      case COMPLETE:
        // 但是当最后一笔订单是COMPLETE的时候 考虑多订单场景
        return confirmStatusInOrderCompleted(paramsVO, isReloan);
      default:
        throw EcException.error("unexpected order status: " + latestOrderVO.status);
    }
  }

  private IDNHomepageLoanStatusV5 getIDNHomepageLoanStatusV5ForFirstLoanAccept(HomePageStatusConfirmVO paramVO, Long build) {
    if (authService.isAuthFinished(paramVO.getLoanAccountId())) {
      return changeHomeStatusForCanLoanStatus(paramVO, ACCEPTED,
          paramVO.hasReadyOrders() ? READY : IDNHomepageLoanStatusV5.REJECTED);
    }

    // 极简低等级用户隐藏额度页时，首页状态为 NEVER_APPLIED ，引导用户继续完件流程。
    return userMinimalistJudgeService.shouldDisplayCreditPage(paramVO.getLoanAccountId(), build) ? MINIMALIST_ACCEPT : NEVER_APPLIED;

  }

  private IDNHomepageLoanStatusV5 confirmStatusInOrderCheckedWithCommonResultVO(Long orderId, BizCheckCommonResultVO commonResultVO, boolean isReloan) {
    EcAsserts.assertNotNull(commonResultVO, "latest common check resultVO is null, orderId is {}", orderId);
    switch (commonResultVO.checkType.checkGroup) {
      case SIGNATURE:
        return isReloan ? IDNHomepageLoanStatusV5.RELOAN_ORDER_PRE_CHECK : IDNHomepageLoanStatusV5.ORDER_PRE_CHECK;
      case GRAB:
        return isReloan ? IDNHomepageLoanStatusV5.RELOAN_GRAB_CHECK : IDNHomepageLoanStatusV5.GRAB_CHECK;
      case FUND:
        return isReloan ? IDNHomepageLoanStatusV5.RELOAN_FUND_CHECK : IDNHomepageLoanStatusV5.FUND_CHECK;
      case DEBT:
        return isReloan ? IDNHomepageLoanStatusV5.RELOAN_DEBT_CHECK : IDNHomepageLoanStatusV5.DEBT_CHECK;
      case FUND_PAYOUT:
        return isReloan ? IDNHomepageLoanStatusV5.RELOAN_FUND_PAYING : IDNHomepageLoanStatusV5.FUND_PAYING;
      default:
        throw EcException.error("latest common check resultVO is error, checkType is error, checkType is {}, orderId is {}", commonResultVO.checkType, commonResultVO.businessId);
    }
  }

  /**
   * 判断订单还完后的首页状态
   *
   * @param isReloan 是否复贷
   * @return
   */
  private IDNHomepageLoanStatusV5 confirmStatusInOrderCompleted(HomePageStatusConfirmVO paramVsO,
                                                                boolean isReloan) {
    boolean havingReadyOrder = paramVsO.hasReadyOrders();
    boolean isOverdue = paramVsO.isOverdue();
    IDNHomepageLoanStatusV5 homepageLoanStatusV5 = paramVsO.getHomepageStatusByCreditsExpired();
    // 虽然最后一笔是已还清 但可能存在其他在还订单 判断是否有待还订单
    if (havingReadyOrder) {
      // 这里就回到根据ready态的订单决定首页状态
      return confirmStatusInOrderReady(isOverdue, isReloan);
    } else {
      // 没有其他在还订单则根据授信状态决定
      if (homepageLoanStatusV5 != null) {
        return homepageLoanStatusV5;
      }
      return changeHomeStatusForCanLoanStatus(paramVsO, IDNHomepageLoanStatusV5.RELOAN_INIT, RELOAN_REJECTED);
    }
  }

  /**
   * 确认有订单被拒后的首页状态
   *
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInOrderRejected(HomePageStatusConfirmVO paramsVO, boolean isReloan) {
    boolean havingReadyOrder = paramsVO.hasReadyOrders();
    boolean isOverdue = paramsVO.isOverdue();
    IDNHomepageLoanStatusV5 homepageLoanStatusV5 = paramsVO.getHomepageStatusByCreditsExpired();
    String mhtOrderNo = paramsVO.getLatestOrderVO().mhtOrderNo;
    CashLoanOrderAdditionalInfoVO orderRejectReasonVO = paramsVO.getOrderRejectReasonVO();

    // 虽然最后一笔是拒绝 但可能存在其他在还订单 判断是否有待还订单
    if (havingReadyOrder) {
      // 这里就回到根据ready态的订单决定首页状态
      return confirmStatusInOrderReady(isOverdue, isReloan);
    } else {
      // 首贷与复贷都存在过期的情况
      if (homepageLoanStatusV5 != null) {
        return homepageLoanStatusV5;
      }

      if (mhtOrderNo != null) {
        return changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.PAYOUT_FAILED, getStatusOfChangeStatus(isReloan, false));
      }
      if (orderRejectReasonVO != null && CashLoanOrderRejectReason.valueOf(orderRejectReasonVO.info).equals(CashLoanOrderRejectReason.INSUFFICIENT_QUOTA)) {
        IDNHomepageLoanStatusV5 statusOfChangeStatus = getStatusOfChangeStatus(isReloan, paramsVO.hasReadyOrders());
        return isReloan ? changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.RELOAN_CREDITS_DECREASE, statusOfChangeStatus)
            : changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.LOAN_CREDITS_DECREASE, statusOfChangeStatus);
      }
      IDNHomepageLoanStatusV5 statusOfChangeStatus = getStatusOfChangeStatus(isReloan, paramsVO.hasReadyOrders());
      return isReloan ? changeHomeStatusForCanLoanStatus(paramsVO, IDNHomepageLoanStatusV5.RELOAN_INIT, statusOfChangeStatus)
          : changeHomeStatusForCanLoanStatus(paramsVO, ACCEPTED, statusOfChangeStatus);
    }

  }

  private IDNHomepageLoanStatusV5 getStatusOfChangeStatus(boolean isReloan, boolean havingReadyOrder) {
    if (isReloan) {
      return havingReadyOrder ? RELOAN_READY : RELOAN_REJECTED;
    }
    return havingReadyOrder ? READY : IDNHomepageLoanStatusV5.REJECTED;
  }

  /**
   * 根据逾期状态确认首复贷下有订单已打款后的首页状态
   *
   * @param isOverdue 用户是否逾期
   * @param isReloan  是否首复贷
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInOrderReady(boolean isOverdue, boolean isReloan) {
    if (isOverdue) {
      if (isReloan) {
        return IDNHomepageLoanStatusV5.RELOAN_OVERDUE;
      } else {
        return IDNHomepageLoanStatusV5.OVERDUE;
      }
    } else {
      if (isReloan) {
        return IDNHomepageLoanStatusV5.RELOAN_READY;
      } else {
        return IDNHomepageLoanStatusV5.READY;
      }
    }
  }

  /**
   * 确认首复贷授信拒绝后的结果
   *
   * @param creditsInfoVO 授信状态
   * @param isReloan      是否是复贷
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInCreditsRejected(LoanUserCreditsInfoVO creditsInfoVO, boolean isReloan, Long build) {
    // 被拒绝用户有 timeReapply
    if (creditsInfoVO.timeReapply > 0) {
      //时间到了，就可以重新申请
      if (creditsInfoVO.timeReapply <= Clock.now()) {
        return getIDNHomepageLoanStatusV5ForTimeReapply(creditsInfoVO.userId, creditsInfoVO.accountId, build, isReloan);
      }
      // 提交了资料
      return increaseReviewRejectSupplier.submitAndReviewingStatus(creditsInfoVO.accountId) ?
          IDNHomepageLoanStatusV5.INCREASE_REVIEW_REJECT : IDNHomepageLoanStatusV5.CAN_REAPPLY_IN_FUTURE;
    }
    if (increaseReviewRejectSupplier.submitAndReviewingStatus(creditsInfoVO.accountId)) {
      return IDNHomepageLoanStatusV5.INCREASE_REVIEW_REJECT;
    }
    return isReloan ? IDNHomepageLoanStatusV5.RELOAN_REJECTED : IDNHomepageLoanStatusV5.REJECTED;
  }

  private IDNHomepageLoanStatusV5 getIDNHomepageLoanStatusV5ForTimeReapply(Long userId, Long loanAccountId, Long build, Boolean isReloan) {
    if (increaseReviewNeverReappliedSupplier.showExtraInfoReapplyPage(userId, loanAccountId, build)) {
      //未提交增信重审资料
      return IDNHomepageLoanStatusV5.INCREASE_REVIEW_NEVER_REAPPLIED;
    }
    if (increaseReviewRejectSupplier.showExtraInfoReapplyPage(userId, loanAccountId, build)) {
      //已提交增信重审资料
      return IDNHomepageLoanStatusV5.INCREASE_REVIEW_REJECT;
    }
    return isReloan ? IDNHomepageLoanStatusV5.RELOAN_CAN_REAPPLY_NOW : IDNHomepageLoanStatusV5.CAN_REAPPLY_NOW;
  }

  /**
   * 确认首贷人审、机审的结果
   *
   * @param loanAccountId 借贷账号id
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInFirstReview(Long loanAccountId) {
    ReuploadStatusVO reuploadStatusVO = riskReuploadService.getStatusVO(loanAccountId);
    if (reuploadStatusVO.enterReUploadProcess()) {
      return confirmStatusInReUpload(loanAccountId, reuploadStatusVO);
    }
    return userMinimalistJudgeService.getMinimaListProcessByLoanAccountId(loanAccountId) ? IDNHomepageLoanStatusV5.MINIMALIST_IN_REVIEW : IDNHomepageLoanStatusV5.IN_REVIEW;
  }

  /**
   * 确认证件重传结果
   *
   * @param loanAccountId
   * @param reuploadStatusVO
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInReUpload(Long loanAccountId, ReuploadStatusVO reuploadStatusVO) {
    if (reuploadStatusVO.needReupload) {
      return IDNHomepageLoanStatusV5.IMAGE_REVIEW_REJECTED;
    } else if (reuploadStatusVO.reuploadFinished) {
      return IDNHomepageLoanStatusV5.REUPLOAD_FINISHED;
    }
    throw EcException.error("invalid reuploadStatusVO, loanAccountId:{}", reuploadStatusVO, loanAccountId);
  }

  /**
   * 确认复贷人审、机审的结果
   *
   * @param riskTraceVO 用户风控状态
   * @return 首页状态
   */
  private IDNHomepageLoanStatusV5 confirmStatusInReloanReview(LoanUserRiskTraceVO riskTraceVO) {
    EcAsserts.assertTrue(riskTraceVO != null, "Latest traceVO is null but reloan creditsStatus is not null! AccountId is {}.", riskTraceVO.accountId);
    ReuploadStatusVO reuploadStatusVO = riskReuploadService.getStatusVO(riskTraceVO.accountId);
    //先判断证件重传
    if (reuploadStatusVO.enterReUploadProcess()) {
      return confirmStatusInReUpload(riskTraceVO.accountId, reuploadStatusVO);
    }

    if (LoanUserRiskType.isCalcCredits(riskTraceVO.riskType)) {
      // 额度测算状态的风控类型 均流转至 复贷额度测算中
      return IDNHomepageLoanStatusV5.RELOAN_CALC_CREDITS_IN_REVIEW;
    } else {
      // 其余状态流转为 RELOAN_IN_REVIEW
      return IDNHomepageLoanStatusV5.RELOAN_IN_REVIEW;
    }
  }

  public boolean getSubHomePageFloatingIconDisplaySwitch() {
    return homepageV5Config.getSubHomePageFloatingIconDisplaySwitch();
  }
}
