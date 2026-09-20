package com.yqg.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yqg.common.kafka.consumer.ConsumerRunnerPool;
import com.yqg.common.kafka.consumer.ConsumerRunnerPoolConfig;
import com.yqg.common.kafka.consumer.MessageDeliverySemantics;
import com.yqg.common.kafka.consumer.processor.ProcessorFactory;
import com.yqg.core.service.kafka.consumer.ConsumerGroup;
import com.yqg.core.service.kafka.consumer.context.IConsumerGroupContext;
import com.yqg.core.service.monitor.RetentionPolicies;
import com.yqg.core.util.ioc.SpringUtils;
import com.yqg.ec.common.configuration.ISiteVars;
import com.yqg.ec.common.serialization.JsonUtils;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * Created by xiuqichenyang on 19/9/15.
 */
@Service
@Slf4j
public class ConsumerPoolService implements ApplicationRunner {
  @Autowired
  protected InfluxDB influxDB;
  @Autowired
  protected ISiteVars iSiteVars;

  protected static ConsumerRunnerPoolConfig genRunnerPoolConfig(
      IConsumerGroupContext context,
      int numOfConsumers
  ) {

    return ConsumerRunnerPoolConfig.builder()
        .topic(context.getTopic())
        .numberOfRunners(numOfConsumers)
        .deliverySemantics(context.getDeliverySemantics())
        .consumerProperties(context.getConsumerProperties())
        .build();
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    String value = iSiteVars.getString("kafka.topic_config");
    Map<ConsumerGroup, Integer> map = JsonUtils.fromOrException(value, new TypeReference<Map<ConsumerGroup, Integer>>() {});
    for (Map.Entry<ConsumerGroup, Integer> entry : Objects.requireNonNull(map).entrySet()) {
      IConsumerGroupContext context = SpringUtils.getInstance(entry.getKey().getContextClass());
      ConsumerRunnerPoolConfig config = genRunnerPoolConfig(context, entry.getValue());

      influxDB.setDatabase(iSiteVars.getString("metrics.datasource.influxdb.dbname"));
      influxDB.setRetentionPolicy(RetentionPolicies.ONE_MONTH.value);

      ConsumerRunnerPool pool = ConsumerRunnerPool.builder()
          .config(config)
          .influxDB(influxDB)
          .processorFactory(() -> SpringUtils.getInstance(context.getProcessorClass()))
          .build();
      pool.start();
      log.info("ConsumerGroup {} start {} runners", entry.getKey(), entry.getValue());
    }
  }
}
