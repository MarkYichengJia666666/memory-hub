package com.miyou.controllers.cashloan.newhomepage.elementmodel.orderstep;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.RedirectAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.ElementType;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.BaseElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.ButtonElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.element.ProgressBarElement;
import com.yqg.core.model.sql.cashloan.enums.BusinessType;
import com.yqg.core.service.bizcheck.BizCheckGroupStepService;
import com.yqg.core.service.bizcheck.vo.BizCheckGroupStepVO;
import com.yqg.core.service.cashloan.homepage.abtest.FraudWarningExperimentService;
import com.yqg.core.service.cashloan.homepage.config.NativePathConstant;
import com.yqg.core.service.cashloan.homepage.config.OrderProgressStepDTO;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageWholeProcessTool;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId.*;
import static com.yqg.ec.common.enums.order.CashLoanOrderStatus.UNDONE_STATUSES;

@Slf4j
@Service
public class OrderProgressStepProcessor extends AbstractPageCardV3Processor {
  @Autowired
  protected BizCheckGroupStepService bizCheckGroupStepService;
  @Autowired
  private HomepageWholeProcessTool homepageWholeProcessTool;
  @Autowired
  private FraudWarningExperimentService fraudWarningExperimentService;

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.ORDER_PROGRESS_STEP;
  }

  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    CashLoanOrderVO latestOrderVO = homePageContext.getUserCashLoanOrderContext().getLatestOrderVO();
    //TODO(gxs, --task=1250606 --user=郭晓帅 新首页UI/结构优化--下单之后 https://www.tapd.cn/53182677/s/5826330)拆分首页review状态，拆成订单和非订单
    //如果当前用户不存在最后一笔订单或者订单状态不是未打款，不返回进度条
    if (Objects.isNull(latestOrderVO) || !CashLoanOrderStatus.isUndoneStatusWithoutReady(latestOrderVO.status)) {
      log.info("not exist order or order status is error for order progress step processor, loanAccountId is {}", homePageContext.getLoanAccountVO().id);
      return;
    }

    pageCardV3VO.addElementForMainCard(ElementBuilder.image()
        .id(MAIN_ICON)
        .imageUrl(elementConfig.getOrderProgressStepConfigByElementId(MAIN_ICON.name()))
        .build());

    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(TITLE)
        .text(TT.gen(elementConfig.getOrderProgressStepConfigByElementId(TITLE.name())))
        .build());

    if (homepageWholeProcessTool.showDownloadButton()) {
      ElementBuilder<BaseElement> mainButtonBuilder = ElementBuilder.button()
          .id(MAIN_BUTTON)
          .text(TT.gen("审核处理中"));

      if (fraudWarningExperimentService.shouldShowFraudWarning(homePageContext.getStatus(), homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild())) {
        mainButtonBuilder.elementParam(
            ButtonElement.ButtonParam.builder().colorType(ButtonElement.ButtonColor.SHIELD).build());
      }

      BaseElement mainButtonElement = mainButtonBuilder.build();
      if (latestOrderVO.status == CashLoanOrderStatus.CHECK || latestOrderVO.status == CashLoanOrderStatus.INIT) {
        mainButtonElement.setAction(new RedirectAction(new RedirectAction.LinkActionParam(elementConfig.getRedirectPage(NativePathConstant.SUB_HOME_REVIEW_PAGE))));
      }
      pageCardV3VO.addElementForMainCard(mainButtonElement);
    }

    addProgressBarElement(pageCardV3VO, latestOrderVO);
  }

  private void addProgressBarElement(PageCardV3VO pageCardV3VO, CashLoanOrderVO latestOrderVO) {
    Map<CashLoanOrderStatus, OrderProgressStepDTO> orderProgressStep = elementConfig.getOrderProgressStep();
    List<TT> allStepTitleList = getAllOrderProgressStep(orderProgressStep);
    OrderProgressStepDTO currentStep = orderProgressStep.get(latestOrderVO.status);
    Long waitSeconds = elementConfig.getOrderProgressStepWaitSeconds(latestOrderVO.status);
    Long startTime = getStartTime(latestOrderVO);
    pageCardV3VO.addElementForMainCard(ElementBuilder.progressBar()
        .id(MainCardElementId.LOAN_PROGRESS_INFO)
        .type(ElementType.PROGRESSBAR)
        .elementParam(ProgressBarElement.ProgressBarParam.builder()
            .nodes(allStepTitleList)
            .currentNode(TT.gen(currentStep.title))
            .passTime(Clock.now() - startTime)
            .waitingSecond(waitSeconds)
            .apiRequestPath(currentStep.apiRequestPath)
            .build())
        .build());
  }

  private List<TT> getAllOrderProgressStep(Map<CashLoanOrderStatus, OrderProgressStepDTO> orderProgressStep) {
    return Arrays.stream(UNDONE_STATUSES)
        .map(orderProgressStep::get)
        .map(stepInfo -> stepInfo.title)
        .map(TT::gen)
        .collect(Collectors.toList());
  }

  private Long getStartTime(CashLoanOrderVO latestOrderVO) {
    switch (latestOrderVO.status) {
      case RESERVE:
        return latestOrderVO.timeCreated;
      case CHECK:
        //TODO(gxs, --task=1250606 --user=郭晓帅 新首页UI/结构优化--下单之后 https://www.tapd.cn/53182677/s/5826330)和产品确认对于订单check状态是不是直接返回所有check步骤的开始时间
        List<BizCheckGroupStepVO> bizCheckGroupStepVOS = bizCheckGroupStepService.findByBusinessIdAndBusinessType(latestOrderVO.id, BusinessType.ORDER_CHECK);
        EcAsserts.assertTrue(CollectionUtils.isNotEmpty(bizCheckGroupStepVOS), "not find bizCheckGroupStepVO, businessId is {}, businessType is {}", latestOrderVO.id, BusinessType.ORDER_CHECK);
        return bizCheckGroupStepVOS
            .stream()
            .map(vo -> vo.timeCreated)
            .min(Long::compareTo)
            .get();
      case INIT:
        List<BizCheckGroupStepVO> checkGroupStepVOS = bizCheckGroupStepService.findByBusinessIdAndBusinessType(latestOrderVO.id, BusinessType.ORDER_CHECK);
        EcAsserts.assertTrue(CollectionUtils.isNotEmpty(checkGroupStepVOS), "not find bizCheckGroupStepVO, businessId is {}, businessType is {}", latestOrderVO.id, BusinessType.ORDER_CHECK);
        return checkGroupStepVOS
            .stream()
            .map(vo -> vo.timeUpdated)
            .max(Long::compareTo)
            .get();
      default:
        throw EcException.error("latestOrderVO status is error, status is {}, orderId is {}", latestOrderVO.status, latestOrderVO.id);
    }
  }
}
