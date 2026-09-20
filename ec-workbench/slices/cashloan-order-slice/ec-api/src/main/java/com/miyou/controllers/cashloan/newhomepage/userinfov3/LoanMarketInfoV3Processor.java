package com.miyou.controllers.cashloan.newhomepage.userinfov3;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoV3ProcessorType;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayDecision;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyService;
import com.miyou.controllers.cashloan.newhomepage.loanmarket.display.LoanMarketEntranceDisplayStrategyType;
import com.miyou.controllers.cashloan.response.v5.pagev3.PageUserInfoV3Response;
import com.miyou.controllers.cashloan.response.v5.pagev3.userinfo.LoanMarketInfoV3;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.service.loanmarket.LoanMarketOverdueExperimentDecisionService;
import com.yqg.core.service.loanmarket.config.LoanMarketConfig;
import com.yqg.core.service.loanmarket.enums.LoanMarketDisplayStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description
 * @Author: lihancock
 * @Email: wenyaoli@fintopia.tech
 * @Date: 2025/7/28 16:59
 */
@Slf4j
@Component
public class LoanMarketInfoV3Processor extends AbstractHomepageUserInfoV3Processor {

  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private LoanMarketConfig loanMarketConfig;
  @Autowired
  private LoanMarketEntranceDisplayStrategyService loanMarketEntranceDisplayStrategyService;
  @Autowired
  private LoanMarketOverdueExperimentDecisionService loanMarketOverdueExperimentDecisionService;
  @Override
  public void process(PageUserInfoV3Response fieldsInfo, HomePageContext homePageContext) {
    try {
      LoanMarketEntranceDisplayDecision loanMarketDecision = loanMarketEntranceDisplayStrategyService.evaluate(homePageContext,
          LoanMarketEntranceDisplayStrategyType.LOAN_MARKET_V3, false);
      if (!loanMarketDecision.isShowLoanMarketEntrance()) {
        return;
      }

      // 逾期用户场景，注意和非逾期用户走的是不同实验，需要兜底return，否则会影响后面非逾期用户的进组
      if (loanMarketDecision.isOverdue()) {
        Long userId = homePageContext.getUserId();
        Long build = homePageContext.getUserDeviceContextVO().getBuild();
        // 贷超入口拓展逾期实验分流
        boolean shownLoanMarket = loanMarketOverdueExperimentDecisionService.decide(userId, build,
            homePageContext.getUserDeviceContextVO().getSourceType());
        if (!shownLoanMarket) {
          return;
        }
        boolean displayInH5 = build != null && build >= 38410L;
        fieldsInfo.setLoanMarketInfo(buildLoanMarketInfoV3(displayInH5, LoanMarketDisplayStrategy.LOAN_MARKET_LIST_BANNER_DOWN));
        return;
      }
      //无在贷场景+永拒
      //版本控制（已在策略判断）
      LoanMarketDisplayStrategy displayStrategy =
          homepageContentTool.getLoanMarketCardDisplayResult(homePageContext.getUserId(),
              homePageContext.getUserDeviceContextVO().getBuild(),
              homePageContext.getUserDeviceContextVO().getSourceType());
      //进入实验组才进行H5和Native分流
      if (displayStrategy.strategyInExprGroup()) {
        boolean displayInH5 = homepageContentTool.getLoanMarketDisplayInH5Result(
            homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild(), homePageContext.getUserDeviceContextVO().getSourceType());
        fieldsInfo.setLoanMarketInfo(buildLoanMarketInfoV3(displayInH5, displayStrategy));
      }
    } catch (Exception e) {
      log.warn("LoanMarketInfoV3Processor process error, ", e);
    }
  }

  private LoanMarketInfoV3 buildLoanMarketInfoV3(boolean displayInH5, LoanMarketDisplayStrategy displayStrategy) {
    return LoanMarketInfoV3.builder()
        .displayInH5(displayInH5)
        .displayStrategy(displayStrategy)
        .loanListH5Url(loanMarketConfig.getLoanMarketDisplayH5UrlPrefix())
        .h5Height(loanMarketConfig.getLoanMarketH5CardHeight())
        .build();
  }

  @Override
  public HomepageUserInfoV3ProcessorType getProcessorType() {
    return HomepageUserInfoV3ProcessorType.LOAN_MARKET_INFO_V3;
  }
}
