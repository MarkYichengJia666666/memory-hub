package com.yqg.core.service.cashloan;

import com.google.common.collect.Sets;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.LoanAccountDetailsNewRecord;
import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.sql.cashloan.CalcCreditsInfoLogModel;
import com.yqg.core.model.sql.loan.account.LoanAccountDetailsModel;
import com.yqg.core.model.sql.loan.account.LoanAccountDetailsSnapshotModel;
import com.yqg.core.model.sql.loan.account.LoanUserCreditsInfoModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatus;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.service.cashloan.enums.CashLoanCalcCreditsStatus;
import com.yqg.core.service.cashloan.enums.CreditsExpireType;
import com.yqg.core.service.cashloan.enums.HitConsistentHashPrefix;
import com.yqg.core.service.cashloan.multiloan.MultiLoanConfig;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.multiloan.monitor.ReloanRiskApiReturnMonitor;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.*;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.infos.CashLoanEmploymentInfo;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.loan.vo.LoanUserSimpleCreditsInfoVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsVO;
import com.yqg.core.service.risk.facade.RiskTypeTool;
import com.yqg.core.service.risk.usergroup.RiskUserGroupEntranceService;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.util.EcHashUtil;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.RiskSubmitException;
import com.yqg.ec.common.exception.RiskSubmitFailReason;
import com.yqg.ec.common.i18n.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * Created by jiewu on 2020/10/9
 */
@Service
@Slf4j
public class CashLoanCalcCreditsService {
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  private LoanUserCreditsInfoModel userCreditsInfoModel;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private RiskTypeTool riskTypeTool;
  @Autowired
  private CalcCreditsInfoLogModel calcCreditsInfoLogModel;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private LoanAccountDetailsModel loanAccountDetailsModel;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private RiskUserGroupEntranceService riskUserGroupEntranceService;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;
  @Autowired
  private LoanAccountDetailsSnapshotModel loanAccountDetailsSnapshotModel;
  @Autowired
  private MultiLoanConfig multiLoanConfig;
  @Autowired
  private ReloanRiskApiReturnMonitor reloanRiskApiReturnMonitor;

  private static final Set<LoanCreditsStatus> IN_REVIEW_STATUSES = Sets.newHashSet(LoanCreditsStatus.IN_REVIEW, LoanCreditsStatus.MANUAL_REVIEW);
  public static final Long DEFAULT_EXPIRED_TIME = 2524579200000L;//2050-01-01 00:00:00


  //TODO (chaoye) 后续简化这个方法的逻辑
  public CashLoanCalcCreditsVO getCalcCreditsStatus(Long accountId) {
    return threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(accountId);
      // 如果是首贷，过期跑首贷过期测算
      if (!loanAccountService.isReloan(accountId)) {
        return isCreditsExpiredByLoan(accountVO);
      }
      LoanUserCreditsInfoVO creditsInfoVO = loanAccountService.getCreditsInfo(accountId);
      // 循环额度用户
      boolean isInRevolving = loanAccountRevolvingService.checkUserInRevolvingLoanProcess(accountId);
      if (isInRevolving) {
        return getCalcCreditsVOForRevolvingCredit(accountVO, creditsInfoVO);
      }
      return getCalcCreditsVOForReloanWithoutRevolvingCredit(accountVO, creditsInfoVO);
    });
  }

  private CashLoanCalcCreditsVO getCalcCreditsVOForRevolvingCredit(LoanAccountVO loanAccountVO, LoanUserCreditsInfoVO creditsInfoVO) {
    // 复贷授信状态为空
    if (creditsInfoVO.reloanStatus == null) {
      throw EcException.error("循环额度用户，复贷授信状态不应该为空, accountId:{}", loanAccountVO.id);
    }
    switch (creditsInfoVO.reloanStatus) {
      case IN_REVIEW:
      case MANUAL_REVIEW:
        LoanUserRiskTraceVO lastRiskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(loanAccountVO.id);
        //如果是额度测算,返回 CALC_CREDITS_IN_REVIEW
        if (lastRiskTraceVO.riskType == LoanUserRiskType.REVOLVING_LOAN_FIRST && lastRiskTraceVO.status == RiskFlowTraceStatusV2.INIT) {
          return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_IN_REVIEW);
        }
        return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, DEFAULT_EXPIRED_TIME);
      case ACCEPTED:
        return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, DEFAULT_EXPIRED_TIME);
      default:
        log.info("unexpected reloan status: {}, accountId: {}", creditsInfoVO.reloanStatus, creditsInfoVO.accountId);
        return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, DEFAULT_EXPIRED_TIME);
    }
  }

  private CashLoanCalcCreditsVO getCalcCreditsVOForReloanWithoutRevolvingCredit(LoanAccountVO accountVO, LoanUserCreditsInfoVO creditsInfoVO) {
    // 有在途订单
    boolean uncompletedOrder = ecOrderService.existOrder(accountVO.id, CashLoanOrderStatus.UNDONE_STATUSES_WITHOUT_READY);
    if (uncompletedOrder) {
      return getCalcCreditsVOForUncompletedOrderOrCreditsInReview(accountVO);
    }
    // 续借
    boolean hasReadyOrder = ecOrderService.existOrder(accountVO.id, CashLoanOrderStatus.READY);
    if (hasReadyOrder) {
      return getCalcCreditsVOForMultiLoan(accountVO, creditsInfoVO);
    }

    // todo(shubo)是否可以删除这块逻辑
    // 首贷的授信状态
    if (creditsInfoVO.creditsStatus != LoanCreditsStatus.ACCEPTED) {
      log.info("unexpected loan status: {}, accountId: {}", creditsInfoVO.creditsStatus, creditsInfoVO.accountId);
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, DEFAULT_EXPIRED_TIME);
    }

    // 复贷授信状态为空
    if (creditsInfoVO.reloanStatus == null) {
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED, null,
          LoanUserRiskType.CALC_CREDITS, CreditsExpireType.NO_CALC_AFTER_PAYOUT);
    }

    // 复贷授信状态不为空
    switch (creditsInfoVO.reloanStatus) {
      case IN_REVIEW:
      case MANUAL_REVIEW:
        if (Objects.isNull(creditsInfoVO.reloanTraceId) || isCalcCreditsTrace(creditsInfoVO.reloanTraceId)) {
          return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_IN_REVIEW);
        }
        return getCalcCreditsVOForUncompletedOrderOrCreditsInReview(accountVO);
      case ACCEPTED:
        return isCreditsExpiredByReloan(accountVO);
      default:
        log.info("unexpected reloan status: {}, accountId: {}", creditsInfoVO.reloanStatus, creditsInfoVO.accountId);
        return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, DEFAULT_EXPIRED_TIME);
    }
  }

  /**
   * @param loanAccountVO
   * @return
   */
  private CashLoanCalcCreditsVO getCalcCreditsVOForUncompletedOrderOrCreditsInReview(LoanAccountVO loanAccountVO) {

    //最新的riskType不是复贷测额，过期时间以当前时间为基准计算
    LoanUserRiskTraceRecord userRiskTraceRecord = loanUserRiskTraceModel.findLastedByAccountIdAndRiskType(loanAccountVO.id, LoanUserRiskType.getReloanCalcCreditWithoutRevolvingRiskType());
    if (userRiskTraceRecord == null) {
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED,
          Clock.now() + cashLoanConfig.getReloanCreditsExpiredHours(loanAccountVO.sdkType) * Clock.MILLS_PER_HOUR);
    }
    //最新的riskType是复贷测额，过期时间以测额时间为基准计算
    return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED,
        userRiskTraceRecord.getTimeCreated() + cashLoanConfig.getReloanCreditsExpiredHours(loanAccountVO.sdkType) * Clock.MILLS_PER_HOUR);
  }

  public CashLoanCalcCreditsVO getCalcCreditsVOForMultiLoan(LoanAccountVO loanAccountVO, LoanUserCreditsInfoVO creditsInfoVO) {
    //判断整体授信状态和续借状态
    if (!creditsInfoVO.isCreditsAccept()) {
      return getCalcCreditsVOForUncompletedOrderOrCreditsInReview(loanAccountVO);
    }
    MultiLoanStatus multiLoanStatus = multiLoanStatusService.getStatusOrNull(loanAccountVO.id);
    if (multiLoanStatus != MultiLoanStatus.CALC_ACCEPTED) {
      return getCalcCreditsVOForUncompletedOrderOrCreditsInReview(loanAccountVO);
    }

    // 判断额度过期，目前仅回捞等级可能过期
    boolean isUserGroupExpire = riskUserGroupEntranceService.checkUserGroupExpireByAccountId(loanAccountVO.id);
    if (!isUserGroupExpire) {
      return getCalcCreditsVOForUncompletedOrderOrCreditsInReview(loanAccountVO);
    }

    LoanUserRiskTraceVO latestCalcTraceVO = loanUserRiskTraceService.findLatestCalcCreditRiskByAccountId(loanAccountVO.id);
    if (!LoanUserRiskType.getMultiLoanCalcRiskType().contains(latestCalcTraceVO.riskType)) {
      log.error("risk type not be multi loan, accountId:{}, riskType : {}", latestCalcTraceVO.accountId, latestCalcTraceVO.riskType);
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, DEFAULT_EXPIRED_TIME);
    }

    LoanRiskUserGroupVO userGroupVO = riskUserGroupEntranceService.getLoanRiskUserGroupVOByAccountIdOrThrow(loanAccountVO.id);
    LoanUserRiskType nextRiskType = riskTypeTool.getExpireNextRiskTypeForAllVersion(loanAccountVO.id);
    // 本分支的失效判定入口即 checkUserGroupExpireByAccountId，归因恒为等级管制期到期
    return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED, latestCalcTraceVO.riskType,
        nextRiskType, userGroupVO.expireTime, CreditsExpireType.USER_GROUP_EXPIRED);
  }

  /**
   * 是否 API 渠道且已完件鉴权（跑批拒绝归因等前置识别）。
   */
  public boolean isApiChannelForAuthFinish(Long loanAccountId) {
    LoanAccountDetailsNewRecord loanAccountDetailsNewRecord = loanAccountDetailsModel.newFindByAccountId(loanAccountId);
    return loanAccountDetailsNewRecord != null
        && SourceType.valueOf(loanAccountDetailsNewRecord.getSourceType()).isApiChannelSourceType()
        && loanAccountDetailsNewRecord.getTimeFinished() != null;
  }

  /**
   * 跑批触发：API 完件用户续借准入
   * <ul>
   *   <li>非 API 完件：允许</li>
   *   <li>API 完件且非白名单渠道：不允许</li>
   *   <li>白名单渠道：须 snapshot 有补件记录 就业信息未补全仅打点不拦截</li>
   * </ul>
   *
   * @param loanAccountId 借款账户 id
   * @return true=允许续借相关流程继续
   */
  public boolean allowMultiLoanForChannelWhenBatch(Long loanAccountId) {
    ApiReturnMultiLoanContext ctx = resolveApiReturnMultiLoanContext(loanAccountId);
    if (ctx == null) {
      log.info("[allowMultiLoanForChannelWhenBatch]not api return or no snapshot, loanAccountId: {}", loanAccountId);
      return true;
    }
    if (!ctx.whitelisted) {
      log.info("[allowMultiLoanForChannelWhenBatch]channel not whitelisted, loanAccountId: {}", loanAccountId);
      return false;
    }
    if (multiLoanConfig.isOpenApiMultiLoanIntercept() && !ctx.hasApiReturnSnapshot) {
      log.info("[allowMultiLoanForChannelWhenBatch]multi loan intercept and api snapshot not exists, loanAccountId: {}", loanAccountId);
      reloanRiskApiReturnMonitor.logPreCheckFail(
          loanAccountId, ctx.sourceTypeName, ReloanRiskApiReturnMonitor.FAIL_REASON_INTERCEPT_AND_EMPLOYMENT_INFO_INCOMPLETE, LoanUserRiskTriggerSource.BATCH_RISK);
      return false;
    }
    return true;
  }

  /**
   * Kafka/还款触发：API 完件用户续借准入（TAPD-1364251）。
   * <ul>
   *   <li>非 API 完件：允许</li>
   *   <li>API 完件且非白名单渠道：不允许</li>
   *   <li>白名单渠道：须 snapshot 有补件记录 + 在贷 READY 有 COMPLETE 分期；就业信息未补全仅打点不拦截</li>
   * </ul>
   *
   * @param loanAccountId 借款账户 id
   * @return true=允许续借相关流程继续
   */
  public boolean allowMultiLoanForChannelRetAppWhenRepayment(Long loanAccountId) {
    ApiReturnMultiLoanContext ctx = resolveApiReturnMultiLoanContext(loanAccountId);
    if (ctx == null) {
      log.info("[allowMultiLoanForChannelRetAppWhenRepayment]not api return or no snapshot, loanAccountId: {}", loanAccountId);
      return true;
    }
    if (!ctx.whitelisted) {
      log.info("[allowMultiLoanForChannelRetAppWhenRepayment]channel not whitelisted, loanAccountId: {}", loanAccountId);
      return false;
    }
    if (multiLoanConfig.isOpenApiMultiLoanIntercept() && !ctx.hasApiReturnSnapshot) {
      log.info("[allowMultiLoanForChannelRetAppWhenRepayment]multi loan intercept and api snapshot not exists, loanAccountId: {}", loanAccountId);
      reloanRiskApiReturnMonitor.logPreCheckFail(
          loanAccountId, ctx.sourceTypeName, ReloanRiskApiReturnMonitor.FAIL_REASON_INTERCEPT_AND_EMPLOYMENT_INFO_INCOMPLETE, LoanUserRiskTriggerSource.BATCH_RISK);
      return false;
    }
    return true;
  }

  /**
   * API/补件触发：与还款路径共用白名单闸门，但就业补全也作为硬门槛；
   * 失败抛出 {@link RiskSubmitException}，原因见 {@link RiskSubmitException#getReason()}。
   * <p>供 {@code submitReloanRiskApiReturnSupplement} 等显式失败入口。</p>
   *
   * @param loanAccountId 借款账户 id
   */
  public void allowMultiLoanForChannelRetAppWhenSupplementWithException(Long loanAccountId) {
    ApiReturnMultiLoanContext ctx = resolveApiReturnMultiLoanContext(loanAccountId);
    if (ctx == null) {
      log.info("[allowMultiLoanForChannelRetAppWhenSupplementWithException]not api return or no snapshot, loanAccountId: {}", loanAccountId);
      return;
    }
    if (!ctx.whitelisted) {
      log.info("[allowMultiLoanForChannelRetAppWhenSupplementWithException]channel not whitelisted, loanAccountId: {}", loanAccountId);
      throwRiskSubmit(loanAccountId, ctx.sourceTypeName, RiskSubmitFailReason.CHANNEL_NOT_WHITELISTED, null);
    }
    if (!ctx.hasApiReturnSnapshot) {
      log.info("[allowMultiLoanForChannelRetAppWhenSupplementWithException]employment info incomplete, loanAccountId: {}", loanAccountId);
      throwRiskSubmit(loanAccountId, ctx.sourceTypeName, RiskSubmitFailReason.EMPLOYMENT_INFO_INCOMPLETE,
          ReloanRiskApiReturnMonitor.FAIL_REASON_EMPLOYMENT_INFO_INCOMPLETE);
    }
    // snapshot 通过后再查就业 / 账单
    if (!isEmploymentComplete(loanAccountId)) {
      log.info("[allowMultiLoanForChannelRetAppWhenSupplementWithException]employment snapshot inconsistent, loanAccountId: {}", loanAccountId);
      throwRiskSubmit(loanAccountId, ctx.sourceTypeName, RiskSubmitFailReason.EMPLOYMENT_SNAPSHOT_INCONSISTENT,
          ReloanRiskApiReturnMonitor.FAIL_REASON_EMPLOYMENT_SNAPSHOT_INCONSISTENT);
    }
    if (!ecOrderService.hasReadyOrderWithCompletedInstalment(loanAccountId)) {
      log.info("[allowMultiLoanForChannelRetAppWhenSupplementWithException]bill not qualified, loanAccountId: {}", loanAccountId);
      throwRiskSubmit(loanAccountId, ctx.sourceTypeName, RiskSubmitFailReason.BILL_NOT_QUALIFIED,
          ReloanRiskApiReturnMonitor.FAIL_REASON_BILL_NOT_QUALIFIED);
    }
  }

  private void throwRiskSubmit(
      Long loanAccountId,
      String sourceTypeName,
      RiskSubmitFailReason reason,
      String monitorFailReason) {
    if (monitorFailReason != null) {
      reloanRiskApiReturnMonitor.logPreCheckFail(loanAccountId, sourceTypeName, monitorFailReason, LoanUserRiskTriggerSource.API_RETURN_SUPPLEMENT);
    }
    throw RiskSubmitException.of(
        reason,
        "allowMultiLoanForApiAuthFinishForApi rejected, loanAccountId={}, reason={}",
        loanAccountId,
        reason.name());
  }

  private boolean isEmploymentComplete(Long loanAccountId) {
    LoanAccountDetailsVO detailsVO = loanAccountDetailsService.getByAccountIdOrNull(loanAccountId);
    CashLoanEmploymentInfo employmentInfo = detailsVO == null || detailsVO.detailsPojo == null
        ? null
        : detailsVO.detailsPojo.cashLoanEmploymentInfo;
    return CashLoanEmploymentInfo.isApiReturnSupplementWorkingInfoComplete(employmentInfo);
  }

  /**
   * @return null=非 API 完件（不拦截）；非 null 为渠道白名单 + snapshot 上下文（就业/账单按调用方分阶段查询）
   */
  private ApiReturnMultiLoanContext resolveApiReturnMultiLoanContext(Long loanAccountId) {
    LoanAccountDetailsNewRecord detailsNewRecord = loanAccountDetailsModel.newFindByAccountId(loanAccountId);
    // 非 API 完件（含 details 缺失 / 未完件）：不拦截
    if (detailsNewRecord == null || detailsNewRecord.getTimeFinished() == null) {
      return null;
    }
    SourceType detailsSourceType = SourceType.valueOf(detailsNewRecord.getSourceType());
    if (!detailsSourceType.isApiChannelSourceType()) {
      return null;
    }
    boolean whitelisted = SourceType.isReloanRiskApiChannelWhitelisted(detailsSourceType);
    if (!whitelisted) {
      return ApiReturnMultiLoanContext.notWhitelisted(detailsSourceType.name());
    }

    String snapshotLogReason = multiLoanConfig.getApiReturnSnapshotLogReason();
    boolean hasApiReturnSnapshot = loanAccountDetailsSnapshotModel.existsByAccountIdAndLogReason(loanAccountId, snapshotLogReason);
    return ApiReturnMultiLoanContext.whitelisted(detailsSourceType.name(), hasApiReturnSnapshot);
  }

  private static final class ApiReturnMultiLoanContext {
    public final boolean whitelisted;
    public final String sourceTypeName;
    public final boolean hasApiReturnSnapshot;

    private ApiReturnMultiLoanContext(
        boolean whitelisted,
        String sourceTypeName,
        boolean hasApiReturnSnapshot) {
      this.whitelisted = whitelisted;
      this.sourceTypeName = sourceTypeName;
      this.hasApiReturnSnapshot = hasApiReturnSnapshot;
    }

    private static ApiReturnMultiLoanContext notWhitelisted(String sourceTypeName) {
      return new ApiReturnMultiLoanContext(false, sourceTypeName, false);
    }

    private static ApiReturnMultiLoanContext whitelisted(String sourceTypeName, boolean hasApiReturnSnapshot) {
      return new ApiReturnMultiLoanContext(true, sourceTypeName, hasApiReturnSnapshot);
    }
  }

  private CashLoanCalcCreditsVO isCreditsExpiredByLoan(LoanAccountVO accountVO) {
    if (!cashLoanConfig.getLoanExpiredSwitch(accountVO.sdkType) || cashLoanConfig.getLoanAccountIdByLoanExpiredWhiteList().contains(accountVO.id)) {
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, DEFAULT_EXPIRED_TIME);
    }
    // todo(shubo)是否可以删除这块逻辑
    // 如果用户没有额度测算记录
    LoanUserRiskTraceRecord userRiskTraceRecord = loanUserRiskTraceModel.findLastedByAccountIdAndRiskType(accountVO.id, LoanUserRiskType.getLoanCalcRiskType());
    if (userRiskTraceRecord == null) {
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, DEFAULT_EXPIRED_TIME);
    }

    // 看下最新的额度测算或者首贷重审额度测算是不是已经超过配置时间
    return getCashLoanCalcCreditsAndRiskTypeVO(userRiskTraceRecord, cashLoanConfig.getLoanCreditsExpiredHours(accountVO.sdkType));
  }

  private CashLoanCalcCreditsVO isCreditsExpiredByReloan(LoanAccountVO accountVO) {
    // 循环贷用户没有过期概念
    boolean revolvingUserCheck = loanAccountRevolvingService.checkUserInRevolvingLoanProcess(accountVO.id);
    if (revolvingUserCheck) {
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED);
    }
    // 如果用户没有额度测算记录，直接return riskType=C
    // 虽然上层做了循环额度过滤，这里riskType还是排除一下
    LoanUserRiskTraceRecord userRiskTraceRecord = loanUserRiskTraceModel.findLastedByAccountIdAndRiskType(accountVO.id, LoanUserRiskType.getReloanCalcCreditWithoutRevolvingRiskType());
    if (userRiskTraceRecord == null) {
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED, null,
          LoanUserRiskType.CALC_CREDITS, CreditsExpireType.NO_CALC_AFTER_PAYOUT);
    }

    // 如果 最后一次复贷额度测算之后 有 成功打款的订单，则额度失效，return true
    CashLoanOrderVO latestPayoutOrderAfterLastedCreditsRisk = ecOrderService.getLatestOrderAfterSpecifiedTime(accountVO.id, userRiskTraceRecord.getTimeCreated(), CashLoanOrderStatus.READY, CashLoanOrderStatus.COMPLETE);
    if (latestPayoutOrderAfterLastedCreditsRisk != null) {
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED, null,
          LoanUserRiskType.CALC_CREDITS, CreditsExpireType.NO_CALC_AFTER_PAYOUT);
    }

    // 最后一次复贷额度测算之后 没有 成功打款的订单，看下最新的额度测算或者复贷重审额度测算是不是已经超过配置时间
    return getCashLoanCalcCreditsAndRiskTypeVO(userRiskTraceRecord, cashLoanConfig.getReloanCreditsExpiredHours(accountVO.sdkType));
  }

  public CashLoanCalcCreditsVO getCashLoanCalcCreditsAndRiskTypeVO(LoanUserRiskTraceRecord userRiskTraceRecord, int remainingTime) {
    LoanUserRiskType loanUserRiskType = LoanUserRiskType.fromCode(userRiskTraceRecord.getRiskType());
    LoanUserRiskType nextRiskType = riskTypeTool.getExpireNextRiskTypeForAllVersion(userRiskTraceRecord.getLoanAccountId());
    // 最后一次复贷额度测算之后 没有 成功打款的订单，看下最新的额度测算或者复贷重审额度测算是不是已经超过配置时间
    //获取最后一条风控记录的创建时间

    Long userGroupHavingExpireTime = Long.MAX_VALUE;
    boolean isUserGroupHavingExpireTimeByAccountId = riskUserGroupEntranceService.isUserGroupHavingExpireTimeByAccountId(userRiskTraceRecord.getLoanAccountId());
    if (isUserGroupHavingExpireTimeByAccountId) {
      LoanRiskUserGroupVO userGroupVO = riskUserGroupEntranceService.getLoanRiskUserGroupVOByAccountIdOrThrow(userRiskTraceRecord.getLoanAccountId());
      userGroupHavingExpireTime = userGroupVO.expireTime;
    }

    long now = Clock.now();
    long creditsExpiredTime = userRiskTraceRecord.getTimeCreated() + remainingTime * Clock.MILLS_PER_HOUR;
    //过期时间以 用户等级过期时间 和 额度测算过期时间的最小值为准
    Long finalCreditsExpiredTime = Math.min(creditsExpiredTime, userGroupHavingExpireTime);
    if (now <= finalCreditsExpiredTime) {
      return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED, finalCreditsExpiredTime);
    }
    // 归因口径：等级管制期已到期即判等级到期（此时测算可能同时过期），否则判测算过期；
    // 无管制期时 userGroupHavingExpireTime 为 Long.MAX_VALUE，恒落测算过期
    CreditsExpireType creditsExpireType = now > userGroupHavingExpireTime
        ? CreditsExpireType.USER_GROUP_EXPIRED : CreditsExpireType.CALC_CREDITS_EXPIRED;
    return CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED, loanUserRiskType,
        nextRiskType, finalCreditsExpiredTime, creditsExpireType);
  }

  private boolean isCalcCreditsTrace(Long traceId) {
    LoanUserRiskTraceRecord traceRecord = loanUserRiskTraceModel.findByTraceId(traceId);
    return LoanUserRiskType.fromCode(traceRecord.getRiskType()) == LoanUserRiskType.CALC_CREDITS;
  }

  public CashLoanCalcCreditsVO getCalcCreditsStatusWithBuildAndPercent(Long userId, Long accountId,
                                                                       Long build, SDKType sdkType,
                                                                       LoanUserTypeVO loanUserTypeVO) {
    CashLoanCalcCreditsVO cashLoanCalcCreditsVO = getCalcCreditsStatus(accountId);
    switch (cashLoanCalcCreditsVO.calcCreditsStatus) {
      // 先获取用户状态，如果不是额度失效的话直接return
      case CALC_CREDITS_NOT_NEEDED:
      case CALC_CREDITS_IN_REVIEW:
        return cashLoanCalcCreditsVO;
      case CALC_CREDITS_EXPIRED:
        // 如果是额度失效的话，再进行开关、版本、分流的校验
        boolean result = canCalcCredits(sdkType, build, loanUserTypeVO, userId);
        return result ? cashLoanCalcCreditsVO : CashLoanCalcCreditsVO.from(CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED);
      default:
        throw EcException.error("unsupported status {}", cashLoanCalcCreditsVO.calcCreditsStatus);
    }
  }

  public CashLoanCalcCreditsProcessInfoVO getCalcCreditsProcessInfo(Long accountId, SDKType sdkType) {
    CashLoanCalcCreditsProcessInfoVO processInfoVO = new CashLoanCalcCreditsProcessInfoVO();
    processInfoVO.isInCalcCredits = false;
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountIdOrThrow(accountId);
    if (creditsInfoRecord == null
        || creditsInfoRecord.getReloanStatus() == null
        || !IN_REVIEW_STATUSES.contains(LoanCreditsStatus.fromCode(creditsInfoRecord.getReloanStatus()))) {
      return processInfoVO;
    }
    LoanUserRiskTraceRecord traceRecord = loanUserRiskTraceModel.findByTraceId(creditsInfoRecord.getReloanTraceId());
    LoanUserRiskType loanUserRiskType = LoanUserRiskType.fromCode(traceRecord.getRiskType());
    if (loanUserRiskType == LoanUserRiskType.CALC_CREDITS) {
      processInfoVO.isInCalcCredits = true;
      processInfoVO.processedMills = Clock.now() - traceRecord.getTimeCreated();
    }
    return processInfoVO;
  }

  public boolean canCalcCredits(SDKType sdkType, Long build, LoanUserTypeVO loanUserTypeVO, Long userId) {
    if (cashLoanConfig.getSkipCalcCreditsPercent(sdkType)) {
      return true;
    }
    // 1、版本大于等于配置的版本 2、配置的分流大于0  3、命中了分流
    Long openBuild = cashLoanConfig.getCalcCreditsOpenBuild(sdkType);
    Double percent = cashLoanConfig.getCalcCreditsPercent(sdkType, loanUserTypeVO.name);
    return build >= openBuild
        && percent > 0
        && EcHashUtil.hitConsistentHash(HitConsistentHashPrefix.CAN_CALC_CREDITS, userId.toString(), 0.0D, percent);
  }

  public void insertLog(Long accountId, SDKType sdkType, CashLoanCalcCreditsStatus status) {
    calcCreditsInfoLogModel.insert(accountId, sdkType, status);
  }

  public CalcCreditsInfoLogVO findLatestLog(Long accountId) {
    return CalcCreditsInfoLogVO.from(calcCreditsInfoLogModel.findLatestByAccountId(accountId));
  }

  public CashLoanCalcCreditsStatus getMultiLoanCalcCreditsStatus(Long accountId) {
    MultiLoanStatus currentStatus = multiLoanStatusService.getStatusOrThrow(accountId);
    switch (currentStatus) {
      case INVALID:
      case CALC_REJECTED:
      case ORDER_CHECKING:
      case ORDER_ACCEPTED:
      case ORDER_REJECTED:
      case CALC_ACCEPTED:
        return CashLoanCalcCreditsStatus.CALC_CREDITS_NOT_NEEDED;
      case CALC_STARTED:
        return CashLoanCalcCreditsStatus.CALC_CREDITS_IN_REVIEW;
      case INIT:
        return CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED;
      default:
        throw EcException.error("Unknown MultiLoanStatus {}!", currentStatus);
    }
  }

  /**
   * 判断用户当前额度是否无效（包括额度过期，测额后下单导致原额度无效等情况）
   *
   * @param loanAccountId
   * @return
   */
  public Boolean isCreditsInvalid(Long loanAccountId) {
    CashLoanCalcCreditsVO cashLoanCalcCreditsVO = getCalcCreditsStatus(loanAccountId);
    return cashLoanCalcCreditsVO.calcCreditsStatus == CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED;
  }

  /**
   * TODO（chaoye）后续和getCalcCreditsStatus方法合并
   * 用户最近一笔通过的风控trace为复贷测额的情况下，判断此trace额度是否过期
   * 注意：与getCalcCreditsStatus方法中的CALC_CREDITS_EXPIRED不完全相同
   *
   * @param loanAccountId
   * @return
   */
  @Deprecated
  public Boolean isRecentReloanCreditsExpired(Long loanAccountId) {
    //先找到最新的常规流程风控
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(loanAccountId);
    //判断是否为除了循环的复贷测额（循环流程没有过期概念）
    if (!LoanUserRiskType.getReloanCalcCreditWithoutRevolvingRiskType().contains(loanUserRiskTraceVO.riskType)) {
      return false;
    }
    //判断额度有效期
    return isCreditsInvalid(loanAccountId);
  }

  //判断用户之前的额度是否过期
  public Boolean isRecentCreditsExpiredForSubmitRisk(Long loanAccountId) {
    //先找到最新的常规流程测额风控
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findLatestCalcCreditRiskByAccountId(loanAccountId);
    //判断是否为允许过期的测额风控
    if (!validExpiredRiskType(loanUserRiskTraceVO.riskType)) {
      return false;
    }
    //判断是否为风控通过状态
    LoanUserSimpleCreditsInfoVO creditsInfoVO = loanUserCreditsService.getLoanUserSimpleCreditsInfoVOByUserId(loanUserRiskTraceVO.userId);
    if (!creditsInfoVO.isCreditsAccept()) {
      return false;
    }

    //额度有效期
    if (isCreditsInvalid(loanAccountId)) {
      return true;
    }

    //续借用户
    // 如果MultiLoanStatus不为CALC_ACCEPTED，在上述isCreditsInvalid方法中并不认为失效，而是退出续借
    // 此时用户依然可以通过还款，跑批等方式提交风控，如果回捞有效期到了，这里需要视为额度失效
    boolean hasReadyOrder = ecOrderService.existOrder(loanAccountId, CashLoanOrderStatus.READY);
    if (hasReadyOrder) {
      return riskUserGroupEntranceService.checkUserGroupExpireByAccountId(loanAccountId);
    }

    return false;
  }

  public boolean validExpiredRiskType(LoanUserRiskType riskType) {
    if (LoanUserRiskType.getReloanCalcCreditWithoutRevolvingRiskType().contains(riskType)) {
      return true;
    }
    if (LoanUserRiskType.getLoanCalcRiskType().contains(riskType)) {
      return true;
    }
    if (LoanUserRiskType.getMultiLoanCalcRiskType().contains(riskType)) {
      return true;
    }
    return false;
  }
}
