package com.yqg.core.service.risk.usergroup.vo;

import com.yqg.core.model.generated.tables.records.LoanRiskUserGroupLogRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanRiskUserGroupChangeReason;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;

/**
 * @author chaoye
 * @date 2025/7/29
 */
public class LoanRiskUserGroupLogVO {

  public Long id;
  public Long userId;
  public Long accountId;
  public LoanRiskUserGroupEnum userGroup;
  public Long traceId;
  public LoanRiskUserGroupChangeReason reason;
  public Long expireTime;
  public Long timeCreated;
  public String externalId;
  public Long expireTimeSourceLogId;


  public static LoanRiskUserGroupLogVO from(LoanRiskUserGroupLogRecord record) {
    if (record == null) {
      return null;
    }
    LoanRiskUserGroupLogVO vo = new LoanRiskUserGroupLogVO();
    vo.id = record.getId();
    vo.userId = record.getUserId();
    vo.accountId = record.getAccountId();
    vo.userGroup = LoanRiskUserGroupEnum.valueOf(record.getUserGroup());
    vo.traceId = record.getTraceId();
    vo.reason = LoanRiskUserGroupChangeReason.valueOf(record.getReason());
    vo.expireTime = record.getExpireTime();
    vo.timeCreated = record.getTimeCreated();
    vo.externalId = record.getExternalId();
    vo.expireTimeSourceLogId = record.getExpireTimeSourceLogId();
    return vo;
  }
}