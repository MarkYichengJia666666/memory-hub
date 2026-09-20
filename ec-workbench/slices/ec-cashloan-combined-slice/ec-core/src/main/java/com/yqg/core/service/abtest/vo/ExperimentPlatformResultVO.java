package com.yqg.core.service.abtest.vo;

import com.alibaba.druid.util.StringUtils;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.abtest.enums.ExperimentPlatformExperimentStatus;
import com.yqg.core.service.abtest.enums.ExperimentPlatformGroupResult;
import com.yqg.core.service.abtest.enums.ExperimentPlatformGroupType;

/**
 * @author chaoye
 * @date 2025/1/14
 */
public class ExperimentPlatformResultVO {
  public ExperimentPlatformGroupType groupType;
  public ExperimentPlatformExperimentStatus experimentStatus;
  public ExperimentPlatformGroupResult noHitResult;  //未命中实验组，空白组，对照组时的具体原因
  public String hitResult;  //命中实验组，空白组，对照组时的分流结果
  public String groupParam; //实验组，空白组，对照组在实验中台配置的json参数

  private static final String BLANK_GROUP = "BLANK_GROUP";
  private static final String DIVERSION_BLANK_GROUP = "DIVERSION_BLANK_GROUP";

  public static ExperimentPlatformResultVO from(ExperimentPlatformGroupType groupType,
                                                ExperimentPlatformExperimentStatus experimentStatus,
                                                ExperimentPlatformGroupResult noHitResult,
                                                String hitResult,
                                                String groupParam) {
    ExperimentPlatformResultVO resultVO = new ExperimentPlatformResultVO();
    resultVO.groupType = groupType;
    resultVO.experimentStatus = experimentStatus;
    resultVO.noHitResult = noHitResult;
    resultVO.hitResult = hitResult;
    resultVO.groupParam = groupParam;
    return resultVO;
  }

  public static ExperimentPlatformResultVO fromSimple(String hitResult) {
    if (ExperimentPlatformResultVO.hitBlankGroup(hitResult)) {
      return ExperimentPlatformResultVO.fromExperimentNotIn();
    }
    ExperimentPlatformResultVO resultVO = new ExperimentPlatformResultVO();
    resultVO.hitResult = hitResult;
    return resultVO;
  }

  public static ExperimentPlatformResultVO fromFallback() {
    ExperimentPlatformResultVO resultVO = new ExperimentPlatformResultVO();
    resultVO.noHitResult = ExperimentPlatformGroupResult.EXPERIMENT_FALL_BACK;
    return resultVO;
  }

  public static ExperimentPlatformResultVO fromExperimentNotIn() {
    ExperimentPlatformResultVO resultVO = new ExperimentPlatformResultVO();
    resultVO.noHitResult = ExperimentPlatformGroupResult.EXPERIMENT_NOT_IN;
    return resultVO;
  }

  public static ExperimentPlatformResultVO fromDiversionBlankGroup() {
    ExperimentPlatformResultVO resultVO = new ExperimentPlatformResultVO();
    resultVO.noHitResult = ExperimentPlatformGroupResult.DIVERSION_BLANK_GROUP;
    return resultVO;
  }

  public boolean hitBlankGroup() {
    return noHitResult != null
        || StringUtils.equals(BLANK_GROUP, hitResult)
        || StringUtils.equals(DIVERSION_BLANK_GROUP, hitResult)
        || StringUtils.equals(ExperimentPlatformGroupResult.DIVERSION_CROWD_NO_PASS.name(), hitResult)
        || StringUtils.equals(ExperimentPlatformGroupResult.PARENT_DIVERSION_BLANK_GROUP.name(), hitResult)
        || StringUtils.equals(ExperimentPlatformGroupResult.PARENT_DIVERSION_CROWD_NO_PASS.name(), hitResult)
        || StringUtils.equals(ExperimentPlatformGroupResult.DIVERSION_FALL_BACK.name(), hitResult)
        ;
  }

  public static boolean hitBlankGroup(String hitResult) {
    return  StringUtils.equals(BLANK_GROUP, hitResult)
        || StringUtils.equals(DIVERSION_BLANK_GROUP, hitResult)
        || StringUtils.equals(ExperimentPlatformGroupResult.DIVERSION_CROWD_NO_PASS.name(), hitResult)
        || StringUtils.equals(ExperimentPlatformGroupResult.PARENT_DIVERSION_BLANK_GROUP.name(), hitResult)
        || StringUtils.equals(ExperimentPlatformGroupResult.PARENT_DIVERSION_CROWD_NO_PASS.name(), hitResult)
        || StringUtils.equals(ExperimentPlatformGroupResult.DIVERSION_FALL_BACK.name(), hitResult)
        ;
  }
  public static ExperimentPlatformResultVO fromHitDefaultResult(ExperimentNameSpace nameSpace) {
    ExperimentPlatformResultVO resultVO = new ExperimentPlatformResultVO();
    resultVO.groupType = ExperimentPlatformGroupType.EXPERIMENT_GROUP; // 默认结果命中实验组,有依赖
    resultVO.hitResult = nameSpace.apiChannelDefaultScene;
    return resultVO;
  }

}
