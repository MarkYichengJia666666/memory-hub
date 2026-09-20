package com.yqg.core.service.kafka.consumer.context;

import com.yqg.common.kafka.consumer.MessageDeliverySemantics;
import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;

import java.util.Properties;

public interface IConsumerGroupContext {

  Properties getConsumerProperties();

  Class<? extends IKafkaMessageProcessor> getProcessorClass();

  MessageDeliverySemantics getDeliverySemantics();

  String getTopic();
}
