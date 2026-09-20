package com.yqg.scheduler.jobs;

import com.yqg.core.model.sql.loanusertrace.LoanUserRiskTraceModel;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.enums.risk.RiskFlowTraceStatusV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author shubo
 * @date 2023/8/2 4:00 下午
 */
@Service
@Slf4j
public class RiskIncreaseCreditsJob extends BaseAutoReviewCreditsInfoJob {
  @Autowired
  private LoanUserRiskTraceModel loanUserRiskTraceModel;

  @Override
  List<Long> listInReviewTraceIds(Param param) {
    return loanUserRiskTraceModel.listTraceIdByStatusAndRiskType(
        LoanUserRiskType.getUserExtraRiskTypes(), RiskFlowTraceStatusV2.INIT, resolveMinTimeUpdated(param));
  }
}
