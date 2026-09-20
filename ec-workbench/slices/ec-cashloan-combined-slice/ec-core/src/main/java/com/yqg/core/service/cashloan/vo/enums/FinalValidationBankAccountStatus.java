package com.yqg.core.service.cashloan.vo.enums;


import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * 根据配置，三方验卡结果得到的最终验卡状态
 */
public enum FinalValidationBankAccountStatus {
  //银行卡、电子钱包通用
  PENDING("P", "待处理"),
  CARD_UNAVAILABLE("U","卡号不存在（填写了错误的账号或用户无卡瞎填）"),
  MATCH("M","匹配"),
  NAME_NOT_MATCH("N", "姓名不匹配"),
  VALIDATION_NAME_EMPTY("E","三方返回的姓名为空（仅适用于会返回姓名的校验）"),
  CARD_FREEZE("F","被冻结、被停用"),

  //只有电子钱包用
  E_WALLET_MOBILE_NUMBER_NOT_MATCH("Y","电子钱包账号与手机号校验不匹配"),
  E_WALLET_MOBILE_NUMBER_AND_NAME_NOT_MATCH("X","电子钱包账号与手机号校验以及姓名校验都不匹配"),
  ;

  public static final List<FinalValidationBankAccountStatus> E_WALLET_NEED_RECHECK = ImmutableList.of(MATCH, NAME_NOT_MATCH, VALIDATION_NAME_EMPTY);

  public String code;
  public String desc;

  FinalValidationBankAccountStatus(String code, String desc) {
    this.code = code;
    this.desc = desc;
  }

}
