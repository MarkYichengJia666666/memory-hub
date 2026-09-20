package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @ClassName: ReloanBranchDecisions
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树复贷阶段（5 个节点）：被拒、可重审、被拒天数、授信未下单
 */
@Component
public class ReloanBranchDecisions extends AbstractBranchDecision {

    @Override
    public Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> decisions() {
        Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> map = new HashMap<>();
        map.put(CollisionProcessNode.BRANCH_RELOAN_REJECTED, this::reloanRejected);
        map.put(CollisionProcessNode.BRANCH_RELOAN_CAN_RETRY, this::reloanCanRetry);
        map.put(CollisionProcessNode.BRANCH_RELOAN_REJECT_720D, this::reloanReject720d);
        map.put(CollisionProcessNode.BRANCH_RELOAN_REJECT_180D, this::reloanReject180d);
        map.put(CollisionProcessNode.BRANCH_RELOAN_CREDIT_NO_ORDER, this::reloanCreditNoOrder);
        return map;
    }

    /**
     * reloan_rejected — 复贷 trace 为被拒？优先取 reloanStatus
     */
    private boolean reloanRejected(ApiChannelUserCheckConditionVO cond) {
        LoanUserCreditsInfoVO credits = getCreditsInfoVOOrNull(cond);
        if (credits == null) {
            return false;
        }
        LoanCreditsStatus effective = credits.reloanStatus != null ? credits.reloanStatus : credits.creditsStatus;
        return effective == LoanCreditsStatus.REJECTED;
    }

    /**
     * reloan_can_retry — 可重审？timeReapply > 0 表示非永久拒
     */
    private boolean reloanCanRetry(ApiChannelUserCheckConditionVO cond) {
        LoanUserCreditsInfoVO credits = getCreditsInfoVOOrNull(cond);
        return credits != null && credits.timeReapply != null && credits.timeReapply > 0;
    }

    /**
     * reloan_reject_long — 被拒间隔 <= 720 天？（复贷不可重审）
     */
    private boolean reloanReject720d(ApiChannelUserCheckConditionVO cond) {
        LoanAccountVO account = getLoanAccountVOOrThrow(cond);
        LoanUserRiskTraceVO trace = getActiveRiskTraceOrNull(cond);
        if (trace == null || trace.timeUpdated == null) {
            return false;
        }
        int days = Clock.getDaysBetween(trace.timeUpdated, cond.getCurrentTimeMillis(), account.sdkType.getTimeZone());
        return days <= apiChannelConfig.getReloanRejectNoRetryLongDays(cond.getChannel());
    }

    /**
     * reloan_reject_short — 被拒间隔 <= 180 天？（复贷可重审）
     */
    private boolean reloanReject180d(ApiChannelUserCheckConditionVO cond) {
        LoanAccountVO account = getLoanAccountVOOrThrow(cond);
        LoanUserRiskTraceVO trace = getActiveRiskTraceOrNull(cond);
        if (trace == null || trace.timeUpdated == null) {
            return false;
        }
        int days = Clock.getDaysBetween(trace.timeUpdated, cond.getCurrentTimeMillis(), account.sdkType.getTimeZone());
        return days <= apiChannelConfig.getReloanRejectRetryShortDays(cond.getChannel());
    }

    /**
     * reloan_credit_no_order — 授信通过未下单？（复贷）
     */
    private boolean reloanCreditNoOrder(ApiChannelUserCheckConditionVO cond) {
        return !havingUncompletedOrder(cond);
    }
}
