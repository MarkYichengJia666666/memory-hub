package com.yqg.core.service.cashloan.repayment;

import com.yqg.core.service.abtest.ExpDiversionClient;
import com.yqg.core.service.abtest.ExpUser;
import com.yqg.core.service.loan.repayment.account.RepaymentAccountConfig;
import com.yqg.ec.common.constant.ExperimentKeyConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RepaymentAccountExpService {

  @Autowired
  private RepaymentAccountConfig repaymentAccountConfig;

  @Autowired
  private ExpDiversionClient expDiversionClient;

  public boolean shouldUseRefactoredPath(Long userId) {
    //重构代码降级开关，默认关闭
    return repaymentAccountConfig.getRepaymentAccountRefactorSwitch() && fetchExpRes(userId);
  }

  /**
   * 还款VA重构的技术实验结果
   *
   * @param userId
   * @return false对照组走老逻辑 true实验组走新逻辑
   */
  private boolean fetchExpRes(Long userId) {
    return expDiversionClient.getBoolean(ExperimentKeyConstants.REPAYMENT_ACCOUNT_EXP_KEY,
        ExpUser.builder().userId(userId).versionBuild(1L).build(), false);
  }
}
