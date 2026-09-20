package com.yqg.core.service.directdebit;

import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.common.enums.BusinessName;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.DirectDebitPaymentRecord;
import com.yqg.core.model.generated.tables.records.DirectDebitUserTaskFlowRecord;
import com.yqg.core.model.sql.directdebit.DirectDebitPaymentModel;
import com.yqg.core.model.sql.directdebit.DirectDebitUserTaskFlowModel;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitAccountStatus;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitPaymentStatus;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitPaymentType;
import com.yqg.core.model.sql.directdebit.enums.DirectDebitRepaymentTaskFlowStatus;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.payment.PaymentModel;
import com.yqg.core.model.sql.payment.enums.PaymentStatus;
import com.yqg.core.model.sql.payment.enums.PaymentTransType;
import com.yqg.core.service.directdebit.enums.DirectDebitAuthorizationBusinessType;
import com.yqg.core.service.directdebit.enums.DirectDebitFailedReason;
import com.yqg.core.service.directdebit.enums.DirectDebitNextStep;
import com.yqg.core.service.directdebit.enums.DirectDebitProvider;
import com.yqg.core.service.directdebit.enums.OpDirectDebitPaymentStatus;
import com.yqg.core.service.directdebit.factory.DirectDebitBusinessTypeHandlerFactory;
import com.yqg.core.service.directdebit.factory.IDirectDebitAccountMethod;
import com.yqg.core.service.directdebit.factory.LinkAccountFactory;
import com.yqg.core.service.directdebit.handler.DirectDebitBusinessTypeHandler;
import com.yqg.core.service.directdebit.vo.*;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.PaymentCredential;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.pm.pmenum.PaymentErrorCode;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.service.tool.IdGeneratorService;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.overseas.client.spring.api.payment.IOverseasPaymentDirectDebitService;
import com.yqg.overseas.spring.request.directdebit.DirectDebitOtpRequest;
import com.yqg.overseas.spring.request.directdebit.DirectDebitPaymentRequest;
import com.yqg.overseas.spring.response.directdebit.DirectDebitPaymentResponse;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author chaoye
 * @date 2024/8/19
 */
@Service
@Slf4j
public class DirectDebitRepaymentService {
  @Autowired
  private DirectDebitPaymentModel directDebitPaymentModel;
  @Autowired
  private PaymentModel paymentModel;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private DirectDebitConfig directDebitConfig;
  @Autowired
  private DirectDebitAccountLocker directDebitAccountLocker;
  @Autowired
  private IOverseasPaymentDirectDebitService overseasPaymentDirectDebitService;
  @Autowired
  private IdGeneratorService idGeneratorService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private DirectDebitAccountService directDebitAccountService;
  @Autowired
  private DirectDebitUserTaskFlowModel directDebitUserTaskFlowModel;
  @Autowired
  private DirectDebitCreditEntrySupport directDebitCreditEntrySupport;


  public List<DirectDebitPaymentVO> findByTransNoList(Collection<String> transNoList) {
    List<DirectDebitPaymentRecord> recordList = directDebitPaymentModel.findByTransNoList(transNoList);
    return recordList
        .stream()
        .map(DirectDebitPaymentVO::from)
        .collect(Collectors.toList());
  }

  public Map<String, DirectDebitPaymentVO> findMapByTransNoList(Collection<String> transNoList) {
    List<DirectDebitPaymentVO> paymentVOList = findByTransNoList(transNoList);

    return paymentVOList
        .stream()
        .collect(Collectors.toMap(o -> o.transNo, o -> o));
  }

  public Optional<DirectDebitPaymentVO> findByTransNo(String transNo) {
    DirectDebitPaymentRecord record = directDebitPaymentModel.findByTransNo(transNo);
    return Optional.ofNullable(record).map(DirectDebitPaymentVO::from);
  }

  public DirectDebitPaymentVO getPaymentVOById(Long id) {
    DirectDebitPaymentRecord record = directDebitPaymentModel.findByIdOrThrow(id);
    DirectDebitPaymentVO paymentVO = DirectDebitPaymentVO.from(record);
    return paymentVO;
  }

  /**
   * 代扣支付 - 用户任务Flow
   */
  public void createPaymentByAutoForUserTaskFlow(
      DirectDebitPaymentType type,
      DirectDebitLinkAccountVO accountVO,
      DirectDebitUserTaskFlowRecord flowRecord
  ) {
    DirectDebitAuthorizationBusinessType bizType = DirectDebitAuthorizationBusinessType.fromCode(flowRecord.getBusinessType());
    directDebitAccountService.checkBankCanBeUsedByBiz(bizType, accountVO.provider, accountVO.bankType);
    directDebitAccountLocker.lockAndRun(DirectDebitUtils.getUserUniqueKey(accountVO.userId, accountVO.sdkType), () -> {
      checkCurrentUnfinishedPaymentRecordsAndThrowWarn(accountVO.userId, accountVO.sdkType);

      DirectDebitBusinessTypeHandler handler = DirectDebitBusinessTypeHandlerFactory.getHandler(bizType);

      BigDecimal needRepayAmount = handler.calculateNeedRepayAmount(accountVO.userId, accountVO.sdkType, Clock.now());
      if (!preHandlePaymentForUserTaskFlow(accountVO, flowRecord, needRepayAmount)) {
        return;
      }

      DirectDebitPaymentRecord directDebitPaymentRecord = threadTransactionalModel.transactionResult(config -> {
        DirectDebitUserTaskFlowRecord freshRecord = directDebitUserTaskFlowModel.findByIdOrThrow(flowRecord.getId());
        directDebitUserTaskFlowModel.updateStatus(freshRecord, DirectDebitRepaymentTaskFlowStatus.PAYING);
        if (bizType == DirectDebitAuthorizationBusinessType.EC) {
          paymentModel.insert(PaymentTransType.REPAY, PaymentAccount.getDefaultAccount(accountVO.sdkType), accountVO.userId,
              PaymentBusinessName.fromSDkTypeOrNull(accountVO.sdkType), freshRecord.getTransNo(),
              new PaymentCredential(PaymentMethod.DIRECT_DEBIT, accountVO.id), CurrencyAmount.fromYuan(EcCurrency.IDR, freshRecord.getAmount()),
              PaymentStatus.PENDING, null, null, PaymentErrorCode.NO_ERROR);
        }
        return directDebitPaymentModel.insert(accountVO.userId, freshRecord.getTransNo(), type, DirectDebitPaymentStatus.INIT, freshRecord.getAmount(), EcCurrency.IDR, accountVO.id, null);
      });

      DirectDebitPaymentResponse response = createPaymentByOp(accountVO.id, flowRecord.getTransNo(), directDebitPaymentRecord.getAmount(), null);
      log.info("createPaymentByOp response, transNo={}, flowId={}, userId={}, status={}, paymentProvider={}",
          flowRecord.getTransNo(), flowRecord.getId(), accountVO.userId,
          response != null ? response.getStatus() : "null",
          response != null ? response.getPaymentProvider() : "null");
      handlePaymentResponse(directDebitPaymentRecord, response);
    });
  }

  private boolean preHandlePaymentForUserTaskFlow(DirectDebitLinkAccountVO accountVO, DirectDebitUserTaskFlowRecord flowRecord, BigDecimal needRepayAmount) {
    BigDecimal prevOverdueAmount = flowRecord.getAmount();
    BigDecimal finalDirectDebitAmount = BigDecimalHelper.min(needRepayAmount, prevOverdueAmount);
    DirectDebitUserTaskFlowRecord freshRecord = directDebitUserTaskFlowModel.findByIdOrThrow(flowRecord.getId());
    if (0 == BigDecimalHelper.compareTo(finalDirectDebitAmount, BigDecimal.ZERO)) {
      directDebitUserTaskFlowModel.updateStatus(freshRecord, DirectDebitRepaymentTaskFlowStatus.PAYMENT_FAILED);
      log.info("Direct debit user task flow status has been ended because of no overdue amount, flowId: {}, userId: {}", flowRecord.getId(), accountVO.userId);
      return false;
    }
    if (BigDecimalHelper.compareTo(finalDirectDebitAmount, prevOverdueAmount) < 0) {
      directDebitUserTaskFlowModel.updateAmount(freshRecord, finalDirectDebitAmount);
      log.info("Direct debit user task flow amount has been decreased, flowId: {}, userId: {}, finalAmount: {}, prevAmount: {}", flowRecord.getId(), accountVO.userId, finalDirectDebitAmount, prevOverdueAmount);
    }
    IDirectDebitAccountMethod directDebitAccountMethod = LinkAccountFactory.getMethod(accountVO.provider);
    DirectDebitAuthorizationBusinessType bizType = DirectDebitAuthorizationBusinessType.fromCode(flowRecord.getBusinessType());
    if (!directDebitAccountMethod.checkCanTransfer(bizType, accountVO, finalDirectDebitAmount)) {
      directDebitUserTaskFlowModel.updateStatus(freshRecord, DirectDebitRepaymentTaskFlowStatus.PAYMENT_FAILED);
      log.info("Direct debit user task flow status has been ended because of insufficient balance, flowId: {}, userId: {}", flowRecord.getId(), accountVO.userId);
      return false;
    }
    return true;
  }

  /**
   * 代扣支付 - 用户发起（默认 EC 业务线，兼容旧接口）
   */
  public DirectDebitPaymentVO createPaymentByUser(Long linkAccountId, BigDecimal amount, String overseasPaymentProvider) {
    return createPaymentByUser(linkAccountId, amount, overseasPaymentProvider, DirectDebitAuthorizationBusinessType.EC, DirectDebitPaymentType.USER_ACTION);
  }

  /**
   * 代扣支付 - 用户发起
   */
  public DirectDebitPaymentVO createPaymentByUser(Long linkAccountId, BigDecimal amount, String overseasPaymentProvider,
      DirectDebitAuthorizationBusinessType businessType, DirectDebitPaymentType paymentType) {
    DirectDebitAuthorizationBusinessType bizType = businessType != null ? businessType : DirectDebitAuthorizationBusinessType.EC;
    DirectDebitLinkAccountVO accountVO = directDebitAccountService.findAccountVOByIdOrThrow(linkAccountId);
    directDebitAccountService.checkBankCanBeUsedByBiz(bizType, accountVO.provider, accountVO.bankType);
    String transNo = idGeneratorService.genId(IdGeneratorService.Type.DIRECT_DEBIT_PAYMENT);
    return directDebitAccountLocker.lockAndRunResult(DirectDebitUtils.getUserUniqueKey(accountVO.userId, accountVO.sdkType), () -> {
      if (paymentType == DirectDebitPaymentType.USER_ACTION) {
        checkCurrentUnfinishedPaymentRecordsAndThrowWarn(accountVO.userId, accountVO.sdkType);
      }

      DirectDebitPaymentRecord directDebitPaymentRecord = threadTransactionalModel.transactionResult(config -> {
        paymentModel.insert(PaymentTransType.REPAY,
            PaymentAccount.getDefaultAccount(accountVO.sdkType),
            accountVO.userId,
            PaymentBusinessName.fromSDkTypeOrNull(accountVO.sdkType),
            transNo,
            new PaymentCredential(PaymentMethod.DIRECT_DEBIT, accountVO.id),
            CurrencyAmount.fromYuan(EcCurrency.IDR, amount),
            PaymentStatus.PENDING,
            null,
            null,
            PaymentErrorCode.NO_ERROR
        );
        return directDebitPaymentModel.insert(accountVO.userId, transNo, paymentType, DirectDebitPaymentStatus.INIT, amount, EcCurrency.IDR, accountVO.id, null);

      });

      DirectDebitPaymentResponse response = createPaymentByOp(linkAccountId, transNo, amount, overseasPaymentProvider);
      DirectDebitPaymentVO latestAccountVO = handlePaymentResponse(directDebitPaymentRecord, response);
      handlePaymentFailedResult(latestAccountVO, false);

      return latestAccountVO;
    });

  }

  /**
   * 代扣支付 - 校验otp
   */
  public DirectDebitPaymentVO paymentOtpAuthorize(Long userId, SDKType sdkType, String transNo, String otp) {
    return directDebitAccountLocker.lockAndRunResult(DirectDebitUtils.getUserUniqueKey(userId, sdkType), () -> {

      DirectDebitPaymentRecord record = directDebitPaymentModel.findByTransNo(transNo);
      DirectDebitPaymentVO currentPaymentVO = DirectDebitPaymentVO.from(record);

      //可能因为网络原因，导致用户上次输入otp的时候，表中状态已经是成功，但是给用户报了网络异常
      //这种情况下直接返回当前vo
      if (currentPaymentVO.status == DirectDebitPaymentStatus.SUCCESS) {
        return currentPaymentVO;
      }
      //如果当前状态已经是failed了，用户依然会停留在otp页面，可以输入otp，这种情况要给用户提示
      if (currentPaymentVO.status == DirectDebitPaymentStatus.FAILED) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("验证码已失效，请重新获取"));
      }

      if (currentPaymentVO.status != DirectDebitPaymentStatus.PENDING || currentPaymentVO.additionalInfo.nextStep != DirectDebitNextStep.INPUT_OTP) {
        log.error("current payment record status invalid, id:{}, status:{}, nextStep:{}", currentPaymentVO.id, currentPaymentVO.status, currentPaymentVO.additionalInfo.nextStep);
        return currentPaymentVO;
      }
      //调用op
      DirectDebitPaymentResponse response = paymentOtpAuthorizeByOp(record, otp);

      //处理response，获取最新状态的vo
      DirectDebitPaymentVO latestAccountVO = handlePaymentResponse(record, response);
      //处理失败的情况，给用户提示
      handlePaymentFailedResult(latestAccountVO, true);

      return latestAccountVO;
    });

  }

  /**
   * 代扣支付 - 轮询
   */
  public DirectDebitPaymentVO queryPaymentByUser(String transNo) {
    DirectDebitPaymentVO paymentVO = getPaymentVOByTransNo(transNo);
    handlePaymentFailedResult(paymentVO, false);
    return paymentVO;
  }

  public DirectDebitPaymentVO getPaymentVOByTransNo(String transNo) {
    DirectDebitPaymentRecord record = directDebitPaymentModel.findByTransNo(transNo);
    DirectDebitPaymentVO paymentVO = DirectDebitPaymentVO.from(record);
    return paymentVO;
  }

  private DirectDebitPaymentResponse createPaymentByOp(Long accountId, String transNo, BigDecimal amount, String overseasPaymentProvider) {
    DirectDebitPaymentRequest request = new DirectDebitPaymentRequest();
    request.accountBusinessId = accountId.toString();
    request.businessName = BusinessName.EC.name();
    request.amount = amount;
    request.transNo = transNo;
    request.currency = com.yqg.overseas.common.i18n.EcCurrency.IDR;
    request.provider = overseasPaymentProvider;
    return overseasPaymentDirectDebitService.createDebitPayment(request);
  }

  private DirectDebitPaymentResponse paymentOtpAuthorizeByOp(DirectDebitPaymentRecord record, String otp) {
    DirectDebitOtpRequest request = new DirectDebitOtpRequest(record.getLinkAccountId().toString(), BusinessName.EC.name(), record.getTransNo(), otp);
    return overseasPaymentDirectDebitService.createDebitPaymentOtp(request);
  }

  private DirectDebitPaymentVO handlePaymentResponse(DirectDebitPaymentRecord record, DirectDebitPaymentResponse response) {
    DirectDebitPaymentStatus latestStatus = DirectDebitPaymentStatus.convertFromOp(OpDirectDebitPaymentStatus.valueOf(response.getStatus()));
    DirectDebitLinkAccountVO accountVO = directDebitAccountService.findAccountVOByIdOrThrow(record.getLinkAccountId());
    IDirectDebitAccountMethod method = LinkAccountFactory.getMethod(accountVO.provider);
    DirectDebitPaymentAdditionalInfo additionalInfo = method.checkAdditionalInfo(latestStatus, response);

    if (!DirectDebitPaymentStatus.FINAL_STATUS_LIST.contains(latestStatus)) {
      record = directDebitCreditEntrySupport.updateDirectDebitPaymentByLatestStatus(record, latestStatus, additionalInfo);
    } else {
      record = directDebitCreditEntrySupport.syncFinalStatusPayment(record.getTransNo(), latestStatus, PaymentProvider.valueOf(response.getPaymentProvider()), response.getTransactionTime(), additionalInfo);
    }

    return DirectDebitPaymentVO.from(record);

  }


  private void handlePaymentFailedResult(DirectDebitPaymentVO directDebitPaymentVO, boolean allUseToast) {
    DirectDebitLinkAccountVO accountVO = directDebitAccountService.findAccountVOByIdOrThrow(directDebitPaymentVO.linkAccountId);
    IDirectDebitAccountMethod method = LinkAccountFactory.getMethod(accountVO.provider);
    method.handlePaymentFailedResult(directDebitPaymentVO, allUseToast);
  }

  //检查用户当前是否有进行中的代扣交易
  private void checkCurrentUnfinishedPaymentRecordsAndThrowWarn(Long userId, SDKType sdkType) {
    boolean hasUnfinishedPaymentRecordsForCheck = hasUnfinishedPaymentRecordsForCheck(userId, sdkType);
    if (hasUnfinishedPaymentRecordsForCheck) {
      throw EcException.warn(EcExceptionType.DIRECT_DEBIT_CURRENT_PAYMENT_NOT_FINISHED, TT.gen("当前有在进行中的代扣还款，请稍后再试"));
    }
  }

  public boolean hasUnfinishedPaymentRecordsForCheck(Long userId, SDKType sdkType) {
    if (!directDebitConfig.checkNotFinishPayment()) {
      return false;
    }
    List<DirectDebitLinkAccountVO> linkAccountVOS = directDebitAccountService.findByUserIdAndSdkType(userId, sdkType);
    List<Long> linkAccountIds = linkAccountVOS.stream().map(DirectDebitLinkAccountVO::getId).collect(Collectors.toList());
    List<DirectDebitPaymentRecord> notFinishedPaymentRecords = directDebitPaymentModel.findByLinkAccountIdsAndStatusAndTypes(
        linkAccountIds, DirectDebitPaymentStatus.UNFINISHED_STATUS_LIST, DirectDebitPaymentType.TYPES_REQUIRING_UNIQUENESS_CHECK);
    return CollectionUtils.isNotEmpty(notFinishedPaymentRecords);
  }

  //检查当前账号是否启用
  private void checkCurrentAccountStatus(Long linkAccountId) {
    DirectDebitLinkAccountVO accountVO = directDebitAccountService.findAccountVOByIdOrThrow(linkAccountId);
    if (accountVO.status != DirectDebitAccountStatus.ENABLED) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("当前账号未启用，请启用后重试"));
    }
  }

  public List<DirectDebitPaymentVO> findDirectDebitPaymentByRangeTimeAndStatuses(List<DirectDebitPaymentStatus> directDebitPaymentStatuses, Long startTime, Long endTime) {
    return directDebitPaymentModel.findTransNoByRangeAndStatuses(directDebitPaymentStatuses, startTime, endTime)
        .stream().map(DirectDebitPaymentVO::from).collect(Collectors.toList());
  }

  public void handleInitDirectDebitPayment(DirectDebitProvider provider, List<String> transNos) {

    List<DirectDebitPaymentResponse> responses = overseasPaymentDirectDebitService.getPaymentByTransNo(transNos, provider.name());
    Map<String, DirectDebitPaymentResponse> transNoToResponse = CollectionUtils.isEmpty(responses) ? new HashMap<>() : responses.stream().collect(Collectors.toMap(r -> r.getTransNo(), r -> r));

    transNos.forEach(transNo -> {
      DirectDebitPaymentResponse response = transNoToResponse.get(transNo);
      if (Objects.isNull(response)) {
        directDebitCreditEntrySupport.syncFinalStatusPayment(transNo, DirectDebitPaymentStatus.FAILED, null, null, DirectDebitPaymentAdditionalInfo.from(null, DirectDebitFailedReason.INTERNAL_SERVER_ERROR, null));
      }
    });
  }

  public BigDecimal sumTotalTransferAmountByUserId(Long userId, SDKType sdkType) {
    long now = Clock.now();
    long minMillisOfMonth = Clock.getMinMillisOfMonth(now, sdkType.getTimeZone());
    long maxMillisOfMonth = Clock.getMaxMillisOfMonth(now, sdkType.getTimeZone());
    return directDebitPaymentModel.findByUserIdAndStatusAndTimeCreated(userId, DirectDebitPaymentStatus.EFFECTIVE_STATUS_LIST, minMillisOfMonth, maxMillisOfMonth)
        .stream()
        .map(DirectDebitPaymentRecord::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
