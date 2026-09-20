package com.miyou.controllers.cashloan.newhomepage.elementmodel.repayment;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContext;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContextFactory;
import com.miyou.controllers.cashloan.repayment.card.element.provider.MainCardElementProvider;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementModuleType;
import com.yqg.core.service.cashloan.apireturncredit.ApiReturnCreditEligibilityCommand;
import com.yqg.core.service.cashloan.apireturncredit.ApiReturnCreditEligibilityService;
import com.yqg.core.service.cashloan.apireturncredit.ApiReturnCreditEligibilitySnapshot;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MainCardRepaymentInfoProcessor extends AbstractPageCardV3Processor {

  @Autowired
  private RepaymentContextFactory repaymentContextFactory;
  @Autowired
  private MainCardElementProvider mainCardElementProvider;
  @Autowired
  private ApiReturnCreditEligibilityRequestFactory eligibilityRequestFactory;
  @Autowired
  private ApiReturnCreditEligibilityService eligibilityService;
  @Autowired
  private ApiReturnCreditMainCardDecorator mainCardDecorator;

  @Override
  public void process(PageCardV3VO fieldsInfo, HomePageContext homePageContext) {
    RepaymentContext ctx = repaymentContextFactory.buildRepaymentContext(homePageContext);
    if (ctx.hasOutstanding()) {
      List<IElement> mainCardElements = mainCardElementProvider.buildMainCard(ctx);
      if (isTargetStatus(homePageContext.getStatus())) {
        mainCardElements = tryDecorateMainCard(homePageContext, ctx, mainCardElements);
      }
      fieldsInfo.forceAddAllElements(ElementModuleType.MAIN_CARD, mainCardElements);
    }
  }

  private List<IElement> tryDecorateMainCard(
      HomePageContext homePageContext, RepaymentContext ctx, List<IElement> mainCardElements) {
    try {
      ApiReturnCreditEligibilityCommand command =
          eligibilityRequestFactory.create(homePageContext, ctx);
      ApiReturnCreditEligibilitySnapshot snapshot = eligibilityService.evaluate(command);
      if (!isOverrideMode(snapshot)) {
        return mainCardElements;
      }
      return mainCardDecorator.decorate(mainCardElements, snapshot);
    } catch (Exception e) {
      log.warn("API return credit main card fallback, userId:{}", homePageContext.getUserId(), e);
      return mainCardElements;
    }
  }

  private boolean isOverrideMode(ApiReturnCreditEligibilitySnapshot snapshot) {
    return snapshot != null
        && (snapshot.getCardMode()
        == ApiReturnCreditEligibilitySnapshot.CardMode.NEED_SUPPLEMENT
        || snapshot.getCardMode()
        == ApiReturnCreditEligibilitySnapshot.CardMode.WAIT_FIRST_REPAYMENT);
  }

  private boolean isTargetStatus(IDNHomepageLoanStatusV5 status) {
    return status == IDNHomepageLoanStatusV5.READY
        || status == IDNHomepageLoanStatusV5.RELOAN_READY;
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.MAIN_CARD_REPAYMENT_INFO;
  }
}
