package com.miyou.controllers.cashloan.newhomepage.elementmodel.ordersupplement;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.yqg.core.service.cashloan.homepage.config.OrderProgressStepDTO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.yqg.core.service.cashloan.homepage.config.ApiRequestPath;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementType;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.ProgressBarElement;
import com.yqg.core.service.cashloan.ordercenter.CashLoanUserSupplementCreateOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.LoanUserSupplementCreateOrderVO;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.*;
import static com.yqg.ec.common.enums.order.CashLoanOrderStatus.UNDONE_STATUSES;

@Service
public class SupplementReview extends AbstractPageCardV3Processor {
  @Autowired
  private CashLoanUserSupplementCreateOrderService cashLoanUserSupplementCreateOrderService;

  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
        .id(MAIN_BUTTON)
        .text(TT.gen("审核处理中"))
        .build());
    pageCardV3VO.addElementForMainCard(ElementBuilder.image()
        .id(MAIN_ICON)
        .imageUrl(elementConfig.getOrderProgressStepConfigByElementId(MAIN_ICON.name()))
        .build());

    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(TITLE)
        .text(TT.gen(elementConfig.getOrderProgressStepConfigByElementId(TITLE.name())))
        .build());

    addProgressBarElement(pageCardV3VO, homePageContext.getLoanAccountId());
  }

  private List<TT> getAllOrderProgressStep(Map<CashLoanOrderStatus, OrderProgressStepDTO> orderProgressStep) {
    return Arrays.stream(UNDONE_STATUSES)
        .map(orderProgressStep::get)
        .map(stepInfo -> stepInfo.title)
        .map(TT::gen)
        .collect(Collectors.toList());
  }

  private void addProgressBarElement(PageCardV3VO pageCardV3VO, Long loanAccountId) {

    Map<CashLoanOrderStatus, OrderProgressStepDTO> orderProgressStep = elementConfig.getOrderProgressStep();
    List<TT> allStepTitleList = getAllOrderProgressStep(orderProgressStep);
    OrderProgressStepDTO currentStep = orderProgressStep.get(CashLoanOrderStatus.RESERVE);

    Long waitSeconds = elementConfig.getOrderProgressStepWaitSeconds(CashLoanOrderStatus.RESERVE);
    LoanUserSupplementCreateOrderVO supplementCreateOrderVO = cashLoanUserSupplementCreateOrderService.fetchLastedByAccountId(loanAccountId);
    Long startTime = supplementCreateOrderVO.timeUpdated;
    Long passSeconds = (long) Math.toIntExact((Clock.now() - startTime) / Clock.MILLS_PER_SECOND);
    pageCardV3VO.addElementForMainCard(ElementBuilder.progressBar()
        .id(MainCardElementId.LOAN_PROGRESS_INFO)
        .type(ElementType.PROGRESSBAR)
        .elementParam(ProgressBarElement.ProgressBarParam.builder()
            .nodes(allStepTitleList)
            .currentNode(TT.gen(currentStep.title))
            .passTime(passSeconds)
            .waitingSecond(waitSeconds)
            .apiRequestPath(ApiRequestPath.REVIEW)
            .build())
        .build());
  }


  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.ORDER_SUPPLEMENT_REVIEW;
  }
}
