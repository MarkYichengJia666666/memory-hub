package com.miyou.controllers.cashloan.newhomepage.userinfo.processor;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.newhomepage.enums.HomepageUserInfoProcessorType;
import com.miyou.controllers.cashloan.response.v5.user.RepaymentResponse;
import com.miyou.controllers.cashloan.response.v5.user.UserResponse;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 登录后的用户
 */
@Service
public class LoginUserDefaultProcessor extends AbstractUserInfoProcessor {
  @Override
  public void process(UserResponse userResponse, HomePageContext homePageContext) {

    Long latestPayOutOrderId = Optional.ofNullable(ecOrderService.getLatestOrder(homePageContext.getLoanAccountId(), Clock.now(), CashLoanOrderStatus.PAYOUT_STATUSES))
        .map(item -> item.id).orElse(null);
    RepaymentResponse repaymentInfo = null;
    // 还款信息---待还款状态不需要再次返回
    if (homePageContext.newHomePageUI()) {
      repaymentInfo = homepageContentTool.getRepaymentInfo(homePageContext);
    }
    userResponse
        .setExactStatus(homePageContext.getStatus().name())
        .setDisplayStatus(homePageContext.getStatus().displayStatusV5.name())
        .setUiV2(homePageContext.newHomePageUI())
        .setLatestPayoutSuccessOrderId(latestPayOutOrderId)
        .setRepayment(repaymentInfo)
    ;
  }

  @Override
  protected HomepageUserInfoProcessorType getUserInfoProcessorType() {
    return HomepageUserInfoProcessorType.LOGIN_USER_INFO_PROCESSOR;
  }
}
