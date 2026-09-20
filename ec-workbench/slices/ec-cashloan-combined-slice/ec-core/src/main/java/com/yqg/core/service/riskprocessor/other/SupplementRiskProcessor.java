package com.yqg.core.service.riskprocessor.other;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.cashloan.ordercenter.CashLoanUserSupplementCreateOrderService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.infos.SubmitCreditsInfo;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SupplementRiskProcessor extends BaseRiskProcessor {

  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private CashLoanUserSupplementCreateOrderService supplementCreateOrderService;
  @Autowired
  private LoanAccountService loanAccountService;

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.SUPPLEMENT_INFO_BEFORE_CREATE_ORDER;
  }

  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    if (Objects.nonNull(creditsInfoRecord.getReloanStatus())
        && LoanCreditsStatus.ACCEPTED == LoanCreditsStatus.fromCode(creditsInfoRecord.getReloanStatus())) {
      userCreditsInfoModel.updateReloanStatus(creditsInfoRecord, LoanCreditsStatus.IN_REVIEW);
      LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(param.accountId);
      supplementCreateOrderService.insertOrUpdateSupplementTraceInfo(loanAccountVO, null, param.cashLoanCreateOrderRequestVO);
      return;
    }

    if (Objects.isNull(creditsInfoRecord.getReloanStatus())
        && Objects.nonNull(creditsInfoRecord.getCreditsStatus())
        && LoanCreditsStatus.ACCEPTED == LoanCreditsStatus.fromCode(creditsInfoRecord.getCreditsStatus())) {
      userCreditsInfoModel.updateStatus(creditsInfoRecord, LoanCreditsStatus.IN_REVIEW);
      LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(param.accountId);
      supplementCreateOrderService.insertOrUpdateSupplementTraceInfo(loanAccountVO, null, param.cashLoanCreateOrderRequestVO);
    }
 }

  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    //这个函数同时校验首复贷的情况
    assertCreditsStatus(creditsInfoRecord, LoanCreditsStatus.ACCEPTED);
    EcAsserts.assertTrueOrThrowWarn(
        0 == ecOrderService.countOrder(creditsInfoRecord.getLoanAccountId(), CashLoanOrderStatus.UNDONE_STATUSES_WITHOUT_READY),
        EcExceptionType.RISK_SUBMIT_CONDITION_NOT_MET,
        TT.gen("提交补件风控条件不满足"),
        "can not submit supplement info before create order with undone orders,accountId is {}", creditsInfoRecord.getLoanAccountId()
    );
  }

  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {

  }

  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    updateSupplementTraceInfo(param, traceVO);
  }

  private void updateSupplementTraceInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    if (param.needSaveContextInfo && Objects.nonNull(param.environmentInfo) && Objects.nonNull(param.terminalInfo)) {
      SubmitCreditsInfo submitCreditsInfo = new SubmitCreditsInfo();
      submitCreditsInfo.environmentInfo = param.environmentInfo;
      submitCreditsInfo.terminalInfo = param.terminalInfo;
      submitCreditsAdditionalInfoService.saveContextInfoIgnoreException(param.accountId, traceVO.getId(), submitCreditsInfo, LoanUserRiskType.SUPPLEMENT_INFO_BEFORE_CREATE_ORDER);
    }
    LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(param.accountId);
    supplementCreateOrderService.updateTraceInfo(loanAccountVO.userId, traceVO.id);
  }
}
