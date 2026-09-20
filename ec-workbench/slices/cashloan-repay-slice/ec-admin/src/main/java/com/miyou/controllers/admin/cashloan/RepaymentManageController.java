package com.miyou.controllers.admin.cashloan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.miyou.controllers.admin.cashloan.request.MarkInstalmentCompleteRequest;
import com.miyou.controllers.admin.cashloan.request.RepaymentForM2BatchMarkCompleteRequest;
import com.miyou.controllers.admin.cashloan.request.RepaymentManageMarkCompleteRequest;
import com.miyou.controllers.admin.cashloan.response.*;
import com.miyou.controllers.admin.operationlog.utilities.EcAdminOperationLog;
import com.miyou.controllers.admin.review.decorator.MarkCompleteDecorator;
import com.miyou.controllers.core.YqgBaseController;
import com.yqg.chidori.client.spring.internalReview.PostMessage;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.model.sql.cashloan.CashLoanSearchCondition;
import com.yqg.core.model.sql.cashloan.enums.CashLoanRepaymentStatus;
import com.yqg.core.model.sql.operationlog.enums.LogAccountType;
import com.yqg.core.model.sql.user.UserSearchCondition;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.cashloan.instalmentcutcoupon.InstalmentCutInterestCouponDeductDetailService;
import com.yqg.core.service.cashloan.manualdeduction.CashLoanManualDeductionService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.core.service.cashloan.repay.CashLoanRepaymentService;
import com.yqg.core.service.cashloan.vo.*;
import com.yqg.core.service.cashloan.vo.orderview.CashLoanInstalmentViewVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.additioanalinfo.LoanUserAdditionalInfoService;
import com.yqg.core.service.loan.credits.LoanUserCreditsService;
import com.yqg.core.service.loan.manualreduction.ManualReductionTaskService;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionTaskVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserSimpleCreditsInfoVO;
import com.yqg.core.service.loan.vo.UserInfoVO;
import com.yqg.core.service.ly.LyAdminService;
import com.yqg.core.service.ly.LyConfig;
import com.yqg.core.service.payment.ICredential;
import com.yqg.core.service.payment.PaymentCredential;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.refund.RefundService;
import com.yqg.core.service.payment.refund.enums.RefundStatus;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.core.service.payment.vo.ThirdPartyPaymentInfo;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.UserSimpleInfoVO;
import com.yqg.customer.client.api.ITicketClientService;
import com.yqg.customer.common.vo.TicketClientVo;
import com.yqg.ec.common.enums.CashLoanManualDeductionReferer;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.i18n.time.EcTimeZone;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.spring.response.EcResponseUtil;
import com.yqg.ec.common.spring.response.Result;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by YZY on 16/6/28.
 */
@RestController
public class RepaymentManageController extends YqgBaseController {
  @Autowired
  private CashLoanService cashLoanService;
  @Autowired
  private UserService userService;
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private CashLoanManualDeductionService manualDeductionService;
  @Autowired
  private CashLoanRepaymentService repaymentService;
  @Autowired
  private LyAdminService lyAdminService;
  @Autowired
  private LyConfig lyConfig;
  @Autowired
  private LoanUserCreditsService loanUserCreditsService;
  @Autowired
  private RefundService refundService;
  @Autowired
  private ManualReductionTaskService manualReductionTaskService;
  @Autowired
  private ITicketClientService ticketClientService;
  @Autowired
  private LoanUserAdditionalInfoService loanUserAdditionalInfoService;
  @Autowired
  private InstalmentCutInterestCouponDeductDetailService interestCouponDeductDetailService;

  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.QUERY')")
  @GetMapping(path = "/admin/operation/cashLoan/repayment/listOrders")
  public Result listOrders(
      @RequestParam("pageNo") Integer pageNo,
      @RequestParam("pageSize") Integer pageSize,
      @RequestParam(value = "mobileNumbers", required = false) String mobileNumbers,
      @RequestParam(value = "loanAccountId", required = false) Long loanAccountId,
      @RequestParam(value = "identityNumber", required = false) String identityNumber,
      @RequestParam(value = "sdkType", required = false) String sdkType,
      @RequestParam(value = "isCompleted", required = false) String isCompleted,
      @RequestParam(value = "isOverdue", required = false) String isOverdue,
      @RequestParam(value = "startOverdueTime", required = false) Long startOverdueTime,
      @RequestParam(value = "endOverdueTime", required = false) Long endOverdueTime,
      @RequestParam(value = "overdueDays", required = false) Integer overdueDays
  ) {
    CashLoanSearchCondition searchCondition = getSearchCondition(
        mobileNumbers, loanAccountId, identityNumber, sdkType, isCompleted, isOverdue, overdueDays, startOverdueTime, endOverdueTime);
    int limit = pageSize;
    int offset = (pageNo - 1) * pageSize;

    boolean isLyAdminUser = lyConfig.isLyAdminUser(getAdminUserEmail());
    int totalCount;
    BigDecimal totalRemainAmount;
    List<CashLoanOrderDetailVO> cashLoanOrderDetailVOList;
    RepaymentManageRecordsResponse response;
    if (isLyAdminUser) {
      totalCount = lyAdminService.getRepaymentTotalCount(searchCondition);
      totalRemainAmount = lyAdminService.getRepaymentTotalRemainAmount(searchCondition);
      cashLoanOrderDetailVOList = lyAdminService.searchCashLoanOrders(offset, limit, searchCondition);
      response = RepaymentManageRecordsResponse.fromLy(cashLoanOrderDetailVOList, totalCount, totalRemainAmount);
    } else {
      totalCount = ecOrderService.getTotalCountByConditions(searchCondition);
      Assert.assertTrue("添加具体的查询条件",totalCount <= 2000);
      List<Long> orderIds = ecOrderService.getOrderIdsByConditions(searchCondition);
      BigDecimal totalRemainAmountByConditions = ecOrderService.getTotalRemainAmountByConditions(searchCondition);
      BigDecimal couponDeductionAmount = interestCouponDeductDetailService.calcInitCutAmountByOrderId(orderIds);
      totalRemainAmount = BigDecimalHelper.substractNullAsZeroAndScale(totalRemainAmountByConditions, couponDeductionAmount);
      cashLoanOrderDetailVOList = cashLoanService.searchCashLoanOrdersByConditions(offset, limit, searchCondition);
      response = RepaymentManageRecordsResponse.from(cashLoanOrderDetailVOList, totalCount, totalRemainAmount);
    }

    return EcResponseUtil.generate(response);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.QUERY')")
  @GetMapping(path = "/admin/operation/cashLoan/repayment/listInstalments/{orderId}")
  public Result listInstalments(@PathVariable("orderId") Long orderId) {
    OrderInstalment orderInstalment = ecOrderService.getOrderInstalment(orderId);
    Map<Long, List<RepaymentUnitVO>> orderIdRepaymentUnitVOsMap = cashLoanService.getRepaymentUnitVOsByOrderIdsAndStatus(Collections.singleton(orderId), CashLoanRepaymentStatus.SUCCEED);
    List<RepaymentUnitVO> repaymentUnitVOs = orderIdRepaymentUnitVOsMap.getOrDefault(orderId, Collections.emptyList());
    Map<Long, List<RepaymentUnitVO>> instalmentIdRepaymentUnitVOsMap = repaymentUnitVOs
        .stream()
        .collect(Collectors.groupingBy(
            vo -> vo.instalmentId,
            Collectors.mapping(vo -> vo, Collectors.toList())));
    CashLoanInstalmentResponse response = CashLoanInstalmentResponse.from(interestCouponDeductDetailService.getViewInstalments(orderInstalment.instalmentVOS), instalmentIdRepaymentUnitVOsMap);
    return EcResponseUtil.generate(response);
  }


  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.QUERY')")
  @GetMapping(path = "/admin/operation/cashLoan/repayment/listOrderInstalments")
  public Result listOrderInstalments(
      @RequestParam(value = "mobileNumbers", required = false) String mobileNumbers,
      @RequestParam(value = "loanAccountId", required = false) Long loanAccountId,
      @RequestParam(value = "identityNumber", required = false) String identityNumber,
      @RequestParam(value = "sdkType", required = false) String sdkType,
      @RequestParam(value = "manualReductionTaskId", required = false) Long manualReductionTaskId) {
    if (!ObjectUtils.anyNotNull(mobileNumbers, loanAccountId, identityNumber, manualReductionTaskId)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("请输入查询条件"));
    }
    if (Objects.nonNull(loanAccountId) && !loanAccountService.existLoanAccount(loanAccountId)) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("该用户不存在，loanAccountId:{0}", loanAccountId.toString()));
    }
    List<Long> instalmentIds = new ArrayList<>();
    List<Long> orderIds = new ArrayList<>();
    TicketClientVo ticketClientVo = null;
    ManualReductionTaskVO manualReductionTaskVO = null;
    if (manualReductionTaskId != null) {
      manualReductionTaskVO = manualReductionTaskService.findById(manualReductionTaskId);
      ticketClientVo = ticketClientService.fetchByNumber(manualReductionTaskVO.getCsTaskNo());
      instalmentIds.addAll(manualReductionTaskVO.obtainAllInstalmentIds());
      orderIds.addAll(manualReductionTaskVO.obtainAllOrderIds());
    }


    List<Long> orderIdRes = getCashLoanOrderDetailVOS(mobileNumbers, loanAccountId, identityNumber, sdkType, orderIds);

    CashLoanOrderInstalmentResponse cashLoanOrderInstalmentResponse = getCashLoanOrderInstalmentResponse(instalmentIds, orderIds, ticketClientVo, orderIdRes, manualReductionTaskVO);

    return EcResponseUtil.generate(cashLoanOrderInstalmentResponse);
  }

  @NotNull
  private CashLoanOrderInstalmentResponse getCashLoanOrderInstalmentResponse(List<Long> instalmentIds, List<Long> orderIds, TicketClientVo ticketClientVo,
                                                                             List<Long> orderIdRes, ManualReductionTaskVO manualReductionTaskVO) {
    Map<Long, List<RepaymentUnitVO>> orderIdRepaymentUnitVOsMap = cashLoanService.getRepaymentUnitVOsByOrderIdsAndStatus(orderIds, CashLoanRepaymentStatus.SUCCEED);
    Map<Long, OrderInstalment> orderInstalments = ecOrderService.getOrderInstalments(orderIdRes);
    Long loanAccountId = orderInstalments.values().stream().findFirst().map(item -> item.orderVO.accountId).orElse(null);
    String userName = Optional.ofNullable(loanUserAdditionalInfoService.genLoanUserInfoVO(loanAccountId)).map(UserInfoVO::getName).orElse("");
    CashLoanOrderInstalmentResponse cashLoanOrderInstalmentResponse = new CashLoanOrderInstalmentResponse(ticketClientVo, loanAccountId, userName, manualReductionTaskVO);
    orderIdRes.forEach(orderId -> {
      OrderInstalment orderInstalment = orderInstalments.get(orderId);
      if (Objects.isNull(orderInstalment)) {
        return;
      }
      List<RepaymentUnitVO> repaymentUnitVOs = orderIdRepaymentUnitVOsMap.getOrDefault(orderId, Collections.emptyList());
      Map<Long, List<RepaymentUnitVO>> instalmentIdRepaymentUnitVOsMap = repaymentUnitVOs
          .stream()
          .collect(Collectors.groupingBy(
              vo -> vo.instalmentId,
              Collectors.mapping(vo -> vo, Collectors.toList())));
      List<CashLoanInstalmentVO> cashLoanInstalmentVOS = orderInstalment.instalmentVOS.stream()
          .filter(item -> CollectionUtils.isEmpty(instalmentIds) || instalmentIds.contains(item.id))
          .filter(item -> (Objects.isNull(manualReductionTaskVO) && item.status == CashLoanInstalmentStatus.INIT) || Objects.nonNull(manualReductionTaskVO))
          .collect(Collectors.toList());
      if (CollectionUtils.isEmpty(cashLoanInstalmentVOS)) {
        return;
      }
      List<CashLoanInstalmentViewVO> viewInstalments = interestCouponDeductDetailService.getViewInstalments(cashLoanInstalmentVOS);
      cashLoanOrderInstalmentResponse.addOrderInstalment(orderId, CashLoanInstalmentResponse.from(viewInstalments, instalmentIdRepaymentUnitVOsMap));
    });
    return cashLoanOrderInstalmentResponse;
  }

  private List<Long> getCashLoanOrderDetailVOS(String mobileNumbers, Long loanAccountId, String identityNumber, String sdkType, List<Long> orderIds) {
    String completed = CollectionUtils.isNotEmpty(orderIds) ? null : Boolean.FALSE.toString();
    CashLoanSearchCondition searchCondition = getSearchCondition(
        mobileNumbers, loanAccountId, identityNumber, sdkType, completed, null, null, null, null);
    int limit = Integer.MAX_VALUE;
    int offset = 0;

    boolean isLyAdminUser = lyConfig.isLyAdminUser(getAdminUserEmail());
    List<CashLoanOrderDetailVO> cashLoanOrderDetailVOList;
    if (isLyAdminUser) {
      cashLoanOrderDetailVOList = lyAdminService.searchCashLoanOrders(offset, limit, searchCondition);
    } else {
      cashLoanOrderDetailVOList = cashLoanService.searchCashLoanOrdersByConditions(offset, limit, searchCondition);
    }
    return cashLoanOrderDetailVOList
        .stream()
        .map(item -> item.cashLoanOrderVO.id)
        .filter(item -> CollectionUtils.isEmpty(orderIds) || orderIds.contains(item))
        .collect(Collectors.toList());
  }

  private UserSearchCondition getUserSearchCondition(
      String mobileNumbers,
      String identityNumber,
      SDKType sdkType
  ) {
    List<String> mobiles = new ArrayList<>();
    if (!StringUtils.isBlank(mobileNumbers)) {
      mobiles = JsonUtils.fromOrException(mobileNumbers, new TypeReference<ArrayList<String>>() {
      });
      if (mobiles.size() > 1000) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("Could query by up to 1000 mobile numbers."));
      }
    }

    UserSearchCondition condition = new UserSearchCondition();
    condition.mobileNumbers = mobiles;
    condition.identityNumber = identityNumber;
    condition.sdkType = sdkType;

    return condition;
  }

  private CashLoanSearchCondition getSearchCondition(
      String mobileNumbers,
      Long loanAccountId,
      String identityNumber,
      String sdkType,
      String isCompleted,
      String isOverdue,
      Integer overdueDays,
      Long startOverdueTime,
      Long endOverdueTime
  ) {
    List<Long> userIds = null;
    if (Objects.nonNull(loanAccountId)) {
      userIds = Optional.ofNullable(loanAccountService.getLoanAccountVO(loanAccountId))
          .map(item -> Collections.singletonList(item.userId))
          .orElseThrow(() -> EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("未找到相关的订单, 请检查查询条件")));
    }

    SDKType sdk = StringUtils.isBlank(sdkType) ? null : SDKType.valueOf(sdkType);
    if (!(StringUtils.isBlank(mobileNumbers) && StringUtils.isBlank(identityNumber))) {
      UserSearchCondition userCondition = getUserSearchCondition(mobileNumbers, identityNumber, sdk);
      if (sdk != null && sdk.isIdnSDKType()) {
        List<String> normalizedMobileNumberList = new ArrayList<>();
        List<String> mobileNumberList = userCondition.mobileNumbers;
        mobileNumberList.forEach(mobileNumber -> {
          if (!mobileNumber.startsWith("+")) {
            mobileNumber = MobileConverter.nationalToNormalizedOrThrow(sdk.getLocale(), mobileNumber);
          }
          normalizedMobileNumberList.add(mobileNumber);
        });
        userCondition.mobileNumbers = normalizedMobileNumberList;
      }
      userIds = userService.fetchIdsByConditions(userCondition);
      if (CollectionUtils.isEmpty(userIds)) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("未找到相关的订单, 请检查查询条件"));
      }
    }

    List<CashLoanOrderStatus> statuses = new ArrayList<>();
    Boolean completed = StringUtils.isBlank(isCompleted) ? null : Boolean.valueOf(isCompleted);
    if (completed != null) {
      if (completed) {
        statuses.add(CashLoanOrderStatus.COMPLETE);
      } else {
        statuses.add(CashLoanOrderStatus.READY);
      }
    } else {
      statuses.add(CashLoanOrderStatus.COMPLETE);
      statuses.add(CashLoanOrderStatus.READY);
    }

    CashLoanSearchCondition condition = new CashLoanSearchCondition();
    condition.sdkType = sdk;
    condition.userIds = userIds;
    condition.statuses = statuses;
    condition.overdueDays = overdueDays;
    condition.isOverdue = StringUtils.isBlank(isOverdue) ? null : Boolean.valueOf(isOverdue);
    condition.startOverdueTime = startOverdueTime;
    condition.endOverdueTime = endOverdueTime;

    return condition;
  }

  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.QUERY')")
  @GetMapping(path = "/admin/operation/cashLoan/repayment/getOrderDetail")
  public Result getOrderDetail(
      @RequestParam("orderId") Long orderId
  ) {
    CashLoanOrderDetailVO cashLoanOrderDetailVO = cashLoanService.getCashLoanOrderDetailVObyOrderId(orderId);

    RepaymentManageRecordResponse response = RepaymentManageRecordResponse.from(cashLoanOrderDetailVO);
    return EcResponseUtil.generate(response);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.QUERY')")
  @GetMapping(path = "/admin/operation/cashLoan/repayment/exportOrdersByCondition")
  public Result exportOrdersByCondition(
      @RequestParam(value = "mobileNumbers", required = false) String mobileNumbers,
      @RequestParam(value = "loanAccountId", required = false) Long loanAccountId,
      @RequestParam(value = "identityNumber", required = false) String identityNumber,
      @RequestParam(value = "sdkType", required = false) String sdkType,
      @RequestParam(value = "isCompleted", required = false) String isCompleted,
      @RequestParam(value = "isOverdue", required = false) String isOverdue,
      @RequestParam(value = "startOverdueTime", required = false) Long startOverdueTime,
      @RequestParam(value = "endOverdueTime", required = false) Long endOverdueTime,
      @RequestParam(value = "overdueDays", required = false) Integer overdueDays
  ) {
    CashLoanSearchCondition searchCondition = getSearchCondition(
        mobileNumbers, loanAccountId, identityNumber, sdkType, isCompleted, isOverdue, overdueDays, startOverdueTime, endOverdueTime);
    int totalCount = ecOrderService.getTotalCountByConditions(searchCondition);
    if (totalCount > 1000) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("Could export up to 1000 records."));
    }

    List<CashLoanOrderDetailVO> cashLoanOrderDetailVOList = cashLoanService.searchCashLoanOrdersByConditions(searchCondition);
    List<RepaymentManageRecordResponse> recordList = cashLoanOrderDetailVOList
        .stream()
        .map(RepaymentManageRecordResponse::from)
        .collect(Collectors.toList());

    List<LinkedHashMap<String, Object>> dataList = Lists
        .transform(recordList, RepaymentManageRecordResponse::convertToMapForExport);
    String fileName = String.format(
        "订单信息_%s.xlsx", Clock.nowDate(EcTimeZone.getDefaultTimeZone()).toString("yyyy-MM-dd_HH-mm-ss")
    );

    return EcResponseUtil.originOK(generateExcel(dataList), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fileName);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.PROCESS')")
  @PostMapping(path = "/admin/operation/cashLoan/repayment/markInstalmentComplete")
  @PostMessage(decoratorBean = MarkCompleteDecorator.class, template = "mark_order_instalment.ftl")
  public Result markInstalmentComplete(@RequestBody @Valid MarkInstalmentCompleteRequest request) {
    manualDeductionService.markInstalmentComplete(request.instalmentId, getAdminUserEmail(), request.remark, CashLoanManualDeductionReferer.ADMIN_MARK_INSTALMENT_COMPLETE, request.classification);
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.PROCESS')")
  @PostMapping(path = "/admin/operation/cashLoan/repayment/markComplete")
  @PostMessage(decoratorBean = MarkCompleteDecorator.class, template = "mark_order_instalment.ftl")
  public Result markComplete(@RequestBody @Valid RepaymentManageMarkCompleteRequest request) {
    manualDeductionService.markOrderComplete(request.orderId, getAdminUserEmail(), request.remark, CashLoanManualDeductionReferer.ADMIN_MARK_ORDER_COMPLETE, request.classification);
    return EcResponseUtil.generateSuccess();
  }

  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.PROCESS')")
  @PostMapping(path = "/admin/operation/cashLoan/repayment/markM2Complete")
  @PostMessage(decoratorBean = MarkCompleteDecorator.class, template = "mark_order_instalment.ftl")
  public Result batchMarkComplete(@RequestBody @Valid RepaymentForM2BatchMarkCompleteRequest request) {
    List<Long> orderIdList = Arrays.stream(request.orderIds.split(",")).map(Long::parseLong).collect(Collectors.toList());
    List<Long> successfulIdList = manualDeductionService.batchMarkOrderComplete(orderIdList, getAdminUserEmail(), request.remark, CashLoanManualDeductionReferer.ADMIN_BATCH_MARK_M2_ORDER_COMPLETE, request.classification);
    Collection<Long> failedIdList = ListUtils.removeAll(orderIdList, successfulIdList);
    String resultStr = CollectionUtils.isEmpty(failedIdList) ? "手动减免成功！" : "手动减免出错，未成功减免的订单如下: " + Joiner.on(",").join(failedIdList);
    return EcResponseUtil.generate(resultStr);
  }

  @PreAuthorize("hasAnyAuthority('LOAN.ORDER.PROCESS')")
  @GetMapping(path = "/admin/operation/cashLoan/repayment/markComplete/classification")
  public Result getMarkCompleteClassificationOptions(@RequestParam(value = "referer", required = false) CashLoanManualDeductionReferer referer) {
    List<String> options = manualDeductionService.getManualDeductionClassificationOptions(referer);
    return EcResponseUtil.generate(CashLoanMarkCompleteClassificationResponse.from(options));
  }

  @EcAdminOperationLog(logAccountType = LogAccountType.CHIDORI, operation = "查询退款申请")
  @PreAuthorize("hasAnyAuthority('CAPITAL.REFUND.APPROVE','CAPITAL.REFUND.QUERY')")
  @GetMapping(path = "/admin/operation/cashLoan/repayment/listForRefund")
  public Result listForRefund(
      @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
      @RequestParam(value = "loanAccountId", required = false) Long loanAccountId
  ) {
    Long userId = null;
    if (Objects.nonNull(mobileNumber)) {
      UserSimpleInfoVO userSimpleInfoVO = userService.fetchByNormalizedMobileNumber(mobileNumber, getSDKType());
      userId = Optional.ofNullable(userSimpleInfoVO)
          .map(item -> item.userId)
          .orElseThrow(() -> EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("mobileNumber does not exist")));
    }

    if (Objects.nonNull(loanAccountId)) {
      LoanAccountVO loanAccountVO = loanAccountService.getLoanAccountVO(loanAccountId);
      userId = Optional.ofNullable(loanAccountVO)
          .map(item -> item.userId)
          .orElseThrow(() -> EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("loanAccountId does not exist")));
    }

    if (Objects.isNull(userId)) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("user does not exist"));
    }

    LoanUserSimpleCreditsInfoVO creditsInfoVO = loanUserCreditsService.getLoanUserSimpleCreditsInfoVOByUserId(userId);
    if (creditsInfoVO == null || creditsInfoVO.hasNotBeenAccepted()) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_PARAM_TOAST, TT.gen("user credits is not accepted!"));
    }
    LoanAccountVO loanAccountVO = loanAccountService.getAccountByUserIdOrThrow(userId, getSDKType());

    List<PaymentInfoResponse> overFlowResponseThatRepayToOrder = getOverFlowResponseThatRepayToOrder(loanAccountVO);
    List<PaymentInfoResponse> overFlowResponseThatNotRepayToOrder = getOverFlowResponseThatNotRepayToOrder(loanAccountVO);

    List<PaymentInfoResponse> tempResponseList = Lists.newArrayList();
    tempResponseList.addAll(overFlowResponseThatRepayToOrder);
    tempResponseList.addAll(overFlowResponseThatNotRepayToOrder);

    PaymentInfoListResponse response = new PaymentInfoListResponse();
    // 统一按照接收到第三方还款确认信息时间由近到远排序(从大到小排序)
    response.infoResponseList = tempResponseList.stream().sorted(Comparator.comparingLong(r -> -r.timeCreated)).collect(Collectors.toList());
    return EcResponseUtil.generate(response);
  }

  /**
   * 所有还到订单上之后仍然有溢出金额的还款。还款还到订单上，就说明有相应的 CashLoanRepaymentRecord 存在
   *
   * @param loanAccountVO
   * @return
   */
  private List<PaymentInfoResponse> getOverFlowResponseThatRepayToOrder(LoanAccountVO loanAccountVO) {
    // 所有有溢出金额的 CashLoanRepaymentRecord ，组装成 Map<transId, RepaymentVO>
    Map<String, RepaymentVO> overFlowTransIdToRepaymentMap = repaymentService.findAllOverFlowRepaymentVOMap(loanAccountVO.id);

    // 每个 transId 对应的 Payment 表中的记录 ，组装成 Map<transId, PaymentVO>
    Map<String, PaymentVO> overFlowTransIdPaymentVOMap = paymentService.getPaymentVOMaps(overFlowTransIdToRepaymentMap.keySet());


    // 每次溢出还款对应的还款最后一笔订单（一笔还款可以换多个订单），因为要按照订单打款方式进行退款，所以需要获取一个OrderVO
    Map<String, Long> overFlowTransIdToLastedOrderIdMap = overFlowTransIdToRepaymentMap.entrySet().stream().collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> repaymentService.findLastedRepayOrderIdWithRepaymentId(entry.getValue().id))
    );

    // 一次性查出所有 OrderVOList，组装成 Map<orderId, orderVO>
    Map<Long, CashLoanOrderVO> allLastedOrderIdToOrderVOMap = ecOrderService.getOrderVOList(overFlowTransIdToLastedOrderIdMap.values())
        .stream()
        .collect(Collectors.toMap(vo -> vo.id, vo -> vo));

    // 组装 Map<transId, orderVO>
    Map<String, CashLoanOrderVO> overFlowTransIdToLastedOrderVOMap = overFlowTransIdToLastedOrderIdMap.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> allLastedOrderIdToOrderVOMap.get(entry.getValue())));
    Map<String, ThirdPartyPaymentInfo> transIdToThirdPartyPaymentMap = paymentService.get3rdPartyPaymentInfo(loanAccountVO.sdkType, overFlowTransIdPaymentVOMap.values());

    return overFlowTransIdToRepaymentMap.entrySet()
        .stream()
        .map(entry -> {
          String transId = entry.getKey();
          RepaymentVO repaymentVO = entry.getValue();

          PaymentVO paymentVO = overFlowTransIdPaymentVOMap.get(transId);
          CashLoanOrderVO orderVO = overFlowTransIdToLastedOrderVOMap.get(transId);
          PaymentCredential refundCredential = getRefundCredential(paymentVO, orderVO);
          ThirdPartyPaymentInfo thirdPartyPaymentInfo = transIdToThirdPartyPaymentMap.get(paymentVO.thirdPartyPayOrderId);
          RefundStatus refundStatus = refundService.getRefundStatusByPaymentTransId(transId);

          return PaymentInfoResponse.fromRepayPaymentVOWhichRepayToOrder(loanAccountVO.id, repaymentVO, paymentVO, refundCredential, thirdPartyPaymentInfo, refundStatus);
        }).collect(Collectors.toList());
  }

  /**
   * 所有没有还到订单上，产生溢出金额的还款。只有 Payment 表记录，没有 CashLoanRepaymentRecord 存在
   *
   * @param loanAccountVO
   * @return
   */
  private List<PaymentInfoResponse> getOverFlowResponseThatNotRepayToOrder(LoanAccountVO loanAccountVO) {
    // 退款使用默认打款路径
    PaymentCredential defaultCredential = getDefaultCredential(loanAccountVO.userId, loanAccountVO.sdkType);
    List<PaymentVO> repayPaymentVOsWhichNotRepayToOrder = paymentService.getRepayPaymentVOsWhichNotRepayToOrder(loanAccountVO.userId, loanAccountVO.sdkType);
    Map<String, ThirdPartyPaymentInfo> transIdToThirdPartyPaymentMap = paymentService.get3rdPartyPaymentInfo(loanAccountVO.sdkType, repayPaymentVOsWhichNotRepayToOrder);

    return repayPaymentVOsWhichNotRepayToOrder.stream()
        .map(paymentVO -> PaymentInfoResponse.fromRepayPaymentVOWhichNotRepayToOrder(
            loanAccountVO.id,
            paymentVO,
            defaultCredential,
            transIdToThirdPartyPaymentMap.get(paymentVO.thirdPartyPayOrderId),
            refundService.getRefundStatusByPaymentTransId(paymentVO.thirdPartyPayOrderId)))
        .collect(Collectors.toList());
  }

  private PaymentCredential getDefaultCredential(Long userId, SDKType sdkType) {
    Map<PaymentMethod, List<ICredential>> credentialMap = paymentService.getPayoutCredentialInfo(userId, sdkType);
    Map.Entry<PaymentMethod, List<ICredential>> entry = credentialMap.entrySet().iterator().next();
    return new PaymentCredential(entry.getKey(), entry.getValue().get(0).getId());
  }

  private PaymentCredential getRefundCredential(PaymentVO paymentVO, CashLoanOrderVO orderVO) {
    switch (paymentVO.paymentCredential.getMethod()) {
      case VIRTUAL_ACCOUNT:
      case DYNAMIC_ACCOUNT:
      case PHI_PAYMENT_INVOICE:
      case DIRECT_DEBIT:
        /**
         *  采用虚拟账号还款，退款到打款账号
         */
        return orderVO.paymentCredential;
      default:
        throw EcException.error("paymentMethod not supported for id = " + paymentVO.id);
    }
  }
}
