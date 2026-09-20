package com.yqg.core.service.kafka.producer;

import com.yqg.common.kafka.producer.KafkaMessage;
import com.yqg.common.kafka.producer.PublishResult;

import java.util.Map;

/**
 * Created by xiahonggao on 2017/3/22.
 */
public interface IKafkaMessageService {
  KafkaMessage schedule(String topic, String message);
  KafkaMessage schedule(String topic, String partitionKey, String message);
  PublishResult publish(Integer batchSize);
  Map<String, PublishResult> publishWithMultipleMessageTables(Integer batchSize);

  /**
   * 使用专用 publisher 发布 cash_loan_event 拆表（kafka_message_cash_loan_event）的消息。
   * 与 {@link #publishWithMultipleMessageTables(Integer)} 由不同 publisher 实例驱动，
   * 两路并发互不阻塞。详见 specs/TAPD-349202-kafka-publish-job-split/sketch.md。
   */
  Map<String, PublishResult> publishCashLoanEvent(Integer batchSize);
}
