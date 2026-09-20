package com.yqg.core.service.payment;

import com.google.common.collect.*;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.core.service.cashloan.funding.FundingProvider;
import com.yqg.core.service.payment.pm.PaymentMethod;
import com.yqg.core.service.payment.pp.PaymentProvider;
import com.yqg.ec.common.exception.EcException;
import com.yqg.ec.common.utils.EcAsserts;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 支付账户设置
 * <p>
 * Created by jpdu on 2018/5/28.
 */
@Deprecated
public enum PaymentAccount {
  IDN("N"),
  IDN_MASBRO("M"),
  IDN_FIN("F"),
  TH("T"),
  BRA("B"),
  PHI("P"),
  INDIA("I"),
  MEX("X"),
  POL("W"),
  ;

  public String code;

  PaymentAccount(String code) {
    this.code = code;
  }

  public static final List<PaymentAccount> loanAccountList = ImmutableList.of(IDN, IDN_MASBRO, TH, BRA, PHI, INDIA, MEX);
  public static final List<PaymentAccount> finAccountList = ImmutableList.of(IDN_FIN);

  private static Map<FundingProvider, PaymentAccount> fundingToAccount = Maps.newHashMap();
  private static Map<PaymentAccount, ListMultimap<PaymentMethod, PaymentProvider>> PAYOUT_ROUTE_TO_PROVIDER = Maps.newHashMap();
  private static Map<PaymentAccount, ListMultimap<PaymentMethod, PaymentProvider>> REPAY_ROUTE_TO_PROVIDER = Maps.newHashMap();
  private static Map<PaymentAccount, ListMultimap<PaymentProvider, PaymentMethod>> REPAY_ROUTE_TO_METHOD = Maps.newHashMap();
  private static Map<SDKType, PaymentAccount> sdkDefaultAccount = Maps.newHashMap();
  private static SetMultimap<SDKType, PaymentAccount> SDK_TO_ACCOUNTS = HashMultimap.create();
  private static SetMultimap<PaymentMethod, SDKType> METHOD_TO_SDK = HashMultimap.create();
  private static SetMultimap<SDKType, PaymentMethod> SDK_TO_PAYOUT_METHOD = HashMultimap.create();
  private static SetMultimap<SDKType, PaymentMethod> SDK_TO_REPAY_METHOD = HashMultimap.create();

  static {

    // indonesia
    newRouting()
        .usedIn(SDKType.IDN_YQD, SDKType.IDN_NXT, SDKType.IDN_CASHCASH, SDKType.IDN_RONG_360)
        .setFundingToAccount(FundingProvider.IDNYQD, IDN)
        .setPayout(PaymentMethod.BANKCARD, PaymentProvider.ODEO, PaymentProvider.BCA,
            PaymentProvider.INSTAMONEY, PaymentProvider.ILUMA, PaymentProvider.IRIS)
        .setRepay(PaymentMethod.VIRTUAL_ACCOUNT,PaymentProvider.INSTAMONEY,
            PaymentProvider.BCA, PaymentProvider.XENDIT_IDN)
        .setRepay(PaymentMethod.DYNAMIC_ACCOUNT, PaymentProvider.INSTAMONEY, PaymentProvider.XENDIT_IDN)
        .finish();

    // indonesia financing
    newRouting()
        .usedIn(SDKType.IDN_FIN)
        .setFundingToAccount(FundingProvider.IDN_FIN, IDN_FIN)
        .setPayout(PaymentMethod.BANKCARD, PaymentProvider.ODEO, PaymentProvider.BCA,
            PaymentProvider.INSTAMONEY, PaymentProvider.ILUMA)
        .setRepay(PaymentMethod.VIRTUAL_ACCOUNT, PaymentProvider.BCA, PaymentProvider.BNI)
        .finish();

    // 设置每个sdk所使用的默认account
    setDefaultAccountToSDK(PaymentAccount.IDN, SDKType.IDN_YQD, SDKType.IDN_NXT, SDKType.IDN_CASHCASH, SDKType.IDN_RONG_360);
    setDefaultAccountToSDK(PaymentAccount.IDN_FIN, SDKType.IDN_FIN);
  }

  public static PaymentAccount fromCode(String code) {
    for (PaymentAccount account : values()) {
      if (account.code.equals(code)) {
        return account;
      }
    }
    throw EcException.error("unknown payment account code: " + code);
  }

  public static PaymentAccount from(FundingProvider fundingProvider) {
    PaymentAccount account = fundingToAccount.get(fundingProvider);
    if (account == null) {
      throw EcException.error("can't find payment_account. funding_provider: " + fundingProvider);
    }
    return account;
  }

  public List<PaymentProvider> getPayoutProvider(PaymentMethod method) {
    return PAYOUT_ROUTE_TO_PROVIDER.get(this).get(method);
  }

  public List<PaymentProvider> getRepayProvider(PaymentMethod method) {
    return REPAY_ROUTE_TO_PROVIDER.get(this).get(method);
  }

  public static PaymentAccount getDefaultAccount(SDKType sdkType) {
    PaymentAccount account = sdkDefaultAccount.get(sdkType);
    if (account == null) {
      throw EcException.error("can't find payment_account. sdk: " + sdkType);
    }
    return account;
  }

  public static Set<PaymentAccount> getAccounts(SDKType sdkType) {
    return SDK_TO_ACCOUNTS.get(sdkType);
  }

  public static Set<PaymentMethod> getPayoutMethodSet(SDKType sdkType) {
    return SDK_TO_PAYOUT_METHOD.get(sdkType);
  }

  private static class AccountConfig {
    FundingProvider fundingProvider;
    PaymentAccount paymentAccount;
    Set<PaymentMethod> methods = Sets.newHashSet();
    List<SDKType> sdkTypes;

    AccountConfig usedIn(SDKType... sdkTypes) {
      this.sdkTypes = Lists.newArrayList(sdkTypes);
      return this;
    }

    AccountConfig setFundingToAccount(FundingProvider funding, PaymentAccount account) {
      this.fundingProvider = funding;
      this.paymentAccount = account;
      return this;
    }

    AccountConfig setPayout(PaymentMethod method, PaymentProvider... providers) {
      PAYOUT_ROUTE_TO_PROVIDER.putIfAbsent(paymentAccount, ArrayListMultimap.create());
      ListMultimap<PaymentMethod, PaymentProvider> map = PAYOUT_ROUTE_TO_PROVIDER.get(paymentAccount);
      for (PaymentProvider provider : providers) {
        map.put(method, provider);
      }
      for (SDKType sdkType : sdkTypes) {
        SDK_TO_PAYOUT_METHOD.put(sdkType, method);
      }

      this.methods.add(method);
      return this;
    }

    AccountConfig setRepay(PaymentMethod method, PaymentProvider... providers) {
      REPAY_ROUTE_TO_PROVIDER.putIfAbsent(paymentAccount, ArrayListMultimap.create());
      ListMultimap<PaymentMethod, PaymentProvider> map = REPAY_ROUTE_TO_PROVIDER.get(paymentAccount);
      for (PaymentProvider provider : providers) {
        map.put(method, provider);
      }

      REPAY_ROUTE_TO_METHOD.putIfAbsent(paymentAccount, ArrayListMultimap.create());
      ListMultimap<PaymentProvider, PaymentMethod> providerAndPaymentMap = REPAY_ROUTE_TO_METHOD.get(paymentAccount);
      for (PaymentProvider provider : providers) {
        providerAndPaymentMap.put(provider, method);
      }

      for (SDKType sdkType : sdkTypes) {
        SDK_TO_REPAY_METHOD.put(sdkType, method);
      }

      this.methods.add(method);
      return this;
    }

    void finish() {
      EcAsserts.assertNotNull(sdkTypes);
      fundingToAccount.put(fundingProvider, paymentAccount);
      for (PaymentMethod method : this.methods) {
        METHOD_TO_SDK.putAll(method, sdkTypes);
      }

      for (SDKType sdk : sdkTypes) {
        SDK_TO_ACCOUNTS.put(sdk, paymentAccount);
      }
    }
  }

  private static AccountConfig newRouting() {
    return new AccountConfig();
  }

  private static void setDefaultAccountToSDK(PaymentAccount paymentAccount, SDKType... sdkTypes) {
    for (SDKType sdk : sdkTypes) {
      sdkDefaultAccount.put(sdk, paymentAccount);
    }
  }

  public static Boolean isIdnLoanAccount(PaymentAccount account) {
    return account == IDN || account == IDN_MASBRO;
  }

  public static Boolean isLoanAccount(PaymentAccount account) {
    return loanAccountList.contains(account);
  }

  public static Boolean isFinAccount(PaymentAccount account) {
    return finAccountList.contains(account);
  }
}
