package com.yqg.core.service.payment;

import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.DynamicAccountRecord;
import com.yqg.core.model.generated.tables.records.ReceiptCreationCredentialRecord;
import com.yqg.core.model.generated.tables.records.StaticVirtualAccountRecord;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.payment.DynamicAccountModel;
import com.yqg.core.model.sql.payment.ReceiptCreationCredentialModel;
import com.yqg.core.model.sql.payment.StaticVirtualAccountModel;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.payment.locks.ReceiptCollectionLocker;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.pm.pmenum.VirtualAccountChannel;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.service.payment.response.ReceiptCreationCredentialResponse;
import com.yqg.core.service.payment.vo.AccountCheckVO;
import com.yqg.core.service.receipt.enums.ReceiptAccountCreationStatus;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class PaymentReceiptAccountService {
  @Autowired
  private ReceiptCollectionLocker locker;
  @Autowired
  private DynamicAccountModel dynamicAccountModel;
  @Autowired
  private LoanAccountModel loanAccountModel;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private StaticVirtualAccountModel staticVirtualAccountModel;
  @Autowired
  private ReceiptCreationCredentialModel receiptCreationCredentialModel;

  public boolean syncReceiptCreationCredential(
      String businessId,
      String receiptId,
      String receiptAccount,
      String receiptContent) {

    return locker.nonBlockingLockWithFallback(businessId, () ->
            threadTransactionalModel.transactionResult(configuration -> {
              ReceiptCreationCredentialRecord record = receiptCreationCredentialModel.findByBusinessIdForUpdate(businessId);
              if (StringUtils.isNotBlank(record.getReceiptId()) && record.getReceiptId().equals(receiptId)) {
                log.info("synchronize receipt creation credentials already associated  of businessId:{},receiptId:{},receiptAccount:{}", businessId, receiptId, receiptAccount);
                return true;
              }
              AccountCheckVO accountCheckVO = proceedReceiptAccount(
                  record,
                  receiptAccount,
                  receiptContent
              );
              if (Objects.nonNull(accountCheckVO)) {
                receiptCreationCredentialModel.update(
                    record,
                    receiptId,
                    accountCheckVO.relatedId,
                    ReceiptAccountCreationStatus.SUCCESS
                );
                return true;
              } else {
                return false;
              }
            }),
        () -> {
          log.error("can not get lock to sync receipt creation credential of businessId:{},receiptId:{},receiptAccount:{}", businessId, receiptId, receiptAccount);
          return false;
        }
    );
  }

  protected AccountCheckVO proceedReceiptAccount(
      ReceiptCreationCredentialRecord record,
      String receiptAccount,
      String receiptContent
  ) {

    AccountCheckVO accountCheckVO = checkAccountBasedOnPaymentMethode(record, receiptAccount, receiptContent);
    ReceiptAccountCreationStatus currentStatus = ReceiptAccountCreationStatus.valueOf(record.getStatus());
    if (Objects.isNull(accountCheckVO) && currentStatus == ReceiptAccountCreationStatus.INIT) {
      accountCheckVO = insertAccountBasedOnPaymentMethode(
          record,
          receiptAccount
      );
    }
    return accountCheckVO;
  }

  protected AccountCheckVO insertAccountBasedOnPaymentMethode(
      ReceiptCreationCredentialRecord record,
      String receiptAccount
  ) {

    PaymentBusinessName businessName = PaymentBusinessName.valueOf(record.getBusinessName());
    PaymentAccount paymentAccount = PaymentAccount.getDefaultAccount(businessName.sdkType);
    PaymentProvider paymentProvider = PaymentProvider.valueOf(record.getPaymentProvider());
    PaymentMethod paymentMethod = PaymentMethod.valueOf(record.getPaymentMethod());

    switch (paymentMethod) {
      case VIRTUAL_ACCOUNT:
        VirtualAccountChannel virtualAccountChannel = VirtualAccountChannel.valueOf(record.getPaymentChannel());
        StaticVirtualAccountRecord virtualAccountRecord = staticVirtualAccountModel.initOrIgnore(
            paymentAccount,
            record.getUserId(),
            businessName,
            virtualAccountChannel,
            receiptAccount,
            paymentProvider);
        return new AccountCheckVO()
            .setExist(true)
            .setRelatedId(virtualAccountRecord.getId());
      default:
        return null;
    }
  }

  protected AccountCheckVO checkAccountBasedOnPaymentMethode(
      ReceiptCreationCredentialRecord record,
      String receiptAccount,
      String receiptContent
  ) {
    PaymentMethod paymentMethod = PaymentMethod.valueOf(record.getPaymentMethod());
    switch (paymentMethod) {
      case VIRTUAL_ACCOUNT:
        StaticVirtualAccountRecord virtualAccountRecord = staticVirtualAccountModel.getOrNull(receiptAccount);
        if (Objects.isNull(virtualAccountRecord)) {
          log.warn("can not find Static Virtual Account of receiptAccount:{}", receiptAccount);
          return null;
        }
        return new AccountCheckVO()
            .setExist(true)
            .setRelatedId(virtualAccountRecord.getId());
      case DYNAMIC_ACCOUNT:
        DynamicAccountRecord dynamicAccountRecord = dynamicAccountModel.findByRequestId(record.getTransNo());
        if (Objects.isNull(dynamicAccountRecord)) {
          log.warn("can not find Dynamic Virtual Account of receiptAccount:{}", receiptAccount);
          throw EcException.error("can not find Dynamic Virtual Account of receiptAccount:{}, creationCredential:{},", record.getTransNo(), JsonUtils.toString(record));
        }
        dynamicAccountModel.updateAccountToEffective(dynamicAccountRecord, receiptAccount, receiptContent);
        return new AccountCheckVO()
            .setExist(true)
            .setRelatedId(dynamicAccountRecord.getId());
      default:
        return null;
    }
  }

  public ReceiptCreationCredentialResponse getPaymentReceiptAccount(String transNo) {
    ReceiptCreationCredentialRecord credentialRecord = receiptCreationCredentialModel.findByTransNo(transNo);
    if (Objects.isNull(credentialRecord)) {
      return new ReceiptCreationCredentialResponse();
    }
    PaymentMethod paymentMethod = PaymentMethod.valueOf(credentialRecord.getPaymentMethod());
    String accountNumber = null;
    if (paymentMethod == PaymentMethod.DYNAMIC_ACCOUNT) {
      DynamicAccountRecord dynamicAccountRecord = dynamicAccountModel.findByRequestId(transNo);
      if (Objects.nonNull(dynamicAccountRecord.getExtraData())) {
        Map<String, String> accountMap = JsonUtils.fromOrException(dynamicAccountRecord.getExtraData(), HashMap.class);
        accountNumber = accountMap.get("account");
      }
    }
    return ReceiptCreationCredentialResponse.from(credentialRecord, accountNumber);
  }
}
