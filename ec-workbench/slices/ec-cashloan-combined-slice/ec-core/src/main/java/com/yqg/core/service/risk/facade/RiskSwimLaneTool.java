package com.yqg.core.service.risk.facade;

import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.ec.common.utils.SysEnvironment;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaoye
 * @date 2025/12/31
 */
@Service
public class RiskSwimLaneTool {

  @Autowired
  private RiskConfig riskConfig;

  public boolean canHandleByCurrentSwimLane(String swimLaneId, LoanUserRiskTraceVO loanUserRiskTraceVO) {
    //仅在测试环境检查
    if (!SysEnvironment.isTest()) {
      return true;
    }

    Long accountId = loanUserRiskTraceVO.accountId;

    List<String> targetLanes = riskConfig.getAccountIdToSwimLanesMap().get(accountId);
    if (CollectionUtils.isEmpty(targetLanes)) {
      //未配置泳道，不拦截，返回true
      return true;
    }

    //配置了泳道，但是header里面没有泳道id，说明是主干，则拦截，返回false
    if (swimLaneId == null) {
      return false;
    }

    //检查是否配置了当前泳道
    return targetLanes.contains(swimLaneId);
  }
}
