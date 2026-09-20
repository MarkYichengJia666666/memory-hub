package com.miyou.controllers.cashloan.newhomepage.appresource;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayDecision;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyService;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.HomePageType;
import com.yqg.ec.common.constant.AppResourceExtraParam;
import java.util.Map;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoanMarketDialogService {

  @Resource
  private ExpDiversionClient expDiversionClient;
  @Resource
  private LoanMarketEntranceDisplayStrategyService loanMarketEntranceDisplayStrategyService;

  /**
   * 填充贷超升级APP弹窗参数，只在home使用
   *
   * @param homePageContext
   * @param params
   */
  public void setLoanMarketUpdateDialogParam(HomePageContext homePageContext, Map<String, Object> params) {
    LoanMarketEntranceDisplayDecision loanMarketDecision = canShowLoanMarketUpdateDialog(homePageContext);
    AppResourceExtraParam.DISPLAY_LOAN_MARKET.setValue(params, loanMarketDecision.isShowLoanMarketEntrance());
    AppResourceExtraParam.LOAN_MARKET_RULE.setValue(params,
        loanMarketDecision.getLoanMarketRule() == null ? "NULL" : loanMarketDecision.getLoanMarketRule().name());

  }

  private LoanMarketEntranceDisplayDecision canShowLoanMarketUpdateDialog(HomePageContext homePageContext) {
    // 入组时机：进入首页
    if (homePageContext.getHomePageType() != HomePageType.HOME_PAGE_FOR_LEVEL_1) {
      // 非home
      return LoanMarketEntranceDisplayDecision.notShow();
    }
    // 资源位参数表达“展示贷超入口的可能性”，入组后跳过关于贷超的版本与实验判断
    LoanMarketEntranceDisplayDecision loanMarketDecision = loanMarketEntranceDisplayStrategyService.getDecisionMatchAll(homePageContext,
        true);
    if (!loanMarketDecision.isShowLoanMarketEntrance()) {
      // 人群不符合展示贷超入口
      return LoanMarketEntranceDisplayDecision.notShow();
    }
    String expResult = expDiversionClient.getResult("braavos-other-abroad-loan_all-loanmarketupgradeAPPpopup");
    if (!"B".equals(expResult)) {
      // 非实验组
      return LoanMarketEntranceDisplayDecision.notShow();
    }
    return loanMarketDecision;
  }
}
