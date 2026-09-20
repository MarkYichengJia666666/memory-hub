package com.yqg.core.service.cashloan.repayment.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yqg.core.service.cashloan.enums.RepayCodeShowType;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.vo.AccountRelatedInfo;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * 还款方式 Created by shihao on 17/12/26.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CashLoanRepaymentAccountResVO implements RepaymentAccountResVO {

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
  private TT tag;
  // 兼容qrcode
  private String qrCodeLink;
  private Long qrCodeExpireTime;

  //墨西哥新增还款账号分类
  private Integer repayChannelType;
  private String repayChannelTypeDesc;
  private String contentUrl;
  private String bankName;
  private String repayNeedTimeDesc;

  private BigDecimal repayChannelDeductAmount;
  private String repayChannelDeductDesc;
  private BigDecimal repayCodeAmount;
  private Boolean subChannelTag;
  private String subChannelLogoUrl;
  private Boolean haveUsedTag;
  private BigDecimal channelLimitAmount;
  private Long expireTime;
  private String expireTimeWithFormat;
  private RepayCodeShowType repayCodeShowType;
  private String jumpMapPageUrl;

  private CashLoanRepaymentAccountResVO() {
  }

  public static CashLoanRepaymentAccountResVO fromQrCode(DynamicAccountChannel channel, List<Mode> modes, String qrCodeLink, Long qrCodeExpireTime) {
    CashLoanRepaymentAccountResVO response = new CashLoanRepaymentAccountResVO();
    response.channelType = channel.name();
    response.channelName = TT.gen(channel.desc);
    response.qrCodeLink = qrCodeLink;
    response.qrCodeExpireTime = qrCodeExpireTime;
    response.modes = modes;
    return response;
  }

  public static CashLoanRepaymentAccountResVO from(VirtualAccountResVO data, List<Mode> modes, List<RelatedInfo> relatedInfo) {
    CashLoanRepaymentAccountResVO response = new CashLoanRepaymentAccountResVO();
    response.accountNumber = data.getVirtualAccountNumber();
    response.channelType = data.getBankType();
    response.channelName = data.getBankName();
    response.logoUrl = data.getLogoUrl();
    response.modes = modes;
    response.relatedInfo = relatedInfo;
    return response;
  }

  public static CashLoanRepaymentAccountResVO from(VirtualAccountResVO data, List<Mode> modes, List<RelatedInfo> relatedInfo,
      Boolean canCopy) {
    CashLoanRepaymentAccountResVO response = from(data, modes, relatedInfo);
    response.canCopy = canCopy;
    return response;
  }

  public static CashLoanRepaymentAccountResVO from(VirtualAccountResVO data, List<Mode> modes, List<RelatedInfo> relatedInfo,
      Boolean canCopy, RepaymentAccountVO repaymentAccountVO) {
    CashLoanRepaymentAccountResVO response = from(data, modes, relatedInfo);
    response.canCopy = canCopy;
    response.bankName = repaymentAccountVO.getBankName();
    response.repayNeedTimeDesc = repaymentAccountVO.getRepayNeedTimeDesc();
    return response;
  }

  public static CashLoanRepaymentAccountResVO from(RepaymentAccountVO accountVO, String logoUrl) {
    CashLoanRepaymentAccountResVO response = new CashLoanRepaymentAccountResVO();
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
    return response;
  }

  public static CashLoanRepaymentAccountResVO from(RepaymentAccountVO accountVO, String logoUrl, Boolean canCopy) {
    CashLoanRepaymentAccountResVO response = from(accountVO, logoUrl);
    response.canCopy = canCopy;
    return response;
  }

  public static CashLoanRepaymentAccountResVO fromOtherResponse(String logoUrl) {
    CashLoanRepaymentAccountResVO response = new CashLoanRepaymentAccountResVO();
    response.channelType = "OTHER";
    response.channelName = TT.gen("其它银行");
    response.logoUrl = logoUrl;
    return response;
  }

  public static CashLoanRepaymentAccountResVO fromOtherResponse(String logoUrl, List<Mode> modes) {
    CashLoanRepaymentAccountResVO response = new CashLoanRepaymentAccountResVO();
    response.channelType = "OTHER";
    response.channelName = TT.gen("其它银行");
    response.logoUrl = logoUrl;
    response.modes = modes;
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
}
