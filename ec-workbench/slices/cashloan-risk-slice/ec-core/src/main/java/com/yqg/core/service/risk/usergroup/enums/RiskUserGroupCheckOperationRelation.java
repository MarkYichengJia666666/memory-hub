package com.yqg.core.service.risk.usergroup.enums;

import lombok.Getter;

/**
 * 用户组风控检查运算关系
 */
@Getter
public enum RiskUserGroupCheckOperationRelation {
  CONTAINS("包含"),
  NOT_CONTAINS("不包含"),
  ;

  public final String desc;

  RiskUserGroupCheckOperationRelation(String desc) {
    this.desc = desc;
  }
}
