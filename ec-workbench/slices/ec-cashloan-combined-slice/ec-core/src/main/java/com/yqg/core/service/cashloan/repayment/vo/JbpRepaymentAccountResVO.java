package com.yqg.core.service.cashloan.repayment.vo;

import com.yqg.core.service.cashloan.repayment.vo.CashLoanRepaymentAccountResVO.Mode;
import com.yqg.core.service.cashloan.repayment.vo.CashLoanRepaymentAccountResVO.RelatedInfo;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 还款方式 Created by Yicheng Jia.
 */
@Data
public class JbpRepaymentAccountResVO implements RepaymentAccountResVO {

  // 兼容channel
  private String channelType;
  private String accountNumber;
  private String deepLink;
  private TT channelName;
  private BigDecimal needPayAmount;

  private List<Mode> modes;
  private List<RelatedInfo> relatedInfo;
  private Boolean canCopy = false;
  private Boolean canBeUsed = true;
  private String logoUrl;
  private TT tag;

  public static JbpRepaymentAccountResVO from(RepaymentAccountVO repaymentAccountVO, BigDecimal needPayAmount, String deepLink, List<RelatedInfo> relatedInfo) {
    JbpRepaymentAccountResVO response = new JbpRepaymentAccountResVO();
    response.setAccountNumber(repaymentAccountVO.getAccount());
    response.setChannelType(repaymentAccountVO.getChannel());
    response.setChannelName(TT.gen(repaymentAccountVO.getChannelDesc()));
    response.setDeepLink(deepLink);
    response.setNeedPayAmount(needPayAmount);
    response.setRelatedInfo(relatedInfo);
    return response;
  }

  public static JbpRepaymentAccountResVO from(VirtualAccountResVO virtualAccountResVO, List<Mode> modes, List<RelatedInfo> relatedInfo,
      Boolean canCopy, RepaymentAccountVO repaymentAccountVO, BigDecimal needPayAmount, String deepLink) {
    JbpRepaymentAccountResVO response = new JbpRepaymentAccountResVO();
    response.setAccountNumber(repaymentAccountVO.getAccount());
    response.setChannelType(repaymentAccountVO.getChannel());
    response.setChannelName(TT.gen(repaymentAccountVO.getChannelDesc()));
    response.setDeepLink(deepLink);
    response.setNeedPayAmount(needPayAmount);
    response.setModes(modes);
    response.setRelatedInfo(relatedInfo);
    response.setCanCopy(canCopy);
    response.setCanBeUsed(repaymentAccountVO.getCanBeUsed());
    response.setLogoUrl(virtualAccountResVO.getLogoUrl());
    if (!repaymentAccountVO.getCanBeUsed()) {
      response.setTag(TT.gen("正在维护"));
    }
    return response;
  }
}
