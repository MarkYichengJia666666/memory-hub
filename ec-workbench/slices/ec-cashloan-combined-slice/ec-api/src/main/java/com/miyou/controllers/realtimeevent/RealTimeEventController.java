package com.miyou.controllers.realtimeevent;

import com.miyou.controllers.advertisement.OpenAppAdRequest;
import com.miyou.controllers.core.YqgBaseController;
import com.miyou.controllers.realtimeevent.request.RealTimeEventOpenAppRequest;
import com.miyou.utilities.secureapi.RequestParser;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.loader.OpenAppFirstCreateIdempotentLoader;
import com.yqg.core.model.sql.risk.enums.RiskOutputType;
import com.yqg.core.model.sql.user.LoginStatusCacheModel;
import com.yqg.core.service.advertisement.AdvertisementService;
import com.yqg.core.service.advertisement.attribution.vo.AdRawDataEventVO;
import com.yqg.core.service.advertisement.attribution.vo.AdRealTimeEventVO;
import com.yqg.core.service.advertisement.enums.AdPlatform;
import com.yqg.core.service.advertisement.monitor.FirebaseAppsflyerIdMonitor;
import com.yqg.core.service.app.AppStartupService;
import com.yqg.core.service.pointgrowth.admission.PointEligibilityEvaluation;
import com.yqg.core.service.pointgrowth.admission.PointEligibilityService;
import com.yqg.core.userflow.domain.point.model.PointScene;
import com.yqg.core.userflow.infrastructure.adapter.IPointAdapter;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.firebase.FirebaseAnalyticsLogService;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.coupon.openappgrantcoupon.OpenAppGrantCouponService;
import com.yqg.core.service.loan.coupon.openappgrantcoupon.vo.GrantCouponRequest;
import com.yqg.core.service.loan.creditsdetails.CreditLoanStatus;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.mc.MarketingCenterClientService;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.notification.param.system.AppActivationParam;
import com.yqg.core.service.notification.param.system.OpenAppParam;
import com.yqg.core.service.realtimeevent.enums.OpenAppScene;
import com.yqg.core.service.risk.usergroup.RiskUserGroupEntranceService;
import com.yqg.core.service.risk.usergroup.vo.LoanRiskUserGroupVO;
import com.yqg.core.service.sourcetype.SourceTypeService;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.enums.AccessType;
import com.yqg.core.service.user.vo.AdjustInfo;
import com.yqg.core.service.user.vo.LoginStatusCacheVO;
import com.yqg.core.util.env.EnvironmentInfo;
import com.yqg.core.util.env.TerminalInfo;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.whatopia.common.util.Clock;
import com.yqg.whatopia.common.util.SnowFlakeUtils;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chaoye
 * @date 2024/4/25
 */
@Slf4j
@RestController
public class RealTimeEventController extends YqgBaseController {
  @Autowired
  private MarketingCenterClientService marketingCenterClientService;
  @Autowired
  private UserService userService;
  @Autowired
  private AdvertisementService advertisementService;
  @Autowired
  private AppStartupService appStartupService;
  @Autowired
  private FirebaseAnalyticsLogService firebaseAnalyticsLogService;
  @Autowired
  private FirebaseAppsflyerIdMonitor firebaseAppsflyerIdMonitor;
  @Autowired
  private LoginStatusCacheModel loginStatusCacheModel;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private OpenAppGrantCouponService openAppGrantCouponService;
  @Autowired
  private OpenAppFirstCreateIdempotentLoader openAppFirstCreateIdempotentLoader;
  @Autowired
  private PointEligibilityService pointEligibilityService;
  @Autowired
  private IPointAdapter pointAdapter;


  @PostMapping("/api/realTimeEvent/openApp")
  public Result openApp(@RequestBody @Valid RealTimeEventOpenAppRequest request) {
    Long businessId = SnowFlakeUtils.getId();
    String deviceToken = RequestParser.getDeviceToken(ecRequest());
    boolean idempotentAcquired = false;
    try {
      // 首启 CREATE + 前端重试：同设备 30min 内只执行一次完整副作用；Redis 异常时降级放行
      if (request.openAppScene == OpenAppScene.CREATE
          && request.openTimes != null
          && request.openTimes == 1L
          && StringUtils.isNotBlank(deviceToken)) {
        if (!tryAcquireIdempotent(deviceToken, request.openTimes, request.openAppScene)) {
          return EcResponseUtil.generateSuccess();
        }
        idempotentAcquired = true;
      }
      Long userId = getUserIdByToken();
      RequestClientType requestClientType = ImpliedContextUtils.requestClientType();
      appStartupService.processAppStartupEvent(userId, requestClientType, request.openAppPage);
      //sdk需要使用request中的pageSdkType，因为android框架问题，启动页都是归属借贷，导致刚启动的启动页从header获取都是借贷的
      SystemNotifScene systemNotifScene = userId == null ? SystemNotifScene.OPEN_APP_WITHOUT_LOIN : SystemNotifScene.OPEN_APP_WITH_LOGIN;
      Long build = RequestParser.getBuild(ecRequest());
      OpenAppParam openAppParam = new OpenAppParam(userId, request.pageSdkType, deviceToken, request.openAppScene, request.openAppPage, businessId, build);
      if (userId != null && !userService.checkUserDeleted(userId)) {
        Long loanAccountId = loanAccountService.getAccountIdByUserId(userId, request.pageSdkType);
        try {
          PointEligibilityEvaluation evaluation =
              pointEligibilityService.evaluate(userId, requestClientType, request.pageSdkType, loanAccountId, build);
          if (evaluation.isEligible()) {
            pointAdapter.initAdmission(userId, PointScene.OPEN_APP.getCode());
          }
        } catch (Exception e) {
          log.error("point growth admission on openApp failed, userId={}", userId, e);
        }
        if (Objects.nonNull(loanAccountId)) {
          openAppParam = openAppGrantCouponService.buildOpenAppGrantCouponParam(GrantCouponRequest.builder()
                  .businessId(businessId)
                  .requestClientType(requestClientType)
                  .openAppPage(request.openAppPage)
                  .openAppScene(request.openAppScene)
                  .loanAccountId(loanAccountId)
                  .userId(userId)
                  .deviceToken(deviceToken)
                  .sdkType(request.pageSdkType)
                  .buildVersion(build)
              .build());
        }
      }

      marketingCenterClientService.publishSystemEvent(systemNotifScene, openAppParam);
      advertisementService.sendRealTimeEvent(AdRawDataEventVO.DataType.REAL_TIME_EVENT, buildRealTimeEventVO(request));
    } catch (Exception e) {
      // 业务失败时回滚幂等槽位，允许前端重试重新执行
      tryReleaseIdempotent(idempotentAcquired, deviceToken, request.openTimes, request.openAppScene);
      log.error("error when call OPEN_APP SystemNotifScene, businessId:{} ", businessId, e);
    }
    return EcResponseUtil.generateSuccess();
  }

  @PostMapping("/api/realTimeEvent/firstOpenApp")
  public Result firstOpenApp() {
    try {
      HttpServletRequest request = ecRequest();
      AppActivationParam firstOpenAppParam = AppActivationParam.builder()
          .deviceToken(RequestParser.getDeviceToken(request))
          .gaid(RequestParser.getPlatformEnvironmentInfo(request).gaid)
          .sdkType(RequestParser.getSDKType(request))
          .platformType(RequestParser.getPlatformType(request))
          .buildId(RequestParser.getBuild(request))
          .timeActivation(Clock.now())
          .appsflyerId(RequestParser.getAppsflyerId(request))
          .appInstanceId(RequestParser.getAppInstanceId(request))
          .adjustId(RequestParser.getAdjustId(request))
          .build();
      advertisementService.sendAdRawDataEvent(AdRawDataEventVO.DataType.APP_ACTIVATION, firstOpenAppParam);
    } catch (Exception e) {
      log.error("firstOpenApp error", e);
    }
    return EcResponseUtil.generateSuccess();
  }

  @PostMapping(path = "/api/realTimeEvent/openApp/{platform}")
  public Result openAppAdInfo(@PathVariable String platform, @RequestBody @Valid OpenAppAdRequest request) {
    // 使用userToken查找
    Long userId = getUserIdByToken();
    if (userId == null) {
      // 使用deviceToken查找
      String deviceToken = RequestParser.getDeviceToken(ecRequest());
      userId = loginStatusCacheModel.findLastUserIdByDeviceToken(deviceToken);
    }
    firebaseAppsflyerIdMonitor.addPoint(userId, AccessType.FIRST_OPEN_APP, request.appsflyerId, null, request.appInstanceId);
    HttpServletRequest originRequest = ecRequest();
    PlatformType platformType = PlatformType.fromString(RequestParser.getPlatformType(originRequest));
    Long build = RequestParser.getBuild(originRequest);
    SDKType sdkType = RequestParser.getSDKType(originRequest);
    AdPlatform adPlatform = AdPlatform.from(platform);
    String deviceToken = RequestParser.getDeviceToken(originRequest);
    switch (adPlatform) {
      case APPSFLYER:
        advertisementService.insertOrIgnoreAdAppsflyerLogRecord(userId, null, request.appsflyerId, platformType, sdkType, build, AccessType.FIRST_OPEN_APP, null, deviceToken);
        break;
      case FIREBASE:
        firebaseAnalyticsLogService.insertOrIgnore(userId, request.appInstanceId, platformType, sdkType, build, AccessType.FIRST_OPEN_APP, null, deviceToken);
        break;
      case ADJUST:
        AdjustInfo adjustInfo = new AdjustInfo();
        adjustInfo.setAdjustId(request.adjustId);
        advertisementService.insertOrIgnoreAdAdjustRecord(userId, adjustInfo, platformType, build, null, AccessType.FIRST_OPEN_APP, deviceToken);
        break;
      default:
        throw EcException.error("unsupported platform " + adPlatform);
    }
    return EcResponseUtil.generateSuccess();
  }

  private AdRealTimeEventVO buildRealTimeEventVO(RealTimeEventOpenAppRequest request) {
    EnvironmentInfo env = RequestParser.getPlatformEnvironmentInfo(ecRequest());
    String channel = RequestParser.getChannelOrNull(ecRequest());
    SDKType sdkType = RequestParser.getSDKType(ecRequest());
    Long build = RequestParser.getBuild(ecRequest());
    String deviceToken = RequestParser.getDeviceToken(ecRequest());
    TerminalInfo terminalInfo = RequestParser.getPlatformTerminalInfo(ecRequest());
    Long appCurrentOpenTIme = RequestParser.getAppCurrentOpenTIme(ecRequest());
    String platformType = RequestParser.getPlatformType(ecRequest());
    AdRealTimeEventVO realTimeEventVO = AdRealTimeEventVO.builder()
        .env(env)
        .terminalInfo(terminalInfo)
        .deviceToken(deviceToken)
        .channel(channel)
        .buildId(build)
        .sdkType(sdkType)
        .openTime(appCurrentOpenTIme)
        .openTimes(request.openTimes)
        .openAppPage(request.openAppPage)
        .pageSdkType(request.pageSdkType)
        .platformType(platformType)
        .appsFlyerId(RequestParser.getAppsflyerId(ecRequest()))
        .build();
    if (request.openAppScene != null) {
      realTimeEventVO.setOpenAppScene(request.openAppScene.name());
    }
    return realTimeEventVO;
  }

  /**
   * 尝试占用首启 CREATE 幂等槽位，Redis 异常时降级放行（返回 true）
   *
   * @param deviceToken 设备标识
   * @param openTimes   启动次数
   * @param scene       启动场景
   * @return true 首次处理或缓存异常降级；false 重复请求应跳过
   */
  private boolean tryAcquireIdempotent(String deviceToken, long openTimes, OpenAppScene scene) {
    try {
      boolean acquired = openAppFirstCreateIdempotentLoader.tryAcquire(deviceToken, openTimes, scene);
      if (!acquired) {
        log.info("openApp idempotent duplicate skipped, deviceToken={} scene={}, openTimes={}", deviceToken, scene, openTimes);
      }
      return acquired;
    } catch (Exception e) {
      log.warn("openApp idempotent check failed, degrade to pass-through, deviceToken={}, scene={}, openTimes={}", deviceToken, scene, openTimes, e);
      return true;
    }
  }

  /**
   * 业务异常时回滚幂等槽位，释放失败仅打日志不影响主流程
   */
  private void tryReleaseIdempotent(boolean acquired, String deviceToken, Long openTimes, OpenAppScene scene) {
    if (!acquired || StringUtils.isBlank(deviceToken) || openTimes == null) {
      return;
    }
    try {
      openAppFirstCreateIdempotentLoader.release(deviceToken, openTimes, scene);
      log.info("openApp idempotent key released after business failure, deviceToken={}, scene={}, openTimes={}", deviceToken, scene, openTimes);
    } catch (Exception e) {
      log.warn("openApp idempotent key release failed, deviceToken={}, scene={}, openTimes={}", deviceToken, scene, openTimes, e);
    }
  }

  //调用此接口时，用户的token可能过期，如果调用com.miyou.utilities.secureapi.UserLoginService.getUserId会抛warning让用户重新登录
  // 所以直接查LOGIN_STATUS_CACHE表
  public Long getUserIdByToken() {
    String userToken = RequestParser.getUserToken(ecRequest());
    if (userToken == null) {
      return null;
    }
    List<LoginStatusCacheVO> loginStatusCacheVOList = userService.findByUserToken(userToken);
    if (CollectionUtils.isEmpty(loginStatusCacheVOList)) {
      return null;
    }
    return loginStatusCacheVOList.get(0).userId;
  }
}
