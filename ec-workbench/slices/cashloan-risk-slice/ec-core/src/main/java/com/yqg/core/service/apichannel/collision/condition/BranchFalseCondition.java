package com.yqg.core.service.apichannel.collision.condition;

import com.yqg.core.service.apichannel.collision.entity.BranchOutput;
import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.collision.enums.CollisionProcessEnum;
import com.yqg.platform.process_manage.entity.Process;
import com.yqg.platform.process_manage.entity.ProcessContext;
import com.yqg.platform.process_manage.service.TransCondition;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @ClassName: BranchFalseCondition
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树 false 分支转移条件（code="false"），基于 BranchOutput 判断
 */
@Component
public class BranchFalseCondition implements TransCondition<CollisionProcessNode, BranchOutput> {

    private static final String CODE = "false";
    private static final List<Process> PARENT_PROCESSES =
            Collections.unmodifiableList(Arrays.asList(CollisionProcessEnum.values()));

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getName() {
        return "条件不成立";
    }

    @Override
    public List<Process> getParentProcessList() {
        return PARENT_PROCESSES;
    }

    @Override
    public boolean isSatisfied(ProcessContext<CollisionProcessNode> context) {
        Object output = context.getPreviousNodeOutput();
        if (output instanceof BranchOutput) {
            return !((BranchOutput) output).isResult();
        }
        return false;
    }

    @Override
    public Class<?> getSourceOutputType() {
        return BranchOutput.class;
    }
}
