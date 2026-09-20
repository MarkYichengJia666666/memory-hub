package com.yqg.core.service.cashloan.auth;

import com.yqg.core.model.core.ThreadTransactionalModel;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.loan.infos.BindCardInfo;
import com.yqg.core.service.payment.ICredential;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.util.env.EnvironmentInfo;
import com.yqg.core.util.env.TerminalInfo;
import com.yqg.ec.common.enums.LoanInfoLoanUse;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author: ListenYoung
 * @date: Created on 13:41 2021/11/12
 * @modified By:
 */

@Slf4j
@Service
public class AuthBindCardService {
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private LoanAccountDetailsService loanAccountDetailsService;
  @Autowired
  private ThreadTransactionalModel threadTransactionalModel;

  public void updateBindCardInfo(
      Long userId,
      SDKType sdkType,
      Long accountId,
      EnvironmentInfo environmentInfo,
      TerminalInfo terminalInfo,
      LoanInfoLoanUse loanInfoLoanUse,
      String relativeId,
      SourceType sourceType
  ) {
    verify(userId, relativeId, sdkType);
    BindCardInfo bindCardInfo = new BindCardInfo();
    bindCardInfo.environmentInfo = environmentInfo;
    bindCardInfo.terminalInfo = terminalInfo;
    bindCardInfo.relativeId = relativeId;
    threadTransactionalModel.transaction(configuration -> {
      loanAccountDetailsService.writeToMongo(userId, accountId, sourceType, pojo -> {
        pojo.setBindCardInfo(bindCardInfo);
        if (loanInfoLoanUse != null) {
          pojo.setLoanInfoLoanUse(loanInfoLoanUse);
        }
      });
    });
  }

  private void verify(Long userId, String relativeId, SDKType sdkType) {
    Map<PaymentMethod, List<ICredential>> credentialInfo = paymentService.getPayoutCredentialInfo(userId, sdkType);
    if (MapUtils.isEmpty(credentialInfo)) {
      throw EcException.warn(EcExceptionType.LOAN_BANK_ACCOUNT_BIND_CARD_FAILED, TT.gen("绑卡失败"));
    }
  }
}
