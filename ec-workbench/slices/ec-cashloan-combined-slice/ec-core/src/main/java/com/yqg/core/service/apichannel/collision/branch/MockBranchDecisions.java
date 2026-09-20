package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @ClassName: MockBranchDecisions
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树 Mock 阶段（1 个节点），仅检查 mock 通过名单
 */
@Component
public class MockBranchDecisions extends AbstractBranchDecision {

    @Override
    public Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> decisions() {
        Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> map = new HashMap<>();
        map.put(CollisionProcessNode.MOCK_CHECK, this::mockCheck);
        return map;
    }

    /**
     * mock_check — 按渠道判断手机号 MD5 或 NIK MD5 是否命中 Mock 通过名单
     */
    private boolean mockCheck(ApiChannelUserCheckConditionVO cond) {
        return apiChannelConfig.inCollisionMockApproveList(
            cond.getMobileNumberMd5(), cond.getIdentityNumberMd5(), cond.getChannel());
    }
}
