package com.yqg.core.service.risk.usergroup.enums;

import lombok.Getter;

/**
 * 用户组风控检查决策流程
 */
@Getter
public enum RiskUserGroupCheckDecision {
  BLOCK("是"),//卡件
  NOT_BLOCK("否"),//不卡件
  ;

  public final String desc;

  RiskUserGroupCheckDecision(String desc) {
    this.desc = desc;
  }
}
