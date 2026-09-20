package com.yqg.core.service.risk.usergroup.enums;

import lombok.Getter;

@Getter
public enum RiskUserGroupCheckAvailableStatus {
  AVAILABLE("生效"),
  UNAVAILABLE("失效"),
  ;

  public final String desc;

  RiskUserGroupCheckAvailableStatus(String desc) {
    this.desc = desc;
  }

}
