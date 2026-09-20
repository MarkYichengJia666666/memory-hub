package com.yqg.core.service.riskprocessor.loan;

import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.stereotype.Service;
/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/28 11:11 上午
 */
@Service
public class SecondRiskProcessor extends BaseSecondRiskProcessor {

  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.SECOND;
  }
}
