package com.yqg.core.service.risk.riskflow;

import com.yqg.core.model.core.RevType;
import com.yqg.core.model.generated.tables.records.RiskFlowAudRecord;
import com.yqg.core.model.sql.risk.RiskFlowAudModel;
import com.yqg.core.service.risk.riskflow.vo.RiskFlowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskFlowAudService {
  @Autowired
  private RiskFlowAudModel riskFlowAudModel;

  public RiskFlowAudRecord insertRiskFlowAud(RiskFlowVO riskFlowVO, Long userOpt, RevType revType) {
    return riskFlowAudModel.insert(riskFlowVO, userOpt, revType);
  }

  public List<RiskFlowAudRecord> getRiskFlowAuds(Long id) {
    return riskFlowAudModel.findById(id);
  }
}