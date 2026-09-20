package com.miyou.controllers.cashloan.repayment.billpage.strategy.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.miyou.controllers.cashloan.repayment.billpage.strategy.BillPageContext;
import com.miyou.controllers.cashloan.repayment.billpage.strategy.BillPageDisplayableInstalment;
import com.miyou.controllers.cashloan.repayment.billpage.strategy.infra.AbstractBillPageDisplayStrategy;
import com.miyou.controllers.cashloan.response.instalment.*;
import com.miyou.controllers.cashloan.response.instalment.details.ReminderGuideResponse;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.enums.CashLoanRepayStrategyStatus;
import com.yqg.core.service.cashloan.cashloanrepaystrategy.vo.CashLoanRepayStrategyVO;
import com.yqg.core.service.cashloan.util.rate.CalcFeeUtil;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionDetail;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionTaskVO;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import com.yqg.core.service.loan.repayment.status.RepaymentDisplayStatus;
import com.yqg.core.service.loan.repayment.status.RepaymentStatusSupport;
import com.yqg.ec.common.i18n.AmountFormatter;
import com.yqg.ec.common.utils.EcAsserts;
import com.yqg.translation.client.utils.TT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.miyou.controllers.cashloan.response.v5.pagev3.element.TextElement.TextColor.*;

@Slf4j
@Component
public class CustomerServiceReductionBillPageDisplayStrategy extends AbstractBillPageDisplayStrategy {

  @Override
  public BillPageDisplayStrategyKey getKey() {
    return BillPageDisplayStrategyKey.CUSTOMER_SERVICE_REDUCTION_STRATEGY;
  }

  @Override
  protected BillPageDisplayableInstalment calculateSortedBillPageInstalmentList(BillPageContext ctx) {
    if (ctx.repaymentReminderStrategy.isC()) {
      return calculateSortedBillPageInstalmentListV2(ctx);
    }
    return calculateSortedBillPageInstalmentListV1(ctx);
  }

  private BillPageDisplayableInstalment calculateSortedBillPageInstalmentListV2(BillPageContext ctx) {
    List<CashLoanInstalmentVO> customerReductionInstalmentList = Lists.newArrayList();
    List<CashLoanInstalmentVO> normalInstalmentList = Lists.newArrayList();
    ImmutableMap<Long, ManualReductionDetail.InstalmentReductionDetail> reduction = ctx.billPageParam.manualReductionCheckResult.validInstalmentManualReduction;
    for (CashLoanInstalmentVO instalment : ctx.unpaidInstalmentList) {
      if (reduction.containsKey(instalment.id)) {
        customerReductionInstalmentList.add(instalment);
      } else {
        normalInstalmentList.add(instalment);
      }
    }
    customerReductionInstalmentList.sort(RepaymentStatusSupport.UNPAID_INSTALMENT_COMPARATOR);
    normalInstalmentList.sort(RepaymentStatusSupport.UNPAID_INSTALMENT_COMPARATOR);

    List<CashLoanInstalmentVO> unpaidInstalmentList = Lists.newArrayList();
    unpaidInstalmentList.addAll(customerReductionInstalmentList);
    unpaidInstalmentList.addAll(normalInstalmentList);

    List<CashLoanInstalmentVO> paidInstalmentList = ctx.paidInstalmentList.stream().sorted(RepaymentStatusSupport.PAID_INSTALMENT_COMPARATOR).collect(Collectors.toList());
    return new BillPageDisplayableInstalment(unpaidInstalmentList, paidInstalmentList);
  }

  private BillPageDisplayableInstalment calculateSortedBillPageInstalmentListV1(BillPageContext ctx) {
    List<CashLoanInstalmentVO> unpaidInstalmentList = ctx.unpaidInstalmentList;
    ManualReductionTaskVO manualReductionTask = ctx.billPageParam.manualReductionCheckResult.validManualReductionTask;
    EcAsserts.assertTrue(Objects.nonNull(manualReductionTask), "Manual reduction task must be present when calculate displayable instalment for customer service reduction, loanAccountId = {}", ctx.billPageParam.loanAccountId);
    List<Long> customerServiceInstalmentIdList = cashLoanRepaymentStrategyService.findByReductionTask(manualReductionTask)
        .stream()
        .filter(strategy -> CashLoanRepayStrategyStatus.VALID == strategy.getStatus())
        .map(CashLoanRepayStrategyVO::getInstalmentId)
        .collect(Collectors.toList());
    // 在客服减免生效的情况下，查询账单时（无论查询已结清还是待还），待还账单只保留客服减免的账单
    if (CollectionUtils.isNotEmpty(customerServiceInstalmentIdList)) {
      unpaidInstalmentList = cashLoanInstalmentService.findListByInstalmentIds(customerServiceInstalmentIdList);
    }
    return BillPageDisplayableInstalment.builder()
        .unpaidInstalmentList(unpaidInstalmentList.stream().sorted(RepaymentStatusSupport.UNPAID_INSTALMENT_COMPARATOR).collect(Collectors.toList()))
        .paidInstalmentList(ctx.paidInstalmentList.stream().sorted(RepaymentStatusSupport.PAID_INSTALMENT_COMPARATOR).collect(Collectors.toList()))
        .build();
  }

  @Override
  protected Map<Integer, InstalmentIndexContentResponse> buildInstalmentIndices(BillPageContext billPageContext, InstalmentListResponse instalmentListResponse) {
    if (billPageContext.repaymentDisplayStatus == RepaymentDisplayStatus.NO_OUTSTANDING) {
      return Collections.emptyMap();
    }
    // 筛选出待还账单 ID 列表
    // 代码走到这里 instalmentListResponse.instalmentList 一定是待还账单列表。
    // 如果用户查询的是已还账单列表，就会被提前拦截返回，不会走到强化样式的处理逻辑中。
    Set<Long> unpaidInstalmentIds = instalmentListResponse.instalmentList.stream().map(i -> i.instalmentId).collect(Collectors.toSet());

    // 从客服减免涉及到的账单中，筛选出待还的账单。
    // validStrategyInstalmentIds 与 unpaidInstalmentIds 两个集合求交集，是为了避免：
    // 账单已结清 (cash_loan_instalment.status = Complete) 但 cash_loan_repayment_strategy.status 尚未被 Job 更新成 Invalid 的情况。
    int index = (int) billPageContext.billPageParam.manualReductionCheckResult.validInstalmentManualReduction
        .keySet().stream().filter(unpaidInstalmentIds::contains).count();
    return ImmutableMap.of(index, InstalmentIndexContentResponse.from(TT.gen("剩余待还账单")));
  }

  @Override
  protected LoanOrderAmountResponse buildLoanOrderAmountResponse(BillPageContext billPageContext, InstalmentListResponse instalmentListResponse) {
    LoanOrderAmountResponse amountResponse = super.buildLoanOrderAmountResponse(billPageContext, instalmentListResponse);
    if (Objects.isNull(amountResponse)) {
      return null;
    }
    Set<Long> customerReductionInstalmentIdList = billPageContext.billPageParam.manualReductionCheckResult.validInstalmentManualReduction.keySet();
    BigDecimal recentUnpaidAmount = billPageContext.unpaidInstalmentList.stream()
        .filter(instalment -> customerReductionInstalmentIdList.contains(instalment.id))
        .map(CalcFeeUtil::getOwedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return amountResponse.mutate()
        .urgentUnpaidAmountTitle(TT.gen("减免后待还"))
        .urgentUnpaidAmount(AmountFormatter.format(billPageContext.billPageParam.sdkType.getCurrency(), recentUnpaidAmount))
        .build();
  }

  @Override
  protected void postBuildInstalmentResponse(InstalmentResponse instalmentResponse, BillPageContext billPageContext, CashLoanInstalmentVO instalment) {
    ImmutableMap<Long, ManualReductionDetail.InstalmentReductionDetail> reduction = billPageContext.billPageParam.manualReductionCheckResult.validInstalmentManualReduction;
    instalmentResponse.disable = !reduction.containsKey(instalment.id);
  }


  @Override
  protected ReminderGuideResponse buildRepaymentReminderGuide(BillPageContext billPageContext) {
    return ReminderGuideResponse.builder()
        .subTitle(TT.gen("您的客服减免将于今日24:00到期，请及时还款！"))
        .subTitleColor(BROWN)
        .iconUrl(repaymentConfig.getRepaymentOrderCardReminderIconUrl().get(RepaymentDisplayStatus.HAS_UNPAID_INSTALMENT_WITHIN_24H))
        .bgColor(BEIGE)
        .build();
  }

  @Override
  protected TimeToBillingResponse buildTimeToBillingResponse(BillPageContext billPageContext, InstalmentListResponse instalmentListResponse, InstalmentResponse instalment) {
    TimeToBillingResponse timeToBillingResponse = super.buildTimeToBillingResponse(billPageContext, instalmentListResponse, instalment);
    ImmutableMap<Long, ManualReductionDetail.InstalmentReductionDetail> reduction = billPageContext.billPageParam.manualReductionCheckResult.validInstalmentManualReduction;
    if (!reduction.containsKey(instalment.instalmentId)) {
      timeToBillingResponse.bgColor = GRAY_999;
    }
    return timeToBillingResponse;
  }
}
