package com.miyou.controllers.cashloan.newhomepage.homepagecontext;

import com.miyou.controllers.cashloan.newhomepage.HomepageV3AbTestService;
import com.miyou.controllers.cashloan.newhomepage.IHomepageResponsePostProcessor;
import com.miyou.controllers.cashloan.newhomepage.SafetyModuleExpService;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.homepagecontext.processor.HomePageContextProcessorType;
import com.miyou.controllers.cashloan.newhomepage.processormap.IStatusProcessorSelector;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageV3ExperimentContext;
import com.yqg.core.model.sql.loan.creditsaware.LoanUserCreditsDetailsAwareService;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpPreDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.cashloan.homepage.enums.HomePageScene;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomePageServiceManager;
import com.yqg.core.service.cashloan.homepage.vo.HomepagePrepareAbTestVO;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserCashLoanOrderContext;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.experiment.InterestSplitExpService;
import com.yqg.core.service.experiment.enums.InterestSplitResGroup;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import com.yqg.core.service.loan.credits.LoanUserVirtualCreditsService;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.loan.discount.DisCountFeeExperimentService;
import com.yqg.core.service.loanmarket.LoanMarketUserQualifyService;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;

import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.serialization.JsonUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class HomePageContextManager {
  @Autowired
  private HomepageV3AbTestService homepageV3AbTestService;
  @Autowired
  private LoanMarketUserQualifyService loanMarketUserQualifyService;
  @Autowired
  private HomePageServiceManager homePageServiceManager;
  @Autowired
  private List<IStatusProcessorSelector> statusProcessorSelectors;
  @Autowired
  private List<IHomepageResponsePostProcessor> responsePostProcessorList;
  @Autowired
  private LoanUserCreditsDetailsAwareService creditAwareService;
  @Autowired
  private List<IHomepageHomeContextProcessor> homepageHomeContextProcessors;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private ExpPreDiversionClient expPreDiversionClient;
  @Autowired
  private UserCashLoanOrderContextBuildService userCashLoanOrderContextBuildService;
  @Autowired
  private LoanUserVirtualCreditsService loanUserVirtualCreditsService;
  @Autowired
  private LoanCreditsQuotaService loanCreditsQuotaService;
  @Autowired
  private InterestSplitExpService interestSplitExpService;
  @Autowired
  private DisCountFeeExperimentService disCountFeeExperimentService;
  @Autowired
  private SafetyModuleExpService safetyModuleExpService;

  protected Map<HomePageContextProcessorType, IHomepageHomeContextProcessor> homepageHomeContextProcessorMap;
  private Map<IDNHomepageLoanStatusV5, IHomepageContextProcessorSelector> statusContextProcessorMap = new HashMap<>();

  @PostConstruct
  private void init() {
    homepageHomeContextProcessorMap = homepageHomeContextProcessors.stream()
        .collect(Collectors.toMap(item -> item.getType(), Function.identity()));
    statusProcessorSelectors.forEach(item -> {
      statusContextProcessorMap.put(item.getStatus(), (IHomepageContextProcessorSelector) item);
    });
  }


  protected HomePageContext prepareContext(IDNHomepageLoanStatusV5 status, UserDeviceContextVO userDeviceContextVO,
      HomepageUserParamsVO paramsVO, HomePageType homePageType, HomePageScene homePageScene, String triggerSource) {
    return prepareContextCommon(status, userDeviceContextVO, paramsVO, homePageType, homePageScene, triggerSource, false);
  }

  protected HomePageContext prepareContextPreDiversion(IDNHomepageLoanStatusV5 status, UserDeviceContextVO userDeviceContextVO,
                                           HomepageUserParamsVO paramsVO, HomePageType homePageType, HomePageScene homePageScene, String triggerSource) {
    return prepareContextCommon(status, userDeviceContextVO, paramsVO, homePageType, homePageScene, triggerSource, true);
  }

  private HomePageContext prepareContextCommon(IDNHomepageLoanStatusV5 status, UserDeviceContextVO userDeviceContextVO,
                                               HomepageUserParamsVO paramsVO, HomePageType homePageType, HomePageScene homePageScene, String triggerSource, boolean isPreDiversion) {
    UserCashLoanOrderContext userCashLoanOrderContext = userCashLoanOrderContextBuildService.buildContext(paramsVO);
    LoanMarketUserQualifyCheckResult loanMarketUserQualifyCheckResult = loanMarketUserQualifyService.insertAndGetQualifyCheckResultByUserHit(paramsVO.getLatestUserRiskTraceVO());
    UserCreditsContext userCreditsContext = UserCreditsContext.fromHomepageUserParams(paramsVO, loanMarketUserQualifyCheckResult);
    HomepagePrepareAbTestVO prepareAbTestVO = paramsVO.prepareAbTestVO;
    HomepageV3ExperimentContext homepageV3ExperimentContext;
    List<String> overrideResult;
    if (isPreDiversion) {
      homepageV3ExperimentContext = homepageV3AbTestService.fetchHomepageV3ExperimentContextPrediversion(userDeviceContextVO.getBuild(), paramsVO.getAccountVO(), status);
      overrideResult = overrideTriggerSourcePrediversion(triggerSource, paramsVO.getUserId(), userDeviceContextVO.getDeviceToken(),
          userDeviceContextVO.getSourceType(), userDeviceContextVO.getBuild(), homePageType);
    } else {
      homepageV3ExperimentContext = homepageV3AbTestService.fetchHomepageV3ExperimentContext(userDeviceContextVO.getBuild(), paramsVO.getAccountVO(), status);
      overrideResult = overrideTriggerSource(triggerSource, paramsVO.getUserId(), userDeviceContextVO.getDeviceToken(),
          userDeviceContextVO.getSourceType(), userDeviceContextVO.getBuild(), homePageType);
    }
    triggerSource = overrideResult.get(0);
    // 只有实验组禁用刷新，因此反向判断
    boolean isDisableRefresh = Objects.equals(overrideResult.get(1), "DISABLE_REFRESH");
    if (HomePageType.HOME_PAGE_FOR_LEVEL_2.equals(homePageType)) {
      disCountFeeExperimentService.executeExpDiversion(paramsVO.getUserId(), paramsVO.getBuild(),
          userDeviceContextVO.getSourceType(), userDeviceContextVO.getClientType(), status);
      interestSplitExpService.executeExpDiversion(paramsVO.getUserId(), paramsVO.getBuild(),
          userDeviceContextVO.getSourceType(), userDeviceContextVO.getClientType(), status);
    }
    return HomePageContext.create(
        status,
        userDeviceContextVO,
        userCreditsContext,
        userCashLoanOrderContext,
        paramsVO.accountVO,
        prepareAbTestVO,
        paramsVO,
        homePageType,
        homePageScene,
        homePageServiceManager, homepageV3ExperimentContext, triggerSource, isDisableRefresh
    );
  }

  /**
   * 判断是否命中首页或下单页限频实验，并重写triggerSource<br/>
   * <a href="https://fintopia.feishu.cn/wiki/MQVrwCnFsivlsCkjSOUcgWNUnte">刷新页面限制弹窗曝光</a>
   * @return [triggerSource,expResult]
   */
  private List<String> overrideTriggerSource(String triggerSource, Long userId, String deviceToken, SourceType sourceType, Long build,
      HomePageType homePageType) {
    return overrideTriggerSourceCommon(triggerSource, userId, deviceToken, sourceType, build, homePageType, false);
  }

  /**
   * 判断是否命中首页或下单页限频实验，并重写triggerSource (预分流版本)<br/>
   * <a href="https://fintopia.feishu.cn/wiki/MQVrwCnFsivlsCkjSOUcgWNUnte">刷新页面限制弹窗曝光</a>
   * @return [triggerSource,expResult]
   */
  private List<String> overrideTriggerSourcePrediversion(String triggerSource, Long userId, String deviceToken, SourceType sourceType, Long build,
      HomePageType homePageType) {
    return overrideTriggerSourceCommon(triggerSource, userId, deviceToken, sourceType, build, homePageType, true);
  }

  private List<String> overrideTriggerSourceCommon(String triggerSource, Long userId, String deviceToken, SourceType sourceType, Long build,
      HomePageType homePageType, boolean isPrediversion) {
    if (homePageType == null) {
      return Arrays.asList(null, null);
    }
    if (userId == null) {
      return Arrays.asList(null, null);
    }
    ExpUser expUser = ExpUser.builder()
        .userId(userId)
        .sourceType(sourceType)
        .versionBuild(build)
        .build();
    final String expKey;
    final boolean inExpGroup;
    final String expResult;
    switch (homePageType) {
      case HOME_PAGE_FOR_LEVEL_1:
        expKey = "technology-other-abroad-loan_all-enableHomeResourceRefreshLimit";
        if (isPrediversion) {
          inExpGroup = expPreDiversionClient.getBoolean(expKey, expUser, false);
        } else {
          inExpGroup = expDiversionClient.getBoolean(expKey, expUser, false);
        }
        expResult = String.valueOf(inExpGroup);
        break;
      case HOME_PAGE_FOR_LEVEL_2:
        expKey = "technology-other-abroad-loan_all-subHomeResourceRefreshLimitConfigFlag";
        if (isPrediversion) {
          expResult = expPreDiversionClient.getString(expKey, expUser, "ALWAYS_SHOW");
        } else {
          expResult = expDiversionClient.getString(expKey, expUser, "ALWAYS_SHOW");
        }
        inExpGroup = StringUtils.isNotBlank(expResult) && Arrays.asList("DISABLE_REFRESH", "ENABLE_REFRESH").contains(expResult);
        break;
      default:
        inExpGroup = false;
        expResult = null;
        break;
    }
    return Arrays.asList(inExpGroup ? triggerSource : null, expResult);
  }


  public void preProcessBeforeBuildResponse(HomePageContext homePageContext) {
    // 1. 顺序调用 HomepageContext 处理器
    invokeHomepageContextProcessor(homePageContext);

    // 2. 保存额度快照数据
    creditAwareService.trySaveCreditSnapshot(homePageContext.getStatus(), homePageContext.getUserCreditsContext());

    // 3. 版本不满足虚拟额度拆单的话失效虚拟额度
    checkBuildAndDisableVirtualCredits(homePageContext);
  }

  private void checkBuildAndDisableVirtualCredits(HomePageContext homePageContext) {
    if (Objects.nonNull(homePageContext.getUserCreditsContext()) && Objects.nonNull(homePageContext.getUserCreditsContext().getCreditsInfoVO())) {
      Boolean disableVirtualCreditsSuccess = loanUserVirtualCreditsService.checkBuildAndDisableVirtualCredits(homePageContext.getUserDeviceContextVO().getBuild(),
          homePageContext.getLoanAccountId(), homePageContext.getUserCreditsContext().getCreditsInfoVO().creditsQuota);
      if (disableVirtualCreditsSuccess) {
        RemainCreditsVO remainCreditsVO = loanCreditsQuotaService.getHomepageRemainCredits(homePageContext.getLoanAccountVO());
        // flush context
        homePageContext.flushContext(remainCreditsVO);
        log.info("build:{} disableVirtualCredits success, accountId:{}", homePageContext.getUserDeviceContextVO().getBuild(), homePageContext.getLoanAccountId());
      }
    }
  }

  private void invokeHomepageContextProcessor(HomePageContext homePageContext) {
    IHomepageContextProcessorSelector selector = statusContextProcessorMap.get(homePageContext.getStatus());
    List<HomePageContextProcessorType> contextProcessors = selector.getContextProcessors();
    contextProcessors.forEach(item -> {
      try {
        homepageHomeContextProcessorMap.get(item).processHomeContext(homePageContext);
      } catch (Exception e) {
        log.error("homepage error, userId:{}, status:{}", homePageContext.getUserId(), homePageContext.getStatus(), e);
      }
    });
  }

  public void postProcessAfterBuildResponse(HomepageResponseV5 homepageResponseV5, HomePageContext homePageContext) {
    for (IHomepageResponsePostProcessor processor : responsePostProcessorList) {
      processor.afterProcess(homepageResponseV5, homePageContext);
    }
  }
}
