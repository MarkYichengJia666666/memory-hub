package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.service.apichannel.collision.entity.CollisionProcessNode;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.ec.common.i18n.time.Clock;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @ClassName: InLoanBranchDecisions
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树在贷阶段（3 个节点）：在贷、最大逾期、坏账结清周期
 */
@Component
public class InLoanBranchDecisions extends AbstractBranchDecision {

    @Override
    public Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> decisions() {
        Map<CollisionProcessNode, Predicate<ApiChannelUserCheckConditionVO>> map = new HashMap<>();
        map.put(CollisionProcessNode.BRANCH_IS_IN_LOAN, this::isInLoan);
        map.put(CollisionProcessNode.BRANCH_MAX_OVERDUE, this::maxOverdue);
        map.put(CollisionProcessNode.BRANCH_SETTLE_720D, this::settle720d);
        return map;
    }

    /**
     * is_in_loan — 在贷？
     */
    private boolean isInLoan(ApiChannelUserCheckConditionVO cond) {
        return havingUncompletedOrder(cond);
    }

    /**
     * max_overdue — 历史最大逾期 > N 天？从 cash_loan_instalment 表查询
     */
    private boolean maxOverdue(ApiChannelUserCheckConditionVO cond) {
        int threshold = apiChannelConfig.getBadDebtOverdueDaysThreshold(cond.getChannel());
        return ecOrderService.hasHistoricalOverdueInstalmentWithDays(cond.getUserId(), threshold + 1);
    }

    /**
     * settle_bad_debt — 结清间隔 <= 720 天？
     */
    private boolean settle720d(ApiChannelUserCheckConditionVO cond) {
        LoanAccountVO account = getLoanAccountVOOrThrow(cond);
        CashLoanOrderVO lastCompleted = getLastCompletedOrderOrNull(cond);
        if (lastCompleted == null) {
            return false;
        }
        int days = Clock.getDaysBetween(lastCompleted.timeCompleted, cond.getCurrentTimeMillis(), account.sdkType.getTimeZone());
        return days <= apiChannelConfig.getSettleBadDebtLongDays(cond.getChannel());
    }
}
