package com.yqg.core.service.kafka.generator;

import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.ec.common.utils.SysEnvironment;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: ListenYoung
 * @date: Created on 11:06 2020/11/30
 * @modified By:
 */
public class KafkaTopicGenerator {
  private static String prefix = "";

  public static void setPrefix(String prefix) {
    if (StringUtils.isNotBlank(prefix)) {
      KafkaTopicGenerator.prefix = prefix;
    }
  }
  public static String getTopic(KafkaTopic topic) {
    return SysEnvironment.isProd() ? prefix + topic.str : topic.str;
  }
}