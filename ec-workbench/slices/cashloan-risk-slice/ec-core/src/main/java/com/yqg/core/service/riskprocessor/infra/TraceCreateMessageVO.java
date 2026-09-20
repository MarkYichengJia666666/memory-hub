package com.yqg.core.service.riskprocessor.infra;

import com.yqg.core.service.cashloan.risk.vo.EventTypeVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.ec.common.enums.risk.RiskCrowdCategory;
import lombok.Data;

@Data
public class TraceCreateMessageVO {
  public Long loanUserRiskTraceId;
  public LoanAccountVO accountVO;
  public EventTypeVO eventTypeVO;
  public Long riskFlowId;
  public Long orderId;
  //实时关系图谱需要知道用户的完件时间
  public Long timeFinished;
  public RiskCrowdCategory riskCrowdCategory;
}
