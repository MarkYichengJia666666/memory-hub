package com.yqg.core.service.loan.vo.bankaccount;

import com.yqg.core.model.generated.tables.records.BankConfigRecord;
import com.yqg.core.model.generated.tables.records.LoanBankAccountRecord;
import com.yqg.core.model.sql.bankaccount.enums.BankAccountAvailableStatus;
import com.yqg.core.model.sql.bankaccount.enums.BankCardTransferStatus;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.model.sql.loan.account.enums.BankAccountType;
import com.yqg.core.model.sql.payment.enums.CompanyEnum;
import com.yqg.core.service.payment.ICredential;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.serialization.module.IDLongEncodeStrategy;
import com.yqg.ec.common.serialization.module.IDStringDecodeStrategy;
import com.yqg.ec.common.utils.MaskUtils;
import com.yqg.translation.client.utils.TT;
import lombok.Data;

/**
 * Created by xiahonggao on 2/27/16.
 */
@Data
public class LoanBankAccountVO implements ICredential {
  @IDLongEncodeStrategy
  @IDStringDecodeStrategy
  public Long bankAccountId;

  @IDLongEncodeStrategy
  @IDStringDecodeStrategy
  public Long userId;

  public String bankCode;
  public BankType bankType;
  public BankAccountType bankAccountType;
  public String bankName;
  public String bankLogoUrl;
  public String maskedAccountNumber;
  public String accountNumber;
  public String name;
  public Long timeFirstAdded;
  public BankAccountAvailableStatus bankAccountAvailableStatus;
  public SDKType sdk;
  public String validationId;
  public CompanyEnum companyEnum;
  public TT disableReason;
  public PaymentBusinessName businessName;
  public Long lastTimeUsed;
  public Long timeCreated;
  public Long timeUpdated;
  public BankCardTransferStatus transferStatus;

  public static LoanBankAccountVO from(LoanBankAccountRecord record) {
    LoanBankAccountVO account = new LoanBankAccountVO();
    account.bankAccountId = record.getId();
    account.userId = record.getUserId();
    account.bankCode = record.getBankCode();
    account.bankType = BankType.valueOf(record.getBankCode());
    account.bankAccountType = record.getBankAccountType() == null ? null : BankAccountType.valueOf(record.getBankAccountType());
    account.bankName = account.bankType.description;
    account.accountNumber = record.getAccountNumber();
    account.maskedAccountNumber = MaskUtils.getLastFourNum(account.accountNumber);
    account.name = record.getName();
    account.bankAccountAvailableStatus = BankAccountAvailableStatus.fromCodeOrThrow(record.getAvailableStatus());
    account.sdk = SDKType.fromCode(record.getSdkType());
    account.timeFirstAdded = record.getTimeCreated();
    account.validationId = record.getValidationId();
    account.businessName = PaymentBusinessName.fromCodeOrNull(record.getBusinessName());
    account.lastTimeUsed = record.getLastTimeUsed();
    account.timeCreated = record.getTimeCreated();
    account.timeUpdated = record.getTimeUpdated();
    return account;
  }

  public static LoanBankAccountVO from(LoanBankAccountRecord record, BankConfigRecord configRecord) {
    LoanBankAccountVO vo = from(record);
    vo.bankLogoUrl = configRecord.getLogoUrl();
    return vo;
  }

  public static LoanBankAccountVO from(BankType bankType, String accountNumber, String name, BankAccountAvailableStatus availableStatus) {
    LoanBankAccountVO vo = new LoanBankAccountVO();
    vo.bankType = bankType;
    vo.accountNumber = accountNumber;
    vo.name = name;
    vo.bankName = bankType.description;
    vo.bankAccountAvailableStatus = availableStatus;
    vo.maskedAccountNumber = MaskUtils.getLastFourNum(vo.accountNumber);
    return vo;
  }

  @Override
  public Long getId() {
    return bankAccountId;
  }

  @Override
  public Long getUserId() {
    return userId;
  }

  @Override
  public Boolean isCredentialAvailable() {
    return bankAccountAvailableStatus == BankAccountAvailableStatus.AVAILABLE;
  }

  @Override
  public String getChannelName() {
    return bankType.name();
  }
}
