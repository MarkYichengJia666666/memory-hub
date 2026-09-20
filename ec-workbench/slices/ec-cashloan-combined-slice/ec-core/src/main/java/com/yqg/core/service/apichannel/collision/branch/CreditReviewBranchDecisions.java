package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @ClassName: CreditReviewBranchDecisions
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树风控审核阶段（1 个节点），判断用户是否处于风控审核中
 */
@Component
public class CreditReviewBranchDecisions extends AbstractBranchDecision {

    @Override
    public Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> decisions() {
        Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> map = new HashMap<>();
        map.put(CollisionProcessNode.BRANCH_RISK_REVIEWING, this::riskReviewing);
        return map;
    }

    /**
     * risk_reviewing — 风控审核中？
     *
     * <p>Lazada 下单撞库跳过前置步骤后，先刷新额度，保持旧链路刷新授信快照的语义。
     */
    private boolean riskReviewing(ApiChannelUserCheckConditionVO cond) {
        if (isLazadaCreateOrderUpgrade(cond)) {
            refreshCreditData(cond);
        }
        LoanUserCreditsInfoVO credits = getCreditsInfoVOOrNull(cond);
        return credits != null && credits.isCreditsReview();
    }
}
