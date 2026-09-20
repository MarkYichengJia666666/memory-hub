package com.yqg.core.model.sql.loan.account.enums;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.yqg.ec.common.enums.IDesc;
import com.yqg.ec.common.enums.Label;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.translation.client.utils.TT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum BankAccountType implements IDesc {
  CURRENT("通用账户", "C"),
  SAVING("储蓄账户", "S"),
  DEBIT_CARD("Debit Card", "D"),
  CLABE("Clabe", "B"),
  SPIN_CLABE("Spin Clabe", "P")
  ;

  private String desc;
  public String code;

  BankAccountType(String desc, String code) {
    this.desc = desc;
    this.code = code;
  }
  @Override
  public TT desc() {
    return TT.gen(desc);
  }
  private static ImmutableList<BankAccountType> DEFAULT_ACCOUNT_TYPE = ImmutableList.of(CURRENT, SAVING);
  private static Map<SDKType, ImmutableList<BankAccountType>> ACCOUNT_TYPE_MAP = Maps.newHashMap();

  public static List<Label<?>> getTypeList(SDKType sdkType) {
    return ACCOUNT_TYPE_MAP.getOrDefault(sdkType, DEFAULT_ACCOUNT_TYPE)
        .stream()
        .map(type -> Label.gen(type.desc(), type.name()))
        .collect(Collectors.toList());
  }

  public static BankAccountType fromCode(String code) {
    for (BankAccountType type : BankAccountType.values()) {
      if (type.code.equals(code)) {
        return type;
      }
    }
    throw EcException.error("Not supported BankAccountType. code = {}", code);
  }
}
