package com.yqg.core.service.apichannel.collision.node;

import com.yqg.core.service.apichannel.collision.context.CollisionNodeContext;
import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.collision.entity.RuleOutput;
import com.yqg.platform.process_manage.service.ProcessNodeService;
import org.springframework.stereotype.Component;

/**
 * 撞库流程的终止哨兵节点，对应 {@link CollisionProcessNode#END}。
 *
 * <p>本身不包含任何业务判定逻辑，仅作为流程引擎的出口标记。
 * {@link #isProcessNodeFinished} 返回 {@code false}，表示该节点无需执行 {@link #after}，
 * 流程引擎到达此节点时即视为流程结束。
 */
@Component
public class EndNodeService
        implements ProcessNodeService<CollisionProcessNode, CollisionNodeContext, RuleOutput> {

    /**
     * 返回 {@code false}，表示该节点不需要执行后续逻辑。
     * 与 {@link StartNodeService} 和 {@link BranchingNodeServiceAdapter} 返回 {@code true} 不同，
     * END 节点返回 {@code false} 意味着流程引擎跳过 {@link #after}，直接结束流程。
     */
    @Override
    public boolean isProcessNodeFinished(CollisionNodeContext ctx) {
        return false;
    }

    /** 不会被调用（因为 {@link #isProcessNodeFinished} 返回 {@code false}），仅为满足接口契约。 */
    @Override
    public RuleOutput after(CollisionNodeContext ctx) {
        return null;
    }

    @Override
    public void before(CollisionNodeContext ctx) {
        // no-op
    }

    @Override
    public CollisionProcessNode getProcessNode() {
        return CollisionProcessNode.END;
    }
}
