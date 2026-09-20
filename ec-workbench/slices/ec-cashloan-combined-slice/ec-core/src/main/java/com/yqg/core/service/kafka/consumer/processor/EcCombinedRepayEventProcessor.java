package com.yqg.core.service.kafka.consumer.processor;

import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;
import com.yqg.core.service.cashloan.repay.UnionRepaymentService;
import com.yqg.core.service.jbp.goldencard.vo.RepaymentEventVO;
import com.yqg.ec.common.enums.UnionRepaymentResultType;
import com.yqg.ec.common.serialization.JsonUtils;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EcCombinedRepayEventProcessor implements IKafkaMessageProcessor<String, String> {

  @Autowired
  private UnionRepaymentService unionRepaymentService;

  @Override
  public void processRecord(ConsumerRecord<String, String> record) {
    log.info("Processing EcCombinedRepayEventProcessor message, key : {}, value : {}", record.key(), record.value());
    RepaymentEventVO event = JsonUtils.fromOrException(record.value(), RepaymentEventVO.class);
    switch (UnionRepaymentResultType.valueOf(event.getUnionRepaymentResultType())) {
      case CALLBACK:
        if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0 || Objects.isNull(event.getUserId()) || Objects.isNull(event.getOrderId())) {
          log.warn("Invalid RepaymentEventVO, amount: {}, userId: {}", event.getAmount(), event.getUserId());
          return;
        }
        processEcCombinedRepayEvent(event);
        break;

      default:
        break;
    }
  }

  private void processEcCombinedRepayEvent(RepaymentEventVO event) {
    unionRepaymentService.dealCombinedRepayJbpCallBack(event.getTransId(), event.getAmount(), event.getUserId());
  }
}
