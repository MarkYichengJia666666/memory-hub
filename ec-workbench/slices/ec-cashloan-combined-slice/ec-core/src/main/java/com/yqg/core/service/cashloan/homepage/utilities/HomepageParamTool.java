package com.yqg.core.service.cashloan.homepage.utilities;

import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.sql.cashloan.enums.BusinessType;
import com.yqg.core.model.sql.cashloan.enums.LoanUserSupplementStatus;
import com.yqg.core.model.sql.cashloan.enums.OrderAdditionalInfoType;
import com.yqg.core.model.sql.enums.AvailabilityStatus;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatus;
import com.yqg.core.model.sql.loan.coupon.enums.LoanCouponStatus;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.service.bizcheck.BizCheckConfig;
import com.yqg.core.service.bizcheck.BizCheckListService;
import com.yqg.core.service.bizcheck.resultvo.BizCheckCommonResultVO;
import com.yqg.core.service.cashloan.CashLoanCalcCreditsService;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.cashloan.fee.enums.CalcFeeScale;
import com.yqg.core.service.cashloan.fee.enums.CalcFeeScaleMapper;
import com.yqg.core.service.cashloan.homepage.abtest.uiv2.HomePageUiAbTestManagerService;
import com.yqg.core.service.cashloan.homepage.vo.*;
import com.yqg.core.service.cashloan.homepage.vo.ActivityInfo;
import com.yqg.core.userflow.domain.product.IProductInfoService;
import com.yqg.core.userflow.infrastructure.adapter.IProductAdapter;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanOrderAdditionalInfoService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanUserSupplementCreateOrderService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.CashLoanOrderAdditionalInfoVO;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.util.rate.ProductRateUtil;
import com.yqg.core.service.cashloan.vo.CashLoanCalcCreditsVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.vo.LoanAccountRevolvingCreditVO;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.longshortuser.UserMinimalistJudgeService;
import com.yqg.core.service.risk.usergroup.LoanRiskUserGroupService;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.util.common.NumberFormatter;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.LoanCouponUsageType;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.overseas.activity.clientcommon.enums.ActivityTaskTypeEnum;
import com.yqg.overseas.activity.clientcommon.enums.RewardRuleTypeEnum;
import com.yqg.overseas.activity.clientcommon.response.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Zoran Zhang
 * @Description: 印尼首页基本参数
 * @date 2022/2/18 11:44 上午
 */
@Slf4j
@Component
public class HomepageParamTool {
  @Autowired
  private CashLoanCalcCreditsService calcCreditsService;
  @Autowired
  private EcOrderService orderService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private IProductAdapter productAdapter;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private BizCheckListService checkListService;
  @Autowired
  private BizCheckConfig bizCheckConfig;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private IProductInfoService productInfoService;
  @Autowired
  private CashLoanOrderAdditionalInfoService cashLoanOrderAdditionalInfoService;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private StandardInterestUtil standardInterestUtil;
  @Autowired
  private HomePageUiAbTestManagerService homePageUiAbTestManagerService;
  @Autowired
  private HomepageActivityTool homepageActivityTool;
  @Autowired
  private LoanCreditsQuotaService loanCreditsQuotaService;
  @Autowired
  private UserMinimalistJudgeService userMinimalistJudgeService;
  @Autowired
  private CashLoanUserSupplementCreateOrderService cashLoanUserSupplementCreateOrderService;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;

  /**
   * 读取决定用户首页状态的需要用到的参数，虽然不是每个状态都需要以下全部参数，但统一从入口处就直接读取
   * 1. 因为部分状态涉及到多次读取 加事务会是一个长事务 这里不选该方式
   * 2. 可优化成在枚举指定具体ProviderClass的方式
   *
   * @param loanAccountId 借贷账号id
   * @param sdkType
   */
  public HomepageUserParamsVO getHomepageV5ParamsVO(Long loanAccountId, Long build, SDKType sdkType) {
    return threadTransactionalModel.transactionResult(configuration -> {
      HomepageUserParamsVO paramsVO = new HomepageUserParamsVO();
      // 账号
      paramsVO.accountVO = loanAccountService.checkAndGetLoanAccountVO(loanAccountId);

      paramsVO.loanUserProductTypeVO = productAdapter.getLoanUserProductTypeVOByLoanAccountId(loanAccountId);

      paramsVO.firstLoan = !loanAccountService.isReloan(loanAccountId);
      // 续借状态
      paramsVO.multiLoanStatus = multiLoanStatusService.getExistStatusOrInvalid(loanAccountId);
      // 当前补件状态
      paramsVO.loanUserSupplementStatus = cashLoanUserSupplementCreateOrderService.getSupplementStatusOrNullByAccountId(loanAccountId);

      RemainCreditsVO remainCreditsVO = loanCreditsQuotaService.getHomepageRemainCredits(paramsVO.accountVO);
      paramsVO.creditsInfoVO = remainCreditsVO.creditsInfoVO;
      paramsVO.remainCreditsVO = remainCreditsVO;
      if (remainCreditsVO != RemainCreditsVO.EMPTY) {
        paramsVO.productConfigList = remainCreditsVO.productConfigList;
        paramsVO.isNear = remainCreditsVO.isNear;
        //首页相关的接口展示的总剩余额度考虑虚拟额度
        paramsVO.remainingCredits = remainCreditsVO.remainingCreditsForVirtual;
      }

      // 在还订单
      paramsVO.readyOrderList = orderService.listAllOrderInstalment(loanAccountId, CashLoanOrderStatus.READY);
      // 最新的订单
      paramsVO.latestOrderVO = orderService.getLatestOrderVO(loanAccountId);
      // 最新的风控状态
      paramsVO.latestUserRiskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(loanAccountId);
      // 额度测算状态
      paramsVO.cashLoanCalcCreditsVO = calcCreditsService.getCalcCreditsStatus(paramsVO.accountVO.id);
      // 有可用优惠券
      paramsVO.havingAvailableCoupon = loanUserCouponService.havingAvailableCoupon(paramsVO.accountVO.userId);
      // 账户详细信息
      paramsVO.loanAccountDetailsSimpleVO = loanAccountDetailsService.getSimpleByUserIdOrNull(paramsVO.accountVO.userId);
      // 最新的checkStep
      if (paramsVO.latestOrderVO != null && paramsVO.latestOrderVO.status == CashLoanOrderStatus.CHECK && build < bizCheckConfig.getSupportDebtCheckVersion()) {
        paramsVO.checkResultVO = checkListService.getPendingOrLatestCheckInfo(paramsVO.latestOrderVO.id, BusinessType.ORDER_CHECK);
      }
      if (paramsVO.latestOrderVO != null && paramsVO.latestOrderVO.status == CashLoanOrderStatus.CHECK && build >= bizCheckConfig.getSupportDebtCheckVersion()) {
        paramsVO.commonResultVO = checkListService.getPendingOrLatestCheckResultVO(paramsVO.latestOrderVO.id, BusinessType.ORDER_CHECK);
      }
      if (paramsVO.latestOrderVO != null) {
        paramsVO.orderRejectReasonVO = cashLoanOrderAdditionalInfoService.findByOrderIdAndType(paramsVO.latestOrderVO.id, OrderAdditionalInfoType.REJECT_REASON);
      }
      paramsVO.build = build;
      paramsVO.minimalistProcessUser = userMinimalistJudgeService.isMinimalistProcessUser(paramsVO.getLoanAccountId());
      paramsVO.revolvingLoanUser = loanAccountRevolvingService.checkUserInRevolvingLoanProcess(paramsVO.getLoanAccountId());
      if (paramsVO.revolvingLoanUser) {
        paramsVO.loanAccountRevolvingCreditVO = loanAccountRevolvingService.fetchByLoanAccountId(paramsVO.getLoanAccountId());
      }
      paramsVO.loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(paramsVO.getLoanAccountId());
      paramsVO.homeConfigVO = new HomeConfigVO(cashLoanConfig.getStepAmount(sdkType), homepageV5Config.getLoanLimitAmountOverStepValue());
      paramsVO.prepareAbTestVO = getPrepareAbTestVO(paramsVO.accountVO.userId, build, paramsVO.getLoanAccountId());
      paramsVO.notLoginHomePageU2Switch = homepageV5Config.isNewHomePageUIForNotLogin();
      return paramsVO;
    });
  }

  /**
   * 仅用于确认首页状态
   *
   * @param loanAccountId
   * @return
   */
  public HomePageStatusConfirmVO getHomePageStatusConfirmVO(Long loanAccountId) {
    return threadTransactionalModel.transactionResult(configuration -> {
      // 账号
      LoanAccountVO accountVO = loanAccountService.checkAndGetLoanAccountVO(loanAccountId);
      // 续借状态
      MultiLoanStatus multiLoanStatus = multiLoanStatusService.getExistStatusOrInvalid(loanAccountId);
      // 用户当前补件状态
      LoanUserSupplementStatus loanUserSupplementStatus = cashLoanUserSupplementCreateOrderService.getSupplementStatusOrNullByAccountId(loanAccountId);

      RemainCreditsVO remainCreditsVO = loanCreditsQuotaService.getHomepageRemainCredits(loanAccountId);

      // 在还订单
      List<OrderInstalment> readyOrderList = orderService.listAllOrderInstalment(loanAccountId, CashLoanOrderStatus.READY);
      // 最新的订单
      CashLoanOrderVO latestOrderVO = orderService.getLatestOrderVO(loanAccountId);
      // 最新的风控状态
      LoanUserRiskTraceVO latestUserRiskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(loanAccountId);
      // 额度测算状态
      CashLoanCalcCreditsVO calcCreditsStatus = calcCreditsService.getCalcCreditsStatus(accountVO.id);
      BizCheckCommonResultVO commonResultVO = null;
      if (latestOrderVO != null && latestOrderVO.status == CashLoanOrderStatus.CHECK) {
        commonResultVO = checkListService.getPendingOrLatestCheckResultVO(latestOrderVO.id, BusinessType.ORDER_CHECK);
      }
      CashLoanOrderAdditionalInfoVO orderRejectReasonVO = null;
      if (latestOrderVO != null) {
        orderRejectReasonVO = cashLoanOrderAdditionalInfoService.findByOrderIdAndType(latestOrderVO.id, OrderAdditionalInfoType.REJECT_REASON);
      }
      Boolean revolvingLoanUser = loanAccountRevolvingService.checkUserInRevolvingLoanProcess(accountVO.id);
      LoanAccountRevolvingCreditVO loanAccountRevolvingCreditVO = null;
      if (revolvingLoanUser) {
        loanAccountRevolvingCreditVO = loanAccountRevolvingService.fetchByLoanAccountId(accountVO.id);
      }
      HomeConfigVO homeConfigVO = new HomeConfigVO(cashLoanConfig.getStepAmount(accountVO.sdkType), homepageV5Config.getLoanLimitAmountOverStepValue());
      LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrNull(accountVO.id);
      return HomePageStatusConfirmVO.from(accountVO, remainCreditsVO,
          multiLoanStatus, calcCreditsStatus, latestUserRiskTraceVO, readyOrderList,
          latestOrderVO, orderRejectReasonVO, commonResultVO, loanUserSupplementStatus,
          revolvingLoanUser, loanAccountRevolvingCreditVO, homeConfigVO, loanRiskUserGroupVO);
    });
  }

  private HomepagePrepareAbTestVO getPrepareAbTestVO(Long userId, Long build, Long loanAccountId) {
    //兼容H5对齐UI需求
    if(SourceType.WEB.equals(ImpliedContextUtils.sourceType())){
      if (homepageV5Config.getNewHomepageStartBuildWeb() > build) {
        return new HomepagePrepareAbTestVO();
      }
      return  homePageUiAbTestManagerService.getUiHomePageAbTestResultForH5(userId, loanAccountId, build);
    }

    if (homepageV5Config.getNewHomepageStartBuild() > build) {
      return new HomepagePrepareAbTestVO();
    }
    return homePageUiAbTestManagerService.getUiHomePageAbTestResult(userId, loanAccountId, build);
  }


  public boolean hasLowInterestProduct(Long userId, SDKType sdkType) {
    List<LoanProductConfigVO> productConfigVOS = getProductConfigVOS(userId, sdkType);
    if (productConfigVOS == null) {
      return false;
    }

    BigDecimal standardInterestRateVal = standardInterestUtil.getStandardInterestRate(userId);
    return productConfigVOS.stream().anyMatch(item -> {
      BigDecimal dayInterestRate = ProductRateUtil.genDayInterestRate(item);
      return BigDecimalHelper.compareTo(dayInterestRate, standardInterestRateVal) < 0;
    });
  }

  public List<LoanProductConfigVO> getProductConfigVOS(Long userId, SDKType sdkType) {
    LoanAccountVO loanAccountVO = loanAccountService.getAccountByUserIdOrNull(userId, sdkType);
    if (loanAccountVO == null) {
      return null;
    }
    LoanUserCreditsInfoVO loanUserCreditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(loanAccountVO.id);
    if (Objects.isNull(loanUserCreditsInfoVO)) {
      return null;
    }
    return productInfoService
        .filterProduct(userId, sdkType, loanAccountVO.loanUserTypeVO, loanUserCreditsInfoVO.totalRemainCredits).getProductConfigList();
  }

  public boolean allLowInterestProduct(Long accountId) {
    return allLowInterestProduct(loanAccountService.getLoanAccountVO(accountId));
  }

  public boolean allLowInterestProduct(Long userId, SDKType sdkType) {
    LoanAccountVO loanAccountVO = loanAccountService.getAccountByUserIdOrNull(userId, sdkType);
    if (loanAccountVO == null) {
      return false;
    }
    return allLowInterestProduct(loanAccountVO);
  }

  private boolean allLowInterestProduct(LoanAccountVO loanAccountVO) {
    LoanUserCreditsInfoVO loanUserCreditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(loanAccountVO.id);
    if (Objects.isNull(loanUserCreditsInfoVO)) {
      return false;
    }
    List<LoanProductConfigVO> productConfigVOS = productInfoService
        .filterProduct(loanAccountVO.userId, loanAccountVO.sdkType, loanAccountVO.loanUserTypeVO, loanUserCreditsInfoVO.totalRemainCredits).getProductConfigList();

    if (CollectionUtils.isEmpty(productConfigVOS)) {
      return false;
    }
    BigDecimal standardInterestRateVal = standardInterestUtil.getStandardInterestRate(loanAccountVO.userId);
    return productConfigVOS.stream().allMatch(item -> {
      BigDecimal dayInterestRate = ProductRateUtil.genDayInterestRate(item);
      return BigDecimalHelper.compareTo(dayInterestRate, standardInterestRateVal) < 0;
    });
  }

  public boolean hasCutInterestCoupon(Long userId) {
    List<LoanUserCouponVO> loanUserCouponVOList = loanUserCouponService.listByUserId(userId);
    List<LoanUserCouponVO> validLoanUserCouponVOList = loanUserCouponVOList
        .stream()
        .filter(o -> loanUserCouponService.canShowCouponPopWindow(o))
        .collect(Collectors.toList());
    if (CollectionUtils.isEmpty(validLoanUserCouponVOList)) {
      return false;
    }
    List<LoanCouponStatus> enabledList = LoanCouponStatus.listByAvailabilityStatus(AvailabilityStatus.ENABLED);
    return validLoanUserCouponVOList
        .stream()
        .anyMatch(o -> o.usageType == LoanCouponUsageType.CUT_INTEREST && enabledList.contains(o.status));
  }

  public List<ActivityInfo> getActivityInfoList(Long userId,
                                                Set<Long> activityIds,
                                                LoanUserCreditsInfoVO creditsInfoVO) {
    try {
      if (userId == null || CollectionUtils.isEmpty(activityIds)) {
        return Collections.emptyList();
      }

      List<ActivityConfigResponse> activityConfigResponseList = homepageActivityTool.fetchActivityConfig(activityIds);
      if (CollectionUtils.isEmpty(activityConfigResponseList)) {
        return Collections.emptyList();
      }

      Map<Long, ActivityConfigResponse> activityIdToConfigResponseMap = activityConfigResponseList
          .stream()
          .collect(Collectors.toMap(ActivityConfigResponse::getActivityId, o -> o));

      List<ActivityTaskDetailsResponse> activityTaskDetailsResponseList = homepageActivityTool.fetchTaskDetails(userId, activityIdToConfigResponseMap.keySet());
      if (CollectionUtils.isEmpty(activityTaskDetailsResponseList)) {
        return Collections.emptyList();
      }

      Map<Long, ActivityTaskDetailsResponse> activityIdToTaskDetailResponseMap = activityTaskDetailsResponseList
          .stream()
          .collect(Collectors.toMap(ActivityTaskDetailsResponse::getActivityId, o -> o));

      Long now = Clock.now();
      return activityIds
          .stream()
          .map(o -> getActivityInfo(o, userId, activityIdToConfigResponseMap.get(o), activityIdToTaskDetailResponseMap.get(o), now, creditsInfoVO))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("getActivityInfoList error", e);
      return Collections.emptyList();
    }
  }

  private ActivityInfo getActivityInfo(Long activityId,
                                       Long userId,
                                       ActivityConfigResponse activityConfigResponse,
                                       ActivityTaskDetailsResponse activityTaskDetailsResponse,
                                       Long now,
                                       LoanUserCreditsInfoVO creditsInfoVO) {
    //先判断下活动是否有效
    if (activityConfigResponse == null || activityConfigResponse.getOfflineTime() < now || activityConfigResponse.getOnlineTime() > now) {
      return null;
    }

    if (activityTaskDetailsResponse == null) {
      return null;
    }

    //取下单打款任务，奖励为优惠券或空奖励
    TaskAndRewardResponse taskAndRewardResponse = activityTaskDetailsResponse.getTaskInfoList()
        .stream()
        .filter(o -> {
          ActionInfo actionInfo = o.getActionInfoList().stream().filter(info -> info.getTaskType() == ActivityTaskTypeEnum.PAYOUT).findFirst().orElse(null);
          return actionInfo != null;
        })
        .filter(o -> {
          ActivityRewardResponse rewardResponse = o.getActivityRewardModels()
              .stream()
              .filter(this::isIncreaseCreditsActivityRewardType)
              .findFirst()
              .orElse(null);
          return rewardResponse != null;
        })
        .findFirst()
        .orElse(null);

    if (taskAndRewardResponse == null) {
      return ActivityInfo.from(activityId, activityConfigResponse.getOnlineTime(), activityConfigResponse.getOfflineTime(), null);
    }

    ActionInfo actionInfo = taskAndRewardResponse.getActionInfoList()
        .stream()
        .filter(info -> info.getTaskType() == ActivityTaskTypeEnum.PAYOUT)
        .findFirst()
        .orElseThrow(() -> EcException.error(EcExceptionType.COMMON_SERVER_ERROR, "error when get actionInfo, activityId:{}, userId:{}", activityId, userId));
    ActivityRewardResponse rewardResponse = taskAndRewardResponse.getActivityRewardModels()
        .stream()
        .filter(this::isIncreaseCreditsActivityRewardType)
        .findFirst()
        .orElseThrow(() -> EcException.error(EcExceptionType.COMMON_SERVER_ERROR, "error when get rewardResponse, activityId:{}, userId:{}", activityId, userId));


    SDKType sdkType = SDKType.IDN_YQD;
    CalcFeeScale feeScale = CalcFeeScaleMapper.getScaleBySdk(sdkType);


    //下单打款任务最小金额，如果未设置，取最小步长
    BigDecimal minLoanAmount = StringUtils.isEmpty(actionInfo.getMinLimit()) ? cashLoanConfig.getStepAmount(sdkType)
        : new BigDecimal(actionInfo.getMinLimit());
    String minLoanAmountFormat =
        NumberFormatter.format(sdkType.getLocale(), minLoanAmount.setScale(feeScale.interestScale, feeScale.interestRoundType));
    String activityTaskMinLoanAmount = String.format("Rp%s", minLoanAmountFormat);

    //获取展示的提额券金额
    // 如果奖励为券，计算模拟的提额金额
    // 如果奖励为EMPTY，根据用户总额度百分之10计算
    BigDecimal increaseCreditsAmount;
    if (rewardResponse.getRewardContentId() != null) {
      increaseCreditsAmount = loanUserCouponService.getMockIncreaseCreditsAmount(rewardResponse.getRewardContentId(), userId);
    } else {
      increaseCreditsAmount = getActivityIncreaseCreditsAmountBytTotalCredits(creditsInfoVO);
    }

    String activityRewardIncreaseCreditsAmount = null;
    if (increaseCreditsAmount != null) {
      String increaseCreditsAmountFormat =
          NumberFormatter.format(sdkType.getLocale(), increaseCreditsAmount.setScale(feeScale.interestScale, feeScale.interestRoundType));
      activityRewardIncreaseCreditsAmount = String.format("Rp%s", increaseCreditsAmountFormat);
    }

    ActivityInfo.PayoutTaskCreditCouponReward reward = ActivityInfo.PayoutTaskCreditCouponReward.from(activityTaskMinLoanAmount, activityRewardIncreaseCreditsAmount);
    return ActivityInfo.from(activityId, activityConfigResponse.getOnlineTime(), activityConfigResponse.getOfflineTime(), reward);
  }

  private BigDecimal getActivityIncreaseCreditsAmountBytTotalCredits(LoanUserCreditsInfoVO creditsInfoVO) {
    if (creditsInfoVO == null || creditsInfoVO.totalCredits == null) {
      return null;
    }
    int increaseCreditsAmount = homepageV5Config.getCreditsCouponActivityDisplayAmountPercent().multiply(creditsInfoVO.totalCredits).intValue();
    int stepAmount = cashLoanConfig.getStepAmount(SDKType.IDN_YQD).intValue();
    int finalAmount;
    //如果大于步长金额，则减掉余数
    //否则使用步长金额
    if (increaseCreditsAmount > stepAmount) {
      finalAmount = increaseCreditsAmount - (increaseCreditsAmount % stepAmount);
    } else {
      finalAmount = stepAmount;
    }
    return new BigDecimal(finalAmount);
  }

  private boolean isIncreaseCreditsActivityRewardType(ActivityRewardResponse activityRewardResponse) {
    return activityRewardResponse.getRewardType() == RewardRuleTypeEnum.VOUCHER || activityRewardResponse.getRewardType() == RewardRuleTypeEnum.EMPTY;
  }

  /**
   * 判断风控输出参数重是否有
   *
   * @return
   */
  public boolean needCheckAcceptButCannotLoan(Long loanAccountId) {
    String value = loanUserRiskTraceService.findLatestByLoanAccountIdAndTypeValue(loanAccountId, RiskOutputType.LACK_LIMIT_TEST);
    return Boolean.parseBoolean(value);
  }

}