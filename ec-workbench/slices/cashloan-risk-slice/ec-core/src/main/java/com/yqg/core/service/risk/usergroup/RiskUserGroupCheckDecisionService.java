package com.yqg.core.service.risk.usergroup;

import com.yqg.core.model.generated.tables.records.RiskUserGroupCheckConfigRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.risk.RiskUserGroupCheckConfigModel;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserSimpleCreditsInfoVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.risk.facade.RiskFacadeTool;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckAvailableStatus;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckDecision;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckOperationRelation;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.service.risk.usergroup.vo.RiskUserGroupCheckConfigVO;
import com.yqg.core.service.user.UserService;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RiskUserGroupCheckDecisionService {
  @Autowired
  private RiskUserGroupCheckConfigModel riskUserGroupCheckConfigModel;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;
  @Autowired
  private LoanRiskUserGroupMonitorService loanRiskUserGroupMonitorService;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private UserService userService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private RiskFacadeTool riskFacadeTool;

  public void checkUserGroup(LoanUserRiskTraceVO riskTraceVO, Long loanAccountId) {
    LoanAccountVO accountVO = loanAccountService.getLoanAccountVO(loanAccountId);
    LoanRiskUserGroupVO loanRiskUserGroupVO = loanRiskUserGroupService.getLoanRiskUserGroupVOByAccountIdOrThrow(loanAccountId);
    LoanRiskUserGroupEnum loanRiskUserGroupEnum = loanRiskUserGroupVO.userGroup;

    List<RiskUserGroupCheckConfigVO> riskUserGroupCheckConfigVOs = getByUserGroup(loanRiskUserGroupEnum);
    for (RiskUserGroupCheckConfigVO riskUserGroupCheckConfigVO : riskUserGroupCheckConfigVOs) {
      checkUserGroupHitRule(loanRiskUserGroupEnum, accountVO.loanUserTypeVO.code, riskUserGroupCheckConfigVO, accountVO, riskTraceVO);
    }
  }

  private boolean needCheckUserGroupRule(RiskUserGroupCheckConfigVO riskUserGroupCheckConfigVO, LoanUserRiskTraceVO riskTraceVO) {
    //检查启用状态
    if (riskUserGroupCheckConfigVO.availableStatus != RiskUserGroupCheckAvailableStatus.AVAILABLE) {
      return false;
    }
    List<String> riskTypes = riskUserGroupCheckConfigVO.riskTypeList;
    //检查risktype
    if (CollectionUtils.isNotEmpty(riskTypes) && !isContains(riskTraceVO.riskType.code, riskTypes)) {
      return false;
    }
    //检查授信状态
    if (riskUserGroupCheckConfigVO.creditStatus != null
        && riskTraceVO.creditsStatus != riskUserGroupCheckConfigVO.creditStatus.traceCreditsStatus) {
      return false;
    }
    return true;
  }

  private boolean needCheckUserGroupRuleForAdmin(RiskUserGroupCheckConfigVO riskUserGroupCheckConfigVO, @Nullable LoanCreditsStatus currentCreditStatus) {
    if (riskUserGroupCheckConfigVO.availableStatus != RiskUserGroupCheckAvailableStatus.AVAILABLE) {
      return false;
    }
    if (CollectionUtils.isNotEmpty(riskUserGroupCheckConfigVO.riskTypeList)) {
      return false;
    }
    if (riskUserGroupCheckConfigVO.creditStatus != null
        && riskUserGroupCheckConfigVO.creditStatus.traceCreditsStatus != currentCreditStatus) {
      return false;
    }
    return true;
  }

  /**
   * Admin-side consistency check. Returns true if the given (userType, userGroup) combination
   * violates any active rule with risk_type_list IS NULL. Does NOT throw or write monitors.
   * Degrades gracefully on rule-load failure (returns false to avoid blocking the update).
   */
  private boolean checkUserGroupForAdmin(String userType, LoanRiskUserGroupEnum userGroupEnum, @Nullable LoanCreditsStatus currentCreditStatus) {
    List<RiskUserGroupCheckConfigRecord> records;
    try {
      records = riskUserGroupCheckConfigModel.getByNullRiskType(userGroupEnum);
    } catch (Exception e) {
      log.error("checkUserGroupForAdmin: failed to load rules for userGroup={}, degrade gracefully", userGroupEnum, e);
      return false;
    }
    List<RiskUserGroupCheckConfigVO> configVOs = records.stream()
        .map(RiskUserGroupCheckConfigVO::from)
        .collect(Collectors.toList());
    for (RiskUserGroupCheckConfigVO configVO : configVOs) {
      if (!needCheckUserGroupRuleForAdmin(configVO, currentCreditStatus)) {
        continue;
      }
      if (isRuleHit(configVO, userType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the user group matches the rule and trigger the block.
   */
  private void checkUserGroupHitRule(
      LoanRiskUserGroupEnum userGroupEnum,
      String userType,
      RiskUserGroupCheckConfigVO configVO,
      LoanAccountVO accountVO,
      LoanUserRiskTraceVO riskTraceVO
  ) {
    //pre check
    if (!needCheckUserGroupRule(configVO, riskTraceVO)) {
      return;
    }

    // 1. First, determine if the rule is matched (get userTypes directly from configVO, no need to pass a separate parameter)
    boolean isRuleHit = isRuleHit(configVO, userType);

    // 2. Based on the hit results and decisions, determine whether blocking is necessary.
    boolean shouldBlock = shouldBlock(isRuleHit, configVO);
    log.info("result of userGroupCheckId: {} userGroup: {}, riskUserGroupCheckDecision: {}, loanAccountId: {}, isRuleHit: {}, shouldBlock: {}",
        configVO.id, userGroupEnum, configVO.decision, accountVO.id, isRuleHit, shouldBlock);

    String userStatus = accountVO.timeUserCreated > riskConfig.getRiskUserGroupCheckNewUserRegisterTime() ? "NEW" : "OLD";

    String nationalMobile = userService.getNationalMobileNumberOrThrow(accountVO.userId, accountVO.sdkType);
    Boolean isMobileWhiteListHit = riskConfig.getMobileWhiteListForUserGroupCheck().contains(nationalMobile);

    LoanUserSimpleCreditsInfoVO creditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(accountVO.id);
    Boolean isCreditsAccept = creditsInfoVO != null && creditsInfoVO.isCreditsAccept();
    boolean canGetLoanByTraceOutput = riskFacadeTool.canGetLoanByTraceOutput(riskTraceVO.traceId);

    loanRiskUserGroupMonitorService.logUserGroupCheck(configVO.userGroup,
        isRuleHit,
        configVO.id,
        userType,
        shouldBlock,
        accountVO.id,
        userStatus,
        isMobileWhiteListHit,
        isCreditsAccept,
        canGetLoanByTraceOutput,
        riskTraceVO);
    if (shouldBlock) {
      throw EcException.error(
          EcExceptionType.USER_GROUP_RULE_TRIGGER_BLOCKING,
          "User group rule trigger blocking, userType: {}, riskGroup: {}",
          userType, userGroupEnum
      );
    }
  }

  /**
   * Determine if a rule is matched (directly obtain the operation relationship and target user type list from configVO).
   *
   * @param configVO Rule configuration VO (including operation relationships and a list of target user types)
   * @param userType Current user type
   * @return true = hit the rule, false = miss the rule
   */
  private boolean isRuleHit(RiskUserGroupCheckConfigVO configVO, String userType) {
    RiskUserGroupCheckOperationRelation operationRelation = configVO.operationRelation;
    List<String> targetUserTypes = configVO.userTypeList;
    if (Objects.isNull(targetUserTypes)) {
      throw EcException.error("targetUserTypes is null");
    }

    switch (operationRelation) {
      case CONTAINS:
        return targetUserTypes.contains(userType);
      case NOT_CONTAINS:
        return !targetUserTypes.contains(userType);
      default:
        throw new IllegalArgumentException("Unsupported operation relation: " + operationRelation);
    }
  }

  /**
   * Based on the rule hit results and decisions, determine whether blocking is necessary.
   */
  private boolean shouldBlock(boolean isRuleHit, RiskUserGroupCheckConfigVO configVO) {
    RiskUserGroupCheckDecision decision = configVO.decision;

    return (isRuleHit && decision == RiskUserGroupCheckDecision.BLOCK);
  }

  private boolean isContains(String needToContains, List<String> containList) {
    return containList.contains(needToContains);
  }

  /**
   * Collects rule violations for a batch of accounts given a fixed userType-per-account map and a
   * fixed userGroup-per-account map. One of the two maps represents the current state while the
   * other represents the intended target value, depending on which field is being changed.
   * The violation message format string receives three indexed arguments:
   * %1$s = loan_account_id, %2$s = usertype code, %3$s = user_level name.
   */
  public List<String> collectRuleViolations(
      List<Long> accountIds,
      Map<Long, LoanUserTypeVO> userTypeByAccount,
      Map<Long, LoanRiskUserGroupEnum> userGroupByAccount,
      String violationMessageFormat
  ) {
    List<String> violations = new ArrayList<>();
    Map<Long, LoanUserSimpleCreditsInfoVO> creditsInfoMap = new HashMap<>(
        loanUserCreditsService.genLoanUserCreditsInfoMapByAccountIds(accountIds));
    for (Long accountId : new HashSet<>(accountIds)) {
      LoanUserTypeVO userTypeVO = userTypeByAccount.get(accountId);
      LoanRiskUserGroupEnum userGroup = userGroupByAccount.get(accountId);
      if (userTypeVO == null || userGroup == null) {
        continue;
      }
      try {
        LoanCreditsStatus currentCreditStatus = null;
        LoanUserSimpleCreditsInfoVO creditsInfoVO = creditsInfoMap.get(accountId);
        if (creditsInfoVO != null) {
          currentCreditStatus = creditsInfoVO.reloanStatus != null ? creditsInfoVO.reloanStatus : creditsInfoVO.creditsStatus;
        }
        if (checkUserGroupForAdmin(userTypeVO.code, userGroup, currentCreditStatus)) {
          violations.add(String.format(violationMessageFormat, accountId, userTypeVO.code, userGroup.name()));
        }
      } catch (Exception e) {
        log.error("collectRuleViolations: rule check failed for accountId={}, degrade gracefully", accountId, e);
      }
    }
    return violations;
  }

  public List<RiskUserGroupCheckConfigVO> getByUserGroup(LoanRiskUserGroupEnum userGroupEnum) {
    List<RiskUserGroupCheckConfigRecord> records = riskUserGroupCheckConfigModel.getByUserGroup(userGroupEnum);
    return records.stream().map(RiskUserGroupCheckConfigVO::from).collect(Collectors.toList());
  }
}
