package com.yqg.core.service.apichannel.collision.branch;

import com.yqg.core.service.apichannel.config.ApiChannelConfig;
import com.yqg.core.service.apichannel.enums.ApiChannelUserStatusCheckerType;
import com.yqg.core.service.apichannel.vo.ApiChannelCommonCreditData;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckConditionVO;
import com.yqg.core.service.apichannel.vo.ApiChannelUserCheckOrderConditionVO;
import com.yqg.core.service.apichannel.user.ApiChannelCreditsService;
import com.yqg.core.service.cashloan.LoanAccountDetailsService;
import com.yqg.core.service.cashloan.ordercenter.EcOrderService;
import com.yqg.core.service.cashloan.trace.LoanUserRiskTraceService;
import com.yqg.core.service.cashloan.vo.CashLoanOrderVO;
import com.yqg.core.service.cashloan.vo.LoanUserRiskTraceVO;
import com.yqg.core.service.loan.account.LoanAccountService;
import com.yqg.core.service.loan.vo.LoanAccountDetailsSimpleVO;
import com.yqg.core.service.loan.vo.LoanAccountVO;
import com.yqg.core.service.loan.vo.LoanUserCreditsInfoVO;
import com.yqg.core.service.user.UserService;
import com.yqg.core.service.user.vo.UserSimpleInfoVO;
import com.yqg.ec.common.enums.order.CashLoanOrderStatus;
import com.yqg.ec.common.enums.risk.LoanUserRiskType;
import com.yqg.ec.common.utils.EcAsserts;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @ClassName: AbstractBranchDecision
 * @Author: sensen
 * @Date: 2026/4/9
 * @Description: 决策树领域决策基类，提供公共依赖注入和 conditionVO 懒加载方法
 */
public abstract class AbstractBranchDecision implements BranchDecisionGroup {

    @Autowired
    protected UserService userService;

    @Autowired
    protected ApiChannelConfig apiChannelConfig;

    @Autowired
    protected LoanAccountService loanAccountService;

    @Autowired
    protected EcOrderService ecOrderService;

    @Autowired
    protected LoanUserRiskTraceService loanUserRiskTraceService;

    @Autowired
    protected LoanAccountDetailsService loanAccountDetailsService;

    @Autowired
    protected ApiChannelCreditsService apiChannelCreditsService;

    @Autowired
    protected ChannelCreditApplicationService channelCreditApplicationService;

    // ========== 懒加载方法（与 AbstractApiUserChecker 风格一致） ==========

    protected UserSimpleInfoVO getUserSimpleInfoVOOrThrow(ApiChannelUserCheckConditionVO cond) {
        if (BooleanUtils.isTrue(cond.getQueriedUserSimpleInfo())) {
            return cond.getUserSimpleInfoVO();
        }
        EcAsserts.assertNotNull(cond.getUserId(), "userId should not be null, mobileMd5:{}", cond.getMobileNumberMd5());
        UserSimpleInfoVO vo = userService.fetchByIdOrThrow(cond.getUserId());
        cond.setUserSimpleInfoVO(vo);
        cond.setQueriedUserSimpleInfo(true);
        return vo;
    }

    protected LoanAccountVO getLoanAccountVOOrThrow(ApiChannelUserCheckConditionVO cond) {
        if (BooleanUtils.isTrue(cond.getQueriedLoanAccountVO())) {
            return cond.getLoanAccountVO();
        }
        EcAsserts.assertNotNull(cond.getAccountId(), "accountId should not be null, mobileMd5:{}", cond.getMobileNumberMd5());
        LoanAccountVO vo = loanAccountService.getLoanAccountVO(cond.getAccountId());
        cond.setLoanAccountVO(vo);
        cond.setQueriedLoanAccountVO(true);
        return vo;
    }

    protected LoanUserCreditsInfoVO getCreditsInfoVOOrNull(ApiChannelUserCheckConditionVO cond) {
        if (BooleanUtils.isTrue(cond.getQueriedCreditInfoVO())) {
            return cond.getCreditsInfoVO();
        }
        EcAsserts.assertNotNull(cond.getAccountId(), "accountId should not be null, mobileMd5:{}", cond.getMobileNumberMd5());
        LoanUserCreditsInfoVO vo = loanAccountService.getCreditsInfo(cond.getAccountId());
        cond.setCreditsInfoVO(vo);
        cond.setQueriedCreditInfoVO(true);
        return vo;
    }

    protected ApiChannelCommonCreditData refreshCreditData(ApiChannelUserCheckConditionVO cond) {
        EcAsserts.assertNotNull(cond.getUserId(), "userId should not be null, mobileMd5:{}", cond.getMobileNumberMd5());
        ApiChannelCommonCreditData creditData = apiChannelCreditsService.getCreditData(cond.getUserId(), cond.getChannel());
        cond.setCreditData(creditData);
        cond.setQueriedCreditData(true);
        return creditData;
    }

    protected boolean havingUncompletedOrder(ApiChannelUserCheckConditionVO cond) {
        List<CashLoanOrderVO> list = getUncompletedOrderList(cond);
        return CollectionUtils.isNotEmpty(list);
    }

    protected List<CashLoanOrderVO> getUncompletedOrderList(ApiChannelUserCheckConditionVO cond) {
        ApiChannelUserCheckOrderConditionVO orderCond = cond.getOrderConditionVO();
        if (BooleanUtils.isTrue(orderCond.getQueriedUncompletedOrder())) {
            return orderCond.getUncompletedOrderList();
        }
        EcAsserts.assertNotNull(cond.getAccountId(), "accountId should not be null, mobileMd5:{}", cond.getMobileNumberMd5());
        List<CashLoanOrderVO> list = ecOrderService.getCashLoanOrdersByLoanAccountIdAndStatuses(
                cond.getAccountId(), CashLoanOrderStatus.UNDONE_STATUSES);
        orderCond.setUncompletedOrderList(list);
        orderCond.setQueriedUncompletedOrder(true);
        return list;
    }

    protected CashLoanOrderVO getLastCompletedOrderOrNull(ApiChannelUserCheckConditionVO cond) {
        ApiChannelUserCheckOrderConditionVO orderCond = cond.getOrderConditionVO();
        if (BooleanUtils.isTrue(orderCond.getQueriedLastCompletedOrder())) {
            return orderCond.getLastCompletedOrder();
        }
        EcAsserts.assertNotNull(cond.getAccountId(), "accountId should not be null, mobileMd5:{}", cond.getMobileNumberMd5());
        CashLoanOrderVO vo = ecOrderService.getLatestCompleteOrder(cond.getAccountId());
        orderCond.setLastCompletedOrder(vo);
        orderCond.setQueriedLastCompletedOrder(true);
        return vo;
    }

    // ========== 新增懒加载方法（决策树专用） ==========

    protected LoanAccountDetailsSimpleVO getDetailsSimpleVOOrNull(ApiChannelUserCheckConditionVO cond) {
        if (BooleanUtils.isTrue(cond.getQueriedDetailsSimple())) {
            return cond.getDetailsSimpleVO();
        }
        LoanAccountDetailsSimpleVO details = loanAccountDetailsService.getSimpleByAccountIdOrNull(cond.getAccountId());
        cond.setDetailsSimpleVO(details);
        cond.setQueriedDetailsSimple(true);
        return details;
    }

    protected LoanUserRiskTraceVO getActiveRiskTraceOrNull(ApiChannelUserCheckConditionVO cond) {
        if (BooleanUtils.isTrue(cond.getQueriedActiveRiskTrace())) {
            return cond.getActiveRiskTrace();
        }
        LoanAccountVO account = getLoanAccountVOOrThrow(cond);
        LoanUserRiskTraceVO trace = loanUserRiskTraceService
                .findLatestCreditRiskByAccountIdAndRiskTypes(account.id, LoanUserRiskType.COLLISION_RISK_TYPES);
        cond.setActiveRiskTrace(trace);
        cond.setQueriedActiveRiskTrace(true);
        return trace;
    }

    /**
     * 判断当前是否为 Lazada 下单撞库 + 新决策树流程，需跳过前置步骤
     */
    protected boolean isLazadaCreateOrderUpgrade(ApiChannelUserCheckConditionVO cond) {
        return BooleanUtils.isTrue(cond.getUpgradeFlow())
                && cond.getCheckerType() == ApiChannelUserStatusCheckerType.LAZADA_CREATE_ORDER;
    }

    /**
     * 取三个来源最晚的授信通过时间：trace.timeUpdated、Gopay/Lazada credit application 表 APPROVED 记录的 time_updated
     */
    protected long getLatestCreditAcceptTime(ApiChannelUserCheckConditionVO cond) {
        if (BooleanUtils.isTrue(cond.getQueriedLatestCreditAcceptTime())) {
            return cond.getLatestCreditAcceptTime();
        }
        LoanUserRiskTraceVO trace = getActiveRiskTraceOrNull(cond);
        long traceTime = (trace != null && trace.timeUpdated != null) ? trace.timeUpdated : 0L;

        Long gopayTime = channelCreditApplicationService.getLatestGopayAcceptedTimeUpdated(cond.getUserId());
        Long lazadaTime = channelCreditApplicationService.getLatestLazadaAcceptedTimeUpdated(cond.getUserId());

        long latest = Math.max(traceTime,
                Math.max(gopayTime != null ? gopayTime : 0L, lazadaTime != null ? lazadaTime : 0L));
        cond.setLatestCreditAcceptTime(latest);
        cond.setQueriedLatestCreditAcceptTime(true);
        return latest;
    }
}
