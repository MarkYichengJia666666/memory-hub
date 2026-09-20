package com.yqg.core.service.payment;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.*;
import com.yqg.core.model.sql.bankaccount.LoanBankAccountModel;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.payment.PaymentModel;
import com.yqg.core.model.sql.payment.PaymentUnusualTransactionModel;
import com.yqg.core.model.sql.payment.enums.PaymentStatus;
import com.yqg.core.model.sql.payment.enums.PaymentTransType;
import com.yqg.core.model.sql.thirdparty.ThirdPartyDirectDebitRepaymentModel;
import com.yqg.core.model.sql.thirdparty.ThirdPartyDynamicAccountRepaymentModel;
import com.yqg.core.model.sql.thirdparty.ThirdPartyRepaymentModel;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.service.financing.vo.FinancingWithdrawInfoVO;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.payment.monitor.PaymentMonitor;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.pm.pmenum.PaymentErrorCode;
import com.yqg.core.service.payment.pm.pmenum.VirtualAccountChannel;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.service.payment.vo.*;
import com.yqg.core.util.ioc.SpringUtils;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.overseas.client.spring.api.payment.IOverseasPaymentPaymentService;
import com.yqg.overseas.spring.response.payment.ThirdPartyPaymentResponse;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 支付总入口
 * <p>
 * Created by jpdu on 2017/8/19.
 */
@Slf4j
@Service
public class PaymentService {
  @Autowired
  private ThreadTransactionalModel transactionalModel;
  @Autowired
  private PaymentModel paymentModel;
  @Autowired
  private PaymentMonitor paymentMonitor;
  @Autowired
  private PaymentUnusualTransactionModel unusualTransModel;
  @Autowired
  private LoanBankAccountModel loanBankAccountModel;
  @Autowired
  private ThirdPartyRepaymentModel thirdPartyRepaymentModel;
  @Autowired
  private ThirdPartyDynamicAccountRepaymentModel thirdPartyDynamicAccountRepaymentModel;
  @Autowired
  private ThirdPartyDirectDebitRepaymentModel thirdPartyDirectDebitRepaymentModel;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private IOverseasPaymentPaymentService overSeasPaymentPaymentService;

  public ICredential getCredentialInfo(PaymentCredential paymentCredential) {
    IPaymentMethod method = getMethodInstance(paymentCredential.getMethod());
    return method.getCredential(paymentCredential.getId());
  }

  //TODO(ZORAN, T44644)修改银行卡读取
  @Deprecated
  public Map<PaymentMethod, List<ICredential>> getPayoutCredentialInfo(long userId, SDKType sdkType) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    Map<PaymentMethod, List<ICredential>> map = Maps.newHashMap();
    for (PaymentMethod method : PaymentAccount.getPayoutMethodSet(sdkType)) {
      IPaymentMethod methodInstance = getMethodInstance(method);
      List<ICredential> info = methodInstance.getCredentialInfo(userId, businessName);
      if (CollectionUtils.isEmpty(info)) {
        continue;
      }
      map.put(method, info);
    }
    return map;
  }

  //TODO(ZORAN, T44644)修改银行卡读取
  public Map<PaymentMethod, List<ICredential>> getAllPayoutCredentials(long userId, SDKType sdkType) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    Map<PaymentMethod, List<ICredential>> map = Maps.newHashMap();
    for (PaymentMethod method : PaymentAccount.getPayoutMethodSet(sdkType)) {
      IPaymentMethod methodInstance = getMethodInstance(method);
      List<ICredential> info = methodInstance.getAllCredentials(userId, businessName);
      if (CollectionUtils.isEmpty(info)) {
        continue;
      }
      map.put(method, info);
    }
    return map;
  }

  /**
   * 如果payment中有对应的mht记录，则返回数据中的打款状态。
   * 如果没有找到该条记录，说明payment service没有接收到这笔打款请求，直接返回 "打款失败"
   */
  public PaymentResult getPaymentResult(String transId) {
    PaymentRecord record = paymentModel.findByTransIdOrNull(transId);
    if (record == null) {
      return PaymentResult.from(transId, PaymentStatus.FAILED, null, PaymentErrorCode.GENERAL_ERROR);
    }
    PaymentProvider paymentProvider = record.getServiceProvider() == null ? null : PaymentProvider.from(record.getServiceProvider());
    return PaymentResult.from(transId, PaymentStatus.fromCode(record.getStatus()), paymentProvider, PaymentErrorCode.fromCode(record.getErrorCode()));
  }

  public PaymentResult payWithSuccess(
      PaymentAccount paymentAccount,
      long userId,
      PaymentBusinessName businessName,
      PaymentCredential paymentCredential,
      PaymentProvider paymentProvider,
      CurrencyAmount amount,
      Long transactionTime, String transId,
      String channel) {
    return pay(paymentAccount, userId, businessName, transId, paymentCredential, paymentProvider, amount, PaymentStatus.SUCCEED, transactionTime, PaymentErrorCode.NO_ERROR, channel);
  }

  public PaymentResult pay(
      PaymentAccount paymentAccount,
      long userId,
      PaymentBusinessName businessName,
      String transId,
      PaymentCredential paymentCredential,
      PaymentProvider provider,
      CurrencyAmount amount,
      PaymentStatus paymentStatus,
      Long transactionTime,
      PaymentErrorCode errorCode,
      String channel
  ) {
    PaymentRecord record = paymentModel.insert(
        PaymentTransType.REPAY,
        paymentAccount,
        userId,
        businessName,
        transId,
        paymentCredential,
        amount,
        paymentStatus,
        transactionTime,
        provider,
        errorCode);

    paymentMonitor.logPayment(PaymentVO.from(record), channel);
    return PaymentResult.from(record.getId(), record.getThirdPartyPayOrderId(), paymentStatus, errorCode);
  }

  public PaymentVO getPaymentVO(Long paymentId) {
    return PaymentVO.from(paymentModel.getByIdOrThrow(paymentId));
  }

  public PaymentVO getPaymentVO(String transId) {
    return PaymentVO.from(paymentModel.findByTransIdOrNull(transId));
  }

  public PaymentVO getPaymentVOOrNull(String transId) {
    PaymentRecord record = paymentModel.findByTransIdOrNull(transId);
    return record == null ? null : PaymentVO.from(record);
  }

  public Map<String, PaymentVO> getPaymentVOMaps(Collection<String> transIds) {
    if (CollectionUtils.isEmpty(transIds)) {
      return Collections.emptyMap();
    }
    return paymentModel.findByThirdPartyOrderIds(transIds)
        .stream()
        .collect(Collectors.toMap(PaymentRecord::getThirdPartyPayOrderId, PaymentVO::from, (r1, r2) -> r1));
  }

  public void checkCanRemovePaymentCredential(Long userId, SDKType sdkType, PaymentCredential paymentCredential) {
    checkCanRemovePaymentCredential(userId, null, sdkType, paymentCredential);
  }

  public void checkCanRemovePaymentCredential(Long userId, Long build, SDKType sdkType, PaymentCredential paymentCredential) {
    int credentialSize;
    Map<PaymentMethod, List<ICredential>> credentialMap = getPayoutCredentialInfo(userId, sdkType);
    // 如果是用户最后一个可用的账号 则不可以删除
    credentialSize = credentialMap.values().stream().mapToInt(List::size).sum();

    if (credentialSize == 1) {
      throw EcException.warn(EcExceptionType.LOAN_BANK_ACCOUNT_DELETE_LAST_CARD, TT.gen("只有一张卡时不能删除"));
    }

    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    List<PaymentRecord> records =
        paymentModel.find(userId, businessName, paymentCredential, Lists.newArrayList(PaymentStatus.PENDING));
    if (CollectionUtils.isNotEmpty(records)) {
      throw EcException.warn(EcExceptionType.LOAN_BANK_ACCOUNT_DELETE_USING_CARD, TT.gen("当前卡存在未完成的款项"));
    }
  }

  public int getRepayCount(Long userId, SDKType sdkType, PaymentCredential paymentCredential) {
    return paymentModel.find(userId, PaymentBusinessNameMapper.getBusinessName(sdkType), paymentCredential,
        Lists.newArrayList(PaymentStatus.SUCCEED)).size();
  }

  public void removePaymentCredential(Long userId, PaymentCredential paymentCredential) {
    IPaymentMethod paymentMethod = getMethodInstance(paymentCredential.getMethod());
    ICredential credential = paymentMethod.getCredential(userId, paymentCredential.getId());
    paymentMethod.removePaymentCredential(credential.getId());
  }

  public List<PaymentVO> getPaymentVOs(Long userId, SDKType sdkType, PaymentTransType transType, PaymentStatus status) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    return paymentModel.find(userId, businessName, transType, status).stream().map(PaymentVO::from).collect(Collectors.toList());
  }

  public List<PaymentVO> getRepayPaymentVOsWhichNotRepayToOrder(Long userId, SDKType sdkType) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);

    List<ThirdpartyRepaymentRecord> thirdPartyRepayFailedRecords = thirdPartyRepaymentModel.fetchByUserIdAndBusinessAndStatus(userId, businessName, ProcessStatus.FAILED);
    List<ThirdpartyDynamicAccountRepaymentRecord> thirdpartyDynamicAccountRepaymentRecords = thirdPartyDynamicAccountRepaymentModel.fetchByUserIdAndBusinessNameAndStatus(userId, businessName, ProcessStatus.FAILED);
    List<ThirdpartyDirectDebitRepaymentRecord> thirdpartyDirectDebitRepaymentRecords = thirdPartyDirectDebitRepaymentModel.fetchByUserIdAndBusinessNameAndStatus(userId, businessName, ProcessStatus.FAILED);

    List<String> paymentTransIds = thirdPartyRepayFailedRecords.stream().map(ThirdpartyRepaymentRecord::getPaymentTransId).collect(Collectors.toList());
    paymentTransIds.addAll(thirdpartyDynamicAccountRepaymentRecords.stream().map(ThirdpartyDynamicAccountRepaymentRecord::getPaymentTransId).collect(Collectors.toList()));
    paymentTransIds.addAll(thirdpartyDirectDebitRepaymentRecords.stream().map(ThirdpartyDirectDebitRepaymentRecord::getTransNo).collect(Collectors.toList()));
    return paymentModel.findByThirdPartyOrderIds(paymentTransIds).stream().map(PaymentVO::from).collect(Collectors.toList());
  }

  public Map<String, ThirdPartyPaymentInfo> get3rdPartyPaymentInfo(SDKType sdkType, Collection<PaymentVO> paymentVOS) {
    Map<String, ThirdPartyPaymentInfo> map = new HashMap<>();

    List<String> transIds = paymentVOS
        .stream()
        .map(vo -> vo.thirdPartyPayOrderId)
        .collect(Collectors.toList());
    String transStr = JsonUtils.toString(transIds);
    return overSeasPaymentPaymentService.getThirdPartyRepaymentInfo(transStr)
        .stream()
        .collect(Collectors.toMap(ThirdPartyPaymentResponse::getTransId,
            vo -> ThirdPartyPaymentInfo.from(vo.getThirdPartyId(), vo.getTimeCreated(), vo.getTimeUpdated())));
  }

  public void markUnusualTrans(List<PaymentUnusualTransVO> voList) {
    transactionalModel.transaction(configuration -> {
      for (PaymentUnusualTransVO vo : voList) {
        unusualTransModel.insertOrIgnore(vo.paymentProvider, vo.type, vo.providerId, vo.remark);
      }
    });
  }

  public void deleteUnusualTrans(Long id) {
    PaymentUnusualTransactionRecord record = unusualTransModel.selectById(id);
    if (record == null) {
      return;
    }
    unusualTransModel.delete(record);
  }

  public List<PaymentUnusualTransVO> getAllUnusualTrans() {
    return unusualTransModel.selectAll()
        .stream()
        .map(PaymentUnusualTransVO::from)
        .collect(Collectors.toList());
  }

  public List<PaymentUnusualTransVO> getUnusualTrans(PaymentProvider provider, String providerId) {
    return unusualTransModel.findByConditions(provider, providerId)
        .stream()
        .map(PaymentUnusualTransVO::from)
        .collect(Collectors.toList());
  }

  public Map<PaymentMethod, Map<Long, BaseCredentialVO>> fetchCredentialInfos(List<PaymentCredential> credentials) {
    return credentials.stream()
        .collect(Collectors.groupingBy(
            PaymentCredential::getMethod,
            Collectors.mapping(PaymentCredential::getId, Collectors.toSet())
        )).entrySet().stream().map(
            entry -> new AbstractMap.SimpleEntry<>(
                entry.getKey(),
                getMethodInstance(entry.getKey()).getCredentialInfos(entry.getValue()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public IPaymentMethod getMethodInstance(PaymentMethod method) {
    return SpringUtils.getInstance(PaymentMethod.getMethodClass(method));
  }

  //仅用于va，包括静态va和动态va
  public List<RepaymentAccountVO> moveSpecifyChannelToHead(List<RepaymentAccountVO> repaymentAccountVOS, Long userId, SDKType sdkType) {
    PaymentVO paymentVO = getLatestPayment(userId, PaymentBusinessNameMapper.getBusinessName(sdkType), PaymentTransType.REPAY, ImmutableSet.of(PaymentMethod.VIRTUAL_ACCOUNT, PaymentMethod.DYNAMIC_ACCOUNT));
    if (paymentVO == null) {
      if (SDKType.FIN_SDK_TYPE_LIST.contains(sdkType)) {
        // 理财业务方要求：如果没有充值记录，用户如果绑了BCA则把BCA放前面 如果不是把BRI放前面
        return moveUserBoundCardToHead(repaymentAccountVOS, userId, sdkType);
      } else {
        return repaymentAccountVOS;
      }
    }
    return moveRecentlyUsedChannelToHead(repaymentAccountVOS, userId, sdkType, paymentVO);
  }

  private List<RepaymentAccountVO> moveUserBoundCardToHead(List<RepaymentAccountVO> repaymentAccountVOS,
                                                           Long userId,
                                                           SDKType sdkType) {
    List<LoanBankAccountRecord> bcaCardList = loanBankAccountModel.findAvailableByUserIdAndBankCode(userId, BankType.BCA, sdkType);
    if (CollectionUtils.isNotEmpty(bcaCardList)) {
      return moveToHead(VirtualAccountChannel.BCA.name(), repaymentAccountVOS);
    } else {
      return moveToHead(VirtualAccountChannel.BRI.name(), repaymentAccountVOS);
    }
  }

  public String getLatestRepaymentChannel(Long userId) {
    PaymentVO paymentVO = getLatestPayment(userId, PaymentBusinessName.IDN_YQD, PaymentTransType.REPAY, ImmutableSet.of(PaymentMethod.VIRTUAL_ACCOUNT, PaymentMethod.DYNAMIC_ACCOUNT));
    if (Objects.isNull(paymentVO)) {
      return StringUtils.EMPTY;
    }
    return getChannelNameFromPaymentVO(paymentVO);
  }

  public String getChannelNameFromPaymentVO(PaymentVO paymentVO) {
    IPaymentMethod paymentMethod = getMethodInstance(paymentVO.paymentCredential.getMethod());
    return paymentMethod.getChannelName(paymentVO.paymentCredential.getId());
  }

  private List<RepaymentAccountVO> moveRecentlyUsedChannelToHead(List<RepaymentAccountVO> repaymentAccountVOS,
                                                                 Long userId,
                                                                 SDKType sdkType,
                                                                 PaymentVO paymentVO) {
    String channel = getChannelNameFromPaymentVO(paymentVO);
    if (StringUtils.isBlank(channel)) {
      log.error("channel is empty, payment id: {}, user id: {}, sdkType: {}", paymentVO.id, userId, sdkType);
      return repaymentAccountVOS;
    }
    return moveToHead(channel, repaymentAccountVOS);
  }

  public List<RepaymentAccountVO> moveToHead(String firstChannel, List<RepaymentAccountVO> repaymentAccountVOS) {
    LinkedList<RepaymentAccountVO> resultList = new LinkedList<>();
    for (RepaymentAccountVO accountVO : repaymentAccountVOS) {
      if (firstChannel.equals(accountVO.getChannel())) {
        resultList.addFirst(accountVO);
      } else {
        resultList.add(accountVO);
      }
    }
    return resultList;
  }

  public PaymentVO getLatestPayment(Long userId, PaymentBusinessName businessName, PaymentTransType paymentTransType, Collection<PaymentMethod> paymentMethodList) {
    PaymentRecord paymentRecord = paymentModel.findLatestPayment(userId, businessName, paymentTransType, paymentMethodList);
    return paymentRecord == null ? null : PaymentVO.from(paymentRecord);
  }

  public List<PaymentVO> listByQueryCondition(PaymentQueryCondition queryCondition) {
    return paymentModel.findPaymentListByQueryCondition(queryCondition)
        .stream()
        .map(PaymentVO::from)
        .collect(Collectors.toList());
  }

  public Map<Long, PaymentProvider> listPaymentProviderByWithdrawInfos(List<FinancingWithdrawInfoVO> financingWithdrawInfoVOs) {
    return financingWithdrawInfoVOs.stream()
        .collect(Collectors.toMap(vo -> vo.financingTransactionRecordId,
            vo -> {
              PaymentVO paymentVO = getPaymentVO(vo.mhtOrderNo);
              return paymentVO.getPaymentProvider();
            }));
  }

  public Map<String, String> getRepayTransIdRepayChannelMap(List<String> paymentTransIds) {
    List<PaymentRecord> paymentRecords = paymentModel.findByThirdPartyOrderIds(paymentTransIds);
    Map<String, String> transIdToRepayChannelMap = new HashMap<>();
    if (CollectionUtils.isEmpty(paymentRecords)) {
      return transIdToRepayChannelMap;
    }
    for (PaymentRecord paymentRecord : paymentRecords) {
      Long paymentCredentialId = paymentRecord.getPaymentCredentialId();
      PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentRecord.getPaymentMethod());
      PaymentCredential credential = new PaymentCredential(paymentMethod, paymentCredentialId);
      ICredential credentialInfo = getCredentialInfo(credential);
      transIdToRepayChannelMap.put(paymentRecord.getThirdPartyPayOrderId(), credentialInfo.getChannelName());
    }
    return transIdToRepayChannelMap;
  }

  public PaymentVO syncPayment(String transNo,
                               PaymentStatus paymentStatus,
                               PaymentProvider paymentProvider,
                               PaymentErrorCode errorCode,
                               Long transactionTime) {
    return threadTransactionalModel.transactionResult(configuration -> {
      PaymentRecord paymentRecord = paymentModel.fetchByTransNoForUpdate(transNo);
      if (paymentRecord == null) {
        throw EcException.error("can not find payment record of:{}", transNo);
      }
      PaymentStatus currentStatus = PaymentStatus.fromCode(paymentRecord.getStatus());
      if (currentStatus.isFinalState() || currentStatus == paymentStatus) {
        log.warn("payment status:{} is finished", paymentStatus.name());
        return PaymentVO.from(paymentRecord);
      }
      PaymentRecord record = paymentModel.updatePayment(paymentRecord,
          paymentStatus,
          paymentProvider,
          errorCode,
          transactionTime);
      return PaymentVO.from(record);
    });
  }

  public PaymentVO insertPendingPayment(PaymentTransType transType,
                                        PaymentAccount paymentAccount,
                                        Long userId,
                                        SDKType sdkType,
                                        String transNo,
                                        PaymentCredential paymentCredential,
                                        CurrencyAmount amount) {
    PaymentRecord paymentRecord = paymentModel.insert(transType,
        paymentAccount,
        userId,
        PaymentBusinessNameMapper.getBusinessName(sdkType),
        transNo,
        paymentCredential,
        amount,
        PaymentStatus.PENDING,
        null,
        null,
        PaymentErrorCode.NO_ERROR);
    return PaymentVO.from(paymentRecord);
  }

  public boolean checkPaymentCredentialAvailable(Long userId, PaymentCredential paymentCredential) {
    try {
      IPaymentMethod paymentMethod = getMethodInstance(paymentCredential.getMethod());
      ICredential credential = paymentMethod.getCredential(userId, paymentCredential.getId());
      Boolean result = credential.isCredentialAvailable();
      if (Objects.isNull(result)) {
        return true;
      }
      return result;
    } catch (Exception e) {
      log.error("Incorrect payment credential，userId;{}, credential: {}", userId, paymentCredential.getId(), e);
      return false;
    }
  }
}
