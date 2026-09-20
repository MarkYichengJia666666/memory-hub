package com.yqg.core.service.risk.riskflow;

import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.sql.risk.RiskFlowV2Model;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.cashloan.risk.monitor.RiskMonitor;
import com.yqg.core.service.cashloan.risk.vo.EventTypeVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.auth.LoanAccountDetailsVO;
import com.yqg.core.service.ojk.OjkConfig;
import com.yqg.core.service.risk.RiskEngineService;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.core.service.risk.riskflow.vo.RiskFlow;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowStandard;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.risk.datasource.RiskKeyConstants;
import com.yqg.risk.orm.sql.tables.records.RiskFlowRecord;
import com.yqg.risk.riskflow.IEventType;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowVO;
import com.yqg.risk.util.HashUtil;
import com.yqg.risk.util.RiskGlobal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author shubo
 * @date 31/10/24 11.19
 */
@Service
@Slf4j
public class RiskFlowV2Service {

  @Autowired
  private RiskFlowV2Model riskFlowV2Model;
  @Autowired
  private RiskConfig riskConfig;
  @Autowired
  private OjkConfig ojkConfig;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;
  @Autowired
  private RiskMonitor riskMonitor;
  @Autowired
  protected RiskEngineService riskEngineService;


  public Long setRiskFlow(BigDecimal percentage, String states, BooleanType enabled, String name, BooleanType isDefault, IEventType eventType, String comment) {
    return setRiskFlow(percentage, states, enabled, name, isDefault, eventType, comment, 0);
  }

  public void updateRiskFlow(Long id, String name, BigDecimal percentage, String states, BooleanType enabled, BooleanType isDefault, IEventType eventType, String comment) {
    updateRiskFlow(id, name, percentage, states, enabled, isDefault, eventType, comment, 0);
  }

  public Long setRiskFlow(BigDecimal percentage, String states, BooleanType enabled, String name, BooleanType isDefault, IEventType eventType, String comment, Integer priority) {
    List<RiskFlowRecord> enableRiskFlows = riskFlowV2Model.findEnabledWithEventTypeOrderByPriorityDesc(eventType);
    BigDecimal riskFlowPercentage = BigDecimal.ZERO;
    RiskFlowRecord enabledDefault = null;
    for (RiskFlowRecord record : enableRiskFlows) {
      riskFlowPercentage = riskFlowPercentage.add(record.getPercentage());
      if (BooleanType.fromCharCode(record.getDefault()) == BooleanType.TRUE) {
        enabledDefault = record;
      }
    }
    if (enabled == BooleanType.TRUE) {
      if (enabledDefault != null && isDefault == BooleanType.TRUE) {
        throw new RuntimeException("已有正在启用的默认自动审核流程");
      }
      if (BigDecimalHelper.compareTo(riskFlowPercentage.add(percentage), BigDecimal.ONE) > 0) {
        throw new RuntimeException("已启用的全部审核流程比例相加大于1");
      }
    }
    return riskFlowV2Model.insert(percentage.setScale(4, BigDecimal.ROUND_HALF_UP), states, enabled, name, isDefault, eventType, comment, priority).getId();
  }

  public void updateRiskFlow(Long id, String name, BigDecimal percentage, String states, BooleanType enabled, BooleanType isDefault, IEventType eventType, String comment, Integer priority) {
    RiskFlowRecord currentRiskFlow = riskFlowV2Model.findById(id);
    List<RiskFlowRecord> enableRiskFlows = riskFlowV2Model.findEnabledWithEventTypeOrderByPriorityDesc(eventType);
    RiskFlowRecord enabledDefault = null;
    BigDecimal riskFlowPercentage = BigDecimal.ZERO;

    for (RiskFlowRecord record : enableRiskFlows) {
      if (BooleanType.fromCharCode(record.getDefault()) == BooleanType.TRUE) {
        enabledDefault = record;
      }
      if (record.getId().equals(id)) {
        if (enabled == BooleanType.FALSE) {
          continue;
        } else {
          riskFlowPercentage = riskFlowPercentage.add(percentage);
        }
      } else {
        riskFlowPercentage = riskFlowPercentage.add(record.getPercentage());
      }
    }

    if (enabledDefault != null && !enabledDefault.getId().equals(id) && isDefault == BooleanType.TRUE && enabled == BooleanType.TRUE) {
      throw new RuntimeException("已有正在启用的默认自动审核流程");
    }
    if (BigDecimalHelper.compareTo(riskFlowPercentage, BigDecimal.ONE) > 0) {
      throw new RuntimeException("已启用的全部审核流程比例相加大于1");
    }
    riskFlowV2Model.update(currentRiskFlow, percentage.setScale(4, BigDecimal.ROUND_HALF_UP), states, enabled, name, isDefault, eventType, comment, priority == null ? 0 : priority);
  }

  public RiskFlowVO getRiskFlow(Long id) {
    return RiskFlowVO.from(riskFlowV2Model.findByIdOrThrow(id));
  }

  public Map<Long, RiskFlowVO> findMapByIds(Collection<Long> ids) {
    if (CollectionUtils.isEmpty(ids)) {
      return new HashMap<>();
    }
    List<RiskFlowRecord> records = riskFlowV2Model.findByIds(ids);
    return records.stream().collect(Collectors.toMap(RiskFlowRecord::getId, RiskFlowVO::from));
  }

  public List<RiskFlowVO> listRiskFlow() {
    List<RiskFlowVO> riskFlowVOs = new ArrayList<>();
    List<RiskFlowRecord> records = riskFlowV2Model.fetchAll();
    riskFlowVOs.addAll(records.stream().map(RiskFlowVO::from).collect(Collectors.toList()));
    return riskFlowVOs;
  }


  public Long pickRiskFlow(EventTypeVO eventTypeVO, LoanAccountVO accountVO, SourceType sourceType) {
    try {
      RiskFlowStandard standard = generateStandard(eventTypeVO);
      RiskFlow riskFlow = pickFromWhitelist(accountVO.id, standard);
      if (riskFlow != null) {
        return riskFlow.id;
      }
      riskFlow = pickFromLocation(eventTypeVO, accountVO.id, standard, sourceType);
      if (riskFlow != null) {
        return riskFlow.id;
      }
      // 印尼要支持随机选取风控流程，使用 hashSuffix 作为随机种子
      return randomPickRiskFlow(riskEngineService.initRiskProps(accountVO, null), standard).id;
    } catch (Exception e) {
      // 仅「未配置启用的 default riskFlow」这类配置错误打点；网络/DB 等系统异常直接抛出，避免误判为配置缺失
      if (RiskFlowStandard.isNoDefaultEnabledRiskFlowError(e)) {
        riskMonitor.logDefaultRiskFlowError(eventTypeVO, accountVO.id);
      }
      throw e;
    }
  }

  public RiskFlow randomPickRiskFlow(Map<String, Object> props, RiskFlowStandard flowStandard) {
    Long hashSuffix = flowStandard.eventType.getHashSuffix();
    // hashSuffix为null时，赋值0，防止NPE，实际并未参与到下房的计算
    Random random = new Random(hashSuffix == null ? 0 : hashSuffix);
    // # 随机生成1-20的一个值
    int randomLength = random.nextInt(20) + 1;
    // # 随机产生序列长度为1-20，字母和数字组成的值 传入random参数，保证每次生成的随机数一致
    String randomOutPut = RandomStringUtils.random(randomLength, 0, 0, true, true, null, random);
    BigDecimal basePercentage = BigDecimal.ZERO;
    String hashKey = buildRandomHashKey(randomOutPut, props, flowStandard);
    return getRiskFlow(props, flowStandard, basePercentage, hashKey);
  }

  private String buildRandomHashKey(String randomOutput, Map<String, Object> props, RiskFlowStandard flowStandard) {
    String result = Stream.of(props.get(RiskKeyConstants.MOBILE), flowStandard.eventType.getId())
        .map(String::valueOf)
        .collect(Collectors.joining("_"));
    StringBuilder sb = new StringBuilder(result);
    return Objects.isNull(flowStandard.eventType.getHashSuffix()) ? result : sb.insert(0, randomOutput + "_").toString();
  }

  private RiskFlow getRiskFlow(Map<String, Object> props, RiskFlowStandard flowStandard, BigDecimal basePercentage, String hashKey) {
    for (RiskFlow riskFlow : flowStandard.riskFlows) {
      if (cutPercentage(hashKey, basePercentage, riskFlow.experimentPercentage)) {
        log.info("userMobile {} hit risk flow id {}", props.get(RiskKeyConstants.MOBILE), riskFlow.id);
        return riskFlow;
      }
      basePercentage = BigDecimalHelper.addWithNullAsZeroAndScale(basePercentage, riskFlow.experimentPercentage);
    }
    return flowStandard.defaultFlow;
  }

  private Boolean cutPercentage(String hashKey, BigDecimal basePercentage, BigDecimal percentage) {
    if ("DEV".equals(RiskGlobal.getStage())) {
      return true;
    }
    return HashUtil.hitConsistentHash(hashKey, basePercentage, percentage);
  }


  private RiskFlow pickFromLocation(EventTypeVO eventTypeVO, Long accountId, RiskFlowStandard standard, SourceType sourceType) {
    if (!ojkConfig.getLocationAllowEventIds().contains(eventTypeVO.eventId)) {
      return null;
    }
    Map<String, Long> locationRiskFlowMap = ojkConfig.getLocationRiskFlowMap();
    if (MapUtils.isEmpty(locationRiskFlowMap)) {
      return null;
    }
    LoanAccountDetailsVO accountDetailsVO = loanAccountDetailsService.getAuthFinishedOrSourceTypeDetailsVoByAccountId(accountId, sourceType);
    if (accountDetailsVO != null
        && accountDetailsVO.detailsPojo != null
        && accountDetailsVO.detailsPojo.cashLoanEmploymentInfo != null
        && accountDetailsVO.detailsPojo.cashLoanEmploymentInfo.environmentInfo != null) {
      Long locationMatchRiskFlowId = getRiskFlowByLocationMap(
          accountDetailsVO.detailsPojo.cashLoanEmploymentInfo.environmentInfo.longiTude,
          accountDetailsVO.detailsPojo.cashLoanEmploymentInfo.environmentInfo.latiTude, locationRiskFlowMap);
      if (locationMatchRiskFlowId != null) {
        return standard.riskFlowWithId.get(locationMatchRiskFlowId);
      }
    }
    return null;
  }


  private Long getRiskFlowByLocationMap(String longitude, String latitude, Map<String, Long> locationRiskFlowMap) {
    if (StringUtils.isAnyBlank(longitude, latitude) || !NumberUtils.isCreatable(longitude) || !NumberUtils.isCreatable(latitude)) {
      return null;
    }
    for (Map.Entry<String, Long> entry : locationRiskFlowMap.entrySet()) {
      String[] locationArr = entry.getKey().split(",");
      String longitudeConf = locationArr[0];
      String latitudeConf = locationArr[1];
      if (longitude.startsWith(longitudeConf) && latitude.startsWith(latitudeConf)) {
        return entry.getValue();
      }
    }
    return null;
  }

  private RiskFlow pickFromWhitelist(Long accountId, RiskFlowStandard standard) {
    Map<Long, Long> accountIdRiskFlowMap = riskConfig.getWhitelistRiskFlowMap();
    return standard.riskFlowWithId.get(accountIdRiskFlowMap.get(accountId));
  }

  public RiskFlowStandard generateStandard(IEventType eventType) {
    List<RiskFlow> riskFlows = findEnabledWithEventType(eventType).stream().map(RiskFlow::from).collect(Collectors.toList());
    return RiskFlowStandard.from(Clock.now(), riskFlows, eventType);
  }


  public List<RiskFlowVO> findEnabledWithEventType(IEventType eventType) {
    List<RiskFlowVO> riskFlowVOs = new ArrayList<>();
    List<RiskFlowRecord> records = riskFlowV2Model.findEnabledWithEventTypeOrderByPriorityDesc(eventType);
    riskFlowVOs.addAll(records.stream().map(RiskFlowVO::from).collect(Collectors.toList()));
    return riskFlowVOs;
  }

  public List<RiskFlowVO> findWithEventType(IEventType eventType) {
    List<RiskFlowVO> riskFlowVOs = new ArrayList<>();
    List<RiskFlowRecord> records = riskFlowV2Model.findWithEventTypeOrderByPriorityDesc(eventType);
    riskFlowVOs.addAll(records.stream().map(RiskFlowVO::from).collect(Collectors.toList()));
    return riskFlowVOs;
  }

  public List<RiskFlowVO> findByEventTypes(Collection<? extends IEventType> eventTypes) {
    if (CollectionUtils.isEmpty(eventTypes)) {
      return new ArrayList<>();
    }
    List<RiskFlowRecord> records = riskFlowV2Model.findByEventTypes(eventTypes);
    return records.stream().map(RiskFlowVO::from).collect(Collectors.toList());
  }

  public List<RiskFlowVO> findEnabledByEventTypes(Collection<? extends IEventType> eventTypes) {
    if (CollectionUtils.isEmpty(eventTypes)) {
      return new ArrayList<>();
    }
    List<RiskFlowRecord> records = riskFlowV2Model.findEnabledByEventTypes(eventTypes);
    return records.stream().map(RiskFlowVO::from).collect(Collectors.toList());
  }

  public List<Long> findEnabledEventIdByEventTypesAndFlowId(Collection<? extends IEventType> eventTypes, Long excludeFlowId) {
    if (CollectionUtils.isEmpty(eventTypes)) {
      return new ArrayList<>();
    }
    return riskFlowV2Model.findEnabledEventIdByEventTypesAndFlowId(eventTypes, excludeFlowId);
  }
}
