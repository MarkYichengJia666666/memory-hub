package com.yqg.core.service.apichannel.collision.service;

import com.yqg.core.service.apichannel.collision.context.CollisionNodeContext;
import com.yqg.core.service.apichannel.collision.context.CollisionProcessContext;
import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.collision.enums.CollisionProcessEnum;
import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.platform.process_manage.service.ProcessManageService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 撞库新流程执行服务，负责将业务条件提交给流程引擎并收集执行结果。
 *
 * <p>将 {@link ApiChannelUserCheckConditionVO} 与流程定义（{@link CollisionProcessEnum}）
 * 交给 {@link ProcessManageService} 调度，执行完成后收集用户类型、流程路径和节点轨迹。
 */
@Slf4j
@Component
public class CollisionProcessService {

    @Autowired
    private ProcessManageService processManageService;

    private static final ParameterizedTypeReference<CollisionProcessNode> TYPE_REF =
            new ParameterizedTypeReference<CollisionProcessNode>() {};

    /** 流程执行结果，包含用户类型、流程路径和节点轨迹。 */
    @Getter
    @AllArgsConstructor
    public static class ExecutionResult {
        private final ApiChannelUserType userType;
        private final String processPath;
        private final String nodeTrace;
    }

    /** 执行撞库流程，返回结果与路径信息；若无节点命中则兜底为 REJECT（与旧逻辑一致）。 */
    public ExecutionResult execute(ApiChannelUserCheckConditionVO conditionVO,
                                   CollisionProcessEnum processEnum) {
        AtomicReference<ApiChannelUserType> resultHolder = new AtomicReference<>();

        CollisionNodeContext nodeContext = new CollisionNodeContext(resultHolder, conditionVO, null);
        CollisionProcessContext processContext = CollisionProcessContext.builder().build();
        processContext.setProcessNodeContext(nodeContext);

        processManageService.getNextProcessNode(
                processEnum.getCode(),
                processEnum.getVersion(),
                processContext,
                TYPE_REF
        );

        String processPath = formatProcessPath(processContext.getProcessPath());
        String nodeTrace = String.join(" -> ", nodeContext.getNodeTrace());

        ApiChannelUserType result = resultHolder.get();
        if (result == null) {
            log.warn("CollisionProcessService result is null for process={}, fallback to REJECT",
                    processEnum.getCode());
            return new ExecutionResult(ApiChannelUserType.REJECT, processPath, nodeTrace);
        }
        return new ExecutionResult(result, processPath, nodeTrace);
    }

    /** 将流程路径节点列表格式化为 " -> " 连接的可读字符串，并补全 start 前缀以与旧逻辑路径对齐。 */
    private static String formatProcessPath(List<CollisionProcessNode> path) {
        if (CollectionUtils.isEmpty(path)) {
            return "";
        }
        String joined = path.stream()
                .map(CollisionProcessNode::getCode)
                .collect(Collectors.joining(" -> "));
        // process-manage 框架不会将 start 节点加入 processPath，手动补全以与旧逻辑路径对齐
        if (!joined.startsWith(CollisionProcessNode.START.getCode())) {
            joined = CollisionProcessNode.START.getCode() + " -> " + joined;
        }
        return joined;
    }
}
