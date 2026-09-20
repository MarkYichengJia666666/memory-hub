package com.yqg.core.service.risk.usergroup;

import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.core.service.risk.riskflow.trace.LoanUserTagData;
import com.yqg.core.service.risk.usergroup.vo.UserGroupTriggerResult;
import com.yqg.core.service.riskprocessor.infra.RiskProcessParam;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;

/**
 * @author chaoye
 * @date 2025/5/30
 */
public interface IRiskUserGroupProcessor {

  /**
   * 当前人群枚举
   */
  LoanRiskUserGroupEnum getGroup();

  /**
   * 风控输出结果后升降级处理
   */
  UserGroupTriggerResult triggerAfterRisk(LoanUserRiskTraceVO previousRiskTraceVO, LoanUserTagData data);

  /**
   * 提交风控前升降级处理
   *
   * @param riskProcessParam 风控处理参数
   * @param submitScene      提交场景
   * @param readOnly         是否只读。如果为 true，则只计算并返回升降级结果，不执行实际的数据库更新操作；如果为 false，则会执行相应的数据库更新。
   * @return 升降级处理结果
   */
  UserGroupTriggerResult triggerBeforeSubmitRisk(RiskProcessParam riskProcessParam, LoanUserRiskSubmitScene submitScene, boolean readOnly);

  /**
   * 打款完成升降级处理，当前只变更回捞有效期
   */
  UserGroupTriggerResult onOrderPayoutSuccess(CashLoanOrderVO cashLoanOrderVO);

  /**
   * 常规用户加入次新之后会修改 userType
   */
  void triggerBeforeSubmitRiskForChangeSubNewUserType(RiskProcessParam riskProcessParam, LoanUserRiskType riskType);
}
