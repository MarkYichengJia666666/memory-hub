package com.miyou.controllers.loan.account;

import com.miyou.controllers.loan.BaseLoanController;
import com.miyou.controllers.loan.account.request.CheckCurpRequest;
import com.miyou.controllers.loan.account.request.CheckLivingRequest;
import com.miyou.controllers.loan.account.request.CheckOtpRequest;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.miyou.utilities.secureapi.ECSecuredApi;
import com.miyou.utilities.secureapi.RequestParser;
import com.yqg.core.service.loan.account.RiskAccountService;
import com.yqg.core.service.loan.account.enums.RiskAccountVerifyStatus;
import com.yqg.core.service.loan.account.enums.VerifyType;
import com.yqg.core.service.loan.account.vo.RiskAccountContextVO;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class RiskAccountController extends BaseLoanController {

  @Autowired
  private RiskAccountService riskAccountService;

  @GetMapping(path = "/api/riskAccount/checkCreateOrder")
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result checkCreateOrder() {
    List<VerifyType> result = riskAccountService.checkCreateOrder(getContext());
    return EcResponseUtil.generate(result);
  }

  @PostMapping(path = "/api/riskAccount/checkOtp")
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result checkOtp(@RequestBody CheckOtpRequest request) {
    RiskAccountContextVO context = getContext();
    riskAccountService.checkFailMaxTimes(Collections.singleton(VerifyType.OTP), context);
    Boolean result = riskAccountService.checkOtp(request.verificationCode, context);
    // TODO zhuangyin 前端修复后下掉
    try {
      riskAccountService.insertVerifyResult(VerifyType.OTP, result ? RiskAccountVerifyStatus.SUCCESS : RiskAccountVerifyStatus.FAIL, context);
    } catch (Exception e) {
      log.warn("checkOtp error, accountId: {}, deviceToken: {}", context.loanAccountId, context.deviceToken, e);
      return EcResponseUtil.generate(Boolean.TRUE);
    }
    if (!result) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("验证失败"));
    }
    return EcResponseUtil.generate(result);
  }


  @PostMapping(path = "/api/riskAccount/checkCurp")
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result checkCurp(@RequestBody CheckCurpRequest request) {
    RiskAccountContextVO context = getContext();
    riskAccountService.checkFailMaxTimes(Collections.singleton(VerifyType.CURP), context);
    Boolean result = riskAccountService.checkCurp(request.curp, context);
    riskAccountService.insertVerifyResult(VerifyType.CURP, result ? RiskAccountVerifyStatus.SUCCESS : RiskAccountVerifyStatus.FAIL, context);
    if (!result) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("验证失败"));
    }
    return EcResponseUtil.generate(result);
  }


  @PostMapping(path = "/api/riskAccount/checkLiving")
  @ECSecuredApi(invalidForFrozenUser = true)
  public Result checkLiving(@RequestBody CheckLivingRequest request) {
    RiskAccountContextVO context = getContext();
    riskAccountService.checkFailMaxTimes(Collections.singleton(VerifyType.LIVING), context);
    context.livingImageList = request.livingImageList;
    context.delta = request.delta;
    riskAccountService.insertVerifyResult(VerifyType.LIVING, RiskAccountVerifyStatus.UPLOADED, context);
    return EcResponseUtil.generate(Boolean.TRUE);
  }

  private RiskAccountContextVO getContext() {
    LoanApiViewerContext context = getViewerContextFromRequest();
    return new RiskAccountContextVO(context.sdkType, context.build, context.userId, context.loanAccountId, RequestParser.getDeviceToken(ecRequest()), context.environmentInfo, context.terminalInfo);
  }
}
