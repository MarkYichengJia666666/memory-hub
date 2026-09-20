package com.yqg.core.service.riskprocessor.infra;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.sql.loanusertrace.TriggerSubType;
import com.yqg.core.util.env.DeviceInfo;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.enums.loan.SourceType;

/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/21 5:56 下午
 */
public class SubmitExtraInfo {

  public PlatformType platformType;
  public DeviceInfo deviceInfo;
  public SourceType sourceType;
  // 默认由用户提交
  public boolean submitFromUser = true;
  public TriggerSubType triggerSubType;
  public BooleanType submitRisk;

  public static SubmitExtraInfo fromPlatformType(PlatformType platformType, SourceType sourceType) {
    return from(platformType, null, sourceType);
  }

  public static SubmitExtraInfo fromDeviceInfo(DeviceInfo deviceInfo, SourceType sourceType, boolean submitFromUser) {
    return from(null, deviceInfo, sourceType, submitFromUser, null, null);
  }

  public static SubmitExtraInfo fromSourceType(SourceType sourceType) {
    return from(null, null, sourceType);
  }

  public static SubmitExtraInfo fromTriggerSubTypeAndSubmitRisk(SourceType sourceType, TriggerSubType triggerSubType, BooleanType submitRisk) {
    return from(null, null, sourceType, true, triggerSubType, submitRisk);
  }

  public static SubmitExtraInfo from(PlatformType platformType, DeviceInfo deviceInfo, SourceType sourceType) {
    return from(platformType, deviceInfo, sourceType, true, null, null);
  }

  public static SubmitExtraInfo from(
      PlatformType platformType,
      DeviceInfo deviceInfo,
      SourceType sourceType,
      boolean submitFromUser,
      TriggerSubType triggerSubType,
      BooleanType submitRisk
  ) {
    SubmitExtraInfo extraInfo = new SubmitExtraInfo();
    extraInfo.platformType = platformType;
    extraInfo.deviceInfo = deviceInfo;
    extraInfo.sourceType = sourceType;
    extraInfo.submitFromUser = submitFromUser;
    extraInfo.triggerSubType = triggerSubType;
    extraInfo.submitRisk = submitRisk;
    return extraInfo;
  }

  public static SubmitExtraInfo fromSourceType(SourceType sourceType, boolean submitFromUser) {
    return from(null, null, sourceType, submitFromUser, null, null);
  }

  public static SubmitExtraInfo fromTriggerSubTypeAndSubmitRisk(TriggerSubType triggerSubType, SourceType sourceType, boolean submitFromUser, BooleanType submitRisk) {
    return from(null, null, sourceType, submitFromUser, triggerSubType, submitRisk);
  }
}
