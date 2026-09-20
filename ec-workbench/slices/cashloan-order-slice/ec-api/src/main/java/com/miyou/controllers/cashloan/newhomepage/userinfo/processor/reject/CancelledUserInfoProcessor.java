package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.reject;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.core.service.loanmarket.vo.LoanMarketUserQualifyCheckResult;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CancelledUserInfoProcessor extends AbstractUserInfoProcessor {

  @Override
  public void process(UserResponse fieldsInfo, HomePageContext homePageContext) {
    HomeDisplayStrategy rejectHomeDisplayStrategy = rejectedSupplier.getRejectHomeDisplayStrategy(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());

    BigDecimal maxCredits = ecHomePageProductTool.getDefaultOrHistoryLoanAmount(homePageContext.getLoanAccountId(), homePageContext.getSdkType());
    String cancelLoanTipsPopUp = homepageV5Config.getCancelLoanTipsPopUp();
    LoanTipsPopUpVO loanTipsPopUpVO = JsonUtils.fromOrNull(cancelLoanTipsPopUp, LoanTipsPopUpVO.class);
    boolean cannotReapply = homepageContentTool.isNotQualifiedToReapply(homePageContext);
    LoanMarketUserQualifyCheckResult loanMarketCheckResult = homepageContentTool.postProcessLoanMarketCheckResult(homePageContext, !cannotReapply);
    fieldsInfo.setTitle(TT.gen("审核未通过"))
        .setContent(TT.gen("非常抱歉，您暂时无法借款。"))
        .setRejectHomeDisplayStrategy(rejectHomeDisplayStrategy)
        .setButtonName(rejectHomeDisplayStrategy == HomeDisplayStrategy.B ? TT.gen("申请借款") : null)
        .setHomePageMainCardInfo(homePageMainCardInfoTool.getNoIncreaseCreditsQualificationMainCardInfo(
            homePageContext,
            rejectHomeDisplayStrategy,
            maxCredits,
            homePageContext.newHomePageUI(),
            loanMarketCheckResult))
        .setLoanTipsPopUp(LoanTipsPopUp.from(loanTipsPopUpVO))
        .setReserveLoanCardInfo(homepageContentTool.buildReserveLoanCardInfo(abTestVersionConfigService.getReserveLoanStyle(homePageContext.getUserDeviceContextVO().getBuild(), homePageContext.getUserId())))
        .setLoanMarketCardForV2Response(homepageContentTool.buildLoanMarketForOldHomePageResp(homePageContext, loanMarketCheckResult.qualifiedLoanMarketEntranceByRisk))
    ;
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.CANCELLED;
  }
}
