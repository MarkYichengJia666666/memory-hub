package com.yqg.core.service.kafka.consumer.context;

import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;
import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.core.service.kafka.consumer.ConsumerGroup;
import com.yqg.core.service.kafka.consumer.processor.DownstreamOrderRepaySyncProcessor;
import com.yqg.core.service.kafka.generator.KafkaTopicGenerator;
import org.springframework.stereotype.Component;

/**
 * 下游订单/还款四表同步专用 ConsumerGroup，独立消费 CASH_LOAN_EVENT，仅做同步推送。
 */
@Component
public class DownstreamOrderRepaySyncConsumerGroupContext extends AbstractGenericConsumerGroupContext {

  @Override
  protected String getConsumerGroupName() {
    return ConsumerGroup.CASH_LOAN_EVENT_DOWNSTREAM_SYNC.name();
  }

  @Override
  protected String getAutoOffsetResetConfig() {
    return "latest";
  }

  @Override
  public Class<? extends IKafkaMessageProcessor> getProcessorClass() {
    return DownstreamOrderRepaySyncProcessor.class;
  }

  @Override
  public String getTopic() {
    return KafkaTopicGenerator.getTopic(KafkaTopic.CASH_LOAN_EVENT);
  }
}
