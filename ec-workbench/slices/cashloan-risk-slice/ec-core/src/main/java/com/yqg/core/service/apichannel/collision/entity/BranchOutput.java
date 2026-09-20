package com.yqg.core.service.apichannel.collision.entity;

import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName: BranchOutput
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树节点输出，与线性链的 RuleOutput(hit/miss) 互不干扰
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BranchOutput {

    /** 决策结果：true 表示条件成立 */
    private final boolean result;

    /** 叶子节点输出的用户标签；中间决策节点为 null */
    private final ApiChannelUserType userType;

    public static BranchOutput ofTrue(ApiChannelUserType userType) {
        return new BranchOutput(true, userType);
    }

    public static BranchOutput ofFalse(ApiChannelUserType userType) {
        return new BranchOutput(false, userType);
    }
}
