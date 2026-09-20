package com.yqg.core.service.risk.usergroup;

import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.model.sql.loan.account.enums.LoanUserTypeChangeReason;
import com.yqg.core.service.cashloan.multiloan.MultiLoanConfig;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.RevolvingUserGroupAbtestService;
import com.yqg.core.service.loan.account.RevolvingUserGroupService;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.loan.vo.SimpleLoanAccountVO;
import com.yqg.core.service.risk.feature.BusinessRiskConfig;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.subnew.SubNewAccountService;
import com.yqg.core.service.risk.usergroup.vo.UserGroupTriggerResult;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author chaoye
 * @date 2025/6/25
 */
@Slf4j
@Service
public class NormalRiskGroupProcessor implements IRiskUserGroupProcessor {
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private BusinessRiskConfig businessRiskConfig;
  @Autowired
  private RiskUserGroupTool riskUserGroupTool;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private SubNewAccountService subNewAccountService;
  @Autowired
  private RevolvingUserGroupAbtestService revolvingUserGroupAbtestService;
  @Autowired
  private RevolvingUserGroupService revolvingUserGroupService;
  @Autowired
  private MultiLoanConfig multiLoanConfig;

  @Override
  public LoanRiskUserGroupEnum getGroup() {
    return LoanRiskUserGroupEnum.NORMAL;
  }

  @Override
  public UserGroupTriggerResult triggerAfterRisk(LoanUserRiskTraceVO previousTraceVO, LoanUserTagData data) {
    //续借风控，单独处理
    //todo(chaoye) 续借适配api渠道
    if (LoanUserRiskType.getAllMultiLoanRiskType().contains(previousTraceVO.riskType)) {
      return triggerForAfterMultiLoanRisk(previousTraceVO, data);
    }

    //非续借风控
    //循环用户被管制了，也可能触发降级回捞
    if (LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(previousTraceVO.riskType)) {
      return triggerForAfterRevolvingLoanRisk(previousTraceVO, data);
    }
    return triggerForAfterAllCompleteOrder(previousTraceVO);
  }

  //结清复贷和首贷
  private UserGroupTriggerResult triggerForAfterAllCompleteOrder(LoanUserRiskTraceVO previousTraceVO) {
    //api渠道不降级，如果拒绝策略会永久拒绝

    if (previousTraceVO.sourceType.isApiChannelSourceType()) {
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }

    // 最新trace是特定riskType风控通过，并且输出47,直接降级为回捞
    if (riskUserGroupTool.needDegradeAfterRiskAcceptWithRetrievalUserType(previousTraceVO)) {
      loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(previousTraceVO.accountId, LoanRiskUserGroupEnum.RETRIEVAL, previousTraceVO.traceId, LoanRiskUserGroupChangeReason.RISK_ACCEPT_AND_USER_TYPE_RETRIEVAL, String.valueOf(previousTraceVO.traceId));
      return UserGroupTriggerResult.fromOnlyUpdated(LoanRiskUserGroupEnum.RETRIEVAL, getGroup());
    }

    if (previousTraceVO.creditsStatus != LoanCreditsStatus.REJECTED) {
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }
    return triggerAfterRiskRejectWhenNotMultiLoan(previousTraceVO);
  }

  private UserGroupTriggerResult triggerForAfterRevolvingLoanRisk(LoanUserRiskTraceVO previousTraceVO, LoanUserTagData data) {
    boolean revolvingNeedTriggerRetrieval = loanAccountRevolvingService.checkNeedTriggerRetrieval(previousTraceVO.traceId, previousTraceVO.accountId);
    if (!revolvingNeedTriggerRetrieval) {
      return triggerForAfterAllCompleteOrder(previousTraceVO);
    }
    Long retrievalValidDays = data.revolvingControlDays;
    //修改userType
    return updateUserGroupInMultiAndRevolvingWithRetrievalValidDays(previousTraceVO, retrievalValidDays, LoanRiskUserGroupChangeReason.REVOLVING_CONTROLLED);
  }

  @Override
  public UserGroupTriggerResult triggerBeforeSubmitRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, boolean readOnly) {
    if (LoanUserRiskSubmitScene.REAPPLY_CALC_CREDITS == submitScene) {
      return handleBeforeReapplyCalcCredits(riskProcessParam, readOnly);
    }
    return UserGroupTriggerResult.fromNoUpdate(getGroup());
  }

  /**
   * 重审测额前置处理：根据渠道来源分别处理 API 渠道和端内场景
   */
  private UserGroupTriggerResult handleBeforeReapplyCalcCredits(RiskProcessParam riskProcessParam, boolean readOnly) {
    SourceType sourceType = riskProcessParam.extraInfo == null ? null : riskProcessParam.extraInfo.sourceType;

    if (sourceType != null && sourceType.isApiChannelSourceType()) {
      return handleApiChannelBeforeSubmitRisk(riskProcessParam, readOnly);
    }
    return handleNotApiChannelBeforeSubmitRisk(riskProcessParam, readOnly);
  }

  /**
   * 端外（API 渠道）用户重审测额前置处理：API 渠道首贷被拒≥180天时按渠道重置 userType（Gopay→I18 / Lazada Buyer→I19）
   */
  private UserGroupTriggerResult handleApiChannelBeforeSubmitRisk(RiskProcessParam riskProcessParam, boolean readOnly) {
    riskUserGroupTool.tryUpdateApiChannelFirstLoanUserTypeToReapplyIntervalInit(
        riskProcessParam.extraInfo, riskProcessParam.accountId, readOnly);
    return UserGroupTriggerResult.fromNoUpdate(getGroup());
  }

  /**
   * 端内（非 API 渠道）用户重审测额前置处理：区分端外被拒回端和普通端内用户
   */
  private UserGroupTriggerResult handleNotApiChannelBeforeSubmitRisk(RiskProcessParam riskProcessParam, boolean readOnly) {
    LoanUserRiskTraceVO latestTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(riskProcessParam.accountId);

    // 上笔 trace 来源为端外（API 渠道）：端外被拒用户回端场景
    if (latestTraceVO != null && latestTraceVO.sourceType != null && latestTraceVO.sourceType.isApiChannelSourceType()) {
      return riskUserGroupTool.handleApiChannelRejectUserBackToAppForNormal(riskProcessParam, latestTraceVO, readOnly);
    }

    // 普通端内用户
    return UserGroupTriggerResult.fromNoUpdate(getGroup());
  }

  @Override
  public UserGroupTriggerResult onOrderPayoutSuccess(CashLoanOrderVO cashLoanOrderVO) {
    return UserGroupTriggerResult.fromNoUpdate(getGroup());
  }

  @Override
  public void triggerBeforeSubmitRiskForChangeSubNewUserType(RiskProcessParam riskProcessParam, LoanUserRiskType riskType) {
    riskUserGroupTool.handleSubNewUserTyperForNormalUser(riskProcessParam, riskType);
  }

  private UserGroupTriggerResult triggerForAfterMultiLoanRisk(LoanUserRiskTraceVO previousTraceVO, LoanUserTagData data) {
    if (data == null) {
      log.error("data can not be null when triggerForAfterMultiLoanRisk");
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }
    // API 渠道不降级：策略常开（已移除 multi_loan.open_api_channel_no_update_group 开关）；sourceType 为空按非 API 继续原逻辑
    if (previousTraceVO.sourceType != null && previousTraceVO.sourceType.isApiChannelSourceType()) {
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }

    //TODO 具体字段获取，需要考虑admin，后面diff处理
    Boolean rejectMultiApply = data.rejectMultiApply;
    Boolean rejectMultiOrder = data.rejectMultiOrder;

    boolean isMultiRiskReject = loanUserRiskTraceService.isMultiRiskReject(previousTraceVO, rejectMultiApply, rejectMultiOrder);

    return doTriggerAfterMultiLoan(isMultiRiskReject, previousTraceVO, data);
  }

  /**
   * 主营续借风控正常触发升降级
   *
   * @param isMultiRiskReject
   * @param previousTraceVO
   * @param data
   * @return
   */
  private UserGroupTriggerResult doTriggerAfterMultiLoan(boolean isMultiRiskReject, LoanUserRiskTraceVO previousTraceVO, LoanUserTagData data) {
    if (isMultiRiskReject) {
      return triggerForAfterMultiLoanRiskReject(previousTraceVO, data);
    }

    return UserGroupTriggerResult.fromNoUpdate(getGroup());
  }

  private UserGroupTriggerResult triggerForAfterMultiLoanRiskReject(LoanUserRiskTraceVO previousTraceVO, LoanUserTagData data) {
    if (subNewAccountService.isSubNewUserByTraceTime(previousTraceVO.accountId, previousTraceVO.timeCreated)) {
      log.info("multi loan risk reject but user is subnew, so no degrade, accountId = {}", previousTraceVO.accountId);
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }

    Integer retrievalValidDays = data.retrievalValidDays;

    //降级时，retrievalValidDays必须大于0
    if (retrievalValidDays == null || retrievalValidDays <= 0) {
      throw EcException.error(EcExceptionType.LOAN_RETRIEVAL_VALID_DAYS_ERROR, "multi loan retrieval valid days invalid , traceId = {}", previousTraceVO.traceId);
    }

    return updateUserGroupInMultiAndRevolvingWithRetrievalValidDays(previousTraceVO, retrievalValidDays, LoanRiskUserGroupChangeReason.PRE_RISK_REJECT);
  }

  private UserGroupTriggerResult updateUserGroupInMultiAndRevolvingWithRetrievalValidDays(LoanUserRiskTraceVO previousTraceVO,
                                                                                          long retrievalValidDays,
                                                                                          LoanRiskUserGroupChangeReason reason) {
    //修改userType
    riskUserGroupTool.degradeUserTypeFromNormalToRetrieval(previousTraceVO.accountId);

    //修改人群等级：循环管制/多头被拒每次都新造管制期，登记本次为源头（自引用），external_id 记本次 traceId
    long expireTime = Clock.now() + retrievalValidDays * Clock.MILLS_PER_DAY;
    loanRiskUserGroupService.updateLoanRiskUserGroupWithExpireTimeAsSource(previousTraceVO.accountId,
        LoanRiskUserGroupEnum.RETRIEVAL,
        previousTraceVO.traceId,
        reason,
        expireTime,
        String.valueOf(previousTraceVO.traceId));
    //续借降级不需要准入，直接跑承接风控
    return UserGroupTriggerResult.from(LoanUserRiskSubmitScene.DEGRADE_ALREADY_ACCESS, LoanRiskUserGroupEnum.RETRIEVAL, getGroup());
  }

  /**
   * 非续借场景下，风控被拒后升降级处理
   *
   * @param previousTraceVO
   * @return
   */
  private UserGroupTriggerResult triggerAfterRiskRejectWhenNotMultiLoan(LoanUserRiskTraceVO previousTraceVO) {
    if (!LoanUserRiskType.getAllRetrievalPreRiskType().contains(previousTraceVO.riskType)) {
      return UserGroupTriggerResult.fromNoUpdate(getGroup());
    }

    //白名单
    if (riskConfig.getRetrievalUserIdWhiteList().contains(previousTraceVO.userId)) {
      loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(previousTraceVO.accountId, LoanRiskUserGroupEnum.RETRIEVAL, previousTraceVO.traceId, LoanRiskUserGroupChangeReason.PRE_RISK_REJECT, String.valueOf(previousTraceVO.traceId));
      return UserGroupTriggerResult.from(LoanUserRiskSubmitScene.DEGRADE_TRY_ACCESS, LoanRiskUserGroupEnum.RETRIEVAL, getGroup());
    }
    //用户已经注销，不提交回捞,降级到重审
    SimpleLoanAccountVO simpleLoanAccountVO = loanAccountBasicInfoService.getSimpleLoanAccountVo(previousTraceVO.accountId);

    if (simpleLoanAccountVO.deleted) {
      loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(previousTraceVO.accountId, LoanRiskUserGroupEnum.REAPPLY, previousTraceVO.traceId, LoanRiskUserGroupChangeReason.PRE_RISK_REJECT, String.valueOf(previousTraceVO.traceId));
      return UserGroupTriggerResult.fromOnlyUpdated(LoanRiskUserGroupEnum.REAPPLY, getGroup());
    }

    boolean isReloanRetrievalAccess = riskUserGroupTool.isReloanRetrievalAccessForNormalDegrade(previousTraceVO);
    if (isReloanRetrievalAccess) {
      //如果已经准入，需要手动改下userType为47
      riskUserGroupTool.degradeUserTypeFromNormalToRetrieval(previousTraceVO.accountId);
    } else {
      //未准入，改下userType为I14
      loanAccountBasicInfoService.updateUserTypeWithoutCheckWithTraceId(previousTraceVO.accountId, null, LoanUserTypeVO.FIRST_RETRIEVAL_INIT, LoanUserTypeChangeReason.FIRST_RETRIEVAL_CHANGE);
    }
    loanRiskUserGroupService.updateLoanRiskUserGroupWithNoExpire(previousTraceVO.accountId, LoanRiskUserGroupEnum.RETRIEVAL, previousTraceVO.traceId, LoanRiskUserGroupChangeReason.PRE_RISK_REJECT, String.valueOf(previousTraceVO.traceId));
    LoanUserRiskSubmitScene nextScene = isReloanRetrievalAccess ? LoanUserRiskSubmitScene.DEGRADE_ALREADY_ACCESS : LoanUserRiskSubmitScene.DEGRADE_TRY_ACCESS;
    return UserGroupTriggerResult.from(nextScene, LoanRiskUserGroupEnum.RETRIEVAL, getGroup());

  }

}
