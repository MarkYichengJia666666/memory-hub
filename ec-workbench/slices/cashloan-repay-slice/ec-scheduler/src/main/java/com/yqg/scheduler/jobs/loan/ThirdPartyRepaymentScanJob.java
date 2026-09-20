package com.yqg.scheduler.jobs.loan;

import com.yqg.core.model.generated.tables.records.ManualRepaymentRecord;
import com.yqg.core.model.generated.tables.records.ThirdpartyDirectDebitRepaymentRecord;
import com.yqg.core.model.generated.tables.records.ThirdpartyDynamicAccountRepaymentRecord;
import com.yqg.core.model.generated.tables.records.ThirdpartyRepaymentRecord;
import com.yqg.core.model.sql.thirdparty.ManualRepaymentModel;
import com.yqg.core.model.sql.thirdparty.ThirdPartyDirectDebitRepaymentModel;
import com.yqg.core.model.sql.thirdparty.ThirdPartyDynamicAccountRepaymentModel;
import com.yqg.core.model.sql.thirdparty.ThirdPartyRepaymentModel;
import com.yqg.core.model.sql.thirdparty.enums.ProcessStatus;
import com.yqg.core.service.cashloan.CashLoanService;
import com.yqg.core.service.financing.transaction.FinancingTransactionService;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.payment.ThirdPartyRepaymentService;
import com.yqg.core.service.payment.vo.PaymentProcessResult;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.scheduler.base.YqgBaseJob;
import com.yqg.scheduler.vo.JobExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Created by xiuqichenyang on 17/7/27.
 */
@Service
@Slf4j
public class ThirdPartyRepaymentScanJob extends YqgBaseJob {
  @Autowired
  private ThirdPartyRepaymentModel thirdPartyRepaymentModel;
  @Autowired
  private ThirdPartyRepaymentService thirdPartyRepaymentService;
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private ThirdPartyDynamicAccountRepaymentModel thirdPartyDynamicAccountRepaymentModel;
  @Autowired
  private ManualRepaymentModel manualRepaymentModel;
  @Autowired
  private ThirdPartyDirectDebitRepaymentModel thirdPartyDirectDebitRepaymentModel;

  @Override
  public void exec(JobExecutionContext context) throws Exception {
    Param param = getParam(context, Param.class);
    if (Objects.isNull(param.secondsBefore)) {
      throw EcException.error("ThirdPartyRepaymentScanJob param secondsBefore is null!");
    }
    Long secondsBefore = param.secondsBefore * Clock.MILLS_PER_SECOND;
    Long endTimeStamp = Clock.now() - secondsBefore;

    List<ThirdpartyRepaymentRecord> unprocessedRecords = thirdPartyRepaymentModel.fetchUnprocessedRecords(endTimeStamp);
    unprocessedRecords.forEach(record -> thirdPartyRepaymentService.handleStaticAccountRepayment(record));

    List<ThirdpartyDynamicAccountRepaymentRecord> unprocessedDynamicRecords = thirdPartyDynamicAccountRepaymentModel.fetchUnprocessedRecords(endTimeStamp);
    unprocessedDynamicRecords.forEach(record -> thirdPartyRepaymentService.handleDynamicAccountRepayment(record));

    List<ThirdpartyDirectDebitRepaymentRecord> unprocessedDirectDebits = thirdPartyDirectDebitRepaymentModel.fetchUnprocessedRecords(endTimeStamp);
    unprocessedDirectDebits.forEach(record -> thirdPartyRepaymentService.handleDirectDebitRepayment(record));

    List<ManualRepaymentRecord> unprocessedManualRecords = manualRepaymentModel.fetchUnprocessedRecords();
    unprocessedManualRecords.forEach(record -> {
      try {
        PaymentVO paymentVO = paymentService.getPaymentVO(record.getPaymentTransId());
        handleProcessResult(record, paymentVO.userId, thirdPartyRepaymentService.getPaymentProcessResult(paymentVO));
      } catch (Exception e) {
        log.error("处理手动还款 payment provider = {} id = {} 时发生错误", record.getPaymentProvider(), record.getId(), e);
      }
    });
  }

  private void handleProcessResult(ManualRepaymentRecord record, Long userId, PaymentProcessResult processResult) {
    if (processResult.processStatus == ProcessStatus.UNPROCESSED) {
      return;
    }
    manualRepaymentModel.updateStatus(record, processResult.processStatus, processResult.reasonCode);
    if (processResult.processStatus == ProcessStatus.FAILED) {
      log.warn("Something wrong while handle manual repayment, user id = {} , manual repayment id = {}， reason code = {}", userId, record.getId(), processResult.reasonCode.code);
    }
  }

  public static class Param {
    public Integer secondsBefore = 30;
  }
}



