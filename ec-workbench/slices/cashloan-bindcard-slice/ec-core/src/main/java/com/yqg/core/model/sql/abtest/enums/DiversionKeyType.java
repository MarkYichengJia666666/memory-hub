package com.yqg.core.model.sql.abtest.enums;

import com.google.common.collect.ImmutableSet;
import com.yqg.ec.common.exception.EcException;

import java.util.Set;

public enum DiversionKeyType {
  USER_ID("U"),
  DEVICE_TOKEN("D"),
  NORMALIZED_MOBILE_NUMBER("M"),
  NIK("N"),
  ;

  public String code;

  DiversionKeyType(String code) {
    this.code = code;
  }

  public static final Set<DiversionKeyType> DEFAULT_TYPES = ImmutableSet.of(USER_ID, DEVICE_TOKEN, NORMALIZED_MOBILE_NUMBER, NIK);
  public static final Set<DiversionKeyType> NOT_LOGGED_IN_TYPES = ImmutableSet.of(DEVICE_TOKEN);

  public static DiversionKeyType fromCode(String code) {
    for (DiversionKeyType diversionKeyType : DiversionKeyType.values()) {
      if (diversionKeyType.code.equals(code)) {
        return diversionKeyType;
      }
    }
    throw EcException.error("DiversionKeyType not found, code: {}", code);
  }
}
