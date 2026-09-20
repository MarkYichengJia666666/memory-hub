package com.yqg.core.service.apichannel.collision.condition;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.collision.entity.RuleOutput;
import com.yqg.core.service.apichannel.collision.enums.CollisionProcessEnum;
import com.yqg.platform.process_manage.entity.Process;
import com.yqg.platform.process_manage.entity.ProcessContext;
import com.yqg.platform.process_manage.service.TransCondition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 规则命中转移条件（code="hit"）。
 * 当上一个流程节点的输出 {@link RuleOutput#isHit()} 为 true 时，该条件满足，流程将沿命中分支继续流转。
 */
@Component
public class RuleHitCondition implements TransCondition<CollisionProcessNode, RuleOutput> {

    private static final String CODE = "hit";
    private static final List<Process> PARENT_PROCESSES =
            Collections.unmodifiableList(Arrays.asList(CollisionProcessEnum.values()));

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getName() {
        return "规则命中";
    }

    @Override
    public List<Process> getParentProcessList() {
        return PARENT_PROCESSES;
    }

    @Override
    public boolean isSatisfied(ProcessContext<CollisionProcessNode> context) {
        Object output = context.getPreviousNodeOutput();
        if (output instanceof RuleOutput) {
            return ((RuleOutput) output).isHit();
        }
        return false;
    }

    @Override
    public Class<?> getSourceOutputType() {
        return RuleOutput.class;
    }
}
