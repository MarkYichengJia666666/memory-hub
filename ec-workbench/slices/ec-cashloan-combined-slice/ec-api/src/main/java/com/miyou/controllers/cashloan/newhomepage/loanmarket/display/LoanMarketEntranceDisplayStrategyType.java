package com.miyou.controllers.cashloan.newhomepage.loanmarket.display;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageDisplayStatusV5;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 贷超入口展示策略类型，与具体 processor 一一对应。 用于在请求资源位时，按当前 display_status 选出需执行的策略并聚合结果。
 */
public enum LoanMarketEntranceDisplayStrategyType {

  /**
   * 被拒状态主卡片/按钮（RejectCardProcessor）
   */
  REJECT_STATUS(Collections.singleton(IDNHomepageDisplayStatusV5.REJECTED), Collections.emptySet()),

  /**
   * 可重新提交授信主卡片链接（CanReapplyProcessor）
   */
  CAN_REAPPLY(Collections.singleton(IDNHomepageDisplayStatusV5.ENABLE_REAPPLIED), Collections.emptySet()),

  /**
   * 贷超卡片（LoanMarketProcessor）& 贷超用户信息 V3（LoanMarketInfoV3Processor）& 贷超中通位入口（LoanMarketSmeEntranceMiddleInfoProcessor）
   */
  LOAN_MARKET_V3(Collections.singleton(IDNHomepageDisplayStatusV5.REJECTED),
      new HashSet<>(Arrays.asList(IDNHomepageLoanStatusV5.OVERDUE, IDNHomepageLoanStatusV5.RELOAN_OVERDUE)));

  private final Set<IDNHomepageDisplayStatusV5> supportedDisplayStatusSet;

  private final Set<IDNHomepageLoanStatusV5> supportedLoanStatusSet;

  LoanMarketEntranceDisplayStrategyType(Set<IDNHomepageDisplayStatusV5> supportedDisplayStatusSet,
      Set<IDNHomepageLoanStatusV5> supportedLoanStatusSet) {
    this.supportedDisplayStatusSet = supportedDisplayStatusSet;
    this.supportedLoanStatusSet = supportedLoanStatusSet;
  }

  public static List<LoanMarketEntranceDisplayStrategyType> getSupportedStrategyTypes(IDNHomepageLoanStatusV5 loanStatus) {
    return Arrays.stream(LoanMarketEntranceDisplayStrategyType.values())
        .filter(strategy -> strategy.supportedStatus(loanStatus))
        .collect(Collectors.toList());
  }

  private boolean supportedStatus(IDNHomepageLoanStatusV5 loanStatus) {
    return supportedDisplayStatusSet.contains(loanStatus.displayStatusV5) || supportedLoanStatusSet.contains(loanStatus);
  }
}
