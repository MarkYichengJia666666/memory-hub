package com.yqg.core.service.kafka.consumer.context;

import com.yqg.common.kafka.consumer.MessageDeliverySemantics;
import com.yqg.ec.common.configuration.ISiteVars;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * @author fudongyi
 * @date 2023/4/28
 */
@Component
public abstract class AbstractGenericConsumerGroupContext implements IConsumerGroupContext {
  @Autowired
  private ISiteVars ecSiteVars;

  @Override
  public Properties getConsumerProperties() {
    Properties props = new Properties();
    props.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        ecSiteVars.getString("kafka.bootstrap.server", "http://localhost:9092")
    );
    props.put(ConsumerConfig.GROUP_ID_CONFIG, getConsumerGroupName());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, getAutoOffsetResetConfig());
    props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 900000);
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 200);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    return props;
  }

  protected abstract String getConsumerGroupName();

  protected abstract String getAutoOffsetResetConfig();

  @Override
  public MessageDeliverySemantics getDeliverySemantics() {
    return MessageDeliverySemantics.AT_LEAST_ONCE;
  }
}
