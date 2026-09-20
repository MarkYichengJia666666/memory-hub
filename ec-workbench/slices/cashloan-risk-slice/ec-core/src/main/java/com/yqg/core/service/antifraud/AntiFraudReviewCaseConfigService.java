package com.yqg.core.service.antifraud;

import com.yqg.core.model.core.OperationLogRecordVo;
import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.model.generated.tables.records.AntiFraudReviewCaseConfigRecord;
import com.yqg.core.model.sql.antifraud.AntiFraudReviewCaseConfigModel;
import com.yqg.core.model.sql.loan.account.enums.LogEventType;
import com.yqg.core.model.sql.loan.account.enums.ObjType;
import com.yqg.core.model.sql.manualauth.enums.CaseInReviewType;
import com.yqg.core.service.antifraud.vo.AntiFraudCaseReviewConfigVO;
import com.yqg.core.service.antifraud.vo.AntiFraudCaseReviewConfigParamVO;
import com.yqg.core.service.operationlog.OperationLogService;
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
 * @author chenxianrui
 * @date 2025/10/22
 */
@Service
@Slf4j
public class AntiFraudReviewCaseConfigService {
  @Autowired
  private AntiFraudReviewCaseConfigModel antiFraudReviewCaseConfigModel;
  @Autowired
  private OperationLogService operationLogService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;
  @Autowired
  private AntiFraudConfigService antiFraudConfigService;


  /**
   * 创建反欺诈案件审核配置
   * @param caseInReviewType 审核类型
   * @param assignCaseCount 分案件数
   * @param adminUserEmail 操作人员邮箱
   * @return 配置ID
   */
  public void createConfig(AntiFraudCaseReviewConfigParamVO paramVO, String adminUserEmail, Long adminId) {
    // 检查是否已存在该审核类型的配置
    List<AntiFraudReviewCaseConfigRecord> existingConfigs = antiFraudReviewCaseConfigModel.findByReviewGroup(paramVO.getCaseInReviewType());

    if (!existingConfigs.isEmpty()) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("已存在该审核类型：{0}的配置，无法重复创建", paramVO.getCaseInReviewType().name()));
    }

    threadTransactionalModel.transaction(configuration -> {
      AntiFraudReviewCaseConfigRecord record = antiFraudReviewCaseConfigModel.insert(paramVO.getAssignCaseCount(), paramVO.getCaseInReviewType(), adminUserEmail);
      operationLogService.insert("save anti fraud case review config", null,
          JsonUtils.toPrettyString(paramVO),
          LogEventType.SAVE_ANTI_FRAUD_CASE_REVIEW_CONFIG,
          ObjType.ANTI_FRAUD_CASE_REVIEW_CONFIG,
          record.getId(),
          adminId);
    });
  }

  /**
   * 更新反欺诈案件审核配置
   * @param id 配置ID
   * @param caseInReviewType 审核类型
   * @param assignCaseCount 分案件数
   * @param adminUserEmail 操作人员邮箱
   * @return 配置ID
   */
  public void updateConfig(Long id, AntiFraudCaseReviewConfigParamVO paramVO, String adminUserEmail, Long adminId) {
    // 检查是否存在该审核类型的配置（排除当前要更新的记录）
    List<AntiFraudReviewCaseConfigRecord> existingConfigs = antiFraudReviewCaseConfigModel.findByReviewGroupExcludeId(paramVO.getCaseInReviewType(), id);

    if (!existingConfigs.isEmpty()) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("已存在该审核类型：{0}的配置，无法更新", paramVO.getCaseInReviewType().name()));
    }

    threadTransactionalModel.transaction(configuration -> {
      // 查找要更新的配置
      AntiFraudReviewCaseConfigRecord record = antiFraudReviewCaseConfigModel.findByIdOrThrowForUpdate(id);
      antiFraudReviewCaseConfigModel.update(record, paramVO.getAssignCaseCount(), paramVO.getCaseInReviewType(), adminUserEmail);
      // 获取修改前的内容
      OperationLogRecordVo operationLogRecordVo = operationLogService.findLastedByObj(ObjType.ANTI_FRAUD_CASE_REVIEW_CONFIG, id);
      String beforeContent = Objects.isNull(operationLogRecordVo) ? "" : operationLogRecordVo.getContentAfter();

      // 记录操作日志
      operationLogService.insert("update anti fraud case review config", beforeContent,
          JsonUtils.toPrettyString(paramVO),
          LogEventType.SAVE_ANTI_FRAUD_CASE_REVIEW_CONFIG,
          ObjType.ANTI_FRAUD_CASE_REVIEW_CONFIG,
          id,
          adminId);
    });
  }

  /**
   * 分页查询反欺诈案件审核配置列表
   * @param pageNo 页码
   * @param pageSize 每页大小
   * @return 配置列表
   */
  public List<AntiFraudCaseReviewConfigVO> listPage(Integer pageNo, Integer pageSize) {
    List<AntiFraudReviewCaseConfigRecord> records = antiFraudReviewCaseConfigModel.listPage(pageNo, pageSize);

    return records.stream()
        .map(AntiFraudCaseReviewConfigVO::from)
        .collect(Collectors.toList());
  }

  /**
   * 查询配置总数
   * @return 配置总数
   */
  public Integer count() {
    return antiFraudReviewCaseConfigModel.count();
  }

  public Integer fetchAssignCaseCountOnce(CaseInReviewType caseInReviewType) {
    // todo chenxianrui 后续删除此开关
    Integer pullCaseSizeOnce = antiFraudConfigService.getPullCaseSizeOnce();
    AntiFraudReviewCaseConfigRecord antiFraudReviewCaseConfigRecord = antiFraudReviewCaseConfigModel.findOneByReviewGroup(caseInReviewType);
    if (Objects.nonNull(antiFraudReviewCaseConfigRecord)) {
      pullCaseSizeOnce = antiFraudReviewCaseConfigRecord.getOnceAssignCaseCount();
    }

    return pullCaseSizeOnce;
  }
}
