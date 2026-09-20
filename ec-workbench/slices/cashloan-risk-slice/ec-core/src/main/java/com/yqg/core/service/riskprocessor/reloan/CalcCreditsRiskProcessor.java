package com.yqg.core.service.riskprocessor.reloan;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.LoanAssertion;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.infos.PreTriggerRetrievalInfo;
import com.yqg.core.service.loan.infos.SubmitCreditsInfo;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.event.RiskEventService;
import com.yqg.core.service.risk.event.RiskEventType;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.PreTraceTriggerScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/29 11:27 上午
 */
@Slf4j
@Service
public class CalcCreditsRiskProcessor extends BaseReloanApplyCalcCreditsRiskProcessor {
  @Autowired
  private SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private RiskEventService riskEventService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private LoanAccountService loanAccountService;

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.CALC_CREDITS;
  }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord record, RiskProcessParam param) {
    if (param.preTraceTriggerScene == PreTraceTriggerScene.RETRIEVAL) {
      LoanAssertion.assertAccountAndAppInReloanCreditsStatus(record, LoanCreditsStatus.REJECTED);
      return;
    }

    //需要使用其中的检查
    checkAndGetCashLoanCalcCreditsVO(record);
  }

  @Override
  protected void afterLogLoanUserRiskTrace(RiskProcessParam param, LoanUserRiskTraceRecord record) {
    if (param.lastTraceId == null) {
      return;
    }
    if (!isDegrade(param, record.getTraceId())) {
      return;
    }

    LoanUserRiskTraceVO lastRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(param.lastTraceId);

    //提交一次风控回捞流程，需要记录上次被拒的风控数据
    PreTriggerRetrievalInfo info = PreTriggerRetrievalInfo.from(param.lastTraceId, param.lastTraceType, lastRiskTraceVO.orderId);
    submitCreditsAdditionalInfoService.savePreTriggerRetrievalInfoIgnoreExceptionWithoutTraceId(param.accountId, param.preLastRiskId, record.getId(), info, getLoanUserRiskType());
  }

  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    super.preAdditionalProcess(param);
  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserCreditsInfoVO loanUserCreditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(param.accountId);
    riskEventService.publishEvent(RiskEventType.CREDIT_CALC_INIT, getLoanUserRiskType(), traceVO.id, loanUserCreditsInfoVO);
    if (param.needSaveContextInfo && Objects.nonNull(param.environmentInfo) && Objects.nonNull(param.terminalInfo)) {
      SubmitCreditsInfo submitCreditsInfo = new SubmitCreditsInfo();
      submitCreditsInfo.terminalInfo = param.terminalInfo;
      submitCreditsInfo.environmentInfo = param.environmentInfo;
      submitCreditsAdditionalInfoService.saveContextInfoIgnoreException(param.accountId, traceVO.getId(), submitCreditsInfo, getLoanUserRiskType());
    }
    if (isDegrade(param, traceVO.id)) {
      LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceVO.id);
      submitCreditsAdditionalInfoService.updatePreTriggerRetrievalTraceIdOrThrow(traceVO.id, loanUserRiskTraceVO.id);
    }
  }
}
