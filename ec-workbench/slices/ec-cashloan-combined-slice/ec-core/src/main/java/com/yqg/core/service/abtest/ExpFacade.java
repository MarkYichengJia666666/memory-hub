package com.yqg.core.service.abtest;

import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.abtest.enums.DiversionKeyType;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.abtest.vo.ABTestBaseRequestVO;
import com.yqg.core.service.abtest.vo.ABTestUserIdRequestVO;
import com.yqg.core.service.abtest.vo.ExperimentPlatformClientParam;
import com.yqg.core.service.abtest.vo.ExperimentPlatformResultVO;
import com.yqg.core.util.scope.ImpliedContextUtils;
import com.yqg.ec.common.enums.loan.SourceType;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description：仅用于适配老分流
 * @Author: lihancock
 * @Email: wenyaoli@fintopia.tech
 * @Date: 2025/12/4 19:44
 */
@Slf4j
@Service
public class ExpFacade {

  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private ExpPreDiversionClient expPreDiversionClient;
  @Autowired
  private ExpLastResultClient expLastResultClient;
  @Autowired
  private ExpLastResultWithPreDiversionClient expLastResultWithPreDiversionClient;

  public String fetchResultFallBackWithDefaultScene(String expKey, ClientType clientType, ABTestBaseRequestVO abTestBaseRequestVO,
      Long build, ExperimentNameSpace... parentNameSpaces) {
    String res = dryRun(expKey, clientType, abTestBaseRequestVO, build, ImpliedContextUtils.sourceType());
    if (ExperimentPlatformResultVO.hitBlankGroup(res)) {
      return abTestBaseRequestVO.experimentNameSpace.defaultScene;
    }
    return res;
  }

  public String fetchResultFallBackWithDefaultScene(String expKey, ClientType clientType, ABTestBaseRequestVO abTestBaseRequestVO) {
    String res = dryRun(expKey, clientType, abTestBaseRequestVO, ImpliedContextUtils.build(), ImpliedContextUtils.sourceType());
    if (ExperimentPlatformResultVO.hitBlankGroup(res)) {
      return abTestBaseRequestVO.experimentNameSpace.defaultScene;
    }
    return res;
  }

  public String fetchResultFallBackWithDefaultSceneWithBuildAndSourceType(String expKey, ClientType clientType,
      ABTestBaseRequestVO abTestBaseRequestVO, Long build, SourceType sourceType) {
    String res = dryRun(expKey, clientType, abTestBaseRequestVO, build, sourceType);
    if (ExperimentPlatformResultVO.hitBlankGroup(res)) {
      return abTestBaseRequestVO.experimentNameSpace.defaultScene;
    }
    return res;
  }

  public ExperimentPlatformResultVO fetchResultWithGetType(String expKey, ClientType clientType, ABTestBaseRequestVO abTestBaseRequestVO) {
    String res = dryRun(expKey, clientType, abTestBaseRequestVO, ImpliedContextUtils.build(), ImpliedContextUtils.sourceType());
    return ExperimentPlatformResultVO.fromSimple(res);
  }

  public ExperimentPlatformResultVO fetchResult(String expKey, ClientType clientType, ExperimentPlatformClientParam param,
      ABTestSceneType lane) {
    String result = dryRun(expKey, clientType, param);
    return ExperimentPlatformResultVO.fromSimple(result);
  }

  public String fetchExistOrDefaultResult(String expKey, ABTestSceneType sceneType, Map<DiversionKeyType, String> diversionKeyMap) {
    ABTestUserIdRequestVO requestVO = ABTestUserIdRequestVO.from(ABTestUtil.getExperimentNameSpaceFromAbtestSceneType(sceneType),
        Long.parseLong(diversionKeyMap.get(DiversionKeyType.USER_ID)), null);
    String result = dryRun(expKey, ClientType.LAST_RESULT, requestVO, ImpliedContextUtils.build(), ImpliedContextUtils.sourceType());
    if (ExperimentPlatformResultVO.hitBlankGroup(result)) {
      return sceneType.defaultScene;
    }
    return result;
  }

  public String fetchResult(String expKey, ClientType clientType, ABTestSceneType sceneType,
      Map<DiversionKeyType, String> diversionKeyMap) {
    ABTestUserIdRequestVO requestVO = ABTestUserIdRequestVO.from(ABTestUtil.getExperimentNameSpaceFromAbtestSceneType(sceneType),
        Long.parseLong(diversionKeyMap.get(DiversionKeyType.USER_ID)), null);
    String result = dryRun(expKey, clientType, requestVO, ImpliedContextUtils.build(), ImpliedContextUtils.sourceType());
    if (ExperimentPlatformResultVO.hitBlankGroup(result)) {
      return sceneType.defaultScene;
    }
    return result;
  }

  public String fetchResultWithBuildAndSourceType(String expKey, ClientType clientType, ABTestSceneType sceneType,
      Map<DiversionKeyType, String> diversionKeyMap, Long build, SourceType sourceType) {
    ABTestUserIdRequestVO requestVO = ABTestUserIdRequestVO.from(ABTestUtil.getExperimentNameSpaceFromAbtestSceneType(sceneType),
        Long.parseLong(diversionKeyMap.get(DiversionKeyType.USER_ID)), null);
    String result = dryRun(expKey, clientType, requestVO, build, sourceType);
    if (ExperimentPlatformResultVO.hitBlankGroup(result)) {
      return sceneType.defaultScene;
    }
    return result;
  }

  public String fetchResult(ABTestSceneType sceneType, Map<DiversionKeyType, String> diversionKeyMap, String effectiveValue) {
    return effectiveValue;
  }

  public String fetchResultOrDefaultOrNullByUserIdAndABTestSceneType(ABTestSceneType sceneType, Long userId, String effectiveValue) {
    return effectiveValue;
  }

  public String fetchExistOrDefaultResult(ABTestSceneType sceneType, Map<DiversionKeyType, String> diversionKeyMap, String effectiveValue) {
    return effectiveValue;
  }

  private String dryRun(String expKey, ClientType clientType, ABTestBaseRequestVO abTestBaseRequestVO, Long build, SourceType sourceType) {
    try {
      AbstractExpClient client = getClient(clientType);
      ExpUser expUser = ExpUser.builder().userId(abTestBaseRequestVO.userId).deviceToken(abTestBaseRequestVO.deviceToken)
          .nik(abTestBaseRequestVO.nik).versionBuild(build).sourceType(sourceType).build();
      return client.getResult(expKey, expUser);
    } catch (Exception e) {
      log.warn("dry-run-error expKey: {} error!", expKey, e);
      return abTestBaseRequestVO.experimentNameSpace.defaultScene;
    }
  }

  private String dryRun(String expKey, ClientType clientType, ExperimentPlatformClientParam param) {
    try {
      AbstractExpClient client = getClient(clientType);
      ExpUser expUser = ExpUser.builder().userId(param.getUserId()).deviceToken(param.getDeviceToken()).nik(param.getNik())
          .versionBuild(ImpliedContextUtils.build()).sourceType(ImpliedContextUtils.sourceType()).build();
      return client.getResult(expKey, expUser);
    } catch (Exception e) {
      log.warn("dry-run-error expKey: {} error!", expKey, e);
      return "BLANK_GROUP";
    }
  }

  private AbstractExpClient getClient(ClientType clientType) {
    if (Objects.isNull(clientType)) {
      throw new IllegalArgumentException("clientType is null");
    }
    switch (clientType) {
      case DIVERSION:
        return expDiversionClient;
      case PRE_DIVERSION:
        return expPreDiversionClient;
      case LAST_RESULT:
        return expLastResultClient;
      case LAST_RESULT_WITH_PRE_DIVERSION:
        return expLastResultWithPreDiversionClient;
      default:
        throw new IllegalArgumentException("clientType is invalid:" + clientType);
    }
  }

  public enum ClientType {
    DIVERSION,
    PRE_DIVERSION,
    LAST_RESULT,
    LAST_RESULT_WITH_PRE_DIVERSION,
    ;
  }

}
