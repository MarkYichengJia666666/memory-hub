package com.yqg.core.service.riskprocessor.multi;

import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.stereotype.Service;

@Service
public class FirstMultiLoanRepaymentCalcCreditsRiskProcessor extends BaseMultiLoanCalcCreditsRiskProcessor {
  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.FIRST_MULTI_LOAN_CALC_CREDITS;
  }
}
