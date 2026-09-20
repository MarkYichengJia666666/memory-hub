package com.miyou.controllers.cashloan.repayment.calculate.impl;

import com.miyou.controllers.cashloan.repayment.calculate.RepaymentCalculateRequest;
import com.miyou.controllers.cashloan.repayment.calculate.RepaymentCalculateResponse;
import com.miyou.controllers.cashloan.repayment.calculate.RepaymentCollectionReductionCalculateResponse;
import com.miyou.controllers.cashloan.repayment.calculate.infra.RepaymentCalculateScene;
import com.miyou.controllers.cashloan.repayment.calculate.infra.RepaymentCalculator;
import com.yqg.common.util.math.BigDecimalHelper;
import com.yqg.core.service.cashloan.ordercenter.CashLoanInstalmentService;
import com.yqg.core.service.cashloan.util.rate.CalcFeeUtil;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.loan.manualreduction.ManualReductionTaskService;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionCheckResult;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionDetail;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import com.yqg.core.service.loan.repayment.status.RepaymentStatusSupport;
import com.yqg.core.service.loan.viewercontext.LoanApiViewerContext;
import com.yqg.ec.common.enums.order.CashLoanInstalmentStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CollectionReductionCalculator implements RepaymentCalculator {

  @Autowired
  private ManualReductionTaskService manualReductionTaskService;
  @Autowired
  private CashLoanInstalmentService cashLoanInstalmentService;

  @Override
  public RepaymentCalculateScene getScene() {
    return RepaymentCalculateScene.COLLECTION_REDUCTION;
  }

  @Override
  public void calculateRepaymentInfo(
      RepaymentCalculateRequest request,
      LoanApiViewerContext viewerContext,
      RepaymentCalculateResponse.RepaymentCalculateResponseBuilder builder
  ) {
    ManualReductionCheckResult manualReductionCheckResult = manualReductionTaskService.checkManualReduction(viewerContext.userId, viewerContext.loanAccountId, viewerContext.build);

    RepaymentCollectionReductionCalculateResponse collectionReduction;
    if (BillPageDisplayStrategyKey.COLLECTION_REDUCTION_STRATEGY == manualReductionCheckResult.billPageDisplayStrategy) {
      collectionReduction = doCalculateRepaymentInfo(request, viewerContext, manualReductionCheckResult);
    } else {
      collectionReduction = RepaymentCollectionReductionCalculateResponse.noCollectionReductionResponse();
    }
    builder.collectionReduction(collectionReduction);
  }

  private RepaymentCollectionReductionCalculateResponse doCalculateRepaymentInfo(
      RepaymentCalculateRequest request,
      LoanApiViewerContext viewerContext,
      ManualReductionCheckResult manualReductionCheckResult
  ) {
    List<CashLoanInstalmentVO> unpaidInstalmentList = cashLoanInstalmentService
        .fetchByUserIdAndStatusList(viewerContext.userId, CashLoanInstalmentStatus.INIT)
        .stream()
        .sorted(RepaymentStatusSupport.UNPAID_INSTALMENT_COMPARATOR)
        .collect(Collectors.toList());

    int lastIndex = findLastCollectionReductionInstalmentIndex(unpaidInstalmentList, manualReductionCheckResult);
    BigDecimal allCoveredRepaymentAmount = calculateAllCoveredRepaymentAmount(lastIndex, unpaidInstalmentList, manualReductionCheckResult);
    if (BigDecimalHelper.compareTo(request.amount, allCoveredRepaymentAmount) < 0) {
      return RepaymentCollectionReductionCalculateResponse.partialCoveredResponse(
          viewerContext.sdkType.getCurrency(),
          allCoveredRepaymentAmount.subtract(request.amount),
          allCoveredRepaymentAmount
      );
    }
    return RepaymentCollectionReductionCalculateResponse.allCoveredResponse();
  }

  /**
   * 查找最后一期催收减免账单的索引下标
   */
  private int findLastCollectionReductionInstalmentIndex(
      List<CashLoanInstalmentVO> unpaidInstalmentList,
      ManualReductionCheckResult manualReductionCheckResult
  ) {
    int lastIndex = 0;
    for (int i = unpaidInstalmentList.size() - 1; i >= 0; i--) {
      if (manualReductionCheckResult.validInstalmentManualReduction.containsKey(unpaidInstalmentList.get(i).id)) {
        lastIndex = i;
        break;
      }
    }
    return lastIndex;
  }

  /**
   * 计算覆盖所有催收减免账单至少应进行支付的还款金额
   */
  private BigDecimal calculateAllCoveredRepaymentAmount(
      int lastIndex,
      List<CashLoanInstalmentVO> unpaidInstalmentList,
      ManualReductionCheckResult manualReductionCheckResult
  ) {
    BigDecimal allCoveredRepaymentAmount = BigDecimal.ZERO;
    for (int i = 0; i <= lastIndex; i++) {
      CashLoanInstalmentVO instalment = unpaidInstalmentList.get(i);
      BigDecimal owedAmount = Optional.of(manualReductionCheckResult.validInstalmentManualReduction)
          .map(c -> c.get(instalment.id))
          .map(ManualReductionDetail.InstalmentReductionDetail::getRemainAmount)
          .orElse(CalcFeeUtil.getOwedAmount(instalment));
      allCoveredRepaymentAmount = allCoveredRepaymentAmount.add(owedAmount);
    }
    return allCoveredRepaymentAmount;
  }

}
