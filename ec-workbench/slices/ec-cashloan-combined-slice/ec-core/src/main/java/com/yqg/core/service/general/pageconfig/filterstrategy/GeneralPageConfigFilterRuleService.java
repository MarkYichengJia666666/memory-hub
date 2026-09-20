package com.yqg.core.service.general.pageconfig.filterstrategy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.GeneralPageConfigFilterRuleRecord;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigStatus;
import com.yqg.core.model.sql.pageconfig.filterstrategy.GeneralPageConfigFilterRuleModel;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.GeneralPageConfigFilterRuleType;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.ObtainRulePayloadType;
import com.yqg.core.service.general.pageconfig.filterstrategy.processor.GeneralPageConfigBaseFilterRuleProcessor;
import com.yqg.core.service.general.pageconfig.filterstrategy.processor.GeneralPageConfigFilterRuleProcessorFactory;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigFilterRuleVO;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.payload.BaseFilterRulePayload;
import com.yqg.core.util.Codec;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GeneralPageConfigFilterRuleService {

  @Autowired
  private GeneralPageConfigFilterRuleModel generalPageConfigFilterRuleModel;
  @Autowired
  private GeneralPageConfigFilterRuleProcessorFactory processorFactory;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;

  private LoadingCache<Long, GeneralPageConfigFilterRuleVO> ruleCache = CacheBuilder
      .newBuilder()
      .maximumSize(100L)
      .refreshAfterWrite(1, TimeUnit.DAYS)
      .build(new CacheLoader<Long, GeneralPageConfigFilterRuleVO>() {
        @Override
        public GeneralPageConfigFilterRuleVO load(Long id) {
          return fetchByIdOrThrow(id);
        }
      });


  public Class<? extends BaseFilterRulePayload> getPayloadClass(GeneralPageConfigFilterRuleType type) {
    return processorFactory.getProcessor(type).getPayloadClass();
  }

  public GeneralPageConfigFilterRuleVO create(String name,
                                              GeneralPageConfigFilterRuleType type,
                                              String payload,
                                              String description,
                                              String operator) {
    if (generalPageConfigFilterRuleModel.fetchByName(name) != null) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("相同名称的规则已存在，请使用不同的名称"));
    }
    GeneralPageConfigBaseFilterRuleProcessor processor = processorFactory.getProcessor(type);
    BaseFilterRulePayload rulePayload = processor.getPayload(payload, type, ObtainRulePayloadType.STORE_RULE);
    processor.checkPayload(rulePayload);
    String ruleMd5 = Codec.md5Encrypt(name + type.name() + payload);
    GeneralPageConfigFilterRuleRecord record = generalPageConfigFilterRuleModel.insert(name, type, JsonUtils.toString(rulePayload), description, operator, ruleMd5);
    return GeneralPageConfigFilterRuleVO.from(record);
  }

  public List<GeneralPageConfigFilterRuleVO> fetchByCondition(
      List<GeneralPageConfigFilterRuleType> types,
      String name,
      String operator,
      GeneralPageConfigStatus filterStatus,
      Integer offset,
      Integer limit
  ) {
    return generalPageConfigFilterRuleModel.fetchByCondition(types, name, operator, filterStatus, offset, limit)
        .stream()
        .map(GeneralPageConfigFilterRuleVO::fromWithoutException)
        .filter(t -> t.type != null)
        .collect(Collectors.toList());
  }

  public Integer countByCondition(List<GeneralPageConfigFilterRuleType> types, String name, String operator, GeneralPageConfigStatus filterStatus) {
    return generalPageConfigFilterRuleModel.countByCondition(types, name, operator, filterStatus);
  }

  public Map<Long, GeneralPageConfigFilterRuleVO> fetchIdToVOMapForExperimentPlatform() {
    return generalPageConfigFilterRuleModel.fetchAll().stream()
        .map(GeneralPageConfigFilterRuleVO::fromWithoutException)
        .filter(item -> item.type != null)
        .collect(Collectors.toMap(o -> o.id, o -> o));
  }

  public List<GeneralPageConfigFilterRuleVO> fetchByNameAndEnabled(String name) {
    return generalPageConfigFilterRuleModel.fetchListByNameAndFilterStatus(name, GeneralPageConfigStatus.ENABLED)
        .stream()
        .map(GeneralPageConfigFilterRuleVO::from)
        .collect(Collectors.toList());
  }

  public List<GeneralPageConfigFilterRuleVO> fetchByNameAndEnabledForAdmin(String name) {
    return generalPageConfigFilterRuleModel.fetchListByNameAndFilterStatus(name, GeneralPageConfigStatus.ENABLED)
        .stream()
        .map(GeneralPageConfigFilterRuleVO::fromWithoutException)
        .filter(item -> item != null && item.type != null)
        .collect(Collectors.toList());
  }

  public GeneralPageConfigFilterRuleVO fetchByIdOrThrow(Long id) {
    GeneralPageConfigFilterRuleRecord record = generalPageConfigFilterRuleModel.fetchByIdOrThrow(id);
    return GeneralPageConfigFilterRuleVO.from(record);
  }

  public GeneralPageConfigFilterRuleVO fetchRuleVOFromCache(Long id) {
    if (homepageV5Config.getGenneralRuleByDb()) {
      return fetchByIdOrThrow(id);
    }
    try {
      return ruleCache.get(id);
    } catch (Exception e) {
      log.error("exception when get GeneralPageConfigFilterRuleVO from cache for id={}", id, e);
      return null;
    }
  }

  public boolean hitRule(Long id, GeneralPageConfigParam param) {
    GeneralPageConfigFilterRuleVO ruleVO = fetchRuleVOFromCache(id);
    Long userId = param == null ? null : param.userId;
    if (ruleVO == null) {
      log.debug("FilterRule miss, ruleId={}, reason=RULE_NOT_FOUND, userId={}", id, userId);
      return false;
    }
    GeneralPageConfigBaseFilterRuleProcessor processor = processorFactory.getProcessor(ruleVO.type);
    BaseFilterRulePayload rulePayload = processor.getPayload(ruleVO.payload, ruleVO.type, ObtainRulePayloadType.USE_RULE);
    boolean hit = processor.hitRule(rulePayload, param);
    log.debug("FilterRule evaluated, ruleId={}, type={}, userId={}, hit={}", id, ruleVO.type, userId, hit);
    return hit;
  }

  public void updateFilterStatus(Long id, GeneralPageConfigStatus filterStatus, String operator) {
    GeneralPageConfigFilterRuleRecord record = generalPageConfigFilterRuleModel.fetchByIdOrThrow(id);
    if (StringUtils.equals(record.getFilterStatus(), filterStatus.code)) {
      log.info("Rule - filter status is already {}, no need to update", filterStatus);
      return;
    }
    threadTransactionalModel.transaction(configuration -> generalPageConfigFilterRuleModel.update(filterStatus, operator, record));
  }

  public Map<Long, GeneralPageConfigFilterRuleVO> fetchByIds(List<Long> ruleIds) {
    return generalPageConfigFilterRuleModel.fetchByIds(ruleIds)
        .stream()
        .map(GeneralPageConfigFilterRuleVO::from)
        .collect(Collectors.toMap(o -> o.id, o -> o));
  }
}
