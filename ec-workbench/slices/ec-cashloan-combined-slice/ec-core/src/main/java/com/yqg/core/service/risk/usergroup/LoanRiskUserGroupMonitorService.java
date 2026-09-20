package com.yqg.core.service.risk.usergroup;

import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.monitor.BaseMonitorService;
import com.yqg.core.service.monitor.MonitorMeasurementName;
import com.yqg.core.service.monitor.RetentionPolicies;
import com.yqg.core.service.risk.facade.RiskFacadeTool;
import com.yqg.core.service.risk.usergroup.enums.UserGroupLockState;
import com.yqg.core.service.risk.usergroup.vo.UserGroupChangeSnapshot;
import com.yqg.core.service.risk.usergroup.vo.UserGroupHistoryTraceData;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class LoanRiskUserGroupMonitorService extends BaseMonitorService {

  @Autowired
  private RiskFacadeTool riskFacadeTool;

  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;

  /**
   * 无变更前等级（首次 insert）时 {@code oldUserGroup} 的占位取值。
   */
  private static final String NO_OLD_USER_GROUP = "NONE";

  /**
   * 变更前无管制期时 {@code lockSourceBefore} 的占位取值：该维度值域为
   * {@link LoanRiskUserGroupChangeReason}，缺少「无锁」这一语义，故以保留字补位。
   */
  private static final String NO_LOCK_SOURCE = "NO_LOCK";

  /**
   * 额度状态两个维度取不到值时的占位取值，覆盖三种情况：变更前无管制期（按设计不采集）、
   * 采集失败（{@code UserGroupCreditsSnapshotService} 已记 error 日志）、额度未失效（无失效归因）。
   */
  private static final String NO_CREDITS_VALUE = "NONE";

  /**
   * 记录一次用户等级变更，同时携带变更前后状态。
   *
   * <p>变更后维度（{@code userGroup} / {@code reason}）支撑「首笔、非首笔 90 天加锁量」；
   * 叠加变更前维度（{@code oldUserGroup} / {@code lockStateBefore} / {@code lockSourceBefore}）
   * 支撑「回流主营量」按整体、90 天锁到期、90 天锁内额度失效三档拆分，
   * 以及「锁内被非额度失效原因升主营」这一疑似漏保级的异常观测。
   *
   * <p>注意 {@code reason} 是本次变更的原因，{@code lockSourceBefore} 是变更前那把锁的赋予原因，
   * 两者值域相同但语义不同，面板取数时别混用。
   *
   * <p>{@code calcCreditsStatusBefore} / {@code creditsExpireTypeBefore} 直接坐实「锁内额度失效回流」与
   * 「锁内非额度失效升主营」的归因，不再依赖从 {@code reason} 间接推断；两者仅在变更前有管制期时才有真实取值。
   *
   * <p>{@code before} 中的维度必须由调用方在写库前算好：Point 构建跑在异步线程，此处不得查库。
   *
   * @param before 变更前状态快照，无历史记录时传 {@link UserGroupChangeSnapshot#empty()}
   */
  public void logUserGroupChangeRecord(Long userId,
                                       Long accountId,
                                       LoanRiskUserGroupEnum userGroup,
                                       LoanRiskUserGroupChangeReason reason,
                                       Long expireTime,
                                       UserGroupChangeSnapshot before) {
    writePoint(() -> {
          Point.Builder builder = Point.measurement(MonitorMeasurementName.USER_GROUP_MONITOR.name)
              .tag("userGroup", userGroup.name())
              .tag("reason", reason.name())
              .tag("oldUserGroup", before.oldUserGroup != null ? before.oldUserGroup.name() : NO_OLD_USER_GROUP)
              .tag("lockStateBefore", before.lockStateBefore != null ?
                  before.lockStateBefore.name() : UserGroupLockState.NO_LOCK.name())
              .tag("lockSourceBefore", before.lockSourceReasonBefore != null
                  ? before.lockSourceReasonBefore.name() : NO_LOCK_SOURCE)
              .tag("calcCreditsStatusBefore", before.calcCreditsStatusBefore != null
                  ? before.calcCreditsStatusBefore.name() : NO_CREDITS_VALUE)
              .tag("creditsExpireTypeBefore", before.creditsExpireTypeBefore != null
                  ? before.creditsExpireTypeBefore.name() : NO_CREDITS_VALUE)
              .addField("userId", userId)
              .addField("accountId", accountId)
              .addField("expireTime", expireTime)
              .addField("expireTimeBefore", before.expireTimeBefore);
          return builder.build();
        }, RetentionPolicies.ONE_MONTH
    );
  }

  public void logUserGroupCheck(
      LoanRiskUserGroupEnum userGroup,
      boolean isRuleHit,
      Long checkConfigId,
      String userType,
      boolean shouldBlock,
      Long loanAccountId,
      String userStatus,
      Boolean isMobileWhiteListHit,
      Boolean isCreditsAccept,
      Boolean canGetLoanByTraceOutput,
      LoanUserRiskTraceVO loanUserRiskTraceVO
  ) {
    UserGroupHistoryTraceData userGroupHistoryTraceData = riskFacadeTool.findFirstNotMatchData(loanAccountId);
    String currentSourceType = loanUserRiskTraceVO.sourceType != null ? loanUserRiskTraceVO.sourceType.name() : "";
    List<String> historicalSourceTypes = loanUserRiskTraceModel.listSourceTypeByAccountId(loanAccountId);
    boolean hasApiRiskControl = historicalSourceTypes.stream()
        .filter(Objects::nonNull)
        .map(SourceType::getSourceType)
        .filter(Objects::nonNull)
        .anyMatch(SourceType::isApiChannelSourceType);
    if (userGroupHistoryTraceData != null) {
      log.info("logUserGroupCheck: userGroup={}, isRuleHit={}, checkConfigId={}, userType={}, riskType={}, shouldBlock={}, userStatus={}, isMobileWhiteListHit={}, canGetLoanByTraceOutput={}, isCreditsAccept={}, sourceType={}, hasApiRiskControl={}, firstNotMatchUserGroup={}, firstNotMatchUserType={}, firstNotMatchRiskType={}, firstNotMatchCanGetLoanByTraceOutput={}, firstNotMatchSourceType={}, loanAccountId={}, traceId={}, firstNotMatchTraceId={}",
          userGroup, isRuleHit, checkConfigId, userType, loanUserRiskTraceVO.riskType.code, shouldBlock, userStatus, isMobileWhiteListHit, canGetLoanByTraceOutput, isCreditsAccept, currentSourceType, hasApiRiskControl, userGroupHistoryTraceData.getUserGroup(), userGroupHistoryTraceData.getUserType(), userGroupHistoryTraceData.getRiskType().code, userGroupHistoryTraceData.getCanGetLoanByTraceOutput(), userGroupHistoryTraceData.getSourceType(), loanAccountId, loanUserRiskTraceVO.traceId, userGroupHistoryTraceData.getTraceId());
    } else {
      log.info("logUserGroupCheck: userGroup={}, isRuleHit={}, checkConfigId={}, userType={}, riskType={}, shouldBlock={}, userStatus={}, isMobileWhiteListHit={}, canGetLoanByTraceOutput={}, isCreditsAccept={}, sourceType={}, hasApiRiskControl={}, loanAccountId={}, traceId={}",
          userGroup, isRuleHit, checkConfigId, userType, loanUserRiskTraceVO.riskType.code, shouldBlock, userStatus, isMobileWhiteListHit, canGetLoanByTraceOutput, isCreditsAccept, currentSourceType, hasApiRiskControl, loanAccountId, loanUserRiskTraceVO.traceId);
    }
    writePoint(() -> {
          Point.Builder builder = Point.measurement(MonitorMeasurementName.USER_GROUP_CHECK_MONITOR.name)
              .tag("userGroup", userGroup.name())
              .tag("isRuleHit", String.valueOf(isRuleHit))
              .tag("checkConfigId", String.valueOf(checkConfigId))
              .tag("userType", userType)
              .tag("riskType", loanUserRiskTraceVO.riskType.code)
              .tag("shouldBlock", String.valueOf(shouldBlock))
              .tag("userStatus", userStatus)
              .tag("isMobileWhiteListHit", isMobileWhiteListHit.toString())
              .tag("canGetLoanByTraceOutput", String.valueOf(canGetLoanByTraceOutput))
              .tag("isCreditsAccept", isCreditsAccept.toString())
              .tag("sourceType", currentSourceType)
              .tag("hasApiRiskControl", String.valueOf(hasApiRiskControl))
              .addField("loanAccountId", loanAccountId)
              .addField("traceId", loanUserRiskTraceVO.traceId);
          if (userGroupHistoryTraceData != null) {
            builder.tag("firstNotMatchUserGroup", userGroupHistoryTraceData.getUserGroup() != null ? userGroupHistoryTraceData.getUserGroup().name() : "")
                .tag("firstNotMatchUserType", userGroupHistoryTraceData.getUserType() != null ? userGroupHistoryTraceData.getUserType() : "")
                .tag("firstNotMatchRiskType", userGroupHistoryTraceData.getRiskType() != null ? userGroupHistoryTraceData.getRiskType().code : "")
                .tag("firstNotMatchCanGetLoanByTraceOutput", userGroupHistoryTraceData.getCanGetLoanByTraceOutput() != null ? userGroupHistoryTraceData.getCanGetLoanByTraceOutput().toString() : "")
                .tag("firstNotMatchSourceType", userGroupHistoryTraceData.getSourceType() != null ? userGroupHistoryTraceData.getSourceType().name() : "")
                .addField("firstNotMatchTraceId", userGroupHistoryTraceData.getTraceId());
          }
          return builder.build();
        }, RetentionPolicies.ONE_MONTH
    );
  }


  public void logRepairUserGroupDataForTriggerRetrieval(
      LoanRiskUserGroupEnum oldGroup,
      String userType,
      Long loanAccountId,
      LoanUserRiskType loanUserRiskType,
      Long traceId
  ) {
    writePoint(() -> {
          Point.Builder builder = Point.measurement(MonitorMeasurementName.REPAIR_USER_GROUP_DATA_FOR_TRIGGER_RETRIEVAL.name)
              .tag("oldGroup", oldGroup.name())
              .tag("userType", userType)
              .tag("loanUserRiskType", loanUserRiskType.code)
              .addField("loanAccountId", loanAccountId)
              .addField("traceId", traceId);
          return builder.build();
        }, RetentionPolicies.ONE_MONTH
    );
  }

}
