package com.yqg.core.service.apichannel.collision.enums;

import com.google.common.collect.ImmutableSet;
import com.yqg.core.service.apichannel.enums.ApiChannelUserStatusCheckerType;
import com.yqg.platform.process_manage.entity.Process;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * @ClassName: CollisionProcessEnum
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 撞库流程枚举，全量走统一决策树
 */
@Getter
@AllArgsConstructor
public enum CollisionProcessEnum implements Process {

    COLLISION_DECISION_TREE("collision.decision_tree", 1, "统一决策树撞库");

    private static final String FOLDER_PATH = "collision/collision";

    /** 决策树支持的全部 checkerType */
    private static final Set<ApiChannelUserStatusCheckerType> SUPPORTED_CHECKER_TYPES = ImmutableSet.of(
            ApiChannelUserStatusCheckerType.SUBMIT_APPLICATION,
            ApiChannelUserStatusCheckerType.LAZADA_CREATE_ORDER,
            ApiChannelUserStatusCheckerType.LAZADA_PRE_SELECT,
            ApiChannelUserStatusCheckerType.LAZADA_SECOND_PRE_SELECT
    );

    private final String code;
    private final Integer version;
    private final String name;

    public String getFolderPath() {
        return FOLDER_PATH;
    }

    /** 判断给定的 checkerType 是否走撞库决策树流程。 */
    public static boolean isSupported(ApiChannelUserStatusCheckerType checkerType) {
        return checkerType != null && SUPPORTED_CHECKER_TYPES.contains(checkerType);
    }
}
