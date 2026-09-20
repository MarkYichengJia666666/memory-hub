package com.yqg.core.service.risk.facade.dwlog;

import com.yqg.core.model.generated.tables.records.RiskOutputResultRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatus;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanUserTypeService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loanmarket.LoanMarketUserQualifyService;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.risk.facade.RiskFacadeTool;
import com.yqg.core.service.risk.facade.dwlog.PanoramaRiskDwLogCommon.CreditsQuotaInfo;
import com.yqg.core.service.risk.facade.dwlog.PanoramaRiskDwLogCommon.PanoramaUserGroupRiskValue;
import com.yqg.core.service.risk.facade.dwlog.PanoramaRiskDwLogCommon.PanoramaUserTypeRiskValue;
import com.yqg.core.service.risk.riskoutput.RiskOutputService;
import com.yqg.core.service.risk.usergroup.vo.UserGroupTriggerResult;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.utils.SysEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class PanoramaRiskDwLogService {

  private static final Set<LoanUserRiskType> PANORAMA_RISK_TYPES;
  private static final String REJECT_TYPE_SHORT = "SHORT_REJECT";
  private static final String REJECT_TYPE_PERMANENT = "PERMANENT_REJECT";

  static {
    PANORAMA_RISK_TYPES = new HashSet<>(LoanUserRiskType.getAllUserCreditsRiskTypes());
    PANORAMA_RISK_TYPES.add(LoanUserRiskType.INCREASE_CREDITS);
    PANORAMA_RISK_TYPES.add(LoanUserRiskType.SUPPLEMENT_INFO_BEFORE_CREATE_ORDER);
    PANORAMA_RISK_TYPES.add(LoanUserRiskType.BATCH_LOAN_REJECT_PRE_MARKETING_FILTER);
  }

  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private RiskOutputService riskOutputService;
  @Autowired
  private RiskFacadeTool riskFacadeTool;
  @Autowired
  private LoanUserTypeService loanUserTypeService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanMarketUserQualifyService loanMarketUserQualifyService;

  private static final String TEST_SUFFIX = "_test";

  /**
   * 提交风控审批事件打点
   */
  public void logRiskSubmitEvent(RiskProcessParam param,
                                 LoanUserRiskType riskType,
                                 UserGroupTriggerResult userGroupResult,
                                 LoanUserCreditsInfoVO preSubmitCreditsInfo) {
    if (!PANORAMA_RISK_TYPES.contains(riskType)) {
      return;
    }
    try {
      PanoramaRiskSubmitDwLogVO vo = new PanoramaRiskSubmitDwLogVO();
      vo.userId = preSubmitCreditsInfo != null ? preSubmitCreditsInfo.userId : null;
      vo.time = Clock.now();
      vo.ip = ImpliedContextUtils.ip();

      LoanUserRiskTraceVO latestTrace = loanUserRiskTraceService.findLatestCreditRiskByAccountId(param.accountId);
      vo.ecRiskId = latestTrace != null ? latestTrace.id : null;
      vo.riskTypeCode = riskType.code;
      vo.riskTypeName = riskType.name();
      vo.orderId = param.orderId;

      LoanCreditsStatus creditsStatus = preSubmitCreditsInfo != null ? preSubmitCreditsInfo.creditsStatus : null;
      Long timeReapply = preSubmitCreditsInfo != null ? preSubmitCreditsInfo.timeReapply : null;
      vo.creditStatus = creditsStatus != null ? creditsStatus.name() : null;
      vo.rejectType = buildRejectType(creditsStatus, timeReapply);
      vo.timeReapply = REJECT_TYPE_SHORT.equals(vo.rejectType) ? timeReapply : null;

      MultiLoanStatus multiLoanStatus = multiLoanStatusService.getStatusOrNull(param.accountId);
      vo.multiLoanStatus = multiLoanStatus != null ? multiLoanStatus.name() : null;

      vo.userGroup = buildSubmitUserGroupValue(userGroupResult);
      vo.userType = buildSubmitUserTypeValue(userGroupResult);

      vo.afRank = riskOutputService.findLatestByLoanAccountIdAndTypeValue(param.accountId, RiskOutputType.AF_RANK);
      vo.creditCategory = buildCreditCategory(riskType);
      vo.creditsQuota = buildCreditsQuotaInfo(preSubmitCreditsInfo);
      vo.productTag = riskOutputService.findLatestByLoanAccountIdAndTypeValue(param.accountId,
          RiskOutputType.PRODUCT_TAG);
      vo.rateTag = riskOutputService.findLatestByLoanAccountIdAndTypeValue(param.accountId, RiskOutputType.RATE_TAG);
      vo.qualifiedLoanMarketEntranceByRisk = loanMarketUserQualifyService.checkQualifyOnly(latestTrace);

      DwLogUtil.newLogByStr(getLogName(LogBusinessType.PANORAMA_RISK_SUBMIT), vo);
    } catch (Exception e) {
      log.error("logRiskSubmitEvent error, accountId:{}, riskType:{}", param.accountId, riskType, e);
    }
  }

  /**
   * 风控审批结束事件打点
   */
  public void logRiskCompleteEvent(LoanUserRiskTraceVO traceVO,
                                   UserGroupTriggerResult userGroupResult) {
    if (traceVO.riskType == null || !PANORAMA_RISK_TYPES.contains(traceVO.riskType)) {
      return;
    }
    // 仅在 trace 授信状态为「通过」或「拒绝」时打点（授信中/人工审核/取消等不打）
    if (traceVO.creditsStatus != LoanCreditsStatus.ACCEPTED
        && traceVO.creditsStatus != LoanCreditsStatus.REJECTED) {
      return;
    }
    try {
      PanoramaRiskCompleteDwLogVO vo = new PanoramaRiskCompleteDwLogVO();
      vo.userId = traceVO.userId;
      vo.time = Clock.now();
      //风控结束的事件，不是由用户触发的，所以ip为null
      vo.ip = null;

      vo.ecRiskId = traceVO.id;
      vo.traceId = traceVO.traceId;
      vo.riskTypeCode = traceVO.riskType != null ? traceVO.riskType.code : null;
      vo.riskTypeName = traceVO.riskType != null ? traceVO.riskType.name() : null;
      vo.orderId = traceVO.orderId;

      vo.traceResult = riskFacadeTool.canGetLoanByTraceOutput(traceVO.traceId);

      vo.creditStatus = traceVO.creditsStatus != null ? traceVO.creditsStatus.name() : null;

      LoanUserCreditsInfoVO freshCreditsInfo = loanUserCreditsService
          .genLoanUserCreditsInfoByAccountId(traceVO.accountId);
      Long timeReapply = freshCreditsInfo != null ? freshCreditsInfo.timeReapply : null;
      vo.rejectType = buildRejectType(traceVO.creditsStatus, timeReapply);
      vo.timeReapply = REJECT_TYPE_SHORT.equals(vo.rejectType) ? timeReapply : null;

      MultiLoanStatus multiLoanStatus = multiLoanStatusService.getStatusOrNull(traceVO.accountId);
      vo.multiLoanStatus = multiLoanStatus != null ? multiLoanStatus.name() : null;

      vo.revolvingDay = findRiskOutputValue(traceVO.traceId, RiskOutputType.REVOLVING_LOAN_CONTROL_DAYS);
      vo.rejectOrder = buildRejectOrder(vo.traceResult, traceVO.orderId);

      vo.userGroup = buildCompleteUserGroupValue(userGroupResult);
      vo.userType = buildCompleteUserTypeValue(userGroupResult);

      vo.afRank = riskOutputService.findAfRankByTraceId(traceVO.traceId);
      vo.creditCategory = traceVO.riskType != null ? buildCreditCategory(traceVO.riskType) : null;
      vo.creditsQuota = buildCreditsQuotaInfo(freshCreditsInfo);
      vo.productTag = findRiskOutputValue(traceVO.traceId, RiskOutputType.PRODUCT_TAG);
      vo.rateTag = findRiskOutputValue(traceVO.traceId, RiskOutputType.RATE_TAG);
      vo.qualifiedLoanMarketEntranceByRisk = loanMarketUserQualifyService.checkQualifyOnly(traceVO);

      DwLogUtil.newLogByStr(getLogName(LogBusinessType.PANORAMA_RISK_COMPLETE), vo);
    } catch (Exception e) {
      log.error("logRiskCompleteEvent error, ecRiskId:{}, traceId:{}", traceVO.id, traceVO.traceId, e);
    }
  }

  private String buildRejectType(LoanCreditsStatus creditsStatus, Long timeReapply) {
    if (creditsStatus != LoanCreditsStatus.REJECTED) {
      return null;
    }
    if (timeReapply == null) {
      return null;
    }
    return timeReapply >= 0 ? REJECT_TYPE_SHORT : REJECT_TYPE_PERMANENT;
  }

  private String buildCreditCategory(LoanUserRiskType riskType) {
    return LoanUserRiskType.REVOLVING_LOAN_RISK_TYPE_LIST.contains(riskType) ? "REVOLVING" : "NON_REVOLVING";
  }

  private PanoramaUserGroupRiskValue buildSubmitUserGroupValue(UserGroupTriggerResult result) {
    if (result == null) {
      return null;
    }
    PanoramaUserGroupRiskValue value = new PanoramaUserGroupRiskValue();
    String oldGroup = result.oldUserGroup != null ? result.oldUserGroup.name() : null;
    String updatedGroup = result.updatedUserGroup != null ? result.updatedUserGroup.name() : oldGroup;
    value.oldUserGroupBeforeRisk = oldGroup;
    value.newUserGroupBeforeRisk = updatedGroup;
    return value;
  }

  private PanoramaUserGroupRiskValue buildCompleteUserGroupValue(UserGroupTriggerResult result) {
    if (result == null) {
      return null;
    }
    PanoramaUserGroupRiskValue value = new PanoramaUserGroupRiskValue();
    String oldGroup = result.oldUserGroup != null ? result.oldUserGroup.name() : null;
    value.userGroupAfterRisk = result.updatedUserGroup != null ? result.updatedUserGroup.name() : oldGroup;
    return value;
  }

  private PanoramaUserTypeRiskValue buildSubmitUserTypeValue(UserGroupTriggerResult result) {
    if (result == null) {
      return null;
    }
    PanoramaUserTypeRiskValue value = new PanoramaUserTypeRiskValue();
    String oldType = formatUserType(result.oldUserType);
    String updatedType = result.updatedUserType != null ? formatUserType(result.updatedUserType) : oldType;
    value.oldUserTypeBeforeRisk = oldType;
    value.newUserTypeBeforeRisk = updatedType;
    return value;
  }

  private PanoramaUserTypeRiskValue buildCompleteUserTypeValue(UserGroupTriggerResult result) {
    if (result == null) {
      return null;
    }
    PanoramaUserTypeRiskValue value = new PanoramaUserTypeRiskValue();
    String oldType = formatUserType(result.oldUserType);
    value.userTypeAfterRisk = result.updatedUserType != null ? formatUserType(result.updatedUserType) : oldType;
    return value;
  }

  /**
   * increaseQuotaExpireTime 取
   * loanUserTempCreditsVOForRisk.tempCreditsExpiredTime
   */
  private CreditsQuotaInfo buildCreditsQuotaInfo(LoanUserCreditsInfoVO creditsInfo) {
    if (creditsInfo == null) {
      return null;
    }
    CreditsQuotaInfo info = new CreditsQuotaInfo();
    info.fixedRiskCreditsQuota = creditsInfo.creditsQuota;
    info.experimentRiskCreditsQuota = creditsInfo.tempCreditsForNormalRisk;
    info.increaseRiskCreditsQuota = creditsInfo.tempCreditsForIncreaseCreditsRisk;
    info.increaseQuotaExpireTime = Optional.ofNullable(creditsInfo.loanUserAllTempCreditsVO)
        .map(all -> all.loanUserTempCreditsVOForRisk)
        .map(risk -> risk.tempCreditsExpiredTime)
        .orElse(null);
    return info;
  }

  private String formatUserType(String userTypeCode) {
    if (userTypeCode == null) {
      return null;
    }
    try {
      LoanUserTypeVO userTypeVO = loanUserTypeService.fromUserTypeCode(userTypeCode);
      if (userTypeVO != null && userTypeVO.name != null) {
        return userTypeCode + "-" + userTypeVO.name;
      }
    } catch (Exception e) {
      log.warn("formatUserType failed for code:{}", userTypeCode, e);
    }
    return userTypeCode;
  }

  private Boolean buildRejectOrder(Boolean traceResult, Long orderId) {
    if (!Boolean.TRUE.equals(traceResult) || orderId == null) {
      return null;
    }
    CashLoanOrderVO orderVO = ecOrderService.getOrderVOOrNull(orderId);
    return orderVO != null ? orderVO.status == CashLoanOrderStatus.REJECT : null;
  }

  private String findRiskOutputValue(Long traceId, RiskOutputType type) {
    return Optional.ofNullable(riskOutputService.findByTraceIdAndType(traceId, type))
        .map(RiskOutputResultRecord::getNewValue)
        .orElse(null);
  }

  private String getLogName(LogBusinessType type) {
    return SysEnvironment.isTest() ? type.logName + TEST_SUFFIX : type.logName;
  }
}
