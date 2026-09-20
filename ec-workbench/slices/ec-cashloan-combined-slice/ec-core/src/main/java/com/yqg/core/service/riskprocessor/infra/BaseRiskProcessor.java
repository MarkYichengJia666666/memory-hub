package com.yqg.core.service.riskprocessor.infra;

import com.google.common.collect.ImmutableList;
import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.*;
import com.yqg.core.model.mongo.MongoSubmitRiskParamModel;
import com.yqg.core.model.sql.loan.account.LoanAccountAdditionalInfoModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.LoanUserCreditsInfoModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.risk.BatchTriggerRiskLogModel;
import com.yqg.core.model.sql.risk.MarketingBatchTriggerRiskLogModel;
import com.yqg.core.model.sql.risk.MarketingBatchTriggerRiskRelationModel;
import com.yqg.core.service.cashloan.CashLoanCalcCreditsService;
import com.yqg.core.service.cashloan.CashLoanMonitorService;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.cashloan.enums.CashLoanCalcCreditsStatus;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.risk.monitor.RiskMonitor;
import com.yqg.core.service.cashloan.risk.vo.EventTypeVO;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.upload.UploadInformationService;
import com.yqg.core.service.cashloan.vo.CashLoanCalcCreditsVO;
import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.core.service.kafka.generator.KafkaTopicGenerator;
import com.yqg.core.service.kafka.producer.KafkaMessageService;
import com.yqg.core.service.loan.LoanAssertion;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.LoanUserEventService;
import com.yqg.core.service.loan.account.LoanUserTypeService;
import com.yqg.core.service.loan.additioanalinfo.LoanUserAdditionalInfoService;
import com.yqg.core.service.loan.control.vo.LoanUserControlService;
import com.yqg.core.service.loan.vo.LoanAccountDetailsSimpleVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserTypeVO;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsVO;
import com.yqg.core.service.ojk.OjkConfig;
import com.yqg.core.model.sql.loanusertrace.LoanRiskCreditStage;
import com.yqg.core.service.risk.RiskEngineService;
import com.yqg.core.service.risk.facade.LoanRiskCreditStageService;
import com.yqg.core.service.risk.facade.RiskFacadeTool;
import com.yqg.core.service.risk.feature.CallRiskApiPosition;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.RiskFlowV2Service;
import com.yqg.core.service.risk.thirdparty.ThirdPartyEventService;
import com.yqg.core.service.risk.thirdparty.event.ThirdPartyEventType;
import com.yqg.core.service.user.UserEventService;
import com.yqg.ec.common.enums.LoanAccountAdditionalTypeEnum;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskSource;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.PreTraceTriggerScene;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.risk.riskflow.RiskFlow;
import com.yqg.risk.riskflow.RiskFlowService;
import com.yqg.risk.riskflow.element.RiskFlowStandard;
import com.yqg.risk.riskflow.trace.RiskFlowTraceVO;
import com.yqg.translation.client.utils.TT;
import com.yqg.whatopia.common.util.Clock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/21 5:54 下午
 */
@Slf4j
public abstract class BaseRiskProcessor {
  @Autowired
  protected ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  protected LoanAccountModel accountModel;
  @Autowired
  protected LoanUserCreditsInfoModel userCreditsInfoModel;
  @Autowired
  protected LoanUserTypeService loanUserTypeService;
  @Autowired
  protected LoanUserEventService loanUserEventService;
  @Autowired
  protected RiskFlowService riskFlowService;
  @Autowired
  protected RiskEngineService riskEngineService;
  @Autowired
  protected LoanUserAdditionalInfoService userAdditionalInfoService;
  @Autowired
  protected RiskConfig riskConfig;
  @Autowired
  protected LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  protected RiskFacadeTool riskFacadeTool;
  @Autowired
  protected LoanRiskCreditStageService loanRiskCreditStageService;
  @Autowired
  protected LoanAccountService accountService;
  @Autowired
  protected UserEventService userEventService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private CashLoanCalcCreditsService calcCreditsService;
  @Autowired
  private RiskMonitor riskMonitor;
  @Autowired
  private OjkConfig ojkConfig;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;
  @Autowired
  private CashLoanMonitorService cashLoanMonitorService;
  @Autowired
  private ThirdPartyEventService thirdPartyEventService;
  @Autowired
  private LoanUserControlService loanUserControlService;
  @Autowired
  private KafkaMessageService kafkaMessageService;
  @Autowired
  private MongoSubmitRiskParamModel mongoSubmitRiskParamModel;
  @Autowired
  private LoanAccountAdditionalInfoModel loanAccountAdditionalInfoModel;
  @Autowired
  private RiskFlowV2Service riskFlowV2Service;
  @Autowired
  private BatchTriggerRiskLogModel batchTriggerRiskLogModel;
  @Autowired
  private MarketingBatchTriggerRiskLogModel marketingBatchTriggerRiskLogModel;
  @Autowired
  private MarketingBatchTriggerRiskRelationModel marketingBatchTriggerRiskRelationModel;
  @Autowired
  protected LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private UploadInformationService uploadInformationService;
  @Autowired
  private TraceRiskFlowMappingService traceRiskFlowMappingService;

  /**
   * 提交风控基本流程:
   * 1、完成必须在风控提交之前进行的操作。比如首贷之前重置IOS用户的userType，因为这个决定了后续提交风控时的 eventType; 再比如首贷时检查LoanUserCreditsInfoRecord是否存在，因为用户可能重新提交，也可能是第一次提交
   * 2、检查LoanUserCreditsInfoRecord状态，在不同的 LoanUserRiskType 提交时，应该是不同的状态，分别设置断言
   * 3、提交风控，拿到 RiskFlowTraceVO
   * 4、更新授信状态。根据不同的LoanUserRiskType，需要将 LoanUserCreditsInfoRecord 的不同状态字段，设置成不同的值
   * 5、记录loanUserRiskTrace，固定操作
   * 6、完成必须在风控提交之前进行的操作。比如一些依赖RiskFlowTraceVO结果的操作，目前还没有，依照惯例把kafka事件发布发在这里完成。一些在风控提交前后都可以去完成的操作，也可以放在这里
   *
   * @param param
   * @return
   */
  public RiskFlowTraceVO submitApplication(RiskProcessParam param) {
    return threadTransactionalModel.transactionResult(configuration -> {
      LoanAccountRecord accountRecord = accountModel.findByIdForUpdateOrThrow(param.accountId);
      dealBeforeGetTraceVO(param, accountRecord);

      // 提交风控
      logLoanUserRiskTraceAndSendTraceKafkaMessage(param);
      return null;
    });
  }

  private void logLoanUserRiskTraceAndSendTraceKafkaMessage(RiskProcessParam param) {

    LoanUserRiskTraceRecord loanUserRiskTraceRecord = logLoanUserRiskTraceForPreInitTrace(param);
    //异步提交风控，在记录risktrace数据之后，需要进行一些其他的记录操作
    afterLogLoanUserRiskTrace(param, loanUserRiskTraceRecord);
    if (saveOrIgnoreRiskParam(param, loanUserRiskTraceRecord)) {
      return;
    }
    triggerRiskTraceGenerationKafka(param, loanUserRiskTraceRecord);
  }

  protected void afterLogLoanUserRiskTrace(RiskProcessParam param, LoanUserRiskTraceRecord loanUserRiskTraceRecord) {

  }

  private boolean saveOrIgnoreRiskParam(RiskProcessParam param, LoanUserRiskTraceRecord loanUserRiskTraceRecord) {
    List<LoanAccountAdditionalInfoRecord> additionalInfoRecordList =
        loanAccountAdditionalInfoModel.findByAccountIdsAndTypeAndExternalIds(ImmutableList.of(param.accountId),
            LoanAccountAdditionalTypeEnum.SUBMIT_RISK_PARAM,
            ImmutableList.of(loanUserRiskTraceRecord.getId().toString()));
    if (CollectionUtils.isNotEmpty(additionalInfoRecordList)) {
      return true;
    }
    String objectId = mongoSubmitRiskParamModel.insert(param);
    uploadInformationService.insertAdditionalInfoWithTypeAndObjectIdAndExternalId(param.accountId,
        LoanAccountAdditionalTypeEnum.SUBMIT_RISK_PARAM,
        objectId,
        loanUserRiskTraceRecord.getId().toString(),
        JsonUtils.toString(param));
    return false;
  }

  private LoanUserRiskTraceRecord logLoanUserRiskTraceForPreInitTrace(RiskProcessParam param) {
    LoanUserRiskSource riskSource = (Objects.nonNull(param.isNotBatchRisk) && param.isNotBatchRisk.equals(Boolean.FALSE)) ? LoanUserRiskSource.BATCH : LoanUserRiskSource.NORMAL;
    LoanRiskCreditStage creditStage = loanRiskCreditStageService.resolveCreditStage(param.accountId, getLoanUserRiskType());
    riskMonitor.logSubmitRiskAndComplete(param.accountId, param.extraInfo.sourceType, RiskFlowTraceStatusV2.PRE_INIT, getLoanUserRiskType(), riskSource, param.triggerType.code, param.triggerSource, creditStage, null, null);
    return loanUserRiskTraceModel.insertWithPreInitTraceStatus(
        param.userId,
        param.accountId,
        param.orderId,
        getLoanUserRiskType(),
        param.triggerType,
        param.eventTypeId,
        param.riskFlowId,
        RiskFlowTraceStatusV2.PRE_INIT,
        param.extraInfo.sourceType,
        param.calcTriggerSubType(),
        param.triggerSource,
        param.triggerSourceExternalId,
        creditStage
    );
  }

  private void triggerRiskTraceGenerationKafka(RiskProcessParam param, LoanUserRiskTraceRecord loanUserRiskTraceRecord) {
    TraceCreateMessageVO messageVO = new TraceCreateMessageVO();
    messageVO.loanUserRiskTraceId = loanUserRiskTraceRecord.getId();
    LoanAccountRecord accountRecord = accountModel.findById(param.accountId);
    LoanAccountVO accountVO = accountService.getLoanAccountVO(param.accountId);
    LoanAccountDetailsSimpleVO detailsSimpleVO = loanAccountDetailsService.getSimpleByUserIdOrNull(loanUserRiskTraceRecord.getUserId());

    RiskFlowAndEventTypeVO riskFlowAndEventTypeVO = getRiskFlowAndEventTypeVO(param, accountRecord, accountVO);
    messageVO.accountVO = accountVO;
    messageVO.eventTypeVO = riskFlowAndEventTypeVO.eventTypeVO;
    messageVO.riskFlowId = riskFlowAndEventTypeVO.riskFlowId;
    messageVO.orderId = param.orderId;
    messageVO.timeFinished = detailsSimpleVO.timeFinished;
    if (LoanUserRiskType.getLoanOrderRiskTypes().contains(getLoanUserRiskType())) {
      messageVO.orderId = null;
    }
    messageVO.riskCrowdCategory = riskFacadeTool.getRiskCrowdCategoryForSubmitRisk(getLoanUserRiskType(), accountVO);
    kafkaMessageService.schedule(KafkaTopicGenerator.getTopic(KafkaTopic.RISK_TRACE), String.valueOf(loanUserRiskTraceRecord.getUserId()), JsonUtils.toString(messageVO));
  }


  public void dealAfterGetTraceVO(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    threadTransactionalModel.transaction(configuration -> {
      LoanAccountRecord accountRecord = accountModel.findByIdForUpdateOrThrow(param.accountId);

      //发送消息给风控
      sendRiskEvent(param, traceVO);

      // 更新授信状态
      updateCreditsInfo(param, traceVO);

      // 记录loanUserRiskTrace
      logLoanUserRiskTrace(param, traceVO);

      // 记录下下单时订单的期数信息
      cashLoanMonitorService.logCreateOrderTerms(param.orderId, accountRecord.getUserType(), getLoanUserRiskType().code);  // 记录下下单时订单的期数信息

      // 提交风控后的额外操作
      postAdditionalProcess(param, traceVO);

      //如果是跑批触发的风控，回写各自的log表
      postBatchRiskAdditionalProcess(param, traceVO);
    });
  }


  private void dealBeforeGetTraceVO(RiskProcessParam param, LoanAccountRecord accountRecord) {
    param.userId = accountRecord.getUserId();
    //用户管制中，不可以提交任何风控
    checkUserControl(param);

    // 前置检查
    LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByAccountId(param.accountId);
    assertBeforeSubmit(creditsInfoRecord, param);

    // 提交风控前的额外操作
    preAdditionalProcess(param);
  }

  private void checkUserControl(RiskProcessParam param) {
    if (loanUserControlService.canSameIdentityControl(param.accountId)
        && loanUserControlService.isControlUser(param.accountId)) {
      throw EcException.error("can't get submitRisk for control user loanAccountId is  {}", param.accountId);
    }
  }


  protected abstract LoanUserRiskType getLoanUserRiskType();

  protected abstract void assertBeforeSubmit(LoanUserCreditsInfoRecord creditsInfoRecord, RiskProcessParam param);

  protected abstract void updateCreditsInfo(RiskProcessParam param, RiskFlowTraceVOV2 traceVO);

  protected void preAdditionalProcess(RiskProcessParam param) {
  }

  protected void postAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
  }

  protected void postBatchRiskAdditionalProcess(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    if (!param.isNotBatchRisk) {
      switch (param.batchTaskType) {
        case AUTO_BATCH_TASK:
        case MANUAL_BATCH_TASK:
          // TODO(TAPD-370059): 上线稳定且存量 INIT（旧链路：提交后仍靠回调写 R）已消化完后，
          // 改为只补 traceId，不再 setStatus(RUNNING)。未覆盖前改会让旧行停在 INIT+traceId。勿动 MARKETING。
          BatchTriggerRiskLogRecord batchTriggerRiskLogRecord = batchTriggerRiskLogModel.findById(param.logId);
          batchTriggerRiskLogModel.updateTraceId(batchTriggerRiskLogRecord, traceVO.id);
          break;
        case MARKETING_BATCH_TASK:
          MarketingBatchTriggerRiskLogRecord marketingBatchTriggerRiskLogRecord = marketingBatchTriggerRiskLogModel.findByIdOrThrow(param.logId);
          marketingBatchTriggerRiskLogModel.updateTraceId(marketingBatchTriggerRiskLogRecord, traceVO.id);
          MarketingBatchTriggerRiskRelationRecord marketingBatchTriggerRiskRelationRecord = marketingBatchTriggerRiskRelationModel.findByIdOrThrowForUpdate(marketingBatchTriggerRiskLogRecord.getTaskId());
          marketingBatchTriggerRiskRelationModel.updateTotalTaskCount(marketingBatchTriggerRiskRelationRecord);
          break;
        default:
          log.error("unknown batchTaskType = {}.", param.batchTaskType);
      }
    }
  }

  protected void sendRiskEvent(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    // 发送风控提交事件到risk服务
    thirdPartyEventService.publishRiskProcessEvent(
        param.accountId,
        param.sdkType,
        getLoanUserRiskType(),
        Optional.ofNullable(param.extraInfo).map(o -> o.platformType).orElse(null),
        ThirdPartyEventType.RISK_SUBMIT);
  }

  private EventTypeVO getAuthEventType(LoanAccountRecord accountRecord) {
    LoanUserTypeVO userTypeVO = loanUserTypeService.fromUserTypeCode(accountRecord.getUserType());
    SDKType sdkType = SDKType.fromCode(accountRecord.getSdkType());
    return loanUserEventService.getBySDKTypeAndRiskTypeAndUserTypeName(sdkType, getLoanUserRiskType(), userTypeVO.name, accountRecord.getId());
  }

  private void logLoanUserRiskTrace(RiskProcessParam param, RiskFlowTraceVOV2 traceVO) {
    LoanUserRiskTraceRecord riskTraceRecord = loanUserRiskTraceModel.findByTraceId(traceVO.id);
    LoanRiskCreditStage creditStage;
    //新的流程里面已经插入过了，不需要再插入了
    if (Objects.isNull(riskTraceRecord)) {
      log.info("current trace not in table when get traceId from risk, traceId:{}", traceVO.id);
      creditStage = loanRiskCreditStageService.resolveCreditStage(param.accountId, getLoanUserRiskType());
      loanUserRiskTraceModel.insert(
          param.userId,
          param.accountId,
          traceVO.id,
          param.orderId,
          getLoanUserRiskType(),
          param.triggerType,
          traceVO.getEventId(),
          traceVO.getRiskFlowId(),
          traceVO.getStatus(),
          param.extraInfo.sourceType,
          param.calcTriggerSubType(),
          param.triggerSource,
          param.triggerSourceExternalId,
          creditStage
      );
    } else {
      creditStage = riskTraceRecord.getCreditStage() == null ? null : LoanRiskCreditStage.valueOf(riskTraceRecord.getCreditStage());
    }
    LoanUserRiskSource riskSource = (Objects.nonNull(param.isNotBatchRisk) && param.isNotBatchRisk.equals(Boolean.FALSE)) ? LoanUserRiskSource.BATCH : LoanUserRiskSource.NORMAL;
    SourceType sourceType = Objects.nonNull(param.extraInfo) && Objects.nonNull(param.extraInfo.sourceType) ? param.extraInfo.sourceType : null;
    RiskFlowTraceStatusV2 status = traceVO.getStatus();
    Long traceTimeCreated = null;
    if (traceVO.getTimeCreated() != null) {
      traceTimeCreated = traceVO.getTimeCreated();
    } else {
      LoanUserRiskTraceRecord traceRecordForLatency = loanUserRiskTraceModel.findByTraceId(traceVO.id);
      if (traceRecordForLatency != null) {
        traceTimeCreated = traceRecordForLatency.getTimeCreated();
      }
    }
    riskMonitor.logSubmitRiskAndComplete(
        param.accountId,
        sourceType,
        status,
        getLoanUserRiskType(),
        riskSource,
        param.triggerType.code,
        param.triggerSource,
        creditStage,
        traceVO.id,
        traceTimeCreated);
  }

  private RiskFlowAndEventTypeVO getRiskFlowAndEventTypeVO(RiskProcessParam param, LoanAccountRecord accountRecord, LoanAccountVO accountVO) {
    RiskFlowAndEventTypeVO riskFlowAndEventTypeVO = new RiskFlowAndEventTypeVO();
    if (Objects.isNull(param.riskFlowId)) {
      EventTypeVO eventTypeVO = getAuthEventType(accountRecord);
      Long riskFlowId = resolveRiskFlowId(param, eventTypeVO, accountVO);
      return RiskFlowAndEventTypeVO.from(eventTypeVO, riskFlowId);
    } else {
      if (Objects.isNull(param.eventTypeId)) {
        throw EcException.error("not find eventTypeId ,riskFlowId :{}, accountId :{}", riskFlowAndEventTypeVO.riskFlowId, param.accountId);
      }
      return RiskFlowAndEventTypeVO.from(loanUserEventService.fromEventId(param.eventTypeId), param.riskFlowId);
    }
  }

  private Long resolveRiskFlowId(RiskProcessParam param, EventTypeVO eventTypeVO, LoanAccountVO accountVO) {
    if (traceRiskFlowMappingService.isUserStatusMappingEnabled()) {
      Long mappedRiskFlowId = traceRiskFlowMappingService.mapRiskFlowId(param.accountId, getLoanUserRiskType());
      if (mappedRiskFlowId != null) {
        return mappedRiskFlowId;
      }
    }
    return pickEventTypeRiskFlowId(eventTypeVO, accountVO, param.extraInfo.sourceType);
  }

  private Long pickEventTypeRiskFlowId(EventTypeVO eventTypeVO, LoanAccountVO accountVO, SourceType sourceType) {
    return riskConfig.getOpenSwitchRiskFlowSearchFromEc(CallRiskApiPosition.RISK_FLOW)
        ? riskFlowV2Service.pickRiskFlow(eventTypeVO, accountVO, sourceType)
        : pickRiskFlow(eventTypeVO, accountVO, sourceType);
  }

  private Long pickRiskFlow(EventTypeVO eventTypeVO, LoanAccountVO accountVO, SourceType sourceType) {
    try {
      RiskFlowStandard standard = riskFlowService.generateStandard(eventTypeVO);
      RiskFlow riskFlow = pickFromWhitelist(accountVO.id, standard);
      if (riskFlow != null) {
        return riskFlow.id;
      }
      riskFlow = pickFromLocation(eventTypeVO, accountVO.id, standard, sourceType);
      if (riskFlow != null) {
        return riskFlow.id;
      }
      // 印尼要支持随机选取风控流程，使用 hashSuffix 作为随机种子
      return riskFlowService.randomPickRiskFlow(riskEngineService.initRiskProps(accountVO, null), standard).id;
    } catch (Exception e) {
      // 仅「未配置启用的 default riskFlow」这类配置错误打点；网络/DB 等系统异常直接抛出，避免误判为配置缺失
      // 旧链路 generateStandard 来自 com.yqg.risk JAR，抛出的是裸 RuntimeException，靠 message 前缀识别
      if (com.yqg.core.service.risk.riskflow.vo.RiskFlowStandard.isNoDefaultEnabledRiskFlowError(e)) {
        riskMonitor.logDefaultRiskFlowError(eventTypeVO, accountVO.id);
      }
      throw e;
    }
  }

  private RiskFlow pickFromWhitelist(Long accountId, RiskFlowStandard standard) {
    Map<Long, Long> accountIdRiskFlowMap = riskConfig.getWhitelistRiskFlowMap();
    return standard.riskFlowWithId.get(accountIdRiskFlowMap.get(accountId));
  }

  private RiskFlow pickFromLocation(EventTypeVO eventTypeVO, Long accountId, RiskFlowStandard standard, SourceType sourceType) {
    if (!ojkConfig.getLocationAllowEventIds().contains(eventTypeVO.eventId)) {
      return null;
    }
    Map<String, Long> locationRiskFlowMap = ojkConfig.getLocationRiskFlowMap();
    if (MapUtils.isEmpty(locationRiskFlowMap)) {
      return null;
    }
    LoanAccountDetailsVO accountDetailsVO = loanAccountDetailsService.getAuthFinishedOrSourceTypeDetailsVoByAccountId(accountId, sourceType);
    if (accountDetailsVO != null
        && accountDetailsVO.detailsPojo != null
        && accountDetailsVO.detailsPojo.cashLoanEmploymentInfo != null
        && accountDetailsVO.detailsPojo.cashLoanEmploymentInfo.environmentInfo != null) {
      Long locationMatchRiskFlowId = getRiskFlowByLocationMap(
          accountDetailsVO.detailsPojo.cashLoanEmploymentInfo.environmentInfo.longiTude,
          accountDetailsVO.detailsPojo.cashLoanEmploymentInfo.environmentInfo.latiTude, locationRiskFlowMap);
      if (locationMatchRiskFlowId != null) {
        return standard.riskFlowWithId.get(locationMatchRiskFlowId);
      }
    }
    return null;
  }

  private Long getRiskFlowByLocationMap(String longitude, String latitude, Map<String, Long> locationRiskFlowMap) {
    if (StringUtils.isAnyBlank(longitude, latitude) || !NumberUtils.isCreatable(longitude) || !NumberUtils.isCreatable(latitude)) {
      return null;
    }
    for (Map.Entry<String, Long> entry : locationRiskFlowMap.entrySet()) {
      String[] locationArr = entry.getKey().split(",");
      String longitudeConf = locationArr[0];
      String latitudeConf = locationArr[1];
      if (longitude.startsWith(longitudeConf) && latitude.startsWith(latitudeConf)) {
        return entry.getValue();
      }
    }
    return null;
  }

  protected CashLoanCalcCreditsVO checkAndGetCashLoanCalcCreditsVO(LoanUserCreditsInfoRecord record) {
    assertCreditsStatus(record, LoanCreditsStatus.ACCEPTED);

    // 判断是否有未完成订单(INIT、RESERVE、CHECK、READY)
    int uncompletedOrder = ecOrderService.countOrder(record.getLoanAccountId(), CashLoanOrderStatus.UNDONE_STATUSES);
    if (uncompletedOrder > 0) {
      throw EcException.error("can not calc credits with undone orders,accountId is {}", record.getLoanAccountId());
    }

    return calcCreditsService.getCalcCreditsStatus(record.getLoanAccountId());
  }

  protected void assertCreditsStatus(LoanUserCreditsInfoRecord creditsInfoRecord, LoanCreditsStatus loanCreditsStatus) {
    LoanAssertion.assertAccountAndAppInCreditsStatus(creditsInfoRecord, loanCreditsStatus);
    if (StringUtils.isNotEmpty(creditsInfoRecord.getReloanStatus())) {
      LoanAssertion.assertAccountAndAppInReloanCreditsStatus(creditsInfoRecord, loanCreditsStatus);
    }
  }

  protected void assertIncreaseCreditReviewStatus(LoanUserCreditsInfoRecord creditsInfoRecord) {
    if (StringUtils.isNotEmpty(creditsInfoRecord.getReloanStatus())) {
      if (LoanCreditsStatus.fromCodeOrNull(creditsInfoRecord.getReloanStatus()) != LoanCreditsStatus.REJECTED) {
        throw EcException.warn(EcExceptionType.LOAN_ACCOUNT_INCREASE_CREDITS_NOT_REJECT, TT.gen("user credits status is not reject"));
      }
      checkTimeReapply(creditsInfoRecord);
      return;
    }
    if (LoanCreditsStatus.fromCodeOrNull(creditsInfoRecord.getCreditsStatus()) != LoanCreditsStatus.REJECTED) {
      throw EcException.warn(EcExceptionType.LOAN_ACCOUNT_INCREASE_CREDITS_NOT_REJECT, TT.gen("user credits status is not reject"));
    }
    checkTimeReapply(creditsInfoRecord);
  }

  private void checkTimeReapply(LoanUserCreditsInfoRecord creditsInfoRecord) {
    if (creditsInfoRecord.getTimeReapply() > 0 && creditsInfoRecord.getTimeReapply() < Clock.now()) {
      if (LoanCreditsStatus.fromCodeOrNull(creditsInfoRecord.getCreditsStatus()) != LoanCreditsStatus.REJECTED) {
        throw EcException.warn(EcExceptionType.LOAN_ACCOUNT_INCREASE_CREDITS_NOT_REJECT, TT.gen("user credits status is not reject"));
      }
    }
  }

  protected boolean checkSubmitRisk(BooleanType submitRisk) {
    return Objects.nonNull(submitRisk) && submitRisk.bool;
  }

  protected boolean isCreditsExpired(CashLoanCalcCreditsVO cashLoanCalcCreditsVO, BooleanType submitRisk) {
    return !checkSubmitRisk(submitRisk) &&
        (cashLoanCalcCreditsVO.calcCreditsStatus != CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED || cashLoanCalcCreditsVO.nextRiskType != getLoanUserRiskType());
  }

  protected boolean isCreditsExpired(CashLoanCalcCreditsVO cashLoanCalcCreditsVO, BooleanType submitRisk, List<LoanUserRiskType> loanUserRiskTypeList) {
    return !checkSubmitRisk(submitRisk) &&
        (cashLoanCalcCreditsVO.calcCreditsStatus != CashLoanCalcCreditsStatus.CALC_CREDITS_EXPIRED || !loanUserRiskTypeList.contains(cashLoanCalcCreditsVO.nextRiskType));
  }

  //是否为降级风控
  protected boolean isDegrade(RiskProcessParam param, Long traceId) {
    if ((param.preTraceTriggerScene == PreTraceTriggerScene.RETRIEVAL) != param.submitScene.isDegrade()) {
      log.error("preTraceTriggerScene 和 submitScene结果不一致，traceid：{}", traceId);
    }
    return param.submitScene.isDegrade();
  }
}
