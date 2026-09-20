package com.miyou.controllers.cashloan.newhomepage.elementmodel.repayment;

import com.miyou.controllers.cashloan.newhomepage.content.vo.HomePageContext;
import com.miyou.controllers.cashloan.repayment.card.RepaymentContext;
import com.yqg.core.model.sql.loan.account.enums.MultiLoanStatus;
import com.yqg.core.service.cashloan.apireturncredit.ApiReturnCreditEligibilityCommand;
import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.homepage.vo.HomepageUserParamsVO;
import com.yqg.core.service.cashloan.homepage.vo.UserDeviceContextVO;
import com.yqg.core.service.cashloan.ordercenter.vo.OrderInstalment;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * 将首页聚合上下文转换为 API 回端额度 core 资格请求。
 */
@Component
public class ApiReturnCreditEligibilityRequestFactory {

  public ApiReturnCreditEligibilityCommand create(
      HomePageContext homePageContext, RepaymentContext repaymentContext) {
    HomepageUserParamsVO params = homePageContext.getHomepageUserParamsVO();
    UserDeviceContextVO deviceContext = homePageContext.getUserDeviceContextVO();
    return ApiReturnCreditEligibilityCommand.builder()
        .userId(homePageContext.getUserId())
        .loanAccountId(homePageContext.getLoanAccountId())
        .build(deviceContext == null ? null : deviceContext.getBuild())
        .clientType(deviceContext == null ? null : deviceContext.getClientType())
        .currentSourceType(deviceContext == null ? null : deviceContext.getSourceType())
        .targetHomepageStatus(isTargetStatus(homePageContext.getStatus()))
        .outstanding(hasOutstanding(params))
        .overdue(params != null && params.isOverdue())
        .canCreateOrder(homePageContext.getStatus() != null
            && homePageContext.getStatus().canCreateOrder())
        .creditAssessmentAvailable(
            isCreditAssessmentAvailable(params, homePageContext.getStatus()))
        .internalAvailableCredit(hasInternalAvailableCredit(homePageContext.getStatus()))
        .internalCreditProcessActive(isInternalCreditProcessActive(homePageContext.getStatus()))
        .multiLoanStatus(params == null ? null : params.getMultiLoanStatus())
        .revolvingLoanUser(params != null && Boolean.TRUE.equals(params.getRevolvingLoanUser()))
        .completedInstalment(hasCompletedInstalment(params))
        .latestBusinessEligibilityChecker(() ->
            isLatestBusinessEligible(homePageContext, repaymentContext))
        .build();
  }

  private boolean isLatestBusinessEligible(
      HomePageContext homePageContext, RepaymentContext repaymentContext) {
    HomepageUserParamsVO params = homePageContext.getHomepageUserParamsVO();
    return repaymentContext.hasOutstanding()
        && isTargetStatus(homePageContext.getStatus())
        && hasOutstanding(params)
        && params != null
        && !params.isOverdue()
        && params.getMultiLoanStatus() == MultiLoanStatus.INVALID
        && !Boolean.TRUE.equals(params.getRevolvingLoanUser())
        && !homePageContext.getStatus().canCreateOrder();
  }

  private boolean isTargetStatus(IDNHomepageLoanStatusV5 status) {
    return status == IDNHomepageLoanStatusV5.READY
        || status == IDNHomepageLoanStatusV5.RELOAN_READY;
  }

  private boolean isCreditAssessmentAvailable(
      HomepageUserParamsVO params, IDNHomepageLoanStatusV5 status) {
    return isTargetStatus(status)
        && params != null
        && params.getMultiLoanStatus() == MultiLoanStatus.INVALID;
  }

  private boolean hasInternalAvailableCredit(IDNHomepageLoanStatusV5 status) {
    return status != null && status.canCreateOrder();
  }

  private boolean isInternalCreditProcessActive(IDNHomepageLoanStatusV5 status) {
    return status != null && !isTargetStatus(status) && !status.overdueStatus();
  }

  /**
   * 当前是否有在贷订单；不限制订单来源（端内或任意端外渠道均可）。
   */
  private boolean hasOutstanding(HomepageUserParamsVO params) {
    return readyOrders(params).stream()
        .map(orderInstalment -> orderInstalment.orderVO)
        .anyMatch(order -> order != null);
  }

  /**
   * 任一在贷订单是否已完整结清一期；不限制订单来源。
   */
  private boolean hasCompletedInstalment(HomepageUserParamsVO params) {
    return readyOrders(params).stream()
        .filter(orderInstalment -> orderInstalment.orderVO != null)
        .flatMap(orderInstalment -> orderInstalment.instalmentVOS == null
            ? Stream.empty()
            : orderInstalment.instalmentVOS.stream())
        .anyMatch(instalment -> instalment.status == CashLoanInstalmentStatus.COMPLETE);
  }

  private List<OrderInstalment> readyOrders(HomepageUserParamsVO params) {
    return Optional.ofNullable(params)
        .map(HomepageUserParamsVO::getReadyOrderList)
        .orElse(Collections.emptyList());
  }
}
