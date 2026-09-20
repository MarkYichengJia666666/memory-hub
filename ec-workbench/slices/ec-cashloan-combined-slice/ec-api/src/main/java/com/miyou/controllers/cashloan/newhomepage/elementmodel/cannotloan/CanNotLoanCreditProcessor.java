package com.miyou.controllers.cashloan.newhomepage.elementmodel.cannotloan;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.AbstractPageCardV3Processor;
import com.miyou.controllers.cashloan.newhomepage.elementmodel.PageCardV3VO;
import com.miyou.controllers.cashloan.newhomepage.enums.PageCardV3ProcessorType;
import com.miyou.controllers.cashloan.response.v5.pagev3.action.PopUpAction;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.elementid.MainCardElementId;
import com.miyou.controllers.cashloan.response.v5.pagev3.base.IElement;
import com.miyou.controllers.cashloan.response.v5.pagev3.builder.ElementBuilder;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class CanNotLoanCreditProcessor extends AbstractPageCardV3Processor {
  @Override
  public void process(PageCardV3VO pageCardV3VO, HomePageContext homePageContext) {
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.TITLE)
        .text(TT.gen("剩余可借金额"))
        .build());
    BigDecimal loanAmount = homePageContext.getUserCreditsContext().calcCreditsForCannotLoanStatus(homePageContext.getHomepageUserParamsVO().homeConfigVO);
    pageCardV3VO.addElementForMainCard(ElementBuilder.text()
        .id(MainCardElementId.AMOUNT_TEXT)
        .text(TT.gen("{0}", AmountFormatter.formatForIDNNoRP(loanAmount)))
        .build());
    // 先获取进度条，再统一决定是否展示SUB_TITLE：进度条命中时不展示包含总额度的文案，临时额度文案不受影响
    Optional<IElement> amountProgressBar = homepageV3CardTool.buildAmountProgressBarElement(homePageContext);
    boolean isTotalAmountSubTitle = !hasTempCredits(homePageContext);
    if (!amountProgressBar.isPresent() || !isTotalAmountSubTitle) {
      pageCardV3VO.addElementForMainCard(ElementBuilder.text()
          .id(MainCardElementId.SUB_TITLE)
          .text(getSubTitleText(homePageContext))
          .build());
    }
    pageCardV3VO.addElementForMainCard(ElementBuilder.button()
            .id(MainCardElementId.MAIN_BUTTON)
            .text(TT.gen("立即申请"))
            .action(new PopUpAction(PopUpAction.PopUpParam.builder()
                .content(TT.gen("可借额度不足，最低借款金额为{0}，请保持良好还款习惯，有助于恢复借款额度。", AmountFormatter.format(homePageContext.getSdkType().getCurrency(),homePageContext.minLoanAmount())))
                .buttonContent(TT.gen("知道了"))
                .build()))
        .build());
    homepageV3CardTool.tryAddIncreaseCreditsLink(pageCardV3VO, homePageContext);
    // 大卡顶部额度进度条（已在上方提前获取，此处直接挂载，同时移除TOP_TIP）
    amountProgressBar.ifPresent(bar -> {
      pageCardV3VO.addElementForMainCard(bar);
      pageCardV3VO.deleteFromElementMap(MainCardElementId.TOP_TIP);
    });
  }


  private TT getSubTitleText(HomePageContext homePageContext) {
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    if (BigDecimalHelper.greaterThan(creditsInfoVO.tempCredits, BigDecimal.ZERO)) {
      return TT.gen("临时额度 {0}", AmountFormatter.format(EcCurrency.IDR, creditsInfoVO.tempCredits));
    }
    return TT.gen("总额度 {0}", AmountFormatter.format(EcCurrency.IDR, creditsInfoVO.getTotalFixedCreditsForVirtual()));
  }

  private boolean hasTempCredits(HomePageContext homePageContext) {
    LoanUserCreditsInfoVO creditsInfoVO = homePageContext.getUserCreditsContext().getCreditsInfoVO();
    return BigDecimalHelper.greaterThan(creditsInfoVO.tempCredits, BigDecimal.ZERO);
  }

  @Override
  public PageCardV3ProcessorType getProcessorType() {
    return PageCardV3ProcessorType.CAN_NOT_LOAN_CREDIT;
  }
}
