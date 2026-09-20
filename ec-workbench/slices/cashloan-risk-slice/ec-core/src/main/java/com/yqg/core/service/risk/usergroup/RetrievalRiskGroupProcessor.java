package com.yqg.core.service.risk.usergroup;

import com.yqg.core.model.generated.tables.records.RiskOutputResultRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.model.sql.loan.account.enums.LoanUserTypeChangeReason;
import com.yqg.core.model.sql.risk.RiskOutputResultModel;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.multiloan.enums.MultiLoanTriggerType;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.risk.batchtrigger.RiskBatchTriggerService;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.service.risk.usergroup.vo.UserGroupTriggerResult;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaoye
 * @date 2025/6/25
 */
@Slf4j
@Service
public class RetrievalRiskGroupProcessor implements IRiskUserGroupProcessor {
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;
  @Autowired
  private RiskUserGroupTool riskUserGroupTool;
  @Autowired
  private RiskOutputResultModel riskOutputResultModel;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private RiskBatchTriggerService riskBatchTriggerService;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;

  @Override
  public LoanRiskUserGroupEnum getGroup() {
    return LoanRiskUserGroupEnum.RETRIEVAL;
  }

  @Override
  public UserGroupTriggerResult triggerAfterRisk(LoanUserRiskTraceVO previousTraceVO, LoanUserTagData data) {
    //续借风控，单独处理
    if (LoanUserRiskType.getAllMultiLoanRiskType().contains(previousTraceVO.riskType)) {
      //不进行升降级，保持回捞
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }
    if (previousTraceVO.creditsStatus != LoanCreditsStatus.REJECTED) {
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }
    //回捞被拒
    return handleRetrievalRiskRejectForNotMulti(previousTraceVO.accountId, previousTraceVO.traceId);
  }

  //非续借，回捞被拒
  private UserGroupTriggerResult handleRetrievalRiskRejectForNotMulti(Long accountId, Long traceId) {
    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrThrow(accountId);

    //降级到重审
    if (loanRiskUserGroupService.isControlPeriodSourcedFromRetrievalAndReapplyPayout(loanRiskUserGroupVO)) {
      //管制期源头为回捞/重审打款锁（首笔或非首笔）：继承原管制期，并透传源头，reason 改写为 PRE_RISK_REJECT 但源头不丢
      loanRiskUserGroupService.updateLoanRiskUserGroupWithExpireTime(accountId,
          LoanRiskUserGroupEnum.REAPPLY,
          traceId,
          LoanRiskUserGroupChangeReason.PRE_RISK_REJECT,
          loanRiskUserGroupVO.expireTime,
          String.valueOf(traceId),
          loanRiskUserGroupVO.expireTimeSourceLogId);
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    } else {
      //管制期非打款源头：清空管制期（expire_time_source_log_id 留空），external_id 仍记本次 traceId
      loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(accountId,
          LoanRiskUserGroupEnum.REAPPLY,
          traceId,
          LoanRiskUserGroupChangeReason.PRE_RISK_REJECT,
          String.valueOf(traceId));
    }

    return UserGroupTriggerResult.fromOnlyUpdated(LoanRiskUserGroupEnum.REAPPLY, getGroup());
  }

  @Override
  public UserGroupTriggerResult triggerBeforeSubmitRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, boolean readOnly) {
    if (needForceUpdateByBatchRisk(riskProcessParam)) {
      //命中跑批特殊id时，强制升级，不检查管制期
      return getUserGroupUpgradeToNormalResult(riskProcessParam, LoanRiskUserGroupChangeReason.BATCH_RISK_FORCE_UPGRADE, readOnly);
    }

    //跑批额外功能强制升级
    UserGroupTriggerResult extraResult = riskUserGroupTool.handleBatchRiskExtraFunctionUserGroupChange(riskProcessParam, getGroup(), readOnly);
    if (extraResult != null) {
      return extraResult;
    }

    if (submitScene == LoanUserRiskSubmitScene.AUTO_CALC_CREDITS_AFTER_REPAY_ALL) {
      //自动结清时，回捞升级到主营
      return riskUserGroupTool.handleAutoCalcRepayAllForRetrieval(riskProcessParam, getGroup(), readOnly);
    }
    if (LoanUserRiskSubmitScene.CREDITS_EXPIRE_AND_RE_CALC == submitScene) {
      return getUserGroupUpgradeToNormalResult(riskProcessParam, LoanRiskUserGroupChangeReason.CALC_CREDITS_EXPIRE, readOnly);
    }
    if (LoanUserRiskSubmitScene.AUTO_MULTI_LOAN_CALC_CREDITS == submitScene) {
      return tryUpgradeForAutoSubmitMultiLoanCalcCredits(riskProcessParam, readOnly);
    }
    return UserGroupTriggerResult.fromNoUpdate(getGroup());
  }

  private UserGroupTriggerResult getUserGroupUpgradeToNormalResult(RiskProcessParam riskProcessParam, LoanRiskUserGroupChangeReason changeReason, boolean readOnly) {
    //额度失效重跑，升级到主营
    LoanRiskUserGroupEnum updatedUserGroup = LoanRiskUserGroupEnum.NORMAL;

    if (readOnly) {
      return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, getGroup());
    }
    riskUserGroupTool.handleUserGroupUpgradeToNormal(riskProcessParam, updatedUserGroup,
        changeReason,
        LoanUserTypeVO.RETRIEVAL_CALC_CREDITS_EXPIRE_USER,
        LoanUserTypeChangeReason.RETRIEVAL_CALC_CREDITS_EXPIRE_USER_CHANGE);

    return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, getGroup());
  }


  private UserGroupTriggerResult tryUpgradeForAutoSubmitMultiLoanCalcCredits(RiskProcessParam riskProcessParam, boolean readOnly) {
    //只处理还款续借场景
    MultiLoanTriggerType triggerType = multiLoanStatusService.getTriggerType(riskProcessParam.accountId);
    if (!triggerType.isRepayment()) {
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }

    //判断回捞等级来源
    if (!riskUserGroupTool.isUserGroupFromMultiOrRevolvingTraceId(riskProcessParam.accountId)) {
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }

    //user group 是否有过期时间，且未过期
    boolean hasExpireTimeAndNotExpire = riskUserGroupTool.isUserGroupHasExpireTimeAndNotExpire(riskProcessParam.accountId);
    //判断是否在管制期内
    if (!hasExpireTimeAndNotExpire) {
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }

    //还清部分账单，但是没有结清时
    // 如果用户回捞等级来源于续借被拒/循环管制,并且未过管制期，升级到主营
    return getUserGroupUpgradeToNormalResult(riskProcessParam, LoanRiskUserGroupChangeReason.REPAY_PARTIAL_INSTALMENTS, readOnly);
  }

  @Deprecated
  //TODO (chaoye)后续修改为跑批标签的方式
  private boolean needForceUpdateByBatchRisk(RiskProcessParam riskProcessParam) {
    if (riskProcessParam.logId == null) {
      return false;
    }
    Long autoTaskId = riskBatchTriggerService.getAutoTaskIdByLogId(riskProcessParam.logId);
    if (autoTaskId == null) {
      return false;
    }
    List<Long> forceUpgradeUserGroupAutoBatchRiskIdList = riskConfig.getForceUpgradeUserGroupAutoBatchRiskIdList();
    return forceUpgradeUserGroupAutoBatchRiskIdList.contains(autoTaskId);
  }

  @Override
  public UserGroupTriggerResult onOrderPayoutSuccess(CashLoanOrderVO cashLoanOrderVO) {
    riskUserGroupTool.handleOrderPayoutSuccessForRetrievalAndReapply(cashLoanOrderVO);
    return UserGroupTriggerResult.fromNoUpdate(getGroup());
  }

  @Override
  public void triggerBeforeSubmitRiskForChangeSubNewUserType(RiskProcessParam riskProcessParam, LoanUserRiskType riskType) {
    riskUserGroupTool.handleSubNewUserTyperForNormalUser(riskProcessParam, riskType);
  }

}
