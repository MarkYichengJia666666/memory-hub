package com.miyou.controllers.cashloan.newhomepage.monitor;

import com.yqg.core.service.monitor.MonitorMeasurementName;
import com.yqg.core.userflow.domain.user.model.UserAmountInfo;
import java.math.BigDecimal;
import java.util.Objects;
import org.influxdb.dto.Point;

public final class HomePageBuildContextMonitorPointBuilder {

  private HomePageBuildContextMonitorPointBuilder() {
  }

  public static Point build(HomePageBuildContextMonitorSnapshot snapshot, UserAmountInfo amount) {
    UserAmountInfo safeAmount = amount == null ? UserAmountInfo.empty() : amount;
    return Point.measurement(MonitorMeasurementName.HOME_PAGE_BUILD_CONTEXT.name)
        .tag("displayStatusV5", snapshot.getDisplayStatusV5())
        .tag("displayStatusV5Desc", snapshot.getDisplayStatusV5Desc())
        .tag("loanStatusV5", snapshot.getLoanStatusV5())
        .tag("loanStatusV5Desc", snapshot.getLoanStatusV5Desc())
        .addField("userId", snapshot.getUserIdOrZero())
        .addField("loanAccountId", snapshot.getLoanAccountIdOrZero())
        .addField("authFinished", snapshot.isAuthFinished())
        .addField("riskGrantedCredits", toFieldValue(safeAmount.getRiskGrantedCredits()))
        .addField("riskGrantedCreditsForVirtual", toFieldValue(safeAmount.getRiskGrantedCreditsForVirtual()))
        .addField("tempCreditsForCoupon", toFieldValue(safeAmount.getTempCreditsForCoupon()))
        .addField("couponCreditsExpiredTime", toFieldValue(safeAmount.getCouponCreditsExpiredTime()))
        .addField("tempCreditsForIncreaseCreditsRisk", toFieldValue(safeAmount.getTempCreditsForIncreaseCreditsRisk()))
        .addField("increaseCreditsExpiredTime", toFieldValue(safeAmount.getIncreaseCreditsExpiredTime()))
        .addField("virtualRiskFixedCredits", toFieldValue(safeAmount.getVirtualRiskFixedCredits()))
        .addField("usedCredits", toFieldValue(safeAmount.getUsedCredits()))
        .addField("totalAmount", toFieldValue(safeAmount.getTotalAmount()))
        .addField("totalAmountForVirtual", toFieldValue(safeAmount.getTotalAmountForVirtual()))
        .addField("realRemainingCredits", toFieldValue(safeAmount.getRealRemainingCredits()))
        .addField("remainingCredits", toFieldValue(safeAmount.getRemainingCredits()))
        .addField("realRemainingCreditsForVirtual", toFieldValue(safeAmount.getRealRemainingCreditsForVirtual()))
        .addField("remainingCreditsForVirtual", toFieldValue(safeAmount.getRemainingCreditsForVirtual()))
        .build();
  }

  private static long toFieldValue(Long value) {
    return Objects.isNull(value) ? 0L : value;
  }

  private static double toFieldValue(BigDecimal value) {
    return Objects.isNull(value) ? 0D : value.doubleValue();
  }
}
