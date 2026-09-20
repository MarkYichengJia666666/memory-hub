package com.yqg.core.service.cashloan.ordercenter;

import static com.yqg.core.model.sql.cashloan.enums.LoanUserSupplementStatus.PROCESSING_STATUS;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.generated.tables.records.LoanUserSupplementCreateOrderRecord;
import com.yqg.core.model.loader.RiskSupplementLivingTimeLimitsLoader;
import com.yqg.core.model.mongo.MongoSupplementBeforeCreateOrderModel;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.cashloan.enums.LoanUserSupplementStatus;
import com.yqg.core.model.sql.cashloan.order.CashLoanUserSupplementCreateOrderModel;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.LoanUserCreditsInfoModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.cashloan.CashLoanMonitorService;
import com.yqg.core.service.cashloan.LoanUserOrderService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.ordercenter.vo.LoanUserSupplementCreateOrderVO;
import com.yqg.core.service.cashloan.upload.UploadInformationService;
import com.yqg.core.service.cashloan.vo.CashLoanCreateOrderRequestVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserSimpleCreditsInfoVO;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class CashLoanUserSupplementCreateOrderService {
  @Autowired
  private CashLoanUserSupplementCreateOrderModel cashLoanUserSupplementCreateOrderModel;
  @Autowired
  private MongoSupplementBeforeCreateOrderModel mongoSupplementBeforeCreateOrderModel;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private LoanUserCreditsInfoModel userCreditsInfoModel;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private CashLoanMonitorService cashLoanMonitorService;
  @Autowired
  private LoanUserOrderService loanUserOrderService;
  @Autowired
  private ExpFacade expFacade;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private UploadInformationService uploadInformationService;
  @Autowired
  private RiskSupplementLivingTimeLimitsLoader riskSupplementLivingTimeLimitsLoader;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;

  public LoanUserSupplementCreateOrderRecord insertSupplementInfoWithTimeExpired(LoanAccountVO loanAccountVO, CashLoanCreateOrderRequestVO createOrderDetail, Long traceId, Long timeExpired) {
    String objectId = mongoSupplementBeforeCreateOrderModel.insert(createOrderDetail);
    return cashLoanUserSupplementCreateOrderModel.insert(loanAccountVO.userId, loanAccountVO.id, objectId, traceId, timeExpired);
  }

  public boolean existSupplementInfoInProcess(Long userId) {
    //判断订单是否有未补件完成的数据
    LoanUserSupplementCreateOrderRecord record = cashLoanUserSupplementCreateOrderModel.fetchLastedByUserId(userId);
    List<String> processStatusCodes = PROCESSING_STATUS.stream().map(LoanUserSupplementStatus::getCode).collect(Collectors.toList());
    return record != null && processStatusCodes.contains(record.getStatus());
  }

  public boolean existSupplementInfoInProcessByAccountId(Long loanAccountId) {
    //判断订单是否有未补件完成的数据
    LoanUserSupplementCreateOrderRecord record = cashLoanUserSupplementCreateOrderModel.fetchLastedByLoanAccountId(loanAccountId);
    List<String> processStatusCodes = PROCESSING_STATUS.stream().map(LoanUserSupplementStatus::getCode).collect(Collectors.toList());
    return record != null && processStatusCodes.contains(record.getStatus());
  }


  public boolean checkIsFirstRiskProcess(Long traceId) {
    //第一次提交风控的traceId是不可能更新的
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchByFirstTraceId(traceId);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      return false;
    }
    LoanUserSupplementStatus status = LoanUserSupplementStatus.fromCode(supplementCreateOrderRecord.getStatus());
    return LoanUserSupplementStatus.INIT == status;
  }

  public void insertOrUpdateSupplementTraceInfo(LoanAccountVO loanAccountVO, Long traceId, CashLoanCreateOrderRequestVO orderInfo) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByUserIdInProcess(loanAccountVO.userId);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      log.info("current user first trace ,traceId is {}, loanAccountId is {}", traceId, loanAccountVO.id);
      if (Objects.isNull(orderInfo.principal) || StringUtils.isEmpty(orderInfo.productId)) {
        throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("补件下单缺少金额或者产品信息"));
      }
      insertSupplementInfo(loanAccountVO, orderInfo, traceId);
      return;
    }
    log.info("current user second trace ,traceId is {}, loanAccountId is {}", traceId, loanAccountVO.id);
    //数据过期了，风控已经提交了，提交之后风控查补件数据的时候要校验一下。已经过期的直接拒掉 业务侧处理风控的时候也注意一下
    cashLoanUserSupplementCreateOrderModel.updateConfirmTraceIdAndStatus(supplementCreateOrderRecord, traceId, LoanUserSupplementStatus.SUBMIT_CHECK);
  }

  private void insertSupplementInfo(LoanAccountVO loanAccountVO, CashLoanCreateOrderRequestVO createOrderDetail, Long traceId) {
    String objectId = mongoSupplementBeforeCreateOrderModel.insert(createOrderDetail);
    Long timeExpired = Clock.now() + Clock.MILLS_PER_HOUR * riskConfig.getSupplementExpiredTime();
    cashLoanUserSupplementCreateOrderModel.insert(loanAccountVO.userId, loanAccountVO.id, objectId, traceId, timeExpired);
  }

  public boolean getSupplementSwitch(Long build, SourceType sourceType, Long userId) {
    // api渠道不需要补件
    if (sourceType.isApiChannelSourceType()) {
      return false;
    }
    if (riskConfig.getSupplementProcessWhiteList().contains(userId)) {
      return true;
    }
    long supplementBuild = riskConfig.getSupplementProcessBuild();
    if (sourceType.isAppSourceType()) {
      return supplementBuild <= build;
    }
    //web不关心版本，加个单独的开关，决定是否直接去分流
    return riskConfig.getSupplementProcessWebChannelSwitch();
  }

  /**
   * 补件风控通过，代表不需要用户补件，更新用户状态，然后下单
   */
  public void updateSupplementStatusInfoByFirstRiskAccept(Long userId, Long traceId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchByFirstTraceId(traceId);

    if (Objects.isNull(supplementCreateOrderRecord)) {
      log.error("there is no supplement info with firstTraceId {},userId is {}", traceId, userId);
      return;
    }
    if (LoanUserSupplementStatus.INIT != LoanUserSupplementStatus.fromCode(supplementCreateOrderRecord.getStatus())) {
      log.error("supplement info has error status {},userId is {}, firstTraceId {}", supplementCreateOrderRecord.getStatus(), supplementCreateOrderRecord.getUserId(), traceId);
      return;
    }
    cashLoanUserSupplementCreateOrderModel.updateStatus(supplementCreateOrderRecord, LoanUserSupplementStatus.NO_NEED);
    restoreUserCreditsStatus(userId);
    //特殊处理 IOS 用户的补件信息，恢复授信状态且补件信息过期因为信息已经无法重构
    if (riskConfig.getDealWithErrorInfoUserIdList().contains(userId) && riskConfig.getDealWithErrorInfoTraceIdList().contains(traceId)) {
      cashLoanUserSupplementCreateOrderModel.updateStatus(supplementCreateOrderRecord, LoanUserSupplementStatus.EXPIRED_CREATE_ORDER_FAILED);
      log.info("supplement User restoreCredistStatus success.please check.userId is {},TraceId is {}", userId, traceId);
      return;
    }
    tryToCreateOrderAfterSupplementProcess(supplementCreateOrderRecord);
    log.info("supplement update success,userId is {},traceId is {}, new status is {}", supplementCreateOrderRecord.getUserId(), traceId, LoanUserSupplementStatus.NO_NEED.name());
  }

  /**
   * 这里注意下，创建订单一定要在状态更新前，要不APP的状态可能就有瞬时展示错误的问题
   * 下单补件的信息一定要通过traceId来获取，不能直接获取用户最近的一条数据，避免出现用户多次下单的导致的数据紊乱的问题
   */
  private void tryToCreateOrderAfterSupplementProcess(LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord) {
    //数据过期之后不下单，类似于额度过期不下单的逻辑
    if (supplementCreateOrderRecord.getTimeExpired() < Clock.now()) {
      cashLoanUserSupplementCreateOrderModel.updateStatus(supplementCreateOrderRecord, LoanUserSupplementStatus.EXPIRED_CREATE_ORDER_FAILED);
      return;
    }
    LoanUserSupplementCreateOrderRecord newestRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByUserId(supplementCreateOrderRecord.getUserId());
    //当前的补件数据已经不是最新的了，即使不过期，也不处理
    if (!newestRecord.getId().equals(supplementCreateOrderRecord.getId())) {
      log.error("supplement data is not the newest.userId is {},newId is {},oldId is {}", supplementCreateOrderRecord.getUserId(),
          newestRecord.getId(), supplementCreateOrderRecord.getId());
      return;
    }
    CashLoanCreateOrderRequestVO requestVO = mongoSupplementBeforeCreateOrderModel.findByObjectIdOrNull(supplementCreateOrderRecord.getObjectId());
    try {
      log.info("try to create order.userId is {}", supplementCreateOrderRecord.getUserId());
      CashLoanOrderVO orderVO = loanUserOrderService.checkAndCreateOrder(requestVO, false);
      log.info("try to create order success.userId is {}", supplementCreateOrderRecord.getUserId());
      cashLoanUserSupplementCreateOrderModel.updateStatusAndOrderId(supplementCreateOrderRecord, LoanUserSupplementStatus.CREATE_ORDER_SUCCESS, orderVO.id);
      updateApplistAfterCreateOrder(orderVO, supplementCreateOrderRecord, requestVO);

    } catch (EcException e) {
      log.error("try to create order failed. userId is {}", supplementCreateOrderRecord.getUserId(), e);
      cashLoanUserSupplementCreateOrderModel.updateStatus(supplementCreateOrderRecord, LoanUserSupplementStatus.CREATE_ORDER_FAILED);
      cashLoanMonitorService.logCreateOrder(requestVO.sdkType, requestVO.sourceType, requestVO.loanAccountId, requestVO.build, requestVO.principal, requestVO.days, e.exceptionType);
    }
  }

  private void updateApplistAfterCreateOrder(CashLoanOrderVO orderVO, LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord, CashLoanCreateOrderRequestVO requestVO) {
    if (requestVO.build >= 36613) {
      uploadInformationService.insertApplistWhenSupplementSuccessBySupplementId(supplementCreateOrderRecord.getLoanAccountId(), supplementCreateOrderRecord.getId(), orderVO.id);
      return;
    }
    uploadInformationService.updateOrderIdWhenSupplementOrderSuccess(orderVO.id, supplementCreateOrderRecord.getLoanAccountId());
  }

  /**
   * 补件风控被拒，代表需要用户补件，让用户直接进入补件流程
   */
  public void updateSupplementStatusInfoByFirstRiskReject(Long userId, Long traceId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchByFirstTraceId(traceId);

    if (Objects.isNull(supplementCreateOrderRecord)) {
      log.error("there is no supplement info with firstTraceId {},userId is {}", traceId, userId);
      return;
    }
    if (LoanUserSupplementStatus.INIT != LoanUserSupplementStatus.fromCode(supplementCreateOrderRecord.getStatus())) {
      log.error("supplement info has error status {},userId is {}, firstTraceId {}", supplementCreateOrderRecord.getStatus(), supplementCreateOrderRecord.getUserId(), traceId);
      return;
    }
    cashLoanUserSupplementCreateOrderModel.updateStatusAndStep(supplementCreateOrderRecord, LoanUserSupplementStatus.NEED_SUPPLEMENT, "living");
    restoreUserCreditsStatus(userId);
    //特殊处理 IOS 用户的补件信息，恢复授信状态且补件信息过期因为信息已经无法重构
    if (riskConfig.getDealWithErrorInfoUserIdList().contains(userId) && riskConfig.getDealWithErrorInfoTraceIdList().contains(traceId)) {
      cashLoanUserSupplementCreateOrderModel.updateStatus(supplementCreateOrderRecord, LoanUserSupplementStatus.EXPIRED_CREATE_ORDER_FAILED);
      log.info("supplement User restoreCredistStatus success.please check.userId is {},TraceId is {}", userId, traceId);
      return;
    }
    log.info("supplement update success,userId is {},traceId is {}, new status is {}", supplementCreateOrderRecord.getUserId(), traceId, LoanUserSupplementStatus.NEED_SUPPLEMENT.name());
  }

  /**
   * 补件确认风控被拒
   */
  public void updateSupplementStatusInfoByConfirmRiskReject(Long userId, Long traceId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchByConfirmTraceId(traceId);

    if (Objects.isNull(supplementCreateOrderRecord)) {
      log.error("there is no supplement info,userId is {}", userId);
      return;
    }
    if (LoanUserSupplementStatus.SUBMIT_CHECK != LoanUserSupplementStatus.fromCode(supplementCreateOrderRecord.getStatus())) {
      log.error("supplement info has error status {}, userId is {}, traceId {}", supplementCreateOrderRecord.getStatus(), userId, traceId);
      return;
    }
    //补件检查不通过，回到需补件流程
    cashLoanUserSupplementCreateOrderModel.updateStatus(supplementCreateOrderRecord, LoanUserSupplementStatus.NEED_SUPPLEMENT);
    restoreUserCreditsStatus(userId);
    log.info("supplement update success,userId is {},traceId is {}，new status is {}", supplementCreateOrderRecord.getUserId(), traceId, LoanUserSupplementStatus.NEED_SUPPLEMENT.name());
  }


  public void updateSupplementStatusInfoByConfirmRiskAccept(Long userId, Long confirmTraceId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchByConfirmTraceId(confirmTraceId);

    if (Objects.isNull(supplementCreateOrderRecord)) {
      log.error("there is no supplement info,userId is {}", userId);
      return;
    }
    if (LoanUserSupplementStatus.SUBMIT_CHECK != LoanUserSupplementStatus.fromCode(supplementCreateOrderRecord.getStatus())) {
      log.error("supplement info has error status {}, userId is {}, traceId {}", supplementCreateOrderRecord.getStatus(), userId, confirmTraceId);
      return;
    }
    restoreUserCreditsStatus(userId);
    tryToCreateOrderAfterSupplementProcess(supplementCreateOrderRecord);
    log.info("supplement update success,userId is {},traceId is {}", supplementCreateOrderRecord.getUserId(), confirmTraceId);
  }

  public IDNHomepageLoanStatusV5 fetchHomepageStatusV5(Long loanAccountId, LoanUserSupplementStatus status) {
    if (Objects.isNull(status)) {
      log.info("there is no supplement info,loanAccountId is {}", loanAccountId);
      return null;
    }
    //数据过期之后，通过首页状态限制用户下单补件行为
    if (status == LoanUserSupplementStatus.EXPIRED_CREATE_ORDER_FAILED) {
      return null;
    }
    if (LoanUserSupplementStatus.INIT == status) {
      return IDNHomepageLoanStatusV5.WAITING_SUPPLEMENT;
    }
    if (LoanUserSupplementStatus.SUBMIT_CHECK == status) {
      return IDNHomepageLoanStatusV5.FINISH_SUPPLEMENT;
    }
    if (LoanUserSupplementStatus.NEED_SUPPLEMENT == status) {
      return IDNHomepageLoanStatusV5.NEED_SUPPLEMENT;
    }
    return null;
  }

  public void tryExpiredDataAndUpdateCreditsStatus(LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord) {
    if (Objects.isNull(supplementCreateOrderRecord)) {
      return;
    }
    threadTransactionalModel.transaction(configuration -> {
      loanAccountModel.findByIdForUpdateOrThrow(supplementCreateOrderRecord.getLoanAccountId());
      //已经是终态的数据了，就不要更新了
      if (LoanUserSupplementStatus.FINAl_STATUS.contains(LoanUserSupplementStatus.fromCode(supplementCreateOrderRecord.getStatus()))) {
        return;
      }
      //状态更新之后风控结果处理的时候会直接return，不会修改用户授信状态，也不会下单
      cashLoanUserSupplementCreateOrderModel.updateStatus(supplementCreateOrderRecord, LoanUserSupplementStatus.EXPIRED_CREATE_ORDER_FAILED);
      LoanUserSimpleCreditsInfoVO simpleCreditsInfoVO = loanUserCreditsService.getLoanUserSimpleCreditsInfoVOByUserId(supplementCreateOrderRecord.getUserId());
      //非审核中的状态不处理
      if (simpleCreditsInfoVO.isCreditsReject() || simpleCreditsInfoVO.isCreditsAccept()) {
        return;
      }

      // 如果补件 trace 已结束 → 处理 trace 结果时已恢复授信状态，无需再改
      // 避免: 1)trace结束恢复ACCEPT → 2)用户提交新风控改IN_REVIEW → 3)此方法过期逻辑误改回ACCEPT
      boolean supplementTraceFinished = isSupplementTraceFinished(supplementCreateOrderRecord);
      if (supplementTraceFinished) {
        log.info("supplement trace already finished, skip restore credits status. userId is {}", supplementCreateOrderRecord.getUserId());
        return;
      }

      restoreUserCreditsStatus(supplementCreateOrderRecord.getUserId());
      log.info("user supplement info expired or user credits expired. auto fix. userId is {}", supplementCreateOrderRecord.getUserId());
    });
  }

  /**
   * 判断补件 trace 是否已结束：优先取 confirmTraceId，没有则取 firstTraceId，
   * 通过 traceId 查询 loan_user_risk_trace 表的状态是否为 FINISH
   *
   * @param supplementCreateOrderRecord 补件记录
   * @return true 表示风控 trace 已结束，不应再修改授信状态
   */
  private boolean isSupplementTraceFinished(LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord) {
    Long traceId = supplementCreateOrderRecord.getConfirmTraceId();
    if (Objects.isNull(traceId)) {
      traceId = supplementCreateOrderRecord.getFirstTraceId();
    }
    if (Objects.isNull(traceId)) {
      return false;
    }
    LoanUserRiskTraceRecord riskTraceRecord = loanUserRiskTraceModel.findByTraceId(traceId);
    return Objects.nonNull(riskTraceRecord)
        && RiskFlowTraceStatusV2.FINISH == RiskFlowTraceStatusV2.fromCode(riskTraceRecord.getStatus());
  }

  public void restoreUserCreditsStatus(Long userId) {
    threadTransactionalModel.transaction(configuration -> {
      LoanUserCreditsInfoRecord creditsInfoRecord = userCreditsInfoModel.findByUserId(userId);

      if (Objects.nonNull(creditsInfoRecord.getReloanStatus())
          && LoanCreditsStatus.IN_REVIEW == LoanCreditsStatus.fromCode(creditsInfoRecord.getReloanStatus())) {
        userCreditsInfoModel.updateReloanStatus(creditsInfoRecord, LoanCreditsStatus.ACCEPTED);
        return;
      }

      if (Objects.isNull(creditsInfoRecord.getReloanStatus())
          && Objects.nonNull(creditsInfoRecord.getCreditsStatus())
          && LoanCreditsStatus.IN_REVIEW == LoanCreditsStatus.fromCode(creditsInfoRecord.getCreditsStatus())) {
        userCreditsInfoModel.updateStatus(creditsInfoRecord, LoanCreditsStatus.ACCEPTED);
      }
    });
  }

  public boolean checkNeedSupplementProcess(Long userId, Long build, SourceType sourceType) {
    if (!getSupplementSwitch(build, sourceType, userId)) {
      return false;
    }
    //使用AB完成分流
    return BooleanType.valueOf(expFacade.fetchResult(ABTestSceneType.LOAN_USER_SUPPLEMENT_BEFORE_CREATE_ORDER, ABTestUtil.genDiversionKeyMapByUserId(userId), "TRUE")).bool;
  }

  public boolean checkLastedSupplementInfoExpired(Long userId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByUserId(userId);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      return false;
    }
    return supplementCreateOrderRecord.getTimeExpired() < Clock.now();
  }

  public LoanUserSupplementCreateOrderRecord fetchLastedSupplementInfoByUserId(Long userId) {
    return cashLoanUserSupplementCreateOrderModel.fetchLastedByUserId(userId);
  }

  /**
   * 校验一下用户下单补件，是否需要跳转到活体页面
   */
  public Boolean getUserNeedJumpToLiving(Long loanAccountId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByLoanAccountId(loanAccountId);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      return false;
    }
    if (supplementCreateOrderRecord.getTimeExpired() < Clock.now()) {
      return false;
    }
    //一次补件风控之后，用户二次补件历史
    boolean isNeedSupplement = LoanUserSupplementStatus.fromCode(supplementCreateOrderRecord.getStatus()) == LoanUserSupplementStatus.NEED_SUPPLEMENT;
    if (!isNeedSupplement) {
      return false;
    }
    return Objects.isNull(supplementCreateOrderRecord.getConfirmTraceId());
  }

  public void updateTraceInfo(Long userId, Long traceId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByUserIdInProcess(userId);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      log.error("updateTraceInfo failed,do not has processing record. userId is {}, traceId is {}", userId, traceId);
      return;
    }
    if (Objects.isNull(supplementCreateOrderRecord.getFirstTraceId()) || supplementCreateOrderRecord.getFirstTraceId() == -1) {
      cashLoanUserSupplementCreateOrderModel.updateFirstTraceId(supplementCreateOrderRecord, traceId);
      return;
    }
    cashLoanUserSupplementCreateOrderModel.updateConfirmTraceId(supplementCreateOrderRecord, traceId);
  }

  public List<LoanUserSupplementCreateOrderVO> findByLoanAccountAndTraceTime(
      Long loanAccountId, Long timestamp) {
    List<LoanUserSupplementCreateOrderRecord> loanUserSupplementCreateOrderRecords =
        cashLoanUserSupplementCreateOrderModel.findByLoanAccountIdAndTimestamp(
            loanAccountId, timestamp);
    if (CollectionUtils.isEmpty(loanUserSupplementCreateOrderRecords)) {
      return new ArrayList<>();
    }
    return loanUserSupplementCreateOrderRecords.stream()
        .map(LoanUserSupplementCreateOrderVO::from)
        .collect(Collectors.toList());
  }

  public boolean supplementLivingTimeCheck(Long userId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByUserIdInProcess(userId);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      //false 的时候 APP 刷新一下首页
      return false;
    }
    return supplementLivingTimeCheck(SDKType.IDN_YQD, supplementCreateOrderRecord.getId());
  }


  public Boolean supplementLivingTimeCheck(SDKType sdkType, Long supplementId) {
    long count = riskSupplementLivingTimeLimitsLoader.getCountBySdkType(sdkType, supplementId);
    long limit = riskConfig.getSupplementLivingTimeLimits();
    log.info("count is {} ,riskConfig.riskSupplementLivingTimeLimitsLoader is {} ", count, limit);
    if (count < limit) {
      return true;
    }
    throw EcException.warn(EcExceptionType.LOAN_ACCOUNT_SUPPLEMENT_TOO_MUCH_TIME, TT.gen(riskConfig.getSupplementLivingTimeToastInfo()));
  }

  public LoanUserSupplementStatus getSupplementStatusOrNullByAccountId(Long loanAccountId) {
    LoanUserSupplementCreateOrderRecord loanUserSupplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByLoanAccountId(loanAccountId);
    if (Objects.isNull(loanUserSupplementCreateOrderRecord)) {
      return null;
    }
    return LoanUserSupplementStatus.fromCode(loanUserSupplementCreateOrderRecord.getStatus());
  }

  public void supplementLivingTimeAdd(Long userId, SDKType sdkType) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByUserIdInProcess(userId);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      return;
    }
    long res = riskSupplementLivingTimeLimitsLoader.incr(sdkType, supplementCreateOrderRecord.getId());
    log.info("supplementLivingTimeAdd add 1 ,res is {}", res);
  }

  public LoanUserSupplementCreateOrderVO fetchLastedByFirstTraceId(Long firstTraceId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchByFirstTraceId(firstTraceId);
    return LoanUserSupplementCreateOrderVO.from(supplementCreateOrderRecord);
  }

  public LoanUserSupplementCreateOrderVO fetchLastedByAccountId(Long loanAccountId) {
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchLastedByLoanAccountId(loanAccountId);
    return LoanUserSupplementCreateOrderVO.from(supplementCreateOrderRecord);
  }

 public CashLoanCreateOrderRequestVO getLastedSupplementOrderRequestInfoByAccountId(Long loanAccountId) {
   LoanUserSupplementCreateOrderVO supplementCreateOrderVO = fetchLastedByAccountId(loanAccountId);
   if (Objects.isNull(supplementCreateOrderVO)) {
     return null;
   }
   return mongoSupplementBeforeCreateOrderModel.findByObjectIdOrNull(supplementCreateOrderVO.getObjectId());

 }
}
