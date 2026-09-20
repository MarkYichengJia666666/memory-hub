package com.yqg.core.service.apichannel.collision.node;

import com.yqg.core.service.apichannel.collision.context.CollisionNodeContext;
import com.yqg.core.service.apichannel.collision.entity.BranchOutput;
import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.platform.process_manage.service.ProcessNodeService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

/**
 * @ClassName: BranchingNodeServiceAdapter
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树节点适配器，将决策函数适配为 ProcessNodeService，基于 BranchOutput 输出
 */
@AllArgsConstructor
@Slf4j
public class BranchingNodeServiceAdapter
        implements ProcessNodeService<CollisionProcessNode, CollisionNodeContext, BranchOutput> {

    private final CollisionProcessNode processNode;
    /** 由 BranchDecisionGroup 的方法引用提供 */
    private final Predicate<ApiChannelUserCheckConditionVO> decider;
    private final ApiChannelUserType trueUserType;
    private final ApiChannelUserType falseUserType;

    @Override
    public boolean isProcessNodeFinished(CollisionNodeContext ctx) {
        return true;
    }

    @Override
    public BranchOutput after(CollisionNodeContext ctx) {
        boolean result = decider.test(ctx.getConditionVo());
        ApiChannelUserType outputTag = result ? trueUserType : falseUserType;

        if (outputTag != null) {
            ctx.getResultHolder().set(outputTag);
        }

        String branch = result ? "TRUE" : "FALSE";
        String tag = outputTag != null ? outputTag.name() : "CONTINUE";
        ctx.appendNodeTrace(processNode.getCode() + ":" + branch + "(" + tag + ")");

        log.info("[CollisionBranchTrace] checkerType={}, node={}, branch={}, tag={}",
                ctx.getConditionVo().getCheckerType(), processNode.getCode(), branch, tag);

        return result ? BranchOutput.ofTrue(outputTag) : BranchOutput.ofFalse(outputTag);
    }

    @Override
    public void before(CollisionNodeContext ctx) {
    }

    @Override
    public CollisionProcessNode getProcessNode() {
        return processNode;
    }

    @Override
    public Class<?> getOutputType() {
        return BranchOutput.class;
    }
}
