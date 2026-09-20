package com.miyou.controllers.apichannel;


import com.miyou.controllers.apichannel.utils.ApiChannelCommonConverter;
import com.yqg.core.model.sql.apichannel.enums.ApiChannel;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.agreement.AgreementService;
import com.yqg.core.service.agreement.enums.AgreementType;
import com.yqg.core.service.agreement.vo.AgreementFileVO;
import com.yqg.core.service.apichannel.user.ApiChannelOrderService;
import com.yqg.core.service.apichannel.vo.ApiChannelOrderResultVO;
import com.yqg.core.service.cashloan.LoanUserOrderService;
import com.yqg.core.service.cashloan.vo.CashLoanCreateOrderRequestVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.creditsquota.LoanCreditsQuotaService;
import com.yqg.core.service.loan.creditsquota.vo.RemainCreditsVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.spring.request.EcCreateLoanOrderRequest;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelOrderAgreementRequest;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelOrderDetailsRequest;
import com.yqg.ec.common.spring.response.EcCreateLoanOrderResponse;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelOrderAgreementResponse;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelOrderDetailsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shaded.com.yqg.datasecurity.org.apache.commons.lang3.StringUtils;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/ecInternalApi/apichannel/order")
public class ApiChannelOrderController {

  @Autowired
  private ApiChannelOrderService apiChannelOrderService;
  @Autowired
  private AgreementService agreementService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private LoanBankAccountService loanBankAccountService;
  @Autowired
  private LoanUserOrderService loanUserOrderService;
  @Autowired
  private LoanCreditsQuotaService loanCreditsQuotaService;

  @PostMapping("/details")
  ApiChannelOrderDetailsResponse getOrderDetails(@Valid @RequestBody ApiChannelOrderDetailsRequest request) {
    ApiChannel apiChannel = ApiChannel.from(request.getChannelCode());
    List<ApiChannelOrderResultVO> orderDetails = apiChannelOrderService.getApiChannelOrderDetailByOrderIdsAndStatus(
        request.getOrderIds(), request.getStatusCodeList(), apiChannel);
    return new ApiChannelOrderDetailsResponse(ApiChannelCommonConverter.convertToApiChannelOrderResultList(orderDetails));

  }

  @PostMapping("/getLoanAgreement")
  ApiChannelOrderAgreementResponse getLoanAgreementUrl(@Valid @RequestBody ApiChannelOrderAgreementRequest request) {
    AgreementFileVO agreementFileVO = agreementService.getByAgreementTypeAndBusinessIdOrNull(request.getOrderId(), AgreementType.LOAN_ORDER_CHECK);
    return ApiChannelOrderAgreementResponse.from(Objects.nonNull(agreementFileVO) ? agreementFileVO.storageKey : null);
  }

  @PostMapping(path = "/createOrder")
  public EcCreateLoanOrderResponse createOrder(@RequestBody @Validated EcCreateLoanOrderRequest request) {
    LoanAccountVO loanAccountVO = loanAccountService.getAccountByUserIdOrNull(request.userId, SDKType.IDN_YQD);
    LoanBankAccountVO loanBankAccountVO = loanBankAccountService.getExistLoanBankAccountVO(
        request.userId, SDKType.IDN_YQD, BankType.valueOf(request.bankCode), request.bankNo, PaymentBusinessName.IDN_YQD);
    RemainCreditsVO homepageRemainCredits = loanCreditsQuotaService.getHomepageRemainCredits(loanAccountVO);
    CashLoanCreateOrderRequestVO requestVO = CashLoanCreateOrderRequestVO.fromApiChannel(request.thirdPartyOrderId,
        homepageRemainCredits.remainingCreditsForVirtual, request.principal, request.productId, request.sourceType, loanAccountVO, loanBankAccountVO);
    CashLoanOrderVO vo = loanUserOrderService.checkAndCreateOrder(requestVO, false);
    return EcCreateLoanOrderResponse.from(vo.id, vo.status);
  }

}
