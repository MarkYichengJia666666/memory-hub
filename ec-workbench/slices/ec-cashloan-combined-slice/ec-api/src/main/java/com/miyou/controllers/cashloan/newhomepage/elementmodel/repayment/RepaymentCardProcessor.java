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
import com.yqg.core.service.loanmarket.enums.LoanMarketDisplayStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RepaymentCardProcessor extends AbstractPageCardV3Processor {

  @Autowired
  private RepaymentContextFactory repaymentContextFactory;
  @Autowired
  private RepaymentCardElementProvider repaymentCardElementProvider;
  @Autowired
  private HomepageContentTool homepageContentTool;

  @Override
  public void process(PageCardV3VO fieldsInfo, HomePageContext homePageContext) {
    RepaymentContext ctx = repaymentContextFactory.buildRepaymentContext(homePageContext);
    if (ctx.hasOutstanding()) {
      fieldsInfo.forceAddAllElements(ElementModuleType.REPAYMENT_CARD, repaymentCardElementProvider.buildRepaymentCard(ctx));
    }
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.REPAYMENT_CARD;
  }
}
