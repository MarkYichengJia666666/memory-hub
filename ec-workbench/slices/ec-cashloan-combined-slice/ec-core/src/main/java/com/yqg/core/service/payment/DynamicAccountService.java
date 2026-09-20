package com.yqg.core.service.payment;

import com.yqg.core.common.enums.BusinessName;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.DynamicAccountRecord;
import com.yqg.core.model.generated.tables.records.ReceiptCreationCredentialRecord;
import com.yqg.core.model.sql.payment.DynamicAccountModel;
import com.yqg.core.model.sql.payment.ReceiptCreationCredentialModel;
import com.yqg.core.model.sql.thirdparty.ThirdPartyDynamicAccountRepaymentModel;
import com.yqg.core.service.payment.locks.ReceiptCollectionLocker;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.vo.DynamicAccountVO;
import com.yqg.core.service.payment.vo.PaymentResult;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.core.service.receipt.enums.ReceiptAccountCreationStatus;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.overseas.client.spring.api.receipt.IOverseasPaymentReceiptAccountService;
import com.yqg.overseas.spring.response.receipt.ReceiptResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DynamicAccountService {
  @Autowired
  private ThirdPartyDynamicAccountRepaymentModel thirdPartyDynamicAccountRepaymentModel;
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private ReceiptCreationCredentialModel receiptCreationCredentialModel;
  @Autowired
  private DynamicAccountModel dynamicAccountModel;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private ReceiptCollectionLocker locker;
  @Autowired
  private IOverseasPaymentReceiptAccountService opReceiptAccountService;

  public boolean syncPaymentLog(String businessId,
                                String receiptId,
                                CurrencyAmount paidAmount,
                                Long transactionTime,
                                String transNo,
                                Long thridPartyPrimaryKey) {
    return locker.nonBlockingLockWithFallback(transNo, () ->
        threadTransactionalModel.transactionResult(configuration -> {
          ReceiptCreationCredentialRecord receiptAccountLog = receiptCreationCredentialModel.findByBusinessIdForUpdate(businessId);
          PaymentVO paymentVO = paymentService.getPaymentVOOrNull(transNo);
          if (paymentVO != null) {
            log.info("transNo:{},businessId:{},receiptId:{} had been sync,return true fastly", transNo, businessId, receiptId);
            return true;
          }
          if (receiptAccountLog.getReceiptId() == null) {
            if (DynamicAccountChannel.valueOf(receiptAccountLog.getPaymentChannel()) == DynamicAccountChannel.OVO) {
              receiptAccountLog = updateDynamicAccount(receiptAccountLog, receiptId, transNo, businessId);
            } else {
              log.error("can not find DA of businessId:{},receiptId:{},transNo:{}", businessId, receiptId, transNo);
              return false;
            }
          }
          if (!StringUtils.equals(receiptId, receiptAccountLog.getReceiptId())) {
            log.error("can not find DA of businessId:{},receiptId:{},transNo:{}", businessId, receiptId, transNo);
            return false;
          }
          PaymentMethod paymentMethod = PaymentMethod.valueOf(receiptAccountLog.getPaymentMethod());
          if (PaymentMethod.DYNAMIC_ACCOUNT != paymentMethod) {
            log.error("can not find DA of businessId:{},receiptId:{},transNo:{}", businessId, receiptId, transNo);
            return false;
          }

          final Long relatedId = receiptAccountLog.getRelatedId();
          if (relatedId == null) {
            log.error("can not find DA of businessId:{},receiptId:{}", businessId, receiptId);
            return false;
          }

          DynamicAccountRecord dynamicAccountRecord = dynamicAccountModel.getByIdOrThrow(receiptAccountLog.getRelatedId());
          DynamicAccountVO dynamicAccountVO = DynamicAccountVO.from(dynamicAccountRecord);

          final PaymentBusinessName businessName = PaymentBusinessName.valueOf(receiptAccountLog.getBusinessName());
          PaymentResult paymentResult = paymentService.payWithSuccess(dynamicAccountVO.paymentAccount,
              dynamicAccountVO.getUserId(),
              businessName,
              new PaymentCredential(PaymentMethod.DYNAMIC_ACCOUNT, relatedId),
              dynamicAccountVO.provider,
              paidAmount,
              transactionTime,
              transNo,
              dynamicAccountVO.getChannelName()
          );
          thirdPartyDynamicAccountRepaymentModel.insert(
              dynamicAccountVO.getUserId(),
              businessName,
              dynamicAccountVO.provider,
              thridPartyPrimaryKey,
              paymentResult.transId);
          return paymentResult.status.isSucceed();
        }), () -> {
      log.error("can not get lock to sync DA payment of businessId:{},receiptId:{},transNo:{}", businessId, receiptId, transNo);
      return false;
    });
  }

  private ReceiptCreationCredentialRecord updateDynamicAccount(ReceiptCreationCredentialRecord receiptAccountLog, String receiptId, String transNo, String businessId) {
    ReceiptResponse response = opReceiptAccountService.query(businessId, BusinessName.EC.name());
    DynamicAccountRecord dynamicAccountRecord = dynamicAccountModel.findByRequestId(transNo);
    dynamicAccountModel.updateAccountToEffective(dynamicAccountRecord, response.getReceiptAccount(), response.getReceiptContent());
    log.info("update dynamic account on callback to effective, transNo:{}, receiptId:{}, businessId:{}", transNo, receiptId, businessId);
    return receiptCreationCredentialModel.update(receiptAccountLog, receiptId, dynamicAccountRecord.getId(), ReceiptAccountCreationStatus.SUCCESS);
  }

}
