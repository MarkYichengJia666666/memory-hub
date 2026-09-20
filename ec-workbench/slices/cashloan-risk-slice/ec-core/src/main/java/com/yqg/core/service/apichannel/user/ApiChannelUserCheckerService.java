package com.yqg.core.service.apichannel.user;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.yqg.core.model.generated.tables.records.ApiChannelUserRecord;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.LoanUserEncryptInfoRecord;
import com.yqg.core.model.sql.apichannel.ApiChannelCheckUserResultModel;
import com.yqg.core.model.sql.apichannel.ApiChannelUserModel;
import com.yqg.core.model.sql.apichannel.enums.ApiChannel;
import com.yqg.core.model.sql.apichannel.enums.CheckUserResult;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.service.apichannel.ApiChannelUserMergeFilterService;
import com.yqg.core.service.apichannel.ApiChannelUserMonitorService;
import com.yqg.core.service.apichannel.collision.enums.CollisionProcessEnum;
import com.yqg.core.service.apichannel.collision.orchestrator.CollisionOrchestrator;
import com.yqg.core.service.apichannel.enums.ApiChannelCheckResultType;
import com.yqg.core.service.apichannel.enums.ApiChannelUserRuleType;
import com.yqg.core.service.apichannel.enums.ApiChannelUserStatusCheckerType;
import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import com.yqg.core.service.apichannel.util.ApiChannelCheckUserResultConverter;
import com.yqg.core.service.apichannel.vo.ApiChannelCheckUserResultVO;
import com.yqg.core.service.apichannel.vo.ApiChannelCloseAccountVO;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckRuleResultVO;
import com.yqg.core.service.loan.vo.ApiChannelUserMergeVO;
import com.yqg.core.service.user.LoanUserEncryptInfoService;
import com.yqg.core.service.user.UserDeleteService;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.UserSimpleInfoVO;
import com.yqg.core.util.Codec;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import com.yqg.ec.common.i18n.time.Clock;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ApiChannelUserCheckerService {

  private static final String USER_TABLE_CHANNEL_CODE = "EC";
  private static final List<IApiUserRuleChecker> COMMON_CHECKERS = Lists.newArrayList();
  private static final List<IApiUserRuleChecker> EASYCASH_CHECKERS = Lists.newArrayList();
  private static final List<IApiUserRuleChecker> API_CHANNEL_CHECKERS = Lists.newArrayList();
  private static final Map<ApiChannelUserStatusCheckerType, List<IApiUserRuleChecker>> CHECKER_TYPE_TO_CHECKERS = Maps.newHashMap();

  private static final Map<ApiChannelUserRuleType, IApiUserRuleChecker> ruleType2CheckerMap = new HashMap<>();
  @Autowired
  private LoanUserEncryptInfoService encryptInfoService;
  @Autowired
  private LoanAccountModel accountModel;
  @Autowired
  private List<IApiUserRuleChecker> apiUserRuleCheckers;
  @Autowired
  private ApiChannelCheckUserResultModel apiChannelCheckUserResultModel;
  @Autowired
  private ApiChannelUserMonitorService monitorService;
  @Autowired
  private UserService userService;
  @Autowired
  private UserDeleteService userDeleteService;
  @Autowired
  private ApiChannelUserModel apiChannelUserModel;
  @Autowired
  private CollisionOrchestrator collisionOrchestrator;
  @Autowired
  private ApiChannelUserMergeFilterService mergeFilterService;

  @PostConstruct
  public void init() {
    buildRuleType2CheckerMap();
    buildCheckersToList(COMMON_CHECKERS, ApiChannelUserRuleType.COMMON_RULE_CHECKERS);
    buildCheckersToList(EASYCASH_CHECKERS, ApiChannelUserRuleType.EASY_CASH_RULE_CHECKERS);
    buildCheckersToList(API_CHANNEL_CHECKERS, ApiChannelUserRuleType.API_CHANNEL_RULE_CHECKERS);
    buildCheckersToMap(ApiChannelUserStatusCheckerType.INDOSAT_CHECK_USER_STATUS,
        ApiChannelUserRuleType.INDOSAT_CHECK_USER_STATUS_RULE_CHECKERS);
    buildCheckersToMap(ApiChannelUserStatusCheckerType.CAN_CREATE_ORDER, ApiChannelUserRuleType.API_CHANNEL_CAN_CREATE_ORDER_CHECKERS);
  }

  private void buildRuleType2CheckerMap() {
    for (IApiUserRuleChecker apiUserRuleChecker : apiUserRuleCheckers) {
      ruleType2CheckerMap.put(apiUserRuleChecker.getRuleType(), apiUserRuleChecker);
    }
  }


  private void buildCheckersToList(List<IApiUserRuleChecker> targetList, List<ApiChannelUserRuleType> sourceRuleTypeList) {
    for (ApiChannelUserRuleType apiChannelUserRuleType : sourceRuleTypeList) {
      IApiUserRuleChecker checker = ruleType2CheckerMap.get(apiChannelUserRuleType);
      if (checker == null) {
        throw EcException.error("can not find checker type: {}", apiChannelUserRuleType);
      }
      targetList.add(checker);
    }
  }

  private void buildCheckersToMap(ApiChannelUserStatusCheckerType targetKeyType, List<ApiChannelUserRuleType> sourceRuleTypeList) {
    List<IApiUserRuleChecker> resultList = Lists.newArrayList();
    buildCheckersToList(resultList, sourceRuleTypeList);
    CHECKER_TYPE_TO_CHECKERS.put(targetKeyType, resultList);
  }


  public ApiChannelUserType checkApiChannelUserType(String mobileNumberMd5, String identityNumberMd5, String channelCode) {
    ApiChannel channel = ApiChannel.from(channelCode);
    ApiChannelUserType userType = checkApiChannelUserType(mobileNumberMd5, identityNumberMd5, channel, channel.sdkType);
    log.info("api channel check user result:{}, user mobileNumberMd5:{}, channel:{}", userType, mobileNumberMd5, channel);
    return userType;
  }

  public ApiChannelUserType checkApiChannelUserType(String mobileNumberMd5, String identityNumberMd5, ApiChannel channel) {
    ApiChannelUserType userType = checkApiChannelUserType(mobileNumberMd5, identityNumberMd5, channel, channel.sdkType);
    log.info("api channel check user result:{}, user mobileNumberMd5:{}, channel:{}", userType, mobileNumberMd5, channel);
    return userType;
  }

  public ApiChannelUserType checkApiChannelUserType(String mobileNumberMd5, String identityNumberMd5, ApiChannel channel, SDKType sdkType) {
    ApiChannelUserCheckConditionVO conditionVO = buildApiChannelUserCondition(mobileNumberMd5, identityNumberMd5, channel, sdkType);
    return checkApiUserType(conditionVO);
  }

  public CheckUserResultVO checkApiChannelUserType(String mobileNumberMd5, String identityNumberMd5, ApiChannel channel, SDKType sdkType,
                                                   ApiChannelUserStatusCheckerType checkerType) {
    ApiChannelUserCheckConditionVO conditionVO = buildApiChannelUserCondition(mobileNumberMd5, identityNumberMd5, channel, sdkType,
        checkerType);
    ApiChannelUserType userType = checkApiUserType(conditionVO);
    CheckUserResultVO resultVO = new CheckUserResultVO(conditionVO.getUserId(), userType);
    resultVO.setUpgradeFlow(conditionVO.getUpgradeFlow());
    return resultVO;
  }

  public void saveAndLogCheckUserResult(ApiChannelCheckResultType apiChannelCheckResultType, String mobileNumberMd5,
                                        String identityNumberMd5, ApiChannelUserType userType, ApiChannel channel, CheckUserResult result, Long userId, Boolean upgradeFlow) {
    insertCheckUserResult(mobileNumberMd5, identityNumberMd5, channel.sourceType, apiChannelCheckResultType, userType,
        result, userId);
    monitorService.logCheckUserResult(channel, apiChannelCheckResultType, result.code, mobileNumberMd5, identityNumberMd5, userType, upgradeFlow);
  }

  /**
   * 保存撞库结果并记录监控，新流程额外写入 channel_user_group
   *
   * @param channelUserGroup 用户分组枚举名，可为 null（旧流程或 MOCK_APPROVE 不写）
   */
  public void saveAndLogCheckUserResult(ApiChannelCheckResultType apiChannelCheckResultType, String mobileNumberMd5,
                                        String identityNumberMd5, ApiChannelUserType userType, ApiChannel channel,
                                        CheckUserResult result, Long userId, String channelUserGroup, Boolean upgradeFlow) {
    apiChannelCheckUserResultModel.insert(mobileNumberMd5, identityNumberMd5, channel.sourceType,
        apiChannelCheckResultType, userType, result, userId, channelUserGroup);
    monitorService.logCheckUserResult(channel, apiChannelCheckResultType, result.code, mobileNumberMd5, identityNumberMd5, userType, upgradeFlow);
  }

  private List<IApiUserRuleChecker> getCheckerList(ApiChannelUserCheckConditionVO conditionVO) {
    List<IApiUserRuleChecker> checkers = Lists.newArrayList();

    if (conditionVO.getCheckerType() != null) {
      List<IApiUserRuleChecker> checkerFromMap = CHECKER_TYPE_TO_CHECKERS.get(conditionVO.getCheckerType());
      if (CollectionUtils.isEmpty(checkerFromMap)) {
        throw EcException.error("can not find checker type: {}", conditionVO.getCheckerType());
      }
      return checkerFromMap;
    }

    if (BooleanUtils.isTrue(conditionVO.getEasyCashChannel())) {
      checkers.addAll(COMMON_CHECKERS);
      checkers.addAll(EASYCASH_CHECKERS);
      return checkers;
    }
    checkers.addAll(COMMON_CHECKERS);
    checkers.addAll(API_CHANNEL_CHECKERS);
    return checkers;
  }

  private ApiChannelUserType checkApiUserType(ApiChannelUserCheckConditionVO condition) {
    if (CollisionProcessEnum.isSupported(condition.getCheckerType())) {
      return collisionOrchestrator.execute(condition);
    }
    return checkApiUserTypeLegacy(condition);
  }

  private ApiChannelUserType checkApiUserTypeLegacy(ApiChannelUserCheckConditionVO condition) {
    for (IApiUserRuleChecker checker : getCheckerList(condition)) {
      ApiChannelUserCheckRuleResultVO ruleCheckResultVO = checker.hitRule(condition);
      if (BooleanUtils.isTrue(ruleCheckResultVO.getHitRule())) {
        return ruleCheckResultVO.getApiChannelUserType();
      }
    }
    return ApiChannelUserType.REJECT;
  }

  private ApiChannelUserCheckConditionVO buildApiChannelUserCondition(String mobileNumberMd5, String identityNumberMd5, ApiChannel channel,
                                                                      SDKType sdkType, ApiChannelUserStatusCheckerType checkerType) {
    ApiChannelUserCheckConditionVO conditionVO = new ApiChannelUserCheckConditionVO(mobileNumberMd5, identityNumberMd5, channel, sdkType,
        checkerType);
    return buildApiChannelUserCondition(conditionVO);
  }

  // 这个方法没用nik去做查询
  private ApiChannelUserCheckConditionVO buildApiChannelUserCondition(String mobileNumberMd5, String identityNumberMd5, ApiChannel channel, SDKType sdkType) {
    ApiChannelUserCheckConditionVO conditionVO = new ApiChannelUserCheckConditionVO(mobileNumberMd5, identityNumberMd5, channel, sdkType);
    return buildApiChannelUserCondition(conditionVO);
  }

  private ApiChannelUserCheckConditionVO buildApiChannelUserCondition(ApiChannelUserCheckConditionVO conditionVO) {
    conditionVO.setCurrentTimeMillis(Clock.now());
    String mobileNumberMd5 = conditionVO.getMobileNumberMd5();
    String identityNumberMd5 = conditionVO.getIdentityNumberMd5();
    List<LoanUserEncryptInfoRecord> userEncryptInfoRecords = encryptInfoService.fetchByMobileMd5(mobileNumberMd5);
    // 手机号无匹配时尝试 NIK 回退，结果存入 nikFallback 字段供决策树使用
    if (CollectionUtils.isEmpty(userEncryptInfoRecords) && StringUtils.isNotBlank(identityNumberMd5)) {
      enrichNikFallback(conditionVO, identityNumberMd5);
    }
    conditionVO.setEmptyMobileMd5(CollectionUtils.isEmpty(userEncryptInfoRecords));
    if (CollectionUtils.isEmpty(userEncryptInfoRecords)) {
      return conditionVO;
    }
    LoanUserEncryptInfoRecord loanUserEncryptInfoRecord = getLoanUserEncryptInfoRecord(mobileNumberMd5, identityNumberMd5, userEncryptInfoRecords);
    Long userId = loanUserEncryptInfoRecord.getUserId();
    conditionVO.setUserId(userId);
    conditionVO.setLoanUserEncryptInfoRecord(loanUserEncryptInfoRecord);

    UserSimpleInfoVO userSimpleInfoVO = userService.findById(userId);
    conditionVO.setEasyCashChannel(userSimpleInfoVO.sourceType.isEasyCashSourceType());
    LoanAccountRecord loanAccountRecord = accountModel.findByUserId(userId);
    conditionVO.setAccountId(Objects.nonNull(loanAccountRecord) ? loanAccountRecord.getId() : null);
    return conditionVO;
  }

  /**
   * 手机号无匹配时通过 NIK MD5 回退查找可用于撞库的 userId，
   * 结果写入 conditionVO 的 nikFallback 字段，不改变主字段（userId/accountId/emptyMobileMd5）。
   */
  private void enrichNikFallback(ApiChannelUserCheckConditionVO conditionVO, String identityNumberMd5) {
    List<LoanUserEncryptInfoRecord> nikRecords = encryptInfoService.fetchByIdentityMd5(identityNumberMd5);
    if (CollectionUtils.isEmpty(nikRecords)) {
      return;
    }
    List<Long> nikUserIds = nikRecords.stream()
        .map(LoanUserEncryptInfoRecord::getUserId)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
    if (nikUserIds.isEmpty()) {
      return;
    }
    Map<Long, Long> userIdToAccountId = accountModel.fetchUserIdAndLoanAccountIdMapByUserIds(nikUserIds);
    List<Long> accountIds = nikUserIds.stream()
        .map(userIdToAccountId::get)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());

    ApiChannelUserMergeVO mergeResult = mergeFilterService.filterMergeUserForApiChannelRegister(accountIds);
    Long collisionUserId = mergeResult.getCollisionUserId();
    if (collisionUserId != null) {
      conditionVO.setNikFallbackUserId(collisionUserId);
      conditionVO.setNikFallbackAccountId(userIdToAccountId.get(collisionUserId));
      conditionVO.setMatchedByNik(true);
    }
  }

  public LoanUserEncryptInfoRecord getLoanUserEncryptInfoRecord(String mobileNumberMd5, String identityNumberMd5, List<LoanUserEncryptInfoRecord> userEncryptInfoRecords) {
    return filterCurrentValidUserEncryptInfo(mobileNumberMd5, identityNumberMd5, userEncryptInfoRecords);
  }

  public LoanUserEncryptInfoRecord filterCurrentValidUserEncryptInfo(String mobileNumberMd5, String identityNumberMd5,
                                                                     List<LoanUserEncryptInfoRecord> userEncryptInfoRecords) {
    if (CollectionUtils.isEmpty(userEncryptInfoRecords)) {
      throw EcException.error("no userEncryptInfoRecord found. mobileNumberMd5 is {}, identityNumberMd5 is {}", mobileNumberMd5,
          identityNumberMd5);
    }
    List<LoanUserEncryptInfoRecord> ecRecords = userEncryptInfoRecords.stream()
        .filter(encryptedInfoRecord -> StringUtils.equalsIgnoreCase(encryptedInfoRecord.getChannelCode(), USER_TABLE_CHANNEL_CODE))
        .collect(Collectors.toList());
    LoanUserEncryptInfoRecord matchedEcRecord = findMatchedRecordByUserTableMobile(ecRecords);
    if (matchedEcRecord != null) {
      return matchedEcRecord;
    }

    List<LoanUserEncryptInfoRecord> apiChannelRecords = userEncryptInfoRecords.stream()
        .filter(encryptInfoRecord -> !StringUtils.equalsIgnoreCase(encryptInfoRecord.getChannelCode(), USER_TABLE_CHANNEL_CODE))
        .collect(Collectors.toList());
    LoanUserEncryptInfoRecord matchedApiChannelRecord = findMatchedRecordByApiChannelUserMobile(apiChannelRecords);
    if (matchedApiChannelRecord != null) {
      return matchedApiChannelRecord;
    }

    throw EcException.error("no valid userEncryptInfoRecord found. mobileNumberMd5 is {}, identityNumberMd5 is {}", mobileNumberMd5,
        identityNumberMd5);
  }

  private LoanUserEncryptInfoRecord findMatchedRecordByUserTableMobile(List<LoanUserEncryptInfoRecord> encryptInfoRecords) {
    if (CollectionUtils.isEmpty(encryptInfoRecords)) {
      return null;
    }
    List<Long> userIdList = encryptInfoRecords.stream().map(LoanUserEncryptInfoRecord::getUserId).distinct().collect(Collectors.toList());
    Map<Long, UserSimpleInfoVO> userIdToSimpleInfoMap = userService.findMapByIds(userIdList);
    for (LoanUserEncryptInfoRecord encryptInfoRecord : encryptInfoRecords) {
      UserSimpleInfoVO userSimpleInfoVO = userIdToSimpleInfoMap.get(encryptInfoRecord.getUserId());
      if (isMobileMd5Matched(encryptInfoRecord, userSimpleInfoVO == null ? null : userSimpleInfoVO.normalizedMobileNumber)) {
        return encryptInfoRecord;
      }
    }
    return null;
  }

  private LoanUserEncryptInfoRecord findMatchedRecordByApiChannelUserMobile(List<LoanUserEncryptInfoRecord> encryptInfoRecords) {
    if (CollectionUtils.isEmpty(encryptInfoRecords)) {
      return null;
    }
    List<Long> userIdList = encryptInfoRecords.stream().map(LoanUserEncryptInfoRecord::getUserId).distinct().collect(Collectors.toList());
    Map<String, List<ApiChannelUserRecord>> channelCodeToApiChannelUsersMap = apiChannelUserModel.fetchByUserIds(userIdList)
        .stream()
        .filter(apiChannelUserRecord -> StringUtils.isNotBlank(apiChannelUserRecord.getChannel()))
        .collect(Collectors.groupingBy(apiChannelUserRecord -> StringUtils.upperCase(apiChannelUserRecord.getChannel())));
    for (LoanUserEncryptInfoRecord encryptInfoRecord : encryptInfoRecords) {
      String channelCode = encryptInfoRecord.getChannelCode();
      if (StringUtils.isBlank(channelCode)) {
        continue;
      }
      List<ApiChannelUserRecord> apiChannelUserRecords = channelCodeToApiChannelUsersMap.get(StringUtils.upperCase(channelCode));
      if (CollectionUtils.isEmpty(apiChannelUserRecords)) {
        continue;
      }
      for (ApiChannelUserRecord apiChannelUserRecord : apiChannelUserRecords) {
        if (!Objects.equals(encryptInfoRecord.getUserId(), apiChannelUserRecord.getUserId())) {
          continue;
        }
        if (isMobileMd5Matched(encryptInfoRecord, apiChannelUserRecord.getNormalizedMobileNumber())) {
          return encryptInfoRecord;
        }
      }
    }
    return null;
  }

  private boolean isMobileMd5Matched(LoanUserEncryptInfoRecord encryptInfoRecord, String normalizedMobileNumber) {
    if (StringUtils.isBlank(normalizedMobileNumber)) {
      return false;
    }
    String nationalMobile = MobileConverter.normalizedToNationalOrNull(SDKType.IDN_YQD.getLocale(), normalizedMobileNumber);
    if (nationalMobile == null) {
      return false;
    }
    String phoneNumberMD5 = Codec.md5Encrypt(nationalMobile);
    return StringUtils.equalsIgnoreCase(phoneNumberMD5, encryptInfoRecord.getNationalMobileMd5());
  }

  private void insertCheckUserResult(String mobileNumberMd5, String identityNumberMd5, SourceType sourceType,
                                     ApiChannelCheckResultType apiChannelCheckResultType, ApiChannelUserType channelUserType, CheckUserResult result, Long userId) {
    apiChannelCheckUserResultModel.insert(mobileNumberMd5, identityNumberMd5, sourceType, apiChannelCheckResultType, channelUserType,
        result, userId);
  }

  public ApiChannelCheckUserResultVO findLatestByMobileMD5AndSourceTypeCheckType(String mobileNumberMD5, SourceType sourceType, ApiChannelCheckResultType checkType) {
    return ApiChannelCheckUserResultConverter.convertToApiChannelCheckUserResultVO(
        apiChannelCheckUserResultModel.findLatestByMobileMD5AndSourceTypeCheckType(mobileNumberMD5, sourceType, checkType));
  }

  public List<ApiChannelCheckUserResultVO> findListByMobileMD5AndSourceTypeCheckType(String mobileNumberMD5, SourceType sourceType, ApiChannelCheckResultType checkType) {
    return ApiChannelCheckUserResultConverter.convertToApiChannelCheckUserResultVOList(
        apiChannelCheckUserResultModel.findListByMobileMD5AndSourceTypeCheckType(mobileNumberMD5, sourceType, checkType));
  }

  public ApiChannelCloseAccountVO preCheckCloseAccount(@NotNull Long userId, SDKType sdkType, ApiChannel apiChannel) {
    return userDeleteService.preCheckCloseAccountForApiChannel(userId, sdkType, apiChannel);
  }

  public ApiChannelCloseAccountVO closeAccount(@NotNull Long userId, SDKType sdkType, ApiChannel apiChannel) {
    return userDeleteService.closeAccountForApiChannel(userId, sdkType, apiChannel);
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class CheckUserResultVO {

    private Long userId;

    private ApiChannelUserType userType;

    /** 标识当前结果是否来自新决策树流程，用于 checker 路由到独立的 convertStatusForUpgrade */
    private Boolean upgradeFlow;

    public CheckUserResultVO(Long userId, ApiChannelUserType userType) {
      this.userId = userId;
      this.userType = userType;
    }
  }
}
