package com.yqg.core.util.validator;

import com.google.common.collect.ImmutableMap;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.loan.account.enums.BankAccountType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.i18n.YqgLocale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 银行卡账号校验代理，根据YqgLocale 来调用不同国家的校验规则。
 * 默认使用 CHINA 的校验规则。
 * Created by jpdu on 2017/6/2.
 */
@Component
@Slf4j
public class GlobalBankAccountNumValidator {
  @Autowired
  private IndonesiaBankAccountNumValidator indonesiaBankAccountNumValidator;
  @Autowired
  private IndonesiaEnBankAccountNumValidator indonesiaEnBankAccountNumValidator;

  private static Map<YqgLocale, IBankAccountNumValidator> validatorMapper;

  @PostConstruct
  public void init() {
    ImmutableMap.Builder<YqgLocale, IBankAccountNumValidator> bankAccountNumValidatorBuilder = new ImmutableMap.Builder();
    for (YqgLocale locale : YqgLocale.values()) {
      bankAccountNumValidatorBuilder.put(locale, (bankType, bankAccountType, accountNumber) -> true);
    }
    bankAccountNumValidatorBuilder.put(YqgLocale.INDONESIAN, indonesiaBankAccountNumValidator);
    bankAccountNumValidatorBuilder.put(YqgLocale.INDONESIAN_EN, indonesiaEnBankAccountNumValidator);

    validatorMapper = bankAccountNumValidatorBuilder.buildKeepingLast();
  }

  private IBankAccountNumValidator getValidator(YqgLocale locale) {
    IBankAccountNumValidator validator = validatorMapper.get(locale);
    if (validator == null) {
      throw EcException.error("该locale没有对应的BankAccountNumValidator, locale:" + locale);
    }
    return validator;
  }

  public boolean isValidBankAccountNumber(YqgLocale locale, BankType bankType, BankAccountType bankAccountType, String accountNumber) {
    return getValidator(locale).validAccountNumber(bankType, bankAccountType, accountNumber);
  }
}
