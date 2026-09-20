package com.yqg.core.service.cashloan.auth.processor;

import com.yqg.core.model.sql.loan.account.enums.AuthStep;
import com.yqg.core.service.cashloan.auth.AuthBindCardService;
import com.yqg.core.service.loan.account.form.UploadBinkCardForm;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by jpdu on 2017/8/30.
 */
@Component
public class BindCardProcessor extends AbstractAuthProcessor<UploadBinkCardForm> {
  @Autowired
  private AuthBindCardService authBindCardService;

  @Override
  protected AuthStep getAuthStep() {
    return AuthStep.BANK_CARD_NEW;
  }

  @Override
  protected void processAuth(LoanApiViewerContext context, UploadBinkCardForm form) {
    authBindCardService.updateBindCardInfo(
        context.userId,
        context.sdkType,
        context.loanAccountId,
        context.environmentInfo,
        context.terminalInfo,
        form == null ? null : form.loanInfoLoanUse,
        form == null ? null : form.relativeId,
        context.sourceType
    );
  }
}
