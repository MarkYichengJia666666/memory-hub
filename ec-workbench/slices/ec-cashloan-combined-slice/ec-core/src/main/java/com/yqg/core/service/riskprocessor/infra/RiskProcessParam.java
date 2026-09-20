package com.yqg.core.service.riskprocessor.infra;

import com.yqg.core.model.sql.loan.account.enums.AuthStep;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTriggerSource;
import com.yqg.core.model.sql.loanusertrace.TriggerSubType;
import com.yqg.core.model.sql.loanusertrace.TriggerType;
import com.yqg.core.service.cashloan.vo.CashLoanCreateOrderRequestVO;
import com.yqg.core.service.loan.infos.SubmitCreditsInfo;
import com.yqg.core.service.notification.param.system.enums.BatchTaskTypeEnum;
import com.yqg.core.util.env.EnvironmentInfo;
import com.yqg.core.util.env.TerminalInfo;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.PreTraceTriggerScene;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Zoran Zhang
 * @Description: 风控提交时相关的一些固定基本属性
 * @date 2021/7/23 1:58 下午
 */
public class RiskProcessParam {
  public Long accountId;
  public Long orderId;
  public Long userId;
  public SDKType sdkType;
  public TriggerType triggerType;
  public SubmitExtraInfo extraInfo;
  public Boolean isNotBatchRisk = true;
  public String channel;
  public Long build;
  public Long riskFlowId;
  public Long eventTypeId;
  public EnvironmentInfo environmentInfo;
  public TerminalInfo terminalInfo;
  public CashLoanCreateOrderRequestVO cashLoanCreateOrderRequestVO;
  public Long loanUserExtraInfoId;
  public Long activityOrderId;
  public Long riskIncreaseCreditsReviewLog;
  public Boolean needSaveContextInfo = false;
  public AuthStep step;
  public Long reviewLogRecordId;
  public BatchTaskTypeEnum batchTaskType;
  public Long logId;
  //lastTraceId、lastTraceType这两个字段只有在提交回捞风控的时候才会写入
  public Long lastTraceId;
  public LoanUserRiskType lastTraceType;
  public Long preLastRiskId;
  @Deprecated
  public PreTraceTriggerScene preTraceTriggerScene;
  //该字段目前只在riskType为复贷测额时赋值，记录提交风控前的额度是否处于失效
  public Boolean riskCreditsExpire;
  public LoanUserRiskSubmitScene submitScene;
  public LoanUserRiskTriggerSource triggerSource;
  //触发来源外部 id：指向对象由 triggerSource 决定（如订单 id/还款表 id/前置 trace 主键等），可能为字符串，可空
  public String triggerSourceExternalId;

  @Deprecated
  public static RiskProcessParam from(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo) {
    return from(accountId, null, sdkType, triggerType, submitExtraInfo, null);
  }

  public static RiskProcessParam from(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, LoanUserRiskSubmitScene submitScene) {

    RiskProcessParam vo = from(accountId, null, sdkType, triggerType, submitExtraInfo, submitScene);
    return vo;
  }

  public static RiskProcessParam fromTriggerSource(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, LoanUserRiskSubmitScene submitScene, RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = from(accountId, null, sdkType, triggerType, submitExtraInfo, submitScene);
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam from(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, Boolean riskCreditsExpire, LoanUserRiskSubmitScene submitScene, RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = from(accountId, null, sdkType, triggerType, submitExtraInfo, submitScene);
    vo.riskCreditsExpire = riskCreditsExpire;
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam fromWithNeedSaveContextInfo(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, Boolean needSaveContextInfo, Boolean riskCreditsExpire, LoanUserRiskSubmitScene submitScene, RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = from(accountId, null, sdkType, triggerType, submitExtraInfo, null);
    vo.needSaveContextInfo = needSaveContextInfo;
    vo.riskCreditsExpire = riskCreditsExpire;
    vo.submitScene = submitScene;
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam from(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, SubmitCreditsInfo submitCreditsInfo, CashLoanCreateOrderRequestVO cashLoanCreateOrderRequestVO, LoanUserRiskSubmitScene submitScene, RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam riskProcessParam = from(accountId, null, sdkType, triggerType, submitExtraInfo, submitScene);
    riskProcessParam.environmentInfo = submitCreditsInfo.environmentInfo;
    riskProcessParam.terminalInfo = submitCreditsInfo.terminalInfo;
    riskProcessParam.cashLoanCreateOrderRequestVO = cashLoanCreateOrderRequestVO;
    return riskProcessParam.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam from(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, String channel) {
    RiskProcessParam vo = from(accountId, null, sdkType, triggerType, submitExtraInfo, null);
    vo.channel = channel;
    return vo;
  }

  /**
   * 下单触发二次/复贷测额风控：在既有订单风控参数基础上透传环境信息，写入表2（submit_risk_param）。
   * submitCreditsInfo 为 null 时 environmentInfo/terminalInfo 保持 null。
   */
  public static RiskProcessParam from(Long accountId, Long orderId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo extraInfo, Long build, LoanUserRiskSubmitScene riskSubmitScene, RiskTriggerSourceInfo triggerSourceInfo, SubmitCreditsInfo submitCreditsInfo) {
    RiskProcessParam param = RiskProcessParam.from(accountId, orderId, sdkType, triggerType, extraInfo, riskSubmitScene);
    param.build = build;
    if (submitCreditsInfo != null) {
      param.environmentInfo = submitCreditsInfo.environmentInfo;
      param.terminalInfo = submitCreditsInfo.terminalInfo;
    }
    return param.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam from(Long accountId, Long orderId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo extraInfo, LoanUserRiskSubmitScene riskSubmitScene) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.accountId = accountId;
    vo.orderId = orderId;
    vo.sdkType = sdkType;
    vo.triggerType = triggerType;
    vo.extraInfo = extraInfo;
    vo.submitScene = riskSubmitScene;
    return vo;
  }

  public static RiskProcessParam fromForIncreaseCredits(Long loanUserExtraInfoId, Long accountId, SDKType sdkType, TriggerType triggerType, LoanUserRiskSubmitScene riskSubmitScene, SubmitExtraInfo submitExtraInfo, RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.loanUserExtraInfoId = loanUserExtraInfoId;
    vo.accountId = accountId;
    vo.sdkType = sdkType;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.submitScene = riskSubmitScene;
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam fromForActivityOrderCredits(Long activityOrderId, Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, LoanUserRiskSubmitScene riskSubmitScene, RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.activityOrderId = activityOrderId;
    vo.accountId = accountId;
    vo.sdkType = sdkType;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.submitScene = riskSubmitScene;
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam fromForIncreaseCreditsReview(Long reviewLogId, Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, LoanUserRiskSubmitScene scene) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.riskIncreaseCreditsReviewLog = reviewLogId;
    vo.accountId = accountId;
    vo.sdkType = sdkType;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.submitScene = scene;
    return vo;
  }

  public static RiskProcessParam fromForCalcCredits(SubmitCreditsInfo submitCreditsInfo,
                                                    Long accountId, SDKType sdkType,
                                                    TriggerType triggerType,
                                                    SubmitExtraInfo submitExtraInfo,
                                                    Boolean needSaveContextInfo,
                                                    Boolean riskCreditsExpire,
                                                    Long build,
                                                    LoanUserRiskSubmitScene riskSubmitScene,
                                                    RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.environmentInfo = submitCreditsInfo.environmentInfo;
    vo.terminalInfo = submitCreditsInfo.terminalInfo;
    vo.accountId = accountId;
    vo.sdkType = sdkType;
    vo.build = build;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.needSaveContextInfo = needSaveContextInfo;
    vo.riskCreditsExpire = riskCreditsExpire;
    vo.submitScene = riskSubmitScene;
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam fromForLoan(Long accountId, SDKType sdkType, TriggerType triggerType,
                                             SubmitExtraInfo submitExtraInfo, String channel,
                                             LoanUserRiskSubmitScene submitScene,
                                             EnvironmentInfo environmentInfo, TerminalInfo terminalInfo,
                                             RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.accountId = accountId;
    vo.sdkType = sdkType;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.environmentInfo = environmentInfo;
    vo.terminalInfo = terminalInfo;
    vo.channel = channel;
    vo.submitScene = submitScene;
    vo.needSaveContextInfo = true;
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam fromForReapply(Long accountId, SDKType sdkType, TriggerType triggerType,
                                                SubmitExtraInfo submitExtraInfo,
                                                EnvironmentInfo environmentInfo, TerminalInfo terminalInfo,
                                                Boolean needSaveContextInfo,
                                                LoanUserRiskSubmitScene submitScene,
                                                RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.accountId = accountId;
    vo.sdkType = sdkType;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.environmentInfo = environmentInfo;
    vo.terminalInfo = terminalInfo;
    vo.needSaveContextInfo = needSaveContextInfo;
    vo.submitScene = submitScene;
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  public static RiskProcessParam fromBeforeAuthFinishCredits(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, AuthStep step, LoanUserRiskSubmitScene submitScene) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.accountId = accountId;
    vo.sdkType = sdkType;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.step = step;
    vo.submitScene = submitScene;
    return vo;
  }

  public static RiskProcessParam fromWithReviewLogId(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, Long reviewLogRecordId, LoanUserRiskSubmitScene submitScene) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.accountId = accountId;
    vo.sdkType = sdkType;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.reviewLogRecordId = reviewLogRecordId;
    vo.submitScene = submitScene;
    return vo;
  }

  public static RiskProcessParam fromBatchTrigger(Long accountId,
                                                  TriggerType triggerType,
                                                  SubmitExtraInfo submitExtraInfo,
                                                  Long riskFlowId, Long eventTypeId,
                                                  BatchTaskTypeEnum batchTaskTypeEnum,
                                                  Long logId,
                                                  Boolean riskCreditsExpire,
                                                  LoanUserRiskSubmitScene submitScene,
                                                  RiskTriggerSourceInfo triggerSourceInfo) {
    RiskProcessParam vo = new RiskProcessParam();
    vo.accountId = accountId;
    vo.triggerType = triggerType;
    vo.extraInfo = submitExtraInfo;
    vo.isNotBatchRisk = false;
    vo.riskFlowId = riskFlowId;
    vo.eventTypeId = eventTypeId;
    vo.batchTaskType = batchTaskTypeEnum;
    vo.logId = logId;
    vo.riskCreditsExpire = riskCreditsExpire;
    vo.submitScene = submitScene;
    return vo.applyTriggerSource(triggerSourceInfo);
  }

  /**
   * 构造期统一写入触发来源与触发来源外部 id：由各 from* 工厂在返回前调用，
   * 保证「来源」随 RiskProcessParam 一同构造，避免构造后遗漏赋值（triggerSourceInfo 为空时不覆盖）。
   */
  private RiskProcessParam applyTriggerSource(RiskTriggerSourceInfo triggerSourceInfo) {
    if (triggerSourceInfo != null) {
      this.triggerSource = triggerSourceInfo.source;
      this.triggerSourceExternalId = triggerSourceInfo.externalId;
    }
    return this;
  }

  public TriggerSubType calcTriggerSubType() {
    if (TriggerType.MANUAL == triggerType && !Optional.ofNullable(extraInfo).map(item -> item.submitFromUser).orElse(true)) {
      return TriggerSubType.MANUAL_AUTO;
    }

    if (Objects.nonNull(extraInfo) && Objects.nonNull(extraInfo.triggerSubType)) {
      return extraInfo.triggerSubType;
    }

    return triggerType.subType;
  }

  public static RiskProcessParam fromForKredit(Long accountId, SDKType sdkType, TriggerType triggerType, SubmitExtraInfo submitExtraInfo, Boolean riskCreditsExpire) {
    RiskProcessParam riskProcessParam = from(accountId, null, sdkType, triggerType, submitExtraInfo, null);
    riskProcessParam.riskCreditsExpire = riskCreditsExpire;
    return riskProcessParam;
  }
}
