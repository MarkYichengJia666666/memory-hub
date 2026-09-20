package com.miyou.controllers.jbp;

import com.google.common.collect.ImmutableList;
import com.miyou.controllers.cashloan.response.OVORepaymentInfoResponse;
import com.miyou.controllers.cashloan.response.RepaymentAccountResponse;
import com.miyou.controllers.cashloan.response.RepaymentAccountResponseV4;
import com.miyou.controllers.jbp.request.JbpGetOVOAccountRequest;
import com.miyou.controllers.jbp.request.JbpGetQuickRepaymentAccountRequest;
import com.miyou.controllers.jbp.request.JbpGetRepaymentAccountRequest;
import com.miyou.controllers.cashloan.service.RepaymentAccountResponseBuilder;
import com.miyou.controllers.loan.BaseLoanController;
import com.miyou.controllers.secure.SecureCheckUserContextFactory;
import com.miyou.utilities.secureapi.ECSecuredApi;
import com.yqg.common.util.hashid.YqgHashids;
import com.yqg.core.service.cashloan.repayment.RepaymentAccountFactory;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.cashloan.repayment.enums.RepaymentAccountSourceType;
import com.yqg.core.service.jbp.goldencard.JbpCardService;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountService;
import com.yqg.core.service.loan.repayment.account.vo.OVORepaymentInfoVO;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.core.service.mobile.verification.VerificationService;
import com.yqg.core.service.secure.check.context.SecureCheckUserContext;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.jbp.common.utils.JbpBaseResponse;
import com.yqg.jbp.dto.order.OrderResponse;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class JbpRepaymentController extends BaseLoanController {
  @Autowired
  private RepaymentAccountService repaymentAccountService;
  @Autowired
  private SecureCheckUserContextFactory userContextFactory;
  @Autowired
  private JbpCardService jbpCardService;
  @Autowired
  private VerificationService verificationService;
  @Autowired
  private RepaymentAccountResponseBuilder repaymentAccountResponseBuilder;

  /**
   * @title 获取所有类型的还款账号
   * @desc 目前中收不支持代扣付款
   */
  @PostMapping(path = "/api/jbp/getRepayAccount")
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result<RepaymentAccountResponseV4> getRepayAccount() {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    //获取虚拟还款账号
    List<RepaymentAccountResponse> repaymentAccountResponseList = getRepaymentAccountResponse(viewerContext);
    return EcResponseUtil.generate(RepaymentAccountResponseV4.from(null, null, repaymentAccountResponseList, null, null, RepayStyleVersion.V1));
  }

  public List<RepaymentAccountResponse> getRepaymentAccountResponse(LoanApiViewerContext viewerContext) {
    List<RepaymentAccountVO> repaymentAccountVOS = repaymentAccountService.getJbpRepaymentChannelList(viewerContext.userId,
        viewerContext.build, viewerContext.sourceType, viewerContext.sdkType);
    return repaymentAccountResponseBuilder.buildFromAccountVos(repaymentAccountVOS, viewerContext.userId, viewerContext.sdkType,
        viewerContext.build, RepayStyleVersion.V1);
  }

  /**
   * 重要重要重要
   * ⚠️⚠️⚠️
   * 本方法和com.miyou.controllers.payment.PaymentReceiptAccountCallbackController#getRepaymentInformation(java.lang.String)
   * 具有业务依赖，BCA渠道会调用该方法获取金额，修改本方法请考虑是否需要同步修改上面的方法
   * ⚠️⚠️⚠️
   *
   * @param request
   * @return
   */
  @PostMapping(path = "/api/jbp/getRepaymentAccountByChannel")
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result getRepaymentAccountByChannelV3(@RequestBody @Valid JbpGetRepaymentAccountRequest request) {
    SecureCheckUserContext userContext = userContextFactory.createUserContextForSelfServiceQueryRepaymentPurpose(ecRequest(), request.mobileNumber);
    if (request.verificationPurpose != null) {
      verificationService.checkMobileVerificationCode(request.mobileNumber, request.verificationCode, request.verificationPurpose, userContext.sdkType);
    }
      return EcResponseUtil.generate(
          RepaymentAccountFactory.getRepaymentAccountByChannel(RepaymentAccountSourceType.JBP, request.channel, null, request.amount, null,
              request.mobileNumber, ImmutableList.of(request.orderId), RepayStyleVersion.V1, userContext));
  }

  @PostMapping(path = "/api/jbp/getOVORepaymentInfo")
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result getOVORepaymentInfo(@RequestBody @Valid JbpGetOVOAccountRequest request) {
    LoanApiViewerContext viewerContext = getViewerContextFromRequest();
    Long orderId = YqgHashids.decode(request.orderId);
    OVORepaymentInfoVO ovoRepaymentInfoVO = repaymentAccountService.getUserHistoryOVORepaymentInfoForJbp(request.amount, viewerContext.userId, orderId);
    return EcResponseUtil.generate(OVORepaymentInfoResponse.from(ovoRepaymentInfoVO));
  }

  @PostMapping(path = "/api/jbp/quickPaymentInfo")
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result getQuickPaymentInfo(@RequestBody @Valid JbpGetQuickRepaymentAccountRequest request) {
    SecureCheckUserContext userContext = userContextFactory.createUserContextForSelfServiceQueryRepaymentPurpose(ecRequest(), request.mobileNumber);
    JbpBaseResponse<OrderResponse> response = jbpCardService.createJbpCardOrder(userContext.userId, userContext.sdkType, userContext.build);
    OrderResponse orderResponse = response.body;
    String channel = repaymentAccountService.getJbpQuickPaymentChannel(userContext.userId);
      return EcResponseUtil.generate(
          RepaymentAccountFactory.getRepaymentAccountByChannel(RepaymentAccountSourceType.JBP, channel, null, orderResponse.needPayAmount,
              null, request.mobileNumber, ImmutableList.of(orderResponse.orderId), RepayStyleVersion.V1, userContext));
  }
}
