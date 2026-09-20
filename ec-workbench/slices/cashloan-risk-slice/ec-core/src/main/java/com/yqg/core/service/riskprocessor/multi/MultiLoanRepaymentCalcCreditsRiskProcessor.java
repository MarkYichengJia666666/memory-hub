package com.yqg.core.service.riskprocessor.multi;

import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.infos.PreTriggerRetrievalInfo;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.stereotype.Service;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/29 11:19 上午
 */
@Service
public class MultiLoanRepaymentCalcCreditsRiskProcessor extends BaseMultiLoanCalcCreditsRiskProcessor {

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.MULTI_LOAN_CALC_CREDITS;
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
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    super.postAdditionalProcess(param, traceVO);
    if (isDegrade(param, traceVO.id)) {
      LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceVO.id);
      submitCreditsAdditionalInfoService.updatePreTriggerRetrievalTraceIdOrThrow(traceVO.id, loanUserRiskTraceVO.id);
    }
  }
}
