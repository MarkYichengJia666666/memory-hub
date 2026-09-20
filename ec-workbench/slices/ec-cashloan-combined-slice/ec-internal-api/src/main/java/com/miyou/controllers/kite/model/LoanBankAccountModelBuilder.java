package com.miyou.controllers.kite.model;

import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.LoanBankAccount;
import com.yqg.risk.feature.platform.client.model.builder.KiteModelBuilder;
import org.springframework.stereotype.Component;

@Component
public class LoanBankAccountModelBuilder extends AbstractMysqlEcLoanModel {
  @Override
  public KiteModelBuilder getFieldMapper() {
    LoanBankAccount loanBankAccount = Tables.LOAN_BANK_ACCOUNT;

    return KiteModelBuilder
        .create(loanBankAccount, request -> loanBankAccount.USER_ID.eq(request.getUserId()).and(loanBankAccount.TIME_CREATED.lt(request.getTimestamp())))
        .addByField(loanBankAccount.ID)
        .addByField(loanBankAccount.USER_ID)
        .addByField(loanBankAccount.BUSINESS_NAME)
        .addByField(loanBankAccount.BANK_CODE)
        .addByField(loanBankAccount.BANK_ACCOUNT_TYPE)
        .addByField(loanBankAccount.ACCOUNT_NUMBER)
        .addByField(loanBankAccount.NAME)
        .addByField(loanBankAccount.AVAILABLE_STATUS)
        .addByField(loanBankAccount.SDK_TYPE)
        .addByField(loanBankAccount.VALIDATION_ID)
        .addLongAsDateTime(loanBankAccount.LAST_TIME_USED)
        .addLongAsDateTime(loanBankAccount.TIME_CREATED)
        .addLongAsDateTime(loanBankAccount.TIME_UPDATED)
        ;
  }

  @Override
  public String getTableName() {
    return "loan_bank_account";
  }

  @Override
  public String getTableDesc() {
    return "借款银行卡表";
  }
}
