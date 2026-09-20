package com.yqg.core.service.apichannel.user;

import com.google.api.client.util.Lists;
import com.yqg.core.model.generated.tables.records.ApiChannelCheckMobileResultRecord;
import com.yqg.core.model.sql.apichannel.ApiChannelCheckMobileResultModel;
import com.yqg.core.model.sql.user.LoanUserEncryptInfoModel;
import com.yqg.core.service.apichannel.ApiChannelUserMonitorService;
import com.yqg.core.service.apichannel.ApiMarketingBlackListChecker;
import com.yqg.core.service.apichannel.ApiMarketingConfig;
import com.yqg.core.service.apichannel.IApiMarketingChecker;
import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import com.yqg.core.service.apichannel.enums.ApiMarketingCheckType;
import com.yqg.core.service.apichannel.enums.ApiMarketingUserType;
import com.yqg.core.service.apichannel.vo.ApiMarketingCheckContext;
import com.yqg.core.service.apichannel.vo.ApiMarketingUserHitResultVO;
import com.yqg.core.service.apichannel.vo.ApiMarketingUserVO;
import com.yqg.core.service.checksign.OpenApiSignConfigService;
import com.yqg.core.service.loan.collision.CheckExistUserLocker;
import com.yqg.core.service.loan.collision.vo.CheckFailedUserVO;
import com.yqg.core.service.loan.collision.vo.CheckSuccessUserVO;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.EcTimeZone;
import com.yqg.ec.common.utils.EcAsserts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author chenxianrui
 * @date 2025/2/5
 */
@Service
@Slf4j
public class ApiMarketingUserCheckResultService {

  private static final List<IApiMarketingChecker> CHECKER_LIST = new ArrayList<>();
  private static final Map<ApiMarketingCheckType, IApiMarketingChecker> CHECKER_MAP = new HashMap<>();
  private static final Map<String, ApiChannelUserType> CHANNEL_TO_USER_TYPE_MAP = new HashMap<>();
  private static final Map<ApiChannelUserType, String> USER_TYPE_TO_CHANNEL_MAP = new HashMap<>();
  private static final String UNKNOWN = "unknown";

  static {
    CHANNEL_TO_USER_TYPE_MAP.put("jzdl_yunhx", ApiChannelUserType.API_MARKETING_JZDL_YUNHX);
    CHANNEL_TO_USER_TYPE_MAP.put("jzdl_jinxt", ApiChannelUserType.API_MARKETING_JZDL_JINXT);
    CHANNEL_TO_USER_TYPE_MAP.put("jzdl_ruip", ApiChannelUserType.API_MARKETING_JZDL_RUIP);
    CHANNEL_TO_USER_TYPE_MAP.put("jzdl_tanz", ApiChannelUserType.API_MARKETING_JZDL_TANZ);
    CHANNEL_TO_USER_TYPE_MAP.put("SHT", ApiChannelUserType.API_MARKETING_SHT);
    CHANNEL_TO_USER_TYPE_MAP.put("jzdl_tast", ApiChannelUserType.API_MARKETING_JZDL_TAST);
    CHANNEL_TO_USER_TYPE_MAP.put("jzdl_jish", ApiChannelUserType.API_MARKETING_JZDL_JISH);
    CHANNEL_TO_USER_TYPE_MAP.put("jzdl_keda", ApiChannelUserType.API_MARKETING_JZDL_KEDA);

    USER_TYPE_TO_CHANNEL_MAP.put(ApiChannelUserType.API_MARKETING_JZDL_YUNHX, "jzdl_yunhx");
    USER_TYPE_TO_CHANNEL_MAP.put(ApiChannelUserType.API_MARKETING_JZDL_JINXT, "jzdl_jinxt");
    USER_TYPE_TO_CHANNEL_MAP.put(ApiChannelUserType.API_MARKETING_JZDL_RUIP, "jzdl_ruip");
    USER_TYPE_TO_CHANNEL_MAP.put(ApiChannelUserType.API_MARKETING_JZDL_TANZ, "jzdl_tanz");
    USER_TYPE_TO_CHANNEL_MAP.put(ApiChannelUserType.API_MARKETING_SHT, "SHT");
    USER_TYPE_TO_CHANNEL_MAP.put(ApiChannelUserType.API_MARKETING_JZDL_TAST, "jzdl_tast");
    USER_TYPE_TO_CHANNEL_MAP.put(ApiChannelUserType.API_MARKETING_JZDL_JISH, "jzdl_jish");
    USER_TYPE_TO_CHANNEL_MAP.put(ApiChannelUserType.API_MARKETING_JZDL_KEDA, "jzdl_keda");
  }

  @Autowired
  private ApiChannelCheckMobileResultModel apiChannelCheckMobileResultModel;
  @Autowired
  private LoanUserEncryptInfoModel loanUserEncryptInfoModel;
  @Autowired
  private OpenApiSignConfigService openApiSignConfigService;
  @Autowired
  private CheckExistUserLocker checkExistUserLocker;
  @Autowired
  private List<IApiMarketingChecker> checkerList;
  @Autowired
  private ApiMarketingConfig apiMarketingConfig;
  @Autowired
  private ApiChannelUserMonitorService apiChannelUserMonitorService;

  @PostConstruct
  public void post() {
    Map<ApiMarketingCheckType, IApiMarketingChecker> map = checkerList
        .stream()
        .collect(Collectors.toMap(IApiMarketingChecker::getType, Function.identity()));
    // 构建默认列表
    for (ApiMarketingCheckType type : ApiMarketingCheckType.getCheckTypeOrder()) {
      IApiMarketingChecker iApiMarketingChecker = map.get(type);
      EcAsserts.assertNotNull(iApiMarketingChecker, "checker not found for type:{}", type);
      CHECKER_LIST.add(iApiMarketingChecker);
    }
    // 保存成MAP以便按类型快速取checker
    CHECKER_MAP.putAll(map);
  }

  private List<IApiMarketingChecker> getCheckerListByChannel(String apiChannel) {
    if (StringUtils.isBlank(apiChannel)) {
      return CHECKER_LIST;
    }
    List<ApiMarketingCheckType> typeOrder = apiMarketingConfig.getCheckerTypeOrderByChannel(apiChannel);
    if (CollectionUtils.isEmpty(typeOrder)) {
      return Collections.emptyList();
    }
    List<IApiMarketingChecker> list = Lists.newArrayList();
    for (ApiMarketingCheckType type : typeOrder) {
      IApiMarketingChecker checker = CHECKER_MAP.get(type);
      EcAsserts.assertNotNull(checker, "checker not found for type:{}", type);
      list.add(checker);
    }
    return list;
  }

  public List<CheckSuccessUserVO> batchProcessCheckExistUser(Set<String> phoneMd5List, String apiChannel) {
    return batchCheck(phoneMd5List, apiChannel);
  }

  private Boolean canCheck(String phoneMd5) {
    int apiChannelCollisionDay = openApiSignConfigService.getApiChannelCollisionDay();
    int apiChannelCollisionLimitCount = openApiSignConfigService.getApiChannelCollisionLimitCount();
    long cutoffTime = Clock.plusOffsetDaysMills(Clock.now(), EcTimeZone.DEFAULT_TIMEZONE.tz, -apiChannelCollisionDay);
    List<ApiChannelCheckMobileResultRecord> successUserCheckResultList = apiChannelCheckMobileResultModel.findListByMobileNumberMd5AndCreateTimeAndChannelAndResult(
        phoneMd5, cutoffTime);
    return CollectionUtils.isEmpty(successUserCheckResultList) || successUserCheckResultList.size() < apiChannelCollisionLimitCount;
  }

  public List<CheckSuccessUserVO> batchCheck(Set<String> phoneMd5List, String apiChannel) {
    ApiMarketingUserHitResultVO apiUserHitResultVO = preProcessMd5Phone(phoneMd5List, apiChannel);
    ApiMarketingCheckContext context = ApiMarketingCheckContext.init(apiUserHitResultVO.getUndefinedUserIds(), apiChannel);
    for (IApiMarketingChecker iApiMarketingChecker : getCheckerListByChannel(apiChannel)) {
      apiUserHitResultVO = iApiMarketingChecker.checkWithEffectiveSwitch(apiUserHitResultVO, context);
    }
    return postProcessMd5Result(apiUserHitResultVO, apiChannel);
  }

  /**
   * @param phoneMd5List
   * @param apiChannel
   * @return com.yqg.core.service.apichannel.vo.ApiMarketingUserHitResultVO 预处理手机号码，如果是注册用户，需要获取userId再进一步处理。
   */
  private ApiMarketingUserHitResultVO preProcessMd5Phone(Set<String> phoneMd5List, String apiChannel) {
    Map<String, Long> maps = loanUserEncryptInfoModel.findLatestEcUserIdMapByMobileMd5s(phoneMd5List);
    Set<ApiMarketingUserVO> registeredUserSet = new HashSet<>();
    Set<CheckSuccessUserVO> hitPhoneSet = new HashSet<>();
    phoneMd5List.forEach(phoneMd5 -> {
      if (maps.containsKey(phoneMd5)) {
        registeredUserSet.add(ApiMarketingUserVO.from(maps.get(phoneMd5), phoneMd5));
        return;
      }
      hitPhoneSet.add(CheckSuccessUserVO.from(phoneMd5, apiChannel, ApiMarketingUserType.NOT_REGISTER.name()));
    });
    return ApiMarketingUserHitResultVO.init(registeredUserSet, hitPhoneSet);
  }

  private List<CheckSuccessUserVO> postProcessMd5Result(ApiMarketingUserHitResultVO apiUserHitResultVO, String apiChannel) {
    //所有未被拦截的用户，均设置成未命中撞库的用户
    apiUserHitResultVO.setAllUndefinedToNotHit(apiChannel);
    EcAsserts.assertTrue(CollectionUtils.isEmpty(apiUserHitResultVO.undefinedUserVOs), "undefinedUserVOs should be empty");
    postProcessNotHitResult(apiUserHitResultVO, apiChannel);
    return postProcessHitResult(apiUserHitResultVO, apiChannel);
  }

  @NotNull
  //判断命中的用户是否触发了限流规则
  private List<CheckSuccessUserVO> postProcessHitResult(ApiMarketingUserHitResultVO apiUserHitResultVO, String apiChannel) {
    List<CheckSuccessUserVO> hitPhoneList = Lists.newArrayList();
    for (CheckSuccessUserVO checkExistUserVO : apiUserHitResultVO.hitPhoneSet) {
      checkExistUserLocker.lockAndRun(CheckExistUserLocker.getLockKey(checkExistUserVO.phoneMd5), () -> {
        if (!canCheck(checkExistUserVO.phoneMd5)) {
          DwLogUtil.newLog(LogBusinessType.CHECK_EXIST_USER_FAILED_LOG,
              CheckFailedUserVO.from(checkExistUserVO.phoneMd5, apiChannel, "已分案锁单"));
          return;
        }
        apiChannelCheckMobileResultModel.insert(checkExistUserVO.phoneMd5, apiChannel, checkExistUserVO.reason, checkExistUserVO.lossDays);
        hitPhoneList.add(checkExistUserVO);
        apiChannelUserMonitorService.logApiMarketingCollision(apiChannel, checkExistUserVO.phoneMd5, true);
      });
    }
    return hitPhoneList;
  }

  private void postProcessNotHitResult(ApiMarketingUserHitResultVO apiUserHitResultVO, String apiChannel) {
    for (CheckFailedUserVO checkExistUserVO : apiUserHitResultVO.notHitPhoneSet) {
      if (ApiMarketingBlackListChecker.BLACK_LIST_REASON.equals(checkExistUserVO.failureReason)) {
        log.info("channelBatchCheckExistUser user in blacklist, phoneMd5:{}, channel:{}", checkExistUserVO.phoneMd5, apiChannel);
      }
      DwLogUtil.newLog(LogBusinessType.CHECK_EXIST_USER_FAILED_LOG, checkExistUserVO);
    }
  }


  public String parseUserTypeToChannel(ApiChannelUserType userType) {
    if (userType == null) {
      return UNKNOWN;
    }
    String channel = USER_TYPE_TO_CHANNEL_MAP.getOrDefault(userType, UNKNOWN);
    return apiMarketingConfig.getWhiteListChannel().contains(channel) ? channel : UNKNOWN;
  }

}
