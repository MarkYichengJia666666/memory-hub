package com.yqg.core.service.app;

import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.sql.user.UserPageActiveModel;
import com.yqg.core.service.app.vo.AppStartupAtLeastOnceVO;
import com.yqg.core.service.cashloan.AppConfig;
import com.yqg.core.service.kafka.KafkaTopic;
import com.yqg.core.service.kafka.generator.KafkaTopicGenerator;
import com.yqg.core.service.kafka.producer.IKafkaMessageService;
import com.yqg.core.service.mc.MarketingCenterClientService;
import com.yqg.core.service.notification.enums.SystemNotifScene;
import com.yqg.core.service.notification.param.system.UserLoginParam;
import com.yqg.core.service.user.UserReleaseConfig;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for handling app startup events
 * This service provides methods to send user startup tracking Kafka messages
 * Messages are guaranteed to be delivered at least once
 *
 * @author lihancock
 */
@Slf4j
@Service
public class AppStartupService {

  @Autowired
  private IKafkaMessageService kafkaMessageService;

  @Autowired
  private AppConfig appConfig;

  @Autowired
  private UserReleaseConfig userReleaseConfig;

  @Autowired
  private UserPageActiveModel userPageActiveModel;
  @Autowired
  private MarketingCenterClientService marketingCenterClientService;

  /**
   * Process app startup event This method is called from multiple endpoints and is guaranteed to be delivered at least once
   *
   * @param userId            User ID (can be null for not login users)
   * @param requestClientType
   * @param openAppPage       The page being opened
   */
  public void processAppStartupEvent(Long userId, RequestClientType requestClientType, String openAppPage) {
    if (shouldSkipEventProcessing(userId)) {
      return;
    }

    sendAppStartupEvent(userId, openAppPage);

    if (StringUtils.isNotBlank(openAppPage)) {
      marketingCenterClientService.publishSystemEvent(SystemNotifScene.USER_LOGIN,
          new UserLoginParam(userId, SDKType.IDN_YQD, Clock.now()));
      userPageActiveModel.insertOrUpdate(userId, openAppPage, Optional.ofNullable(requestClientType).orElse(RequestClientType.UNKNOWN));
    }
  }

  private boolean shouldSkipEventProcessing(Long userId) {
    if (appConfig.isAppStartupEventDisabled()) {
      log.debug("App startup event disabled. Skipping for userId: {}", userId);
      return true;
    }
    if (userId == null) {
      return true;
    }
    return !userReleaseConfig.getEnableStartupMsgRelease().isAllowed(userId);
  }

  private void sendAppStartupEvent(Long userId, String openAppPage) {
    try {
      // Create a startup event object
      AppStartupAtLeastOnceVO startupVO = AppStartupAtLeastOnceVO.builder()
          .userId(userId)
          .openAppPage(openAppPage)
          .deviceToken(ImpliedContextUtils.deviceToken())
          .requestClientType(ImpliedContextUtils.requestClientType())
          .sourceType(ImpliedContextUtils.sourceType())
          .sdkType(ImpliedContextUtils.sdkType())
          .appBuild(ImpliedContextUtils.build())
          .timestamp(Clock.now())
          .build();

      // Convert to JSON and send to APP_STARTUP_AT_LEAST_ONCE_EVENT topic
      String message = JsonUtils.toString(startupVO);

      // Use round-robin strategy (no partition key)
      kafkaMessageService.schedule(
          KafkaTopicGenerator.getTopic(KafkaTopic.APP_STARTUP_AT_LEAST_ONCE_EVENT),
          String.valueOf(userId),
          message
      );
    } catch (Exception e) {
      log.error("Error when sending app startup event", e);
    }
  }
}
