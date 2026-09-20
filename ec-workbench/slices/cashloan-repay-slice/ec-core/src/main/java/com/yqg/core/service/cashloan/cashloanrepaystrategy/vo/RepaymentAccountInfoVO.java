package com.yqg.core.service.cashloan.cashloanrepaystrategy.vo;

import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.payment.pp.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentAccountInfoVO {
  private List<RepaymentAccountInfo> repaymentAccountInfoList;


  public static RepaymentAccountInfoVO from(List<RepaymentAccountVO> vos) {
    RepaymentAccountInfoVO res = new RepaymentAccountInfoVO();
    List<RepaymentAccountInfo> infos = vos.stream()
        .filter(item -> StringUtils.isNotBlank(item.getAccount()))
        .map(item -> {
          return RepaymentAccountInfo.builder()
              .account(item.getAccount())
              .provider(item.getProvider())
              .transNo(item.getRequestId())
              .channel(item.getChannel())
              .build();
        }).collect(Collectors.toList());
    res.setRepaymentAccountInfoList(infos);
    return res;
  }


  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RepaymentAccountInfo {
    private String account;
    private PaymentProvider provider;
    private String transNo;
    private String channel;
  }
}
