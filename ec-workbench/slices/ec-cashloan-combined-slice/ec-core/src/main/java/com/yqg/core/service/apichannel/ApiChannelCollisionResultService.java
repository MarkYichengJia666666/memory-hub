package com.yqg.core.service.apichannel;

import com.yqg.core.model.generated.tables.records.ApiChannelCheckMobileResultRecord;
import com.yqg.core.model.sql.apichannel.ApiChannelCheckMobileResultModel;
import com.yqg.core.service.apichannel.vo.CollisionResultVO;
import com.yqg.core.util.Codec;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * API渠道撞库结果服务
 *
 */
@Service
@Slf4j
public class ApiChannelCollisionResultService {
  @Autowired
  private ApiChannelCheckMobileResultModel apiChannelCheckMobileResultModel;

  /**
   * 查询手机号是否撞库通过
   *
   * @param normalizedMobileNumbers +62格式的手机号列表
   * @return 手机号到撞库结果列表的映射
   */
  public Map<String, List<CollisionResultVO>> checkCollisionResult(List<String> normalizedMobileNumbers) {
    if (CollectionUtils.isEmpty(normalizedMobileNumbers)) {
      return Collections.emptyMap();
    }

    // 将+62格式的手机号转换为MD5
    Map<String, String> normalizedToMd5Map = new HashMap<>();
    Set<String> mobileMd5s = new HashSet<>();

    for (String normalizedMobileNumber : normalizedMobileNumbers) {
      if (StringUtils.isBlank(normalizedMobileNumber)) {
        continue;
      }
      try {
        // 将+62格式转换为national格式（例如：+620812345678 -> 0812345678）
        String nationalMobile = MobileConverter.normalizedToNationalOrThrow(
            SDKType.IDN_YQD.getLocale(), normalizedMobileNumber);
        // 计算MD5
        String mobileMd5 = Codec.md5Encrypt(nationalMobile);
        normalizedToMd5Map.put(normalizedMobileNumber, mobileMd5);
        mobileMd5s.add(mobileMd5);
      } catch (Exception e) {
        log.warn("Failed to convert mobile number to MD5, normalizedMobileNumber: {}", normalizedMobileNumber, e);
      }
    }

    if (mobileMd5s.isEmpty()) {
      return Collections.emptyMap();
    }

    // 批量查询撞库结果
    Map<String, List<ApiChannelCheckMobileResultRecord>> md5ToRecordsMap =
        apiChannelCheckMobileResultModel.findMapByMobileNumberMd5s(mobileMd5s);

    // 构建响应：将MD5映射回normalized手机号
    Map<String, List<CollisionResultVO>> normalizedToResultsMap = new HashMap<>();

    for (Map.Entry<String, String> entry : normalizedToMd5Map.entrySet()) {
      String normalizedMobileNumber = entry.getKey();
      String mobileMd5 = entry.getValue();
      List<ApiChannelCheckMobileResultRecord> records = md5ToRecordsMap.get(mobileMd5);

      if (CollectionUtils.isEmpty(records)) {
        // 如果没有撞库结果，返回空列表
        normalizedToResultsMap.put(normalizedMobileNumber, Collections.emptyList());
      } else {
        // 转换为VO对象
        List<CollisionResultVO> resultVOs = records.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        normalizedToResultsMap.put(normalizedMobileNumber, resultVOs);
      }
    }

    // 处理没有转换成功的手机号（返回空列表）
    for (String normalizedMobileNumber : normalizedMobileNumbers) {
      if (StringUtils.isNotBlank(normalizedMobileNumber) && !normalizedToResultsMap.containsKey(normalizedMobileNumber)) {
        normalizedToResultsMap.put(normalizedMobileNumber, Collections.emptyList());
      }
    }

    return normalizedToResultsMap;
  }

  /**
   * 将数据库记录转换为VO对象
   *
   * @param record 数据库记录
   * @return VO对象
   */
  private CollisionResultVO convertToVO(ApiChannelCheckMobileResultRecord record) {
    return CollisionResultVO.builder()
        .id(record.getId())
        .mobileNumberMd5(record.getMobileNumberMd5())
        .apiChannel(record.getApiChannel())
        .timeCreated(record.getTimeCreated())
        .reason(record.getReason())
        .lossDays(record.getLossDays())
        .build();
  }
}
