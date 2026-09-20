package com.yqg.core.service.loan.bankaccount;

import com.yqg.core.configure.BindCardRequestConfig;
import com.yqg.core.model.sql.bankaccount.BindCardRequestModel;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.risk.feature.RiskConfig;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.overseasrisk.client.mesh.api.IBindCardRequestNormalService;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by jiewu on 19/08/22.
 */
@Service
@Slf4j
public class BindCardRequestService {
  @Autowired
  private BindCardRequestModel bindCardRequestModel;
  @Autowired
  private IBindCardRequestNormalService iBindCardRequestNormalService;
  @Autowired
  private BindCardRequestConfig bindCardRequestConfig;
  @Autowired
  private RiskConfig riskConfig;
  private static final int INIT_SUCCESS_CODE = 1;

  public void storeRequestInfo(Long userId, String accountNumber, BankType bankType, SDKType sdkType) {
    Pair<Integer, Long> executiondAndTimeCreatedPair = bindCardRequestModel.init(userId, accountNumber, bankType, sdkType);
    //插入成功 "1"，就同步信息到risk的表 bind_card_request_normal
    if (bindCardRequestConfig.checkNeedInsertBindCardRequestNormal() && executiondAndTimeCreatedPair.getKey() == INIT_SUCCESS_CODE) {
      try {
        if (riskConfig.getStopSaveDataToRiskEngine()) {
          throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("此功能正在维护中，预计5分钟后恢复，请稍后再试"));
        }
        iBindCardRequestNormalService.createBindCardRequestNormal(userId, accountNumber, bankType.name(), sdkType.name(), executiondAndTimeCreatedPair.getValue());
      } catch (Exception e) {
        log.error("BindCardRequestService#storeRequestInfo failed to call '/risk/bindCardRequestNormal/create'", e);
      }
    }
  }
}
