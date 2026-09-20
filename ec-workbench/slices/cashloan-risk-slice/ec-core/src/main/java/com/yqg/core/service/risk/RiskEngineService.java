package com.yqg.core.service.risk;

import com.yqg.core.aop.RunInTransaction;
import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.LoanUserCreditsInfoModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.risk.RiskFlowTraceModel;
import com.yqg.core.service.cashloan.enums.HitConsistentHashPrefix;
import com.yqg.core.service.cashloan.risk.vo.EventTypeVO;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.risk.feature.CallRiskApiPosition;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.riskprocessor.infra.RiskFlowTraceVOV2;
import com.yqg.core.util.EcHashUtil;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.overseasrisk.client.mesh.api.IRiskFlowTraceService;

import com.yqg.risk.orm.sql.tables.records.RiskFlowTraceRecord;
import com.yqg.risk.riskflow.*;
import com.yqg.risk.riskflow.enums.RiskFlowTraceStatus;
import com.yqg.risk.riskflow.trace.RiskFlowTraceVO;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by ember on 2017/1/12.
 */
@Service
@Slf4j
public class RiskEngineService {
  @Autowired
  private RiskFlowTraceModel riskFlowTraceModel;
  @Autowired
  private RiskFlowService riskFlowService;
  @Autowired
  private RiskFlowConfig riskFlowConfig;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private IRiskFlowTraceService iRiskFlowTraceService;
  @Autowired
  private LoanUserCreditsInfoModel loanUserCreditsInfoModel;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;


  public RiskFlowTraceVO submit(EventTypeVO eventVO, LoanAccountVO accountVO, Long riskFlowId, Long orderId) {
    Map<String, Object> props = initRiskProps(accountVO, orderId);
    props.put(RiskKey.TRACE_RUN_TYPE, RiskFlowTraceRunType.NEW_ENGINE_DECISION);
    RiskFlowTimeLine timeLine = isHitTimeBaseLine(accountVO.id, eventVO.riskType) ? RiskFlowTimeLine.NEW_V2 : RiskFlowTimeLine.OLD;
    props.put(RiskKey.TIME_LINE, timeLine);
    RiskFlowUseNewTg useNewTg = isHitUseNewTg(accountVO.id) ? RiskFlowUseNewTg.TRUE : RiskFlowUseNewTg.FALSE;
    props.put(RiskKey.USE_NEW_TG, useNewTg);
    RiskDatasourceRunType datasourceRunType = isHitAsyncExecuteDatasource(accountVO.id, eventVO.riskType) ? RiskDatasourceRunType.SYNC_DATASOURCE_PARALLEL : RiskDatasourceRunType.SYNC_DATASOURCE;
    props.put(RiskKey.DATASOURCE_RUN_TYPE, datasourceRunType);
    RiskFlowTraceRecord record = riskFlowTraceModel.init(props, eventVO, riskFlowId);
    log.info("submit risk flow trace, traceId = {}, riskFlowId = {}, runType = {}, timeLine = {}, riskType = {}, useNewTg ={}", record.getId(), riskFlowId, RiskFlowTraceRunType.NEW_ENGINE_DECISION, timeLine.name(), eventVO.riskType, useNewTg.name());
    return RiskFlowTraceVO.from(record);
  }

  private boolean isHitTimeBaseLine(Long accountId, LoanUserRiskType riskType) {
    Map<LoanUserRiskType, BigDecimal> timeBaseLinePercentageMap = riskFlowConfig.getBaseLineRiskTypePercentageMap();
    BigDecimal percentage = timeBaseLinePercentageMap.getOrDefault(riskType, BigDecimal.ZERO);
    // 0%的流量不走时间基线
    if (percentage.compareTo(BigDecimal.ZERO) == 0) {
      return false;
    }

    // 100%的流量都走时间基线
    if (percentage.compareTo(BigDecimal.ONE) == 0) {
      return true;
    }

    return EcHashUtil.hitConsistentHash(HitConsistentHashPrefix.RISK_TIME_BASE_LINE, accountId.toString(), percentage.doubleValue());
  }

  private boolean isHitAsyncExecuteDatasource(Long accountId, LoanUserRiskType riskType) {
    Map<LoanUserRiskType, BigDecimal> datasourceParallelRiskTypePercentageMap = riskFlowConfig.getDatasourceParallelRiskTypePercentageMap();
    BigDecimal percentage = datasourceParallelRiskTypePercentageMap.getOrDefault(riskType, BigDecimal.ZERO);
    // 0%的流量不走datasource并行计算
    if (percentage.compareTo(BigDecimal.ZERO) == 0) {
      return false;
    }

    // 100%的流量都走datasource并行计算
    if (percentage.compareTo(BigDecimal.ONE) == 0) {
      return true;
    }

    return EcHashUtil.hitConsistentHash(HitConsistentHashPrefix.DATASOURCE_RUN_TYPE, accountId.toString(), percentage.doubleValue());
  }

  private boolean isHitUseNewTg(Long accountId) {
    BigDecimal percentage = riskFlowConfig.getUseNewTgPercentage();
    if (percentage.compareTo(BigDecimal.ZERO) == 0) {
      return false;
    }

    if (percentage.compareTo(BigDecimal.ONE) == 0) {
      return true;
    }

    return EcHashUtil.hitConsistentHash(HitConsistentHashPrefix.RISK_USE_NEW_TG, accountId.toString(), percentage.doubleValue());
  }

  @RunInTransaction
  public void retryRiskFlowTrace(Long traceId) {
    if (riskConfig.getStopSaveDataToRiskEngine()) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("此功能正在维护中，预计5分钟后恢复，请稍后再试"));
    }
    RiskFlowTraceRecord record = riskFlowTraceModel.findByIdOrThrow(traceId);
    RiskFlowTraceVO vo = RiskFlowTraceVO.from(record);
    vo.status = RiskFlowTraceStatus.FAILED;
    //更新到失败状态
    riskFlowTraceModel.insertOrUpdateByRiskFlowTraceVO(vo);
    //重跑风控
    riskFlowService.reinitTrace(Collections.singleton(traceId));
  }

  public Map<String, Object> initRiskProps(LoanAccountVO accountVO, Long orderId) {
    Map<String, Object> props = new HashMap<>();
    props.put(RiskKey.NAME, StringUtils.left(accountVO.name, 50));
    props.put(RiskKey.IDENTITY_NUMBER, accountVO.identityNumber);
    props.put(RiskKey.MOBILE, accountVO.mobileNumber);
    props.put(RiskKey.LOAN_ACCOUNT_ID, accountVO.id);
    props.put(RiskKey.ORDER_ID, orderId);
    return props;
  }


  public void retryRiskFlowTrace(LoanUserCreditsInfoRecord creditsInfoRecord) {
    LoanCreditsStatus status = LoanCreditsStatus.fromCode(creditsInfoRecord.getCreditsStatus());
    EcAsserts.assertTrue(status == LoanCreditsStatus.MANUAL_REVIEW, "loan credits status should be manual review");
    LoanUserRiskTraceVO traceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(creditsInfoRecord.getLoanAccountId());
    EcAsserts.assertTrue(Objects.nonNull(traceVO) && Objects.nonNull(traceVO.traceId) && LoanUserRiskType.getFirstLoanRiskTypes().contains(traceVO.riskType));
    if (Objects.isNull(traceVO)) {
      throw EcException.error("trace is null, loanAccountId is {}", creditsInfoRecord.getLoanAccountId());
    }
    if (!riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW_TRACE)) {
      retryRiskFlowTrace(traceVO.traceId);
    } else {
      iRiskFlowTraceService.retryRiskFlowTrace(Collections.singletonList(traceVO.traceId));
    }
    loanUserCreditsInfoModel.updateStatus(creditsInfoRecord, LoanCreditsStatus.IN_REVIEW);
  }

  public void retryReloanRiskFlowTrace(LoanUserCreditsInfoRecord creditsInfoRecord) {
    LoanCreditsStatus status = LoanCreditsStatus.fromCode(creditsInfoRecord.getReloanStatus());
    Long traceId = creditsInfoRecord.getReloanTraceId();
    EcAsserts.assertTrue(status == LoanCreditsStatus.MANUAL_REVIEW, "loan credits status should be manual review");
    if (!riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW_TRACE)) {
      retryRiskFlowTrace(traceId);
    } else {
      iRiskFlowTraceService.retryRiskFlowTrace(Collections.singletonList(traceId));
    }
    loanUserCreditsInfoModel.updateReloanStatus(creditsInfoRecord, LoanCreditsStatus.IN_REVIEW);
  }

  public RiskFlowTraceVOV2 getRiskFlowTraceVOV2(Long traceId) {
    if (riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW_TRACE)) {
      List<com.yqg.overseasrisk.common.lib.riskflow.trace.RiskFlowTraceVO> riskFlowTraceResps = iRiskFlowTraceService.findByIds(Collections.singletonList(traceId));
      if (CollectionUtils.isEmpty(riskFlowTraceResps) || riskFlowTraceResps.size() > 1) {
        throw EcException.error("exist more than one record, traceid is {}", traceId);
      }
      return RiskFlowTraceVOV2.fromOverseasRiskFlowTraceVO(riskFlowTraceResps.get(0));
    }
    RiskFlowTraceVO riskFlowTraceVO = riskFlowService.queryTrace(traceId);
    return RiskFlowTraceVOV2.fromRiskFlowTraceVO(riskFlowTraceVO);
  }
}

