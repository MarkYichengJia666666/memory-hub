package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;

import java.util.Map;
import java.util.function.Predicate;

/**
 * @ClassName: BranchDecisionGroup
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树领域分组接口，每个实现类按业务阶段聚合多个决策节点的判断逻辑
 */
public interface BranchDecisionGroup {

    /**
     * 返回本组所有决策节点及其判断函数
     *
     * @return 节点 → 判断函数 的映射
     */
    Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> decisions();
}
