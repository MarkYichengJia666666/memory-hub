package com.yqg.core.service.kafka.consumer.processor;

import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.core.service.cashloan.observer.IRiskObserver;
import com.yqg.core.service.risk.event.RiskEvent;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Slf4j
public abstract class BaseRiskEventProcessor implements IKafkaMessageProcessor<String, String> {
  @Override
  public void processRecord(ConsumerRecord<String, String> record) {
    log.info("Processing message, key:{}, value:{}", record.key(), record.value());
    RiskEvent event = JsonUtils.fromOrException(record.value(), RiskEvent.class);
    IRiskObserver observer = getObserver();
    switch (event.eventType) {
      case CREDIT_CALC_INIT:
      case ACCEPT:
      case REJECT:
      case REAPPLY:
        observer.processRiskEvent(event.eventType, LoanUserRiskType.fromValue(event.riskType), event.traceId, event.gaid, event.dataCallbackChannel, event.loanUserCreditsInfoVO);
        break;
      case SUBMIT_ORDER_TRACE:
        observer.processOrderTraceFinishEvent(event.eventType, LoanUserRiskType.fromValue(event.riskType), event.traceId, event.gaid, event.dataCallbackChannel, event.loanUserCreditsInfoVO, event.orderVO);
        break;
      default:
        throw EcException.error("Unsupported type type " + event.eventType);
    }
  }

  abstract protected IRiskObserver getObserver();
}
