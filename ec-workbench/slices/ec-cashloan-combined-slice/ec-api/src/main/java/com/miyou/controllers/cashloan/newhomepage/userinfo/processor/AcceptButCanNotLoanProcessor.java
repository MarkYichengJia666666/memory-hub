package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.google.common.collect.ImmutableList;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.response.v5.user.RepaymentResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

import static com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType.ACCEPT_BUT_CAN_NOT_LOAN;

@Service
public class AcceptButCanNotLoanProcessor extends AbstractUserInfoProcessor {
    @Override
    public void process(UserResponse userResponse, HomePageContext homePageContext) {
        HomeDisplayStrategy revolvingReviewDisplayStrategy = homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy;
        RepaymentResponse repaymentInfo = homepageContentTool.getRepaymentInfo(homePageContext);
        HomeDisplayStrategy repaymentDisplayStrategy = homePageMainCardInfoTool.getRepaymentDisplayStrategy(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());
        Long maxPayoutOrderId = homePageContext.getUserCashLoanOrderContext().getReadyOrderList().stream().map(o -> o.orderVO.id).max(Long::compareTo).orElse(null);
        userResponse.setButtonName(repaymentDisplayStrategy == HomeDisplayStrategy.A ? null : TT.gen("申请借款"))
                .setMaxPayoutOrderId(maxPayoutOrderId)
                .setRepayment(homePageContext.newHomePageUI() && repaymentDisplayStrategy.isStrategyA() ? null : repaymentInfo)
                .setHomePageMainCardInfo(homePageMainCardInfoTool.getRepaidMainCardInfo(homePageContext.getUserCreditsContext(),
                        homePageContext.getUserCashLoanOrderContext(),
                        repaymentInfo,
                        repaymentDisplayStrategy,
                        homePageContext.getUserCreditsContext().calcCreditsForCannotLoanStatus(homePageContext.getHomepageUserParamsVO().homeConfigVO),
                        revolvingReviewDisplayStrategy)
                )
                .setRepaymentHomeDisplayStrategy(repaymentDisplayStrategy)
                .setRevolvingReviewDisplayStrategy(revolvingReviewDisplayStrategy)
                .setLoanTipsPopUp(LoanTipsPopUp.from(buildPopUp(homePageContext)))
        ;
    }

    private LoanTipsPopUpVO buildPopUp(HomePageContext homePageContext) {
        String loanTipsPopup = homepageV5Config.getLoanTipsPopupForAcceptButCanNotLoan();
        LoanTipsPopUpVO loanTipsPopUpVO = JsonUtils.fromOrNull(loanTipsPopup, LoanTipsPopUpVO.class);
        loanTipsPopUpVO.contentArgs = ImmutableList.of(AmountFormatter.format(homePageContext.getSdkType().getCurrency(), homePageContext.minLoanAmount())).toArray();
        return loanTipsPopUpVO;
    }

    @Override
    protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
        return ACCEPT_BUT_CAN_NOT_LOAN;
    }
}
