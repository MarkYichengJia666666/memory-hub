package com.yqg.core.service.risk.facade.strategy;

import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.risk.enums.SubmitCreditsAdditionalInfoType;
import com.yqg.core.service.cashloan.CashLoanCreditsService;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.LoanUserTypeService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.mc.MarketingCenterClientService;
import com.yqg.core.service.notification.NotificationService;
import com.yqg.core.service.risk.facade.RiskFacadeTool;
import com.yqg.core.service.risk.facade.RiskMessageTool;
import com.yqg.core.service.risk.facade.TriggerRiskAfterCurrentRiskVO;
import com.yqg.core.service.risk.facade.enums.CreditCheckResult;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.risk.submitadditional.vo.SubmitCreditsAdditionalInfoVO;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

/**
 * @author chenxianrui
 * @date 2025/8/22
 */
@Slf4j
public abstract class BaseRiskMessageEventStrategy implements RiskMessageEventStrategy {

  @Autowired
  protected LoanAccountService loanAccountService;
  @Autowired
  protected EcOrderService ecOrderService;
  @Autowired
  protected CashLoanCreditsService cashLoanCreditsService;
  @Autowired
  protected LoanUserCreditsService loanUserCreditsService;
  @Autowired
  protected NotificationService notificationService;
  @Autowired
  protected LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  protected SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  protected LoanAccountModel loanAccountModel;
  @Autowired
  protected CashLoanService cashLoanService;
  @Autowired
  protected RiskFacadeTool riskFacadeTool;
  @Autowired
  protected RiskMessageTool riskMessageTool;
  @Autowired
  protected LoanUserTypeService loanUserTypeService;
  @Autowired
  protected LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  protected MarketingCenterClientService marketingCenterClientService;

  /**
   * 校验前置风控类型是否为对应的二次风控类型
   *
   * @param additionalInfo
   * @return
   */
  protected abstract boolean isValidSecondRiskType(SubmitCreditsAdditionalInfoVO additionalInfo);

  /**
   * 校验必须参数
   *
   * @param data
   * @return
   */
  protected abstract boolean validateParamPreconditions(LoanUserTagData data);

  /**
   * 成功消息（测额）
   *
   * @param traceVO
   * @param data
   */
  protected abstract void sendChangeCreditsSuccessMessage(LoanUserRiskTraceVO traceVO, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO);

  /**
   * 失败消息（测额）
   *
   * @param traceVO
   */
  protected abstract void sendChangeCreditsRejectionNotification(LoanUserRiskTraceVO traceVO, LoanUserTagData data);

  /**
   * 处理订单风控结果
   *
   * @param traceVO
   * @param data    用户标签数据
   */
  protected void handleOrderRisk(LoanUserRiskTraceVO traceVO, LoanUserTagData data) {
    boolean rejectResult = riskFacadeTool.isOrderRiskReject(traceVO, data);
    log.info("订单rejectResult：{}, traceId:{}, accountId:{}", rejectResult, traceVO.traceId, traceVO.accountId);
    // 发送拒绝消息
    if (rejectResult) {
      sendOrderRiskRejectNotification(traceVO, data);
      return;
    }
    // 发送成功消息
    sendOrderRiskSuccessNotification(traceVO, data);
  }

  /**
   * 成功消息（订单风控）
   *
   * @param traceVO
   * @param data
   */
  protected abstract void sendOrderRiskSuccessNotification(LoanUserRiskTraceVO traceVO, LoanUserTagData data);

  /**
   * 失败消息（订单风控）
   *
   * @param traceVO
   * @param data
   */
  protected abstract void sendOrderRiskRejectNotification(LoanUserRiskTraceVO traceVO, LoanUserTagData data);

  /**
   * 通过订单创建的结果和风控结果来判断发送哪种类型的消息
   *
   * @return 发送成功或者拒绝消息，或者是不发送
   */
  protected CreditCheckResult checkCreditsStatusAndOrderResult(LoanUserRiskTraceVO traceVO,
                                                               LoanUserTagData data) {
    if (riskFacadeTool.isCalcRiskReject(traceVO, data)) {
      return CreditCheckResult.REJECTED;
    }
    return CreditCheckResult.SUCCESS;
  }

  /**
   * 判断是否为测额风控（一次风控）
   */
  protected boolean isCreditRisk(LoanUserRiskType type) {
    return LoanUserRiskType.getAllCalcCreditsType().contains(type);
  }

  /**
   * 判断是否为订单风控（二次风控）
   */
  protected boolean isOrderRisk(LoanUserRiskType type) {
    return LoanUserRiskType.getAllOrderRiskTypes().contains(type);
  }

  /**
   * 模板方法：根据风控类型分发处理逻辑
   */
  @Override
  public void handlePostEvent(LoanUserRiskTraceVO traceVO, LoanUserTagData data, Boolean createOrderResult,
                              LoanUserCreditsInfoVO oldCreditsInfoVO, TriggerRiskAfterCurrentRiskVO triggerRiskAfterCurrentRiskVO) {
    boolean submitDegradeRisk = Objects.isNull(triggerRiskAfterCurrentRiskVO) ? false
        : triggerRiskAfterCurrentRiskVO.isSubmitDegradeRisk();
    if (submitDegradeRisk) {
      log.info("submitDegradeRisk is true. no need handlePostEvent，traceId:{}, accountId:{}", traceVO.traceId, traceVO.accountId);
      return;
    }
    //当前风控后置触发了循环风控，不发消息
    if (Objects.nonNull(triggerRiskAfterCurrentRiskVO)
        && Objects.nonNull(triggerRiskAfterCurrentRiskVO.isSubmitRevolvingRisk())
        && triggerRiskAfterCurrentRiskVO.isSubmitRevolvingRisk()) {
      return;
    }
    if (createOrderResult) {
      log.info("createOrderResult is true. no need handlePostEvent，traceId:{}, accountId:{}", traceVO.traceId, traceVO.accountId);
      return;
    }
    if (isCreditRisk(traceVO.riskType)) {
      handleCreditRisk(traceVO, data, oldCreditsInfoVO);
    } else if (isOrderRisk(traceVO.riskType)) {
      handleOrderRisk(traceVO, data);
    } else {
      log.warn("未知的风控类型: {}", traceVO.riskType);
    }
  }

  protected void processCreditRiskWhenCurrentDegradeAndPreTargetSecondRisk(LoanUserRiskTraceVO traceVO, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO) {
    CreditCheckResult checkResult = checkCreditsStatusAndOrderResult(traceVO, data);
    if (checkResult == CreditCheckResult.NEEDS_HANDLING) {
      return;
    }

    if (checkResult == CreditCheckResult.SUCCESS) {
      sendChangeCreditsSuccessMessage(traceVO, data, oldCreditsInfoVO);
      return;
    }

    sendChangeCreditsRejectionNotification(traceVO, data);
  }

  /**
   * 一次风控
   *
   * @param userTraceVO
   * @param data
   */
  protected void handleCreditRisk(LoanUserRiskTraceVO userTraceVO, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO) {
    //前置逻辑判断，比如续借需要判断data是否为null
    if (!validateParamPreconditions(data)) {
      return;
    }
    //测额被拒，并且未触发降级风控，才发送测额拒绝消息
    SubmitCreditsAdditionalInfoVO additionalInfo = submitCreditsAdditionalInfoService.fetchByLoanUserRiskIdAndType(userTraceVO.id, SubmitCreditsAdditionalInfoType.PRE_TRIGGER_RETRIEVAL);
    if (additionalInfo != null) {
      processCreditRiskWhenCurrentDegrade(userTraceVO, data, additionalInfo, oldCreditsInfoVO);
      return;
    }

    //当前风控非回捞降级风控
    processCreditRiskWhenCurrentNotDegrade(userTraceVO, data, oldCreditsInfoVO);
  }

  /**
   * 当前为测额风控，并且为回捞降级风控
   */
  protected void processCreditRiskWhenCurrentDegrade(LoanUserRiskTraceVO userTraceVO,
                                                     LoanUserTagData data, SubmitCreditsAdditionalInfoVO additionalInfo,
                                                     LoanUserCreditsInfoVO oldCreditsInfoVO) {
    if (isValidSecondRiskType(additionalInfo)) {
      //前置风控为目标二次风控
      processCreditRiskWhenCurrentDegradeAndPreTargetSecondRisk(userTraceVO, data, oldCreditsInfoVO);
      return;
    }
    //按通过拒绝正常发消息即可
    boolean reject = riskFacadeTool.isCalcRiskReject(userTraceVO, data);
    if (!reject) {
      sendChangeCreditsSuccessMessage(userTraceVO, data, oldCreditsInfoVO);
    } else {
      sendChangeCreditsRejectionNotification(userTraceVO, data);
    }
  }

  /**
   * 当前为测额风控，并且非回捞降级风控
   *
   * @param traceVO
   * @param data
   */
  protected void processCreditRiskWhenCurrentNotDegrade(LoanUserRiskTraceVO traceVO, LoanUserTagData data, LoanUserCreditsInfoVO oldCreditsInfoVO) {
    //判断通过还是拒绝
    // 通过，直接发消息
    boolean reject = riskFacadeTool.isCalcRiskReject(traceVO, data);
    if (!reject) {
      sendChangeCreditsSuccessMessage(traceVO, data, oldCreditsInfoVO);
      return;
    }
    // 拒绝，需要考虑后续是否触发降级风控，根据submitDegradeRisk决定
    sendChangeCreditsRejectionNotification(traceVO, data);
  }
}
