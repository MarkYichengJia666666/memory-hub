package com.miyou.controllers.cashloan.newhomepage.loanmarket;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.response.v5.pagev3.HomepageVersion;
import com.yqg.core.model.sql.loanmarket.enums.LoanMarketReportType;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.monitor.BaseMonitorService;
import com.yqg.core.service.monitor.MonitorMeasurementName;
import org.influxdb.dto.Point;
import org.springframework.stereotype.Service;

/**
 * 贷超入口曝光监控服务
 */
@Service
public class LoanMarketEntranceMonitorService extends BaseMonitorService {

  /**
   * 记录贷超入口曝光
   *
   * @param homePageContext 首页上下文，用于获取 userId / homepageStatus / 首页版本
   * @param reportType      上报类型，与 Sensors 埋点的 LoanMarketReportType 一致
   * @param processorClass  调用方的 Class，用于记录处理器名称（取 simpleName）
   */
  public void logLoanMarketEntranceExposure(HomePageContext homePageContext,
      LoanMarketReportType reportType, Class<?> processorClass) {
    Long userId = homePageContext.getUserId();
    IDNHomepageLoanStatusV5 homepageStatus = homePageContext.getStatus();
    HomepageVersion version = homePageContext.getHomepageV3ExperimentContext().getHomepageDisplayVersion();
    String style = version != null ? version.name() : "UNKNOWN";
    String processor = processorClass.getSimpleName();

    writePoint(() -> Point.measurement(MonitorMeasurementName.LOAN_MARKET_ENTRANCE_EXPOSURE.name)
        .tag("style", style)
        .tag("reportType", reportType.name())
        .tag("processor", processor)
        .tag("homepageStatus", homepageStatus != null ? homepageStatus.name() : "UNKNOWN")
        .addField("userId", userId)
        .build());
  }
}
