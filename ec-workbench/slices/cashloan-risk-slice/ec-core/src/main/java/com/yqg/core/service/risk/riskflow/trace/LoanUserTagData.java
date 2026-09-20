package com.yqg.core.service.risk.riskflow.trace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.yqg.ec.common.serialization.JsonUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by ember on 2017/3/24.
 */
public class LoanUserTagData {
  public LoanUserOperationType type;
  public String userType;
  public BigDecimal credits;
  public Boolean rejectOrder;
  public String operationLevel;
  public Double creditsCoefficient;
  public BigDecimal creditsDelta;
  public BigDecimal maxCredits;
  public BigDecimal minCredits;
  public Boolean rejectMultiApply;
  public Boolean rejectMultiOrder;
  public Integer rejectDays;
  public Integer retrievalValidDays;
  public String tag;
  public BigDecimal tempCredits;
  public Integer tempCreditsExpiredDays;
  public Integer rejectMultiLoanDays;
  public Object customTags;
  public String productTag;
  public String preRateTag;
  public String rateTag;
  public BigDecimal riskTempCreditsA;
  public BigDecimal riskTempCreditsB;
  public String multiLoanReductionInterestTag;
  public BigDecimal operationLevelScore;
  //用户是否需要管制同 NIK 账号
  public Boolean relatedUserNeedReject;
  //首贷用户管制天数
  public Integer relatedUserControlDays;
  //复贷用户管制天数
  public Integer reloanRelatedUserControlDays;
  public Boolean needSupplement;
  public Boolean twoLivingFaceComparePass;
  @JsonProperty("whatsapp_read_model_v1")
  public Float whatsappReadModelV1;
  //循环贷标签
  public Boolean revolvingLoan;
  public Long revolvingControlDays;//循环贷用户被管制，一定会拒绝订单
  @JsonProperty("af_rank")
  public String afRank;
  @JsonProperty("lackLimitTest")
  public String lackLimitTest;
  @JsonProperty("black_rule_detail")
  public String blackRuleDetail;
  @JsonProperty("se_black_rule_detail")
  public String seBlackRuleDetail;
  public String reachLevel;
  public Boolean reQualificationTag;
  @JsonProperty("TAllD_sum_outstanding_ongoingFDC9_General")
  public Integer talldSumOutstandingOngoingFdc9General;
  @JsonProperty("TAllD_uniqCnt_uniqcountOngoingFDC9_General")
  public Integer talldUniqCntUniqcountOngoingFdc9General;
  @JsonProperty("reloan_fee_will_level")
  public Integer reloanFeeWillLevel;
  @JsonProperty("reloan_fee_willv4_group")
  public String reloanFeeWillV4Group;
  @JsonProperty("reloan_fee_will_level_v4")
  public String reloanFeeWillLevelV4;
  @JsonProperty("loan_temp_credits_limit")
  public BigDecimal loanTempCreditsLimit;
  @JsonProperty("v4_model_F4")
  public String V4ModelF4;
  @JsonProperty("Interest_rate_experiment_tag")
  public String interestRateExperimentTag;
  @JsonProperty("rlIncRate_tag")
  public Integer rlIncRateTag;
  @JsonProperty("uplift_01_v1")
  public BigDecimal uplift;
  @JsonProperty("payoff_churn_tag")
  public String payoffChurnTag;


  public static LoanUserTagData from(String loanUserTypeName, BigDecimal credits, String operationLevel) {
    LoanUserTagData data = new LoanUserTagData();
    data.userType = loanUserTypeName;
    data.credits = credits;
    data.operationLevel = operationLevel;
    return data;
  }

  public static LoanUserTagData fromEmpty() {
    LoanUserTagData data = new LoanUserTagData();
    return data;
  }


  public static LoanUserTagData from(String loanUserTypeName, BigDecimal credits,
                                     String operationLevel, Boolean rejectMultiApply,
                                     Boolean rejectMultiOrder, String productTagName,
                                     String preRateTagName, String rateTagName) {

    LoanUserTagData data = from(loanUserTypeName, credits, operationLevel);
    data.rejectMultiApply = rejectMultiApply;
    data.rejectMultiOrder = rejectMultiOrder;
    data.productTag = productTagName;
    data.preRateTag = preRateTagName;
    data.rateTag = rateTagName;
    return data;
  }

  // creditsCoefficient is valid when credits is not exists
  @JsonIgnore
  public String getCreditsCoefficientInfo() {
    if (credits == null && (creditsCoefficient != null || creditsDelta != null)) {
      Map<String, Object> creditsCoefficientInfo = Maps.newHashMap();
      if (creditsCoefficient != null) {
        creditsCoefficientInfo.put("creditsCoefficient", creditsCoefficient);
      }
      if (creditsDelta != null) {
        creditsCoefficientInfo.put("creditsDelta", creditsDelta);
      }
      creditsCoefficientInfo.put("maxCredits", maxCredits);
      creditsCoefficientInfo.put("minCredits", minCredits);
      return JsonUtils.toString(creditsCoefficientInfo);
    }
    return null;
  }

  public boolean hasRiskExperimentCredits() {
    return this.riskTempCreditsA != null || this.riskTempCreditsB != null;
  }
}
