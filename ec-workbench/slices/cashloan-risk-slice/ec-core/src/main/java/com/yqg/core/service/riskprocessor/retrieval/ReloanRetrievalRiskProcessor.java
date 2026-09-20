package com.yqg.core.service.riskprocessor.retrieval;

import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.LoanUserTypeChangeReason;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.LoanAssertion;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.infos.PreTriggerRetrievalInfo;
import com.yqg.core.service.loan.infos.SubmitCreditsInfo;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import groovy.util.logging.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 复贷结清回捞一次风控提交流程处理
 */
@Service
@Slf4j
public class ReloanRetrievalRiskProcessor extends BaseRiskProcessor {

  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private LoanAccountService loanAccountService;

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.RELOAN_RETRIEVAL;
  }

  /**
   * 前置判断，上次的trace数据必须符合回捞一次风控的前置风控类型要求，并且状态必须是拒绝的操作才可以
   *
   * @param creditsInfoRecord
   * @param param
   */
  @Override
  protected void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param) {
    LoanAssertion.assertAccountAndAppInReloanCreditsStatus(creditsInfoRecord, LoanCreditsStatus.REJECTED);
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(creditsInfoRecord.getLoanAccountId());
    if (loanUserRiskTraceVO == null) {
      throw EcException.error("reloan user account {} is not exists in last finish credits risk trace!!!", creditsInfoRecord.getLoanAccountId());
    }
    if (!isValidCreditsStatusForSubmitRetrieval(loanUserRiskTraceVO.creditsStatus)
        || !LoanUserRiskType.getReloanRetrievalPreRiskType().contains(loanUserRiskTraceVO.riskType)) {
      throw EcException.error("reloan user account {} is not rejected and preconditions loan retrieval risk type in last finish credits [{}] risk trace!!!",
          creditsInfoRecord.getLoanAccountId(), StringUtils.join(LoanUserRiskType.getReloanRetrievalPreRiskType(), ","));
    }
  }

  private boolean isValidCreditsStatusForSubmitRetrieval(LoanCreditsStatus creditsStatus) {
    return creditsStatus == LoanCreditsStatus.REJECTED || creditsStatus == LoanCreditsStatus.MANUAL_REVIEW;
  }

  @Override
  protected void afterLogLoanUserRiskTrace(RiskProcessParam param, LoanUserRiskTraceRecord record) {
    LoanUserRiskTraceVO riskTraceVO = loanUserRiskTraceService.findByTraceIdOrNull(param.lastTraceId);
    Long orderId = null;
    if (riskTraceVO != null) {
      orderId = riskTraceVO.orderId;
    }
    //提交一次风控回捞流程，需要记录上次被拒的风控数据
    PreTriggerRetrievalInfo info = PreTriggerRetrievalInfo.from(param.lastTraceId, param.lastTraceType, orderId);
    submitCreditsAdditionalInfoService.savePreTriggerRetrievalInfoIgnoreExceptionWithoutTraceId(param.accountId, param.preLastRiskId, record.getId(), info, getLoanUserRiskType());
  }

  /**
   * 查询授信信息中，状态必须为拒绝，调整为授信中
   *
   * @param param
   */
  @Override
  protected void preAdditionalProcess(RiskProcessParam param) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReLoanRetrievalCreditsApplication(creditsInfoRecord);
    //针对回捞用户，调整用户类型为回捞初始化的用户类型，重新初始化usertype
    //调整usertype
    loanAccountService.updateUserTypeWithoutCheckWithTraceId(param.accountId, null, LoanUserTypeVO.FIRST_RETRIEVAL_INIT, LoanUserTypeChangeReason.FIRST_RETRIEVAL_CHANGE);
  }

  /**
   * 修改用户授信表中的trace数据记录，更新reloantraceid
   *
   * @param param
   * @param traceVO
   */
  @Override
  protected void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    userCreditsInfoModel.submitReloanCreditsApplication(creditsInfoRecord, traceVO.id);
  }

  /**
   * 记录风控额外信息
   *
   * @param param
   * @param traceVO
   */
  @Override
  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    if (param.needSaveContextInfo && Objects.nonNull(param.terminalInfo) && Objects.nonNull(param.environmentInfo)) {
      SubmitCreditsInfo submitCreditsInfo = new SubmitCreditsInfo();
      submitCreditsInfo.terminalInfo = param.terminalInfo;
      submitCreditsInfo.environmentInfo = param.environmentInfo;
      submitCreditsAdditionalInfoService.saveContextInfoIgnoreException(param.accountId, traceVO.getId(), submitCreditsInfo, this.getLoanUserRiskType());
    }
    savePreTriggerRetrievalInfoWithTraceid(traceVO);
  }

  /**
   * 记录触发回捞风控的前置风控操作数据
   *
   * @param traceVO
   */
  private void savePreTriggerRetrievalInfoWithTraceid(RiskFlowTraceVOV2 traceVO) {
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByTraceIdOrThrow(traceVO.id);
    submitCreditsAdditionalInfoService.updatePreTriggerRetrievalTraceIdOrThrow(traceVO.id, loanUserRiskTraceVO.id);
  }
}
