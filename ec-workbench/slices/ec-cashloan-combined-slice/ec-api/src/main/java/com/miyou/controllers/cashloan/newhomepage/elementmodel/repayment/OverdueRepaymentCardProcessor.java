package com.miyou.controllers.cashloan.newhomepage.elementmodel.repayment;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContext;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContextFactory;
import com.miyou.controllers.cashloan.repayment.card.element.provider.RepaymentCardElementProvider;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementModuleType;
import com.miyou.controllers.cashloan.utilities.HomepageContentTool;
import com.yqg.core.service.loanmarket.LoanMarketOverdueExperimentDecisionService;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OverdueRepaymentCardProcessor extends AbstractPageCardV3Processor {

  @Autowired
  private RepaymentContextFactory repaymentContextFactory;
  @Autowired
  private RepaymentCardElementProvider repaymentCardElementProvider;
  @Autowired
  private HomepageContentTool homepageContentTool;
  @Autowired
  private LoanMarketOverdueExperimentDecisionService loanMarketOverdueExperimentDecisionService;

  @Override
  public void process(PageCardV3VO fieldsInfo, HomePageContext homePageContext) {
    boolean cannotReapply = homepageContentTool.isNotQualifiedToReapply(homePageContext);
    LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext,
        !cannotReapply);
    // 风控通过&在贷&有逾期,需要逾期天数(当前+历史)&版本满足限制
    if (loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk
        && homePageContext.getHomepageUserParamsVO().isOverdue()
        && homepageContentTool.checkUserOverdueDaysWithHistory(homePageContext)) {
      // 贷超入口拓展逾期实验分流
      boolean shownLoanMarket = loanMarketOverdueExperimentDecisionService.decide(homePageContext.getUserId(),
          homePageContext.getUserDeviceContextVO().getBuild(),
          homePageContext.getUserDeviceContextVO().getSourceType());
      if (shownLoanMarket) {
        // 展示贷超卡片，隐藏还款卡片
        return;
      }
    }
    RepaymentContext ctx = repaymentContextFactory.buildRepaymentContext(homePageContext);
    if (ctx.hasOutstanding()) {
      fieldsInfo.forceAddAllElements(ElementModuleType.REPAYMENT_CARD, repaymentCardElementProvider.buildRepaymentCard(ctx));
    }
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.OVERDUE_REPAYMENT_CARD;
  }
}
