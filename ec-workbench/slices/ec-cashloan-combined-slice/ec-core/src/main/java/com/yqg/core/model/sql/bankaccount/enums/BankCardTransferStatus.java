package com.yqg.core.model.sql.bankaccount.enums;

/**
 * @author chenxianrui
 * @date 2025/2/20
 */
public enum BankCardTransferStatus {
  SUPPORTED("S", "支持打款"),
  NOT_SUPPORTED("N", "不支持打款");

  public String code;
  public String description;

  // 构造方法
  BankCardTransferStatus(String code, String description) {
    this.code = code;
    this.description = description;
  }
}
