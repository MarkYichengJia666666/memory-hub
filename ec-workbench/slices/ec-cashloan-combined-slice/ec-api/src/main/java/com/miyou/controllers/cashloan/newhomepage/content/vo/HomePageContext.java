package com.miyou.controllers.cashloan.newhomepage.content.vo;

import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.miyou.controllers.cashloan.credit.aware.CreditAwareStrategyExecutor;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageV3ExperimentContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.OrderPageExperimentContext;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.generated.tables.records.LoginStatusCacheRecord;
import com.yqg.core.model.sql.loan.creditsaware.strategy.CreditAwareStrategyApplyResult;
import com.yqg.core.model.sql.loan.creditsaware.strategy.FirstLoanPopupDecisionSnapshot;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.AutoJumpLevel2FrequencyLoader;
import com.yqg.core.service.cashloan.homepage.JumpAuthFrequencyLoader;
import com.yqg.core.service.cashloan.homepage.ReloanDefaultAmountService;
import com.yqg.core.service.cashloan.homepage.enums.HomePageScene;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomePageServiceManager;
import com.yqg.core.service.cashloan.homepage.vo.HomePageContextDataHolder;
import com.yqg.core.service.cashloan.homepage.vo.HomepagePrepareAbTestVO;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserCashLoanOrderContext;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.UserProductDetailVO;
import com.yqg.core.service.cashloan.ordercenter.orderlimit.vo.UserOrderLimitVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.experiment.enums.InterestSplitResGroup;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.mergeaccount.ChannelMergeInfoVO;
import com.yqg.core.service.user.UserService;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.i18n.time.Clock;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.slf4j.MDC;

/**
 * 该context封装一些基础的东西
 * 一些不是每个状态都需要的内容则使用懒加载的方式进行定义
 * 该类的字段都不允许在外层额外赋值，提供homePageContextHolder用于存储公共变量
 */
@Getter
@Slf4j
public class HomePageContext {
  //=================================================基础数据===========================================================
  @NotNull
  private SDKType sdkType = SDKType.IDN_YQD;
  @NotNull
  private IDNHomepageLoanStatusV5 status;
  @NotNull
  private HomePageType homePageType;
  @NotNull
  private HomePageScene homePageScene;
  @NotNull
  private UserDeviceContextVO userDeviceContextVO;
  @NotNull
  private HomePageServiceManager homePageServiceManager;

  // 只有NOT_LOGIN才会为null
  /**
   * 用户信息
   */
  @Nullable
  private LoanAccountVO loanAccountVO;

  /**
   * 订单信息
   */
  @Nullable
  private UserCashLoanOrderContext userCashLoanOrderContext;

  /**
   * 用户额度&风控信息
   */
  @NotNull
  private UserCreditsContext userCreditsContext;

  /**
   * 次对象为了适配老代码，不要在主动使用
   * 后续去除掉
   */
  @Deprecated
  @NotNull
  private HomepageUserParamsVO homepageUserParamsVO;

  private HomepagePrepareAbTestVO prepareAbTestVO;

  /**
   * 新首页实验结果
   */
  private HomepageV3ExperimentContext homepageV3ExperimentContext;

  /**
   * 下单页实验结果结构体
   */
  private OrderPageExperimentContext orderPageExperimentContext;

  /**
   * 实验分流结果
   */
  private Map<String, Object> resourceParamMap = new HashMap<>();

  private String triggerSource;

  private Boolean disableRefresh = false;

  public final AtomicBoolean loanMarketMainButtonReported = new AtomicBoolean(false);
  public final AtomicBoolean loanMarketGotoLinkReported = new AtomicBoolean(false);
  public final AtomicBoolean loanMarketCardReported = new AtomicBoolean(false);

  private HomePageContext() {
  }

  public static HomePageContext create(IDNHomepageLoanStatusV5 status,
                                       UserDeviceContextVO userDeviceContextVO,
                                       UserCreditsContext userCreditsContext,
                                       UserCashLoanOrderContext userCashLoanOrderContext,
                                       LoanAccountVO loanAccountVO,
                                       HomepagePrepareAbTestVO prepareAbTestVO,
                                       HomepageUserParamsVO paramsVO,
                                       HomePageType homePageType,
                                       HomePageScene homePageScene,
                                       HomePageServiceManager homePageServiceManager,
                                       HomepageV3ExperimentContext homepageV3ExperimentContext,
                                       String triggerSource,
                                       Boolean disableRefresh) {
    HomePageContext homePageContext = new HomePageContext();

    homePageContext.status = status;
    homePageContext.homePageServiceManager = homePageServiceManager;
    homePageContext.sdkType = userDeviceContextVO.getSdkType();
    homePageContext.userDeviceContextVO = userDeviceContextVO;
    homePageContext.loanAccountVO = loanAccountVO;
    homePageContext.userCreditsContext = userCreditsContext;
    homePageContext.userCashLoanOrderContext = userCashLoanOrderContext;
    homePageContext.prepareAbTestVO = prepareAbTestVO;
    homePageContext.homepageUserParamsVO = paramsVO;
    homePageContext.homePageType = homePageType;
    homePageContext.homePageScene = homePageScene;
    homePageContext.homepageV3ExperimentContext = homepageV3ExperimentContext;
    homePageContext.triggerSource = triggerSource;
    homePageContext.disableRefresh = disableRefresh;
    return homePageContext;
  }

  //=================================================基础数据===========================================================

  @Getter
  private HomePageContextDataHolder homePageContextHolder = new HomePageContextDataHolder();


  //=================================================懒加载内容===========================================================
  @NotNull
  private final LazyInitializer<FirstLoanPopupDecisionSnapshot> firstLoanPopupDecisionSnapshotLazyInitializer = new LazyInitializer<FirstLoanPopupDecisionSnapshot>() {
    @Override
    protected FirstLoanPopupDecisionSnapshot initialize() {
      return FirstLoanPopupDecisionSnapshot.compute(FirstLoanPopupDecisionSnapshot.ComputeRequest.builder()
          .homePageType(homePageType)
          .manager(homePageServiceManager)
          .sdkType(sdkType)
          .userId(getUserId())
          .loanAccountId(loanAccountVO == null ? null : loanAccountVO.id)
          .buildId(userDeviceContextVO.getBuild())
          .platformType(userDeviceContextVO.getPlatformType())
          .canCreateOrder(status.canCreateOrder())
          .requestClientType(userDeviceContextVO.getClientType())
          .sourceType(userDeviceContextVO.getSourceType())
          .deviceToken(userDeviceContextVO.getDeviceToken())
          .triggerSource(triggerSource)
          .build());
    }
  };

  @SneakyThrows
  public FirstLoanPopupDecisionSnapshot getFirstLoanPopupDecisionSnapshot() {
    return firstLoanPopupDecisionSnapshotLazyInitializer.get();
  }
  
  @NotNull
  private final LazyInitializer<UserProductDetailVO> userProductVO = new LazyInitializer<UserProductDetailVO>() {
    @Override
    protected UserProductDetailVO initialize() {
      return UserProductDetailVO.create(loanAccountVO,
          homePageServiceManager);
    }
  };

  @SneakyThrows
  public UserProductDetailVO getUserProductVO() {
    return userProductVO.get();
  }


  @NotNull
  private final LazyInitializer<UserOrderLimitVO> userOrderLimitVO = new LazyInitializer<UserOrderLimitVO>() {
    @Override
    protected UserOrderLimitVO initialize() {
      return UserOrderLimitVO.create(loanAccountVO.id,
          userDeviceContextVO.getBuild(), userDeviceContextVO.getSourceType(), status, homePageServiceManager);
    }
  };

  @SneakyThrows
  public UserOrderLimitVO getUserOrderLimitVO() {
    return userOrderLimitVO.get();
  }

  private final LazyInitializer<CreditAwareStrategyApplyResult> creditsDetailsAwarePopupType = new CreditAwareStrategyExecutor(this);

  @SneakyThrows
  public CreditAwareStrategyApplyResult getCreditsDetailsAwarePopupType() {
    return creditsDetailsAwarePopupType.get();
  }

  /**
   * {@link Optional} 包装避免 Commons {@link LazyInitializer} 在值为 null 时重复触发 {@link ReloanDefaultAmountService#calcDefaultAmount}。
   */
  private final LazyInitializer<Optional<BigDecimal>> reloanDefaultAmountLazyHolder = new LazyInitializer<Optional<BigDecimal>>() {
    @Override
    protected Optional<BigDecimal> initialize() {
      if (loanAccountVO == null) {
        return Optional.empty();
      }
      BigDecimal computed = homePageServiceManager.getReloanDefaultAmountService()
          .calcDefaultAmount(getUserId(), loanAccountVO.id, userDeviceContextVO.getBuild(), userDeviceContextVO.getSourceType(),
              getUserProductVO().getEnableVirtualCredits(), homePageType);
      return Optional.ofNullable(computed);
    }
  };

  /**
   * 复贷下单页默认金额（懒加载、单次请求内缓存）。
   *
   * @return 有默认金额时返回该值；无账户或服务返回 null 时返回 {@code null}
   */
  @SneakyThrows
  public BigDecimal getReloanDefaultAmount() {
    return reloanDefaultAmountLazyHolder.get().orElse(null);
  }

  //=================================================懒加载内容===========================================================


  public Long getLoanAccountId() {
    return Optional.ofNullable(loanAccountVO)
        .map(item -> item.id)
        .orElse(null);
  }

  public Long getUserId() {
    return Optional.ofNullable(loanAccountVO)
        .map(item -> item.userId)
        .orElse(null);
  }

  public Long getAppCurrentOpenTime() {
    return Optional.ofNullable(userDeviceContextVO)
        .map(UserDeviceContextVO::getAppCurrentOpenTime)
        .orElse(null);
  }

  public boolean newHomePageUI() {
    return Optional.ofNullable(prepareAbTestVO)
        .map(item -> item.homePageUiStrategy == HomeDisplayStrategy.B || item.homePageUiStrategy == HomeDisplayStrategy.C)
        .orElse(false);
  }

  public boolean homePageLevel2() {
    return homePageType == HomePageType.HOME_PAGE_FOR_LEVEL_2;
  }

  // 单次首页请求内 hitJumpLevel2Strategy 的一次性 memoize：本方法被 4 处消费方（响应写入 + 弹窗/资源位/Banner 抑制）
  // 调用，豁免判定含实验入组（abTest 写），须避免重复 RPC 与重复入组（TAPD-365607，NFR-001）。
  private Boolean jumpLevel2StrategyMemo;

  public boolean hitJumpLevel2Strategy() {
    if (jumpLevel2StrategyMemo != null) {
      return jumpLevel2StrategyMemo;
    }
    jumpLevel2StrategyMemo = computeHitJumpLevel2Strategy();
    return jumpLevel2StrategyMemo;
  }

  private boolean computeHitJumpLevel2Strategy() {
    // 首先检查用户是否在应该跳转的实验组中
    boolean shouldJump = Optional.ofNullable(prepareAbTestVO)
        .map(item -> item.homePageUiStrategy == HomeDisplayStrategy.C)
        .orElse(false);

    // 如果不在实验组中，直接返回false
    if (!shouldJump) {
      return false;
    }

    // 需要合并账号，不能自动跳转
    if (isChannelMergeNeeded(this)){
      return false;
    }

    // 这个服务由Spring注入，所以我们需要从应用上下文中获取
    AutoJumpLevel2FrequencyLoader autoJumpLevel2FrequencyLoader = homePageServiceManager.getAutoJumpLevel2FrequencyLoader();

    // 如果在实验组中，检查频率限制
    // 根据频率限制检查用户是否可以跳转
    Long appCurrentOpenTime = Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)
        ? AutoJumpLevel2FrequencyLoader.WHOLE_PROCESS_OPEN_TIME
        : getAppCurrentOpenTime();
    if (!autoJumpLevel2FrequencyLoader.canJump(getUserId(), appCurrentOpenTime)) {
      return false;
    }

    // 豁免短路（TAPD-365607）：现网既有抑制门槛（版本/渠道合并/频控）通过后，若当日命中方案二豁免规则且入组
    // 实验组，则本次不自动跳下单页（同时恢复弹窗/资源位常规体验）；未命中/对照/空白/分流失败均照跳。
    if (homePageServiceManager.getAutoJumpExemptionService().shouldExempt(getUserId())) {
      return false;
    }
    return true;
  }

  // 返回值为大Boolean 可能为null true false
  public Boolean isNeedOpenAuthentication(HomePageContext homePageContext) {
    try {
      // 渠道合并优先：需要合并时抑制自动跳转鉴权页，避免用户跳过合并弹窗
      if (isChannelMergeNeeded(homePageContext)) {
        log.info("isNeedOpenAuthentication false, channel merge needed, userId:{}", homePageContext.getUserId());
        return false;
      }

      JumpAuthFrequencyLoader jumpAuthFrequencyLoader = homePageServiceManager.getJumpAuthFrequencyLoader();
      // 限频不通过
      if (!jumpAuthFrequencyLoader.canJump(homePageContext.getUserId())) {
        log.info("isNeedOpenAuthentication false, canJump false, userId:{}", homePageContext.getUserId());
        return false;
      }

      String userToken = MDC.get("userToken");
      if (userToken == null) {
        return null;
      }
      SDKType sdkType = Optional.ofNullable(MDC.get("sdkType")).map(SDKType::valueOf).orElse(null);
      if (sdkType == null) {
        return null;
      }
      Long build = Optional.ofNullable(MDC.get("build")).map(Long::valueOf).orElse(null);
      if (build == null) {
        return null;
      }
      UserService userService = homePageServiceManager.getUserService();
      LoginStatusCacheRecord loginStatusCacheRecord = userService.fetchLoginStatus(userToken, sdkType, build);
      if (Objects.isNull(loginStatusCacheRecord)) {
        return null;
      }
      long afterLoginMillis = Clock.now() - loginStatusCacheRecord.getTimeLastLogin();
      // 登录后n秒内，不跳转
      HomepageV5Config homepageV5Config = homePageServiceManager.getHomepageV5Config();
      if (SECONDS.toMillis(homepageV5Config.jumpAuthAfterLoginSeconds()) > afterLoginMillis) {
        return null;
      }
      return true;
    } catch (Exception ex) {
      log.error("isNeedOpenAuthentication error, userId={}", homePageContext.getUserId(), ex);
      return null;
    }
  }

  private boolean isChannelMergeNeeded(HomePageContext homePageContext) {
    ChannelMergeInfoVO mergeInfo =
        homePageContext.getHomePageContextHolder().getChannelMergeInfoVO();
    return mergeInfo != null
        && mergeInfo.getNeedMergeAccount() == com.yqg.core.service.user.enums.ForceMergeAccountCheckResult.NEED_MERGE;
  }

  public BigDecimal minLoanAmount() {
    return BigDecimalHelper.max(userCreditsContext.getCreditsInfoVO().calcMinCanLoanCredits(homepageUserParamsVO.homeConfigVO), Optional.ofNullable(this.getUserProductVO().getMinAmount()).orElse(BigDecimal.ZERO));
  }

  public void putParam2ResourceMap(String key, Object value) {
    resourceParamMap.put(key, value);
  }

  public void buildAndSetOrderPageExperimentContext() {
    this.orderPageExperimentContext = this.buildOrderPageExpContext(
        this.homePageType,
        this.homepageUserParamsVO,
        this.userDeviceContextVO
    );
  }

  private OrderPageExperimentContext buildOrderPageExpContext(HomePageType homePageType, HomepageUserParamsVO paramsVO,
      UserDeviceContextVO userDeviceContextVO) {
    if (!this.homePageLevel2()) {
      return null;
    }

    OrderPageExperimentContext context = new OrderPageExperimentContext();

    context.setAmountInputAreaPatternExpResult(CommonABTestResultGroup.A);

    return context;
  }


  public void flushContext(RemainCreditsVO remainCreditsVO) {
    userCreditsContext.updateCreditsInfoVO(remainCreditsVO);
    homepageUserParamsVO.updateRemainCreditsVO(remainCreditsVO);
  }

}
