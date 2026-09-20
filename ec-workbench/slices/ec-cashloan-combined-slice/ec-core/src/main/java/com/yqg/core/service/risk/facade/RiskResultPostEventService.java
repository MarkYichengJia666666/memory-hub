package com.yqg.core.service.risk.facade;

import com.google.common.collect.ImmutableList;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.LoanUserCouponRecord;
import com.yqg.core.model.generated.tables.records.LoanUserSupplementCreateOrderRecord;
import com.yqg.core.model.loader.SecondOrderForRecallLoader;
import com.yqg.core.model.mongo.MongoSupplementBeforeCreateOrderModel;
import com.yqg.core.model.sql.cashloan.enums.LoanUserCreateOrderWillingStatus;
import com.yqg.core.model.sql.cashloan.order.CashLoanUserSupplementCreateOrderModel;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatus;
import com.yqg.core.model.sql.loan.coupon.LoanUserCouponModel;
import com.yqg.core.model.sql.loan.coupon.enums.LoanCouponStatus;
import com.yqg.core.model.sql.risk.enums.SubmitCreditsAdditionalInfoType;
import com.yqg.core.service.GrantCreditsLongTermExpService;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.abtest.h12026.ExpConditionFor2026H1Service;
import com.yqg.core.service.cashloan.CashLoanCreditsService;
import com.yqg.core.service.cashloan.LoanUserOrderService;
import com.yqg.core.service.cashloan.loanproduct.LoanUserProductService;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.multiloan.MultiLoanStatusService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanCreateOrderRequestVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.account.LoanAccountRevolvingService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.account.LoanUserWillingOrderService;
import com.yqg.core.service.loan.account.vo.LoanAccountRevolvingCreditVO;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.creditsconfig.config.CreditsConfig;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.service.loan.vo.LoanUserAllTempCreditsVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.risk.facade.strategy.RiskMessageEventStrategy;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.riskoutput.RiskOutputService;
import com.yqg.core.service.risk.submitadditional.SubmitCreditsAdditionalInfoService;
import com.yqg.core.service.risk.submitadditional.vo.SubmitCreditsAdditionalInfoVO;
import com.yqg.core.service.risk.subnew.SubNewAccountService;
import com.yqg.core.service.risk.subnew.SubNewConfig;
import com.yqg.core.service.risk.subnew.vo.SubNewAccountRequestVO;
import com.yqg.core.service.risk.subnew.vo.SubNewAccountResultVO;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.InferredParams;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static com.yqg.ec.common.enums.risk.LoanUserRiskType.*;

/**
 * 处理风控结果时，后置操作逻辑，包括发送消息，取消意愿订单等
 */
@Slf4j
@Service
public class RiskResultPostEventService {

  private static final List<LoanUserRiskType> RETRIEVAL_AUTO_CREATE_ORDER_RISK_TYPES = ImmutableList.of(MULTI_LOAN_CALC_CREDITS, LOAN_RETRIEVAL, RELOAN_RETRIEVAL, CALC_CREDITS);

  @Autowired
  private LoanUserWillingOrderService loanUserWillingOrderService;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private CashLoanCreditsService cashLoanCreditsService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private ProductConfigService productConfigService;
  @Autowired
  private LoanUserProductService loanUserProductService;
  @Autowired
  private LoanUserOrderService loanUserOrderService;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private CashLoanUserSupplementCreateOrderModel cashLoanUserSupplementCreateOrderModel;
  @Autowired
  private MongoSupplementBeforeCreateOrderModel mongoSupplementBeforeCreateOrderModel;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private SubmitCreditsAdditionalInfoService submitCreditsAdditionalInfoService;
  @Autowired
  private SubNewAccountService subNewAccountService;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private List<RiskMessageEventStrategy> strategies;
  @Autowired
  private LoanUserCouponModel loanUserCouponModel;
  @Autowired
  private SecondOrderForRecallLoader secondOrderForRecallLoader;
  @Autowired
  private CreditsConfig creditsConfig;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private ExpConditionFor2026H1Service expConditionFor2026H1Service;
  @Autowired
  private LoanAccountRevolvingService loanAccountRevolvingService;
  @Autowired
  private MultiLoanStatusService multiLoanStatusService;
  @Autowired
  private SubNewConfig subNewConfig;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  @Autowired
  private UserService userService;
  @Autowired
  private GrantCreditsLongTermExpService grantCreditsLongTermExpService;
  @Autowired
  private RiskOutputService riskOutputService;

  //TODO 先用这种方式处理部分需要降级风控结果才能操作的后置事件，未来重构为根据前置风控+降级风控的结果，统一触发后置事件
  public void handlePostEventAfterDegradeJudge(LoanUserRiskTraceVO preRiskTraceVO, LoanUserTagData data,
                                               CreateOrderAfterRiskResponseVO createOrderAfterRiskResponseVO, LoanUserCreditsInfoVO oldCreditsInfoVO,
                                               TriggerRiskAfterCurrentRiskVO triggerRiskAfterCurrentRiskVO) {
    for (RiskMessageEventStrategy strategy : strategies) {
      if (strategy.supports(preRiskTraceVO.riskType)) {
        strategy.handlePostEvent(preRiskTraceVO, data, createOrderAfterRiskResponseVO.createOrderSuccess, oldCreditsInfoVO, triggerRiskAfterCurrentRiskVO);
        break;
      }
    }
    boolean submitDegradeRisk = Objects.isNull(triggerRiskAfterCurrentRiskVO) ? false
        : triggerRiskAfterCurrentRiskVO.isSubmitDegradeRisk();
    handlePostEventAfterDegradeJudgeForWillingnessOrder(preRiskTraceVO, submitDegradeRisk);
    handleAutoCreateOrderInterruptEvent(preRiskTraceVO.userId, createOrderAfterRiskResponseVO);
    handleABTest(preRiskTraceVO);
  }

  /**
   * 完成风控后的分流操作
   */
  private void handleABTest(LoanUserRiskTraceVO preRiskTraceVO) {
    log.info("handleABTest, preRiskTraceVO Id:{}, traceStatus {}, oldCreditsInfoVO accountId is:{}", preRiskTraceVO.traceId,
       preRiskTraceVO.creditsStatus, preRiskTraceVO.accountId);
    if (preRiskTraceVO.creditsStatus != LoanCreditsStatus.ACCEPTED) {
      log.info("handleABTest creditsStatus is not ACCEPTED, traceId:{} accountId is:{}", preRiskTraceVO.traceId, preRiskTraceVO.accountId);
      return;
    }
    //上线之后观察，没有异常去掉 try Catch
    try {
      triggerCouponTempCreditsLimitAbtestAfterRisk(preRiskTraceVO);
    } catch (Exception e) {
      log.error("triggerCouponTempCreditsLimitAbtestAfterRisk error, traceId:{}, accountId:{}", preRiskTraceVO.traceId, preRiskTraceVO.accountId, e);
    }
  }

  private void triggerCouponTempCreditsLimitAbtestAfterRisk(LoanUserRiskTraceVO preRiskTraceVO) {
    //没优惠券临额，不触发实验
    LoanUserAllTempCreditsVO allTempCreditsVO = loanUserCreditsService.getLoanUserAllTempCreditsVOByAccountIdAndTime(preRiskTraceVO.accountId, Clock.now());
    if (Objects.isNull(allTempCreditsVO)
       || Objects.isNull(allTempCreditsVO.loanUserTempCreditsVOForCoupon)
       || Objects.isNull(allTempCreditsVO.loanUserTempCreditsVOForCoupon.couponId)) {
      log.info("triggerAbtestOfLoanTempCreditsLimitForLoanUserAfterRisk allTempCreditsVO is null, accountId:{}", preRiskTraceVO.accountId);
      return;
    }
    // 触发分流(此处流量目前都来自B端)
    InferredParams inferredParams = userService.inferMissingParams(preRiskTraceVO.userId);
    triggerAbtestOfLoanTempCreditsLimit(preRiskTraceVO.userId, preRiskTraceVO.accountId, inferredParams.getSdkType(),
        inferredParams.getBuild());
    triggerAbtestOfReLoanTempCreditsLimitAfterRisk(preRiskTraceVO);
  }

  private void triggerAbtestOfReLoanTempCreditsLimitAfterRisk(LoanUserRiskTraceVO preRiskTraceVO) {
    //复贷一次风控
    Boolean isReloanRiskType = LoanUserRiskType.getReLoanCalcRiskType().contains(preRiskTraceVO.riskType)
       || LoanUserRiskType.getMultiLoanCalcRiskType().contains(preRiskTraceVO.riskType);
    if (!isReloanRiskType) {
      log.info("triggerAbtestOfReLoanTempCreditsLimitV2 isReloanRiskType is false, accountId:{}", preRiskTraceVO.accountId);
      return;
    }
    triggerAbTestOfReloanTempCreditsLimit(preRiskTraceVO.userId, preRiskTraceVO.accountId);
  }

  /**
   * 触发复贷临额上线实验
   * @param userId
   * @param accountId
   */
  public String triggerAbTestOfReloanTempCreditsLimit(Long userId, Long accountId) {
    Boolean canTriggerNewCreditsAbtest = checkUserCanTriggerNewCouponLimitForReloanNormalAbtest(userId, accountId);
    if (!canTriggerNewCreditsAbtest) {
      return "A";
    }

    ExpUser expUser = ExpUser.builder().userId(userId).versionBuild(0L).build();
    String fatherRes = grantCreditsLongTermExpService.canGrantAfterLongTermLastExpResultForReloan(userId);
    log.info("CouponCreditLimit triggerAbtestOfReloanTempCreditsLimitAfterRisk userId:{}, fatherRes {}", userId, fatherRes);
    boolean hitFather = StringUtils.equals(CommonABTestResultGroup.A.name(), fatherRes);
    if (!hitFather) {
      return "A";
    }
    return triggerSubAbtestOfReloanTempCreditsLimit(userId, accountId, expUser);
  }

  private String triggerSubAbtestOfReloanTempCreditsLimit(Long userId, Long accountId, ExpUser expUser) {
    //常规和非常规用户区分
    boolean isRetrievalUser = loanAccountBasicInfoService.isRetrievalOrReapplyUserByAccountId(accountId);
    String res = "BLANK_GROUP";
    if (isRetrievalUser) {
      res = expDiversionClient.getString(creditsConfig.getTotalCouponCreditLimitReloanNotNormalNewAbtestKey(), expUser, CommonABTestResultGroup.A.name());
    }

    if (!isRetrievalUser) {
      res = expDiversionClient.getString(creditsConfig.getTotalCouponCreditLimitReloanNormalNewAbtestKey(), expUser, CommonABTestResultGroup.A.name());
    }
    log.info("CouponCreditLimit enter subAbtest triggerSubAbtestOfReloanTempCredtisLimit res is {}, userId:{}, accountId:{}", res, userId, accountId);
    return res;
  }

  /**
   * 复贷-常规用户-优惠券额度上限校验
   * 未管制
   * 不是次新
   */
  private Boolean checkUserCanTriggerNewCouponLimitForReloanNormalAbtest(Long userId, Long accountId) {
    boolean couponLimitOfReloanNormalSwitch = creditsConfig.getCouponLimitOfReloanNormalSwitch();
    if (couponLimitOfReloanNormalSwitch == false) {
      log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitAbtest couponLimitOfReloanNormalSwitch is false, accountId:{}", accountId);
      return false;
    }
    boolean isReloan = loanAccountBasicInfoService.isReloan(accountId);
    if (!isReloan) {
      log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitAbtest is not reloan, accountId:{}", accountId);
      return false;
    }
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.getLastFinishCreditsRiskTrace(accountId);

    if (loanUserRiskTraceVO.creditsStatus != LoanCreditsStatus.ACCEPTED) {
      log.info("CouponCreditLimit handleABTest creditsStatus is not ACCEPTED, traceId:{} accountId is:{}", loanUserRiskTraceVO.traceId, loanUserRiskTraceVO.accountId);
      return false;
    }
    if (LoanUserRiskType.getMultiLoanCalcRiskType().contains(loanUserRiskTraceVO.riskType)) {
      MultiLoanStatus multiLoanStatus = multiLoanStatusService.getStatusOrNull(loanUserRiskTraceVO.accountId);
      if (Objects.isNull(multiLoanStatus) || MultiLoanStatus.CALC_ACCEPTED != multiLoanStatus) {
        log.info("CouponCreditLimit triggerAbtestOfReLoanTempCreditsLimitV2 multiLoanStatus is error, skip abtest, accountId:{}", loanUserRiskTraceVO.accountId);
        return false;
      }
    }

    // 排除循环贷风控通过但被管控
    LoanAccountRevolvingCreditVO revolvingCreditVO = loanAccountRevolvingService.fetchByLoanAccountId(accountId);
    if (Objects.nonNull(revolvingCreditVO) && revolvingCreditVO.getUserControl()) {
      log.info("CouponCreditLimit triggerAbtestOfReLoanTempCreditsLimitV2 revolvingCreditVO is false, accountId:{}", accountId);
      return false;
    }
    // 排除次新客（风控标签）
    SubNewAccountResultVO subNewAccountResultVO = subNewAccountService.checkSubnewAccountByIntervalDays(SubNewAccountRequestVO.builder().loanAccountId(accountId).build());
    log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitAbtest subNewAccountResultVO is {}, accountId:{}", JsonUtils.toString(subNewAccountResultVO), accountId);

    if (Objects.nonNull(subNewAccountResultVO) && subNewAccountResultVO.getIsSubNewUser()) {
      log.info("CouponCreditLimit triggerAbtestOfReLoanTempCreditsLimit is subNew user, skip abtest, accountId:{}", accountId);
      return false;
    }
    BigDecimal riskCouponLimitCredits = riskOutputService.getCouponLimitCreditsByUserId(userId);
    if (Objects.isNull(riskCouponLimitCredits) || riskCouponLimitCredits.compareTo(BigDecimal.ZERO) <= 0) {
      log.info("CouponCreditLimit triggerAbtestOfReLoanTempCreditsLimitV2 riskCouponLimitCredits is null, accountId:{}", accountId);
      return false;
    }
    log.info("CouponCreditLimit triggerAbtestOfReLoanTempCreditsLimitV2 is true, accountId:{}", accountId);
    return true;
  }

  public String triggerAbtestOfLoanTempCreditsLimit(Long userId, Long accountId, SDKType sdkType, Long build) {
    boolean isReloan = loanAccountBasicInfoService.isReloan(accountId);
    if (isReloan) {
      log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitForLoanRetrievalAbtest is reloan, accountId:{}", accountId);
      return "A";
    }

    Boolean checkAccount = checkUserCanTriggerNewCouponLimitForLoanAbtest(accountId);
    if (!checkAccount) {
      return "A";
    }

    ExpUser expUser = ExpUser.builder().userId(userId).versionBuild(0L).build();
    String fatherRes = grantCreditsLongTermExpService.canGrantAfterLongTermLastExpResultForLoan(userId);
    boolean hitFather = StringUtils.equals(CommonABTestResultGroup.A.name(), fatherRes);
    log.info("CouponCreditLimit handleABTest getCouponLimitRiskTypeList userId is:{}, hitFather is {}", userId, hitFather);
    if (!hitFather) {
      return "A";
    }

    return triggerSubAbtestForLoanTempCreditsLimit(userId, accountId, sdkType, build, expUser);
  }

  private String triggerSubAbtestForLoanTempCreditsLimit(Long userId, Long accountId, SDKType sdkType, Long build,
                                                         ExpUser expUser) {
    //是否为回捞用户
    boolean isRetrievalUser = loanAccountBasicInfoService.isRetrievalOrReapplyUserByAccountId(accountId);
    String res = "BLANK_GROUP";
    if (isRetrievalUser && expConditionFor2026H1Service.matchesFirstLoanLaneCondition(userId, sdkType, build)) {
      res = expDiversionClient.getString(creditsConfig.getCouponCreditLimitLoanRetrievalAbtestKey(), expUser, CommonABTestResultGroup.A.name());
    }

    if (!isRetrievalUser && expConditionFor2026H1Service.matchesFirstLoanLaneCondition(userId, sdkType, build)) {
      if (creditsConfig.shouldBypassCouponCreditLimitLoanNormalPlatform(userId)) {
        res = creditsConfig.getCouponCreditLimitLoanNormalPushFullResult();
        log.info("CouponCreditLimit loan normal bypass platform, res={}, userId={}", res, userId);
      } else {
        res = expDiversionClient.getString(creditsConfig.getCouponCreditLimitLoanNormalAbtestKey(), expUser, CommonABTestResultGroup.A.name());
      }
    }
    log.info("CouponCreditLimit enter subAbtest triggerSubAbtestForLoanTempCreditsLimit res is {}, userId:{}, accountId:{}", res, userId, accountId);
    return res;
  }

  /**
   * 首贷-回捞用户-优惠券额度上限校验
   */
  private Boolean checkUserCanTriggerNewCouponLimitForLoanAbtest(Long accountId) {
    boolean loanCouponLimitOfRetrievalSwitch = creditsConfig.getLoanCouponLimitOfLoanRetrievalSwitch();
    if (loanCouponLimitOfRetrievalSwitch == false) {
      log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitForLoanRetrievalAbtest loanCouponLimitOfRetrievalSwitch is false, accountId:{}", accountId);
      return false;
    }

    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.getLastFinishCreditsRiskTrace(accountId);

    if (loanUserRiskTraceVO.creditsStatus != LoanCreditsStatus.ACCEPTED) {
      log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitForLoanRetrievalAbtest creditsStatus is not ACCEPTED, traceId:{} accountId is:{}", loanUserRiskTraceVO.traceId, loanUserRiskTraceVO.accountId);
      return false;
    }

    // 排除次新客（风控标签）
    SubNewAccountResultVO subNewAccountResultVO = subNewAccountService.checkSubnewAccountByIntervalDays(SubNewAccountRequestVO.builder().loanAccountId(accountId).build());
    log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitForLoanRetrievalAbtest subNewAccountResultVO is {}, accountId:{}", JsonUtils.toString(subNewAccountResultVO), accountId);

    if (Objects.nonNull(subNewAccountResultVO) && subNewAccountResultVO.getIsSubNewUser()) {
      log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitForLoanRetrievalAbtest is subNew user, skip abtest, accountId:{}", accountId);
      return false;
    }
    log.info("CouponCreditLimit checkUserCanTriggerNewCouponLimitForLoanRetrievalAbtest is true, accountId:{}", accountId);
    return true;
  }

  private void handleAutoCreateOrderInterruptEvent(Long userId, CreateOrderAfterRiskResponseVO
     createOrderAfterRiskResponseVO) {
    if (createOrderAfterRiskResponseVO.retrievalAcceptAndTryCreateOrder && !createOrderAfterRiskResponseVO.createOrderSuccess) {
      log.info("createOrderAfterRiskResponseVO.needCreateOrder is true, but createOrderAfterRiskResponseVO.createOrderSuccess is false, cancel three trace order, userId:{}", userId);
      secondOrderForRecallLoader.set(userId);
    } else {
      secondOrderForRecallLoader.del(userId);
    }
  }

  public void handlePostEventAfterDegradeJudgeForWillingnessOrder(LoanUserRiskTraceVO preRiskTraceVO,
                                                                  boolean submitDegradeRisk) {
    if (preRiskTraceVO.creditsStatus != LoanCreditsStatus.REJECTED) {
      return;
    }
    cancelWillingnessOrderWhenPreRiskReject(preRiskTraceVO.riskType, submitDegradeRisk, preRiskTraceVO.accountId);
  }

  public void cancelWillingnessOrderWhenPreRiskReject(LoanUserRiskType preRiskType, boolean submitDegradeRisk, Long
     accountId) {
    //首贷测额被拒，并且未触发降级风控，才取消意愿订单
    if (!LoanUserRiskType.getLoanCalcRiskType().contains(preRiskType)) {
      return;
    }
    if (submitDegradeRisk) {
      return;
    }
    LoanAccountRecord accountRecord = loanAccountModel.findByIdOrThrow(accountId);
    //取消意愿单
    loanUserWillingOrderService.updateUserWillingnessStatus(accountRecord.getUserId(), LoanUserCreateOrderWillingStatus.MATCH_FAILED);

  }

  public CreateOrderAfterRiskResponseVO createOrderAfterRisk(LoanUserRiskTraceVO currentRiskTraceVO, LoanUserTagData
     data) {
    if (riskConfig.getAutoCreateOrderOldLogicSwitch()) {
      return CreateOrderAfterRiskResponseVO.from(false, false);
    }
    CashLoanOrderVO latestOrderVO = ecOrderService.getLatestOrderVO(currentRiskTraceVO.accountId);
    if (Objects.isNull(latestOrderVO)) {
      log.info("当前用户不存在订单:{}, traceId:{}", currentRiskTraceVO.accountId, currentRiskTraceVO.traceId);
      return CreateOrderAfterRiskResponseVO.from(false, false);
    }
    // 校验是否能创建订单
    if (!shouldTriggerOrderCreationByRiskType(currentRiskTraceVO, latestOrderVO, data)) {
      log.info("当前不能创建订单，前置订单id：{}, traceId:{}", latestOrderVO.id, currentRiskTraceVO.traceId);
      return CreateOrderAfterRiskResponseVO.from(false, false);
    }

    // 查询补件下单信息
    LoanUserSupplementCreateOrderRecord supplementCreateOrderRecord = cashLoanUserSupplementCreateOrderModel.fetchByOrderId(latestOrderVO.id);
    if (Objects.isNull(supplementCreateOrderRecord)) {
      log.warn("not found supplementCreateOrderRecord by orderId:{}, traceId:{}", latestOrderVO.id, currentRiskTraceVO.traceId);
      return CreateOrderAfterRiskResponseVO.from(true, false);
    }
    CashLoanCreateOrderRequestVO requestVO = mongoSupplementBeforeCreateOrderModel.findByObjectIdOrNull(supplementCreateOrderRecord.getObjectId());

    // 获取产品信息
    LoanProductConfigVO productConfigVO = getMatchedProductFromRiskTrace(currentRiskTraceVO, latestOrderVO, requestVO.principal, requestVO.couponId);
    if (productConfigVO == null) {
      log.info("匹配不到产品信息，traceId:{}, userId:{}", currentRiskTraceVO.traceId, currentRiskTraceVO.userId);
      return CreateOrderAfterRiskResponseVO.from(true, false);
    }
    log.info("createOrderAfterRisk productId:{}", productConfigVO.id);
    // 实验分流
    if (cashLoanCreditsService.isInRecallTriggerSecondRiskOrderGroupA(currentRiskTraceVO.userId, currentRiskTraceVO.riskType)) {
      log.info("实验分流，userId:{}, traceId:{} is CommonABTestResultGroup.A", currentRiskTraceVO.userId, currentRiskTraceVO.traceId);
      return CreateOrderAfterRiskResponseVO.from(true, false);
    }

    // 创建订单
    CashLoanOrderVO orderVO = null;
    try {
      requestVO.productId = YqgHashids.encode(productConfigVO.id);
      requestVO.preOrderId = latestOrderVO.id;
      orderVO = loanUserOrderService.checkAndCreateOrder(requestVO, false);
    } catch (Exception e) {
      log.error("Exception occurred while creating order for orderId: {}, traceId:{}, userId:{}, error: ", latestOrderVO.id, currentRiskTraceVO.traceId, currentRiskTraceVO.userId, e);
    }
    boolean createOrderResult = orderVO != null;
    return CreateOrderAfterRiskResponseVO.from(true, createOrderResult);
  }

  public Boolean shouldTriggerOrderCreationByRiskType(LoanUserRiskTraceVO currentRiskTraceVO, CashLoanOrderVO
     latestOrderVO, LoanUserTagData data) {
    //1 风控类型需为指定类型
    if (!RETRIEVAL_AUTO_CREATE_ORDER_RISK_TYPES.contains(currentRiskTraceVO.riskType)) {
      log.info("当前风控类型非指定类型，loanAccountId {}, traceId:{}", currentRiskTraceVO.accountId, currentRiskTraceVO.traceId);
      return false;
    }

    if (loanUserRiskTraceService.isMultiRiskReject(currentRiskTraceVO, data.rejectMultiApply, data.rejectMultiOrder)
       || loanUserRiskTraceService.isLoanRiskReject(RETRIEVAL_AUTO_CREATE_ORDER_RISK_TYPES, currentRiskTraceVO, currentRiskTraceVO.creditsStatus)) {
      log.info("状态未通过，loanAccountId {}, traceId:{}", currentRiskTraceVO.accountId, currentRiskTraceVO.traceId);
      return false;
    }

    //2 订单是否被拒绝
    if (latestOrderVO.status != CashLoanOrderStatus.REJECT) {
      log.info("当前订单非拒绝状态，loanAccountId {}, orderId:{}", currentRiskTraceVO.accountId, currentRiskTraceVO.orderId);
      return false;
    }

    //3 判断当前trace是不是回捞trace
    SubmitCreditsAdditionalInfoVO submitCreditsAdditionalInfoVO = submitCreditsAdditionalInfoService.fetchByLoanUserRiskIdAndType(currentRiskTraceVO.id, SubmitCreditsAdditionalInfoType.PRE_TRIGGER_RETRIEVAL);
    if (submitCreditsAdditionalInfoVO == null) {
      log.info("当前trace非回捞trace，loanAccountId {}, traceId:{}", currentRiskTraceVO.accountId, currentRiskTraceVO.traceId);
      return false;
    }

    //4 查找最近一次订单的风控是不是 指定风控类型
    LoanUserRiskTraceVO loanUserRiskTraceVO = loanUserRiskTraceService.findByIdOrThrow(submitCreditsAdditionalInfoVO.getRecord().getPreLoanUserRiskId());
    if (!Objects.equals(loanUserRiskTraceVO.traceId, latestOrderVO.traceId)) {
      log.info("前一次traceId与订单id比较不同，loanAccountId {}, traceId:{}, compareTraceId:{}", loanUserRiskTraceVO.accountId, loanUserRiskTraceVO.traceId, latestOrderVO.traceId);
      return false;
    }

    if (!isMatchingPreRisk(currentRiskTraceVO.riskType, loanUserRiskTraceVO.riskType)) {
      log.info("前一次订单风控非指定风控，loanAccountId {}, traceId:{}", loanUserRiskTraceVO.accountId, loanUserRiskTraceVO.traceId);
      return false;
    }
    return true;
  }

  private Boolean isMatchingPreRisk(LoanUserRiskType currentRiskType, LoanUserRiskType preRiskType) {
    switch (currentRiskType) {
      case MULTI_LOAN_CALC_CREDITS:
        return preRiskType == FIRST_MULTI_LOAN
           || preRiskType == MULTI_LOAN
           || preRiskType == REVOLVING_LOAN_SECOND;
      case LOAN_RETRIEVAL:
        return preRiskType == SECOND
           || preRiskType == LOAN_RE_ORDER_CREDITS;
      case RELOAN_RETRIEVAL:
      case CALC_CREDITS:
        return preRiskType == RELOAN
           || preRiskType == REVOLVING_LOAN_SECOND;
      default:
        return false;
    }
  }

  private LoanProductConfigVO getMatchedProductFromRiskTrace(LoanUserRiskTraceVO currentRiskTraceVO, CashLoanOrderVO
     latestOrderVO, BigDecimal principal, Long couponId) {
    LoanUserCreditsInfoVO creditsInfoVO = loanUserCreditsService.genLoanUserCreditsInfoByAccountId(currentRiskTraceVO.accountId);
    // 授信额度>=本笔订单提现金额
    if (creditsInfoVO.totalRemainCredits == null || latestOrderVO.principal == null
       || creditsInfoVO.totalRemainCredits.compareTo(latestOrderVO.principal) < 0) {
      log.warn("User credit limit is insufficient. userId:{}, credit:{}, principal:{}", currentRiskTraceVO.userId, creditsInfoVO.totalRemainCredits, latestOrderVO.principal);
      return null;
    }

    // 校验优惠券是否可用
    if (Objects.nonNull(couponId)) {
      LoanUserCouponRecord toBindRecord = loanUserCouponModel.findByIdOrThrow(couponId);
      LoanCouponStatus loanCouponStatus = LoanCouponStatus.fromCode(toBindRecord.getStatus());
      if (!LoanCouponStatus.ENABLED_STATUS_LIST.contains(loanCouponStatus)) {
        log.info("user coupon already expire or invalid, couponId:{}", couponId);
        return null;
      }
    }

    // 获取最近订单产品
    LoanProductConfigVO lastOrderProductConfig = productConfigService.getProductVO(latestOrderVO.productId);
    if (lastOrderProductConfig == null) {
      log.error("not found last order product by productId:{}", latestOrderVO.productId);
      return null;
    }

    LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(currentRiskTraceVO.accountId);
    List<LoanProductConfigVO> availableLoanProductList = loanUserProductService.filterProduct(currentRiskTraceVO.userId, loanAccountVO.sdkType, loanAccountVO.loanUserTypeVO, creditsInfoVO.totalRemainCredits).productConfigList;
    if (CollectionUtils.isEmpty(availableLoanProductList)) {
      log.error("current user does not have any available products, userId:{}", currentRiskTraceVO.userId);
      return null;
    }

    // 查找与订单产品某些属性相同的产品
    return availableLoanProductList.stream()
       .filter(product -> BigDecimalHelper.lessThanOrEqual(principal, product.maxCredits.getAmountInYuan()) &&
          BigDecimalHelper.greaterThanOrEqual(principal, product.minCredits.getAmountInYuan()))
       .filter(product -> LoanProductConfigVO.isSamePeriodAndRateConfig(product, lastOrderProductConfig))
       .findFirst()
       .orElse(null);
  }

}
