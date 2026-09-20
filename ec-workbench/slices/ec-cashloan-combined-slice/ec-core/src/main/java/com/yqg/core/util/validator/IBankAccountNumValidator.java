package com.yqg.core.util.validator;

import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.loan.account.enums.BankAccountType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.translation.client.utils.TT;

/**
 * Created by jpdu on 2017/6/2.
 */
public interface IBankAccountNumValidator {

  default void throwWhenNotValid(boolean valid, String accountNum) {

    if (!valid) {
      throw EcException.warn(EcExceptionType.COMMON_ILLEGAL_ACCOUNT_NUMBER, TT.gen("银行卡号格式不正确"), "银行卡号格式不正确,accountNumber={}", accountNum);
    }
  }

  boolean validAccountNumber(BankType bankType, BankAccountType bankAccountType, String accountNumber);
}
