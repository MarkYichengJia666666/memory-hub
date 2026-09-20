package com.yqg.core.service.loan.account;

import com.google.api.client.util.Sets;
import com.google.common.collect.Lists;
import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.generated.tables.records.LoanUserRiskTraceRecord;
import com.yqg.core.model.generated.tables.records.RiskAccountVerifyLogRecord;
import com.yqg.core.model.loader.RiskAccountVerifyLoader;
import com.yqg.core.model.mongo.MongoRiskAccountVerifyInfoModel;
import com.yqg.core.model.sql.abtest.enums.DiversionKeyType;
import com.yqg.core.model.sql.loan.account.LoanUserCreditsInfoModel;
import com.yqg.core.model.sql.loan.account.RiskAccountVerifyLogModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.feature.enums.LoanType;
import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.core.model.sql.mobile.enums.VerificationPurposeType;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.loan.account.enums.RiskAccountVerifyStatus;
import com.yqg.core.service.loan.account.enums.RiskSceneType;
import com.yqg.core.service.loan.account.enums.VerifyType;
import com.yqg.core.service.loan.account.vo.RiskAccountContextVO;
import com.yqg.core.service.loan.infos.RiskAccountVerifyInfoPojo;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsVO;
import com.yqg.core.service.mobile.verification.VerificationService;
import com.yqg.core.service.user.UserService;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RiskAccountService {
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private VerificationService verificationService;
  @Autowired
  private UserService userService;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private RiskAccountVerifyLogModel riskAccountVerifyLogModel;
  @Autowired
  private LoanUserCreditsInfoModel loanUserCreditsInfoModel;
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;
  @Autowired
  private MongoRiskAccountVerifyInfoModel mongoRiskAccountVerifyInfoModel;
  @Autowired
  private RiskAccountVerifyLoader riskAccountVerifyLoader;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;

  private static final String BEST = "best";
  private static final String ENV = "env";

  public List<VerifyType> checkCreateOrder(RiskAccountContextVO context) {
    boolean isReloan = loanAccountService.isReloan(context.loanAccountId);
    LoanType loanType = isReloan ? LoanType.RELOAN : LoanType.LOAN;
    Set<VerifyType> newDeviceVerifyTypes = checkNewDevice(context, loanType);
    Set<VerifyType> gapDaysVerifyTypes = checkCreateOrderGapDays(context, loanType);

    Set<VerifyType> verifyTypeSet = Sets.newHashSet();
    verifyTypeSet.addAll(newDeviceVerifyTypes);
    verifyTypeSet.addAll(gapDaysVerifyTypes);
    checkFailMaxTimes(verifyTypeSet, context);
    initVerifyLog(context, loanType, newDeviceVerifyTypes, gapDaysVerifyTypes);
    return verifyTypeSet.stream().sorted(Comparator.comparing(VerifyType::getPriority)).collect(Collectors.toList());
  }

  private Set<VerifyType> checkNewDevice(RiskAccountContextVO context, LoanType loanType) {
    Set<VerifyType> verifyTypeSet = Sets.newHashSet();
    Map<DiversionKeyType, String> diversionKeyMap = new HashMap<>();
    diversionKeyMap.put(DiversionKeyType.USER_ID, context.userId.toString());
    LoanUserCreditsInfoRecord record = loanUserCreditsInfoModel.findByAccountId(context.loanAccountId);
    if (record == null || LoanCreditsStatus.ACCEPTED != LoanCreditsStatus.fromCodeOrNull(record.getCreditsStatus())) {
      throw EcException.error(EcExceptionType.COMMON_SERVER_ERROR, "credits info not found or status not accepted, accountId: {}", context.loanAccountId);
    }
    for (VerifyType verifyType : verifyTypeSet.toArray(new VerifyType[0])) {
      List<RiskAccountVerifyStatus> statuses = Collections.singletonList(RiskAccountVerifyStatus.SUCCESS);
      Condition condition = riskAccountVerifyLogModel.buildCondition(context.loanAccountId, context.deviceToken, RiskSceneType.NEW_DEVICE_LIST, null, verifyType, statuses, null, null);
      RiskAccountVerifyLogRecord verifyLogRecord = riskAccountVerifyLogModel.findLatestByCondition(condition);
      if (verifyLogRecord == null && VerifyType.LIVING == verifyType) {
        statuses = Lists.newArrayList(RiskAccountVerifyStatus.UPLOADED);
        Long startTime = Clock.getMinMillisOfDay(Clock.now(), context.sdkType.getTimeZone());
        condition = riskAccountVerifyLogModel.buildCondition(context.loanAccountId, context.deviceToken, RiskSceneType.NEW_DEVICE_LIST, null, verifyType, statuses, startTime, null);
        verifyLogRecord = riskAccountVerifyLogModel.findLatestByCondition(condition);
      }
      if (verifyLogRecord != null) {
        verifyTypeSet.remove(verifyType);
      }
    }
    return verifyTypeSet;
  }

  private Set<VerifyType> checkCreateOrderGapDays(RiskAccountContextVO context, LoanType loanType) {
    Set<VerifyType> verifyTypeSet = Sets.newHashSet();
    int days;
    Map<VerifyType, Integer> verifyTypeGapDaysMap;
    if (LoanType.RELOAN == loanType) {
      // TODO(zhuangyin) 这里先不区分复贷和续借，后面看怎么处理
      List<LoanUserRiskType> riskTypeList = Lists.newArrayList();
      riskTypeList.addAll(LoanUserRiskType.getMultiLoanCalcRiskType());
      riskTypeList.addAll(LoanUserRiskType.getReloanCalcCreditWithoutRevolvingRiskType());
      LoanUserRiskTraceRecord record = loanUserRiskTraceModel.findLastAcceptRiskTraceByAccountIdAndRiskTypes(context.loanAccountId, riskTypeList);
      if (record == null) {
        log.error("reloan credits calc risk trace not found, accountId: {}", context.loanAccountId);
        days = 0;
      } else {
        days = Clock.getAbsCalenderDaysBetween(record.getTimeUpdated(), Clock.now(), context.sdkType.getTimeZone());
      }
      verifyTypeGapDaysMap = cashLoanConfig.getCreateOrderGapDaysConfig(LoanType.RELOAN);
    } else {
      LoanUserCreditsInfoRecord record = loanUserCreditsInfoModel.findByAccountId(context.loanAccountId);
      if (record == null || LoanCreditsStatus.ACCEPTED != LoanCreditsStatus.fromCodeOrNull(record.getCreditsStatus())) {
        throw EcException.error(EcExceptionType.COMMON_SERVER_ERROR, "loan credits info not found, accountId: {}", context.loanAccountId);
      }
      days = Clock.getAbsCalenderDaysBetween(record.getTimeAccepted(), Clock.now(), context.sdkType.getTimeZone());
      verifyTypeGapDaysMap = cashLoanConfig.getCreateOrderGapDaysConfig(LoanType.LOAN);
    }
    int validDays = cashLoanConfig.getCreateOrderGapDaysValidDays();
    Long startTime = Clock.getMinMillisOfPlusDays(Clock.now(), 1 - validDays, context.sdkType.getTimeZone());
    for (Map.Entry<VerifyType, Integer> entry : verifyTypeGapDaysMap.entrySet()) {
      if (days >= entry.getValue()) {
        List<RiskAccountVerifyStatus> statuses = Collections.singletonList(RiskAccountVerifyStatus.SUCCESS);
        Condition condition = riskAccountVerifyLogModel.buildCondition(context.loanAccountId, null, RiskSceneType.GAP_DAYS_LIST, loanType, entry.getKey(), statuses, startTime, null);
        RiskAccountVerifyLogRecord record = riskAccountVerifyLogModel.findLatestByCondition(condition);
        if (record == null) {
          if (VerifyType.LIVING == entry.getKey()) {
            statuses = Lists.newArrayList(RiskAccountVerifyStatus.UPLOADED);
          }
          startTime = Clock.getMinMillisOfDay(Clock.now(), context.sdkType.getTimeZone());
          condition = riskAccountVerifyLogModel.buildCondition(context.loanAccountId, null, RiskSceneType.GAP_DAYS_LIST, loanType, entry.getKey(), statuses, startTime, null);
          record = riskAccountVerifyLogModel.findLatestByCondition(condition);
          if (record != null) {
            continue;
          }
          verifyTypeSet.add(entry.getKey());
        }
      }
    }
    return verifyTypeSet;
  }

  public void checkFailMaxTimes(Set<VerifyType> verifyTypeList, RiskAccountContextVO context) {
    if (CollectionUtils.isEmpty(verifyTypeList)) {
      return;
    }
    Map<VerifyType, Integer> verifyMaxTimesConfig = cashLoanConfig.getCreateOrderVerifyMaxTimesConfig();
    for (VerifyType verifyType : verifyTypeList) {
      int maxTimes = verifyMaxTimesConfig.get(verifyType);
      int failTimes = riskAccountVerifyLoader.getOrDefault(context.loanAccountId, verifyType, 0);
      if (failTimes >= maxTimes) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("今日连续多次验证身份失败，请次日再来尝试。"));
      }
    }
  }

  private void initVerifyLog(RiskAccountContextVO context, LoanType loanType, Set<VerifyType> newDeviceVerifyTypes, Set<VerifyType> gapDaysVerifyTypes) {
    Set<VerifyType> allVerifyTypes = Sets.newHashSet();
    allVerifyTypes.addAll(newDeviceVerifyTypes);
    allVerifyTypes.addAll(gapDaysVerifyTypes);
    if (allVerifyTypes.isEmpty()) {
      return;
    }
    for (VerifyType verifyType : allVerifyTypes) {
      RiskSceneType riskSceneType;
      List<RiskSceneType> riskSceneTypeList;
      if (newDeviceVerifyTypes.contains(verifyType)) {
        if (gapDaysVerifyTypes.contains(verifyType)) {
          riskSceneType = RiskSceneType.NEW_DEVICE_AND_GAP_DAYS;
          riskSceneTypeList = Collections.singletonList(riskSceneType);
        } else {
          riskSceneType = RiskSceneType.NEW_DEVICE;
          riskSceneTypeList = RiskSceneType.NEW_DEVICE_LIST;
        }
      } else {
        riskSceneType = RiskSceneType.GAP_DAYS;
        riskSceneTypeList = RiskSceneType.GAP_DAYS_LIST;
      }
      Condition condition = riskAccountVerifyLogModel.buildCondition(context.loanAccountId, context.deviceToken, riskSceneTypeList, loanType, verifyType, Collections.singletonList(RiskAccountVerifyStatus.INIT), null, null);
      RiskAccountVerifyLogRecord record = riskAccountVerifyLogModel.findLatestByCondition(condition);
      // 每种场景只有一条INIT记录，考虑下设备、风险场景都相同时的场景，新设备验证成功永久有效，不会再触发，只考虑间隔天数验证
      // 触发一次下单间隔天数验证，验证成功有效期内不会再次触发时，再次触发一定是验证有效期过期，需要再次插入一条INIT记录
      if (record != null) {
        if (riskSceneType == RiskSceneType.GAP_DAYS) {
          // 验证成功后再次触发才插入，没有成功的不插入
          Condition successCondition = riskAccountVerifyLogModel.buildCondition(context.loanAccountId, null, riskSceneTypeList, loanType, verifyType, Collections.singletonList(RiskAccountVerifyStatus.SUCCESS), record.getTimeCreated(), null);
          RiskAccountVerifyLogRecord successRecord = riskAccountVerifyLogModel.findLatestByCondition(successCondition);
          if (successRecord == null) {
            continue;
          }
        } else {
          continue;
        }
      }
      RiskAccountVerifyInfoPojo pojo = new RiskAccountVerifyInfoPojo(context);
      String objectId = mongoRiskAccountVerifyInfoModel.insert(pojo);
      riskAccountVerifyLogModel.insert(context.loanAccountId, context.deviceToken, riskSceneType, loanType, verifyType, RiskAccountVerifyStatus.INIT, objectId);
    }
  }

  public Boolean checkOtp(String verificationCode, RiskAccountContextVO context) {
    String normalizedMobileNumberByUserId = userService.getNormalizedMobileNumberByUserId(context.userId);
    try {
      verificationService.checkMobileVerificationCode(normalizedMobileNumberByUserId, verificationCode, VerificationPurposeType.VERIFY_CREATE_ORDER, context.sdkType);
    } catch (Exception e) {
      if (e instanceof EcException) {
        EcException ecException = (EcException) e;
        if (EcExceptionType.MOBILE_INVALID_VERIFICATION_CODE == ecException.exceptionType || EcExceptionType.MOBILE_VERIFICATION_CODE_EXPIRED == ecException.exceptionType) {
          return Boolean.FALSE;
        }
      }
      throw e;
    }
    return Boolean.TRUE;
  }

  public Boolean checkCurp(String curp, RiskAccountContextVO context) {
    LoanAccountDetailsVO detailsVO = loanAccountDetailsService.getByAccountId(context.loanAccountId);
    String idNo = null;
    if (StringUtils.isBlank(idNo)) {
      idNo = detailsVO.detailsPojo.idInfo.getIdNo();
    }
    return curp.equals(idNo);
  }

  public void insertVerifyResult(VerifyType verifyType, RiskAccountVerifyStatus status, RiskAccountContextVO context) {
    // 一个设备最近的一条INIT记录, 有个case是0点前获取验证方式，0点后验证方式变了，这里也要check下
    List<VerifyType> needVerifyTypeList = checkCreateOrder(context);
    if (!needVerifyTypeList.contains(verifyType)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("系统请求超时，请重启app重试。"));
    }
    Condition condition = riskAccountVerifyLogModel.buildCondition(context.loanAccountId, context.deviceToken, null, null, verifyType, Collections.singletonList(RiskAccountVerifyStatus.INIT), null, null);
    RiskAccountVerifyLogRecord record = riskAccountVerifyLogModel.findLatestByCondition(condition);
    if (record == null) {
      throw EcException.error(EcExceptionType.COMMON_SERVER_ERROR, "Error in insert verify result, accountId: {}, deviceToken: {}, verifyType: {}", context.loanAccountId, context.deviceToken, verifyType);
    }
    RiskSceneType sceneType = RiskSceneType.fromCode(record.getRiskSceneType());
    LoanType loanType = LoanType.fromCharCode(record.getLoanType());
    RiskAccountVerifyInfoPojo pojo = new RiskAccountVerifyInfoPojo(context);
    String objectId = mongoRiskAccountVerifyInfoModel.insert(pojo);
    riskAccountVerifyLogModel.insert(context.loanAccountId, context.deviceToken, sceneType, loanType, verifyType, status, objectId);
    if (RiskAccountVerifyStatus.FAIL == status) {
      incrFailCache(context.sdkType, context.loanAccountId, verifyType);
    }
  }

  private void incrFailCache(SDKType sdkType, Long accountId, VerifyType verifyType) {
    Long expiredAt = Clock.getMaxMillisOfDay(Clock.now(), sdkType.getTimeZone()) / Clock.MILLS_PER_SECOND;
    riskAccountVerifyLoader.incrAndExpiredAtIfFirst(accountId, verifyType, expiredAt);
  }
}
