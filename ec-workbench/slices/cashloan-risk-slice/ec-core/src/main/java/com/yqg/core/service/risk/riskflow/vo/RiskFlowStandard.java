package com.yqg.core.service.risk.riskflow.vo;

import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.risk.riskflow.IEventType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shubo
 * @date 05/11/24 17.22
 */
public class RiskFlowStandard {
  /**
   * 「不存在启用的 default riskFlow」异常 message 前缀。
   * 注意：旧链路（com.yqg.risk JAR 内的 RiskFlowStandard）抛出的裸 RuntimeException 也使用同一前缀，
   * isNoDefaultEnabledRiskFlowError 依赖该前缀识别旧链路异常，二者必须保持一致。
   */
  public static final String NO_DEFAULT_ENABLED_RISK_FLOW_ERROR_MSG = "不存在正在启用的default的riskFlow";

  public Long date;
  public List<RiskFlow> riskFlows;
  public RiskFlow defaultFlow;
  public Map<Long, RiskFlow> riskFlowWithId;
  public IEventType eventType;
  public Boolean isSwitchOn;

  public static RiskFlowStandard from(List<RiskFlow> riskFlowList, IEventType eventType) {
    RiskFlowStandard standardVO = new RiskFlowStandard();
    standardVO.riskFlows = riskFlowList;
    standardVO.riskFlowWithId = new HashMap<>();
    standardVO.eventType = eventType;
    BigDecimal totalPercentWithoutDefault = BigDecimal.ZERO;
    for (RiskFlow riskFlow : riskFlowList) {
      standardVO.riskFlowWithId.put(riskFlow.id, riskFlow);
      if (riskFlow.isDefault) {
        standardVO.defaultFlow = riskFlow;
      } else {
        totalPercentWithoutDefault = BigDecimalHelper.addWithNullAsZeroAndScale(totalPercentWithoutDefault, riskFlow.experimentPercentage);
      }
    }
    if (standardVO.defaultFlow == null) {
      throw EcException.error(EcExceptionType.CASH_LOAN_DOES_NOT_EXIST_DEFAULT_ENABLED_RISK_FLOW,
          NO_DEFAULT_ENABLED_RISK_FLOW_ERROR_MSG + ",eventId = {}", eventType.getId());
    }
    standardVO.defaultFlow.experimentPercentage = BigDecimalHelper.greaterThanOrEqual(totalPercentWithoutDefault, BigDecimal.ONE) ? BigDecimal.ZERO
        : BigDecimalHelper.subtractNullAsZeroAndScale(BigDecimal.ONE, totalPercentWithoutDefault, 4);
    return standardVO;
  }

  public static RiskFlowStandard from(Long date, List<RiskFlow> riskFlows, IEventType eventType) {
    RiskFlowStandard standard = from(riskFlows, eventType);
    standard.date = date;
    return standard;
  }

  public static RiskFlowStandard from(Long date, Boolean isSwitchOn, List<RiskFlow> riskFlows, IEventType eventType) {
    RiskFlowStandard standard = from(riskFlows, eventType);
    standard.date = date;
    standard.isSwitchOn = isSwitchOn;
    return standard;
  }

  /**
   * 判断异常是否为「不存在启用的 default riskFlow」配置类错误。
   * 同时兼容新链路（EC 侧抛出的 EcException，类型 CASH_LOAN_DOES_NOT_EXIST_DEFAULT_ENABLED_RISK_FLOW）
   * 与旧链路（com.yqg.risk JAR 内抛出的裸 RuntimeException，仅靠 message 前缀识别）。
   * 会沿 cause 链向上查找，避免被包装异常遮蔽。
   */
  public static boolean isNoDefaultEnabledRiskFlowError(Throwable e) {
    Throwable current = e;
    while (current != null) {
      if (current instanceof EcException
          && ((EcException) current).exceptionType == EcExceptionType.CASH_LOAN_DOES_NOT_EXIST_DEFAULT_ENABLED_RISK_FLOW) {
        return true;
      }
      String message = current.getMessage();
      if (message != null && message.startsWith(NO_DEFAULT_ENABLED_RISK_FLOW_ERROR_MSG)) {
        return true;
      }
      Throwable cause = current.getCause();
      if (cause == current) {
        break;
      }
      current = cause;
    }
    return false;
  }
}
