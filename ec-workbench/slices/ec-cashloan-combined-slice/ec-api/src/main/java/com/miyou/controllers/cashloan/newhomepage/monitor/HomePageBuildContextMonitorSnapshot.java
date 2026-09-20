package com.miyou.controllers.cashloan.newhomepage.monitor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageDisplayStatusV5;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.UserCreditsContext;
import com.yqg.core.service.loan.vo.LoanAccountDetailsSimpleVO;
import java.util.Objects;
import lombok.Getter;

/**
 * 首页 buildResponse 埋点用的 context 快照，供异步任务使用。
 */
@Getter
public final class HomePageBuildContextMonitorSnapshot {

  private final String displayStatusV5;
  private final String displayStatusV5Desc;
  private final String loanStatusV5;
  private final String loanStatusV5Desc;
  private final Long userId;
  private final Long loanAccountId;
  /**
   * 用户是否已完件。口径与 AuthService.isAuthFinished 一致：timeFinished != null。
   */
  private final boolean authFinished;

  private HomePageBuildContextMonitorSnapshot(
      String displayStatusV5,
      String displayStatusV5Desc,
      String loanStatusV5,
      String loanStatusV5Desc,
      Long userId,
      Long loanAccountId,
      boolean authFinished) {
    this.displayStatusV5 = displayStatusV5;
    this.displayStatusV5Desc = displayStatusV5Desc;
    this.loanStatusV5 = loanStatusV5;
    this.loanStatusV5Desc = loanStatusV5Desc;
    this.userId = userId;
    this.loanAccountId = loanAccountId;
    this.authFinished = authFinished;
  }

  public static HomePageBuildContextMonitorSnapshot from(HomePageContext homePageContext) {
    IDNHomepageLoanStatusV5 status = homePageContext.getStatus();
    IDNHomepageDisplayStatusV5 displayStatus = status.displayStatusV5;
    return new HomePageBuildContextMonitorSnapshot(
        displayStatus.name(),
        displayStatus.getDescription(),
        status.name(),
        status.getDescription(),
        homePageContext.getUserId(),
        homePageContext.getLoanAccountId(),
        resolveAuthFinished(homePageContext));
  }

  /**
   * 从首页已加载的 LoanAccountDetailsSimpleVO 取值，避免主线程额外 IO。
   * 未登录或 details 缺失视为未完件。
   */
  private static boolean resolveAuthFinished(HomePageContext homePageContext) {
    UserCreditsContext creditsContext = homePageContext.getUserCreditsContext();
    if (creditsContext == null) {
      return false;
    }
    LoanAccountDetailsSimpleVO details = creditsContext.getLoanAccountDetailsSimpleVO();
    return details != null && details.timeFinished != null;
  }

  public long getUserIdOrZero() {
    return Objects.isNull(userId) ? 0L : userId;
  }

  public long getLoanAccountIdOrZero() {
    return Objects.isNull(loanAccountId) ? 0L : loanAccountId;
  }
}
