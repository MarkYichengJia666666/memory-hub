package com.yqg.core.service.risk.usergroup;

import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.model.sql.loan.account.enums.LoanUserTypeChangeReason;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.usergroup.vo.UserGroupTriggerResult;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author chaoye
 * @date 2025/6/25
 */
@Service
public class ReapplyRiskGroupProcessor implements IRiskUserGroupProcessor {
  @Autowired
  private RiskUserGroupTool riskUserGroupTool;
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;

  @Override
  public LoanRiskUserGroupEnum getGroup() {
    return LoanRiskUserGroupEnum.REAPPLY;
  }

  @Override
  public UserGroupTriggerResult triggerAfterRisk(LoanUserRiskTraceVO previousRiskTraceVO, LoanUserTagData data) {
    //重审不降级
    return UserGroupTriggerResult.fromNoUpdate(getGroup());
  }

  @Override
  public UserGroupTriggerResult triggerBeforeSubmitRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, boolean readOnly) {

    //跑批额外功能强制升级
    UserGroupTriggerResult extraResult = riskUserGroupTool.handleBatchRiskExtraFunctionUserGroupChange(riskProcessParam, getGroup(), readOnly);
    if (extraResult != null) {
      return extraResult;
    }

    if (submitScene == LoanUserRiskSubmitScene.AUTO_CALC_CREDITS_AFTER_REPAY_ALL) {
      return riskUserGroupTool.handleAutoCalcRepayAllForReapply(riskProcessParam, getGroup(), readOnly);
    }
    if (LoanUserRiskSubmitScene.CREDITS_EXPIRE_AND_RE_CALC == submitScene) {
      //额度失效重跑，升级到主营
      LoanRiskUserGroupEnum updatedUserGroup = LoanRiskUserGroupEnum.NORMAL;
      if (readOnly) {
        return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, getGroup());
      }
      riskUserGroupTool.handleUserGroupUpgradeToNormal(riskProcessParam, updatedUserGroup,
          LoanRiskUserGroupChangeReason.CALC_CREDITS_EXPIRE,
          LoanUserTypeVO.RETRIEVAL_CALC_CREDITS_EXPIRE_USER,
          LoanUserTypeChangeReason.RETRIEVAL_CALC_CREDITS_EXPIRE_USER_CHANGE);

      return UserGroupTriggerResult.fromOnlyUpdated(updatedUserGroup, getGroup());
    }

    if (LoanUserRiskSubmitScene.REAPPLY_CALC_CREDITS == submitScene) {
      //渠道来源的请求
      if (riskProcessParam != null
          && riskProcessParam.extraInfo != null
          && riskProcessParam.extraInfo.sourceType != null
          && riskProcessParam.extraInfo.sourceType.isApiChannelSourceType()) {
        return riskUserGroupTool.tryHandleApiChannelReapplyAfterIntervalUpgrade(riskProcessParam, getGroup(), readOnly);
      }
      //非渠道来源的请求
      return riskUserGroupTool.handleReapplyUserGroupUpgrade(riskProcessParam, getGroup(), riskProcessParam.extraInfo, riskProcessParam.build, readOnly);
    }

    return UserGroupTriggerResult.fromNoUpdate(getGroup());
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
