package com.yqg.core.service.riskprocessor.loan;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.model.generated.tables.records.LoanUserCreditsInfoRecord;
import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.cashloan.vo.CashLoanCalcCreditsVO;
import com.yqg.core.service.riskprocessor.infra.BaseRiskProcessor;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author shubo
 * @date 2022/6/21 1:57 下午
 */
public abstract class BaseLoanApplyCalcCreditsRiskProcessor extends BaseRiskProcessor {

  protected void checkLoanReapply(LoanUserCreditsInfoRecord creditsInfoRecord, List<LoanUserRiskType> loanUserRiskTypeList, BooleanType submitRisk) {
    LoanCreditsStatus loanCreditsStatus = LoanCreditsStatus.fromCode(creditsInfoRecord.getCreditsStatus());
    EcAsserts.assertTrue(StringUtils.isEmpty(creditsInfoRecord.getReloanStatus()),
        "Reloan credit status should be null for {}! Account id is {}.", getLoanUserRiskType().name(), creditsInfoRecord.getLoanAccountId());
    if (loanCreditsStatus == LoanCreditsStatus.REJECTED) {
      EcAsserts.assertTrue(checkSubmitRisk(submitRisk) || (Clock.now() > creditsInfoRecord.getTimeReapply() && creditsInfoRecord.getTimeReapply() > 0),
          "It's not the time to {} yet! Account id is {}.", getLoanUserRiskType().name(), creditsInfoRecord.getLoanAccountId());
    } else if (loanCreditsStatus == LoanCreditsStatus.ACCEPTED) {
      CashLoanCalcCreditsVO cashLoanCalcCreditsVO = checkAndGetCashLoanCalcCreditsVO(creditsInfoRecord);
      if (isCreditsExpired(cashLoanCalcCreditsVO, submitRisk, loanUserRiskTypeList)) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("网络异常，请稍后再试"), "can not calc credits with wrong riskType,accountId is {}", creditsInfoRecord.getLoanAccountId());
      }
    } else {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("请勿重复提交"), "current user can't submit risk application, accountId is {}", creditsInfoRecord.getLoanAccountId());
    }
  }
}