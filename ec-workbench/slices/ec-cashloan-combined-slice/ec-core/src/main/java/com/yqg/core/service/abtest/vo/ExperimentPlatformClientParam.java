package com.yqg.core.service.abtest.vo;

import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.experiment.common.enums.ResultGetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author chaoye
 * @date 2025/1/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExperimentPlatformClientParam {
  /**
   * 实验相关属性
   */
  public String experimentName;
  public Long userId;
  public String deviceToken;
  public String nik;
  public ResultGetType resultGetType;
  /**
   * 实时数据，用于 实验中台 调用 ec首页策略
   */
  public GeneralPageConfigParam generalPageConfigParam;
  private boolean checkApiChannelAndRouteToBlankGroup = false;

  public static ExperimentPlatformClientParam from(String experimentName,
                                                   Long userId,
                                                   String deviceToken,
                                                   String nik,
                                                   GeneralPageConfigParam generalPageConfigParam) {
    ExperimentPlatformClientParam param = new ExperimentPlatformClientParam();
    param.experimentName = experimentName;
    param.userId = userId;
    param.deviceToken = deviceToken;
    param.nik = nik;
    param.generalPageConfigParam = generalPageConfigParam;
    return param;
  }

  public static ExperimentPlatformClientParam from(String experimentName,
                                                   Long userId,
                                                   String deviceToken,
                                                   String nik) {
    ExperimentPlatformClientParam param = new ExperimentPlatformClientParam();
    param.experimentName = experimentName;
    param.userId = userId;
    param.deviceToken = deviceToken;
    param.nik = nik;
    return param;
  }

  public static ExperimentPlatformClientParam from(String experimentName,
                                                   Long userId,
                                                   String deviceToken,
                                                   String nik,
                                                   ResultGetType resultGetType) {
    ExperimentPlatformClientParam param = new ExperimentPlatformClientParam();
    param.experimentName = experimentName;
    param.userId = userId;
    param.deviceToken = deviceToken;
    param.nik = nik;
    param.resultGetType = resultGetType;
    return param;
  }

  public static ExperimentPlatformClientParam from(String experimentName,
                                                   Long userId,
                                                   String deviceToken,
                                                   String nik,
                                                   ResultGetType resultGetType,
                                                   GeneralPageConfigParam generalPageConfigParam) {
    ExperimentPlatformClientParam param = new ExperimentPlatformClientParam();
    param.experimentName = experimentName;
    param.userId = userId;
    param.deviceToken = deviceToken;
    param.nik = nik;
    param.resultGetType = resultGetType;
    param.generalPageConfigParam = generalPageConfigParam;
    return param;
  }

}
