package com.miyou.controllers.cashloan.newhomepage.userinfo.processor.orderbizcheck;

import com.miyou.controllers.bizcheck.BizCheckResponseFactory;
import com.miyou.controllers.bizcheck.response.BizCheckResponse;
import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.userinfo.processor.AbstractUserInfoProcessor;
import com.miyou.controllers.cashloan.response.v5.user.HomePageMainCardInfo;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.miyou.controllers.directdebit.response.DirectDebitBankListResponse;
import com.yqg.core.model.sql.cashloan.enums.BusinessType;
import com.yqg.core.service.bizcheck.enums.BizCheckGroup;
import com.yqg.core.service.bizcheck.resultvo.BizCheckCommonResultVO;
import com.yqg.core.service.cashloan.homepage.abtest.FraudWarningExperimentService;
import com.yqg.translation.client.utils.TT;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

public abstract class AbstractBizCheckUserInfoProcessor extends AbstractUserInfoProcessor {
  public abstract TT getBizCheckTitle();

  public abstract BizCheckGroup getBizCheckGroup();

  public abstract HomePageMainCardInfo getHomePageMainCardInfo(UserResponse userResponse, HomePageContext homePageContext);

  public abstract void updateUserResponseAndMainCardInfoForRevolvingLoan(UserResponse userResponse, HomePageContext homePageContext);
  @Autowired
  private FraudWarningExperimentService fraudWarningExperimentService;

  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {
    BizCheckCommonResultVO checkResultVO = bizCheckListService.getPendingOrLatestGroupCheckResultVO(
        homePageContext.getUserCashLoanOrderContext().getLatestOrderVO().id,
        BusinessType.ORDER_CHECK,
        Collections.singletonList(getBizCheckGroup())
    );

    BizCheckResponse response = BizCheckResponseFactory.getBizCheckResponse(checkResultVO.checkType);
    userResponse
        .setDisplayStatus(homePageContext.getStatus().displayStatusV5.name())
        .setExactStatus(homePageContext.getStatus().name())
        .setTitle(getBizCheckTitle())
        .setContent(checkResultVO.homeContent)
        .setHomePageMainCardInfo(getHomePageMainCardInfo(userResponse, homePageContext))
        .setRevolvingReviewDisplayStrategy(homePageContext.getPrepareAbTestVO().revolvingReviewInnerDisplayStrategy)
        .setBizCheckInfo(response.convert(
            checkResultVO,
            bizCheckListService.getGroupCheckStatus(checkResultVO.businessId, checkResultVO.businessType, checkResultVO.checkTypeGroup)
        ));
    updateUserResponseAndMainCardInfoForRevolvingLoan(userResponse, homePageContext);
    if (getBizCheckGroup() == BizCheckGroup.FUND_PAYOUT) {
      DirectDebitBankListResponse directDebitBankListResponse = homepageContentTool.getDirectDebitBankListResponse(
          homePageContext.getUserId(),
          homePageContext.getUserDeviceContextVO().getBuild(),
          homePageContext.getSdkType());

      userResponse.setDirectDebitBankListInfo(directDebitBankListResponse);
    }

    boolean showFraudWarning = fraudWarningExperimentService.shouldShowFraudWarning(
        homePageContext.getStatus(), homePageContext.getUserId(), homePageContext.getUserDeviceContextVO().getBuild());
    userResponse.setShowFraudWarningCard(showFraudWarning);
  }
}
