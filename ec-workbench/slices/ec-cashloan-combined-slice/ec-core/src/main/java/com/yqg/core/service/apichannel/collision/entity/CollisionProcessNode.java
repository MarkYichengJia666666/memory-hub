package com.yqg.core.service.apichannel.collision.entity;

import com.yqg.core.service.apichannel.collision.enums.CollisionProcessEnum;
import com.yqg.core.service.apichannel.enums.ApiChannelUserType;
import com.yqg.platform.process_manage.entity.Process;
import com.yqg.platform.process_manage.entity.ProcessNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @ClassName: CollisionProcessNode
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 撞库流程节点枚举，仅保留统一决策树实际调度的节点
 */
@Getter
@AllArgsConstructor
public enum CollisionProcessNode implements ProcessNode {

    START("start", "开始", null, null),
    END("end", "结束", null, null),

    // ========== 决策树节点（trueUserType/falseUserType 用于标记叶子输出） ==========

    MOCK_CHECK("mock_check", "Mock判断(决策树)",
            ApiChannelUserType.MOCK_APPROVE, null),
    BRANCH_MATCH_MOBILE("match_mobile", "匹配手机号",
            null, ApiChannelUserType.NEW),
    BRANCH_DIFFERENT_NIK("different_nik", "NIK不一致",
            ApiChannelUserType.DIFFERENT_KTP, null),
    BRANCH_ONLY_FINANCING("only_financing", "纯理财用户",
            ApiChannelUserType.ONLY_FINANCING_USER, null),
    BRANCH_IS_DELETED("is_deleted", "已注销",
            ApiChannelUserType.DELETED, null),
    BRANCH_IS_COMPLETED("is_completed", "已完件",
            null, null),
    BRANCH_REGISTER_PERIOD("register_period", "注册间隔<=15天",
            ApiChannelUserType.NO_AUTH_AFTER_REGISTER, ApiChannelUserType.NO_AUTH_AFTER_REGISTER_PASS),
    BRANCH_RISK_REVIEWING("risk_reviewing", "风控审核中",
            ApiChannelUserType.CREDIT_IN_REVIEW, null),
    BRANCH_FIRST_LOAN_SUCCESS("first_loan_success", "首贷放款成功",
            null, null),
    BRANCH_FIRST_LOAN_REJECTED("first_loan_rejected", "首贷trace被拒",
            null, null),
    BRANCH_FIRST_CAN_RETRY("first_can_retry", "可重审(首贷)",
            null, null),
    BRANCH_FIRST_REJECT_720D("first_reject_long", "被拒<=720天(不可重审)",
            ApiChannelUserType.CREDIT_REJECT_FREEZE, ApiChannelUserType.CREDIT_REJECTED_FOREVER),
    BRANCH_FIRST_REJECT_180D("first_reject_short", "被拒<=180天(可重审)",
            ApiChannelUserType.API_CHANNEL_FIRST_LOAN_RE_CREDIT, ApiChannelUserType.FIRST_LOAN_REJECT_LONG_CAN_RETRY),
    BRANCH_CREDIT_PASS_NO_ORDER("credit_pass_no_order", "授信通过未下单",
            null, ApiChannelUserType.INIT_ORDER),
    BRANCH_CREDIT_PERIOD("credit_period", "授信间隔<=15天",
            ApiChannelUserType.NO_ORDER_AFTER_AUTH, ApiChannelUserType.NO_ORDER_AFTER_AUTH_PASS),
    BRANCH_IS_IN_LOAN("is_in_loan", "在贷",
            ApiChannelUserType.IN_LOAN, null),
    BRANCH_MAX_OVERDUE("max_overdue", "最大逾期>7天",
            null, null),
    BRANCH_SETTLE_720D("settle_bad_debt", "结清<=720天",
            ApiChannelUserType.SETTLED_BAD_DEBT_SHORT, ApiChannelUserType.SETTLED_BAD_DEBT_LONG),
    BRANCH_RELOAN_REJECTED("reloan_rejected", "复贷trace被拒",
            null, null),
    BRANCH_RELOAN_CAN_RETRY("reloan_can_retry", "可重审(复贷)",
            null, null),
    BRANCH_RELOAN_REJECT_720D("reloan_reject_long", "被拒<=720天(复贷不可重审)",
            ApiChannelUserType.RELOAN_REJECT_FREEZE, ApiChannelUserType.RELOAN_REJECTED_FOREVER),
    BRANCH_RELOAN_REJECT_180D("reloan_reject_short", "被拒<=180天(复贷可重审)",
            ApiChannelUserType.API_CHANNEL_RELOAN_RE_CREDIT, ApiChannelUserType.RELOAN_REJECT_LONG_CAN_RETRY),
    BRANCH_RELOAN_CREDIT_NO_ORDER("reloan_credit_no_order", "授信通过未下单(复贷)",
            ApiChannelUserType.NO_ORDER_AFTER_RELOAN_COMPLETED, ApiChannelUserType.SETTLED_NO_BAD_DEBT_OTHER);

    private final String code;
    private final String name;
    /** 决策树节点 true 分支到叶子时输出的用户标签；null 表示继续到子树 */
    private final ApiChannelUserType trueUserType;
    /** 决策树节点 false 分支到叶子时输出的用户标签；null 表示继续到子树 */
    private final ApiChannelUserType falseUserType;

    // 使用 unmodifiableMap 防止共享映射被意外修改，保证线程安全和枚举不可变语义
    private static final Map<String, CollisionProcessNode> CODE_MAP =
            Collections.unmodifiableMap(
                    Arrays.stream(values()).collect(
                            Collectors.toMap(
                                    CollisionProcessNode::getCode,
                                    Function.identity()
                            )
                    )
            );

    // 使用 unmodifiableList 防止外部通过 getParentProcessList() 获取引用后修改列表内容
    private static final List<Process> ALL_PROCESSES =
            Collections.unmodifiableList(
                    Arrays.asList(CollisionProcessEnum.values())
            );

    public static CollisionProcessNode fromCode(String code) {
        CollisionProcessNode node = CODE_MAP.get(code);
        if (node == null) {
            throw new IllegalArgumentException(
                    "Unknown collision process node code: " + code);
        }
        return node;
    }

    /**
     * 返回全部流程列表。框架仅用此方法做注册发现（将节点注册到所有流程的候选池中），
     * 实际流转路径由各流程的 PlantUML 拓扑文件约束，不会导致节点被错误调用。
     */
    @Override
    public List<Process> getParentProcessList() {
        return ALL_PROCESSES;
    }

    @Override
    public ProcessNode getStartNode() {
        return START;
    }

    @Override
    public ProcessNode getEndNode() {
        return END;
    }
}
