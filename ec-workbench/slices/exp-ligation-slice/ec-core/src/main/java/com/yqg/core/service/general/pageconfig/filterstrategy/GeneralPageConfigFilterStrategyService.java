package com.yqg.core.service.general.pageconfig.filterstrategy;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.googlecode.aviator.AviatorEvaluator;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.GeneralPageConfigFilterStrategyRecord;
import com.yqg.core.model.sql.pageconfig.enums.GeneralPageConfigStatus;
import com.yqg.core.model.sql.pageconfig.filterstrategy.GeneralPageConfigFilterStrategyModel;
import com.yqg.core.service.cashloan.HomepageV5Config;
import com.yqg.core.service.cashloan.enums.HitConsistentHashPrefix;
import com.yqg.core.service.general.pageconfig.filterstrategy.container.FilterStrategyContainer;
import com.yqg.core.service.general.pageconfig.filterstrategy.enums.GeneralPageConfigFilterRuleType;
import com.yqg.core.service.general.pageconfig.filterstrategy.function.GeneralPageConfigFilterRuleFunction;
import com.yqg.core.service.general.pageconfig.filterstrategy.threadlocal.GeneralPageConfigFilterThreadLocal;
import com.yqg.core.service.general.pageconfig.filterstrategy.util.FilterStrategyUtil;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigFilterRuleVO;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigFilterStrategyVO;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigParam;
import com.yqg.core.service.general.pageconfig.filterstrategy.vo.GeneralPageConfigStrategyDiversion;
import com.yqg.core.service.general.pageconfig.vo.GeneralPageConfigFilterStrategyDivisionLogVO;
import com.yqg.core.util.Codec;
import com.yqg.core.util.EcHashUtil;
import com.yqg.core.util.log.DwLogUtil;
import com.yqg.core.util.log.LogBusinessType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GeneralPageConfigFilterStrategyService {
  @Autowired
  private GeneralPageConfigFilterRuleService ruleService;
  @Autowired
  private GeneralPageConfigFilterStrategyModel strategyModel;
  @Autowired
  private GeneralPageConfigFilterRuleFunction ruleFunction;
  @Autowired
  private HomepageV5Config homepageV5Config;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;

  @PostConstruct
  private void init() {
    AviatorEvaluator.getInstance().setCachedExpressionByDefault(true);
    AviatorEvaluator.addFunction(ruleFunction);
  }

  private LoadingCache<Long, GeneralPageConfigFilterStrategyVO> strategyCache = CacheBuilder
      .newBuilder()
      .maximumSize(100L)
      .refreshAfterWrite(1, TimeUnit.DAYS)
      .build(new CacheLoader<Long, GeneralPageConfigFilterStrategyVO>() {
        @Override
        public GeneralPageConfigFilterStrategyVO load(Long id) {
          return fetchByIdOrThrow(id);
        }
      });

  public GeneralPageConfigFilterStrategyVO create(String name,
                                                  String apiStrategy,
                                                  String description,
                                                  GeneralPageConfigStrategyDiversion diversion,
                                                  String operator) {
    if (strategyModel.fetchByName(name) != null) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("相同名称的策略已存在，请使用不同的名称"));
    }

    checkDivisionInfo(diversion);

    //转换成完整的container
    FilterStrategyContainer strategyContainer = FilterStrategyUtil.convertAPIToGeneralContainer(apiStrategy);
    Map<Long, GeneralPageConfigFilterRuleVO> idToRuleVOMap = ruleService.fetchByIds(getRuleIds(strategyContainer));
    checkStrategy(strategyContainer, idToRuleVOMap);
    String strategyMd5 = Codec.md5Encrypt(name + JsonUtils.toString(strategyContainer));
    GeneralPageConfigFilterStrategyRecord record = strategyModel.insert(
        JsonUtils.toString(strategyContainer),
        name,
        JsonUtils.toString(diversion),
        description,
        operator,
        strategyMd5);
    return GeneralPageConfigFilterStrategyVO.from(record);
  }

  public GeneralPageConfigFilterStrategyVO fetchByIdOrThrow(Long id) {
    GeneralPageConfigFilterStrategyRecord record = strategyModel.fetchByIdOrThrow(id);
    return GeneralPageConfigFilterStrategyVO.from(record);
  }

  public List<GeneralPageConfigFilterStrategyVO> fetchByIdsOrThrow(Collection<Long> ids) {
    List<GeneralPageConfigFilterStrategyRecord> records = strategyModel.fetchByIds(ids);
    if (records.size() != ids.size()) {
      throw EcException.error("id不完全存在表中！,ids:{}", ids);
    }
    return records
        .stream()
        .map(GeneralPageConfigFilterStrategyVO::from)
        .collect(Collectors.toList());
  }

  public List<GeneralPageConfigFilterStrategyVO> fetchEnabledStrategy(Integer offset, Integer limit) {
    return strategyModel.fetch(offset, limit, GeneralPageConfigStatus.ENABLED)
        .stream()
        .map(GeneralPageConfigFilterStrategyVO::from)
        .collect(Collectors.toList());
  }

  public Integer count() {
    return strategyModel.count();
  }

  public List<GeneralPageConfigFilterStrategyVO> fetchByCondition(Long id, String name, String operator, GeneralPageConfigStatus filterStatus, Integer offset, Integer limit) {
    return strategyModel.fetchByCondition(id, name, operator, filterStatus, offset, limit)
        .stream()
        .map(GeneralPageConfigFilterStrategyVO::from)
        .collect(Collectors.toList());
  }

  public Integer countByCondition(Long id, String name, String operator, GeneralPageConfigStatus filterStatus) {
    return strategyModel.countByCondition(id, name, operator, filterStatus);
  }

  public List<GeneralPageConfigFilterStrategyVO> fetchAllEnabledForExperimentPlatform(Integer offset, Integer limit) {
    List<GeneralPageConfigFilterStrategyVO> strategyVOList = strategyModel.fetch(offset, limit, GeneralPageConfigStatus.ENABLED)
        .stream()
        .map(GeneralPageConfigFilterStrategyVO::from)
        .collect(Collectors.toList());

    Map<Long, GeneralPageConfigFilterRuleVO> idToRuleVOMap = ruleService.fetchIdToVOMapForExperimentPlatform();

    return strategyVOList
        .stream()
        .filter(o -> canStrategyUsedForExperimentPlatform(o, idToRuleVOMap))
        .collect(Collectors.toList());

  }

  //策略能否被实验中台使用
  public boolean canStrategyUsedForExperimentPlatform(GeneralPageConfigFilterStrategyVO strategyVO, Map<Long, GeneralPageConfigFilterRuleVO> idToRuleVOMap) {
    FilterStrategyContainer container = FilterStrategyUtil.convertStringToGeneralContainer(strategyVO.strategy);
    List<Long> ruleIds = getRuleIds(container);
    for (Long ruleId : ruleIds) {
      GeneralPageConfigFilterRuleVO ruleVO = idToRuleVOMap.get(ruleId);
      // 测试环境脏数据太多，这里打个log
      if (ruleVO == null) {
        log.error("can not find rule id:{}, strategy id:{}", ruleId, strategyVO.id);
        return false;
      }
      if (GeneralPageConfigFilterRuleType.NO_USED_BY_EXPERIMENT_PLATFORM.contains(ruleVO.type)) {
        return false;
      }
    }
    return true;
  }

  private List<Long> getRuleIds(FilterStrategyContainer container) {
    switch (container.type) {
      case RULE:
        Long id = FilterStrategyContainer.getRuleValue(container.value);
        return Collections.singletonList(id);
      case OPERATOR:
        FilterStrategyContainer.getOperatorValue(container.value);
        return new ArrayList<>();
      case RULE_GROUP:
        List<FilterStrategyContainer> containerList = FilterStrategyContainer.getRuleGroupValue(container.value);
        List<Long> ruleIds = new ArrayList<>();
        containerList.forEach(strategyContainer -> ruleIds.addAll(getRuleIds(strategyContainer)));
        return ruleIds;
      default:
        throw EcException.error("unsupported type :{}", container.type);
    }
  }

  public static void checkStrategy(FilterStrategyContainer container, Map<Long, GeneralPageConfigFilterRuleVO> idToRuleVOMap) {
    switch (container.type) {
      case RULE:
        Long id = FilterStrategyContainer.getRuleValue(container.value);
        GeneralPageConfigFilterRuleVO ruleVO = idToRuleVOMap.get(id);
        if (ruleVO == null) {
          throw EcException.error("rule not exist for id {}", container.value);
        }
        if (!ruleVO.name.equals(container.name)) {
          throw EcException.error("rule name is {} but container name in request is {}", ruleVO.name, container.name);
        }
        break;
      case OPERATOR:
        FilterStrategyContainer.getOperatorValue(container.value);
        break;
      case RULE_GROUP:
        List<FilterStrategyContainer> containerList = FilterStrategyContainer.getRuleGroupValue(container.value);
        containerList.forEach(strategyContainer -> checkStrategy(strategyContainer, idToRuleVOMap));
        break;
      default:
        throw EcException.error("unsupported type :{}", container.type);
    }
  }

  private void checkDivisionInfo(GeneralPageConfigStrategyDiversion diversion) {
    if (diversion == null || diversion.experimentalGroupPercent == null) {
      return;
    }
    if (diversion.experimentalGroupPercent.compareTo(BigDecimal.ONE) > 0 || diversion.experimentalGroupPercent.compareTo(BigDecimal.ZERO) <= 0) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("分流比例应该大于0小于等于1"));
    }
  }

  private GeneralPageConfigFilterStrategyVO getStrategyVOFromCache(Long id) {
    if (homepageV5Config.getGenneralRuleByDb()) {
      return fetchByIdOrThrow(id);
    }
    try {
      return strategyCache.get(id);
    } catch (Exception e) {
      log.error("exception when get GeneralPageConfigFilterStrategyVO from cache for id={}", id, e);
      return null;
    }
  }

  /**
   * 使用此方法必须考虑清除GeneralPageConfigFilterThreadLocal
   *
   * @param strategyId
   * @param param
   * @param strategyIdToResultMap
   * @return
   */
  public boolean hitStrategy(Long strategyId, GeneralPageConfigParam param, Map<Long, Boolean> strategyIdToResultMap) {
    try {
      GeneralPageConfigFilterStrategyVO strategyVO = getStrategyVOFromCache(strategyId);
      if (strategyVO == null) {
        return false;
      }
      GeneralPageConfigFilterThreadLocal.setParam(param, strategyIdToResultMap);
      FilterStrategyContainer strategyContainer = FilterStrategyUtil.convertStringToGeneralContainer(strategyVO.strategy);
      String expression = FilterStrategyUtil.convertContainerToExpression(strategyContainer);
      boolean tempResult = Boolean.parseBoolean(AviatorEvaluator.execute(expression, null, true).toString());

      //若未配置分流，返回结果
      if (strategyVO.diversion == null || strategyVO.diversion.experimentalGroupPercent == null) {
        return tempResult;
      }
      //暂时只支持userId分流
      if (param.userId == null) {
        return tempResult;
      }
      //若未能命中策略，直接返回结果
      if (!tempResult) {
        return false;
      }

      //命中策略，根据分流返回最终结果，并打点
      Integer hashId = strategyVO.diversion.hashId;
      String subKey;
      if (hashId != null) {
        subKey = hashId.toString() + param.userId;
      } else {
        subKey = strategyId.toString() + param.userId;
      }
      boolean finalResult = EcHashUtil.hitConsistentHashWithMurmurHash(HitConsistentHashPrefix.GENERAL_PAGE_CONFIG_FILTER_STRATEGY_DIVISION, subKey, strategyVO.diversion.experimentalGroupPercent);
      //generalPageConfigId不为空，说明是真实的首页配置资源位请求访问，需要对可见性打点
      DwLogUtil.log(LogBusinessType.GENERAL_PAGE_CONFIG_FILTER_STRATEGY_DIVISION_LOG,
          GeneralPageConfigFilterStrategyDivisionLogVO.from(
              finalResult,
              param.userId,
              strategyId,
              param.idnHomepageLoanStatusV5,
              param.platform,
              param.build,
              param.sdkType));
      return finalResult;
    } catch (Exception e) {
      log.info("exception when hitStrategy for strategyId={}", strategyId, e);
      return false;
    }

  }

  public void updateFilterStatus(Long id, GeneralPageConfigStatus filterStatus, String operator) {
    GeneralPageConfigFilterStrategyRecord record = strategyModel.fetchByIdOrThrow(id);
    if (StringUtils.equals(record.getFilterStatus(), filterStatus.code)) {
      log.warn("Strategy - filter status is already {}, no need to update", filterStatus);
      return;
    }
    threadTransactionalModel.transaction(configuration -> strategyModel.update(filterStatus, operator, record));
  }
}
