package com.yqg.core.service.loan.repayment.account;

import static com.yqg.core.util.scope.ImpliedContextUtils.requestClientType;
import static com.yqg.core.util.scope.ImpliedContextUtils.sourceType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yqg.core.common.enums.RequestClientType;
import com.yqg.core.model.generated.tables.records.LoanAccountRecord;
import com.yqg.core.model.generated.tables.records.PaymentRecord;
import com.yqg.core.model.loader.RepaymentAmountLoader;
import com.yqg.core.model.loader.RepaymentChannelAccountRouteConfigLoader;
import com.yqg.core.model.sql.loan.account.LoanAccountModel;
import com.yqg.core.model.sql.payment.DynamicAccountConditions;
import com.yqg.core.model.sql.payment.DynamicAccountModel;
import com.yqg.core.model.sql.payment.PaymentModel;
import com.yqg.core.model.sql.payment.StaticVirtualAccountModel;
import com.yqg.core.model.sql.payment.enums.DynamicAccountStatus;
import com.yqg.core.model.sql.payment.enums.PayEventType;
import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.JbpConfig;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.utilities.HomepageStatusTool;
import com.yqg.core.service.cashloan.ordercenter.CashLoanInstalmentService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.repay.CashLoanRepaymentService;
import com.yqg.core.service.cashloan.repay.vo.CashLoanRepaymentVO;
import com.yqg.core.service.cashloan.repayment.vo.CashLoanRepaymentAccountResVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.infos.AppInfoVO;
import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountCallerType;
import com.yqg.core.service.loan.repayment.account.enums.RepaymentAccountType;
import com.yqg.core.service.loan.repayment.account.vo.OVORepaymentInfoVO;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountDisplayConfig;
import com.yqg.core.service.loan.repayment.account.vo.RepaymentAccountVO;
import com.yqg.core.service.loan.repayment.experiment.RepaymentExperimentSupport;
import com.yqg.core.service.loan.vo.UserInfoVO;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.core.service.mobile.enums.MsgPriority;
import com.yqg.core.service.notif.NotifService;
import com.yqg.core.service.payment.PaymentAccount;
import com.yqg.core.service.payment.PaymentBusinessNameMapper;
import com.yqg.core.service.payment.PaymentService;
import com.yqg.core.service.payment.pm.DynamicAccountPaymentMethod;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.pm.VirtualAccountPaymentMethod;
import com.yqg.core.service.payment.pm.pmenum.DynamicAccountChannel;
import com.yqg.core.service.payment.pm.pmenum.VirtualAccountChannel;
import com.yqg.core.service.payment.pm.vo.DynamicAccountVO;
import com.yqg.core.service.payment.pm.vo.ReceiptCredentialContext;
import com.yqg.core.service.payment.pm.vo.StaticVirtualAccountVO;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.core.service.payment.vo.PaymentQueryCondition;
import com.yqg.core.service.payment.vo.PaymentVO;
import com.yqg.core.service.tool.IdGeneratorService;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.UserSimpleInfoVO;
import com.yqg.core.util.EcHashUtil;
import com.yqg.core.util.common.RenamedThreadFactory;
import com.yqg.ec.common.constant.ExperimentKeyConstants;
import com.yqg.ec.common.enums.PaymentBusinessName;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.exception.EcExceptionType;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.i18n.CurrencyAmount;
import com.yqg.ec.common.i18n.EcCurrency;
import com.yqg.ec.common.i18n.mobile.MobileConverter;
import com.yqg.ec.common.i18n.time.Clock;
import com.yqg.ec.common.serialization.JsonUtils;
import com.yqg.overseas.client.spring.api.payment.IOverseasRepaymentRouteService;
import com.yqg.overseas.spring.response.receipt.RepaymentChannelRouteResponse;
import com.yqg.translation.client.utils.TT;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by wenbincao on 20/03/16.
 */
@Service
@Slf4j
public class RepaymentAccountService {

  @Autowired
  private VirtualAccountPaymentMethod virtualAccountPaymentMethod;
  @Autowired
  private DynamicAccountPaymentMethod dynamicAccountPaymentMethod;
  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;
  @Autowired
  private CashLoanRepaymentService repaymentService;
  @Autowired
  private IdGeneratorService idGeneratorService;
  @Autowired
  private RepaymentAccountMonitorService repaymentAccountMonitorService;
  @Autowired
  private RepaymentAmountLoader amountLoader;
  @Autowired
  private RepaymentAccountSendTimesLoader repaymentAccountSendTimesLoader;
  @Autowired
  private RepaymentAccountSendLocker repaymentAccountSendLocker;
  @Autowired
  private NotifService notifService;
  @Autowired
  private UserService userService;
  @Autowired
  private EcOrderService ecOrderService;
  @Autowired
  private LoanAccountModel accountModel;
  @Autowired
  private IOverseasRepaymentRouteService repaymentRouteService;
  @Autowired
  private RepaymentChannelAccountRouteConfigLoader routeConfigLoader;
  @Autowired
  private PaymentService paymentService;
  @Autowired
  private PaymentModel paymentModel;
  @Autowired
  private StaticVirtualAccountModel staticVirtualAccountModel;
  @Autowired
  private DynamicAccountModel dynamicAccountModel;
  @Autowired
  private JbpConfig jbpConfig;
  @Autowired
  private LoanBankAccountService loanBankAccountService;
  @Autowired
  private RepaymentExperimentSupport repaymentExperimentSupport;
  @Autowired
  private ExpDiversionClient expDiversionClient;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private HomepageStatusTool homepageStatusTool;
  @Autowired
  private PaymentProviderSelectorService paymentProviderSelectorService;
  @Autowired
  private CashLoanInstalmentService cashLoanInstalmentService;

  private static ExecutorService executorService = new ThreadPoolExecutor(5, 10, 200L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(30),
      new RenamedThreadFactory(RepaymentAccountService.class.getSimpleName()), (r, executor) -> {
    log.error("Beyond the maximum of threadPool");
    if (!executor.isShutdown()) {
      executor.getQueue().poll();
      executor.execute(r);
    }
  });
  private static final Long DEFAULT_TIMEOUT = 2L;


  /**
   * 获取还款账户列表（仅理财使用）
   * 理财传入的 sdkType 是 IDN_FIN，不会有订单数据，orderVO 始终为 null
   */
  public List<RepaymentAccountVO> getRepaymentAccounts(Long userId, SDKType sdkType, PaymentAccount account,
                                                       RepaymentAccountType... types) {
    CashLoanRepaymentVO repaymentVO = getRepaymentVO(account, true, null);
    return getRepaymentAccountVOS(userId, sdkType, account, repaymentVO, new ArrayList<>(), types);
  }

  public List<RepaymentAccountVO> getRepaymentAccountVOS(Long userId, SDKType sdkType, PaymentAccount account,
                                                         CashLoanRepaymentVO repaymentVO, List<String> requiredChannel, RepaymentAccountType... types) {

    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    //取整
    if (PaymentAccount.isLoanAccount(account)) {
      repaymentVO.amount = CurrencyAmount.fromYuan(repaymentVO.amount.currency, repaymentVO.amount.roundUpToYuan());
    }
    Map<String, RepaymentAccountDisplayConfig> displayConfigMap = getDisplayConfigMap(repaymentVO, userId, account, requiredChannel, types);
    //并发获取还款账号
    CountDownLatch latch = new CountDownLatch(displayConfigMap.size());
    Map<String, RepaymentAccountVO> repaymentAccountVOMap = Maps.newConcurrentMap();
    displayConfigMap.forEach((key, value) -> executorService.submit(() -> {
      boolean result = true;
      try {
        RepaymentAccountVO repaymentAccountVO = getRepaymentAccountVO(userId, account, businessName, repaymentVO, key, value);
        if (repaymentAccountVO != null) {
          repaymentAccountVOMap.put(key, repaymentAccountVO);
        }
      } catch (Exception e) {
        result = false;
        log.warn("error in get repaymentAccount, userId : {}, channel : {}", userId, key, e);
      } finally {
        repaymentAccountMonitorService.log(key, result, sdkType);
        latch.countDown();
      }
    }));
    try {
      latch.await(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.error("error in get repaymentAccount, userId : {}", userId, e);
    }

    //对其他国家未成功获取的动态还款账号，设置为空；对于墨西哥，所有未成功获取的还款账号，设置为空
    displayConfigMap.forEach((key, value) -> {
      if (!repaymentAccountVOMap.containsKey(key) && value.type == RepaymentAccountType.DYNAMIC) {
        repaymentAccountVOMap.put(key, RepaymentAccountVO.fromWithoutAccount(userId, account, value.provider, key, value.type));
      }
    });

    //按照配置顺序进行排序
    return paymentProviderSelectorService.getChannelDisplayMap(account, userId).keySet().stream().map(repaymentAccountVOMap::get)
        .filter(Objects::nonNull).collect(Collectors.toList());
  }

  private Map<String, RepaymentAccountDisplayConfig> getDisplayConfigMap(CashLoanRepaymentVO repaymentVO, Long userId,
                                                                         PaymentAccount account, List<String> requiredChannels, RepaymentAccountType[] types) {
    //根据传入type获取配置
    Map<String, RepaymentAccountDisplayConfig> displayConfigMap = paymentProviderSelectorService.getChannelDisplayMap(account, userId).entrySet()
        .stream().filter(item -> ArrayUtils.isEmpty(types) || ArrayUtils.contains(types, item.getValue().type))
        .filter(item -> CollectionUtils.isEmpty(requiredChannels) || requiredChannels.contains(item.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    //理财直接返回
    if (PaymentAccount.isFinAccount(account)) {
      return displayConfigMap;
    }

    //过滤金额不符合的渠道
    return displayConfigMap.entrySet().stream()
        .filter(entry -> compareWithLimit(entry.getValue(), repaymentVO.amount.roundUpToYuan()) == Comparator.MATCH)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  //理财不存在CashLoanRepaymentVO
  private CashLoanRepaymentVO getRepaymentVO(PaymentAccount account, boolean deductCoupon, CashLoanOrderVO orderVO) {
    if (PaymentAccount.isFinAccount(account)) {
      return null;
    }
    return repaymentService.getRepaymentVOWithoutTransId(account, deductCoupon, orderVO);
  }

  public RepaymentAccountVO getRepaymentAccount(CashLoanOrderVO orderVO, PaymentAccount account, String channel, BigDecimal repayAmount,
                                                String repaymentMethod, Long instalmentId, String assignedMobileNumber) {
    if (repayAmount != null) {
      repayAmount = repayAmount.setScale(0, BigDecimal.ROUND_UP);
    }
    return getRepaymentAccountByChannel(orderVO.sdkType, account, channel, repayAmount, orderVO, RepaymentAccountCallerType.DEFAULT);
  }

  public RepaymentAccountVO getRepaymentAccountByChannel(Long userId, SDKType sdkType, PaymentAccount account, String channel,
                                                         BigDecimal repayAmount) {
    LoanAccountRecord accountRecord = accountModel.findByUserId(userId, sdkType);
    CashLoanOrderVO orderVO = ecOrderService.getEarliestDueOrder(accountRecord.getId());
    return getRepaymentAccountByChannel(sdkType, account, channel, repayAmount, orderVO, RepaymentAccountCallerType.DEFAULT);
  }

  /**
   * 使用payEventType确定中收VA来源是EC还是JBP
   *
   * @param userId
   * @param sdkType
   * @param orderId
   * @param channel
   * @param amount
   * @param extraData
   * @param payEventType
   * @return
   */
  public RepaymentAccountVO getRepaymentAccountByChannelForJbp(Long userId, SDKType sdkType, Long orderId, String channel,
                                                               BigDecimal amount, Map<String, String> extraData, PayEventType payEventType) {
    RepaymentAccountDisplayConfig config;
    if (Objects.isNull(payEventType)) {
      config = paymentProviderSelectorService.selectProvider(channel, userId, PaymentAccount.IDN, RepaymentAccountCallerType.UNION_REPAYMENT_HANDLER);
    } else {
      config = paymentProviderSelectorService.getRouteFromNewRouteNew(payEventType).get(channel);
    }
    if (config == null) {
      throw EcException.error("unsupported channel : " + channel);
    }
    CurrencyAmount currencyAmount = CurrencyAmount.fromYuan(EcCurrency.IDR, amount);
    validateAmountLimit(config, currencyAmount, sdkType);

    ReceiptCredentialContext receiptCredentialContext;
    switch (config.type) {
      case STATIC:
        VirtualAccountChannel staticAccountChannel = VirtualAccountChannel.valueOf(channel);
        receiptCredentialContext = ReceiptCredentialContext.fromStatic(userId, PaymentBusinessName.IDN_YQD, PaymentAccount.IDN,
            payEventType, config.provider, staticAccountChannel);
        return RepaymentAccountVO.from(virtualAccountPaymentMethod.getVirtualAccount(receiptCredentialContext));
      case DYNAMIC:
        DynamicAccountChannel dynamicAccountChannel = DynamicAccountChannel.valueOf(channel);
        Long expireTime = expiryTimeBasedOnChannel(dynamicAccountChannel, Clock.getMaxMillisOfDay(Clock.now(), sdkType.getTimeZone()),
            PaymentAccount.IDN);
        receiptCredentialContext = ReceiptCredentialContext.fromDynamic(userId, orderId, PaymentBusinessName.IDN_YQD, -1L,
            PaymentAccount.IDN, payEventType, idGeneratorService.genId(IdGeneratorService.Type.JBP_REPAY), currencyAmount, extraData,
            config.provider, dynamicAccountChannel, expireTime);
        return RepaymentAccountVO.from(dynamicAccountPaymentMethod.getDynamicPaymentReceipt(receiptCredentialContext));
      default:
        throw EcException.error(EcExceptionType.CASH_LOAN_UNSUPPORTED_REPAYMENT_ACCOUNT_TYPE,
            "unsupported repaymentAccountType : " + config.type);
    }
  }

  /**
   * 获取还款账号（带调用来源参数）
   *
   * @param sdkType    sdk 类型
   * @param account    支付账户
   * @param channel    还款渠道
   * @param repayAmount 还款金额
   * @param orderVO    订单信息
   * @param callerType 调用来源类型，决定是否应用 Provider 分流选择逻辑
   */
  public RepaymentAccountVO getRepaymentAccountByChannel(SDKType sdkType, PaymentAccount account, String channel, BigDecimal repayAmount,
                                                         CashLoanOrderVO orderVO, RepaymentAccountCallerType callerType) {
    // 1. 准备还款信息
    CashLoanRepaymentVO repaymentVO = prepareRepaymentVO(account, repayAmount, orderVO);
    log.info("Repayment {}", JsonUtils.toString(repaymentVO));

    // 2. 获取渠道配置，根据调用来源决定是否应用 Provider 选择器
    RepaymentAccountDisplayConfig config = paymentProviderSelectorService.selectProvider(channel, orderVO.userId, account, callerType);

    // 3. 校验金额限制（仅对贷款账户）
    if (PaymentAccount.isLoanAccount(account)) {
      validateAmountLimit(config, repaymentVO.amount, sdkType);
    }

    // 4. 印尼账户金额取整
    if (PaymentAccount.isIdnLoanAccount(account)) {
      repaymentVO.amount = CurrencyAmount.fromYuan(repaymentVO.amount.currency, repaymentVO.amount.roundUpToYuan());
    }

    // 5. 获取还款账号
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    return getRepaymentAccountVO(orderVO.userId, account, businessName, repaymentVO, channel, config);
  }

  /**
   * 准备还款 VO，处理部分还款金额
   */
  private CashLoanRepaymentVO prepareRepaymentVO(PaymentAccount account, BigDecimal repayAmount, CashLoanOrderVO orderVO) {
    CashLoanRepaymentVO repaymentVO = getRepaymentVO(account, false, orderVO);
    // 部分还款的金额处理
    if (Objects.nonNull(repaymentVO) && repayAmount != null && repayAmount.compareTo(BigDecimal.ZERO) > 0) {
      repaymentVO.amount = CurrencyAmount.fromYuan(repaymentVO.amount.currency, repayAmount);
    }
    return repaymentVO;
  }

  public RepaymentAccountVO getOVORepaymentAccountVO(BigDecimal amount, Map<String, String> extraData, PaymentAccount paymentAccount,
                                                     CashLoanOrderVO orderVO, Long instalmentId) {
    if (MapUtils.isEmpty(extraData) || extraData.get("account") == null) {
      throw EcException.error("OVO account is empty, orderId : {}", orderVO.id);
    }
    if (amount == null) {
      throw EcException.error("OVO amount is empty, orderId : {}", orderVO.id);
    }
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(orderVO.sdkType);
    RepaymentAccountDisplayConfig config = paymentProviderSelectorService.selectProvider(DynamicAccountChannel.OVO.name(), orderVO.userId, paymentAccount, RepaymentAccountCallerType.DEFAULT);

    CashLoanRepaymentVO repaymentVO = new CashLoanRepaymentVO(orderVO.userId, orderVO.id, businessName, instalmentId, paymentAccount,
        idGeneratorService.genId(IdGeneratorService.Type.REPAY), CurrencyAmount.fromYuan(orderVO.sdkType.getCurrency(), amount), extraData,
        config.provider, DynamicAccountChannel.OVO,
        expiryTimeBasedOnChannel(DynamicAccountChannel.OVO, Clock.getMaxMillisOfDay(Clock.now(), orderVO.sdkType.getTimeZone()),
            paymentAccount));
    return RepaymentAccountVO.from(dynamicAccountPaymentMethod.getDynamicPaymentReceipt(repaymentVO));
  }

  public RepaymentAccountVO getStaticVADanaRepaymentAccountVO(BigDecimal amount, PaymentAccount paymentAccount, CashLoanOrderVO orderVO) {
    if (amount == null) {
      throw EcException.error("DANA amount is empty, orderId : {}", orderVO.id);
    }
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(orderVO.sdkType);
    RepaymentAccountDisplayConfig config = new RepaymentAccountDisplayConfig();
    String danaStaticVAPaymentProvider = repaymentAccountConfig.getDanaStaticVAPaymentProvider();
    config.provider = PaymentProvider.from(danaStaticVAPaymentProvider);
    String virtualAccountChannel = repaymentAccountConfig.getDanaStaticVAChannel();
    String dana = "DANA";

    RepaymentAccountVO vo = RepaymentAccountVO.from(
        virtualAccountPaymentMethod.getVirtualAccount(orderVO.userId, businessName, paymentAccount, config.provider, virtualAccountChannel),
        config);
    vo.setChannel(dana);
    vo.setChannelDesc(dana);

    return vo;
  }


  private RepaymentAccountVO getRepaymentAccountVO(Long userId, PaymentAccount account, PaymentBusinessName businessName,
                                                   CashLoanRepaymentVO tRepaymentVO, String channel, RepaymentAccountDisplayConfig config) {
    if (PaymentAccount.isLoanAccount(account)) {
      LoanAccountRecord accountRecord = accountModel.findByUserIdOrThrow(userId, businessName.sdkType);
      amountLoader.set(accountRecord.getId().toString(), tRepaymentVO.amount.amount);
    }
    switch (config.type) {
      case STATIC:
        return RepaymentAccountVO.from(
            virtualAccountPaymentMethod.getVirtualAccount(userId, businessName, account, config.provider, channel), config);
      case DYNAMIC:
        if (PaymentAccount.isFinAccount(account)) {
          throw EcException.error("unsupported paymentAccount : " + account);
        }
        DynamicAccountChannel dynamicAccountChannel = DynamicAccountChannel.valueOf(channel);
        CashLoanRepaymentVO repaymentVO = CashLoanRepaymentVO.deepClone(tRepaymentVO,
            idGeneratorService.genId(IdGeneratorService.Type.REPAY), dynamicAccountChannel, config.provider,
            expiryTimeBasedOnChannel(dynamicAccountChannel, tRepaymentVO.expiredTime, account));
        log.info("Repayment {}", JsonUtils.toString(repaymentVO));
        return RepaymentAccountVO.from(dynamicAccountPaymentMethod.getDynamicPaymentReceipt(repaymentVO));
      default:
        throw EcException.error(EcExceptionType.CASH_LOAN_UNSUPPORTED_REPAYMENT_ACCOUNT_TYPE,
            "unsupported repaymentAccountType : " + config.type);
    }
  }

  private Long expiryTimeBasedOnChannel(DynamicAccountChannel channel, Long expiryTimeDefault, PaymentAccount paymentAccount) {
    if (!PaymentAccount.isLoanAccount(paymentAccount)) {
      return null;
    }
    switch (channel) {
      case DANA:
      case DANA_V2:
        return Clock.now() + Clock.MILLS_PER_MINUTE * repaymentAccountConfig.getExpiryTimeDanaDynamicVA();
      case SHOPEE:
        return Clock.now() + Clock.MILLS_PER_MINUTE * 30;
      case OVO:
        return Clock.now() + Clock.MILLS_PER_MINUTE;
      case GOPAY:
        return Clock.now() + Clock.MILLS_PER_MINUTE * 15;
      case XENDIT_QRIS:
        return Clock.now() + Clock.MILLS_PER_DAY;
      default:
        return expiryTimeDefault;
    }
  }

  private Comparator compareWithLimit(RepaymentAccountDisplayConfig config, Long amount) {
    if (config.minAmount != null && config.minAmount > amount) {
      return Comparator.LESS;
    }
    if (config.maxAmount != null && config.maxAmount < amount) {
      return Comparator.GREATER;
    }
    return Comparator.MATCH;
  }

  /**
   * 校验还款金额是否在渠道限额范围内，不在范围内则抛出异常
   */
  private void validateAmountLimit(RepaymentAccountDisplayConfig config, CurrencyAmount amount, SDKType sdkType) {
    Comparator comparator = compareWithLimit(config, amount.roundUpToYuan());
    switch (comparator) {
      case LESS:
        throw EcException.warn(EcExceptionType.CASH_LOAN_LESS_THAN_REPAY_LOWER_LIMIT, TT.gen("当前渠道每笔还款最低{0}，请尝试其他渠道",
            AmountFormatter.format(CurrencyAmount.fromYuan(sdkType.getCurrency(), config.minAmount))));
      case GREATER:
        throw EcException.warn(EcExceptionType.CASH_LOAN_REPAY_AMOUNT_TOO_LARGE,
            TT.gen("当前渠道每笔还款最多不超过{0}，您可分多次尝试或使用其他渠道还款。",
                AmountFormatter.format(CurrencyAmount.fromYuan(sdkType.getCurrency(), config.maxAmount))));
      case MATCH:
        break;
    }
  }

  public RepaymentAccountVO getDynamicRepaymentAccountVO(PaymentAccount account, PaymentProvider paymentProvider,
                                                         DynamicAccountChannel channel, CashLoanOrderVO orderVO) {
    if (PaymentAccount.isFinAccount(account)) {
      throw EcException.error("unsupported paymentAccount : " + account);
    }
    CashLoanRepaymentVO repaymentVO = repaymentService.getRepaymentVOWithoutTransId(account, false, orderVO);
    repaymentVO.transId = idGeneratorService.genId(IdGeneratorService.Type.REPAY);
    repaymentVO.channel = channel;
    repaymentVO.provider = paymentProvider;
    return RepaymentAccountVO.from(dynamicAccountPaymentMethod.getDynamicPaymentReceipt(repaymentVO));
  }

  public List<RepaymentAccountVO> getAvailableRepaymentAccountVO(PaymentAccount account, Long userId, SDKType sdkType, Long orderId,
                                                                 Long instalmentId, Long expiredTime, List<String> channels) {
    List<RepaymentAccountVO> repaymentAccountVOList = Lists.newArrayList();
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    for (String channel : channels) {
      RepaymentAccountDisplayConfig config = paymentProviderSelectorService.selectProvider(channel, userId, account, RepaymentAccountCallerType.DEFAULT);
      switch (config.type) {
        case STATIC:
          StaticVirtualAccountVO staticVirtualAccountVO = virtualAccountPaymentMethod.getVirtualAccount(userId, businessName, account,
              config.provider, channel);
          if (staticVirtualAccountVO != null) {
            repaymentAccountVOList.add(RepaymentAccountVO.from(staticVirtualAccountVO));
          }
          break;
        case DYNAMIC:
          if (PaymentAccount.isFinAccount(account)) {
            throw EcException.error("unsupported paymentAccount : " + account);
          }
          List<DynamicAccountVO> dynamicAccountVOS = dynamicAccountPaymentMethod.getAvailableDynamicAccountVO(userId, orderId, instalmentId,
              expiredTime, config.provider, DynamicAccountChannel.valueOf(channel), null);
          repaymentAccountVOList.addAll(
              dynamicAccountVOS.stream().map(RepaymentAccountVO::from).filter(Objects::nonNull).collect(Collectors.toList()));
          break;
        default:
          log.error("unsupported repaymentAccountType : " + config.type);
      }
    }
    return repaymentAccountVOList;
  }

  public List<RepaymentAccountVO> getDisplayRepaymentAccount(PaymentAccount account, Long userId, AppInfoVO appInfoVO, boolean hideSeaBankChannels) {
    Map<String /* channel */, RepaymentAccountDisplayConfig> originRepaymentChannelMap = paymentProviderSelectorService.getChannelDisplayMap(account, userId);
    Map<String /* channel */ , RepaymentAccountDisplayConfig> unpickedChannelConfigMap = new HashMap<>(originRepaymentChannelMap);
    // 是否移除 PERMATA、CIMB、DANAMON 还款渠道（根据实验判断）
    if (canNotDisplayMidtransBankChannels(userId)) {
      unpickedChannelConfigMap.remove(DynamicAccountChannel.PERMATA.name());
      unpickedChannelConfigMap.remove(DynamicAccountChannel.CIMB.name());
      unpickedChannelConfigMap.remove(DynamicAccountChannel.DANAMON.name());
    }
    unpickedChannelConfigMap.remove(DynamicAccountChannel.XENDIT_QRIS.name());
    if (hideSeaBankChannels) {
      unpickedChannelConfigMap.remove(DynamicAccountChannel.SEABANK.name());
    }

    List<String> outList = Lists.newArrayListWithExpectedSize(unpickedChannelConfigMap.size());

    filterAndSortRepaymentAccountDisplayConfig(userId, appInfoVO.sdkType, unpickedChannelConfigMap, outList);
    return outList.stream().map(channel -> buildRepaymentAccountVoFrom(channel, originRepaymentChannelMap)).filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public List<RepaymentAccountVO> getRepaymentAccountApiChannel(PaymentAccount account, Long userId, SDKType sdkType) {
    Map<String, RepaymentAccountDisplayConfig> channelToDisplayConfig = paymentProviderSelectorService.getChannelDisplayMap(account, userId);
    // add new logic on here
    Set<String> channelShowedByApiChannel = repaymentAccountConfig.getRepaymentChannelsByApiChannel();
    if (!channelShowedByApiChannel.isEmpty()) {
      channelToDisplayConfig.entrySet().removeIf(e -> !channelShowedByApiChannel.contains(e.getKey()));
    }

    Map<String, RepaymentAccountDisplayConfig> unpickChannelToDisplayConfig = new HashMap<>(channelToDisplayConfig);

    List<String> outList = Lists.newArrayListWithExpectedSize(unpickChannelToDisplayConfig.size());

    filterAndSortRepaymentAccountDisplayConfig(userId, sdkType, unpickChannelToDisplayConfig, outList);
    return outList.stream().map(channel -> buildRepaymentAccountVoFrom(channel, channelToDisplayConfig)).filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private void filterAndSortRepaymentAccountDisplayConfig(Long userId, SDKType sdkType, Map<String, RepaymentAccountDisplayConfig> unpickChannelToDisplayConfig, List<String> result) {

    // 还款渠道展示-第一梯队：展示用户还款使用过的支付渠道 - follow old code
    Map<String, Long> channelToOrderMap = buildChannelLatestRepayTimeMapForRecentlyUsedChannel(userId, sdkType);
    pickChannelFromUnpickedMapToOutListOrderly(unpickChannelToDisplayConfig, channelToOrderMap,
        java.util.Comparator.comparingLong(OrderedRepaymentChannel::getOrder).reversed(), result);

    // 还款渠道展示-第二梯队：展示用户绑卡的支付渠道 - follow old code
    channelToOrderMap = buildChannelLatestRepayTimeMapForBankAccountChannel(userId, unpickChannelToDisplayConfig);
    pickChannelFromUnpickedMapToOutListOrderly(unpickChannelToDisplayConfig, channelToOrderMap,
        java.util.Comparator.comparingLong(OrderedRepaymentChannel::getOrder).reversed(), result);

    // 还款渠道展示-第三梯队：展示大数据统计的最常使用支付渠道 - follow old code
    Map<String, Integer> channelToSortingId = repaymentAccountConfig.getRepaymentChannelDisplayRule();
    channelToOrderMap = buildChannelOrderMapForCommonUsedChannel(unpickChannelToDisplayConfig, channelToSortingId);
    pickChannelFromUnpickedMapToOutListOrderly(unpickChannelToDisplayConfig, channelToOrderMap, java.util.Comparator.comparing(OrderedRepaymentChannel::getOrder), result);

  }

  private void pickChannelFromUnpickedMapToOutListOrderly(
      Map<String /* channel */ , RepaymentAccountDisplayConfig> unpickedChannelConfigMap,
      Map<String /* channel */, Long /* latestRepayTime */> channelOrderMap,
      java.util.Comparator<OrderedRepaymentChannel> channelSortComparator, List<String> outList) {
    channelOrderMap
        .entrySet()
        .stream()
        .filter(e -> unpickedChannelConfigMap.containsKey(e.getKey()))
        .map(e -> new OrderedRepaymentChannel(e.getKey(), e.getValue()))
        .sorted(channelSortComparator)
        .map(OrderedRepaymentChannel::getChannel).forEachOrdered(channel -> {
          outList.add(channel);
          unpickedChannelConfigMap.remove(channel);
        });
  }

  public Map<String, Long> buildChannelLatestRepayTimeMapForRecentlyUsedChannel(Long userId, SDKType sdkType) {
    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    int limit = repaymentAccountConfig.getRepaymentChannelSampleLimit();

    // 查询最新的n条还款记录
    List<PaymentRecord> paymentRecords = paymentModel.findLatestVAPayments(userId, businessName, limit);

    // 将记录分成两类：静态VA和动态VA
    List<Long> staticVACredentialIds = new ArrayList<>();
    List<Long> dynamicVACredentialIds = new ArrayList<>();
    Map<Long, Long> credentialIdToTimeCompletedMap = new HashMap<>();

    for (PaymentRecord paymentRecord : paymentRecords) {
      Long credentialId = paymentRecord.getPaymentCredentialId();
      Long timeCompleted = paymentRecord.getTimeCompleted();
      credentialIdToTimeCompletedMap.put(credentialId, timeCompleted);

      if (PaymentMethod.VIRTUAL_ACCOUNT.name().equals(paymentRecord.getPaymentMethod())) {
        staticVACredentialIds.add(credentialId);
      } else if (PaymentMethod.DYNAMIC_ACCOUNT.name().equals(paymentRecord.getPaymentMethod())) {
        dynamicVACredentialIds.add(credentialId);
      }
    }

    Map<String, Long> channelToTimeMap = new HashMap<>();

    // 查询静态VA表获取渠道信息
    if (!staticVACredentialIds.isEmpty()) {
      staticVirtualAccountModel.fetch(new HashSet<>(staticVACredentialIds)).forEach(staticVARecord -> {
        Long credentialId = staticVARecord.getId();
        String channel = staticVARecord.getChannel();
        Long timeCompleted = credentialIdToTimeCompletedMap.get(credentialId);
        if (channel != null && timeCompleted != null) {
          channelToTimeMap.merge(channel, timeCompleted, Math::max);
        }
      });
    }

    // 查询动态VA表获取渠道信息
    if (!dynamicVACredentialIds.isEmpty()) {
      dynamicAccountModel.fetch(new HashSet<>(dynamicVACredentialIds)).forEach(dynamicRecord -> {
        Long credentialId = dynamicRecord.getId();
        String channel = dynamicRecord.getChannel();
        Long timeCompleted = credentialIdToTimeCompletedMap.get(credentialId);
        if (channel != null && timeCompleted != null) {
          channelToTimeMap.merge(channel, timeCompleted, Math::max);
        }
      });
    }
    return channelToTimeMap;
  }

  private Map<String /* channel */, Long /* latestRepayTime */> buildChannelLatestRepayTimeMapForBankAccountChannel(Long userId,
                                                                                                                    Map<String /* channel */ , RepaymentAccountDisplayConfig> unpickedChannelConfigMap) {
    List<LoanBankAccountVO> availableBankAccountList = loanBankAccountService.getAvailableBankAccounts(userId, SDKType.IDN_YQD);
    Map<String /* channel */, Long /* latestRepayTime */> channelLatestRepayTimeMap = new HashMap<>();
    for (LoanBankAccountVO loanBankAccount : availableBankAccountList) {
      RepaymentAccountDisplayConfig displayConfig = unpickedChannelConfigMap.get(loanBankAccount.bankCode);
      if (Objects.isNull(displayConfig)) {
        log.info("Unmapped bank code found, userId = {}, bankCode = {}", userId, loanBankAccount.bankCode);
        continue;
      }
      channelLatestRepayTimeMap.compute(loanBankAccount.bankCode, (channel, lastTimeUsed) -> {
        if (Objects.isNull(lastTimeUsed)) {
          return loanBankAccount.lastTimeUsed;
        }
        return Math.max(lastTimeUsed, loanBankAccount.lastTimeUsed);
      });
    }
    return channelLatestRepayTimeMap;
  }

  private Map<String /* channel */, Long /* order */> buildChannelOrderMapForCommonUsedChannel(
      Map<String /* channel */, RepaymentAccountDisplayConfig> unpickedChannelConfigMap,
      Map<String /* channel */, Integer /* sort */> channelSortMap) {
    return unpickedChannelConfigMap.keySet().stream().collect(
        Collectors.toMap(channel -> channel, channel -> Long.valueOf(channelSortMap.getOrDefault(channel, Integer.MAX_VALUE)),
            (i1, i2) -> i1));
  }


  @Getter
  @AllArgsConstructor
  public static class OrderedRepaymentChannel {

    private final String channel;
    private final long order;
  }

  /**
   * 判断是否不展示 PERMATA、CIMB、DANAMON 三个渠道
   * 根据用户是否逾期走不同的实验，实验为 "B" 时展示
   */
  public Boolean canNotDisplayMidtransBankChannels(Long userId) {
    String expKey = ExperimentKeyConstants.MIDTRANS_PERMATA_CIMB_DANAMON_NORMAL;
    return !"B".equals(expDiversionClient.getResult(expKey));
  }

  /**
   * 判断是否不展示 SeaBank 渠道
   * 根据用户是否逾期走不同的实验，实验为 "B" 时展示
   */
  public Boolean canNotDisplaySeaBankChannels(Long userId) {
    if (Optional.ofNullable(requestClientType()).map(RequestClientType::isWholeProcess).orElse(false)) {
      return true;
    }
    return !"B".equals(expDiversionClient.getResult(ExperimentKeyConstants.SEABANK_CHANNEL_NORMAL));
  }

  private RepaymentAccountVO buildRepaymentAccountVoFrom(String channel,
                                                         Map<String /* channel */, RepaymentAccountDisplayConfig> repaymentChannelMap) {
    RepaymentAccountDisplayConfig config = repaymentChannelMap.get(channel);
    if (Objects.isNull(config)) {
      log.warn("Unsupported repayment channel: {}", channel);
      return null;
    }
    return RepaymentAccountVO.from(channel, config);
  }

  private List<String> sortRepaymentChannel(Set<String> channelSet, Map<String, Integer> displayRule) {
    int defaultIndex = displayRule.size();
    return channelSet.stream().sorted(
        (o1, o2) -> ComparisonChain.start().compare(displayRule.getOrDefault(o1, defaultIndex), displayRule.getOrDefault(o2, defaultIndex))
            .compare(o1, o2).result()).collect(Collectors.toList());
  }

  public void sendRepaymentAnnouncement(CashLoanOrderVO orderVO, BigDecimal amount, String channelType, String channelName,
                                        String accountInfo) {
    Integer sendTimes = repaymentAccountSendTimesLoader.getOneDayTimes(orderVO.id);
    if (sendTimes != null && sendTimes > repaymentAccountConfig.getRepaymentAccountLimitOneDay()) {
      throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("还款账号发送次数超过当日限制！"));
    }
    UserInfoVO userInfoVO = userService.fetchById(orderVO.userId, orderVO.sdkType);
    repaymentAccountSendLocker.nonBlockingLockAndRun(orderVO.id, () -> {
      String text = TT.gen(repaymentAccountConfig.getRepaymentAnnouncementText(channelType), amount, channelName, accountInfo)
          .toString(userInfoVO.sdkType.getLocale().locale);
      try {
        notifService.sendMessage(orderVO.sdkType, MsgPriority.HIGH, userInfoVO.normalizedMobileNumber, text);
        repaymentAccountSendTimesLoader.incrOneDayTimes(orderVO.id);
      } catch (Exception e) {
        throw EcException.warn(EcExceptionType.COMMON_CUSTOM_MESSAGE, TT.gen("短信发送异常，请稍后重试！"),
            "send repayment account failed, orderId: {}", orderVO.id, e);
      }
    });
  }

  public OVORepaymentInfoVO getUserHistoryOVORepaymentInfo(BigDecimal amount, Long userId, Long orderId, Long instalmentId) {
    DynamicAccountConditions conditions = DynamicAccountConditions.getConditions(userId, orderId, instalmentId,
        CurrencyAmount.fromYuan(SDKType.IDN_YQD.getCurrency(), amount), Clock.now(), null, DynamicAccountChannel.OVO, null,
        DynamicAccountStatus.EFFECTIVE);
    return buildOVORepaymentInfo(conditions, userId);
  }

  public OVORepaymentInfoVO getUserHistoryOVORepaymentInfoForJbp(BigDecimal amount, Long userId, Long orderId) {
    DynamicAccountConditions conditions = DynamicAccountConditions.getConditions(userId, orderId, -1L,
        CurrencyAmount.fromYuan(SDKType.IDN_YQD.getCurrency(), amount), Clock.now(), null, PayEventType.JBP_RECEIPT,
        DynamicAccountChannel.OVO, null, DynamicAccountStatus.EFFECTIVE);
    return buildOVORepaymentInfo(conditions, userId);
  }

  private OVORepaymentInfoVO buildOVORepaymentInfo(DynamicAccountConditions conditions, Long userId) {
    DynamicAccountVO dynamicAccountVO = dynamicAccountPaymentMethod.findDynamicPaymentReceipt(conditions);
    if (dynamicAccountVO != null) {
      return OVORepaymentInfoVO.from(dynamicAccountVO.account, true);
    }
    UserSimpleInfoVO userSimpleVO = userService.fetchByIdOrThrow(userId);
    String registerMobileNumber = MobileConverter.normalizedToNationalOrThrow(userSimpleVO.initSDKType.getLocale(),
        userSimpleVO.normalizedMobileNumber);
    dynamicAccountVO = dynamicAccountPaymentMethod.getLatestEffectiveAccount(userSimpleVO.userId, DynamicAccountChannel.OVO);
    if (dynamicAccountVO == null) {
      return OVORepaymentInfoVO.from(registerMobileNumber, false);
    }
    return OVORepaymentInfoVO.from(dynamicAccountVO.account, false);
  }

  private enum Comparator {
    LESS, GREATER, MATCH,
    ;
  }

  public List<RepaymentAccountVO> getBorrowerStaticRepaymentAccountVO(Long userId, SDKType sdkType, PaymentAccount account,
                                                                      String channel) {
    RepaymentAccountVO staticRepaymentAccountVO = getStaticRepaymentAccountVO(userId, sdkType, account, channel);
    return Objects.nonNull(staticRepaymentAccountVO) ? Collections.singletonList(staticRepaymentAccountVO) : Collections.emptyList();
  }

  public RepaymentAccountVO getStaticRepaymentAccountVO(Long userId, SDKType sdkType, PaymentAccount account, String channel) {

    if (PaymentAccount.isFinAccount(account)) {
      throw EcException.error("unsupported paymentAccount {} for userId {}", account, userId);
    }

    RepaymentAccountDisplayConfig config = paymentProviderSelectorService.selectProvider(channel, userId, account, RepaymentAccountCallerType.DEFAULT);

    PaymentBusinessName businessName = PaymentBusinessNameMapper.getBusinessName(sdkType);
    StaticVirtualAccountVO staticVirtualAccountVO = virtualAccountPaymentMethod.getVirtualAccount(userId, businessName, account,
        config.provider, channel);

    return RepaymentAccountVO.from(staticVirtualAccountVO);
  }

  public List<String> getExcludedRepaymentChannelsForInternalApi() {
    return repaymentAccountConfig.getExcludedRepaymentChannelsForInternalApi();
  }

  public static PaymentProvider getPaymentProvider(String key, BigDecimal versionPercentage, PaymentProvider defaultProvider,
                                                   PaymentProvider newProvider) {
    if (versionPercentage == null) {
      return defaultProvider;
    }
    boolean isUseNewVersion = EcHashUtil.hitConsistentHashWithMurmurHash(key, BigDecimal.ZERO, versionPercentage);
    if (isUseNewVersion) {
      return newProvider;
    } else {
      return defaultProvider;
    }
  }

  private Map<String, RepaymentAccountDisplayConfig> getRouteFromNewRouteNew(PayEventType payEventType) {
    String configRouteConfigStr = routeConfigLoader.get(payEventType);
    if (StringUtils.isNotBlank(configRouteConfigStr)) {
      return JsonUtils.fromOrException(configRouteConfigStr, new TypeReference<Map<String, RepaymentAccountDisplayConfig>>() {
      });
    } else {
      Map<String, RepaymentAccountDisplayConfig> channelToRouteConfigMap = repaymentRouteService.getRepaymentChannelRoute(
              payEventType.name()).stream()
          .collect(Collectors.toMap(RepaymentChannelRouteResponse::getChannel, RepaymentAccountDisplayConfig::from));
      routeConfigLoader.set(payEventType, JsonUtils.toString(channelToRouteConfigMap));
      return channelToRouteConfigMap;
    }
  }

  public List<RepaymentAccountVO> getJbpRepaymentChannelList(Long userId, Long build, SourceType sourceType, SDKType sdkType) {
    Map<String, RepaymentAccountDisplayConfig> repaymentChannelMap = getRouteFromNewRouteNew(PayEventType.JBP_RECEIPT);
    //渠道顺序走配置
    Map<String, Integer> channelDisplayRule = repaymentAccountConfig.getJbpRepaymentChannelDisplayRule();

    Long loanAccountId = loanAccountService.getAccountIdByUserId(userId, sdkType);
    IDNHomepageLoanStatusV5 homePageStatus = homepageStatusTool.getStatus(loanAccountId, build, sdkType);

    // 父实验是否进实验组标记
    boolean newChannelExperimentFlag = false;
    // 入组用户：可借 or (在贷&不可借)
    if (homePageStatus.canCreateOrder() || homePageStatus.repaymentOrder()) {
      ExpUser expUser = ExpUser.builder().userId(userId).sourceType(sourceType).versionBuild(build).build();
      // 父实验：展示所有支付渠道
      String expValForChannelDisplay = expDiversionClient.getResult(ExperimentKeyConstants.JBP_ADD_NEW_PAYMENT_CHANNEL, expUser);
      if (CommonABTestResultGroup.B.desc.equals(expValForChannelDisplay)) {
        newChannelExperimentFlag = true;
      }
      // 根据实验结果展示支付渠道（基于flag判断是否进组）
      channelDisplayRule = updateChannelDisplayRuleWithExpResult(channelDisplayRule, expUser, newChannelExperimentFlag);
    }
    // 根据实验结果屏蔽新渠道（基于flag判断是否进组）
    removeNewChannelWithConfig(repaymentChannelMap, newChannelExperimentFlag);

    List<String> sortedRepaymentChannelList = sortRepaymentChannel(repaymentChannelMap.keySet(), channelDisplayRule);

    List<RepaymentAccountVO> repaymentAccountVOS = new ArrayList<>();
    for (String channel : sortedRepaymentChannelList) {
      RepaymentAccountDisplayConfig config = repaymentChannelMap.get(channel);
      repaymentAccountVOS.add(RepaymentAccountVO.from(channel, config));
    }
    return repaymentAccountVOS;
  }

  /**
   * 根据实验结果判断渠道展示顺序
   * @param channelDisplayRule
   * @param expUser
   */
  public Map<String, Integer> updateChannelDisplayRuleWithExpResult(Map<String, Integer> channelDisplayRule, ExpUser expUser, boolean newChannelExperimentFlag) {
    // 父实验：展示所有支付渠道，通过传入的flag判断是否进组
    if (newChannelExperimentFlag) {
      // 子实验：展示顺序。实验组会将新的支付渠道放在前面
      String expValForChannelDisplayOrder = expDiversionClient.getResult(ExperimentKeyConstants.JBP_PAYMENT_CHANNEL_DISPLAY_ORDER, expUser);
      if (expValForChannelDisplayOrder.equals(CommonABTestResultGroup.B.desc)) {
        channelDisplayRule = repaymentAccountConfig.getJbpRepaymentChannelDisplayRuleWithNewEwalletChannel();
      }
    }
    return channelDisplayRule;
  }

  /**
   * 过滤掉新增的支付渠道
   * @param repaymentChannelMap map
   */
  public void removeNewChannelWithConfig(Map<String, RepaymentAccountDisplayConfig> repaymentChannelMap, boolean newChannelExperimentFlag) {
    // 进实验组不过滤，其他场景过滤
    if (!newChannelExperimentFlag) {
      Set<String> defaultChannel = repaymentAccountConfig.getJbpDefaultChannel();
      // 过滤掉不在默认渠道里的元素
      if (CollectionUtils.isNotEmpty(defaultChannel)) {
        repaymentChannelMap.entrySet().removeIf(e -> !defaultChannel.contains(e.getKey()));
      }
    }
  }

  public String getJbpQuickPaymentChannel(Long userId) {
    String channel = paymentService.getLatestRepaymentChannel(userId);
    if (StringUtils.isNotBlank(channel) && !jbpConfig.getQuickPaymentBannedChannels().contains(channel)) {
      return channel;
    }
    return jbpConfig.getDefaultDynamicRepaymentChannel();
  }

  public List<CashLoanRepaymentAccountResVO.RelatedInfo> getRelatedInfo(RepaymentAccountVO data) {
    Boolean internalOpen = repaymentAccountConfig.getRepaymentRelatedInfoInternalOpenByChannel(data.getChannel());
    return Optional.ofNullable(data.getRelatedInfo()).map(Map::entrySet).map(
        set -> set.stream().map(e -> new CashLoanRepaymentAccountResVO.RelatedInfo(e.getKey(), e.getValue(), internalOpen))
            .collect(Collectors.toList())).orElse(null);
  }
}
