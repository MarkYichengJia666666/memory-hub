package com.yqg.core.service.risk.usergroup;

import com.yqg.core.service.cashloan.CashLoanCalcCreditsService;
import com.yqg.core.service.cashloan.vo.CashLoanCalcCreditsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 为等级变更监控采集额度状态的适配层。
 *
 * <p>存在的唯一理由是把「等级服务 → 现金贷额度服务」这条反向依赖及其异常兜底收在一个类里：
 * {@link CashLoanCalcCreditsService} 依赖 {@code RiskUserGroupEntranceService}，后者又依赖
 * {@link LoanRiskUserGroupService}，直接注入会让等级服务本身参与循环依赖。
 *
 * <p><b>调用约束</b>：必须在等级表写库<b>之前</b>调用——额度失效判定读的就是等级表的 {@code expire_time}，
 * 写库后拿到的是变更后状态。同时不得在监控 {@code writePoint} 的 lambda 里调用，那段代码跑在异步线程。
 */
@Service
@Slf4j
public class UserGroupCreditsSnapshotService {

  @Autowired
  private CashLoanCalcCreditsService cashLoanCalcCreditsService;

  /**
   * 采集等级变更前的额度状态。
   *
   * <p>额度状态计算涉及订单、风控 trace、授信等多张表并会主动抛异常（如循环额度用户复贷授信状态为空），
   * 而调用点位于 {@code @RunInTransaction} 的等级变更事务内，因此这里必须兜住全部异常：
   * 打点是旁路诉求，任何失败都只记 error 日志后返回 null，不得影响等级变更。
   *
   * @param accountId 借款账户 id
   * @return 额度状态，采集失败时为 null
   */
  public CashLoanCalcCreditsVO resolveBeforeChange(Long accountId) {
    if (Objects.isNull(accountId)) {
      return null;
    }
    try {
      return cashLoanCalcCreditsService.getCalcCreditsStatus(accountId);
    } catch (Exception e) {
      log.error("[UserGroupCreditsSnapshot]resolve credits status failed, accountId: {}", accountId, e);
      return null;
    }
  }
}
