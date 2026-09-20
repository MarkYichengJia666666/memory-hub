package com.yqg.core.service.risk.facade.dwlog;

import com.yqg.core.service.risk.facade.dwlog.PanoramaRiskDwLogCommon.CreditsQuotaInfo;
import com.yqg.core.service.risk.facade.dwlog.PanoramaRiskDwLogCommon.PanoramaUserGroupRiskValue;
import com.yqg.core.service.risk.facade.dwlog.PanoramaRiskDwLogCommon.PanoramaUserTypeRiskValue;
import com.yqg.core.util.log.PanoramaBaseDwLogVO;

public class PanoramaRiskCompleteDwLogVO extends PanoramaBaseDwLogVO {

  public Long ecRiskId;
  public Long traceId;
  public String riskTypeCode;
  public String riskTypeName;
  public Long orderId;
  public Boolean traceResult;
  public String creditStatus;
  public String rejectType;
  public Long timeReapply;
  public String multiLoanStatus;
  public String revolvingDay;
  public Boolean rejectOrder;
  public PanoramaUserGroupRiskValue userGroup;
  public PanoramaUserTypeRiskValue userType;
  public String afRank;
  public String creditCategory;
  public CreditsQuotaInfo creditsQuota;
  public String productTag;
  public String rateTag;
  public Boolean qualifiedLoanMarketEntranceByRisk;
}
