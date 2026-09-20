package com.miyou.controllers.cashloan;

import static com.yqg.core.configure.devconfig.DynamicThreadExecutorConfiguration.AUTO_SUBMIT_CREDITS_ON_RETURN_EXECUTOR;
import static com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5.NOT_LOGIN;

import com.miyou.controllers.cashloan.request.AppPushShowRequest;
import com.miyou.controllers.cashloan.request.VipConfirmRequest;
import com.miyou.controllers.cashloan.response.AppPushShowCheckResponse;
import com.miyou.controllers.cashloan.response.CreateOrderSignFormatResponse;
import com.miyou.controllers.cashloan.response.SignFormatResponse;
import com.yqg.core.service.cashloan.vo.CreateOrderSignFormatVO;
import com.miyou.controllers.cashloan.response.SignInfoResponse;
import com.miyou.controllers.cashloan.response.home.SeaProductFeeCalcDetailResponse;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.SubHomePageResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.miyou.controllers.cashloan.utilities.LoanHomePageLogService;
import com.miyou.controllers.cashloan.utilities.subhomepage.SubHomepageContentTool;
import com.miyou.controllers.loan.BaseLoanController;
import com.miyou.utilities.secureapi.ECSecuredApi;
import com.miyou.utilities.secureapi.RequestParser;
import com.miyou.utilities.secureapi.UserLoginService;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.model.sql.signature.enums.VidaSignatureDivisionStrategy;
import com.yqg.core.service.apppush.AppPushCheckShowService;
import com.yqg.core.service.apppush.vo.AppPushCheckParamVO;
import com.yqg.core.service.bizcheck.signature.HandWrittenSignatureService;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.abtest.uiv2.HomePageUiAbTestManagerService;
import com.yqg.core.service.cashloan.homepage.enums.HomePageScene;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.EcHomePageProductTool;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageParamTool;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.SeaCouponDetail;
import com.yqg.core.service.cashloan.homepage.vo.prodcut.SeaProductFeeCache;
import com.yqg.core.service.cashloan.monthlyincome.MonthlyIncomeSignFormatService;
import com.yqg.core.service.cashloan.util.rate.ProductRateUtil;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.loan.coupon.vo.CutInterestVO;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.loan.vo.LoanReservationReceiptVO;
import com.yqg.core.service.secure.UserSecureService;
import com.yqg.core.userflow.application.risk.IAutoSubmitCreditsOnReturnApplicationService;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 印尼首页
 */
@Slf4j
@RestController
public class IDNHomepageV5Controller extends BaseLoanController {
  @Autowired
  private UserLoginService loginService;
  @Autowired
  private HomepageStatusTool homepageStatusTool;
  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private HomepageParamTool homepageParamTool;
  @Autowired
  private EcHomePageProductTool ecHomePageProductTool;
  @Autowired
  private UserSecureService userSecureService;
  @Autowired
  private AppPushCheckShowService appPushCheckShowService;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private SubHomepageContentTool subHomepageContentTool;
  @Autowired
  private HomePageUiAbTestManagerService homePageUiAbTestManagerService;
  @Autowired
  private HandWrittenSignatureService handWrittenSignatureService;
  @Autowired
  private LoanHomePageLogService loanHomePageLogService;
  @Autowired
  private MonthlyIncomeSignFormatService monthlyIncomeSignFormatService;
  @Autowired
  private IAutoSubmitCreditsOnReturnApplicationService autoSubmitCreditsOnReturnApplicationService;
  @Resource(name = AUTO_SUBMIT_CREDITS_ON_RETURN_EXECUTOR)
  private Executor autoSubmitCreditsOnReturnExecutor;

  @GetMapping(path = "/api/v5/cashloan/home")
  public Result<HomepageResponseV5> getHomepage(@RequestParam(value = "triggerSource", required = false) String triggerSource) {
    SDKType sdkType = RequestParser.getSDKTypeOrThrow(ecRequest());

    if (sdkType != SDKType.IDN_YQD) {
      throw EcException.error("unsupported sdkType:" + sdkType);
    }
    boolean isLogin = loginService.isUserLogin(ecRequest());
    HomepageResponseV5 homepageResponseV5;
    LoanApiViewerContext viewerContext;
    IDNHomepageLoanStatusV5 status;
    if (isLogin) {
      viewerContext = getViewerContextFromRequest();
      // 如果token状态是需要二次检查的 则要求二次检查通过才能访问
      userSecureService.assertSecondVerificationFinished(RequestParser.getUserToken(ecRequest()), sdkType, viewerContext.build, viewerContext.environmentInfo);
      beforeGetParams(viewerContext);
      // 避免由于读写时间导致的数据前后不一致问题
      HomepageUserParamsVO paramsVO = homepageParamTool.getHomepageV5ParamsVO(viewerContext.loanAccountId, viewerContext.build, sdkType);
      // 确认首页状态
      status = homepageStatusTool.confirmStatus(paramsVO, viewerContext.build);
      // 获取首页输出数据
      homepageResponseV5 = homepageContentTool.getHomepage(status, viewerContext, paramsVO, triggerSource, ecRequest());
      loanHomePageLogService.asyncLogUserInfo(paramsVO, status);
      // 额度失效用户回端自动戳额（TAPD-1365670）：异步触发，不阻塞首页返回；线程池由调用方注入
      autoSubmitCreditsOnReturnApplicationService.tryAutoSubmitAsync(viewerContext, status, autoSubmitCreditsOnReturnExecutor);
    } else {
      viewerContext = getNotLoginViewerContextFromRequest();
      status = NOT_LOGIN;
      homepageResponseV5 = homepageContentTool.getHomepage(status, viewerContext, getHomepageParamForNotLogin(), triggerSource, ecRequest());
    }
    return EcResponseUtil.generate(homepageResponseV5);
  }

  private HomepageUserParamsVO getHomepageParamForNotLogin() {
    HomepageUserParamsVO res = new HomepageUserParamsVO();
    res.notLoginHomePageU2Switch = homepageV5Config.isNewHomePageUIForNotLogin();
    res.build = RequestParser.getBuild(ecRequest());
    return res;
  }

  private void beforeGetParams(LoanApiViewerContext viewerContext) {
    homePageUiAbTestManagerService.executeHomePageUiV2AbTest(viewerContext.userId, viewerContext.build,
        viewerContext.sourceType, viewerContext.loanAccountId);
  }


  @ECSecuredApi
  @GetMapping(path = "/api/cashloan/subHomePage")
  public Result<HomepageResponseV5> getSubHomePage(@RequestParam(value = "homePageScene", required = false) HomePageScene homePageScene,
      @RequestParam(value = "triggerSource", required = false) String triggerSource) {
    SDKType sdkType = RequestParser.getSDKTypeOrThrow(ecRequest());
    if (sdkType != SDKType.IDN_YQD) {
      throw EcException.error("unsupported sdkType:" + sdkType);
    }
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    // 如果token状态是需要二次检查的 则要求二次检查通过才能访问
    userSecureService.assertSecondVerificationFinished(RequestParser.getUserToken(ecRequest()), sdkType, viewerContext.build, viewerContext.environmentInfo);
    beforeGetParamsForSubHomePage(viewerContext);
    // 避免由于读写时间导致的数据前后不一致问题
    HomepageUserParamsVO paramsVO = homepageParamTool.getHomepageV5ParamsVO(viewerContext.loanAccountId, viewerContext.build, sdkType);
    // 确认首页状态
    IDNHomepageLoanStatusV5 status = homepageStatusTool.confirmStatus(paramsVO, viewerContext.build);
    // 获取首页输出数据
    SubHomePageResponse subHomePageResponse = subHomepageContentTool.getSubHomepage(status, viewerContext, paramsVO,
        homePageScene == null ? HomePageScene.DEFAULT : homePageScene, triggerSource);
    return EcResponseUtil.generate(subHomePageResponse);
  }

  private void beforeGetParamsForSubHomePage(LoanApiViewerContext viewerContext) {
    homePageUiAbTestManagerService.executeAbTestForSubHomePage(viewerContext.userId, viewerContext.loanAccountId);
  }

  @GetMapping(path = "/api/v5/cashloan/userStatus")
  public Result getStatus() {
    SDKType sdkType = RequestParser.getSDKTypeOrThrow(ecRequest());
    if (sdkType != SDKType.IDN_YQD) {
      throw EcException.error("unsupported sdkType:" + sdkType);
    }
    boolean isLogin = loginService.isUserLogin(ecRequest());
    LoanApiViewerContext viewerContext;
    UserResponse userInfo = new UserResponse();
    IDNHomepageLoanStatusV5 status;
    if (isLogin) {
      viewerContext = getViewerContextFromRequest();
      // 如果token状态是需要二次检查的 则要求二次检查通过才能访问
      userSecureService.assertSecondVerificationFinished(RequestParser.getUserToken(ecRequest()), sdkType, viewerContext.build, viewerContext.environmentInfo);
      // 确认首页状态
      status = homepageStatusTool.getStatus(viewerContext.loanAccountId, viewerContext.build, sdkType);
    } else {
      status = NOT_LOGIN;
    }

    userInfo.setExactStatus(status.name());

    return EcResponseUtil.generate(userInfo);
  }

  @PostMapping("/api/cashloan/ignoreCreditsDecreaseQuickOrder")
  @ECSecuredApi
  @Deprecated
  public Result ignoreCreditsDecreaseQuickOrder() {
    log.warn("ignoreCreditsDecreaseQuickOrder is deprecated");
    return EcResponseUtil.generateSuccess();
  }


  @GetMapping(path = "/api/cashloan/productFeeCalcDetail")
  public Result getProductFeeCalcDetail(
      @RequestParam("productId") String productId,
      @RequestParam("amount") BigDecimal amount,
      @RequestParam(value = "couponId", required = false) Long couponId) {
    Long decodedProductId = YqgHashids.decode(productId);
    SeaProductFeeCache feeCache = ecHomePageProductTool.getProductFeeCache(decodedProductId, amount);
    BigDecimal dayInterestRate = ProductRateUtil.genDayInterestRate(feeCache.productConfigVO);

    Long build = RequestParser.getBuild(ecRequest());
    Long userId = !loginService.isUserLogin(ecRequest()) ? null : loginService.getUserId(ecRequest());
    // 获取可用优惠券信息
    Pair<SeaCouponDetail, CutInterestVO> couponPair = ecHomePageProductTool.getAvailableCouponPair(feeCache, couponId, userId, null);
    SeaCouponDetail couponDetail = couponPair == null ? null : couponPair.getLeft();

    return EcResponseUtil.generate(SeaProductFeeCalcDetailResponse.from(feeCache, couponDetail, dayInterestRate));
  }

  @PostMapping(path = "/api/appPushShowCheck")
  public Result appPushShowCheck(@RequestBody AppPushShowRequest request) {
    boolean isLogin = loginService.isUserLogin(ecRequest());
    if (!isLogin) {
      return EcResponseUtil.generate(AppPushShowCheckResponse.from(false));
    }
    AppPushCheckParamVO appPushCheckParamVO = JsonUtils.fromOrNull(request.checkParam, AppPushCheckParamVO.class);
    if (Objects.isNull(appPushCheckParamVO)) {
      return EcResponseUtil.generate(AppPushShowCheckResponse.from(false));
    }
    boolean canShow = appPushCheckShowService.checkShow(appPushCheckParamVO, getViewerContextFromRequest().userId);

    return EcResponseUtil.generate(AppPushShowCheckResponse.from(canShow));
  }

  @PostMapping("/api/cashloan/vipConfirm")
  @ECSecuredApi
  public Result vipConfirm(@RequestBody @Validated VipConfirmRequest request) {
    return EcResponseUtil.generateSuccess();
  }

  /**
   * 用户预约借款意愿按钮上报记录
   *
   * @return
   */
  @PostMapping(path = "/api/cashloan/reserveLoanIntentionSubmit")
  @ECSecuredApi
  public Result reserveLoanIntentionSubmit() {
    SDKType sdkType = RequestParser.getSDKTypeOrThrow(ecRequest());
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    // 确认首页状态
    IDNHomepageLoanStatusV5 status = homepageStatusTool.getStatus(viewerContext.loanAccountId, viewerContext.build, sdkType);
    DwLogUtil.newLog(LogBusinessType.RESERVE_LOAN_BUTTON_SUBMIT_LOG, LoanReservationReceiptVO.from(viewerContext.userId, status));

    return EcResponseUtil.generateSuccess();
  }

  /**
   * 获取签名信息
   * <p>
   * 包含手写签名策略/格式（用于 VIDA 签名），以及月收入声明格式（orderPage/dialog，AB 分流控制）。
   *
   * @param selectedProductId 用户选中的产品 ID（供月收入声明场景使用）
   * @param couponId          优惠券 ID（可选，供月收入声明场景使用）
   * @param loanAmount        用户申请的贷款金额（用于月收入 Z 值计算）
   * @return 签名信息响应，包含签名策略、手写签名格式及月收入声明格式
   */
  @GetMapping(path = "/api/cashloan/signInfo")
  @ECSecuredApi
  public Result<SignInfoResponse> getSignInfo(
      @RequestParam(value = "selectedProductId", required = false) String selectedProductId,
      @RequestParam(value = "couponId", required = false) Long couponId,
      @RequestParam(value = "loanAmount", required = false) BigDecimal loanAmount) {
    SDKType sdkType = RequestParser.getSDKTypeOrThrow(ecRequest());
    if (sdkType != SDKType.IDN_YQD) {
      throw EcException.error("unsupported sdkType:" + sdkType);
    }

    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    // 如果token状态是需要二次检查的 则要求二次检查通过才能访问
    userSecureService.assertSecondVerificationFinished(RequestParser.getUserToken(ecRequest()), sdkType, viewerContext.build, viewerContext.environmentInfo);

    // 获取签名策略
    VidaSignatureDivisionStrategy signatureDivisionStrategy = handWrittenSignatureService.fetchVidaSignatureStrategyByDivision(viewerContext.userId);
    HomeDisplayStrategy orderPageSignStrategy = handWrittenSignatureService.getSignFormatStrategy(viewerContext.sourceType, viewerContext.userId, viewerContext.build, signatureDivisionStrategy);

    // 先构建基础协议 VO，再追加月收入声明（月收入是协议字符串的最后一跳）
    CreateOrderSignFormatVO orderPageBaseVO = homepageContentTool.getOrderPageSignFormatVO(
        orderPageSignStrategy, signatureDivisionStrategy,
        homepageV5Config.getCreateOrderSignFormatConfigMap(), viewerContext.userId, viewerContext.build);
    CreateOrderSignFormatVO dialogPageBaseVO = homepageContentTool.getOrderPageSignFormatVO(
        orderPageSignStrategy, signatureDivisionStrategy,
        homepageV5Config.getDialogSignFormatConfigMap(), viewerContext.userId, viewerContext.build);
    // feeCache 用于计算 B（本笔单期应还 MAX）；selectedProductId 或 loanAmount 缺失时降级为 null
    SeaProductFeeCache signInfoFeeCache = null;
    if (selectedProductId != null && loanAmount != null) {
      signInfoFeeCache = ecHomePageProductTool.getProductFeeCache(
          YqgHashids.decode(selectedProductId), loanAmount);
    }
    // 一次计算，两次追加，避免重复调 A/B/Z/Y；对照组不返回月收入协议
    BigDecimal effectiveIncome = monthlyIncomeSignFormatService.computeEffectiveIncome(
        viewerContext.userId, viewerContext.loanAccountId, signInfoFeeCache, viewerContext.build);

    if (effectiveIncome != null) {
      monthlyIncomeSignFormatService.appendSignText(orderPageBaseVO, false);
    }
    SignInfoResponse response = new SignInfoResponse()
        .setOrderPageSignStrategy(orderPageSignStrategy)
        .setOrderPageSignFormat(CreateOrderSignFormatResponse.from(orderPageBaseVO))
        ;

    return EcResponseUtil.generate(response);
  }
}
