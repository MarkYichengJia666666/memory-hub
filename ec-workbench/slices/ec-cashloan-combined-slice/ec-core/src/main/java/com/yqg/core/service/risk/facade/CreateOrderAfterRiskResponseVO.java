package com.yqg.core.service.risk.facade;

import lombok.Data;

@Data
public class CreateOrderAfterRiskResponseVO {
  public Boolean retrievalAcceptAndTryCreateOrder = false;
  public Boolean createOrderSuccess = false;

  public static CreateOrderAfterRiskResponseVO from(Boolean retrievalAcceptAndTryCreateOrder, Boolean createOrderSuccess) {
    CreateOrderAfterRiskResponseVO vo = new CreateOrderAfterRiskResponseVO();
    vo.retrievalAcceptAndTryCreateOrder = retrievalAcceptAndTryCreateOrder;
    vo.createOrderSuccess = createOrderSuccess;
    return vo;
  }
}
