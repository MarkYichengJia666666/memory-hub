package com.yqg.core.service.loan.bankaccount;

import com.yqg.common.util.type.BooleanType;
import com.yqg.core.service.cashloan.vo.enums.ValidationCardStatus;
import com.yqg.core.service.monitor.BaseMonitorService;
import com.yqg.core.service.monitor.MonitorMeasurementName;
import com.yqg.core.service.monitor.RetentionPolicies;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.dto.Point;
import org.springframework.stereotype.Service;

/**
 * @author yuchenghuang
 * @date 2021/12/16
 */
@Slf4j
@Service
public class LoanBankAccountMonitorService extends BaseMonitorService {
  public void logCreateOrderBankCardMatch(ValidationCardStatus status, Long userId, Boolean isMatch) {
    writePoint(() -> Point.measurement(MonitorMeasurementName.CREATE_ORDER_BANK_NAME_MATCH.name)
        .tag("isMatch", BooleanType.fromBoolean(isMatch).charCode)
        .tag("status", status.name())
        .addField("userId", userId)
        .build(), RetentionPolicies.TWO_MONTH);
  }
}
