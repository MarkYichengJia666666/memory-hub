package com.yqg.core.service.apichannel.collision.orchestrator;

import com.yqg.core.service.apichannel.collision.enums.CollisionProcessEnum;
import com.yqg.core.service.apichannel.collision.service.CollisionProcessService;
import com.yqg.core.service.apichannel.collision.service.CollisionProcessService.ExecutionResult;
import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @ClassName: CollisionOrchestrator
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 撞库流程编排器，统一走决策树流程
 */
@Slf4j
@Component
public class CollisionOrchestrator {

    @Autowired
    private CollisionProcessService processService;

    /**
     * 撞库流程入口：统一执行决策树流程。
     *
     * @param conditionVO 撞库校验条件
     * @return 用户标签类型
     */
    public ApiChannelUserType execute(ApiChannelUserCheckConditionVO conditionVO) {
        conditionVO.setUpgradeFlow(true);
        return executeProcess(conditionVO, CollisionProcessEnum.COLLISION_DECISION_TREE);
    }

    private ApiChannelUserType executeProcess(ApiChannelUserCheckConditionVO conditionVO,
                                               CollisionProcessEnum processEnum) {
        long start = System.currentTimeMillis();
        ExecutionResult result = processService.execute(conditionVO, processEnum);
        long duration = System.currentTimeMillis() - start;
        log.info("[CollisionOrchestrator] result={}, process={}, durationMs={}, path={}, nodeTrace={}, condition={}",
                result.getUserType(), processEnum.getCode(), duration,
                result.getProcessPath(), result.getNodeTrace(), conditionVO.toLogString());
        return result.getUserType();
    }
}
