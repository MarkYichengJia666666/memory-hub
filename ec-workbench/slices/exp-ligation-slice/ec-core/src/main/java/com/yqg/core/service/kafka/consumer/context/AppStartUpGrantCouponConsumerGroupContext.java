package com.yqg.core.service.kafka.consumer.context;

import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;
import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.core.service.kafka.consumer.ConsumerGroup;
import com.yqg.core.service.kafka.consumer.processor.AppStartUpGrantCouponProcessor;
import com.yqg.core.service.kafka.generator.KafkaTopicGenerator;
import org.springframework.stereotype.Service;

@Service
public class AppStartUpGrantCouponConsumerGroupContext extends AbstractGenericConsumerGroupContext {

  @Override
  protected String getConsumerGroupName() {
    return ConsumerGroup.APP_STARTUP_GRANT_COUPON_GROUP.name();
  }

  @Override
  protected String getAutoOffsetResetConfig() {
    return "latest";
  }

  @Override
  public Class<? extends IKafkaMessageProcessor> getProcessorClass() {
    return AppStartUpGrantCouponProcessor.class;
  }

  @Override
  public String getTopic() {
    return KafkaTopicGenerator.getTopic(KafkaTopic.APP_STARTUP_AT_LEAST_ONCE_EVENT);
  }
}
