package com.yqg.core.service.cashloan.risk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yqg.core.model.generated.tables.records.*;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.MultiLoanStatusLogModel;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.model.sql.loan.coupon.LoanUserCouponModel;
import com.yqg.core.model.sql.loan.coupon.enums.LoanCouponStatus;
import com.yqg.core.model.sql.loanusertrace.TriggerSubType;
import com.yqg.core.model.sql.risk.RiskIncreaseCreditsReviewLogModel;
import com.yqg.core.model.sql.risk.RiskOutputResultCondition;
import com.yqg.core.model.sql.risk.RiskOutputResultModel;
import com.yqg.core.model.sql.risk.enums.RiskIncreaseCreditsReviewStatus;
import com.yqg.core.service.advertisement.AdAppsflyerService;
import com.yqg.core.service.advertisement.vo.AdAppsflyerVO;
import com.yqg.core.service.cashloan.ordercenter.CashLoanOrderAdditionalInfoService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.CashLoanOrderAdditionalInfoVO;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.risk.vo.*;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.coupon.LoanUserCouponService;
import com.yqg.core.service.loan.coupon.vo.LoanUserCouponVO;
import com.yqg.core.service.loan.coupon.vos.LoanCutInterestCouponVO;
import com.yqg.core.service.notification.NotificationService;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.notification.param.system.NotifIncreaseCreditsRejectParam;
import com.yqg.core.service.notification.param.system.NotifyMarketingBatchTaskCompleteParam;
import com.yqg.core.service.risk.batchtrigger.MarketingRiskBatchTriggerService;
import com.yqg.core.service.risk.batchtrigger.vo.MarketingRiskFlowVO;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.UserLoginInfoVO;
import com.yqg.ec.common.enums.LoanCouponUsageType;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.overseas.ads.client.api.IAdsDeviceService;
import com.yqg.overseas.ads.client.common.request.device.AdsDeviceQueryRequest;
import com.yqg.overseas.ads.client.common.response.AdsResponse;
import com.yqg.overseas.ads.client.common.vo.AdsDeviceInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shubo
 * @date 2022/6/30 7:22 下午
 */
@Service
@Slf4j
public class RiskDetailService {
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private CashLoanOrderAdditionalInfoService cashLoanOrderAdditionalInfoService;
  @Autowired
  private RiskOutputResultModel riskOutputResultModel;
  @Autowired
  private RiskDataTransferService riskDataTransferService;
  @Autowired
  private MultiLoanStatusLogModel multiLoanStatusLogModel;
  @Autowired
  private LoanUserCouponModel loanUserCouponModel;
  @Autowired
  private LoanUserCouponService loanUserCouponService;
  @Autowired
  private RiskIncreaseCreditsReviewLogModel riskIncreaseCreditsReviewLogModel;
  @Autowired
  private NotificationService notificationService;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private UserService userService;
  @Autowired
  private IAdsDeviceService adsDeviceService;
  @Autowired
  private AdAppsflyerService adAppsflyerService;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private MarketingRiskBatchTriggerService marketingRiskBatchTriggerService;

  public RiskUserOrderDetailVO getRiskUserOrderDetailVO(Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = new RiskUserOrderDetailVO();
    riskUserOrderDetailVO.riskOrderDetailVOList = getRiskOrderDetailVOList(loanAccountId);
    riskUserOrderDetailVO.riskOutputResultDetailVOList = getRiskOutputResultDetailVOList(loanAccountId);
    return riskUserOrderDetailVO;
  }

  public RiskUserOrderDetailVO getRiskUserOrderDetailVOV2(Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = new RiskUserOrderDetailVO();
    riskUserOrderDetailVO.riskOrderDetailVOList = getRiskOrderDetailVOListV2(loanAccountId);
    return riskUserOrderDetailVO;
  }

  /**
   * 仅返回订单信息，不查询分期和 additional info，性能优于 V2
   *
   * @param loanAccountId 账户 ID
   * @return 仅含订单的 RiskUserOrderDetailVO，instalmentVOS 和 riskOrderAdditionalVO 为 null
   */
  public RiskUserOrderDetailVO getRiskUserOrderInfoOnlyVO(Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = new RiskUserOrderDetailVO();
    riskUserOrderDetailVO.riskOrderDetailVOList = getRiskOrderInfoOnlyList(loanAccountId);
    return riskUserOrderDetailVO;
  }

  public RiskUserOrderDetailVO getRiskUserOrderDetailVOSimple(Long loanAccountId) {
    RiskUserOrderDetailVO riskUserOrderDetailVO = new RiskUserOrderDetailVO();
    riskUserOrderDetailVO.riskOrderDetailVOList = getRiskOrderDetailVOListSimple(loanAccountId);
    return riskUserOrderDetailVO;
  }

  public List<RiskMultiLoanLogVO> getRiskMultiLoanLogVOList(Long loanAccountId, Long startTime, Long endTime) {
    List<RiskMultiLoanLogVO> riskMultiLoanLogVOList = new ArrayList<>();
    List<MultiLoanStatusLogRecord> multiLoanStatusLogRecords = multiLoanStatusLogModel.fetchByLoanAccountIdAndIntervalTime(loanAccountId, startTime, endTime);
    multiLoanStatusLogRecords.forEach(multiLoanStatusLogRecord -> {
      riskMultiLoanLogVOList.add(RiskMultiLoanLogVO.from(multiLoanStatusLogRecord));
    });
    return riskMultiLoanLogVOList;
  }

  public void updateStatusForRiskIncreaseCreditsRiskReject(Long traceId, LoanCreditsStatus loanCreditsStatus) {
    List<RiskIncreaseCreditsReviewLogRecord> riskIncreaseCreditsReviewLogRecordList = riskIncreaseCreditsReviewLogModel.findByTraceId(traceId);
    if (CollectionUtils.isEmpty(riskIncreaseCreditsReviewLogRecordList)) {
      return;
    }
    if (riskIncreaseCreditsReviewLogRecordList.size() > 1) {
      log.error("current user exits many traceid,it is error, traceId is {}", traceId);
      return;
    }
    RiskIncreaseCreditsReviewLogRecord riskIncreaseCreditsReviewLogRecord = riskIncreaseCreditsReviewLogRecordList.get(0);
    if (RiskIncreaseCreditsReviewStatus.fromCode(riskIncreaseCreditsReviewLogRecord.getStatus()) == RiskIncreaseCreditsReviewStatus.SUBMIT_RISK) {
      riskIncreaseCreditsReviewLogModel.updateStatus(riskIncreaseCreditsReviewLogRecordList.get(0), RiskIncreaseCreditsReviewStatus.RISK_COMPLETE);
      if (loanCreditsStatus == LoanCreditsStatus.ACCEPTED) {
        Long loanAccountId = riskIncreaseCreditsReviewLogRecord.getLoanAccountId();
        LoanAccountRecord loanAccountRecord = loanAccountModel.findById(loanAccountId);
        NotifIncreaseCreditsRejectParam notifIncreaseCreditsRejectParam = new NotifIncreaseCreditsRejectParam(loanAccountRecord.getUserId(), SDKType.fromCode(loanAccountRecord.getSdkType()), riskIncreaseCreditsReviewLogRecord.getId());
        notificationService.pushSystemNotif(SystemNotifScene.INCREASE_CREDITS_REJECT_PASS, notifIncreaseCreditsRejectParam);
      }
    }
  }

  public void notifyBatchTaskComplete(long traceId, Long userId, SDKType sdkType) {
    LoanUserRiskTraceVO traceVo = loanUserRiskTraceService.findByTraceIdOrThrow(traceId);
    if (traceVo.triggerSubType == TriggerSubType.MARKETING) {
      Pair<Long, MarketingRiskFlowVO> riskFlowVOPair = marketingRiskBatchTriggerService.findMarketingRiskFlowByTraceId(traceId);
      NotifyMarketingBatchTaskCompleteParam notifyMarketingBatchTaskCompleteParam = new NotifyMarketingBatchTaskCompleteParam(userId, sdkType, riskFlowVOPair.getLeft(), riskFlowVOPair.getRight(), traceId);
      notificationService.pushSystemNotif(SystemNotifScene.NOTIFY_BATCH_TASK_COMPLETED, notifyMarketingBatchTaskCompleteParam);
    }
  }

  private List<RiskOrderDetailVO> getRiskOrderDetailVOList(Long loanAccountId) {
    List<RiskOrderDetailVO> orderDetailVOList = new ArrayList<>();
    List<OrderInstalment> orderInstalments = ecOrderService.listAllOrderInstalment(loanAccountId);
    List<Long> orderIds = orderInstalments.stream().map(orderInstalment -> orderInstalment.orderVO.id).collect(Collectors.toList());
    Map<Long, List<CashLoanOrderAdditionalInfoVO>> additionalInfoServiceByOrderIds = cashLoanOrderAdditionalInfoService.findByOrderIds(orderIds);
    orderInstalments.forEach(orderInstalment -> {
      RiskOrderDetailVO orderDetailVO = new RiskOrderDetailVO();
      orderDetailVO.orderVO = orderInstalment.orderVO;
      orderDetailVO.instalmentVOS = orderInstalment.instalmentVOS;
      //order_addition_info 信息
      orderDetailVO.riskOrderAdditionalVO = riskDataTransferService.getRiskOrderAdditionalVO(additionalInfoServiceByOrderIds.get(orderInstalment.orderVO.id));
      //额外计算的一些信息
      wrapRiskOrderAdditionalVO(orderDetailVO.riskOrderAdditionalVO, orderInstalment.orderVO);
      orderDetailVOList.add(orderDetailVO);
    });
    return orderDetailVOList;
  }

  private List<RiskOrderDetailVO> getRiskOrderDetailVOListV2(Long loanAccountId) {
    List<RiskOrderDetailVO> orderDetailVOList = new ArrayList<>();
    List<OrderInstalment> orderInstalments = ecOrderService.listAllOrderInstalment(loanAccountId);
    List<Long> orderIds = orderInstalments.stream().map(orderInstalment -> orderInstalment.orderVO.id).collect(Collectors.toList());
    Map<Long, List<CashLoanOrderAdditionalInfoVO>> additionalInfoServiceByOrderIds = cashLoanOrderAdditionalInfoService.findByOrderIds(orderIds);
    orderInstalments.forEach(orderInstalment -> {
      RiskOrderDetailVO orderDetailVO = new RiskOrderDetailVO();
      orderDetailVO.orderVO = orderInstalment.orderVO;
      orderDetailVO.instalmentVOS = orderInstalment.instalmentVOS;
      //order_addition_info 信息
      orderDetailVO.riskOrderAdditionalVO = riskDataTransferService.getRiskOrderAdditionalVO(additionalInfoServiceByOrderIds.get(orderInstalment.orderVO.id));
      orderDetailVOList.add(orderDetailVO);
    });
    return orderDetailVOList;
  }

  private List<RiskOrderDetailVO> getRiskOrderDetailVOListSimple(Long loanAccountId) {
    List<RiskOrderDetailVO> orderDetailVOList = new ArrayList<>();
    List<OrderInstalment> orderInstalments = ecOrderService.listAllOrderInstalment(loanAccountId);
    List<Long> orderIds = orderInstalments.stream().map(orderInstalment -> orderInstalment.orderVO.id).collect(Collectors.toList());
    Map<Long, List<CashLoanOrderAdditionalInfoVO>> additionalInfoServiceByOrderIds = cashLoanOrderAdditionalInfoService.findByOrderIds(orderIds);
    orderInstalments.forEach(orderInstalment -> {
      RiskOrderDetailVO orderDetailVO = new RiskOrderDetailVO();
      orderDetailVO.orderVO = orderInstalment.orderVO;
      orderDetailVO.instalmentVOS = orderInstalment.instalmentVOS;
      orderDetailVOList.add(orderDetailVO);
    });
    return orderDetailVOList;
  }

  private List<RiskOrderDetailVO> getRiskOrderInfoOnlyList(Long loanAccountId) {
    List<CashLoanOrderVO> orderVOs = ecOrderService.getCashLoanOrdersByLoanAccountIdAndStatuses(loanAccountId);
    return orderVOs.stream().map(orderVO -> {
      RiskOrderDetailVO orderDetailVO = new RiskOrderDetailVO();
      orderDetailVO.orderVO = orderVO;
      return orderDetailVO;
    }).collect(Collectors.toList());
  }

  private void wrapRiskOrderAdditionalVO(RiskOrderAdditionalVO riskOrderAdditionalVO, CashLoanOrderVO orderVO) {
    if (Objects.isNull(riskOrderAdditionalVO)) {
      return;
    }
    //降息券使用的额度，目前取绑定或者使用的最后一个
    List<LoanUserCouponRecord> loanUserCouponRecords = loanUserCouponModel.listByOrderIdAndUsageTypeAndStatuses(orderVO.id, LoanCouponUsageType.CUT_INTEREST, Arrays.asList(LoanCouponStatus.PENDING, LoanCouponStatus.USED));
    loanUserCouponRecords.stream()
        .sorted(Comparator.comparing(LoanUserCouponRecord::getId).reversed())
        .findFirst()
        .ifPresent(loanUserCouponRecord -> {
          LoanUserCouponVO loanUserCouponVO = loanUserCouponService.genLoanUserCouponVOWithConfig(loanUserCouponRecord);
          OrderInstalment orderInstalment = ecOrderService.getOrderInstalment(orderVO.id);
          Map<Integer, BigDecimal> termIndex2PostInterest = orderInstalment.genTermIndex2PostInterest();
          BigDecimal cutInterest = ((LoanCutInterestCouponVO) loanUserCouponVO).genCutInterestAmount(orderVO.interest, orderVO.postInterest, orderVO.days.intValue(), termIndex2PostInterest, orderVO.sdkType);
          riskOrderAdditionalVO.cutInterest = cutInterest;
        });
  }

  public List<RiskOutputResultDetailVO> getRiskOutputResultDetailVOList(Long loanAccountId) {
    List<RiskOutputResultRecord> riskOutputResultRecordList = riskOutputResultModel.findByLoanAccountId(loanAccountId);
    return getRiskOutputResultDetailVOS(riskOutputResultRecordList);
  }

  public List<RiskOutputResultDetailVO> getRiskOutputResultDetailVOListByTraceIds(List<Long> traceIds) {
    List<RiskOutputResultRecord> riskOutputResultRecordList = riskOutputResultModel.fetchByTraceIds(traceIds);
    return getRiskOutputResultDetailVOS(riskOutputResultRecordList);
  }

  private List<RiskOutputResultDetailVO> getRiskOutputResultDetailVOS(List<RiskOutputResultRecord> riskOutputResultRecordList) {
    List<RiskOutputResultDetailVO> riskOutputResultDetailVOList = new ArrayList<>();
    Map<Long, List<RiskOutputResultRecord>> riskOutputResultVOMap = riskOutputResultRecordList.stream().filter(Objects::nonNull)
        .collect(Collectors.groupingBy(RiskOutputResultRecord::getTraceId));

    riskOutputResultVOMap.forEach((traceId, riskOutputResultListByTraceId) -> {
      RiskOutputResultDetailVO riskOutputResultDetailVO = riskDataTransferService.getRiskOutputResultDetailVO(riskOutputResultListByTraceId);
      riskOutputResultDetailVO.traceId = traceId;
      riskOutputResultDetailVOList.add(riskOutputResultDetailVO);
    });
    return riskOutputResultDetailVOList;
  }

  public RiskUserDeviceInfoVo getLatestRiskUserDeviceInfo(Long userId, Long maxQueryTime) {
    UserLoginInfoVO userLoginInfoVO = userService.getLatestUserLoginDetailVO(userId, maxQueryTime);
    AdAppsflyerVO adAppsflyerVO = null;
    if (userLoginInfoVO == null) {
      adAppsflyerVO = adAppsflyerService.fetchByUserIdOrNull(userId);
      if (adAppsflyerVO == null || adAppsflyerVO.getPushBody() == null) {
        return null;
      }
      log.info("getLatestRiskUserDeviceInfo adAppsflyerVO: {}", JsonUtils.toString(adAppsflyerVO));
    }
    AdsDeviceQueryRequest adsDeviceQueryRequest;
    if (userLoginInfoVO != null) {
      adsDeviceQueryRequest = new AdsDeviceQueryRequest(userLoginInfoVO.getDeviceId(), userLoginInfoVO.getDeviceType(), maxQueryTime);
    } else {
      Map<String, String> pushMap = JsonUtils.from(adAppsflyerVO.getPushBody(), new TypeReference<Map<String, String>>() {
      });
      String deviceId;
      String deviceType;
      if (StringUtils.isNotEmpty(pushMap.get("advertising_id"))) {
        deviceId = pushMap.get("advertising_id");
        deviceType = "gaid";
      } else {
        deviceId = pushMap.get("idfa");
        deviceType = "idfa";
      }
      adsDeviceQueryRequest = new AdsDeviceQueryRequest(deviceId, deviceType, maxQueryTime);
    }
    if (StringUtils.isEmpty(adsDeviceQueryRequest.getDeviceId())) {
      log.info("getLatestRiskUserDeviceInfo user deviceId not exist userId: {}", userId);
      return null;
    }
    AdsResponse<AdsDeviceInfoVo> adsDeviceInfoVoAdsResponse = adsDeviceService.queryAdsDeviceInfo(adsDeviceQueryRequest);
    if (adsDeviceInfoVoAdsResponse.status.code != 0) {
      log.error("IAdsDeviceService.queryAdsDeviceInfo failed, adsDeviceQueryRequest {}, response : {}", JsonUtils.toString(adsDeviceQueryRequest), JsonUtils.toString(adsDeviceInfoVoAdsResponse));
      return null;
    }
    log.info("IAdsDeviceService.queryAdsDeviceInfo success!, adsDeviceQueryRequest {}, response {}", JsonUtils.toString(adsDeviceQueryRequest), JsonUtils.toString(adsDeviceInfoVoAdsResponse.body));
    AdsDeviceInfoVo adsDeviceInfoVo = adsDeviceInfoVoAdsResponse.body;
    return RiskUserDeviceInfoVo.from(adsDeviceInfoVo);
  }

  public List<RiskOutputResultSimpleVO> fetchRiskOutputResultByCondition(RiskOutputResultCondition riskOutputResultCondition) {
    return riskOutputResultModel.fetchByCondition(riskOutputResultCondition)
        .stream()
        .map(RiskOutputResultSimpleVO::fromOrNull)
        .collect(Collectors.toList());
  }
}
