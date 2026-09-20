package com.yqg.core.service.risk.usergroup.vo;

import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.Getter;
import lombok.Setter;

/**
 * @author chaoye
 * @date 2026/1/20
 */
@Getter
@Setter
public class UserGroupHistoryTraceData {
  private LoanRiskUserGroupEnum userGroup;
  private String userType;
  private Long traceId;
  private LoanUserRiskType riskType;
  private Boolean canGetLoanByTraceOutput;
  private SourceType sourceType;
}
