package com.miyou.controllers.kite.model;

import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.BindCardRequest;
import com.yqg.risk.feature.platform.client.model.builder.KiteModelBuilder;
import org.springframework.stereotype.Component;

@Component
public class BindCardRequestModelBuilder extends AbstractMysqlEcLoanModel {
  @Override
  public KiteModelBuilder getFieldMapper() {
    final BindCardRequest bindCardRequest = Tables.BIND_CARD_REQUEST;
    return KiteModelBuilder
        .create(bindCardRequest, request -> bindCardRequest.USER_ID.eq(request.getUserId()).and(bindCardRequest.TIME_CREATED.lt(request.getTimestamp())))
        .addByField(bindCardRequest.ID)
        .addByField(bindCardRequest.USER_ID)
        .addByField(bindCardRequest.ACCOUNT_NUMBER)
        .addByField(bindCardRequest.BANK_CODE)
        .addByField(bindCardRequest.SDK_TYPE)
        .addLongAsDateTime(bindCardRequest.TIME_CREATED)
        ;
  }

  @Override
  public String getTableName() {
    return "bind_card_request";
  }

  @Override
  public String getTableDesc() {
    return "用户绑卡请求日志表";
  }
}
