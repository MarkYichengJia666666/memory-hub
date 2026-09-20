package com.yqg.core.service.kafka.consumer.context;

import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;
import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.core.service.kafka.consumer.ConsumerGroup;
import com.yqg.core.service.kafka.consumer.processor.EcCombinedRepayEventProcessor;
import com.yqg.core.service.kafka.generator.KafkaTopicGenerator;
import org.springframework.stereotype.Service;

/**
 * EC 合并支付事件，包含支付和回调用type区分
 */
@Service
public class EcCombinedRepayEventGroupContext extends AbstractGenericConsumerGroupContext {

  @Override
  protected String getConsumerGroupName() {
    return ConsumerGroup.EC_COMBINED_REPAY_CALL_BACK_EVENT_GROUP.name();
  }

  @Override
  protected String getAutoOffsetResetConfig() {
    return "latest";
  }

  @Override
  public Class<? extends IKafkaMessageProcessor> getProcessorClass() {
    return EcCombinedRepayEventProcessor.class;
  }

  @Override
  public String getTopic() {
    return KafkaTopicGenerator.getTopic(KafkaTopic.EC_COMBINED_REPAY_EVENT);
  }
}
