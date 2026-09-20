package com.yqg.core.service.risk.facade.dwlog;

import java.math.BigDecimal;

public class PanoramaRiskDwLogCommon {

  public static class PanoramaUserGroupRiskValue {
    public String oldUserGroupBeforeRisk;
    public String newUserGroupBeforeRisk;
    public String userGroupAfterRisk;
  }

  public static class PanoramaUserTypeRiskValue {
    public String oldUserTypeBeforeRisk;
    public String newUserTypeBeforeRisk;
    public String userTypeAfterRisk;
  }

  public static class CreditsQuotaInfo {
    public BigDecimal fixedRiskCreditsQuota;
    public BigDecimal experimentRiskCreditsQuota;
    public BigDecimal increaseRiskCreditsQuota;
    public Long increaseQuotaExpireTime;
  }
}
