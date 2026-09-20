package com.yqg.core.service.risk.usergroup.vo;

import com.yqg.core.service.loan.credits.enums.LoanRiskUserGroupEnum;
import com.yqg.ec.common.enums.risk.LoanUserRiskSubmitScene;
import lombok.Data;

/**
 * @author chaoye
 * @date 2025/6/3
 */
@Data
public class UserGroupTriggerResult {
  /**
   * 需要立即触发的降级风控场景
   * 若不需要触发，则为null
   */
  public LoanUserRiskSubmitScene nextRiskSubmitScene;
  /**
   * trigger后，用户最新的归属人群
   * 若未变更，则为null
   */
  public LoanRiskUserGroupEnum updatedUserGroup;
  /**
   * trigger之前，用户的归属人群
   */
  public LoanRiskUserGroupEnum oldUserGroup;
  /**
   * trigger之前，用户的userType
   */
  public String oldUserType;
  /**
   * trigger后，用户最新的userType
   * 若未变更，则为null，与updatedUserGroup语义一致
   */
  public String updatedUserType;

  public static UserGroupTriggerResult from(LoanUserRiskSubmitScene nextRiskSubmitScene, LoanRiskUserGroupEnum updatedUserGroup, LoanRiskUserGroupEnum oldUserGroup) {
    UserGroupTriggerResult result = new UserGroupTriggerResult();
    result.nextRiskSubmitScene = nextRiskSubmitScene;
    result.updatedUserGroup = updatedUserGroup;
    result.oldUserGroup = oldUserGroup;
    return result;
  }

  public static UserGroupTriggerResult fromOnlyUpdated(LoanRiskUserGroupEnum updatedUserGroup, LoanRiskUserGroupEnum oldUserGroup) {
    UserGroupTriggerResult result = new UserGroupTriggerResult();
    result.updatedUserGroup = updatedUserGroup;
    result.oldUserGroup = oldUserGroup;
    return result;
  }

  public static UserGroupTriggerResult fromNoUpdate(LoanRiskUserGroupEnum oldUserGroup) {
    UserGroupTriggerResult result = new UserGroupTriggerResult();
    result.oldUserGroup = oldUserGroup;
    return result;
  }

}
