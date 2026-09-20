package com.yqg.core.service.riskprocessor.loan;

import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.stereotype.Service;

/**
 * @author shubo
 * @date 2022/8/12 2:49 下午
 */
@Service
public class LoanRepplyOrderCreditsRiskProcessor extends BaseSecondRiskProcessor {
  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.LOAN_REPPLY_ORDER_CREDITS;
  }
}
