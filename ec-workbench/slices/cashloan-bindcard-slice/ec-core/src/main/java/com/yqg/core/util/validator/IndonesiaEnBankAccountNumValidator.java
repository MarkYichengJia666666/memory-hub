package com.yqg.core.util.validator;

import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.loan.account.enums.BankAccountType;
import com.yqg.core.service.loan.bankaccount.LoanBankConfig;
import com.yqg.ec.common.i18n.YqgLocale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by xiuqichenyang on 17/7/21.
 */
@Component
public class IndonesiaEnBankAccountNumValidator implements IBankAccountNumValidator {
  @Autowired
  private LoanBankConfig loanBankConfig;

  @Override
  public boolean validAccountNumber(BankType bankType, BankAccountType bankAccountType, String accountNumber) {
    return IndonesiaBankAccountNumValidateUtils.validAccountNumber(
        bankType,
        accountNumber,
        loanBankConfig.getSupportMaxAccountNumberLength(bankType)
    );
  }
}
