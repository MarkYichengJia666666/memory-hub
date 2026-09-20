package com.miyou.controllers.admin.repayment;

import com.miyou.controllers.admin.repayment.request.*;
import com.miyou.controllers.core.YqgBaseController;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.overseas.client.spring.api.payment.IOverseasRepaymentRouteConfigService;
import com.yqg.overseas.client.spring.api.paymentchannel.IOverseasPaymentPaymentChannelService;
import com.yqg.overseas.common.enums.AvailabilityStatus;
import com.yqg.overseas.common.enums.ReceiptPaymentChannel;
import com.yqg.overseas.common.payment.PaymentProvider;
import com.yqg.overseas.common.payment.annotation.PayMethod;
import com.yqg.overseas.spring.request.receipt.RepaymentChannelConfigRequest;
import com.yqg.overseas.spring.request.receipt.RepaymentChannelConfigUpdateRequest;
import com.yqg.overseas.spring.response.receipt.RepaymentChannelConfigResponse;
import com.yqg.overseas.spring.response.receipt.RepaymentChannelConfigResponses;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/admin/repay")
public class RepaymentRouteController extends YqgBaseController {

  @Autowired
  private IOverseasPaymentPaymentChannelService overseasPaymentPaymentChannelService;
  @Autowired
  private IOverseasRepaymentRouteConfigService overseasRepaymentRouteConfigService;

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.QUERY')")
  @GetMapping("/queryRoute")
  public Result queryRepaymentRoute(@RequestParam(value = "channel", required = false) ReceiptPaymentChannel channel,
                                    @RequestParam(value = "status", required = false) AvailabilityStatus status,
                                    @RequestParam("pageNo") @Min(1) Integer pageNo,
                                    @RequestParam("pageSize") @Min(1) Integer pageSize) {
    return EcResponseUtil.generate(
        overseasRepaymentRouteConfigService.getListPaymentChannelConfig(channel, status, pageNo, pageSize));
  }

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.WRITE')")
  @PostMapping("/createRoute")
  public Result createRoute(@RequestBody RepaymentRouteConfigCreateRequest request) {
    RepaymentChannelConfigRequest opRequest = request.convertToOPRequest();
    return getCreateRouteResult(request.getRepaymentChannel(), opRequest);
  }

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.WRITE')")
  @PostMapping("/createRouteV2")
  public Result createRouteV2(@RequestBody RepaymentRouteConfigCreateV2Request request) {
    RepaymentChannelConfigRequest opRequest = request.convertToOPRequest();
    return getCreateRouteResult(request.getRepaymentChannel(), opRequest);
  }

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.WRITE')")
  @PostMapping("/createRouteV3")
  public Result createRouteV3(@RequestBody RepaymentRouteConfigV3Request request) {
    RepaymentChannelConfigRequest opRequest = request.convertToOPCreateRequest();
    return getCreateRouteResult(request.getRepaymentChannel(), opRequest);
  }

  private Result getCreateRouteResult(ReceiptPaymentChannel channel, RepaymentChannelConfigRequest opRequest) {
    // Check if the route already existing
    RepaymentChannelConfigResponses existingChannelConfig = overseasRepaymentRouteConfigService.getListPaymentChannelConfig(
        channel, null, 1, 1);

    if (Objects.nonNull(existingChannelConfig) && CollectionUtils.isNotEmpty(existingChannelConfig.getData())) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("Channel already have repayment config!"));
    }

    RepaymentChannelConfigResponse response = overseasRepaymentRouteConfigService.createRepaymentChannelConfig(opRequest);

    if (Objects.isNull(response)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("Channel config creation error, check payment side!"));
    }

    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.WRITE')")
  @PostMapping("/updateRoute")
  public Result updateRoute(@RequestBody RepaymentRouteConfigUpdateRequest request) {
    RepaymentChannelConfigUpdateRequest opRequest = request.convertToOPRequest();
    return getUpdateRouteResult(opRequest);
  }

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.WRITE')")
  @PostMapping("/updateRouteV2")
  public Result updateRouteV2(@RequestBody RepaymentRouteConfigUpdateV2Request request) {
    RepaymentChannelConfigUpdateRequest opRequest = request.convertToOPRequest();
    return getUpdateRouteResult(opRequest);
  }

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.WRITE')")
  @PostMapping("/updateRouteV3")
  public Result updateRouteV3(@RequestBody RepaymentRouteConfigV3Request request) {
    RepaymentChannelConfigUpdateRequest opRequest = request.convertToOPUpdateRequest();
    return getUpdateRouteResult(opRequest);
  }

  private Result getUpdateRouteResult(RepaymentChannelConfigUpdateRequest opRequest) {
    RepaymentChannelConfigResponse response = overseasRepaymentRouteConfigService.updateRepaymentChannelConfig(opRequest);

    if (Objects.isNull(response)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("Channel config update error, check payment side!"));
    }

    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.QUERY')")
  @GetMapping("/channels")
  public Result getChannelList() {
    return EcResponseUtil.generate(
        overseasPaymentPaymentChannelService.getSubChannelList(PaymentProvider.NONE, PayMethod.Entry.BUSINESS_RECEIPT));
  }

  @PreAuthorize("hasAnyAuthority('PAY.CONFIG.QUERY')")
  @GetMapping("/statusList")
  public Result getStatusList() {
    return EcResponseUtil.generate(
        overseasRepaymentRouteConfigService.getStatusList());
  }
}
