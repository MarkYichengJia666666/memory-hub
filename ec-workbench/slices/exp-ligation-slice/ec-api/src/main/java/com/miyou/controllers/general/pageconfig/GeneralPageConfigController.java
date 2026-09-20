package com.miyou.controllers.general.pageconfig;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.miyou.controllers.cashloan.newhomepage.HomepageV3AbTestService;
import com.miyou.controllers.core.YqgBaseController;
import com.miyou.controllers.general.pageconfig.request.DrawInterestFreeCouponRequest;
import com.miyou.controllers.general.pageconfig.request.EcAppResourceRequest;
import com.miyou.controllers.general.pageconfig.response.BottomTabResponse;
import com.miyou.controllers.general.pageconfig.response.BottomTabResponse.BottomTab;
import com.miyou.controllers.general.pageconfig.response.FloatingIconResponse;
import com.miyou.controllers.user.BaseViewerDeviceContext;
import com.miyou.utilities.AppResourceResponseVO;
import com.miyou.utilities.McAppResourceConvertUtil;
import com.miyou.utilities.secureapi.ECSecuredApi;
import com.miyou.utilities.secureapi.RequestParser;
import com.miyou.utilities.secureapi.UserLoginService;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigType;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.ExpLastResultRunningClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.risk.AppListDialogSuppressService;
import com.yqg.core.service.cashloan.activity.HomeInterestFreeCardService;
import com.yqg.core.service.cashloan.activity.HomeInterestFreeCardService.DrawResult;
import com.yqg.core.service.cashloan.homepage.abtest.uiv2.HomePageUiAbTestManagerService;
import com.yqg.core.service.cashloan.homepage.abtest.uiv2.HomePageUiAbTestService;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.general.mcresource.handler.AppResourceExtraHandleContext;
import com.yqg.core.service.general.mcresource.handler.AppResourceExtraParamHandlerFactory;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.general.mcresource.AppResourceManagerService;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.general.pageconfig.vo.GeneralPageConfigShowLogVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.mc.appresource.AppResourceConfig;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.constant.AppResourceExtraParam;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.mc.common.vo.appresource.AppResourceBaseInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class GeneralPageConfigController extends YqgBaseController {

  @Autowired
  private UserLoginService loginService;
  @Autowired
  private HomepageStatusTool homepageStatusTool;
  @Autowired
  private LoanAccountService accountService;
  @Autowired
  private HomePageUiAbTestManagerService homePageUiAbTestManagerService;
  @Autowired
  private HomePageUiAbTestService homePageUiAbTestService;
  @Autowired
  private AppResourceManagerService appResourceManagerService;
  @Autowired
  private HomepageV3AbTestService homepageV3AbTestService;
  @Autowired
  private ExpLastResultRunningClient expLastResultRunningClient;
  @Autowired
  private AppResourceConfig appResourceConfig;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private HomeInterestFreeCardService homeInterestFreeCardService;
  @Autowired
  private ExpFacade expFacade;
  @Autowired
  private AppResourceExtraParamHandlerFactory appResourceExtraParamHandlerFactory;
  @Autowired
  private AppListDialogSuppressService appListDialogSuppressService;

  @GetMapping(path = "/api/pageConfig")
  public Result getPageConfig(@RequestParam GeneralPageConfigType type, @RequestParam(required = false) Integer limit) {
    SDKType sdkType = getSdkType();
    Long build = RequestParser.getBuild(ecRequest());
    PlatformType platform = PlatformType.fromString(RequestParser.getPlatformType(ecRequest()));
    Long userId = loginService.isUserLogin(ecRequest()) ? getBaseViewerContextFromRequest().userId : null;
    Long loanAccountId = sdkType.isLoanSDKType() ? accountService.getAccountIdByUserId(userId, sdkType) : null;
    IDNHomepageLoanStatusV5 status = homepageStatusTool.getStatus(loanAccountId, build, sdkType);
    Map<String, List<AppResourceBaseInfo>> appResourceMap = appResourceManagerService.getResourceFromMc(
        GeneralPageConfigParam.from(userId, sdkType, build, status, RequestParser.getDeviceToken(ecRequest()),
            RequestParser.getSourceType(ecRequest())), Lists.newArrayList(type));
    AppResourceResponseVO appResourceResponseVO = McAppResourceConvertUtil.buildResponse(appResourceMap, type);
    Object res = appResourceResponseVO.resource;
    List<Long> generalPageConfigIdList = appResourceResponseVO.sourceIds;
    if (CollectionUtils.isNotEmpty(generalPageConfigIdList)) {
      DwLogUtil.log(LogBusinessType.GENERAL_PAGE_CONFIG_SHOW_LOG,
          GeneralPageConfigShowLogVO.from(ecRequest().getRequestURI(), userId, generalPageConfigIdList, status,
              RequestParser.getRemoteIp(ecRequest()), RequestParser.getDeviceToken(ecRequest()), RequestParser.getPlatformType(ecRequest()),
              RequestParser.getBuild(ecRequest()), RequestParser.getSDKType(ecRequest())));
    }
    return EcResponseUtil.generate(res);
  }


  @Deprecated
  @GetMapping(path = "/api/appResource")
  public Result getAppResource(@RequestParam List<String> type) {
    return doGetAppResource(getSdkType(), type, new HashMap<>(), false);
  }

  @PostMapping(path = "/api/appResourceV2")
  public Result getAppResource(@RequestBody EcAppResourceRequest request) {
    Boolean needTriggerSource = isInResourceV2RefreshLimitExpGroup();
    return doGetAppResource(getSdkType(), request.types, request.extraParams, needTriggerSource);
  }

  /**
   * 判断是否命中账单页和个人中心限频实验，此类场景保证使用resourceV2<br/>
   * <a href="https://fintopia.feishu.cn/wiki/MQVrwCnFsivlsCkjSOUcgWNUnte">刷新页面限制弹窗曝光</a>
   *
   * @return
   */
  private Boolean isInResourceV2RefreshLimitExpGroup() {
    final String expKey = "technology-other-abroad-loan_all-enableResourceV2RefreshLimit";
    if (!loginService.isUserLogin(ecRequest())) {
      return false;
    }
    Long userId = loginService.getUserId(ecRequest());
    ExpUser expUser = ExpUser.builder()
        .userId(userId)
        .sourceType(RequestParser.getSourceType(ecRequest()))
        .versionBuild(RequestParser.getBuild(ecRequest()))
        .build();
    return expDiversionClient.getBoolean(expKey, expUser, false);
  }

  private Result doGetAppResource(SDKType sdkType, List<String> types, Map<String, Object> extraParams, Boolean needTriggerSource) {
    Long build = RequestParser.getBuild(ecRequest());
    Long userId = loginService.isUserLogin(ecRequest()) ? getBaseViewerContextFromRequest().userId : null;
    Long loanAccountId = sdkType.isLoanSDKType() ? accountService.getAccountIdByUserId(userId, sdkType) : null;
    IDNHomepageLoanStatusV5 status = homepageStatusTool.getStatus(loanAccountId, build, sdkType);
    String triggerSource = needTriggerSource ? AppResourceExtraParam.TRIGGER_SOURCE.getOrNull(extraParams) : null;
    // 资源位旁路副作用：按 type 调度已注册的处理器（如下单页 Back 挽留灌券 TAPD-1153182677001368547），
    // 仅命中资源位的处理器执行副作用并 enrich extraParams；未命中不处理，各处理器自兜底不阻断响应（NFR-001）。
    extraParams = appResourceExtraParamHandlerFactory.handle(new AppResourceExtraHandleContext(
        userId, sdkType, build, loanAccountId, RequestParser.getSourceType(ecRequest()), types, extraParams));
    return EcResponseUtil.generate(appResourceManagerService.getResourceFromMcForApp(
        GeneralPageConfigParam.fromWithTriggerSource(userId, sdkType, build, status, RequestParser.getDeviceToken(ecRequest()),
            RequestParser.getSourceType(ecRequest()), triggerSource,
            GeneralPageConfigParam.buildExtraParams(RequestParser.getAppCurrentOpenTIme(ecRequest()), extraParams)), types));
  }

  /**
   * 由于android端历史框架的原因，在注册页调用此接口的时候，header里面的SDKType固定是借贷 所以这里加了一个参数，给app自由选择想要的SDKType对应的底tab
   *
   * @param sdkTypeList 改参数只有android端会传借贷和理财的两个sdk，与app开发沟通之后，理财的SDK可以不用传的， 这里后端先处理一下：列表参数有两个时，只处理借贷的sdk
   * @return
   */
  @GetMapping(path = "/api/bottomTab")
  public Result getBottomTab(@RequestParam List<SDKType> sdkTypeList) {
    SDKType chooseSdkType = null;
    if (sdkTypeList.size() > 1) {
      chooseSdkType = SDKType.IDN_YQD;
    } else {
      chooseSdkType = sdkTypeList.get(0);
    }
    List<BottomTab> bottomTabList = new ArrayList<>();
    Long build = RequestParser.getBuild(ecRequest());
    String deviceToken = RequestParser.getDeviceToken(ecRequest());
    PlatformType platform = PlatformType.fromString(RequestParser.getPlatformType(ecRequest()));
    Long userId = loginService.isUserLogin(ecRequest()) ? getBaseViewerContextFromRequest().userId : null;
    // Live Demo：命中 AppListDialogSuppressService 时直接返回隐藏 tab3 的前置配置
    if (appListDialogSuppressService.shouldSuppress(userId, deviceToken, build)) {
      return EcResponseUtil.generate(new BottomTabResponse(CommonABTestResultGroup.B));
    }
    List<Long> generalPageConfigIdList = new ArrayList<>();
    Long loanAccountId = chooseSdkType.isLoanSDKType() ? accountService.getAccountIdByUserId(userId, chooseSdkType) : null;
    IDNHomepageLoanStatusV5 sdkStatus = homepageStatusTool.getStatus(loanAccountId, build, chooseSdkType);
    HashMap<String, Object> params = Maps.newHashMap();
    params.put("homepageVersion", homepageV3AbTestService.fetchExistOrDefaultHomepageVersion(build, userId, sdkStatus));
    Map<String, List<AppResourceBaseInfo>> appResourceMap = appResourceManagerService.getResourceFromMc(
        GeneralPageConfigParam.from(userId, RequestParser.getSDKType(ecRequest()), RequestParser.getBuild(ecRequest()), sdkStatus,
            RequestParser.getDeviceToken(ecRequest()), RequestParser.getSourceType(ecRequest()), params),
        Lists.newArrayList(GeneralPageConfigType.BOTTOM_TAB));
    AppResourceResponseVO bannerResource = McAppResourceConvertUtil.buildResponse(appResourceMap, GeneralPageConfigType.BOTTOM_TAB);
    bottomTabList = Optional.of(bannerResource).map(item -> item.resource).map(item -> ((BottomTabResponse) (item)).bottomTabList)
        .orElse(new ArrayList<>());
    CommonABTestResultGroup commonABTestResultGroup = showBottomTab(userId, chooseSdkType, bottomTabList);
    if (bannerResource.resource == null) {
      return EcResponseUtil.generate(new BottomTabResponse(commonABTestResultGroup));
    }
    generalPageConfigIdList = bannerResource.sourceIds;
    if (CollectionUtils.isNotEmpty(generalPageConfigIdList)) {
      DwLogUtil.log(LogBusinessType.GENERAL_PAGE_CONFIG_SHOW_LOG,
          GeneralPageConfigShowLogVO.from(ecRequest().getRequestURI(), userId, generalPageConfigIdList, sdkStatus,
              RequestParser.getRemoteIp(ecRequest()), RequestParser.getDeviceToken(ecRequest()), RequestParser.getPlatformType(ecRequest()),
              RequestParser.getBuild(ecRequest()), RequestParser.getSDKType(ecRequest())));
    }
    if (!getShowTabRes(userId, bottomTabList, commonABTestResultGroup)) {
      return EcResponseUtil.generate(new BottomTabResponse(commonABTestResultGroup));
    }
    return EcResponseUtil.generate(BottomTabResponse.fromTabList(bottomTabList, commonABTestResultGroup));
  }

  private CommonABTestResultGroup showBottomTab(Long userId, SDKType sdkType, List<BottomTab> bottomTabList) {
    if (userId == null) {
      return CommonABTestResultGroup.A;
    }
    if (sdkType != SDKType.IDN_YQD) {
      return CommonABTestResultGroup.A;
    }
    if (CollectionUtils.isEmpty(bottomTabList)) {
      return CommonABTestResultGroup.A;
    }
    String result = expLastResultRunningClient.getResult("pretii-other-abroad-loan_all-EASYPLUS_tab3",
        ExpUser.builder().userId(userId).versionBuild(ImpliedContextUtils.build()).build());
    if (StringUtils.equals(CommonABTestResultGroup.B.name(), result)) {
      return CommonABTestResultGroup.B;
    }
    return CommonABTestResultGroup.A;
  }

  private @Nullable boolean getShowTabRes(Long userId, List<BottomTab> bottomTabList, CommonABTestResultGroup res) {
    if (ImpliedContextUtils.build() < 37900L) {
      return true;
    }
    if (res == CommonABTestResultGroup.A) {
      return true;
    }
    BottomTab bottomTab = bottomTabList.get(0);
    String name = MapUtils.getString((Map) bottomTab.detail, "textInImage");
    boolean showTab = appResourceConfig.getEcPlusName().contains(StringUtils.upperCase(name));
    if (!showTab) {
      log.info("命中easy plus ab, 第一个元素不是plus，不展示 bottomTab， userId:{}", userId);
    }
    return showTab;
  }

  @GetMapping(path = "/api/loan/floatingIcon")
  public Result getLoanFloatingIcon() {

    boolean isLogin = loginService.isUserLogin(ecRequest());
    Long userId = isLogin ? loginService.getUserId(ecRequest()) : null;
    BaseViewerDeviceContext dc = getBaseViewerDeviceContextFromRequest();

    LoanAccountVO accountVO = (dc.sdkType.isLoanSDKType() && isLogin) ? accountService.initOrGetLoanAccount(userId, dc.sdkType, dc.build,
        dc.environmentInfo.blackBox, dc.platform, dc.deviceInfo, dc.sourceType, dc.deviceToken) : null;
    IDNHomepageLoanStatusV5 status = accountVO == null ? null : homepageStatusTool.getStatus(accountVO.id, dc.build, dc.sdkType);
    if (orderPageNotShow(accountVO, status)) {
      return EcResponseUtil.generate(new FloatingIconResponse());
    }
    Object resourceResult = null;
    List<Long> resourceId = new ArrayList<>();
    Map<String, Object> extraParams = new HashMap<>();
    AppResourceExtraParam.HOME_PAGE_TYPE.setValue(extraParams, "HOME_PAGE_FOR_LEVEL_1");
    Map<String, List<AppResourceBaseInfo>> appResourceMap = appResourceManagerService.getResourceFromMc(
        GeneralPageConfigParam.from(userId, RequestParser.getSDKType(ecRequest()), RequestParser.getBuild(ecRequest()), status,
            RequestParser.getDeviceToken(ecRequest()), RequestParser.getSourceType(ecRequest()), extraParams),
        Lists.newArrayList(GeneralPageConfigType.FLOATING_ICON));
    resourceResult = McAppResourceConvertUtil.buildResponse(appResourceMap, GeneralPageConfigType.FLOATING_ICON).resource;

    if (CollectionUtils.isNotEmpty(resourceId)) {
      DwLogUtil.log(LogBusinessType.GENERAL_PAGE_CONFIG_SHOW_LOG,
          GeneralPageConfigShowLogVO.from(ecRequest().getRequestURI(), userId, resourceId, status, RequestParser.getRemoteIp(ecRequest()),
              RequestParser.getDeviceToken(ecRequest()), RequestParser.getPlatformType(ecRequest()), RequestParser.getBuild(ecRequest()),
              RequestParser.getSDKType(ecRequest())));
    }
    return EcResponseUtil.generate(resourceResult);
  }

  /**
   * 新首页一级页面需要展示挂件，旧首页根据实验判断是否展示
   *
   * @param accountVO
   * @param status
   * @return
   */
  private boolean orderPageNotShow(LoanAccountVO accountVO, IDNHomepageLoanStatusV5 status) {
    if (Objects.isNull(status)) {
      return false;
    }
    HomeDisplayStrategy homePageUiV2PopupAbTest = homePageUiAbTestService.getHomePageUiV2PopupAbTest(accountVO.userId, accountVO.id);
    if (homePageUiV2PopupAbTest.isStrategyB() || homePageUiV2PopupAbTest == HomeDisplayStrategy.C) {
      return false;
    }
    if (!status.canCreateOrder()) {
      return false;
    }
    CommonABTestResultGroup floatAbResult = CommonABTestResultGroup.valueOf(
        expFacade.fetchResult(ABTestSceneType.ORDER_PAGE_FLOAT, ABTestUtil.genDiversionKeyMapByUserId(accountVO.userId), "A"));
    return CommonABTestResultGroup.B == floatAbResult;
  }


  @ECSecuredApi
  @GetMapping(path = "/api/loan/subHomePage/floatingIcon")
  public Result getLoanFloatingIconForSubHomePage() {
    Long userId = loginService.getUserId(ecRequest());

    BaseViewerDeviceContext dc = getBaseViewerDeviceContextFromRequest();

    LoanAccountVO accountVO = accountService.getAccountByUserIdOrThrow(userId, dc.sdkType);
    if(!homepageStatusTool.getSubHomePageFloatingIconDisplaySwitch()){
      return EcResponseUtil.generate(new FloatingIconResponse());
    }

    IDNHomepageLoanStatusV5 status = homepageStatusTool.getStatus(accountVO.id, dc.build, dc.sdkType);

    Map<String, Object> extraParams = new HashMap<>();
    AppResourceExtraParam.HOME_PAGE_TYPE.setValue(extraParams, "HOME_PAGE_FOR_LEVEL_2");
    Map<String, List<AppResourceBaseInfo>> appResourceMap = appResourceManagerService.getResourceFromMc(
        GeneralPageConfigParam.from(userId, RequestParser.getSDKType(ecRequest()), RequestParser.getBuild(ecRequest()), status,
            RequestParser.getDeviceToken(ecRequest()), RequestParser.getSourceType(ecRequest()), extraParams),
        Lists.newArrayList(GeneralPageConfigType.FLOATING_ICON));
    AppResourceResponseVO appResourceResponseVO = McAppResourceConvertUtil.buildResponse(appResourceMap,
        GeneralPageConfigType.FLOATING_ICON);
    Object resourceResult = appResourceResponseVO.resource;
    return EcResponseUtil.generate(resourceResult);
  }


  @GetMapping(path = {"/api/financing/floatingIcon"})
  public Result getFinFloatingIcon() {

    boolean isLogin = loginService.isUserLogin(ecRequest());
    Long userId = isLogin ? loginService.getUserId(ecRequest()) : null;

    BaseViewerDeviceContext dc = getBaseViewerDeviceContextFromRequest();
    Map<String, List<AppResourceBaseInfo>> appResourceMap = appResourceManagerService.getResourceFromMc(
        GeneralPageConfigParam.from(userId, RequestParser.getSDKType(ecRequest()), RequestParser.getBuild(ecRequest()), null,
            RequestParser.getDeviceToken(ecRequest()), RequestParser.getSourceType(ecRequest())),
        Lists.newArrayList(GeneralPageConfigType.FLOATING_ICON));
    AppResourceResponseVO appResourceResponseVO = McAppResourceConvertUtil.buildResponse(appResourceMap,
        GeneralPageConfigType.FLOATING_ICON);
    Object resourceResult = appResourceResponseVO.resource;
    List<Long> resourceId = new ArrayList<>(appResourceResponseVO.sourceIds);

    if (CollectionUtils.isNotEmpty(resourceId)) {
      DwLogUtil.log(LogBusinessType.GENERAL_PAGE_CONFIG_SHOW_LOG,
          GeneralPageConfigShowLogVO.from(ecRequest().getRequestURI(), userId, resourceId, null, RequestParser.getRemoteIp(ecRequest()),
              RequestParser.getDeviceToken(ecRequest()), RequestParser.getPlatformType(ecRequest()), RequestParser.getBuild(ecRequest()),
              RequestParser.getSDKType(ecRequest())));
    }
    return EcResponseUtil.generate(resourceResult);
  }


  private SDKType getSdkType() {
    SDKType sdkType = RequestParser.getSDKType(ecRequest());
    if (sdkType == null) {
      throw EcException.error("sdk type is null");
    }
    return sdkType;
  }

  /**
   * 首页免息卡抽奖发券接口
   * 用户点击下单页弹窗后触发发券
   */
  @ECSecuredApi
  @PostMapping(path = "/api/loan/interestFreeCard/draw")
  public Result drawInterestFreeCoupon(@RequestBody DrawInterestFreeCouponRequest request) {
    Long userId = loginService.getUserId(ecRequest());
    Long build = RequestParser.getBuild(ecRequest());
    DrawResult result = homeInterestFreeCardService.drawInterestFreeCoupon(userId, build, request.getRuleId());
    return EcResponseUtil.generate(result);
  }

}
