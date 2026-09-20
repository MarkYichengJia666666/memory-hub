package com.yqg.core.service.notif;

import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.NotifCouponGrantTaskRecord;
import com.yqg.core.model.sql.loan.coupon.enums.LoanCouponStatus;
import com.yqg.core.model.sql.notif.NotifCouponGrantTaskModel;
import com.yqg.core.model.sql.notif.enums.NotifCouponGrantTaskStatus;
import com.yqg.core.service.GrantCreditsLongTermExpService;
import com.yqg.core.service.abtest.CouponLongTermExpService;
import com.yqg.core.service.abtest.ExperimentPlatformClientService;
import com.yqg.core.service.abtest.enums.AbLaneType;
import com.yqg.core.service.abtest.h12026.ExpConditionFor2026H1Service;
import com.yqg.core.service.coupongrantrule.CouponExtraInfoFetcherFactory;
import com.yqg.core.service.coupongrantrule.CouponGrantMonitorService;
import com.yqg.core.service.coupongrantrule.CouponGrantRuleService;
import com.yqg.core.service.coupongrantrule.CouponGrantTaskLogService;
import com.yqg.core.service.coupongrantrule.enums.CouponGrantRulePlatformType;
import com.yqg.core.service.coupongrantrule.rules.BaseCouponGrantRule;
import com.yqg.core.service.coupongrantrule.vo.CouponGrantExtraInfoVO;
import com.yqg.core.service.coupongrantrule.vo.CouponGrantRuleVO;
import com.yqg.core.service.coupongrantrule.vo.ICouponGrantExtraInfo;
import com.yqg.core.service.financing.coupon.FinancingCouponService;
import com.yqg.core.service.financing.coupon.FinancingUserCouponService;
import com.yqg.core.service.financing.map.ExtraInterestRateFinUsageRuleMap;
import com.yqg.core.service.financing.map.MoneyGivenFinUsageRuleMap;
import com.yqg.core.service.financing.map.MoneyOffFinUsageRuleMap;
import com.yqg.core.service.financing.vo.FinancingCouponVO;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.loan.coupon.vos.PercentCutInterestCouponVO;
import com.yqg.core.service.loan.coupon.vos.PercentIncreaseCreditsCouponVO;
import com.yqg.core.service.loan.coupon.vos.UnifiedCreditsCouponVO;
import com.yqg.core.service.loan.coupon.vos.UnifiedCutInterestCouponVO;
import com.yqg.core.service.loan.coupon.vos.UnifiedMoneyOffCouponVO;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.creditsconfig.vo.MarketCreditsUpperBoundDetailsDwVO;
import com.yqg.core.service.loan.creditsconfig.vo.UpperBoundAmountVO;
import com.yqg.core.service.mc.MarketingCenterClientService;
import com.yqg.core.service.mc.vo.MarketingSceneSimpleVO;
import com.yqg.core.service.notif.locker.NotifCouponGrantTaskLocker;
import com.yqg.core.service.notif.vo.CouponExperimentResultVO;
import com.yqg.core.service.notif.vo.NotifCouponGrantTaskCondition;
import com.yqg.core.service.notif.vo.NotifCouponGrantTaskVO;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.InferredParams;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.ec.common.enums.LoanCouponUsageType;
import com.yqg.ec.common.enums.NotifCouponGrantTaskSourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.EcAsserts;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author fudongyi
 * @date 2022/7/15
 */
@Slf4j
@Service
public class NotifCouponGrantTaskService {

  @Autowired
  private NotifCouponGrantTaskModel notifCouponGrantTaskModel;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private CouponGrantTaskLogService couponGrantTaskLogService;
  @Autowired
  private ThreadTransactionalModel transactionalModel;
  @Autowired
  private CouponGrantRuleService couponGrantRuleService;
  @Autowired
  private CouponGrantMonitorService monitorService;
  @Autowired
  private FinancingUserCouponService financingUserCouponService;
  @Autowired
  private FinancingCouponService financingCouponService;
  @Autowired
  private NotifCouponGrantTaskLocker notifCouponGrantTaskLocker;
  @Autowired
  private MarketingCenterClientService marketingCenterClientService;
  @Autowired
  private CouponExtraInfoFetcherFactory couponExtraInfoFetcherFactory;
  @Autowired
  private GrantCreditsLongTermExpService grantCreditsLongTermExpService;
  @Autowired
  private CouponGrantConfig couponGrantConfig;
  @Autowired
  private CouponLongTermExpService couponLongTermExpService;
  @Autowired
  private ExperimentPlatformClientService experimentPlatformClientService;
  @Autowired
  private ExpConditionFor2026H1Service expConditionFor2026H1Service;
  @Autowired
  private UserService userService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private NotifCouponGrantDetailMonitorService couponGrantDetailMonitorService;

  private static final String MONITOR_STATUS_PREPARE = "prepare";
  private static final String MONITOR_STATUS_SUCCESS = "success";
  private static final String MONITOR_STATUS_FAILURE = "failure";
  private static final String MONITOR_STATUS_IGNORE = "ignore";
  private static final String MONITOR_STATUS_INVALID = "invalid";

  public NotifCouponGrantTaskVO createTask(Long ruleConfigId, Long sourceId, NotifCouponGrantTaskSourceType sourceType, Long mediumId,
                                           CouponGrantRulePlatformType platformType, Long userId, String batchNo, String extraData) {
    checkIsSupportedSourceType(sourceType, couponGrantRuleService.getEnabledOrThrowWarn(ruleConfigId, platformType, sourceType));

    NotifCouponGrantTaskRecord record = notifCouponGrantTaskModel.insert(ruleConfigId, sourceId, sourceType, mediumId, platformType, userId,
        batchNo, extraData);
    return NotifCouponGrantTaskVO.from(record);
  }

  public List<Long> grantCouponInstant(Long ruleConfigId, Long sourceId, NotifCouponGrantTaskSourceType sourceType, Long mediumId,
                                       CouponGrantRulePlatformType platformType, Long userId, String batchNo, String extraData,
      boolean canGrantCoupon) {
    NotifCouponGrantTaskVO task = createTask(ruleConfigId, sourceId, sourceType, mediumId, platformType, userId, batchNo, extraData);
    return doConsume(task, canGrantCoupon);
  }

  public List<NotifCouponGrantTaskVO> listByStatusAndLimit(NotifCouponGrantTaskStatus status, Integer limit) {
    return notifCouponGrantTaskModel.listByStatusAndLimit(status, limit).stream().map(NotifCouponGrantTaskVO::from)
        .collect(Collectors.toList());
  }

  public List<NotifCouponGrantTaskVO> listByRuleIdAndUserIdAndSourceIdAndSourceType(Long ruleId, Long userId, Long sourceId,
                                                                                    NotifCouponGrantTaskSourceType sourceType) {
    return notifCouponGrantTaskModel.listByRuleIdAndUserIdAndSourceIdAndSourceType(ruleId, userId, sourceId, sourceType).stream()
        .map(NotifCouponGrantTaskVO::from).collect(Collectors.toList());
  }

  public List<NotifCouponGrantTaskVO> listByIds(Collection<Long> taskIds) {
    return notifCouponGrantTaskModel.listByIds(taskIds).stream().map(NotifCouponGrantTaskVO::from).collect(Collectors.toList());
  }

  public List<NotifCouponGrantTaskVO> listByUserIdAndRuleConfigIdsAndBatchNos(
      Long userId,
      Collection<Long> ruleConfigIds,
      Collection<String> batchNos
  ) {
    if (CollectionUtils.isEmpty(ruleConfigIds) || CollectionUtils.isEmpty(batchNos)) {
      return Collections.emptyList();
    }
    return notifCouponGrantTaskModel.listByUserIdAndRuleConfigIdsAndBatchNos(userId, ruleConfigIds, batchNos)
        .stream()
        .map(NotifCouponGrantTaskVO::from)
        .collect(Collectors.toList());
  }

  public int updateStatusById(Long id, NotifCouponGrantTaskStatus status) {
    return notifCouponGrantTaskModel.updateStatusById(id, status);
  }

  public List<NotifCouponGrantTaskVO> listOrderedByPageSizeAndPageNoAndCondition(NotifCouponGrantTaskCondition condition, Integer pageSize,
                                                                                 Integer pageNo) {
    pageNo = Math.max(1, pageNo);
    Integer offset = (pageNo - 1) * pageSize;

    return notifCouponGrantTaskModel.listOrderedByLimitAndOffsetAndCondition(condition, pageSize, offset).stream()
        .map(NotifCouponGrantTaskVO::from).collect(Collectors.toList());
  }

  public Long getCountByCondition(NotifCouponGrantTaskCondition condition) {
    return notifCouponGrantTaskModel.getCount(condition);
  }

  public Map<Long, CouponGrantExtraInfoVO> fetchCouponGrantInfoMapByUserCouponIds(Collection<Long> userCouponIds,
                                                                                  CouponGrantRulePlatformType platformType) {
    Map<Long, Pair<Long, NotifCouponGrantTaskSourceType>> couponIdToTaskSourceInfoMap = notifCouponGrantTaskModel.fetchCouponIdToTaskIdMap(
        userCouponIds, platformType);

    Map<NotifCouponGrantTaskSourceType, List<Long>> sourceTypeToSourceIdListMap = couponIdToTaskSourceInfoMap.values().stream()
        .collect(Collectors.groupingBy(Pair::getValue, Collectors.mapping(Pair::getKey, Collectors.toList())));
    Map<Pair<Long, NotifCouponGrantTaskSourceType>, ICouponGrantExtraInfo> sourceExtraInfoIndex = sourceTypeToSourceIdListMap.entrySet()
        .stream().flatMap(entry -> couponExtraInfoFetcherFactory.batchFetchExtraInfo(entry.getValue(), entry.getKey()).entrySet().stream()
            .map(e -> Pair.of(e, entry.getKey())))
        .collect(Collectors.toMap(e -> Pair.of(e.getKey().getKey(), e.getValue()), e -> e.getKey().getValue()));

    return userCouponIds.stream().filter(couponIdToTaskSourceInfoMap::containsKey).map(e -> {
      Pair<Long, NotifCouponGrantTaskSourceType> sourceIdToSourceTypePair = couponIdToTaskSourceInfoMap.get(e);
      return Pair.of(e, new CouponGrantExtraInfoVO(Optional.ofNullable(sourceIdToSourceTypePair).map(Pair::getValue).orElse(null),
          Optional.ofNullable(sourceIdToSourceTypePair).map(Pair::getKey).orElse(null),
          sourceExtraInfoIndex.get(sourceIdToSourceTypePair)));
    }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }

  public void batchDoTask(List<NotifCouponGrantTaskVO> taskList) {
    if (CollectionUtils.isEmpty(taskList)) {
      return;
    }

    Map<String, List<NotifCouponGrantTaskVO>> batchHandleMap = taskList.stream().filter(Objects::nonNull)
        .filter(task -> task.status == NotifCouponGrantTaskStatus.INIT).collect(Collectors.groupingBy(e -> e.batchNo));
    batchHandleMap.forEach((batchNo, tList) -> tList.forEach(task -> {
      try {
        // todo 停止该job 后续删除代码
        doConsume(task, true);
      } catch (Throwable throwable) {
        log.error("执行发券Task失败. TASK_ID:{}", task.id, throwable);
      }
    }));
  }

  public NotifCouponGrantTaskVO getOrThrow(Long id) {
    NotifCouponGrantTaskRecord record = notifCouponGrantTaskModel.getById(id);
    if (Objects.isNull(record)) {
      throw EcException.error("NotifCouponGrantTaskVO is null. TASK_ID:{}", id);
    }
    return NotifCouponGrantTaskVO.from(record);
  }

  private List<Long> doConsume(NotifCouponGrantTaskVO task, boolean canGrantCoupon) {
    List<Long> couponRecordIds = null;
    CouponGrantRuleVO ruleVO = couponGrantRuleService.getOrThrow(task.ruleConfigId);

    CouponExperimentResultVO resultVO = CouponExperimentResultVO.from(true, false);

    Long startTime = Clock.now();
    try {
      couponRecordIds = notifCouponGrantTaskLocker.lockAndRunResult(task.id, () -> doGrantCoupon(task, resultVO, ruleVO));
      List<Long> finalCouponRecordIds = couponRecordIds;
      dwLogMarketCreditsDetail(finalCouponRecordIds, task.userId);
    } catch (Throwable e) {
      updateStatusById(task.id, NotifCouponGrantTaskStatus.EXCEPTION);
      log.warn("发券失败. TASK_ID:{}", task.id, e);
    } finally {
      Long endTime = Clock.now();
      // 优惠券打点
      logPoint(task, ruleVO, couponRecordIds, resultVO, endTime - startTime);
      couponGrantDetailMonitorService.submitLogDetailPoint(task, ruleVO, couponRecordIds, resultVO, endTime - startTime);
    }
    return couponRecordIds;
  }

  private List<Long> doGrantCoupon(NotifCouponGrantTaskVO task, CouponExperimentResultVO resultVO, CouponGrantRuleVO ruleVO) {
    return transactionalModel.transactionResult(configuration -> {
      NotifCouponGrantTaskVO taskVO = getOrThrow(task.id);
      if (taskVO.status != NotifCouponGrantTaskStatus.INIT) {
        return Collections.emptyList();
      }
      List<Long> couponIds = initGrantCouponIds(taskVO, resultVO, ruleVO);
      if (CollectionUtils.isEmpty(couponIds)) {
        updateStatusById(taskVO.id, NotifCouponGrantTaskStatus.FAIL);
      } else {
        couponGrantTaskLogService.batchCreate(taskVO.id, couponIds);
        updateStatusById(taskVO.id, NotifCouponGrantTaskStatus.SUCCESS);
      }
      return couponIds;
    });
  }

  private void dwLogMarketCreditsDetail(List<Long> couponIds, Long userId) {
    if (CollectionUtils.isEmpty(couponIds)) {
      return;
    }
    EcAsserts.assertTrue(couponIds.size() == 1, "grant coupon size must one, userId is {}, couponIds is {}", userId, couponIds);
    LoanUserCouponVO loanUserCouponVO = loanUserCouponService.findByIdWithConfig(couponIds.get(0));
    if (LoanCouponUsageType.CREDITS != loanUserCouponVO.usageType) {
      return;
    }
    if (LoanCouponStatus.USED != loanUserCouponVO.status) {
      return;
    }
    try {
      UpperBoundAmountVO upperBoundResult = loanUserCreditsService.getMarketingCouponCreditsUpperBoundByUserId(userId);
      if (Objects.isNull(upperBoundResult)) {
        log.warn("marketCreditsUpperBound is null, userId is {}", userId);
        return;
      }
      MarketCreditsUpperBoundDetailsDwVO marketCreditsUpperBoundDetailsDwVO = MarketCreditsUpperBoundDetailsDwVO.from(loanUserCouponVO.id, userId,
          loanUserCouponVO.allAffectedMoney, loanUserCouponVO.timeCreated, loanUserCouponVO.timeExpired, upperBoundResult.getCalcDetail());
      DwLogUtil.newLog(LogBusinessType.MARKET_CREDITS_DETAILS_WHEN_GRANT_COUPON, marketCreditsUpperBoundDetailsDwVO);
      log.info("dwLogMarketCreditsDetail, marketCreditsUpperBoundDetailsDwVO is {}", JsonUtils.toString(marketCreditsUpperBoundDetailsDwVO));
    } catch (Exception e) {
      log.error("dwLogMarketCreditsDetail error, userId is {}, couponIds is {}", userId, couponIds, e);
    }
  }

  private List<Long> initGrantCouponIds(NotifCouponGrantTaskVO taskVO, CouponExperimentResultVO resultVO, CouponGrantRuleVO ruleVO) {
    switch (taskVO.platformType) {
      case LOAN:
        if (!resultVO.isShouldGrantCoupon()) {
          return Collections.emptyList();
        }
        return grantLoanCoupon(taskVO, resultVO.isShouldGrantExpiredCoupon());
      case FINANCING:
        return grantFinancingCoupon(taskVO, ruleVO);
      default:
        throw EcException.error("未知的发券方式:{}", taskVO.platformType);
    }
  }


  public CouponExperimentResultVO checkCouponLongTermExperimentForUpper(Long ruleConfigId, Long userId, CouponGrantRulePlatformType platformType,
                                                                NotifCouponGrantTaskSourceType sourceType) {
    if (platformType != CouponGrantRulePlatformType.LOAN) {
      return CouponExperimentResultVO.from(true, false);
    }
    CouponGrantRuleVO ruleVO = couponGrantRuleService.getOrThrow(ruleConfigId);

    LoanCouponUsageType usageType = (LoanCouponUsageType) ruleVO.ruleConfigType.usageType;
    InferredParams inferredParams = userService.inferMissingParams(userId);
    switch (usageType) {
      case CREDITS:
        // 触发分流(此处流量来自B/C两端)
        if (grantCreditsLongTermExpService.canGrantAfterLongTermExpDiversionForLastResult(userId, inferredParams.getSdkType(),
            inferredParams.getBuild(), sourceType)) {
          return CouponExperimentResultVO.from(true, false);
        }
        switch (sourceType) {
          case NOTIF:
            return CouponExperimentResultVO.from(false, false);
          default:
            throw EcException.error("don't send expired coupon any more userId={}, configId={}, sourceType={}", userId, ruleConfigId, sourceType);
        }
      default:
        if (couponGrantConfig.shouldSkipByScenarios(ruleVO.scenarios)) {
          log.info("skip coupon experiment by scenarios whitelist, ruleConfigId={}, scenarios={}", ruleConfigId, ruleVO.scenarios);
          return CouponExperimentResultVO.from(true, false);
        }
        if (sourceType == NotifCouponGrantTaskSourceType.NOTIF) {
          // 首贷：命中首贷产品泳道后进入首贷优惠券长期子实验；复贷：复贷非回捞进入复贷长期实验（后置于各子实验发券判断）
          boolean res;
          if (expConditionFor2026H1Service.loanTimeLtOne(userId)) {
            res = couponLongTermExpService.canGrantCouponForFirstLoan(userId, inferredParams.getSdkType(), inferredParams.getBuild());
          } else {
            res = experimentPlatformClientService.laneAbTest(userId, AbLaneType.RELOAN_MARKETING.getLaneKey());
          }
          return CouponExperimentResultVO.from(res, false);
        } else {
          return CouponExperimentResultVO.from(true, false);
        }
    }
  }


  private List<Long> grantLoanCoupon(NotifCouponGrantTaskVO task, Boolean isCouponExpired) {
    Map<Long, List<Long>> userIdCouponIdsMap = loanUserCouponService.batchInsertByUserIdV2(task, isCouponExpired);
    return userIdCouponIdsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  private List<Long> grantFinancingCoupon(NotifCouponGrantTaskVO task, CouponGrantRuleVO ruleVO) {
    Map<Long, List<Long>> userIdCouponIdsMap = financingUserCouponService.batchInsertByUserIdsV2(ruleVO.id, task);
    return userIdCouponIdsMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  private String getSceneInfoByNotifLog(Long notifLogId) {
    MarketingSceneSimpleVO sceneSimpleVO = marketingCenterClientService.queryByNotifLogId(notifLogId);
    return sceneSimpleVO == null || sceneSimpleVO.sceneName == null ? "unknownScene" : sceneSimpleVO.sceneName;
  }

  private void logPoint(NotifCouponGrantTaskVO task, CouponGrantRuleVO ruleVO, List<Long> couponRecordIds, CouponExperimentResultVO resultVO,
      long cost) {
    if (CollectionUtils.isNotEmpty(couponRecordIds)) {
      switch (task.platformType) {
        case LOAN:
          logLoanCoupon(couponRecordIds, ruleVO, task, resultVO, cost);
          break;
        case FINANCING:
          logFinancingCoupon(couponRecordIds, ruleVO, task, resultVO, cost);
          break;
        default:
          log.error("未知的PlatformType,没做打点处理逻辑.PlatformType:{}", task.platformType);
          break;
      }
    }
  }

  private void logLoanCoupon(List<Long> couponIds, CouponGrantRuleVO ruleVO, NotifCouponGrantTaskVO taskVO, CouponExperimentResultVO resultVO,
      long cost) {
    Map<Long, LoanUserCouponVO> couponVOMap = loanUserCouponService.mapByIds(couponIds);
    List<CouponPoint> points = couponVOMap.values().stream().map(this::loanToCouponPoint).collect(Collectors.toList());
    doLogPoint(points, ruleVO, taskVO, couponVOMap.size(), resultVO, cost);
  }

  private void logFinancingCoupon(List<Long> couponIds, CouponGrantRuleVO ruleVO, NotifCouponGrantTaskVO taskVO, CouponExperimentResultVO resultVO,
      long cost) {
    List<CouponPoint> points = financingCouponService.listByIds(couponIds).stream().map(this::finToCouponPoint)
        .collect(Collectors.toList());
    doLogPoint(points, ruleVO, taskVO, couponIds.size(), resultVO, cost);
  }

  private void doLogPoint(List<CouponPoint> points, CouponGrantRuleVO ruleVO, NotifCouponGrantTaskVO taskVO, int size, CouponExperimentResultVO resultVO,
      long cost) {
    if (CollectionUtils.isEmpty(points)) {
      return;
    }
    String sourceName = taskVO.sourceType.code;
    CouponPoint point;
    if (CollectionUtils.size(points) > 1) {
      point = points.stream().reduce(CouponPoint.builder().build(), CouponPoint::add);
    } else {
      point = points.get(0);
    }

    monitorService.logCouponGrantInfo(ruleVO.ruleConfig.configType, point.amount, point.percent, sourceName, taskVO.sourceType,
        taskVO.platformType, size, cost);
    monitorService.logTaskExecution(ruleVO.ruleConfig.configType, sourceName, taskVO.sourceType, taskVO.platformType, MONITOR_STATUS_PREPARE, taskVO.id);
    String couponGrantMonitorStatus = getCouponGrantMonitorStatus(points, resultVO);
    monitorService.logTaskExecution(ruleVO.ruleConfig.configType, sourceName, taskVO.sourceType, taskVO.platformType, couponGrantMonitorStatus, taskVO.id);
  }

  private String getCouponGrantMonitorStatus(List<CouponPoint> points, CouponExperimentResultVO resultVO) {
    if (CollectionUtils.isEmpty(points)) {
      return resultVO.isShouldGrantCoupon() ? MONITOR_STATUS_FAILURE : MONITOR_STATUS_IGNORE;
    }
    return resultVO.isShouldGrantExpiredCoupon() ? MONITOR_STATUS_INVALID : MONITOR_STATUS_SUCCESS;
  }

  private CouponPoint finToCouponPoint(FinancingCouponVO couponVO) {

    switch (couponVO.usageType) {
      case MONEY_OFF:
        MoneyOffFinUsageRuleMap moneyOffFinUsageRuleMap = (MoneyOffFinUsageRuleMap) couponVO.usageRuleMap;
        return CouponPoint.builder().amount(moneyOffFinUsageRuleMap.deductAmount).build();
      case MONEY_GIVEN:
        MoneyGivenFinUsageRuleMap moneyGivenFinUsageRuleMap = (MoneyGivenFinUsageRuleMap) couponVO.usageRuleMap;
        return CouponPoint.builder().amount(moneyGivenFinUsageRuleMap.givenAmount).build();
      case EXTRA_INTEREST_RATE:
        ExtraInterestRateFinUsageRuleMap extraInterestRateFinUsageRuleMap = (ExtraInterestRateFinUsageRuleMap) couponVO.usageRuleMap;
        return CouponPoint.builder().percent(extraInterestRateFinUsageRuleMap.extraRate).build();
      default:
        log.error("礼金类型的理财优惠券不允许发送以及打点!");
        return null;
    }
  }

  private CouponPoint loanToCouponPoint(LoanUserCouponVO couponVO) {
    if (couponVO instanceof UnifiedCreditsCouponVO) {
      UnifiedCreditsCouponVO unifiedCreditsCouponVO = (UnifiedCreditsCouponVO) couponVO;
      return CouponPoint.builder().amount(unifiedCreditsCouponVO.acquireIncreaseCredits()).build();
    } else if (couponVO instanceof UnifiedMoneyOffCouponVO) {
      UnifiedMoneyOffCouponVO unifiedMoneyOffCouponVO = (UnifiedMoneyOffCouponVO) couponVO;
      return CouponPoint.builder().amount(unifiedMoneyOffCouponVO.deductAmount).build();
    } else if (couponVO instanceof PercentCutInterestCouponVO) {
      PercentCutInterestCouponVO percentCutInterestCouponVO = (PercentCutInterestCouponVO) couponVO;
      return CouponPoint.builder().percent(percentCutInterestCouponVO.deductPercent).build();
    } else if (couponVO instanceof UnifiedCutInterestCouponVO) {
      UnifiedCutInterestCouponVO vo = (UnifiedCutInterestCouponVO) couponVO;
      return CouponPoint.builder().amount(vo.deductAmount).build();
    } else if (couponVO instanceof PercentIncreaseCreditsCouponVO) {
      PercentIncreaseCreditsCouponVO vo = (PercentIncreaseCreditsCouponVO) couponVO;
      return CouponPoint.builder().amount(vo.acquireIncreaseCredits()).percent(vo.increasePercent).build();
    }
    throw EcException.error("Unknown LoanUserCouponVO class. ClassName : {}", couponVO.getClass().getName());
  }

  private void checkIsSupportedSourceType(NotifCouponGrantTaskSourceType sourceType, CouponGrantRuleVO ruleVO) {
    BaseCouponGrantRule<?> rule = couponGrantRuleService.getRuleOrThrow(ruleVO.ruleConfig.configType);
    if (!rule.supportedSourceType().contains(sourceType)) {
      throw EcException.error("发券工具不支持的发券来源类型. SourceType:{}, ruleId:{}", sourceType, ruleVO.id);
    }
  }

  public Long getRuleConfigIdById(long id) {
    return notifCouponGrantTaskModel.getRuleConfigIdById(id);
  }

  public Map<Long, Long> getRuleConfigIdMapByIds(Collection<Long> taskIds) {
    return notifCouponGrantTaskModel.getRuleConfigIdMapByIds(taskIds);
  }

  @Builder
  private static class CouponPoint {

    BigDecimal amount;
    BigDecimal percent;

    CouponPoint add(CouponPoint point) {
      return builder().amount(BigDecimalHelper.addWithNullAsZeroAndScale(amount, point.amount))
          .percent(BigDecimalHelper.addWithNullAsZeroAndScale(percent, point.percent)).build();
    }
  }

  public int countByUserIdAndRuleConfigId(long userId, long ruleConfigId, long startTime) {
    return notifCouponGrantTaskModel.countByUserIdAndRuleConfigId(userId, ruleConfigId, startTime);
  }

  /**
   * 查询指定用户在多个 ruleConfigId 下最近的成功发券记录，按 TIME_UPDATED 降序返回。
   *
   * @param userId  用户 ID
   * @param ruleIds 候选 ruleConfigId 列表
   * @param limit   最大返回条数
   * @return 状态为 SUCCESS 的发券任务 VO 列表，按 TIME_UPDATED 降序
   */
  public List<NotifCouponGrantTaskVO> listRecentSuccessByUserIdAndRuleIds(Long userId, List<Long> ruleIds, int limit) {
    return notifCouponGrantTaskModel.listRecentSuccessByUserIdAndRuleIds(userId, ruleIds, limit)
        .stream()
        .map(NotifCouponGrantTaskVO::from)
        .collect(Collectors.toList());
  }
}
