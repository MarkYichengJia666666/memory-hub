package com.yqg.core.service.apichannel.user;

import com.google.common.collect.Maps;
import com.yqg.core.model.generated.tables.records.ApiChannelRepaymentRecord;
import com.yqg.core.model.sql.apichannel.ApiChannelRepaymentModel;
import com.yqg.core.model.sql.apichannel.enums.ApiChannel;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentType;
import com.yqg.core.model.sql.tool.enums.Purpose;
import com.yqg.core.service.apichannel.ApiChannelUserMonitorService;
import com.yqg.core.service.apichannel.vo.*;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.loanproduct.ProductConfigService;
import com.yqg.core.service.cashloan.ordercenter.CashLoanInstalmentService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.repay.CashLoanRepaymentService;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.RepaymentUnitVO;
import com.yqg.core.service.cashloan.vo.RepaymentVO;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountService;
import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountCallerType;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.loan.vo.LoanProductConfigVO;
import com.yqg.core.service.payment.ICredential;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.PaymentCredential;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.tool.IdGeneratorService;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApiChannelRepaymentService {

  @Autowired
  private ApiChannelOrderService apiChannelOrderService;
  @Autowired
  private EcOrderService orderService;
  @Autowired
  private ProductConfigService productConfigService;
  @Autowired
  private CashLoanService cashLoanService;
  @Autowired
  private RepaymentAccountService repaymentAccountService;
  @Autowired
  private CashLoanRepaymentService cashLoanRepaymentService;
  @Autowired
  private ApiChannelUserMonitorService monitorService;

  @Autowired
  private ApiUserChannelService apiChannelUserService;

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private CashLoanInstalmentService cashLoanInstalmentService;

  @Autowired
  private ApiChannelRepaymentModel apiChannelRepaymentModel;

  @Autowired
  private InstalmentCutInterestCouponDeductDetailService instalmentCutInterestCouponDeductDetailService;

  @Autowired
  private IdGeneratorService idGeneratorService;

  public ApiChannelRepaymentPlanVO getRepaymentPlan(String thirdPartyOrderId, ApiChannel channel) {
    ApiChannelOrderVO apiChannelOrderVO = apiChannelOrderService.findByThirdOrderIdAndChannelOrThrow(thirdPartyOrderId, channel);
    verifyChannelOrder(apiChannelOrderVO, thirdPartyOrderId, channel);

    OrderInstalment orderInstalment = orderService.getOrderInstalment(apiChannelOrderVO.getOrderId());
    verifyEcOrder(orderInstalment, thirdPartyOrderId, channel);
    checkOrderCompleted(thirdPartyOrderId, orderInstalment);

    LoanProductConfigVO productVO = productConfigService.getProductVO(orderInstalment.orderVO.productId);

    ApiChannelUserVO apiChannelUserVO = apiChannelUserService.findByUserIdAndChannel(orderInstalment.orderVO.userId, channel);

    Map<Long, Long> lastedRepaymentTimeMap = Maps.newHashMap();
    orderInstalment.instalmentVOS.forEach(instalmentVO -> {
      List<RepaymentUnitVO> repaymentUnitVOs = cashLoanService.getRepaymentUnitsByInstalmentIdAndType(instalmentVO.id, CashLoanRepaymentType.NORMAL);
      repaymentUnitVOs.stream()
          .skip(Math.max(repaymentUnitVOs.size() - 1, 0))
          .findFirst()
          .ifPresent(lastedRepaymentUnitVO -> lastedRepaymentTimeMap.put(instalmentVO.id, lastedRepaymentUnitVO.repaymentTime));
    });

    ApiChannelLoanRepaymentResultVO repayment = getRepayment(thirdPartyOrderId, channel);
    return new ApiChannelRepaymentPlanVO(apiChannelUserVO, orderInstalment, productVO, apiChannelOrderVO, lastedRepaymentTimeMap, repayment);
  }

  public ApiChannelRepaymentPlanVO getRepaymentPlanWhenCannotRepay(String thirdPartyOrderId, ApiChannel channel) {
    ApiChannelOrderVO apiChannelOrderVO = apiChannelOrderService.findByThirdOrderIdAndChannelOrThrow(thirdPartyOrderId, channel);
    OrderInstalment orderInstalment = orderService.getOrderInstalment(apiChannelOrderVO.getOrderId());
    LoanProductConfigVO productVO = productConfigService.getProductVO(orderInstalment.orderVO.productId);
    ApiChannelUserVO apiChannelUserVO = apiChannelUserService.findByUserIdAndChannel(orderInstalment.orderVO.userId, channel);
    return new ApiChannelRepaymentPlanVO(apiChannelUserVO, orderInstalment, productVO, apiChannelOrderVO);
  }

  private void verifyChannelOrder(ApiChannelOrderVO apiChannelOrderVO, String thirdPartyOrderId, ApiChannel channel) {
    if (Objects.isNull(apiChannelOrderVO)) {
      throw EcException.error(EcExceptionType.API_CHANNEL_ORDER_NOT_EXIST, "不存在api渠道订单! thirdPartyOrderId:{}, channel:{}", thirdPartyOrderId, channel);
    }
    if (Objects.isNull(apiChannelOrderVO.getOrderId())) {
      throw EcException.error(EcExceptionType.API_CHANNEL_ORDER_NOT_HAVE_CASH_LOAN_ORDER_ID, "查询api渠道订单时，orderId为空, 暂无还款计划! thirdPartyOrderId:{}, channel:{}, apiChannelOrderId:{}", thirdPartyOrderId, channel, apiChannelOrderVO.getId());
    }
  }

  private void verifyEcOrder(OrderInstalment orderInstalment, String thirdPartyOrderId, ApiChannel channel) {
    if (Objects.isNull(orderInstalment) || Objects.isNull(orderInstalment.orderVO)) {
      throw EcException.error(EcExceptionType.API_CHANNEL_ORDER_INSTALMENT_DATA_NOT_COMPLETE, "获取api渠道订单时，orderInstalment数据缺失, thirdPartyOrderId:{}, channel:{}, orderInstalment:{}", thirdPartyOrderId, channel, JsonUtils.toString(orderInstalment));
    }

    if (!CashLoanOrderStatus.isPayoutStatus(orderInstalment.orderVO.status)) {
      throw EcException.error(EcExceptionType.API_CHANNEL_GET_REPAYMENT_PLAN_ORDER_STATUS_ERROR, "获取api渠道订单时，cash loan order status状态异常, thirdPartyOrderId:{}, channel:{}", thirdPartyOrderId, channel);
    }

    if (CollectionUtils.isEmpty(orderInstalment.instalmentVOS)) {
      throw EcException.error(EcExceptionType.API_CHANNEL_GET_REPAYMENT_PLAN_NO_INSTALMENTS, "api渠道订单查询还款计划时, 已打款的cash loan订单没有找到账单信息! thirdPartyOrderId:{}, channel:{}, cashLoanOrderId:{}", thirdPartyOrderId, channel, orderInstalment.orderVO.id);
    }
  }

  private void checkOrderCompleted(String thirdPartyOrderId, OrderInstalment orderInstalment) {
    if (CashLoanOrderStatus.COMPLETE == orderInstalment.orderVO.status) {
      throw EcException.error(EcExceptionType.API_CHANNEL_ORDER_COMPLETED_CANNOT_REPAY, "当前订单为:{}状态, 不支持继续还款!thirdPartyOrderId:{}", orderInstalment.orderVO.status.desc, thirdPartyOrderId);
    }
  }

  public RepaymentAccountVO getRepaymentAccount(Long apiChannelOrderId, BigDecimal repaymentAmount, String vaChannel) {
    ApiChannelOrderVO apiChannelOrderVO = apiChannelOrderService.getByIdOrThrow(apiChannelOrderId);
    CashLoanOrderVO orderVO = orderService.getOrderVO(apiChannelOrderVO.getOrderId());
    RepaymentAccountVO repaymentAccountVO = repaymentAccountService.getRepaymentAccountByChannel(orderVO.sdkType, PaymentAccount.getDefaultAccount(orderVO.sdkType), vaChannel, repaymentAmount, orderVO, RepaymentAccountCallerType.DEFAULT);
    monitorService.logApiChannelRepayment(orderVO, repaymentAccountVO);
    return repaymentAccountVO;
  }

  public ApiChannelLoanRepaymentResultVO getRepayment(String thirdPartyOrderId, ApiChannel channel) {
    return getRepayments(Collections.singletonList(thirdPartyOrderId), channel);
  }

  public ApiChannelLoanRepaymentResultVO getRepayments(List<String> thirdPartyOrderIds, ApiChannel channel) {
    List<ApiChannelOrderVO> apiChannelOrderVOList = apiChannelOrderService.findByThirdPartyOrderIdsAndChannel(thirdPartyOrderIds, channel);
    if (CollectionUtils.isEmpty(apiChannelOrderVOList)) {
      throw EcException.error(EcExceptionType.API_CHANNEL_ORDER_NOT_EXIST, "query apiChannelOrders empty! thirdPartyOrderIds:{}, channel:{}", JsonUtils.toString(thirdPartyOrderIds), channel);
    }
    return getRepaymentsByOrderId(apiChannelOrderVOList);
  }

  public ApiChannelLoanRepaymentResultVO getRepayments(List<Long> apiChannelOrderIds) {
    List<ApiChannelOrderVO> apiChannelOrderVOList = apiChannelOrderService.getByIds(apiChannelOrderIds);
    if (CollectionUtils.isEmpty(apiChannelOrderVOList)) {
      throw EcException.error(EcExceptionType.API_CHANNEL_ORDER_NOT_EXIST, "query apiChannelOrders empty! apiChannelOrderIds:{}", JsonUtils.toString(apiChannelOrderIds));
    }
    return getRepaymentsByOrderId(apiChannelOrderVOList);
  }

  public ApiChannelLoanRepaymentResultVO getRepaymentsByOrderId(List<ApiChannelOrderVO> apiChannelOrderVOList) {
    List<Long> orderIds = apiChannelOrderVOList.stream().map(ApiChannelOrderVO::getOrderId).filter(Objects::nonNull).collect(Collectors.toList());
    if (CollectionUtils.isEmpty(orderIds)) {
      return new ApiChannelLoanRepaymentResultVO();
    }

    Map<Long, List<RepaymentVO>> orderIdRepaymentMap = cashLoanRepaymentService.getOrderIdRepaymentMap(orderIds, CashLoanRepaymentType.NORMAL);
    List<ApiChannelLoanRepaymentVO> apiChannelLoanRepaymentVOList = apiChannelOrderVOList.stream().map(apiChannelOrderVO -> ApiChannelLoanRepaymentVO.from(apiChannelOrderVO, orderIdRepaymentMap.get(apiChannelOrderVO.getOrderId()))).collect(Collectors.toList());

    List<String> paymentTransIds = apiChannelLoanRepaymentVOList
        .stream()
        .map(ApiChannelLoanRepaymentVO::getRepaymentVOS)
        .filter(CollectionUtils::isNotEmpty)
        .flatMap(Collection::stream)
        .map(repaymentVO -> repaymentVO.paymentTransId)
        .collect(Collectors.toList());
    Map<String, String> repayTranIdRepayChannelMap = paymentService.getRepayTransIdRepayChannelMap(paymentTransIds);
    return new ApiChannelLoanRepaymentResultVO(apiChannelLoanRepaymentVOList, repayTranIdRepayChannelMap);
  }

  public ApiChannelLoanEachRepaymentVO getRepaymentByApiChannelRepaymentId(Long apiChannelRepaymentId, ApiChannel channel) {
    ApiChannelRepaymentVO apiChannelRepaymentVO = findByApiChannelRepaymentId(apiChannelRepaymentId);
    ICredential credentialVO = paymentService.getCredentialInfo(new PaymentCredential(apiChannelRepaymentVO.getRepaymentMethod(), apiChannelRepaymentVO.getRepaymentCredentialId()));

    if (Objects.isNull(apiChannelRepaymentVO.getRepaymentId())) {
      return ApiChannelLoanEachRepaymentVO.from(apiChannelRepaymentVO, credentialVO);
    }

    RepaymentVO repaymentVO = cashLoanRepaymentService.getRepaymentVOByIdOrThrow(apiChannelRepaymentVO.getRepaymentId());
    List<RepaymentUnitVO> repaymentUnitVOList = cashLoanRepaymentService.getRepaymentUnitVOListByRepaymentId(apiChannelRepaymentVO.getRepaymentId());

    Set<Long> orderIds = repaymentUnitVOList.stream().map(RepaymentUnitVO::getOrderId).collect(Collectors.toSet());

    Set<Long> installmentIds = repaymentUnitVOList.stream().map(RepaymentUnitVO::getInstalmentId).collect(Collectors.toSet());

    Map<Long, ApiChannelOrderVO> orderIdAndApiChannelOrderMap = apiChannelOrderService.findListByOrderIdsAndChannel(orderIds, channel).stream().collect(Collectors.toMap(ApiChannelOrderVO::getOrderId, Function.identity()));
    Map<Long, CashLoanInstalmentVO> instalmentIdAndInstalmentMap = cashLoanInstalmentService.findListByInstalmentIds(installmentIds).stream().collect(Collectors.toMap(CashLoanInstalmentVO::getId, Function.identity()));

    // 过滤一下, 只保留api渠道的订单, key: orderId, value: instalmentIds
    Map<Long, Set<Long>> orderIdAndInstalmentIdsMap = repaymentUnitVOList
        .stream()
        .filter(repaymentUnitVO -> orderIdAndApiChannelOrderMap.containsKey(repaymentUnitVO.getOrderId()))
        .collect(Collectors.groupingBy(RepaymentUnitVO::getOrderId, Collectors.mapping(RepaymentUnitVO::getInstalmentId, Collectors.toSet())));

    List<ApiChannelLoanEachRepaymentVO.ApiChannelOrderInfoForEachRepayment> apiOrderInfoListForEachRepayment = orderIdAndInstalmentIdsMap.entrySet()
        .stream()
        .map(entry -> {
          Long orderId = entry.getKey();
          Set<Long> instalmentIds = entry.getValue();
          ApiChannelOrderVO apiChannelOrderVO = orderIdAndApiChannelOrderMap.get(orderId);
          List<CashLoanInstalmentVO> instalmentVOList = instalmentIds.stream().map(instalmentIdAndInstalmentMap::get).collect(Collectors.toList());
          List<RepaymentUnitVO> targetRepaymentUnitVOList = repaymentUnitVOList.stream().filter(repaymentUnitVO -> Objects.equals(repaymentUnitVO.getOrderId(), orderId)).collect(Collectors.toList());
          return new ApiChannelLoanEachRepaymentVO.ApiChannelOrderInfoForEachRepayment(apiChannelOrderVO, instalmentVOList, targetRepaymentUnitVOList);
        })
        .collect(Collectors.toList());

    return new ApiChannelLoanEachRepaymentVO(apiChannelRepaymentVO, credentialVO, repaymentVO, apiOrderInfoListForEachRepayment);
  }


  public ApiChannelLoanEachRepaymentStandVO getRepaymentStandVOBylRepaymentId(RepaymentVO repaymentVO, ApiChannelRepaymentVO apiChannelRepaymentVO) {
    Long repaymentId = repaymentVO.getId();
    // 分期的还款记录
    List<RepaymentUnitVO> repaymentUnitVOList = cashLoanRepaymentService.getRepaymentUnitVOListByRepaymentId(repaymentId);

    // 根据分期的还款记录获取分期表
    Set<Long> installmentIds = repaymentUnitVOList.stream().map(RepaymentUnitVO::getInstalmentId).collect(Collectors.toSet());
    List<CashLoanInstalmentVO> instalmentViewVOS = cashLoanInstalmentService.findListByInstalmentIds(installmentIds)
        .stream()
        .map(instalmentVO -> {
          return instalmentCutInterestCouponDeductDetailService.getViewInstalment(instalmentVO);
        })
        .collect(Collectors.toList());

    return new ApiChannelLoanEachRepaymentStandVO(repaymentVO, apiChannelRepaymentVO, repaymentUnitVOList, instalmentViewVOS);
  }

  /**
   * 动态还款发起还款的时候写入, 此时还没有repaymentId
   */
  public ApiChannelRepaymentVO insertApiChannelRepayment(Long userId,
                                                         ApiChannel channel,
                                                         String thirdPartyRepaymentNo,
                                                         String repaymentNo,
                                                         Long repaymentCredentialId,
                                                         PaymentMethod paymentMethod) {
    ApiChannelRepaymentRecord apiChannelRepaymentRecord = apiChannelRepaymentModel.insert(userId,
        channel,
        thirdPartyRepaymentNo,
        repaymentNo,
        repaymentCredentialId,
        paymentMethod);
    return ApiChannelRepaymentVO.fromOrThrow(apiChannelRepaymentRecord);
  }

  /**
   * 静态还款成功的时候写入,包含repaymentId
   */
  public ApiChannelRepaymentVO insertApiChannelRepayment(Long userId,
                                                         ApiChannel channel,
                                                         Long repaymentCredentialId,
                                                         PaymentMethod paymentMethod,
                                                         Long repaymentId) {
    String repaymentNo = idGeneratorService.genAutoIncrIdStr(Purpose.LAZADA_REPAYMENT_NO) + "_" + Clock.now();
    ApiChannelRepaymentRecord apiChannelRepaymentRecord = apiChannelRepaymentModel.insert(userId,
        channel,
        repaymentNo,
        repaymentNo,
        repaymentCredentialId,
        paymentMethod,
        repaymentId);
    return ApiChannelRepaymentVO.fromOrThrow(apiChannelRepaymentRecord);
  }

  public ApiChannelRepaymentVO findByThirdPartyRepaymentNoAndChannel(String thirdPartyRepaymentNo, ApiChannel apiChannel) {
    ApiChannelRepaymentRecord record = apiChannelRepaymentModel.findByThirdPartyRepaymentNoAndChannel(thirdPartyRepaymentNo, apiChannel);
    if (record == null) {
      throw EcException.error("cannot find ApiChannelRepaymentRecord, thirdPartyRepaymentNo is {}, channel is {}", thirdPartyRepaymentNo, apiChannel);
    }
    return ApiChannelRepaymentVO.fromOrThrow(record);
  }

  public ApiChannelRepaymentVO findByRepaymentNoAndChannel(String repaymentNo, ApiChannel apiChannel) {
    ApiChannelRepaymentRecord record = apiChannelRepaymentModel.findByRepaymentNoAndChannel(repaymentNo, apiChannel);
    if (record == null) {
      throw EcException.error("cannot find ApiChannelRepaymentRecord, repaymentNo is {}, channel is {}", repaymentNo, apiChannel);
    }
    return ApiChannelRepaymentVO.fromOrThrow(record);
  }

  public ApiChannelRepaymentVO findByApiChannelRepaymentId(Long apiChannelRepaymentId) {
    ApiChannelRepaymentRecord record = apiChannelRepaymentModel.findById(apiChannelRepaymentId);
    if (record == null) {
      throw EcException.error("cannot find ApiChannelRepaymentRecord, id is {}", apiChannelRepaymentId);
    }
    return ApiChannelRepaymentVO.fromOrThrow(record);
  }

  public List<Long> findIdsByConditionWithoutLimit(ApiChannelRepaymentConditionVO conditionVO) {
    return apiChannelRepaymentModel.findIdsByConditionWithoutLimit(conditionVO);
  }


}
