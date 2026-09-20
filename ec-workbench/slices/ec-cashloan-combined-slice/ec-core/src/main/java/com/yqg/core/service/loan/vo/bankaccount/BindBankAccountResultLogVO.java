package com.yqg.core.service.loan.vo.bankaccount;

import com.yqg.core.model.sql.bankaccount.enums.BankAccountAvailableStatus;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.cashloan.vo.enums.FinalValidationBankAccountStatus;
import com.yqg.core.service.cashloan.vo.enums.ValidationCardStatus;
import com.yqg.ec.common.enums.SDKType;

/**
 * @author chaoye
 * @date 2023/9/22
 */
public class BindBankAccountResultLogVO {
  public Boolean canModifyName;
  public Long bankAccountId;
  public Long userId;
  public BindBankAccountResultLogApi api;
  public BankType bankType;
  public BankAccountAvailableStatus bankAccountAvailableStatus;
  public FinalValidationBankAccountStatus newPageStatus;
  public ValidationCardStatus oldPageStatus;
  public String exceptionMessage;
  public String ip;
  public String deviceToken;
  public Long build;
  public SDKType sdkType;
  public String platformType;
  public String userAgent;
  public String deviceId;
  public Long loanAccountId;


  public static BindBankAccountResultLogVO fromNewPage(
      BankAccountAvailableStatus bankAccountAvailableStatus,
      FinalValidationBankAccountStatus newPageStatus,
      Boolean canModifyName,
      Long bankAccountId,
      Long userId,
      BankType bankType,
      BindBankAccountResultLogApi api,
      String exceptionMessage,
      String ip,
      String deviceToken,
      String platformType,
      Long build,
      SDKType sdkType,
      String userAgent,
      String deviceId,
      Long loanAccountId) {
    BindBankAccountResultLogVO resultLogVO = from(
        bankAccountAvailableStatus,
        canModifyName,
        bankAccountId,
        userId,
        bankType,
        api,
        exceptionMessage,
        ip,
        deviceToken,
        platformType,
        build,
        sdkType,
        userAgent,
        deviceId,
        loanAccountId);

    resultLogVO.newPageStatus = newPageStatus;
    return resultLogVO;
  }

  public static BindBankAccountResultLogVO fromOldPage(
      BankAccountAvailableStatus bankAccountAvailableStatus,
      ValidationCardStatus oldPageStatus,
      Boolean canModifyName,
      Long bankAccountId,
      Long userId,
      BankType bankType,
      BindBankAccountResultLogApi api,
      String exceptionMessage,
      String ip,
      String deviceToken,
      String platformType,
      Long build,
      SDKType sdkType,
      String userAgent,
      String deviceId,
      Long loanAccountId) {
    BindBankAccountResultLogVO resultLogVO = from(
        bankAccountAvailableStatus,
        canModifyName,
        bankAccountId,
        userId,
        bankType,
        api,
        exceptionMessage,
        ip,
        deviceToken,
        platformType,
        build,
        sdkType,
        userAgent,
        deviceId,
        loanAccountId);

    resultLogVO.oldPageStatus = oldPageStatus;
    return resultLogVO;
  }

  public static BindBankAccountResultLogVO from(
      BankAccountAvailableStatus bankAccountAvailableStatus,
      Boolean canModifyName,
      Long bankAccountId,
      Long userId,
      BankType bankType,
      BindBankAccountResultLogApi api,
      String exceptionMessage,
      String ip,
      String deviceToken,
      String platformType,
      Long build,
      SDKType sdkType,
      String userAgent,
      String deviceId,
      Long loanAccountId) {
    BindBankAccountResultLogVO resultLogVO = new BindBankAccountResultLogVO();
    resultLogVO.bankAccountAvailableStatus = bankAccountAvailableStatus;
    resultLogVO.canModifyName = canModifyName;
    resultLogVO.bankAccountId = bankAccountId;
    resultLogVO.userId = userId;
    resultLogVO.bankType = bankType;
    resultLogVO.api = api;
    resultLogVO.exceptionMessage = exceptionMessage;
    resultLogVO.ip = ip;
    resultLogVO.deviceToken = deviceToken;
    resultLogVO.platformType = platformType;
    resultLogVO.build = build;
    resultLogVO.sdkType = sdkType;
    resultLogVO.userAgent = userAgent;
    resultLogVO.deviceId = deviceId;
    resultLogVO.loanAccountId = loanAccountId;
    return resultLogVO;
  }
}
