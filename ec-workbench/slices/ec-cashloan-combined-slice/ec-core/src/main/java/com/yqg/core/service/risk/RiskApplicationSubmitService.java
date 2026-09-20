package com.yqg.core.service.risk;

import com.alibaba.excel.util.CollectionUtils;
import com.google.common.collect.ImmutableList;
import com.yqg.core.model.generated.tables.records.LoanAccountAdditionalInfoRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.mongo.MongoSubmitRiskParamModel;
import com.yqg.core.model.sql.loan.account.LoanAccountAdditionalInfoModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.core.service.riskprocessor.infra.RiskProcessorFactory;
import com.yqg.ec.common.enums.LoanAccountAdditionalTypeEnum;
import com.yqg.ec.common.enums.risk.CallBackTraceVO;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.risk.riskflow.trace.RiskFlowTraceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/8/25 11:17 上午
 */
@Service
public class RiskApplicationSubmitService {
  @Autowired
  private RiskProcessorFactory riskProcessorFactory;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  private LoanAccountAdditionalInfoModel loanAccountAdditionalInfoModel;
  @Autowired
  private MongoSubmitRiskParamModel mongoSubmitRiskParamModel;

  public RiskFlowTraceVO submitRiskApplication(RiskProcessParam riskProcessParam, LoanUserRiskType loanUserRiskType) {
    BaseRiskProcessor riskProcessor = riskProcessorFactory.getInstance(loanUserRiskType);
    return riskProcessor.submitApplication(riskProcessParam);
  }

  public void dealAfterGetTraceVO(LoanUserRiskTraceRecord riskTraceRecord, CallBackTraceVO callBackTraceVO) {
    BaseRiskProcessor riskProcessor = riskProcessorFactory.getInstance(LoanUserRiskType.fromCode(riskTraceRecord.getRiskType()));
    RiskFlowTraceVOV2 riskFlowTraceVO = RiskFlowTraceVOV2.fromInit(callBackTraceVO.id, callBackTraceVO.riskFlowId, callBackTraceVO.eventId, callBackTraceVO.timeCreated);

    RiskProcessParam riskProcessParam = loanUserRiskTraceService.getRiskProcessParam(riskTraceRecord);
    riskProcessor.dealAfterGetTraceVO(riskProcessParam, riskFlowTraceVO);
  }

  private RiskProcessParam getRiskProcessParam(LoanUserRiskTraceRecord riskTraceRecord) {
    LoanAccountAdditionalInfoRecord additionalInfoRecord = findSubmitRiskParamRecordOrNull(riskTraceRecord);
    if (Objects.isNull(additionalInfoRecord)) {
      throw EcException.error("can't get submitRisk for user, do not find addition info. loanAccountId is  {}", riskTraceRecord.getLoanAccountId());
    }
    RiskProcessParam riskProcessParam = mongoSubmitRiskParamModel.findByObjectIdOrNull(additionalInfoRecord.getValue());
    if (Objects.isNull(riskProcessParam)) {
      throw EcException.error("can't get submitRisk for user, do not find param info. loanAccountId is  {}", riskTraceRecord.getLoanAccountId());
    }
    return riskProcessParam;
  }

  private LoanAccountAdditionalInfoRecord findSubmitRiskParamRecordOrNull(LoanUserRiskTraceRecord riskTraceRecord) {
    List<LoanAccountAdditionalInfoRecord> additionalInfoRecordList =
        loanAccountAdditionalInfoModel.findByAccountIdsAndTypeAndExternalIds(ImmutableList.of(riskTraceRecord.getLoanAccountId()),
            LoanAccountAdditionalTypeEnum.SUBMIT_RISK_PARAM,
            ImmutableList.of(riskTraceRecord.getId().toString()));

    if (CollectionUtils.isEmpty(additionalInfoRecordList) || additionalInfoRecordList.size() != 1) {
      return null;
    }
    return additionalInfoRecordList.get(0);
  }

  public RiskProcessParam getRiskProcessParamByTraceId(Long traceId) {
    LoanUserRiskTraceRecord riskTraceRecord = loanUserRiskTraceModel.findByTraceId(traceId);
    if (Objects.isNull(riskTraceRecord)) {
      throw EcException.error("can't get trace record, traceId:{}", traceId);
    }
    return getRiskProcessParam(riskTraceRecord);
  }

  /**
   * 按 traceId 取风控参数，任一环节缺数据都返回 null 而非抛异常。
   * 供归因特征查询等「取不到就降级为空值、不能打断调用方」的只读场景使用。
   */
  public RiskProcessParam getRiskProcessParamByTraceIdOrNull(Long traceId) {
    if (Objects.isNull(traceId)) {
      return null;
    }
    LoanUserRiskTraceRecord riskTraceRecord = loanUserRiskTraceModel.findByTraceId(traceId);
    if (Objects.isNull(riskTraceRecord)) {
      return null;
    }
    return getRiskProcessParamOrNull(riskTraceRecord);
  }

  /**
   * 同 {@link #getRiskProcessParamByTraceIdOrNull}，供调用方已经持有 trace 记录时使用，省掉一次 trace 表查询。
   */
  public RiskProcessParam getRiskProcessParamOrNull(LoanUserRiskTraceRecord riskTraceRecord) {
    if (Objects.isNull(riskTraceRecord)) {
      return null;
    }
    LoanAccountAdditionalInfoRecord additionalInfoRecord = findSubmitRiskParamRecordOrNull(riskTraceRecord);
    if (Objects.isNull(additionalInfoRecord)) {
      return null;
    }
    return mongoSubmitRiskParamModel.findByObjectIdOrNull(additionalInfoRecord.getValue());
  }
}
