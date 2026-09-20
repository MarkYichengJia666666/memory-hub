package com.miyou.controllers.thirdparty;

import com.google.common.collect.Sets;
import com.miyou.controllers.thirdparty.request.BatchBlackListMockRequest;
import com.miyou.controllers.thirdparty.request.BatchCheckExistUserRequest;
import com.miyou.controllers.thirdparty.request.CheckRegisteredRequest;
import com.miyou.controllers.thirdparty.response.BatchBlackListMockResponse;
import com.miyou.controllers.thirdparty.response.BatchCheckExistUserResponse;
import com.miyou.controllers.thirdparty.response.CheckRegisteredResponse;
import com.miyou.utilities.ApiMarketingCollisionConverterUtils;
import com.miyou.utilities.SignCheck;
import com.yqg.core.service.apichannel.ApiMarketingCollisionLimitService;
import com.yqg.core.service.apichannel.ApiMarketingConfig;
import com.yqg.core.service.apichannel.blacklist.NotifBlacklistPushService;
import com.yqg.core.service.apichannel.user.ApiMarketingUserCheckResultService;
import com.yqg.core.service.checksign.CheckRegisteredMonitorService;
import com.yqg.core.service.loan.collision.vo.CheckSuccessUserVO;
import com.yqg.core.service.user.UserService;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/external")
@Slf4j
public class CheckRegisteredController {
  @Autowired
  private UserService userService;
  @Autowired
  private CheckRegisteredMonitorService checkRegisteredMonitorService;
  @Autowired
  private ApiMarketingUserCheckResultService apiUserCheckResultService;
  @Autowired
  private ApiMarketingConfig config;
  @Autowired
  private NotifBlacklistPushService notifBlacklistPushService;
  @Autowired
  private ApiMarketingCollisionLimitService apiMarketingCollisionLimitService;


  @SignCheck
  @PostMapping("checkHadRegistered")
  public Result checkHadRegistered(@RequestBody CheckRegisteredRequest request,
                                   @RequestParam("appId") String appId) {
    if (request.getNormalizedMobileNumbers().size() > 500) {
      log.warn("手机号超限制");
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM, TT.gen("参数错误"));
    }
    Set<String> registeredNormalizedMobileNumbers =
        userService.fetchByNormalizedMobileNumbersAndSdks(request.getNormalizedMobileNumbers(),
                Arrays.stream(SDKType.values()).collect(Collectors.toList()))
            .stream().map(item -> item.normalizedMobileNumber)
            .collect(Collectors.toSet());
    Set<String> allNormalizedMobileNumbers = request.getNormalizedMobileNumbers();
    allNormalizedMobileNumbers.removeAll(registeredNormalizedMobileNumbers);
    Set<String> unregisteredNormalizedMobileNumbers = Sets.newHashSet(allNormalizedMobileNumbers);
    checkRegisteredMonitorService.logCheckResultRequest(appId, registeredNormalizedMobileNumbers.size(), unregisteredNormalizedMobileNumbers.size());
    return EcResponseUtil.generate(CheckRegisteredResponse.builder()
        .registeredNormalizedMobileNumbers(registeredNormalizedMobileNumbers)
        .unregisteredNormalizedMobileNumbers(unregisteredNormalizedMobileNumbers)
        .build());
  }

  @SignCheck
  @PostMapping("/channel/batch/checkExistUser")
  public Result<BatchCheckExistUserResponse> channelBatchCheckExistUser(@RequestBody BatchCheckExistUserRequest request) {
    if (StringUtils.isBlank(request.getChannel())) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM, TT.gen("渠道不能为空"));
    }
    if (!config.getWhiteListChannel().contains(request.getChannel())) {
      log.info("not hit white list channel, channel:{}", request.getChannel());
      return EcResponseUtil.generate(BatchCheckExistUserResponse.emptyResponse());
    }
    if (CollectionUtils.isEmpty(request.getPhoneList()) || request.getPhoneList().size() > 50) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM, TT.gen("手机号列表不能为空且最多传递50个"));
    }
    // 渠道当日撞库配额门禁，超限整批拒绝
    if (!apiMarketingCollisionLimitService.tryAcquireDailyQuota(request.getChannel(), request.getPhoneList())) {
      throw EcException.warn(EcExceptionType.CHANNEL_DAILY_CHECK_TIMES_LIMIT_EXCEEDED, TT.gen("超过渠道每日撞库上限"));
    }

    List<CheckSuccessUserVO> checkSuccessUserResults = apiUserCheckResultService.batchProcessCheckExistUser(request.getPhoneList(), request.getChannel());

    // 是否命中撞库成功详情(注册成功手机号集合, 未注册手机号集合)渠道白名单
    boolean hitCollisionSuccessDetailChannelWhiteList = config.hitCollisionSuccessDetailChannelWhiteList(request.getChannel());

    return EcResponseUtil.generate(ApiMarketingCollisionConverterUtils.successResponse(checkSuccessUserResults, hitCollisionSuccessDetailChannelWhiteList));
  }

  @PostMapping("/channel/blackListMock")
  public Result channelBatchCheckExistUser(@RequestBody BatchBlackListMockRequest request) {
    if (!config.getChannelBlackListPwd().contains(request.getChannelPwd())) {
      log.info("not hit white list channel, channelPwd:{}", request.getChannelPwd());
      return EcResponseUtil.generate(BatchBlackListMockResponse.pwdNotMatchResponse());
    }
    if (request.getPhoneList() != null) {
      request.getPhoneList().forEach(md5 -> {
        log.info("blackListMock manual push, channelName: {}, mobileMd5: {}", request.getChannelName(), md5);
        notifBlacklistPushService.asyncPushToAllConfiguredChannels(md5);
      });
    }
    return EcResponseUtil.generateSuccess();
  }
}
