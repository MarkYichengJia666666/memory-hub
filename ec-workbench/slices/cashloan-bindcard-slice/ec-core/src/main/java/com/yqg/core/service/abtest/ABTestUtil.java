package com.yqg.core.service.abtest;

import com.google.common.collect.Sets;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.abtest.enums.DiversionKeyType;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.general.appconfig.vo.GeneralAppConfigParamVO;
import com.yqg.core.util.Codec;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.ec.common.utils.EcAsserts;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author yuchenghuang
 * @date 2022/7/1
 */
public class ABTestUtil {
  public static final String APP_CONFIG_PRE_FIX = "app_config.";
  private static final String AB_TEST_LAYER_RESERVE_TRAFFIC_SUFFIX = "reserve";
  private static final String LAYER_BLANK_TRAFFIC_KEY = "available_traffic";
  public static final String AB_TEST_EXPERIMENT_BLANK_GROUP_NAME = "BLANK_GROUP";

  public static Set<DiversionKeyType> getSupportDiversionKeyTypesBySceneTypes(Collection<ABTestSceneType> sceneTypes) {
    Set<DiversionKeyType> result = Sets.newHashSet();
    List<Set<DiversionKeyType>> diversionKeyTypes = sceneTypes
        .stream()
        .map(o -> o.supportKeyTypes)
        .collect(Collectors.toList());
    for (int i = 0; i < diversionKeyTypes.size(); i++) {
      if (i == 0) {
        result.addAll(diversionKeyTypes.get(i));
        continue;
      }
      result.retainAll(diversionKeyTypes.get(i));
    }
    return result;
  }

  public static ABTestSceneType getABTestSceneFromAppConfigKey(String key) {
    String scene = key.substring(APP_CONFIG_PRE_FIX.length()).toUpperCase();
    try {
      return ABTestSceneType.valueOf(scene);

    } catch (Exception e) {
      throw EcException.error("can not convert app_config to abtest scene, config : {}", key, e);
    }
  }

  public static ExperimentNameSpace getExperimentNameSpaceFromAppConfigKey(String key) {
    String scene = key.substring(APP_CONFIG_PRE_FIX.length()).toUpperCase();
    try {
      return ExperimentNameSpace.valueOf(scene);
    } catch (Exception e) {
      throw EcException.error("can not convert app_config to experiment name space, config : {}", key, e);
    }
  }

  public static Map<DiversionKeyType, String> buildDiversionKeyMapFromAppConfig(GeneralAppConfigParamVO paramVO, Set<DiversionKeyType> diversionKeyTypeSet) {
    return diversionKeyTypeSet.stream().collect(Collectors.toMap(o -> o, o -> getDiversionKeyFromAppConfig(paramVO, o)));
  }

  private static String getDiversionKeyFromAppConfig(GeneralAppConfigParamVO paramVO, DiversionKeyType diversionKeyType) {
    switch (diversionKeyType) {
      case USER_ID:
        return paramVO.userId == null ? "" : paramVO.userId.toString();
      case DEVICE_TOKEN:
        return paramVO.deviceToken;
      default:
        throw EcException.error("unsupported diversion key type: {}, paramVO: {}", diversionKeyType, JsonUtils.toString(paramVO));
    }
  }

  public static Map<DiversionKeyType, String> genDiversionKeyMapByUserId(Long userId) {
    Map<DiversionKeyType, String> diversionKeyMap = new HashMap<>();
    diversionKeyMap.put(DiversionKeyType.USER_ID, userId.toString());
    return diversionKeyMap;
  }

  public static Map<DiversionKeyType, String> genDiversionKeyMapByDeviceToken(String deviceToken) {
    Map<DiversionKeyType, String> diversionKeyMap = new HashMap<>();
    diversionKeyMap.put(DiversionKeyType.DEVICE_TOKEN, deviceToken);
    return diversionKeyMap;
  }

  public static String getExperimentReserveTrafficKey(Long experimentId) {
    return experimentId + AB_TEST_LAYER_RESERVE_TRAFFIC_SUFFIX;
  }

  public static String getExperimentUsedTrafficKey(Long experimentId) {
    return String.valueOf(experimentId);
  }

  public static String getLayerBlankTrafficKey() {
    return LAYER_BLANK_TRAFFIC_KEY;
  }

  public static boolean isUsedTrafficKey(String key) {
    if (StringUtils.equals(key, getLayerBlankTrafficKey())) {
      return false;
    }
    if (StringUtils.contains(key, AB_TEST_LAYER_RESERVE_TRAFFIC_SUFFIX)) {
      return false;
    }
    return true;
  }

  public static BigDecimal calcLayerAvailableTraffic(Map<String, ABTestTrafficConfigInfo> trafficConfigMap) {
    return trafficConfigMap.get(getLayerBlankTrafficKey()).percent;
  }

  private static TrafficBucket getAvailableTrafficBucket(Map<String, ABTestTrafficConfigInfo> configMap) {
    ABTestTrafficConfigInfo abTestTrafficConfigInfo = configMap.getOrDefault(getLayerBlankTrafficKey(), null);
    if (abTestTrafficConfigInfo == null || StringUtils.isBlank(abTestTrafficConfigInfo.bucketInfo)) {
      TrafficBucket trafficBucket = new TrafficBucket();
      trafficBucket.set(0, TrafficBucket.MAX_SIZE);
      return trafficBucket;
    }
    byte[] bytes = Codec.deserializeAndDecompress(configMap.get(getLayerBlankTrafficKey()).bucketInfo);
    return TrafficBucket.valueOf(bytes);
  }

  public static Map<String, ABTestTrafficConfigInfo> expandExperimentTraffic(
      Map<String, ABTestTrafficConfigInfo> configMap,
      Long layerId,
      Long experimentId,
      BigDecimal targetUsedTraffic,
      BigDecimal targetReservedTraffic
  ) {
    String usedTrafficKey = getExperimentUsedTrafficKey(experimentId);
    if (configMap.containsKey(usedTrafficKey)) {
      return expandExistExperimentTraffic(configMap, layerId, experimentId, targetUsedTraffic, targetReservedTraffic);
    }
    return initExperimentTraffic(configMap, layerId, experimentId, targetUsedTraffic, targetReservedTraffic);
  }

  @NotNull
  private static Map<String, ABTestTrafficConfigInfo> initExperimentTraffic(Map<String, ABTestTrafficConfigInfo> configMap, Long layerId, Long experimentId, BigDecimal usedTraffic, BigDecimal reservedTraffic) {
    try {
      TrafficBucket reservedBucket = new TrafficBucket();
      TrafficBucket usedBucket = new TrafficBucket();
      if (!configMap.containsKey(getLayerBlankTrafficKey())) {
        configMap.put(getLayerBlankTrafficKey(), ABTestTrafficConfigInfo.from(BigDecimal.ONE, null));
      }
      ABTestTrafficConfigInfo availableTrafficConfig = configMap.get(getLayerBlankTrafficKey());
      TrafficBucket availableBucket = getAvailableTrafficBucket(configMap);
      BigDecimal targetOccupiedTraffic = usedTraffic.add(reservedTraffic);
      EcAsserts.assertTrue(BigDecimalHelper.compareTo(availableTrafficConfig.percent, targetOccupiedTraffic) >= 0,
          "current available traffic must be greater or equal than target occupied traffic, experimentId: {}", experimentId);
      BigDecimal targetAvailableTraffic = availableTrafficConfig.percent.subtract(targetOccupiedTraffic);
      return doExpandExperiment(
          configMap,
          layerId,
          experimentId,
          usedTraffic,
          reservedTraffic,
          reservedBucket,
          usedBucket,
          availableBucket,
          targetAvailableTraffic,
          BigDecimal.ZERO,
          BigDecimal.ZERO
      );
    } catch (Exception e) {
      throw EcException.error("init experiment traffic failed, experimentId: {}", experimentId, e);
    }
  }

  @NotNull
  private static Map<String, ABTestTrafficConfigInfo> doExpandExperiment(
      Map<String, ABTestTrafficConfigInfo> configMap,
      Long layerId,
      Long experimentId,
      BigDecimal targetUsedTraffic,
      BigDecimal targetReservedTraffic,
      TrafficBucket reservedBucket,
      TrafficBucket usedBucket,
      TrafficBucket availableBucket,
      BigDecimal targetAvailableTraffic,
      BigDecimal currentUsedTraffic,
      BigDecimal currentReservedTraffic) {
    String usedTrafficKey = getExperimentUsedTrafficKey(experimentId);
    String reserveTrafficKey = getExperimentReserveTrafficKey(experimentId);
    int moveUseBucketNum = getExpandGroupBucketNum(currentUsedTraffic, targetUsedTraffic);
    int moveReserveBucketNum = getExpandGroupBucketNum(currentReservedTraffic, targetReservedTraffic);
    moveFromAvailableToTargetBucket(availableBucket, reservedBucket, moveReserveBucketNum, layerId);
    moveFromAvailableToTargetBucket(availableBucket, usedBucket, moveUseBucketNum, layerId);

    configMap.put(usedTrafficKey, ABTestTrafficConfigInfo.from(targetUsedTraffic, Codec.compressAndSerialize(usedBucket.toByteArray())));
    configMap.put(reserveTrafficKey, ABTestTrafficConfigInfo.from(targetReservedTraffic, Codec.compressAndSerialize(reservedBucket.toByteArray())));
    configMap.put(getLayerBlankTrafficKey(), ABTestTrafficConfigInfo.from(targetAvailableTraffic, Codec.compressAndSerialize(availableBucket.toByteArray())));
    return configMap;
  }

  private static Map<String, ABTestTrafficConfigInfo> expandExistExperimentTraffic(
      Map<String, ABTestTrafficConfigInfo> configMap,
      Long layerId,
      Long experimentId,
      BigDecimal targetUsedTraffic,
      BigDecimal targetReservedTraffic
  ) {
    String usedTrafficKey = getExperimentUsedTrafficKey(experimentId);
    String reserveTrafficKey = getExperimentReserveTrafficKey(experimentId);
    ABTestTrafficConfigInfo usedTrafficConfig = configMap.get(usedTrafficKey);
    ABTestTrafficConfigInfo reserveTrafficConfig = configMap.get(reserveTrafficKey);
    ABTestTrafficConfigInfo availableTrafficConfig = configMap.get(getLayerBlankTrafficKey());
    BigDecimal targetOccupiedTraffic = targetUsedTraffic.add(targetReservedTraffic);
    BigDecimal currentOccupiedTraffic = usedTrafficConfig.percent.add(reserveTrafficConfig.percent);
    BigDecimal moveTraffic = targetOccupiedTraffic.subtract(currentOccupiedTraffic);
    BigDecimal targetAvailableTraffic = availableTrafficConfig.percent.subtract(moveTraffic);

    EcAsserts.assertTrue(BigDecimalHelper.greaterThan(targetReservedTraffic, reserveTrafficConfig.percent),
        "reserved traffic must be greater than current reserved traffic, experimentId: {}", experimentId);
    EcAsserts.assertTrue(BigDecimalHelper.greaterThan(targetUsedTraffic, usedTrafficConfig.percent),
        "used traffic must be greater than current used traffic, experimentId: {}", experimentId);
    EcAsserts.assertTrue(BigDecimalHelper.greaterThan(availableTrafficConfig.percent, targetAvailableTraffic),
        "current available traffic must be greater than target available traffic, experimentId: {}", experimentId);

    try {
      TrafficBucket reservedBucket = TrafficBucket.valueOf(Codec.deserializeAndDecompress(reserveTrafficConfig.bucketInfo));
      TrafficBucket usedBucket = TrafficBucket.valueOf(Codec.deserializeAndDecompress(usedTrafficConfig.bucketInfo));
      TrafficBucket availableBucket = getAvailableTrafficBucket(configMap);

      return doExpandExperiment(
          configMap,
          layerId,
          experimentId,
          targetUsedTraffic,
          targetReservedTraffic,
          reservedBucket,
          usedBucket,
          availableBucket,
          targetAvailableTraffic,
          usedTrafficConfig.percent,
          reserveTrafficConfig.percent
      );
    } catch (Exception e) {
      throw EcException.error("expand group traffic failed, experimentId: {}", experimentId, e);
    }
  }

  private static void moveFromAvailableToTargetBucket(TrafficBucket availableBucket, TrafficBucket targetBucket, int bucketNum, Long layerId) {
    for (int i = availableBucket.nextSetBit(TrafficBucket.MIN_INDEX); i >= 0; i = availableBucket.nextSetBit(i + 1)) {
      if (bucketNum <= 0) {
        break;
      }
      EcAsserts.assertTrue(!targetBucket.get(i), "bucket already used, layer id: {}, bucket: {}", layerId, i);
      availableBucket.clear(i);
      targetBucket.set(i);
      bucketNum--;
    }
  }

  private static int getExpandGroupBucketNum(BigDecimal currenTraffic, BigDecimal targetTraffic) {
    BigDecimal diff = targetTraffic.subtract(currenTraffic);
    return diff.multiply(BigDecimal.valueOf(TrafficBucket.MAX_SIZE)).intValue();
  }

  public static boolean blankScene(String result) {
    return StringUtils.equals(result, ABTestSceneType.BLANK_SCENE);
  }

  public static ExperimentNameSpace getExperimentNameSpaceFromAbtestSceneType(ABTestSceneType sceneType) {
    return ExperimentNameSpace.valueOf(sceneType.name());
  }
}
