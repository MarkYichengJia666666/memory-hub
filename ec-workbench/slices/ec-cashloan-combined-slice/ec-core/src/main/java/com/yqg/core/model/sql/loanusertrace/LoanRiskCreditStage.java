package com.yqg.core.model.sql.loanusertrace;

/**
 * 信贷阶段（原 LoanUserRiskCategory 更名而来，枚举值名保持不变以兼容通知模板透传的 name）。
 *
 * @author chaoye
 * @date 2025/10/13
 */
public enum LoanRiskCreditStage {
  LOAN("首贷"),
  RELOAN_WITHOUT_READY_ORDER("结清复贷"),
  MULTI_LOAN("续借"),
  REVOLVING_LOAN("循环贷"),
  ;

  public String desc;

  LoanRiskCreditStage(String desc) {
    this.desc = desc;
  }

}
