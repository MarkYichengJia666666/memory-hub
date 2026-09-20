package com.yqg.core.service.loan.coupon.openappgrantcoupon;

import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.common.enums.UserFlowExperimentEnum;
import com.yqg.core.model.loader.AppStartUpGrantCouponCacheLoader;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.service.GrantCreditsLongTermExpService;
import com.yqg.core.service.abtest.AbstractExpClient;
import com.yqg.core.service.abtest.CouponLongTermExpService;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpDiversionDecision;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.h12026.ExpConditionFor2026H1Service;
import com.yqg.core.service.antisettlement.AntiSettlementLongTermExpService;
import com.yqg.core.service.antisettlement.AntiSettlementScene;
import com.yqg.core.service.app.vo.AppStartupAtLeastOnceVO;
import com.yqg.core.userflow.domain.coupon.model.AgentCouponDecision;
import com.yqg.core.userflow.domain.common.CouponAgentScene;
import com.yqg.core.userflow.domain.coupon.model.CouponDecision;
import com.yqg.core.userflow.domain.coupon.model.FirstLoanHeadMidBoostContext;
import com.yqg.core.userflow.domain.coupon.service.IBackToAppGrantCouponService;
import com.yqg.core.userflow.domain.coupon.service.IFirstLoanHeadMidBoostCouponService;
import com.yqg.core.userflow.domain.coupon.service.IUserGrantCouponStrategyService;
import com.yqg.core.userflow.domain.user.model.CrowdAfRankClassifier;
import com.yqg.core.userflow.domain.user.model.UserGroupInfo;
import com.yqg.core.userflow.domain.user.service.IUserInfoService;
import com.yqg.core.userflow.infrastructure.adapter.IBusiRiskAdapter;
import com.yqg.core.userflow.infrastructure.adapter.IExperimentAdapter;
import com.yqg.core.service.cashloan.enums.ReloanUserAfRankLevel;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.cashloan.risk.LoanRiskMetricService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.coupongrantrule.CouponGrantService;
import com.yqg.core.service.coupongrantrule.enums.CouponGrantRulePlatformType;
import com.yqg.core.service.experiment.RiskAcceptGrantCouponExpService;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.coupon.CouponMonitorService;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.erank.FirstLoanErankOfflineGrantCouponService;
import com.yqg.core.service.loan.coupon.enums.T0CouponGrantUserType;
import com.yqg.core.service.loan.coupon.openappgrantcoupon.vo.EnterExperimentNoGrantCouponLogVO;
import com.yqg.core.service.loan.coupon.openappgrantcoupon.vo.GrantCouponRequest;
import com.yqg.core.service.loan.coupon.openappgrantcoupon.vo.GrantCouponUserInfo;
import com.yqg.core.service.loan.creditsdetails.CreditLoanStatus;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.notif.CouponGrantConfig;
import com.yqg.core.service.notification.param.system.OpenAppParam;
import com.yqg.core.service.risk.riskoutput.RiskOutputService;
import com.yqg.core.service.risk.usergroup.RiskUserGroupEntranceService;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.service.sourcetype.SourceTypeService;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.ec.common.enums.NotifCouponGrantTaskSourceType;
import com.yqg.ec.common.constant.ExperimentKeyConstants;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OpenAppGrantCouponService {

  private static final String ANTI_SETTLEMENT_RATE_TAG_R01 = "R01";
  private static final String ANTI_SETTLEMENT_RATE_TAG_R02 = "R02";
  private static final String ANTI_SETTLEMENT_RATE_TAG_R03 = "R03";
  /**
   * 首贷 T1 头中部回端加码发券实验批次号前缀，配合 userId、ruleConfigId、时间戳形成唯一 batchNo。
   */
  private static final String FIRST_LOAN_HEAD_MID_BOOST_BATCH_NO_PREFIX =
      "tn_first_loan_head_mid_boost_";

  /**
   * 首贷 T1 头中部回端加码发券实验专用缓存 TTL（6 小时）。与本次发券的 3h 有效期匹配，
   * 允许同一用户在券失效后（跨过 3h 且未消耗完 6h TTL 前一小段时间）不重复触发本实验发券；
   * 6h 覆盖 3h 券有效期 + 用户复访窗口，避免过短 TTL 导致重发、过长 TTL 拖住实验期外行为。
   */
  private static final int FIRST_LOAN_HEAD_MID_BOOST_CACHE_EXPIRE_SECONDS = 6 * 60 * 60;

  @Autowired
  private SourceTypeService sourceTypeService;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  @Autowired
  private HomepageStatusTool homepageStatusTool;
  @Autowired
  private RiskUserGroupEntranceService riskUserGroupEntranceService;
  @Autowired
  private LoanCreditsQuotaService loanCreditsQuotaService;
  @Autowired
  private ExpConditionFor2026H1Service expConditionFor2026H1Service;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private CouponGrantConfig couponGrantConfig;
  @Autowired
  private AppStartUpGrantCouponCacheLoader appStartUpGrantCouponCacheLoader;
  @Autowired
  private CouponLongTermExpService couponLongTermExpService;
  @Autowired
  private CouponGrantService couponGrantService;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private RiskAcceptGrantCouponExpService riskAcceptGrantCouponExpService;
  @Autowired
  private GrantCreditsLongTermExpService grantCreditsLongTermExpService;
  @Autowired
  private RiskOutputService riskOutputService;
  @Autowired
  private CouponMonitorService couponMonitorService;
  @Autowired
  private LoanRiskMetricService loanRiskMetricService;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private IUserGrantCouponStrategyService userGrantCouponStrategyService;
  @Autowired
  private IExperimentAdapter experimentAdapter;
  @Autowired
  private IBusiRiskAdapter busiRiskAdapter;
  @Autowired
  private IUserInfoService userInfoService;
  @Autowired
  private AntiSettlementLongTermExpService longTermExpService;
  @Autowired
  private IBackToAppGrantCouponService backToAppGrantCouponService;
  @Autowired
  private FirstLoanErankOfflineGrantCouponService firstLoanErankOfflineGrantCouponService;
  @Autowired
  private IFirstLoanHeadMidBoostCouponService firstLoanHeadMidBoostCouponService;


  public OpenAppParam buildOpenAppGrantCouponParam(GrantCouponRequest request) {
    Long userId = request.userId;
    GrantCouponUserInfo grantCouponUserInfo = buildUserInfo(userId, request.buildVersion);
    String injectCouponAbResult = canGrantCreditsCouponByBusinessCondition(userId, request.buildVersion,
        request.getSdkType(), grantCouponUserInfo) ? "B" : "A";
    SourceType sourceType = sourceTypeService.getCurrentOrRegisterSourceType(userId);
    Boolean lowReloanFeeWill = loanUserRiskTraceService.isLowReloanFeeWill(request.loanAccountId);
    Boolean issueCoupon = true;
    Boolean shouldGrantCoupon = !RequestClientType.isWholeProcess(request.requestClientType);

    return new OpenAppParam(userId, SDKType.IDN_YQD, request.deviceToken, request.openAppScene, request.openAppPage, request.businessId,
        request.buildVersion, sourceType, grantCouponUserInfo.riskTraceVO, grantCouponUserInfo.reloan, grantCouponUserInfo.canOrder, lowReloanFeeWill, issueCoupon, shouldGrantCoupon,
        grantCouponUserInfo.userGroup, grantCouponUserInfo.riskUserLevel, grantCouponUserInfo.getRemainingCreditsForVirtual(), injectCouponAbResult);
  }

  private String getResultForCreditCouponAb(Long userId, SDKType sdkType, Long build, boolean isReloan) {
    String result = "A";
    if (isReloan) {
      result = expDiversionClient.getResult("reloan_order_26h1-not_withdraw-abroad-loan_all-reloan_limit_coupon_V1", ExpUser.builder().userId(userId).versionBuild(build).build());
    } else if (expConditionFor2026H1Service.matchesFirstLoanLaneCondition(userId, sdkType, build)) {
      result = expDiversionClient.getResult("first_loan_product_26h1-lending-abroad-loan-limit_coupon_V1", ExpUser.builder().userId(userId).versionBuild(build).build());
    }
    // 防御性兜底：当实验返回为空或空白组时，统一默认到"A"
    if (StringUtils.isBlank(result) || StringUtils.equals(result, AbstractExpClient.BLANK_GROUP)) {
      return "A";
    }
    return result;
  }

  private GrantCouponUserInfo buildUserInfo(Long userId, Long build) {
    Long loanAccountId = loanAccountService.getAccountIdByUserId(userId, SDKType.IDN_YQD);
    boolean reloan = loanAccountBasicInfoService.isReloan(loanAccountId);
    IDNHomepageLoanStatusV5 homepageLoanStatus = homepageStatusTool.getStatusByUserId(userId, build, SDKType.IDN_YQD);
    CreditLoanStatus creditLoanStatus = CreditLoanStatus.forHomepageLoanStatus(homepageLoanStatus);

    LoanRiskUserGroupVO loanRiskUserGroupVO = riskUserGroupEntranceService.getLoanRiskUserGroupVOByAccountIdOrNull(loanAccountId);
    String riskUserLevel = loanUserRiskTraceService.findLatestByLoanAccountIdAndTypeValue(loanAccountId, RiskOutputType.AF_RANK);
    RemainCreditsVO remainCreditsVO = loanCreditsQuotaService.getHomepageRemainCreditsByUserIdOrThrow(userId);
    LoanUserRiskTraceVO riskTraceVO = loanUserRiskTraceService.findLatestCalcCreditRiskByAccountId(loanAccountId);
    BigDecimal riskCouponLimitCredits = riskOutputService.getCouponLimitCreditsByUserId(userId);

    return GrantCouponUserInfo.builder()
        .loanAccountId(loanAccountId)
        .reloan(reloan)
        .riskTraceVO(riskTraceVO)
        .canOrder(creditLoanStatus == CreditLoanStatus.AVAILABLE)
        .homepageLoanStatus(homepageLoanStatus)
        .remainingCreditsForVirtual(Optional.ofNullable(remainCreditsVO).map(t -> t.remainingCreditsForVirtual).orElse(BigDecimal.ZERO))
        .creditsQuota(Optional.ofNullable(remainCreditsVO).map(t -> t.creditsInfoVO).map(t -> t.totalFixedCredits).orElse(BigDecimal.ZERO))
        .buildVersion(build)
        .userGroup(Optional.ofNullable(loanRiskUserGroupVO).map(t -> t.userGroup).orElse(null))
        .riskUserLevel(riskUserLevel)
        .riskCouponLimitCredits(riskCouponLimitCredits)
        .build();
  }

  /**
   * 注入额度券（首贷/复贷通用）
   * 延迟加载：只在缓存检查通过后才构建用户信息
   */
  public void grantCreditsCouponForOpenApp(AppStartupAtLeastOnceVO event, boolean isReloan, Long accountId) {
    try {
      // 先检查缓存，如果已经发过券，则不重复发
      if (isAlreadyGranted(event.getUserId(), isReloan, InjectCouponType.CREDIT_COUPON)) {
        return;
      }

      // 缓存检查通过后，再构建用户信息
      GrantCouponUserInfo grantCouponUserInfo = buildUserInfo(event.getUserId(), event.getAppBuild());
      log.info("User {} grantCreditsCouponForOpenApp, userInfo: {}", event.getUserId(), JsonUtils.toString(grantCouponUserInfo));

      if (!canGrantCreditsCouponByBusinessCondition(event.getUserId(), event.getAppBuild(), event.getSdkType(), grantCouponUserInfo)) {
        return;
      }

      // 触发分流(此处流量目前都来自Consumer)
      if (!grantCreditsLongTermExpService.canGrantAfterLongTermExpDiversion(event.getUserId(), event.getSdkType(),
          event.getAppBuild(), NotifCouponGrantTaskSourceType.OPEN_APP_GRANT_COUPON)) {
        return;
      }

      Long couponId = getCouponRuleId(event.getUserId(), accountId, isReloan, grantCouponUserInfo.riskUserLevel, event.getSdkType(), event.getAppBuild());
      if (Objects.isNull(couponId)) {
        return;
      }

      if (grantCouponByRuleId(event.getUserId(), couponId, isReloan, InjectCouponType.CREDIT_COUPON)) {
        couponMonitorService.logOpenAppGrantCoupon(event.getUserId(), couponId,Clock.now() - event.getTimestamp(), InjectCouponType.CREDIT_COUPON);
      }
    } catch (Exception e) {
      log.error("doInjectCreditsCoupon error, userId:{}, isReloan:{}, event:{}",
          event.getUserId(), isReloan, JsonUtils.toString(event), e);
    }
  }

  public boolean canGrantCreditsCouponByBusinessCondition(Long userId, Long build, SDKType sdkType,GrantCouponUserInfo grantCouponUserInfo) {
    if (!grantCouponUserInfo.canGrantCreditsCoupon()) {
      log.info("User {} can not grant CreditsCoupon, userInfo: {}", userId, JsonUtils.toString(grantCouponUserInfo));
      return false;
    }

    if (!canGrantCreditsCouponForReloanHomeStatusAndRiskLimit(userId, build, grantCouponUserInfo)) {
      log.info("User {} can not grant CreditsCoupon by homeStatus and riskLimit, userInfo: {}", userId, JsonUtils.toString(grantCouponUserInfo));
      return false;
    }

    if (!shouldGrantCreditsCouponByAbTest(userId, sdkType, build, grantCouponUserInfo.reloan)) {
      log.info("User {} can not grant CreditsCoupon, get credits long term coupon result is false", userId);
      return false;
    }
    return true;
  }

  private boolean canGrantCreditsCouponForReloanHomeStatusAndRiskLimit(Long userId, Long build, GrantCouponUserInfo grantCouponUserInfo) {
    if (!grantCouponUserInfo.isReloan()) {
      return true;
    }
    ExpUser expUser = ExpUser.builder().userId(userId).versionBuild(build).build();
    // 若首页状态为“有在贷授信通过但不可借”，则通过“复贷登录APP静默时发临额-放开下单资格限制2.0”实验来判断是否可以给用户发券
    if (grantCouponUserInfo.homepageLoanStatus.acceptedButCanNotLoan()) {
      String expResult = expDiversionClient.getResult(UserFlowExperimentEnum.RELOAN_LIMIT_COUPON_NO_LIMIT_V2.getKey(), expUser);
      if (!UserFlowConstants.isExperimentGroup(expResult)) {
        return false;
      }
    }
    if (!CrowdAfRankClassifier.RELOAN_NORMAL.isHead(grantCouponUserInfo.riskUserLevel)) {
      return true;
    }
    // 复贷头部人群根据实验判断是否必须有风控上限输出
    if (Objects.isNull(grantCouponUserInfo.riskCouponLimitCredits) || BigDecimalHelper.lessThanOrEqual(grantCouponUserInfo.riskCouponLimitCredits, BigDecimal.ZERO)) {
      String expResult = expDiversionClient.getResult(UserFlowExperimentEnum.RELOAN_LIMIT_COUPON_NO_LIMIT_V3.getKey(), expUser);
      if (!UserFlowConstants.isExperimentGroup(expResult)) {
        return false;
      }
    }
    return true;
  }

  private Long getCouponRuleId(Long userId, Long accountId, Boolean isReloan, String riskUserLevel, SDKType sdkType, Long build) {
    // 触发分流(此处流量目前都来自Consumer)
    String couponCreditsLimitAbResult = loanUserCouponService.triggerAbtestForCouponCreditsLimit(userId, accountId, sdkType, build);
    //非头部用户如果命中临额上限子实验对照组，不发券
    if (StringUtils.equals("A", couponCreditsLimitAbResult) && ReloanUserAfRankLevel.isMiddle(riskUserLevel)) {
      EnterExperimentNoGrantCouponLogVO experimentNoGrantCouponLogVO = EnterExperimentNoGrantCouponLogVO.from(userId, Clock.now(),
          NotifCouponGrantTaskSourceType.OPEN_APP_GRANT_COUPON.name);
      DwLogUtil.newLog(LogBusinessType.ENTER_EXPERIMENT_NO_GRANT_CREDITS_COUPON, experimentNoGrantCouponLogVO);
      log.info("User {} can not grant CreditsCoupon for middle, couponCreditsLimitAbResult: {}", userId, couponCreditsLimitAbResult);
      return null;
    }
    if (StringUtils.equals(couponCreditsLimitAbResult, "B")) {
      return couponGrantConfig.getCreditCouponIdForRiskCouponCreditsLimit(isReloan);
    }
    return couponGrantConfig.getCreditCouponId(isReloan);
  }

  /**
   * 根据AB测试结果判断是否应该发券
   */
  private boolean shouldGrantCreditsCouponByAbTest(Long userId, SDKType sdkType, Long build, boolean isReloan) {
    String abResult = getResultForCreditCouponAb(userId, sdkType, build, isReloan);
    return !StringUtils.equals(abResult, "A");
  }

  /**
   * 复贷降息券发放
   * 延迟加载：只在缓存检查通过后才构建用户信息
   */
  private void doInjectCutInterestCouponForReloan(AppStartupAtLeastOnceVO event) {
    try {
      List<Long> couponRuleIds = getCutInterestCouponRuleIds(event);
      if (CollectionUtils.isEmpty(couponRuleIds)) {
        return;
      }

      couponRuleIds.forEach(couponRuleId -> tryGrantCutInterestCoupon(event, couponRuleId));
    } catch (Exception e) {
      log.error("doInjectCutInterestCouponForReloan error, userId:{}, event:{}",
          event.getUserId(), JsonUtils.toString(event), e);
    }
  }

  private void tryGrantCutInterestCoupon(AppStartupAtLeastOnceVO event, Long couponRuleId) {
    try {
      if (grantCouponByRuleId(event.getUserId(), couponRuleId, true, InjectCouponType.CUT_INTEREST_COUPON)) {
        couponMonitorService.logOpenAppGrantCoupon(
            event.getUserId(),
            couponRuleId,
            Clock.now() - event.getTimestamp(),
            InjectCouponType.CUT_INTEREST_COUPON);
      }
    } catch (Exception e) {
      log.error("grant reloan cut-interest coupon failed, userId:{}, couponRuleId:{}, event:{}",
          event.getUserId(), couponRuleId, JsonUtils.toString(event), e);
    }
  }
  /**
   * 获取复贷开 App 待发放降息券 ruleId 列表（防结清静默灌券 V1 + 13752 头部低意愿）。
   * <p>共用 {@link InjectCouponType#CUT_INTEREST_COUPON} 幂等 cache，满足条件时合并返回一并发券。
   */
  private List<Long> getCutInterestCouponRuleIds(AppStartupAtLeastOnceVO event) {
    if (isAlreadyGranted(event.getUserId(), true, InjectCouponType.CUT_INTEREST_COUPON)) {
      return Collections.emptyList();
    }
    GrantCouponUserInfo userInfo = buildUserInfo(event.getUserId(), event.getAppBuild());
    log.info("User {} getCutInterestCouponId, userInfo: {}", event.getUserId(), JsonUtils.toString(userInfo));

    // 公共硬门闸：reloan(复贷) + canOrder(可借) + !wholeProcess + !apiChannel
    if (!userInfo.isReloan()
        || !userInfo.isCanOrder()
        || RequestClientType.isWholeProcess(event.getRequestClientType())
        || isApiChannelUserBySourceType(event.getSourceType())) {
      return Collections.emptyList();
    }

    List<Long> grantCouponIds = new ArrayList<>();

    Long antiSettlementRuleId = resolveAntiSettlementSilentCouponRuleId(event, userInfo);
    if (antiSettlementRuleId != null) {
      grantCouponIds.add(antiSettlementRuleId);
    }

    List<Long> headCouponIds = resolveReloanHeadCutInterestCouponIds(event, userInfo);
    if (CollectionUtils.isNotEmpty(headCouponIds)) {
      grantCouponIds.addAll(headCouponIds);
    }

    // 复贷常规-头/中部灌首期无门槛券v1
    List<Long> headMidV1RuleIdList = backToAppGrantCouponService.getHeadMidV1CouponRuleIdList(event, userInfo.getLoanAccountId(), grantCouponIds);
    if (CollectionUtils.isNotEmpty(headMidV1RuleIdList)) {
      grantCouponIds.addAll(headMidV1RuleIdList);
    }

    return grantCouponIds;
  }

  /**
   * 复贷头部低意愿灌券（13752）：门闸 + 实验入组后解析 ruleId 列表；未满足则返回 emptyList。
   * <p>公共硬门闸（reloan + canOrder + !wholeProcess + !apiChannel）已在 {@link #getCutInterestCouponRuleIds} 入口前置；
   * 本方法内聚：头部人群口径 → {@link AntiSettlementLongTermExpService#isHoldoutControl} 只读防污染
   * → {@link UserFlowExperimentEnum#RELOAN_TOP_WILLING_COUPON_V2} 分流 → 按组解析 toolId。
   */
  private List<Long> resolveReloanHeadCutInterestCouponIds(AppStartupAtLeastOnceVO event, GrantCouponUserInfo userInfo) {
    Long accountId = userInfo.getLoanAccountId();
    if (!loanRiskMetricService.isNormalUser(accountId)
        || !loanRiskMetricService.isReloanHeadCrowd(accountId)
        || !Boolean.TRUE.equals(loanUserRiskTraceService.isLowReloanFeeWill(accountId))) {
      log.info("User {} does not meet reloan head low-will coupon conditions, userInfo: {}",
          event.getUserId(), JsonUtils.toString(userInfo));
      return Collections.emptyList();
    }

    if (longTermExpService.isHoldoutControl(event.getUserId())) {
      return Collections.emptyList();
    }

    ExpUser expUser = ExpUser.builder().userId(event.getUserId())
        .sourceType(event.getSourceType()).versionBuild(event.getAppBuild()).build();
    String parentResult = expDiversionClient.getResult(
        UserFlowExperimentEnum.RELOAN_TOP_WILLING_COUPON_V2.getKey(), expUser);
    if (!UserFlowConstants.isExperimentGroup(parentResult)) {
      return Collections.emptyList();
    }

    if (isInAgentStrategyBucket(event.getUserId())) {
      return resolveAgentStrategyCouponIds(event.getUserId(), CouponAgentScene.INSTANT_GRANT);
    }
    return resolveV3StaticCouponIds(parentResult, userInfo);
  }

  /**
   * 是否进入 Agent 圈人策略桶：策略总开关开启 + Agent 子实验命中实验组。
   *
   * <p>开关关闭时直接返回 false，<b>不调子实验 {@code getResult}</b>——避免在策略全量关闭/灰度回滚期间
   */
  private boolean isInAgentStrategyBucket(Long userId) {
    if (!userGrantCouponStrategyService.isSceneStrategyEnabled(CouponAgentScene.INSTANT_GRANT)) {
      return false;
    }
    String abTest = experimentAdapter.abTestByUserId(UserFlowExperimentEnum.RELOAN_AGENT_COUPON_V1_1_EXCLUDE.getKey(), userId);
    return UserFlowConstants.isExperimentGroup(abTest);
  }

  private List<Long> resolveV3StaticCouponIds(String parentResult, GrantCouponUserInfo userInfo) {
    BigDecimal availableQuota = userInfo.getRemainingCreditsForVirtual();
    if (UserFlowConstants.EXPERIMENT_GROUP_ONE.equals(parentResult)) {
      List<CouponGrantConfig.QuotaRange> ranges = couponGrantConfig.getReloanTopWillingV3GroupOneQuotaRanges();
      Long toolId = matchCouponToolIdByQuotaOrLog(availableQuota, ranges, parentResult);
      return toolId == null ? Collections.emptyList() : Collections.singletonList(toolId);
    }
    if (UserFlowConstants.EXPERIMENT_GROUP_TWO.equals(parentResult)) {
      Long fixed = couponGrantConfig.getReloanTopWillingV3GroupTwoFixedCouponToolId();
      if (fixed == null) {
        log.error("复贷头部低意愿用户灌券-策略V3 GROUP_TWO 固定 toolId 缺失, accountId:{}", userInfo.getLoanAccountId());
        return Collections.emptyList();
      }
      return Collections.singletonList(fixed);
    }
    if (UserFlowConstants.EXPERIMENT_GROUP_THREE.equals(parentResult)) {
      Long fixed = couponGrantConfig.getReloanTopWillingV3GroupThreeFixedCouponToolId();
      List<CouponGrantConfig.QuotaRange> ranges = couponGrantConfig.getReloanTopWillingV3GroupThreeQuotaRanges();
      Long quotaToolId = matchCouponToolIdByQuotaOrLog(availableQuota, ranges, parentResult);
      if (fixed == null || quotaToolId == null) {
        log.error("复贷头部低意愿用户灌券-策略V3 GROUP_THREE 配置不完整 (任一缺失则不发半个组合), fixed:{}, quota:{}, accountId:{}",
            fixed, quotaToolId, userInfo.getLoanAccountId());
        return Collections.emptyList();
      }
      return Arrays.asList(fixed, quotaToolId);
    }
    return Collections.emptyList();
  }

  private List<Long> resolveAgentStrategyCouponIds(Long userId, CouponAgentScene scene) {
    AgentCouponDecision decision = userGrantCouponStrategyService.resolveAgentCouponDecision(userId, scene);
    if (decision.getType() == AgentCouponDecision.Type.EXCLUDED) {
      couponMonitorService.logAgentGrantCouponStrategyExclude(
          userId, scene.getCode(), decision.getVersionId(), decision.getExclusionDt(),
          CouponMonitorService.AGENT_EXCLUDE_MONITOR_GROUP_NOT_APPLICABLE);
      log.info("Agent strategy excluded, userId:{}", userId);
      return Collections.emptyList();
    }
    if (decision.getType() == AgentCouponDecision.Type.GRANT) {
      return Collections.singletonList(decision.getCouponRuleId());
    }
    log.info("Agent strategy miss, userId:{}", userId);
    return Collections.emptyList();
  }

  /**
   * 包装 {@link #resolveCutInterestCouponToolIdByQuotaRanges}：把"额度非正 / 区间空 / 无匹配区间"等异常情况
   * 统一转为 null + log.error，避免直接抛 {@link EcException} 中断 {@link #doInjectCutInterestCouponForReloan} 链路。
   */
  private Long matchCouponToolIdByQuotaOrLog(BigDecimal availableQuota,
      List<CouponGrantConfig.QuotaRange> orderedRanges, String expResult) {
    if (CollectionUtils.isEmpty(orderedRanges)) {
      log.error("复贷头部低意愿用户灌券-策略V3 quota ranges missing, expResult:{}", expResult);
      return null;
    }
    if (availableQuota == null || availableQuota.compareTo(BigDecimal.ZERO) <= 0) {
      log.error("复贷头部低意愿用户灌券-策略V3 availableQuota invalid, expResult:{}, quota:{}", expResult, availableQuota);
      return null;
    }
    try {
      return resolveCutInterestCouponToolIdByQuotaRanges(availableQuota, orderedRanges);
    } catch (Exception e) {
      log.error("复贷头部低意愿用户灌券-策略V3 match quota range failed, expResult:{}, quota:{}", expResult, availableQuota, e);
      return null;
    }
  }

  /**
   * 按有序额度区间从大到小匹配 min，语义与 {@link com.yqg.core.service.cashloan.activity.ProductDetailPushCouponService#calculate} 一致。
   *
   * @param availableQuotaIDR 印尼盾可借额度，须为正数
   * @param orderedRanges     有序区间列表（通常按 min 升序配置）
   * @return 命中的发券工具 ID
   * @throws IllegalArgumentException 额度非正或区间列表为空
   * @throws IllegalStateException    某段 min 为空或无匹配区间
   */
  private Long resolveCutInterestCouponToolIdByQuotaRanges(
      BigDecimal availableQuotaIDR, List<CouponGrantConfig.QuotaRange> orderedRanges) {
    if (availableQuotaIDR == null || availableQuotaIDR.compareTo(BigDecimal.ZERO) <= 0) {
      throw EcException.error("印尼盾可借额度必须为正数");
    }
    if (CollectionUtils.isEmpty(orderedRanges)) {
      throw EcException.error("区间配置不能为空");
    }
    for (int i = orderedRanges.size() - 1; i >= 0; i--) {
      CouponGrantConfig.QuotaRange range = orderedRanges.get(i);
      if (range.getMin() == null) {
        throw EcException.error("区间min不能为空，配置：{}", range);
      }
      if (availableQuotaIDR.compareTo(range.getMin()) > 0) {
        return range.getCouponToolId();
      }
    }
    throw EcException.error("无对应发券工具ID，额度：{}, 配置: {}", availableQuotaIDR, JsonUtils.toString(orderedRanges));
  }

  public void grantCutInterestCouponForOpenApp(AppStartupAtLeastOnceVO event, Boolean isReloan) {
    if (isReloan) {
      doInjectCutInterestCouponForReloan(event);
    } else {
      doInjectCutInterestCouponForLoan(event);
    }
  }

  /**
   * 首贷降息券发放（头部高意愿和低意愿用户+中部低意愿和高意愿用户）
   *
   * <p>TN 回端发券逻辑说明：
   * <ol>
   *   <li>T0 过件后 10min 内，由延迟任务处理发券逻辑</li>
   *   <li>TN 回端发券只针对过件超过 10min 的用户，避免实验全量时对新过件用户立即发券</li>
   * </ol>
   *
   * <p>4.0 升级要点（与 T0 延迟 Job 共享判定语义、但分别落地以避免跨类引用私有方法）：
   * <ul>
   *   <li>新增 API 渠道排除：直接读 {@code event.getSourceType()}，VO 已携带零开销，
   *       不调 {@link SourceTypeService}（T0 Job 走 SourceTypeService 是因为延迟 Job 不持有 VO）</li>
   *   <li>新增日息门槛重校：与延迟 Job 同实现，NFR-001 视取数失败为未达标</li>
   *   <li>分流接口：{@code executeExpDiversion} 返回 {@link ExpDiversionDecision}；
   *       发券时把 {@code decision.getGroupResult()} 作为字符串入参传给
   *       {@link #grantCutInterestCouponForFirstLoan}（其签名保持 {@code String} 不变，与 TAPD-346834 兼容）</li>
   * </ul>
   */
  private void doInjectCutInterestCouponForLoan(AppStartupAtLeastOnceVO event) {
    try {
      Long userId = event.getUserId();
      Long accountId = loanAccountService.getAccountIdByUserId(userId, event.getSdkType());

      if (!canCreateOrder(accountId, event.getAppBuild(), event.getSdkType())) {
        return;
      }

      // 检查版本是否满足要求
      if (!riskAcceptGrantCouponExpService.isVersionEligible(event.getAppBuild(), event.getRequestClientType())) {
        return;
      }

      // API 渠道排除（FR-006 / NFR-002）：直接读 VO 已携带的 sourceType，无需走 SourceTypeService
      if (isApiChannelUserBySourceType(event.getSourceType())) {
        return;
      }

      // 日息门槛（FR-004 / NFR-001）：与延迟 Job 同实现；
      // 上移到本实验分流之前，作为「TAPD-1364147 首贷 T1 头中部回端加码发券」实验组
      // 与既有 T0/TN 四分类链路的共享护栏（spec §入组条件第 7 项）。
      if (!riskAcceptGrantCouponExpService.isMinDailyRateAbove(userId, event.getSdkType())) {
        return;
      }

      if (isAlreadyGranted(userId, false, InjectCouponType.CUT_INTEREST_COUPON)) {
        return;
      }

      // E 补发：独立 10min；放在全局 CUT_INTEREST cache 之后
      if (tryFirstLoanErankOpenAppGrant(userId, accountId, event)) {
        return;
      }

      // 检查用户过件时间是否已超过延迟时间（默认20分钟，头中部 TN）
      // 如果没有超过，则跳过TN回端发券，让T0延迟任务来处理
      if (!isAcceptTimeExceedThresholdByAccountId(accountId)) {
        log.info("AccountId {} accept time not exceed delay, skip TN grant coupon", accountId);
        return;
      }

      // 首贷 T1 头中部回端加码发券实验分流（TAPD-1364147，先于既有 T0/TN 四分类判定）。
      // 命中实验组则由本分支完成发券后早退；对照 / BLANK / fallback / 配置未生效等
      // 分支由 tryFirstLoanHeadMiddleBoost 返回 false，主流程继续走既有 T0/TN 链路。
      String afRank = loanUserRiskTraceService.findLatestByLoanAccountIdAndTypeValue(
          accountId, RiskOutputType.AF_RANK);

      if (tryFirstLoanHeadMiddleBoost(userId, accountId, event, afRank)) {
        return;
      }

      T0CouponGrantUserType userType = determineUserType(accountId);
      if (userType == null) {
        return;
      }

      if (!expConditionFor2026H1Service.matchesFirstLoanLaneCondition(userId, event.getSdkType(), event.getAppBuild())) {
        return;
      }

      ExpDiversionDecision decision = riskAcceptGrantCouponExpService.executeExpDiversion(userId, event.getAppBuild(), userType.getExpKey());
      if (riskAcceptGrantCouponExpService.shouldGrantCoupon(decision)) {
        grantCutInterestCouponForFirstLoan(userId, event.getSdkType(), event.getAppBuild(), userType, decision.getGroupResult());
      }
    } catch (Exception e) {
      log.error("doInjectCutInterestCouponForLoan error, userId:{}, event:{}",
          event.getUserId(), JsonUtils.toString(event), e);
    }
  }

  /**
   * 判定 TN 入口的用户是否为 API 渠道用户（{@link SourceType#isApiChannelSourceType()}=true，
   * 即基于 {@code API_CHANNEL_LIST} 的全量 API 渠道，4.0 入组前置统一排除）。
   *
   * <p><b>为何直接读 {@code event.getSourceType()}：</b>VO 已经在 Kafka 消息链路里携带 sourceType，
   * 直接复用即可，避免再调 {@link SourceTypeService#resolveSourceType}（多一次 Scope/DB 取数）。
   * 与 T0 延迟 Job 走 SourceTypeService 的差异是必要的（plan.md Decision 4）。
   *
   * <p><b>为何 null 视为非 API 渠道放行（NFR-002）：</b>同 T0 延迟 Job 一致，避免大面积漏发。
   *
   * @param sourceType VO 携带的 sourceType，允许 null
   * @return true-是 API 渠道用户（应排除）；false-非 API 渠道（继续后续流程）
   */
  private boolean isApiChannelUserBySourceType(SourceType sourceType) {
    boolean blocked = sourceType != null && sourceType.isApiChannelSourceType();
    if (blocked) {
      log.info("api channel user excluded from coupon for TN, sourceType={}", sourceType);
    }
    return blocked;
  }

  /**
   * 根据 accountId 识别用户类型（头部/中部 × 低意愿/高意愿）
   */
  private T0CouponGrantUserType determineUserType(Long accountId) {
    if (riskAcceptGrantCouponExpService.checkHeadLowWillByAccountId(accountId)) {
      return T0CouponGrantUserType.HEAD_LOW_WILL_USER;
    }
    if (riskAcceptGrantCouponExpService.checkHeadHighWillByAccountId(accountId)) {
      return T0CouponGrantUserType.HEAD_HIGH_WILL_USER;
    }
    if (riskAcceptGrantCouponExpService.checkMidLowWillByAccountId(accountId)) {
      return T0CouponGrantUserType.MID_LOW_WILL_USER;
    }
    if (riskAcceptGrantCouponExpService.checkMidHighWillByAccountId(accountId)) {
      return T0CouponGrantUserType.MID_HIGH_WILL_USER;
    }
    return null;
  }

  /**
   * 检查是否可以创建订单
   */
  private boolean canCreateOrder(Long accountId, Long build, SDKType sdkType) {
    IDNHomepageLoanStatusV5 homeStatus = homepageStatusTool.getStatus(accountId, build, sdkType);
    log.info("User homeStatus: {}, accountId: {}", homeStatus, accountId);
    return homeStatus.canCreateOrder();
  }

  /**
   * 检查首贷未下单用户最近的风控过件时间是否已超过阈值
   *
   * 用于TN回端发券时，确保只对过件超过阈值时间（默认20分钟）的用户发券，
   * 避免实验全量时，新过件用户在TN回端立即被发券，不满足"过件后10分钟没有创建订单才发券"的需求
   * 阈值设置为大于延迟任务时间（10分钟），确保延迟任务已执行完毕，考虑job执行延时
   *
   * @param accountId accountId
   * @return true-过件时间已超过阈值，false-未超过
   */
  private boolean isAcceptTimeExceedThresholdByAccountId(Long accountId) {
    try {
      LoanUserRiskTraceVO riskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountIdAndRiskTypes(accountId,
          LoanUserRiskType.getLoanCalcRiskType());
      if (riskTraceVO == null || riskTraceVO.timeUpdated == null) {
        log.info("riskTraceVO info or timeUpdated not found for accountId: {}", accountId);
        return false;
      }

      // 使用TN回端专用的阈值配置（默认20分钟），而不是延迟任务时间（10分钟）
      Long thresholdMs = couponGrantConfig.getTnAcceptTimeThresholdMs();
      Long currentTime = Clock.now();
      Long acceptTime = riskTraceVO.timeUpdated;

      boolean exceed = (currentTime - acceptTime) > thresholdMs;
      log.info("Check accept time exceed threshold, accountId: {}, acceptTime: {}, currentTime: {}, thresholdMs: {}, exceed: {}",
          accountId, acceptTime, currentTime, thresholdMs, exceed);
      return exceed;
    } catch (Exception e) {
      log.error("Check accept time exceed threshold error, accountId: {}", accountId, e);
      return false;
    }
  }

  /**
   * 首贷降息券发放（统一处理头部/中部、低意愿/高意愿四类用户）
   * <p>后置于子实验分流：子实验命中后，再由长期实验决定是否最终发券。
   */
  private void grantCutInterestCouponForFirstLoan(Long userId, SDKType sdkType, Long build, T0CouponGrantUserType userType, String expResult) {
    try {
      // 后置长期实验检查：命中实验组（FALSE）则不发券
      if (!CouponLongTermExpService.isAllowedByExp(couponLongTermExpService.getFirstLoanExpDiversionResult(userId, build))) {
        log.info("Grant cut interest coupon blocked by first loan long term experiment, userId={}", userId);
        return;
      }
      Long couponConfigId = riskAcceptGrantCouponExpService.resolveCouponConfigId(userId, userType, expResult);
      if (couponConfigId == null) {
        log.error("Coupon config not found for first loan, userType: {}, expResult: {}", userType, expResult);
        return;
      }

      // 幂等检查
      Pair<Boolean, Long> eligibilityCheck = loanUserCouponService.isUserEligibleToCouponByRuleConfig(userId, couponConfigId);
      if (!eligibilityCheck.getLeft()) {
        log.info("User {} already have valid coupon, couponId: {}", userId, eligibilityCheck.getRight());
        return;
      }

      NotifCouponGrantTaskSourceType sourceType = userType.tnSourceType;
      String batchNo = MessageFormat.format("tn_first_loan_grant_coupon_{0}_{1}_{2}_{3}",
          userType.name(), userId, couponConfigId, Clock.now());
      Pair<Boolean, Long> grantResult = loanUserCouponService.grantCouponOrGetExist(
          userId, couponConfigId, batchNo, sourceType);

      log.info("Grant cut interest coupon for first loan, success: {}, couponId: {}, userId: {}, userType: {}, couponConfigId: {}",
          grantResult.getLeft(), grantResult.getRight(), userId, userType, couponConfigId);

      if (grantResult.getLeft()) {
        appStartUpGrantCouponCacheLoader.setCacheByExpiredSeconds(userId, false, InjectCouponType.CUT_INTEREST_COUPON);
      }
    } catch (Exception e) {
      log.error("Grant cut interest coupon for first loan error, userId: {}, userType: {}, expResult: {}", userId, userType, expResult, e);
    }
  }

  /**
   * 检查是否已经发过券
   */
  private boolean isAlreadyGranted(Long userId, boolean isReloan, InjectCouponType couponType) {
    return appStartUpGrantCouponCacheLoader.exists(userId, isReloan, couponType);
  }

  /**
   * 首贷常规 E 打开 App 补发。独立 10min；调用方已先过全局 CUT_INTEREST cache。
   *
   * @param userId    用户 ID
   * @param accountId 账户 ID
   * @param event     打开 App 事件
   * @return true 表示已按 E 路径处理，调用方应 return
   */
  private boolean tryFirstLoanErankOpenAppGrant(Long userId, Long accountId,
      AppStartupAtLeastOnceVO event) {
    LoanUserRiskTraceVO riskTraceVO = loanUserRiskTraceService
        .findLatestCreditRiskByAccountIdAndRiskTypes(
            accountId, LoanUserRiskType.getLoanCalcRiskType());
    Long riskAcceptTime = riskTraceVO == null ? null : riskTraceVO.timeUpdated;
    return firstLoanErankOfflineGrantCouponService.tryGrantOnOpenApp(
        userId, accountId, event.getAppBuild(), riskAcceptTime, event.getSdkType());
  }

  /**
   * 首贷 T1 头中部回端加码发券实验（TAPD-1364147）分流入口。
   *
   * <p>命中实验组或完成幂等收敛时返回 {@code true}，让 {@link #doInjectCutInterestCouponForLoan}
   * 主流程早退；对照组 / BLANK / fallback / 前置未通过 / 配置未生效等场景返回 {@code false}，
   * 让主流程回落到既有 T0/TN 四分类链路。
   *
   * <p>发券失败一律 catch + log.error 并返回 {@code true}（视为已处理），避免同一用户同时被本实验
   * 与既有 T0/TN 链路发券。上游 {@link #doInjectCutInterestCouponForLoan} 已有一层 try/catch 兜底，
   * 本方法的 try/catch 只是为了保证「不回落既有链路」的语义。
   *
   * @return true 主流程需要 return；false 主流程继续走既有 T0/TN 链路
   */
  private boolean tryFirstLoanHeadMiddleBoost(Long userId, Long accountId,
      AppStartupAtLeastOnceVO event, String afRank) {
    try {
      // 1. 前置入组条件：头/中部客群 + 授信过件已跨自然日进入 T+1 及以后 + 首贷未完件泳道
      // 平台不做限制：本实验 iOS 与 Android 均可发券
      if (!CrowdAfRankClassifier.FIRST_LOAN_NORMAL.isHeadOrMiddle(afRank)) {
        return false;
      }
      if (!isRiskAcceptanceNaturalDayPassed(accountId)) {
        return false;
      }
      if (!expConditionFor2026H1Service.matchesFirstLoanLaneCondition(
          userId, event.getSdkType(), event.getAppBuild())) {
        return false;
      }

      // 2. 分流（走 userflow 防腐层 IExperimentAdapter，与其他 userflow 域实验保持同一接入姿势）
      // 底层调用实验中台 abTest；无效参数 / RPC 失败 / 白名单收敛统一返回空字符串
      String groupResult = experimentAdapter.abTestByUserId(
          ExperimentKeyConstants.FIRST_LOAN_T1_HEAD_MIDDLE_MORE_DISCOUNT_0707, userId);

      // 3. 只有命中 EXPERIMENT_GROUP 才进入本实验；其他分组（CONTROL / BLANK / 未识别 / 空串 fallback）
      // 一律 return false，让主流程回落既有 T0/TN 链路，符合 NFR-005 边界对称
      if (!UserFlowConstants.EXPERIMENT_GROUP.equals(groupResult)) {
        return false;
      }

      // 5. 配置未生效兜底
      CouponGrantConfig.FirstLoanHeadMiddleBoostConfig config =
          couponGrantConfig.getFirstLoanHeadMiddleBoostConfig();
      if (config == null) {
        log.info("[first-loan-head-mid-boost] config not loaded, userId:{}", userId);
        return false;
      }

      // 6. 跨本次 6 个 ruleConfigId 幂等：已持有本实验任一 ENABLED 券则不重发，但写入缓存防止
      // 跨自然日轮换 6 款券里的另一款。
      Set<Long> allRuleIds = config.allRuleIds();
      if (CollectionUtils.isEmpty(allRuleIds)) {
        log.error("[first-loan-head-mid-boost] allRuleIds empty, config:{}, userId:{}",
            JsonUtils.toString(config), userId);
        return false;
      }
      Pair<Boolean, Long> eligibility = loanUserCouponService.checkEligibleAcrossRuleIds(
          userId, new ArrayList<>(allRuleIds));
      if (!eligibility.getLeft()) {
        Long existingCouponId = eligibility.getRight();
        log.info("[first-loan-head-mid-boost] user already holds coupon in this experiment, "
                + "userId:{}, existingCouponId:{}", userId, existingCouponId);
        appStartUpGrantCouponCacheLoader.setCacheByExpiredSeconds(
            userId, false, InjectCouponType.CUT_INTEREST_COUPON,
            FIRST_LOAN_HEAD_MID_BOOST_CACHE_EXPIRE_SECONDS);
        return true;
      }

      // 7. 挑券：按用户历史所有降息券的最高上限分档（13823 基础规则）+ 是否命中 14004 换券条件，
      // 全部判定逻辑委托给领域服务；委托前先取历史最高减免上限，避免当前券被消耗/过期后误判为无券档。
      BigDecimal maxUpperLimit = loanUserCouponService.getHistoricalMaxDeductAmountMaxLimit(userId);
      FirstLoanHeadMidBoostContext context = FirstLoanHeadMidBoostContext.builder()
          .userId(userId).accountId(accountId).afRank(afRank).maxUpperLimit(maxUpperLimit)
          .appBuild(event.getAppBuild()).requestClientType(event.getRequestClientType()).sdkType(event.getSdkType())
          .config(config)
          .build();
      CouponDecision decision = firstLoanHeadMidBoostCouponService.resolveCouponRuleId(context);
      if (decision == null) {
        log.error("[first-loan-head-mid-boost] no rule matched, userId:{}, afRank:{}, "
                + "maxUpperLimit:{}, config:{}",
            userId, afRank, maxUpperLimit, JsonUtils.toString(config));
        return false;
      }

      // 8-9. 发券（现有幂等 API）+ 写缓存
      grantHeadMiddleBoostCoupon(userId, afRank, maxUpperLimit, decision.getRuleConfigId(), decision.getSourceType());
      return true;
    } catch (Exception e) {
      log.error("[first-loan-head-mid-boost] error, userId:{}, accountId:{}, afRank:{}",
          userId, accountId, afRank, e);
      // 已经进入本实验主流程时任何异常都视为「本实验已尝试处理」，避免同一用户被既有 T0/TN 链路重复发券。
      return true;
    }
  }

  /**
   * 判断用户最近一次授信过件（ACCEPTED）时刻是否已跨自然日进入 T+1 及以后（印尼时区）。
   *
   * <p>本方法只是 13823 场景下对 {@link IBusiRiskAdapter#findRiskAcceptanceNaturalDaysPassed} 原始天数差的
   * 一次阈值判断（阈值=1）；防腐器本身只负责取数与翻译，不承载具体业务阈值，天数差为 {@code null}
   * （未找到过件记录 / 未处于 ACCEPTED 状态）时按未达标处理。
   */
  private boolean isRiskAcceptanceNaturalDayPassed(Long accountId) {
    Integer daysPassed = busiRiskAdapter.findRiskAcceptanceNaturalDaysPassed(accountId);
    return daysPassed != null && daysPassed >= 1;
  }

  /**
   * 首贷 T1 头中部回端加码发券（13823）真正发放这一步：batchNo 拼装 + 幂等发券 + 成功后写缓存。
   * 从 {@link #tryFirstLoanHeadMiddleBoost} 原第 8-9 步逐行抽取，行为不变；
   * {@code ruleConfigId}/{@code sourceType} 可能是 13823 原挑出的档位，也可能已被 14004 换券判定替换为 2101。
   */
  private void grantHeadMiddleBoostCoupon(Long userId, String afRank, BigDecimal maxUpperLimit,
      Long ruleConfigId, NotifCouponGrantTaskSourceType sourceType) {
    String batchNo = FIRST_LOAN_HEAD_MID_BOOST_BATCH_NO_PREFIX + userId + "_"
        + ruleConfigId + "_" + Clock.now();
    Pair<Boolean, Long> grantResult = loanUserCouponService.grantCouponOrGetExist(
        userId, ruleConfigId, batchNo, sourceType);
    log.info("[first-loan-head-mid-boost] grant result:{}, userId:{}, ruleConfigId:{}, sourceType:{}, "
            + "afRank:{}, maxUpperLimit:{}",
        grantResult, userId, ruleConfigId, sourceType, afRank, maxUpperLimit);

    if (Boolean.TRUE.equals(grantResult.getLeft())) {
      appStartUpGrantCouponCacheLoader.setCacheByExpiredSeconds(
          userId, false, InjectCouponType.CUT_INTEREST_COUPON,
          FIRST_LOAN_HEAD_MID_BOOST_CACHE_EXPIRE_SECONDS);
    }
  }

  /**
   * 通过规则ID发券
   */
  private boolean grantCouponByRuleId(Long userId, Long couponId, boolean isReloan, InjectCouponType couponType) {
    String batchNo = couponId + "-" + userId + "_" + Clock.now();
    List<Long> grantResult = couponGrantService.grantCouponByRuleId(
        couponId, couponId,
        NotifCouponGrantTaskSourceType.OPEN_APP_GRANT_COUPON,
        null, CouponGrantRulePlatformType.LOAN, userId, batchNo, "OPEN_APP_GRANT_COUPON");

    if (CollectionUtils.isEmpty(grantResult)) {
      return false;
    }

    switch (couponType) {
      case CREDIT_COUPON:
        appStartUpGrantCouponCacheLoader.setCacheExpireAtEndOfDay(userId, isReloan, couponType);
        return true;
      case CUT_INTEREST_COUPON:
        appStartUpGrantCouponCacheLoader.setCacheByExpiredSeconds(userId, isReloan, couponType);
        return true;
      default:
        throw EcException.error("not support couponType: {}, userId is {}", couponType, userId);
    }
  }

  /**
   * 防结清静默灌券 V1（TAPD-1364841）：通过门闸后解析发券 ruleId；未满足则返回 null。
   */
  private Long resolveAntiSettlementSilentCouponRuleId(AppStartupAtLeastOnceVO event, GrantCouponUserInfo userInfo) {
    Long accountId = userInfo.getLoanAccountId();
    Long userId = event.getUserId();
    if (accountId == null) {
      return null;
    }
    if (!isAntiSettlementSilentCouponEligible(event, accountId)) {
      return null;
    }

    String subExpGroup = experimentAdapter.abTestByUserId(ExperimentKeyConstants.ANTI_SETTLEMENT_SILENT_COUPON_V1, userId);
    if (!UserFlowConstants.isExperimentGroup(subExpGroup)) {
      log.info("[anti-settlement-silent] skip by sub experiment, userId={}, group={}", userId, subExpGroup);
      return null;
    }

    // payoff_churn Agent 排除 gate（NFR-004/008）：所有参与 v2 分流的用户的排除决策；
    // 命中排除者提前 return（不发券、不入组长期实验 holdout）；
    // 未命中排除（MISS，payoff_churn 不读券配置故不会 GRANT）则继续走后移到此的长期实验判定。
    String payoffChurnGroup = resolvePayoffChurnAgentGroup(userId);
    if (payoffChurnGroup != null) {
      AgentCouponDecision decision = userGrantCouponStrategyService.resolveAgentCouponDecisionByGroup(
          userId, CouponAgentScene.PAYOFF_CHURN, payoffChurnGroup);
      if (decision.getType() == AgentCouponDecision.Type.EXCLUDED) {
        couponMonitorService.logAgentGrantCouponStrategyExclude(
            userId, CouponAgentScene.PAYOFF_CHURN.getCode(),
            decision.getVersionId(), decision.getExclusionDt(), payoffChurnGroup);
        log.info("[anti-settlement-silent] excluded by payoff_churn agent, userId={}, versionId={}",
            userId, decision.getVersionId());
        return null;
      }
    }

    // ★长期实验判定后移到排除 gate 之后（NFR-008）：命中排除者已在上方 return，不触达此处、不入组 holdout；
    // 对照组 / 未入桶 / 未命中排除者仍过长期实验，行为与上线前一致。
    // 长期实验入组统一走 AntiSettlementLongTermExpService（唯一入口，内置公共入组资格过滤）。
    if (!longTermExpService.passesHoldout(
        userId, accountId, AntiSettlementScene.SILENT_COUPON)) {
      return null;
    }

    return mapAntiSettlementRateTagToRuleId(busiRiskAdapter.getRateTag(accountId));
  }

  /**
   * 解析 payoff_churn v2 实验分组：scene 总开关关闭返回 {@code null}（不调 abTest）；
   * 否则返回 v2 experimentKey 分流字面量（可能为空字符串，代表未被本实验分流命中）。
   */
  private String resolvePayoffChurnAgentGroup(Long userId) {
    if (!userGrantCouponStrategyService.isSceneStrategyEnabled(CouponAgentScene.PAYOFF_CHURN)) {
      return null;
    }
    return experimentAdapter.abTestByUserId(
        UserFlowExperimentEnum.ANTI_SETTLEMENT_PAYOFF_CHURN_EXCLUDE_AGENT_V2.getKey(), userId);
  }

  /**
   * 防结清静默灌券 V1 子实验分流前门闸：公共入组资格
   * （{@link UserGroupInfo#antiSettlementCrowd()}）+ 非 R01（风控 RATE_TAG）。
   * RATE_TAG 缺失时放行，发券阶段按 Other 处理；模型分取不到时本项通过，不因取数失败拦截。
   */
  private boolean isAntiSettlementSilentCouponEligible(AppStartupAtLeastOnceVO event, Long accountId) {
    UserGroupInfo userGroupInfo = userInfoService.getUserGroupInfoByAccountId(accountId);
    if (!UserGroupInfo.antiSettlementCrowd(userGroupInfo)) {
      log.info("[anti-settlement-silent] skip by crowd, userId={}, accountId={}",
          event.getUserId(), accountId);
      return false;
    }
    String rateTag = busiRiskAdapter.getRateTag(accountId);
    if (ANTI_SETTLEMENT_RATE_TAG_R01.equals(rateTag)) {
      log.info("[anti-settlement-silent] skip by R01 rate tag, userId={}, accountId={}, rateTag={}",
          event.getUserId(), accountId, rateTag);
      return false;
    }
    return true;
  }

  /**
   * 风控计息类型标签（RATE_TAG）→ ruleId：R02/R03 直映射；缺失及其他非 R01 标签（含 R015、R99）归入 Other。
   */
  private Long mapAntiSettlementRateTagToRuleId(String rateTag) {
    if (ANTI_SETTLEMENT_RATE_TAG_R01.equals(rateTag)) {
      return null;
    }
    if (ANTI_SETTLEMENT_RATE_TAG_R02.equals(rateTag)) {
      return couponGrantConfig.getAntiSettlementSilentCouponR02RuleId();
    }
    if (ANTI_SETTLEMENT_RATE_TAG_R03.equals(rateTag)) {
      return couponGrantConfig.getAntiSettlementSilentCouponR03RuleId();
    }
    return couponGrantConfig.getAntiSettlementSilentCouponOtherRuleId();
  }
}
