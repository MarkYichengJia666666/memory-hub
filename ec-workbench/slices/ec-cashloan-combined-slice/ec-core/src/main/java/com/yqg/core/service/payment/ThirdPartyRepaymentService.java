package com.yqg.core.service.payment;

import com.yqg.core.configure.EcExecutorConfig;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.ThirdpartyDirectDebitRepaymentRecord;
import com.yqg.core.model.generated.tables.records.ThirdpartyDynamicAccountRepaymentRecord;
import com.yqg.core.model.generated.tables.records.ThirdpartyRepaymentRecord;
import com.yqg.core.model.sql.thirdparty.ThirdPartyDirectDebitRepaymentModel;
import com.yqg.core.model.sql.thirdparty.ThirdPartyDynamicAccountRepaymentModel;
import com.yqg.core.model.sql.thirdparty.ThirdPartyRepaymentModel;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.JbpConfig;
import com.yqg.core.service.cashloan.repay.UnionRepaymentService;
import com.yqg.core.service.financing.transaction.FinancingTransactionService;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.loan.repayment.analysis.RepaymentAnalysisService;
import com.yqg.core.service.payment.vo.PaymentProcessResult;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.core.service.sensors.SensorsService;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author chaoye
 * @date 2024/6/5
 */
@Slf4j
@Service
public class ThirdPartyRepaymentService {
  @Autowired
  private ThirdPartyRepaymentModel thirdPartyRepaymentModel;
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private FinancingTransactionService financingTransactionService;
  @Autowired
  private ThirdPartyDynamicAccountRepaymentModel thirdPartyDynamicAccountRepaymentModel;
  @Resource(name = EcExecutorConfig.HANDLE_REPAYMENT_EXECUTOR)
  private Executor handleRepaymentExecutor;
  @Autowired
  private StaticVirtualAccountService staticVirtualAccountService;
  @Autowired
  private DynamicAccountService dynamicAccountService;
  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private ThirdPartyDirectDebitRepaymentModel thirdPartyDirectDebitRepaymentModel;
  @Autowired
  private SensorsService sensorsService;
  @Autowired
  private UnionRepaymentService unionRepaymentService;
  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  private RepaymentAnalysisService repaymentAnalysisService;
  @Autowired
  private JbpConfig jbpConfig;

  public boolean handleStaticAccountRepaymentCallback(String businessId,
                                                      String receiptId,
                                                      CurrencyAmount paidAmount,
                                                      Long transactionTime,
                                                      String transNo,
                                                      Long thridPartyPrimaryKey) {
    //先记录还款信息
    boolean result = staticVirtualAccountService.syncPaymentLog(businessId, receiptId, paidAmount, transactionTime, transNo, thridPartyPrimaryKey);
    if (result && cashLoanConfig.quickHandleRepaymentCallback()) {
      ThirdpartyRepaymentRecord record = thirdPartyRepaymentModel.fetchByTransId(transNo);
      if (Objects.nonNull(record)) {
        //异步发起抵扣还款
        asyncHandleStaticAccountRepayment(record);
      }
    }
    return result;
  }

  public boolean handleDynamicAccountRepaymentCallback(String businessId,
                                                       String receiptId,
                                                       CurrencyAmount paidAmount,
                                                       Long transactionTime,
                                                       String transNo,
                                                       Long thridPartyPrimaryKey) {
    //先记录还款信息
    boolean result = dynamicAccountService.syncPaymentLog(businessId, receiptId, paidAmount, transactionTime, transNo, thridPartyPrimaryKey);
    if (result && cashLoanConfig.quickHandleRepaymentCallback()) {
      ThirdpartyDynamicAccountRepaymentRecord record = thirdPartyDynamicAccountRepaymentModel.fetchByTransIdOrThrow(transNo);
      //异步发起抵扣还款
      asyncHandleDynamicAccountRepayment(record);
    }
    return result;
  }

  public void asyncHandleStaticAccountRepayment(ThirdpartyRepaymentRecord record) {
    handleRepaymentExecutor.execute(() -> {
      handleStaticAccountRepayment(record);
    });
  }

  public void asyncHandleDynamicAccountRepayment(ThirdpartyDynamicAccountRepaymentRecord record) {
    handleRepaymentExecutor.execute(() -> {
      handleDynamicAccountRepayment(record);
    });
  }

  public void asyncHandleDirectDebitRepayment(ThirdpartyDirectDebitRepaymentRecord record) {
    handleRepaymentExecutor.execute(() -> {
      handleDirectDebitRepayment(record);
    });
  }

  public void handleStaticAccountRepayment(ThirdpartyRepaymentRecord record) {
    try {
      ProcessStatus status = ProcessStatus.fromCode(record.getStatus());
      if (status != ProcessStatus.UNPROCESSED) {
        return;
      }
      PaymentVO paymentVO = paymentService.getPaymentVO(record.getPaymentTransId());
      PaymentProcessResult result = getPaymentProcessResult(paymentVO);
      sensorsService.uploadRepaymentSuccessEvent(paymentVO, null);
      handleProcessResult(record, paymentVO.userId, result);
    } catch (Exception e) {
      log.error("处理第三方payment provider = {} id = {} 时发生错误", record.getPaymentProvider(), record.getId(), e);
    }
  }

  public void handleDynamicAccountRepayment(ThirdpartyDynamicAccountRepaymentRecord record) {
    try {
      ProcessStatus status = ProcessStatus.fromCode(record.getStatus());
      if (status != ProcessStatus.UNPROCESSED) {
        return;
      }
      PaymentVO paymentVO = paymentService.getPaymentVO(record.getPaymentTransId());
      PaymentProcessResult result = getPaymentProcessResult(paymentVO);
      sensorsService.uploadRepaymentSuccessEvent(paymentVO, null);
      handleProcessResult(record, paymentVO.userId, result);
    } catch (Exception e) {
      log.error("处理动态账号还款 payment provider = {} id = {} 时发生错误", record.getPaymentProvider(), record.getId(), e);
    }
  }

  public void handleDirectDebitRepayment(ThirdpartyDirectDebitRepaymentRecord record) {
    try {
      ProcessStatus status = ProcessStatus.fromCode(record.getStatus());
      if (status != ProcessStatus.UNPROCESSED) {
        return;
      }
      PaymentVO paymentVO = paymentService.getPaymentVO(record.getTransNo());
      PaymentProcessResult result = getPaymentProcessResult(paymentVO);
      handleProcessResult(record, paymentVO.userId, result);
    } catch (Exception e) {
      log.error("handle direct debit payment provider = {} id = {} is error", record.getTransNo(), record.getId(), e);
    }
  }

  public PaymentProcessResult getPaymentProcessResult(PaymentVO paymentVO) {
    PaymentProcessResult processResult;
    switch (paymentVO.paymentAccount) {
      case IDN_FIN:
        return financingTransactionService.onlineTopUp(paymentVO);
      case IDN:
        // 如果是逾期用户 还款意向中有jbp的意向则失效
        unionRepaymentService.expireDeductIntentionForOverdueUser(paymentVO.userId);
        processResult = unionRepaymentService.repay(paymentVO);
        repaymentAnalysisService.completeRepaymentAnalysisRecord(paymentVO);
        return processResult;
      default:
        throw EcException.error("not support this operation, paymentAccount is {}, paymentVO is {}", paymentVO.paymentAccount, JsonUtils.toString(paymentVO));
    }
  }

  private void handleProcessResult(ThirdpartyRepaymentRecord record, Long userId, PaymentProcessResult processResult) {
    if (processResult.processStatus == ProcessStatus.UNPROCESSED) {
      return;
    }
    thirdPartyRepaymentModel.updateStatus(record, processResult.processStatus, processResult.reasonCode);
    if (processResult.processStatus == ProcessStatus.FAILED) {
      log.warn("Something wrong while handle repayment, user id = {} , 3rdParty repayment id = {}， reason code = {}", userId, record.getId(), processResult.reasonCode.code);
    }
  }

  private void handleProcessResult(ThirdpartyDynamicAccountRepaymentRecord record, Long userId, PaymentProcessResult processResult) {
    if (processResult.processStatus == ProcessStatus.UNPROCESSED) {
      return;
    }
    thirdPartyDynamicAccountRepaymentModel.updateStatus(record, processResult.processStatus, processResult.reasonCode);
    if (processResult.processStatus == ProcessStatus.FAILED) {
      log.warn("Something wrong while handle dynamic account repayment, user id = {} , 3rdParty repayment id = {}， reason code = {}", userId, record.getId(), processResult.reasonCode.code);
    }
  }

  private void handleProcessResult(ThirdpartyDirectDebitRepaymentRecord record, Long userId, PaymentProcessResult processResult) {
    if (processResult.processStatus == ProcessStatus.UNPROCESSED) {
      return;
    }
    threadTransactionalModel.transaction(configuration -> {
      ThirdpartyDirectDebitRepaymentRecord currentRecord = thirdPartyDirectDebitRepaymentModel.fetchByIdForUpdate(record.getId());
      if (ProcessStatus.fromCode(currentRecord.getStatus()) == processResult.processStatus) {
        return;
      }
      thirdPartyDirectDebitRepaymentModel.updateStatus(currentRecord, processResult.processStatus, processResult.reasonCode);
      if (processResult.processStatus == ProcessStatus.FAILED) {
        log.warn("Something wrong while handle direct debit account repayment, user id = {} , 3rdParty repayment id = {}， reason code = {}", userId, record.getId(), processResult.reasonCode.code);
      }
    });
  }

}
