package com.yqg.core.service.risk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.serialization.JsonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Data
@ConfigurationProperties(prefix = "risk.riskFlow")
@Component
@Slf4j
public class RiskFlowConfig {

  /**
   * 切时间基线的比例
   */

  private Map<LoanUserRiskType, BigDecimal> baseLineRiskTypePercentageMap = new HashMap<>();

  /**
   * 切new tg比例
   */

  private BigDecimal useNewTgPercentage = BigDecimal.ZERO;

  /**
   * 切新决策引擎的比例
   */
  private Map<Long, BigDecimal> flowIdPercentageMap = new HashMap<>();

  /**
   * 切datasource并行计算的比例
   */
  private Map<LoanUserRiskType, BigDecimal> datasourceParallelRiskTypePercentageMap = new HashMap<>();

  public void setBaseLineRiskTypePercentageMap(String json) {
    if (StringUtils.isBlank(json)) {
      this.baseLineRiskTypePercentageMap = new HashMap<>();
      return;
    }

    this.baseLineRiskTypePercentageMap = JsonUtils.fromOrException(json, new TypeReference<Map<LoanUserRiskType, BigDecimal>>() {
    });
  }

  public void setFlowIdPercentageMap(String json) {
    if (StringUtils.isBlank(json)) {
      this.flowIdPercentageMap = new HashMap<>();
      return;
    }

    this.flowIdPercentageMap = JsonUtils.fromOrException(json, new TypeReference<Map<Long, BigDecimal>>() {
    });
  }

  public void setDatasourceParallelRiskTypePercentageMap(String json) {
    if (StringUtils.isBlank(json)) {
      this.datasourceParallelRiskTypePercentageMap = new HashMap<>();
      return;
    }

    this.datasourceParallelRiskTypePercentageMap = JsonUtils.fromOrException(json, new TypeReference<Map<LoanUserRiskType, BigDecimal>>() {
    });
  }

  /**
   * 不存在决策切换的环境，配置true直接走新决策引擎
   */
  private Boolean useNewRuleEngine = false;
  private Set<Long> remainOldEngineFlowIds = Sets.newHashSet();

  /**
   * ojk环境-拒绝原因展示需过滤的规则id
   */
  private List<Long> excludeOldRuleIdConfig = new ArrayList<>();

  /**
   * ojk审核替换特征和模型名称
   */
  private Map<String, String> modelAndFeatureReplaceConfig = new HashMap<>();

  public void setExcludeOldRuleIdConfig(String excludeOldRuleIdConfig) {
    this.excludeOldRuleIdConfig = StringUtils.isBlank(excludeOldRuleIdConfig) ? new ArrayList<>() : JsonUtils.from(excludeOldRuleIdConfig, new TypeReference<ArrayList>(){});
  }

  private List<String> excludeNewRuleIdConfig = new ArrayList<>();

  public void setExcludeNewRuleIdConfig(String excludeNewRuleIdConfig) {
    this.excludeNewRuleIdConfig = StringUtils.isBlank(excludeNewRuleIdConfig) ? new ArrayList<>() : JsonUtils.from(excludeNewRuleIdConfig, new TypeReference<ArrayList>(){});
  }

  private List<String> replaceNewRuleNameConfig = new ArrayList<>();
  private List<String> replaceModelFeatureNameConfig = new ArrayList<>();

  public void setReplaceNewRuleNameConfig(String replaceNewRuleNameConfig) {
    this.replaceNewRuleNameConfig = StringUtils.isBlank(replaceNewRuleNameConfig) ? new ArrayList<>() : JsonUtils.from(replaceNewRuleNameConfig, new TypeReference<ArrayList>(){});
  }

  public void setReplaceModelFeatureNameConfig(String replaceModelFeatureNameConfig) {
    this.replaceModelFeatureNameConfig = StringUtils.isBlank(replaceModelFeatureNameConfig) ? new ArrayList<>() : JsonUtils.from(replaceModelFeatureNameConfig, new TypeReference<ArrayList>(){});
  }

  public void setModelAndFeatureReplaceConfig(String json) {
    if (StringUtils.isBlank(json)) {
      this.modelAndFeatureReplaceConfig = new HashMap<>();
      return;
    }

    this.modelAndFeatureReplaceConfig = JsonUtils.fromOrException(json, new TypeReference<Map<String, String>>() {
    });
  }
}
