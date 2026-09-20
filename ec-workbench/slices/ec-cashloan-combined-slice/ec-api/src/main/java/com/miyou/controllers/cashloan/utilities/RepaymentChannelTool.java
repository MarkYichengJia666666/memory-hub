package com.miyou.controllers.cashloan.utilities;

import com.miyou.controllers.cashloan.response.RepaymentAccountResponse;
import com.miyou.controllers.directdebit.response.DirectDebitAccountListResponse;
import com.yqg.core.model.generated.Tables;
import com.yqg.core.model.generated.tables.records.DynamicAccountRecord;
import com.yqg.core.model.sql.payment.DynamicAccountConditions;
import com.yqg.core.model.sql.payment.DynamicAccountModel;
import com.yqg.core.model.sql.payment.PaymentModel;
import com.yqg.core.model.sql.payment.StaticVirtualAccountModel;
import com.yqg.core.model.sql.payment.enums.PaymentStatus;
import com.yqg.core.model.sql.payment.enums.PaymentTransType;
import com.yqg.core.service.cashloan.repayment.enums.RepayStyleVersion;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.core.service.payment.PaymentBusinessNameMapper;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.pmenum.RepaymentChannelGroup;
import com.yqg.core.service.payment.pm.pmenum.VirtualAccountChannel;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.translation.client.utils.TT;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 基于最近一次还款类型和渠道，决定渠道分组返回结构。默认VA＞电子钱包＞代扣快捷支付
 */
@Slf4j
@Service
public class RepaymentChannelTool {

  @Autowired
  private PaymentService paymentService;
  @Autowired
  private PaymentModel paymentModel;
  @Autowired
  private DynamicAccountModel dynamicAccountModel;
  @Autowired
  private StaticVirtualAccountModel staticVirtualAccountModel;

  /**
   * 根据最近一次还款的渠道，决定返回哪种分组结构。
   */
  public Map<String, Object> buildGroupedChannelsByLatestChannel(Long userId, SDKType sdkType,
      List<RepaymentAccountResponse> repaymentAccountResponseList, DirectDebitAccountListResponse directDebitAccountList,
      RepayStyleVersion repayStyleVersion) {

    // 对VA和电子钱包分组（判空保护）
    Map<String, List<RepaymentAccountResponse>> groupedChannels = groupAccountChannelByPaymentMethod(repaymentAccountResponseList);

    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    PaymentVO paymentVO = paymentService.getLatestPayment(userId, businessName, PaymentTransType.REPAY, PaymentMethod.VA_AND_DD_PAYMENT_METHOD_LIST);

    // 默认顺序
    if (paymentVO == null) {
      return buildChannelGroupByFirstChannelType(directDebitAccountList, groupedChannels, RepaymentChannelGroup.VIRTUAL_ACCOUNT);
    }

    // 代扣快捷支付>VA>电子钱包（必须匹配第一条支持银行）
    if (PaymentMethod.DIRECT_DEBIT.equals(paymentVO.getPaymentMethod())) {
      return buildChannelGroupByFirstChannelType(directDebitAccountList, groupedChannels, RepaymentChannelGroup.DIRECT_DEBIT);
    }
    String channel = paymentService.getChannelNameFromPaymentVO(paymentVO);

    if (StringUtils.equals(channel, DynamicAccountChannel.XENDIT_QRIS.name())) {
      return buildChannelGroupByFirstChannelType(directDebitAccountList, groupedChannels, RepaymentChannelGroup.VIRTUAL_ACCOUNT);
    }

    if (CollectionUtils.isNotEmpty(repaymentAccountResponseList)) {
      RepaymentAccountResponse repaymentAccountResponse = repaymentAccountResponseList.get(0);
      if (Objects.equals(repaymentAccountResponse.getChannelType(), channel) && repayStyleVersion == RepayStyleVersion.V2) {
        // 统计该用户在该渠道下的成功还款次数
        Set<Long> credentialIds = getCredentialIdsByChannel(userId, businessName, channel);
        if (CollectionUtils.isEmpty(credentialIds)) {
          repaymentAccountResponse.setUserCount(TT.gen("使用{0}次", 0));
        } else {
          // 统计这些credential对应的成功还款记录数量
          int repayCount = paymentModel.countByCredentialIds(userId, businessName, PaymentTransType.REPAY, PaymentStatus.SUCCEED, credentialIds);
          repaymentAccountResponse.setUserCount(TT.gen("使用{0}次", repayCount));
        }
      }
    }

    // 非代扣：根据最近一次渠道所属分组，尝试优先该分组（要求分组存在且含该渠道），否则走默认顺序
    String preferGroup = resolvePaymentMethodByChannelType(channel);

    if (preferGroup != null && groupedChannels.containsKey(preferGroup)) {
      return buildChannelGroupByFirstChannelType(directDebitAccountList, groupedChannels, RepaymentChannelGroup.valueOf(preferGroup));
    }

    // 默认顺序
    return buildChannelGroupByFirstChannelType(directDebitAccountList, groupedChannels, RepaymentChannelGroup.VIRTUAL_ACCOUNT);
  }


  /**
   * 构建以指定渠道组优先的渠道分组
   * 根据 firstChannelType 决定优先顺序，其他组按默认顺序排列
   *
   * @param directDebitAccountList 代扣账户列表
   * @param groupedChannels 已分组的渠道列表
   * @param firstChannelType 优先展示的渠道组类型
   * @return 按指定顺序排列的渠道分组
   */
  private Map<String, Object> buildChannelGroupByFirstChannelType(
      DirectDebitAccountListResponse directDebitAccountList,
      Map<String, List<RepaymentAccountResponse>> groupedChannels,
      RepaymentChannelGroup firstChannelType) {

    // 根据指定的第一渠道类型确定排序
    switch (firstChannelType) {
      case E_WALLETS:
        // 电子钱包优先：EW > VA > DD
        return buildOrderedGroupingMap(groupedChannels,
            directDebitAccountList,
            RepaymentChannelGroup.E_WALLETS,
            RepaymentChannelGroup.VIRTUAL_ACCOUNT,
            RepaymentChannelGroup.DIRECT_DEBIT);

      case DIRECT_DEBIT:
        // 代扣优先：DD > VA > EW
        return buildOrderedGroupingMap(groupedChannels,
            directDebitAccountList,
            RepaymentChannelGroup.DIRECT_DEBIT,
            RepaymentChannelGroup.VIRTUAL_ACCOUNT,
            RepaymentChannelGroup.E_WALLETS);
      case VIRTUAL_ACCOUNT:
      default:
        // 默认虚拟账户优先：VA > EW > DD
        return buildOrderedGroupingMap(groupedChannels,
            directDebitAccountList,
            RepaymentChannelGroup.VIRTUAL_ACCOUNT,
            RepaymentChannelGroup.E_WALLETS,
            RepaymentChannelGroup.DIRECT_DEBIT);
    }
  }

  /**
   * 根据给定顺序构建分组Map。对于 DIRECT_DEBIT 从 directDebitAccountList 取值，其余从 groupedChannels 取值。
   */
  private Map<String, Object> buildOrderedGroupingMap(
      Map<String, List<RepaymentAccountResponse>> groupedChannels,
      DirectDebitAccountListResponse directDebitAccountList,
      RepaymentChannelGroup... order) {

    Map<String, Object> result = new LinkedHashMap<>();
    if (ArrayUtils.isEmpty(order)) {
      return result;
    }
    for (RepaymentChannelGroup group : order) {
      if (group == RepaymentChannelGroup.DIRECT_DEBIT) {
        if (directDebitAccountList != null) {
          result.put(group.name(), directDebitAccountList);
        }
      } else {
        List<RepaymentAccountResponse> list = groupedChannels.get(group.name());
        if (list != null) {
          result.put(group.name(), list);
        }
      }
    }
    return result;
  }

  /**
   * 按渠道类型分组
   */
  private Map<String, List<RepaymentAccountResponse>> groupAccountChannelByPaymentMethod(
      List<RepaymentAccountResponse> repaymentAccountResponseList) {

    if (repaymentAccountResponseList == null) {
      return new LinkedHashMap<>();
    }

    return repaymentAccountResponseList.stream()
        .collect(Collectors.groupingBy(
            account -> resolvePaymentMethodByChannelType(account.getChannelType()),
            LinkedHashMap::new,
            Collectors.toList()
        ));
  }

  /**
   * 根据渠道类型获取对应的支付方法类型
   */
  public String resolvePaymentMethodByChannelType(String channelType) {
    try {
      DynamicAccountChannel channel = DynamicAccountChannel.valueOf(channelType);
      String opPaymentMethod = channel.opPaymentMethod;
      if (RepaymentChannelGroup.isValid(opPaymentMethod)) {
        return opPaymentMethod;
      }
      return RepaymentChannelGroup.VIRTUAL_ACCOUNT.name();
    } catch (Exception e) {
      if (!RepaymentAccountConfig.CHANNEL_OTHER_BANK.equals(channelType)) {
        log.warn("Unknown channel type: {}, defaulting to VIRTUAL_ACCOUNT", channelType);
      }
      return RepaymentChannelGroup.VIRTUAL_ACCOUNT.name();
    }
  }

  /**
   * 根据渠道名称获取该用户该渠道的所有credential IDs
   * 同时查询DYNAMIC_ACCOUNT和STATIC_VIRTUAL_ACCOUNT表
   */
  private Set<Long> getCredentialIdsByChannel(Long userId, PaymentBusinessName businessName, String channelName) {
    Set<Long> credentialIds = new java.util.HashSet<>();
    
    // 查询DYNAMIC_ACCOUNT表
    try {
      DynamicAccountChannel dynamicChannel = DynamicAccountChannel.valueOf(channelName);
      DynamicAccountConditions conditions = new DynamicAccountConditions();
      conditions.userId = userId;
      conditions.channel = dynamicChannel;
      List<DynamicAccountRecord> dynamicRecords = dynamicAccountModel.getByConditions(conditions);
      // 过滤出匹配businessName的记录
      credentialIds.addAll(dynamicRecords.stream()
          .filter(r -> businessName.code.equals(r.getBusinessName()))
          .map(DynamicAccountRecord::getId)
          .collect(Collectors.toList()));
    } catch (IllegalArgumentException e) {
      // channel不在DynamicAccountChannel中，忽略
      log.debug("Channel {} not found in DynamicAccountChannel", channelName);
    }
    
    // 查询STATIC_VIRTUAL_ACCOUNT表
    try {
      VirtualAccountChannel virtualChannel = VirtualAccountChannel.valueOf(channelName);
      List<Long> staticCredentialIds = staticVirtualAccountModel.findCredentialIdsByChannel(userId, businessName, virtualChannel);
      credentialIds.addAll(staticCredentialIds);
    } catch (IllegalArgumentException e) {
      // channel不在VirtualAccountChannel中，忽略
      log.debug("Channel {} not found in VirtualAccountChannel", channelName);
    }
    
    return credentialIds;
  }
}
