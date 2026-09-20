package com.yqg.core.service.cashloan.auth;

import static com.yqg.core.service.abtest.AbstractExpClient.BLANK_GROUP;

import com.yqg.core.model.generated.tables.records.LoanBankAccountDelayUserLogRecord;
import com.yqg.core.model.sql.bankaccount.LoanBankAccountDelayUserLogModel;
import com.yqg.core.model.sql.user.UserModel;
import com.yqg.core.service.abtest.ExpLastResultRunningClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.abtest.ExpFacade;
import com.yqg.core.service.abtest.enums.ExperimentNameSpace;
import com.yqg.core.service.abtest.vo.ABTestUserIdRequestVO;
import com.yqg.core.service.cashloan.CashLoanAuthConfig;
import com.yqg.core.service.cashloan.auth.step.enums.BindCardDelayGroup;
import com.yqg.core.service.loan.bankaccount.LoanBankAccountService;
import com.yqg.core.service.loan.vo.bankaccount.LoanBankAccountVO;
import com.yqg.ec.common.enums.SDKType;
import com.yqg.ec.common.enums.loan.SourceType;
import com.yqg.ec.common.i18n.time.Clock;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 绑卡后置服务
 */
@Slf4j
@Service
public class BindCardDelayService {
  @Autowired
  private CashLoanAuthConfig authConfig;
  @Autowired
  private ExpFacade expFacade;
  @Autowired
  private LoanBankAccountDelayUserLogModel delayUserLogModel;
  @Autowired
  private LoanBankAccountService loanBankAccountService;
  @Autowired
  private UserModel userModel;
  @Autowired
  private AuthService authService;
  @Autowired
  private ExpLastResultRunningClient expLastResultRunningClient;

  /**
   * 获取是否为绑卡后置用户
   *
   * @param userId
   * @param build
   * @return
   */
  public boolean fetchBindCardDelayRes(Long userId, Long build, SDKType sdkType) {
    //从表中获取用户注册时的sourceType而不是当前登录方式的sourceType
    SourceType sourceType = SourceType.valueOf(userModel.findById(userId).getSourceType());
    if (!isEligibleForBindCardDelay(userId, build, sourceType, sdkType)) {
      return false;
    }
    //log表中有记录代表用户选择跳过绑卡环节，或者为固定跳过绑卡环节的用户
    LoanBankAccountDelayUserLogRecord record = findDelayUserLog(userId);
    if (Objects.nonNull(record)) {
      return true;
    }
    BindCardDelayGroup bindCardDelayGroup = fetchExperimentRes(userId, build);
    if (bindCardDelayGroup == BindCardDelayGroup.FIXED_SKIP) {
      //固定跳过绑卡环节用户记录到log表中
      insertDelayUserLog(userId, bindCardDelayGroup);
      return true;
    }
    return false;
  }

  public boolean skipBindCard(Long userId, Long build) {
    LoanBankAccountDelayUserLogRecord record = findDelayUserLog(userId);
    if (Objects.nonNull(record)) {
      return true;
    }
    BindCardDelayGroup bindCardDelayGroup = fetchExperimentLastRes(userId, build);
    if (bindCardDelayGroup == BindCardDelayGroup.OPTIONAL_SKIP) {
      //可选择跳过的实验组用户记录到log表中
      insertDelayUserLog(userId, bindCardDelayGroup);
      return true;
    }
    log.warn("skip bindCardDelay fail, user is not in experiment group:OPTIONAL_SKIP userId:{}, build:{}", userId, build);
    return false;
  }

  /**
   * 获取上一次实验结果，查询是否为OPTIONAL_SKIP组用户
   *
   * @param userId
   * @param build
   * @return
   */
  public boolean isOptionalSkipUser(Long userId, Long build) {
    if (Objects.isNull(userId) || Objects.isNull(build)) {
      return false;
    }
    BindCardDelayGroup bindCardDelayGroup = fetchExperimentLastRes(userId, build);
    return bindCardDelayGroup == BindCardDelayGroup.OPTIONAL_SKIP;
  }

  /**
     * 在完件流程跳过了绑卡环节并且还未绑卡或当前没有绑卡
   */
  public boolean isMissingBankCard(Long userId, SDKType sdkType) {
    if (Objects.isNull(userId)) {
      return false;
    }
    LoanBankAccountDelayUserLogRecord record = findDelayUserLog(userId);
    List<LoanBankAccountVO> bankAccountVOS = loanBankAccountService.getBankAccounts(userId, sdkType);
    return (Objects.nonNull(record) && Objects.isNull(record.getTimeBind())) || CollectionUtils.isEmpty(bankAccountVOS);
  }

  public boolean isDelayBindCardUser(Long userId) {
    LoanBankAccountDelayUserLogRecord record = findDelayUserLog(userId);
    return Objects.nonNull(record);
  }

  /**
   * 检查用户是否符合绑卡后置功能的条件
   */
  private boolean isEligibleForBindCardDelay(Long userId, Long build, SourceType sourceType, SDKType sdkType) {
    // API渠道用户不支持绑卡后置功能
    if (sourceType.isApiChannelSourceType()) {
      return false;
    }
    // 检查应用版本
    if (build <= authConfig.getBindCardDelayStartBuild()) {
      return false;
    }
    //已完件用户不进实验
    if (authService.isAuthFinishedByUserId(userId)) {
      return false;
    }
    //已经完成绑卡用户不进实验
    List<LoanBankAccountVO> bankAccountVOS = loanBankAccountService.getBankAccounts(userId, sdkType);
    if (CollectionUtils.isNotEmpty(bankAccountVOS)) {
      return false;
    }
    return true;
  }

  private BindCardDelayGroup fetchExperimentRes(Long userId, Long build) {
    ABTestUserIdRequestVO requestVO = ABTestUserIdRequestVO.from(ExperimentNameSpace.BIND_CARD_DELAY, userId);
    return BindCardDelayGroup.valueOf(
        expFacade.fetchResultFallBackWithDefaultScene("product_operation_auth-auth-abroad-loan-Card_binding_1_0928", ExpFacade.ClientType.DIVERSION, requestVO, build)
    );
  }

  private BindCardDelayGroup fetchExperimentLastRes(Long userId, Long build) {
    String result = expLastResultRunningClient.getResult("product_operation_auth-auth-abroad-loan-Card_binding_1_0928",
        ExpUser.builder().userId(userId).versionBuild(build).build());
    return BLANK_GROUP.equals(result) ? BindCardDelayGroup.CONTROL_GROUP : BindCardDelayGroup.valueOf(result);
  }

  private LoanBankAccountDelayUserLogRecord findDelayUserLog(Long userId) {
    return delayUserLogModel.findByUserId(userId);
  }

  private int insertDelayUserLog(Long userId, BindCardDelayGroup experimentRes) {
    return delayUserLogModel.insertOrIgnore(userId, experimentRes.name());
  }

  public void updateTimeBind(Long userId) {
    delayUserLogModel.updateTimeBind(userId, Clock.now());
  }
}
