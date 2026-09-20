package com.miyou.controllers.cashloan.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yqg.core.service.cashloan.enums.RepayCodeShowType;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.model.sql.bankaccount.enums.BankType;
import com.yqg.core.service.directdebit.enums.DirectDebitProvider;
import com.yqg.core.service.directdebit.enums.EWalletRepayNextAction;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.vo.AccountRelatedInfo;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * 还款方式 Created by shihao on 17/12/26.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepaymentAccountResponse {

  // 兼容channel
  private String channelType;
  private String accountNumber;
  // 兼容channel
  private TT channelName;
  private String logoUrl;
  private List<Mode> modes;
  private List<RelatedInfo> relatedInfo;
  private Boolean canCopy = false;
  private Boolean canBeUsed = true;
  @Setter
  private TT userCount;
  @Setter
  private Boolean recommend = false;
  private TT tag;
  private String paymentMethod;
  //墨西哥新增还款账号分类
  private Integer repayChannelType;
  private String repayChannelTypeDesc;
  private String contentUrl;
  private String repayNeedTimeDesc;

  private BigDecimal repayChannelDeductAmount;
  private String repayChannelDeductDesc;

  /**
   * 命中电子钱包直连时返回；未命中时为 null（与线上一致）。
   */
  private EWalletRepayNextAction nextAction;
  private BankType bankType;
  private DirectDebitProvider provider;
  private String repaymentPaymentProvider;
  private Long accountId;

  private RepaymentAccountResponse() {
  }

  /**
   * 理财侧虚拟账户还款展示。
   */
  public static RepaymentAccountResponse fromFinancingVirtualAccount(VirtualAccountResponse data, List<Mode> modes,
      List<RelatedInfo> relatedInfo) {
    RepaymentAccountResponse response = new RepaymentAccountResponse();
    response.accountNumber = data.getVirtualAccountNumber();
    response.channelType = data.getBankType();
    response.channelName = data.getBankName();
    response.logoUrl = data.getLogoUrl();
    response.modes = modes;
    response.relatedInfo = relatedInfo;
    return response;
  }

  /**
   * 二维码
   */
  public static RepaymentAccountResponse fromRepaymentAccountForQrCode(RepaymentAccountVO accountVO, String logoUrl) {
    return fromRepaymentAccountForVirtualAccount(accountVO, logoUrl, null);
  }

  /**
   * VA 和 电子钱包非直连
   */
  public static RepaymentAccountResponse fromRepaymentAccountForVirtualAccount(RepaymentAccountVO accountVO, String logoUrl,
      Boolean canCopy) {
    return fromRepaymentAccountWithEWalletDirectDebit(accountVO, logoUrl, canCopy, null, null, null, null);
  }

  /**
   * 电子钱包直连
   */
  public static RepaymentAccountResponse fromRepaymentAccountWithEWalletDirectDebit(RepaymentAccountVO accountVO, String logoUrl,
      Boolean canCopy, EWalletRepayNextAction nextAction, BankType bankType, DirectDebitProvider provider, Long linkAccountId) {
    RepaymentAccountResponse response = new RepaymentAccountResponse();
    response.channelType = accountVO.getChannel();
    response.logoUrl = logoUrl;
    response.channelName = TT.gen(accountVO.getChannelDesc());

    response.canBeUsed = accountVO.getCanBeUsed();
    if (!accountVO.getCanBeUsed()) {
      response.tag = TT.gen("正在维护");
    }
    response.repayChannelType = accountVO.getRepayChannelType();
    response.repayChannelTypeDesc = accountVO.getRepayChannelTypeDesc();
    response.contentUrl = accountVO.getContentUrl();
    response.repayChannelDeductAmount = accountVO.getRepayChannelDeductAmount();
    response.repayChannelDeductDesc = accountVO.getRepayChannelDeductDesc();
    response.repayNeedTimeDesc = accountVO.getRepayNeedTimeDesc();
    response.paymentMethod = DynamicAccountChannel.getOpPaymentMethod(accountVO.getChannel());
    response.canCopy = canCopy;
    response.nextAction = nextAction;
    response.bankType = bankType;
    response.provider = provider;
    response.repaymentPaymentProvider = accountVO.getProvider() != null
        ? accountVO.getProvider().name()
        : PaymentProvider.NONE.name();
    response.accountId = linkAccountId;
    return response;
  }

  /**
   * 其它银行入口（无具体 VA），展示条件与 SeaBank 渠道一致：仅实验组 B。
   */
  public static RepaymentAccountResponse fromOtherResponse(String logoUrl) {
    RepaymentAccountResponse response = new RepaymentAccountResponse();
    response.channelType = RepaymentAccountConfig.CHANNEL_OTHER_BANK;
    response.channelName = TT.gen("其它银行");
    response.logoUrl = logoUrl;
    return response;
  }

  @Data
  public static class Mode {

    private String mode;
    private String desc;

    public Mode(String mode, String desc) {
      this.mode = mode;
      this.desc = desc;
    }
  }

  @Data
  @AllArgsConstructor
  public static class RelatedInfo {

    private AccountRelatedInfo.Type type;
    private String data;
    private boolean internalOpen = false;
  }

  public PaymentProvider resolveRepaymentPaymentProvider() {
    if (repaymentPaymentProvider == null) {
      return PaymentProvider.NONE;
    }
    try {
      return PaymentProvider.valueOf(repaymentPaymentProvider);
    } catch (IllegalArgumentException ex) {
      return PaymentProvider.NONE;
    }
  }
}
