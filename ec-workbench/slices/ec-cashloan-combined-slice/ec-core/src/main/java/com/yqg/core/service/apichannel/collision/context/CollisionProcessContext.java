package com.yqg.core.service.apichannel.collision.context;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.platform.process_manage.entity.ProcessContext;
import com.yqg.platform.process_manage.entity.impl.AbstractProcessContext;
import lombok.experimental.SuperBuilder;

/**
 * 撞库流程级上下文，承载整个流程执行所需的共享数据。
 *
 * <p>继承 {@link AbstractProcessContext}，框架自动维护执行路径和上一节点产出；
 * 业务数据（撞库校验条件 {@code conditionVO}）由 {@link CollisionNodeContext} 单一持有，
 * 避免 copy 时出现引用分裂。</p>
 *
 * @see CollisionNodeContext
 * @see CollisionProcessNode
 */
// 使用 @SuperBuilder 而非 @Builder，因为本类继承了 AbstractProcessContext，
// @Builder 不会包含父类字段，而 @SuperBuilder 能将父类与子类字段统一纳入 Builder 链。
@SuperBuilder
public class CollisionProcessContext extends AbstractProcessContext<CollisionProcessNode, CollisionNodeContext> {

    /**
     * 供流程引擎在执行前复制上下文使用。
     *
     * <p>{@code process-manage} 在调度时会先对 {@link ProcessContext} 调用 {@code copy()}，再在副本上
     * 维护 {@code processPath}、驱动节点服务并写入 {@link CollisionNodeContext#getResultHolder()} 等。
     *
     * <p>撞库流程为单线程、线性拓扑，不存在引擎内并行分支共享同一上下文的问题。</p>
     */
    @Override
    public ProcessContext<CollisionProcessNode> copy() {
        return this;
    }
}
