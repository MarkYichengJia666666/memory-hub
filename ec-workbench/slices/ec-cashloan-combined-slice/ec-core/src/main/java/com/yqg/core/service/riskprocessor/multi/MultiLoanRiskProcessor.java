package com.yqg.core.service.riskprocessor.multi;

import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import org.springframework.stereotype.Service;
/**
 * @author Zoran Zhang
 * @Description:
 * @date 2021/7/29 10:47 上午
 */
@Service
public class MultiLoanRiskProcessor extends BaseMultiLoanRiskProcessor {
  @Override
  protected LoanUserRiskType getLoanUserRiskType() {
    return LoanUserRiskType.MULTI_LOAN;
  }
}
