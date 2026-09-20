package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.LoanTipsPopUp;
import com.miyou.controllers.cashloan.response.v5.user.RepaymentResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.homepage.abtest.FraudWarningExperimentService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.homepage.display.dto.LoanTipsPopUpVO;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.MaskUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
public class PayingUserInfoProcessor extends AbstractUserInfoProcessor {
  @Autowired
  private FraudWarningExperimentService fraudWarningExperimentService;

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    CashLoanOrderVO orderVO = homePageContext.getUserCashLoanOrderContext().getLatestOrderVO();
    String payingAmount = AmountFormatter.format(orderVO.currency, orderVO.receivedAmount);
    LoanBankAccountVO credentialInfo = (LoanBankAccountVO) paymentService.getCredentialInfo(orderVO.paymentCredential);
    HomepageV5Config homepageV5Config = homePageContext.getHomePageServiceManager().getHomepageV5Config();
    HomePageMainCardInfo mainCardInfo = homePageMainCardInfoTool.getFundPayingMainCardInfo();
    String loanTipsPopup = null;
    HomeDisplayStrategy revolvingReviewDisplayStrategy = homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy;
    userResponse
        .setTitle(TT.gen("已申请成功，正在打款中"))
        .setContent(TT.gen(homepageV5Config.getPayingStatusContent(), payingAmount,
            MaskUtils.getLastFourNum(String.valueOf(credentialInfo.accountNumber)), credentialInfo.bankType));

    if (revolvingReviewDisplayStrategy.isStrategyB()) {
      loanTipsPopup = homepageV5Config.getRevolvingLoanTipsPopUpWhenPaying();
      mainCardInfo = homePageMainCardInfoTool.updateMainCardInfoForRevolvingLoan(homePageContext, mainCardInfo, orderVO);
      updateRepaymentInfo(userResponse, homePageContext, mainCardInfo);
      userResponse
          .setTitle(TT.gen("打款中"))
          .setContent(TT.gen("预计3分钟完成，请耐心等待"));
    }
    if (Objects.nonNull(loanTipsPopup)) {
      userResponse.setLoanTipsPopUp(LoanTipsPopUp.from(JsonUtils.fromOrNull(loanTipsPopup, LoanTipsPopUpVO.class)));
    }
    userResponse
        .setHomePageMainCardInfo(mainCardInfo)
        .setRevolvingReviewDisplayStrategy(revolvingReviewDisplayStrategy)
        .setDirectDebitBankListInfo(homepageContentTool.getDirectDebitBankListResponse(
            homePageContext.getUserId(),
            homePageContext.getUserDeviceContextVO().getBuild(),
            homePageContext.getSdkType()));

    boolean showFraudWarning = fraudWarningExperimentService.shouldShowFraudWarning(
        homePageContext.getStatus(), homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());
    userResponse.setShowFraudWarningCard(showFraudWarning);
  }


  private void updateRepaymentInfo(UserResponse userResponse, HomePageContext homePageContext, HomePageMainCardInfo mainCardInfo) {
    HomeDisplayStrategy repaymentDisplayStrategy = homePageMainCardInfoTool.getRepaymentDisplayStrategy(homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());
    RepaymentResponse repaymentInfo = homepageContentTool.getRepaymentInfo(homePageContext);
    if (Objects.nonNull(repaymentInfo)) {
      mainCardInfo.setRepaymentInfo(HomePageMainCardInfo.UserRepaymentInfo.builder()
          .overdue(repaymentInfo.overdue)
          .tip(repaymentInfo.tip)
          .recentlyBillingDateContent(repaymentInfo.recentlyBillingDateDesc)
          .build());
    }
    userResponse.setRepayment(homePageContext.newHomePageUI() && repaymentDisplayStrategy.isStrategyA() ? null : repaymentInfo);
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.PAYING_USER_INFO;
  }
}
