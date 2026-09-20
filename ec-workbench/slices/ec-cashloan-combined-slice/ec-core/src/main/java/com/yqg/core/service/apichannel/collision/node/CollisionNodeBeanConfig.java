package com.yqg.core.service.apichannel.collision.node;

import com.yqg.core.service.apichannel.collision.branch.BranchDecisionGroup;
import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @ClassName: CollisionNodeBeanConfig
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 撞库流程节点 Bean 自动注册，注册统一决策树节点
 */
@Configuration
public class CollisionNodeBeanConfig {

    private final ConfigurableListableBeanFactory beanFactory;
    private final List<BranchDecisionGroup> branchDecisionGroups;

    public CollisionNodeBeanConfig(
            ConfigurableListableBeanFactory beanFactory,
            List<BranchDecisionGroup> branchDecisionGroups) {
        this.beanFactory = beanFactory;
        this.branchDecisionGroups = branchDecisionGroups;
    }

    /**
     * 决策树节点由各 BranchDecisionGroup 自注册，通过 decisions() 聚合后统一注册。
     */
    @PostConstruct
    public void registerCollisionNodeAdapters() {
        Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> decisionMap = new HashMap<>();
        branchDecisionGroups.forEach(group -> decisionMap.putAll(group.decisions()));

        decisionMap.forEach((node, decider) ->
                beanFactory.registerSingleton(node.getCode(),
                        new BranchingNodeServiceAdapter(
                                node, decider,
                                node.getTrueUserType(), node.getFalseUserType())));
    }
}
