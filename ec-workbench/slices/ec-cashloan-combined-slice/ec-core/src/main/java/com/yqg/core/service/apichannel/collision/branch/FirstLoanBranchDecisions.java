package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.model.sql.loan.account.enums.LoanCreditsStatus;
import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.enums.ApiChannelUserStatusCheckerType;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @ClassName: FirstLoanBranchDecisions
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树首贷阶段（7 个节点）：放款成功、被拒、可重审、被拒天数、授信未下单、授信保护期
 */
@Component
public class FirstLoanBranchDecisions extends AbstractBranchDecision {

    @Override
    public Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> decisions() {
        Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> map = new HashMap<>();
        map.put(CollisionProcessNode.BRANCH_FIRST_LOAN_SUCCESS, this::firstLoanSuccess);
        map.put(CollisionProcessNode.BRANCH_FIRST_LOAN_REJECTED, this::firstLoanRejected);
        map.put(CollisionProcessNode.BRANCH_FIRST_CAN_RETRY, this::firstCanRetry);
        map.put(CollisionProcessNode.BRANCH_FIRST_REJECT_720D, this::firstReject720d);
        map.put(CollisionProcessNode.BRANCH_FIRST_REJECT_180D, this::firstReject180d);
        map.put(CollisionProcessNode.BRANCH_CREDIT_PASS_NO_ORDER, this::creditPassNoOrder);
        map.put(CollisionProcessNode.BRANCH_CREDIT_PERIOD, this::creditPeriod);
        return map;
    }

    /**
     * first_loan_success — 首贷放款成功？存在 COMPLETE 或 READY 状态的订单即表示首贷已放款
     */
    private boolean firstLoanSuccess(ApiChannelUserCheckConditionVO cond) {
        CashLoanOrderVO completedOrder = getLastCompletedOrderOrNull(cond);
        if (completedOrder != null) {
            return true;
        }
        List<CashLoanOrderVO> uncompleted = getUncompletedOrderList(cond);
        return uncompleted.stream().anyMatch(o -> o.status == CashLoanOrderStatus.READY);
    }

    /**
     * first_loan_rejected — 首贷 trace 为被拒？
     */
    private boolean firstLoanRejected(ApiChannelUserCheckConditionVO cond) {
        LoanUserCreditsInfoVO credits = getCreditsInfoVOOrNull(cond);
        return credits != null && credits.creditsStatus == LoanCreditsStatus.REJECTED;
    }

    /**
     * first_can_retry — 可重审？timeReapply > 0 表示非永久拒
     */
    private boolean firstCanRetry(ApiChannelUserCheckConditionVO cond) {
        LoanUserCreditsInfoVO credits = getCreditsInfoVOOrNull(cond);
        return credits != null && credits.timeReapply != null && credits.timeReapply > 0;
    }

    /**
     * first_reject_long — 被拒间隔 <= 720 天？（不可重审）
     */
    private boolean firstReject720d(ApiChannelUserCheckConditionVO cond) {
        LoanAccountVO account = getLoanAccountVOOrThrow(cond);
        LoanUserRiskTraceVO trace = getActiveRiskTraceOrNull(cond);
        if (trace == null || trace.timeUpdated == null) {
            return false;
        }
        int days = Clock.getDaysBetween(trace.timeUpdated, cond.getCurrentTimeMillis(), account.sdkType.getTimeZone());
        return days <= apiChannelConfig.getFirstLoanRejectNoRetryLongDays(cond.getChannel());
    }

    /**
     * first_reject_short — 被拒间隔 <= 180 天？（可重审）
     */
    private boolean firstReject180d(ApiChannelUserCheckConditionVO cond) {
        LoanAccountVO account = getLoanAccountVOOrThrow(cond);
        LoanUserRiskTraceVO trace = getActiveRiskTraceOrNull(cond);
        if (trace == null || trace.timeUpdated == null) {
            return false;
        }
        int days = Clock.getDaysBetween(trace.timeUpdated, cond.getCurrentTimeMillis(), account.sdkType.getTimeZone());
        return days <= apiChannelConfig.getFirstLoanRejectRetryShortDays(cond.getChannel());
    }

    /**
     * credit_pass_no_order — 授信通过且未下单？有未完成订单 = 已下单但未放款
     */
    private boolean creditPassNoOrder(ApiChannelUserCheckConditionVO cond) {
        return !havingUncompletedOrder(cond);
    }

    /**
     * credit_period — 授信间隔 <= N 天？Lazada 下单撞库不判断此节点，永远返回保护期外
     */
    private boolean creditPeriod(ApiChannelUserCheckConditionVO cond) {
        if (cond.getCheckerType() == ApiChannelUserStatusCheckerType.LAZADA_CREATE_ORDER) {
            return false;
        }
        LoanAccountVO account = getLoanAccountVOOrThrow(cond);
        long latestAcceptTime = getLatestCreditAcceptTime(cond);
        int days = Clock.getDaysBetween(latestAcceptTime, cond.getCurrentTimeMillis(), account.sdkType.getTimeZone());
        return days <= apiChannelConfig.getCreditProtectionDays(cond.getChannel());
    }
}
