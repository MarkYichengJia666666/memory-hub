package com.yqg.core.service.kafka.consumer.processor;

import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.service.capital.CapitalFeeEventService;
import com.yqg.core.service.capital.enums.CapitalFeeEventStatus;
import com.yqg.core.service.capital.event.FundRepayEvent;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.institu.fund.client.api.IFundEvent;
import com.yqg.institu.fund.common.dto.FundRepayEventDto;
import com.yqg.institu.fund.common.enums.repay.RepayEventStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author: manyeLiu
 * @description: TODO
 */
@Component
public class FundRepayEventProcessor implements IKafkaMessageProcessor<String, String> {
  @Autowired
  private IFundEvent iFundEvent;
  @Autowired
  private CapitalFeeEventService capitalFeeEventService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;

  @Override
  public void processRecord(ConsumerRecord<String, String> record) {
    try {
      FundRepayEvent fundRepayEvent = JsonUtils.fromOrException(record.value(), FundRepayEvent.class);
      threadTransactionalModel.transactionResult(configuration -> {// 写入二次入账capitalFeeEvent表
        Boolean needRepay = fundRepayEvent.getNeedRepay();
        capitalFeeEventService.createCapitalFeeEvent(fundRepayEvent.getOrderId(), fundRepayEvent.getDeductId(), fundRepayEvent.getLenderType(), fundRepayEvent.getInstallmentId(), Objects.isNull(needRepay) || needRepay ? CapitalFeeEventStatus.INIT : CapitalFeeEventStatus.SUCCESS);
        // 写入二次入账event表
        FundRepayEventDto fundRepayEventDto = FundRepayEventDto.from(fundRepayEvent.getOrderId(),
            fundRepayEvent.getDeductId(),
            fundRepayEvent.getInstallmentId(),
            fundRepayEvent.getLenderType().name(),
            fundRepayEvent.getSourceType().name(),
            fundRepayEvent.getRepayAmount(),
            Objects.isNull(needRepay) || needRepay ? RepayEventStatus.INIT : RepayEventStatus.SUCCESS);

        iFundEvent.insertRepayEvent(fundRepayEventDto); // 通知机构资金写入event
        return null;
      });
    } catch (Exception e) {
      throw EcException.error("FundRepayEventProcessor processRecord error", e);
    }
  }
}
