package com.miyou.controllers.cashloan.newhomepage;

import static com.yqg.core.configure.devconfig.DynamicThreadExecutorConfiguration.HOME_PAGE_EXECUTOR;

import com.miyou.controllers.cashloan.newhomepage.monitor.HomePageBuildContextMonitorPointBuilder;
import com.miyou.controllers.cashloan.newhomepage.monitor.HomePageBuildContextMonitorSnapshot;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.monitor.BaseMonitorService;
import com.yqg.core.service.monitor.MonitorMeasurementName;
import com.yqg.core.service.monitor.RetentionPolicies;
import com.yqg.core.userflow.domain.user.model.UserAmountInfo;
import com.yqg.core.userflow.domain.user.service.IUserInfoService;
import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HomepageMonitorService extends BaseMonitorService {

  @Resource(name = HOME_PAGE_EXECUTOR)
  private ExecutorService homepageExecutor;

  @Autowired
  private IUserInfoService userInfoService;

  public void logHomeApiCost(long cost, boolean threadBuild) {
    writePoint(() -> Point.measurement(MonitorMeasurementName.HOME_API_BUILDER.name)
        .tag("threadBuild", threadBuild ? "B" : "A")
        .addField("cost", cost)
        .build(), RetentionPolicies.TWO_MONTH);
  }

  /**
   * 首页版本号解析结果监控，用于看板按版本/状态聚合。
   */
  public void logHomepageVersionResolve(Long userId, Long build, IDNHomepageLoanStatusV5 homepageStatus, String homepageVersion) {
    writePoint(() -> Point.measurement(MonitorMeasurementName.HOME_PAGE_VERSION_RESOLVE.name)
        .tag("homepageVersion", homepageVersion != null ? homepageVersion : "UNKNOWN")
        .tag("homepageStatus", homepageStatus != null ? homepageStatus.name() : "UNKNOWN")
        .addField("userId", userId == null ? 0L : userId)
        .addField("build", build == null ? 0L : build)
        .build());
  }

  /**
   * 首页 buildResponse 完成后异步记录展示态与用户额度（全量 UserAmountInfo 公开字段）。
   * 仅登录用户打点；未登录直接跳过，不占 homepageExecutor、不查额度。
   */
  public void logHomePageBuildContext(HomePageBuildContextMonitorSnapshot snapshot) {
    if (snapshot.getUserId() == null) {
      return;
    }
    try {
      homepageExecutor.execute(() -> {
        try {
          UserAmountInfo amount = userInfoService.getUserAmountInfoByAccountId(snapshot.getLoanAccountId());
          writePoint(
              () -> HomePageBuildContextMonitorPointBuilder.build(snapshot, amount),
              RetentionPolicies.ONE_MONTH);
        } catch (Exception e) {
          log.warn("homepage build context monitor failed, userId={}, loanAccountId={}",
              snapshot.getUserId(), snapshot.getLoanAccountId(), e);
        }
      });
    } catch (Exception e) {
      log.warn("homepage build context monitor task submit failed, userId={}, loanAccountId={}",
          snapshot.getUserId(), snapshot.getLoanAccountId(), e);
    }
  }
}
