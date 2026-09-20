package com.yqg.core.service.apichannel.collision.context;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.platform.process_manage.entity.ProcessNodeContext;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 撞库流程节点上下文，每次流转到新节点时由框架注入。
 *
 * <ul>
 *   <li>{@code resultHolder} — 节点间共享的结果容器，命中规则时写入用户类型</li>
 *   <li>{@code conditionVo} — 当前请求的业务校验条件</li>
 *   <li>{@code pathOrderNum} — 当前节点在流程路径中的序号（从 0 开始，框架自动维护）</li>
 *   <li>{@code nodeTrace} — 流程经过的节点轨迹列表，用于日志追踪与问题排查</li>
 * </ul>
 *
 * @see CollisionProcessNode
 * @see CollisionProcessContext
 */
@Getter
public class CollisionNodeContext implements ProcessNodeContext<CollisionProcessNode> {

    private final AtomicReference<ApiChannelUserType> resultHolder;
    private final ApiChannelUserCheckConditionVO conditionVo;
    private CollisionProcessNode currProcessNode;
    private Integer pathOrderNum;
    private final List<String> nodeTrace = new ArrayList<>();

    public CollisionNodeContext(AtomicReference<ApiChannelUserType> resultHolder,
                                ApiChannelUserCheckConditionVO conditionVo,
                                CollisionProcessNode currProcessNode) {
        this.resultHolder = resultHolder;
        this.conditionVo = conditionVo;
        this.currProcessNode = currProcessNode;
    }

    @Override
    public void setCurrProcessNode(CollisionProcessNode processNode) {
        this.currProcessNode = processNode;
    }

    @Override
    public void setPathOrderNum(Integer pathOrderNum) {
        this.pathOrderNum = pathOrderNum;
    }

    /**
     * 深拷贝当前上下文（分支并行执行时使用），{@code resultHolder} 独立复制，
     * {@code conditionVo} 通过快照复制，{@code nodeTrace} 保留已有轨迹。
     */
    @Override
    public CollisionNodeContext copy() {
        CollisionNodeContext copied = new CollisionNodeContext(
                new AtomicReference<>(this.resultHolder.get()),
                this.conditionVo.copySnapshot(), this.currProcessNode);
        copied.setPathOrderNum(this.pathOrderNum);
        copied.nodeTrace.addAll(this.nodeTrace);
        return copied;
    }

    /** 追加一条节点轨迹记录，通常在节点执行完毕后调用 */
    public void appendNodeTrace(String nodeTraceItem) {
        this.nodeTrace.add(nodeTraceItem);
    }
}
