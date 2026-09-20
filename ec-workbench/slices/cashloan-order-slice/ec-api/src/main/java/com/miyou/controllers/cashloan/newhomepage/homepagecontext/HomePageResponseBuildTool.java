package com.miyou.controllers.cashloan.newhomepage.homepagecontext;

import static com.yqg.core.configure.devconfig.DynamicThreadExecutorConfiguration.HOME_PAGE_EXECUTOR;

import com.google.common.collect.ArrayListMultimap;
import com.miyou.controllers.cashloan.newhomepage.HomePageResponseFields;
import com.miyou.controllers.cashloan.newhomepage.HomepageMonitorService;
import com.miyou.controllers.cashloan.newhomepage.IHomePageResponseFactory;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.monitor.HomePageBuildContextMonitorSnapshot;
import com.miyou.controllers.cashloan.newhomepage.enums.FactoryThreadGroup;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageProcessorType;
import com.miyou.controllers.cashloan.response.v5.HomepageResponseV5;
import com.yqg.core.service.pointgrowth.vo.PointGrowthSummaryVo;
import com.yqg.core.userflow.domain.point.model.PointEntry;
import com.yqg.core.userflow.domain.point.model.PointScene;
import com.yqg.core.userflow.infrastructure.adapter.IPointAdapter;
import com.yqg.core.service.cashloan.homepage.enums.HomePageScene;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import com.yqg.core.util.ScopeCompletableFutures;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * HomepageResponseV5 的构建类 聚合了 所有的IHomePageResponseFactory 处理流程： 1、准备上下文 2、遍历所有的IHomePageResponseFactory，调用getResult方法获取结果，最后设值
 */
@Service
@Slf4j
public class HomePageResponseBuildTool {

  @Resource(name = HOME_PAGE_EXECUTOR)
  private ExecutorService homepageExecutor;
  @Autowired
  private HomepageMonitorService homepageMonitorService;
  @Autowired
  private HomepageFactoryFilter homepageFactoryFilter;
  @Autowired
  private HomePageContextManager homePageContextManager;
  @Autowired
  private IPointAdapter pointAdapter;


  public HomepageResponseV5 buildResponse(
      IDNHomepageLoanStatusV5 status,
      UserDeviceContextVO userDeviceContextVO,
      HomepageUserParamsVO paramsVO,
      HomePageType homePageType,
      HomePageScene homePageScene,
      String triggerSource
  ) {
    // 1. 构造首页响应体 - 初始化上下文
    HomePageContext homePageContext = homePageContextManager.prepareContext(status, userDeviceContextVO, paramsVO, homePageType,
        homePageScene, triggerSource);

    // 2. 构造首页响应体 - 预处理
    homePageContextManager.preProcessBeforeBuildResponse(homePageContext);

    // 3. 构造首页响应体 - 核心逻辑
    HomepageResponseV5 homepageResponseV5 = doBuildResponse(homePageContext);

    // 4. 构造首页响应体 - 后处理
    homePageContextManager.postProcessAfterBuildResponse(homepageResponseV5, homePageContext);
    // 5. 构建用户积分模块
    enrichPointSummary(homePageContext, homepageResponseV5);
    // 6. 首页异步埋点（仅登录用户）
    if (homePageContext.getUserId() != null) {
      homepageMonitorService.logHomePageBuildContext(HomePageBuildContextMonitorSnapshot.from(homePageContext));
    }
    return homepageResponseV5;
  }

  /**
   * 5. 构建用户积分模块。
   *
   * <p>首页仅透出轻量摘要，不再依赖 EC 本地积分跳转配置。</p>
   */
  private void enrichPointSummary(HomePageContext homePageContext, HomepageResponseV5 homepageResponseV5) {
    Long userId = homePageContext.getUserId();
    if (userId == null) {
      return;
    }
    if (!pointAdapter.shouldShowModule(userId)) {
      return;
    }
    try {
      PointGrowthSummaryVo pointSummary = toHomeSummary(pointAdapter.queryEntry(userId, PointScene.HOME));
      if (homepageResponseV5.userInfo != null) {
        homepageResponseV5.userInfo.setPointSummary(pointSummary);
      }
    } catch (Exception e) {
      log.warn("homepage pointSummary enrich failed userId={}", userId, e);
    }
  }

  private static PointGrowthSummaryVo toHomeSummary(PointEntry entry) {
    if (entry == null) {
      return null;
    }
    PointGrowthSummaryVo vo = new PointGrowthSummaryVo();
    vo.setShowEntry(entry.isVisible());
    vo.setLevelIconUrl(entry.getLevelIconUrl());
    vo.setBenefitCount(entry.getBenefitCount());
    vo.setJumpUrl(null);
    return vo;
  }

  @NotNull
  private HomepageResponseV5 doBuildResponse(HomePageContext homePageContext) {
    HomepageResponseV5 res = new HomepageResponseV5();
    long start = Clock.now();
    try {
      ArrayListMultimap<FactoryThreadGroup, HomepageProcessorType> homepageProcessorTypeList = homepageFactoryFilter.getHomepageProcessorTypeGroup(
          homePageContext);
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      Arrays.stream(FactoryThreadGroup.values()).forEach(group -> {
        // 透传请求 Scope，保证首页并发池内实验 force / ImpliedContext 可读
        CompletableFuture<Void> future = ScopeCompletableFutures.runAsync(() -> {
          long s = Clock.now();
          try {
            // 在这里执行实际的处理器逻辑
            doSetResponse(homePageContext, res, homepageProcessorTypeList.get(group));
          } finally {
            // 清除线程本地变量
            log.info("build homepage response success, group:{}, cost:{}", group, Clock.now() - s);
          }
        }, homepageExecutor);
        futures.add(future);
      });
      // 等待所有任务完成
      try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      } catch (Exception e) {
        throw EcException.error(EcExceptionType.COMMON_CUSTOM_MESSAGE, "homepage error", e);
      }
      homepageMonitorService.logHomeApiCost(Clock.now() - start, true);
      return res;
    } catch (Exception e) {
      throw EcException.error("homepage error", e);
    }
  }

  private void doSetResponse(HomePageContext homePageContext, HomepageResponseV5 res, List<HomepageProcessorType> types) {
    for (HomepageProcessorType type : types) {
      try {
        IHomePageResponseFactory factory = homepageFactoryFilter.getFactoryByType(type);
        Object value = factory.getResult(homePageContext);
        factory.setValue(res, (HomePageResponseFields) value);
      } catch (Exception e) {
        log.error("business scenario:new homepage error; userId:{}, status:{}; stack trace:", homePageContext.getUserId(),
            homePageContext.getStatus(), e);
      }
    }
  }


  public Map<String, String> getHomePointInfo(IDNHomepageLoanStatusV5 status, UserDeviceContextVO userDeviceContextVO,
      HomepageUserParamsVO paramsVO) {
    HomePageContext homePageContext = homePageContextManager.prepareContextPreDiversion(status, userDeviceContextVO, paramsVO, HomePageType.NONE,
        HomePageScene.DEFAULT, null);
    HomepageResponseV5 homepageResponseV5 = new HomepageResponseV5();
    doSetResponse(homePageContext, homepageResponseV5, Collections.singletonList(HomepageProcessorType.BURIED_PINT_INTO));
    return homepageResponseV5.reportContentMap;
  }
}
