package com.miyou.controllers.cashloan.repayment.billpage.strategy;

import com.yqg.core.service.cashloan.homepage.enums.IDNHomepageLoanStatusV5;
import com.yqg.core.service.cashloan.repay.enums.RepaymentReminderStrategy;
import com.yqg.core.service.cashloan.vo.CashLoanInstalmentVO;
import com.yqg.core.service.cashloan.vo.enums.HomeDisplayStrategy;
import com.yqg.core.service.loan.manualreduction.vo.ManualReductionDetail;
import com.yqg.core.service.loan.repayment.billpage.BillPageDisplayStrategyKey;
import com.yqg.core.service.loan.repayment.status.RepaymentDisplayStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@ToString
@Builder
@AllArgsConstructor
public class BillPageContext {
  /**
   * 账单页请求参数
   */
  public final BillPageParam billPageParam;
  /**
   * 用户首页状态
   */
  public final IDNHomepageLoanStatusV5 homepageLoanStatus;
  /**
   * 用户已结清账单
   */
  public final List<CashLoanInstalmentVO> paidInstalmentList;
  /**
   * 用户待还账单
   */
  public final List<CashLoanInstalmentVO> unpaidInstalmentList;
  /**
   * 用户账单降息券抵扣
   */
  public final Map<Long /*instalmentId*/, BigDecimal /* deductAmount*/> deductAmountMap;
  /**
   * 合规低息展示策略
   */
  public final HomeDisplayStrategy complianceStrategy;
  /**
   * 续借状态、不可借状态下，气泡样式展示策略
   */
  public final Supplier<HomeDisplayStrategy> reloanRejectedBubbleStrategy;
  /**
   * 还款提醒强化样式分流
   */
  public final RepaymentReminderStrategy repaymentReminderStrategy;
  /**
   * 用户还款状态
   */
  public final RepaymentDisplayStatus repaymentDisplayStatus;

  public Optional<ManualReductionDetail.InstalmentReductionDetail> getCollectionReductionDetailByInstalmentId(Long instalmentId) {
    return Optional.of(billPageParam)
        .map(p -> p.manualReductionCheckResult)
        .filter(r -> r.billPageDisplayStrategy == BillPageDisplayStrategyKey.COLLECTION_REDUCTION_STRATEGY)
        .map(r -> r.validInstalmentManualReduction)
        .map(m -> m.get(instalmentId));
  }
}
