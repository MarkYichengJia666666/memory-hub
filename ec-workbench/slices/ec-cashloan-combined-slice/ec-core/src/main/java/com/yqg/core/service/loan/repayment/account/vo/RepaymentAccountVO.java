package com.yqg.core.service.loan.repayment.account.vo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountType;
import com.yqg.core.service.payment.ICredential;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.pmenum.VirtualAccountChannel;
import com.yqg.core.service.payment.pm.vo.AccountRelatedInfo;
import com.yqg.core.service.payment.pm.vo.DynamicAccountVO;
import com.yqg.core.service.payment.pm.vo.StaticVirtualAccountVO;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.serialization.JsonUtils;
import jodd.util.StringUtil;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
public class RepaymentAccountVO implements ICredential {
  private Long id;
  private Long userId;
  private String account;
  private String channel;
  private Boolean canBeUsed = true;
  private String channelDesc;
  private PaymentProvider provider;
  private PaymentAccount paymentAccount;
  private RepaymentAccountType type;
  private String requestId;
  private CurrencyAmount currencyAmount;

  //兼容催收展示逻辑
  private String virtualAccount;
  private String accountName;
  private String ifsc;

  //墨西哥添加还款渠道大类
  private Integer repayChannelType;
  private String repayChannelTypeDesc;
  private String contentUrl;
  private String bankName;
  private BigDecimal channelLimitAmount;
  private String repayNeedTimeDesc;

  private BigDecimal repayChannelDeductAmount;
  private String repayChannelDeductDesc;

  private Boolean subChannelTag;
  private String subChannelLogoUrl;
  private Boolean haveUsedTag;
  private Long expireTime;
  private String jumpMapPageUrl;

  private Map<AccountRelatedInfo.Type, String> relatedInfo;

  public static RepaymentAccountVO from(StaticVirtualAccountVO vo) {
    return from(vo, null);
  }

  public static RepaymentAccountVO from(String channel, RepaymentAccountDisplayConfig config) {
    RepaymentAccountVO accountVO = new RepaymentAccountVO();
    accountVO.setType(config.type);
    accountVO.setChannel(channel);
    accountVO.setProvider(config.provider);
    accountVO.setCanBeUsed(config.canBeUsed);
    accountVO.setRepayNeedTimeDesc(config.repayNeedTimeDesc);
    return accountVO;
  }

  /**
   * @param vo
   * @param config  墨西哥使用
   * @return
   */
  public static RepaymentAccountVO from(StaticVirtualAccountVO vo, RepaymentAccountDisplayConfig config) {
    if (vo == null) {
      return null;
    }

    RepaymentAccountVO accountVO = new RepaymentAccountVO();
    accountVO.id = vo.getId();
    accountVO.userId = vo.getUserId();
    accountVO.account = vo.getVirtualAccount();
    accountVO.virtualAccount = vo.getVirtualAccount();
    accountVO.channel = vo.getChannel().name();
    accountVO.channelDesc = vo.getChannel().desc;
    accountVO.provider = vo.getProvider();
    accountVO.paymentAccount = vo.getPaymentAccount();
    accountVO.type = RepaymentAccountType.STATIC;
    if (config != null){
      accountVO.bankName = config.bankName;
      accountVO.repayNeedTimeDesc = config.repayNeedTimeDesc;
    }
    return accountVO;
  }

  public static RepaymentAccountVO from(DynamicAccountVO vo){
    return from(vo, null);
  }

  public static RepaymentAccountVO from(DynamicAccountVO vo, String repayNeedTimeDesc) {
    if (vo == null) {
      return null;
    }

    RepaymentAccountVO accountVO = new RepaymentAccountVO();
    accountVO.id = vo.getId();
    accountVO.userId = vo.getUserId();
    accountVO.account = vo.account;
    accountVO.channel = vo.channel.name();
    accountVO.channelDesc = vo.channel.desc;
    accountVO.provider = vo.provider;
    accountVO.paymentAccount = vo.paymentAccount;
    accountVO.type = RepaymentAccountType.DYNAMIC;
    accountVO.requestId = vo.requestId;
    accountVO.currencyAmount = vo.currencyAmount;
    accountVO.expireTime = vo.expireTime;
    if (StringUtil.isNotBlank(vo.relatedInfo)) {
      accountVO.relatedInfo = JsonUtils.from(vo.relatedInfo, new TypeReference<HashMap<AccountRelatedInfo.Type, String>>() {
      });
    }
    accountVO.repayNeedTimeDesc = repayNeedTimeDesc;
    return accountVO;
  }

  public static RepaymentAccountVO fromWithoutAccount(Long userId,
                                                      PaymentAccount paymentAccount,
                                                      PaymentProvider provider,
                                                      String channel,
                                                      RepaymentAccountType type) {
    RepaymentAccountVO vo = new RepaymentAccountVO();
    vo.userId = userId;
    vo.paymentAccount = paymentAccount;
    vo.provider = provider;
    vo.type = type;
    vo.channel = channel;
    return vo;
  }

  public String getChannelDesc() {
    switch (type) {
      case DYNAMIC:
        return DynamicAccountChannel.valueOf(channel).desc;
      case STATIC:
        return VirtualAccountChannel.valueOf(channel).desc;
      default:
        throw EcException.error(EcExceptionType.CASH_LOAN_UNSUPPORTED_REPAYMENT_ACCOUNT_TYPE,
            "unsupported repaymentAccountType : " + type);
    }
  }
}
