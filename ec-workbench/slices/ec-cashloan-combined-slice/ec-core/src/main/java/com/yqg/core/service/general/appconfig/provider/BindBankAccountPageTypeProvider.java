package com.yqg.core.service.general.appconfig.provider;

import com.google.common.collect.Sets;
import com.yqg.core.model.sql.abtest.enums.ABTestSceneType;
import com.yqg.core.model.sql.loan.account.enums.BindBankAccountPageType;
import com.yqg.core.service.abtest.ABTestUtil;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.enums.CommonABTestResultGroup;
import com.yqg.core.service.cashloan.risk.AppListService;
import com.yqg.core.service.general.appconfig.enums.AppConfigKeyEnum;
import com.yqg.core.service.general.appconfig.vo.GeneralAppConfigParamVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.bankaccount.LoanBankConfig;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.ec.common.enums.LoanAccountAdditionalTypeEnum;
import com.yqg.ec.common.enums.loan.PlatformType;
import com.yqg.ec.common.exception.EcException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BindBankAccountPageTypeProvider implements GeneralAppConfigProvider {
  @Autowired
  private ExpFacade expFacade;
  @Autowired
  private LoanAccountService loanAccountService;
  @Autowired
  private AppListService appListService;
  @Autowired
  private LoanBankConfig loanBankConfig;

  @Override
  public Object getAppConfig(GeneralAppConfigParamVO paramVO) {

    if (loanBankConfig.useOldBankAccountPage()) {
      return BindBankAccountPageType.BANK;
    }

    if (paramVO.userId == null) {
      log.error("userId is null for BindBankAccountPageTypeProvider!");
      return BindBankAccountPageType.BANK_AND_EWALLET;
    }

    if (paramVO.platformType == PlatformType.IOS) {
      return BindBankAccountPageType.BANK_AND_EWALLET;
    }

    String strategyStr = expFacade.fetchResult(ABTestSceneType.BIND_BANK_ACCOUNT_NEW_PAGE, ABTestUtil.genDiversionKeyMapByUserId(paramVO.userId), "B");
    CommonABTestResultGroup abTestResult = CommonABTestResultGroup.valueOf(strategyStr);
    switch (abTestResult) {
      case A:
        LoanAccountVO accountVO = loanAccountService.getAccountByUserIdOrThrow(paramVO.userId, paramVO.sdkType);
        Set<String> appPackageSet = appListService.loadLatestAppPackageListFromDb(accountVO.id, LoanAccountAdditionalTypeEnum.APP_LIST_INFO_OF_REGISTER);
        Set<String> bankAppPackageSet = loanBankConfig.getBankAppPackageSet();
        appPackageSet = appPackageSet.stream().filter(bankAppPackageSet::contains).collect(Collectors.toSet());
        log.info("user has these bank app, userId:{}, appList:{}", paramVO.userId, appPackageSet);
        if (CollectionUtils.isEmpty(appPackageSet)) {
          return BindBankAccountPageType.BANK_AND_EWALLET;
        }
        return BindBankAccountPageType.NEW_BANK_AND_EWALLET;
      case B:
        return BindBankAccountPageType.BANK_AND_EWALLET;
      case C:
        return BindBankAccountPageType.BANK;
      default:
        throw EcException.error("invalid abTestResult of ABTestSceneType : BIND_BANK_ACCOUNT_NEW_PAGE, abTestResult: {}", abTestResult);
    }

  }

  @Override
  public Set<AppConfigKeyEnum> supportKeys() {
    return Sets.newHashSet(AppConfigKeyEnum.BIND_BANK_ACCOUNT_PAGE_TYPE);
  }
}
