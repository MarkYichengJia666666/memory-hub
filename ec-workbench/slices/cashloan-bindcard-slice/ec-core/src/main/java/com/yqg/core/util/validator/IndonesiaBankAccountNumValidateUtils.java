package com.yqg.core.util.validator;

import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.ec.common.i18n.YqgLocale;
import com.yqg.ec.common.i18n.mobile.GlobalMobileNumValidator;
import org.apache.commons.lang3.StringUtils;

public class IndonesiaBankAccountNumValidateUtils {

  public static boolean validAccountNumber(BankType bankType, String accountNumber, Integer supportMaxAccountNumberLength) {
    return isAccountNumberLengthAvailable(bankType, accountNumber, supportMaxAccountNumberLength) && StringUtils.isNumeric(accountNumber);
  }

  private static boolean isAccountNumberLengthAvailable(BankType bankType, String accountNumber, Integer supportMaxAccountNumberLength) {
    if (bankType == BankType.BCA) {
      return accountNumber.trim().length() == 10;
    }
    if (bankType == BankType.BRI) {
      return accountNumber.trim().length() == 15;
    }
    //电子钱包是手机号，也统一放在这里吧
    if (bankType.isIdnEWalletType()) {
      return GlobalMobileNumValidator.isMobileNumber(YqgLocale.INDONESIAN, accountNumber);
    }
    return accountNumber.length() <= supportMaxAccountNumberLength;
  }
}
