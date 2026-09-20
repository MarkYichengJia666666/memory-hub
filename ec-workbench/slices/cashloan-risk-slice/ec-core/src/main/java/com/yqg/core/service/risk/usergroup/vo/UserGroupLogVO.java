package com.yqg.core.service.risk.usergroup.vo;

import com.yqg.core.model.generated.tables.records.LoanRiskUserGroupLogRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import lombok.Data;

/**
 * 用户组变更日志VO
 *
 * @author chenxianrui
 * @date 2025/9/28
 */
@Data
public class UserGroupLogVO {

  /** 对应 loan_risk_user_group_log.id，用于精确关联 operation_log */
  private Long logId;
  private Long userId;
  private Long loanAccountId;
  public LoanRiskUserGroupEnum userGroup;
  private Long traceId;
  public LoanRiskUserGroupChangeReason reason;
  /** 修改原因备注，admin 批量操作时填写 */
  private String changeReasonRemark;
  /** 操作人邮箱 */
  private String operatorEmail;
  private Long expireTime;
  private Long timeCreated;
  /** 本次变更的来源外部业务 id，语义由 reason 的 {@code ExternalIdSourceType} 决定（订单 id / traceId / 空） */
  private String externalId;
  /** 当前管制期的源头 log id，无管制期时为 null */
  private Long expireTimeSourceLogId;

  public static UserGroupLogVO from(LoanRiskUserGroupLogRecord record) {
    if (record == null) {
      return null;
    }

    UserGroupLogVO vo = new UserGroupLogVO();
    vo.setLogId(record.getId());
    vo.setUserId(record.getUserId());
    vo.setLoanAccountId(record.getAccountId());
    vo.setUserGroup(LoanRiskUserGroupEnum.valueOf(record.getUserGroup()));
    vo.setTraceId(record.getTraceId());
    vo.setReason(LoanRiskUserGroupChangeReason.valueOf(record.getReason()));
    vo.setExpireTime(record.getExpireTime());
    vo.setTimeCreated(record.getTimeCreated());
    vo.setExternalId(record.getExternalId());
    vo.setExpireTimeSourceLogId(record.getExpireTimeSourceLogId());
    return vo;
  }
}