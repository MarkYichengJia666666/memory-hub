package com.yqg.core.service.payment;

import com.google.common.collect.ImmutableMap;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.core.service.sdktype.factory.ISDKTypeMapper;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.enums.PaymentBusinessName;

import static com.yqg.ec.common.enums.PaymentBusinessName.*;

public class PaymentBusinessNameMapper implements ISDKTypeMapper<PaymentBusinessName> {
  private static final ImmutableMap<SDKType, PaymentBusinessName> sdkTypePaymentBusinessNameMap = new ImmutableMap
      .Builder<SDKType, PaymentBusinessName>()
      .put(SDKType.IDN_YQD, IDN_YQD)
      .put(SDKType.IDN_NXT, IDN_NXT)
      .put(SDKType.IDN_FIN, IDN_FIN)
      .build();

  public static PaymentBusinessName getBusinessName(SDKType sdkType) {
    PaymentBusinessName businessName = sdkTypePaymentBusinessNameMap.get(sdkType);
    if (businessName == null) {
      throw EcException.error("can't find payment business name by sdk : {}", sdkType);
    }
    return businessName;
  }

  @Override
  public ImmutableMap<SDKType, PaymentBusinessName> getImmutableMap() {
    return sdkTypePaymentBusinessNameMap;
  }
}
