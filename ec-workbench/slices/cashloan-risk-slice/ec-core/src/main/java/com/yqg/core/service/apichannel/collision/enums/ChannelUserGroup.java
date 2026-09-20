package com.yqg.core.service.apichannel.collision.enums;

/**
 * @ClassName: ChannelUserGroup
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树输出的用户分组，写入 api_channel_check_user_result.channel_user_group
 */
public enum ChannelUserGroup {
    /** 新用户 */
    NEW_USER,
    /** 流失用户 */
    LAPSED_USER,
    /** 可经营老客 */
    MANAGEABLE_OLD_USER,
    /** 历史坏账 */
    BAD_DEBT_HISTORY,
    /** 风控拒量 */
    RISK_REJECTION,
    /** 营销保护期限制 */
    MARKETING_PROTECTION,
    /** 用户信息不可用 */
    USER_INFO_UNAVAILABLE
}
