package com.yqg.core.service.cashloan.ordercenter.orderlimit;

import com.google.common.collect.ImmutableSet;
import com.yqg.core.model.loader.OrderLimitInfoLoader;
import com.yqg.core.model.loader.lock.OrderLimitLocker;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.vo.OrderLimitCacheInfo;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.vo.UserOrderLimitVO;
import com.yqg.core.service.loan.infos.CashLoanEmploymentInfo;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsVO;
import com.yqg.core.service.monitor.EcOrderLimitMonitorService;
import com.yqg.core.service.risk.afpi.AfpiService;
import com.yqg.core.service.risk.afpi.vo.UserAfpiLoanInfoVO;
import com.yqg.core.util.common.RenamedThreadFactory;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.thread.EcExecutor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CashLoanOrderLimitService {

  @Autowired
  private CashLoanOrderLimitConfig cashLoanOrderLimitConfig;
  @Autowired
  private OrderLimitInfoLoader orderLimitInfoLoader;
  @Autowired
  private OrderLimitLocker orderLimitLocker;
  @Autowired
  private LoanAccountDetailsService accountDetailsService;
  @Autowired
  private AfpiService afpiService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private EcOrderLimitMonitorService ecOrderLimitMonitorService;

  private static final Set<String> EC_COMPANY_IDS = ImmutableSet.of("1", "AFDC239");

  // 一年12个月
  private static final String MONTHS_OF_YEAR = "12";

  private static ExecutorService executor = new EcExecutor(10,
      20,
      5L, TimeUnit.MINUTES,
      new LinkedBlockingQueue<>(2000),
      new RenamedThreadFactory(CashLoanOrderLimitService.class.getSimpleName()),
      new ThreadPoolExecutor.DiscardPolicy());

  private UserOrderLimitVO getOrderLimitInfo(Long loanAccountId) {
    boolean whiteListValue = cashLoanOrderLimitConfig.getWhiteList().contains(loanAccountId.toString());

    UserOrderLimitType limitType = cashLoanOrderLimitConfig.getLimitType();

    if (!whiteListValue && UserOrderLimitType.LIMIT_TYPE_NONE == limitType) {
      return UserOrderLimitVO.from(limitType, OrderOverLimitLevel.LIMIT_NONE);
    }

    OrderOverLimitLevel orderOverLimitLevel = getUserOrderLimitLevelFromCache(loanAccountId);
    if (Objects.nonNull(orderOverLimitLevel)) {
      return UserOrderLimitVO.from(limitType, orderOverLimitLevel);
    }
    executor.submit(() -> getOverLimitLevelFromRisk(loanAccountId));
    return UserOrderLimitVO.from(limitType, OrderOverLimitLevel.LIMIT_NONE);
  }

  @Nullable
  private OrderOverLimitLevel getUserOrderLimitLevelFromCache(Long loanAccountId) {
    OrderLimitCacheInfo orderLimitCacheInfo = orderLimitInfoLoader.get(loanAccountId.toString());
    if (Objects.nonNull(orderLimitCacheInfo) && (orderLimitCacheInfo.expiredTime > Clock.now())) {
      return orderLimitCacheInfo.level;
    }
    return null;
  }

  private OrderOverLimitLevel getOverLimitLevelFromRisk(Long loanAccountId) {
    try {
      return orderLimitLocker.lockAndRunResult(loanAccountId, () -> {
        OrderOverLimitLevel cachedLimitLevel = getUserOrderLimitLevelFromCache(loanAccountId);
        if (cachedLimitLevel != null) {
          return cachedLimitLevel;
        }
        OrderOverLimitLevel res = calculateLimitLevel(loanAccountId);
        ecOrderLimitMonitorService.log(loanAccountId, res);
        return res;
      });

    } catch (Exception e) {
      log.warn("Error calculating over limit level for account {}, e:", loanAccountId, e);
      cacheQueryResultWithQueryError(loanAccountId);
      return OrderOverLimitLevel.LIMIT_NONE;
    }
  }

  private OrderOverLimitLevel calculateLimitLevel(Long loanAccountId) {
    boolean ecHasOrder = ecOrderService.existOrder(loanAccountId, CashLoanOrderStatus.READY);
    OrderOverLimitLevel limitCondition = cashLoanOrderLimitConfig.getLimitCondition();

    if (limitReachedForPlatform(ecHasOrder, limitCondition)) {
      cacheQueryResult(Clock.now() + cashLoanOrderLimitConfig.getQueryFailOrDefaultCacheTime() * Clock.MILLS_PER_SECOND, loanAccountId,
          OrderOverLimitLevel.LIMIT_NONE, cashLoanOrderLimitConfig.getQueryFailOrDefaultCacheTime());
      return OrderOverLimitLevel.LIMIT_NONE;
    }

    return calculateLimitBasedOnLoanInfo(loanAccountId, ecHasOrder);
  }

  private boolean limitReachedForPlatform(boolean ecHasOrder, OrderOverLimitLevel limitCondition) {
    return OrderOverLimitLevel.OVER_PLATFORM_NUM_LIMIT == limitCondition && ecHasOrder;
  }

  private OrderOverLimitLevel calculateLimitBasedOnLoanInfo(Long loanAccountId, boolean ecHasOrder) {
    BigDecimal userEcAnnualAmount = getUserEcAnnualAmount(loanAccountId);
    UserAfpiLoanInfoVO afpiLoanInfoVO = getAfpiLoanInfo(loanAccountId);

    if (!afpiLoanInfoVO.querySuccess) {
      cacheQueryResultWithQueryError(loanAccountId);
      return OrderOverLimitLevel.LIMIT_NONE;
    }

    if (afpiLoanInfoVO.queryAfpiTime < Clock.now() - cashLoanOrderLimitConfig.getAfpiDataValidTime()) {
      cacheQueryResultWithQueryError(loanAccountId);
      return OrderOverLimitLevel.LIMIT_NONE;
    }

    int orderPlatformNum = calculateOrderPlatformNumber(ecHasOrder, afpiLoanInfoVO);
    BigDecimal afpiLoanTotalAmount = afpiLoanInfoVO.loanTotalAmount;

    OrderOverLimitLevel orderOverLimitLevel = calcOrderLimitLevel(orderPlatformNum, afpiLoanTotalAmount, userEcAnnualAmount);
    cacheQueryResult(afpiLoanInfoVO.queryAfpiTime + cashLoanOrderLimitConfig.getAfpiDataValidTime(), loanAccountId,
        orderOverLimitLevel, cashLoanOrderLimitConfig.getOrderLimitCacheExpireTime());
    return orderOverLimitLevel;
  }

  private void cacheQueryResultWithQueryError(Long loanAccountId) {
    cacheQueryResult(Clock.now() + cashLoanOrderLimitConfig.getQueryFailOrDefaultCacheTime() * Clock.MILLS_PER_SECOND,
        loanAccountId, OrderOverLimitLevel.LIMIT_NONE, cashLoanOrderLimitConfig.getQueryFailOrDefaultCacheTime());
  }

  private UserAfpiLoanInfoVO getAfpiLoanInfo(Long loanAccountId) {
    return afpiService.getUserLoanAmountAndCompanies(loanAccountId);

  }

  private int calculateOrderPlatformNumber(boolean ecHasOrder, UserAfpiLoanInfoVO afpiLoanInfoVO) {
    return ecHasOrder || afpiLoanInfoVO.containsCompany(EC_COMPANY_IDS) ? 0 : afpiLoanInfoVO.getCompanySize();
  }


  private void cacheQueryResult(long dataExpiredTime,
                                Long loanAccountId,
                                OrderOverLimitLevel limitLevel, int cacheExpiredTime) {
    orderLimitInfoLoader.set(loanAccountId.toString(), OrderLimitCacheInfo.from(limitLevel, dataExpiredTime));
    orderLimitInfoLoader.expire(loanAccountId.toString(), cacheExpiredTime);
  }


  private BigDecimal getUserEcAnnualAmount(Long loanAccountId) {
    LoanAccountDetailsVO detailsVO = accountDetailsService.getAuthFinishedDetailsVoByAccountId(loanAccountId);
    CashLoanEmploymentInfo cashLoanEmploymentInfo = detailsVO.getCashLoanEmploymentInfo();
    if (Objects.isNull(cashLoanEmploymentInfo)) {
      return BigDecimal.valueOf(Long.MAX_VALUE);
    }
    BigDecimal monthIncome = Objects.isNull(cashLoanEmploymentInfo.monthlyIncome) ? BigDecimal.valueOf(Long.MAX_VALUE) : new BigDecimal(cashLoanEmploymentInfo.monthlyIncome);
    return monthIncome.multiply(new BigDecimal(MONTHS_OF_YEAR));
  }

  @NotNull
  public UserOrderLimitVO getOrderLimitVO(Long loanAccountId) {
    try {
      return getOrderLimitInfo(loanAccountId);
    } catch (Exception e) {
      log.error("Error getting order limit prompt info for account {}", loanAccountId, e);
      return UserOrderLimitVO.from(UserOrderLimitType.LIMIT_TYPE_NONE, OrderOverLimitLevel.LIMIT_NONE);
    }
  }

  public String getPromptJsonInfo(UserOrderLimitType limitType, OrderOverLimitLevel orderOverLimitLevel) {
    return cashLoanOrderLimitConfig.getOrderLimitPrompt(orderOverLimitLevel, limitType);
  }


  /**
   * 根据用户的借款情况生成提示信息
   *
   * @param orderPlatformNum    除ec在贷平台数量
   * @param afpiLoanTotalAmount afpi总在贷金额
   * @param userEcAnnualAmount  用户ec填写的年收入
   * @return 用户提示信息
   */
  private OrderOverLimitLevel calcOrderLimitLevel(int orderPlatformNum, BigDecimal afpiLoanTotalAmount, BigDecimal userEcAnnualAmount) {
    // 负债比例
    double debtRatio = userEcAnnualAmount.compareTo(BigDecimal.ZERO) <= 0 ? 100 : afpiLoanTotalAmount.divide(userEcAnnualAmount, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    boolean isOverPlatformLimit = orderPlatformNum >= cashLoanOrderLimitConfig.getPlatformLimitNum();
    boolean isOverDebtRatioLimit = debtRatio > cashLoanOrderLimitConfig.getDebtRatioLimit();
    OrderOverLimitLevel limitCondition = cashLoanOrderLimitConfig.getLimitCondition();
    if (limitCondition == OrderOverLimitLevel.OVER_PLATFORM_NUM_LIMIT) {
      return isOverPlatformLimit ? OrderOverLimitLevel.OVER_PLATFORM_NUM_LIMIT : OrderOverLimitLevel.LIMIT_NONE;
    } else if (limitCondition == OrderOverLimitLevel.OVER_DEBT_RATIO_LIMIT) {
      return isOverDebtRatioLimit ? OrderOverLimitLevel.OVER_DEBT_RATIO_LIMIT : OrderOverLimitLevel.LIMIT_NONE;
    } else {
      if (isOverPlatformLimit && isOverDebtRatioLimit) {
        return OrderOverLimitLevel.OVER_PLATFORM_NUM_LIMIT_AND_OVER_DEBT_RATIO_LIMIT;
      } else if (isOverPlatformLimit) {
        return OrderOverLimitLevel.OVER_PLATFORM_NUM_LIMIT;
      } else if (isOverDebtRatioLimit) {
        return OrderOverLimitLevel.OVER_DEBT_RATIO_LIMIT;
      } else {
        return OrderOverLimitLevel.LIMIT_NONE;
      }
    }
  }

  public UserOrderLimitVO getOrderLimitVOBySourceTypeAndBuild(Long loanAccountId, Long build, SourceType sourceType, IDNHomepageLoanStatusV5 status) {
    if (build < cashLoanOrderLimitConfig.getOrderLimitVersion()
        || sourceType.createOrderWithoutLimitCheckSourceType()
        || !status.canCreateOrder()) {
      return UserOrderLimitVO.from(UserOrderLimitType.LIMIT_TYPE_NONE, OrderOverLimitLevel.LIMIT_NONE);
    }
    return getOrderLimitVO(loanAccountId);
  }
}
