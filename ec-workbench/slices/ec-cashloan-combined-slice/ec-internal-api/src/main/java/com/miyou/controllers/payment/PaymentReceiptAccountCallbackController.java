package com.miyou.controllers.payment;

import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.service.cashloan.repayment.RepaymentAccountExpService;
import com.yqg.core.service.cashloan.repayment.RepaymentAccountFactory;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.loan.vo.UserInfoVO;
import com.yqg.core.service.payment.PaymentReceiptAccountService;
import com.yqg.core.service.payment.pm.VirtualAccountPaymentMethod;
import com.yqg.core.service.payment.pm.vo.StaticVirtualAccountVO;
import com.yqg.core.service.payment.response.ReceiptAccountTypeResponse;
import com.yqg.core.service.payment.response.ReceiptCreationCredentialResponse;
import com.yqg.core.service.payment.response.RepaymentInformationResponse;
import com.yqg.core.service.payment.thirdparty.bca.BcaService;
import com.yqg.core.service.receipt.ReceiptAccountService;
import com.yqg.core.service.user.UserService;
import com.yqg.ec.common.spring.request.payment.PaymentReceiptAccountCallbackRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.math3.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PaymentReceiptAccountCallbackController {

  @Autowired
  PaymentReceiptAccountService paymentReceiptAccountService;
  @Autowired
  private UserService userService;
  @Autowired
  private VirtualAccountPaymentMethod virtualAccountPaymentMethod;
  @Autowired
  private ReceiptAccountService receiptAccountService;

  /**
   * Delete expired time because still only insert static virtual account
   * Dynamic virtual account inserted on the first time trigger
   *
   * @param request
   * @return
   */
  @PatchMapping(path = "/ecInternalApi/payment/receipt/account/callback")
  boolean syncToReceiptCreationCredential(@RequestBody @Validated PaymentReceiptAccountCallbackRequest request) {
    return paymentReceiptAccountService.syncReceiptCreationCredential(
        request.getBusinessId(),
        request.getReceiptId(),
        request.getReceiptAccount(),
        request.getReceiptContent()
    );
  }

  @PatchMapping(path = "/ecInternalApi/payment/receipt/account/callback/batch")
  public Map<String, Boolean> batchSyncToAccountInfoCallback(@RequestBody List<PaymentReceiptAccountCallbackRequest> requests) {
    return requests.stream()
        .map(request -> new Pair<>(request.getReceiptId(), syncToReceiptCreationCredential(request)))
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (v1, v2) -> v1));
  }

  @GetMapping(path = "/ecInternalApi/payment/receipt/account/callback/getReceiptCreationCredential")
  public ReceiptCreationCredentialResponse getReceiptCreationCredential(@RequestParam(value = "transNo", required = true) String transNo) {
    return paymentReceiptAccountService.getPaymentReceiptAccount(transNo);
  }

  @GetMapping(path = "/ecInternalApi/payment/receipt/repaymentInformation")
  public RepaymentInformationResponse getRepaymentInformation(@RequestParam(value = "virtualAccountNo", required = true) String virtualAccountNo) {
    StaticVirtualAccountVO virtualAccountVO = virtualAccountPaymentMethod.getVirtualAccountVO(virtualAccountNo);
    Long ownAmount = RepaymentAccountFactory.getOwnedAmount(virtualAccountVO.getUserId(), virtualAccountVO.getPayEventType(),
          virtualAccountVO.getPaymentAccount());
    UserInfoVO userInfoVO = userService.fetchById(virtualAccountVO.getUserId(), virtualAccountVO.getBusinessName().sdkType);
    return RepaymentInformationResponse.from(virtualAccountNo, ownAmount, userInfoVO.name, virtualAccountVO.getPaymentAccount(),
        virtualAccountVO.getBusinessName());
  }

  @GetMapping(path = "/ecInternalApi/payment/receipt/accountType")
  public ReceiptAccountTypeResponse getReceiptAccountType(@RequestParam(value = "businessId") String businessId) {
    PayEventType payEventType = receiptAccountService.getPayEventTypeByBusinessId(businessId);
    return ReceiptAccountTypeResponse.from(payEventType);
  }
}
