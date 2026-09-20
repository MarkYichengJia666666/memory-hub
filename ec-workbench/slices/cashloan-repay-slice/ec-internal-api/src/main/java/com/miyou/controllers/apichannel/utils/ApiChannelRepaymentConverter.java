package com.miyou.controllers.apichannel.utils;


import com.yqg.core.service.cashloan.vo.RepaymentUnitVO;
import com.yqg.core.service.cashloan.vo.RepaymentVO;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelGetRepaymentDetailsResponse;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelGetRepaymentDetailsResponse.ApiChannelRepaymentDetailVO;
import com.yqg.ec.common.spring.response.apichannel.ApiChannelGetRepaymentDetailsResponse.ApiChannelRepaymentUnitVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ApiChannelRepaymentConverter {

  public static ApiChannelGetRepaymentDetailsResponse.ApiChannelRepaymentVO buildApiChannelRepaymentVO(RepaymentVO repaymentVO) {
    return ApiChannelGetRepaymentDetailsResponse.ApiChannelRepaymentVO.builder()
        .id(repaymentVO.getId())
        .accountId(repaymentVO.getAccountId())
        .statusCode(repaymentVO.getStatus() == null ? null : repaymentVO.getStatus().code)
        .currency(repaymentVO.getCurrency())
        .principal(repaymentVO.getPrincipal())
        .interest(repaymentVO.getInterest())
        .prePlatformFee(repaymentVO.getPrePlatformFee())
        .preInterestExcludeFee(repaymentVO.getPreInterestExcludeFee())
        .postInterest(repaymentVO.getPostInterest())
        .postPlatformFee(repaymentVO.getPostPlatformFee())
        .postInterestExcludeFee(repaymentVO.getPostInterestExcludeFee())
        .overdueInterest(repaymentVO.getOverdueInterest())
        .penalty(repaymentVO.getPenalty())
        .timeCreated(repaymentVO.getTimeCreated())
        .timeUpdated(repaymentVO.getTimeUpdated())
        .timeRepaid(repaymentVO.getTimeRepaid())
        .userOpt(repaymentVO.getUserOpt())
        .typeCode(repaymentVO.getType() == null ? null : repaymentVO.getType().code)
        .extraData(repaymentVO.getExtraData())
        .couponId(repaymentVO.getCouponId())
        .push(repaymentVO.getPush())
        .userId(repaymentVO.getUserId())
        .amount(repaymentVO.getAmount())
        .overflowAmount(repaymentVO.getOverflowAmount())
        .transactionTime(repaymentVO.getTransactionTime())
        .paymentTransId(repaymentVO.getPaymentTransId())
        .repayCompletedTerms(repaymentVO.getRepayCompletedTerms())
        .repayCompletedTimelyTerms(repaymentVO.getRepayCompletedTimelyTerms())
        .build();

  }

  public static ApiChannelGetRepaymentDetailsResponse.ApiChannelRepaymentUnitVO buildApiChannelRepaymentUnitVO(
      RepaymentUnitVO repaymentUnitVO) {
    return ApiChannelGetRepaymentDetailsResponse.ApiChannelRepaymentUnitVO.builder()
        .id(repaymentUnitVO.getId())
        .accountId(repaymentUnitVO.getAccountId())
        .repaymentId(repaymentUnitVO.getRepaymentId())
        .orderId(repaymentUnitVO.getOrderId())
        .currency(repaymentUnitVO.getCurrency())
        .principal(repaymentUnitVO.getPrincipal())
        .interest(repaymentUnitVO.getInterest())
        .prePlatformFee(repaymentUnitVO.getPrePlatformFee())
        .preInterestExcludeFee(repaymentUnitVO.getPreInterestExcludeFee())
        .postInterest(repaymentUnitVO.getPostInterest())
        .postPlatformFee(repaymentUnitVO.getPostPlatformFee())
        .postInterestExcludeFee(repaymentUnitVO.getPostInterestExcludeFee())
        .overdueInterest(repaymentUnitVO.getOverdueInterest())
        .penalty(repaymentUnitVO.getPenalty())
        .timeCreated(repaymentUnitVO.getTimeCreated())
        .timeUpdated(repaymentUnitVO.getTimeUpdated())
        .type(repaymentUnitVO.getType())
        .userOpt(repaymentUnitVO.getUserOpt())
        .extraData(repaymentUnitVO.getExtraData())
        .repaymentTime(repaymentUnitVO.getRepaymentTime())
        .repaymentTimeUpdated(repaymentUnitVO.getRepaymentTimeUpdated())
        .instalmentId(repaymentUnitVO.getInstalmentId())
        .userId(repaymentUnitVO.getUserId())
        .amount(repaymentUnitVO.getAmount())
        .build();
  }

  public static List<ApiChannelRepaymentDetailVO> buildApiChannelRepaymentDetailVOList(List<RepaymentVO> repaymentVOList,
      List<RepaymentUnitVO> repaymentUnitVOList) {
    Map<Long, List<RepaymentUnitVO>> repaymentIdToUnitsMap = repaymentUnitVOList.stream()
        .collect(Collectors.groupingBy(RepaymentUnitVO::getRepaymentId));
    return repaymentVOList.stream().map(
        repaymentVO -> {
          List<RepaymentUnitVO> repaymentUnitVOS = repaymentIdToUnitsMap.get(repaymentVO.getId());
          List<ApiChannelRepaymentUnitVO> apiChannelRepaymentUnitVOList = repaymentUnitVOS == null ? Collections.emptyList() :
              repaymentUnitVOS.stream().map(ApiChannelRepaymentConverter::buildApiChannelRepaymentUnitVO).collect(Collectors.toList());
          return ApiChannelRepaymentDetailVO.builder()
              .repaymentVO(buildApiChannelRepaymentVO(repaymentVO))
              .repaymentUnitVOList(apiChannelRepaymentUnitVOList)
              .build();
        }
    ).collect(Collectors.toList());
  }

}
