package com.yqg.core.service.risk.usergroup.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yqg.core.model.generated.tables.records.RiskUserGroupCheckConfigLogRecord;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckAvailableStatus;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckCreditStatus;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckDecision;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckOperationRelation;
import com.yqg.ec.common.serialization.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RiskUserGroupCheckConfigLogVO {
  public Long id;
  public Long groupCheckConfigId;
  public LoanRiskUserGroupEnum userGroup;
  public RiskUserGroupCheckCreditStatus creditStatus;
  public RiskUserGroupCheckAvailableStatus availableStatus;
  public List<String> userTypeList;
  public List<String> riskTypeList;
  public RiskUserGroupCheckOperationRelation operationRelation;
  public RiskUserGroupCheckDecision decision;
  public String operator;
  public Long timeCreated;


  public static RiskUserGroupCheckConfigLogVO from(RiskUserGroupCheckConfigLogRecord record) {
    if (record == null) {
      return null;
    }

    List<String> riskTypeList = new ArrayList<>();
    if (Objects.nonNull(record.getRiskTypeList())) {
      riskTypeList = JsonUtils.from(record.getRiskTypeList(), new TypeReference<List<String>>() {
      });
    }

    RiskUserGroupCheckConfigLogVO vo = new RiskUserGroupCheckConfigLogVO();
    vo.id = record.getId();
    vo.groupCheckConfigId = record.getUserGroupCheckConfigId();
    vo.userGroup = LoanRiskUserGroupEnum.valueOf(record.getUserGroup());
    vo.creditStatus = record.getCreditStatus() == null ? null : RiskUserGroupCheckCreditStatus.valueOf(record.getCreditStatus());
    vo.availableStatus = RiskUserGroupCheckAvailableStatus.valueOf(record.getAvailableStatus());
    vo.userTypeList = JsonUtils.from(record.getUserTypeList(), new TypeReference<List<String>>() {
    });
    vo.riskTypeList = riskTypeList;
    vo.operationRelation = RiskUserGroupCheckOperationRelation.valueOf(record.getOperationRelation());
    vo.decision = RiskUserGroupCheckDecision.valueOf(record.getDecision());
    vo.operator = record.getOperator();
    vo.timeCreated = record.getTimeCreated();
    return vo;
  }

}
