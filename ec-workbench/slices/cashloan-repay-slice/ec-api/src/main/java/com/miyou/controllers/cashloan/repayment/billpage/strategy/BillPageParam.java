package com.miyou.controllers.cashloan.repayment.billpage.strategy;

import com.miyou.controllers.cashloan.enums.InstalmentDisplayStatus;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionCheckResult;
import com.yqg.core.util.env.EnvironmentInfo;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.translation.client.utils.TT;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder(buildMethodName = "create")
@AllArgsConstructor
public class BillPageParam {
  public final Long userId;
  public final Long loanAccountId;
  public final SDKType sdkType;
  public final Long build;
  public final String deviceToken;
  public final EnvironmentInfo environmentInfo;
  public final boolean isLogin;
  /**
   * 展示什么状态的账单？已结清、未结清
   */
  public final InstalmentDisplayStatus instalmentDisplayStatus;
  /**
   * 手动减免信息
   */
  public final ManualReductionCheckResult manualReductionCheckResult;

  public final TT fraudAlert;
}
