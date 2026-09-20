package com.yqg.core.service.cashloan.vo.enums;

import com.google.common.collect.ImmutableList;

/**
 * 验卡服务返回的验卡状态
 */
public enum ValidationBankAccountStatusByThirdParty {
  PENDING("待处理"),
  CARD_UNAVAILABLE("卡号不存在（填写了错误的账号或用户无卡瞎填）"),
  MATCH("卡号可用且姓名匹配"),
  NAME_NOT_MATCH("姓名不匹配"),
  VALIDATION_NAME_EMPTY("三方返回的姓名为空（仅适用于会返回姓名的校验）"),
  CARD_FREEZE("被冻结、被停用"),
  ;

  public static final ImmutableList<ValidationBankAccountStatusByThirdParty> NAME_VALIDATION_STATUS = ImmutableList.of(MATCH, NAME_NOT_MATCH, VALIDATION_NAME_EMPTY);

  public String desc;

  ValidationBankAccountStatusByThirdParty(String desc) {
    this.desc = desc;
  }
}
