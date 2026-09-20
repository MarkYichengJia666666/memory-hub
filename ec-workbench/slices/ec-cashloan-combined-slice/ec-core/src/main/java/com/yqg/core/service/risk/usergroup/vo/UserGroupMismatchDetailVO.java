package com.yqg.core.service.risk.usergroup.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户组不匹配详细信息
 */
@Getter
@Setter
public class UserGroupMismatchDetailVO {
    /**
     * 用户ID
     */
    private Long accountId;

    /**
     * 首次不匹配的UserGroup
     */
    private String firstNotMatchUserGroup;
    /**
     * 首次不匹配的UserType
     */
    private String firstNotMatchUserType;
    /**
     * 首次不匹配的TraceId
     */
    private Long firstNotMatchTraceId;
    /**
     * 首次不匹配的RiskType
     */
    private String firstNotMatchRiskType;
    /**
     * 首次不匹配时，trace输出是否可借
     */
    private Boolean firstNotMatchCanGetLoanByTraceOutput;

    /**
     * 当前UserGroup
     */
    private String currentUserGroup;
    /**
     * 当前UserType
     */
    private String currentUserType;
    /**
     * 当前TraceId
     */
    private Long currentTraceId;
    /**
     * 当前不匹配的最新RiskType
     */
    private String currentRiskType;
    /**
     * 当前trace输出是否可借
     */
    private Boolean currentCanGetLoanByTraceOutput;

    /**
     * 任务触发时间
     */
    private Long jobTriggerTime;
}
