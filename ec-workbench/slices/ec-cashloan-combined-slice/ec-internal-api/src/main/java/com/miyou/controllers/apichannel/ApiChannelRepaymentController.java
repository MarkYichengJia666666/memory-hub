package com.miyou.controllers.apichannel;

import com.miyou.controllers.apichannel.utils.ApiChannelRepaymentConverter;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.cashloan.repay.CashLoanRepaymentService;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.cashloan.vo.RepaymentUnitVO;
import com.yqg.core.service.cashloan.vo.RepaymentVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountService;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.pm.pmenum.ReceiptPaymentChannel;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelGetRepaymentAccountRequest;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelGetRepaymentChannelRequest;
import com.yqg.ec.common.spring.request.apichannel.ApiChannelGetRepaymentDetailsRequest;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelGetRepaymentAccountResponse;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelGetRepaymentChannelResponse;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelGetRepaymentDetailsResponse;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/ecInternalApi/apichannel/repayment")
public class ApiChannelRepaymentController {

  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private RepaymentAccountService repaymentAccountService;
  @Autowired
  private CashLoanRepaymentService cashLoanRepaymentService;
  @Autowired
  private CashLoanService cashLoanService;

  @PostMapping(path = "/getRepayChannel")
  public ApiChannelGetRepaymentChannelResponse getRepayChannelList(@Valid @RequestBody ApiChannelGetRepaymentChannelRequest request) {
    LoanAccountVO accountVO = loanAccountService.getAccountByUserIdOrThrow(request.userId, SDKType.IDN_YQD);
    List<RepaymentAccountVO> repaymentAccountVOs = repaymentAccountService.getRepaymentAccountApiChannel(PaymentAccount.IDN, accountVO.userId, accountVO.sdkType);
    List<ApiChannelGetRepaymentChannelResponse.RepaymentChannel> repaymentChannels = repaymentAccountVOs.stream()
        .map(repaymentAccountVO -> {
          String logoUrl = cashLoanConfig.channelLogoMap(RepayStyleVersion.V1, repaymentAccountVO.getChannel());
          String channelDisplayName = StringUtils.isNotEmpty(repaymentAccountVO.getChannel()) ? ReceiptPaymentChannel.valueOf(repaymentAccountVO.getChannel()).desc : repaymentAccountVO.getChannel();
          return ApiChannelGetRepaymentChannelResponse.RepaymentChannel.from(channelDisplayName, repaymentAccountVO.getChannel(), logoUrl);
        }).collect(Collectors.toList());
    return ApiChannelGetRepaymentChannelResponse.from(repaymentChannels);
  }

  @PostMapping(path = "/getRepayAccount")
  public ApiChannelGetRepaymentAccountResponse getRepayAccount(@Valid @RequestBody ApiChannelGetRepaymentAccountRequest request) {
    RepaymentAccountVO repaymentAccountVO = repaymentAccountService.getRepaymentAccountByChannel(request.userId, SDKType.IDN_YQD, PaymentAccount.IDN, request.getChannelId(), request.getRepayAmount());
    if (Objects.isNull(repaymentAccountVO)) {
      return null;
    }
    String channelDisplayName = ReceiptPaymentChannel.valueOf(request.getChannelId()).desc;
    return ApiChannelGetRepaymentAccountResponse.from(channelDisplayName, repaymentAccountVO.getAccount(), repaymentAccountVO.getExpireTime());
  }

  @PostMapping(path = "/getRepaymentDetails")
  public ApiChannelGetRepaymentDetailsResponse getRepaymentDetails(@Valid @RequestBody ApiChannelGetRepaymentDetailsRequest request) {
    List<Long> repaymentIds = request.getRepaymentIds();
    if (repaymentIds == null || repaymentIds.isEmpty()) {
      return new ApiChannelGetRepaymentDetailsResponse(Collections.emptyList());
    }
    List<RepaymentVO> repaymentVOList = cashLoanRepaymentService.fetchByIds(repaymentIds);
    if (repaymentVOList == null || repaymentVOList.isEmpty()) {
      return new ApiChannelGetRepaymentDetailsResponse(Collections.emptyList());
    }
    List<RepaymentUnitVO> repaymentUnitVOList = cashLoanService.getRepaymentUnitsByRepaymentIds(repaymentIds);
    return new ApiChannelGetRepaymentDetailsResponse(ApiChannelRepaymentConverter.buildApiChannelRepaymentDetailVOList(
        repaymentVOList, repaymentUnitVOList
    ));
  }
}
