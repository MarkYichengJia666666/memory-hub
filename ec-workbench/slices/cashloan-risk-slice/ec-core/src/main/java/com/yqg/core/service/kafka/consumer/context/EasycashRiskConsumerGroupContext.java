package com.yqg.core.service.kafka.consumer.context;

import com.yqg.common.kafka.consumer.MessageDeliverySemantics;
import com.yqg.common.kafka.consumer.processor.IKafkaMessageProcessor;
import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.core.service.kafka.consumer.ConsumerGroup;
import com.yqg.core.service.kafka.consumer.processor.EasycashRiskProcessor;
import com.yqg.core.service.kafka.generator.KafkaTopicGenerator;
import com.yqg.ec.common.configuration.ISiteVars;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/8/13 3:42 下午
 */
@Service
public class EasycashRiskConsumerGroupContext implements IConsumerGroupContext {
  @Autowired
  private ISiteVars ecSiteVars;

  @Override
  public Properties getConsumerProperties() {
    Properties props = new Properties();
    props.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        ecSiteVars.getString("kafka.bootstrap.server", "http://localhost:9092")
    );
    props.put(ConsumerConfig.GROUP_ID_CONFIG, ConsumerGroup.RISK_EASYCASH.name());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 600000);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    return props;
  }

  @Override
  public Class<? extends IKafkaMessageProcessor> getProcessorClass() {
    return EasycashRiskProcessor.class;
  }

  @Override
  public MessageDeliverySemantics getDeliverySemantics() {
    return MessageDeliverySemantics.AT_LEAST_ONCE;
  }

  @Override
  public String getTopic() {
    return KafkaTopicGenerator.getTopic(KafkaTopic.RISK_EVENT);
  }
}
