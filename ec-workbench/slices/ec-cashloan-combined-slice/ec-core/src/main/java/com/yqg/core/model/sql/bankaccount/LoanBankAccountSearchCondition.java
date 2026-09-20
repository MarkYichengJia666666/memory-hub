package com.yqg.core.model.sql.bankaccount;

import java.util.List;

public class LoanBankAccountSearchCondition {
  public List<String> bankCodes;
  public List<String> accountNumbers;
  public List<String> availableStatusCodes;
  public List<Long> userIds;
  public Long startTimeCreated;
  public Long endTimeCreated;
}
