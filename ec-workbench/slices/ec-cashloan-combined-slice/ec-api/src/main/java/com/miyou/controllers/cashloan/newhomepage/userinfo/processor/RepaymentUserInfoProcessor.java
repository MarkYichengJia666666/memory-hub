package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.LoanMarketForOldHomePageResponse;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.response.v5.user.RepaymentResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class RepaymentUserInfoProcessor extends AbstractUserInfoProcessor {

    @Override
    public void process(UserResponse userResponse, HomePageContext homePageContext) {
        Long maxPayoutOrderId = homePageContext.getUserCashLoanOrderContext().getReadyOrderList().stream().map(o -> o.orderVO.id).max(Long::compareTo).orElse(null);
        if (maxPayoutOrderId == null) {
            log.error("MaxPayoutOrderId is null for repayment home page！status is {}, accountId is {}.", homePageContext.getStatus(), homePageContext.getLoanAccountVO().id);
        }
        HomeDisplayStrategy revolvingReviewDisplayStrategy = homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy;
        RepaymentResponse repaymentInfo = homepageContentTool.getRepaymentInfo(homePageContext);
        String loanTipsPopup = homePageMainCardInfoTool.getRepaymentLoanTipsPopup(revolvingReviewDisplayStrategy, homePageContext);
        HomeDisplayStrategy repaymentDisplayStrategy = homePageMainCardInfoTool.getRepaymentDisplayStrategy(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());

        boolean cannotReapply = homepageContentTool.isNotQualifiedToReapply(homePageContext);
        LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext, !cannotReapply);
        boolean showLoanMarketEntrance = loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk;
        LoanMarketForOldHomePageResponse loanMarketForOldHomePageResponse = homepageContentTool.buildLoanMarketForOldHomePageResp(homePageContext, showLoanMarketEntrance);
        boolean showLoanMarketCard = Objects.nonNull(loanMarketForOldHomePageResponse);
        // 如果展示贷超卡片，屏蔽还款卡片。主卡片依旧显示还款按钮
        if (showLoanMarketCard) {
            repaymentInfo.setRepaymentCard(null);
        }
        userResponse.setButtonName(repaymentDisplayStrategy == HomeDisplayStrategy.A ? null : TT.gen("申请借款"))
                .setMaxPayoutOrderId(maxPayoutOrderId)
                .setRepayment(homePageContext.newHomePageUI() && repaymentDisplayStrategy.isStrategyA() ? null : repaymentInfo)
                .setHomePageMainCardInfo(homePageMainCardInfoTool.getRepaidMainCardInfo(homePageContext.getUserCreditsContext(),
                        homePageContext.getUserCashLoanOrderContext(),
                        repaymentInfo,
                        repaymentDisplayStrategy,
                        homePageContext.getUserProductVO().getEnableVirtualCredits(),
                        revolvingReviewDisplayStrategy)
                )
                .setRepaymentHomeDisplayStrategy(repaymentDisplayStrategy)
                .setRevolvingReviewDisplayStrategy(revolvingReviewDisplayStrategy)
                .setLoanTipsPopUp(LoanTipsPopUp.from(JsonUtils.fromOrNull(loanTipsPopup, LoanTipsPopUpVO.class)))
                .setLoanMarketCardForV2Response(loanMarketForOldHomePageResponse)
        ;
    }

    @Override
    protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
        return HomepageUserInfoProcessorType.REPAYMENT_USER_INFO;
    }
}
