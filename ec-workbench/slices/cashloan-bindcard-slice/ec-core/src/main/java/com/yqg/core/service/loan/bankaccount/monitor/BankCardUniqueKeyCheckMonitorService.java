package com.yqg.core.service.loan.bankaccount.monitor;

import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.monitor.BaseMonitorService;
import com.yqg.core.service.monitor.MonitorMeasurementName;
import com.yqg.core.service.monitor.RetentionPolicies;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import org.influxdb.dto.Point;
import org.springframework.stereotype.Service;

@Service
public class BankCardUniqueKeyCheckMonitorService extends BaseMonitorService {

  public void log(
      Long userId,
      SDKType sdkType,
      BankType bankType,
      String accountNumber,
      PaymentBusinessName businessName,
      boolean isBankCardUniqueKeyWithBankCode,
      boolean recordIsNull
      ) {
    writePoint(() -> Point.measurement(MonitorMeasurementName.BANK_CARD_UNIQUE_KEY_CHECK.name)
        .tag("sdkType", sdkType.code)
        .tag("bankType", bankType.name())
        .tag("businessName", businessName.code)
        .tag("uniqueKeyWithBankCode", isBankCardUniqueKeyWithBankCode ? "T" : "F")
        .tag("recordIsNull", recordIsNull ? "T" : "F")
        .addField("userId", userId)
        .addField("accountNumber", accountNumber)
        .build(), RetentionPolicies.ONE_MONTH);
  }
}
