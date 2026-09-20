package com.miyou.controllers.cashloan.service;

import com.miyou.controllers.cashloan.response.RepaymentAccountResponse;
import com.yqg.core.common.UserFlowConstants;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.cashloan.CashLoanConfig;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.directdebit.DirectDebitAccountService;
import com.yqg.core.service.directdebit.enums.DirectDebitProvider;
import com.yqg.core.service.directdebit.enums.DirectDebtDisplayStrategy;
import com.yqg.core.service.directdebit.enums.EWalletRepayNextAction;
import com.yqg.core.service.directdebit.vo.DirectDebitLinkAccountVO;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.ec.common.constant.ExperimentKeyConstants;
import com.yqg.ec.common.enums.SDKType;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 将还款渠道 {@link RepaymentAccountVO} 装配为 API 层 {@link RepaymentAccountResponse}， 包含电子钱包直连等逻辑。
 */
@Service
public class RepaymentAccountResponseBuilder {

  @Autowired
  private CashLoanConfig cashLoanConfig;
  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  private DirectDebitAccountService directDebitAccountService;
  @Autowired
  private ExpDiversionClient expDiversionClient;

  public List<RepaymentAccountResponse> buildFromAccountVos(List<RepaymentAccountVO> repaymentAccountVos, Long userId, SDKType sdkType,
      Long build, RepayStyleVersion repayStyleVersion) {
    return repaymentAccountVos.stream().map(accountVO -> buildOne(accountVO, userId, sdkType, build, repayStyleVersion))
        .collect(Collectors.toList());
  }

  /**
   * 将单笔 {@link RepaymentAccountVO} 转为 {@link RepaymentAccountResponse}（含电子钱包直连装配）。
   */
  public RepaymentAccountResponse buildOne(RepaymentAccountVO accountVO, Long userId, SDKType sdkType, Long build,
      RepayStyleVersion repayStyleVersion) {
    String channel = accountVO.getChannel();
    String logoUrl = cashLoanConfig.channelLogoMap(repayStyleVersion, channel);
    Boolean canCopy = repaymentAccountConfig.getEnableCopyWithBuild(channel, build);
    RepaymentAccountResponse eWalletResponse = tryBuildEWalletDirectDebitResponse(channel, accountVO, logoUrl, canCopy, userId, sdkType);
    return eWalletResponse != null ? eWalletResponse
        : RepaymentAccountResponse.fromRepaymentAccountForVirtualAccount(accountVO, logoUrl, canCopy);
  }

  /**
   * 尝试构建电子钱包直连还款响应。
   *
   * @return 实验命中且 channel 为已配置电子钱包时返回对应 response；否则返回 null，由调用方降级为虚拟账户。
   */
  @Nullable
  private RepaymentAccountResponse tryBuildEWalletDirectDebitResponse(String channel, RepaymentAccountVO accountVO, String logoUrl,
      Boolean canCopy, Long userId, SDKType sdkType) {
    DynamicAccountChannel channelEnum = DynamicAccountChannel.fromNameOrNull(channel);
    if (channelEnum == null) {
      return null;
    }
    BankType bankType;
    DirectDebitProvider provider;
    switch (channelEnum) {
      case SHOPEE:
        if (!UserFlowConstants.isExperimentGroup(expDiversionClient.getResult(ExperimentKeyConstants.EC_SHOPEEPAY_DIRECT))) {
          return null;
        }
        bankType = BankType.SHOPEE_PAY;
        provider = DirectDebitProvider.SHOPEE_PAY;
        break;
      case DANA:
        if (!directDebitAccountService.userHasDanaExperience(userId, sdkType)) {
          return null;
        }
        if (!DirectDebtDisplayStrategy.REPAY_ANYTIME.name().equals(expDiversionClient.getResult(ExperimentKeyConstants.DANA_REPAY_DIRECT_DEBIT_KEY))) {
          return null;
        }
        bankType = BankType.DANA;
        provider = DirectDebitProvider.DANA;
        break;
      default:
        return null;
    }
    DirectDebitLinkAccountVO debitLinkAccountVO = directDebitAccountService.getDirectDebitLinkAccountVO(userId, sdkType, provider, bankType);
    EWalletRepayNextAction action = debitLinkAccountVO == null ? EWalletRepayNextAction.LINK : EWalletRepayNextAction.PAY;
    Long linkedAccountId = debitLinkAccountVO == null ? null : debitLinkAccountVO.id;
    return RepaymentAccountResponse.fromRepaymentAccountWithEWalletDirectDebit(accountVO, logoUrl, canCopy, action, bankType, provider,
        linkedAccountId);
  }
}
