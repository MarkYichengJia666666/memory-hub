package com.yqg.core.service.apichannel.collision.node;

import com.yqg.core.service.apichannel.collision.context.CollisionNodeContext;
import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.collision.entity.RuleOutput;
import com.yqg.platform.process_manage.service.ProcessNodeService;
import org.springframework.stereotype.Component;

/**
 * 撞库流程的起始哨兵节点，对应 {@link CollisionProcessNode#START}。
 *
 * <p>本身不包含任何业务判定逻辑，仅作为流程引擎的入口标记。
 * {@link #after} 始终返回 {@code RuleOutput.miss()}，使流程引擎沿默认（miss）分支继续流转到第一个实际规则节点。
 */
@Component
public class StartNodeService
        implements ProcessNodeService<CollisionProcessNode, CollisionNodeContext, RuleOutput> {

    @Override
    public boolean isProcessNodeFinished(CollisionNodeContext ctx) {
        return true;
    }

    @Override
    public RuleOutput after(CollisionNodeContext ctx) {
        return RuleOutput.miss();
    }

    @Override
    public void before(CollisionNodeContext ctx) {
        // no-op
    }

    @Override
    public CollisionProcessNode getProcessNode() {
        return CollisionProcessNode.START;
    }
}
