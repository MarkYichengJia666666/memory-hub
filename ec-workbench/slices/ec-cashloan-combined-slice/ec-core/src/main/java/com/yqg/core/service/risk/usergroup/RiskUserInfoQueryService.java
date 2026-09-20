package com.yqg.core.service.risk.usergroup;

import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.loan.account.LoanUserTypeLogModel;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountBasicInfoService;
import com.yqg.core.service.risk.subnew.SubNewConfig;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 用户在风控的各类信息查询
 *
 * @author chaoye
 * @date 2026/1/23
 */
@Service
@Slf4j
public class RiskUserInfoQueryService {
  @Autowired
  private LoanUserTypeLogModel loanUserTypeLogModel;
  @Autowired
  private LoanUserRiskTraceService loanUserRiskTraceService;
  @Autowired
  private LoanAccountBasicInfoService loanAccountBasicInfoService;
  @Autowired
  private SubNewConfig subNewConfig;
  @Autowired
  private LoanRiskUserGroupService loanRiskUserGroupService;
  @Autowired
  private LoanAccountModel loanAccountModel;

  /**
   * 最近的风控是完件风控，并且在主营通过
   * 注意：如果未提交完件风控，返回false
   *
   * @return
   */
  public boolean isLatestTraceAuthRiskAcceptByNormalUserGroup(Long accountId) {
    LoanUserRiskTraceVO latestTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(accountId);
    if (latestTraceVO == null) {
      return false;
    }
    LoanUserRiskTraceVO authRiskAcceptTraceVO = loanUserRiskTraceService.findAuthLoanAcceptedTraceVO(latestTraceVO.userId);
    if (authRiskAcceptTraceVO == null) {
      return false;
    }
    return Objects.equals(latestTraceVO.traceId, authRiskAcceptTraceVO.traceId) && authRiskAcceptTraceVO.riskType == LoanUserRiskType.LOAN;
  }

  /**
   * 最近的风控是完件风控，并且在非主营通过
   * 注意：如果未提交完件风控，返回false
   *
   * @return
   */
  public boolean isLatestTraceAuthRiskAcceptByNotNormalUserGroup(Long accountId) {
    LoanUserRiskTraceVO latestTraceVO = loanUserRiskTraceService.findLatestCreditRiskByAccountId(accountId);
    if (latestTraceVO == null) {
      return false;
    }
    LoanUserRiskTraceVO authRiskAcceptTraceVO = loanUserRiskTraceService.findAuthLoanAcceptedTraceVO(latestTraceVO.userId);
    if (authRiskAcceptTraceVO == null) {
      return false;
    }
    return Objects.equals(latestTraceVO.traceId, authRiskAcceptTraceVO.traceId) && authRiskAcceptTraceVO.riskType != LoanUserRiskType.LOAN;
  }

  /**
   * 最近的风控是完件风控，并且通过（包括主营/非主营两种情况）
   * 注意：如果未提交完件风控，返回false
   *
   * @return
   */
  public boolean isLatestTraceAuthRiskAccept(Long accountId) {
    LoanAccountRecord loanAccountRecord = loanAccountModel.findByIdOrThrow(accountId);
    return loanUserRiskTraceService.isFirstLoanAcceptedByUserId(loanAccountRecord.getUserId());
  }

}
