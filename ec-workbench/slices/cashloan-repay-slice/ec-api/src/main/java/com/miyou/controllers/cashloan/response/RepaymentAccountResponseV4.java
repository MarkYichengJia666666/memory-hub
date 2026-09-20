package com.miyou.controllers.cashloan.response;


import com.miyou.controllers.cashloan.response.repayment.UserRepaymentResponse;
import com.miyou.controllers.directdebit.response.DirectDebitAccountListResponse;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RepaymentAccountResponseV4 {

  public UserRepaymentResponse userRepayment;
  // 二维码列表
  public RepaymentAccountResponse qrCode;
  // VA和电子钱包列表
  public List<RepaymentAccountResponse> virtualAccountList;
  // 代扣列表
  public DirectDebitAccountListResponse directDebitAccount;
  //对VA、电子钱包、代扣的渠道进行分组
  public Map<String, Object> repaymentChannels;
  public String expResult;

  public static RepaymentAccountResponseV4 from(UserRepaymentResponse userRepayment, RepaymentAccountResponse qrCode,
      List<RepaymentAccountResponse> virtualAccountList, DirectDebitAccountListResponse directDebitAccountList,
      Map<String, Object> repaymentChannels, RepayStyleVersion repayStyleVersion) {
    RepaymentAccountResponseV4 responseV4 = new RepaymentAccountResponseV4();
    responseV4.userRepayment = userRepayment;
    responseV4.qrCode = qrCode;
    responseV4.virtualAccountList = virtualAccountList;
    responseV4.directDebitAccount = directDebitAccountList;
    responseV4.repaymentChannels = repaymentChannels;
    responseV4.expResult = repayStyleVersion == RepayStyleVersion.V2 ? "C" : "A";
    return responseV4;
  }
}
