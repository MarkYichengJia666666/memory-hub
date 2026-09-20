package com.yqg.core.service.risk.usergroup;

import com.alibaba.excel.util.CollectionUtils;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.RiskUserGroupCheckConfigLogRecord;
import com.yqg.core.model.generated.tables.records.RiskUserGroupCheckConfigRecord;
import com.yqg.core.model.sql.risk.RiskUserGroupCheckConfigLogModel;
import com.yqg.core.model.sql.risk.RiskUserGroupCheckConfigModel;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckAvailableStatus;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckCreditStatus;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckDecision;
import com.yqg.core.service.risk.usergroup.enums.RiskUserGroupCheckOperationRelation;
import com.yqg.core.service.risk.usergroup.vo.RiskUserGroupCheckConfigLogVO;
import com.yqg.core.service.risk.usergroup.vo.RiskUserGroupCheckConfigVO;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author chaoye
 * @date 2025/11/26
 */
@Service
@Slf4j
public class RiskUserGroupCheckConfigService {
  @Autowired
  private RiskUserGroupCheckConfigModel riskUserGroupCheckConfigModel;
  @Autowired
  private RiskUserGroupCheckConfigLogModel riskUserGroupCheckConfigLogModel;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;

  public RiskUserGroupCheckConfigVO create(
    LoanRiskUserGroupEnum userGroup,
    RiskUserGroupCheckCreditStatus creditStatus,
    RiskUserGroupCheckAvailableStatus availableStatus,
    RiskUserGroupCheckOperationRelation operationRelation,
    List<String> userTypeList,
    List<String> riskTypeList,
    RiskUserGroupCheckDecision decision,
    String operator
  ) {
    return threadTransactionalModel.transactionResult(configuration -> {
      String userTypeString = JsonUtils.toString(userTypeList);
      String riskTypeString = CollectionUtils.isEmpty(riskTypeList) ? null : JsonUtils.toString(riskTypeList);
      checkDuplicate(userGroup, creditStatus, availableStatus, operationRelation, userTypeString, riskTypeString, null);

      RiskUserGroupCheckConfigRecord riskUserGroupCheckConfigRecord = riskUserGroupCheckConfigModel.insert(
        userGroup,
        creditStatus,
        availableStatus,
        operationRelation,
        userTypeString,
        riskTypeString,
        decision,
        operator
      );

      riskUserGroupCheckConfigLogModel.insert(riskUserGroupCheckConfigRecord);
      return RiskUserGroupCheckConfigVO.from(riskUserGroupCheckConfigRecord);
    });
  }

  public RiskUserGroupCheckConfigVO update(
    Long id,
    LoanRiskUserGroupEnum userGroup,
    RiskUserGroupCheckCreditStatus creditStatus,
    RiskUserGroupCheckAvailableStatus availableStatus,
    RiskUserGroupCheckOperationRelation operationRelation,
    List<String> userTypeList,
    List<String> riskTypeList,
    RiskUserGroupCheckDecision decision,
    String operator
  ) {
    RiskUserGroupCheckConfigRecord record = riskUserGroupCheckConfigModel.findById(id);
    if (Objects.isNull(record)) {
      throw EcException.error("RiskUserGroupCheckConfigRecord is null");
    }
    return threadTransactionalModel.transactionResult(configuration -> {
      String userTypeString = JsonUtils.toString(userTypeList);
      String riskTypeString = CollectionUtils.isEmpty(riskTypeList) ? null : JsonUtils.toString(riskTypeList);

      checkDuplicate(userGroup, creditStatus, availableStatus, operationRelation, userTypeString, riskTypeString, record.getId());

      RiskUserGroupCheckConfigRecord riskUserGroupCheckConfigRecord = riskUserGroupCheckConfigModel.update(
        record,
        userGroup,
        creditStatus,
        availableStatus,
        operationRelation,
        userTypeString,
        riskTypeString,
        decision,
        operator
      );

      riskUserGroupCheckConfigLogModel.insert(riskUserGroupCheckConfigRecord);
      return RiskUserGroupCheckConfigVO.from(riskUserGroupCheckConfigRecord);
    });
  }

  private void checkDuplicate(
    LoanRiskUserGroupEnum userGroup,
    RiskUserGroupCheckCreditStatus creditStatus,
    RiskUserGroupCheckAvailableStatus availableStatus,
    RiskUserGroupCheckOperationRelation operationRelation,
    String userTypeString,
    String riskTypeString,
    Long id
  ) {
    List<RiskUserGroupCheckConfigRecord> existingRecords = riskUserGroupCheckConfigModel.getExisting(userGroup, creditStatus, availableStatus, operationRelation, userTypeString, riskTypeString);
    existingRecords = existingRecords
      .stream()
      .filter(record -> !Objects.equals(record.getId(), id))
      .collect(Collectors.toList());

    if (!CollectionUtils.isEmpty(existingRecords)) {
      throw EcException.warn(EcExceptionType.USER_GROUP_RULE_DUPLICATE,
        TT.gen("Data already exists, please change the User Group/Operation Relation/User Type/Risk Type"));
    }
  }

  public List<RiskUserGroupCheckConfigVO> queryByCondition(
    LoanRiskUserGroupEnum userGroup,
    RiskUserGroupCheckCreditStatus creditStatus,
    RiskUserGroupCheckAvailableStatus availableStatus,
    String userType,
    Integer pageNo,
    Integer pageSize
  ) {
    int offset = (pageNo - 1) * pageSize;
    List<RiskUserGroupCheckConfigRecord> records = riskUserGroupCheckConfigModel.findByCondition(userGroup, creditStatus, availableStatus, userType, offset, pageSize);

    return records.stream().map(RiskUserGroupCheckConfigVO::from).collect(Collectors.toList());
  }

  public Integer countByCondition(LoanRiskUserGroupEnum userGroup, RiskUserGroupCheckCreditStatus creditStatus, RiskUserGroupCheckAvailableStatus availableStatus, String userType) {
    return riskUserGroupCheckConfigModel.countByCondition(userGroup, creditStatus, availableStatus, userType);
  }

  public List<RiskUserGroupCheckConfigLogVO> queryHistory(Long configId, Integer pageNo, Integer pageSize) {
    int offset = (pageNo - 1) * pageSize;
    List<RiskUserGroupCheckConfigLogRecord> records = riskUserGroupCheckConfigLogModel.findByConfigId(configId, offset, pageSize);

    return records.stream().map(RiskUserGroupCheckConfigLogVO::from).collect(Collectors.toList());
  }

  public Integer countHistory(Long configId) {
    return riskUserGroupCheckConfigLogModel.countByConfigId(configId);
  }
}
